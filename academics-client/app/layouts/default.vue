<script setup>
import { useAuthStore } from "~/stores/auth-store.js";
const router = useRouter()
const authStore = useAuthStore()

function logout() {
  authStore.logout()
  router.push('/')
}
</script>

<template>
  <nav>
    <nuxt-link to="/auth/login">Login</nuxt-link>
    |
    <nuxt-link to="/upload-test">Upload</nuxt-link>
    |
    <nuxt-link to="/documents">Publicações</nuxt-link>
|
    <nuxt-link to="/students">Subscrições</nuxt-link>
    <template v-if="authStore.user">
      |
      <span>👋 {{ authStore.user.name }}</span>
      |
      <a href="#" @click.prevent="logout">Logout</a>
    </template>
  </nav>

  <div>
    <slot />
  </div>
</template>
