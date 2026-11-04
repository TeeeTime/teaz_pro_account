import { useAuth } from '@/auth'

// Relative /api URLs: same host in prod (Traefik), Vite's proxy in dev.
const OWN_AVATAR = '/api/me/avatar'

async function send(method: 'PUT' | 'DELETE', body?: FormData): Promise<void> {
  const token = await useAuth().getToken()
  const response = await fetch(OWN_AVATAR, {
    method,
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body,
  })
  if (response.ok) return
  const problem = await response.json().catch(() => null)
  throw new Error(problem?.detail ?? 'That did not work. Please try again.')
}

export function uploadAvatar(picture: Blob): Promise<void> {
  const form = new FormData()
  form.append('file', picture, 'avatar.jpg')
  return send('PUT', form)
}

export function deleteAvatar(): Promise<void> {
  return send('DELETE')
}

/** Own picture, from this app rather than avatarUrl(), so it also works against the local backend. */
export function ownAvatarUrl(userId: string, version: string | null): string {
  return `/api/avatars/${userId}?size=256${version ? `&v=${version}` : ''}`
}

/*
 * Picture URLs are stable and cached for minutes. After a change, a new
 * version in the query shows the new one here at once, also after a reload.
 */
const VERSION_KEY = 'account:avatar-version'

export function avatarVersion(): string | null {
  return localStorage.getItem(VERSION_KEY)
}

export function bumpAvatarVersion(): string {
  const version = Date.now().toString(36)
  localStorage.setItem(VERSION_KEY, version)
  return version
}
