<template>
  <div class="cosmos-page" :class="mode">
    <!-- Loading overlay -->
    <Transition name="fade">
      <div v-if="loading" class="cosmos-loading">
        <div class="cosmos-loading-spinner"></div>
        <p>{{ $t('cosmos.loading') }}</p>
      </div>
    </Transition>

    <!-- Error state -->
    <Transition name="fade">
      <div v-if="loadError" class="cosmos-error">
        <p>{{ $t('cosmos.loadError') }}</p>
        <button class="cosmos-retry-btn" @click="retryLoad">{{ $t('cosmos.retry') }}</button>
      </div>
    </Transition>

    <!-- Mode switcher -->
    <ModeSwitcher v-model="mode" />

    <!-- RPG Panel -->
    <RPGPanel
      :level="levelInfo.level"
      :levelName="levelName"
      :currentXP="levelInfo.currentXP"
      :neededXP="levelInfo.neededXP"
      :percentage="levelInfo.percentage"
      :badgeCount="profile.badges.length"
      :stats="stats"
      :isMobile="isMobile"
      :todayXP="todayXP"
      :isCheckedIn="isCheckedIn"
      :mode="mode"
      @toggleAchievements="showAchievements = !showAchievements"
    />

    <!-- Alchemy Lab mode -->
    <Transition name="mode-slide">
      <AlchemyLab
        v-if="mode === 'alchemy' && !loading"
        key="alchemy"
        :nodes="nodes"
        :planets="planets"
        @selectNode="showContentCard"
      />
    </Transition>

    <!-- Starship mode -->
    <Transition name="mode-slide">
      <StarshipMode
        v-if="mode === 'starship' && !loading"
        key="starship"
        :nodes="nodes"
        :planets="planets"
        @selectNode="showContentCard"
      />
    </Transition>

    <!-- Content Card -->
    <ContentCard
      :node="selectedNode"
      @close="selectedNode = null"
    />

    <!-- Achievement Panel -->
    <AchievementPanel
      :visible="showAchievements"
      :unlocked="profile.badges"
      :profile="profile"
      @close="showAchievements = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { levelFromXP, getTier, recordVisit, checkBadges, loadProfile, saveProfile, BADGE_DEFS } from '../cosmos/CosmosLevels'
import { useCosmosData } from '../composables/useCosmosData'
import ModeSwitcher from '../cosmos/ModeSwitcher.vue'
import AlchemyLab from '../cosmos/AlchemyLab.vue'
import StarshipMode from '../cosmos/StarshipMode.vue'
import RPGPanel from '../components/cosmos/RPGPanel.vue'
import ContentCard from '../components/cosmos/ContentCard.vue'
import AchievementPanel from '../components/cosmos/AchievementPanel.vue'

const { t, tm } = useI18n()

const loading = ref(true)
const loadError = ref(false)
const mode = ref('alchemy')
const showAchievements = ref(false)
const selectedNode = ref(null)
const isMobile = ref(window.innerWidth < 768)

const { nodes, planets, stats, totalXP, fetchAll } = useCosmosData()

const profile = ref(loadProfile())
const contentXP = ref(0)

const levelInfo = computed(() => {
  return levelFromXP((profile.value.xp || 0) + (contentXP.value || 0))
})

const levelNames = computed(() => tm('cosmos.levelNames'))
const levelName = computed(() => {
  const names = levelNames.value
  const tier = getTier(levelInfo.value.level)
  return names[tier] || names[0] || ''
})

const isCheckedIn = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return Array.isArray(profile.value.visitDays) && profile.value.visitDays.includes(today)
})

const todayXP = computed(() => {
  return isCheckedIn.value ? 20 : 0
})

function computeTypesVisited() {
  if (!Array.isArray(profile.value.visitedNodes) || !nodes.value.length) return 0
  const types = new Set()
  for (const id of profile.value.visitedNodes) {
    const node = nodes.value.find(n => n.id === id)
    if (node && node.type) types.add(node.type)
  }
  return types.size
}

function computeComeback() {
  const days = profile.value.visitDays
  if (!Array.isArray(days) || days.length < 2) return false
  const sorted = [...days].sort()
  for (let i = 1; i < sorted.length; i++) {
    const prev = new Date(sorted[i - 1])
    const curr = new Date(sorted[i])
    const diff = (curr - prev) / (1000 * 60 * 60 * 24)
    if (diff >= 30) return true
  }
  return false
}

function printCosmosProfile() {
  const lvl = levelInfo.value
  const badgeCount = profile.value.badges?.length || 0
  const totalBadges = BADGE_DEFS.length
  const visitDays = profile.value.visitDays?.length || 0

  console.groupCollapsed(
    '%c  灵境档案 %c Level ' + lvl.level + ' ',
    'background: linear-gradient(135deg, #1a110b, #2d1f0e); color: #d4af37; font-size: 16px; font-weight: bold; padding: 6px 12px; border-radius: 6px 0 0 6px;',
    'background: #d4af37; color: #1a110b; font-size: 16px; font-weight: bold; padding: 6px 12px; border-radius: 0 6px 6px 0;'
  )
  console.log('%c等级: %c' + lvl.level + '%c / 30', 'color: #8b6b4a;', 'color: #d4af37; font-weight: bold;', 'color: #8b6b4a;')
  console.log('%c经验: %c' + lvl.currentXP + '%c / ' + lvl.neededXP + ' (总 ' + ((profile.value.xp || 0) + (contentXP.value || 0)) + ' XP)', 'color: #8b6b4a;', 'color: #e8e4d9; font-weight: bold;', 'color: #8b6b4a;')
  console.log('%c成就: %c' + badgeCount + '%c / ' + totalBadges, 'color: #8b6b4a;', 'color: #22c55e; font-weight: bold;', 'color: #8b6b4a;')
  console.log('%c访问: %c' + visitDays + ' 天', 'color: #8b6b4a;', 'color: #3b82f6; font-weight: bold;')
  console.log('%c─────────────────────────', 'color: #8b6b4a;')
  console.log('%c  欢迎来到灵境 ✨', 'color: #d4af37; font-size: 12px;')
  console.groupEnd()
}

function showContentCard(node) {
  profile.value.xp = (profile.value.xp || 0) + 1
  // Track visited nodes for exploration badges
  if (!Array.isArray(profile.value.visitedNodes)) profile.value.visitedNodes = []
  if (node && node.id && !profile.value.visitedNodes.includes(node.id)) {
    profile.value.visitedNodes.push(node.id)
  }
  saveProfile(profile.value)
  selectedNode.value = node
}

function handleResize() {
  isMobile.value = window.innerWidth < 768
}

async function retryLoad() {
  loadError.value = false
  loading.value = true
  try {
    await fetchAll()
    contentXP.value = totalXP.value
    stats.level = levelInfo.value.level
    checkBadges(profile.value, {
      ...stats,
      visitDays: Array.isArray(profile.value.visitDays) ? profile.value.visitDays.length : 0,
      uniqueNodesVisited: Array.isArray(profile.value.visitedNodes) ? profile.value.visitedNodes.length : 0,
      typesVisited: computeTypesVisited(),
      comeback: computeComeback()
    })
    saveProfile(profile.value)
  } catch (e) {
    console.error('Cosmos data load failed:', e)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  recordVisit(profile.value)

  try {
    await fetchAll()
    contentXP.value = totalXP.value
    stats.level = levelInfo.value.level
    checkBadges(profile.value, {
      ...stats,
      visitDays: Array.isArray(profile.value.visitDays) ? profile.value.visitDays.length : 0,
      uniqueNodesVisited: Array.isArray(profile.value.visitedNodes) ? profile.value.visitedNodes.length : 0,
      typesVisited: computeTypesVisited(),
      comeback: computeComeback()
    })
    saveProfile(profile.value)
    printCosmosProfile()
  } catch (e) {
    console.error('Cosmos data load failed:', e)
    loadError.value = true
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.cosmos-page {
  min-height: 100vh;
  width: 100%;
  overflow-x: hidden;
  transition: background-color 0.8s ease;
}
.cosmos-page.alchemy {
  background: #1a110b;
}
.cosmos-page.starship {
  background: #0f0f0f;
}

/* Loading */
.cosmos-loading {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #1a110b;
}
.cosmos-loading p {
  color: #d4af37;
  font-size: 14px;
  font-family: serif;
  letter-spacing: 2px;
}
.cosmos-loading-spinner {
  width: 40px;
  height: 40px;
  border: 2px solid rgba(212, 175, 55, 0.2);
  border-top-color: #d4af37;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Error state */
.cosmos-error {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #1a110b;
}
.cosmos-error p {
  color: #ef4444;
  font-size: 14px;
  font-family: serif;
  letter-spacing: 1px;
}
.cosmos-retry-btn {
  padding: 8px 24px;
  background: rgba(212, 175, 55, 0.15);
  border: 1px solid rgba(212, 175, 55, 0.4);
  color: #d4af37;
  font-family: serif;
  font-size: 13px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
}
.cosmos-retry-btn:hover {
  background: rgba(212, 175, 55, 0.25);
  border-color: #d4af37;
}

/* Transitions */
.fade-enter-active { transition: opacity 0.5s; }
.fade-leave-active { transition: opacity 0.8s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* Mode slide+blur transition (matches reference AnimatePresence mode="wait") */
.mode-slide-enter-active {
  transition: all 0.4s ease;
}
.mode-slide-leave-active {
  transition: all 0.3s ease;
}
.mode-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
  filter: blur(4px);
}
.mode-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px);
  filter: blur(4px);
}
</style>
