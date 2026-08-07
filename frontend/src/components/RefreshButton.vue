<template>
  <button class="refresh-btn" :class="{ spinning: loading }" @click="handleRefresh" :title="displayTitle">
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M21 2v6h-6"/><path d="M3 12a9 9 0 0 1 15-6.7L21 8"/>
      <path d="M3 22v-6h6"/><path d="M21 12a9 9 0 0 1-15 6.7L3 16"/>
    </svg>
  </button>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps({
  onRefresh: { type: Function, required: true },
  title: { type: String, default: '' }
})

const displayTitle = computed(() => props.title || t('common.refreshData'))

const loading = ref(false)

async function handleRefresh() {
  if (loading.value) return
  loading.value = true
  try {
    await props.onRefresh()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.refresh-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}
.refresh-btn:hover {
  border-color: #409eff;
  color: #409eff;
}
.refresh-btn.spinning svg {
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
