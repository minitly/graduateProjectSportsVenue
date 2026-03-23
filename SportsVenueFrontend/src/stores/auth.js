import { defineStore } from 'pinia'

const STORAGE_KEY = 'sports-venue-auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    user: null
  }),
  getters: {
    isAuthed: (state) => Boolean(state.token),
    role: (state) => state.user?.role || 'USER'
  },
  actions: {
    hydrate() {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) return
      try {
        const payload = JSON.parse(raw)
        this.token = payload.token || ''
        this.user = payload.user || null
      } catch (error) {
        localStorage.removeItem(STORAGE_KEY)
      }
    },
    setSession(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ token: this.token, user: this.user })
      )
    },
    clear() {
      this.token = ''
      this.user = null
      localStorage.removeItem(STORAGE_KEY)
    }
  }
})
