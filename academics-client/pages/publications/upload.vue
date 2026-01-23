<template>
  <div>
    <h1>Upload de Publicação</h1>

    <div v-if="uploading">A fazer upload...</div>
    <div v-else-if="success">
      <p style="color: green;">Publicação criada com sucesso!</p>
      <nuxt-link :to="`/publications/${publicationId}`">Ver publicação</nuxt-link>
    </div>
    <div v-else>
      <form @submit.prevent="uploadFile">
        <div style="margin: 20px 0;">
          <label>Ficheiro (PDF ou ZIP): </label>
          <input type="file" ref="fileInput" accept=".pdf,.zip" required />
        </div>
        <button type="submit" :disabled="uploading">Fazer Upload</button>
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
const uploading = ref(false)
const success = ref(false)
const publicationId = ref(null)

async function uploadFile() {
  if (!fileInput.value || !fileInput.value.files || fileInput.value.files.length === 0) {
    alert('Por favor, seleciona um ficheiro')
    return
  }

  uploading.value = true
  success.value = false

  try {
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
    success.value = true
  } catch (e) {
    alert('Erro ao fazer upload: ' + (e.message || 'Erro desconhecido'))
    console.error('Erro:', e)
  } finally {
    uploading.value = false
  }
}

onMounted(() => {
  if (!token.value) {
    router.push('/auth/login')
  }
})
</script>

