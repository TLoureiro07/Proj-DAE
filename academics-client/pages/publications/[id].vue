<template>
  <div>
    <div v-if="loading">A carregar...</div>
    <div v-else-if="error">{{ error }}</div>
    <div v-else-if="publication">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
      <h1>{{ publication.title || 'Sem título' }}</h1>
        <div style="display: flex; gap: 0.5rem;">
          <button
            v-if="isOwner || isResponsible"
            @click="openEditModal"
            class="btn btn-small btn-primary"
          >
            ✏️ Editar
          </button>
          <button
            v-if="isOwner || isResponsible"
            @click="showHistory"
            class="btn btn-small btn-secondary"
          >
            📋 Histórico
          </button>
          <button
            v-if="isResponsible"
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
        <p v-if="publication.visibility === 'hidden'" style="color: red; font-weight: bold;">
          [Publicação Ocultada]
        </p>
        <p v-if="publication.summary"><strong>Resumo:</strong> {{ publication.summary }}</p>
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
        <p v-if="publication.fileName" style="margin: 15px 0;">
          <strong>Ficheiro:</strong>
          <button
            @click="downloadFile"
            class="btn btn-primary btn-small"
            :disabled="downloading"
          >
            📥 Descarregar {{ publication.fileName }}
          </button>
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

    <div v-if="showEditModal" class="modal-overlay" @click="closeEditModal">
      <div class="modal-content modal-large" @click.stop>
        <div class="modal-header">
          <h2>Editar Publicação</h2>
          <button @click="closeEditModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div v-if="saving" class="loading-state">A guardar alterações...</div>
          <div v-else-if="editError" class="error-message">{{ editError }}</div>
          <form v-else @submit.prevent="savePublication" class="edit-form">
            <div class="form-group">
              <label>Título *</label>
              <input v-model="editForm.title" type="text" required class="form-input" />
            </div>

            <div class="form-group">
              <label>Área Científica</label>
              <input v-model="editForm.scientificArea" type="text" class="form-input" />
            </div>

            <div class="form-group">
              <label>Autores (um por linha)</label>
              <textarea v-model="editAuthorsText" rows="3" class="form-textarea"></textarea>
              <small class="form-hint">Separa cada autor numa linha diferente</small>
            </div>

            <div class="form-group">
              <label>Resumo</label>
              <textarea v-model="editForm.summary" rows="5" class="form-textarea"></textarea>
            </div>

            <div class="form-group" v-if="isResponsible">
              <label>Visibilidade</label>
              <select v-model="editForm.visibility" class="form-input">
                <option value="public">Pública</option>
                <option value="internal">Interna</option>
                <option value="hidden">Oculta</option>
              </select>
            </div>

            <div class="form-group">
              <label>Tags</label>
              <div v-if="loadingTags">A carregar tags...</div>
              <div v-else-if="availableTags.length === 0" class="tags-empty">Não há tags disponíveis</div>
              <div v-else class="tags-list">
                <label v-for="tag in availableTags" :key="tag.id" class="tag-checkbox">
                  <input
                    type="checkbox"
                    :value="tag.id"
                    v-model="editForm.selectedTags"
                  />
                  <span>{{ tag.name }}</span>
                </label>
              </div>
            </div>

            <div class="form-group">
              <label>Ficheiro</label>
              <div v-if="editForm.fileName" class="current-file">
                <p>Ficheiro atual: <strong>{{ editForm.fileName }}</strong></p>
              </div>
              <div
                class="file-upload-area"
                :class="{ 'has-file': editSelectedFile !== null, 'dragover': editIsDragging }"
                @click="editFileInput?.click()"
                @dragover.prevent="handleEditDragOver"
                @dragleave.prevent="handleEditDragLeave"
                @drop.prevent="handleEditDrop"
              >
                <input
                  type="file"
                  ref="editFileInput"
                  accept=".pdf,.zip"
                  @change="handleEditFileChange"
                  class="file-input"
                />
                <div class="file-upload-content">
                  <span v-if="!editSelectedFile" class="file-placeholder">
                    Clique para selecionar ou arraste um ficheiro aqui (opcional)
                  </span>
                  <span v-else class="file-selected">
                    📄 {{ editSelectedFile.name }}
                  </span>
                </div>
              </div>
            </div>

            <div class="form-actions">
              <button type="button" @click="closeEditModal" class="btn btn-secondary">Cancelar</button>
              <button type="submit" class="btn btn-primary" :disabled="saving">Guardar Alterações</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <div v-if="showHistoryModal" class="modal-overlay" @click="closeHistoryModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>Histórico de Edições</h2>
          <button @click="closeHistoryModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div v-if="loadingHistory">A carregar histórico...</div>
          <div v-else-if="historyError" class="error-message">{{ historyError }}</div>
          <div v-else-if="history.length === 0" class="empty-history">
            <p>Não há histórico de edições para esta publicação.</p>
          </div>
          <div v-else class="history-list">
            <div v-for="entry in history" :key="entry.editId" class="history-item">
              <div class="history-header">
                <strong>{{ formatDateTime(entry.editDate) }}</strong>
                <span v-if="entry.editedBy" class="edited-by">por {{ entry.editedBy }}</span>
              </div>
              <div class="history-changes">
                <strong>Alterações:</strong>
                <ul>
                  <li v-for="change in entry.changes" :key="change">{{ change }}</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { useAuthStore } from "~/stores/auth-store.js"
import { ref, computed, onMounted } from 'vue'

const route = useRoute()
const config = useRuntimeConfig()
const api = config.public.apiBase
const authStore = useAuthStore()
const { token, user } = storeToRefs(authStore)
const editableAiSummary = ref('')
const savingAiSummary = ref(false)
const generatingAiSummary = ref(false)
const canGenerateAiSummary = computed(() => isOwner.value || isResponsible.value)

const publicationId = route.params.id

const publication = ref(null)
const comments = ref([])
const ratings = ref([])
const loading = ref(true)
const error = ref(null)
const submitting = ref(false)
const updatingVisibility = ref(false)
const removingTag = ref(false)
const togglingComment = ref(false)
const downloading = ref(false)
const showHistoryModal = ref(false)
const history = ref([])
const loadingHistory = ref(false)
const historyError = ref(null)
const showEditModal = ref(false)
const editForm = ref({
  title: '',
  scientificArea: '',
  summary: '',
  visibility: 'public',
  selectedTags: []
})
const editAuthorsText = ref('')
const availableTags = ref([])
const loadingTags = ref(false)
const saving = ref(false)
const editError = ref(null)
const editSelectedFile = ref(null)
const editIsDragging = ref(false)
const editFileInput = ref(null)

const newComment = ref({ text: '' })
const newRating = ref(5)

const isResponsible = computed(() =>
  user.value?.role === 'Responsible' || user.value?.role === 'Administrator'
)

const isOwner = computed(() =>
  publication.value && user.value && publication.value.owner === user.value.username
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
  if (newRating.value < 1 || newRating.value > 5) {
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
      body: { value: newRating.value }
    })
    await loadRatings()
    await loadPublication()
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

async function downloadFile() {
  if (!publication.value?.fileName) return
  downloading.value = true
  try {
    const response = await $fetch(`${api}/publications/${publicationId}/file`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token.value}`,
      },
      responseType: 'blob'
    })

    const url = window.URL.createObjectURL(new Blob([response]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', publication.value.fileName)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    alert('Erro ao descarregar ficheiro: ' + (e.message || 'Erro desconhecido'))
    console.error('Erro ao descarregar ficheiro:', e)
  } finally {
    downloading.value = false
  }
}

async function showHistory() {
  if (!isOwner.value && !isResponsible.value) return

  showHistoryModal.value = true
  loadingHistory.value = true
  historyError.value = null

  try {
    const response = await $fetch(`${api}/publications/${publicationId}/history`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    history.value = response.history || []
  } catch (e) {
    historyError.value = 'Erro ao carregar histórico: ' + (e.message || 'Erro desconhecido')
    console.error('Erro:', e)
  } finally {
    loadingHistory.value = false
  }
}

function closeHistoryModal() {
  showHistoryModal.value = false
  history.value = []
  historyError.value = null
}

async function openEditModal() {
  if (!isOwner.value && !isResponsible.value) return

  editForm.value = {
    title: publication.value.title || '',
    scientificArea: publication.value.scientificArea || '',
    summary: publication.value.summary || '',
    visibility: publication.value.visibility || 'public',
    selectedTags: publication.value.tags ? publication.value.tags.map(t => t.id) : [],
    fileName: publication.value.fileName || ''
  }
  editAuthorsText.value = publication.value.authors ? publication.value.authors.join('\n') : ''
  editSelectedFile.value = null
  editError.value = null
  showEditModal.value = true

  if (availableTags.value.length === 0) {
    await loadTags()
  }
}

function closeEditModal() {
  showEditModal.value = false
  editForm.value = {
    title: '',
    scientificArea: '',
    summary: '',
    visibility: 'public',
    selectedTags: [],
    fileName: ''
  }
  editAuthorsText.value = ''
  editSelectedFile.value = null
  editError.value = null
}

async function loadTags() {
  loadingTags.value = true
  try {
    const response = await $fetch(`${api}/tags`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    availableTags.value = response || []
  } catch (e) {
    console.error('Erro ao carregar tags:', e)
    availableTags.value = []
  } finally {
    loadingTags.value = false
  }
}

function handleEditDragOver(e) {
  e.preventDefault()
  editIsDragging.value = true
}

function handleEditDragLeave(e) {
  e.preventDefault()
  editIsDragging.value = false
}

function handleEditDrop(e) {
  e.preventDefault()
  editIsDragging.value = false
  const files = e.dataTransfer.files
  if (files && files.length > 0) {
    editSelectedFile.value = files[0]
  }
}

function handleEditFileChange(e) {
  const files = e.target.files
  if (files && files.length > 0) {
    editSelectedFile.value = files[0]
  }
}

async function savePublication() {
  if (!isOwner.value && !isResponsible.value) return

  saving.value = true
  editError.value = null

  try {
    const authors = editAuthorsText.value
      .split('\n')
      .map(a => a.trim())
      .filter(a => a.length > 0)

    const updateData = {
      title: editForm.value.title,
      scientificArea: editForm.value.scientificArea || null,
      summary: editForm.value.summary || null,
      authors: authors
    }

    if (isResponsible.value) {
      updateData.visibility = editForm.value.visibility
    }

    await $fetch(`${api}/publications/${publicationId}`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: updateData
    })

    if (editSelectedFile.value) {
      const formData = new FormData()
      formData.append('file', editSelectedFile.value)

      await $fetch(`${api}/publications/${publicationId}/file`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token.value}`
        },
        body: formData
      })
    }

    if (editForm.value.selectedTags.length > 0 || publication.value.tags) {
      const currentTagIds = publication.value.tags ? publication.value.tags.map(t => t.id) : []
      const tagsToAdd = editForm.value.selectedTags.filter(id => !currentTagIds.includes(id))
      const tagsToRemove = currentTagIds.filter(id => !editForm.value.selectedTags.includes(id))

      for (const tagId of tagsToAdd) {
        await $fetch(`${api}/publications/${publicationId}/tags/${tagId}`, {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${token.value}` }
        })
      }

      for (const tagId of tagsToRemove) {
        await $fetch(`${api}/publications/${publicationId}/tags/${tagId}`, {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${token.value}` }
        })
      }
    }

    await loadPublication()
    closeEditModal()
    alert('Publicação atualizada com sucesso!')
  } catch (e) {
    editError.value = 'Erro ao guardar alterações: ' + (e.message || 'Erro desconhecido')
    console.error('Erro:', e)
  } finally {
    saving.value = false
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

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #5568d3;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background: #5a6268;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #eee;
}

.modal-header h2 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 2rem;
  color: #666;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #333;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.empty-history {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.history-item {
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 1rem;
  background: #f9f9f9;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
  color: #333;
}

.edited-by {
  color: #666;
  font-size: 0.9rem;
}

.history-changes {
  margin-top: 0.5rem;
}

.history-changes ul {
  margin: 0.5rem 0 0 1.5rem;
  padding: 0;
}

.history-changes li {
  margin: 0.25rem 0;
  color: #555;
}

.error-message {
  background: #f8d7da;
  color: #721c24;
  padding: 1rem;
  border-radius: 4px;
  margin: 1rem 0;
}

.modal-large {
  max-width: 800px;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-weight: 500;
  color: #333;
}

.form-input, .form-textarea {
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  font-family: inherit;
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.form-hint {
  color: #666;
  font-size: 0.85rem;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #f9f9f9;
}

.tag-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  padding: 0.5rem;
  background: white;
  border-radius: 4px;
  border: 1px solid #ddd;
}

.tag-checkbox input[type="checkbox"] {
  cursor: pointer;
}

.tags-empty {
  padding: 1rem;
  text-align: center;
  color: #666;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.current-file {
  padding: 0.75rem;
  background: #e7f3ff;
  border-radius: 4px;
  margin-bottom: 0.5rem;
}

.file-upload-area {
  border: 2px dashed #ddd;
  border-radius: 4px;
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.file-upload-area:hover {
  border-color: #667eea;
  background: #f0f0ff;
}

.file-upload-area.dragover {
  border-color: #667eea;
  background: #e7f3ff;
}

.file-upload-area.has-file {
  border-color: #28a745;
  background: #d4edda;
}

.file-input {
  display: none;
}

.file-placeholder {
  color: #666;
}

.file-selected {
  color: #28a745;
  font-weight: 500;
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
}

.loading-state {
  text-align: center;
  padding: 2rem;
  color: #666;
}
</style>

