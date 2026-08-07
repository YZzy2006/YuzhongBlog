<template>
  <div
    class="floating-player"
    :class="{ 'is-hidden': isHidden, 'is-night': isNight }"
    :style="containerStyle"
    @mousedown="onMouseDown"
  >
    <div class="fp-disc" :class="{ spinning: isPlaying }" @click.stop="goToMusic">
      <img v-if="activeCover" :src="activeCover" alt="cover" class="fp-cover" />
      <div v-else class="fp-cover-placeholder">♪</div>
      <div class="fp-disc-center"></div>
    </div>

    <div class="fp-middle" @click.stop="goToMusic">
      <div class="fp-info">
        <span class="fp-title">{{ activeTitle || '' }}</span>
        <span class="fp-lyric">{{ currentLyric || '' }}</span>
      </div>
      <div class="fp-progress" @click.stop @mousedown.stop>
        <input
          type="range" min="0" max="100"
          :value="musicProgress"
          @input="onProgressSeek"
          class="fp-progress-input"
          :style="progressStyle"
        />
      </div>
    </div>

    <div class="fp-controls">
      <button class="fp-btn fp-btn-prev" @click.stop="prev" @mousedown.stop>
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/></svg>
      </button>
      <button class="fp-btn fp-btn-play" @click.stop="togglePlay" @mousedown.stop>
        <svg v-if="!isPlaying" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
        <svg v-else viewBox="0 0 24 24" fill="currentColor"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
      </button>
      <button class="fp-btn fp-btn-next" @click.stop="next" @mousedown.stop>
        <svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMusicPlayer } from '../composables/useMusicPlayer'

const route = useRoute()
const router = useRouter()
const { currentSong, isPlaying, currentLyric, progress, togglePlay, next, prev, seek, initialize } = useMusicPlayer()

const activeCover = computed(() => currentSong.value?.coverUrl)
const activeTitle = computed(() => currentSong.value?.name)

const musicProgress = computed(() => progress.value || 0)
const progressStyle = computed(() => ({
  background: `linear-gradient(to right, #3b82f6 ${musicProgress.value}%, rgba(0,0,0,0.15) 0)`
}))

function onProgressSeek(e) {
  seek(Number(e.target.value))
}

const centeredRight = computed(() => {
  const el = document.querySelector('.floating-player')
  const w = el ? el.offsetWidth : 280
  return Math.max(0, (window.innerWidth - w) / 2)
})

function onPetChatToggle(e) {
  const open = e.detail?.open
  if (open) {
    savedPosX = posX.value
    centered.value = true
  } else {
    centered.value = false
    posX.value = savedPosX
  }
}

const isHidden = computed(() => route.path === '/')
const isNight = ref(document.body.classList.contains('body-night'))

// Drag state — position from right/bottom edge
const posX = ref(8)
const posY = ref(16)
const centered = ref(false)
let savedPosX = 8
const isDragging = ref(false)
let dragStartX = 0
let dragStartY = 0
let startPosX = 0
let startPosY = 0

const containerStyle = computed(() => ({
  right: (centered.value ? centeredRight.value : posX.value) + 'px',
  bottom: posY.value + 'px',
  pointerEvents: isHidden.value ? 'none' : 'auto',
  opacity: isHidden.value ? 0 : 1,
  scale: isHidden.value ? '0.8' : '1',
  transition: isDragging.value
    ? 'opacity 0.3s ease, scale 0.3s ease, box-shadow 0.3s ease'
    : 'opacity 0.3s ease, scale 0.3s ease, box-shadow 0.3s ease, right 0.4s cubic-bezier(0.22, 1, 0.36, 1)'
}))

function onMouseDown(e) {
  if (e.target.closest('.fp-btn') || centered.value) return
  isDragging.value = true
  dragStartX = e.clientX
  dragStartY = e.clientY
  startPosX = posX.value
  startPosY = posY.value
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

function onMouseMove(e) {
  if (!isDragging.value) return
  const dx = e.clientX - dragStartX
  const dy = e.clientY - dragStartY
  const el = document.querySelector('.floating-player')
  const w = el ? el.offsetWidth : 280
  const h = el ? el.offsetHeight : 64
  // right increases when dragging left, bottom increases when dragging up
  posX.value = Math.max(0, Math.min(window.innerWidth - w, startPosX - dx))
  posY.value = Math.max(0, Math.min(window.innerHeight - h, startPosY - dy))
}

function onMouseUp() {
  isDragging.value = false
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
}

function goToMusic() {
  if (isDragging.value) return
  router.push('/music')
}

// Observe night class changes
let observer = null
onMounted(() => {
  initialize()
  observer = new MutationObserver(() => {
    isNight.value = document.body.classList.contains('body-night')
  })
  observer.observe(document.body, { attributes: true, attributeFilter: ['class'] })
  // Clamp position on resize
  window.addEventListener('resize', clampPosition)
  window.addEventListener('pet-chat-toggle', onPetChatToggle)
})

onUnmounted(() => {
  observer?.disconnect()
  window.removeEventListener('resize', clampPosition)
  window.removeEventListener('pet-chat-toggle', onPetChatToggle)
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
})

function clampPosition() {
  const el = document.querySelector('.floating-player')
  const w = el ? el.offsetWidth : 280
  const h = el ? el.offsetHeight : 64
  posX.value = Math.max(0, Math.min(window.innerWidth - w, posX.value))
  posY.value = Math.max(0, Math.min(window.innerHeight - h, posY.value))
}
</script>

<style scoped>
.floating-player {
  position: fixed;
  z-index: 10001;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 18px 10px 10px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 999px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12), 0 0 0 1px rgba(255, 255, 255, 0.2);
  cursor: grab;
  transition: opacity 0.3s ease, scale 0.3s ease, box-shadow 0.3s ease;
  user-select: none;
  will-change: transform;
  overflow: hidden;
}

.floating-player:hover {
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(59, 130, 246, 0.15);
}

.floating-player:active {
  cursor: grabbing;
}

.floating-player.is-hidden {
  opacity: 0;
  pointer-events: none;
  scale: 0.8;
}

/* Night mode */
.floating-player.is-night {
  background: rgba(30, 41, 59, 0.88);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.35);
}

.floating-player.is-night:hover {
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(96, 165, 250, 0.2);
}

.fp-disc {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
  cursor: pointer;
  border: 3px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transition: box-shadow 0.3s ease;
}

.fp-disc.spinning {
  animation: fpSpin 6s linear infinite;
}

@keyframes fpSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.fp-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.fp-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: white;
  font-size: 18px;
}

.fp-disc-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 10px;
  height: 10px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 50%;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.1);
}

.fp-middle {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  flex: 1;
  cursor: pointer;
}

.fp-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.fp-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.is-night .fp-title {
  color: #e2e8f0;
}

.fp-lyric {
  font-size: 10px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fp-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.fp-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: none;
  cursor: pointer;
  padding: 0;
  transition: transform 0.15s ease;
}

.fp-btn:hover {
  transform: scale(1.1);
}

.fp-btn-play {
  width: 34px;
  height: 34px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border-radius: 50%;
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35);
}

.fp-btn-play svg {
  width: 14px;
  height: 14px;
}

.fp-btn-prev,
.fp-btn-next {
  width: 28px;
  height: 28px;
  color: #64748b;
}

.fp-btn-prev:hover,
.fp-btn-next:hover {
  color: #3b82f6;
}

.is-night .fp-btn-prev,
.is-night .fp-btn-next {
  color: #94a3b8;
}

.fp-btn-prev svg,
.fp-btn-next svg {
  width: 18px;
  height: 18px;
}

/* Progress bar */
.fp-progress {
  width: 100%;
  height: 14px;
  display: flex;
  align-items: center;
}

.fp-progress-input {
  width: 100%;
  height: 4px;
  border-radius: 2px;
  appearance: none;
  -webkit-appearance: none;
  outline: none;
  cursor: pointer;
  border: none;
  opacity: 0.7;
  transition: opacity 0.2s, height 0.2s;
}

.fp-progress:hover .fp-progress-input {
  opacity: 1;
  height: 6px;
}

.fp-progress-input::-webkit-slider-runnable-track {
  height: 4px;
  border-radius: 2px;
}

.fp-progress-input::-moz-range-track {
  height: 4px;
  border-radius: 2px;
  background: transparent;
}

.fp-progress-input::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  box-shadow: 0 0 4px rgba(59, 130, 246, 0.4);
  margin-top: -3px;
}

.fp-progress-input::-moz-range-thumb {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  border: none;
  box-shadow: 0 0 4px rgba(59, 130, 246, 0.4);
}

/* Mobile */
@media (max-width: 768px) {
  .floating-player {
    padding: 8px 12px 8px 8px;
    gap: 10px;
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
  }
  .fp-disc {
    width: 42px;
    height: 42px;
  }
  .fp-title {
    font-size: 12px;
    max-width: 120px;
  }
  .fp-artist {
    font-size: 10px;
  }
  .fp-controls {
    gap: 2px;
  }
  .fp-ctrl-btn {
    width: 28px;
    height: 28px;
  }
  .fp-ctrl-btn svg {
    width: 14px;
    height: 14px;
  }
  .fp-play-btn {
    width: 32px;
    height: 32px;
  }
  .fp-play-btn svg {
    width: 16px;
    height: 16px;
  }
  .fp-progress-wrap {
    display: none;
  }
}
</style>
