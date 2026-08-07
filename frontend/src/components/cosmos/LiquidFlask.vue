<template>
  <div
    ref="flaskEl"
    class="flask"
    :class="{ hovered: isHovered }"
    :style="flaskStyle"
    @mouseenter="onEnter"
    @mouseleave="onLeave"
    @click="$emit('click', item)"
  >
    <!-- Cork -->
    <div class="cork"></div>
    <!-- Rising particle (hover) -->
    <div class="rising-particle" :style="{ background: color }"></div>
    <!-- Bottle -->
    <div class="bottle">
      <!-- Liquid fill -->
      <div class="liquid" :style="{ height: fillLevel + '%', background: color + 'b3' }">
        <!-- Back wave -->
        <svg class="wave wave-back" viewBox="0 0 200 100" preserveAspectRatio="none">
          <path :fill="color" opacity="0.6" d="M0,15 Q25,5 50,15 T100,15 T150,15 T200,15 L200,100 L0,100 Z" />
        </svg>
        <!-- Front wave -->
        <svg class="wave wave-front" viewBox="0 0 200 100" preserveAspectRatio="none">
          <path :fill="color" opacity="0.9" d="M0,20 Q25,30 50,20 T100,20 T150,20 T200,20 L200,100 L0,100 Z" />
        </svg>
      </div>
      <!-- Glass highlight -->
      <div class="highlight"></div>
      <!-- Bubbles -->
      <div class="bubble b1"></div>
      <div class="bubble b2"></div>
    </div>
    <!-- Glass border outline -->
    <div class="glass-outline"></div>
    <!-- Hover glow orb -->
    <div class="glow-orb" :style="{ background: `radial-gradient(circle, ${color}, transparent)` }"></div>
  </div>

  <!-- Magic Tooltip (portaled to body to escape overflow clipping) -->
  <Teleport to="body">
    <Transition name="tooltip">
      <div v-if="isHovered" class="magic-tooltip" :style="tooltipPos">
        <div class="tooltip-inner" :style="{ '--tip-color': color }">
          <!-- Arrow -->
          <div class="tooltip-arrow"></div>
          <!-- Bottom decorative line -->
          <div class="tooltip-deco-line" :style="{ background: `linear-gradient(90deg, transparent, ${color}, transparent)` }"></div>
          <!-- Type header -->
          <div class="tooltip-type">{{ typeLabel }}</div>
          <!-- Title -->
          <div class="tooltip-title">{{ item.title || '' }}</div>
          <!-- Subtitle -->
          <div v-if="item.subtitle" class="tooltip-subtitle">{{ item.subtitle }}</div>
          <!-- Action hint -->
          <div class="tooltip-action">{{ $t('cosmos.clickToView') }}</div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, ref, nextTick, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  item: { type: Object, required: true },
  color: { type: String, default: '#3b82f6' },
  fillLevel: { type: Number, default: 50 },
  seed: { type: Number, default: 0 }
})

defineEmits(['click'])

const { t } = useI18n()
const isHovered = ref(false)
const flaskEl = ref(null)
const tooltipPos = ref({})
let rafId = null

const TYPE_LABELS = {
  article: 'cosmos.shelfArticles',
  project: 'cosmos.shelfProjects',
  moment: 'cosmos.shelfMoments',
  album: 'cosmos.shelfAlbums',
  song: 'cosmos.shelfSongs',
  notice: 'cosmos.shelfNotices'
}

const typeLabel = computed(() => {
  const key = TYPE_LABELS[props.item.type]
  return key ? t(key) : ''
})

function updateTooltipPos() {
  const el = flaskEl.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const tooltipW = 200
  let left = rect.left + rect.width / 2 - tooltipW / 2
  left = Math.max(8, Math.min(left, window.innerWidth - tooltipW - 8))
  tooltipPos.value = {
    position: 'fixed',
    top: (rect.bottom + 6) + 'px',
    left: left + 'px',
    width: tooltipW + 'px',
    zIndex: 10000
  }
}

function trackPosition() {
  updateTooltipPos()
  if (isHovered.value) rafId = requestAnimationFrame(trackPosition)
}

function onEnter() {
  isHovered.value = true
  nextTick(() => {
    updateTooltipPos()
    rafId = requestAnimationFrame(trackPosition)
  })
}
function onLeave() {
  isHovered.value = false
  if (rafId) { cancelAnimationFrame(rafId); rafId = null }
}

onUnmounted(() => {
  if (rafId) { cancelAnimationFrame(rafId); rafId = null }
})

// Seeded random for consistent per-flask offsets
function seededRandom(s) {
  return ((Math.sin(s * 9301 + 49297) * 49297) % 1 + 1) % 1
}

const flaskStyle = computed(() => {
  const r = seededRandom(props.seed)
  const marginTop = Math.round(r * 12) // 0-12px vertical offset
  const marginLeft = Math.round((seededRandom(props.seed + 7) - 0.5) * 8) // -4 to +4px horizontal
  return {
    '--flask-color': props.color,
    marginTop: marginTop + 'px',
    marginLeft: marginLeft + 'px',
    marginRight: marginLeft + 'px'
  }
})
</script>

<style scoped>
.flask {
  position: relative;
  width: 56px;
  min-width: 56px;
  flex-shrink: 0;
  height: 76px;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), z-index 0s;
  z-index: 10;
  filter: drop-shadow(0 4px 6px color-mix(in srgb, var(--flask-color, #3b82f6) 27%, transparent));
}
.flask.hovered {
  transform: translateY(-10px) scale(1.15);
  z-index: 100;
}

/* Cork */
.cork {
  position: absolute;
  top: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 12px;
  background: #5d4037;
  border-radius: 2px 2px 0 0;
  border-bottom: 1px solid #3e2723;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  z-index: 30;
  transform-origin: bottom left;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.flask.hovered .cork {
  transform: translateY(-14px) translateX(12px) rotate(45deg);
}

/* Rising particle */
.rising-particle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 16px;
  height: 16px;
  margin: -8px 0 0 -8px;
  border-radius: 50%;
  filter: blur(4px);
  opacity: 0;
  pointer-events: none;
  z-index: 5;
  transition: none;
}
.flask.hovered .rising-particle {
  animation: rise-particle 1.5s ease-out infinite;
}

/* Bottle */
.bottle {
  position: absolute;
  inset: 0;
  z-index: 10;
  overflow: hidden;
  backdrop-filter: blur(2px);
  clip-path: polygon(38% 0%, 62% 0%, 62% 35%, 95% 90%, 85% 100%, 15% 100%, 5% 90%, 38% 35%);
  background: rgba(255, 255, 255, 0.05);
  box-shadow: inset 0 0 15px color-mix(in srgb, var(--flask-color, #3b82f6) 20%, transparent);
}

/* Glass outline */
.glass-outline {
  position: absolute;
  inset: 0;
  z-index: 20;
  clip-path: polygon(38% 0%, 62% 0%, 62% 35%, 95% 90%, 85% 100%, 15% 100%, 5% 90%, 38% 35%);
  border: 1.5px solid rgba(255, 255, 255, 0.3);
  pointer-events: none;
}

/* Liquid */
.liquid {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  transition: height 1s ease;
}

/* Waves */
.wave {
  position: absolute;
  top: -8px;
  left: 0;
  width: 200%;
  height: 100%;
}
.wave-back {
  animation: potion-wave 3s linear infinite;
}
.wave-front {
  animation: potion-wave 2s linear infinite reverse;
}

/* Glass highlight */
.highlight {
  position: absolute;
  top: 15%;
  left: 25%;
  width: 6px;
  height: 32px;
  background: rgba(255, 255, 255, 0.4);
  border-radius: 3px;
  transform: rotate(15deg);
  filter: blur(0.5px);
  pointer-events: none;
}

/* Bubbles */
.bubble {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  animation: bubble-rise 2s ease-out infinite;
  pointer-events: none;
}
.b1 { width: 6px; height: 6px; bottom: 8px; left: 30%; animation-delay: 0s; }
.b2 { width: 4px; height: 4px; bottom: 4px; left: 60%; animation-delay: 0.5s; background: rgba(255, 255, 255, 0.4); }

/* Glow orb */
.glow-orb {
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 30px;
  border-radius: 50%;
  opacity: 0;
  pointer-events: none;
  transition: all 0.5s ease;
  filter: blur(8px);
}
.flask.hovered .glow-orb {
  opacity: 0.5;
  top: -30px;
}

@keyframes potion-wave {
  from { transform: translateX(0%); }
  to { transform: translateX(-50%); }
}
@keyframes bubble-rise {
  0% { transform: translateY(0) scale(1); opacity: 0; }
  20% { opacity: 0.8; }
  100% { transform: translateY(-40px) scale(0.5); opacity: 0; }
}
@keyframes rise-particle {
  0% { transform: translateY(0) scale(0.5); opacity: 0; }
  30% { opacity: 0.8; }
  100% { transform: translateY(-30px) scale(1.5); opacity: 0; }
}

@media (max-width: 768px) {
  .flask { width: 48px; min-width: 48px; height: 65px; }
}
</style>

<style>
/* Magic Tooltip — global (portaled to body, not scoped) */
.magic-tooltip {
  pointer-events: none;
}
.tooltip-inner {
  position: relative;
  padding: 12px 16px;
  background: rgba(35, 26, 22, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(139, 107, 74, 0.6);
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
/* Arrow pointing up */
.tooltip-arrow {
  position: absolute;
  top: -6px;
  left: 50%;
  transform: translateX(-50%) rotate(45deg);
  width: 12px;
  height: 12px;
  background: rgba(35, 26, 22, 0.95);
  border-top: 1px solid rgba(139, 107, 74, 0.6);
  border-left: 1px solid rgba(139, 107, 74, 0.6);
}
/* Bottom decorative line */
.tooltip-deco-line {
  position: absolute;
  bottom: 0;
  left: 15%;
  width: 70%;
  height: 1px;
}
/* Type label */
.tooltip-type {
  font-family: serif;
  font-size: 10px;
  color: #8b6b4a;
  letter-spacing: 2px;
  margin-bottom: 6px;
  text-transform: uppercase;
}
/* Title */
.tooltip-title {
  font-family: serif;
  font-size: 14px;
  font-weight: 700;
  color: #e8e4d9;
  line-height: 1.4;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
/* Subtitle */
.tooltip-subtitle {
  font-family: serif;
  font-size: 11px;
  color: #8b6b4a;
  margin-top: 4px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* Action hint */
.tooltip-action {
  font-family: monospace;
  font-size: 9px;
  color: var(--tip-color, #8b6b4a);
  letter-spacing: 2px;
  margin-top: 8px;
  opacity: 0.7;
  text-transform: uppercase;
}

/* Tooltip transition */
.tooltip-enter-active {
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.tooltip-leave-active {
  transition: all 0.15s ease;
}
.tooltip-enter-from {
  opacity: 0;
  transform: translateY(-8px) scale(0.9);
}
.tooltip-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.95);
}
</style>
