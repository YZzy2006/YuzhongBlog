<template>
  <Teleport to="body">
    <Transition name="shelf-detail">
      <div v-if="visible" class="shelf-detail-overlay" @click.self="$emit('close')">
        <div class="shelf-detail-panel">
          <!-- Header -->
          <div class="detail-header">
            <div class="detail-header-left">
              <span class="detail-dot" :style="{ background: color }"></span>
              <h3 class="detail-title">{{ label }}</h3>
              <span class="detail-count">{{ filteredItems.length }}</span>
            </div>
            <button class="detail-close" @click="$emit('close')">&times;</button>
          </div>

          <div class="detail-divider" :style="{ background: `linear-gradient(90deg, ${color}, transparent)` }"></div>

          <!-- Type filter (for announcements) -->
          <div v-if="typeFilters.length > 1" class="detail-filter-bar">
            <button
              class="filter-btn"
              :class="{ active: !activeTypeFilter }"
              @click="activeTypeFilter = null"
            >{{ $t('cosmos.filterAll') }}</button>
            <button
              v-for="ft in typeFilters"
              :key="ft.value"
              class="filter-btn"
              :class="{ active: activeTypeFilter === ft.value }"
              @click="activeTypeFilter = ft.value"
            >{{ ft.label }}</button>
          </div>

          <!-- Sort indicator -->
          <div class="detail-sort-bar">
            <span class="sort-label">{{ $t('cosmos.sortedByDate') }}</span>
          </div>

          <!-- Items list -->
          <div class="detail-list">
            <div
              v-for="(item, i) in filteredItems"
              :key="item.id || i"
              class="detail-item"
              :style="{ '--item-delay': (i * 0.04) + 's' }"
              @click="handleClick(item)"
            >
              <div class="item-index" :style="{ color }">{{ String(i + 1).padStart(2, '0') }}</div>
              <div class="item-body">
                <div class="item-title">{{ item.title || '' }}</div>
                <div class="item-meta">
                  <span v-if="item.date" class="item-date">{{ formatDate(item.date) }}</span>
                  <span v-if="item.apiTag" class="item-api-tag" :style="{ borderColor: color + '40' }">{{ item.apiTag }}</span>
                  <span v-if="item.subtitle" class="item-subtitle">{{ item.subtitle }}</span>
                </div>
                <div v-if="item.tags && item.tags.length" class="item-tags">
                  <span v-for="tag in item.tags.slice(0, 4)" :key="tag" class="item-tag">{{ tag }}</span>
                </div>
              </div>
              <div class="item-arrow" :style="{ color }">&#8250;</div>
            </div>

            <div v-if="!filteredItems.length" class="detail-empty">
              {{ $t('cosmos.noItems') }}
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  visible: { type: Boolean, default: false },
  items: { type: Array, default: () => [] },
  color: { type: String, default: '#d4af37' },
  label: { type: String, default: '' },
  type: { type: String, default: '' }
})

defineEmits(['close'])

const router = useRouter()
const { t } = useI18n()
const activeTypeFilter = ref(null)

// Reset filter when items change
watch(() => props.items, () => { activeTypeFilter.value = null })

// Extract unique apiType values for filter buttons
const typeFilters = computed(() => {
  if (props.type !== 'notice') return []
  const map = new Map()
  for (const item of props.items) {
    const val = item.apiType
    if (val && !map.has(val)) {
      map.set(val, { value: val, label: t(`cosmos.noticeType.${val}`, val) })
    }
  }
  return [...map.values()]
})

// Extract numeric suffix from id (e.g. "notices-3" → 3)
function extractNumericId(id) {
  if (typeof id === 'number') return id
  const m = String(id).match(/(\d+)$/)
  return m ? parseInt(m[1]) : 0
}

// Sort by date descending, then filter by type
const filteredItems = computed(() => {
  let list = [...props.items].sort((a, b) => {
    const da = a.date ? new Date(a.date).getTime() : 0
    const db = b.date ? new Date(b.date).getTime() : 0
    if (db !== da) return db - da
    return extractNumericId(b.id) - extractNumericId(a.id)
  })
  if (activeTypeFilter.value) {
    list = list.filter(item => item.apiType === activeTypeFilter.value)
  }
  return list
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d)) return ''
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function handleClick(item) {
  if (item.link) {
    router.push(item.link)
  }
}
</script>

<style scoped>
.shelf-detail-overlay {
  position: fixed;
  inset: 0;
  z-index: 9000;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.shelf-detail-panel {
  width: 100%;
  max-width: 560px;
  max-height: 80vh;
  background: #1a110b;
  border: 1px solid rgba(139, 107, 74, 0.3);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.8);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Header */
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 12px;
}
.detail-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.detail-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.detail-title {
  font-family: serif;
  font-size: 18px;
  font-weight: 700;
  color: #d4af37;
  letter-spacing: 2px;
  margin: 0;
}
.detail-count {
  font-family: monospace;
  font-size: 11px;
  color: #6b5b3e;
  background: rgba(139, 107, 74, 0.2);
  padding: 1px 8px;
  border-radius: 9999px;
}
.detail-close {
  background: none;
  border: none;
  color: #6b5b3e;
  font-size: 24px;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
  transition: color 0.2s;
}
.detail-close:hover {
  color: #d4af37;
}

.detail-divider {
  height: 1px;
  margin: 0 20px;
}

/* Type filter bar */
.detail-filter-bar {
  display: flex;
  gap: 6px;
  padding: 10px 20px 4px;
  flex-wrap: wrap;
}
.filter-btn {
  font-family: monospace;
  font-size: 10px;
  letter-spacing: 1px;
  padding: 3px 10px;
  border: 1px solid rgba(139, 107, 74, 0.3);
  border-radius: 9999px;
  background: transparent;
  color: #8b6b4a;
  cursor: pointer;
  transition: all 0.2s;
}
.filter-btn:hover {
  border-color: rgba(139, 107, 74, 0.6);
  color: #d4af37;
}
.filter-btn.active {
  background: rgba(212, 175, 55, 0.15);
  border-color: #d4af37;
  color: #d4af37;
}

/* Sort bar */
.detail-sort-bar {
  padding: 8px 20px;
}
.sort-label {
  font-family: monospace;
  font-size: 10px;
  color: #6b5b3e;
  letter-spacing: 1px;
  text-transform: uppercase;
}

/* List */
.detail-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 16px;
  scrollbar-width: thin;
  scrollbar-color: rgba(139, 107, 74, 0.3) transparent;
}
.detail-list::-webkit-scrollbar {
  width: 4px;
}
.detail-list::-webkit-scrollbar-thumb {
  background: rgba(139, 107, 74, 0.3);
  border-radius: 2px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  animation: fadeInUp 0.3s ease both;
  animation-delay: var(--item-delay, 0s);
}
.detail-item:hover {
  background: rgba(139, 107, 74, 0.1);
}

.item-index {
  font-family: monospace;
  font-size: 12px;
  font-weight: 700;
  opacity: 0.5;
  flex-shrink: 0;
  width: 24px;
  text-align: right;
}

.item-body {
  flex: 1;
  min-width: 0;
}
.item-title {
  font-family: serif;
  font-size: 14px;
  font-weight: 600;
  color: #e8e4d9;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}
.item-date {
  font-family: monospace;
  font-size: 10px;
  color: #6b5b3e;
}
.item-api-tag {
  font-family: monospace;
  font-size: 9px;
  color: #8b6b4a;
  border: 1px solid;
  padding: 0 5px;
  border-radius: 3px;
  line-height: 1.5;
}
.item-subtitle {
  font-size: 11px;
  color: #8b6b4a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-tags {
  display: flex;
  gap: 4px;
  margin-top: 4px;
  flex-wrap: wrap;
}
.item-tag {
  font-family: monospace;
  font-size: 9px;
  color: #8b6b4a;
  background: rgba(139, 107, 74, 0.15);
  padding: 1px 6px;
  border-radius: 3px;
}

.item-arrow {
  font-size: 20px;
  opacity: 0.3;
  flex-shrink: 0;
  transition: opacity 0.2s;
}
.detail-item:hover .item-arrow {
  opacity: 0.8;
}

.detail-empty {
  text-align: center;
  padding: 40px 20px;
  font-family: serif;
  font-size: 14px;
  color: #6b5b3e;
}

/* Transition */
.shelf-detail-enter-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.shelf-detail-leave-active {
  transition: all 0.2s ease;
}
.shelf-detail-enter-from {
  opacity: 0;
}
.shelf-detail-leave-to {
  opacity: 0;
}
.shelf-detail-enter-from .shelf-detail-panel {
  transform: translateY(30px) scale(0.95);
}
.shelf-detail-leave-to .shelf-detail-panel {
  transform: translateY(10px) scale(0.98);
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 600px) {
  .shelf-detail-panel {
    max-height: 85vh;
    border-radius: 8px;
  }
  .detail-title { font-size: 16px; }
  .item-title { font-size: 13px; }
  .filter-btn { font-size: 9px; padding: 2px 8px; }
}
</style>
