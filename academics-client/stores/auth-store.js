import { defineStore } from "pinia";

export const useAuthStore = defineStore("authStore", () => {
  const token = ref(null)
  const user = ref(null)

  function setToken(newToken) {
    token.value = newToken
  }

  function setUser(newUser) {
    user.value = newUser
  }

  function logout() {
    token.value = null
    user.value = null
  }

  return { token, user, setToken, setUser, logout }
})
