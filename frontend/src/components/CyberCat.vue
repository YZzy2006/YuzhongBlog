<template>
  <div class="cyber-cat-wrapper" :class="{ 'is-dragging': isDragging }" :style="dragStyle" @mousedown="onDragStart">
    <!-- Speech Bubble + Route Chips -->
    <div class="bubble-area">
      <transition name="bubble">
        <div v-if="speech || isStreaming" class="bubble-inner">
          <div class="speech-bubble" ref="speechBubbleRef">
            <span v-if="isStreaming && !streamContent" class="pet-typing">
              <span></span><span></span><span></span>
            </span>
            <template v-else>{{ speech }}</template>
          </div>
          <div class="bubble-tail"></div>
        </div>
      </transition>
      <transition-group name="chip" tag="div" class="route-chips">
        <button
          v-for="route in detectedRoutes"
          :key="route.path"
          class="route-chip"
          @click.stop="navigateRoute(route.path)"
        >
          {{ route.name }}
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="chip-arrow">
            <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
          </svg>
        </button>
      </transition-group>
    </div>

    <!-- Action Buttons -->
    <div class="action-buttons">
      <button class="action-btn" @click.stop="toggleInput" :title="$t('pet.chat')">
        <svg viewBox="0 0 24 24" fill="currentColor" class="icon">
          <path fill-rule="evenodd" d="M4.804 21.644A6.707 6.707 0 006 21.75a6.721 6.721 0 003.583-1.029c.774.182 1.584.279 2.417.279 5.322 0 9.75-3.97 9.75-9 0-5.03-4.428-9-9.75-9s-9.75 3.97-9.75 9c0 2.409 1.025 4.587 2.674 6.192.232.226.277.428.254.543a3.73 3.73 0 01-.814 1.686.75.75 0 00.44 1.223zM8.25 10.875a1.125 1.125 0 100 2.25 1.125 1.125 0 000-2.25zM10.875 12a1.125 1.125 0 112.25 0 1.125 1.125 0 01-2.25 0zm4.875-1.125a1.125 1.125 0 100 2.25 1.125 1.125 0 000-2.25z" clip-rule="evenodd" />
        </svg>
      </button>
      <button class="action-btn" @click.stop="handleFeed" :disabled="isThinking" :title="$t('pet.feed')">
        <span class="text-xl leading-none">🐟</span>
      </button>
      <button class="action-btn" @click.stop="showPicker = true" :title="$t('pet.switch')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="icon">
          <path d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      </button>
      <button v-if="chatMessages.length > 0" class="action-btn" @click.stop="clearConversation" :title="$t('pet.clearChat')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="icon">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- Cat Sprite Container -->
    <div class="sprite-container" @click="handlePetCat" ref="spriteRef">
      <canvas
        ref="canvasRef"
        :key="currentPetId"
        class="sprite-canvas"
      ></canvas>
    </div>

    <!-- Chat Input -->
    <transition name="input-slide">
      <form v-if="showInput" class="chat-input-form" @submit.prevent="handleChatSubmit">
        <input
          ref="chatInputRef"
          v-model="inputValue"
          type="text"
          :placeholder="$t('pet.placeholder')"
          :disabled="isThinking"
          class="chat-input"
        />
        <button
          type="submit"
          :disabled="isThinking || !inputValue.trim()"
          class="send-btn"
        >
          <svg viewBox="0 0 20 20" fill="currentColor" class="w-4 h-4">
            <path d="M3.105 2.289a.75.75 0 00-.826.95l1.414 4.925A1.5 1.5 0 005.135 9.25h6.115a.75.75 0 010 1.5H5.135a1.5 1.5 0 00-1.442 1.086l-1.414 4.926a.75.75 0 00.826.95 28.896 28.896 0 0015.293-7.154.75.75 0 000-1.115A28.897 28.897 0 003.105 2.289z" />
          </svg>
        </button>
      </form>
    </transition>

    <!-- Pet Picker Dialog -->
    <teleport to="body">
      <transition name="fade">
        <div v-if="showPicker" class="picker-overlay" @click.self="showPicker = false">
          <div class="picker-dialog">
            <div class="picker-header">
              <h3>{{ $t('pet.pickTitle') }}</h3>
              <span class="picker-count">{{ $t('pet.totalCount', { count: petList.length }) }}</span>
              <button class="picker-close" @click="showPicker = false">&times;</button>
            </div>

            <!-- Preview Area -->
            <div class="preview-area">
              <div class="preview-sprite-container">
                <canvas ref="previewCanvasRef" class="preview-canvas" width="64" height="64"></canvas>
              </div>
              <div class="preview-info">
                <div class="preview-name">{{ previewPet?.name || '' }}</div>
                <div class="preview-id">{{ previewPet?.id || '' }}</div>
              </div>
              <button
                v-if="previewPet && previewPet.id !== currentPetId"
                class="apply-btn"
                @click="applyPet(previewPet)"
              >
                {{ $t('pet.apply') }}
              </button>
              <span v-else-if="previewPet && previewPet.id === currentPetId" class="current-badge">
                {{ $t('pet.current') }}
              </span>
            </div>

            <!-- Pet Grid -->
            <div class="picker-grid">
              <div
                v-for="pet in pagePets"
                :key="pet.id"
                class="picker-item"
                :class="{ active: pet.id === currentPetId, previewing: previewPet?.id === pet.id }"
                @mouseenter="onPickerHover(pet)"
                @click="applyPet(pet)"
              >
                <div class="picker-thumb-wrapper">
                  <img :src="pet.thumb" :alt="pet.name" class="picker-thumb-img" />
                </div>
                <span class="picker-name">{{ pet.name }}</span>
              </div>
            </div>
            <!-- Pagination -->
            <div class="picker-pagination">
              <button class="page-btn" :disabled="currentPage === 0" @click="currentPage--">&lsaquo;</button>
              <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
              <button class="page-btn" :disabled="currentPage >= totalPages - 1" @click="currentPage++">&rsaquo;</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup>
// ==================== Page-level Preload ====================
// 在 Vue 挂载前注入 preload，让浏览器立即开始下载当前宠物精灵图
;(() => {
  try {
    const saved = localStorage.getItem('cyber-pet-id')
    const petId = saved || 'nightleaf'
    const pet = PET_LIST.find(p => p.id === petId) || PET_LIST[0]
    if (pet) {
      const link = document.createElement('link')
      link.rel = 'preload'
      link.as = 'fetch'
      link.crossOrigin = ''
      link.href = pet.sprite
      document.head.appendChild(link)
    }
  } catch {}
})()

import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { PET_LIST, DEFAULT_PET_ID } from '../config/pets.js'
import { petChatStream } from '../utils/ai.js'
import { checkContent } from '../utils/contentFilter.js'
import request from '../utils/request.js'

// ==================== Sprite Cache (LRU) ====================
const MAX_CACHED_SPRITES = 10
const spriteCache = new Map() // LRU: 最旧的在前，最近访问的在后
const inflight = new Map()

function lruTouch(src) {
  if (!spriteCache.has(src)) return
  const val = spriteCache.get(src)
  spriteCache.delete(src)
  spriteCache.set(src, val)
}

function lruSet(src, val) {
  if (spriteCache.has(src)) spriteCache.delete(src)
  spriteCache.set(src, val)
  // 驱逐最旧的条目
  while (spriteCache.size > MAX_CACHED_SPRITES) {
    const oldest = spriteCache.keys().next().value
    spriteCache.delete(oldest)
  }
}

function loadImage(src) {
  if (spriteCache.has(src)) { lruTouch(src); return Promise.resolve(spriteCache.get(src)) }
  if (inflight.has(src)) return inflight.get(src)
  const p = fetch(src)
    .then(res => { if (!res.ok) throw new Error(`HTTP ${res.status}`); return res.blob() })
    .then(blob => createImageBitmap(blob))
    .then(bitmap => { lruSet(src, bitmap); inflight.delete(src); return bitmap })
    .catch(err => { inflight.delete(src); throw err })
  inflight.set(src, p)
  return p
}

const { t } = useI18n()
const router = useRouter()

// ==================== State ====================
const isPetted = ref(false)
const speech = ref(null)
const showInput = ref(false)
const inputValue = ref('')
const isThinking = ref(false)
const chatMessages = ref([])
const isStreaming = ref(false)
const streamContent = ref('')
const detectedRoutes = ref([])

watch(showInput, (open) => {
  window.dispatchEvent(new CustomEvent('pet-chat-toggle', { detail: { open } }))
})
let petAbortFn = null
let chatIdleTimeout = null
const CHAT_IDLE_MS = 5 * 60 * 1000 // 5 minutes
const showPicker = ref(false)
const previewPet = ref(null)
const saved = localStorage.getItem('cyber-pet-id')
const currentPetId = ref(saved && PET_LIST.some(p => p.id === saved) ? saved : DEFAULT_PET_ID)

let speechTimeout = null
let idleInterval = null

// ==================== Refs ====================
const chatInputRef = ref(null)
const spriteRef = ref(null)
const speechBubbleRef = ref(null)
const canvasRef = ref(null)
const previewCanvasRef = ref(null)
const isVisible = ref(true)

// ==================== Drag ====================
const dragOffset = ref({ x: 0, y: 0 })
const isDragging = ref(false)
let dragStartX = 0
let dragStartY = 0
let dragStartOffsetX = 0
let dragStartOffsetY = 0
let hasMoved = false

const dragStyle = computed(() => {
  if (dragOffset.value.x === 0 && dragOffset.value.y === 0) return {}
  return { transform: `translate(${dragOffset.value.x}px, ${dragOffset.value.y}px)` }
})

function onDragStart(e) {
  // Don't drag on interactive elements
  const tag = e.target.tagName
  if (tag === 'BUTTON' || tag === 'INPUT' || tag === 'SVG' || tag === 'PATH' ||
      e.target.closest('button') || e.target.closest('input') || e.target.closest('.picker-overlay') ||
      e.target.closest('.chat-input-form')) return

  isDragging.value = true
  hasMoved = false
  dragStartX = e.clientX
  dragStartY = e.clientY
  dragStartOffsetX = dragOffset.value.x
  dragStartOffsetY = dragOffset.value.y
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  e.preventDefault()
}

function onDragMove(e) {
  if (!isDragging.value) return
  const dx = e.clientX - dragStartX
  const dy = e.clientY - dragStartY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasMoved = true
  dragOffset.value = {
    x: dragStartOffsetX + dx,
    y: dragStartOffsetY + dy
  }
}

function onDragEnd() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
}

onUnmounted(() => {
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
})

// ==================== Pet Data ====================
const petList = PET_LIST
const PAGE_SIZE = 40
const totalPages = computed(() => Math.ceil(petList.length / PAGE_SIZE))
const currentPage = ref(0)
const pagePets = computed(() => {
  const start = currentPage.value * PAGE_SIZE
  return petList.slice(start, start + PAGE_SIZE)
})

const currentPet = computed(() => {
  return petList.find(p => p.id === currentPetId.value) || petList[0]
})

// ==================== Sprite Frame Animation ====================
let animInterval = null
let idleTimeout = null
let animId = 0
let updateVersion = 0

function drawCell(canvas, img, cols, rows, row, col) {
  const ctx = canvas.getContext('2d')
  const cellW = img.width / cols
  const cellH = img.height / rows
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.drawImage(img, col * cellW, row * cellH, cellW, cellH, 0, 0, canvas.width, canvas.height)
}

function startSpriteAnimation(canvas, img, cols, rows) {
  const myId = ++animId
  stopAnim()

  const idleFrames = 3
  const actionRows = []
  for (let r = 1; r < rows; r++) actionRows.push(r)

  let currentRow = 0
  let currentFrame = 0
  let isIdle = true

  function tick() { drawCell(canvas, img, cols, rows, currentRow, currentFrame) }

  function advance() {
    if (animId !== myId) return
    currentFrame++
    if (isIdle) {
      if (currentFrame >= idleFrames) currentFrame = 0
    } else {
      if (currentFrame >= 6) {
        isIdle = true
        currentRow = 0
        currentFrame = 0
        scheduleNextIdle()
      }
    }
    tick()
  }

  function scheduleNextIdle() {
    if (animId !== myId) return
    idleTimeout = setTimeout(() => {
      if (animId !== myId || actionRows.length === 0) return
      isIdle = false
      currentRow = actionRows[Math.floor(Math.random() * actionRows.length)]
      currentFrame = 0
      tick()
    }, 5000 + Math.random() * 3000)
  }

  tick()
  animInterval = setInterval(advance, 600)
  scheduleNextIdle()
}

function updateSprite() {
  const pet = currentPet.value
  const cols = pet.cols || 8
  const rows = pet.rows || 9
  const canvas = canvasRef.value
  const container = spriteRef.value
  if (!canvas || !container) return

  const displayW = container.clientWidth || 120
  const myVer = ++updateVersion

  // 有缓存时立即显示首帧动画
  if (spriteCache.has(pet.sprite)) {
    const img = spriteCache.get(pet.sprite)
    const cellW = img.width / cols
    const cellH = img.height / rows
    const scale = displayW / cellW
    const displayH = Math.round(cellH * scale)
    canvas.width = displayW
    canvas.height = displayH
    canvas.style.width = displayW + 'px'
    canvas.style.height = displayH + 'px'
    startSpriteAnimation(canvas, img, cols, rows)
    return
  }

  // 无缓存时：先用缩略图占位，再加载完整精灵图
  const thumbImg = new Image()
  thumbImg.onload = () => {
    // 精灵图已加载完成则跳过缩略图绘制，避免覆盖动画
    if (updateVersion !== myVer) return
    canvas.width = displayW
    canvas.height = displayW
    canvas.style.width = displayW + 'px'
    canvas.style.height = displayW + 'px'
    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(thumbImg, 0, 0, canvas.width, canvas.height)
  }
  thumbImg.src = pet.thumb

  // 旧动画保留播放，新精灵图加载完成后再切换
  loadImage(pet.sprite).then(img => {
    if (updateVersion !== myVer) return
    const cellW = img.width / cols
    const cellH = img.height / rows
    const scale = displayW / cellW
    const displayH = Math.round(cellH * scale)
    canvas.width = displayW
    canvas.height = displayH
    canvas.style.width = displayW + 'px'
    canvas.style.height = displayH + 'px'
    startSpriteAnimation(canvas, img, cols, rows)
  }).catch(() => {})
}

function stopAnim() {
  if (animInterval) { clearInterval(animInterval); animInterval = null }
  if (idleTimeout) { clearTimeout(idleTimeout); idleTimeout = null }
}

// ==================== Speech ====================
function speak(text, duration = 6000) {
  speech.value = text
  if (speechTimeout) clearTimeout(speechTimeout)
  if (duration > 0) {
    speechTimeout = setTimeout(() => {
      if (!isStreaming.value) speech.value = null
    }, duration)
  }
}

// ==================== Interactions ====================
function handlePetCat() {
  if (hasMoved) return
  if (isPetted.value) return
  isPetted.value = true
  speak(t('pet.petResponse'), 2000)
  setTimeout(() => { isPetted.value = false }, 2000)
}

function toggleInput() {
  showInput.value = !showInput.value
  if (showInput.value) {
    nextTick(() => chatInputRef.value?.focus())
  }
}

function clearConversation() {
  chatMessages.value = []
  streamContent.value = ''
  detectedRoutes.value = []
  if (petAbortFn) { petAbortFn(); petAbortFn = null }
  if (chatIdleTimeout) { clearTimeout(chatIdleTimeout); chatIdleTimeout = null }
  speech.value = null
  inputValue.value = ''
  showInput.value = false
  isStreaming.value = false
  isThinking.value = false
  if (speechTimeout) clearTimeout(speechTimeout)
}

async function handleFeed() {
  if (isThinking.value) return
  showInput.value = false
  isThinking.value = true
  speak(t('pet.feedResponse'), 6000)

  const feedMsg = '我刚刚喂了你好吃的！你有什么表示？'
  chatMessages.value.push({ role: 'user', content: feedMsg })
  resetChatIdleTimer()

  try {
    const data = await request.post('/api/ai/pet-chat', {
      message: feedMsg,
      messages: chatMessages.value.map(m => ({ role: m.role, content: m.content }))
    })
    if (data?.response) {
      speak(data.response, 8000)
      chatMessages.value.push({ role: 'assistant', content: data.response })
      trimChatMessages()
      detectedRoutes.value = parseRoutes(data.response)
    } else {
      chatMessages.value.pop()
      speak(t('pet.feedError'), 4000)
    }
  } catch {
    chatMessages.value.pop()
    speak(t('pet.feedError'), 4000)
  } finally {
    isThinking.value = false
  }
}

const MAX_CHAT_MESSAGES = 10

const ROUTE_MAP = {
  '首页': '/', 'home': '/',
  '文章': '/articles', 'articles': '/articles',
  '项目': '/projects', 'projects': '/projects',
  '关于': '/about', 'about': '/about',
  '归档': '/archive', 'archive': '/archive',
  '动态': '/moments', 'moments': '/moments',
  '公告': '/announcements', 'announcements': '/announcements',
  '搜索': '/search', 'search': '/search',
  '游戏': '/games', 'games': '/games',
  '照片墙': '/photowall', 'photowall': '/photowall',
  '音乐': '/music', 'music': '/music',
  '宇宙': '/cosmos', 'cosmos': '/cosmos',
}

function parseRoutes(text) {
  if (!text) return []
  const routes = []
  const seen = new Set()
  // Match 「pageName」 pattern
  const bracketRegex = /「([^」]+)」/g
  let match
  while ((match = bracketRegex.exec(text)) !== null) {
    const name = match[1]
    const path = ROUTE_MAP[name]
    if (path && !seen.has(path)) {
      seen.add(path)
      routes.push({ name, path })
    }
  }
  // Fallback: match raw paths like /articles, /search
  const pathRegex = /\/(articles|projects|about|archive|moments|announcements|search|games|photowall|music|cosmos)(?=[\s，。！？、）).,;:]|$)/g
  while ((match = pathRegex.exec(text)) !== null) {
    const slug = match[1]
    const path = `/${slug}`
    if (!seen.has(path)) {
      // Find Chinese name from ROUTE_MAP
      const name = Object.keys(ROUTE_MAP).find(k => ROUTE_MAP[k] === path && /[一-龥]/.test(k)) || slug
      seen.add(path)
      routes.push({ name, path })
    }
  }
  return routes
}

function navigateRoute(path) {
  if (router.currentRoute.value.path === path) return
  router.push(path)
}

function resetChatIdleTimer() {
  if (chatIdleTimeout) clearTimeout(chatIdleTimeout)
  chatIdleTimeout = setTimeout(() => {
    clearConversation()
  }, CHAT_IDLE_MS)
}

function trimChatMessages() {
  if (chatMessages.value.length <= MAX_CHAT_MESSAGES) return
  chatMessages.value = chatMessages.value.slice(-MAX_CHAT_MESSAGES)
  // Ensure array starts with a user message (preserve pairing)
  while (chatMessages.value.length > 0 && chatMessages.value[0]?.role === 'assistant') {
    chatMessages.value.shift()
  }
}

function handleChatSubmit() {
  const msg = inputValue.value.trim()
  if (!msg || isThinking.value) return

  // 前端即时过滤（用户端）
  const check = checkContent(msg)
  if (check.blocked) {
    speak(check.message)
    return
  }

  inputValue.value = ''
  showInput.value = false

  // Abort previous stream if still running
  if (petAbortFn) { petAbortFn(); petAbortFn = null }

  // Add user message to history
  chatMessages.value.push({ role: 'user', content: msg })
  trimChatMessages()
  resetChatIdleTimer()

  isThinking.value = true
  isStreaming.value = true
  streamContent.value = ''
  speech.value = t('pet.thinking')
  if (speechTimeout) clearTimeout(speechTimeout)

  petAbortFn = petChatStream(chatMessages.value.map(m => ({ role: m.role, content: m.content })), {
    onChunk(content) {
      streamContent.value += content
      speech.value = streamContent.value
      nextTick(() => {
        if (speechBubbleRef.value) speechBubbleRef.value.scrollTop = speechBubbleRef.value.scrollHeight
      })
    },
    onDone() {
      if (streamContent.value) {
        chatMessages.value.push({ role: 'assistant', content: streamContent.value })
        trimChatMessages()
        detectedRoutes.value = parseRoutes(streamContent.value)
        isStreaming.value = false
        isThinking.value = false
        petAbortFn = null
        speechTimeout = setTimeout(() => {
          speech.value = null
          detectedRoutes.value = []
        }, 10000)
      } else {
        // Empty response — pop orphaned user message, show error
        chatMessages.value.pop()
        detectedRoutes.value = []
        isStreaming.value = false
        isThinking.value = false
        petAbortFn = null
        speak(t('pet.chatError'), 4000)
      }
    },
    onError() {
      isStreaming.value = false
      isThinking.value = false
      petAbortFn = null
      detectedRoutes.value = []
      if (streamContent.value) {
        chatMessages.value.push({ role: 'assistant', content: streamContent.value })
        streamContent.value = ''
        trimChatMessages()
      } else {
        // No partial content — pop orphaned user message
        chatMessages.value.pop()
      }
      speak(t('pet.chatError'), 4000)
    }
  })
}

// ==================== Preview Canvas ====================
let lastPreviewId = null
let previewVersion = 0

function updatePreview() {
  const pet = previewPet.value || currentPet.value
  const canvas = previewCanvasRef.value
  if (!canvas || !pet) return
  if (pet.id === lastPreviewId) return
  lastPreviewId = pet.id
  const ver = ++previewVersion
  const cols = pet.cols || 8, rows = pet.rows || 9
  loadImage(pet.sprite).then(img => {
    if (previewVersion !== ver) return
    const ctx = canvas.getContext('2d')
    const cellW = img.width / cols
    const cellH = img.height / rows
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(img, 0, 0, cellW, cellH, 0, 0, canvas.width, canvas.height)
  })
}

// ==================== Background Prefetch ====================
let prefetchAbort = false

function prefetchSprites() {
  prefetchAbort = false
  // 只预加载当前宠物 + 相邻宠物（±2），控制首屏流量（每个精灵图 1-5MB）
  const idx = petList.findIndex(p => p.id === currentPetId.value)
  const start = Math.max(0, idx - 2)
  const end = Math.min(petList.length, idx + 3)
  const targets = petList.slice(start, end)
  let i = 0
  function next() {
    if (prefetchAbort || i >= targets.length) return
    const pet = targets[i++]
    loadImage(pet.sprite).then(next).catch(next)
  }
  next(); next(); next()
}

// ==================== Pet Switcher ====================
function onPickerHover(pet) {
  previewPet.value = pet
  loadImage(pet.sprite) // hover 时立即开始加载精灵图
}

function applyPet(pet) {
  currentPetId.value = pet.id
  localStorage.setItem('cyber-pet-id', pet.id)
  speak(t('pet.switched', { name: pet.name }), 3000)
  showPicker.value = false
}

// ==================== Random Idle Talk ====================
const randomBarks = [
  'pet.bark1', 'pet.bark2', 'pet.bark3', 'pet.bark4', 'pet.bark5',
  'pet.bark6', 'pet.bark7', 'pet.bark8', 'pet.bark9', 'pet.bark10',
  'pet.bark11', 'pet.bark12', 'pet.bark13', 'pet.bark14', 'pet.bark15'
]

function startIdleTalk() {
  idleInterval = setInterval(() => {
    if (!speech.value && !showInput.value && !isThinking.value && Math.random() > 0.8) {
      const key = randomBarks[Math.floor(Math.random() * randomBarks.length)]
      speak(t(key), 4000)
    }
  }, 20000)
}

// ==================== Lifecycle ====================
let intersectionObserver = null
let onVisibilityChange = null

onMounted(async () => {
  previewPet.value = currentPet.value
  startIdleTalk()
  await nextTick()
  updateSprite()

  // Pause animation when off-screen
  intersectionObserver = new IntersectionObserver(([entry]) => {
    isVisible.value = entry.isIntersecting
    if (entry.isIntersecting) {
      if (!animInterval) updateSprite()
      if (!idleInterval) startIdleTalk()
    } else {
      stopAnim()
      if (idleInterval) { clearInterval(idleInterval); idleInterval = null }
    }
  }, { threshold: 0 })
  if (spriteRef.value) intersectionObserver.observe(spriteRef.value)

  // Pause animation when tab is hidden
  onVisibilityChange = () => {
    if (document.hidden) {
      stopAnim()
      if (idleInterval) { clearInterval(idleInterval); idleInterval = null }
    } else if (isVisible.value) {
      updateSprite()
      if (!idleInterval) startIdleTalk()
    }
  }
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  if (intersectionObserver) intersectionObserver.disconnect()
  if (onVisibilityChange) document.removeEventListener('visibilitychange', onVisibilityChange)
  if (speechTimeout) clearTimeout(speechTimeout)
  if (chatIdleTimeout) clearTimeout(chatIdleTimeout)
  if (idleInterval) clearInterval(idleInterval)
  if (petAbortFn) petAbortFn()
  stopAnim()
  prefetchAbort = true
})

watch(showPicker, (val) => {
  if (val) {
    previewPet.value = currentPet.value
    lastPreviewId = null
    currentPage.value = 0
    nextTick(updatePreview)
    prefetchSprites()
  } else {
    prefetchAbort = true
  }
})

watch(previewPet, () => {
  if (showPicker.value) updatePreview()
})

watch(currentPetId, async () => {
  await nextTick()
  updateSprite()
})
</script>

<!-- Non-scoped: picker dialog (teleported to body) -->
<style>
/* ==================== Canvas Sprite ==================== */
.sprite-canvas {
  image-rendering: pixelated;
  pointer-events: none;
}

/* ---- Preview canvas (picker) ---- */
.preview-sprite-container {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
}

.preview-canvas {
  display: block;
  width: 64px;
  height: 64px;
  image-rendering: pixelated;
}

.picker-thumb-wrapper {
  width: 48px;
  height: 48px;
  overflow: hidden;
  border-radius: 8px;
}

.picker-thumb-img {
  display: block;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  image-rendering: auto;
}

/* Picker Dialog (teleported to body) */
.picker-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  backdrop-filter: blur(4px);
}

.picker-dialog {
  background: white;
  border-radius: 20px;
  width: 90vw;
  max-width: 680px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  overflow: hidden;
}

:root.dark .picker-dialog {
  background: #0f172a;
  border: 1px solid #1e293b;
}

.picker-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  gap: 12px;
}

:root.dark .picker-header { border-color: #1e293b; }

.picker-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #1e293b;
}

:root.dark .picker-header h3 { color: #f1f5f9; }

.picker-count {
  font-size: 0.8rem;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 8px;
}

:root.dark .picker-count { background: #1e293b; color: #94a3b8; }

.picker-close {
  margin-left: auto;
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #64748b;
  line-height: 1;
  padding: 0 4px;
}

.picker-close:hover { color: #1e293b; }
:root.dark .picker-close:hover { color: #f1f5f9; }

/* Preview Area */
.preview-area {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

:root.dark .preview-area {
  background: #1e293b;
  border-color: #334155;
}


.preview-info {
  flex: 1;
  min-width: 0;
}

.preview-name {
  font-size: 0.85rem;
  font-weight: 600;
  color: #1e293b;
}

:root.dark .preview-name { color: #f1f5f9; }

.preview-id {
  font-size: 0.7rem;
  color: #94a3b8;
  margin-top: 1px;
}

.apply-btn {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 10px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.apply-btn:hover { background: #2563eb; transform: scale(1.02); }

.current-badge {
  font-size: 0.75rem;
  color: #10b981;
  background: #ecfdf5;
  padding: 4px 12px;
  border-radius: 8px;
  font-weight: 600;
  white-space: nowrap;
}

:root.dark .current-badge {
  background: #064e3b;
  color: #34d399;
}

/* Pet Grid */
.picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 8px;
  padding: 16px;
  overflow-y: auto;
  max-height: 45vh;
}

.picker-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.picker-item:hover { background: #f1f5f9; }
:root.dark .picker-item:hover { background: #1e293b; }

.picker-item.active {
  border-color: #3b82f6;
  background: #eff6ff;
}

:root.dark .picker-item.active {
  background: #1e3a5f;
  border-color: #60a5fa;
}

.picker-item.previewing:not(.active) {
  border-color: #93c5fd;
  background: #f0f9ff;
}

:root.dark .picker-item.previewing:not(.active) {
  border-color: #3b82f6;
  background: #172554;
}


.picker-name {
  font-size: 0.65rem;
  color: #64748b;
  text-align: center;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:root.dark .picker-name { color: #94a3b8; }

/* Pagination */
.picker-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 10px 16px;
  border-top: 1px solid #e2e8f0;
}

:root.dark .picker-pagination { border-color: #1e293b; }

.page-btn {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 4px 12px;
  font-size: 1.1rem;
  cursor: pointer;
  color: #334155;
  transition: all 0.15s;
  line-height: 1;
}

.page-btn:hover:not(:disabled) { background: #e2e8f0; }
.page-btn:disabled { opacity: 0.35; cursor: not-allowed; }

:root.dark .page-btn {
  background: #1e293b;
  border-color: #334155;
  color: #e2e8f0;
}

:root.dark .page-btn:hover:not(:disabled) { background: #334155; }

.page-info {
  font-size: 0.8rem;
  color: #64748b;
  min-width: 50px;
  text-align: center;
}

:root.dark .page-info { color: #94a3b8; }

/* Chat Input - Dark Mode (must be here: :root.dark doesn't work in scoped CSS) */
.cyber-cat-wrapper .chat-input-form {
  background: white;
  border: 1px solid #e2e8f0;
}

:root.dark .chat-input-form {
  background: #1e293b;
  border-color: #334155;
}

:root.dark .chat-input {
  color: #f1f5f9;
}

:root.dark .chat-input::placeholder {
  color: #64748b;
}

/* Action Buttons - Dark Mode */
:root.dark .action-btn {
  background: rgba(30,41,59,0.9);
  border-color: #334155;
}

/* Speech Bubble - Dark Mode */
:root.dark .speech-bubble {
  background: #1e293b;
  color: #e2e8f0;
  border-color: #334155;
}

:root.dark .bubble-tail {
  background: #1e293b;
  border-color: #334155;
}

/* Route Chips - Dark Mode */
:root.dark .route-chip {
  background: linear-gradient(135deg, #60a5fa, #93c5fd);
  color: #172554;
  box-shadow: 0 2px 8px rgba(96, 165, 250, 0.3);
}

:root.dark .route-chip:hover {
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: white;
}

/* Transitions */
.fade-enter-active { transition: all 0.3s ease; }
.fade-leave-active { transition: all 0.2s ease; }
.fade-enter-from { opacity: 0; }
.fade-leave-to { opacity: 0; }
.bubble-enter-active { transition: all 0.3s ease; }
.bubble-leave-active { transition: all 0.2s ease; }
.bubble-enter-from { opacity: 0; transform: translateY(10px) scale(0.9); }
.bubble-leave-to { opacity: 0; transform: scale(0.9); }
.input-slide-enter-active { transition: all 0.3s ease; }
.input-slide-leave-active { transition: all 0.2s ease; }
.input-slide-enter-from { opacity: 0; transform: translateY(-10px) scale(0.9); }
.input-slide-leave-to { opacity: 0; transform: translateY(-10px) scale(0.9); }
</style>

<!-- Scoped: component-specific layout -->
<style scoped>
.cyber-cat-wrapper {
  position: fixed;
  bottom: 7rem;
  right: 5rem;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: grab;
  user-select: none;
  will-change: transform;
}
.cyber-cat-wrapper.is-dragging {
  cursor: grabbing;
  transition: none !important;
}

/* Bubble Area - groups bubble + chips + tail */
.bubble-area {
  position: absolute;
  bottom: calc(100% + 12px);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  pointer-events: none;
}

.bubble-inner {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.speech-bubble {
  background: white;
  color: #334155;
  padding: 10px 16px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.12);
  border: 1px solid #e2e8f0;
  font-size: 0.85rem;
  min-width: 120px;
  max-width: 220px;
  max-height: 200px;
  overflow-y: auto;
  text-align: center;
  line-height: 1.5;
  pointer-events: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.pet-typing {
  display: inline-flex;
  gap: 4px;
  padding: 2px 0;
}

.pet-typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  animation: pet-typing-bounce 1.2s infinite;
}

.pet-typing span:nth-child(2) { animation-delay: 0.2s; }
.pet-typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes pet-typing-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

.bubble-tail {
  position: absolute;
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%) rotate(45deg);
  width: 12px;
  height: 12px;
  background: white;
  border-right: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
}

/* Route Chips */
.route-chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
  pointer-events: auto;
}

.route-chips:empty {
  display: none;
}

.route-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: white;
  font-size: 0.75rem;
  font-weight: 500;
  border: none;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.route-chip:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
  background: linear-gradient(135deg, #2563eb, #3b82f6);
}

.chip-arrow {
  width: 12px;
  height: 12px;
}

.chip-enter-active { transition: all 0.3s ease; }
.chip-leave-active { transition: all 0.2s ease; }
.chip-enter-from { opacity: 0; transform: translateY(8px) scale(0.9); }
.chip-leave-to { opacity: 0; transform: translateY(-4px) scale(0.9); }

/* Action Buttons */
.action-buttons {
  position: absolute;
  left: -3rem;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 20;
}

.action-btn {
  background: rgba(255,255,255,0.9);
  border: 1px solid #e2e8f0;
  border-radius: 50%;
  padding: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  backdrop-filter: blur(8px);
}

.action-btn:hover { transform: scale(1.1); }
.action-btn:active { transform: scale(0.95); }
.action-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.action-btn .icon { width: 20px; height: 20px; color: #3b82f6; }

/* Sprite Container */
.sprite-container {
  width: 120px;
  min-height: 120px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
}
/* Chat Input */
.chat-input-form {
  position: absolute;
  bottom: -3.5rem;
  display: flex;
  align-items: center;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 9999px;
  padding: 4px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  width: 14rem;
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 6px 12px;
  font-size: 0.85rem;
  color: inherit;
}

.chat-input::placeholder { color: #94a3b8; }

.send-btn {
  border-radius: 50%;
  padding: 6px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  background: #3b82f6;
  color: white;
}

.send-btn:disabled { background: #cbd5e1; color: #64748b; cursor: not-allowed; }
.send-btn:not(:disabled):hover { background: #2563eb; }

/* Responsive */
@media (max-width: 768px) {
  .cyber-cat-wrapper {
    bottom: 2.5rem;
    right: 1rem;
  }

  .action-buttons {
    left: -2.5rem;
  }

  .action-btn {
    padding: 6px;
  }

  .sprite-container {
    width: 80px;
    min-height: 80px;
  }

  .picker-grid {
    grid-template-columns: repeat(auto-fill, minmax(70px, 1fr));
  }
}
</style>
