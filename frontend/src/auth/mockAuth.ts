import { readonly, ref } from 'vue'
import type { AccountAction, ActionResult, AuthService, AuthUser } from './types'

/*
 * Local-dev stand-in for Keycloak: always "logged in" as a fixed user, no
 * network calls. Used whenever VITE_KEYCLOAK_URL is not set. Actions report
 * success right away, so the feedback in the UI can be seen without Keycloak.
 */
export function createMockAuth(): AuthService {
  const user = ref<AuthUser | null>(null)
  const actionResult = ref<ActionResult | null>(null)

  return {
    user: readonly(user),
    actionResult: readonly(actionResult),
    signingInUrl: '#',
    async init() {
      // Give the mock user every role the app checks, so all of it stays usable in dev.
      user.value = {
        // Same id as the backend's MockSecurityConfig.
        id: '00000000-0000-0000-0000-000000000001',
        name: 'Jordan Lee',
        username: 'jlee',
        email: 'jordan.lee@example.com',
        roles: [],
      }
    },
    async logout() {
      user.value = null
      window.location.reload()
    },
    async getToken() {
      return undefined
    },
    async runAction(action: AccountAction) {
      actionResult.value = { action, status: 'success' }
    },
    async disconnectApp() {},
  }
}
