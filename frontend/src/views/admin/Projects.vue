<template>
  <div class="admin-page">
    <!-- List view -->
    <template v-if="!showForm">
      <div class="page-header">
        <h2>{{ $t('adminProjects.pageTitle') }}</h2>
        <div class="header-actions">
          <RefreshButton :onRefresh="loadProjects" />
          <el-button v-if="selectedIds.length > 0" type="danger" @click="bulkDelete">
            {{ $t('adminProjects.bulkDelete') }} ({{ selectedIds.length }})
          </el-button>
          <el-button v-if="authStore.hasPermission('project:manage')" type="primary" @click="startAdd">
            <el-icon><Plus /></el-icon> {{ $t('adminProjects.createProject') }}
          </el-button>
        </div>
      </div>

      <!-- Search bar -->
      <div class="cir-search-wrap" style="margin-bottom: 16px">
        <div class="cir-search" :class="{ 'ai-active': aiMode }">
          <button class="cir-search-btn" @click="aiMode = !aiMode" :title="aiMode ? $t('adminProjects.disableAiSearch') : $t('adminProjects.enableAiSearch')">
            <svg v-if="!aiMode" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
            <svg v-else class="ai-icon-spin" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a4 4 0 0 1 4 4c0 1.95-1.4 3.58-3.25 3.93L12 22"/><path d="M12 2a4 4 0 0 0-4 4c0 1.95 1.4 3.58 3.25 3.93"/><path d="M2 12h4m12 0h4"/><circle cx="12" cy="12" r="1"/></svg>
          </button>
          <input v-model="keyword" type="text" class="cir-search-input"
            :placeholder="aiMode ? $t('adminProjects.aiSearchPlaceholder') : $t('adminProjects.searchPlaceholder')"
            @input="onKeywordInput" @keydown.enter="doSearch" />
          <div class="cir-search-right">
            <button v-if="keyword" class="cir-clear-btn" @click="keyword = ''; doSearch()">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
            </button>
            <button class="cir-go-btn" @click="doSearch" :disabled="searching">
              <span v-if="searching" class="go-spinner"></span>
              <span v-else>{{ aiMode ? 'AI' : $t('adminProjects.search') }}</span>
            </button>
          </div>
        </div>
        <button class="cir-filter-btn" :class="{ active: showFilters || activeFilterCount > 0 }" @click="showFilters = !showFilters">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
          <span>{{ $t('adminProjects.filter') }}</span>
          <span v-if="activeFilterCount > 0" class="filter-badge">{{ activeFilterCount }}</span>
        </button>
      </div>

      <!-- Filter panel -->
      <el-collapse-transition>
        <div v-if="showFilters" class="filter-panel">
          <DropdownMenu :label="$t('adminProjects.statusLabel')" :items="statusOptions" v-model="filterStatus" @change="page = 0; loadProjects()" />
          <DropdownMenu :label="$t('adminProjects.sortLabel')" :items="sortOptions" v-model="sortBy" @change="page = 0; loadProjects()" />
          <el-button text size="small" @click="clearFilters" style="margin-left: 8px">{{ $t('adminProjects.clearFilters') }}</el-button>
        </div>
      </el-collapse-transition>

      <!-- Result count -->
      <div v-if="totalElements > 0" style="font-size: 13px; color: #94a3b8; margin-bottom: 12px">
        {{ $t('adminProjects.foundProjects', { count: totalElements }) }}
      </div>

      <el-card>
        <el-table :data="projects" row-key="id" stripe v-loading="loading" :empty-text="$t('adminProjects.emptyText')"
          @selection-change="onSelectionChange" ref="tableRef">
          <el-table-column type="selection" width="45" />
          <el-table-column :label="$t('adminProjects.cover')" width="70">
            <template #default="{ row }">
              <el-image v-if="row.coverImage" :src="row.coverImage" lazy style="width: 48px; height: 32px; border-radius: 4px" fit="cover" />
              <span v-else style="color: #94a3b8">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" :label="$t('adminProjects.name')" min-width="150">
            <template #default="{ row }">
              <div style="font-weight: 500">{{ row.name }}</div>
              <div v-if="row.subtitle" style="font-size: 12px; color: #94a3b8; margin-top: 2px">{{ row.subtitle }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="$t('adminProjects.status')" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" effect="plain" size="small" @click="toggleStatus(row)" style="cursor: pointer">
                {{ row.status === 'PUBLISHED' ? $t('adminProjects.published') : $t('adminProjects.draft') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="isFeatured" :label="$t('adminProjects.pin')" width="80">
            <template #default="{ row }">
              <el-button size="small" :disabled="pendingIds.has(row.id)" @click="toggleFeatured(row)">{{ row.isFeatured ? $t('adminProjects.unpin') : $t('adminProjects.pin') }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="techStack" :label="$t('adminProjects.techStack')" width="160" show-overflow-tooltip />
          <el-table-column prop="sortOrder" :label="$t('adminProjects.sortOrder')" width="70" />
          <el-table-column prop="createdAt" :label="$t('adminProjects.createdAt')" width="170">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('adminProjects.action')" width="160" fixed="right">
            <template #default="{ row }">
              <el-button v-if="authStore.hasPermission('project:manage')" type="primary" size="small" @click="startEdit(row)">{{ $t('adminProjects.edit') }}</el-button>
              <el-button v-if="authStore.hasPermission('project:manage')" type="danger" size="small" :disabled="pendingIds.has(row.id)" @click="handleDelete(row)">{{ $t('adminProjects.delete') }}</el-button>
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

    <!-- Edit view (full page) -->
    <template v-else>
      <div class="project-edit-page">
      <div class="page-header">
        <h2>{{ editingId ? $t('adminProjects.editProject') : $t('adminProjects.createProject') }}</h2>
        <div style="display: flex; gap: 8px">
          <el-button @click="aiDrawerVisible = true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align: -2px; margin-right: 4px"><path d="M12 2a4 4 0 0 0-4 4v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2h-2V6a4 4 0 0 0-4-4z"/><circle cx="9" cy="14" r="1"/><circle cx="15" cy="14" r="1"/></svg>
            {{ $t('adminProjects.aiAssist') }}
          </el-button>
          <el-button @click="showForm = false">{{ $t('adminProjects.cancel') }}</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">
            {{ editingId ? $t('adminProjects.save') : $t('adminProjects.create') }}
          </el-button>
        </div>
      </div>

      <div class="project-edit-layout">
        <div class="project-edit-main">
          <el-input v-model="form.name" :placeholder="$t('adminProjects.projectNamePlaceholder')" class="title-input" />
          <el-input v-model="form.subtitle" :placeholder="$t('adminProjects.subtitlePlaceholder')" style="margin-bottom: 12px" />
          <div class="project-editors">
            <div class="editor-section">
              <div class="editor-section-label">{{ $t('adminProjects.descriptionLabel') }}</div>
              <MdEditor v-model="form.description" previewTheme="github" :preview="true" :toolbarsExclude="['github']" class="md-editor-desc" @onUploadImg="onUploadImg" />
            </div>
            <div class="editor-section">
              <div class="editor-section-label">{{ $t('adminProjects.featuresLabel') }}</div>
              <MdEditor v-model="form.features" previewTheme="github" :preview="true" :toolbarsExclude="['github']" class="md-editor-features" @onUploadImg="onUploadImg" />
            </div>
          </div>
        </div>

        <div class="project-edit-sidebar">
          <el-card shadow="never" style="margin-bottom: 12px">
            <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminProjects.coverImageLabel') }}</span></template>
            <div style="display: flex; flex-direction: column; gap: 8px">
              <el-image v-if="form.coverImage" :src="form.coverImage" style="width: 100%; max-height: 160px; border-radius: 6px" fit="cover" />
              <template v-if="form.coverImage">
                <div style="display: flex; gap: 8px">
                  <el-button size="small" @click="form.coverImage = ''">{{ $t('adminProjects.changeCover') }}</el-button>
                  <el-button type="danger" text size="small" @click="form.coverImage = ''">{{ $t('adminProjects.remove') }}</el-button>
                </div>
              </template>
              <FileUpload v-else endpoint="/admin/upload/cover" accept="image/*" @uploaded="onCoverUploaded" style="height: 180px" />
            </div>
          </el-card>

          <el-card shadow="never" style="margin-bottom: 12px">
            <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminProjects.screenshotsLabel') }}</span></template>
            <div style="display: flex; flex-direction: column; gap: 8px">
              <div v-if="form.screenshots.length" class="shot-grid">
                <div v-for="(url, i) in form.screenshots" :key="i" class="shot-thumb">
                  <el-image :src="url" fit="cover" style="width: 100%; height: 100%; border-radius: 6px" />
                  <button class="shot-remove" @click="removeScreenshot(i)" :title="$t('adminProjects.remove')">&times;</button>
                </div>
              </div>
              <FileUpload v-if="form.screenshots.length < 9" endpoint="/admin/upload/image" accept="image/*" multiple @all-uploaded="onScreenshotsUploaded" style="height: 120px" />
            </div>
          </el-card>

          <el-card shadow="never" style="margin-bottom: 12px">
            <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminProjects.basicInfoLabel') }}</span></template>
            <el-form :model="form" label-position="top">
              <el-form-item :label="$t('adminProjects.techStack')">
                <el-input v-model="form.techStack" placeholder="Spring Boot, Vue 3, MySQL" />
              </el-form-item>
              <el-form-item :label="$t('adminProjects.sortLabel')">
                <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item :label="$t('adminProjects.statusLabel')">
                <el-select v-model="form.status" style="width: 100%">
                  <el-option :label="$t('adminProjects.draft')" value="DRAFT" />
                  <el-option :label="$t('adminProjects.published')" value="PUBLISHED" />
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('adminProjects.featuredLabel')">
                <el-switch v-model="form.isFeatured" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never">
            <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminProjects.linksLabel') }}</span></template>
            <el-form :model="form" label-position="top">
              <el-form-item label="GitHub">
                <el-input v-model="form.githubUrl" placeholder="https://github.com/..." />
              </el-form-item>
              <el-form-item :label="$t('adminProjects.demoLabel')">
                <el-input v-model="form.demoUrl" placeholder="https://..." />
              </el-form-item>
              <el-form-item :label="$t('adminProjects.subdomainLabel')">
                <el-input v-model="form.subdomainUrl" placeholder="https://xxx.example.com" />
              </el-form-item>
            </el-form>
          </el-card>
        </div>
      </div>
      </div>
    </template>

    <!-- AI Assist Drawer -->
    <el-drawer v-model="aiDrawerVisible" :title="$t('adminProjects.aiAssistTitle')" direction="rtl" size="480px" :before-close="closeAiDrawer">
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
          <button class="category-tag" :class="{ active: presetCategory === 'tone' }" @click="presetCategory = 'tone'">{{ $t('ai.category.tone') }}</button>
        </div>

        <!-- Preset modes -->
        <div class="ai-presets">
          <button v-for="p in filteredPresets" :key="p.id"
            class="ai-preset-btn" :class="{ active: aiDrawerMode === p.id }"
            :disabled="aiLoading || (p.needsName && !form.name.trim()) || (p.needsDescription && !form.description.trim())"
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
            :placeholder="$t('adminProjects.customPromptPlaceholder')"
            @keydown.ctrl.enter="runCustom" :disabled="aiLoading" />
          <div style="display: flex; align-items: center; gap: 8px; margin-top: 6px">
            <button class="ai-generate-btn" :disabled="aiLoading || !aiPrompt.trim()" @click="runCustom">
              {{ aiLoading ? $t('adminProjects.generating') : $t('adminProjects.generate') }}
            </button>
            <span class="ai-shortcut-hint">Ctrl+Enter</span>
          </div>
        </div>

        <!-- Result preview -->
        <div v-if="aiResult || aiLoading || chatHistory.length > 0" ref="aiResultSection" class="ai-result-section">
          <div class="ai-result-header">
            <span class="ai-result-title">{{ $t('adminProjects.preview') }}</span>
            <div style="display: flex; gap: 4px">
              <button v-if="aiResult && !aiLoading" class="ai-cancel-btn" style="font-size: 12px; padding: 2px 8px" @click="compareMode = !compareMode">
                {{ compareMode ? $t('adminProjects.exitCompare') : $t('adminProjects.compareView') }}
              </button>
              <button v-if="aiLoading" class="ai-cancel-btn" @click="cancelAi">{{ $t('adminProjects.cancel') }}</button>
            </div>
          </div>

          <!-- Compare mode -->
          <div v-if="compareMode && aiResult && !aiLoading" class="ai-compare">
            <div class="ai-compare-pane">
              <div class="ai-compare-label">{{ $t('adminProjects.originalText') }}</div>
              <div class="ai-compare-content">
                <MdPreview :modelValue="selectedRange ? form.description.slice(selectedRange.start, selectedRange.end) : form.description" previewTheme="github" :codeFoldable="false" />
              </div>
            </div>
            <div class="ai-compare-pane">
              <div class="ai-compare-label">{{ $t('adminProjects.aiResult') }}</div>
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
            <button v-if="selectedRange" class="ai-action-btn primary" @click="replaceSelected">{{ $t('adminProjects.replaceSelected') }}</button>
            <button v-if="aiDrawerMode !== 'features'" class="ai-action-btn" @click="insertToDescription">{{ $t('adminProjects.insertToDescription') }}</button>
            <button v-if="aiDrawerMode !== 'features'" class="ai-action-btn" @click="replaceDescription">{{ $t('adminProjects.replaceDescription') }}</button>
            <button v-if="aiDrawerMode === 'features'" class="ai-action-btn" @click="insertToFeatures">{{ $t('adminProjects.insertToFeatures') }}</button>
            <button v-if="aiDrawerMode === 'features'" class="ai-action-btn" @click="replaceFeatures">{{ $t('adminProjects.replaceFeatures') }}</button>
            <CopyButton :text="aiResult" @copied="ElMessage.success(t('adminProjects.copiedMsg'))" />
            <button v-if="previousContent" class="ai-action-btn" @click="undoReplace" style="color: var(--el-color-warning)">{{ $t('adminProjects.undoReplace') }}</button>
          </div>

          <!-- Continue chat -->
          <div v-if="chatHistory.length > 0 && !aiLoading" class="ai-continue-section">
            <textarea v-model="continueInput" class="ai-prompt-input" rows="2"
              :placeholder="$t('adminProjects.continuePlaceholder')"
              @keydown.ctrl.enter="continueChat" />
            <button class="ai-generate-btn" :disabled="!continueInput.trim()" @click="continueChat">
              {{ $t('adminProjects.send') }}
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick, defineAsyncComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { Plus } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'
const MdEditor = defineAsyncComponent(() => import('md-editor-v3').then(m => m.MdEditor))
const MdPreview = defineAsyncComponent(() => import('md-editor-v3').then(m => m.MdPreview))
const CustomPresetManager = defineAsyncComponent(() => import('../../components/ai/CustomPresetManager.vue'))
const TemplateManager = defineAsyncComponent(() => import('../../components/ai/TemplateManager.vue'))
const KnowledgeManager = defineAsyncComponent(() => import('../../components/ai/KnowledgeManager.vue'))
const AIHistoryManager = defineAsyncComponent(() => import('../../components/ai/AIHistoryManager.vue'))
const AgentWriter = defineAsyncComponent(() => import('../../components/ai/AgentWriter.vue'))
import { getCustomPresets } from '../../ai/presets/custom'
import { buildKnowledgeContext } from '../../ai/knowledge/index'
import { addHistory } from '../../ai/history/index'
import 'md-editor-v3/lib/style.css'
import 'md-editor-v3/lib/preview.css'
import('../../utils/mdEditorConfig')
import DropdownMenu from '../../components/DropdownMenu.vue'
import { aiEditorStream } from '../../utils/ai'
import FileUpload from '../../components/FileUpload.vue'
import CopyButton from '../../components/CopyButton.vue'
import RefreshButton from '../../components/RefreshButton.vue'
import { injectCopyButtons } from '../../utils/copyUtils'

const authStore = useAuthStore()
const { t } = useI18n()

const projects = ref([])
const loading = ref(false)
const showForm = ref(false)
const editingId = ref(null)
const saving = ref(false)
const selectedIds = ref([])
const tableRef = ref(null)

// Search & filter state
const keyword = ref('')
const page = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const filterStatus = ref('')
const sortBy = ref('default')
const showFilters = ref(false)
const aiMode = ref(false)
const searching = ref(false)
let searchTimer = null
const pendingIds = new Set()

const statusOptions = computed(() => [
  { label: t('adminProjects.filterAll'), value: '' },
  { label: t('adminProjects.draft'), value: 'DRAFT' },
  { label: t('adminProjects.published'), value: 'PUBLISHED' }
])
const sortOptions = computed(() => [
  { label: t('adminProjects.sortDefault'), value: 'default' },
  { label: t('adminProjects.sortNewest'), value: 'newest' },
  { label: t('adminProjects.sortOldest'), value: 'oldest' }
])

const activeFilterCount = ref(0)
function updateFilterCount() {
  let c = 0
  if (filterStatus.value) c++
  if (sortBy.value !== 'default') c++
  activeFilterCount.value = c
}

function clearFilters() {
  filterStatus.value = ''
  sortBy.value = 'default'
  keyword.value = ''
  aiMode.value = false
  page.value = 0
  loadProjects()
}

function onSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

async function bulkDelete() {
  try {
    await ElMessageBox.confirm(
      t('adminProjects.confirmBulkDeleteMsg', { count: selectedIds.value.length }),
      t('adminProjects.confirmDeleteTitle'),
      { type: 'warning' }
    )
    await request.delete(`/admin/projects?ids=${selectedIds.value.join(',')}`)
    ElMessage.success(t('adminProjects.deleteSuccess'))
    selectedIds.value = []
    loadProjects()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminProjects.deleteFailed'))
  }
}

const defaultForm = () => ({
  name: '', subtitle: '', description: '', techStack: '',
  coverImage: '', githubUrl: '', demoUrl: '', subdomainUrl: '',
  features: '', sortOrder: 0, status: 'DRAFT', isFeatured: false,
  screenshots: []
})
const form = reactive(defaultForm())

// === AI Assist ===
const PROJECT_SYSTEM_PROMPT = `你是一个专业的项目展示内容创作助手，专门帮助用户撰写和优化项目介绍内容。

能力范围：
- 根据项目名称和技术栈生成专业的项目描述
- 润色和优化项目描述（改进表达、增强专业性）
- 生成项目功能特性列表
- 扩展简短描述为详细的项目介绍
- 压缩冗长描述为精炼版本
- 中英文双向翻译（保持技术文档的专业风格）
- 调整语气风格（技术/商业/轻松）
- 技术栈分析和建议

输出要求：
- 使用 Markdown 格式输出
- 保持专业、清晰的技术文档风格
- 结构化表达，便于用户快速获取信息
- 突出项目亮点和技术特色`

const aiDrawerVisible = ref(false)
const aiPrompt = ref('')
const aiResult = ref('')
const aiLoading = ref(false)
const aiError = ref('')
const aiDrawerMode = ref('')
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
const chatHistory = ref([])
const previousContent = ref('')
const previousField = ref('')
const selectedRange = ref(null)
const compareMode = ref(false)
const modelInfo = ref(null)
const continueInput = ref('')
let abortFn = null

watch(aiResult, () => {
  nextTick(() => { if (aiResultSection.value) injectCopyButtons(aiResultSection.value) })
})

function detectLanguage(text) {
  if (!text) return 'zh'
  const chineseChars = (text.match(/[一-鿿]/g) || []).length
  return chineseChars > text.length * 0.15 ? 'zh' : 'en'
}

const LOCAL_OP_PRESETS = ['polish', 'translateZH', 'translateEN', 'condense']

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

const aiPresets = computed(() => {
  const builtIn = [
    { id: 'generate', icon: '✨', label: t('adminProjects.aiPresetGenerate'), needsName: true, category: 'writing' },
    { id: 'features', icon: '⚡', label: t('adminProjects.aiPresetFeatures'), needsName: true, category: 'writing' },
    { id: 'polish', icon: ' ', label: t('adminProjects.aiPresetPolish'), needsDescription: true, localOp: true, category: 'writing' },
    { id: 'expand', icon: ' ', label: t('adminProjects.aiPresetExpand'), needsDescription: true, localOp: true, category: 'writing' },
    { id: 'condense', icon: ' ', label: t('adminProjects.aiPresetCondense'), needsDescription: true, localOp: true, category: 'writing' },
    { id: 'continue', icon: '✍️', label: t('adminProjects.aiPresetContinue'), needsDescription: true, category: 'writing' },
    { id: 'tech', icon: ' ️', label: t('adminProjects.aiPresetTech'), needsName: true, category: 'analysis' },
    { id: 'translate', icon: ' ', label: t('adminProjects.aiPresetTranslate'), needsDescription: true, category: 'translate' },
    { id: 'translateZH', icon: ' ', label: t('adminProjects.aiPresetTranslateZH'), needsDescription: true, localOp: true, category: 'translate' },
    { id: 'translateEN', icon: ' ', label: t('adminProjects.aiPresetTranslateEN'), needsDescription: true, localOp: true, category: 'translate' },
    { id: 'summary', icon: ' ', label: t('adminProjects.aiPresetSummary'), needsDescription: true, category: 'analysis' },
    { id: 'toneFormal', icon: ' ', label: t('adminProjects.aiPresetToneFormal'), needsDescription: true, localOp: true, category: 'tone' },
    { id: 'toneCasual', icon: ' ', label: t('adminProjects.aiPresetToneCasual'), needsDescription: true, localOp: true, category: 'tone' },
  ]
  const custom = customPresets.value.map(p => ({
    id: p.id, icon: p.icon, label: p.name, needsName: p.needsTitle, needsDescription: p.needsContent,
    localOp: p.localOp, isCustom: true, prompt: p.prompt, category: p.category || 'custom',
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

function buildAiContext() {
  const parts = []
  const lang = detectLanguage(form.description)
  parts.push(`[内容语言: ${lang === 'zh' ? '中文' : 'English'}]`)
  if (form.name) parts.push(`【项目名称】\n${form.name}`)
  if (form.subtitle) parts.push(`【副标题】\n${form.subtitle}`)

  const selectedText = getSelectedText()
  if (selectedText) {
    parts.push(`【选中的文本】\n${selectedText}`)
  } else {
    if (form.description) parts.push(`【项目描述】\n${form.description}`)
  }
  if (form.techStack) parts.push(`【技术栈】\n${form.techStack}`)
  if (form.features) parts.push(`【功能特性】\n${form.features.slice(0, 2000)}`)
  return parts.join('\n\n')
}

function buildSystemPrompt() {
  const lang = detectLanguage(form.description)
  let prompt = PROJECT_SYSTEM_PROMPT
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
  aiDrawerMode.value = mode
  aiError.value = ''
  aiResult.value = ''
  selectedRange.value = null
  compareMode.value = false
  modelInfo.value = null

  const ctx = buildAiContext()
  const selectedText = getSelectedText()
  const isLocalOp = LOCAL_OP_PRESETS.includes(mode) && selectedText

  const customPreset = customPresets.value.find(p => p.id === mode)
  if (customPreset) {
    const useLocal = customPreset.localOp && selectedText
    const contextBlock = useLocal ? `【待处理的文本】\n${selectedText}` : ctx
    const userMsg = `${customPreset.prompt}\n\n${contextBlock}`
    chatHistory.value = [{ role: 'user', content: userMsg }]
    runAi(userMsg)
    return
  }

  const prompts = {
    generate: `${t('adminProjects.aiPromptGenerate')}\n\n${ctx}`,
    features: `${t('adminProjects.aiPromptFeatures')}\n\n${ctx}`,
    polish: isLocalOp
      ? `${t('adminProjects.aiPromptPolishLocal')}\n\n【待润色的文本】\n${selectedText}`
      : `${t('adminProjects.aiPromptPolish')}\n\n${ctx}`,
    expand: isLocalOp
      ? `${t('adminProjects.aiPromptExpandLocal')}\n\n【待扩展的文本】\n${selectedText}`
      : `${t('adminProjects.aiPromptExpand')}\n\n${ctx}`,
    condense: isLocalOp
      ? `${t('adminProjects.aiPromptCondenseLocal')}\n\n【待压缩的文本】\n${selectedText}`
      : `${t('adminProjects.aiPromptCondense')}\n\n${ctx}`,
    continue: `${t('adminProjects.aiPromptContinue')}\n\n${ctx}`,
    tech: `${t('adminProjects.aiPromptTech')}\n\n${ctx}`,
    translate: `${t('adminProjects.aiPromptTranslate')}\n\n${ctx}`,
    translateZH: isLocalOp
      ? `${t('adminProjects.aiPromptTranslateZHLocal')}\n\n【待翻译的文本】\n${selectedText}`
      : `${t('adminProjects.aiPromptTranslateZH')}\n\n${ctx}`,
    translateEN: isLocalOp
      ? `${t('adminProjects.aiPromptTranslateENLocal')}\n\n【待翻译的文本】\n${selectedText}`
      : `${t('adminProjects.aiPromptTranslateEN')}\n\n${ctx}`,
    summary: `${t('adminProjects.aiPromptSummary')}\n\n${ctx}`,
    toneFormal: isLocalOp
      ? `${t('adminProjects.aiPromptToneFormalLocal')}\n\n【待改写的文本】\n${selectedText}`
      : `${t('adminProjects.aiPromptToneFormal')}\n\n${ctx}`,
    toneCasual: isLocalOp
      ? `${t('adminProjects.aiPromptToneCasualLocal')}\n\n【待改写的文本】\n${selectedText}`
      : `${t('adminProjects.aiPromptToneCasual')}\n\n${ctx}`,
  }
  const userMsg = prompts[mode]
  chatHistory.value = [{ role: 'user', content: userMsg }]
  runAi(userMsg)
}

function runCustom() {
  if (!aiPrompt.value.trim()) return
  aiDrawerMode.value = 'custom'
  aiError.value = ''
  aiResult.value = ''
  selectedRange.value = null
  compareMode.value = false
  modelInfo.value = null
  const ctx = buildAiContext()
  const msg = `${aiPrompt.value}\n\n---\n${t('adminProjects.aiContextLabel')}\n${ctx}`
  chatHistory.value = [{ role: 'user', content: msg }]
  runAi(msg)
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
        type: aiDrawerMode.value || 'chat',
        preset: aiDrawerMode.value,
        prompt: message,
        response: aiResult.value,
        contentType: 'project',
      })
    },
    onError(err) { aiError.value = String(err.message || err); aiLoading.value = false; abortFn = null }
  })
}

function cancelAi() {
  if (abortFn) { abortFn(); abortFn = null }
  aiLoading.value = false
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
    onError(err) { aiError.value = String(err.message || err); aiLoading.value = false; abortFn = null }
  })
}

function closeAiDrawer(done) {
  cancelAi()
  aiResult.value = ''
  aiError.value = ''
  aiPrompt.value = ''
  aiDrawerMode.value = ''
  chatHistory.value = []
  previousContent.value = ''
  selectedRange.value = null
  compareMode.value = false
  modelInfo.value = null
  continueInput.value = ''
  done()
}

function insertToDescription() {
  if (!aiResult.value) return
  const text = aiResult.value.replace(/\n{3,}/g, '\n\n').trim()
  form.description = form.description ? form.description + '\n\n' + text : text
  ElMessage.success(t('adminProjects.insertedMsg'))
}

async function replaceDescription() {
  if (!aiResult.value) return
  try {
    await ElMessageBox.confirm(
      t('adminProjects.confirmReplaceMsg'),
      t('adminProjects.confirmReplaceTitle'),
      { confirmButtonText: t('adminProjects.confirm'), cancelButtonText: t('adminProjects.cancel'), type: 'warning' }
    )
  } catch { return }
  previousContent.value = form.description
  previousField.value = 'description'
  form.description = aiResult.value
  ElMessage({ message: t('adminProjects.replacedMsg'), type: 'success', duration: 5000, showClose: true })
}

function undoReplace() {
  if (!previousContent.value) return
  form[previousField.value || 'description'] = previousContent.value
  previousContent.value = ''
  previousField.value = ''
  ElMessage.success(t('adminProjects.undoSuccessMsg'))
}

function replaceSelected() {
  if (!aiResult.value || !selectedRange.value) return
  const { start, end } = selectedRange.value
  const before = form.description.slice(0, start)
  const after = form.description.slice(end)
  form.description = before + aiResult.value + after
  selectedRange.value = null
  ElMessage.success(t('adminProjects.replacedSelectedMsg'))
}

function insertToFeatures() {
  if (!aiResult.value) return
  form.features = form.features ? form.features + '\n\n' + aiResult.value : aiResult.value
  ElMessage.success(t('adminProjects.insertedMsg'))
}

async function replaceFeatures() {
  if (!aiResult.value) return
  try {
    await ElMessageBox.confirm(
      t('adminProjects.confirmReplaceMsg'),
      t('adminProjects.confirmReplaceTitle'),
      { confirmButtonText: t('adminProjects.confirm'), cancelButtonText: t('adminProjects.cancel'), type: 'warning' }
    )
  } catch { return }
  previousContent.value = form.features
  previousField.value = 'features'
  form.features = aiResult.value
  ElMessage({ message: t('adminProjects.replacedMsg'), type: 'success', duration: 5000, showClose: true })
}


function formatDate(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 16)
}

function onKeywordInput() {
  if (aiMode.value) return
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 0
    loadProjects()
  }, 300)
}

async function doSearch() {
  if (aiMode.value && keyword.value.trim()) {
    await aiSearch()
  } else {
    page.value = 0
    loadProjects()
  }
}

async function aiSearch() {
  searching.value = true
  try {
    const res = await request.post('/api/ai/search/parse', { message: keyword.value })
    if (res.keyword) keyword.value = res.keyword
    if (res.status) filterStatus.value = res.status
    page.value = 0
    await loadProjects()
  } catch (e) {
    ElMessage.error(t('adminProjects.aiSearchFailed') + ': ' + (e.message || ''))
  } finally {
    searching.value = false
  }
}

function handlePageChange(p) {
  page.value = p - 1
  loadProjects()
}

function handleSizeChange(s) {
  pageSize.value = s
  page.value = 0
  loadProjects()
}

async function loadProjects() {
  loading.value = true
  updateFilterCount()
  try {
    let url = `/admin/projects?page=${page.value}&size=${pageSize.value}`
    if (keyword.value.trim()) url += `&keyword=${encodeURIComponent(keyword.value.trim())}`
    if (filterStatus.value) url += `&status=${filterStatus.value}`
    if (sortBy.value !== 'default') url += `&sort=${sortBy.value}`
    const data = await request.get(url)
    projects.value = data.content
    totalPages.value = data.totalPages
    totalElements.value = data.totalElements
  } catch {
    ElMessage.error(t('adminProjects.loadFailed'))
  } finally {
    loading.value = false
  }
}

function startAdd() {
  editingId.value = null
  Object.assign(form, defaultForm())
  showForm.value = true
}

function parseScreenshots(str) {
  if (!str) return []
  try {
    const arr = JSON.parse(str)
    return Array.isArray(arr) ? arr.filter(Boolean) : []
  } catch {
    return []
  }
}

function startEdit(project) {
  editingId.value = project.id
  Object.assign(form, {
    name: project.name || '', subtitle: project.subtitle || '',
    description: project.description || '', techStack: project.techStack || '',
    coverImage: project.coverImage || '', githubUrl: project.githubUrl || '',
    demoUrl: project.demoUrl || '', subdomainUrl: project.subdomainUrl || '',
    features: project.features || '', sortOrder: project.sortOrder || 0,
    status: project.status || 'DRAFT', isFeatured: project.isFeatured || false,
    screenshots: parseScreenshots(project.screenshots)
  })
  showForm.value = true
}

function onScreenshotsUploaded(results) {
  const urls = results.filter(Boolean).map(r => r.url).filter(Boolean)
  if (urls.length) form.screenshots.push(...urls)
}

function removeScreenshot(i) {
  form.screenshots.splice(i, 1)
}

async function handleSave() {
  if (!form.name.trim()) { ElMessage.warning(t('adminProjects.nameRequired')); return }
  saving.value = true
  try {
    const body = { ...form }
    body.screenshots = JSON.stringify(form.screenshots || [])
    if (editingId.value) {
      await request.put(`/admin/projects/${editingId.value}`, body)
    } else {
      await request.post('/admin/projects', body)
    }
    showForm.value = false
    ElMessage.success(t('adminProjects.saveSuccess'))
    loadProjects()
  } catch (e) {
    ElMessage.error(t('adminProjects.saveFailed') + ': ' + (e.message || t('adminProjects.unknownError')))
  } finally {
    saving.value = false
  }
}

function onCoverUploaded(res) {
  form.coverImage = res.url
}

async function onUploadImg(files, callback) {
  const results = await Promise.allSettled(
    files.map(async (file) => {
      const formData = new FormData()
      formData.append('file', file)
      const res = await request.post('/admin/upload/image', formData)
      return res.url
    })
  )
  const urls = results.filter(r => r.status === 'fulfilled').map(r => r.value)
  callback(urls)
}

async function toggleStatus(project) {
  if (pendingIds.has(project.id)) return
  pendingIds.add(project.id)
  try {
    const res = await request.patch(`/admin/projects/${project.id}/status`)
    project.status = res.status
    ElMessage.success(res.status === 'PUBLISHED' ? t('adminProjects.publishedSuccess') : t('adminProjects.unpublishedSuccess'))
  } catch (e) {
    if (e.code === 8002) {
      ElMessage.warning(e.message)
    } else {
      ElMessage.error(t('adminProjects.operationFailed'))
    }
  } finally {
    pendingIds.delete(project.id)
  }
}

async function toggleFeatured(project) {
  if (pendingIds.has(project.id)) return
  pendingIds.add(project.id)
  try {
    const res = await request.patch(`/admin/projects/${project.id}/featured`)
    project.isFeatured = res.isFeatured
    ElMessage.success(res.isFeatured ? t('adminProjects.featuredSuccess') : t('adminProjects.cancelFeaturedSuccess'))
  } catch (e) {
    ElMessage.error(t('adminProjects.operationFailed'))
  } finally {
    pendingIds.delete(project.id)
  }
}

async function handleDelete(project) {
  if (pendingIds.has(project.id)) return
  pendingIds.add(project.id)
  try {
    await ElMessageBox.confirm(t('adminProjects.confirmDeleteMsg', { name: project.name }), t('adminProjects.confirmDeleteTitle'), { type: 'warning' })
    await request.delete(`/admin/projects/${project.id}`)
    ElMessage.success(t('adminProjects.deleteSuccess'))
    loadProjects()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminProjects.deleteFailed'))
  } finally {
    pendingIds.delete(project.id)
  }
}

function handleProjectKeyboard(e) {
  if (e.ctrlKey && e.shiftKey && e.key === 'A') {
    e.preventDefault()
    aiDrawerVisible.value = true
  }
}

onMounted(() => {
  loadProjects()
  loadCustomPresets()
  document.addEventListener('keydown', handleProjectKeyboard)
})
onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
  if (abortFn) { abortFn(); abortFn = null }
  document.removeEventListener('keydown', handleProjectKeyboard)
})
</script>

<style scoped>
.cir-search-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}
.cir-search {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 24px;
  padding: 4px 4px 4px 12px;
  flex: 1;
  max-width: 500px;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.cir-search:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}
.cir-search.ai-active {
  border-color: #a855f7;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}
.cir-search-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  transition: color 0.2s;
}
.cir-search-btn:hover { color: #3b82f6; }
.cir-search-btn { color: #94a3b8; }
.cir-search.ai-active .cir-search-btn { color: #a855f7; }
.cir-search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  padding: 6px 8px;
  background: transparent;
  min-width: 0;
}
.cir-search-right {
  display: flex;
  align-items: center;
  gap: 4px;
}
.cir-clear-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: #cbd5e1;
  display: flex;
  align-items: center;
}
.cir-clear-btn:hover { color: #94a3b8; }
.cir-go-btn {
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}
.cir-go-btn:hover { background: #2563eb; }
.cir-go-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.cir-search.ai-active .cir-go-btn { background: #a855f7; }
.cir-search.ai-active .cir-go-btn:hover { background: #9333ea; }
.go-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #fff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.ai-icon-spin { animation: spin 2s linear infinite; }
.cir-filter-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  font-size: 13px;
  color: #475569;
  transition: border-color 0.2s, color 0.2s, background 0.2s;
  position: relative;
  flex-shrink: 0;
}
.cir-filter-btn:hover { border-color: #3b82f6; color: #3b82f6; }
.cir-filter-btn.active { border-color: #3b82f6; color: #3b82f6; background: rgba(59, 130, 246, 0.08); }
.filter-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.filter-panel {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  margin-bottom: 12px;
}
.project-edit-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px - 48px);
}
.project-edit-layout {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}
.project-edit-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  padding: 20px 24px;
  overflow: auto;
}
.project-edit-main .title-input :deep(.el-input__inner) {
  font-size: 1.25rem;
  font-weight: 600;
  padding: 12px;
}
.project-editors {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.editor-section {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 830px;
}
.editor-section-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 6px;
  flex-shrink: 0;
}
.md-editor-desc {
  flex: 3;
  min-height: 0;
}
.md-editor-features {
  flex: 2;
  min-height: 0;
}
.project-edit-sidebar {
  width: 300px;
  flex-shrink: 0;
  overflow-y: auto;
}
@media (max-width: 900px) {
  .project-edit-page {
    height: auto;
  }
  .project-edit-layout {
    flex-direction: column;
    height: auto;
  }
  .project-edit-sidebar {
    width: 100%;
  }
  .md-editor-desc,
  .md-editor-features {
    min-height: 250px;
  }
  .cir-search-wrap {
    flex-wrap: wrap;
  }
  .cir-search {
    max-width: 100%;
  }
}

/* AI Drawer Styles */
.ai-drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
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
  transition: border-color 0.2s, color 0.2s, background 0.2s;
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
  gap: 8px;
}
.ai-preset-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}
.ai-preset-btn:hover:not(:disabled) {
  border-color: #a855f7;
  background: rgba(59, 130, 246, 0.05);
}
.ai-preset-btn.active {
  border-color: #a855f7;
  background: rgba(59, 130, 246, 0.05);
}
.ai-preset-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.ai-preset-icon {
  font-size: 20px;
}
.ai-preset-label {
  font-size: 12px;
  color: #475569;
}
.manage-btn {
  border-style: dashed;
  opacity: 0.7;
}
.manage-btn:hover {
  opacity: 1;
}
.ai-custom-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ai-prompt-input {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  padding: 10px 12px;
  font-size: 14px;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.6);
}
.ai-prompt-input:focus {
  border-color: #a855f7;
}
.ai-generate-btn {
  align-self: flex-end;
  padding: 8px 20px;
  background: #a855f7;
  color: #fff;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.2s;
}
.ai-generate-btn:hover:not(:disabled) { background: #9333ea; }
.ai-generate-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.ai-result-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ai-result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ai-result-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}
.ai-cancel-btn {
  padding: 4px 12px;
  background: rgba(239, 68, 68, 0.08);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.15);
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
}
.ai-result-preview {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.4);
}
.ai-typing {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}
.ai-typing span {
  width: 8px;
  height: 8px;
  background: #a855f7;
  border-radius: 50%;
  animation: typing 1.4s infinite both;
}
.ai-typing span:nth-child(2) { animation-delay: 0.2s; }
.ai-typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}
.ai-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.ai-action-btn {
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #475569;
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}
.ai-action-btn:hover {
  border-color: #a855f7;
  color: #a855f7;
  background: rgba(59, 130, 246, 0.05);
}
.ai-error {
  padding: 8px 12px;
  background: rgba(239, 68, 68, 0.06);
  color: #ef4444;
  border-radius: 8px;
  font-size: 13px;
}
/* AI Compare */
.ai-compare { display: flex; gap: 8px; }
.ai-compare-pane { flex: 1; min-width: 0; }
.ai-compare-label { font-size: 12px; color: #94a3b8; margin-bottom: 4px; font-weight: 600; }
.ai-compare-content { background: rgba(255,255,255,0.5); border-radius: 8px; padding: 8px; max-height: 300px; overflow-y: auto; font-size: 13px; }
/* AI Chat messages */
.ai-chat-msg { margin-bottom: 8px; }
.ai-chat-msg.user .ai-chat-bubble { background: rgba(59,130,246,0.08); border-radius: 8px; padding: 6px 10px; font-size: 13px; color: #475569; }
.ai-chat-msg.assistant .ai-chat-bubble { background: rgba(255,255,255,0.5); border-radius: 8px; padding: 6px 10px; }
/* Model info */
.ai-model-info { font-size: 11px; color: #94a3b8; margin-top: 4px; text-align: right; }
/* Continue section */
.ai-continue-section { margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(0,0,0,0.04); }

/* 项目展示图片网格 */
.shot-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.shot-thumb {
  position: relative;
  aspect-ratio: 4 / 3;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.08);
}
.shot-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}
.shot-remove:hover { background: rgba(220, 38, 38, 0.85); }
</style>
