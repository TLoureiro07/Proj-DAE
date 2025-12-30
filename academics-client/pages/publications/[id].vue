<template>
  <div>
    <div v-if="loading">A carregar...</div>
    <div v-else-if="error">{{ error }}</div>
    <div v-else-if="publication">
      <h1>{{ publication.title || 'Sem título' }}</h1>
      
      <div style="margin: 20px 0;">
        <p><strong>Autor:</strong> {{ publication.owner }}</p>
        <p v-if="publication.summary"><strong>Resumo:</strong> {{ publication.summary }}</p>
        <p v-if="publication.ratingAvg !== null && publication.ratingAvg !== undefined">
          <strong>Rating médio:</strong> {{ publication.ratingAvg.toFixed(1) }} estrelas ({{ ratings.length }} avaliações)
        </p>
        <p v-if="publication.tags && publication.tags.length > 0">
          <strong>Tags:</strong>
          <span v-for="tag in publication.tags" :key="tag.id" style="margin-right: 5px;">
            <span style="background: #e0e0e0; padding: 2px 8px; border-radius: 3px;">{{ tag.name }}</span>
          </span>
        </p>
        <p><small>Data de upload: {{ formatDate(publication.uploadDate) }}</small></p>
      </div>

      <!-- Rating -->
      <div style="border: 1px solid #ccc; padding: 15px; margin: 20px 0; border-radius: 5px;">
        <h2>Avaliar Publicação</h2>
        <div>
          <label>Rating (1-5): </label>
          <select v-model="newRating.value">
            <option value="1">1 estrela</option>
            <option value="2">2 estrelas</option>
            <option value="3">3 estrelas</option>
            <option value="4">4 estrelas</option>
            <option value="5">5 estrelas</option>
          </select>
          <button @click="submitRating" :disabled="submitting">Avaliar</button>
        </div>
      </div>

      <!-- Comentários -->
      <div style="border: 1px solid #ccc; padding: 15px; margin: 20px 0; border-radius: 5px;">
        <h2>Comentários ({{ comments.length }})</h2>
        
        <div style="margin-bottom: 20px;">
          <textarea v-model="newComment.text" placeholder="Escreve um comentário..." rows="3" style="width: 100%;"></textarea>
          <button @click="submitComment" :disabled="submitting">Comentar</button>
        </div>

        <div v-if="comments.length === 0">
          <p>Não há comentários ainda.</p>
        </div>
        <div v-else>
          <div v-for="comment in comments" :key="comment.id" 
               style="border-top: 1px solid #eee; padding: 10px 0;"
               :style="comment.hidden ? 'opacity: 0.5;' : ''">
            <p><strong>{{ comment.authorName }}</strong> 
               <small>{{ formatDateTime(comment.commentDate) }}</small>
               <span v-if="comment.hidden" style="color: red;">[Oculto]</span>
            </p>
            <p>{{ comment.text }}</p>
          </div>
        </div>
      </div>

      <!-- Ratings existentes -->
      <div style="border: 1px solid #ccc; padding: 15px; margin: 20px 0; border-radius: 5px;">
        <h2>Avaliações ({{ ratings.length }})</h2>
        <div v-if="ratings.length === 0">
          <p>Não há avaliações ainda.</p>
        </div>
        <div v-else>
          <div v-for="rating in ratings" :key="rating.id" style="border-top: 1px solid #eee; padding: 5px 0;">
            <strong>{{ rating.authorName }}</strong>: 
            <span v-for="i in rating.value" :key="i">*</span>
            <small>{{ formatDateTime(rating.ratingDate) }}</small>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { useAuthStore } from "~/stores/auth-store.js"

const route = useRoute()
const config = useRuntimeConfig()
const api = config.public.apiBase
const authStore = useAuthStore()
const { token, user } = storeToRefs(authStore)

const publicationId = route.params.id

const publication = ref(null)
const comments = ref([])
const ratings = ref([])
const loading = ref(true)
const error = ref(null)
const submitting = ref(false)

const newComment = ref({ text: '' })
const newRating = ref({ value: 5 })

async function loadPublication() {
  loading.value = true
  error.value = null
  try {
    publication.value = await $fetch(`${api}/publications/${publicationId}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Accept': 'application/json'
      }
    })
    await loadComments()
    await loadRatings()
  } catch (e) {
    error.value = 'Erro ao carregar publicação: ' + (e.message || 'Erro desconhecido')
    console.error('Erro:', e)
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  try {
    comments.value = await $fetch(`${api}/publications/${publicationId}/comments`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Accept': 'application/json'
      }
    })
  } catch (e) {
    console.error('Erro ao carregar comentários:', e)
  }
}

async function loadRatings() {
  try {
    ratings.value = await $fetch(`${api}/publications/${publicationId}/ratings`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Accept': 'application/json'
      }
    })
  } catch (e) {
    console.error('Erro ao carregar ratings:', e)
  }
}

async function submitComment() {
  if (!newComment.value.text.trim()) return
  
  submitting.value = true
  try {
    await $fetch(`${api}/publications/${publicationId}/comments`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: { text: newComment.value.text.trim() }
    })
    newComment.value.text = ''
    await loadComments()
  } catch (e) {
    alert('Erro ao adicionar comentário: ' + (e.message || 'Erro desconhecido'))
  } finally {
    submitting.value = false
  }
}

async function submitRating() {
  submitting.value = true
  try {
    await $fetch(`${api}/publications/${publicationId}/ratings`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: { value: parseInt(newRating.value) }
    })
    await loadRatings()
    await loadPublication() // Recarregar para atualizar rating médio
  } catch (e) {
    alert('Erro ao avaliar: ' + (e.message || 'Erro desconhecido'))
  } finally {
    submitting.value = false
  }
}

function formatDate(dateString) {
  if (!dateString) return 'N/A'
  try {
    return new Date(dateString).toLocaleDateString('pt-PT')
  } catch {
    return dateString
  }
}

function formatDateTime(dateString) {
  if (!dateString) return 'N/A'
  try {
    return new Date(dateString).toLocaleString('pt-PT')
  } catch {
    return dateString
  }
}

onMounted(() => {
  if (token.value) {
    loadPublication()
  } else {
    error.value = 'Precisa de fazer login para ver publicações'
  }
})
</script>

