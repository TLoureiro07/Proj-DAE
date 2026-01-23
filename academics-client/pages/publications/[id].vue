<template>
  <div>
    <div v-if="loading">A carregar...</div>
    <div v-else-if="error">{{ error }}</div>
    <div v-else-if="publication">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
        <h1>{{ publication.title || 'Sem título' }}</h1>
        <div v-if="isResponsible" style="display: flex; gap: 0.5rem;">
          <button
            @click="togglePublicationVisibility"
            class="btn btn-small"
            :class="publication.visibility === 'hidden' ? 'btn-success' : 'btn-warning'"
            :disabled="updatingVisibility"
          >
            {{ publication.visibility === 'hidden' ? 'Mostrar Publicação' : 'Ocultar Publicação' }}
          </button>
        </div>
      </div>

      <div style="margin: 20px 0;">
        <p><strong>Autor:</strong> {{ publication.owner }}</p>
        <p v-if="publication.visibility === 'hidden'" style="color: red; font-weight: bold;">
          [Publicação Ocultada]
        </p>
        <p v-if="publication.summary">
          <strong>Resumo (Autor):</strong> {{ publication.summary }}
        </p>

        <p v-if="publication.aiSummary">
          <strong>Resumo (IA):</strong> {{ publication.aiSummary }}
        </p>
        <div v-if="publication.aiSummary !== undefined" style="margin-top: 15px;">
          <strong>Editar Resumo por IA:</strong>
          <textarea
              v-model="editableAiSummary"
              rows="5"
              style="width: 100%; margin-top: 5px; padding: 5px;"
              placeholder="Pode editar o resumo gerado pela IA aqui..."
          ></textarea>
          <button
              class="btn btn-small btn-primary"
              @click="saveAiSummary"
              :disabled="savingAiSummary"
              style="margin-top: 5px;"
          >
            {{ savingAiSummary ? 'A guardar...' : 'Guardar Resumo IA' }}
          </button>
        </div>

        <p v-else style="font-style: italic; color: #666;">
          Resumo por IA ainda não disponível.
        </p>
        <p v-if="publication.ratingAvg !== null && publication.ratingAvg !== undefined">
          <strong>Rating médio:</strong> {{ publication.ratingAvg.toFixed(1) }} estrelas ({{ ratings.length }} avaliações)
        </p>
        <p v-if="publication.tags && publication.tags.length > 0">
          <strong>Tags:</strong>
          <span v-for="tag in publication.tags" :key="tag.id" style="margin-right: 5px;">
            <span style="background: #e0e0e0; padding: 2px 8px; border-radius: 3px;">
              {{ tag.name }}
              <button
                v-if="isResponsible"
                @click="removeTag(tag.id)"
                style="margin-left: 5px; background: #dc3545; color: white; border: none; border-radius: 3px; padding: 2px 6px; cursor: pointer; font-size: 0.8em;"
                :disabled="removingTag"
                title="Desassociar tag"
              >
                ×
              </button>
            </span>
          </span>
        </p>
        <p><small>Data de upload: {{ formatDate(publication.uploadDate) }}</small></p>
        <div v-if="canGenerateAiSummary" style="margin-top: 10px;">
          <button
              @click="generateAiSummary"
              class="btn btn-small btn-primary"
              :disabled="generatingAiSummary"
          >
            {{ generatingAiSummary ? 'A gerar resumo por IA...' : 'Gerar / Atualizar Resumo por IA' }}
          </button>
        </div>
      </div>

      <!-- Rating -->
      <div style="border: 1px solid #ccc; padding: 15px; margin: 20px 0; border-radius: 5px;">
        <h2>Avaliar Publicação</h2>
        <div>
          <label>Rating (1-5): </label>
          <select v-model.number="newRating">
            <option :value="1">1 estrela</option>
            <option :value="2">2 estrelas</option>
            <option :value="3">3 estrelas</option>
            <option :value="4">4 estrelas</option>
            <option :value="5">5 estrelas</option>
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
            <div style="display: flex; justify-content: space-between; align-items: start;">
              <div style="flex: 1;">
                <p><strong>{{ comment.authorName }}</strong>
                   <small>{{ formatDateTime(comment.commentDate) }}</small>
                   <span v-if="comment.hidden" style="color: red; margin-left: 10px;">[Oculto]</span>
                </p>
                <p>{{ comment.text }}</p>
              </div>
              <div v-if="isResponsible" style="margin-left: 10px;">
                <button
                  @click="toggleCommentHidden(comment.id, !comment.hidden)"
                  class="btn btn-small"
                  :class="comment.hidden ? 'btn-success' : 'btn-warning'"
                  :disabled="togglingComment"
                  style="font-size: 0.85em; padding: 0.3rem 0.6rem;"
                >
                  {{ comment.hidden ? 'Mostrar' : 'Ocultar' }}
                </button>
              </div>
            </div>
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
            <span v-for="i in 5" :key="i" style="color: darkred;">
  {{ i <= rating.value ? '★' : '☆' }}
</span>
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
const updatingVisibility = ref(false)

const newTagId = ref('')       // selected tag ID
const allTags = ref([])        // all tags available in the system
const addingTag = ref(false)
const removingTag = ref(false)

const togglingComment = ref(false)

//edit ai summary
const editableAiSummary = ref('')
const savingAiSummary = ref(false)

const newComment = ref({ text: '' })
const newRating = ref(5)

const isResponsible = computed(() =>
  user.value?.role === 'Responsible' || user.value?.role === 'Administrator'
)

const generatingAiSummary = ref(false)

const canGenerateAiSummary = computed(() =>
    publication.value &&
    (publication.value.owner === user.value?.username ||
        user.value?.role === 'Responsible' ||
        user.value?.role === 'Administrator')
)

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
    editableAiSummary.value = publication.value.aiSummary || ''
    await loadComments()
    await loadRatings()
  } catch (e) {
    error.value = 'Erro ao carregar publicação: ' + (e.message || 'Erro desconhecido')
    console.error('Erro:', e)
  } finally {
    loading.value = false
  }
}

async function generateAiSummary() {
  if (!canGenerateAiSummary.value) return

  generatingAiSummary.value = true
  try {
    await $fetch(`${api}/publications/${publicationId}/generate-summary`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`
      }
    })

    await loadPublication()
    alert('Resumo por IA atualizado com sucesso!')
  } catch (e) {
    alert('Erro ao gerar resumo por IA: ' + (e.message || 'Erro desconhecido'))
  } finally {
    generatingAiSummary.value = false
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
  // Garantir que temos um valor válido
  const ratingValue = newRating.value

  if (!ratingValue || isNaN(ratingValue) || ratingValue < 1 || ratingValue > 5) {
    alert('Por favor, seleciona um rating válido entre 1 e 5')
    return
  }

  submitting.value = true
  try {
    await $fetch(`${api}/publications/${publicationId}/ratings`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: { value: ratingValue }
    })
    await loadRatings()
    await loadPublication() // Recarregar para atualizar rating médio
  } catch (e) {
    console.error('Erro completo no rating:', e)
    let errorMsg = 'Erro ao avaliar'
    if (e.data?.error) {
      errorMsg += ': ' + e.data.error
    } else if (e.response?._data?.error) {
      errorMsg += ': ' + e.response._data.error
    } else if (e.message) {
      errorMsg += ': ' + e.message
    }
    alert(errorMsg)
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

async function togglePublicationVisibility() {
  if (!isResponsible.value) return

  updatingVisibility.value = true
  try {
    const newVisibility = publication.value.visibility === 'hidden' ? 'public' : 'hidden'
    await $fetch(`${api}/publications/${publicationId}`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: { visibility: newVisibility }
    })
    await loadPublication()
    alert(`Publicação ${newVisibility === 'hidden' ? 'ocultada' : 'mostrada'} com sucesso!`)
  } catch (e) {
    alert('Erro ao alterar visibilidade: ' + (e.message || 'Erro desconhecido'))
  } finally {
    updatingVisibility.value = false
  }
}

async function removeTag(tagId) {
  if (!isResponsible.value) return
  if (!confirm('Tens a certeza que queres desassociar esta tag da publicação?')) return

  removingTag.value = true
  try {
    await $fetch(`${api}/publications/${publicationId}/tags/${tagId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token.value}`
      }
    })
    await loadPublication()
    alert('Tag desassociada com sucesso!')
  } catch (e) {
    alert('Erro ao desassociar tag: ' + (e.message || 'Erro desconhecido'))
  } finally {
    removingTag.value = false
  }
}

async function toggleCommentHidden(commentId, newHiddenState) {
  if (!isResponsible.value) return

  togglingComment.value = true
  try {
    await $fetch(`${api}/publications/${publicationId}/comments/${commentId}/hidden`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: { hidden: newHiddenState }
    })
    await loadComments()
  } catch (e) {
    alert('Erro ao alterar estado do comentário: ' + (e.message || 'Erro desconhecido'))
  } finally {
    togglingComment.value = false
  }
}

async function saveAiSummary() {
  if (!editableAiSummary.value.trim()) {
    alert('Resumo IA não pode estar vazio.')
    return
  }

  savingAiSummary.value = true
  try {
    await $fetch(`${api}/publications/${publicationId}/ai-summary`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: { aiSummary: editableAiSummary.value.trim() }
    })
    await loadPublication()
    alert('Resumo por IA atualizado com sucesso!')
  } catch (e) {
    alert('Erro ao guardar resumo IA: ' + (e.message || 'Erro desconhecido'))
  } finally {
    savingAiSummary.value = false
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

<style scoped>
.btn {
  padding: 0.5rem 1rem;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
  font-size: 1rem;
}

.btn-small {
  padding: 0.4rem 0.8rem;
  font-size: 0.9rem;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #218838;
}

.btn-warning {
  background: #ffc107;
  color: #333;
}

.btn-warning:hover:not(:disabled) {
  background: #e0a800;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

