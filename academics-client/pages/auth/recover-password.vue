<script setup>
const config = useRuntimeConfig()
const api = config.public.apiBase

const email = ref('')
const loading = ref(false)
const success = ref(false)
const error = ref(null)

async function recoverPassword() {
  loading.value = true
  error.value = null

  try {
    await $fetch(`${api}/auth/recover-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json'
      },
      body: { email: email.value }
    })

    success.value = true
  } catch (e) {
    error.value = 'Erro ao tentar recuperar palavra-passe'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-container">
      <h1>Recuperar palavra-passe</h1>

      <form @submit.prevent="recoverPassword" class="login-form">
        <div class="form-group">
          <label>Email</label>
          <input
            v-model="email"
            type="email"
            required
            placeholder="Digite o seu email"
            class="form-input"
          />
        </div>

        <button class="btn btn-primary btn-large" :disabled="loading">
          {{ loading ? 'A enviar...' : 'Recuperar palavra-passe' }}
        </button>
      </form>

      <p v-if="success" class="success-message">
        Se o email existir, foi enviada uma mensagem com instruções.
      </p>

      <p v-if="error" class="error-message">
        {{ error }}
      </p>

      <NuxtLink to="/auth/login" class="forgot-password">
        Voltar ao login
      </NuxtLink>
    </div>
  </div>
</template>

<style scoped>
.success-message {
  background: #d4edda;
  color: #155724;
  padding: 0.75rem;
  border-radius: 4px;
  margin-top: 1rem;
  font-size: 0.9rem;
}
</style>
