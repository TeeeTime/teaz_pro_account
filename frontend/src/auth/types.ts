import type { Ref } from 'vue'

export interface AuthUser {
  /** Keycloak user id (sub). Also the key of the user's profile picture: avatarUrl(id). */
  id: string
  name: string
  username: string
  email?: string
  /** Keycloak realm roles (realm_access.roles). For showing/hiding UI only; backends enforce them. */
  roles: readonly string[]
}

/** Keycloak's own pages for changing an account, opened through the login redirect (kc_action). */
export type AccountAction = 'UPDATE_PROFILE' | 'UPDATE_PASSWORD' | 'CONFIGURE_TOTP'

/** How the last AccountAction ended, after Keycloak redirected back. */
export interface ActionResult {
  action?: string
  status: 'success' | 'cancelled' | 'error'
}

/*
 * The contract every auth implementation (mock or Keycloak) fulfils, so
 * components only ever talk to `useAuth()` and never to a concrete client.
 */
export interface AuthService {
  user: Readonly<Ref<AuthUser | null>>
  actionResult: Readonly<Ref<ActionResult | null>>
  /** Keycloak Account Console, page for the user's sign-in methods. */
  signingInUrl: string
  /** Resolves once a user is logged in. May redirect to the login page instead. */
  init(): Promise<void>
  logout(): Promise<void>
  /** A valid access token for backend calls, refreshed if needed. */
  getToken(): Promise<string | undefined>
  /** Leaves the app for the Keycloak page of this action; it redirects back afterwards. */
  runAction(action: AccountAction): Promise<void>
  /**
   * Revokes a client's access to this account on every device: its consent and
   * offline tokens. Other sessions, like this browser's login, stay.
   */
  disconnectApp(clientId: string): Promise<void>
}
