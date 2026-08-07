<template>
  <div class="admin-page article-edit-page">
    <div class="page-header">
      <h2>{{ isEdit ? $t('adminArticleEdit.editArticle') : $t('adminArticleEdit.writeArticle') }}</h2>
      <div style="display: flex; gap: 8px">
        <el-button @click="aiDrawerVisible = true">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align: -2px; margin-right: 4px"><path d="M12 2a4 4 0 0 0-4 4v2H6a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2h-2V6a4 4 0 0 0-4-4z"/><circle cx="9" cy="14" r="1"/><circle cx="15" cy="14" r="1"/></svg>
          {{ $t('adminArticleEdit.aiAssist') }}
        </el-button>
        <el-button @click="saveDraft" :loading="saving">{{ $t('adminArticleEdit.saveDraft') }}</el-button>
        <el-button type="primary" @click="savePublish" :loading="saving">
          <el-icon><Upload /></el-icon> {{ $t('adminArticleEdit.publish') }}
        </el-button>
      </div>
    </div>

    <div class="editor-layout">
      <div class="editor-main">
        <div class="lang-toggle-bar">
          <button class="lang-tab" :class="{ active: editLang === 'zh' }" @click="editLang = 'zh'">中文</button>
          <button class="lang-tab" :class="{ active: editLang === 'en' }" @click="editLang = 'en'">English</button>
        </div>
        <template v-if="editLang === 'zh'">
          <el-input v-model="form.title" :placeholder="$t('adminArticleEdit.titlePlaceholder')" class="title-input" />
          <MdEditor v-model="form.contentMd" :theme="'light'" class="md-editor-fill" @onUploadImg="onUploadImg" />
        </template>
        <template v-else>
          <el-input v-model="form.titleEn" :placeholder="$t('adminArticleEdit.titleEnPlaceholder')" class="title-input" />
          <MdEditor v-model="form.contentMdEn" :theme="'light'" class="md-editor-fill" @onUploadImg="onUploadImg" />
        </template>
      </div>

      <div class="editor-sidebar">
        <el-card shadow="never" style="margin-bottom: 12px">
          <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminArticleEdit.category') }}</span></template>
          <el-select v-model="form.categoryId" :placeholder="$t('adminArticleEdit.uncategorized')" clearable style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-card>

        <el-card shadow="never" style="margin-bottom: 12px">
          <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminArticleEdit.tags') }}</span></template>
          <el-checkbox-group v-model="form.tagIds">
            <el-checkbox v-for="tag in tags" :key="tag.id" :label="tag.id" :value="tag.id">{{ tag.name }}</el-checkbox>
          </el-checkbox-group>
        </el-card>

        <el-card shadow="never" style="margin-bottom: 12px">
          <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminArticleEdit.urlSlug') }}</span></template>
          <el-input v-model="form.slug" placeholder="my-article-slug" />
        </el-card>

        <el-card shadow="never" style="margin-bottom: 12px">
          <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminArticleEdit.summary') }}</span></template>
          <el-input v-model="form.summary" type="textarea" :rows="10" :placeholder="$t('adminArticleEdit.summaryPlaceholder')" />
          <el-input v-model="form.summaryEn" type="textarea" :rows="10" :placeholder="$t('adminArticleEdit.summaryEnPlaceholder')" style="margin-top: 8px" />
        </el-card>

        <el-card shadow="never" style="margin-bottom: 12px">
          <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminArticleEdit.authorNotes') }}</span></template>
          <el-input v-model="form.authorNotes" type="textarea" :rows="4" :placeholder="$t('adminArticleEdit.authorNotesPlaceholder')" />
        </el-card>

        <el-card shadow="never" style="margin-bottom: 12px">
          <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminArticleEdit.cardStyle') }}</span></template>
          <div class="card-style-grid">
            <div v-for="s in cardStyles" :key="s.value"
              class="card-style-item" :class="{ active: form.cardStyle === s.value, [`style-${s.value}`]: true }"
              @click="form.cardStyle = s.value"
              @mouseenter="onStyleHover($event, s.value)"
              @mousemove="onStyleHover($event, s.value)"
              @mouseleave="onStyleLeave">
              <div class="card-style-preview">
                <div class="card-style-mini"></div>
              </div>
              <span class="card-style-label">{{ s.label }}</span>
            </div>
          </div>
        </el-card>

        <el-card shadow="never">
          <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminArticleEdit.coverImage') }}</span></template>
          <div style="display: flex; flex-direction: column; gap: 8px">
            <el-image v-if="form.coverImage" :src="form.coverImage" style="width: 100%; max-height: 160px; border-radius: 6px" fit="cover" />
            <template v-if="form.coverImage">
              <div style="display: flex; gap: 8px">
                <el-button size="small" @click="form.coverImage = ''">{{ $t('adminArticleEdit.changeCover') }}</el-button>
                <el-button type="danger" text size="small" @click="form.coverImage = ''">{{ $t('adminArticleEdit.remove') }}</el-button>
              </div>
            </template>
            <FileUpload v-else endpoint="/admin/upload/cover" @uploaded="onCoverUploaded" style="height: 180px" />
          </div>
        </el-card>
      </div>
    </div>

    <!-- AI Assist Drawer -->
    <el-drawer v-model="aiDrawerVisible" :title="$t('adminArticleEdit.aiAssistTitle')" direction="rtl" size="480px" :before-close="closeAiDrawer">
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

        <!-- Selected text indicator -->
        <div v-if="hasSelectedText" class="ai-selected-indicator">
          <el-icon><Document /></el-icon>
          <span>{{ $t('ai.ui.textSelected') }}</span>
          <el-button size="small" text @click="clearSelection">{{ $t('ai.ui.clearSelection') }}</el-button>
        </div>

        <!-- Preset modes -->
        <div class="ai-presets">
          <button v-for="p in filteredPresets" :key="p.id"
            class="ai-preset-btn" :class="{ active: aiMode === p.id }"
            :disabled="aiLoading || (p.needsTitle && !form.title.trim()) || (p.needsContent && !form.contentMd.trim())"
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

        <!-- Continue suggestions -->
        <div v-if="aiMode === 'continue' && continueSuggestions.length > 0" class="ai-continue-suggestions">
          <div class="suggestions-label">{{ $t('ai.continue.suggestions') }}</div>
          <div class="suggestions-list">
            <button v-for="(suggestion, index) in continueSuggestions" :key="index"
              class="suggestion-btn" @click="applySuggestion(suggestion)">
              {{ suggestion }}
            </button>
          </div>
        </div>

        <!-- Custom prompt -->
        <div class="ai-custom-section">
          <textarea v-model="aiPrompt" class="ai-prompt-input" rows="3"
            :placeholder="$t('adminArticleEdit.customPromptPlaceholder')"
            @keydown.ctrl.enter="runCustom" :disabled="aiLoading" />
          <div style="display: flex; align-items: center; gap: 8px; margin-top: 6px">
            <button class="ai-generate-btn" :disabled="aiLoading || !aiPrompt.trim()" @click="runCustom">
              {{ aiLoading ? $t('adminArticleEdit.generating') : $t('adminArticleEdit.generate') }}
            </button>
            <span class="ai-shortcut-hint">Ctrl+Enter</span>
          </div>
        </div>

        <!-- Result preview -->
        <div v-if="aiResult || aiLoading || chatHistory.length > 0" ref="aiResultSection" class="ai-result-section">
          <div class="ai-result-header">
            <span class="ai-result-title">{{ $t('adminArticleEdit.preview') }}</span>
            <div style="display: flex; gap: 4px">
              <button v-if="aiResult && !aiLoading" class="ai-cancel-btn" style="font-size: 12px; padding: 2px 8px" @click="compareMode = !compareMode">
                {{ compareMode ? $t('adminArticleEdit.exitCompare') : $t('adminArticleEdit.compareView') }}
              </button>
              <button v-if="aiLoading" class="ai-cancel-btn" @click="cancelAi">{{ $t('adminArticleEdit.cancel') }}</button>
            </div>
          </div>

          <!-- Compare mode: side by side -->
          <div v-if="compareMode && aiResult && !aiLoading" class="ai-compare">
            <div class="ai-compare-pane">
              <div class="ai-compare-label">{{ $t('adminArticleEdit.originalText') }}</div>
              <div class="ai-compare-content">
                <MdPreview :modelValue="selectedRange ? form.contentMd.slice(selectedRange.start, selectedRange.end) : form.contentMd" previewTheme="github" :codeFoldable="false" />
              </div>
            </div>
            <div class="ai-compare-pane">
              <div class="ai-compare-label">{{ $t('adminArticleEdit.aiResult') }}</div>
              <div class="ai-compare-content">
                <MdPreview :modelValue="aiResult" previewTheme="github" :codeFoldable="false" />
              </div>
            </div>
          </div>

          <!-- Normal preview -->
          <div v-else class="ai-result-preview">
            <!-- Chat history -->
            <div v-for="(msg, i) in chatHistory" :key="i" class="ai-chat-msg" :class="msg.role">
              <div class="ai-chat-bubble">
                <MdPreview v-if="msg.role === 'assistant'" :modelValue="msg.content" previewTheme="github" :codeFoldable="false" />
                <span v-else>{{ msg.content.length > 200 ? msg.content.slice(0, 200) + '...' : msg.content }}</span>
              </div>
            </div>
            <!-- Current streaming result -->
            <div v-if="aiResult && chatHistory.length > 0 && chatHistory[chatHistory.length - 1]?.role === 'user'" class="ai-chat-msg assistant">
              <div class="ai-chat-bubble">
                <MdPreview v-if="aiResult" :modelValue="aiResult" previewTheme="github" :codeFoldable="false" />
                <div v-if="aiLoading && !aiResult" class="ai-typing">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
            <!-- First result (no history yet) -->
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
            <span v-if="modelInfo.tokens"> · {{ modelInfo.tokens }} tokens</span>
          </div>

          <!-- Actions -->
          <div v-if="aiResult && !aiLoading" class="ai-actions">
            <button v-if="selectedRange" class="ai-action-btn primary" @click="replaceSelected">{{ $t('adminArticleEdit.replaceSelected') }}</button>
            <button class="ai-action-btn" @click="insertToContent">{{ $t('adminArticleEdit.insertToContent') }}</button>
            <button class="ai-action-btn" @click="replaceContent">{{ $t('adminArticleEdit.replaceContent') }}</button>
            <CopyButton :text="aiResult" @copied="ElMessage.success(t('adminArticleEdit.copiedMsg'))" />
            <button v-if="aiMode === 'summary'" class="ai-action-btn primary" @click="applyToSummary">{{ $t('adminArticleEdit.applyToSummary') }}</button>
            <button v-if="aiMode === 'summaryBi'" class="ai-action-btn primary" @click="applyToSummaryBi">{{ $t('adminArticleEdit.applyToSummaryBi') }}</button>
            <button v-if="aiMode === 'translate'" class="ai-action-btn primary" @click="applyToEnglishFields">{{ $t('adminArticleEdit.applyToEnglish') }}</button>
            <button v-if="previousContent" class="ai-action-btn" @click="undoReplace" style="color: var(--el-color-warning)">{{ $t('adminArticleEdit.undoReplace') }}</button>
          </div>

          <!-- Continue chat -->
          <div v-if="chatHistory.length > 0 && !aiLoading" class="ai-continue-section">
            <textarea v-model="continueInput" class="ai-prompt-input" rows="2"
              :placeholder="$t('adminArticleEdit.continuePlaceholder')"
              @keydown.ctrl.enter="continueChat" />
            <button class="ai-generate-btn" :disabled="!continueInput.trim()" @click="continueChat">
              {{ $t('adminArticleEdit.send') }}
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

    <!-- Floating card style preview -->
    <Teleport to="body">
      <div v-if="hoverStyle !== null" class="style-hover-preview" :class="`preview-style-${hoverStyle}`"
        :style="{ left: hoverPos.x + 'px', top: hoverPos.y + 'px' }">
        <div class="preview-card">
          <div class="preview-cover"></div>
          <div class="preview-body">
            <div class="preview-title">{{ $t('adminArticleEdit.exampleTitle') }}</div>
            <div class="preview-summary">{{ $t('adminArticleEdit.exampleSummary') }}</div>
            <div class="preview-meta">
              <span class="preview-cat">{{ $t('adminArticleEdit.category') }}</span>
              <span class="preview-date">2026-07-04</span>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick, onUnmounted, watch, defineAsyncComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { Upload, Document } from '@element-plus/icons-vue'
const MdEditor = defineAsyncComponent(() => import('md-editor-v3').then(m => m.MdEditor))
const MdPreview = defineAsyncComponent(() => import('md-editor-v3').then(m => m.MdPreview))
import 'md-editor-v3/lib/style.css'
import 'md-editor-v3/lib/preview.css'
import('../../utils/mdEditorConfig')
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import request from '../../utils/request'
import { useAuthStore } from '../../stores/auth'
import { aiEditorStream } from '../../utils/ai'
import { stripMarkdown } from '../../utils/stripMarkdown'
import FileUpload from '../../components/FileUpload.vue'
import CopyButton from '../../components/CopyButton.vue'
const CustomPresetManager = defineAsyncComponent(() => import('../../components/ai/CustomPresetManager.vue'))
const TemplateManager = defineAsyncComponent(() => import('../../components/ai/TemplateManager.vue'))
const KnowledgeManager = defineAsyncComponent(() => import('../../components/ai/KnowledgeManager.vue'))
const AIHistoryManager = defineAsyncComponent(() => import('../../components/ai/AIHistoryManager.vue'))
const AgentWriter = defineAsyncComponent(() => import('../../components/ai/AgentWriter.vue'))
import { injectCopyButtons } from '../../utils/copyUtils'
import { getCustomPresets } from '../../ai/presets/custom'
import { buildContinuePrompt, getContinueSuggestions } from '../../ai/core/continue'
import { generateSeoPrompt } from '../../ai/analysis/index'
import { buildKnowledgeContext } from '../../ai/knowledge/index'
import { addHistory } from '../../ai/history/index'
import { analyzeImageUsage, generateAltTextPrompt, generateImageSuggestionPrompt } from '../../ai/image/index'
import { analyzeLanguage, getLanguageTips, generateMultilingualSummaryPrompt } from '../../ai/language/index'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { t } = useI18n()

const isEdit = computed(() => !!route.params.id)
const saving = ref(false)
const categories = ref([])
const tags = ref([])

const cardStyles = computed(() => [
  { value: 0, label: t('adminArticleEdit.styleDefault') },
  { value: 1, label: t('adminArticleEdit.styleShadow') },
  { value: 2, label: t('adminArticleEdit.styleMagic') },
  { value: 3, label: t('adminArticleEdit.styleRotatingBorder') },
  { value: 4, label: t('adminArticleEdit.styleNotebook') },
  { value: 5, label: t('adminArticleEdit.styleTicket') },
  { value: 6, label: t('adminArticleEdit.styleFlip') },
  { value: 7, label: t('adminArticleEdit.styleCyber') },
  { value: 8, label: t('adminArticleEdit.styleGradientBorder') },
  { value: 9, label: t('adminArticleEdit.styleEducation') },
  { value: 10, label: t('adminArticleEdit.styleHoloTicket') },
  { value: 12, label: t('adminArticleEdit.style3D') },
  { value: 13, label: t('adminArticleEdit.styleGlow') },
  { value: 14, label: t('adminArticleEdit.styleNeonBorder') },
  { value: 15, label: t('adminArticleEdit.styleGradientGlow') },
  { value: 17, label: t('adminArticleEdit.styleMagicGradient') },
  { value: 18, label: t('adminArticleEdit.stylePhysicsCard') },
  { value: 19, label: t('adminArticleEdit.style3DFlip') },
  { value: 20, label: t('adminArticleEdit.styleCyber3D') },
  { value: 21, label: t('adminArticleEdit.styleGradientHalo') },
  { value: 22, label: t('adminArticleEdit.styleEducationPlus') },
  { value: 23, label: t('adminArticleEdit.styleNightSky') },
  { value: 24, label: t('adminArticleEdit.styleEnhancedTicket') },
  { value: 25, label: t('adminArticleEdit.style3DTilt') },
  { value: 26, label: t('adminArticleEdit.styleLightSweep') },
  { value: 27, label: t('adminArticleEdit.styleNeonZoom') },
  { value: 28, label: t('adminArticleEdit.styleGlassmorphism') },
  { value: 29, label: t('adminArticleEdit.styleAurora') },
  { value: 30, label: t('adminArticleEdit.styleRetroPixel') },
  { value: 31, label: t('adminArticleEdit.styleWaterRipple') },
  { value: 32, label: t('adminArticleEdit.styleMinimalLine') },
  { value: 33, label: t('adminArticleEdit.styleSunset') },
  { value: 34, label: t('adminArticleEdit.styleFrostedEdge') },
  { value: 35, label: t('adminArticleEdit.styleMagnetic') },
  { value: 36, label: t('adminArticleEdit.styleOrigami') },
  { value: 37, label: t('adminArticleEdit.styleMatrix') },
  { value: 38, label: t('adminArticleEdit.styleVaporwave') },
  { value: 39, label: t('adminArticleEdit.styleCircuit') }
])

// Card style hover preview
const hoverStyle = ref(null)
const hoverPos = reactive({ x: 0, y: 0 })
let hoverTimer = null

function onStyleHover(e, styleValue) {
  if (hoverTimer) { clearTimeout(hoverTimer); hoverTimer = null }
  hoverStyle.value = styleValue
  const cardW = 220, cardH = 260, gap = 14
  let x = e.clientX - cardW / 2
  let y = e.clientY - cardH - gap
  if (x < 8) x = 8
  if (x + cardW > window.innerWidth - 8) x = window.innerWidth - cardW - 8
  if (y < 8) y = e.clientY + gap
  hoverPos.x = x
  hoverPos.y = y
}

function onStyleLeave() {
  hoverTimer = setTimeout(() => { hoverStyle.value = null }, 80)
}

const form = reactive({
  title: '', titleEn: '', contentMd: '', contentMdEn: '', summary: '', summaryEn: '',
  coverImage: '', slug: '', categoryId: null, tagIds: [], cardStyle: 0, authorNotes: ''
})
const editLang = ref('zh') // 'zh' or 'en'

// === AI Assist ===
const WRITING_SYSTEM_PROMPT = `你是一个专业技术博客写作助手。你的职责是帮助作者撰写、润色、续写、翻译和优化技术博客文章。

能力范围：
- 文章撰写与生成（根据标题和大纲生成完整文章）
- 内容润色与优化（改进表达、增强可读性）
- 续写与扩展（延续风格和主题）
- 中英文双向翻译（保持技术术语准确）
- SEO 优化（标题、摘要、关键词建议）
- 语气改写（正式/轻松/学术风格切换）
- 代码解释（将代码段用通俗语言解释）
- 段落拆分（将冗长段落拆分为清晰短段）
- 补充参考来源（建议相关的权威资料链接）

输出要求：
- 使用 Markdown 格式输出
- 保持专业技术性，尊重原文风格
- 代码块使用正确的语言标识
- 翻译时保留技术术语，不生硬翻译专有名词`

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
const continueSuggestions = ref([])

watch(aiResult, () => {
  nextTick(() => { if (aiResultSection.value) injectCopyButtons(aiResultSection.value) })
})
const chatHistory = ref([]) // [{role:'user'|'assistant', content}]
const previousContent = ref('')
const selectedRange = ref(null) // {start, end}
const hasSelectedText = ref(false)
const persistedSelectedText = ref('') // 保存选中文本，防止点击按钮时丢失
const compareMode = ref(false)
const modelInfo = ref(null) // {model, tokens}
const continueInput = ref('')
let abortFn = null

// Language detection for system prompt
function detectLanguage(text) {
  if (!text) return 'zh'
  const chineseChars = (text.match(/[一-鿿]/g) || []).length
  return chineseChars > text.length * 0.15 ? 'zh' : 'en'
}

const aiPresets = computed(() => {
  const builtIn = [
    { id: 'generate', icon: '✨', label: t('adminArticleEdit.aiPresetGenerate'), needsTitle: true, category: 'writing' },
    { id: 'outline', icon: '🗂️', label: t('adminArticleEdit.aiPresetOutline'), needsTitle: true, category: 'writing' },
    { id: 'expand', icon: '📖', label: t('adminArticleEdit.aiPresetExpand'), needsContent: true, localOp: true, category: 'writing' },
    { id: 'polish', icon: '🔧', label: t('adminArticleEdit.aiPresetPolish'), needsContent: true, localOp: true, category: 'writing' },
    { id: 'continue', icon: '✍️', label: t('adminArticleEdit.aiPresetContinue'), needsContent: true, category: 'writing' },
    { id: 'summary', icon: '📝', label: t('adminArticleEdit.aiPresetSummary'), needsContent: true, category: 'analysis' },
    { id: 'summaryBi', icon: '📋', label: t('adminArticleEdit.aiPresetSummaryBi'), needsContent: true, category: 'analysis' },
    { id: 'seo', icon: '🎯', label: t('adminArticleEdit.aiPresetSeo'), needsContent: true, category: 'enhance' },
    { id: 'title', icon: '💡', label: t('adminArticleEdit.aiPresetTitle'), needsContent: true, category: 'writing' },
    { id: 'translate', icon: '🌐', label: t('adminArticleEdit.aiPresetTranslate'), needsContent: true, category: 'translate' },
    { id: 'translateZH', icon: '🇨🇳', label: t('adminArticleEdit.aiPresetTranslateZH'), needsContent: true, localOp: true, category: 'translate' },
    { id: 'translateEN', icon: '🇬🇧', label: t('adminArticleEdit.aiPresetTranslateEN'), needsContent: true, localOp: true, category: 'translate' },
    { id: 'toneRewrite', icon: '🎨', label: t('adminArticleEdit.aiPresetToneRewrite'), needsContent: true, localOp: true, category: 'tone' },
    { id: 'codeExplain', icon: '💻', label: t('adminArticleEdit.aiPresetCodeExplain'), needsContent: true, localOp: true, category: 'analysis' },
    { id: 'splitParagraph', icon: '📄', label: t('adminArticleEdit.aiPresetSplitParagraph'), needsContent: true, localOp: true, category: 'writing' },
    { id: 'references', icon: '📚', label: t('adminArticleEdit.aiPresetReferences'), needsContent: true, category: 'enhance' },
    { id: 'imageAlt', icon: ' ️', label: t('adminArticleEdit.aiPresetImageAlt'), needsContent: true, category: 'enhance' },
    { id: 'imageSuggest', icon: ' ️', label: t('adminArticleEdit.aiPresetImageSuggest'), needsContent: true, category: 'enhance' },
    { id: 'multilingualSummary', icon: ' ', label: t('adminArticleEdit.aiPresetMultilingualSummary'), needsContent: true, category: 'translate' },
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
  form.contentMd = result
  ElMessage.success(t('ai.agent.completed'))
}

// Get selected text from the active editor
function getSelectedText() {
  const sel = window.getSelection()
  if (sel && !sel.isCollapsed && sel.toString().trim()) {
    const text = sel.toString().trim()
    const content = editLang.value === 'en' ? form.contentMdEn : form.contentMd
    const idx = content.indexOf(text)
    if (idx !== -1) {
      selectedRange.value = { start: idx, end: idx + text.length }
    }
    hasSelectedText.value = true
    persistedSelectedText.value = text
    return text
  }
  // Fallback: use persisted text (selection may be lost due to button click)
  if (persistedSelectedText.value) {
    hasSelectedText.value = true
    const content = editLang.value === 'en' ? form.contentMdEn : form.contentMd
    const idx = content.indexOf(persistedSelectedText.value)
    if (idx !== -1) {
      selectedRange.value = { start: idx, end: idx + persistedSelectedText.value.length }
    }
    return persistedSelectedText.value
  }
  hasSelectedText.value = false
  return null
}

function clearSelection() {
  window.getSelection()?.removeAllRanges()
  selectedRange.value = null
  hasSelectedText.value = false
  persistedSelectedText.value = ''
}

function buildContext() {
  const parts = []
  const content = editLang.value === 'en' ? form.contentMdEn : form.contentMd
  const lang = detectLanguage(content)
  parts.push(`[文章语言: ${lang === 'zh' ? '中文' : 'English'}]`)
  const title = editLang.value === 'en' ? form.titleEn : form.title
  const summary = editLang.value === 'en' ? form.summaryEn : form.summary
  if (title) parts.push(`【标题】\n${title}`)
  if (summary) parts.push(`【摘要】\n${summary}`)

  const selectedText = getSelectedText()
  if (selectedText) {
    parts.push(`【用户选中的文本（重点关注）】\n${selectedText}`)
    if (content && content.length > selectedText.length) {
      parts.push(`【全文上下文（供参考）】\n${content}`)
    }
  } else {
    if (content) parts.push(`【正文】\n${content}`)
  }
  return parts.join('\n\n')
}

function buildSystemPrompt() {
  const content = editLang.value === 'en' ? form.contentMdEn : form.contentMd
  const lang = detectLanguage(content)
  let prompt = WRITING_SYSTEM_PROMPT
  if (lang === 'en') {
    prompt += '\n\nThe article is written in English. Please respond in English.'
  } else {
    prompt += '\n\n文章是中文撰写的，请用中文回复。'
  }
  const kbContext = buildKnowledgeContext()
  if (kbContext) {
    prompt += '\n\n' + kbContext
  }
  const tips = getLanguageTips(lang)
  if (tips.length > 0) {
    prompt += '\n\n【写作建议】\n' + tips.map(t => `- ${t}`).join('\n')
  }
  return prompt
}

// Local operation presets that work on selected text
const LOCAL_OP_PRESETS = ['polish', 'expand', 'toneRewrite', 'translateZH', 'translateEN', 'splitParagraph', 'codeExplain']

function runPreset(mode) {
  aiMode.value = mode
  aiError.value = ''
  aiResult.value = ''
  selectedRange.value = null
  compareMode.value = false
  modelInfo.value = null

  // Generate continue suggestions when continue preset is selected
  if (mode === 'continue' && form.contentMd) {
    continueSuggestions.value = getContinueSuggestions(form.contentMd)
  } else {
    continueSuggestions.value = []
  }

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
    generate: `${t('adminArticleEdit.aiPromptGenerate')}\n\n${ctx}`,
    outline: `${t('adminArticleEdit.aiPromptOutline')}\n\n${ctx}`,
    expand: isLocalOp
      ? `${t('adminArticleEdit.aiPromptExpandLocal')}\n\n【待扩展的文本】\n${selectedText}`
      : `${t('adminArticleEdit.aiPromptExpand')}\n\n${ctx}`,
    polish: isLocalOp
      ? `${t('adminArticleEdit.aiPromptPolishLocal')}\n\n【待润色的文本】\n${selectedText}`
      : `${t('adminArticleEdit.aiPromptPolish')}\n\n${ctx}`,
    continue: selectedText
      ? buildContinuePrompt(selectedText, { language: detectLanguage(selectedText) })
      : buildContinuePrompt(form.contentMd, { language: detectLanguage(form.contentMd) }),
    summary: selectedText
      ? `${t('adminArticleEdit.aiPromptSummary')}\n\n【请对选中文本进行摘要】\n${ctx}`
      : `${t('adminArticleEdit.aiPromptSummary')}\n\n${ctx}`,
    summaryBi: selectedText
      ? `${t('adminArticleEdit.aiPromptSummaryBi')}\n\n【请对选中文本进行双语摘要】\n${ctx}`
      : `${t('adminArticleEdit.aiPromptSummaryBi')}\n\n${ctx}`,
    seo: selectedText
      ? generateSeoPrompt(selectedText, { keyword: form.title })
      : generateSeoPrompt(form.contentMd, { keyword: form.title }),
    title: selectedText
      ? `${t('adminArticleEdit.aiPromptTitle')}\n\n【请为选中文本生成标题】\n${ctx}`
      : `${t('adminArticleEdit.aiPromptTitle')}\n\n${ctx}`,
    translate: `${t('adminArticleEdit.aiPromptTranslate')}\n\n${ctx}`,
    translateZH: isLocalOp
      ? `${t('adminArticleEdit.aiPromptTranslateZHLocal')}\n\n【待翻译的文本】\n${selectedText}`
      : `${t('adminArticleEdit.aiPromptTranslateZH')}\n\n${ctx}`,
    translateEN: isLocalOp
      ? `${t('adminArticleEdit.aiPromptTranslateENLocal')}\n\n【待翻译的文本】\n${selectedText}`
      : `${t('adminArticleEdit.aiPromptTranslateEN')}\n\n${ctx}`,
    toneRewrite: isLocalOp
      ? `${t('adminArticleEdit.aiPromptToneRewriteLocal')}\n\n【待改写的文本】\n${selectedText}`
      : `${t('adminArticleEdit.aiPromptToneRewrite')}\n\n${ctx}`,
    codeExplain: isLocalOp
      ? `${t('adminArticleEdit.aiPromptCodeExplainLocal')}\n\n【待解释的代码】\n${selectedText}`
      : `${t('adminArticleEdit.aiPromptCodeExplain')}\n\n${ctx}`,
    splitParagraph: isLocalOp
      ? `${t('adminArticleEdit.aiPromptSplitParagraphLocal')}\n\n【待拆分的段落】\n${selectedText}`
      : `${t('adminArticleEdit.aiPromptSplitParagraph')}\n\n${ctx}`,
    references: `${t('adminArticleEdit.aiPromptReferences')}\n\n${ctx}`,
    imageAlt: generateAltTextPrompt('', selectedText || form.contentMd),
    imageSuggest: generateImageSuggestionPrompt(selectedText || form.contentMd),
    multilingualSummary: generateMultilingualSummaryPrompt(selectedText || form.contentMd, detectLanguage(selectedText || form.contentMd)),
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
  const msg = `${aiPrompt.value}\n\n---\n${t('adminArticleEdit.aiContextLabel')}\n${ctx}`
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
  abortFn = aiEditorStream({ messages, systemPrompt, maxTokens: 16384 }, {
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
  abortFn = aiEditorStream({ messages, systemPrompt, maxTokens: 16384 }, {
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
        contentType: 'article',
      })
    },
    onError(err) { aiError.value = String(err); aiLoading.value = false; abortFn = null }
  })
}

function cancelAi() {
  if (abortFn) { abortFn(); abortFn = null }
  aiLoading.value = false
}

function applySuggestion(suggestion) {
  aiPrompt.value = suggestion
  runCustom()
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
  hasSelectedText.value = false
  persistedSelectedText.value = ''
  compareMode.value = false
  modelInfo.value = null
  continueInput.value = ''
  done()
}

function insertToContent() {
  if (!aiResult.value) return
  if (editLang.value === 'en') {
    form.contentMdEn = form.contentMdEn ? form.contentMdEn + '\n\n' + aiResult.value : aiResult.value
  } else {
    form.contentMd = form.contentMd ? form.contentMd + '\n\n' + aiResult.value : aiResult.value
  }
  ElMessage.success(t('adminArticleEdit.insertedMsg'))
}

async function replaceContent() {
  if (!aiResult.value) return
  try {
    await ElMessageBox.confirm(
      t('adminArticleEdit.confirmReplaceMsg'),
      t('adminArticleEdit.confirmReplaceTitle'),
      { confirmButtonText: t('adminArticleEdit.confirm'), cancelButtonText: t('adminArticleEdit.cancel'), type: 'warning' }
    )
  } catch { return }
  if (editLang.value === 'en') {
    previousContent.value = form.contentMdEn
    form.contentMdEn = aiResult.value
  } else {
    previousContent.value = form.contentMd
    form.contentMd = aiResult.value
  }
  ElMessage({
    message: t('adminArticleEdit.replacedMsg'),
    type: 'success',
    duration: 5000,
    showClose: true
  })
}

function undoReplace() {
  if (!previousContent.value) return
  if (editLang.value === 'en') {
    form.contentMdEn = previousContent.value
  } else {
    form.contentMd = previousContent.value
  }
  previousContent.value = ''
  ElMessage.success(t('adminArticleEdit.undoSuccessMsg'))
}

function replaceSelected() {
  if (!aiResult.value || !selectedRange.value) return
  const { start, end } = selectedRange.value
  const content = editLang.value === 'en' ? form.contentMdEn : form.contentMd
  const before = content.slice(0, start)
  const after = content.slice(end)
  if (editLang.value === 'en') {
    form.contentMdEn = before + aiResult.value + after
  } else {
    form.contentMd = before + aiResult.value + after
  }
  selectedRange.value = null
  ElMessage.success(t('adminArticleEdit.replacedSelectedMsg'))
}


function applyToSummary() {
  if (!aiResult.value) return
  form.summary = stripMarkdown(aiResult.value)
  ElMessage.success(t('adminArticleEdit.appliedToSummaryMsg'))
}

function applyToSummaryBi() {
  if (!aiResult.value) return
  const text = aiResult.value
  const parts = text.split(/(?=\s*(?:英文摘要|English Summary)\s*[:：])/i)
  const zhMatch = parts[0]?.match(/(?:中文摘要|Chinese Summary)\s*[:：]\s*([\s\S]*)/i)
  const enMatch = parts[1]?.match(/(?:英文摘要|English Summary)\s*[:：]\s*([\s\S]*)/i)
  if (zhMatch) form.summary = stripMarkdown(zhMatch[1].trim())
  if (enMatch) form.summaryEn = stripMarkdown(enMatch[1].trim())
  if (!zhMatch && !enMatch) {
    form.summary = stripMarkdown(text)
  }
  ElMessage.success(t('adminArticleEdit.appliedToSummaryBiMsg'))
}

function applyToEnglishFields() {
  if (!aiResult.value) return
  const text = aiResult.value
  const titleMatch = text.match(/(?:^|\n)\s*(?:标题|Title)\s*[:：]\s*(.+)/i)
  const summaryMatch = text.match(/(?:^|\n)\s*(?:摘要|Summary)\s*[:：]\s*([\s\S]*?)(?=\n\s*(?:正文|Content|Body)\s*[:：]|\n\n\n)/i)
  let applied = []
  if (titleMatch) { form.titleEn = titleMatch[1].trim(); applied.push('titleEn') }
  if (summaryMatch) { form.summaryEn = stripMarkdown(summaryMatch[1].trim()); applied.push('summaryEn') }
  if (titleMatch || summaryMatch) {
    const contentMatch = text.match(/(?:^|\n)\s*(?:正文|Content|Body)\s*[:：]\s*([\s\S]*)/i)
    if (contentMatch) { form.contentMdEn = contentMatch[1].trim(); applied.push('contentMdEn') }
  }
  if (applied.length === 0) { form.contentMdEn = text; applied.push('contentMdEn') }
  ElMessage.success(t('adminArticleEdit.appliedToEnglishMsg'))
}

// Keyboard shortcuts
function onGlobalKeydown(e) {
  if (e.ctrlKey && e.shiftKey && e.key === 'A') {
    e.preventDefault()
    aiDrawerVisible.value = !aiDrawerVisible.value
  }
}
onMounted(() => document.addEventListener('keydown', onGlobalKeydown))
onUnmounted(() => {
  document.removeEventListener('keydown', onGlobalKeydown)
  if (abortFn) { abortFn(); abortFn = null }
  if (hoverTimer) { clearTimeout(hoverTimer); hoverTimer = null }
})

function onSelectionChange() {
  const sel = window.getSelection()
  if (sel && !sel.isCollapsed && sel.toString().trim()) {
    hasSelectedText.value = true
    persistedSelectedText.value = sel.toString().trim()
  }
  // 不在选区清除时重置 hasSelectedText，让抽屉内的指示器保持显示
}
onMounted(() => document.addEventListener('selectionchange', onSelectionChange))
onUnmounted(() => document.removeEventListener('selectionchange', onSelectionChange))

async function loadData() {
  try {
    const [cats, tagList] = await Promise.all([
      request.get('/admin/categories'),
      request.get('/admin/tags')
    ])
    categories.value = cats
    tags.value = tagList

    if (isEdit.value) {
      const article = await request.get(`/admin/articles/${route.params.id}`)
      form.title = article.title
      form.titleEn = article.titleEn || ''
      form.contentMd = article.contentMd || ''
      form.contentMdEn = article.contentMdEn || ''
      form.summary = article.summary || ''
      form.summaryEn = article.summaryEn || ''
      form.coverImage = article.coverImage || ''
      form.slug = article.slug || ''
      form.categoryId = article.categoryId
      form.tagIds = (article.tags || []).map(t => t.id)
      form.cardStyle = article.cardStyle ?? 0
      form.authorNotes = article.authorNotes || ''
    }
  } catch (e) {
    ElMessage.error(e.message || t('adminArticleEdit.loadFailed'))
    if (isEdit.value) {
      router.push('/admin/articles')
    }
  }
}

async function saveDraft() { await doSave(0) }
async function savePublish() { await doSave(1) }

async function doSave(status) {
  if (saving.value) return
  if (!form.title.trim()) { ElMessage.warning(t('adminArticleEdit.enterTitleWarning')); return }
  saving.value = true
  try {
    const htmlContent = form.contentMd ? DOMPurify.sanitize(marked.parse(form.contentMd)) : ''
    const htmlContentEn = form.contentMdEn ? DOMPurify.sanitize(marked.parse(form.contentMdEn)) : ''
    const body = {
      title: form.title, titleEn: form.titleEn || null,
      contentMd: form.contentMd, contentMdEn: form.contentMdEn || null,
      contentHtml: htmlContent, contentHtmlEn: htmlContentEn || null,
      summary: form.summary || null, summaryEn: form.summaryEn || null,
      coverImage: form.coverImage || null,
      slug: form.slug || null, categoryId: form.categoryId, tagIds: form.tagIds,
      cardStyle: form.cardStyle, authorNotes: form.authorNotes || null
    }
    let articleId
    if (isEdit.value) {
      await request.put(`/admin/articles/${route.params.id}`, body)
      articleId = route.params.id
    } else {
      const result = await request.post('/admin/articles', body)
      articleId = result.id
    }
    if (status === 1) {
      await request.patch(`/admin/articles/${articleId}/status?status=1`)
    }
    if (status === 1 && authStore.role === 'admin') {
      ElMessage.success(t('adminArticleEdit.pendingReviewSuccessMsg'))
    } else {
      ElMessage.success(status === 1 ? t('adminArticleEdit.publishedMsg') : t('adminArticleEdit.draftSavedMsg'))
    }
    router.push('/admin/articles')
  } catch (e) {
    if (e.code === 8002) {
      ElMessage.warning(e.message || t('adminArticleEdit.pendingReviewMsg'))
      router.push('/admin/articles')
    } else {
      ElMessage.error(t('adminArticleEdit.saveFailed', { error: e.message || t('adminArticleEdit.unknownError') }))
    }
  } finally {
    saving.value = false
  }
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

function onCoverUploaded(res) {
  form.coverImage = res.url
}

onMounted(loadData)
onMounted(loadCustomPresets)
</script>

<style scoped>
.article-edit-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px - 48px); /* minus header 60px, main padding 48px */
}
.editor-layout {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
}
.editor-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.lang-toggle-bar {
  display: flex;
  gap: 0;
  margin-bottom: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
  width: fit-content;
}
.lang-tab {
  padding: 6px 20px;
  border: none;
  background: var(--el-fill-color-blank);
  color: var(--el-text-color-secondary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s, box-shadow 0.2s;
}
.lang-tab + .lang-tab {
  border-left: 1px solid var(--el-border-color);
}
.lang-tab.active {
  background: var(--el-color-primary);
  color: #fff;
}
.lang-tab:hover:not(.active) {
  background: var(--el-fill-color-light);
}
.editor-main .title-input {
  margin-bottom: 12px;
}
.editor-main :deep(.el-input__inner) {
  font-size: 1.25rem;
  font-weight: 600;
  padding: 12px;
}
.md-editor-fill {
  flex: 1;
  min-height: 0;
}
.editor-sidebar {
  width: 280px;
  flex-shrink: 0;
  overflow-y: auto;
}
.card-style-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  contain: layout style;
}
.card-style-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px;
  border: 2px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s, box-shadow 0.2s;
  position: relative;
}
.card-style-item:hover {
  background: var(--el-fill-color-light);
}
.card-style-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.card-style-preview {
  width: 56px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.card-style-mini {
  width: 44px;
  height: 30px;
  border-radius: 4px;
  background: var(--el-fill-color-lighter);
  overflow: hidden;
  position: relative;
  isolation: isolate;
}
/* Style 0: 默认 */
.style-0 .card-style-mini {
  border: 1px solid var(--el-border-color-lighter);
}
/* Style 1: 阴影 */
.style-1 .card-style-mini {
  box-shadow: 2px 3px 8px rgba(0,0,0,0.2);
  border: none;
}
/* Style 2: 魔法 */
.style-2 .card-style-mini {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
}
/* Style 3: 旋转边框 */
.style-3 .card-style-mini {
  border: none;
  background: transparent;
}
.style-3 .card-style-mini::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: conic-gradient(from 0deg, #f09, #0ff, #f09, #0ff, #f09);
  animation: mini-spin 3s linear infinite;
  animation-play-state: paused;
}
.style-3:hover .card-style-mini::before { animation-play-state: running; }
.style-3 .card-style-mini::after {
  content: '';
  position: absolute;
  inset: 2px;
  background: var(--el-fill-color-lighter);
  border-radius: 2px;
}
@keyframes mini-spin { to { transform: rotate(360deg); } }
/* Style 4: 笔记本 */
.style-4 .card-style-mini {
  background: #fef9ef;
  border: 1px solid #e8dcc8;
  border-left: 3px solid #f59e0b;
}
/* Style 5: 门票 */
.style-5 .card-style-mini {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  border-radius: 4px 4px 0 0;
  position: relative;
}
.style-5 .card-style-mini::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  border-bottom: 1px dashed rgba(255,255,255,0.5);
}
/* Style 6: 翻转 */
.style-6 .card-style-mini {
  background: linear-gradient(135deg, #00c6fb, #005bea);
  border: none;
  perspective: 60px;
}
/* Style 7: 赛博 */
.style-7 .card-style-mini {
  background: #0a0a2e;
  border: 1px solid #0ff;
  box-shadow: 0 0 4px rgba(0,255,255,0.3);
}
/* Style 8: 渐变边框 */
.style-8 .card-style-mini {
  border: 2px solid transparent;
  background: linear-gradient(#fff, #fff) padding-box,
              linear-gradient(135deg, #f09, #0ff) border-box;
}
/* Style 9: 教育 */
.style-9 .card-style-mini {
  background: #ffd861;
  border-radius: 4px 4px 4px 0;
  position: relative;
}
.style-9 .card-style-mini::after {
  content: '';
  position: absolute;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #ffeeba;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
/* Style 10: 全息门票 */
.style-10 .card-style-mini {
  background: linear-gradient(135deg, #ccc, #ff6bfe, #00f9f8, #0081fd, #ccc);
  border: none;
}
/* Style 12: 立体 */
.style-12 .card-style-mini {
  background: linear-gradient(135deg, #38bdf8, #0284c7, #075985);
  transform: rotate3d(1, -1, 1, 10deg);
  box-shadow: 4px 4px 0 0 #0d0d0d;
  border: 1px solid rgba(255,255,255,0.2);
}
/* Style 13: 光晕 */
.style-13 .card-style-mini {
  background: #171717;
  border: none;
}
.style-13 .card-style-mini::before {
  content: '';
  position: absolute;
  inset: -2px;
  background: linear-gradient(#ff2288, #387ef0);
  animation: mini-spin 3s linear infinite;
  animation-play-state: paused;
  opacity: 0;
}
.style-13.active .card-style-mini::before,
.style-13:hover .card-style-mini::before { opacity: 1; animation-play-state: running; }
/* Style 14: 霓虹边框 */
.style-14 .card-style-mini {
  background: linear-gradient(163deg, #00ff75, #3700ff);
  border-radius: 6px;
  padding: 2px;
}
.style-14 .card-style-mini::after {
  content: '';
  display: block;
  width: 100%;
  height: 100%;
  background: #1a1a1a;
  border-radius: 4px;
}
/* Style 15: 渐变发光 */
.style-15 .card-style-mini {
  background: linear-gradient(to left, #f7ba2b, #ea5358);
  border-radius: 6px;
  position: relative;
}
.style-15 .card-style-mini::after {
  content: '';
  position: absolute;
  inset: 3px;
  background: #181818;
  border-radius: 4px;
}
.card-style-label {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}
/* Hover preview tooltip */
.style-hover-preview {
  position: fixed;
  z-index: 2500;
  pointer-events: none;
}
/* Preview animations always play when visible */
.preview-card::before,
.preview-card::after {
  animation-play-state: running;
}
.preview-card {
  width: 220px;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 8px 30px rgba(0,0,0,0.18);
  border: 1px solid rgba(0,0,0,0.08);
}
.preview-cover {
  height: 70px;
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
}
.preview-body {
  padding: 10px 12px;
}
.preview-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}
.preview-summary {
  font-size: 11px;
  color: #666;
  margin-bottom: 6px;
  line-height: 1.4;
}
.preview-meta {
  display: flex;
  gap: 8px;
  font-size: 10px;
  color: #999;
}
.preview-cat {
  color: #4f8ef7;
  font-weight: 500;
}

/* ===== Preview style variants ===== */
/* Style 0: 默认 */
.preview-style-0 .preview-card {
  border: 1px solid #e5e7eb;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
/* Style 1: 阴影 */
.preview-style-1 .preview-card {
  box-shadow: inset 0 0 15px rgba(0,0,0,0.08), 0 4px 12px rgba(0,0,0,0.1);
}
/* Style 2: 魔法 */
.preview-style-2 .preview-cover { display: none; }
.preview-style-2 .preview-card {
  border: none;
  position: relative;
}
.preview-style-2 .preview-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 12px;
  padding: 2px;
  background: linear-gradient(135deg, #667eea, #764ba2, #f093fb, #667eea);
  background-size: 300% 300%;
  animation: pv-magic 3s ease infinite;
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
}
.preview-style-2 .preview-card::after {
  content: '';
  position: absolute;
  inset: 2px;
  border-radius: 10px;
  background: #fff;
  z-index: -1;
}
@keyframes pv-magic { 0%{background-position:0% 50%} 50%{background-position:100% 50%} 100%{background-position:0% 50%} }
/* Style 3: 旋转边框 */
.preview-style-3 .preview-card {
  border: none;
  position: relative;
  overflow: hidden;
}
.preview-style-3 .preview-card::before {
  content: '';
  position: absolute;
  top: -50%; left: -50%;
  width: 200%; height: 200%;
  background: conic-gradient(from 0deg, #f09, #0ff, #f09, #0ff, #f09);
  animation: pv-rot 3s linear infinite;
}
.preview-style-3 .preview-card::after {
  content: '';
  position: absolute;
  inset: 2px;
  border-radius: 10px;
  background: #fff;
  z-index: 0;
}
.preview-style-3 .preview-cover { display: none; }
.preview-style-3 .preview-body { position: relative; z-index: 1; }
@keyframes pv-rot { to { transform: rotate(360deg); } }
/* Style 4: 笔记本 */
.preview-style-4 .preview-cover { display: none; }
.preview-style-4 .preview-card {
  background: #fef9ef;
  border-color: #e8dcc8;
  border-left: 3px solid #f59e0b;
}
.preview-style-4 .preview-title { font-family: Georgia, serif; }
/* Style 5: 门票 */
.preview-style-5 .preview-card {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
}
.preview-style-5 .preview-cover { display: none; }
.preview-style-5 .preview-title { color: #fff; }
.preview-style-5 .preview-summary { color: rgba(255,255,255,0.8); }
.preview-style-5 .preview-meta { color: rgba(255,255,255,0.6); }
.preview-style-5 .preview-cat { color: #ffd700; }
/* Style 6: 翻转 */
.preview-style-6 .preview-cover { display: none; }
.preview-style-6 .preview-card {
  background: linear-gradient(135deg, #00c6fb, #005bea);
  border: none;
}
.preview-style-6 .preview-title { color: #fff; }
.preview-style-6 .preview-summary { color: rgba(255,255,255,0.8); }
.preview-style-6 .preview-meta { color: rgba(255,255,255,0.6); }
.preview-style-6 .preview-cat { color: #ffd700; }
/* Style 7: 赛博 */
.preview-style-7 .preview-card {
  background: #0a0a2e;
  border-color: #0ff;
  box-shadow: 0 0 8px rgba(0,255,255,0.2);
}
.preview-style-7 .preview-cover { display: none; }
.preview-style-7 .preview-title { color: #0ff; text-shadow: 0 0 6px rgba(0,255,255,0.4); }
.preview-style-7 .preview-summary { color: rgba(0,255,255,0.7); }
.preview-style-7 .preview-meta { color: rgba(0,255,255,0.5); }
.preview-style-7 .preview-cat { color: #f0f; }
/* Style 8: 渐变边框 */
.preview-style-8 .preview-cover { display: none; }
.preview-style-8 .preview-card {
  border: none;
  position: relative;
}
.preview-style-8 .preview-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 12px;
  padding: 2px;
  background: linear-gradient(135deg, #f09, #0ff, #f09);
  background-size: 200% 200%;
  animation: pv-magic 3s ease infinite;
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
}
.preview-style-8 .preview-card::after {
  content: '';
  position: absolute;
  inset: 2px;
  border-radius: 10px;
  background: #fff;
  z-index: -1;
}
/* Style 9: 教育 */
.preview-style-9 .preview-card {
  border-top-right-radius: 10px;
  border-bottom-right-radius: 10px;
  border-bottom-left-radius: 0;
  position: relative;
  overflow: hidden;
}
.preview-style-9 .preview-card::before {
  content: '';
  position: absolute;
  width: 60px; height: 60px;
  border-radius: 50%;
  background: #ffd861;
  top: 40%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 0;
}
.preview-style-9 .preview-cover { display: none; }
.preview-style-9 .preview-body { position: relative; z-index: 1; }
/* Style 10: 全息门票 */
.preview-style-10 .preview-card {
  background: conic-gradient(at 60% 50%, #ccc, #ff6bfe, #00f9f8, #ddd, #0081fd, #eef0bc, #0081fd, #ff6bfe, rgba(0,0,0,0.1), #0081fd, #ddd, #01fefb, #ccc);
  border: none;
}
.preview-style-10 .preview-cover { display: none; }
.preview-style-10 .preview-title { color: #fff; mix-blend-mode: difference; }
.preview-style-10 .preview-summary { color: rgba(0,0,0,0.7); }
/* Style 12: 立体 */
.preview-style-12 .preview-cover { display: none; }
.preview-style-12 .preview-card {
  background: linear-gradient(135deg, #38bdf8, #0284c7, #075985);
  border-color: rgba(255,255,255,0.2);
  transform: rotate3d(1, -1, 1, 8deg);
  box-shadow: 6px 6px 0 0 #0d0d0d;
}
.preview-style-12 .preview-title { color: #fff; }
.preview-style-12 .preview-summary { color: rgba(255,255,255,0.8); }
.preview-style-12 .preview-meta { color: rgba(255,255,255,0.6); }
.preview-style-12 .preview-cat { color: #ffd700; }
/* Style 13: 光晕 */
.preview-style-13 .preview-card {
  background: #171717;
  border-color: transparent;
  position: relative;
  overflow: hidden;
}
.preview-style-13 .preview-card::before {
  content: '';
  position: absolute;
  width: 40px; height: 200px;
  background: linear-gradient(#ff2288, #387ef0);
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  animation: pv-rot 3s linear infinite;
  opacity: 0.6;
}
.preview-style-13 .preview-card::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(23,23,23,0.8);
  backdrop-filter: blur(10px);
}
.preview-style-13 .preview-cover { display: none; }
.preview-style-13 .preview-body { position: relative; z-index: 1; }
.preview-style-13 .preview-title { color: #fff; }
.preview-style-13 .preview-summary { color: rgba(255,255,255,0.7); }
.preview-style-13 .preview-meta { color: rgba(255,255,255,0.5); }
.preview-style-13 .preview-cat { color: #ff2288; }
/* Style 14: 霓虹边框 */
.preview-style-14 .preview-card {
  background: linear-gradient(163deg, #00ff75, #3700ff);
  border: none;
  padding: 3px;
}
.preview-style-14 .preview-cover { display: none; }
.preview-style-14 .preview-body {
  background: #fff;
  border-radius: 9px;
}
/* Style 15: 渐变发光 */
.preview-style-15 .preview-card {
  background: linear-gradient(to left, #f7ba2b, #ea5358);
  border: none;
  padding: 4px;
  position: relative;
}
.preview-style-15 .preview-card::before {
  content: '';
  position: absolute;
  top: 10px;
  left: 0;
  right: 0;
  height: 100%;
  width: 100%;
  transform: scale(0.8);
  filter: blur(15px);
  background: linear-gradient(to left, #f7ba2b, #ea5358);
}
.preview-style-15 .preview-cover { display: none; }
.preview-style-15 .preview-body {
  background: #fff;
  border-radius: 8px;
}
/* Style 17: 魔法渐变 */
.style-17 .card-style-mini {
  background: linear-gradient(to right, #74ebd5 0%, #acb6e5 100%);
  border-radius: 6px;
  padding: 2px;
}
.style-17 .card-style-mini::after {
  content: '';
  display: block;
  width: 100%;
  height: 100%;
  background: #292b2c;
  border-radius: 4px;
}
.preview-style-17 .preview-cover { display: none; }
.preview-style-17 .preview-card {
  background: linear-gradient(to right, #74ebd5 0%, #acb6e5 100%);
  border: none;
  padding: 3px;
}
.preview-style-17 .preview-body {
  background: #292b2c;
  border-radius: 8px;
}
.preview-style-17 .preview-title { color: #fff; }
.preview-style-17 .preview-summary { color: rgba(255,255,255,0.8); }
.preview-style-17 .preview-meta { color: rgba(255,255,255,0.6); }
/* Style 18: 物理卡片 */
.style-18 .card-style-mini {
  background: #f8f9fa;
  border: 2px solid #222;
  border-radius: 6px;
  position: relative;
}
.style-18 .card-style-mini::before {
  content: '';
  position: absolute;
  top: -2px;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 8px;
  background: #222;
  border-radius: 0 0 4px 4px;
}
.preview-style-18 .preview-card {
  background: #f8f9fa;
  border: 3px solid #222;
  box-shadow: 5px 5px 2.5px 6px rgb(209, 218, 218);
}
/* Style 19: 3D翻转 */
.style-19 .card-style-mini {
  background: #151515;
  border-radius: 6px;
}
.style-19 .card-style-mini::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, #ffbb66, transparent);
  animation: pv-rot 3s linear infinite;
  animation-play-state: paused;
}
.style-19:hover .card-style-mini::after { animation-play-state: running; }
.preview-style-19 .preview-cover { display: none; }
.preview-style-19 .preview-card {
  background: #151515;
  border: none;
  box-shadow: 0px 0px 10px 1px rgba(0,0,0,0.93);
}
.preview-style-19 .preview-title { color: #fff; }
.preview-style-19 .preview-summary { color: rgba(255,255,255,0.85); }
.preview-style-19 .preview-meta { color: rgba(255,255,255,0.5); }
.preview-style-19 .preview-cat { color: #ffbb66; }
/* Style 20: 赛博3D */
.style-20 .card-style-mini {
  background: linear-gradient(45deg, #1a1a1a, #262626);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
}
.style-20 .card-style-mini::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(45deg, transparent 40%, rgba(92,103,255,0.15) 50%, transparent 60%);
}
.preview-style-20 .preview-cover { display: none; }
.preview-style-20 .preview-card {
  background: linear-gradient(45deg, #1a1a1a, #262626);
  border: 2px solid rgba(255,255,255,0.1);
}
.preview-style-20 .preview-title { color: #00ffaa; }
.preview-style-20 .preview-summary { color: rgba(255,255,255,0.7); }
.preview-style-20 .preview-meta { color: rgba(255,255,255,0.5); }
.preview-style-20 .preview-cat { color: #00ffaa; }
/* Style 21: 渐变光晕 */
.style-21 .card-style-mini {
  background: #000;
  border-radius: 6px;
  position: relative;
}
.style-21 .card-style-mini::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 8px;
  background: linear-gradient(-45deg, #e81cff, #40c9ff);
}
.style-21 .card-style-mini::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: 5px;
  background: #000;
}
.preview-style-21 .preview-cover { display: none; }
.preview-style-21 .preview-card {
  background: #000;
  border: none;
  position: relative;
}
.preview-style-21 .preview-card::before {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 15px;
  background: linear-gradient(-45deg, #e81cff, #40c9ff);
  z-index: -1;
}
.preview-style-21 .preview-title { color: #fff; }
.preview-style-21 .preview-summary { color: rgba(255,255,255,0.8); }
.preview-style-21 .preview-meta { color: rgba(255,255,255,0.6); }
.preview-style-21 .preview-cat { color: #e81cff; }
/* Style 22: 教育增强 */
.style-22 .card-style-mini {
  background: #fff;
  border-radius: 6px;
  border-bottom-left-radius: 0;
  position: relative;
  overflow: hidden;
}
.style-22 .card-style-mini::before {
  content: '';
  position: absolute;
  width: 30px; height: 30px;
  border-radius: 50%;
  background: #ffd861;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
}
.preview-style-22 .preview-card {
  background: #fff;
  border-top-right-radius: 10px;
  border-bottom-right-radius: 10px;
  border-bottom-left-radius: 0;
}
.preview-style-22 .preview-title { color: #4C5656; }
/* Style 23: 夜空 */
.style-23 .card-style-mini {
  background: linear-gradient(45deg, #000000, #0a0a2e);
  border-radius: 6px;
  position: relative;
}
.style-23 .card-style-mini::before {
  content: '';
  position: absolute;
  width: 16px; height: 16px;
  background: linear-gradient(145deg, #f0f0f0, #ffffff);
  border-radius: 50%;
  right: 8px; top: 6px;
  box-shadow: 0 0 10px rgba(235,235,235,0.5);
}
.preview-style-23 .preview-cover { display: none; }
.preview-style-23 .preview-card {
  background: linear-gradient(45deg, #000000, #0a0a2e);
  border: none;
  box-shadow: 0 0 20px rgba(0,0,255,0.1);
}
.preview-style-23 .preview-title { color: #e0e0e0; }
.preview-style-23 .preview-summary { color: #a0aec0; }
.preview-style-23 .preview-meta { color: #718096; }
/* Style 24: 增强门票 */
.style-24 .card-style-mini {
  background: linear-gradient(135deg, #ff6bfe, #00f9f8);
  border-radius: 6px;
  padding: 2px;
}
.style-24 .card-style-mini::after {
  content: '';
  display: block;
  width: 100%;
  height: 100%;
  background: #fff;
  border-radius: 4px;
}
.preview-style-24 .preview-card {
  background: transparent;
  border: none;
  position: relative;
  filter: drop-shadow(0 2px 1px rgba(0,0,0,0.15)) drop-shadow(0 4px 3px rgba(0,0,0,0.12));
}
.preview-style-24 .preview-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: #fff;
  border-radius: 12px;
  z-index: -1;
}
/* Style 25: 3D倾斜 */
.style-25 .card-style-mini {
  background: linear-gradient(to bottom right, #38bdf8, #0ea5e9, #0369a1);
  border-radius: 6px;
  transform: perspective(200px) rotate3d(1, -1, 1, 8deg);
}
.preview-style-25 .preview-card {
  background: linear-gradient(to bottom right, #38bdf8, #0ea5e9, #0369a1);
  border-color: #525252;
  transform: perspective(800px) rotate3d(1, -1, 1, 10deg);
  box-shadow: 8px 8px 0 0 #0d0d0d;
}
.preview-style-25 .preview-title { color: #fff; }
.preview-style-25 .preview-summary { color: rgba(255,255,255,0.8); }
.preview-style-25 .preview-meta { color: rgba(255,255,255,0.6); }
.preview-style-25 .preview-cat { color: #ffd700; }
.preview-style-25 .preview-cover { display: none; }
/* Style 26: 光条扫过 */
.style-26 .card-style-mini {
  background: #171717;
  border-radius: 8px;
}
.style-26 .card-style-mini::before {
  content: '';
  position: absolute;
  width: 20px; height: 100px;
  background: linear-gradient(#ff2288, #387ef0);
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0.6;
  animation: pv-rot 3s linear infinite;
  animation-play-state: paused;
}
.style-26:hover .card-style-mini::before { animation-play-state: running; }
.style-26 .card-style-mini::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(23,23,23,0.2);
  backdrop-filter: blur(10px);
}
.preview-style-26 .preview-card {
  background: #171717;
  border-color: transparent;
  border-radius: 20px;
}
.preview-style-26 .preview-title { color: #fff; }
.preview-style-26 .preview-summary { color: rgba(255,255,255,0.7); }
.preview-style-26 .preview-meta { color: rgba(255,255,255,0.5); }
.preview-style-26 .preview-cat { color: #ff2288; }
.preview-style-26 .preview-cover { display: none; }
/* Style 27: 霓虹缩放 */
.style-27 .card-style-mini {
  background: linear-gradient(163deg, #00ff75, #3700ff);
  border-radius: 8px;
  padding: 2px;
}
.style-27 .card-style-mini::after {
  content: '';
  display: block;
  width: 100%;
  height: 100%;
  background: #1a1a1a;
  border-radius: 6px;
}
.preview-style-27 .preview-card {
  background: linear-gradient(163deg, #00ff75, #3700ff);
  border: none;
  padding: 3px;
}
.preview-style-27 .preview-body {
  background: #1a1a1a;
  border-radius: 20px;
}
.preview-style-27 .preview-title { color: #fff; }
.preview-style-27 .preview-summary { color: rgba(255,255,255,0.8); }
.preview-style-27 .preview-cover { display: none; }
/* Style 28: 毛玻璃 */
.style-28 .card-style-mini {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
}
.preview-style-28 .preview-card {
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}
.preview-style-28 .preview-title { color: #fff; }
.preview-style-28 .preview-summary { color: rgba(255,255,255,0.75); }
.preview-style-28 .preview-meta { color: rgba(255,255,255,0.5); }
.preview-style-28 .preview-cover { display: none; }
/* Style 29: 极光 */
.style-29 .card-style-mini {
  background: linear-gradient(135deg, #00c6ff, #0072ff, #7b2ff7, #ff6ec7);
  background-size: 200% 200%;
  animation: auroraMini 3s ease infinite;
  border-radius: 8px;
}
@keyframes auroraMini { 0%,100% { background-position: 0% 50% } 50% { background-position: 100% 50% } }
.preview-style-29 .preview-card {
  background: linear-gradient(135deg, #0a2e38, #1a0533, #0a2e38);
  border: none;
  position: relative;
  overflow: hidden;
}
.preview-style-29 .preview-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(0,198,255,0.3), rgba(123,47,247,0.3), rgba(255,110,199,0.3));
  background-size: 200% 200%;
  animation: auroraMini 4s ease infinite;
  opacity: 0.6;
}
.preview-style-29 .preview-cover { display: none; }
.preview-style-29 .preview-body { position: relative; z-index: 1; }
.preview-style-29 .preview-title { color: #e0f0ff; }
.preview-style-29 .preview-summary { color: rgba(200,220,255,0.7); }
.preview-style-29 .preview-meta { color: rgba(200,220,255,0.5); }
/* Style 30: 像素风 */
.style-30 .card-style-mini {
  border: 3px solid #333;
  border-radius: 0;
  box-shadow: inset -2px -2px 0 #555, inset 2px 2px 0 #fff;
  background: #c0c0c0;
}
.preview-style-30 .preview-card {
  border: 4px solid #333;
  border-radius: 0;
  box-shadow: inset -3px -3px 0 #666, inset 3px 3px 0 #e0e0e0;
  background: #d4d4d4;
  image-rendering: pixelated;
}
.preview-style-30 .preview-title { font-family: 'Courier New', monospace; font-weight: 900; color: #222; }
.preview-style-30 .preview-summary { font-family: 'Courier New', monospace; color: #555; }
.preview-style-30 .preview-cover { display: none; }
/* Style 31: 水波纹 */
.style-31 .card-style-mini {
  background: #e8f4f8;
  border: 1px solid #b8d4e3;
  border-radius: 50%;
  position: relative;
  overflow: hidden;
}
.preview-style-31 .preview-card {
  background: linear-gradient(135deg, #e8f4f8, #d0e8f0);
  border: 1px solid #b8d4e3;
  position: relative;
  overflow: hidden;
}
.preview-style-31 .preview-title { color: #1a5276; }
.preview-style-31 .preview-summary { color: #2e86c1; }
.preview-style-31 .preview-cover { display: none; }
/* Style 32: 极简线条 */
.style-32 .card-style-mini {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 6px;
  position: relative;
}
.style-32 .card-style-mini::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: #333;
  border-radius: 6px 6px 0 0;
}
.preview-style-32 .preview-card {
  background: #fff;
  border: 1px solid #eee;
  position: relative;
}
.preview-style-32 .preview-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 4px;
  background: #333;
}
.preview-style-32 .preview-title { color: #222; }
.preview-style-32 .preview-summary { color: #666; }
.preview-style-32 .preview-cover { display: none; }
/* Style 33: 日落 */
.style-33 .card-style-mini {
  background: linear-gradient(180deg, #ff6b35, #f7931e, #ffd700);
  border-radius: 8px;
}
.preview-style-33 .preview-card {
  background: linear-gradient(180deg, #1a0a2e, #ff6b35, #f7931e, #ffd700);
  border: none;
}
.preview-style-33 .preview-cover { display: none; }
.preview-style-33 .preview-title { color: #fff; }
.preview-style-33 .preview-summary { color: rgba(255,255,255,0.8); }
.preview-style-33 .preview-meta { color: rgba(255,255,255,0.6); }
/* Style 34: 磨砂边缘 */
.style-34 .card-style-mini {
  background: #fff;
  border-radius: 8px;
  box-shadow: inset 0 0 10px rgba(0,0,0,0.05);
  position: relative;
  overflow: hidden;
}
.style-34 .card-style-mini::before {
  content: '';
  position: absolute;
  inset: 0;
  box-shadow: inset 0 0 20px rgba(200,200,200,0.5);
  border-radius: 8px;
}
.preview-style-34 .preview-card {
  background: #fafafa;
  border: 1px solid #e8e8e8;
  box-shadow: inset 0 0 30px rgba(200,200,200,0.3);
}
.preview-style-34 .preview-title { color: #333; }
.preview-style-34 .preview-summary { color: #666; }
.preview-style-34 .preview-cover { display: none; }
/* Style 35: 磁吸 */
.style-35 .card-style-mini {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 10px;
  transition: transform 0.2s;
}
.style-35:hover .card-style-mini { transform: scale(1.15); }
.preview-style-35 .preview-card {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  transition: transform 0.3s;
}
.preview-style-35:hover .preview-card { transform: scale(1.05); }
.preview-style-35 .preview-title { color: #fff; }
.preview-style-35 .preview-summary { color: rgba(255,255,255,0.8); }
.preview-style-35 .preview-meta { color: rgba(255,255,255,0.6); }
.preview-style-35 .preview-cover { display: none; }
/* Style 36: 折纸 */
.style-36 .card-style-mini {
  background: #f5f0e8;
  border: 1px solid #d4c5a9;
  border-radius: 2px;
  position: relative;
}
.style-36 .card-style-mini::after {
  content: '';
  position: absolute;
  bottom: 0; right: 0;
  width: 40%;
  height: 40%;
  background: linear-gradient(135deg, transparent 50%, #e8dcc8 50%);
  border-radius: 0 0 6px 0;
}
.preview-style-36 .preview-card {
  background: #f5f0e8;
  border: 1px solid #d4c5a9;
  position: relative;
}
.preview-style-36 .preview-card::after {
  content: '';
  position: absolute;
  bottom: 0; right: 0;
  width: 50px; height: 50px;
  background: linear-gradient(135deg, transparent 50%, #e8dcc8 50%);
  box-shadow: -2px -2px 4px rgba(0,0,0,0.1);
}
.preview-style-36 .preview-title { color: #4a3728; font-family: Georgia, serif; }
.preview-style-36 .preview-summary { color: #7a6a5a; }
.preview-style-36 .preview-cover { display: none; }
/* Style 37: 矩阵 */
.style-37 .card-style-mini {
  background: #000;
  border: 1px solid #0f0;
  border-radius: 4px;
  position: relative;
  overflow: hidden;
}
.style-37 .card-style-mini::before {
  content: '01';
  position: absolute;
  inset: 0;
  color: #0f0;
  font-size: 8px;
  font-family: monospace;
  opacity: 0.4;
  line-height: 1;
  overflow: hidden;
}
.preview-style-37 .preview-card {
  background: #0a0a0a;
  border: 1px solid rgba(0, 255, 0, 0.3);
  position: relative;
  overflow: hidden;
}
.preview-style-37 .preview-card::before {
  content: '01001 10110 01101 11010 00111 10100 01011';
  position: absolute;
  inset: 0;
  color: rgba(0, 255, 0, 0.15);
  font-size: 10px;
  font-family: monospace;
  line-height: 1.2;
  overflow: hidden;
  word-break: break-all;
}
.preview-style-37 .preview-cover { display: none; }
.preview-style-37 .preview-body { position: relative; z-index: 1; }
.preview-style-37 .preview-title { color: #0f0; text-shadow: 0 0 8px rgba(0,255,0,0.5); }
.preview-style-37 .preview-summary { color: rgba(0,255,0,0.6); }
.preview-style-37 .preview-meta { color: rgba(0,255,0,0.4); }
/* Style 38: 蒸汽波 */
.style-38 .card-style-mini {
  background: linear-gradient(180deg, #ff71ce, #01cdfe, #05ffa1);
  border-radius: 6px;
}
.preview-style-38 .preview-card {
  background: linear-gradient(180deg, #2d1b69, #ff71ce, #01cdfe);
  border: none;
  position: relative;
}
.preview-style-38 .preview-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(0deg, transparent, transparent 8px, rgba(255,255,255,0.05) 8px, rgba(255,255,255,0.05) 9px);
}
.preview-style-38 .preview-cover { display: none; }
.preview-style-38 .preview-body { position: relative; z-index: 1; }
.preview-style-38 .preview-title { color: #ff71ce; text-shadow: 0 0 10px rgba(255,113,206,0.5); }
.preview-style-38 .preview-summary { color: rgba(1,205,254,0.8); }
.preview-style-38 .preview-meta { color: rgba(5,255,161,0.6); }
/* Style 39: 电路板 */
.style-39 .card-style-mini {
  background: #1a472a;
  border: 2px solid #2d8b4e;
  border-radius: 4px;
  position: relative;
}
.style-39 .card-style-mini::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(45,139,78,0.3) 1px, transparent 1px), linear-gradient(0deg, rgba(45,139,78,0.3) 1px, transparent 1px);
  background-size: 6px 6px;
}
.preview-style-39 .preview-card {
  background: #0d2818;
  border: 2px solid #2d8b4e;
  position: relative;
  overflow: hidden;
}
.preview-style-39 .preview-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(45,139,78,0.2) 1px, transparent 1px), linear-gradient(0deg, rgba(45,139,78,0.2) 1px, transparent 1px);
  background-size: 12px 12px;
}
.preview-style-39 .preview-card::after {
  content: '';
  position: absolute;
  width: 6px; height: 6px;
  background: #4ade80;
  border-radius: 50%;
  top: 10px; right: 10px;
  box-shadow: 0 0 8px rgba(74,222,128,0.5);
}
.preview-style-39 .preview-cover { display: none; }
.preview-style-39 .preview-body { position: relative; z-index: 1; }
.preview-style-39 .preview-title { color: #4ade80; }
.preview-style-39 .preview-summary { color: rgba(74,222,128,0.7); }
.preview-style-39 .preview-meta { color: rgba(74,222,128,0.5); }
/* ===== AI Drawer ===== */
.ai-drawer-body {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 16px;
  overflow-x: hidden;
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
.ai-selected-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 6px;
  font-size: 12px;
  color: var(--el-color-primary);
}
.ai-selected-indicator .el-icon {
  font-size: 14px;
}
.category-tag {
  padding: 4px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s, box-shadow 0.2s;
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
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s, box-shadow 0.2s;
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
  opacity: 0.5;
  cursor: not-allowed;
}
.ai-preset-icon { font-size: 20px; }
.ai-preset-label { font-weight: 500; }
.manage-btn {
  border-style: dashed;
  opacity: 0.7;
}
.manage-btn:hover {
  opacity: 1;
}

.ai-continue-suggestions {
  padding: 8px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
}
.suggestions-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}
.suggestions-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.suggestion-btn {
  padding: 4px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-fill-color-blank);
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s, box-shadow 0.2s;
  font-size: 12px;
  color: var(--el-text-color-regular);
}
.suggestion-btn:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.ai-custom-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ai-prompt-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.ai-prompt-input:focus {
  border-color: var(--el-color-primary);
}
.ai-generate-btn {
  align-self: flex-end;
  padding: 8px 20px;
  background: var(--el-color-primary);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}
.ai-generate-btn:hover:not(:disabled) { background: var(--el-color-primary-dark-2); }
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
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.ai-cancel-btn {
  padding: 4px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: transparent;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
}
.ai-cancel-btn:hover { border-color: var(--el-color-danger); color: var(--el-color-danger); }

.ai-result-preview {
  flex: 1;
  min-height: 0;
  max-height: 400px;
  overflow-y: auto;
  overflow-x: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
  background: var(--el-fill-color-blank);
  position: relative;
  z-index: 0;
}
.ai-result-preview :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  white-space: pre;
  word-break: normal;
}
.ai-result-preview :deep(.md-preview) {
  overflow: hidden;
}

.ai-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.ai-action-btn {
  padding: 6px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: transparent;
  font-size: 12px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s, box-shadow 0.2s;
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
.ai-action-btn.primary:hover { background: var(--el-color-primary-dark-2); }

.ai-error {
  padding: 8px 12px;
  background: var(--el-color-error-light-9);
  border: 1px solid var(--el-color-error-light-7);
  border-radius: 6px;
  font-size: 12px;
  color: var(--el-color-error);
}

.ai-typing {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}
.ai-typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary);
  animation: ai-bounce 1.2s infinite;
}
.ai-typing span:nth-child(2) { animation-delay: 0.2s; }
.ai-typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes ai-bounce {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

.ai-shortcut-hint {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  white-space: nowrap;
}

/* Chat messages */
.ai-chat-msg {
  margin-bottom: 12px;
}
.ai-chat-msg.user .ai-chat-bubble {
  background: var(--el-color-primary-light-9);
  border-radius: 8px 8px 2px 8px;
  padding: 8px 12px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  max-height: 120px;
  overflow-y: auto;
}
.ai-chat-msg.assistant .ai-chat-bubble {
  padding: 0;
  overflow: hidden;
  position: relative;
  z-index: 0;
}
.ai-chat-msg.assistant .ai-chat-bubble :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  white-space: pre;
  word-break: normal;
}

/* Compare view */
.ai-compare {
  display: flex;
  gap: 8px;
  max-height: 400px;
}
.ai-compare-pane {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.ai-compare-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.ai-compare-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 8px;
  background: var(--el-fill-color-blank);
  font-size: 13px;
  position: relative;
  z-index: 0;
}
.ai-compare-content :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  white-space: pre;
  word-break: normal;
}
.ai-compare-content :deep(.md-preview) {
  overflow: hidden;
}

/* Model info */
.ai-model-info {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  text-align: right;
  padding: 2px 0;
}

/* Continue chat */
.ai-continue-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}

@media (max-width: 768px) {
  .article-edit-page {
    height: auto;
  }
  .editor-layout {
    flex-direction: column;
  }
  .editor-sidebar {
    width: 100%;
  }
}
</style>
