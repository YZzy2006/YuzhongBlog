<template>
  <div class="admin-page">
    <!-- List view -->
    <template v-if="!showForm">
      <div class="page-header">
        <h2>{{ $t('adminTimeline.pageTitle') }}</h2>
        <div class="header-actions">
          <RefreshButton :onRefresh="loadEntries" />
          <el-button v-if="selectedIds.length > 0" @click="bulkToggleStatus">
            {{ $t('adminTimeline.bulkToggle') }} ({{ selectedIds.length }})
          </el-button>
          <el-button v-if="selectedIds.length > 0" type="danger" @click="bulkDelete">
            {{ $t('adminTimeline.bulkDelete') }} ({{ selectedIds.length }})
          </el-button>
          <el-button v-if="authStore.hasPermission('project:manage')" type="primary" @click="startAdd">
            <el-icon><Plus /></el-icon> {{ $t('adminTimeline.createEntry') }}
          </el-button>
        </div>
      </div>

      <!-- Search and filter bar -->
      <div class="filter-bar">
        <el-input v-model="keyword" :placeholder="$t('adminTimeline.searchPlaceholder')" clearable
          style="width: 280px" @input="onKeywordInput" @clear="page = 0; loadEntries()" @keydown.enter="page = 0; loadEntries()" />
        <el-select v-model="filterStatus" style="width: 120px" @change="page = 0; loadEntries()">
          <el-option :label="$t('adminTimeline.filterAll')" value="" />
          <el-option :label="$t('adminTimeline.draft')" value="DRAFT" />
          <el-option :label="$t('adminTimeline.published')" value="PUBLISHED" />
        </el-select>
        <el-select v-model="sortBy" style="width: 120px" @change="page = 0; loadEntries()">
          <el-option :label="$t('adminTimeline.sortDefault')" value="default" />
          <el-option :label="$t('adminTimeline.sortNewest')" value="newest" />
          <el-option :label="$t('adminTimeline.sortOldest')" value="oldest" />
        </el-select>
      </div>

      <!-- Result count -->
      <div v-if="totalElements > 0" class="result-count">
        {{ $t('adminTimeline.foundEntries', { count: totalElements }) }}
      </div>

      <el-card>
        <el-table :data="entries" row-key="id" stripe v-loading="loading" :empty-text="$t('adminTimeline.emptyText')"
          @selection-change="onSelectionChange" ref="tableRef">
          <el-table-column type="selection" width="45" />
          <el-table-column :label="$t('adminTimeline.cover')" width="70">
            <template #default="{ row }">
              <el-image v-if="row.coverImage" :src="row.coverImage" lazy style="width: 48px; height: 32px; border-radius: 4px" fit="cover" />
              <span v-else style="color: #94a3b8">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="title" :label="$t('adminTimeline.title')" min-width="180" show-overflow-tooltip />
          <el-table-column :label="$t('adminTimeline.descriptionPreview')" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="desc-preview">{{ row.description || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="entryDate" :label="$t('adminTimeline.entryDate')" width="120" />
          <el-table-column prop="category" :label="$t('adminTimeline.category')" width="100" show-overflow-tooltip />
          <el-table-column prop="status" :label="$t('adminTimeline.status')" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" effect="plain" size="small" @click="toggleStatus(row)" style="cursor: pointer">
                {{ row.status === 'PUBLISHED' ? $t('adminTimeline.published') : $t('adminTimeline.draft') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" :label="$t('adminTimeline.sortOrder')" width="70" />
          <el-table-column prop="createdAt" :label="$t('adminTimeline.createdAt')" width="170">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('adminTimeline.action')" width="160" fixed="right">
            <template #default="{ row }">
              <el-button v-if="authStore.hasPermission('project:manage')" type="primary" size="small" @click="startEdit(row)">{{ $t('adminTimeline.edit') }}</el-button>
              <el-button v-if="authStore.hasPermission('project:manage')" type="danger" size="small" :disabled="pendingIds.has(row.id)" @click="handleDelete(row)">{{ $t('adminTimeline.delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- Pagination -->
      <div v-if="totalPages > 1" style="display: flex; justify-content: center; margin-top: 16px">
        <el-pagination background layout="total, prev, pager, next, sizes"
          :total="totalElements" :page-size="pageSize" :current-page="page + 1"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange" />
      </div>
    </template>

    <!-- Edit view -->
    <template v-else>
      <div class="edit-page-content">
      <div class="page-header">
        <h2>{{ editingId ? $t('adminTimeline.editEntry') : $t('adminTimeline.createEntry') }}</h2>
        <div style="display: flex; gap: 8px">
          <el-button @click="showUploadDialog = true">
            <el-icon style="margin-right: 4px"><Upload /></el-icon>
            {{ $t('adminTimeline.uploadFile') }}
          </el-button>
          <el-button @click="aiDrawerVisible = true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align: -2px; margin-right: 4px"><path d="M12 2a4 4 0 0 0-4 4v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2h-2V6a4 4 0 0 0-4-4z"/><circle cx="9" cy="14" r="1"/><circle cx="15" cy="14" r="1"/></svg>
            {{ $t('adminTimeline.aiAssist') }}
          </el-button>
          <el-button @click="showForm = false">{{ $t('adminTimeline.cancel') }}</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">
            {{ editingId ? $t('adminTimeline.save') : $t('adminTimeline.create') }}
          </el-button>
        </div>
      </div>

      <div class="edit-layout">
        <div class="edit-main">
          <el-input v-model="form.title" :placeholder="$t('adminTimeline.titlePlaceholder')" class="title-input" />
          <MdEditor ref="editorRef" v-model="form.description" :theme="'light'" class="md-editor-moments" @onUploadImg="onUploadImg" />
        </div>

        <div class="edit-sidebar">
          <el-card shadow="never" style="margin-bottom: 12px">
            <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminTimeline.coverImageLabel') }}</span></template>
            <div style="display: flex; flex-direction: column; gap: 8px">
              <el-image v-if="form.coverImage" :src="form.coverImage" style="width: 100%; max-height: 160px; border-radius: 6px" fit="cover" />
              <template v-if="form.coverImage">
                <div style="display: flex; gap: 8px">
                  <el-button size="small" @click="form.coverImage = ''">{{ $t('adminTimeline.changeCover') }}</el-button>
                  <el-button type="danger" text size="small" @click="form.coverImage = ''">{{ $t('adminTimeline.delete') }}</el-button>
                </div>
              </template>
              <FileUpload v-else endpoint="/admin/upload/cover" accept="image/*" @uploaded="onCoverUploaded" style="height: 180px" />
            </div>
          </el-card>

          <el-card shadow="never" style="margin-bottom: 12px">
            <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminTimeline.imagesLabel') || '附加图片' }}</span></template>
            <div style="display: flex; flex-direction: column; gap: 8px">
              <div v-if="imagesList.length" style="display: flex; flex-wrap: wrap; gap: 8px">
                <div v-for="(img, idx) in imagesList" :key="idx" style="position: relative; width: 80px; height: 80px">
                  <el-image :src="img" style="width: 100%; height: 100%; border-radius: 6px" fit="cover" />
                  <el-button type="danger" :icon="Delete" circle size="small" style="position: absolute; top: -8px; right: -8px" @click="removeImage(idx)" />
                </div>
              </div>
              <FileUpload endpoint="/admin/upload/cover" accept="image/*" @uploaded="onImageUploaded" style="height: 100px" />
            </div>
          </el-card>

          <el-card shadow="never">
            <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminTimeline.basicInfoLabel') }}</span></template>
            <el-form :model="form" label-position="top">
              <el-form-item :label="$t('adminTimeline.entryDateLabel')" required>
                <el-date-picker v-model="form.entryDate" type="date" value-format="YYYY-MM-DD"
                  :placeholder="$t('adminTimeline.entryDateLabel')" style="width: 100%" />
              </el-form-item>
              <el-form-item :label="$t('adminTimeline.categoryLabel')">
                <el-input v-model="form.category" :placeholder="$t('adminTimeline.categoryPlaceholder')" />
              </el-form-item>
              <el-form-item :label="$t('adminTimeline.moodLabel')">
                <el-input v-model="form.mood" :placeholder="$t('adminTimeline.moodPlaceholder')" maxlength="20" />
              </el-form-item>
              <el-form-item :label="$t('adminTimeline.tagsLabel')">
                <div class="tags-editor">
                  <el-tag
                    v-for="(tag, idx) in tagsList"
                    :key="idx"
                    closable
                    size="small"
                    @close="removeTag(idx)"
                  >{{ tag }}</el-tag>
                  <input
                    ref="tagInputRef"
                    v-model="tagInput"
                    class="tag-input"
                    :placeholder="$t('adminTimeline.tagsPlaceholder')"
                    @keydown="onTagKeydown"
                    @blur="addTag"
                  />
                </div>
              </el-form-item>
              <el-form-item :label="$t('adminTimeline.linkLabel')">
                <el-input v-model="form.linkUrl" placeholder="https://..." />
              </el-form-item>
              <el-form-item :label="$t('adminTimeline.sortLabel')">
                <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item :label="$t('adminTimeline.statusLabel')">
                <el-select v-model="form.status" style="width: 100%">
                  <el-option :label="$t('adminTimeline.draft')" value="DRAFT" />
                  <el-option :label="$t('adminTimeline.published')" value="PUBLISHED" />
                </el-select>
              </el-form-item>
            </el-form>
          </el-card>
        </div>
      </div>
      </div>
    </template>

    <!-- AI Assist Drawer -->
    <el-drawer v-model="aiDrawerVisible" :title="$t('adminTimeline.aiAssistTitle')" direction="rtl" size="480px" :before-close="closeAiDrawer">
      <div class="ai-drawer-body">
        <!-- Preset search -->
        <div class="ai-preset-search">
          <el-input v-model="presetSearch" :placeholder="$t('ai.ui.searchPresets')" prefix-icon="Search" size="small" clearable />
        </div>

        <!-- Preset category filter -->
        <div class="ai-preset-categories">
          <button class="category-tag" :class="{ active: presetCategory === 'all' }" @click="presetCategory = 'all'">{{ $t('ai.ui.all') }}</button>
          <button class="category-tag" :class="{ active: presetCategory === 'writing' }" @click="presetCategory = 'writing'">{{ $t('ai.category.writing') }}</button>
          <button class="category-tag" :class="{ active: presetCategory === 'translate' }" @click="presetCategory = 'translate'">{{ $t('ai.category.translate') }}</button>
          <button class="category-tag" :class="{ active: presetCategory === 'analysis' }" @click="presetCategory = 'analysis'">{{ $t('ai.category.analysis') }}</button>
          <button class="category-tag" :class="{ active: presetCategory === 'enhance' }" @click="presetCategory = 'enhance'">{{ $t('ai.category.enhance') }}</button>
          <button class="category-tag" :class="{ active: presetCategory === 'tone' }" @click="presetCategory = 'tone'">{{ $t('ai.category.tone') }}</button>
        </div>

        <!-- Preset modes -->
        <div class="ai-presets">
          <button v-for="p in filteredPresets" :key="p.id"
            class="ai-preset-btn" :class="{ active: aiMode === p.id }"
            :disabled="aiLoading || (p.needsTitle && !form.title.trim()) || (p.needsContent && !form.description.trim())"
            @click="runPreset(p.id)">
            <span class="ai-preset-icon">{{ p.icon }}</span>
            <span class="ai-preset-label">{{ p.label }}</span>
          </button>
          <button class="ai-preset-btn manage-btn" @click="showCustomPresetManager = true">
            <span class="ai-preset-icon">⚙️</span>
            <span class="ai-preset-label">{{ $t('ai.custom.managePresets') }}</span>
          </button>
          <button class="ai-preset-btn manage-btn" @click="showTemplateManager = true">
            <span class="ai-preset-icon"> </span>
            <span class="ai-preset-label">{{ $t('ai.template.addTemplate') }}</span>
          </button>
          <button class="ai-preset-btn manage-btn" @click="showKnowledgeManager = true">
            <span class="ai-preset-icon"> </span>
            <span class="ai-preset-label">{{ $t('ai.knowledge.manage') }}</span>
          </button>
          <button class="ai-preset-btn manage-btn" @click="showHistoryManager = true">
            <span class="ai-preset-icon"> </span>
            <span class="ai-preset-label">{{ $t('ai.history.manage') }}</span>
          </button>
          <button class="ai-preset-btn manage-btn" @click="showAgentWriter = true">
            <span class="ai-preset-icon"> </span>
            <span class="ai-preset-label">{{ $t('ai.agent.manage') }}</span>
          </button>
        </div>

        <!-- Custom prompt -->
        <div class="ai-custom-section">
          <textarea v-model="aiPrompt" class="ai-prompt-input" rows="3"
            :placeholder="$t('adminTimeline.customPromptPlaceholder')"
            @keydown.ctrl.enter="runCustom" :disabled="aiLoading" />
          <div style="display: flex; align-items: center; gap: 8px; margin-top: 6px">
            <button class="ai-generate-btn" :disabled="aiLoading || !aiPrompt.trim()" @click="runCustom">
              {{ aiLoading ? $t('adminTimeline.generating') : $t('adminTimeline.generate') }}
            </button>
            <span class="ai-shortcut-hint">Ctrl+Enter</span>
          </div>
        </div>

        <!-- Result preview -->
        <div v-if="aiResult || aiLoading || chatHistory.length > 0" ref="aiResultSection" class="ai-result-section">
          <div class="ai-result-header">
            <span class="ai-result-title">{{ $t('adminTimeline.preview') }}</span>
            <div style="display: flex; gap: 4px">
              <button v-if="aiResult && !aiLoading" class="ai-cancel-btn" style="font-size: 12px; padding: 2px 8px" @click="compareMode = !compareMode">
                {{ compareMode ? $t('adminTimeline.exitCompare') : $t('adminTimeline.compareView') }}
              </button>
              <button v-if="aiLoading" class="ai-cancel-btn" @click="cancelAi">{{ $t('adminTimeline.cancel') }}</button>
            </div>
          </div>

          <!-- Compare mode -->
          <div v-if="compareMode && aiResult && !aiLoading" class="ai-compare">
            <div class="ai-compare-pane">
              <div class="ai-compare-label">{{ $t('adminTimeline.originalText') }}</div>
              <div class="ai-compare-content">
                <MdPreview :modelValue="selectedRange ? form.description.slice(selectedRange.start, selectedRange.end) : form.description" previewTheme="github" :codeFoldable="false" />
              </div>
            </div>
            <div class="ai-compare-pane">
              <div class="ai-compare-label">{{ $t('adminTimeline.aiResult') }}</div>
              <div class="ai-compare-content">
                <MdPreview :modelValue="aiResult" previewTheme="github" :codeFoldable="false" />
              </div>
            </div>
          </div>

          <!-- Normal preview -->
          <div v-else class="ai-result-preview">
            <div v-for="(msg, i) in chatHistory" :key="i" class="ai-chat-msg" :class="msg.role">
              <div class="ai-chat-bubble">
                <MdPreview v-if="msg.role === 'assistant'" :modelValue="msg.content" previewTheme="github" :codeFoldable="false" />
                <span v-else>{{ msg.content.length > 200 ? msg.content.slice(0, 200) + '...' : msg.content }}</span>
              </div>
            </div>
            <div v-if="aiResult && chatHistory.length > 0 && chatHistory[chatHistory.length - 1]?.role === 'user'" class="ai-chat-msg assistant">
              <div class="ai-chat-bubble">
                <MdPreview v-if="aiResult" :modelValue="aiResult" previewTheme="github" :codeFoldable="false" />
                <div v-if="aiLoading && !aiResult" class="ai-typing">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
            <template v-if="chatHistory.length === 0">
              <MdPreview v-if="aiResult" :modelValue="aiResult" previewTheme="github" :codeFoldable="false" />
              <div v-if="aiLoading && !aiResult" class="ai-typing">
                <span></span><span></span><span></span>
              </div>
            </template>
          </div>

          <!-- Model info -->
          <div v-if="modelInfo" class="ai-model-info">
            <span v-if="modelInfo.model">{{ modelInfo.model }}</span>
            <span v-if="modelInfo.tokens"> &middot; {{ modelInfo.tokens }} tokens</span>
          </div>

          <!-- Actions -->
          <div v-if="aiResult && !aiLoading" class="ai-actions">
            <button v-if="selectedRange" class="ai-action-btn primary" @click="replaceSelected">{{ $t('adminTimeline.replaceSelected') }}</button>
            <button class="ai-action-btn" @click="insertToContent">{{ $t('adminTimeline.insertToContent') }}</button>
            <button class="ai-action-btn" @click="replaceContent">{{ $t('adminTimeline.replaceContent') }}</button>
            <CopyButton :text="aiResult" @copied="ElMessage.success(t('adminTimeline.copiedMsg'))" />
            <button v-if="previousContent" class="ai-action-btn" @click="undoReplace" style="color: var(--el-color-warning)">{{ $t('adminTimeline.undoReplace') }}</button>
          </div>

          <!-- Continue chat -->
          <div v-if="chatHistory.length > 0 && !aiLoading" class="ai-continue-section">
            <textarea v-model="continueInput" class="ai-prompt-input" rows="2"
              :placeholder="$t('adminTimeline.continuePlaceholder')"
              @keydown.ctrl.enter="continueChat" />
            <button class="ai-generate-btn" :disabled="!continueInput.trim()" @click="continueChat">
              {{ $t('adminTimeline.send') }}
            </button>
          </div>
        </div>

        <!-- Error -->
        <div v-if="aiError" class="ai-error">{{ aiError }}</div>
      </div>
    </el-drawer>

    <!-- Custom Preset Manager Dialog -->
    <el-dialog v-model="showCustomPresetManager" :title="$t('ai.custom.managePresets')" width="520px" append-to-body>
      <CustomPresetManager @update="onCustomPresetUpdate" />
    </el-dialog>

    <!-- Template Manager Dialog -->
    <el-dialog v-model="showTemplateManager" :title="$t('ai.template.addTemplate')" width="520px" append-to-body>
      <TemplateManager ref="templateManagerRef" @select="onTemplateSelect" />
    </el-dialog>

    <!-- Knowledge Manager Dialog -->
    <el-dialog v-model="showKnowledgeManager" :title="$t('ai.knowledge.manage')" width="600px" append-to-body>
      <KnowledgeManager />
    </el-dialog>

    <!-- History Manager Dialog -->
    <el-dialog v-model="showHistoryManager" :title="$t('ai.history.manage')" width="600px" append-to-body>
      <AIHistoryManager @select="onHistorySelect" @reuse="onHistoryReuse" />
    </el-dialog>

    <!-- Agent Writer Dialog -->
    <el-dialog v-model="showAgentWriter" :title="$t('ai.agent.manage')" width="700px" append-to-body>
      <AgentWriter @complete="onAgentComplete" />
    </el-dialog>

    <!-- Upload Dialog -->
    <el-dialog v-model="showUploadDialog" :title="$t('adminTimeline.uploadFile')" width="480px" :close-on-click-modal="true">
      <div class="upload-dialog-body">
        <div class="upload-options">
          <el-button @click="handleFileUpload('image')">
            <el-icon style="margin-right: 6px"><Upload /></el-icon>
            {{ $t('adminTimeline.uploadImage') }}
          </el-button>
          <el-button @click="handleFileUpload('document')">
            <el-icon style="margin-right: 6px"><Upload /></el-icon>
            {{ $t('adminTimeline.uploadDocument') }}
          </el-button>
        </div>
        <div class="upload-drop-area" :class="{ 'drag-active': isDragging }"
          @dragover.prevent="isDragging = true" @dragleave.prevent="isDragging = false"
          @drop.prevent="onDropFile">
          <input ref="imageInput" type="file" accept="image/*" multiple style="display:none" @change="onImageSelect" />
          <input ref="docInput" type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.csv,.txt,.zip,.rar,.7z" multiple style="display:none" @change="onDocSelect" />
          <el-icon :size="32" class="upload-icon"><Upload /></el-icon>
          <p class="upload-text">{{ $t('adminTimeline.dropFiles') }}</p>
          <p class="upload-hint">{{ $t('adminTimeline.uploadHint') }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch, defineAsyncComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { Plus, Upload, Delete } from '@element-plus/icons-vue'
const MdEditor = defineAsyncComponent(() => import('md-editor-v3').then(m => m.MdEditor))
const MdPreview = defineAsyncComponent(() => import('md-editor-v3').then(m => m.MdPreview))
import 'md-editor-v3/lib/style.css'
import 'md-editor-v3/lib/preview.css'
import('../../utils/mdEditorConfig')
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'
import FileUpload from '../../components/FileUpload.vue'
import CopyButton from '../../components/CopyButton.vue'
import RefreshButton from '../../components/RefreshButton.vue'
const CustomPresetManager = defineAsyncComponent(() => import('../../components/ai/CustomPresetManager.vue'))
const TemplateManager = defineAsyncComponent(() => import('../../components/ai/TemplateManager.vue'))
const KnowledgeManager = defineAsyncComponent(() => import('../../components/ai/KnowledgeManager.vue'))
const AIHistoryManager = defineAsyncComponent(() => import('../../components/ai/AIHistoryManager.vue'))
const AgentWriter = defineAsyncComponent(() => import('../../components/ai/AgentWriter.vue'))
import { injectCopyButtons } from '../../utils/copyUtils'
import { aiEditorStream } from '../../utils/ai'
import { getCustomPresets } from '../../ai/presets/custom'
import { buildKnowledgeContext } from '../../ai/knowledge/index'
import { addHistory } from '../../ai/history/index'

const authStore = useAuthStore()
const { t } = useI18n()

// === List state ===
const entries = ref([])
const loading = ref(false)
const showForm = ref(false)
const editingId = ref(null)
const saving = ref(false)
const selectedIds = ref([])
const tableRef = ref(null)

const keyword = ref('')
const page = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const filterStatus = ref('')
const sortBy = ref('default')
let searchTimer = null
const pendingIds = new Set()

const defaultForm = () => ({
  title: '', description: '', coverImage: '', entryDate: '',
  linkUrl: '', category: '', mood: '', tags: '', images: '', sortOrder: 0, status: 'DRAFT'
})
const form = reactive(defaultForm())

function formatDate(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 16)
}

function onKeywordInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { page.value = 0; loadEntries() }, 300)
}

function onSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

function handlePageChange(p) {
  page.value = p - 1
  loadEntries()
}

function handleSizeChange(s) {
  pageSize.value = s
  page.value = 0
  loadEntries()
}

async function loadEntries() {
  loading.value = true
  try {
    let url = `/admin/timeline-entries?page=${page.value}&size=${pageSize.value}`
    if (keyword.value.trim()) url += `&keyword=${encodeURIComponent(keyword.value.trim())}`
    if (filterStatus.value) url += `&status=${filterStatus.value}`
    if (sortBy.value !== 'default') url += `&sort=${sortBy.value}`
    const data = await request.get(url)
    entries.value = data.content
    totalPages.value = data.totalPages
    totalElements.value = data.totalElements
  } catch {
    ElMessage.error(t('adminTimeline.loadFailed'))
  } finally {
    loading.value = false
  }
}

function startAdd() {
  editingId.value = null
  Object.assign(form, defaultForm())
  showForm.value = true
}

function startEdit(entry) {
  editingId.value = entry.id
  Object.assign(form, {
    title: entry.title || '', description: entry.description || '',
    coverImage: entry.coverImage || '', entryDate: entry.entryDate || '',
    linkUrl: entry.linkUrl || '', category: entry.category || '',
    mood: entry.mood || '', tags: entry.tags || '', images: entry.images || '',
    sortOrder: entry.sortOrder || 0, status: entry.status || 'DRAFT'
  })
  showForm.value = true
}

async function handleSave() {
  if (!form.title.trim()) { ElMessage.warning(t('adminTimeline.titlePlaceholder')); return }
  if (!form.entryDate) { ElMessage.warning(t('adminTimeline.entryDateLabel')); return }
  saving.value = true
  try {
    const body = { ...form }
    if (editingId.value) {
      await request.put(`/admin/timeline-entries/${editingId.value}`, body)
    } else {
      await request.post('/admin/timeline-entries', body)
    }
    showForm.value = false
    ElMessage.success(t('adminTimeline.saveSuccess'))
    loadEntries()
  } catch (e) {
    ElMessage.error(t('adminTimeline.saveFailed') + ': ' + (e.message || ''))
  } finally {
    saving.value = false
  }
}

function onCoverUploaded(res) {
  form.coverImage = res.url
}

const imagesList = computed(() => {
  if (!form.images) return []
  try { return JSON.parse(form.images) } catch { return [] }
})

function onImageUploaded(res) {
  const list = [...imagesList.value, res.url]
  form.images = JSON.stringify(list)
}

function removeImage(idx) {
  const list = [...imagesList.value]
  list.splice(idx, 1)
  form.images = JSON.stringify(list)
}

// === Tags chip editor ===
const tagInput = ref('')

const tagsList = computed(() => {
  if (!form.tags) return []
  try {
    const arr = JSON.parse(form.tags)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
})

function addTag() {
  const val = tagInput.value.trim().replace(/^["\s]+|["\s]+$/g, '')
  if (!val) return
  const list = [...tagsList.value]
  if (!list.includes(val)) {
    list.push(val)
    form.tags = JSON.stringify(list)
  }
  tagInput.value = ''
}

function removeTag(idx) {
  const list = [...tagsList.value]
  list.splice(idx, 1)
  form.tags = list.length ? JSON.stringify(list) : ''
}

function onTagKeydown(e) {
  if (e.key === 'Enter' || e.key === ',') {
    e.preventDefault()
    addTag()
  }
}

async function onUploadImg(files, callback) {
  const uploaded = []
  for (const file of files) {
    const formData = new FormData()
    formData.append('file', file)
    try {
      const res = await request.post('/admin/upload/image', formData)
      uploaded.push(res.url)
    } catch {
      ElMessage.error(t('adminTimeline.uploadFailed'))
    }
  }
  callback(uploaded)
}

// === File upload for editor ===
const imageInput = ref(null)
const docInput = ref(null)
const isDragging = ref(false)
const showUploadDialog = ref(false)
const editorRef = ref(null)
const tagInputRef = ref(null)

function handleFileUpload(type) {
  if (type === 'image') imageInput.value?.click()
  else docInput.value?.click()
}

function onImageSelect(e) {
  const files = Array.from(e.target.files || [])
  if (files.length) uploadAndInsert(files, 'image')
  e.target.value = ''
}

function onDocSelect(e) {
  const files = Array.from(e.target.files || [])
  if (files.length) uploadAndInsert(files, 'doc')
  e.target.value = ''
}

function onDropFile(e) {
  isDragging.value = false
  const files = Array.from(e.dataTransfer?.files || [])
  if (!files.length) return
  const images = files.filter(f => f.type.startsWith('image/'))
  const docs = files.filter(f => !f.type.startsWith('image/'))
  if (images.length) uploadAndInsert(images, 'image')
  if (docs.length) uploadAndInsert(docs, 'doc')
}

async function uploadAndInsert(files, type) {
  const endpoint = type === 'image' ? '/admin/upload/image' : '/admin/upload/document'
  for (const file of files) {
    const formData = new FormData()
    formData.append('file', file)
    try {
      const res = await request.post(endpoint, formData)
      const url = res.url
      const name = file.name
      let md = ''
      if (type === 'image') {
        md = `![${name}](${url})`
      } else {
        const ext = name.split('.').pop()?.toUpperCase() || 'FILE'
        md = `[${ext} - ${name}](${url})`
      }
      // Insert at cursor position
      if (editorRef.value?.insert) {
        editorRef.value.insert(() => ({ targetValue: md, select: false }))
      } else {
        form.description = form.description ? form.description + '\n\n' + md : md
      }
      ElMessage.success(t('adminTimeline.fileInserted', { name }))
    } catch {
      ElMessage.error(t('adminTimeline.uploadFailed'))
    }
  }
  showUploadDialog.value = false
}

async function toggleStatus(entry) {
  if (pendingIds.has(entry.id)) return
  pendingIds.add(entry.id)
  try {
    const res = await request.patch(`/admin/timeline-entries/${entry.id}/status`)
    entry.status = res.status
    ElMessage.success(res.status === 'PUBLISHED' ? t('adminTimeline.publishedSuccess') : t('adminTimeline.unpublishedSuccess'))
  } catch (e) {
    if (e.code === 8002) {
      ElMessage({ message: e.message, type: 'warning', duration: 5000 })
    } else {
      ElMessage.error(t('adminTimeline.operationFailed'))
    }
  } finally {
    pendingIds.delete(entry.id)
  }
}

async function handleDelete(entry) {
  if (pendingIds.has(entry.id)) return
  pendingIds.add(entry.id)
  try {
    await ElMessageBox.confirm(
      t('adminTimeline.confirmDeleteMsg', { name: entry.title }),
      t('adminTimeline.confirmDeleteTitle'),
      { type: 'warning' }
    )
    await request.delete(`/admin/timeline-entries/${entry.id}`)
    ElMessage.success(t('adminTimeline.deleteSuccess'))
    loadEntries()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminTimeline.deleteFailed'))
  } finally {
    pendingIds.delete(entry.id)
  }
}

async function bulkToggleStatus() {
  try {
    await ElMessageBox.confirm(
      t('adminTimeline.confirmBulkToggleMsg', { count: selectedIds.value.length }),
      t('adminTimeline.confirmBulkToggleTitle'),
      { type: 'warning' }
    )
  } catch { return }
  const results = await Promise.allSettled(selectedIds.value.map(id =>
    request.patch(`/admin/timeline-entries/${id}/status`)
  ))
  const succeeded = results.filter(r => r.status === 'fulfilled').length
  const failed = results.filter(r => r.status === 'rejected').length
  if (failed === 0) {
    ElMessage.success(t('adminTimeline.bulkSuccess'))
  } else {
    ElMessage.warning(t('adminTimeline.bulkPartialSuccess', { succeeded, failed }))
  }
  selectedIds.value = []
  loadEntries()
}

async function bulkDelete() {
  try {
    await ElMessageBox.confirm(
      t('adminTimeline.confirmBulkDeleteMsg', { count: selectedIds.value.length }),
      t('adminTimeline.confirmDeleteTitle'),
      { type: 'warning' }
    )
  } catch { return }
  const results = await Promise.allSettled(selectedIds.value.map(id =>
    request.delete(`/admin/timeline-entries/${id}`)
  ))
  const succeeded = results.filter(r => r.status === 'fulfilled').length
  const failed = results.filter(r => r.status === 'rejected').length
  if (failed === 0) {
    ElMessage.success(t('adminTimeline.deleteSuccess'))
  } else {
    ElMessage.warning(t('adminTimeline.bulkPartialSuccess', { succeeded, failed }))
  }
  selectedIds.value = []
  loadEntries()
}

// === AI Assist ===
const MOMENTS_SYSTEM_PROMPT = `你是一个专业的社交媒体内容创作助手，专门帮助用户撰写和优化"动态"内容（类似朋友圈、微博、推特等短内容）。

能力范围：
- 根据主题生成生动有趣的动态文案
- 润色和优化短文本（改进表达、增强感染力）
- 扩展简短想法为完整的动态内容
- 添加合适的 emoji 表情增强表达力
- 调整语气风格（轻松/正式/幽默/文艺/专业）
- 压缩冗长内容为精炼短文
- 生成话题标签（hashtags）
- 推荐适合的心情/情绪标签
- 中英文双向翻译（保持社交媒体风格）
- SEO 优化（标题、关键词建议）

输出要求：
- 使用 Markdown 格式输出
- 保持社交媒体的轻松活泼风格
- 内容简洁有力，适合快速阅读
- 适当使用 emoji 但不过度
- 标签放在内容末尾`

const aiDrawerVisible = ref(false)
const aiPrompt = ref('')
const aiResult = ref('')
const aiLoading = ref(false)
const aiError = ref('')
const aiMode = ref('')
const aiResultSection = ref(null)
const showCustomPresetManager = ref(false)
const showTemplateManager = ref(false)
const showKnowledgeManager = ref(false)
const showHistoryManager = ref(false)
const showAgentWriter = ref(false)
const customPresets = ref([])
const templateManagerRef = ref(null)
const presetSearch = ref('')
const presetCategory = ref('all')

watch(aiResult, () => {
  nextTick(() => { if (aiResultSection.value) injectCopyButtons(aiResultSection.value) })
})
const chatHistory = ref([])
const previousContent = ref('')
const selectedRange = ref(null)
const compareMode = ref(false)
const modelInfo = ref(null)
const continueInput = ref('')
let abortFn = null

function detectLanguage(text) {
  if (!text) return 'zh'
  const chineseChars = (text.match(/[一-鿿]/g) || []).length
  return chineseChars > text.length * 0.15 ? 'zh' : 'en'
}

const aiPresets = computed(() => {
  const builtIn = [
    // Moments-specific presets
    { id: 'generateMoment', icon: ' ', label: t('adminTimeline.aiPresetGenerateMoment'), needsTitle: true, category: 'writing' },
    { id: 'polishMoment', icon: ' ', label: t('adminTimeline.aiPresetPolishMoment'), needsContent: true, localOp: true, category: 'writing' },
    { id: 'expandMoment', icon: ' ', label: t('adminTimeline.aiPresetExpandMoment'), needsContent: true, localOp: true, category: 'writing' },
    { id: 'condense', icon: ' ', label: t('adminTimeline.aiPresetCondense'), needsContent: true, localOp: true, category: 'writing' },
    { id: 'emojiSuggest', icon: ' ', label: t('adminTimeline.aiPresetEmojiSuggest'), needsContent: true, category: 'enhance' },
    { id: 'moodSuggest', icon: ' ', label: t('adminTimeline.aiPresetMoodSuggest'), needsContent: true, category: 'enhance' },
    { id: 'hashtag', icon: '#', label: t('adminTimeline.aiPresetHashtag'), needsContent: true, category: 'enhance' },
    { id: 'toneCasual', icon: ' ', label: t('adminTimeline.aiPresetToneCasual'), needsContent: true, localOp: true, category: 'tone' },
    { id: 'toneProfessional', icon: ' ', label: t('adminTimeline.aiPresetToneProfessional'), needsContent: true, localOp: true, category: 'tone' },
    { id: 'toneHumorous', icon: ' ', label: t('adminTimeline.aiPresetToneHumorous'), needsContent: true, localOp: true, category: 'tone' },
    { id: 'toneLiterary', icon: ' ', label: t('adminTimeline.aiPresetToneLiterary'), needsContent: true, localOp: true, category: 'tone' },
    // General presets (from ArticleEdit)
    { id: 'generate', icon: ' ', label: t('adminTimeline.aiPresetGenerate'), needsTitle: true, category: 'writing' },
    { id: 'polish', icon: ' ', label: t('adminTimeline.aiPresetPolish'), needsContent: true, localOp: true, category: 'writing' },
    { id: 'translate', icon: ' ', label: t('adminTimeline.aiPresetTranslate'), needsContent: true, category: 'translate' },
    { id: 'translateZH', icon: ' ', label: t('adminTimeline.aiPresetTranslateZH'), needsContent: true, localOp: true, category: 'translate' },
    { id: 'translateEN', icon: ' ', label: t('adminTimeline.aiPresetTranslateEN'), needsContent: true, localOp: true, category: 'translate' },
    { id: 'summary', icon: ' ', label: t('adminTimeline.aiPresetSummary'), needsContent: true, category: 'analysis' },
    { id: 'title', icon: ' ', label: t('adminTimeline.aiPresetTitle'), needsContent: true, category: 'writing' },
  ]
  const custom = customPresets.value.map(p => ({
    id: p.id,
    icon: p.icon,
    label: p.name,
    needsTitle: p.needsTitle,
    needsContent: p.needsContent,
    localOp: p.localOp,
    isCustom: true,
    prompt: p.prompt,
    category: p.category || 'custom',
  }))
  return [...builtIn, ...custom]
})

const filteredPresets = computed(() => {
  let presets = aiPresets.value
  if (presetCategory.value !== 'all') {
    presets = presets.filter(p => p.category === presetCategory.value)
  }
  if (presetSearch.value.trim()) {
    const search = presetSearch.value.trim().toLowerCase()
    presets = presets.filter(p => p.label.toLowerCase().includes(search))
  }
  return presets
})

function loadCustomPresets() {
  customPresets.value = getCustomPresets()
}

function onCustomPresetUpdate() {
  loadCustomPresets()
}

function onTemplateSelect(template) {
  showTemplateManager.value = false
  aiPrompt.value = template.filledPrompt
}

function onHistorySelect(item) {
  showHistoryManager.value = false
  aiResult.value = item.response
  aiMode.value = item.type || 'chat'
}

function onHistoryReuse(item) {
  showHistoryManager.value = false
  aiPrompt.value = item.prompt
}

function onAgentComplete(result) {
  showAgentWriter.value = false
  form.description = result
  ElMessage.success(t('ai.agent.completed'))
}

const LOCAL_OP_PRESETS = ['polishMoment', 'expandMoment', 'condense', 'toneCasual', 'toneProfessional', 'toneHumorous', 'toneLiterary', 'polish', 'translateZH', 'translateEN']

function getSelectedText() {
  const sel = window.getSelection()
  if (!sel || sel.isCollapsed || !sel.toString().trim()) return null
  const text = sel.toString().trim()
  const idx = form.description.indexOf(text)
  if (idx !== -1) {
    selectedRange.value = { start: idx, end: idx + text.length }
  }
  return text
}

function buildContext() {
  const parts = []
  const lang = detectLanguage(form.description)
  parts.push(`[内容语言: ${lang === 'zh' ? '中文' : 'English'}]`)
  if (form.title) parts.push(`【标题】\n${form.title}`)

  const selectedText = getSelectedText()
  if (selectedText) {
    parts.push(`【选中的文本】\n${selectedText}`)
  } else {
    if (form.description) parts.push(`【动态内容】\n${form.description}`)
  }
  if (form.category) parts.push(`【分类】\n${form.category}`)
  return parts.join('\n\n')
}

function buildSystemPrompt() {
  const lang = detectLanguage(form.description)
  let prompt = MOMENTS_SYSTEM_PROMPT
  if (lang === 'en') {
    prompt += '\n\nThe content is written in English. Please respond in English.'
  } else {
    prompt += '\n\n内容是中文撰写的，请用中文回复。'
  }
  const kbContext = buildKnowledgeContext()
  if (kbContext) {
    prompt += '\n\n' + kbContext
  }
  return prompt
}

function runPreset(mode) {
  aiMode.value = mode
  aiError.value = ''
  aiResult.value = ''
  selectedRange.value = null
  compareMode.value = false
  modelInfo.value = null

  const ctx = buildContext()
  const selectedText = getSelectedText()
  const isLocalOp = LOCAL_OP_PRESETS.includes(mode) && selectedText

  // Check if this is a custom preset
  const customPreset = customPresets.value.find(p => p.id === mode)
  if (customPreset) {
    const useLocal = customPreset.localOp && selectedText
    const contextBlock = useLocal
      ? `【待处理的文本】\n${selectedText}`
      : ctx
    const userMsg = `${customPreset.prompt}\n\n${contextBlock}`
    chatHistory.value = [{ role: 'user', content: userMsg }]
    runAi(userMsg)
    return
  }

  const prompts = {
    // Moments-specific
    generateMoment: `${t('adminTimeline.aiPromptGenerateMoment')}\n\n${ctx}`,
    polishMoment: isLocalOp
      ? `${t('adminTimeline.aiPromptPolishMomentLocal')}\n\n【待润色的文本】\n${selectedText}`
      : `${t('adminTimeline.aiPromptPolishMoment')}\n\n${ctx}`,
    expandMoment: isLocalOp
      ? `${t('adminTimeline.aiPromptExpandMomentLocal')}\n\n【待扩展的文本】\n${selectedText}`
      : `${t('adminTimeline.aiPromptExpandMoment')}\n\n${ctx}`,
    condense: isLocalOp
      ? `${t('adminTimeline.aiPromptCondenseLocal')}\n\n【待压缩的文本】\n${selectedText}`
      : `${t('adminTimeline.aiPromptCondense')}\n\n${ctx}`,
    emojiSuggest: `${t('adminTimeline.aiPromptEmojiSuggest')}\n\n${ctx}`,
    moodSuggest: `${t('adminTimeline.aiPromptMoodSuggest')}\n\n${ctx}`,
    hashtag: `${t('adminTimeline.aiPromptHashtag')}\n\n${ctx}`,
    toneCasual: isLocalOp
      ? `${t('adminTimeline.aiPromptToneCasualLocal')}\n\n【待改写的文本】\n${selectedText}`
      : `${t('adminTimeline.aiPromptToneCasual')}\n\n${ctx}`,
    toneProfessional: isLocalOp
      ? `${t('adminTimeline.aiPromptToneProfessionalLocal')}\n\n【待改写的文本】\n${selectedText}`
      : `${t('adminTimeline.aiPromptToneProfessional')}\n\n${ctx}`,
    toneHumorous: isLocalOp
      ? `${t('adminTimeline.aiPromptToneHumorousLocal')}\n\n【待改写的文本】\n${selectedText}`
      : `${t('adminTimeline.aiPromptToneHumorous')}\n\n${ctx}`,
    toneLiterary: isLocalOp
      ? `${t('adminTimeline.aiPromptToneLiteraryLocal')}\n\n【待改写的文本】\n${selectedText}`
      : `${t('adminTimeline.aiPromptToneLiterary')}\n\n${ctx}`,
    // General
    generate: `${t('adminTimeline.aiPromptGenerate')}\n\n${ctx}`,
    polish: isLocalOp
      ? `${t('adminTimeline.aiPromptPolishLocal')}\n\n【待润色的文本】\n${selectedText}`
      : `${t('adminTimeline.aiPromptPolish')}\n\n${ctx}`,
    translate: `${t('adminTimeline.aiPromptTranslate')}\n\n${ctx}`,
    translateZH: isLocalOp
      ? `${t('adminTimeline.aiPromptTranslateZHLocal')}\n\n【待翻译的文本】\n${selectedText}`
      : `${t('adminTimeline.aiPromptTranslateZH')}\n\n${ctx}`,
    translateEN: isLocalOp
      ? `${t('adminTimeline.aiPromptTranslateENLocal')}\n\n【待翻译的文本】\n${selectedText}`
      : `${t('adminTimeline.aiPromptTranslateEN')}\n\n${ctx}`,
    summary: `${t('adminTimeline.aiPromptSummary')}\n\n${ctx}`,
    title: `${t('adminTimeline.aiPromptTitle')}\n\n${ctx}`,
  }

  const userMsg = prompts[mode]
  chatHistory.value = [{ role: 'user', content: userMsg }]
  runAi(userMsg)
}

function runCustom() {
  if (!aiPrompt.value.trim()) return
  aiMode.value = 'custom'
  aiError.value = ''
  aiResult.value = ''
  selectedRange.value = null
  compareMode.value = false
  modelInfo.value = null

  const ctx = buildContext()
  const msg = `${aiPrompt.value}\n\n---\n${t('adminTimeline.aiContextLabel')}\n${ctx}`
  chatHistory.value = [{ role: 'user', content: msg }]
  runAi(msg)
}

function continueChat() {
  if (!continueInput.value.trim() || aiLoading.value) return
  const userMsg = continueInput.value.trim()
  continueInput.value = ''

  chatHistory.value.push({ role: 'user', content: userMsg })
  aiResult.value = ''
  aiError.value = ''
  compareMode.value = false
  modelInfo.value = null

  const messages = chatHistory.value.map(m => ({ role: m.role, content: m.content }))
  const systemPrompt = buildSystemPrompt()

  aiLoading.value = true
  abortFn = aiEditorStream({ messages, systemPrompt, maxTokens: 8192 }, {
    onChunk(content) { aiResult.value += content },
    onDone() {
      aiLoading.value = false
      abortFn = null
      chatHistory.value.push({ role: 'assistant', content: aiResult.value })
    },
    onError(err) { aiError.value = String(err); aiLoading.value = false; abortFn = null }
  })
}

function runAi(message) {
  const systemPrompt = buildSystemPrompt()
  const messages = [{ role: 'user', content: message }]

  aiLoading.value = true
  abortFn = aiEditorStream({ messages, systemPrompt, maxTokens: 8192 }, {
    onChunk(content) { aiResult.value += content },
    onDone() {
      aiLoading.value = false
      abortFn = null
      chatHistory.value.push({ role: 'assistant', content: aiResult.value })
      addHistory({
        type: aiMode.value || 'chat',
        preset: aiMode.value,
        prompt: message,
        response: aiResult.value,
        contentType: 'moments',
      })
    },
    onError(err) { aiError.value = String(err); aiLoading.value = false; abortFn = null }
  })
}

function cancelAi() {
  if (abortFn) { abortFn(); abortFn = null }
  aiLoading.value = false
}

function closeAiDrawer(done) {
  cancelAi()
  aiResult.value = ''
  aiError.value = ''
  aiPrompt.value = ''
  aiMode.value = ''
  chatHistory.value = []
  previousContent.value = ''
  selectedRange.value = null
  compareMode.value = false
  modelInfo.value = null
  continueInput.value = ''
  done()
}

function insertToContent() {
  if (!aiResult.value) return
  form.description = form.description ? form.description + '\n\n' + aiResult.value : aiResult.value
  ElMessage.success(t('adminTimeline.insertedMsg'))
}

async function replaceContent() {
  if (!aiResult.value) return
  try {
    await ElMessageBox.confirm(
      t('adminTimeline.confirmReplaceMsg'),
      t('adminTimeline.confirmReplaceTitle'),
      { confirmButtonText: t('adminTimeline.confirm'), cancelButtonText: t('adminTimeline.cancel'), type: 'warning' }
    )
  } catch { return }
  previousContent.value = form.description
  form.description = aiResult.value
  ElMessage({ message: t('adminTimeline.replacedMsg'), type: 'success', duration: 5000, showClose: true })
}

function undoReplace() {
  if (!previousContent.value) return
  form.description = previousContent.value
  previousContent.value = ''
  ElMessage.success(t('adminTimeline.undoSuccessMsg'))
}

function replaceSelected() {
  if (!aiResult.value || !selectedRange.value) return
  const { start, end } = selectedRange.value
  const before = form.description.slice(0, start)
  const after = form.description.slice(end)
  form.description = before + aiResult.value + after
  selectedRange.value = null
  ElMessage.success(t('adminTimeline.replacedSelectedMsg'))
}

// Keyboard shortcuts
function onGlobalKeydown(e) {
  if (e.ctrlKey && e.shiftKey && e.key === 'A') {
    e.preventDefault()
    if (showForm.value) aiDrawerVisible.value = !aiDrawerVisible.value
  }
}

onMounted(() => {
  loadEntries()
  loadCustomPresets()
  document.addEventListener('keydown', onGlobalKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', onGlobalKeydown)
  if (abortFn) { abortFn(); abortFn = null }
  if (searchTimer) { clearTimeout(searchTimer); searchTimer = null }
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}
.result-count {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 12px;
}
.desc-preview {
  font-size: 13px;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.edit-page-content {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px - 48px);
}
.edit-layout {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}
.edit-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.edit-sidebar {
  width: 340px;
  flex-shrink: 0;
  overflow-y: auto;
  position: sticky;
  top: 80px;
}
/* Tags chip editor */
.tags-editor {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  padding: 6px 10px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background: var(--el-bg-color);
  min-height: 32px;
  cursor: text;
  transition: border-color 0.2s;
}
.tags-editor:focus-within {
  border-color: var(--el-color-primary);
}
.tag-input {
  flex: 1;
  min-width: 80px;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  padding: 2px 0;
  color: var(--el-text-color-regular);
}
.tag-input::placeholder {
  color: var(--el-text-color-placeholder);
}

.title-input :deep(.el-input__inner) {
  font-size: 1.25rem;
  font-weight: 600;
  padding: 12px;
}
.md-editor-moments {
  flex: 1;
  min-height: 0;
}

/* ===== Upload Dialog ===== */
.upload-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.upload-options {
  display: flex;
  gap: 12px;
}
.upload-drop-area {
  border: 2px dashed var(--el-border-color);
  border-radius: 12px;
  padding: 32px;
  text-align: center;
  transition: box-shadow 0.3s, transform 0.3s;
  cursor: pointer;
  background: var(--el-fill-color-lighter);
}
.upload-drop-area:hover,
.upload-drop-area.drag-active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.upload-icon {
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}
.upload-text {
  font-size: 14px;
  color: var(--el-text-color-regular);
  margin: 0 0 4px;
}
.upload-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin: 0;
}

/* ===== AI Drawer Styles ===== */
.ai-drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.ai-preset-search {
  margin-bottom: 4px;
}
.ai-preset-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.category-tag {
  padding: 4px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s;
  font-size: 12px;
  color: var(--el-text-color-regular);
}
.category-tag:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}
.category-tag.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
  color: #fff;
}
.ai-presets {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}
.ai-preset-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 4px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s;
  font-size: 12px;
  color: var(--el-text-color-regular);
}
.ai-preset-btn:hover:not(:disabled) {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.ai-preset-btn.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.ai-preset-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.ai-preset-icon { font-size: 18px; }
.ai-preset-label { font-size: 11px; line-height: 1.3; text-align: center; }
.manage-btn {
  border-style: dashed;
  opacity: 0.7;
}
.manage-btn:hover {
  opacity: 1;
}

.ai-custom-section {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
}
.ai-prompt-input {
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
  resize: vertical;
  font-family: inherit;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
}
.ai-prompt-input:focus {
  outline: none;
  border-color: var(--el-color-primary);
}
.ai-generate-btn {
  padding: 6px 16px;
  border-radius: 6px;
  border: none;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: opacity 0.2s;
}
.ai-generate-btn:hover:not(:disabled) { opacity: 0.85; }
.ai-generate-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.ai-shortcut-hint {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
}

.ai-result-section {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
}
.ai-result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.ai-result-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.ai-result-preview {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
  background: var(--el-fill-color-lighter);
}

/* Compare mode */
.ai-compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  max-height: 400px;
}
.ai-compare-pane {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}
.ai-compare-label {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 8px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.ai-compare-content {
  padding: 8px;
  max-height: 360px;
  overflow-y: auto;
}

/* Chat messages */
.ai-chat-msg { margin-bottom: 8px; }
.ai-chat-msg.user .ai-chat-bubble {
  background: var(--el-color-primary-light-9);
  border-radius: 8px 8px 2px 8px;
  padding: 8px 12px;
  font-size: 13px;
  max-width: 90%;
  margin-left: auto;
}
.ai-chat-msg.assistant .ai-chat-bubble {
  background: var(--el-fill-color-lighter);
  border-radius: 8px 8px 8px 2px;
  padding: 8px 12px;
  font-size: 13px;
}

/* Typing indicator */
.ai-typing {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}
.ai-typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary);
  animation: typing-bounce 1.4s infinite ease-in-out;
}
.ai-typing span:nth-child(2) { animation-delay: 0.2s; }
.ai-typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

.ai-model-info {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  text-align: right;
  margin-top: 4px;
}

.ai-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.ai-action-btn {
  padding: 4px 12px;
  border-radius: 6px;
  border: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s;
  color: var(--el-text-color-regular);
}
.ai-action-btn:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}
.ai-action-btn.primary {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
  color: #fff;
}
.ai-action-btn.primary:hover { opacity: 0.85; }

.ai-cancel-btn {
  padding: 2px 10px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  font-size: 12px;
  cursor: pointer;
  color: var(--el-text-color-regular);
}
.ai-cancel-btn:hover {
  border-color: var(--el-color-danger);
  color: var(--el-color-danger);
}

.ai-continue-section {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ai-error {
  padding: 8px 12px;
  background: var(--el-color-error-light-9);
  border: 1px solid var(--el-color-error-light-7);
  border-radius: 6px;
  color: var(--el-color-error);
  font-size: 13px;
}

@media (max-width: 900px) {
  .edit-layout { flex-direction: column; }
  .edit-sidebar { width: 100%; position: static; }
  .ai-presets { grid-template-columns: repeat(2, 1fr); }
  .ai-compare { grid-template-columns: 1fr; }
}
</style>
