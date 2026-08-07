<template>
  <div class="knowledge-manager">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 站点信息 -->
      <el-tab-pane :label="$t('ai.knowledge.siteInfo')" name="site">
        <el-form :model="siteForm" label-position="top" class="kb-form">
          <el-form-item :label="$t('ai.knowledge.siteName')">
            <el-input v-model="siteForm.name" :placeholder="$t('ai.knowledge.siteNamePlaceholder')" />
          </el-form-item>
          <el-form-item :label="$t('ai.knowledge.siteDescription')">
            <el-input v-model="siteForm.description" type="textarea" :rows="2" :placeholder="$t('ai.knowledge.siteDescPlaceholder')" />
          </el-form-item>
          <el-form-item :label="$t('ai.knowledge.author')">
            <el-input v-model="siteForm.author" :placeholder="$t('ai.knowledge.authorPlaceholder')" />
          </el-form-item>
          <el-form-item :label="$t('ai.knowledge.topics')">
            <div class="topic-tags">
              <el-tag v-for="(topic, idx) in siteForm.topics" :key="idx" closable @close="removeTopic(idx)">{{ topic }}</el-tag>
            </div>
            <div class="topic-input">
              <el-input v-model="newTopic" :placeholder="$t('ai.knowledge.topicPlaceholder')" @keyup.enter="addTopic" style="width: 200px" />
              <el-button size="small" @click="addTopic" :disabled="!newTopic.trim()">{{ $t('ai.knowledge.add') }}</el-button>
            </div>
          </el-form-item>
          <el-button type="primary" @click="saveSiteInfo">{{ $t('ai.knowledge.save') }}</el-button>
        </el-form>
      </el-tab-pane>

      <!-- 写作风格 -->
      <el-tab-pane :label="$t('ai.knowledge.style')" name="style">
        <el-form :model="styleForm" label-position="top" class="kb-form">
          <el-form-item :label="$t('ai.knowledge.tone')">
            <el-select v-model="styleForm.tone" style="width: 100%">
              <el-option :label="$t('ai.knowledge.toneProfessional')" value="professional" />
              <el-option :label="$t('ai.knowledge.toneFriendly')" value="friendly" />
              <el-option :label="$t('ai.knowledge.toneCasual')" value="casual" />
              <el-option :label="$t('ai.knowledge.toneHumorous')" value="humorous" />
              <el-option :label="$t('ai.knowledge.toneAcademic')" value="academic" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('ai.knowledge.formality')">
            <el-radio-group v-model="styleForm.formality">
              <el-radio value="formal">{{ $t('ai.knowledge.formal') }}</el-radio>
              <el-radio value="semi-formal">{{ $t('ai.knowledge.semiFormal') }}</el-radio>
              <el-radio value="informal">{{ $t('ai.knowledge.informal') }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="$t('ai.knowledge.technicalLevel')">
            <el-select v-model="styleForm.technicalLevel" style="width: 100%">
              <el-option :label="$t('ai.knowledge.levelBeginner')" value="beginner" />
              <el-option :label="$t('ai.knowledge.levelIntermediate')" value="intermediate" />
              <el-option :label="$t('ai.knowledge.levelAdvanced')" value="advanced" />
              <el-option :label="$t('ai.knowledge.levelExpert')" value="expert" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('ai.knowledge.preferredLength')">
            <el-select v-model="styleForm.preferredLength" style="width: 100%">
              <el-option :label="$t('ai.knowledge.lengthShort')" value="short" />
              <el-option :label="$t('ai.knowledge.lengthMedium')" value="medium" />
              <el-option :label="$t('ai.knowledge.lengthLong')" value="long" />
              <el-option :label="$t('ai.knowledge.lengthComprehensive')" value="comprehensive" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('ai.knowledge.avoidWords')">
            <div class="topic-tags">
              <el-tag v-for="(word, idx) in styleForm.avoidWords" :key="idx" type="warning" closable @close="removeAvoidWord(idx)">{{ word }}</el-tag>
            </div>
            <div class="topic-input">
              <el-input v-model="newAvoidWord" :placeholder="$t('ai.knowledge.avoidWordPlaceholder')" @keyup.enter="addAvoidWord" style="width: 200px" />
              <el-button size="small" @click="addAvoidWord" :disabled="!newAvoidWord.trim()">{{ $t('ai.knowledge.add') }}</el-button>
            </div>
          </el-form-item>
          <el-button type="primary" @click="saveStyle">{{ $t('ai.knowledge.save') }}</el-button>
        </el-form>
      </el-tab-pane>

      <!-- 术语表 -->
      <el-tab-pane :label="$t('ai.knowledge.terminology')" name="terms">
        <div class="term-list">
          <div v-if="Object.keys(terminology).length === 0" class="empty-state">
            <p>{{ $t('ai.knowledge.noTerms') }}</p>
          </div>
          <div v-for="(data, term) in terminology" :key="term" class="term-item">
            <div class="term-content">
              <span class="term-name">{{ term }}</span>
              <span class="term-def">{{ data.definition }}</span>
            </div>
            <el-button size="small" text type="danger" @click="deleteTerm(term)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="term-input">
          <el-input v-model="newTerm" :placeholder="$t('ai.knowledge.termName')" style="width: 120px" />
          <el-input v-model="newTermDef" :placeholder="$t('ai.knowledge.termDefinition')" style="flex: 1" />
          <el-button type="primary" size="small" @click="addTermEntry" :disabled="!newTerm.trim() || !newTermDef.trim()">
            {{ $t('ai.knowledge.add') }}
          </el-button>
        </div>
      </el-tab-pane>

      <!-- 写作建议 -->
      <el-tab-pane :label="$t('ai.knowledge.patterns')" name="patterns">
        <div class="pattern-list">
          <div v-if="patterns.length === 0" class="empty-state">
            <p>{{ $t('ai.knowledge.noPatterns') }}</p>
          </div>
          <div v-for="(tip, idx) in patterns" :key="idx" class="pattern-item">
            <span class="pattern-content">{{ tip.content }}</span>
            <el-button size="small" text type="danger" @click="removePattern(idx)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="pattern-input">
          <el-input v-model="newPattern" :placeholder="$t('ai.knowledge.patternPlaceholder')" @keyup.enter="addPatternEntry" style="flex: 1" />
          <el-button type="primary" size="small" @click="addPatternEntry" :disabled="!newPattern.trim()">
            {{ $t('ai.knowledge.add') }}
          </el-button>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 导入导出 -->
    <div class="import-export">
      <el-button size="small" text @click="exportKb">
        <el-icon><Download /></el-icon> {{ $t('ai.knowledge.export') }}
      </el-button>
      <el-button size="small" text @click="triggerImport">
        <el-icon><Upload /></el-icon> {{ $t('ai.knowledge.import') }}
      </el-button>
      <input ref="importInput" type="file" accept=".json" style="display: none" @change="importKb" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { Delete, Download, Upload } from '@element-plus/icons-vue'
import {
  getKnowledgeBase,
  updateSiteInfo,
  updateStyle,
  addTerm,
  removeTerm,
  addPattern,
  exportKnowledgeBase,
  importKnowledgeBase,
} from '../../ai/knowledge/index'

const { t } = useI18n()
const emit = defineEmits(['update'])

const activeTab = ref('site')
const importInput = ref(null)

// 站点信息
const siteForm = reactive({
  name: '',
  description: '',
  author: '',
  topics: [],
})
const newTopic = ref('')

// 写作风格
const styleForm = reactive({
  tone: 'professional',
  formality: 'formal',
  technicalLevel: 'intermediate',
  preferredLength: 'medium',
  avoidWords: [],
})
const newAvoidWord = ref('')

// 术语表
const terminology = ref({})
const newTerm = ref('')
const newTermDef = ref('')

// 写作建议
const patterns = ref([])
const newPattern = ref('')

function loadKb() {
  const kb = getKnowledgeBase()
  Object.assign(siteForm, {
    name: kb.site.name,
    description: kb.site.description,
    author: kb.site.author,
    topics: [...kb.site.topics],
  })
  Object.assign(styleForm, {
    tone: kb.style.tone,
    formality: kb.style.formality,
    technicalLevel: kb.style.technicalLevel,
    preferredLength: kb.style.preferredLength,
    avoidWords: [...kb.style.avoidWords],
  })
  terminology.value = { ...kb.terminology }
  patterns.value = [...kb.patterns.writingTips]
}

function saveSiteInfo() {
  updateSiteInfo({ ...siteForm })
  ElMessage.success(t('ai.knowledge.saved'))
  emit('update')
}

function saveStyle() {
  updateStyle({ ...styleForm })
  ElMessage.success(t('ai.knowledge.saved'))
  emit('update')
}

function addTopic() {
  const topic = newTopic.value.trim()
  if (topic && !siteForm.topics.includes(topic)) {
    siteForm.topics.push(topic)
    newTopic.value = ''
  }
}

function removeTopic(idx) {
  siteForm.topics.splice(idx, 1)
}

function addAvoidWord() {
  const word = newAvoidWord.value.trim()
  if (word && !styleForm.avoidWords.includes(word)) {
    styleForm.avoidWords.push(word)
    newAvoidWord.value = ''
  }
}

function removeAvoidWord(idx) {
  styleForm.avoidWords.splice(idx, 1)
}

function addTermEntry() {
  const term = newTerm.value.trim()
  const def = newTermDef.value.trim()
  if (term && def) {
    addTerm(term, def)
    terminology.value = getKnowledgeBase().terminology
    newTerm.value = ''
    newTermDef.value = ''
    ElMessage.success(t('ai.knowledge.termAdded'))
    emit('update')
  }
}

function deleteTerm(term) {
  removeTerm(term)
  terminology.value = getKnowledgeBase().terminology
  ElMessage.success(t('ai.knowledge.termRemoved'))
  emit('update')
}

function addPatternEntry() {
  const content = newPattern.value.trim()
  if (content) {
    addPattern({ content })
    patterns.value = getKnowledgeBase().patterns.writingTips
    newPattern.value = ''
    ElMessage.success(t('ai.knowledge.patternAdded'))
    emit('update')
  }
}

function removePattern(idx) {
  const kb = getKnowledgeBase()
  kb.patterns.writingTips.splice(idx, 1)
  kb.lastUpdated = new Date().toISOString()
  localStorage.setItem('ai-knowledge-base', JSON.stringify(kb))
  patterns.value = kb.patterns.writingTips
  emit('update')
}

function exportKb() {
  const json = exportKnowledgeBase()
  const blob = new Blob([json], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'ai-knowledge-base.json'
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success(t('ai.knowledge.exported'))
}

function triggerImport() {
  importInput.value?.click()
}

function importKb(e) {
  const file = e.target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (event) => {
    const result = importKnowledgeBase(event.target.result)
    if (result.success) {
      ElMessage.success(t('ai.knowledge.imported'))
      loadKb()
      emit('update')
    } else {
      ElMessage.error(t('ai.knowledge.importFailed', { error: result.error }))
    }
  }
  reader.readAsText(file)
  e.target.value = ''
}

onMounted(loadKb)

defineExpose({ loadKb })
</script>

<style scoped>
.knowledge-manager {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.kb-form {
  padding: 8px 0;
}
.topic-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.topic-input {
  display: flex;
  gap: 8px;
  align-items: center;
}
.term-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 250px;
  overflow-y: auto;
  margin-bottom: 12px;
}
.empty-state {
  text-align: center;
  padding: 16px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.term-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
}
.term-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.term-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-color-primary);
}
.term-def {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.term-input {
  display: flex;
  gap: 8px;
  align-items: center;
}
.pattern-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 250px;
  overflow-y: auto;
  margin-bottom: 12px;
}
.pattern-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
}
.pattern-content {
  font-size: 13px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pattern-input {
  display: flex;
  gap: 8px;
  align-items: center;
}
.import-export {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
