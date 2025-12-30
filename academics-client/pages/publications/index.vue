<template>
  <div>
    <h1>Publicações</h1>
    
    <!-- Formulário de Pesquisa e Filtros -->
    <div style="border: 1px solid #ccc; padding: 15px; margin: 20px 0; border-radius: 5px; background: #f5f5f5;">
      <h3>Pesquisar e Filtrar</h3>
      <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; margin-bottom: 10px;">
        <div>
          <label>Pesquisa (título/resumo):</label>
          <input v-model="filters.search" type="text" placeholder="Pesquisar..." style="width: 100%; padding: 5px;" />
        </div>
        <div>
          <label>Área Científica:</label>
          <input v-model="filters.scientificArea" type="text" placeholder="Ex: Ciência de Dados" style="width: 100%; padding: 5px;" />
        </div>
        <div>
          <label>Tag:</label>
          <input v-model="filters.tag" type="text" placeholder="Ex: Projeto X" style="width: 100%; padding: 5px;" />
        </div>
      </div>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 10px;">
        <div>
          <label>Ordenar por:</label>
          <select v-model="filters.sortBy" style="width: 100%; padding: 5px;">
            <option value="">Data de upload</option>
            <option value="rating">Rating médio</option>
            <option value="comments">Número de comentários</option>
            <option value="ratings">Número de ratings</option>
          </select>
        </div>
        <div>
          <label>Ordem:</label>
          <select v-model="filters.order" style="width: 100%; padding: 5px;">
            <option value="desc">Descendente</option>
            <option value="asc">Ascendente</option>
          </select>
        </div>
      </div>
      <button @click="applyFilters" :disabled="loading" style="padding: 8px 15px; background: #007bff; color: white; border: none; border-radius: 3px; cursor: pointer;">
        Aplicar Filtros
      </button>
      <button @click="clearFilters" style="padding: 8px 15px; background: #6c757d; color: white; border: none; border-radius: 3px; cursor: pointer; margin-left: 10px;">
        Limpar
      </button>
    </div>

    <div v-if="loading">A carregar...</div>
    <div v-else-if="error">{{ error }}</div>
    <div v-else>
      <div v-if="publications.length === 0">
        <p>Não há publicações disponíveis com os critérios selecionados.</p>
      </div>
      <div v-else>
        <p><strong>{{ publications.length }}</strong> publicação(ões) encontrada(s)</p>
        <div v-for="pub in publications" :key="pub.id" style="border: 1px solid #ccc; padding: 15px; margin: 10px 0; border-radius: 5px;">
          <h3>
            <nuxt-link :to="`/publications/${pub.id}`">{{ pub.title || 'Sem título' }}</nuxt-link>
          </h3>
          <p><strong>Autor:</strong> {{ pub.owner || 'N/A' }}</p>
          <p v-if="pub.scientificArea"><strong>Área Científica:</strong> {{ pub.scientificArea }}</p>
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

