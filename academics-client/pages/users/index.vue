<template>
  <div class="users-page">
    <div class="page-header">
      <h1>Gestão de Utilizadores</h1>
      <p>Criar, editar, remover, ativar e desativar utilizadores</p>
    </div>

    <!-- Criar Novo Utilizador -->
    <div class="card">
      <h2>Criar Novo Utilizador</h2>
      <form @submit.prevent="createUser" class="create-form">
        <div class="form-grid">
          <div class="form-group">
            <label>Username</label>
            <input v-model="newUser.username" type="text" required class="input" />
          </div>
          <div class="form-group">
            <label>Password</label>
            <input v-model="newUser.password" type="password" required class="input" />
          </div>
          <div class="form-group">
            <label>Nome</label>
            <input v-model="newUser.name" type="text" required class="input" />
          </div>
          <div class="form-group">
            <label>Email</label>
            <input v-model="newUser.email" type="email" required class="input" />
          </div>
          <div class="form-group">
            <label>Role</label>
            <select v-model="newUser.role" required class="input">
              <option value="Collaborator">Colaborador</option>
              <option value="Responsible">Responsável</option>
              <option value="Administrator">Administrador</option>
            </select>
          </div>
        </div>
        <button type="submit" class="btn btn-primary" :disabled="creating">
          Criar Utilizador
        </button>
      </form>
    </div>

    <!-- Lista de Utilizadores -->
    <div class="card">
      <h2>Utilizadores ({{ users.length }})</h2>
      <div v-if="loading" class="loading">A carregar...</div>
      <div v-else-if="error" class="error-message">{{ error }}</div>
      <div v-else-if="users.length === 0" class="empty-state">
        <p>Não há utilizadores.</p>
      </div>
      <div v-else class="users-table">
        <table>
          <thead>
            <tr>
              <th>Username</th>
              <th>Nome</th>
              <th>Email</th>
              <th>Role</th>
              <th>Estado</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.username" :class="{ 'inactive': !u.active }">
              <td>{{ u.username }}</td>
              <td>{{ u.name }}</td>
              <td>{{ u.email }}</td>
              <td>
                <select 
                  v-model="u.role" 
                  @change="changeRole(u.username, u.role)"
                  class="select-small"
                  :disabled="updating"
                >
                  <option value="Collaborator">Colaborador</option>
                  <option value="Responsible">Responsável</option>
                  <option value="Administrator">Administrador</option>
                </select>
              </td>
              <td>
                <span :class="u.active ? 'status-active' : 'status-inactive'">
                  {{ u.active ? 'Ativo' : 'Inativo' }}
                </span>
              </td>
              <td>
                <div class="actions">
                  <button 
                    @click="toggleActive(u.username, !u.active)"
                    class="btn btn-small"
                    :class="u.active ? 'btn-warning' : 'btn-success'"
                    :disabled="updating"
                  >
                    {{ u.active ? 'Desativar' : 'Ativar' }}
                  </button>
                  <button 
                    @click="editUser(u)"
                    class="btn btn-small btn-secondary"
                    :disabled="updating"
                  >
                    Editar
                  </button>
                  <button 
                    @click="deleteUser(u.username)"
                    class="btn btn-small btn-danger"
                    :disabled="updating"
                  >
                    Remover
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal de Edição -->
    <div v-if="editingUser" class="modal-overlay" @click="editingUser = null">
      <div class="modal" @click.stop>
        <h2>Editar Utilizador</h2>
        <form @submit.prevent="saveUser">
          <div class="form-group">
            <label>Nome</label>
            <input v-model="editingUser.name" type="text" required class="input" />
          </div>
          <div class="form-group">
            <label>Email</label>
            <input v-model="editingUser.email" type="email" required class="input" />
          </div>
          <div class="modal-actions">
            <button type="submit" class="btn btn-primary" :disabled="updating">
              Guardar
            </button>
            <button type="button" @click="editingUser = null" class="btn btn-secondary">
              Cancelar
            </button>
          </div>
        </form>
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
const { token, user } = storeToRefs(authStore)
const router = useRouter()

const users = ref([])
const newUser = ref({
  username: '',
  password: '',
  name: '',
  email: '',
  role: 'Collaborator'
})
const editingUser = ref(null)
const loading = ref(true)
const creating = ref(false)
const updating = ref(false)
const error = ref(null)

const isAdmin = computed(() => user.value?.role === 'Administrator')

async function loadUsers() {
  if (!token.value) {
    router.push('/auth/login')
    return
  }

  if (!isAdmin.value) {
    router.push('/')
    return
  }

  loading.value = true
  error.value = null
  try {
    users.value = await $fetch(`${api}/users`, {
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
  } catch (e) {
    error.value = 'Erro ao carregar utilizadores: ' + (e.message || 'Erro desconhecido')
    console.error('Erro:', e)
  } finally {
    loading.value = false
  }
}

async function createUser() {
  creating.value = true
  try {
    const created = await $fetch(`${api}/users`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: newUser.value
    })
    users.value.push(created)
    newUser.value = {
      username: '',
      password: '',
      name: '',
      email: '',
      role: 'Collaborator'
    }
    alert('Utilizador criado com sucesso!')
  } catch (e) {
    let errorMsg = 'Erro ao criar utilizador'
    if (e.data?.error) {
      errorMsg += ': ' + e.data.error
    } else if (e.response?._data?.error) {
      errorMsg += ': ' + e.response._data.error
    }
    alert(errorMsg)
  } finally {
    creating.value = false
  }
}

async function changeRole(username, newRole) {
  updating.value = true
  try {
    await $fetch(`${api}/users/${username}/role`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: { role: newRole }
    })
    alert('Role atualizado com sucesso!')
  } catch (e) {
    alert('Erro ao atualizar role: ' + (e.message || 'Erro desconhecido'))
    await loadUsers() // Recarregar para reverter mudança
  } finally {
    updating.value = false
  }
}

async function toggleActive(username, active) {
  updating.value = true
  try {
    await $fetch(`${api}/users/${username}/active`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: { active }
    })
    const user = users.value.find(u => u.username === username)
    if (user) user.active = active
  } catch (e) {
    alert('Erro ao alterar estado: ' + (e.message || 'Erro desconhecido'))
  } finally {
    updating.value = false
  }
}

function editUser(u) {
  editingUser.value = { ...u }
}

async function saveUser() {
  updating.value = true
  try {
    await $fetch(`${api}/users/${editingUser.value.username}`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: {
        name: editingUser.value.name,
        email: editingUser.value.email
      }
    })
    const user = users.value.find(u => u.username === editingUser.value.username)
    if (user) {
      user.name = editingUser.value.name
      user.email = editingUser.value.email
    }
    editingUser.value = null
    alert('Utilizador atualizado com sucesso!')
  } catch (e) {
    alert('Erro ao atualizar utilizador: ' + (e.message || 'Erro desconhecido'))
  } finally {
    updating.value = false
  }
}

async function deleteUser(username) {
  if (!confirm(`Tens a certeza que queres remover o utilizador "${username}"?`)) return

  updating.value = true
  try {
    await $fetch(`${api}/users/${username}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token.value}` }
    })
    users.value = users.value.filter(u => u.username !== username)
    alert('Utilizador removido com sucesso!')
  } catch (e) {
    alert('Erro ao remover utilizador: ' + (e.message || 'Erro desconhecido'))
  } finally {
    updating.value = false
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.users-page {
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

.card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  margin-bottom: 2rem;
}

.card h2 {
  color: #667eea;
  margin-bottom: 1rem;
  font-size: 1.2rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
}

.input {
  padding: 0.75rem;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  font-size: 1rem;
}

.input:focus {
  outline: none;
  border-color: #667eea;
}

.select-small {
  padding: 0.4rem;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  font-size: 0.9rem;
}

.users-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f8f9fa;
}

th, td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #dee2e6;
}

tr.inactive {
  opacity: 0.6;
}

.status-active {
  color: #28a745;
  font-weight: 500;
}

.status-inactive {
  color: #dc3545;
  font-weight: 500;
}

.actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 4px;
  border: none;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
  font-size: 1rem;
}

.btn-small {
  padding: 0.4rem 0.8rem;
  font-size: 0.9rem;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #5568d3;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background: #5a6268;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #218838;
}

.btn-warning {
  background: #ffc107;
  color: #333;
}

.btn-warning:hover:not(:disabled) {
  background: #e0a800;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #c82333;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  max-width: 500px;
  width: 90%;
}

.modal h2 {
  margin-bottom: 1.5rem;
  color: #333;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  margin-top: 1.5rem;
}

.loading {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.error-message {
  background: #f8d7da;
  color: #721c24;
  padding: 1rem;
  border-radius: 4px;
  margin: 1rem 0;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #666;
}
</style>
