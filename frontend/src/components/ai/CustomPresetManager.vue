<template>
  <div class="custom-preset-manager">
    <!-- 预设列表 -->
    <div class="preset-list">
      <div v-if="presets.length === 0" class="empty-state">
        <p>{{ $t('ai.custom.noPresets') }}</p>
      </div>
      <div v-for="preset in presets" :key="preset.id" class="preset-item" @click="$emit('select', preset)">
        <span class="preset-icon">{{ preset.icon }}</span>
        <div class="preset-info">
          <span class="preset-name">{{ preset.name }}</span>
          <span class="preset-category">{{ getCategoryLabel(preset.category) }}</span>
        </div>
        <div class="preset-actions">
          <el-button size="small" text @click.stop="editPreset(preset)">
            <el-icon><Edit /></el-icon>
          </el-button>
          <el-button size="small" text type="danger" @click.stop="deletePreset(preset)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 添加按钮 -->
    <el-button class="add-btn" @click="showEditor = true">
      <el-icon><Plus /></el-icon>
      {{ $t('ai.custom.addPreset') }}
    </el-button>

    <!-- 编辑器弹窗 -->
    <el-dialog v-model="showEditor" :title="editingId ? $t('ai.custom.editPreset') : $t('ai.custom.addPreset')" width="480px">
      <el-form :model="editorForm" label-position="top">
        <el-form-item :label="$t('ai.custom.name')" required>
          <el-input v-model="editorForm.name" :placeholder="$t('ai.custom.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('ai.custom.icon')">
          <div class="icon-selector">
            <span v-for="icon in iconOptions" :key="icon"
              class="icon-option" :class="{ active: editorForm.icon === icon }"
              @click="editorForm.icon = icon">
              {{ icon }}
            </span>
          </div>
        </el-form-item>
        <el-form-item :label="$t('ai.custom.prompt')" required>
          <el-input v-model="editorForm.prompt" type="textarea" :rows="4"
            :placeholder="$t('ai.custom.promptPlaceholder')" />
          <div class="prompt-hint">{{ $t('ai.custom.promptHint') }}</div>
        </el-form-item>
        <el-form-item :label="$t('ai.custom.category')">
          <el-select v-model="editorForm.category" style="width: 100%">
            <el-option v-for="cat in categoryOptions" :key="cat.value" :label="cat.label" :value="cat.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('ai.custom.conditions')">
          <el-checkbox v-model="editorForm.needsTitle">{{ $t('ai.custom.needsTitle') }}</el-checkbox>
          <el-checkbox v-model="editorForm.needsContent">{{ $t('ai.custom.needsContent') }}</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditor = false">{{ $t('ai.custom.cancel') }}</el-button>
        <el-button type="primary" @click="savePreset" :disabled="!editorForm.name || !editorForm.prompt">
          {{ $t('ai.custom.save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 导入导出 -->
    <div class="import-export">
      <el-button size="small" text @click="exportPresets">
        <el-icon><Download /></el-icon> {{ $t('ai.custom.export') }}
      </el-button>
      <el-button size="small" text @click="triggerImport">
        <el-icon><Upload /></el-icon> {{ $t('ai.custom.import') }}
      </el-button>
      <input ref="importInput" type="file" accept=".json" style="display: none" @change="importPresets" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { Edit, Delete, Plus, Download, Upload } from '@element-plus/icons-vue'
import {
  getCustomPresets,
  addCustomPreset,
  updateCustomPreset,
  deleteCustomPreset as deletePresetApi,
  exportCustomPresets,
  importCustomPresets,
  presetIconOptions,
  presetCategoryOptions,
} from '../../ai/presets/custom'

const { t } = useI18n()
const emit = defineEmits(['select', 'update'])

const presets = ref([])
const showEditor = ref(false)
const editingId = ref(null)
const importInput = ref(null)

const iconOptions = presetIconOptions
const categoryOptions = computed(() =>
  presetCategoryOptions.map((c) => ({ ...c, label: t(`ai.category.${c.value}`) }))
)

const editorForm = ref({
  name: '',
  icon: ' ',
  prompt: '',
  category: 'custom',
  needsTitle: false,
  needsContent: true,
})

function loadPresets() {
  presets.value = getCustomPresets()
}

function getCategoryLabel(category) {
  const cat = categoryOptions.value.find((c) => c.value === category)
  return cat ? cat.label : category
}

function editPreset(preset) {
  editingId.value = preset.id
  editorForm.value = {
    name: preset.name,
    icon: preset.icon,
    prompt: preset.prompt,
    category: preset.category,
    needsTitle: preset.needsTitle,
    needsContent: preset.needsContent,
  }
  showEditor.value = true
}

function savePreset() {
  if (editingId.value) {
    updateCustomPreset(editingId.value, editorForm.value)
    ElMessage.success(t('ai.custom.saved'))
  } else {
    addCustomPreset(editorForm.value)
    ElMessage.success(t('ai.custom.created'))
  }
  showEditor.value = false
  editingId.value = null
  editorForm.value = { name: '', icon: ' ', prompt: '', category: 'custom', needsTitle: false, needsContent: true }
  loadPresets()
  emit('update')
}

async function deletePreset(preset) {
  try {
    await ElMessageBox.confirm(
      t('ai.custom.confirmDelete', { name: preset.name }),
      t('ai.custom.deleteTitle'),
      { type: 'warning' }
    )
    deletePresetApi(preset.id)
    ElMessage.success(t('ai.custom.deleted'))
    loadPresets()
    emit('update')
  } catch {
    // cancelled
  }
}

function exportPresets() {
  const json = exportCustomPresets()
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'ai-presets.json'
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success(t('ai.custom.exported'))
}

function triggerImport() {
  importInput.value?.click()
}

function importPresets(e) {
  const file = e.target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (event) => {
    const result = importCustomPresets(event.target.result)
    if (result.success) {
      ElMessage.success(t('ai.custom.imported', { count: result.count }))
      loadPresets()
      emit('update')
    } else {
      ElMessage.error(t('ai.custom.importFailed', { error: result.error }))
    }
  }
  reader.readAsText(file)
  e.target.value = ''
}

onMounted(loadPresets)
</script>

<style scoped>
.custom-preset-manager {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.preset-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}
.empty-state {
  text-align: center;
  padding: 20px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.preset-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.preset-item:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.preset-icon {
  font-size: 20px;
  width: 32px;
  text-align: center;
}
.preset-info {
  flex: 1;
  min-width: 0;
}
.preset-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
}
.preset-category {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.preset-actions {
  display: flex;
  gap: 4px;
}
.add-btn {
  width: 100%;
  border-style: dashed;
}
.icon-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.icon-option {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.icon-option:hover {
  border-color: var(--el-color-primary);
}
.icon-option.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.prompt-hint {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.import-export {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
