<script setup lang="ts">
import { ref } from 'vue'
import { Icon } from '@teeetime/teaz-pro-ui'
import { useAuth } from '@/auth'

const WIKI_MCP_CLIENT = 'wiki-mcp'

const auth = useAuth()

const confirming = ref(false)
const busy = ref(false)
const outcome = ref<{ failed: boolean; text: string } | null>(null)

async function disconnect() {
  busy.value = true
  outcome.value = null
  try {
    await auth.disconnectApp(WIKI_MCP_CLIENT)
    outcome.value = {
      failed: false,
      text: 'Wiki MCP is disconnected on all your devices. It can take a few minutes until running AI clients stop.',
    }
  } catch {
    outcome.value = { failed: true, text: 'That did not work. Please try again.' }
  } finally {
    busy.value = false
    confirming.value = false
  }
}
</script>

<template>
  <section class="panel">
    <h2 class="panel__title">Connected apps</h2>

    <div class="connection">
      <div class="connection__text">
        <h3 class="connection__title">Wiki MCP</h3>
        <p class="connection__hint">
          AI assistants you signed in to read your private wikis. Disconnecting signs out all of them on every device.
          You stay signed in here, and can connect again at any time.
        </p>
      </div>

      <div v-if="confirming" class="connection__actions">
        <button type="button" class="btn" :disabled="busy" @click="confirming = false">Cancel</button>
        <button type="button" class="btn btn--danger" :disabled="busy" @click="disconnect">
          <Icon name="unlink" />
          Disconnect all
        </button>
      </div>
      <button v-else type="button" class="btn" @click="confirming = true">
        <Icon name="unlink" />
        Disconnect
      </button>
    </div>

    <p v-if="outcome" class="message" :class="{ 'message--error': outcome.failed }" role="status">{{ outcome.text }}</p>
  </section>
</template>

<style scoped>
.connection {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px 20px;
}

.connection__text {
  flex: 1 1 240px;
  min-width: 0;
}

.connection__title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-primary);
}

.connection__hint {
  margin-top: 2px;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}

.connection__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
