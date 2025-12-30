<template>
  <div class="publications-page">
    <div class="page-header">
      <h1>📚 Publicações Científicas</h1>
      <p>Explora e pesquisa publicações do Centro de Investigação</p>
    </div>
    
    <!-- Formulário de Pesquisa e Filtros -->
    <div class="filters-card">
      <h2>🔍 Pesquisar e Filtrar</h2>
      <div class="filters-grid">
        <div class="filter-group">
          <label>Pesquisa (título/resumo)</label>
          <input 
            v-model="filters.search" 
            type="text" 
            placeholder="Pesquisar..." 
            class="filter-input"
          />
        </div>
        <div class="filter-group">
          <label>Área Científica</label>
          <input 
            v-model="filters.scientificArea" 
            type="text" 
            placeholder="Ex: Ciência de Dados" 
            class="filter-input"
          />
        </div>
        <div class="filter-group">
          <label>Tag</label>
          <input 
            v-model="filters.tag" 
            type="text" 
            placeholder="Ex: Projeto X" 
            class="filter-input"
          />
        </div>
      </div>
      <div class="filters-row">
        <div class="filter-group">
          <label>Ordenar por</label>
          <select v-model="filters.sortBy" class="filter-select">
            <option value="">Data de upload</option>
            <option value="rating">Rating médio</option>
            <option value="comments">Número de comentários</option>
            <option value="ratings">Número de ratings</option>
          </select>
        </div>
        <div class="filter-group">
          <label>Ordem</label>
          <select v-model="filters.order" class="filter-select">
            <option value="desc">Descendente</option>
            <option value="asc">Ascendente</option>
          </select>
        </div>
      </div>
      <div class="filters-actions">
        <button @click="applyFilters" :disabled="loading" class="btn btn-primary">
          🔍 Aplicar Filtros
        </button>
        <button @click="clearFilters" class="btn btn-secondary">
          🗑️ Limpar
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <p>⏳ A carregar publicações...</p>
    </div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    <div v-else>
      <div v-if="publications.length === 0" class="empty-state">
        <p>📭 Não há publicações disponíveis com os critérios selecionados.</p>
        <button @click="clearFilters" class="btn btn-secondary">Limpar Filtros</button>
      </div>
      <div v-else>
        <div class="results-header">
          <p class="results-count">
            <strong>{{ publications.length }}</strong> publicação(ões) encontrada(s)
          </p>
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
              <p class="author"><strong>Autor:</strong> {{ pub.owner || 'N/A' }}</p>
              <p v-if="pub.scientificArea" class="info-item">
                <strong>Área Científica:</strong> {{ pub.scientificArea }}
              </p>
              <p v-if="pub.summary" class="summary">{{ truncate(pub.summary, 200) }}</p>
              <div class="metrics">
                <span v-if="pub.ratingAvg !== null && pub.ratingAvg !== undefined" class="metric">
                  ⭐ {{ pub.ratingAvg.toFixed(1) }}
                </span>
                <span v-if="pub.tags && pub.tags.length > 0" class="metric">
                  🏷️ {{ pub.tags.length }} tag(s)
                </span>
              </div>
              <div v-if="pub.tags && pub.tags.length > 0" class="tags">
                <span 
                  v-for="tag in pub.tags" 
                  :key="tag.id" 
                  class="tag-badge"
                >
                  {{ tag.name }}
                </span>
              </div>
            </div>
            <div class="card-footer">
              <small class="date">📅 {{ formatDate(pub.uploadDate) }}</small>
              <nuxt-link :to="`/publications/${pub.id}`" class="btn btn-small">Ver Detalhes →</nuxt-link>
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
const { token } = storeToRefs(authStore)

const publications = ref([])
const loading = ref(true)
const error = ref(null)

const filters = ref({
  search: '',
  scientificArea: '',
  tag: '',
  sortBy: '',
  order: 'desc'
})

async function loadPublications() {
  loading.value = true
  error.value = null
  try {
    const params = new URLSearchParams()
    if (filters.value.search) params.append('search', filters.value.search)
    if (filters.value.scientificArea) params.append('scientificArea', filters.value.scientificArea)
    if (filters.value.tag) params.append('tag', filters.value.tag)
    if (filters.value.sortBy) params.append('sortBy', filters.value.sortBy)
    if (filters.value.order) params.append('order', filters.value.order)

    const url = `${api}/publications${params.toString() ? '?' + params.toString() : ''}`
    const response = await $fetch(url, {
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

function applyFilters() {
  loadPublications()
}

function clearFilters() {
  filters.value = {
    search: '',
    scientificArea: '',
    tag: '',
    sortBy: '',
    order: 'desc'
  }
  loadPublications()
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
  if (token.value) {
    loadPublications()
  } else {
    error.value = 'Precisa de fazer login para ver publicações'
  }
})
</script>

<style scoped>
.publications-page {
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

.filters-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 2rem;
}

.filters-card h2 {
  color: #667eea;
  margin-bottom: 1rem;
  font-size: 1.2rem;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.filters-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.filter-group {
  display: flex;
  flex-direction: column;
}

.filter-group label {
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
  font-size: 0.9rem;
}

.filter-input, .filter-select {
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.filter-input:focus, .filter-select:focus {
  outline: none;
  border-color: #667eea;
}

.filters-actions {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.results-header {
  margin-bottom: 1.5rem;
}

.results-count {
  font-size: 1.1rem;
  color: #667eea;
}

.publications-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 1.5rem;
}

.publication-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
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
  font-size: 1.2rem;
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
  flex: 1;
  margin-bottom: 1rem;
}

.author, .info-item {
  margin: 0.5rem 0;
  color: #666;
  font-size: 0.9rem;
}

.summary {
  margin: 1rem 0;
  color: #555;
  line-height: 1.6;
}

.metrics {
  display: flex;
  gap: 1rem;
  margin: 1rem 0;
  flex-wrap: wrap;
}

.metric {
  color: #667eea;
  font-weight: 500;
  font-size: 0.9rem;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.tag-badge {
  background: #e7f3ff;
  color: #667eea;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 500;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #eee;
  padding-top: 1rem;
  margin-top: auto;
}

.date {
  color: #999;
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

.btn-small {
  padding: 0.5rem 1rem;
  font-size: 0.9rem;
  background: #667eea;
  color: white;
}

.btn-small:hover {
  background: #5568d3;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 3rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.empty-state p {
  font-size: 1.1rem;
  color: #666;
  margin-bottom: 1rem;
}

.error-message {
  background: #f8d7da;
  color: #721c24;
  padding: 1rem;
  border-radius: 4px;
  margin: 1rem 0;
}
</style>
