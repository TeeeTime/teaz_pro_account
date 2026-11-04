<script setup lang="ts">
import { computed } from 'vue'
import { Icon } from '@teeetime/teaz-pro-ui'
import { useAuth } from '@/auth'
import type { AccountAction } from '@/auth'

interface Row {
  action: AccountAction
  title: string
  text: string
  label: string
  icon: string
}

const ROWS: Row[] = [
  {
    action: 'UPDATE_PROFILE',
    title: 'Username and profile',
    text: 'Your username, name and email address.',
    label: 'Change',
    icon: 'pencil',
  },
  {
    action: 'UPDATE_PASSWORD',
    title: 'Password',
    text: 'You may be asked to sign in again first.',
    label: 'Change password',
    icon: 'lock',
  },
  {
    action: 'CONFIGURE_TOTP',
    title: 'Two-factor authentication',
    text: 'Codes from an authenticator app, on top of your password.',
    label: 'Set up 2FA',
    icon: 'shield',
  },
]

const auth = useAuth()

const NAMES: Record<string, string> = {
  UPDATE_PROFILE: 'Your profile',
  UPDATE_PASSWORD: 'Your password',
  CONFIGURE_TOTP: 'Two-factor authentication',
}

const result = computed(() => {
  const last = auth.actionResult.value
  if (!last || last.status === 'cancelled') return null
  const name = (last.action && NAMES[last.action]) || 'That change'
  return last.status === 'success'
    ? { failed: false, text: `${name}: saved.` }
    : { failed: true, text: `${name}: that did not work. Please try again.` }
})
</script>

<template>
  <section class="panel">
    <h2 class="panel__title">Sign-in and security</h2>
    <p class="panel__text">These open the Teaz login pages and bring you back here afterwards.</p>

    <ul class="rows">
      <li v-for="row in ROWS" :key="row.action" class="row">
        <div class="row__text">
          <h3 class="row__title">{{ row.title }}</h3>
          <p class="row__hint">{{ row.text }}</p>
          <a v-if="row.action === 'CONFIGURE_TOTP'" class="row__link" :href="auth.signingInUrl">
            Remove a 2FA device
            <Icon name="external" />
          </a>
        </div>
        <button type="button" class="btn" @click="auth.runAction(row.action)">
          <Icon :name="row.icon" />
          {{ row.label }}
        </button>
      </li>
    </ul>

    <p v-if="result" class="message" :class="{ 'message--error': result.failed }" role="status">{{ result.text }}</p>
  </section>
</template>

<style scoped>
.rows {
  margin: 0;
  padding: 0;
  list-style: none;
}

.row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px 20px;
  padding: 14px 0;
  border-top: 1px solid var(--color-border);
}

.row:last-child {
  padding-bottom: 0;
}

.row__text {
  flex: 1 1 240px;
  min-width: 0;
}

.row__title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.row__hint {
  margin-top: 2px;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}

.row__link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  font-size: 0.85rem;
  color: var(--color-accent);
}

.row__link .icon {
  width: 14px;
  height: 14px;
}
</style>
