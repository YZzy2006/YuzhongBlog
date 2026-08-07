<template>
  <div class="starship-mode">
    <!-- Ambient glow orbs -->
    <div class="glow-orb glow-yellow"></div>
    <div class="glow-orb glow-blue"></div>

    <!-- CRT scanline overlay -->
    <div class="crt-overlay"></div>

    <div ref="canvasContainer" class="starship-canvas"></div>

    <div class="starship-overlay" v-if="sceneReady">
      <TacticalPoint
        v-for="tp in tacticalPoints"
        :key="tp.id"
        :x="tp.screenX"
        :y="tp.screenY"
        :color="tp.color"
        :label="tp.label"
        :count="tp.items.length"
        :active="activeCategory === tp.id"
        @select="toggleCategory(tp.id)"
      />
    </div>

    <!-- Category buttons (bottom-left) -->
    <div class="category-panel">
      <button
        v-for="cat in TACTICAL_CONFIG"
        :key="cat.id"
        class="cat-btn"
        :class="{ active: activeCategory === cat.id }"
        :style="catBtnStyle(cat.id)"
        @click="toggleCategory(cat.id)"
      >
        <span class="cat-dot" :style="{ background: getCatColor(cat.id) }"></span>
        <span class="cat-label">{{ t(`cosmos.${cat.i18nKey}`) }}</span>
        <span class="cat-count">{{ getCatCount(cat.type) }}</span>
      </button>
    </div>

    <StackedHUD
      :visible="!!activeCategory"
      :items="activeItems"
      :color="activeColor"
      :label="activeLabel"
      @selectNode="$emit('selectNode', $event)"
      @close="activeCategory = null"
    />

    <!-- Ship title overlay -->
    <div class="ship-title-overlay">
      <div class="ship-name">TC-01</div>
      <div class="ship-class">TAICHU-CLASS STARSHIP</div>
      <div class="ship-divider"></div>
      <div class="ship-status">
        <span class="status-dot"></span>
        <span>SYSTEMS ONLINE</span>
      </div>
    </div>

    <!-- Data summary panel (right side) — hidden when HUD is open -->
    <div v-show="!activeCategory" class="data-summary">
      <div class="summary-header">DATA ARCHIVE</div>
      <div class="summary-divider"></div>
      <div
        v-for="cat in TACTICAL_CONFIG"
        :key="cat.id"
        class="summary-row"
        :class="{ active: activeCategory === cat.id }"
        @click="toggleCategory(cat.id)"
      >
        <span class="summary-dot" :style="{ background: getCatColor(cat.id) }"></span>
        <span class="summary-label">{{ t(`cosmos.${cat.i18nKey}`) }}</span>
        <span class="summary-count">{{ getCatCount(cat.type) }}</span>
      </div>
    </div>

    <div class="starship-hint">
      <span>{{ $t('cosmos.starshipHint') }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import TacticalPoint from '../components/cosmos/TacticalPoint.vue'
import StackedHUD from '../components/cosmos/StackedHUD.vue'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  planets: { type: Array, default: () => [] }
})

defineEmits(['selectNode'])

const { t } = useI18n()

const TACTICAL_CONFIG = [
  { id: 'articles', type: 'article', i18nKey: 'shelfArticles', worldPos: { x: -1.0, y: -0.5, z: 1.5 } },
  { id: 'projects', type: 'project', i18nKey: 'shelfProjects', worldPos: { x: 1.5, y: 0.3, z: 1.2 } },
  { id: 'moments', type: 'moment', i18nKey: 'shelfMoments', worldPos: { x: -1.5, y: -2.0, z: 1.5 } },
  { id: 'albums', type: 'album', i18nKey: 'shelfAlbums', worldPos: { x: 2.0, y: -0.8, z: 1.0 } },
  { id: 'songs', type: 'song', i18nKey: 'shelfSongs', worldPos: { x: -2.3, y: 0, z: 1.5 } },
  { id: 'notices', type: 'notice', i18nKey: 'shelfNotices', worldPos: { x: 0, y: 1.5, z: 1.8 } }
]

const canvasContainer = ref(null)
const sceneReady = ref(false)
const activeCategory = ref(null)
const tacticalPoints = ref([])
let scene = null
let animId = null

const activeConfig = computed(() => TACTICAL_CONFIG.find(c => c.id === activeCategory.value))

const activeItems = computed(() => {
  if (!activeCategory.value) return []
  const cfg = activeConfig.value
  return cfg ? props.nodes.filter(n => n.type === cfg.type) : []
})

const activeColor = computed(() => {
  const planet = props.planets.find(p => p.id === activeCategory.value)
  return planet?.color || '#eab308'
})

const activeLabel = computed(() => {
  const cfg = activeConfig.value
  return cfg ? t(`cosmos.${cfg.i18nKey}`) : ''
})

function getCatColor(id) {
  const planet = props.planets.find(p => p.id === id)
  return planet?.color || '#eab308'
}

function getCatCount(type) {
  return props.nodes.filter(n => n.type === type).length
}

function catBtnStyle(id) {
  const color = getCatColor(id)
  if (activeCategory.value === id) {
    return {
      background: color,
      borderColor: color,
      color: '#111'
    }
  }
  return {}
}

function toggleCategory(id) {
  activeCategory.value = activeCategory.value === id ? null : id
}

function updateScreenPositions() {
  if (!scene || scene.disposed) return
  tacticalPoints.value = TACTICAL_CONFIG.map(cfg => {
    const planet = props.planets.find(p => p.id === cfg.id)
    const pos = scene.getScreenPosition(cfg.worldPos)
    return {
      ...cfg,
      screenX: pos.x,
      screenY: pos.y,
      color: planet?.color || '#eab308',
      label: cfg.id.toUpperCase(),
      items: props.nodes.filter(n => n.type === cfg.type)
    }
  })
}

let _lastUpdate = 0
function animationLoop(ts) {
  if (!scene || scene.disposed) return
  animId = requestAnimationFrame(animationLoop)
  // Throttle screen position updates to ~15fps to reduce GC pressure
  if (ts - _lastUpdate > 66) {
    _lastUpdate = ts
    updateScreenPositions()
  }
}

onMounted(async () => {
  await nextTick()
  if (!canvasContainer.value) return

  try {
    const { default: StarshipScene } = await import('./StarshipScene')
    scene = new StarshipScene()
    await scene.init(canvasContainer.value)
    sceneReady.value = true
    animationLoop()
  } catch (e) {
    console.error('StarshipScene init failed:', e)
  }
})

onBeforeUnmount(() => {
  if (animId) cancelAnimationFrame(animId)
  if (scene) {
    scene.dispose()
    scene = null
  }
})
</script>

<style scoped>
.starship-mode {
  position: relative;
  width: 100%;
  height: 100vh;
  background: #0f0f0f;
  overflow: hidden;
}

/* Ambient glow orbs — matches reference: w-96 h-96 bg-[#eab308]/10 blur-[120px] */
.glow-orb {
  position: absolute;
  width: 384px;
  height: 384px;
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
}
.glow-yellow {
  top: 25%;
  left: 25%;
  background: rgba(234, 179, 8, 0.1);
  filter: blur(120px);
}
.glow-blue {
  bottom: 25%;
  right: 25%;
  background: rgba(14, 165, 233, 0.1);
  filter: blur(120px);
}

.starship-canvas {
  position: absolute;
  inset: 0;
  z-index: 1;
}

/* CRT scanline overlay — subtle retro effect */
.crt-overlay {
  position: absolute;
  inset: 0;
  opacity: 0.03;
  pointer-events: none;
  z-index: 5;
  background:
    linear-gradient(rgba(18, 16, 16, 0) 50%, rgba(0, 0, 0, 0.25) 50%),
    linear-gradient(90deg, rgba(255, 0, 0, 0.06), rgba(0, 255, 0, 0.02), rgba(0, 0, 255, 0.06));
  background-size: 100% 2px, 3px 100%;
}

.starship-overlay {
  position: absolute;
  inset: 0;
  z-index: 10;
  pointer-events: none;
}

/* Category buttons panel */
.category-panel {
  position: absolute;
  bottom: 60px;
  left: 20px;
  z-index: 20;
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 220px;
  padding: 8px;
  background: rgba(30, 30, 30, 0.7);
  backdrop-filter: blur(12px);
  border: 1px solid #333;
  border-radius: 12px;
}
.cat-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px solid transparent;
  background: rgba(30, 30, 30, 0.8);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  color: #e2e8f0;
  font-family: monospace;
  font-size: 11px;
}
.cat-btn:hover {
  background: #2a2a2a;
  border-color: #444;
}
.cat-btn.active {
  color: #111;
  font-weight: 700;
}
.cat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.cat-label {
  flex: 1;
  letter-spacing: 1px;
}
.cat-count {
  font-size: 10px;
  color: #94a3b8;
}
.cat-btn.active .cat-count {
  color: rgba(0, 0, 0, 0.5);
}

.starship-hint {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 20;
  font-size: 11px;
  font-family: monospace;
  color: #64748b;
  letter-spacing: 1px;
  pointer-events: none;
}

/* Ship title overlay */
.ship-title-overlay {
  position: absolute;
  top: 160px;
  left: 24px;
  z-index: 20;
  pointer-events: none;
  border-left: 3px solid #eab308;
  padding-left: 16px;
}
.ship-name {
  font-size: 36px;
  font-weight: 900;
  font-family: monospace;
  color: #e2e8f0;
  letter-spacing: 6px;
  line-height: 1;
  text-transform: uppercase;
}
.ship-class {
  font-size: 10px;
  font-family: monospace;
  color: #64748b;
  letter-spacing: 0.2em;
  margin-top: 4px;
  line-height: 1.6;
}
.ship-divider {
  width: 60px;
  height: 1px;
  background: rgba(234, 179, 8, 0.2);
  margin: 8px 0;
}
.ship-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 9px;
  font-family: monospace;
  color: rgba(16, 185, 129, 0.6);
  letter-spacing: 2px;
}
.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
  animation: blink 2s ease-in-out infinite;
}

/* Data summary panel — clipped corner like reference */
.data-summary {
  position: absolute;
  top: 50%;
  right: 24px;
  transform: translateY(-50%);
  z-index: 20;
  width: 180px;
  padding: 14px;
  background: rgba(10, 10, 10, 0.7);
  backdrop-filter: blur(12px);
  border: 1px solid #333;
  clip-path: polygon(0 0, 100% 0, 100% calc(100% - 16px), calc(100% - 16px) 100%, 0 100%);
  pointer-events: auto;
}
.summary-header {
  font-size: 10px;
  font-family: monospace;
  font-weight: 900;
  color: #94a3b8;
  letter-spacing: 2px;
  margin-bottom: 8px;
}
.summary-divider {
  height: 1px;
  background: #333;
  margin-bottom: 8px;
}
.summary-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 6px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 2px;
}
.summary-row:hover {
  background: rgba(255, 255, 255, 0.05);
}
.summary-row.active {
  background: rgba(234, 179, 8, 0.1);
}
.summary-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.summary-label {
  flex: 1;
  font-size: 10px;
  font-family: monospace;
  color: #e2e8f0;
  letter-spacing: 1px;
}
.summary-count {
  font-size: 10px;
  font-family: monospace;
  color: #64748b;
}

@keyframes blink {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
}

@media (max-width: 768px) {
  .category-panel {
    bottom: 50px;
    left: 8px;
    width: 180px;
    padding: 6px;
  }
  .cat-btn { padding: 6px 8px; font-size: 10px; }
  .starship-hint { bottom: 12px; font-size: 10px; }
  .glow-orb { width: 250px; height: 250px; }
  .ship-title-overlay { top: 140px; left: 12px; }
  .ship-name { font-size: 24px; letter-spacing: 4px; }
  .data-summary {
    display: none;
  }
}
</style>
