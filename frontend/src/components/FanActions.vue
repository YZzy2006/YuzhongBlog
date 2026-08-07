<template>
  <div class="fan-actions" :class="{ expanded: hoverZone }" :style="{ left: pos.x + 'px', bottom: pos.y + 'px' }"
    @mouseenter="hoverZone = true" @mouseleave="hoverZone = false">

    <!-- Buttons -->
    <div class="button-box"
      @mousedown="onDragStart" @touchstart.passive="onTouchStart">
      <!-- Drag dot (visible when expanded) -->
      <div class="drag-dot" @mousedown.stop="onDragStart" @touchstart.stop.passive="onTouchStart"></div>
      <!-- Left: Like -->
      <div class="fan-btn like" :class="{ liked: liked }" @click="$emit('like')" :title="liked ? $t('fanActions.liked') : $t('fanActions.like')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path v-if="liked" d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" fill="currentColor"/>
          <path v-else d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
        </svg>
      </div>

      <!-- Middle: Back to top -->
      <div class="fan-btn top" @click="$emit('scrollTop')" :title="$t('fanActions.backToTop')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 2L11 13"/>
          <path d="M22 2L15 22L11 13L2 9L22 2Z"/>
        </svg>
      </div>

      <!-- Right: AI -->
      <div class="fan-btn ai" @click="handleAiClick" :title="selectedText ? $t('fanActions.aiInterpret') : $t('fanActions.aiChat')">
        <div class="fan-ai-loader">
          <svg width="100" height="100" viewBox="0 0 100 100">
            <defs>
              <mask id="fan-ai-clipping">
                <polygon points="0,0 100,0 100,100 0,100" fill="black"></polygon>
                <polygon points="25,25 75,25 50,75" fill="white"></polygon>
                <polygon points="50,25 75,75 25,75" fill="white"></polygon>
                <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                <polygon points="35,35 65,35 50,65" fill="white"></polygon>
              </mask>
            </defs>
          </svg>
          <div class="fan-ai-loader-box"></div>
        </div>
        <span v-if="selectedText" class="ai-badge"></span>
      </div>
    </div>

    <!-- AI Dialog -->
    <Transition :name="dialogLeft ? 'ai-dialog-left' : 'ai-dialog'">
      <div v-if="showAi" class="ai-dialog" :class="{ 'dialog-left': dialogLeft }" @click.stop>
        <div class="ai-dialog-header">
          <span class="ai-dialog-title">{{ $t('fanActions.aiDialogTitle') }}</span>
          <div class="ai-dialog-actions">
            <button class="ai-dialog-side-btn" @click="dialogLeft = !dialogLeft" :title="dialogLeft ? $t('fanActions.moveRight') : $t('fanActions.moveLeft')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path v-if="dialogLeft" d="M19 12H5M11 19l-7-7 7-7"/>
                <path v-else d="M5 12h14M13 5l7 7-7 7"/>
              </svg>
            </button>
            <button class="ai-dialog-close" @click="closeAi">&times;</button>
          </div>
        </div>
        <div class="ai-dialog-context">
          <span class="context-label">{{ $t('fanActions.selectedContent') }}</span>
          <span class="context-text">{{ displayText }}</span>
        </div>
        <div class="ai-dialog-messages" ref="aiMessagesRef">
          <div v-for="(msg, i) in aiMessages" :key="i" class="ai-msg" :class="msg.role">
            <div v-if="msg.role === 'user'" class="msg-bubble user-bubble">{{ msg.content }}</div>
            <div v-else class="msg-bubble ai-bubble">
              <MdPreview :modelValue="msg.content" previewTheme="github" :codeFoldable="false" />
            </div>
          </div>
          <div v-if="aiStreaming" class="ai-msg assistant">
            <div class="msg-bubble ai-bubble">
              <div v-if="!aiStreamContent" class="typing-dots"><span></span><span></span><span></span></div>
              <MdPreview v-else :modelValue="aiStreamContent" previewTheme="github" :codeFoldable="false" />
            </div>
          </div>
          <div v-if="aiError" class="ai-error-msg">{{ aiError }}</div>
        </div>
        <div class="ai-dialog-input">
          <input v-model="aiInput" type="text" :placeholder="$t('fanActions.followUp')" @keyup.enter="sendAiMessage" :disabled="aiStreaming" />
          <button class="ai-send-btn" @click="sendAiMessage" :disabled="aiStreaming || !aiInput.trim()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 2 11 13"/><path d="M22 2 15 22 11 13 2 9z"/></svg>
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { aiChatStream } from '../utils/ai'

const { t } = useI18n()
import { MdPreview } from 'md-editor-v3'
import '../utils/mdEditorConfig'

const props = defineProps({
  liked: { type: Boolean, default: false }
})
defineEmits(['like', 'scrollTop'])

const hoverZone = ref(false)
const selectedText = ref('')
const showAi = ref(false)
const aiMessages = ref([])
const aiInput = ref('')
const aiStreaming = ref(false)
const aiStreamContent = ref('')
const aiError = ref('')
const aiMessagesRef = ref(null)
const dialogLeft = ref(false)
let abortFn = null

// --- Drag state ---
const BOX_SIZE = 80 // 5rem
const pos = reactive({ x: 0, y: 0 })
let dragging = false
let dragOffset = { x: 0, y: 0 }
let hasMoved = false

function initPosition() {
  pos.x = 132
  pos.y = 32
}

function clampPosition() {
  pos.x = Math.max(0, Math.min(pos.x, window.innerWidth - BOX_SIZE))
  pos.y = Math.max(0, Math.min(pos.y, window.innerHeight - BOX_SIZE))
}

function onDragStart(e) {
  if (e.button !== 0) return
  dragging = true
  hasMoved = false
  dragOffset.x = pos.x - e.clientX
  dragOffset.y = e.clientY + pos.y
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
}

function onDragMove(e) {
  if (!dragging) return
  const newX = dragOffset.x + e.clientX
  const newY = dragOffset.y - e.clientY
  if (Math.abs(newX - pos.x) > 3 || Math.abs(newY - pos.y) > 3) {
    hasMoved = true
  }
  pos.x = newX
  pos.y = newY
  clampPosition()
}

function onDragEnd() {
  dragging = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
}

function onTouchStart(e) {
  if (e.touches.length !== 1) return
  const t = e.touches[0]
  dragging = true
  hasMoved = false
  dragOffset.x = pos.x - t.clientX
  dragOffset.y = t.clientY + pos.y
  document.addEventListener('touchmove', onTouchMove, { passive: false })
  document.addEventListener('touchend', onTouchEnd)
}

function onTouchMove(e) {
  if (!dragging) return
  e.preventDefault()
  const t = e.touches[0]
  const newX = dragOffset.x + t.clientX
  const newY = dragOffset.y - t.clientY
  if (Math.abs(newX - pos.x) > 3 || Math.abs(newY - pos.y) > 3) {
    hasMoved = true
  }
  pos.x = newX
  pos.y = newY
  clampPosition()
}

function onTouchEnd() {
  dragging = false
  document.removeEventListener('touchmove', onTouchMove)
  document.removeEventListener('touchend', onTouchEnd)
}

// --- Auto-detect dialog side based on position ---
function autoDialogSide() {
  dialogLeft.value = pos.x > window.innerWidth / 2
}

// --- AI logic ---
const displayText = computed(() => {
  const txt = selectedText.value
  return txt.length > 80 ? txt.slice(0, 80) + '...' : txt
})

function onSelectionChange() {
  const sel = window.getSelection()
  const text = sel?.toString().trim() || ''
  if (text.length > 2) {
    selectedText.value = text
  }
}

function handleAiClick() {
  if (hasMoved) return
  const sel = window.getSelection()
  const freshText = sel?.toString().trim() || ''
  if (freshText.length > 2) {
    selectedText.value = freshText
  }

  if (!selectedText.value) {
    showAi.value = true
    autoDialogSide()
    scrollAiBottom()
    return
  }

  if (abortFn) { abortFn(); abortFn = null }
  showAi.value = true
  autoDialogSide()
  aiMessages.value = []
  aiStreamContent.value = ''
  aiError.value = ''
  aiStreaming.value = false
  const prompt = `${t('fanActions.interpretPrompt')}\n\n> ${selectedText.value}`
  sendToAi(prompt)
}

function sendAiMessage() {
  const msg = aiInput.value.trim()
  if (!msg || aiStreaming.value) return
  aiInput.value = ''
  sendToAi(msg)
}

function sendToAi(message) {
  aiMessages.value.push({ role: 'user', content: message })
  aiStreaming.value = true
  aiStreamContent.value = ''
  aiError.value = ''
  scrollAiBottom()

  abortFn = aiChatStream(message, {
    onChunk(content) {
      aiStreamContent.value += content
      scrollAiBottom()
    },
    onDone() {
      aiMessages.value.push({ role: 'assistant', content: aiStreamContent.value })
      aiStreamContent.value = ''
      aiStreaming.value = false
      scrollAiBottom()
    },
    onError(e) {
      aiError.value = e.message || t('fanActions.aiUnavailable')
      if (aiStreamContent.value) {
        aiMessages.value.push({ role: 'assistant', content: aiStreamContent.value })
        aiStreamContent.value = ''
      }
      aiStreaming.value = false
      scrollAiBottom()
    }
  })
}

function closeAi() {
  showAi.value = false
  selectedText.value = ''
  if (abortFn) { abortFn(); abortFn = null }
}

function scrollAiBottom() {
  nextTick(() => {
    if (aiMessagesRef.value) {
      aiMessagesRef.value.scrollTop = aiMessagesRef.value.scrollHeight
    }
  })
}

onMounted(() => {
  initPosition()
  document.addEventListener('mouseup', onSelectionChange)
  window.addEventListener('resize', clampPosition)
})
onBeforeUnmount(() => {
  document.removeEventListener('mouseup', onSelectionChange)
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('touchmove', onTouchMove)
  document.removeEventListener('touchend', onTouchEnd)
  window.removeEventListener('resize', clampPosition)
  if (abortFn) abortFn()
})
</script>

<style scoped>
.fan-actions {
  position: fixed;
  z-index: 100;
  width: 5rem;
  height: 5rem;
}

/* Expanded hover hit area covering fanned-out buttons */
.fan-actions::after {
  content: '';
  position: absolute;
  inset: -4rem -2rem -1rem;
  z-index: 1;
}

.button-box {
  position: relative;
  width: 5rem;
  height: 5rem;
  cursor: grab;
}
.button-box:active {
  cursor: grabbing;
}

.drag-dot {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.2);
  border: 2px solid rgba(0, 0, 0, 0.15);
  transform: translate(-50%, -50%) scale(0);
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), background 0.2s;
  z-index: 60;
  cursor: grab;
}
.drag-dot:active {
  cursor: grabbing;
  background: rgba(0, 0, 0, 0.3);
}
.fan-actions.expanded .drag-dot {
  transform: translate(-50%, -50%) scale(1);
}

.fan-btn {
  width: 2.4rem;
  height: 2.4rem;
  position: absolute;
  left: 50%;
  top: 50%;
  cursor: pointer;
  border: 2px solid rgba(0, 0, 0, 0.15);
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  opacity: 0.85;
  box-shadow: inset 0 0 3px rgba(0, 0, 0, 0.15), 0 2px 8px rgba(0, 0, 0, 0.1);
}
.fan-btn svg {
  width: 18px;
  height: 18px;
  opacity: 0.8;
  transition: 0.25s;
}
.fan-btn.like {
  transform: translate(-50%, -50%) rotate(90deg);
  z-index: 30;
  background: #ff7f50;
  color: #fff;
}
.fan-btn.top {
  transform: translate(-50%, -50%) rotate(-115deg);
  z-index: 40;
  background: #ffd700;
  color: #fff;
}
.fan-btn.ai {
  transform: translate(-50%, -50%) rotate(-45deg);
  z-index: 50;
  background: #019b98;
  color: #fff;
  position: relative;
  overflow: hidden;
}

/* Fan AI Loader */
.fan-ai-loader {
  --color-one: #ffbf48;
  --color-two: #be4a1d;
  --color-three: #ffbf4780;
  --color-four: #bf4a1d80;
  --color-five: #ffbf4740;
  --time-animation: 2s;
  --size: 0.25;
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  transform: scale(var(--size));
  box-shadow:
    0 0 25px 0 var(--color-three),
    0 20px 50px 0 var(--color-four);
  animation: fan-colorize calc(var(--time-animation) * 3) ease-in-out infinite;
  flex-shrink: 0;
  pointer-events: none;
}
.fan-ai-loader::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border-top: solid 1px var(--color-one);
  border-bottom: solid 1px var(--color-two);
  background: linear-gradient(180deg, var(--color-five), var(--color-four));
  box-shadow:
    inset 0 10px 10px 0 var(--color-three),
    inset 0 -10px 10px 0 var(--color-four);
}
.fan-ai-loader-box {
  width: 100px;
  height: 100px;
  background: linear-gradient(180deg, var(--color-one) 30%, var(--color-two) 70%);
  mask: url(#fan-ai-clipping);
  -webkit-mask: url(#fan-ai-clipping);
}
.fan-ai-loader svg {
  position: absolute;
}
.fan-ai-loader svg #fan-ai-clipping {
  filter: contrast(15);
  animation: fan-roundness calc(var(--time-animation) / 2) linear infinite;
}
.fan-ai-loader svg #fan-ai-clipping polygon {
  filter: blur(7px);
}
.fan-ai-loader svg #fan-ai-clipping polygon:nth-child(1) {
  transform-origin: 75% 25%;
  transform: rotate(90deg);
}
.fan-ai-loader svg #fan-ai-clipping polygon:nth-child(2) {
  transform-origin: 50% 50%;
  animation: fan-rotation var(--time-animation) linear infinite reverse;
}
.fan-ai-loader svg #fan-ai-clipping polygon:nth-child(3) {
  transform-origin: 50% 60%;
  animation: fan-rotation var(--time-animation) linear infinite;
  animation-delay: calc(var(--time-animation) / -3);
}
.fan-ai-loader svg #fan-ai-clipping polygon:nth-child(4) {
  transform-origin: 40% 40%;
  animation: fan-rotation var(--time-animation) linear infinite reverse;
}
.fan-ai-loader svg #fan-ai-clipping polygon:nth-child(5) {
  transform-origin: 40% 40%;
  animation: fan-rotation var(--time-animation) linear infinite reverse;
  animation-delay: calc(var(--time-animation) / -2);
}
.fan-ai-loader svg #fan-ai-clipping polygon:nth-child(6) {
  transform-origin: 60% 40%;
  animation: fan-rotation var(--time-animation) linear infinite;
}
.fan-ai-loader svg #fan-ai-clipping polygon:nth-child(7) {
  transform-origin: 60% 40%;
  animation: fan-rotation var(--time-animation) linear infinite;
  animation-delay: calc(var(--time-animation) / -1.5);
}
@keyframes fan-rotation {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
@keyframes fan-roundness {
  0% { filter: contrast(15); }
  20% { filter: contrast(3); }
  40% { filter: contrast(3); }
  60% { filter: contrast(15); }
  100% { filter: contrast(15); }
}
@keyframes fan-colorize {
  0% { filter: hue-rotate(0deg); }
  20% { filter: hue-rotate(-30deg); }
  40% { filter: hue-rotate(-60deg); }
  60% { filter: hue-rotate(-90deg); }
  80% { filter: hue-rotate(-45deg); }
  100% { filter: hue-rotate(0deg); }
}
.ai-badge {
  position: absolute;
  top: -3px;
  right: -3px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  border: 1.5px solid #fff;
  animation: badgePulse 1.5s ease-in-out infinite;
}
@keyframes badgePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.3); }
}

/* Expand on hover - fan upward in an arc */
.fan-actions.expanded .fan-btn.like,
.fan-actions:hover .fan-btn.like {
  transform: translate(80%, -160%) rotate(0deg) scale(1.08);
  opacity: 1;
}
.fan-actions.expanded .fan-btn.top,
.fan-actions:hover .fan-btn.top {
  transform: translate(-50%, -190%) rotate(0deg) scale(1.08);
  opacity: 1;
}
.fan-actions.expanded .fan-btn.ai,
.fan-actions:hover .fan-btn.ai {
  transform: translate(-180%, -160%) rotate(0deg) scale(1.08);
  opacity: 1;
}
.fan-actions.expanded .fan-btn svg,
.fan-actions:hover .fan-btn svg {
  width: 20px;
  opacity: 1;
}

/* Active press */
.fan-actions.expanded .fan-btn.like:active,
.fan-actions:hover .fan-btn.like:active {
  transform: translate(80%, -160%) rotate(0deg) scale(0.9);
}
.fan-actions.expanded .fan-btn.top:active,
.fan-actions:hover .fan-btn.top:active {
  transform: translate(-50%, -190%) rotate(0deg) scale(0.9);
}
.fan-actions.expanded .fan-btn.ai:active,
.fan-actions:hover .fan-btn.ai:active {
  transform: translate(-180%, -160%) rotate(0deg) scale(0.9);
}

/* Liked state */
.fan-btn.liked {
  background: #ef4444 !important;
  border-color: #dc2626 !important;
}

/* ===== AI Dialog ===== */
.ai-dialog {
  position: absolute;
  bottom: 0;
  left: calc(100% + 14px);
  width: 360px;
  max-height: 420px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(16px);
  border: 1px solid var(--color-border-light, #e5e7eb);
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 1000;
}
.ai-dialog.dialog-left {
  left: auto;
  right: calc(100% + 14px);
}

/* Right-side transitions (default) */
.ai-dialog-enter-active,
.ai-dialog-leave-active {
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.ai-dialog-enter-from,
.ai-dialog-leave-to {
  opacity: 0;
  transform: translateX(12px) scale(0.95);
}

/* Left-side transitions */
.ai-dialog-left-enter-active,
.ai-dialog-left-leave-active {
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.ai-dialog-left-enter-from,
.ai-dialog-left-leave-to {
  opacity: 0;
  transform: translateX(-12px) scale(0.95);
}

.ai-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.6rem 0.85rem;
  border-bottom: 1px solid var(--color-border-light, #e5e7eb);
  background: linear-gradient(135deg, rgba(1, 155, 152, 0.06), rgba(59, 130, 246, 0.06));
}
.ai-dialog-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text, #111);
}
.ai-dialog-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.ai-dialog-side-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}
.ai-dialog-side-btn svg {
  width: 14px;
  height: 14px;
}
.ai-dialog-side-btn:hover { background: #f3f4f6; color: #333; }
.ai-dialog-close {
  background: none;
  border: none;
  font-size: 1.15rem;
  color: #999;
  cursor: pointer;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}
.ai-dialog-close:hover { background: #f3f4f6; color: #333; }

.ai-dialog-context {
  padding: 0.5rem 0.85rem;
  background: rgba(1, 155, 152, 0.04);
  border-bottom: 1px solid var(--color-border-light, #e5e7eb);
  font-size: 0.78rem;
  line-height: 1.5;
}
.context-label {
  color: #019b98;
  font-weight: 500;
}
.context-text {
  color: #666;
}

.ai-dialog-messages {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem 0.85rem;
  max-height: 260px;
}
.ai-msg { margin-bottom: 0.6rem; }
.ai-msg.user { display: flex; justify-content: flex-end; }
.msg-bubble { max-width: 88%; font-size: 0.82rem; line-height: 1.6; }
.user-bubble {
  background: #019b98;
  color: #fff;
  padding: 0.4rem 0.75rem;
  border-radius: 10px 10px 2px 10px;
  word-break: break-word;
}
.ai-bubble {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  padding: 0.4rem 0.65rem;
  border-radius: 10px 10px 10px 2px;
  color: #333;
}
.ai-bubble :deep(.md-editor) { background: transparent; border: none; }
.ai-bubble :deep(.md-editor-preview) { font-size: 0.82rem; line-height: 1.6; }
.ai-bubble :deep(.md-editor-preview p) { margin-bottom: 0.3rem; }
.ai-bubble :deep(.md-editor-preview pre) { font-size: 0.78rem; background: #f3f4f6; border: 1px solid #e5e7eb; }

.typing-dots { display: flex; gap: 4px; padding: 4px 0; }
.typing-dots span {
  width: 5px; height: 5px; border-radius: 50%; background: #999;
  animation: dotBounce 1.4s ease-in-out infinite;
}
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes dotBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-5px); opacity: 1; }
}

.ai-dialog-input {
  display: flex; gap: 0.4rem; padding: 0.6rem 0.85rem;
  border-top: 1px solid var(--color-border-light, #e5e7eb);
}
.ai-dialog-input input {
  flex: 1; border: 1px solid #e5e7eb; border-radius: 6px;
  padding: 0.4rem 0.65rem; font-size: 0.82rem; outline: none;
  transition: border-color 0.2s;
}
.ai-dialog-input input:focus { border-color: #019b98; }
.ai-dialog-input input:disabled { opacity: 0.6; }
.ai-error-msg {
  padding: 0.4rem 0.85rem;
  font-size: 0.78rem;
  color: #dc2626;
  background: rgba(220, 38, 38, 0.06);
  border-top: 1px solid rgba(220, 38, 38, 0.1);
}
.ai-send-btn {
  width: 32px; height: 32px; border: none; border-radius: 6px;
  background: #019b98; color: #fff; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.2s; flex-shrink: 0;
}
.ai-send-btn svg { width: 14px; height: 14px; }
.ai-send-btn:hover:not(:disabled) { background: #017a78; }
.ai-send-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* Night mode */
.night .fan-btn {
  border-color: rgba(255, 255, 255, 0.15);
  box-shadow: inset 0 0 3px rgba(255, 255, 255, 0.08), 0 2px 8px rgba(0, 0, 0, 0.3);
}
.night .drag-dot {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.15);
}
.night .ai-dialog {
  background: rgba(30, 41, 59, 0.97);
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}
.night .ai-dialog-header {
  background: linear-gradient(135deg, rgba(1, 155, 152, 0.1), rgba(59, 130, 246, 0.1));
  border-bottom-color: rgba(255, 255, 255, 0.08);
}
.night .ai-dialog-title { color: #e0e0e0; }
.night .ai-dialog-side-btn { color: #718096; }
.night .ai-dialog-side-btn:hover { background: rgba(255, 255, 255, 0.08); color: #e0e0e0; }
.night .ai-dialog-close { color: #718096; }
.night .ai-dialog-close:hover { background: rgba(255, 255, 255, 0.08); color: #e0e0e0; }
.night .ai-dialog-context {
  background: rgba(1, 155, 152, 0.06);
  border-bottom-color: rgba(255, 255, 255, 0.08);
}
.night .context-label { color: #5eead4; }
.night .context-text { color: #a0aec0; }
.night .user-bubble {
  background: #0d9488;
}
.night .ai-bubble {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
  color: #d1d5db;
}
.night .ai-bubble :deep(.md-editor) { background: transparent; }
.night .ai-bubble :deep(.md-editor-preview) { color: #d1d5db; }
.night .ai-bubble :deep(.md-editor-preview pre) { background: #1a1b2e; border-color: rgba(255, 255, 255, 0.08); }
.night .ai-bubble :deep(.md-editor-preview code) { color: #f687b3; }
.night .ai-bubble :deep(.md-editor-preview pre code) { color: #cdd6f4; }
.night .typing-dots span { background: #718096; }
.night .ai-dialog-input {
  border-top-color: rgba(255, 255, 255, 0.08);
}
.night .ai-dialog-input input {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.12);
  color: #e0e0e0;
}
.night .ai-dialog-input input:focus { border-color: #019b98; }
.night .ai-dialog-input input::placeholder { color: #718096; }
.night .ai-send-btn {
  background: #0d9488;
}
.night .ai-send-btn:hover:not(:disabled) { background: #0f766e; }
.night .ai-error-msg {
  color: #fc8181;
  background: rgba(252, 129, 129, 0.1);
  border-top-color: rgba(252, 129, 129, 0.15);
}
</style>
