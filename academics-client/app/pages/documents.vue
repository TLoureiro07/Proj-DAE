<template>
  <h2>My Documents</h2>

  <ul>
    <li v-for="doc in docs" :key="doc.id">
      {{ doc.filename }}
      <button @click="download(doc.id)">Download</button>
    </li>
  </ul>
</template>

<script setup>
const docs = ref([])
const config = useRuntimeConfig()
const api = config.public.apiBase
const { token } = storeToRefs(useAuthStore())

onMounted(async () => {
  docs.value = await $fetch(`${api}/documents`, {
    headers: { Authorization: `Bearer ${token.value}` },
  })
})

function download(id) {
  window.location.href = `${api}/documents/download/${id}?token=${token.value}`
}
</script>
