<template>
  <Transition name="panel-slide">
    <div v-if="visible" class="cosmos-achieve-panel" @click.stop>
      <div class="cap-header">
        <div class="cap-header-left">
          <span class="cap-header-icon"> </span>
          <h3>{{ $t('cosmos.achievements') }}</h3>
        </div>
        <button class="cap-close" @click="$emit('close')">&times;</button>
      </div>

      <!-- Progress summary -->
      <div class="cap-summary">
        <div class="cap-progress-ring">
          <svg viewBox="0 0 48 48">
            <circle cx="24" cy="24" r="20" fill="none" stroke="rgba(139,107,74,0.2)" stroke-width="3" />
            <circle cx="24" cy="24" r="20" fill="none" stroke="#d4af37" stroke-width="3"
              stroke-linecap="round"
              :stroke-dasharray="progressCircumference"
              :stroke-dashoffset="progressOffset"
              transform="rotate(-90 24 24)" />
          </svg>
          <span class="cap-progress-text">{{ unlockedCount }}/{{ totalCount }}</span>
        </div>
        <div class="cap-summary-info">
          <span class="cap-summary-title">{{ $t('cosmos.codexTitle') }}</span>
          <span class="cap-summary-sub">{{ $t('cosmos.codexProgress', { pct: progressPct }) }}</span>
        </div>
      </div>

      <!-- Quick category overview -->
      <div class="cap-categories">
        <div
          v-for="cat in categoryStats"
          :key="cat.id"
          class="cap-cat-row"
          @click="openCodex(cat.id)"
        >
          <span class="cap-cat-icon">{{ cat.icon }}</span>
          <span class="cap-cat-name">{{ $t(`cosmos.${cat.i18nKey}`) }}</span>
          <span class="cap-cat-count">{{ cat.unlocked }}/{{ cat.total }}</span>
          <div class="cap-cat-bar">
            <div class="cap-cat-bar-fill" :style="{ width: cat.pct + '%' }"></div>
          </div>
        </div>
      </div>

      <div class="cap-scroll">
        <!-- Recent unlocks -->
        <div class="cap-section-title">{{ $t('cosmos.recentUnlocks') }}</div>
        <div class="cap-recent-grid">
          <div
            v-for="badge in recentBadges"
            :key="badge.id"
            class="cap-badge-item"
          >
            <HexBadge :badge="badge" :unlocked="true" :tier="tier" />
            <span class="cap-badge-name">{{ $t(`cosmos.badges.${badge.id}.name`) }}</span>
          </div>
        </div>
        <div v-if="recentBadges.length === 0" class="cap-empty">
          {{ $t('cosmos.noUnlocks') }}
        </div>
      </div>

      <!-- Open codex button -->
      <button class="cap-codex-btn" @click="openCodex(null)">
        <span> </span>
        <span>{{ $t('cosmos.openCodex') }}</span>
      </button>

      <div class="cap-accent"></div>
    </div>
  </Transition>

  <!-- Full-screen Codex Modal -->
  <Transition name="codex-fade">
    <div v-if="showCodex" class="codex-overlay" @click.self="showCodex = false" @keydown.escape.window="showCodex = false">
      <div class="codex-modal">
        <div class="codex-header">
          <div class="codex-header-left">
            <span class="codex-title-icon"> </span>
            <h2>{{ $t('cosmos.memoryCodex') }}</h2>
            <span class="codex-count">{{ unlockedCount }}/{{ totalCount }}</span>
          </div>
          <button class="codex-close" @click="showCodex = false">&times;</button>
        </div>

        <!-- Category tabs -->
        <div class="codex-tabs">
          <button
            class="codex-tab"
            :class="{ active: codexTab === 'all' }"
            @click="codexTab = 'all'"
          >
            {{ $t('cosmos.codexAll') }}
            <span class="tab-count">{{ totalCount }}</span>
          </button>
          <button
            v-for="cat in BADGE_CATEGORIES"
            :key="cat.id"
            class="codex-tab"
            :class="{ active: codexTab === cat.id }"
            @click="codexTab = cat.id"
          >
            {{ cat.icon }} {{ $t(`cosmos.${cat.i18nKey}`) }}
            <span class="tab-count">{{ getCategoryTotal(cat.id) }}</span>
          </button>
        </div>

        <!-- Rarity filter -->
        <div class="codex-rarity-filter">
          <button
            class="rarity-btn"
            :class="{ active: rarityFilter === 'all' }"
            @click="rarityFilter = 'all'"
          >{{ $t('cosmos.codexAll') }}</button>
          <button
            v-for="(cfg, key) in RARITY_CONFIG"
            :key="key"
            class="rarity-btn"
            :class="{ active: rarityFilter === key }"
            :style="rarityFilter === key ? { borderColor: cfg.color, color: cfg.color } : {}"
            @click="rarityFilter = key"
          >
            {{ $t(`cosmos.${cfg.i18nKey}`) }}
            <span class="rarity-count">{{ getRarityCount(key) }}</span>
          </button>
        </div>

        <!-- Badge grid -->
        <div ref="codexBody" class="codex-body">
          <template v-for="cat in visibleCategories" :key="cat.id">
            <div class="codex-category-header">
              <span class="codex-cat-icon">{{ cat.icon }}</span>
              <span class="codex-cat-name">{{ $t(`cosmos.${cat.i18nKey}`) }}</span>
              <span class="codex-cat-divider"></span>
              <span class="codex-cat-progress">{{ getCategoryUnlocked(cat.id) }}/{{ getCategoryTotal(cat.id) }}</span>
            </div>
            <div class="codex-grid">
              <div
                v-for="item in getDisplayBadges(cat.id)"
                :key="item.badge.id"
                class="codex-badge-item"
              >
                <HexBadge
                  :badge="item.badge"
                  :unlocked="item.unlocked"
                  :tier="tier"
                  :evolution="item.evolution"
                />
                <span class="codex-badge-name" :style="item.unlocked ? { color: getRarityColor(item.badge.rarity) } : {}">
                  {{ $t(`cosmos.badges.${item.badge.id}.name`) }}
                </span>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch, nextTick, onUnmounted } from 'vue'
import { BADGE_DEFS, BADGE_CATEGORIES, RARITY_CONFIG, getTier, levelFromXP, getEvolutionInfo, getHighestInChain } from '../../cosmos/CosmosLevels'
import HexBadge from './HexBadge.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  unlocked: { type: Array, default: () => [] },
  profile: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['close'])

const showCodex = ref(false)
const codexTab = ref('all')
const rarityFilter = ref('all')
const codexBody = ref(null)

// Lock body scroll when codex is open
watch(showCodex, (open) => {
  if (open) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})
onUnmounted(() => { document.body.style.overflow = '' })

// Reset scroll position on tab/rarity switch
watch([codexTab, rarityFilter], () => {
  nextTick(() => {
    if (codexBody.value) codexBody.value.scrollTop = 0
  })
})

const tier = computed(() => {
  const lvl = levelFromXP(props.profile?.xp || 0).level
  return getTier(lvl)
})

const totalCount = BADGE_DEFS.length
const unlockedCount = computed(() => props.unlocked.length)
const progressPct = computed(() => totalCount > 0 ? Math.round((unlockedCount.value / totalCount) * 100) : 0)
const progressCircumference = 2 * Math.PI * 20
const progressOffset = computed(() => progressCircumference * (1 - progressPct.value / 100))

function isUnlocked(id) {
  return props.unlocked.includes(id)
}

// Category stats for sidebar
const categoryStats = computed(() =>
  BADGE_CATEGORIES.map(cat => {
    const total = BADGE_DEFS.filter(b => b.group === cat.id).length
    const unlocked = BADGE_DEFS.filter(b => b.group === cat.id && isUnlocked(b.id)).length
    return {
      ...cat,
      total,
      unlocked,
      pct: total > 0 ? Math.round((unlocked / total) * 100) : 0
    }
  })
)

// Recent unlocked badges (last 6)
const recentBadges = computed(() => {
  const ids = props.unlocked.slice(-6).reverse()
  return ids.map(id => BADGE_DEFS.find(b => b.id === id)).filter(Boolean)
})

// Pre-computed category → badges map
const badgesByCategory = new Map()
for (const cat of BADGE_CATEGORIES) {
  badgesByCategory.set(cat.id, BADGE_DEFS.filter(b => b.group === cat.id))
}

function getCategoryTotal(catId) {
  return badgesByCategory.get(catId)?.length || 0
}
function getCategoryUnlocked(catId) {
  return (badgesByCategory.get(catId) || []).filter(b => isUnlocked(b.id)).length
}

function getRarityCount(rarity) {
  return BADGE_DEFS.filter(b => b.rarity === rarity).length
}
function getRarityColor(rarity) {
  return RARITY_CONFIG[rarity]?.color || '#9ca3af'
}

const visibleCategories = computed(() => {
  if (codexTab.value === 'all') return BADGE_CATEGORIES
  return BADGE_CATEGORIES.filter(c => c.id === codexTab.value)
})

// Main display logic: merge evolution chains + apply rarity filter
function getDisplayBadges(catId) {
  const catBadges = badgesByCategory.get(catId) || []
  const result = []

  // Group badges by chain
  const chainGroups = {}
  const standalone = []
  for (const badge of catBadges) {
    if (badge.chain) {
      if (!chainGroups[badge.chain]) chainGroups[badge.chain] = []
      chainGroups[badge.chain].push(badge)
    } else {
      standalone.push(badge)
    }
  }

  // For each chain, show only the highest unlocked (or the first locked if none unlocked)
  for (const [chainId, chainBadges] of Object.entries(chainGroups)) {
    const highest = getHighestInChain(chainId, props.unlocked)
    if (highest) {
      // Show the highest unlocked badge with evolution info
      const badge = BADGE_DEFS.find(b => b.id === highest)
      if (badge) {
        const evo = getEvolutionInfo(highest, props.unlocked)
        // Check if this badge is "evolved" (absorbed lower badges)
        const absorbedCount = chainBadges.filter(b => isUnlocked(b.id) && b.id !== highest).length
        const evolution = absorbedCount > 0 ? { ...evo, absorbed: absorbedCount } : evo
        result.push({ badge, unlocked: true, evolution })
      }
    } else {
      // No badge unlocked in chain — show the first one (locked)
      const badge = chainBadges[0]
      const evo = { current: 1, total: chainBadges.length, nextId: badge.id, chainId }
      result.push({ badge, unlocked: false, evolution: evo })
    }
  }

  // Standalone badges
  for (const badge of standalone) {
    result.push({ badge, unlocked: isUnlocked(badge.id), evolution: null })
  }

  // Apply rarity filter
  if (rarityFilter.value !== 'all') {
    return result.filter(item => item.badge.rarity === rarityFilter.value)
  }
  return result
}

function openCodex(catId) {
  codexTab.value = catId || 'all'
  showCodex.value = true
  emit('close') // close sidebar when opening codex
}
</script>

<style scoped>
/* ─── Sidebar Panel ─── */
.cosmos-achieve-panel {
  position: fixed;
  top: 0;
  right: 0;
  width: 360px;
  height: 100vh;
  background: rgba(26, 17, 11, 0.95);
  backdrop-filter: blur(16px);
  border-left: 1px solid rgba(212, 175, 55, 0.3);
  z-index: 30;
  display: flex;
  flex-direction: column;
  pointer-events: auto;
  font-family: serif;
}
.cap-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 16px 12px;
  border-bottom: 1px solid rgba(139, 107, 74, 0.3);
  flex-shrink: 0;
}
.cap-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.cap-header-icon { font-size: 18px; }
.cap-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 900;
  color: #d4af37;
  letter-spacing: 2px;
}
.cap-close {
  width: 28px;
  height: 28px;
  border: 1px solid rgba(139, 107, 74, 0.4);
  background: rgba(26, 17, 11, 0.8);
  color: #8b6b4a;
  font-size: 18px;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.cap-close:hover { background: rgba(139, 107, 74, 0.3); color: #d4af37; }

/* Progress summary */
.cap-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(139, 107, 74, 0.15);
  flex-shrink: 0;
}
.cap-progress-ring {
  position: relative;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}
.cap-progress-ring svg {
  width: 100%;
  height: 100%;
}
.cap-progress-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-family: monospace;
  color: #d4af37;
  font-weight: 700;
}
.cap-summary-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cap-summary-title {
  font-size: 13px;
  color: #e8e4d9;
  font-weight: 700;
}
.cap-summary-sub {
  font-size: 11px;
  color: #8b6b4a;
}

/* Category rows */
.cap-categories {
  padding: 10px 16px;
  border-bottom: 1px solid rgba(139, 107, 74, 0.15);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.cap-cat-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.cap-cat-row:hover { background: rgba(139, 107, 74, 0.15); }
.cap-cat-icon { font-size: 14px; width: 20px; text-align: center; }
.cap-cat-name { flex: 1; font-size: 12px; color: #e8e4d9; }
.cap-cat-count { font-size: 10px; font-family: monospace; color: #8b6b4a; width: 36px; text-align: right; }
.cap-cat-bar {
  width: 40px;
  height: 3px;
  background: rgba(139, 107, 74, 0.2);
  border-radius: 2px;
  overflow: hidden;
}
.cap-cat-bar-fill {
  height: 100%;
  background: #d4af37;
  border-radius: 2px;
  transition: width 0.5s ease;
}

/* Scroll area */
.cap-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px;
}
.cap-scroll::-webkit-scrollbar { width: 4px; }
.cap-scroll::-webkit-scrollbar-track { background: transparent; }
.cap-scroll::-webkit-scrollbar-thumb { background: rgba(234, 179, 8, 0.5); }

.cap-section-title {
  font-size: 11px;
  color: #8b6b4a;
  letter-spacing: 2px;
  text-transform: uppercase;
  margin-bottom: 12px;
}
.cap-recent-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  justify-items: center;
}
.cap-badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.cap-badge-name {
  font-size: 10px;
  color: #8b6b4a;
  text-align: center;
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cap-empty {
  text-align: center;
  padding: 30px 0;
  font-size: 12px;
  color: #6b5b3e;
  font-style: italic;
}

/* Open codex button */
.cap-codex-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 0 16px 12px;
  padding: 10px;
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.15), rgba(184, 134, 11, 0.1));
  border: 1px solid rgba(212, 175, 55, 0.4);
  color: #d4af37;
  font-family: serif;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
  flex-shrink: 0;
}
.cap-codex-btn:hover {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.25), rgba(184, 134, 11, 0.2));
  box-shadow: 0 0 15px rgba(212, 175, 55, 0.15);
}

.cap-accent {
  flex-shrink: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212, 175, 55, 0.3), transparent);
}

/* ─── Codex Modal ─── */
.codex-overlay {
  position: fixed;
  inset: 0;
  z-index: 99;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: auto;
  overflow: hidden;
}
.codex-modal {
  width: 90vw;
  max-width: 860px;
  height: 85vh;
  background: rgba(26, 17, 11, 0.97);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.8), 0 0 40px rgba(212, 175, 55, 0.05);
}
.codex-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(139, 107, 74, 0.3);
  flex-shrink: 0;
}
.codex-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.codex-title-icon { font-size: 22px; }
.codex-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 900;
  color: #d4af37;
  letter-spacing: 3px;
}
.codex-count {
  font-size: 12px;
  font-family: monospace;
  color: #8b6b4a;
  background: rgba(139, 107, 74, 0.2);
  padding: 2px 8px;
  border-radius: 9999px;
}
.codex-close {
  width: 32px;
  height: 32px;
  border: 1px solid rgba(139, 107, 74, 0.4);
  background: rgba(26, 17, 11, 0.8);
  color: #8b6b4a;
  font-size: 20px;
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.codex-close:hover { background: rgba(139, 107, 74, 0.3); color: #d4af37; }

/* Tabs */
.codex-tabs {
  display: flex;
  gap: 2px;
  padding: 10px 24px;
  border-bottom: 1px solid rgba(139, 107, 74, 0.15);
  overflow-x: auto;
  flex-shrink: 0;
}
.codex-tabs::-webkit-scrollbar { height: 0; }
.codex-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid transparent;
  background: transparent;
  color: #8b6b4a;
  font-family: serif;
  font-size: 12px;
  cursor: pointer;
  border-radius: 8px;
  white-space: nowrap;
  transition: all 0.2s;
}
.codex-tab:hover { background: rgba(139, 107, 74, 0.1); color: #e8e4d9; }
.codex-tab.active {
  background: rgba(212, 175, 55, 0.15);
  border-color: rgba(212, 175, 55, 0.4);
  color: #d4af37;
  font-weight: 700;
}
.tab-count {
  font-size: 10px;
  font-family: monospace;
  opacity: 0.6;
}

/* Rarity filter */
.codex-rarity-filter {
  display: flex;
  gap: 4px;
  padding: 8px 24px;
  border-bottom: 1px solid rgba(139, 107, 74, 0.15);
  overflow-x: auto;
  flex-shrink: 0;
}
.codex-rarity-filter::-webkit-scrollbar { height: 0; }
.rarity-btn {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 4px 10px;
  border: 1px solid rgba(139, 107, 74, 0.2);
  background: transparent;
  color: #8b6b4a;
  font-family: serif;
  font-size: 11px;
  cursor: pointer;
  border-radius: 6px;
  white-space: nowrap;
  transition: all 0.2s;
}
.rarity-btn:hover { background: rgba(139, 107, 74, 0.1); color: #e8e4d9; }
.rarity-btn.active {
  background: rgba(212, 175, 55, 0.1);
  font-weight: 700;
}
.rarity-count {
  font-size: 9px;
  font-family: monospace;
  opacity: 0.5;
}

/* Body */
.codex-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}
.codex-body::-webkit-scrollbar { width: 4px; }
.codex-body::-webkit-scrollbar-track { background: transparent; }
.codex-body::-webkit-scrollbar-thumb { background: rgba(234, 179, 8, 0.5); }

.codex-category-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 20px 0 14px;
}
.codex-category-header:first-child { margin-top: 0; }
.codex-cat-icon { font-size: 16px; }
.codex-cat-name {
  font-size: 13px;
  font-weight: 700;
  color: #e8e4d9;
  letter-spacing: 2px;
}
.codex-cat-divider {
  flex: 1;
  height: 1px;
  background: rgba(139, 107, 74, 0.2);
}
.codex-cat-progress {
  font-size: 11px;
  font-family: monospace;
  color: #8b6b4a;
}

.codex-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 20px 12px;
  justify-items: center;
}
.codex-badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.codex-badge-name {
  font-size: 10px;
  color: #8b6b4a;
  text-align: center;
  max-width: 76px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.3;
}

/* Transitions */
.panel-slide-enter-active { transition: transform 0.3s ease; }
.panel-slide-leave-active { transition: transform 0.2s ease; }
.panel-slide-enter-from { transform: translateX(100%); }
.panel-slide-leave-to { transform: translateX(100%); }

.codex-fade-enter-active { transition: opacity 0.3s ease; }
.codex-fade-leave-active { transition: opacity 0.2s ease; }
.codex-fade-enter-from, .codex-fade-leave-to { opacity: 0; }

@media (max-width: 768px) {
  .cosmos-achieve-panel { width: 100%; }
  .codex-modal {
    width: 100vw;
    height: 100vh;
    border-radius: 0;
  }
  .codex-grid {
    grid-template-columns: repeat(auto-fill, minmax(68px, 1fr));
    gap: 14px 8px;
  }
  .codex-rarity-filter {
    padding: 8px 16px;
  }
  .rarity-btn {
    padding: 3px 8px;
    font-size: 10px;
  }
}
</style>
