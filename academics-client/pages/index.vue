<template>
  <div class="dashboard">
    <div v-if="!token" class="welcome-section">
      <h1>Bem-vindo à Plataforma de Gestão de Publicações Científicas</h1>
      <p class="subtitle">Centro de Investigação e Desenvolvimento XYZ</p>
      <div class="welcome-actions">
        <nuxt-link to="/auth/login" class="btn btn-primary">Fazer Login</nuxt-link>
        <nuxt-link to="/publications" class="btn btn-secondary">Ver Publicações Públicas</nuxt-link>
      </div>
    </div>

    <div v-else class="dashboard-content">
      <div class="dashboard-header">
        <h1>Dashboard</h1>
        <p class="welcome-text">Bem-vindo, <strong>{{ user?.name }}</strong>!</p>
      </div>

      <div class="dashboard-grid">
        <!-- Ações Rápidas -->
        <div class="dashboard-card">
          <h2>Ações Rápidas</h2>
          <div class="card-content">
            <nuxt-link to="/publications/create" class="action-btn">
              Criar Nova Publicação
            </nuxt-link>
            <nuxt-link to="/publications" class="action-btn">
              Pesquisar Publicações
            </nuxt-link>
            <nuxt-link v-if="isCollaborator" to="/tags/subscriptions" class="action-btn">
              Gerir Subscrições de Tags
            </nuxt-link>
          </div>
        </div>

        <!-- Minhas Publicações -->
        <div class="dashboard-card">
          <h2>Minhas Publicações</h2>
          <div class="card-content">
            <div v-if="loadingPublications">A carregar...</div>
            <div v-else-if="myPublications.length === 0">
              <p>Ainda não criaste nenhuma publicação.</p>
              <nuxt-link to="/publications/create" class="btn btn-small">Criar Primeira Publicação</nuxt-link>
            </div>
            <div v-else>
              <p class="count">{{ myPublications.length }} publicação(ões)</p>
              <div class="publication-list">
                <div v-for="pub in myPublications.slice(0, 3)" :key="pub.id" class="publication-item">
                  <nuxt-link :to="`/publications/${pub.id}`">{{ pub.title || 'Sem título' }}</nuxt-link>
                  <span class="badge" :class="getVisibilityClass(pub.visibility)">{{ pub.visibility }}</span>
                </div>
              </div>
              <nuxt-link to="/publications/my" class="btn btn-small">Ver Todas</nuxt-link>
            </div>
          </div>
        </div>

        <!-- Atividades Recentes -->
        <div class="dashboard-card">
          <h2>Atividades Recentes</h2>
          <div class="card-content">
            <div v-if="loadingActivity">A carregar...</div>
            <div v-else-if="activities.length === 0">
              <p>Nenhuma atividade registada.</p>
            </div>
            <div v-else>
              <div class="activity-list">
                <div v-for="activity in activities.slice(0, 5)" :key="activity.id" class="activity-item">
                  <span class="activity-icon">{{ getActivityIcon(activity.activityType) }}</span>
                  <div class="activity-details">
                    <p class="activity-desc">{{ activity.description }}</p>
                    <small class="activity-date">{{ formatDate(activity.activityDate) }}</small>
                  </div>
                </div>
              </div>
              <nuxt-link to="/profile" class="btn btn-small">Ver Histórico Completo</nuxt-link>
            </div>
          </div>
        </div>

        <!-- Gestão (Admin/Responsible) -->
        <div v-if="isResponsible" class="dashboard-card">
          <h2>Gestão</h2>
          <div class="card-content">
            <nuxt-link v-if="isResponsible" to="/tags" class="action-btn">
              Gerir Tags
            </nuxt-link>
            <nuxt-link v-if="isAdmin" to="/users" class="action-btn">
              Gerir Utilizadores
            </nuxt-link>
          </div>
        </div>

        <!-- Estatísticas -->
        <div class="dashboard-card">
          <h2>Estatísticas</h2>
          <div class="card-content">
            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-value">{{ totalPublications }}</div>
                <div class="stat-label">Publicações Totais</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">{{ myPublications.length }}</div>
                <div class="stat-label">Minhas Publicações</div>
              </div>
              <div v-if="isCollaborator" class="stat-item">
                <div class="stat-value">{{ subscribedTagsCount }}</div>
                <div class="stat-label">Tags Subscritas</div>
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

const config = useRuntimeConfig()
const api = config.public.apiBase
const authStore = useAuthStore()
const { user, token } = storeToRefs(authStore)

const isAdmin = computed(() => user.value?.role === 'Administrator')
const isResponsible = computed(() => user.value?.role === 'Responsible' || isAdmin.value)
const isCollaborator = computed(() => user.value?.role === 'Collaborator' || isResponsible.value)

const myPublications = ref([])
const activities = ref([])
const totalPublications = ref(0)
const subscribedTagsCount = ref(0)
const loadingPublications = ref(false)
const loadingActivity = ref(false)

async function loadMyPublications() {
  if (!token.value || !user.value) return
  
  loadingPublications.value = true
  try {
    const response = await $fetch(`${api}/users/${user.value.username}/publications`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    myPublications.value = response
  } catch (e) {
    console.error('Erro ao carregar publicações:', e)
  } finally {
    loadingPublications.value = false
  }
}

async function loadActivities() {
  if (!token.value || !user.value) return
  
  loadingActivity.value = true
  try {
    const response = await $fetch(`${api}/users/${user.value.username}/activity`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    activities.value = response
  } catch (e) {
    console.error('Erro ao carregar atividades:', e)
  } finally {
    loadingActivity.value = false
  }
}

async function loadTotalPublications() {
  if (!token.value) return
  
  try {
    const response = await $fetch(`${api}/publications`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    totalPublications.value = response.length
  } catch (e) {
    console.error('Erro ao carregar total:', e)
  }
}

async function loadSubscribedTags() {
  if (!token.value || !user.value || !isCollaborator.value) return
  
  try {
    const response = await $fetch(`${api}/users/${user.value.username}/tags`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    subscribedTagsCount.value = response.length
  } catch (e) {
    console.error('Erro ao carregar tags:', e)
  }
}

function getVisibilityClass(visibility) {
  const classes = {
    'public': 'badge-success',
    'internal': 'badge-info',
    'hidden': 'badge-warning'
  }
  return classes[visibility] || 'badge-default'
}

function getActivityIcon(type) {
  const icons = {
    'UPLOAD': 'UPLOAD',
    'EDIT': 'EDIT',
    'COMMENT': 'COMMENT',
    'RATING': 'RATING',
    'TAG_SUBSCRIPTION': 'TAG_SUB'
  }
  return icons[type] || type
}

function formatDate(dateString) {
  if (!dateString) return 'N/A'
  try {
    return new Date(dateString).toLocaleString('pt-PT', { 
      day: '2-digit', 
      month: '2-digit', 
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return dateString
  }
}

onMounted(() => {
  if (token.value) {
    loadMyPublications()
    loadActivities()
    loadTotalPublications()
    loadSubscribedTags()
  }
})
</script>

<style scoped>
.dashboard {
  width: 100%;
}

.welcome-section {
  text-align: center;
  padding: 4rem 2rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.welcome-section h1 {
  color: #667eea;
  margin-bottom: 0.5rem;
}

.subtitle {
  color: #666;
  font-size: 1.1rem;
  margin-bottom: 2rem;
}

.welcome-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.dashboard-header {
  margin-bottom: 2rem;
}

.dashboard-header h1 {
  color: #333;
  margin-bottom: 0.5rem;
}

.welcome-text {
  color: #666;
  font-size: 1.1rem;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.dashboard-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.dashboard-card h2 {
  margin: 0 0 1rem 0;
  color: #333;
  font-size: 1.2rem;
  border-bottom: 2px solid #667eea;
  padding-bottom: 0.5rem;
}

.card-content {
  min-height: 150px;
}

.action-btn {
  display: block;
  padding: 0.75rem 1rem;
  margin-bottom: 0.5rem;
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  text-decoration: none;
  color: #333;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.publication-list {
  margin: 1rem 0;
}

.publication-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px solid #eee;
}

.publication-item a {
  color: #667eea;
  text-decoration: none;
  flex: 1;
}

.publication-item a:hover {
  text-decoration: underline;
}

.badge {
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
}

.badge-success { background: #d4edda; color: #155724; }
.badge-info { background: #d1ecf1; color: #0c5460; }
.badge-warning { background: #fff3cd; color: #856404; }
.badge-default { background: #e9ecef; color: #495057; }

.count {
  font-size: 1.5rem;
  font-weight: bold;
  color: #667eea;
  margin-bottom: 1rem;
}

.activity-list {
  margin: 1rem 0;
}

.activity-item {
  display: flex;
  gap: 0.75rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid #eee;
}

.activity-icon {
  font-size: 1.5rem;
}

.activity-details {
  flex: 1;
}

.activity-desc {
  margin: 0 0 0.25rem 0;
  color: #333;
}

.activity-date {
  color: #666;
  font-size: 0.85rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 1rem;
  margin-top: 1rem;
}

.stat-item {
  text-align: center;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 4px;
}

.stat-value {
  font-size: 2rem;
  font-weight: bold;
  color: #667eea;
}

.stat-label {
  font-size: 0.85rem;
  color: #666;
  margin-top: 0.5rem;
}

.btn {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.2s;
  border: none;
  cursor: pointer;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover {
  background: #5568d3;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #5a6268;
}

.btn-small {
  padding: 0.5rem 1rem;
  font-size: 0.9rem;
  background: #667eea;
  color: white;
  margin-top: 0.5rem;
}

.btn-small:hover {
  background: #5568d3;
}
</style>
