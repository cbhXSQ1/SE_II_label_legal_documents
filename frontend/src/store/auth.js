import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { getSession, saveSession, clearSession } from '@/utils/session'

export const useAuthStore = defineStore('auth', {
  state: () => {
    const s = getSession() || {}
    return {
      credential: s.credential || null,
      accountId: s.accountId || null,
      name: s.name || '',
      unit: s.unit || '',
      system: s.system || null,
      identity: s.identity || null,
      systemName: s.systemName || '',
      identityName: s.identityName || ''
    }
  },
  getters: {
    loggedIn: (s) => !!s.credential
  },
  actions: {
    applySession(s) {
      this.credential = s.credential
      this.accountId = s.accountId
      this.name = s.name
      this.unit = s.unit
      this.system = s.system
      this.identity = s.identity
      this.systemName = s.systemName
      this.identityName = s.identityName
      saveSession(s)
    },
    async login(account, password) {
      const data = await authApi.login(account, password)
      this.applySession(data)
      return data
    },
    async select(system, identity) {
      const data = await authApi.select(system, identity)
      this.applySession(data)
      return data
    },
    async logout() {
      try { await authApi.logout() } catch (e) { /* 忽略 */ }
      clearSession()
      this.$reset()
    }
  }
})
