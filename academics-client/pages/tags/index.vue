<template>
  <div class="tags-page">
    <div class="page-header">
      <h1>Gestão de Tags</h1>
      <p>Definir e remover tags do sistema</p>
    </div>

    <!-- Criar Nova Tag -->
    <div class="card">
      <h2>Criar Nova Tag</h2>
      <div class="create-form">
        <input 
          v-model="newTagName" 
          type="text" 
          placeholder="Nome da tag"
          class="input"
          @keyup.enter="createTag"
        />
        <button 
          @click="createTag" 
          class="btn btn-primary"
          :disabled="creating || !newTagName.trim()"
        >
          Criar Tag
        </button>
      </div>
    </div>

    <!-- Lista de Tags -->
    <div class="card">
      <h2>Tags Existentes ({{ tags.length }})</h2>
      <div v-if="loading" class="loading">A carregar...</div>
      <div v-else-if="error" class="error-message">{{ error }}</div>
      <div v-else-if="tags.length === 0" class="empty-state">
        <p>Não há tags criadas ainda.</p>
      </div>
      <div v-else class="tags-list">
        <div 
          v-for="tag in tags" 
          :key="tag.id" 
          class="tag-item"
        >
          <span class="tag-name">{{ tag.name }}</span>
          <button 
            @click="deleteTag(tag.id)" 
            class="btn btn-danger btn-small"
            :disabled="deleting"
          >
            Remover
          </button>
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
const { token, user } = storeToRefs(authStore)
const router = useRouter()

const tags = ref([])
const newTagName = ref('')
const loading = ref(true)
const creating = ref(false)
const deleting = ref(false)
const error = ref(null)

const isResponsible = computed(() => 
  user.value?.role === 'Responsible' || user.value?.role === 'Administrator'
)

async function loadTags() {
  if (!token.value) {
    router.push('/auth/login')
    return
  }

  if (!isResponsible.value) {
    router.push('/')
    return
  }

  loading.value = true
  error.value = null
  try {
    tags.value = await $fetch(`${api}/tags`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
  } catch (e) {
    error.value = 'Erro ao carregar tags: ' + (e.message || 'Erro desconhecido')
    console.error('Erro:', e)
  } finally {
    loading.value = false
  }
}

async function createTag() {
  if (!newTagName.value.trim()) return

  creating.value = true
  try {
    const newTag = await $fetch(`${api}/tags`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: { name: newTagName.value.trim() }
    })
    tags.value.push(newTag)
    newTagName.value = ''
  } catch (e) {
    let errorMsg = 'Erro ao criar tag'
    if (e.data?.error) {
      errorMsg += ': ' + e.data.error
    } else if (e.response?._data?.error) {
      errorMsg += ': ' + e.response._data.error
    }
    alert(errorMsg)
  } finally {
    creating.value = false
  }
}

async function deleteTag(tagId) {
  if (!confirm('Tens a certeza que queres remover esta tag?')) return

  deleting.value = true
  try {
    await $fetch(`${api}/tags/${tagId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    tags.value = tags.value.filter(t => t.id !== tagId)
  } catch (e) {
    alert('Erro ao remover tag: ' + (e.message || 'Erro desconhecido'))
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  loadTags()
})
</script>

<style scoped>
.tags-page {
  width: 100%;
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

.card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 2rem;
}

.card h2 {
  color: #667eea;
  margin-bottom: 1rem;
  font-size: 1.2rem;
}

.create-form {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.input {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  font-size: 1rem;
}

.input:focus {
  outline: none;
  border-color: #667eea;
}

.tags-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.tag-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: #f8f9fa;
  border-radius: 4px;
  border: 1px solid #dee2e6;
}

.tag-name {
  font-weight: 500;
  color: #333;
}

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

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #5568d3;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #c82333;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.error-message {
  background: #f8d7da;
  color: #721c24;
  padding: 1rem;
  border-radius: 4px;
  margin: 1rem 0;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #666;
}
</style>
