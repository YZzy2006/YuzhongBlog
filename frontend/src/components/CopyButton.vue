<template>
  <button class="copy-btn" :class="{ copied }" @click="handleCopy" :style="btnStyle">
    <span class="copy-btn-tooltip">{{ copied ? $t('copyButton.copied') : $t('copyButton.copy') }}</span>
    <span class="copy-btn-icons">
      <svg class="copy-icon-clipboard" viewBox="0 0 6.35 6.35" height="20" width="20" xmlns="http://www.w3.org/2000/svg">
        <g><path fill="currentColor" d="M2.43.265c-.3 0-.548.236-.573.53h-.328a.74.74 0 0 0-.735.734v3.822a.74.74 0 0 0 .735.734H4.82a.74.74 0 0 0 .735-.734V1.529a.74.74 0 0 0-.735-.735h-.328a.58.58 0 0 0-.573-.53zm0 .529h1.49c.032 0 .049.017.049.049v.431c0 .032-.017.049-.049.049H2.43c-.032 0-.05-.017-.05-.049V.843c0-.032.018-.05.05-.05zm-.901.53h.328c.026.292.274.528.573.528h1.49a.58.58 0 0 0 .573-.529h.328a.2.2 0 0 1 .206.206v3.822a.2.2 0 0 1-.206.205H1.53a.2.2 0 0 1-.206-.205V1.529a.2.2 0 0 1 .206-.206z"/></g>
      </svg>
      <svg class="copy-icon-check" viewBox="0 0 24 24" height="18" width="18" xmlns="http://www.w3.org/2000/svg">
        <g><path fill="currentColor" d="M9.707 19.121a.997.997 0 0 1-1.414 0l-5.646-5.647a1.5 1.5 0 0 1 0-2.121l.707-.707a1.5 1.5 0 0 1 2.121 0L9 14.171l9.525-9.525a1.5 1.5 0 0 1 2.121 0l.707.707a1.5 1.5 0 0 1 0 2.121z"/></g>
      </svg>
    </span>
  </button>
</template>

<script setup>
import { ref, onUnmounted } from 'vue'

const props = defineProps({
  text: { type: String, default: '' },
  size: { type: Number, default: 36 },
  borderRadius: { type: Number, default: 10 }
})

const emit = defineEmits(['copied'])
const copied = ref(false)
let timer = null

onUnmounted(() => { clearTimeout(timer) })

const btnStyle = {
  '--btn-size': props.size + 'px',
  '--btn-radius': props.borderRadius + 'px'
}

async function handleCopy() {
  const text = props.text
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.cssText = 'position:fixed;left:-9999px'
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    ta.remove()
  }
  copied.value = true
  clearTimeout(timer)
  timer = setTimeout(() => { copied.value = false }, 2000)
  emit('copied', text)
}
</script>

<style scoped>
.copy-btn {
  --btn-size: 36px;
  --btn-radius: 10px;
  width: var(--btn-size);
  height: var(--btn-size);
  border-radius: var(--btn-radius);
  background: #353434;
  color: #ccc;
  border: none;
  cursor: pointer;
  position: relative;
  outline: none;
  flex-shrink: 0;
}

.copy-btn-tooltip {
  position: absolute;
  opacity: 0;
  visibility: hidden;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
  font: 12px Menlo, 'Roboto Mono', monospace;
  color: rgb(50, 50, 50);
  background: #f4f3f3;
  padding: 7px;
  border-radius: 4px;
  pointer-events: none;
  transition: all 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55);
  z-index: 10;
}

.copy-btn-tooltip::after {
  content: '';
  position: absolute;
  bottom: -3.5px;
  width: 7px;
  height: 7px;
  background: inherit;
  left: 50%;
  transform: translateX(-50%) rotate(45deg);
  z-index: -1;
  pointer-events: none;
}

.copy-btn-icons {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.copy-btn-icons svg {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  transition: opacity 0.2s;
}

.copy-icon-check {
  display: none;
}

.copy-btn:hover .copy-btn-tooltip,
.copy-btn.copied .copy-btn-tooltip {
  opacity: 1;
  visibility: visible;
  top: calc((100% + 8px) * -1);
}

.copy-btn.copied .copy-icon-clipboard {
  display: none;
}

.copy-btn.copied .copy-icon-check {
  display: block;
  color: #4ade80;
}

.copy-btn:hover {
  background: #464646;
  color: #8bb9fe;
}

.copy-btn:active {
  outline: 1px solid rgb(141, 141, 141);
}
</style>
