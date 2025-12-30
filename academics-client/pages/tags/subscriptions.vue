<template>
  <div class="subscriptions-page">
    <div class="page-header">
      <h1>📌 Minhas Subscrições de Tags</h1>
      <p>Gerir as tags que subscreveste para receber notificações</p>
    </div>

    <div v-if="loading">⏳ A carregar...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else class="content-grid">
      <div class="section">
        <h2>Tags Disponíveis</h2>
        <div v-if="loadingTags">A carregar tags...</div>
        <div v-else-if="availableTags.length === 0">
          <p>Não há tags disponíveis.</p>
        </div>
        <div v-else class="tags-list">
          <div 
            v-for="tag in availableTags" 
            :key="tag.id" 
            class="tag-item"
            :class="{ 'subscribed': isSubscribed(tag.id) }"
          >
            <span class="tag-name">{{ tag.name }}</span>
            <button 
              v-if="!isSubscribed(tag.id)"
              @click="subscribe(tag.id)" 
              class="btn btn-small btn-primary"
              :disabled="subscribing"
            >
              Subscrever
            </button>
            <button 
              v-else
              @click="unsubscribe(tag.id)" 
              class="btn btn-small btn-danger"
              :disabled="subscribing"
            >
              Desinscrever
            </button>
          </div>
        </div>
      </div>

      <div class="section">
        <h2>Tags Subscritas ({{ subscribedTags.length }})</h2>
        <div v-if="subscribedTags.length === 0" class="empty-state">
          <p>Não tens nenhuma tag subscrita.</p>
          <p class="hint">Subscreve tags para receber notificações quando houver novidades!</p>
        </div>
        <div v-else class="tags-list">
          <div 
            v-for="tag in subscribedTags" 
            :key="tag.id" 
            class="tag-item subscribed"
          >
            <span class="tag-name">{{ tag.name }}</span>
            <button 
              @click="unsubscribe(tag.id)" 
              class="btn btn-small btn-danger"
              :disabled="subscribing"
            >
              Desinscrever
            </button>
          </div>
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

const subscribedTags = ref([])
const availableTags = ref([])
const loading = ref(true)
const loadingTags = ref(true)
const error = ref(null)
const subscribing = ref(false)

async function loadSubscribedTags() {
  if (!token.value || !user.value) {
    router.push('/auth/login')
    return
  }

  loading.value = true
  error.value = null
  try {
    subscribedTags.value = await $fetch(`${api}/users/${user.value.username}/tags`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
  } catch (e) {
    error.value = 'Erro ao carregar subscrições: ' + (e.message || 'Erro desconhecido')
    console.error('Erro:', e)
  } finally {
    loading.value = false
  }
}

async function loadAvailableTags() {
  if (!token.value) return

  loadingTags.value = true
  try {
    availableTags.value = await $fetch(`${api}/tags`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
  } catch (e) {
    console.error('Erro ao carregar tags:', e)
  } finally {
    loadingTags.value = false
  }
}

function isSubscribed(tagId) {
  return subscribedTags.value.some(t => t.id === tagId)
}

async function subscribe(tagId) {
  if (!token.value || !user.value) return

  subscribing.value = true
  try {
    await $fetch(`${api}/users/${user.value.username}/tags/${tagId}`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    await loadSubscribedTags()
  } catch (e) {
    alert('Erro ao subscrever tag: ' + (e.message || 'Erro desconhecido'))
  } finally {
    subscribing.value = false
  }
}

async function unsubscribe(tagId) {
  if (!token.value || !user.value) return

  subscribing.value = true
  try {
    await $fetch(`${api}/users/${user.value.username}/tags/${tagId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    await loadSubscribedTags()
  } catch (e) {
    alert('Erro ao desinscrever tag: ' + (e.message || 'Erro desconhecido'))
  } finally {
    subscribing.value = false
  }
}

onMounted(() => {
  loadSubscribedTags()
  loadAvailableTags()
})
</script>

<style scoped>
.subscriptions-page {
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

.content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 2rem;
}

.section {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.section h2 {
  color: #667eea;
  margin-bottom: 1rem;
  font-size: 1.2rem;
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
  transition: all 0.2s;
}

.tag-item.subscribed {
  background: #e7f3ff;
  border-color: #667eea;
}

.tag-name {
  font-weight: 500;
  color: #333;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.hint {
  font-size: 0.9rem;
  margin-top: 0.5rem;
  font-style: italic;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
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

.error-message {
  background: #f8d7da;
  color: #721c24;
  padding: 1rem;
  border-radius: 4px;
  margin: 1rem 0;
}
</style>

