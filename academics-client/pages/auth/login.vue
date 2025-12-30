<template>
  <h1>Login Form</h1>
  <div>Username: <input v-model="loginFormData.username"></div>
  <div>Password: <input v-model="loginFormData.password"></div>
  <button @click="login">LOGIN</button>
  <button @click="reset">RESET</button>

  <div v-if="token">
    <div>Token: {{ token }}</div>
  </div>

  <div v-if="user">
    <div>User: <pre>{{ user }}</pre></div>
  </div>

  <div v-if="messages.length > 0">
    <h2>Messages</h2>

    <div v-for="message in messages">
      <pre>{{ message }}</pre>
    </div>
  </div>
</template>

<script setup>
const config = useRuntimeConfig()
const api = config.public.apiBase;
const loginFormData = reactive({
  username: "",
  password: ""
})
const token = ref(null)
const user = ref(null)
const messages = ref([])

function reset() {
  token.value = null
  user.value = null
  messages.value = []
}

async function login() {
  try {
    await $fetch(`${api}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json'
      },
      body: loginFormData,
      onResponse({ request, response, options }) {
        messages.value.push({
          method: options.method,
          request: request,
          status: response.status,
          statusText: response.statusText,
          payload: response._data
        })
        if (response.status == 200) {
          token.value = response._data.token || response._data
          getUserInfo()
        }
      }
    })
  } catch (e) {
    console.error('login request failed: ', e)
  }
}

async function getUserInfo() {
  try {
    await $fetch(`${api}/auth/user`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        Authorization: `Bearer ${token.value}`
      },
      onResponse({ request, response, options }) {
        messages.value.push({
          method: options.method,
          request: request,
          status: response.status,
          statusText: response.statusText,
          payload: response._data
        })
        if (response.status == 200) {
          user.value = response._data
        }
      }
    })
  } catch (e) {
    console.error('user info request failed: ', e)
  }
}
</script>
