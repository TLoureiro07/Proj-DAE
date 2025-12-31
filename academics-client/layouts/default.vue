<script setup>
import { useAuthStore } from "~/stores/auth-store.js";
import { storeToRefs } from 'pinia'

const router = useRouter()
const authStore = useAuthStore()
const { user, token } = storeToRefs(authStore)

function logout() {
  authStore.logout()
  router.push('/')
}

const isAdmin = computed(() => user.value?.role === 'Administrator')
const isResponsible = computed(() => user.value?.role === 'Responsible' || isAdmin.value)
const isCollaborator = computed(() => user.value?.role === 'Collaborator' || isResponsible.value)
</script>

<template>
  <div class="app-container">
    <header class="app-header">
      <div class="header-content">
        <h1 class="logo">
          <nuxt-link to="/">Plataforma de Gestão de Publicações Científicas</nuxt-link>
        </h1>
        <nav class="main-nav">
          <template v-if="!token">
            <nuxt-link to="/auth/login" class="nav-link">Login</nuxt-link>
          </template>
          <template v-else>
            <nuxt-link to="/publications" class="nav-link">Publicações</nuxt-link>
            <nuxt-link v-if="isCollaborator" to="/publications/create" class="nav-link">Criar Publicação</nuxt-link>
            <nuxt-link v-if="isCollaborator" to="/tags/subscriptions" class="nav-link">Minhas Subscrições</nuxt-link>
            <nuxt-link v-if="isResponsible" to="/tags" class="nav-link">Gestão de Tags</nuxt-link>
            <nuxt-link v-if="isAdmin" to="/users" class="nav-link">Utilizadores</nuxt-link>
            <nuxt-link v-if="token" to="/profile" class="nav-link">Perfil</nuxt-link>
            <div class="user-info">
              <span class="user-name">{{ user?.name || 'Utilizador' }}</span>
              <span class="user-role">({{ user?.role }})</span>
              <button @click="logout" class="logout-btn">Sair</button>
            </div>
          </template>
        </nav>
      </div>
    </header>

    <main class="app-main">
      <slot />
    </main>

  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
}

.app-header {
  background: #2d3748;
  color: white;
  padding: 0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.header-content {
  max-width: 100%;
  margin: 0;
  padding: 1rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.logo {
  margin: 0;
  font-size: 1.5rem;
  font-weight: bold;
}

.logo a {
  color: white;
  text-decoration: none;
}

.main-nav {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.nav-link {
  color: white;
  text-decoration: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  transition: background 0.2s;
  font-weight: 500;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.2);
}

.nav-link.router-link-active {
  background: rgba(255, 255, 255, 0.3);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-left: 1rem;
  padding-left: 1rem;
  border-left: 1px solid rgba(255, 255, 255, 0.3);
}

.user-name {
  font-weight: 500;
}

.user-role {
  font-size: 0.85rem;
  opacity: 0.9;
}

.logout-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  padding: 0.4rem 0.8rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.2s;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.app-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 2rem;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
}

.app-footer {
  background: #333;
  color: white;
  text-align: center;
  padding: 1rem;
  margin-top: auto;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .main-nav {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
  }
  
  .user-info {
    margin-left: 0;
    padding-left: 0;
    border-left: none;
    border-top: 1px solid rgba(255, 255, 255, 0.3);
    padding-top: 0.5rem;
    width: 100%;
  }
}
</style>
