<template>
  <div class="ai-history-manager">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 历史记录 -->
      <el-tab-pane :label="$t('ai.history.history')" name="history">
        <div class="history-header">
          <el-input v-model="searchKeyword" :placeholder="$t('ai.history.search')" clearable size="small" style="width: 200px" />
          <el-button size="small" type="danger" text @click="handleClearHistory">
            <el-icon><Delete /></el-icon> {{ $t('ai.history.clearAll') }}
          </el-button>
        </div>
        <div class="history-list">
          <div v-if="filteredHistory.length === 0" class="empty-state">
            <p>{{ $t('ai.history.noHistory') }}</p>
          </div>
          <div v-for="item in filteredHistory" :key="item.id" class="history-item" @click="$emit('select', item)">
            <div class="history-meta">
              <el-tag size="small" :type="getTypeTag(item.type)">{{ getTypeLabel(item.type) }}</el-tag>
              <span class="history-time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <div class="history-prompt">{{ truncate(item.prompt, 100) }}</div>
            <div class="history-response">{{ truncate(item.response, 150) }}</div>
            <div class="history-actions">
              <el-button size="small" text :type="isFavorited(item.id) ? 'warning' : 'default'" @click.stop="toggleFavorite(item)">
                <el-icon><Star /></el-icon>
              </el-button>
              <el-button size="small" text type="info" @click.stop="$emit('reuse', item)">
                <el-icon><RefreshRight /></el-icon>
              </el-button>
              <el-button size="small" text type="danger" @click.stop="handleDeleteHistory(item.id)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 收藏 -->
      <el-tab-pane :label="$t('ai.history.favorites')" name="favorites">
        <div class="history-list">
          <div v-if="favorites.length === 0" class="empty-state">
            <p>{{ $t('ai.history.noFavorites') }}</p>
          </div>
          <div v-for="item in favorites" :key="item.id" class="history-item" @click="$emit('select', item)">
            <div class="history-meta">
              <el-tag size="small" :type="getTypeTag(item.type)">{{ getTypeLabel(item.type) }}</el-tag>
              <span class="history-time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <div class="history-prompt">{{ truncate(item.prompt, 100) }}</div>
            <div class="history-response">{{ truncate(item.response, 150) }}</div>
            <div v-if="item.note" class="history-note">
              <el-icon><Notebook /></el-icon> {{ item.note }}
            </div>
            <div class="history-actions">
              <el-button size="small" text type="info" @click.stop="editNote(item)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button size="small" text type="info" @click.stop="$emit('reuse', item)">
                <el-icon><RefreshRight /></el-icon>
              </el-button>
              <el-button size="small" text type="danger" @click.stop="handleDeleteFavorite(item.id)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 导入导出 -->
    <div class="import-export">
      <el-button size="small" text @click="handleExport">
        <el-icon><Download /></el-icon> {{ $t('ai.history.export') }}
      </el-button>
      <el-button size="small" text @click="triggerImport">
        <el-icon><Upload /></el-icon> {{ $t('ai.history.import') }}
      </el-button>
      <input ref="importInput" type="file" accept=".json" style="display: none" @change="handleImport" />
    </div>

    <!-- 备注编辑弹窗 -->
    <el-dialog v-model="showNoteDialog" :title="$t('ai.history.editNote')" width="400px">
      <el-input v-model="editingNote" type="textarea" :rows="3" :placeholder="$t('ai.history.notePlaceholder')" />
      <template #footer>
        <el-button @click="showNoteDialog = false">{{ $t('ai.history.cancel') }}</el-button>
        <el-button type="primary" @click="saveNote">{{ $t('ai.history.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { Delete, Star, RefreshRight, Download, Upload, Edit, Notebook } from '@element-plus/icons-vue'
import {
  getHistory,
  deleteHistory,
  clearHistory,
  getFavorites,
  addFavorite,
  deleteFavorite,
  updateFavoriteNote,
  isFavorited as checkFavorited,
  exportHistory,
  importHistory,
} from '../../ai/history/index'

const { t } = useI18n()
const emit = defineEmits(['select', 'reuse', 'update'])

const activeTab = ref('history')
const history = ref([])
const favorites = ref([])
const searchKeyword = ref('')
const importInput = ref(null)
const showNoteDialog = ref(false)
const editingNote = ref('')
const editingFavoriteId = ref(null)

const filteredHistory = computed(() => {
  if (!searchKeyword.value) return history.value
  const lower = searchKeyword.value.toLowerCase()
  return history.value.filter(h =>
    h.prompt.toLowerCase().includes(lower) ||
    h.response.toLowerCase().includes(lower)
  )
})

function loadData() {
  history.value = getHistory()
  favorites.value = getFavorites()
}

function getTypeTag(type) {
  const tags = {
    chat: '',
    preset: 'success',
    continue: 'warning',
    seo: 'info',
  }
  return tags[type] || ''
}

function getTypeLabel(type) {
  const labels = {
    chat: t('ai.history.typeChat'),
    preset: t('ai.history.typePreset'),
    continue: t('ai.history.typeContinue'),
    seo: t('ai.history.typeSeo'),
  }
  return labels[type] || type
}

function formatTime(isoString) {
  const date = new Date(isoString)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return t('ai.history.justNow')
  if (diff < 3600000) return t('ai.history.minutesAgo', { n: Math.floor(diff / 60000) })
  if (diff < 86400000) return t('ai.history.hoursAgo', { n: Math.floor(diff / 3600000) })

  return date.toLocaleDateString()
}

function truncate(text, max) {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '...' : text
}

function isFavorited(historyId) {
  return checkFavorited(historyId)
}

function toggleFavorite(item) {
  if (isFavorited(item.id)) {
    const fav = favorites.value.find(f => f.historyId === item.id)
    if (fav) {
      deleteFavorite(fav.id)
      ElMessage.success(t('ai.history.removedFromFavorites'))
    }
  } else {
    addFavorite(item)
    ElMessage.success(t('ai.history.addedToFavorites'))
  }
  loadData()
  emit('update')
}

function editNote(item) {
  editingFavoriteId.value = item.id
  editingNote.value = item.note || ''
  showNoteDialog.value = true
}

function saveNote() {
  if (editingFavoriteId.value) {
    updateFavoriteNote(editingFavoriteId.value, editingNote.value)
    showNoteDialog.value = false
    loadData()
    ElMessage.success(t('ai.history.noteSaved'))
  }
}

async function handleDeleteHistory(id) {
  try {
    await ElMessageBox.confirm(
      t('ai.history.confirmDelete'),
      t('ai.history.deleteTitle'),
      { type: 'warning' }
    )
    deleteHistory(id)
    loadData()
    emit('update')
    ElMessage.success(t('ai.history.deleted'))
  } catch {
    // cancelled
  }
}

async function handleClearHistory() {
  try {
    await ElMessageBox.confirm(
      t('ai.history.confirmClear'),
      t('ai.history.clearTitle'),
      { type: 'warning' }
    )
    clearHistory()
    loadData()
    emit('update')
    ElMessage.success(t('ai.history.cleared'))
  } catch {
    // cancelled
  }
}

async function handleDeleteFavorite(id) {
  try {
    await ElMessageBox.confirm(
      t('ai.history.confirmDeleteFavorite'),
      t('ai.history.deleteTitle'),
      { type: 'warning' }
    )
    deleteFavorite(id)
    loadData()
    emit('update')
    ElMessage.success(t('ai.history.deleted'))
  } catch {
    // cancelled
  }
}

function handleExport() {
  const json = exportHistory()
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'ai-history.json'
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success(t('ai.history.exported'))
}

function triggerImport() {
  importInput.value?.click()
}

function handleImport(e) {
  const file = e.target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (event) => {
    const result = importHistory(event.target.result)
    if (result.success) {
      ElMessage.success(t('ai.history.imported'))
      loadData()
      emit('update')
    } else {
      ElMessage.error(t('ai.history.importFailed', { error: result.error }))
    }
  }
  reader.readAsText(file)
  e.target.value = ''
}

onMounted(loadData)

defineExpose({ loadData })
</script>

<style scoped>
.ai-history-manager {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
}
.empty-state {
  text-align: center;
  padding: 24px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.history-item {
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.history-item:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.history-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.history-time {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.history-prompt {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 4px;
  color: var(--el-text-color-primary);
}
.history-response {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}
.history-note {
  font-size: 12px;
  color: var(--el-color-warning);
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.history-actions {
  display: flex;
  gap: 4px;
  margin-top: 8px;
  justify-content: flex-end;
}
.import-export {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
