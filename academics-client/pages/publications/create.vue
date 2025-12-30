<template>
  <div>
    <h1>Criar Publicação</h1>
    
    <div v-if="submitting">A criar publicação...</div>
    <div v-else-if="success">
      <p style="color: green;">Publicação criada com sucesso!</p>
      <nuxt-link :to="`/publications/${publicationId}`">Ver publicação</nuxt-link>
    </div>
    <div v-else>
      <form @submit.prevent="createPublication">
        <div style="margin: 15px 0;">
          <label>Título: *</label>
          <input v-model="form.title" type="text" required style="width: 100%; padding: 5px;" />
        </div>
        
        <div style="margin: 15px 0;">
          <label>Área Científica:</label>
          <input v-model="form.scientificArea" type="text" placeholder="Ex: Ciência de Dados" style="width: 100%; padding: 5px;" />
        </div>
        
        <div style="margin: 15px 0;">
          <label>Autores (um por linha):</label>
          <textarea v-model="authorsText" rows="3" placeholder="Autor 1&#10;Autor 2" style="width: 100%; padding: 5px;"></textarea>
        </div>
        
        <div style="margin: 15px 0;">
          <label>Resumo:</label>
          <textarea v-model="form.summary" rows="5" style="width: 100%; padding: 5px;"></textarea>
        </div>
        
        <div style="margin: 15px 0;">
          <label>Visibilidade:</label>
          <select v-model="form.visibility" style="width: 100%; padding: 5px;">
            <option value="public">Pública</option>
            <option value="internal">Interna</option>
            <option value="hidden">Oculta</option>
          </select>
        </div>
        
        <div style="margin: 15px 0;">
          <label>Ficheiro (opcional - PDF ou ZIP):</label>
          <input type="file" ref="fileInput" accept=".pdf,.zip" />
        </div>
        
        <button type="submit" :disabled="submitting" style="padding: 10px 20px; background: #28a745; color: white; border: none; border-radius: 3px; cursor: pointer;">
          Criar Publicação
        </button>
        <nuxt-link to="/publications" style="margin-left: 10px; padding: 10px 20px; background: #6c757d; color: white; text-decoration: none; border-radius: 3px; display: inline-block;">
          Cancelar
        </nuxt-link>
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

const authorsText = ref('')
const form = ref({
  title: '',
  scientificArea: '',
  summary: '',
  visibility: 'internal'
})

async function createPublication() {
  if (!form.value.title.trim()) {
    alert('O título é obrigatório')
    return
  }

  submitting.value = true
  success.value = false

  try {
    // Se houver ficheiro, usar upload; senão, criar sem ficheiro
    if (fileInput.value && fileInput.value.files && fileInput.value.files.length > 0) {
      const formData = new FormData()
      formData.append('file', fileInput.value.files[0])

      const response = await $fetch(`${api}/publications/upload`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token.value}`
        },
        body: formData
      })

      publicationId.value = response.id
      
      // Atualizar com os dados do formulário (se diferentes do nome do ficheiro)
      if (form.value.title !== response.title || form.value.scientificArea || form.value.summary || form.value.visibility !== 'internal') {
        await $fetch(`${api}/publications/${response.id}`, {
          method: 'PATCH',
          headers: {
            'Authorization': `Bearer ${token.value}`,
            'Content-Type': 'application/json'
          },
          body: {
            title: form.value.title,
            scientificArea: form.value.scientificArea,
            summary: form.value.summary,
            visibility: form.value.visibility
          }
        })
      }
    } else {
      // Criar sem ficheiro
      const authors = authorsText.value.split('\n')
        .map(a => a.trim())
        .filter(a => a.length > 0)

      const response = await $fetch(`${api}/publications`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token.value}`,
          'Content-Type': 'application/json'
        },
        body: {
          title: form.value.title,
          scientificArea: form.value.scientificArea,
          authors: authors,
          summary: form.value.summary,
          visibility: form.value.visibility
        }
      })

      publicationId.value = response.id
    }

    success.value = true
  } catch (e) {
    alert('Erro ao criar publicação: ' + (e.message || 'Erro desconhecido'))
    console.error('Erro:', e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (!token.value) {
    router.push('/auth/login')
  }
})
</script>

