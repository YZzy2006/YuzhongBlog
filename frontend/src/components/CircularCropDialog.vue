<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('adminMusic.cropCover')"
    width="420px"
    :close-on-click-modal="false"
    @opened="initCanvas"
  >
    <div class="crop-container" ref="containerRef">
      <canvas ref="canvasRef" @pointerdown="onPointerDown" @wheel.prevent="onWheel" @touchmove.prevent="onTouchMove" @touchend="lastPinchDist = 0"></canvas>
    </div>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" @click="confirmCrop">{{ t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps({
  modelValue: Boolean,
  imageFile: File,
  outputSize: { type: Number, default: 300 }
})

const emit = defineEmits(['update:modelValue', 'cropped'])

const canvasRef = ref(null)
const containerRef = ref(null)

let img = null
let imgW = 0, imgH = 0
let scale = 1
let circleX = 0, circleY = 0, circleR = 0
let dragStartX = 0, dragStartY = 0, startCircleX = 0, startCircleY = 0
let isDragging = false
const DISPLAY_SIZE = 380

function initCanvas() {
  if (!props.imageFile || !canvasRef.value) return
  const canvas = canvasRef.value
  canvas.width = DISPLAY_SIZE
  canvas.height = DISPLAY_SIZE

  const url = URL.createObjectURL(props.imageFile)
  img = new Image()
  img.onload = () => {
    imgW = img.naturalWidth
    imgH = img.naturalHeight
    // Scale image to fit canvas
    scale = Math.min(DISPLAY_SIZE / imgW, DISPLAY_SIZE / imgH)
    const drawW = imgW * scale
    const drawH = imgH * scale
    // Center circle
    circleX = drawW / 2
    circleY = drawH / 2
    circleR = Math.min(drawW, drawH) / 2 * 0.85
    draw()
    URL.revokeObjectURL(url)
  }
  img.onerror = () => {
    URL.revokeObjectURL(url)
    img = null
  }
  img.src = url
}

function draw() {
  if (!canvasRef.value || !img) return
  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0, 0, DISPLAY_SIZE, DISPLAY_SIZE)

  const drawW = imgW * scale
  const drawH = imgH * scale

  // Draw image
  ctx.save()
  ctx.drawImage(img, 0, 0, drawW, drawH)

  // Dark overlay outside circle
  ctx.globalCompositeOperation = 'source-over'
  ctx.fillStyle = 'rgba(0, 0, 0, 0.5)'
  ctx.beginPath()
  ctx.rect(0, 0, DISPLAY_SIZE, DISPLAY_SIZE)
  ctx.arc(circleX, circleY, circleR, 0, Math.PI * 2, true)
  ctx.fill()

  // Circle border
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.8)'
  ctx.lineWidth = 2
  ctx.beginPath()
  ctx.arc(circleX, circleY, circleR, 0, Math.PI * 2)
  ctx.stroke()
  ctx.restore()
}

let lastPinchDist = 0

function onPointerDown(e) {
  isDragging = true
  dragStartX = e.offsetX
  dragStartY = e.offsetY
  startCircleX = circleX
  startCircleY = circleY
  canvasRef.value.setPointerCapture(e.pointerId)
  document.addEventListener('pointermove', onPointerMove)
  document.addEventListener('pointerup', onPointerUp)
}

function onPointerMove(e) {
  if (!isDragging) return
  const rect = canvasRef.value.getBoundingClientRect()
  const dx = (e.clientX - rect.left) - dragStartX
  const dy = (e.clientY - rect.top) - dragStartY
  const drawW = imgW * scale
  const drawH = imgH * scale
  circleX = Math.max(circleR, Math.min(drawW - circleR, startCircleX + dx))
  circleY = Math.max(circleR, Math.min(drawH - circleR, startCircleY + dy))
  draw()
}

function onPointerUp(e) {
  isDragging = false
  canvasRef.value?.releasePointerCapture(e.pointerId)
  document.removeEventListener('pointermove', onPointerMove)
  document.removeEventListener('pointerup', onPointerUp)
}

function onWheel(e) {
  const delta = e.deltaY > 0 ? -5 : 5
  const drawW = imgW * scale
  const drawH = imgH * scale
  const maxR = Math.min(drawW, drawH) / 2
  circleR = Math.max(20, Math.min(maxR, circleR + delta))
  // Re-clamp position
  circleX = Math.max(circleR, Math.min(drawW - circleR, circleX))
  circleY = Math.max(circleR, Math.min(drawH - circleR, circleY))
  draw()
}

function onTouchMove(e) {
  if (e.touches.length < 2) return
  const t0 = e.touches[0]
  const t1 = e.touches[1]
  const dist = Math.hypot(t1.clientX - t0.clientX, t1.clientY - t0.clientY)
  if (lastPinchDist > 0) {
    const delta = (dist - lastPinchDist) * 0.3
    const drawW = imgW * scale
    const drawH = imgH * scale
    const maxR = Math.min(drawW, drawH) / 2
    circleR = Math.max(20, Math.min(maxR, circleR + delta))
    circleX = Math.max(circleR, Math.min(drawW - circleR, circleX))
    circleY = Math.max(circleR, Math.min(drawH - circleR, circleY))
    draw()
  }
  lastPinchDist = dist
}

function confirmCrop() {
  if (!img) return
  const outCanvas = document.createElement('canvas')
  outCanvas.width = props.outputSize
  outCanvas.height = props.outputSize
  const ctx = outCanvas.getContext('2d')

  // Map circle from display coords to original image coords
  const invScale = 1 / scale
  const srcX = (circleX - circleR) * invScale
  const srcY = (circleY - circleR) * invScale
  const srcSize = circleR * 2 * invScale

  ctx.beginPath()
  ctx.arc(props.outputSize / 2, props.outputSize / 2, props.outputSize / 2, 0, Math.PI * 2)
  ctx.clip()
  ctx.drawImage(img, srcX, srcY, srcSize, srcSize, 0, 0, props.outputSize, props.outputSize)

  outCanvas.toBlob(blob => {
    if (blob) {
      emit('cropped', blob)
      emit('update:modelValue', false)
    }
  }, 'image/png')
}

watch(() => props.imageFile, (file) => {
  if (file && props.modelValue) initCanvas()
})

onUnmounted(() => {
  document.removeEventListener('pointermove', onPointerMove)
  document.removeEventListener('pointerup', onPointerUp)
})
</script>

<style scoped>
.crop-container {
  display: flex;
  justify-content: center;
  align-items: center;
}
canvas {
  border-radius: 8px;
  cursor: move;
  touch-action: none;
}
</style>
