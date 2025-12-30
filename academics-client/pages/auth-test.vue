<template>
  <div>
    <h1>Login Form</h1>
    <div>Username: <input v-model="loginFormData.username" /></div>
    <div>Password: <input type="password" v-model="loginFormData.password" /></div>
    <button @click="login">LOGIN</button>
    <button @click="reset">RESET</button>
  </div>

  <div v-if="token">
    <h2>API Request Form</h2>
    <div>Request: <code>GET {{ api }}</code>/<input v-model="apiFormData.path" /></div>
    <div>Token: {{ token }}</div>
    <button @click="sendRequest">SEND REQUEST</button>
  </div>

  <div v-if="messages.length > 0">
    <h2>Messages</h2>
    <div v-for="message in messages"><pre>{{ message }}</pre></div>
  </div>
</template>

<script setup>
const config = useRuntimeConfig()
const api = config.public.apiBase

const loginFormData = reactive({ username: null, password: null })
const apiFormData = reactive({ path: "students" })

const token = ref(null)
const messages = ref([])

async function login() {
  reset()
  try {
    await $fetch(`${api}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: loginFormData,
      onResponse({ request, response, options }) {
        messages.value.push({
          method: options.method,
          request,
          status: response.status,
          statusText: response.statusText,
          payload: response._data,
        })
        if (response.status === 200) token.value = response._data.token || response._data
      },
    })
  } catch (e) {
    console.error("login request failed: ", e)
  }
}

function reset() {
  token.value = null
  messages.value = []
}

async function sendRequest() {
  try {
    await $fetch(`${api}/${apiFormData.path}`, {
      method: "GET",
      headers: { Accept: "application/json", Authorization: `Bearer ${token.value}` },
      onResponse({ request, response, options }) {
        messages.value.push({
          method: options.method,
          request,
          status: response.status,
          statusText: response.statusText,
          payload: response._data,
        })
      },
    })
  } catch (e) {
    console.error("api request failed: ", e)
  }
}
</script>
