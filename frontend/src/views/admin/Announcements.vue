<template>
  <div class="admin-page">
    <!-- List view -->
    <template v-if="!showForm">
      <div class="page-header">
        <h2>{{ $t('adminAnnouncements.pageTitle') }}</h2>
        <div class="header-actions">
          <RefreshButton :onRefresh="load" />
          <el-button v-if="selectedIds.length > 0" @click="bulkToggleStatus">
            {{ $t('adminAnnouncements.bulkToggle') }} ({{ selectedIds.length }})
          </el-button>
          <el-button v-if="selectedIds.length > 0" type="danger" @click="bulkDelete">
            {{ $t('adminAnnouncements.bulkDelete') }} ({{ selectedIds.length }})
          </el-button>
          <el-button v-if="authStore.hasPermission('announcement:manage')" type="primary" @click="openCreate">
            <el-icon><Plus /></el-icon> {{ $t('adminAnnouncements.createAnnouncement') }}
          </el-button>
        </div>
      </div>

      <!-- Search and filter bar -->
      <div class="filter-bar">
        <el-input v-model="keyword" :placeholder="$t('adminAnnouncements.searchPlaceholder')" clearable
          style="width: 280px" @input="onKeywordInput" @clear="page = 0; load()" @keydown.enter="page = 0; load()" />
        <el-select v-model="filterType" style="width: 120px" @change="page = 0; load()">
          <el-option :label="$t('adminAnnouncements.filterAll')" value="" />
          <el-option :label="$t('adminAnnouncements.typeInfo')" value="info" />
          <el-option :label="$t('adminAnnouncements.typeFeature')" value="feature" />
          <el-option :label="$t('adminAnnouncements.typeUpdate')" value="update" />
        </el-select>
        <el-select v-model="filterActive" style="width: 120px" @change="page = 0; load()">
          <el-option :label="$t('adminAnnouncements.filterAllStatus')" value="" />
          <el-option :label="$t('adminAnnouncements.enable')" value="true" />
          <el-option :label="$t('adminAnnouncements.disable')" value="false" />
        </el-select>
        <el-select v-model="sortBy" style="width: 120px" @change="page = 0; load()">
          <el-option :label="$t('adminAnnouncements.sortDefault')" value="default" />
          <el-option :label="$t('adminAnnouncements.sortNewest')" value="newest" />
          <el-option :label="$t('adminAnnouncements.sortOldest')" value="oldest" />
        </el-select>
      </div>

      <!-- Result count -->
      <div v-if="totalElements > 0" class="result-count">
        {{ $t('adminAnnouncements.foundItems', { count: totalElements }) }}
      </div>

      <el-card>
        <el-table :data="announcements" row-key="id" stripe v-loading="loading" :empty-text="$t('adminAnnouncements.emptyText')"
          @selection-change="onSelectionChange" ref="tableRef">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="sortOrder" :label="$t('adminAnnouncements.sort')" width="70" />
          <el-table-column prop="tag" :label="$t('adminAnnouncements.tag')" width="100">
            <template #default="{ row }">
              <el-tag :type="tagType(row.type)" effect="plain" size="small">{{ row.tag }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" :label="$t('adminAnnouncements.title')" min-width="200" show-overflow-tooltip />
          <el-table-column prop="active" :label="$t('adminAnnouncements.status')" width="80">
            <template #default="{ row }">
              <el-tag :type="row.active ? 'success' : 'info'" effect="plain" size="small" @click="toggleActive(row)" style="cursor: pointer">
                {{ row.active ? $t('adminAnnouncements.enable') : $t('adminAnnouncements.disable') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" :label="$t('adminAnnouncements.createdAt')" width="170">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('adminAnnouncements.action')" width="240" fixed="right">
            <template #default="{ row }">
              <el-button v-if="authStore.hasPermission('announcement:manage')" type="primary" size="small" @click="openEdit(row)">{{ $t('adminAnnouncements.edit') }}</el-button>
              <el-button v-if="authStore.hasPermission('announcement:manage')" :type="row.active ? 'warning' : 'success'" size="small" :disabled="pendingIds.has(row.id)" @click="toggleActive(row)">{{ row.active ? $t('adminAnnouncements.disable') : $t('adminAnnouncements.enable') }}</el-button>
              <el-button v-if="authStore.hasPermission('announcement:manage')" type="danger" size="small" :disabled="pendingIds.has(row.id)" @click="handleDelete(row)">{{ $t('adminAnnouncements.delete') }}</el-button>
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
      <div class="page-header">
        <h2>{{ editingId ? $t('adminAnnouncements.editAnnouncement') : $t('adminAnnouncements.createAnnouncement') }}</h2>
        <div style="display: flex; gap: 8px">
          <el-button @click="aiDrawerVisible = true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align: -2px; margin-right: 4px"><path d="M12 2a4 4 0 0 0-4 4v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2h-2V6a4 4 0 0 0-4-4z"/><circle cx="9" cy="14" r="1"/><circle cx="15" cy="14" r="1"/></svg>
            {{ $t('adminAnnouncements.aiAssist') }}
          </el-button>
          <el-button @click="showForm = false">{{ $t('adminAnnouncements.cancel') }}</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">
            {{ editingId ? $t('adminAnnouncements.save') : $t('adminAnnouncements.create') }}
          </el-button>
        </div>
      </div>

      <div class="announce-edit-layout">
        <!-- Left: Editor -->
        <div class="announce-edit-main">
          <el-tabs v-model="editorTab">
            <el-tab-pane :label="$t('adminAnnouncements.editTab')" name="edit">
              <el-form :model="form" label-position="top" class="announce-form">
                <el-form-item :label="$t('adminAnnouncements.titleLabel')">
                  <el-input v-model="form.title" :placeholder="$t('adminAnnouncements.titlePlaceholder')" class="title-input" />
                </el-form-item>
                <el-form-item :label="$t('adminAnnouncements.titleLabelEn')">
                  <el-input v-model="form.titleEn" :placeholder="$t('adminAnnouncements.titlePlaceholderEn')" class="title-input" />
                </el-form-item>
                <el-form-item :label="$t('adminAnnouncements.contentLabel')">
                  <el-input v-model="form.content" type="textarea" :rows="10" :placeholder="$t('adminAnnouncements.contentPlaceholder')" class="content-textarea" />
                </el-form-item>
                <el-form-item :label="$t('adminAnnouncements.contentLabelEn')">
                  <el-input v-model="form.contentEn" type="textarea" :rows="10" :placeholder="$t('adminAnnouncements.contentPlaceholderEn')" class="content-textarea" />
                </el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane :label="$t('adminAnnouncements.previewTab')" name="preview">
              <div class="preview-container">
                <!-- Banner preview -->
                <div class="preview-section">
                  <h4 class="preview-label">{{ $t('adminAnnouncements.bannerPreview') }}</h4>
                  <div v-if="form.displayStyle === 'banner'" class="notice-banner" :class="`notice-banner--${previewLevel}`">
                    <span class="notice-icon">&#128161;</span>
                    <div class="notice-banner-body">
                      <span class="notice-banner-title">{{ form.title || $t('adminAnnouncements.defaultTitle') }}</span>
                      <span class="notice-banner-text">{{ form.content || $t('adminAnnouncements.defaultContent') }}</span>
                    </div>
                    <span class="notice-banner-arrow">&rsaquo;</span>
                  </div>
                  <div v-else class="notice-alert" :class="`notice-alert--${previewLevel}`">
                    <svg class="notice-alert-icon" stroke="currentColor" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M13 16h-1v-4h1m0-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke-width="2" stroke-linejoin="round" stroke-linecap="round"></path>
                    </svg>
                    <div class="notice-alert-body">
                      <span class="notice-alert-title">{{ form.title || $t('adminAnnouncements.defaultTitle') }}</span>
                      <span class="notice-alert-text">{{ form.content || $t('adminAnnouncements.defaultContent') }}</span>
                    </div>
                  </div>
                </div>

                <!-- Sidebar card preview -->
                <div class="preview-section">
                  <h4 class="preview-label">{{ $t('adminAnnouncements.sidebarCard') }}</h4>
                  <div class="notice-card-item" :class="`notice-card-item--${previewLevel}`">
                    <svg class="notice-card-wave" viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg">
                      <path d="M0 40 Q10 20 20 40 T40 40 T60 40 T80 40 V80 H0 Z" :fill="previewWaveColor" />
                    </svg>
                    <div class="notice-card-icon-wrap">
                      <svg class="notice-card-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path v-if="form.type === 'feature'" d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" :fill="previewIconColor" stroke="none"/>
                        <path v-else-if="form.type === 'update'" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" :fill="previewIconColor" stroke="none"/>
                        <path v-else d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" :fill="previewIconColor" stroke="none"/>
                      </svg>
                    </div>
                    <div class="notice-card-text">
                      <span class="notice-card-title">{{ form.title || $t('adminAnnouncements.defaultTitle') }}</span>
                      <span class="notice-card-sub">{{ form.tag || $t('adminAnnouncements.defaultTag') }}</span>
                    </div>
                  </div>
                </div>

                <!-- Detail modal preview -->
                <div class="preview-section">
                  <h4 class="preview-label">{{ $t('adminAnnouncements.detailModal') }}</h4>
                  <div class="preview-modal">
                    <div class="notice-modal-header">
                      <div class="notice-modal-icon" :class="form.type">{{ typeIcon(form.type) }}</div>
                      <div class="notice-modal-title-wrap">
                        <h3 class="notice-modal-title">{{ form.title || $t('adminAnnouncements.defaultTitle') }}</h3>
                        <p class="notice-modal-subtitle">
                          <span class="notice-modal-tag" :class="form.type">{{ form.tag || $t('adminAnnouncements.defaultTag') }}</span>
                        </p>
                      </div>
                    </div>
                    <div class="notice-modal-body">
                      <p>{{ form.content || $t('adminAnnouncements.defaultContentLong') }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- Right: Sidebar -->
        <div class="announce-edit-sidebar">
          <el-card shadow="never" style="margin-bottom: 12px">
            <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminAnnouncements.typeLabel') }}</span></template>
            <el-radio-group v-model="form.type" class="type-radio-group">
              <el-radio-button v-for="opt in typeOptions" :key="opt.value" :value="opt.value">
                {{ opt.icon }} {{ opt.label }}
              </el-radio-button>
            </el-radio-group>
          </el-card>

          <el-card shadow="never" style="margin-bottom: 12px">
            <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminAnnouncements.tagTextLabel') }}</span></template>
            <el-input v-model="form.tag" :placeholder="$t('adminAnnouncements.tagPlaceholder')" />
            <div class="tag-presets">
              <el-button v-for="preset in tagPresets" :key="preset" size="small" text
                :type="form.tag === preset ? 'primary' : 'default'"
                @click="form.tag = preset">
                {{ preset }}
              </el-button>
            </div>
            <el-input v-model="form.tagEn" placeholder="Tag (English)" style="margin-top: 8px" />
          </el-card>

          <el-card shadow="never" style="margin-bottom: 12px">
            <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminAnnouncements.levelLabel') }}</span></template>
            <el-radio-group v-model="form.level" class="level-radio-group">
              <el-radio-button v-for="l in levelOptions" :key="l.value" :value="l.value">
                <span class="level-dot" :style="{ background: l.color }"></span>{{ l.label }}
              </el-radio-button>
            </el-radio-group>
          </el-card>

          <el-card shadow="never" style="margin-bottom: 12px">
            <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminAnnouncements.styleLabel') }}</span></template>
            <el-radio-group v-model="form.displayStyle" class="style-radio-group">
              <el-radio-button v-for="s in styleOptions" :key="s.value" :value="s.value">
                {{ s.label }}
              </el-radio-button>
            </el-radio-group>
          </el-card>

          <el-card shadow="never" style="margin-bottom: 12px">
            <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminAnnouncements.settingsLabel') }}</span></template>
            <el-form :model="form" label-position="top">
              <el-form-item :label="$t('adminAnnouncements.sortOrderLabel')">
                <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
              </el-form-item>
              <el-form-item :label="$t('adminAnnouncements.activeLabel')">
                <el-switch v-model="form.active" :active-text="$t('adminAnnouncements.enable')" :inactive-text="$t('adminAnnouncements.disable')" />
              </el-form-item>
            </el-form>
          </el-card>
        </div>
      </div>
    </template>

    <!-- AI Assist Drawer -->
    <el-drawer v-model="aiDrawerVisible" :title="$t('adminAnnouncements.aiAssistTitle')" direction="rtl" size="480px" :before-close="closeAiDrawer">
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
            class="ai-preset-btn" :class="{ active: aiMode === p.id }"
            :disabled="aiLoading || (p.needsTitle && !form.title.trim()) || (p.needsContent && !form.content.trim())"
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
            :placeholder="$t('adminAnnouncements.customPromptPlaceholder')"
            @keydown.ctrl.enter="runCustom" :disabled="aiLoading" />
          <div style="display: flex; align-items: center; gap: 8px; margin-top: 6px">
            <button class="ai-generate-btn" :disabled="aiLoading || !aiPrompt.trim()" @click="runCustom">
              {{ aiLoading ? $t('adminAnnouncements.generating') : $t('adminAnnouncements.generate') }}
            </button>
            <span class="ai-shortcut-hint">Ctrl+Enter</span>
          </div>
        </div>

        <!-- Result preview -->
        <div v-if="aiResult || aiLoading || chatHistory.length > 0" ref="aiResultSection" class="ai-result-section">
          <div class="ai-result-header">
            <span class="ai-result-title">{{ $t('adminAnnouncements.preview') }}</span>
            <div style="display: flex; gap: 4px">
              <button v-if="aiResult && !aiLoading" class="ai-cancel-btn" style="font-size: 12px; padding: 2px 8px" @click="compareMode = !compareMode">
                {{ compareMode ? $t('adminAnnouncements.exitCompare') : $t('adminAnnouncements.compareView') }}
              </button>
              <button v-if="aiLoading" class="ai-cancel-btn" @click="cancelAi">{{ $t('adminAnnouncements.cancel') }}</button>
            </div>
          </div>

          <!-- Compare mode -->
          <div v-if="compareMode && aiResult && !aiLoading" class="ai-compare">
            <div class="ai-compare-pane">
              <div class="ai-compare-label">{{ $t('adminAnnouncements.originalText') }}</div>
              <div class="ai-compare-content">
                <MdPreview :modelValue="selectedRange ? form.content.slice(selectedRange.start, selectedRange.end) : form.content" previewTheme="github" :codeFoldable="false" />
              </div>
            </div>
            <div class="ai-compare-pane">
              <div class="ai-compare-label">{{ $t('adminAnnouncements.aiResult') }}</div>
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
            <button v-if="selectedRange" class="ai-action-btn primary" @click="replaceSelected">{{ $t('adminAnnouncements.replaceSelected') }}</button>
            <button class="ai-action-btn" @click="insertToContent">{{ $t('adminAnnouncements.insertToContent') }}</button>
            <button class="ai-action-btn" @click="replaceContent">{{ $t('adminAnnouncements.replaceContent') }}</button>
            <CopyButton :text="aiResult" @copied="ElMessage.success(t('adminAnnouncements.copiedMsg'))" />
            <button v-if="previousContent" class="ai-action-btn" @click="undoReplace" style="color: var(--el-color-warning)">{{ $t('adminAnnouncements.undoReplace') }}</button>
          </div>

          <!-- Continue chat -->
          <div v-if="chatHistory.length > 0 && !aiLoading" class="ai-continue-section">
            <textarea v-model="continueInput" class="ai-prompt-input" rows="2"
              :placeholder="$t('adminAnnouncements.continuePlaceholder')"
              @keydown.ctrl.enter="continueChat" />
            <button class="ai-generate-btn" :disabled="!continueInput.trim()" @click="continueChat">
              {{ $t('adminAnnouncements.send') }}
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
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { Plus } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'
import RefreshButton from '../../components/RefreshButton.vue'
import { defineAsyncComponent } from 'vue'
const MdPreview = defineAsyncComponent(() => import('md-editor-v3').then(m => m.MdPreview))
import 'md-editor-v3/lib/preview.css'
import CopyButton from '../../components/CopyButton.vue'
import { aiEditorStream } from '../../utils/ai'
import { getCustomPresets } from '../../ai/presets/custom'
import { buildKnowledgeContext } from '../../ai/knowledge/index'
import { addHistory } from '../../ai/history/index'
import { injectCopyButtons } from '../../utils/copyUtils'
const CustomPresetManager = defineAsyncComponent(() => import('../../components/ai/CustomPresetManager.vue'))
const TemplateManager = defineAsyncComponent(() => import('../../components/ai/TemplateManager.vue'))
const KnowledgeManager = defineAsyncComponent(() => import('../../components/ai/KnowledgeManager.vue'))
const AIHistoryManager = defineAsyncComponent(() => import('../../components/ai/AIHistoryManager.vue'))
const AgentWriter = defineAsyncComponent(() => import('../../components/ai/AgentWriter.vue'))

const authStore = useAuthStore()
const { t } = useI18n()

const announcements = ref([])
const loading = ref(false)
const showForm = ref(false)
const editingId = ref(null)
const saving = ref(false)
const editorTab = ref('edit')

// Search, filter, pagination
const keyword = ref('')
const page = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const filterType = ref('')
const filterActive = ref('')
const sortBy = ref('default')
let searchTimer = null
const selectedIds = ref([])
const tableRef = ref(null)
const pendingIds = new Set()

const typeOptions = computed(() => [
  { value: 'info', label: t('adminAnnouncements.typeInfo'), icon: '💡' },
  { value: 'feature', label: t('adminAnnouncements.typeFeature'), icon: '✨' },
  { value: 'update', label: t('adminAnnouncements.typeUpdate'), icon: '🚀' }
])
const tagPresets = computed(() => [
  t('adminAnnouncements.presetAnnouncement'),
  t('adminAnnouncements.presetFeature'),
  t('adminAnnouncements.presetUpdate'),
  t('adminAnnouncements.presetActivity'),
  t('adminAnnouncements.presetMaintenance'),
  t('adminAnnouncements.presetImportant')
])

const levelOptions = computed(() => [
  { value: 'info', label: t('adminAnnouncements.levelInfo'), color: '#3b82f6', bg: '#eff6ff' },
  { value: 'success', label: t('adminAnnouncements.levelSuccess'), color: '#10b981', bg: '#ecfdf5' },
  { value: 'warning', label: t('adminAnnouncements.levelWarning'), color: '#f59e0b', bg: '#fffbeb' },
  { value: 'error', label: t('adminAnnouncements.levelError'), color: '#ef4444', bg: '#fef2f2' }
])
const styleOptions = computed(() => [
  { value: 'banner', label: t('adminAnnouncements.styleBanner') },
  { value: 'alert', label: t('adminAnnouncements.styleAlert') }
])

const form = ref({ tag: t('adminAnnouncements.defaultTag'), tagEn: '', type: 'info', title: '', titleEn: '', content: '', contentEn: '', sortOrder: 0, active: true, level: 'info', displayStyle: 'banner' })

async function load() {
  loading.value = true
  try {
    let url = `/admin/announcements?page=${page.value}&size=${pageSize.value}`
    if (keyword.value.trim()) url += `&keyword=${encodeURIComponent(keyword.value.trim())}`
    if (filterType.value) url += `&type=${filterType.value}`
    if (filterActive.value) url += `&active=${filterActive.value}`
    if (sortBy.value !== 'default') url += `&sort=${sortBy.value}`
    const data = await request.get(url)
    announcements.value = data.content
    totalPages.value = data.totalPages
    totalElements.value = data.totalElements
  } catch {
    ElMessage.error(t('adminAnnouncements.loadFailed'))
  } finally {
    loading.value = false
  }
}

function onKeywordInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { page.value = 0; load() }, 300)
}

function onSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.id)
}

function handlePageChange(p) {
  page.value = p - 1
  load()
}

function handleSizeChange(s) {
  pageSize.value = s
  page.value = 0
  load()
}

async function bulkToggleStatus() {
  try {
    await ElMessageBox.confirm(
      t('adminAnnouncements.confirmBulkToggleMsg', { count: selectedIds.value.length }),
      t('adminAnnouncements.confirmBulkToggleTitle'),
      { type: 'warning' }
    )
  } catch { return }
  const results = await Promise.allSettled(selectedIds.value.map(id =>
    request.patch(`/admin/announcements/${id}/toggle`)
  ))
  const succeeded = results.filter(r => r.status === 'fulfilled').length
  const failed = results.filter(r => r.status === 'rejected').length
  if (failed === 0) {
    ElMessage.success(t('adminAnnouncements.bulkSuccess'))
  } else {
    ElMessage.warning(t('adminAnnouncements.bulkPartialSuccess', { succeeded, failed }))
  }
  selectedIds.value = []
  load()
}

async function bulkDelete() {
  try {
    await ElMessageBox.confirm(
      t('adminAnnouncements.confirmBulkDeleteMsg', { count: selectedIds.value.length }),
      t('adminAnnouncements.confirmDeleteTitle'),
      { type: 'warning' }
    )
    await request.delete(`/admin/announcements?ids=${selectedIds.value.join(',')}`)
    ElMessage.success(t('adminAnnouncements.deleteSuccess'))
    selectedIds.value = []
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminAnnouncements.deleteFailed'))
  }
}

function openCreate() {
  editingId.value = null
  form.value = { tag: t('adminAnnouncements.defaultTag'), tagEn: '', type: 'info', title: '', titleEn: '', content: '', contentEn: '', sortOrder: 0, active: true, level: 'info', displayStyle: 'banner' }
  editorTab.value = 'edit'
  showForm.value = true
}

function openEdit(item) {
  editingId.value = item.id
  form.value = {
    tag: item.tag || t('adminAnnouncements.defaultTag'), tagEn: item.tagEn || '', type: item.type || 'info',
    title: item.title || '', titleEn: item.titleEn || '',
    content: item.content || '', contentEn: item.contentEn || '',
    sortOrder: item.sortOrder || 0, active: item.active !== false,
    level: item.level || 'info', displayStyle: item.displayStyle || 'banner'
  }
  editorTab.value = 'edit'
  showForm.value = true
}

async function handleSave() {
  if (!form.value.title.trim()) { ElMessage.warning(t('adminAnnouncements.titleRequired')); return }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`/admin/announcements/${editingId.value}`, form.value)
    } else {
      await request.post('/admin/announcements', form.value)
    }
    showForm.value = false
    ElMessage.success(t('adminAnnouncements.saveSuccess'))
    load()
  } catch (e) {
    if (e.code === 8002) {
      ElMessage.warning(e.message || t('adminAnnouncements.pendingReview'))
      showForm.value = false
      load()
    } else {
      ElMessage.error(t('adminAnnouncements.saveFailed') + ': ' + (e.message || t('adminAnnouncements.unknownError')))
    }
  } finally {
    saving.value = false
  }
}

async function toggleActive(item) {
  if (pendingIds.has(item.id)) return
  pendingIds.add(item.id)
  try {
    await request.patch(`/admin/announcements/${item.id}/toggle`)
    ElMessage.success(item.active ? t('adminAnnouncements.disabledSuccess') : t('adminAnnouncements.enabledSuccess'))
    load()
  } catch (e) {
    if (e.code === 8002) {
      ElMessage.warning(e.message)
    } else {
      ElMessage.error(t('adminAnnouncements.operationFailed'))
    }
  } finally {
    pendingIds.delete(item.id)
  }
}

async function handleDelete(item) {
  if (pendingIds.has(item.id)) return
  pendingIds.add(item.id)
  try {
    await ElMessageBox.confirm(t('adminAnnouncements.confirmDeleteMsg', { title: item.title }), t('adminAnnouncements.confirmDeleteTitle'), { type: 'warning' })
    await request.delete(`/admin/announcements/${item.id}`)
    ElMessage.success(t('adminAnnouncements.deleteSuccess'))
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminAnnouncements.deleteFailed'))
  } finally {
    pendingIds.delete(item.id)
  }
}

function tagType(type) {
  return { info: '', feature: 'success', update: 'warning' }[type] || ''
}

function typeIcon(type) {
  return { info: '💡', feature: '✨', update: '🚀' }[type] || '💡'
}

const previewLevel = computed(() => {
  const lvl = form.value.level
  if (lvl && lvl !== 'info') return lvl
  const tp = (form.value.type || '').toLowerCase()
  if (tp === 'feature') return 'success'
  if (tp === 'update') return 'warning'
  return 'info'
})

const previewWaveColor = computed(() => {
  const m = { info: '#3b82f63a', success: '#22c55e3a', warning: '#f59e0b3a', error: '#ef44443a' }
  return m[previewLevel.value]
})

const previewIconColor = computed(() => {
  const m = { info: '#3b82f6', success: '#22c55e', warning: '#f59e0b', error: '#ef4444' }
  return m[previewLevel.value]
})

function formatDate(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 16)
}

// === AI Assist ===
const ANNOUNCEMENT_SYSTEM_PROMPT = `你是一个专业的公告内容创作助手，专门帮助用户撰写和优化网站公告内容。

能力范围：
- 根据主题生成清晰专业的公告文案
- 润色和优化公告文本（改进表达、增强专业性）
- 扩展简短想法为完整的公告内容
- 压缩冗长内容为精炼公告
- 中英文双向翻译（保持公告的专业风格）
- 调整语气风格（正式/友好/紧急/轻松）
- SEO 优化（标题、关键词建议）
- 生成不同展示样式的内容建议

输出要求：
- 使用 Markdown 格式输出
- 保持专业、清晰、简洁的风格
- 结构化表达，便于用户快速获取信息
- 标题突出重点，内容层次分明`

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
const chatHistory = ref([])
const previousContent = ref('')
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

const aiPresets = computed(() => {
  const builtIn = [
    { id: 'generateAnnouncement', icon: ' ', label: t('adminAnnouncements.aiPresetGenerate'), needsTitle: true, category: 'writing' },
    { id: 'polishAnnouncement', icon: ' ', label: t('adminAnnouncements.aiPresetPolish'), needsContent: true, localOp: true, category: 'writing' },
    { id: 'expandAnnouncement', icon: ' ', label: t('adminAnnouncements.aiPresetExpand'), needsContent: true, localOp: true, category: 'writing' },
    { id: 'condense', icon: ' ', label: t('adminAnnouncements.aiPresetCondense'), needsContent: true, localOp: true, category: 'writing' },
    { id: 'toneFormal', icon: ' ', label: t('adminAnnouncements.aiPresetToneFormal'), needsContent: true, localOp: true, category: 'tone' },
    { id: 'toneFriendly', icon: ' ', label: t('adminAnnouncements.aiPresetToneFriendly'), needsContent: true, localOp: true, category: 'tone' },
    { id: 'toneUrgent', icon: ' ', label: t('adminAnnouncements.aiPresetToneUrgent'), needsContent: true, localOp: true, category: 'tone' },
    { id: 'translate', icon: ' ', label: t('adminAnnouncements.aiPresetTranslate'), needsContent: true, category: 'translate' },
    { id: 'translateZH', icon: ' ', label: t('adminAnnouncements.aiPresetTranslateZH'), needsContent: true, localOp: true, category: 'translate' },
    { id: 'translateEN', icon: ' ', label: t('adminAnnouncements.aiPresetTranslateEN'), needsContent: true, localOp: true, category: 'translate' },
    { id: 'summary', icon: ' ', label: t('adminAnnouncements.aiPresetSummary'), needsContent: true, category: 'analysis' },
    { id: 'title', icon: ' ', label: t('adminAnnouncements.aiPresetTitle'), needsContent: true, category: 'writing' },
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
  form.value.content = result
  ElMessage.success(t('ai.agent.completed'))
}

const LOCAL_OP_PRESETS = ['polishAnnouncement', 'expandAnnouncement', 'condense', 'toneFormal', 'toneFriendly', 'toneUrgent', 'translateZH', 'translateEN']

function getSelectedText() {
  const sel = window.getSelection()
  if (!sel || sel.isCollapsed || !sel.toString().trim()) return null
  const text = sel.toString().trim()
  const idx = form.value.content.indexOf(text)
  if (idx !== -1) {
    selectedRange.value = { start: idx, end: idx + text.length }
  }
  return text
}

function buildContext() {
  const parts = []
  const lang = detectLanguage(form.value.content)
  parts.push(`[内容语言: ${lang === 'zh' ? '中文' : 'English'}]`)
  if (form.value.title) parts.push(`【标题】\n${form.value.title}`)

  const selectedText = getSelectedText()
  if (selectedText) {
    parts.push(`【选中的文本】\n${selectedText}`)
  } else {
    if (form.value.content) parts.push(`【公告内容】\n${form.value.content}`)
  }
  if (form.value.tag) parts.push(`【标签】\n${form.value.tag}`)
  if (form.value.type) parts.push(`【类型】\n${form.value.type}`)
  return parts.join('\n\n')
}

function buildSystemPrompt() {
  const lang = detectLanguage(form.value.content)
  let prompt = ANNOUNCEMENT_SYSTEM_PROMPT
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
    generateAnnouncement: `${t('adminAnnouncements.aiPromptGenerate')}\n\n${ctx}`,
    polishAnnouncement: isLocalOp
      ? `${t('adminAnnouncements.aiPromptPolishLocal')}\n\n【待润色的文本】\n${selectedText}`
      : `${t('adminAnnouncements.aiPromptPolish')}\n\n${ctx}`,
    expandAnnouncement: isLocalOp
      ? `${t('adminAnnouncements.aiPromptExpandLocal')}\n\n【待扩展的文本】\n${selectedText}`
      : `${t('adminAnnouncements.aiPromptExpand')}\n\n${ctx}`,
    condense: isLocalOp
      ? `${t('adminAnnouncements.aiPromptCondenseLocal')}\n\n【待压缩的文本】\n${selectedText}`
      : `${t('adminAnnouncements.aiPromptCondense')}\n\n${ctx}`,
    toneFormal: isLocalOp
      ? `${t('adminAnnouncements.aiPromptToneFormalLocal')}\n\n【待改写的文本】\n${selectedText}`
      : `${t('adminAnnouncements.aiPromptToneFormal')}\n\n${ctx}`,
    toneFriendly: isLocalOp
      ? `${t('adminAnnouncements.aiPromptToneFriendlyLocal')}\n\n【待改写的文本】\n${selectedText}`
      : `${t('adminAnnouncements.aiPromptToneFriendly')}\n\n${ctx}`,
    toneUrgent: isLocalOp
      ? `${t('adminAnnouncements.aiPromptToneUrgentLocal')}\n\n【待改写的文本】\n${selectedText}`
      : `${t('adminAnnouncements.aiPromptToneUrgent')}\n\n${ctx}`,
    translate: `${t('adminAnnouncements.aiPromptTranslate')}\n\n${ctx}`,
    translateZH: isLocalOp
      ? `${t('adminAnnouncements.aiPromptTranslateZHLocal')}\n\n【待翻译的文本】\n${selectedText}`
      : `${t('adminAnnouncements.aiPromptTranslateZH')}\n\n${ctx}`,
    translateEN: isLocalOp
      ? `${t('adminAnnouncements.aiPromptTranslateENLocal')}\n\n【待翻译的文本】\n${selectedText}`
      : `${t('adminAnnouncements.aiPromptTranslateEN')}\n\n${ctx}`,
    summary: `${t('adminAnnouncements.aiPromptSummary')}\n\n${ctx}`,
    title: `${t('adminAnnouncements.aiPromptTitle')}\n\n${ctx}`,
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
  const msg = `${aiPrompt.value}\n\n---\n${t('adminAnnouncements.aiContextLabel')}\n${ctx}`
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
        contentType: 'announcement',
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
  form.value.content = form.value.content ? form.value.content + '\n\n' + aiResult.value : aiResult.value
  ElMessage.success(t('adminAnnouncements.insertedMsg'))
}

async function replaceContent() {
  if (!aiResult.value) return
  try {
    await ElMessageBox.confirm(
      t('adminAnnouncements.confirmReplaceMsg'),
      t('adminAnnouncements.confirmReplaceTitle'),
      { confirmButtonText: t('adminAnnouncements.confirm'), cancelButtonText: t('adminAnnouncements.cancel'), type: 'warning' }
    )
  } catch { return }
  previousContent.value = form.value.content
  form.value.content = aiResult.value
  ElMessage({ message: t('adminAnnouncements.replacedMsg'), type: 'success', duration: 5000, showClose: true })
}

function undoReplace() {
  if (!previousContent.value) return
  form.value.content = previousContent.value
  previousContent.value = ''
  ElMessage.success(t('adminAnnouncements.undoSuccessMsg'))
}

function replaceSelected() {
  if (!aiResult.value || !selectedRange.value) return
  const { start, end } = selectedRange.value
  const before = form.value.content.slice(0, start)
  const after = form.value.content.slice(end)
  form.value.content = before + aiResult.value + after
  selectedRange.value = null
  ElMessage.success(t('adminAnnouncements.replacedSelectedMsg'))
}

// Keyboard shortcuts
function handleKeyboard(e) {
  if (e.ctrlKey && e.shiftKey && e.key === 'A') {
    e.preventDefault()
    aiDrawerVisible.value = true
  }
}

onMounted(() => {
  load()
  loadCustomPresets()
  document.addEventListener('keydown', handleKeyboard)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyboard)
  if (abortFn) { abortFn(); abortFn = null }
  if (searchTimer) { clearTimeout(searchTimer); searchTimer = null }
})
</script>

<style scoped>
/* Filter bar */
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

/* Edit layout */
.announce-edit-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 60px - 48px - 60px);
  min-height: 0;
}
.announce-edit-main {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  padding: 16px 24px;
}
.announce-edit-main :deep(.el-tabs__content) {
  padding-top: 8px;
}
.announce-form .title-input :deep(.el-input__inner) {
  font-size: 1.25rem;
  font-weight: 600;
  padding: 12px;
}
.content-textarea :deep(.el-textarea__inner) {
  font-size: 0.95rem;
  line-height: 1.8;
  padding: 12px;
  resize: none;
}
.announce-edit-sidebar {
  width: 280px;
  flex-shrink: 0;
  overflow-y: auto;
}

/* Tag presets */
.tag-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

/* Type radio group */
.type-radio-group {
  display: flex;
  width: 100%;
}
.type-radio-group :deep(.el-radio-button) {
  flex: 1;
}
.type-radio-group :deep(.el-radio-button__inner) {
  width: 100%;
}
/* Level radio group */
.level-radio-group {
  display: flex;
  width: 100%;
}
.level-radio-group :deep(.el-radio-button) {
  flex: 1;
}
.level-radio-group :deep(.el-radio-button__inner) {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12px;
}
.level-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
/* Style radio group */
.style-radio-group {
  display: flex;
  width: 100%;
}
.style-radio-group :deep(.el-radio-button) {
  flex: 1;
}
.style-radio-group :deep(.el-radio-button__inner) {
  width: 100%;
}

/* Preview */
.preview-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.preview-section {
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  padding: 16px 20px;
}
.preview-label {
  font-size: 12px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0 0 12px;
}

/* Notice Banner (matches Home.vue) */
.notice-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1.1rem;
  border-radius: 12px;
  cursor: default;
  transition: background 0.25s, border-color 0.25s;
}
.notice-banner--info { background: linear-gradient(135deg, #eff6ff, #dbeafe); border: 1px solid #93c5fd; }
.notice-banner--info .notice-icon { background: rgba(59, 130, 246, 0.1); }
.notice-banner--info .notice-banner-title { color: #1e40af; }
.notice-banner--info .notice-banner-text { color: #3b82f6; }
.notice-banner--info .notice-banner-arrow { color: #3b82f6; }

.notice-banner--success { background: linear-gradient(135deg, #ecfdf5, #d1fae5); border: 1px solid #6ee7b7; }
.notice-banner--success .notice-icon { background: rgba(16, 185, 129, 0.1); }
.notice-banner--success .notice-banner-title { color: #065f46; }
.notice-banner--success .notice-banner-text { color: #10b981; }
.notice-banner--success .notice-banner-arrow { color: #10b981; }

.notice-banner--warning { background: linear-gradient(135deg, #fffbeb, #fef3c7); border: 1px solid #fcd34d; }
.notice-banner--warning .notice-icon { background: rgba(245, 158, 11, 0.1); }
.notice-banner--warning .notice-banner-title { color: #92400e; }
.notice-banner--warning .notice-banner-text { color: #a16207; }
.notice-banner--warning .notice-banner-arrow { color: #d97706; }

.notice-banner--error { background: linear-gradient(135deg, #fef2f2, #fee2e2); border: 1px solid #fca5a5; }
.notice-banner--error .notice-icon { background: rgba(239, 68, 68, 0.1); }
.notice-banner--error .notice-banner-title { color: #991b1b; }
.notice-banner--error .notice-banner-text { color: #ef4444; }
.notice-banner--error .notice-banner-arrow { color: #ef4444; }

.notice-icon {
  font-size: 1.3rem;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
}
.notice-banner-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}
.notice-banner-title {
  font-size: 0.88rem;
  font-weight: 600;
}
.notice-banner-text {
  font-size: 0.8rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-banner-arrow {
  font-size: 1.2rem;
  flex-shrink: 0;
  opacity: 0.5;
}

/* Notice Alert (matches Home.vue) */
.notice-alert {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  border-left: 4px solid;
  cursor: default;
  transition: background 0.3s, border-color 0.3s;
}
.notice-alert--info { background: #eff6ff; border-color: #3b82f6; }
.notice-alert--info .notice-alert-icon { color: #3b82f6; }
.notice-alert--info .notice-alert-title { color: #1e40af; }
.notice-alert--info .notice-alert-text { color: #3b82f6; }

.notice-alert--success { background: #ecfdf5; border-color: #10b981; }
.notice-alert--success .notice-alert-icon { color: #10b981; }
.notice-alert--success .notice-alert-title { color: #065f46; }
.notice-alert--success .notice-alert-text { color: #10b981; }

.notice-alert--warning { background: #fffbeb; border-color: #f59e0b; }
.notice-alert--warning .notice-alert-icon { color: #f59e0b; }
.notice-alert--warning .notice-alert-title { color: #92400e; }
.notice-alert--warning .notice-alert-text { color: #a16207; }

.notice-alert--error { background: #fef2f2; border-color: #ef4444; }
.notice-alert--error .notice-alert-icon { color: #ef4444; }
.notice-alert--error .notice-alert-title { color: #991b1b; }
.notice-alert--error .notice-alert-text { color: #ef4444; }

.notice-alert-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}
.notice-alert-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}
.notice-alert-title {
  font-size: 0.88rem;
  font-weight: 600;
}
.notice-alert-text {
  font-size: 0.78rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Notice Card Item (sidebar, matches Home.vue) */
.notice-card-item {
  width: 100%;
  height: 80px;
  border-radius: 8px;
  box-sizing: border-box;
  padding: 10px 15px;
  background-color: #ffffff;
  box-shadow: rgba(149, 157, 165, 0.2) 0px 8px 24px;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: default;
  transition: background 0.3s, border-color 0.3s;
}
.notice-card-wave {
  position: absolute;
  transform: rotate(90deg);
  left: -31px;
  top: 32px;
  width: 80px;
  pointer-events: none;
}
.notice-card-icon-wrap {
  width: 35px;
  height: 35px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;
  flex-shrink: 0;
}
.notice-card-icon {
  width: 17px;
  height: 17px;
}
.notice-card-text {
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex: 1;
  min-width: 0;
}
.notice-card-title {
  font-size: 0.82rem;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-card-sub {
  font-size: 0.68rem;
  color: #94a3b8;
  margin-top: 2px;
}
.notice-card-item--info { background-color: #dbeafe; }
.notice-card-item--info .notice-card-icon-wrap { background-color: #bfdbfe; }
.notice-card-item--info .notice-card-title { color: #2563eb; }
.notice-card-item--success { background-color: #dcfce7; }
.notice-card-item--success .notice-card-icon-wrap { background-color: #bbf7d0; }
.notice-card-item--success .notice-card-title { color: #16a34a; }
.notice-card-item--warning { background-color: #fef3c7; }
.notice-card-item--warning .notice-card-icon-wrap { background-color: #fde68a; }
.notice-card-item--warning .notice-card-title { color: #d97706; }
.notice-card-item--error { background-color: #fee2e2; }
.notice-card-item--error .notice-card-icon-wrap { background-color: #fecaca; }
.notice-card-item--error .notice-card-title { color: #dc2626; }

/* Notice Modal (matches Home.vue) */
.preview-modal {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  max-width: 460px;
}
.notice-modal-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 28px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
}
.notice-modal-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
  flex-shrink: 0;
}
.notice-modal-icon.info { background: #e0f2fe; }
.notice-modal-icon.feature { background: #ecfdf5; }
.notice-modal-icon.update { background: #fef3c7; }
.notice-modal-title-wrap {
  flex: 1;
  min-width: 0;
}
.notice-modal-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px;
}
.notice-modal-subtitle {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.notice-modal-tag {
  font-size: 0.7rem;
  padding: 2px 8px;
  border-radius: 5px;
  font-weight: 500;
}
.notice-modal-tag.info { background: #e0f2fe; color: #0369a1; }
.notice-modal-tag.feature { background: #ecfdf5; color: #047857; }
.notice-modal-tag.update { background: #fef3c7; color: #b45309; }
.notice-modal-body {
  padding: 20px 28px;
}
.notice-modal-body p {
  font-size: 0.88rem;
  color: #475569;
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
}

/* Responsive */
@media (max-width: 900px) {
  .announce-edit-layout {
    flex-direction: column;
    height: auto;
  }
  .announce-edit-sidebar {
    width: 100%;
  }
  .ai-presets { grid-template-columns: repeat(2, 1fr); }
  .ai-compare { grid-template-columns: 1fr; }
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
</style>
