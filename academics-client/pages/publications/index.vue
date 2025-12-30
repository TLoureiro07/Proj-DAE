<template>
  <div>
    <h1>Publicações</h1>
    
    <div v-if="loading">A carregar...</div>
    <div v-else-if="error">{{ error }}</div>
    <div v-else>
      <div v-if="publications.length === 0">
        <p>Não há publicações disponíveis.</p>
      </div>
      <div v-else>
        <div v-for="pub in publications" :key="pub.id" style="border: 1px solid #ccc; padding: 15px; margin: 10px 0; border-radius: 5px;">
          <h3>
            <nuxt-link :to="`/publications/${pub.id}`">{{ pub.title || 'Sem título' }}</nuxt-link>
          </h3>
          <p><strong>Autor:</strong> {{ pub.owner || 'N/A' }}</p>
          <p v-if="pub.summary"><strong>Resumo:</strong> {{ pub.summary }}</p>
          <p v-if="pub.ratingAvg !== null && pub.ratingAvg !== undefined">
            <strong>Rating:</strong> {{ pub.ratingAvg.toFixed(1) }} ⭐
          </p>
          <p v-if="pub.tags && pub.tags.length > 0">
            <strong>Tags:</strong>
            <span v-for="tag in pub.tags" :key="tag.id" style="margin-right: 5px;">
              <span style="background: #e0e0e0; padding: 2px 8px; border-radius: 3px;">{{ tag.name }}</span>
            </span>
          </p>
          <p><small>Data: {{ formatDate(pub.uploadDate) }}</small></p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { useAuthStore } from "~/stores/auth-store.js"

const config = useRuntimeConfig()
const api = config.public.apiBase
const authStore = useAuthStore()
const { token } = storeToRefs(authStore)

const publications = ref([])
const loading = ref(true)
const error = ref(null)

async function loadPublications() {
  loading.value = true
  error.value = null
  try {
    const response = await $fetch(`${api}/publications`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Accept': 'application/json'
      }
    })
    publications.value = response
  } catch (e) {
    error.value = 'Erro ao carregar publicações: ' + (e.message || 'Erro desconhecido')
    console.error('Erro:', e)
  } finally {
    loading.value = false
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

onMounted(() => {
  if (token.value) {
    loadPublications()
  } else {
    error.value = 'Precisa de fazer login para ver publicações'
  }
})
</script>

