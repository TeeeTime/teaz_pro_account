<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

/*
 * Square crop in the browser: drag to move, slider to zoom. Decoding here
 * instead of on the server also takes care of WebP and of the rotation stored
 * in phone photos. What gets uploaded is a plain 512 px JPEG.
 */
const props = defineProps<{ image: ImageBitmap }>()
const emit = defineEmits<{ save: [Blob]; cancel: [] }>()

const VIEW = 280
const OUTPUT = 512
const MAX_ZOOM = 4

const dialog = ref<HTMLDialogElement | null>(null)
const canvas = ref<HTMLCanvasElement | null>(null)

const zoom = ref(1)
const centerX = ref(props.image.width / 2)
const centerY = ref(props.image.height / 2)

/** Side of the cropped square, in image pixels. */
const side = computed(() => Math.min(props.image.width, props.image.height) / zoom.value)

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max)
}

// Keep the crop inside the image, also when zooming out near an edge.
function keepInside() {
  const half = side.value / 2
  centerX.value = clamp(centerX.value, half, props.image.width - half)
  centerY.value = clamp(centerY.value, half, props.image.height - half)
}

function draw(target: HTMLCanvasElement, size: number) {
  const context = target.getContext('2d')!
  // White like the server's background, for transparent images.
  context.fillStyle = '#fff'
  context.fillRect(0, 0, size, size)
  context.imageSmoothingQuality = 'high'
  const half = side.value / 2
  context.drawImage(props.image, centerX.value - half, centerY.value - half, side.value, side.value, 0, 0, size, size)
}

function redraw() {
  keepInside()
  if (canvas.value) draw(canvas.value, VIEW)
}

watch([zoom, centerX, centerY], redraw)

let dragFrom: { x: number; y: number } | null = null

function startDrag(event: PointerEvent) {
  dragFrom = { x: event.clientX, y: event.clientY }
  ;(event.target as HTMLElement).setPointerCapture(event.pointerId)
}

function drag(event: PointerEvent) {
  if (!dragFrom) return
  const scale = side.value / VIEW
  centerX.value -= (event.clientX - dragFrom.x) * scale
  centerY.value -= (event.clientY - dragFrom.y) * scale
  dragFrom = { x: event.clientX, y: event.clientY }
}

function stopDrag() {
  dragFrom = null
}

function save() {
  const output = document.createElement('canvas')
  output.width = OUTPUT
  output.height = OUTPUT
  draw(output, OUTPUT)
  output.toBlob(
    (blob) => {
      if (blob) emit('save', blob)
    },
    'image/jpeg',
    0.92,
  )
}

onMounted(() => {
  dialog.value?.showModal()
  redraw()
})

onBeforeUnmount(() => props.image.close())
</script>

<template>
  <dialog ref="dialog" class="crop" aria-label="Crop your profile picture" @cancel.prevent="emit('cancel')">
    <h2 class="crop__title">Crop your picture</h2>
    <canvas
      ref="canvas"
      class="crop__canvas"
      :width="VIEW"
      :height="VIEW"
      @pointerdown="startDrag"
      @pointermove="drag"
      @pointerup="stopDrag"
      @pointercancel="stopDrag"
    />
    <label class="crop__zoom">
      Zoom
      <input v-model.number="zoom" type="range" min="1" :max="MAX_ZOOM" step="0.01" />
    </label>
    <div class="crop__actions">
      <button type="button" class="btn" @click="emit('cancel')">Cancel</button>
      <button type="button" class="btn btn--primary" @click="save">Save picture</button>
    </div>
  </dialog>
</template>

<style scoped>
.crop {
  margin: auto;
  padding: 20px;
  color: var(--color-text-primary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  box-shadow: 0 4px 12px var(--color-shadow-strong);
}

.crop::backdrop {
  background: var(--color-shadow-strong);
}

.crop__title {
  margin-bottom: 14px;
  font-size: 1rem;
}

.crop__canvas {
  display: block;
  width: 280px;
  max-width: 100%;
  aspect-ratio: 1;
  touch-action: none;
  cursor: move;
  border: 1px solid var(--color-border);
}

.crop__zoom {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.crop__zoom input {
  flex: 1;
  min-width: 0;
  accent-color: var(--color-accent);
}

.crop__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>
