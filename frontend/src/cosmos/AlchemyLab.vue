<template>
  <div class="alchemy-lab">
    <div class="alchemy-header">
      <div class="alchemy-ornament">
        <span class="ornament-line"></span>
        <span class="ornament-symbol">&#9830;</span>
        <span class="ornament-line"></span>
      </div>
      <h2 class="alchemy-title">{{ $t('cosmos.alchemyLab') }}</h2>
      <p class="alchemy-subtitle">{{ $t('cosmos.subtitle') }}</p>
    </div>

    <MonthNav
      v-if="months.length > 1"
      :months="months"
      v-model="selectedMonth"
    />

    <div class="alchemy-shelves">
      <WoodenShelf
        v-for="(shelf, i) in shelves"
        :key="shelf.type"
        :items="shelf.items"
        :color="shelf.color"
        :label="shelf.label"
        :type="shelf.type"
        :shelfIndex="i"
        @selectNode="$emit('selectNode', $event)"
        @showDetail="detailShelfType = shelf.type"
      />
    </div>

    <ShelfDetail
      v-if="detailShelf"
      :visible="!!detailShelfType"
      :items="detailShelf.items"
      :color="detailShelf.color"
      :label="detailShelf.label"
      :type="detailShelf.type"
      @close="detailShelfType = null"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import WoodenShelf from '../components/cosmos/WoodenShelf.vue'
import MonthNav from '../components/cosmos/MonthNav.vue'
import ShelfDetail from '../components/cosmos/ShelfDetail.vue'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  planets: { type: Array, default: () => [] }
})

defineEmits(['selectNode'])

const { t } = useI18n()

const SHELF_TYPES = [
  { type: 'article', planetId: 'articles', i18nKey: 'shelfArticles' },
  { type: 'project', planetId: 'projects', i18nKey: 'shelfProjects' },
  { type: 'moment', planetId: 'moments', i18nKey: 'shelfMoments' },
  { type: 'album', planetId: 'albums', i18nKey: 'shelfAlbums' },
  { type: 'song', planetId: 'songs', i18nKey: 'shelfSongs' },
  { type: 'notice', planetId: 'notices', i18nKey: 'shelfNotices' }
]

const selectedMonth = ref(null)
const detailShelfType = ref(null)

const detailShelf = computed(() => {
  if (!detailShelfType.value) return null
  return shelves.value.find(s => s.type === detailShelfType.value) || null
})

const months = computed(() => {
  const set = new Set()
  for (const n of props.nodes) {
    if (!n.date) continue
    const d = new Date(n.date)
    if (!isNaN(d)) set.add(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return [...set].sort().reverse()
})

const filteredNodes = computed(() => {
  if (!selectedMonth.value) return props.nodes
  return props.nodes.filter(n => {
    if (!n.date) return true
    const d = new Date(n.date)
    if (isNaN(d)) return true
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}` === selectedMonth.value
  })
})

const shelves = computed(() =>
  SHELF_TYPES.map(st => {
    const planet = props.planets.find(p => p.id === st.planetId)
    return {
      type: st.type,
      label: t(`cosmos.${st.i18nKey}`),
      color: planet?.color || '#6b7280',
      items: filteredNodes.value.filter(n => n.type === st.type)
    }
  }).filter(s => s.items.length > 0)
)
</script>

<style scoped>
.alchemy-lab {
  min-height: 100vh;
  background: #1a110b;
  padding: 80px 24px 60px;
  max-width: 900px;
  margin: 0 auto;
}

.alchemy-header {
  text-align: center;
  margin-bottom: 32px;
  animation: fadeInUp 0.6s ease both;
}
.alchemy-ornament {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
}
.ornament-line {
  width: 40px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212, 175, 55, 0.4));
}
.ornament-line:last-child {
  background: linear-gradient(90deg, rgba(212, 175, 55, 0.4), transparent);
}
.ornament-symbol {
  color: #d4af37;
  font-size: 10px;
  opacity: 0.6;
}
.alchemy-title {
  font-family: serif;
  font-size: 24px;
  font-weight: 700;
  color: #d4af37;
  letter-spacing: 4px;
  margin: 0 0 8px;
  text-shadow: 0 0 20px rgba(212, 175, 55, 0.15);
}
.alchemy-subtitle {
  font-family: serif;
  font-size: 13px;
  color: #8b6b4a;
  margin: 0;
  letter-spacing: 1px;
}

.alchemy-shelves {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .alchemy-lab { padding: 70px 12px 40px; }
  .alchemy-title { font-size: 20px; }
}
</style>
