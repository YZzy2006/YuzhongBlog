<template>
  <div class="template-manager">
    <!-- 模板列表 -->
    <div class="template-list">
      <div v-if="allTemplates.length === 0" class="empty-state">
        <p>{{ $t('ai.template.noTemplates') }}</p>
      </div>
      <div v-for="tpl in allTemplates" :key="tpl.id" class="template-item" @click="$emit('select', tpl)">
        <span class="template-icon">{{ tpl.icon }}</span>
        <div class="template-info">
          <span class="template-name">{{ tpl.name }}</span>
          <span class="template-category">{{ getCategoryLabel(tpl.category) }}</span>
          <div class="template-placeholders">
            <span v-for="ph in tpl.placeholders" :key="ph.name" class="placeholder-tag">{{ ph.name }}</span>
          </div>
        </div>
        <div class="template-actions">
          <el-button v-if="!tpl.isBuiltin" size="small" text @click.stop="editTemplate(tpl)">
            <el-icon><Edit /></el-icon>
          </el-button>
          <el-button v-if="!tpl.isBuiltin" size="small" text type="danger" @click.stop="deleteTemplate(tpl)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 添加按钮 -->
    <el-button class="add-btn" @click="showEditor = true">
      <el-icon><Plus /></el-icon>
      {{ $t('ai.template.addTemplate') }}
    </el-button>

    <!-- 编辑器弹窗 -->
    <el-dialog v-model="showEditor" :title="editingId ? $t('ai.template.editTemplate') : $t('ai.template.addTemplate')" width="480px">
      <el-form :model="editorForm" label-position="top">
        <el-form-item :label="$t('ai.template.name')" required>
          <el-input v-model="editorForm.name" :placeholder="$t('ai.template.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('ai.template.icon')">
          <div class="icon-selector">
            <span v-for="icon in iconOptions" :key="icon"
              class="icon-option" :class="{ active: editorForm.icon === icon }"
              @click="editorForm.icon = icon">
              {{ icon }}
            </span>
          </div>
        </el-form-item>
        <el-form-item :label="$t('ai.template.prompt')" required>
          <el-input v-model="editorForm.prompt" type="textarea" :rows="4"
            :placeholder="$t('ai.template.promptPlaceholder')" />
          <div class="prompt-hint">{{ $t('ai.template.promptHint') }}</div>
          <div v-if="detectedPlaceholders.length > 0" class="detected-placeholders">
            <span class="detected-label">{{ $t('ai.template.detectedPlaceholders') }}</span>
            <span v-for="ph in detectedPlaceholders" :key="ph.name" class="placeholder-tag">
              {{ ph.name }}{{ ph.default ? `: ${ph.default}` : '' }}
            </span>
          </div>
        </el-form-item>
        <el-form-item :label="$t('ai.template.category')">
          <el-select v-model="editorForm.category" style="width: 100%">
            <el-option v-for="cat in categoryOptions" :key="cat.value" :label="cat.label" :value="cat.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditor = false">{{ $t('ai.template.cancel') }}</el-button>
        <el-button type="primary" @click="saveTemplate" :disabled="!editorForm.name || !editorForm.prompt">
          {{ $t('ai.template.save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 占位符填写弹窗 -->
    <el-dialog v-model="showFillDialog" :title="$t('ai.template.fillPlaceholders')" width="480px">
      <el-form label-position="top">
        <el-form-item v-for="ph in currentPlaceholders" :key="ph.name" :label="ph.name">
          <el-input v-model="fillValues[ph.name]" :placeholder="ph.default || $t('ai.template.enterValue')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFillDialog = false">{{ $t('ai.template.cancel') }}</el-button>
        <el-button type="primary" @click="confirmFill">{{ $t('ai.template.apply') }}</el-button>
      </template>
    </el-dialog>

    <!-- 导入导出 -->
    <div class="import-export">
      <el-button size="small" text @click="exportTemplates">
        <el-icon><Download /></el-icon> {{ $t('ai.template.export') }}
      </el-button>
      <el-button size="small" text @click="triggerImport">
        <el-icon><Upload /></el-icon> {{ $t('ai.template.import') }}
      </el-button>
      <input ref="importInput" type="file" accept=".json" style="display: none" @change="importTemplates" />
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
  getTemplates,
  addTemplate,
  updateTemplate,
  deleteTemplate as deleteTemplateApi,
  extractPlaceholders,
  fillTemplate,
  exportTemplates as exportTemplatesApi,
  importTemplates as importTemplatesApi,
  builtinTemplates,
} from '../../ai/templates/index'

const { t } = useI18n()
const emit = defineEmits(['select', 'update'])

const templates = ref([])
const showEditor = ref(false)
const editingId = ref(null)
const importInput = ref(null)
const showFillDialog = ref(false)
const currentPlaceholders = ref([])
const fillValues = ref({})
const pendingTemplate = ref(null)

const iconOptions = [' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '#', '⭐', ' ', ' ']
const categoryOptions = computed(() => [
  { value: 'writing', label: t('ai.category.writing') },
  { value: 'translate', label: t('ai.category.translate') },
  { value: 'analysis', label: t('ai.category.analysis') },
  { value: 'enhance', label: t('ai.category.enhance') },
  { value: 'custom', label: t('ai.category.custom') },
])

const allTemplates = computed(() => {
  const builtin = builtinTemplates.map(t => ({
    ...t,
    placeholders: extractPlaceholders(t.prompt),
  }))
  return [...builtin, ...templates.value]
})

const editorForm = ref({
  name: '',
  icon: ' ',
  prompt: '',
  category: 'custom',
})

const detectedPlaceholders = computed(() => {
  return extractPlaceholders(editorForm.value.prompt)
})

function loadTemplates() {
  templates.value = getTemplates()
}

function getCategoryLabel(category) {
  const cat = categoryOptions.value.find(c => c.value === category)
  return cat ? cat.label : category
}

function editTemplate(tpl) {
  editingId.value = tpl.id
  editorForm.value = {
    name: tpl.name,
    icon: tpl.icon,
    prompt: tpl.prompt,
    category: tpl.category,
  }
  showEditor.value = true
}

function saveTemplate() {
  if (editingId.value) {
    updateTemplate(editingId.value, editorForm.value)
    ElMessage.success(t('ai.template.saved'))
  } else {
    addTemplate(editorForm.value)
    ElMessage.success(t('ai.template.created'))
  }
  showEditor.value = false
  editingId.value = null
  editorForm.value = { name: '', icon: ' ', prompt: '', category: 'custom' }
  loadTemplates()
  emit('update')
}

async function deleteTemplate(tpl) {
  try {
    await ElMessageBox.confirm(
      t('ai.template.confirmDelete', { name: tpl.name }),
      t('ai.template.deleteTitle'),
      { type: 'warning' }
    )
    deleteTemplateApi(tpl.id)
    ElMessage.success(t('ai.template.deleted'))
    loadTemplates()
    emit('update')
  } catch {
    // cancelled
  }
}

function useTemplate(tpl) {
  const placeholders = tpl.placeholders || extractPlaceholders(tpl.prompt)
  if (placeholders.length > 0) {
    pendingTemplate.value = tpl
    currentPlaceholders.value = placeholders
    fillValues.value = {}
    for (const ph of placeholders) {
      fillValues.value[ph.name] = ph.default || ''
    }
    showFillDialog.value = true
  } else {
    emit('select', { ...tpl, filledPrompt: tpl.prompt })
  }
}

function confirmFill() {
  if (pendingTemplate.value) {
    const filledPrompt = fillTemplate(pendingTemplate.value.prompt, fillValues.value)
    emit('select', { ...pendingTemplate.value, filledPrompt })
    showFillDialog.value = false
    pendingTemplate.value = null
  }
}

function exportTemplates() {
  const json = exportTemplatesApi()
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'ai-templates.json'
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success(t('ai.template.exported'))
}

function triggerImport() {
  importInput.value?.click()
}

function importTemplates(e) {
  const file = e.target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (event) => {
    const result = importTemplatesApi(event.target.result)
    if (result.success) {
      ElMessage.success(t('ai.template.imported', { count: result.count }))
      loadTemplates()
      emit('update')
    } else {
      ElMessage.error(t('ai.template.importFailed', { error: result.error }))
    }
  }
  reader.readAsText(file)
  e.target.value = ''
}

onMounted(loadTemplates)

defineExpose({ useTemplate })
</script>

<style scoped>
.template-manager {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.template-list {
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
.template-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.template-item:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.template-icon {
  font-size: 20px;
  width: 32px;
  text-align: center;
}
.template-info {
  flex: 1;
  min-width: 0;
}
.template-name {
  display: block;
  font-size: 13px;
  font-weight: 500;
}
.template-category {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.template-placeholders {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}
.placeholder-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.template-actions {
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
.detected-placeholders {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
  align-items: center;
}
.detected-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.import-export {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
