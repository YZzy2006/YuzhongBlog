<template>
  <Transition name="hud-slide">
    <div v-if="visible" class="stacked-hud" @click.stop>
      <div class="hud-header" :style="{ borderRightColor: color }" @click="expanded = !expanded">
        <div class="hud-header-left">
          <LayersIcon :size="14" :color="color" />
          <span class="hud-title">{{ label }} ARCHIVE</span>
        </div>
        <div class="hud-header-right">
          <span class="hud-count">{{ items.length }} FILES</span>
          <span class="hud-arrow" :class="{ rotated: expanded }">&#9660;</span>
        </div>
      </div>

      <div class="hud-body">
        <TransitionGroup name="card-stack" tag="div" class="hud-cards" :class="{ expanded }">
          <template v-if="expanded">
            <div
              v-for="item in items"
              :key="item.id"
              class="hud-card-full"
              :style="{ borderLeftColor: color }"
              @click="$emit('selectNode', item)"
            >
              <span class="hud-card-date">{{ formatDate(item.date) }}</span>
              <span class="hud-card-title">{{ item.title }}</span>
            </div>
          </template>
          <template v-else>
            <div
              v-for="(item, i) in items.slice(0, 3)"
              :key="item.id"
              class="hud-card-stacked"
              :style="{
                top: i * 10 + 'px',
                zIndex: 30 - i,
                transform: `rotate(${[-6, 4, -2][i] || 0}deg)`,
                borderLeftColor: color
              }"
              @click="expanded = true"
            >
              <span class="hud-card-date">{{ formatDate(item.date) }}</span>
              <span class="hud-card-title">{{ item.title }}</span>
            </div>
          </template>
        </TransitionGroup>
      </div>

      <button class="hud-close" @click="$emit('close')">&times;</button>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch } from 'vue'

const LayersIcon = {
  props: { size: { type: Number, default: 14 }, color: { type: String, default: '#eab308' } },
  template: `<svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" :stroke="color" stroke-width="2"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>`
}

const props = defineProps({
  visible: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
  color: { type: String, default: '#eab308' },
  label: { type: String, default: '' }
})

defineEmits(['selectNode', 'close'])

const expanded = ref(false)

// Reset expanded when items change (switching category)
watch(() => props.items, () => { expanded.value = false })

function formatDate(d) {
  if (!d) return ''
  return d.includes('T') ? d.replace('T', ' ').substring(0, 10) : d.substring(0, 10)
}
</script>

<style scoped>
.stacked-hud {
  position: fixed;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  width: 320px;
  pointer-events: auto;
  z-index: 50;
  background: rgba(10, 10, 10, 0.85);
  backdrop-filter: blur(16px);
  border: 1px solid #333;
  border-radius: 16px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.8);
  overflow: hidden;
}

.hud-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #333;
  border-right: 4px solid;
  cursor: pointer;
  transition: background 0.2s;
}
.hud-header:hover { background: rgba(255, 255, 255, 0.05); }

.hud-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.hud-title {
  font-size: 11px;
  font-family: monospace;
  font-weight: 900;
  letter-spacing: 2px;
  color: #e2e8f0;
  text-transform: uppercase;
}
.hud-header-right {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  color: #94a3b8;
  font-family: monospace;
}
.hud-arrow {
  display: inline-block;
  transition: transform 0.3s;
  font-size: 8px;
}
.hud-arrow.rotated { transform: rotate(180deg); }

.hud-body {
  padding: 12px;
  position: relative;
  overflow: hidden;
}
.hud-cards {
  position: relative;
  min-height: 116px;
}
.hud-cards.expanded {
  max-height: 320px;
  overflow-y: auto;
}

.hud-card-stacked {
  position: absolute;
  left: 0;
  right: 0;
  height: 96px;
  padding: 12px 16px;
  background: #222;
  border: 1px solid #333;
  border-left: 2px solid;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 4px;
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s;
}
.hud-card-stacked:hover {
  transform: translateY(-5px) rotate(0deg) !important;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.5);
}

.hud-card-full {
  padding: 12px 16px;
  background: #222;
  border: 1px solid #333;
  border-left: 2px solid;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: background 0.2s, transform 0.2s;
}
.hud-card-full:hover {
  background: #2a2a2a;
  transform: translateX(-4px);
}

.hud-card-date {
  font-size: 10px;
  font-family: monospace;
  color: #94a3b8;
}
.hud-card-title {
  font-size: 12px;
  font-weight: 700;
  color: #e2e8f0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hud-close {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  border: 1px solid #444;
  background: rgba(0, 0, 0, 0.5);
  color: #94a3b8;
  font-size: 14px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.hud-close:hover { background: rgba(139, 107, 74, 0.3); color: #d4af37; }

/* Custom scrollbar */
.stacked-hud :deep(.hud-cards)::-webkit-scrollbar {
  width: 4px;
}
.stacked-hud :deep(.hud-cards)::-webkit-scrollbar-track {
  background: transparent;
}
.stacked-hud :deep(.hud-cards)::-webkit-scrollbar-thumb {
  background: rgba(234, 179, 8, 0.5);
}
.stacked-hud :deep(.hud-cards)::-webkit-scrollbar-thumb:hover {
  background: rgba(234, 179, 8, 0.8);
}

/* Custom scrollbar for expanded list */
.hud-card-full + .hud-card-full {
  margin-top: 6px;
}

.hud-slide-enter-active { transition: all 0.3s ease; }
.hud-slide-leave-active { transition: all 0.2s ease; }
.hud-slide-enter-from { opacity: 0; transform: translateY(-50%) translateX(20px); }
.hud-slide-leave-to { opacity: 0; transform: translateY(-50%) translateX(10px); }

.card-stack-enter-active { transition: all 0.3s ease; }
.card-stack-leave-active { transition: all 0.2s ease; }
.card-stack-enter-from { opacity: 0; transform: translateY(10px); }
.card-stack-leave-to { opacity: 0; }

@media (max-width: 768px) {
  .stacked-hud {
    right: 8px;
    left: 8px;
    width: auto;
    top: auto;
    bottom: 80px;
    transform: none;
  }
}
</style>
