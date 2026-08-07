<template>
  <Teleport to="body">
    <Transition name="lightbox">
      <div v-if="visible" class="lightbox-overlay" @click.self="close">
        <button class="lightbox-close" @click="close">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="24" height="24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
        <button v-if="images.length > 1" class="lightbox-nav prev" @click.stop="prev">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="28" height="28"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <div class="lightbox-content" @click.stop>
          <img :src="images[currentIndex]" :alt="`Image ${currentIndex + 1}`" class="lightbox-img" />
        </div>
        <button v-if="images.length > 1" class="lightbox-nav next" @click.stop="next">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="28" height="28"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
        <div v-if="images.length > 1" class="lightbox-counter">{{ currentIndex + 1 }} / {{ images.length }}</div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  images: { type: Array, default: () => [] },
  initialIndex: { type: Number, default: 0 },
})

const visible = defineModel({ type: Boolean, default: false })
const currentIndex = ref(0)

watch(visible, (v) => {
  if (v) {
    currentIndex.value = props.initialIndex
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

function close() { visible.value = false }
function prev() { currentIndex.value = (currentIndex.value - 1 + props.images.length) % props.images.length }
function next() { currentIndex.value = (currentIndex.value + 1) % props.images.length }

function onKeydown(e) {
  if (!visible.value) return
  if (e.key === 'Escape') close()
  if (e.key === 'ArrowLeft') prev()
  if (e.key === 'ArrowRight') next()
}
onMounted(() => {
  window.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.lightbox-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
}
.lightbox-close {
  position: absolute;
  top: 16px;
  right: 16px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  z-index: 10;
  padding: 8px;
  transition: color 0.2s;
}
.lightbox-close:hover { color: white; }
.lightbox-content {
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.lightbox-img {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
}
.lightbox-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  padding: 12px;
  border-radius: 50%;
  transition: all 0.2s;
  z-index: 10;
}
.lightbox-nav:hover { background: rgba(255, 255, 255, 0.2); color: white; }
.lightbox-nav.prev { left: 16px; }
.lightbox-nav.next { right: 16px; }
.lightbox-counter {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  font-weight: 600;
}

/* Transition */
.lightbox-enter-active, .lightbox-leave-active { transition: opacity 0.3s; }
.lightbox-enter-from, .lightbox-leave-to { opacity: 0; }

/* Mobile */
@media (max-width: 768px) {
  .lightbox-close {
    top: 8px;
    right: 8px;
    width: 44px;
    height: 44px;
    font-size: 20px;
  }
  .lightbox-nav {
    width: 44px;
    height: 44px;
    font-size: 18px;
  }
  .lightbox-nav.prev { left: 8px; }
  .lightbox-nav.next { right: 8px; }
  .lightbox-counter {
    bottom: 8px;
    font-size: 12px;
    padding: 4px 10px;
  }
}
</style>
