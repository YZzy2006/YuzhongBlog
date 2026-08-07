<template>
  <div class="agent-writer">
    <!-- 初始化表单 -->
    <div v-if="!plan" class="agent-init">
      <el-form :model="initForm" label-position="top">
        <el-form-item :label="$t('ai.agent.topic')" required>
          <el-input v-model="initForm.topic" :placeholder="$t('ai.agent.topicPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('ai.agent.type')">
          <el-select v-model="initForm.type" style="width: 100%">
            <el-option :label="$t('ai.agent.typeArticle')" value="article" />
            <el-option :label="$t('ai.agent.typeTutorial')" value="tutorial" />
            <el-option :label="$t('ai.agent.typeGuide')" value="guide" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('ai.agent.length')">
          <el-select v-model="initForm.targetLength" style="width: 100%">
            <el-option :label="$t('ai.agent.lengthShort')" value="short" />
            <el-option :label="$t('ai.agent.lengthMedium')" value="medium" />
            <el-option :label="$t('ai.agent.lengthLong')" value="long" />
            <el-option :label="$t('ai.agent.lengthComprehensive')" value="comprehensive" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('ai.agent.options')">
          <el-checkbox v-model="initForm.includeCode">{{ $t('ai.agent.includeCode') }}</el-checkbox>
        </el-form-item>
        <el-button type="primary" @click="startAgent" :disabled="!initForm.topic.trim()">
          {{ $t('ai.agent.start') }}
        </el-button>
      </el-form>
    </div>

    <!-- Agent 执行界面 -->
    <div v-else class="agent-execution">
      <!-- 进度条 -->
      <div class="agent-progress">
        <div class="progress-header">
          <span class="progress-title">{{ $t('ai.agent.progress') }}</span>
          <span class="progress-count">{{ progress.completed }}/{{ progress.total }}</span>
        </div>
        <el-progress :percentage="progress.percentage" :status="progressStatus" />
        <div class="progress-step">{{ progress.currentStep }}</div>
      </div>

      <!-- 步骤列表 -->
      <div class="agent-steps">
        <div v-for="(step, index) in plan.steps" :key="step.id"
          class="step-item" :class="getStepClass(step, index)">
          <div class="step-header">
            <el-icon v-if="step.status === 'completed'" class="step-icon completed"><CircleCheck /></el-icon>
            <el-icon v-else-if="step.status === 'running'" class="step-icon running"><Loading /></el-icon>
            <el-icon v-else class="step-icon pending"><Clock /></el-icon>
            <span class="step-name">{{ step.name }}</span>
          </div>
          <div v-if="step.result" class="step-preview">
            {{ truncate(step.result, 100) }}
          </div>
        </div>
      </div>

      <!-- 控制按钮 -->
      <div class="agent-controls">
        <el-button v-if="plan.status === 'idle'" type="primary" @click="runNextStep">
          {{ $t('ai.agent.runStep') }}
        </el-button>
        <el-button v-if="plan.status === 'idle' || plan.status === 'completed'" @click="runAllSteps">
          {{ $t('ai.agent.runAll') }}
        </el-button>
        <el-button v-if="plan.status === 'running'" type="warning" @click="pauseAgent">
          {{ $t('ai.agent.pause') }}
        </el-button>
        <el-button v-if="plan.status === 'completed'" type="success" @click="exportResult">
          {{ $t('ai.agent.export') }}
        </el-button>
        <el-button @click="resetAgent">
          {{ $t('ai.agent.reset') }}
        </el-button>
      </div>

      <!-- 结果预览 -->
      <div v-if="finalResult" class="agent-result">
        <div class="result-header">
          <span>{{ $t('ai.agent.result') }}</span>
          <el-button size="small" text @click="copyResult">
            <el-icon><CopyDocument /></el-icon> {{ $t('ai.agent.copy') }}
          </el-button>
        </div>
        <div class="result-content" ref="resultContent">
          <MdPreview :modelValue="finalResult" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { CircleCheck, Loading, Clock, CopyDocument } from '@element-plus/icons-vue'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import {
  createWritingPlan,
  prepareStepPrompt,
  getAgentProgress,
  exportAgentResult,
  AGENT_STATUS,
} from '../../ai/agent/index'
import { aiEditorStream } from '../../utils/ai'

const { t } = useI18n()
const emit = defineEmits(['complete', 'update'])

const initForm = ref({
  topic: '',
  type: 'article',
  targetLength: 'medium',
  includeCode: false,
  language: 'zh',
})

const plan = ref(null)
const isRunning = ref(false)
const resultContent = ref(null)

const progress = computed(() => {
  if (!plan.value) return { completed: 0, total: 0, percentage: 0, currentStep: '' }
  return getAgentProgress(plan.value)
})

const progressStatus = computed(() => {
  if (plan.value?.status === AGENT_STATUS.COMPLETED) return 'success'
  if (plan.value?.status === AGENT_STATUS.ERROR) return 'exception'
  return ''
})

const finalResult = computed(() => {
  if (!plan.value) return ''
  const allCompleted = plan.value.steps.every(s => s.status === 'completed')
  if (!allCompleted) return ''
  return exportAgentResult(plan.value)
})

function startAgent() {
  plan.value = createWritingPlan(initForm.value.topic, {
    type: initForm.value.type,
    targetLength: initForm.value.targetLength,
    includeCode: initForm.value.includeCode,
    language: initForm.value.language,
  })
}

function getStepClass(step, index) {
  return {
    completed: step.status === 'completed',
    running: step.status === 'running',
    pending: step.status === 'pending',
    current: index === plan.value?.currentStep,
  }
}

function truncate(text, max) {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '...' : text
}

async function runNextStep() {
  if (!plan.value || isRunning.value) return

  const stepIndex = plan.value.currentStep
  if (stepIndex >= plan.value.steps.length) return

  const step = plan.value.steps[stepIndex]
  step.status = 'running'
  plan.value.status = AGENT_STATUS.WRITING
  isRunning.value = true

  try {
    const prompt = prepareStepPrompt(plan.value, stepIndex)
    const result = await callAi(prompt)
    step.result = result
    step.status = 'completed'
    plan.value.currentStep++

    if (plan.value.currentStep >= plan.value.steps.length) {
      plan.value.status = AGENT_STATUS.COMPLETED
      ElMessage.success(t('ai.agent.completed'))
      emit('complete', finalResult.value)
    } else {
      plan.value.status = AGENT_STATUS.IDLE
    }
  } catch (error) {
    step.status = 'error'
    plan.value.status = AGENT_STATUS.ERROR
    ElMessage.error(t('ai.agent.error', { error: error.message }))
  } finally {
    isRunning.value = false
    emit('update')
  }
}

async function runAllSteps() {
  if (!plan.value || isRunning.value) return

  isRunning.value = true
  plan.value.status = AGENT_STATUS.WRITING

  try {
    while (plan.value.currentStep < plan.value.steps.length) {
      const stepIndex = plan.value.currentStep
      const step = plan.value.steps[stepIndex]
      step.status = 'running'

      const prompt = prepareStepPrompt(plan.value, stepIndex)
      const result = await callAi(prompt)
      step.result = result
      step.status = 'completed'
      plan.value.currentStep++
      emit('update')
    }

    plan.value.status = AGENT_STATUS.COMPLETED
    ElMessage.success(t('ai.agent.completed'))
    emit('complete', finalResult.value)
  } catch (error) {
    plan.value.status = AGENT_STATUS.ERROR
    ElMessage.error(t('ai.agent.error', { error: error.message }))
  } finally {
    isRunning.value = false
  }
}

function pauseAgent() {
  isRunning.value = false
  plan.value.status = AGENT_STATUS.IDLE
}

function resetAgent() {
  plan.value = null
  isRunning.value = false
}

function copyResult() {
  if (finalResult.value) {
    navigator.clipboard.writeText(finalResult.value)
    ElMessage.success(t('ai.agent.copied'))
  }
}

function exportResult() {
  emit('complete', finalResult.value)
}

async function callAi(prompt) {
  return new Promise((resolve, reject) => {
    let result = ''
    const abort = aiEditorStream({
      messages: [{ role: 'user', content: prompt }],
      systemPrompt: '你是一个专业的技术博客写作助手，请根据要求生成高质量的内容。使用 Markdown 格式输出。',
      maxTokens: 4096,
    }, {
      onChunk(content) { result += content },
      onDone() { resolve(result) },
      onError(err) { reject(err) },
    })
    // Store abort function for cancellation
    if (plan.value) {
      plan.value._abortFn = abort
    }
  })
}

onUnmounted(() => {
  isRunning.value = false
})

defineExpose({ startAgent, resetAgent })
</script>

<style scoped>
.agent-writer {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.agent-init {
  padding: 8px 0;
}
.agent-execution {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.agent-progress {
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}
.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
.progress-title {
  font-size: 14px;
  font-weight: 500;
}
.progress-count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.progress-step {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 6px;
}
.agent-steps {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.step-item {
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  transition: all 0.2s;
}
.step-item.current {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.step-item.completed {
  border-color: var(--el-color-success);
  background: var(--el-color-success-light-9);
}
.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.step-icon {
  font-size: 16px;
}
.step-icon.completed {
  color: var(--el-color-success);
}
.step-icon.running {
  color: var(--el-color-primary);
  animation: spin 1s linear infinite;
}
.step-icon.pending {
  color: var(--el-text-color-secondary);
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.step-name {
  font-size: 13px;
  font-weight: 500;
}
.step-preview {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 6px;
  padding-left: 24px;
}
.agent-controls {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.agent-result {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}
.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  font-size: 14px;
  font-weight: 500;
}
.result-content {
  max-height: 400px;
  overflow-y: auto;
  padding: 12px;
}
</style>
