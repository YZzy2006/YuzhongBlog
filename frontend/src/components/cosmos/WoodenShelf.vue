<template>
  <div class="shelf-section" :style="{ '--shelf-delay': (shelfIndex * 0.15) + 's' }">
    <!-- Section label (clickable) -->
    <div class="shelf-label-row" @click="$emit('showDetail')">
      <span class="shelf-label" :style="{ color }">{{ label }}</span>
      <span class="shelf-count">{{ items.length }}</span>
      <span class="shelf-expand-icon">&#8250;</span>
    </div>
    <!-- Flask row (scrollable) -->
    <div class="flask-scroll-wrap">
      <div v-if="canScrollLeft" class="scroll-fade scroll-fade-left" :style="{ '--fade-color': shelfBg }"></div>
      <div v-if="canScrollRight" class="scroll-fade scroll-fade-right" :style="{ '--fade-color': shelfBg }"></div>
      <div ref="flaskRow" class="flask-row" :style="{ justifyContent: isOverflowing ? 'flex-start' : 'center' }" @scroll="updateScrollFade" @wheel="onWheel">
        <div class="flask-edge-spacer" aria-hidden="true"></div>
        <LiquidFlask
          v-for="(item, i) in items"
          :key="item.id || i"
          :item="item"
          :color="color"
          :fillLevel="getFillLevel(item, i)"
          :seed="(item.id || '').length + i * 13 + shelfIndex * 100"
          @click="$emit('selectNode', item)"
        />
        <div class="flask-edge-spacer" aria-hidden="true"></div>
      </div>
    </div>
    <!-- Wooden plank -->
    <div class="plank">
      <div class="plank-highlight"></div>
    </div>
    <!-- Shelf legs -->
    <div class="leg leg-left"></div>
    <div class="leg leg-right"></div>
    <!-- Sticky notes (if moments/notices) — bilateral -->
    <div v-if="stickyNotes.length" class="sticky-area sticky-left">
      <StickyNote
        v-for="(note, i) in stickyNotes.slice(0, 2)"
        :key="'l'+i"
        :text="note.title || note.subtitle || ''"
        :rotation="-5 + (i * 8)"
      />
    </div>
    <div v-if="stickyNotesRight.length" class="sticky-area sticky-right">
      <StickyNote
        v-for="(note, i) in stickyNotesRight.slice(0, 2)"
        :key="'r'+i"
        :text="note.title || note.subtitle || ''"
        :rotation="3 - (i * 6)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import LiquidFlask from './LiquidFlask.vue'
import StickyNote from './StickyNote.vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  color: { type: String, default: '#3b82f6' },
  label: { type: String, default: '' },
  type: { type: String, default: '' },
  shelfIndex: { type: Number, default: 0 }
})

defineEmits(['selectNode', 'showDetail'])

function seededRandom(seed) {
  const x = Math.sin(seed) * 10000
  return x - Math.floor(x)
}

const shelfBg = '#1a110b'

const flaskRow = ref(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)
const isOverflowing = ref(false)

function updateScrollFade() {
  const el = flaskRow.value
  if (!el) return
  isOverflowing.value = el.scrollWidth > el.clientWidth + 2
  canScrollLeft.value = el.scrollLeft > 2
  canScrollRight.value = el.scrollLeft < el.scrollWidth - el.clientWidth - 2
}

function onWheel(e) {
  const el = flaskRow.value
  if (!el || el.scrollWidth <= el.clientWidth) return
  if (Math.abs(e.deltaY) > Math.abs(e.deltaX)) {
    e.preventDefault()
    el.scrollLeft += e.deltaY
  }
}

onMounted(() => {
  nextTick(updateScrollFade)
  window.addEventListener('resize', updateScrollFade)
})
onUnmounted(() => {
  window.removeEventListener('resize', updateScrollFade)
})
watch(() => props.items, () => {
  if (flaskRow.value) flaskRow.value.scrollTo({ left: 0, behavior: 'smooth' })
  nextTick(updateScrollFade)
})

function getFillLevel(item, index) {
  const seed = (item.id || '').length + index * 7 + (item.title || '').charCodeAt(0)
  return 30 + Math.floor(seededRandom(seed) * 55)
}

// Sort by date descending, null dates go to bottom; fallback to numeric suffix of id
function extractNumericId(id) {
  if (typeof id === 'number') return id
  const m = String(id).match(/(\d+)$/)
  return m ? parseInt(m[1]) : 0
}

const sortedItems = computed(() => {
  return [...props.items].sort((a, b) => {
    const da = a.date ? new Date(a.date).getTime() : 0
    const db = b.date ? new Date(b.date).getTime() : 0
    if (db !== da) return db - da
    return extractNumericId(b.id) - extractNumericId(a.id)
  })
})

const stickyNotes = computed(() => {
  if (props.type === 'moment' || props.type === 'notice') {
    return sortedItems.value.slice(0, 2)
  }
  return []
})

const stickyNotesRight = computed(() => {
  if (props.type === 'moment' || props.type === 'notice') {
    return sortedItems.value.slice(2, 4)
  }
  return []
})
</script>

<style scoped>
.shelf-section {
  position: relative;
  padding: 0 20px 36px;
  animation: fadeInUp 0.6s ease both;
  animation-delay: var(--shelf-delay, 0s);
}

/* Label */
.shelf-label-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-left: 4px;
}
.shelf-label {
  font-family: serif;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 2px;
}
.shelf-count {
  font-family: monospace;
  font-size: 11px;
  color: #6b5b3e;
  background: rgba(139, 107, 74, 0.2);
  padding: 1px 6px;
  border-radius: 9999px;
}
.shelf-expand-icon {
  font-size: 16px;
  color: #6b5b3e;
  opacity: 0;
  transition: opacity 0.2s, transform 0.2s;
  margin-left: 2px;
}
.shelf-label-row:hover {
  cursor: pointer;
}
.shelf-label-row:hover .shelf-label {
  text-decoration: underline;
  text-underline-offset: 3px;
}
.shelf-label-row:hover .shelf-expand-icon {
  opacity: 1;
  transform: translateX(2px);
}

/* Flask scroll wrapper */
.flask-scroll-wrap {
  position: relative;
  z-index: 10;
}
.flask-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 44px 0 10px;
  min-height: 130px;
  overflow-x: auto;
  overflow-y: visible;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
/* 真实 spacer 元素：保证首/末瓶子在滚动两端都有空间，避免投影被裁切（flex 容器的 padding 在滚动末尾可能折叠失效） */
.flask-edge-spacer {
  flex-shrink: 0;
  width: 14px;
}
.flask-row::-webkit-scrollbar {
  display: none;
}
/* Scroll fade edges */
.scroll-fade {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 36px;
  pointer-events: none;
  z-index: 15;
}
.scroll-fade-left {
  left: 0;
  background: linear-gradient(to right, var(--fade-color, #1a110b), transparent);
}
.scroll-fade-right {
  right: 0;
  background: linear-gradient(to left, var(--fade-color, #1a110b), transparent);
}

/* Wooden plank */
.plank {
  position: relative;
  left: 5%;
  right: 5%;
  width: 90%;
  height: 14px;
  background: linear-gradient(180deg, #4a3628, #2c1e16);
  border-bottom: 6px solid #1a110b;
  border-radius: 2px;
  box-shadow: 0 15px 30px -5px rgba(0, 0, 0, 0.8);
  margin: 0 auto;
}
.plank-highlight {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
}

/* Legs */
.leg {
  position: absolute;
  bottom: 0;
  width: 16px;
  height: 24px;
  background: #2c1e16;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.6);
  border-left: 1px solid rgba(93, 64, 55, 0.2);
  border-right: 1px solid rgba(93, 64, 55, 0.2);
}
.leg-left { left: 15%; }
.leg-right { right: 15%; }

/* Sticky notes area */
.sticky-area {
  position: absolute;
  top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 5;
}
.sticky-left { left: -40px; }
.sticky-right { right: -40px; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 480px) {
  .flask-row { gap: 4px; }
  .scroll-fade { width: 20px; }
}

@media (max-width: 768px) {
  .shelf-section { padding: 0 8px 28px; }
  .flask-row { gap: 6px; }
  .sticky-area { display: none; }
}
</style>
