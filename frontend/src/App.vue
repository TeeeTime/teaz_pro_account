<script setup lang="ts">
import { ref } from 'vue'
import { TopBar, useTheme } from '@teeetime/teaz-pro-ui'
import type { TopBarMenuItem } from '@teeetime/teaz-pro-ui'
import { useAuth } from '@/auth'
import ConnectedApps from '@/components/ConnectedApps.vue'
import ProfilePicture from '@/components/ProfilePicture.vue'
import SignInSecurity from '@/components/SignInSecurity.vue'

// Applies the persisted/system theme to <html data-theme> on first render.
useTheme()

const auth = useAuth()

// Set by ProfilePicture, so a new picture shows in the top bar right away.
const avatar = ref<string>()

// No "Account" here: this is the account page.
const MENU: TopBarMenuItem[] = [{ label: 'Sign out', icon: 'logout' }]

function onMenuSelect(item: TopBarMenuItem) {
  if (item.label === 'Sign out') auth.logout()
}
</script>

<template>
  <TopBar
    app-name="Account"
    home-url="https://portal.teaz.pro"
    :user-name="auth.user.value?.name"
    :avatar-url="avatar"
    :menu-items="MENU"
    @select="onMenuSelect"
  />

  <main class="page">
    <h1 class="page__title">{{ auth.user.value?.name }}</h1>
    <p class="page__text">
      {{ auth.user.value?.username }}<template v-if="auth.user.value?.email"> · {{ auth.user.value.email }}</template>
    </p>

    <ProfilePicture @changed="avatar = $event" />
    <SignInSecurity />
    <ConnectedApps />
  </main>
</template>

<style scoped>
.page {
  max-width: 720px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}

.page__title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text-primary);
}

.page__text {
  margin: 4px 0 24px;
  overflow-wrap: anywhere;
  color: var(--color-text-secondary);
}

@media (max-width: 420px) {
  .page {
    padding: 20px 12px 40px;
  }
}
</style>
