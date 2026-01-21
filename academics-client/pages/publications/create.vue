<template>
  <div class="create-publication">
    <div class="page-header">
      <h1>Criar Nova Publicação</h1>
      <p>Adiciona uma nova publicação científica à plataforma</p>
    </div>

    <div v-if="submitting" class="loading-state">
      <p>A criar publicação...</p>
    </div>

    <div v-else-if="success" class="success-state">
      <div class="success-card">
        <h2>Publicação criada com sucesso!</h2>
        <p>A tua publicação foi adicionada à plataforma.</p>
        <div class="success-actions">
          <nuxt-link :to="`/publications/${publicationId}`" class="btn btn-primary">Ver Publicação</nuxt-link>
          <nuxt-link to="/publications" class="btn btn-secondary">Voltar à Lista</nuxt-link>
        </div>
      </div>
    </div>

    <div v-else class="form-container">
      <form @submit.prevent="createPublication" class="publication-form">
        <div class="form-section">
          <h2>Informações Básicas</h2>
          
          <div class="form-group">
            <label for="title">Título *</label>
            <input 
              id="title"
              v-model="form.title" 
              type="text" 
              required 
              placeholder="Ex: Deep Learning Applications in Medical Imaging"
              class="form-input"
            />
          </div>

          <div class="form-group">
            <label for="scientificArea">Área Científica</label>
            <input 
              id="scientificArea"
              v-model="form.scientificArea" 
              type="text" 
              placeholder="Ex: Ciência de Dados, Ciência dos Materiais"
              class="form-input"
            />
          </div>

          <div class="form-group">
            <label for="authors">Autores (um por linha)</label>
            <textarea 
              id="authors"
              v-model="authorsText" 
              rows="3" 
              placeholder="João Silva&#10;Maria Santos&#10;Pedro Costa"
              class="form-textarea"
            ></textarea>
            <small class="form-hint">Separa cada autor numa linha diferente</small>
          </div>

          <div class="form-group">
            <label for="summary">Resumo</label>
            <textarea 
              id="summary"
              v-model="form.summary" 
              rows="5" 
              placeholder="Escreve um resumo da publicação..."
              class="form-textarea"
            ></textarea>
            <small class="form-hint">Podes editar o resumo gerado automaticamente por IA mais tarde</small>
          </div>
        </div>

        <div class="form-section">
          <h2>Ficheiro (Opcional)</h2>
          <div class="form-group">
            <label for="file">Ficheiro PDF ou ZIP</label>
            <div 
              class="file-upload-area" 
              :class="{ 'has-file': selectedFile !== null, 'dragover': isDragging }"
              @click="fileInput?.click()"
              @dragover.prevent="handleDragOver"
              @dragleave.prevent="handleDragLeave"
              @drop.prevent="handleDrop"
            >
              <input 
                id="file"
                type="file" 
                ref="fileInput" 
                accept=".pdf,.zip" 
                @change="handleFileChange"
                class="file-input"
              />
              <div class="file-upload-content">
                <span v-if="!selectedFile" class="file-placeholder">
                  Clique para selecionar ou arraste um ficheiro aqui
                </span>
                <span v-else class="file-selected">
                  {{ selectedFile.name }} ({{ formatFileSize(selectedFile.size) }})
                  <button 
                    type="button"
                    @click.stop="removeFile"
                    class="file-remove"
                  >
                    ×
                  </button>
                </span>
              </div>
            </div>
            <small class="form-hint">Podes criar a publicação sem ficheiro e adicioná-lo depois</small>
          </div>
        </div>

        <div class="form-section">
          <h2>Configurações</h2>
          
          <div class="form-group">
            <label for="visibility">Visibilidade</label>
            <select id="visibility" v-model="form.visibility" class="form-select">
              <option value="public">Pública - Visível para todos</option>
              <option value="internal">Interna - Apenas membros do Centro</option>
              <option value="hidden">Oculta - Apenas para ti</option>
            </select>
          </div>

          <div class="form-group">
            <label>Tags</label>
            <div class="tags-input">
              <div v-if="loadingTags" class="tags-loading">A carregar tags...</div>
              <div v-else-if="tagsError" class="tags-error">{{ tagsError }}</div>
              <div v-else-if="availableTags.length === 0" class="tags-empty">
                Nenhuma tag disponível. Podes criar a publicação sem tags.
              </div>
              <div v-else class="tags-list">
                <label 
                  v-for="tag in availableTags" 
                  :key="tag.id" 
                  class="tag-checkbox"
                >
                  <input 
                    type="checkbox" 
                    :value="tag.id" 
                    v-model="selectedTags"
                  />
                  <span class="tag-label">{{ tag.name }}</span>
                </label>
              </div>
            </div>
            <small class="form-hint">Seleciona as tags relevantes para esta publicação (opcional)</small>
          </div>
        </div>

        <div class="form-actions">
          <button type="submit" :disabled="submitting" class="btn btn-primary btn-large">
            {{ submitting ? 'A criar...' : 'Criar Publicação' }}
          </button>
          <nuxt-link to="/" class="btn btn-secondary">Cancelar</nuxt-link>
        </div>
      </form>
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
const router = useRouter()

const fileInput = ref(null)
const submitting = ref(false)
const success = ref(false)
const publicationId = ref(null)
const selectedFile = ref(null)
const isDragging = ref(false)

const authorsText = ref('')
const selectedTags = ref([])
const availableTags = ref([])
const loadingTags = ref(false)
const tagsError = ref(null)

const form = ref({
  title: '',
  scientificArea: '',
  summary: '',
  visibility: 'internal'
})

async function loadTags() {
  if (!token.value) {
    tagsError.value = 'Precisa de estar autenticado para carregar tags'
    loadingTags.value = false
    return
  }
  
  loadingTags.value = true
  tagsError.value = null
  
  try {
    const response = await $fetch(`${api}/tags`, {
      method: 'GET',
      headers: { 
        'Authorization': `Bearer ${token.value}`,
        'Accept': 'application/json'
      }
    })
    availableTags.value = response || []
  } catch (e) {
    console.error('Erro ao carregar tags:', e)
    tagsError.value = 'Não foi possível carregar as tags. Podes criar a publicação sem tags e adicioná-las depois.'
    availableTags.value = []
  } finally {
    loadingTags.value = false
  }
}

function handleFileChange(event) {
  const files = event.target.files
  if (files && files.length > 0) {
    const file = files[0]
    if (validateFile(file)) {
      selectedFile.value = file
    }
  }
}

function handleDragOver(event) {
  isDragging.value = true
  event.preventDefault()
}

function handleDragLeave(event) {
  isDragging.value = false
  event.preventDefault()
}

function handleDrop(event) {
  isDragging.value = false
  event.preventDefault()
  
  const files = event.dataTransfer.files
  if (files && files.length > 0) {
    const file = files[0]
    if (validateFile(file)) {
      selectedFile.value = file
      // Atualizar o input file para manter sincronizado
      const dataTransfer = new DataTransfer()
      dataTransfer.items.add(file)
      fileInput.value.files = dataTransfer.files
    }
  }
}

function validateFile(file) {
  const allowedTypes = ['application/pdf', 'application/zip', 'application/x-zip-compressed']
  const allowedExtensions = ['.pdf', '.zip']
  const fileName = file.name.toLowerCase()
  
  const hasValidExtension = allowedExtensions.some(ext => fileName.endsWith(ext))
  const hasValidType = allowedTypes.includes(file.type)
  
  if (!hasValidExtension && !hasValidType) {
    alert('Por favor, seleciona um ficheiro PDF ou ZIP')
    return false
  }
  
  return true
}

function removeFile() {
  selectedFile.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

async function createPublication() {
  if (!form.value.title.trim()) {
    alert('O título é obrigatório')
    return
  }

  submitting.value = true
  success.value = false

  try {
    const authors = authorsText.value.split('\n')
      .map(a => a.trim())
      .filter(a => a.length > 0)

    const publicationData = {
      title: form.value.title,
      scientificArea: form.value.scientificArea || null,
      authors: authors.length > 0 ? authors : null,
      summary: form.value.summary || null,
      visibility: form.value.visibility || 'internal',
      tags: selectedTags.value.length > 0 ? selectedTags.value.map(tagId => {
        const tag = availableTags.value.find(t => t.id === tagId)
        return tag ? { id: tag.id, name: tag.name } : null
      }).filter(t => t !== null) : null
    }

    let response

    // Se houver ficheiro, usar upload; senão, criar sem ficheiro
    if (selectedFile.value) {
      const formData = new FormData()
      formData.append('file', selectedFile.value)

      response = await $fetch(`${api}/publications/upload`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token.value}`
        },
        body: formData
      })

      publicationId.value = response.id
      
      // Atualizar com os dados do formulário
      await $fetch(`${api}/publications/${response.id}`, {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${token.value}`,
          'Content-Type': 'application/json'
        },
        body: {
          title: publicationData.title,
          scientificArea: publicationData.scientificArea,
          summary: publicationData.summary,
          visibility: publicationData.visibility
        }
      })

      // Associar tags
      for (const tag of publicationData.tags) {
        try {
          await $fetch(`${api}/publications/${response.id}/tags/${tag.id}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token.value}` }
          })
        } catch (e) {
          console.error('Erro ao associar tag:', e)
        }
      }
    } else {
      // Criar sem ficheiro
      response = await $fetch(`${api}/publications`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token.value}`,
          'Content-Type': 'application/json'
        },
        body: publicationData
      })

      publicationId.value = response.id
    }

    success.value = true
  } catch (e) {
    let errorMsg = 'Erro ao criar publicação'
    if (e.data?.error) {
      errorMsg += ': ' + e.data.error
    } else if (e.message) {
      errorMsg += ': ' + e.message
    } else if (e.statusMessage) {
      errorMsg += ': ' + e.statusMessage
    } else {
      errorMsg += ': Erro desconhecido'
    }
    alert(errorMsg)
    console.error('Erro completo:', e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (!token.value) {
    router.push('/auth/login')
  } else {
    loadTags()
  }
})
</script>

<style scoped>
.create-publication {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  color: #333;
  margin-bottom: 0.5rem;
}

.page-header p {
  color: #666;
}

.loading-state, .success-state {
  text-align: center;
  padding: 3rem;
}

.success-card {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.success-card h2 {
  color: #28a745;
  margin-bottom: 1rem;
}

.success-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-top: 1.5rem;
}

.form-container {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.form-section {
  margin-bottom: 2rem;
  padding-bottom: 2rem;
  border-bottom: 1px solid #eee;
}

.form-section:last-of-type {
  border-bottom: none;
}

.form-section h2 {
  color: #667eea;
  font-size: 1.2rem;
  margin-bottom: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
}

.form-input, .form-textarea, .form-select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.form-input:focus, .form-textarea:focus, .form-select:focus {
  outline: none;
  border-color: #667eea;
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.form-hint {
  display: block;
  margin-top: 0.25rem;
  color: #666;
  font-size: 0.85rem;
}

.file-upload-area {
  border: 2px dashed #ddd;
  border-radius: 4px;
  padding: 2rem;
  text-align: center;
  transition: all 0.2s;
  cursor: pointer;
}

.file-upload-area:hover {
  border-color: #667eea;
  background: #f8f9ff;
}

.file-upload-area.has-file {
  border-color: #28a745;
  background: #f0fff4;
}

.file-upload-area.dragover {
  border-color: #667eea;
  background: #f0f4ff;
  transform: scale(1.02);
}

.file-selected {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.file-remove {
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  font-size: 1.2rem;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: background 0.2s;
}

.file-remove:hover {
  background: #c82333;
}

.file-input {
  display: none;
}

.file-placeholder, .file-selected {
  display: block;
  color: #666;
}

.file-selected {
  color: #28a745;
  font-weight: 500;
}

.tags-input {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 1rem;
  min-height: 100px;
  max-height: 200px;
  overflow-y: auto;
}

.tags-loading, .tags-error, .tags-empty {
  padding: 1rem;
  text-align: center;
  color: #666;
  font-size: 0.9rem;
}

.tags-error {
  color: #dc3545;
  background: #f8d7da;
  border-radius: 4px;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.tag-checkbox {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.tag-checkbox input {
  margin-right: 0.5rem;
}

.tag-label {
  padding: 0.25rem 0.75rem;
  background: #f0f0f0;
  border-radius: 12px;
  font-size: 0.9rem;
  transition: background 0.2s;
}

.tag-checkbox input:checked + .tag-label {
  background: #667eea;
  color: white;
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid #eee;
}

.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.2s;
  border: none;
  cursor: pointer;
  display: inline-block;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #5568d3;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #5a6268;
}

.btn-large {
  padding: 1rem 2rem;
  font-size: 1.1rem;
}
</style>
