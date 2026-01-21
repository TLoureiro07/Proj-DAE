<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const token = route.query.token as string

const newPassword = ref('')
const confirmPassword = ref('')
const error = ref('')
const success = ref('')

const resetPassword = async () => {
  error.value = ''
  success.value = ''

  if (!token) {
    error.value = 'Token inválido'
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    error.value = 'As passwords não coincidem'
    return
  }

  try {
    await $fetch('http://localhost:8080/academics/api/auth/reset-password', {
      method: 'POST',
      body: {
        token,
        newPassword: newPassword.value,
        confirmPassword: confirmPassword.value
      }
    })

    success.value = 'Password alterada com sucesso'
    setTimeout(() => router.push('/auth/login'), 2000)

  } catch (e: any) {
    error.value = 'Token inválido ou expirado'
  }
}
</script>

<template>
  <div class="reset-container">
    <h1>Redefinir palavra-passe</h1>

    <input
      type="password"
      placeholder="Nova palavra-passe"
      v-model="newPassword"
    />

    <input
      type="password"
      placeholder="Confirmar palavra-passe"
      v-model="confirmPassword"
    />

    <button @click="resetPassword">
      Alterar palavra-passe
    </button>

    <p v-if="error" style="color:red">{{ error }}</p>
    <p v-if="success" style="color:green">{{ success }}</p>
  </div>
</template>

<style scoped>
.reset-container {
  max-width: 400px;
  margin: 100px auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
</style>
