<template>
  <div class="profile-page">
    <div class="page-header">
      <h1>Meu Perfil</h1>
      <p>Gerir os teus dados pessoais e histórico de atividade</p>
    </div>

    <div v-if="loading" class="loading">A carregar...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else class="content-grid">
      <!-- Informações Pessoais -->
      <div class="card">
        <h2>Informações Pessoais</h2>
        <div v-if="!editing" class="info-display">
          <div class="info-item">
            <label>Username:</label>
            <span>{{ profileData.username }}</span>
          </div>
          <div class="info-item">
            <label>Nome:</label>
            <span>{{ profileData.name }}</span>
          </div>
          <div class="info-item">
            <label>Email:</label>
            <span>{{ profileData.email }}</span>
          </div>
          <div class="info-item">
            <label>Role:</label>
            <span>{{ profileData.role }}</span>
          </div>
          <button @click="editing = true" class="btn btn-primary">
            Editar Dados
          </button>
        </div>
        <form v-else @submit.prevent="saveProfile" class="edit-form">
          <div class="form-group">
            <label>Nome</label>
            <input v-model="editData.name" type="text" required class="input" />
          </div>
          <div class="form-group">
            <label>Email</label>
            <input v-model="editData.email" type="email" required class="input" />
          </div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="updating">
              Guardar
            </button>
            <button type="button" @click="cancelEdit" class="btn btn-secondary">
              Cancelar
            </button>
          </div>
        </form>
      </div>

      <!-- Alterar Palavra-passe -->
      <div class="card">
        <h2>Alterar Palavra-passe</h2>
        <form @submit.prevent="changePassword" class="password-form">
          <div class="form-group">
            <label>Palavra-passe Atual</label>
            <input v-model="passwordData.old_password" type="password" required class="input" />
          </div>
          <div class="form-group">
            <label>Nova Palavra-passe</label>
            <input v-model="passwordData.new_password" type="password" required class="input" />
          </div>
          <div class="form-group">
            <label>Confirmar Nova Palavra-passe</label>
            <input v-model="passwordData.confirm_password" type="password" required class="input" />
          </div>
          <button type="submit" class="btn btn-primary" :disabled="changingPassword">
            Alterar Palavra-passe
          </button>
        </form>
      </div>

      <!-- Histórico de Atividade -->
      <div class="card full-width">
        <h2>Histórico de Atividade</h2>
        <div v-if="loadingActivity" class="loading">A carregar...</div>
        <div v-else-if="activities.length === 0" class="empty-state">
          <p>Nenhuma atividade registada.</p>
        </div>
        <div v-else class="activities-list">
          <div 
            v-for="activity in activities" 
            :key="activity.id" 
            class="activity-item"
          >
            <div class="activity-icon">{{ getActivityIcon(activity.activityType) }}</div>
            <div class="activity-details">
              <p class="activity-desc">{{ activity.description }}</p>
              <small class="activity-date">{{ formatDate(activity.activityDate) }}</small>
              <nuxt-link 
                v-if="activity.publicationId" 
                :to="`/publications/${activity.publicationId}`"
                class="activity-link"
              >
                Ver publicação
              </nuxt-link>
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

const config = useRuntimeConfig()
const api = config.public.apiBase
const authStore = useAuthStore()
const { token, user } = storeToRefs(authStore)
const router = useRouter()

const profileData = ref({})
const editData = ref({})
const passwordData = ref({
  old_password: '',
  new_password: '',
  confirm_password: ''
})
const activities = ref([])
const loading = ref(true)
const loadingActivity = ref(true)
const editing = ref(false)
const updating = ref(false)
const changingPassword = ref(false)
const error = ref(null)

async function loadProfile() {
  if (!token.value || !user.value) {
    router.push('/auth/login')
    return
  }

  loading.value = true
  error.value = null
  try {
    profileData.value = await $fetch(`${api}/users/${user.value.username}`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    editData.value = {
      name: profileData.value.name,
      email: profileData.value.email
    }
  } catch (e) {
    error.value = 'Erro ao carregar perfil: ' + (e.message || 'Erro desconhecido')
    console.error('Erro:', e)
  } finally {
    loading.value = false
  }
}

async function loadActivities() {
  if (!token.value || !user.value) return

  loadingActivity.value = true
  try {
    activities.value = await $fetch(`${api}/users/${user.value.username}/activity`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
  } catch (e) {
    console.error('Erro ao carregar atividades:', e)
  } finally {
    loadingActivity.value = false
  }
}

async function saveProfile() {
  updating.value = true
  try {
    await $fetch(`${api}/users/${user.value.username}`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: {
        name: editData.value.name,
        email: editData.value.email
      }
    })
    profileData.value.name = editData.value.name
    profileData.value.email = editData.value.email
    // Atualizar store também
    if (authStore.user) {
      authStore.user.name = editData.value.name
      authStore.user.email = editData.value.email
    }
    editing.value = false
    alert('Perfil atualizado com sucesso!')
  } catch (e) {
    alert('Erro ao atualizar perfil: ' + (e.message || 'Erro desconhecido'))
  } finally {
    updating.value = false
  }
}

function cancelEdit() {
  editData.value = {
    name: profileData.value.name,
    email: profileData.value.email
  }
  editing.value = false
}

async function changePassword() {
  if (passwordData.value.new_password !== passwordData.value.confirm_password) {
    alert('As palavras-passe não coincidem!')
    return
  }

  changingPassword.value = true
  try {
    await $fetch(`${api}/auth/change-password`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: passwordData.value
    })
    passwordData.value = {
      old_password: '',
      new_password: '',
      confirm_password: ''
    }
    alert('Palavra-passe alterada com sucesso!')
  } catch (e) {
    let errorMsg = 'Erro ao alterar palavra-passe'
    if (e.data?.message) {
      errorMsg += ': ' + e.data.message
    } else if (e.response?._data?.message) {
      errorMsg += ': ' + e.response._data.message
    } else if (e.message) {
      errorMsg += ': ' + e.message
    }
    alert(errorMsg)
  } finally {
    changingPassword.value = false
  }
}

function getActivityIcon(type) {
  const icons = {
    'UPLOAD': '📤',
    'EDIT': '✏️',
    'COMMENT': '💬',
    'RATING': '⭐',
    'TAG_SUBSCRIPTION': '🏷️',
    'PUBLICATION_CREATE': '📄',
    'PUBLICATION_UPDATE': '📝'
  }
  return icons[type] || '📋'
}

function formatDate(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('pt-PT')
}

onMounted(() => {
  loadProfile()
  loadActivities()
})
</script>

<style scoped>
.profile-page {
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

.card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.card.full-width {
  grid-column: 1 / -1;
}

.card h2 {
  color: #667eea;
  margin-bottom: 1rem;
  font-size: 1.2rem;
}

.info-display {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.info-item {
  display: flex;
  gap: 1rem;
}

.info-item label {
  font-weight: 500;
  color: #666;
  min-width: 100px;
}

.info-item span {
  color: #333;
}

.edit-form, .password-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
}

.input {
  padding: 0.75rem;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  font-size: 1rem;
}

.input:focus {
  outline: none;
  border-color: #667eea;
}

.form-actions {
  display: flex;
  gap: 1rem;
  margin-top: 0.5rem;
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

.activities-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.activity-item {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 4px;
  border-left: 3px solid #667eea;
}

.activity-icon {
  font-size: 1.5rem;
}

.activity-details {
  flex: 1;
}

.activity-desc {
  margin: 0 0 0.5rem 0;
  color: #333;
  font-weight: 500;
}

.activity-date {
  color: #666;
  display: block;
  margin-bottom: 0.5rem;
}

.activity-link {
  color: #667eea;
  text-decoration: none;
  font-size: 0.9rem;
}

.activity-link:hover {
  text-decoration: underline;
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
