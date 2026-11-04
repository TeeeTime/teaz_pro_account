<script setup lang="ts">
import { computed, ref, shallowRef, watch } from 'vue'
import { Avatar, Icon } from '@teeetime/teaz-pro-ui'
import { useAuth } from '@/auth'
import { avatarVersion, bumpAvatarVersion, deleteAvatar, ownAvatarUrl, uploadAvatar } from '@/avatarApi'
import CropDialog from './CropDialog.vue'

const emit = defineEmits<{ changed: [url: string] }>()

const auth = useAuth()
const userId = computed(() => auth.user.value?.id ?? '')
const version = ref(avatarVersion())
const url = computed(() => ownAvatarUrl(userId.value, version.value))

const hasPicture = ref(false)
const error = ref('')
const busy = ref(false)
const toCrop = shallowRef<ImageBitmap | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

// Runs for each new URL, so it also tells the top bar after a change.
watch(
  url,
  async (current) => {
    hasPicture.value = (await fetch(current, { method: 'HEAD' }).catch(() => null))?.ok ?? false
    emit('changed', current)
  },
  { immediate: true },
)

async function onFileChosen() {
  const file = fileInput.value?.files?.[0]
  if (fileInput.value) fileInput.value.value = ''
  if (!file) return
  error.value = ''
  try {
    toCrop.value = await createImageBitmap(file)
  } catch {
    error.value = 'This file can not be read as an image. PNG, JPEG, GIF and WebP work.'
  }
}

async function run(change: () => Promise<void>) {
  busy.value = true
  error.value = ''
  try {
    await change()
    version.value = bumpAvatarVersion()
  } catch (failure) {
    error.value = (failure as Error).message
  } finally {
    busy.value = false
  }
}

function save(picture: Blob) {
  toCrop.value = null
  run(() => uploadAvatar(picture))
}
</script>

<template>
  <section class="panel">
    <h2 class="panel__title">Profile picture</h2>
    <p class="panel__text">Shown in every Teaz app. Anyone with the link to it can see it, also without an account.</p>

    <div class="picture">
      <Avatar :key="url" :src="url" :name="auth.user.value?.name" :size="96" round />
      <div class="picture__actions">
        <button type="button" class="btn" :disabled="busy" @click="fileInput?.click()">
          <Icon name="upload" />
          {{ hasPicture ? 'Change picture' : 'Upload picture' }}
        </button>
        <button v-if="hasPicture" type="button" class="btn btn--danger" :disabled="busy" @click="run(deleteAvatar)">
          <Icon name="trash" />
          Remove
        </button>
      </div>
    </div>
    <p v-if="error" class="message message--error" role="alert">{{ error }}</p>

    <input ref="fileInput" class="picture__input" type="file" accept="image/*" aria-label="Choose a picture" @change="onFileChosen" />
    <CropDialog v-if="toCrop" :image="toCrop" @save="save" @cancel="toCrop = null" />
  </section>
</template>

<style scoped>
.picture {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px 20px;
}

.picture__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.picture__input {
  display: none;
}
</style>
