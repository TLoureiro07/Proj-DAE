<template>
  <div class="my-publications">
    <div class="page-header">
      <h1>Minhas Publicações</h1>
      <p>Gerir as publicações que criaste</p>
    </div>

    <div v-if="loading">A carregar...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else>
      <div v-if="publications.length === 0" class="empty-state">
        <p>Ainda não criaste nenhuma publicação.</p>
        <nuxt-link to="/publications/create" class="btn btn-primary">Criar Primeira Publicação</nuxt-link>
      </div>
      <div v-else>
        <div class="publications-header">
          <p class="count">{{ publications.length }} publicação(ões)</p>
        </div>
        <div class="publications-grid">
          <div v-for="pub in publications" :key="pub.id" class="publication-card">
            <div class="card-header">
              <h3>
                <nuxt-link :to="`/publications/${pub.id}`">{{ pub.title || 'Sem título' }}</nuxt-link>
              </h3>
              <span class="badge" :class="getVisibilityClass(pub.visibility)">{{ pub.visibility }}</span>
            </div>
            <div class="card-body">
              <p v-if="pub.scientificArea" class="info-item">
                <strong>Área:</strong> {{ pub.scientificArea }}
              </p>
              <p v-if="pub.summary" class="summary">{{ truncate(pub.summary, 150) }}</p>
              <p v-if="pub.ratingAvg !== null && pub.ratingAvg !== undefined" class="info-item">
                <strong>Rating:</strong> {{ pub.ratingAvg.toFixed(1) }} estrelas
              </p>
              <p class="info-item">
                <strong>Data:</strong> {{ formatDate(pub.uploadDate) }}
              </p>
            </div>
            <div class="card-footer">
              <nuxt-link :to="`/publications/${pub.id}`" class="btn btn-small">Ver Detalhes</nuxt-link>
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

const publications = ref([])
const loading = ref(true)
const error = ref(null)

async function loadPublications() {
  if (!token.value || !user.value) {
    router.push('/auth/login')
    return
  }

  loading.value = true
  error.value = null
  try {
    publications.value = await $fetch(`${api}/users/${user.value.username}/publications`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
  } catch (e) {
    error.value = 'Erro ao carregar publicações: ' + (e.message || 'Erro desconhecido')
    console.error('Erro:', e)
  } finally {
    loading.value = false
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

function formatDate(dateString) {
  if (!dateString) return 'N/A'
  try {
    return new Date(dateString).toLocaleDateString('pt-PT')
  } catch {
    return dateString
  }
}

function truncate(text, length) {
  if (!text) return ''
  return text.length > length ? text.substring(0, length) + '...' : text
}

onMounted(() => {
  loadPublications()
})
</script>

<style scoped>
.my-publications {
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

.publications-header {
  margin-bottom: 1.5rem;
}

.count {
  font-size: 1.2rem;
  color: #667eea;
  font-weight: 500;
}

.publications-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.publication-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}

.publication-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.card-header h3 {
  margin: 0;
  flex: 1;
}

.card-header h3 a {
  color: #333;
  text-decoration: none;
}

.card-header h3 a:hover {
  color: #667eea;
}

.badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;
}

.badge-success { background: #d4edda; color: #155724; }
.badge-info { background: #d1ecf1; color: #0c5460; }
.badge-warning { background: #fff3cd; color: #856404; }
.badge-default { background: #e9ecef; color: #495057; }

.card-body {
  margin-bottom: 1rem;
}

.info-item {
  margin: 0.5rem 0;
  color: #666;
  font-size: 0.9rem;
}

.summary {
  margin: 1rem 0;
  color: #555;
  line-height: 1.6;
}

.card-footer {
  border-top: 1px solid #eee;
  padding-top: 1rem;
  margin-top: 1rem;
}

.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.empty-state p {
  font-size: 1.2rem;
  color: #666;
  margin-bottom: 1.5rem;
}

.btn {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover {
  background: #5568d3;
}

.btn-small {
  padding: 0.5rem 1rem;
  font-size: 0.9rem;
  background: #667eea;
  color: white;
}

.btn-small:hover {
  background: #5568d3;
}

.error-message {
  background: #f8d7da;
  color: #721c24;
  padding: 1rem;
  border-radius: 4px;
  margin: 1rem 0;
}
</style>

