<template>
  <div class="lyric-bar" :class="{ 'is-night': isNight }" v-show="isPlaying">
    <div class="lb-equalizer">
      <div
        v-for="(bar, i) in eqBars"
        :key="i"
        class="lb-eq-bar"
        :class="{ active: isPlaying }"
        :style="{ animationDelay: bar.delay, background: bar.color }"
      ></div>
    </div>

    <div class="lb-lyric">
      <span class="lb-text">{{ displayedLyric }}</span>
      <span class="lb-cursor"></span>
    </div>

    <router-link to="/music" class="lb-music-icon">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M9 18V5l12-2v13"/>
        <circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/>
      </svg>
    </router-link>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useMusicPlayer } from '../composables/useMusicPlayer'

const { isPlaying, currentLyric } = useMusicPlayer()

const displayedLyric = ref('')
const isNight = ref(document.body.classList.contains('body-night'))

const eqBars = [
  { color: '#60a5fa', delay: '0ms' },
  { color: '#60a5fa', delay: '200ms' },
  { color: '#3b82f6', delay: '400ms' },
  { color: '#93c5fd', delay: '100ms' },
  { color: '#93c5fd', delay: '300ms' }
]

let typingTimer = null

function startTyping(text) {
  if (typingTimer) clearInterval(typingTimer)
  displayedLyric.value = ''
  if (!text) return
  let i = 0
  typingTimer = setInterval(() => {
    if (i <= text.length) {
      displayedLyric.value = text.slice(0, i)
      i++
    } else {
      clearInterval(typingTimer)
    }
  }, 50)
}

watch(currentLyric, (val) => {
  startTyping(val)
})

let observer = null
onMounted(() => {
  if (currentLyric.value) startTyping(currentLyric.value)
  observer = new MutationObserver(() => {
    isNight.value = document.body.classList.contains('body-night')
  })
  observer.observe(document.body, { attributes: true, attributeFilter: ['class'] })
})

onUnmounted(() => {
  if (typingTimer) clearInterval(typingTimer)
  observer?.disconnect()
})
</script>

<style scoped>
.lyric-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.1);
  transition: opacity 0.3s ease;
}

.lyric-bar.is-night {
  background: rgba(10, 15, 30, 0.92);
}

/* Equalizer */
.lb-equalizer {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 28px;
  width: 48px;
  flex-shrink: 0;
}

.lb-eq-bar {
  width: 5px;
  border-radius: 2px 2px 0 0;
  height: 4px;
  transition: height 0.3s ease;
}

.lb-eq-bar.active {
  animation: eqWave 1s ease-in-out infinite;
}

@keyframes eqWave {
  0%, 100% { height: 6px; }
  50% { height: 24px; }
}

/* Lyric */
.lb-lyric {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 0 16px;
}

.lb-text {
  color: #e2e8f0;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-shadow: 0 0 12px rgba(59, 130, 246, 0.5);
}

.lb-cursor {
  display: inline-block;
  width: 2px;
  height: 16px;
  background: #60a5fa;
  margin-left: 2px;
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.6);
  animation: cursorBlink 0.8s step-end infinite;
}

@keyframes cursorBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* Music icon */
.lb-music-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(96, 165, 250, 0.5);
  text-decoration: none;
  flex-shrink: 0;
  transition: color 0.2s;
}

.lb-music-icon:hover {
  color: #60a5fa;
}

.lb-music-icon svg {
  width: 20px;
  height: 20px;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}
</style>
