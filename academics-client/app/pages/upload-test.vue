<template>
    <h2>File Upload</h2>
    <input type="file" ref="fileInput" multiple accept="image/jpeg,image/png" @change="files = $event.target.files" />
    <button @click="uploadFiles()">Upload</button>
    <h2>Files</h2>
    <div v-if="files && files.length > 0">
        <p>Selected files:</p>
        <ul>
            <li v-for="(file, i) in files" :key="i">
                {{ file.name }} ({{ (file.size / 1024 / 1024).toFixed(2) }} MB)
            </li>
        </ul>
    </div>
    <div v-else>No files selected.</div>
    <h2>User Info</h2>
    {{ user ? `Welcome, ${user.name}` : 'Not logged in' }}
    <h2>Messages</h2>
    <div v-if="messages.length > 0">
        <div v-for="message in messages">
            <pre>{{ message }}</pre>
        </div>
    </div>
    <div v-else>No messages yet.</div>
</template>
<script setup>
import { useAuthStore } from '~/stores/auth-store.js'
const authStore = useAuthStore()
const { token, user } = storeToRefs(authStore)
const config = useRuntimeConfig()
const api = config.public.apiBase
const fileInput = ref(null)
const files = ref(null)
const messages = ref([])
async function uploadFiles() {
    if (!files.value || files.value.length === 0) {
        return messages.value.push('No files selected for upload.')
    }
    Array.from(files.value).forEach(async (file) => {
        const formData = new FormData()
        formData.append('file', file)
        await $fetch(`${api}/documents/upload`, {
            method: 'POST',
            body: formData,
            headers: {
                Authorization: `Bearer ${token.value}`,
            },
            onResponse({ request, response, options }) {
                messages.value.push({
                    method: options.method,
                    request: request,
                    status: response.status,
                    statusText: response.statusText,
                    payload: response._data,
                })
            },
        })
    })
    return messages.value.push('Files uploaded successfully.')
}
</script>