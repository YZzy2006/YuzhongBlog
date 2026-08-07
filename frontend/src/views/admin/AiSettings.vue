<template>
  <div class="admin-page ai-config-page">
    <!-- Active model bar -->
    <el-alert v-if="activeConfig" type="success" :closable="false" style="margin-bottom: 16px">
      <template #title>
        <span style="font-weight: 600">{{ $t('adminAiSettings.currentModel') }}</span>
        {{ activeConfig.name }} — {{ activeConfig.model }}
        <el-tag size="small" style="margin-left: 8px">{{ formatLabel(apiFormatOptions, activeConfig.apiFormat) }}</el-tag>
        <el-tag size="small" type="info" style="margin-left: 4px">{{ formatLabel(authTypeOptions, activeConfig.authType) }}</el-tag>
      </template>
    </el-alert>
    <el-alert v-else :title="$t('adminAiSettings.noModelAlert')" type="info" :closable="false" style="margin-bottom: 16px" />

    <!-- Toolbar -->
    <div style="display: flex; gap: 8px; margin-bottom: 16px">
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon> {{ $t('adminAiSettings.configModel') }}
      </el-button>
      <el-button @click="loadConfigs" :loading="loading">
        <el-icon><Refresh /></el-icon> {{ $t('adminAiSettings.refresh') }}
      </el-button>
    </div>

    <!-- Config table -->
    <el-card>
      <el-table :data="configs" row-key="id" stripe v-loading="loading" :empty-text="$t('adminAiSettings.emptyTable')">
        <el-table-column prop="name" :label="$t('adminAiSettings.configName')" min-width="90" />
        <el-table-column prop="model" :label="$t('adminAiSettings.model')" width="150" />
        <el-table-column prop="apiFormat" :label="$t('adminAiSettings.apiFormat')" width="190">
          <template #default="{ row }">
            <el-tag effect="plain" size="small">{{ formatLabel(apiFormatOptions, row.apiFormat) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="authType" :label="$t('adminAiSettings.authType')" width="120">
          <template #default="{ row }">
            <span style="font-size: 12px; color: #666">{{ formatLabel(authTypeOptions, row.authType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="baseUrl" :label="$t('adminAiSettings.apiUrl')" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-size: 12px; color: #666">{{ row.baseUrl }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="apiKey" label="API Key" width="160">
          <template #default="{ row }">
            <code>{{ maskApiKey(row.apiKey) }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="$t('adminAiSettings.balance')" width="100">
          <template #default="{ row }">
            <template v-if="balanceMap[row.id]">
              <span v-if="balanceMap[row.id].success && balanceMap[row.id].planType === 'subscription'" style="color: #0ea5e9; font-size: 13px">
                {{ balanceMap[row.id].planName || $t('adminAiSettings.subscription') }}
              </span>
              <span v-else-if="balanceMap[row.id].success" :style="{ color: balanceMap[row.id].isAvailable ? '#22c55e' : '#ef4444', fontSize: '13px' }">
                {{ balanceMap[row.id].currency === 'USD' ? '$' : '¥' }}{{ balanceMap[row.id].balance }}
              </span>
              <span v-else style="font-size: 12px; color: #999; cursor: help" :title="balanceMap[row.id].message">{{ $t('adminAiSettings.queryFailed') }}</span>
            </template>
            <span v-else style="font-size: 12px; color: #999">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="isActive" :label="$t('adminAiSettings.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'" effect="plain" size="small">
              {{ row.isActive ? $t('adminAiSettings.activated') : $t('adminAiSettings.notActivated') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('adminAiSettings.action')" width="340" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="showEditDialog(row)">{{ $t('adminAiSettings.edit') }}</el-button>
            <el-button size="small" @click="handleTest(row)" :loading="testingId === row.id">{{ $t('adminAiSettings.test') }}</el-button>
            <el-button size="small" @click="handleBalanceCheck(row)" :loading="balanceCheckingId === row.id">{{ $t('adminAiSettings.checkBalance') }}</el-button>
            <el-button v-if="!row.isActive" type="success" size="small" @click="handleActivate(row)">{{ $t('adminAiSettings.activate') }}</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">{{ $t('adminAiSettings.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Test result dialog -->
    <el-dialog v-model="testResultVisible" :title="$t('adminAiSettings.testResult')" width="400px">
      <div style="display: flex; flex-direction: column; align-items: center; padding: 20px 0; gap: 12px">
        <div :style="{ width: '72px', height: '72px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '32px', fontWeight: 700, background: testResult?.success ? '#f0fdf4' : '#fef2f2', color: testResult?.success ? '#22c55e' : '#ef4444' }">
          {{ testResult?.success ? '✓' : '✗' }}
        </div>
        <div style="font-size: 16px; font-weight: 500">{{ testResult?.message }}</div>
        <div v-if="testResult?.latency > 0" style="font-size: 13px; color: #999">{{ $t('adminAiSettings.responseTime') }}: {{ testResult.latency }}ms</div>
        <div v-if="testResult?.model" style="font-size: 13px; color: #999">{{ $t('adminAiSettings.serverModel') }}: {{ testResult.model }}</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="testResultVisible = false">{{ $t('adminAiSettings.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- Add/Edit dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? $t('adminAiSettings.editConfig') : (selectedPreset ? $t('adminAiSettings.addConfig') : $t('adminAiSettings.selectProvider'))" :width="!isEdit && !selectedPreset ? '640px' : '560px'">
      <!-- Provider selection grid -->
      <template v-if="!isEdit && !selectedPreset">
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px">
          <div v-for="p in providerPresets" :key="p.id"
            class="provider-card"
            @click="selectProvider(p)">
            <span :style="{ width: '40px', height: '40px', borderRadius: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '18px', fontWeight: 700, color: '#fff', background: p.color, flexShrink: 0 }">{{ p.letter }}</span>
            <div>
              <div style="font-size: 14px; font-weight: 600">{{ p.name }}</div>
              <div style="font-size: 12px; color: #999">{{ p.desc }}</div>
            </div>
          </div>
        </div>
      </template>

      <!-- Form -->
      <template v-if="isEdit || selectedPreset">
        <el-link v-if="!isEdit && selectedPreset" type="primary" :underline="false" style="margin-bottom: 16px" @click="selectedPreset = null">
          {{ $t('adminAiSettings.backToProviders') }}
        </el-link>
        <el-form :model="form" label-position="top">
          <el-form-item :label="$t('adminAiSettings.configName')" required>
            <el-input v-model="form.name" :placeholder="$t('adminAiSettings.configNamePlaceholder')" maxlength="100" />
          </el-form-item>
          <el-form-item label="API Key" required>
            <el-input v-model="form.apiKey" :type="showApiKey ? 'text' : 'password'" :placeholder="isEdit ? $t('adminAiSettings.apiKeyPlaceholderEdit') : $t('adminAiSettings.apiKeyPlaceholderAdd')" maxlength="200">
              <template #suffix>
                <el-icon style="cursor: pointer" @click="showApiKey = !showApiKey">
                  <View v-if="showApiKey" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item :label="$t('adminAiSettings.apiUrl')" required>
            <el-input v-model="form.baseUrl" placeholder="https://api.example.com/v1" maxlength="500" />
          </el-form-item>
          <el-form-item :label="$t('adminAiSettings.modelName')" required>
            <el-input v-model="form.model" placeholder="deepseek-chat" maxlength="100" />
          </el-form-item>

          <!-- Advanced toggle -->
          <el-divider content-position="left" style="cursor: pointer" @click="showAdvanced = !showAdvanced">
            <span style="font-size: 13px; color: #999">
              {{ $t('adminAiSettings.advancedOptions') }}
              <el-icon style="margin-left: 4px; transition: transform 0.2s" :style="{ transform: showAdvanced ? 'rotate(180deg)' : '' }"><ArrowDown /></el-icon>
            </span>
          </el-divider>

          <template v-if="showAdvanced">
            <el-form-item :label="$t('adminAiSettings.apiFormat')">
              <el-select v-model="form.apiFormat" style="width: 100%">
                <el-option v-for="opt in apiFormatOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
              <div style="font-size: 12px; color: #999; margin-top: 4px">{{ apiFormatDescMap[form.apiFormat] || '' }}</div>
            </el-form-item>
            <el-form-item :label="$t('adminAiSettings.authField')">
              <el-select v-model="form.authType" style="width: 100%">
                <el-option v-for="opt in authTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
              <div style="font-size: 12px; color: #999; margin-top: 4px">{{ authTypeDescMap[form.authType] || '' }}</div>
            </el-form-item>
          </template>

          <el-form-item label="Max Tokens">
            <el-input-number v-model="form.maxTokens" :min="1" :max="128000" :step="512" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="Temperature">
            <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input />
          </el-form-item>
          <el-form-item :label="$t('adminAiSettings.websiteUrl')">
            <el-input v-model="form.websiteUrl" placeholder="https://platform.deepseek.com" maxlength="500" />
          </el-form-item>
          <el-form-item :label="$t('adminAiSettings.balanceQuery')">
            <el-input v-model="form.balanceUrl" :placeholder="$t('adminAiSettings.balanceQueryPlaceholder')" maxlength="500" />
            <div style="font-size: 12px; color: #999; margin-top: 4px">{{ $t('adminAiSettings.balanceQueryHint') }}</div>
          </el-form-item>
          <el-form-item :label="$t('adminAiSettings.scriptLabel')">
            <el-input v-model="form.balanceScript" type="textarea" :rows="6" :placeholder="$t('adminAiSettings.scriptPlaceholder')" maxlength="5000" />
          </el-form-item>
          <el-form-item :label="$t('adminAiSettings.remark')">
            <el-input v-model="form.description" type="textarea" :rows="2" :placeholder="$t('adminAiSettings.remarkPlaceholder')" maxlength="500" />
          </el-form-item>
        </el-form>
        <el-alert v-if="validationErrors.length" :title="$t('adminAiSettings.validationPrefix') + validationErrors.join(locale === 'zh-CN' ? '、' : ', ')" type="error" show-icon :closable="false" style="margin-top: 12px" />
      </template>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('adminAiSettings.cancel') }}</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          {{ isEdit ? $t('adminAiSettings.save') : $t('adminAiSettings.create') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { Plus, Refresh, View, Hide, ArrowDown } from '@element-plus/icons-vue'
import request from '../../utils/request'

const { t, locale } = useI18n()

const configs = ref([])
const loading = ref(false)
const saving = ref(false)
const testingId = ref(null)
const testResult = ref(null)
const testResultVisible = ref(false)
const showApiKey = ref(false)
const showAdvanced = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const selectedPreset = ref(null)
const balanceCheckingId = ref(null)
const balanceMap = ref({})

const form = reactive({
  name: '', apiKey: '', baseUrl: '', model: '',
  maxTokens: 4096, temperature: 0.7,
  apiFormat: 'OPENAI', authType: 'BEARER',
  websiteUrl: '', balanceUrl: '', balanceScript: '',
  description: '', sortOrder: 0
})

const apiFormatOptions = [
  { value: 'OPENAI', label: 'OpenAI Chat Completions' },
  { value: 'ANTHROPIC', label: 'Anthropic Messages (native)' }
]
const authTypeOptions = computed(() => [
  { value: 'BEARER', label: t('adminAiSettings.bearerDefault') },
  { value: 'X_API_KEY', label: 'x-api-key' },
  { value: 'RAW_TOKEN', label: t('adminAiSettings.rawTokenLabel') },
  { value: 'API_KEY', label: 'api-key (Azure)' }
])

const authTypeDescMap = computed(() => ({
  'BEARER': t('adminAiSettings.authBearerDesc'),
  'X_API_KEY': t('adminAiSettings.authXApiKeyDesc'),
  'RAW_TOKEN': t('adminAiSettings.authRawTokenDesc'),
  'API_KEY': t('adminAiSettings.authApiKeyDesc')
}))
const apiFormatDescMap = computed(() => ({
  'OPENAI': t('adminAiSettings.apiFormatOpenaiDesc'),
  'ANTHROPIC': t('adminAiSettings.apiFormatAnthropicDesc')
}))

const providerPresets = computed(() => [
  { id: 'deepseek', name: 'DeepSeek', letter: 'D', color: '#4D6BFE', desc: t('adminAiSettings.providerDeepseekDesc'),
    preset: { baseUrl: 'https://api.deepseek.com', model: 'deepseek-chat', websiteUrl: 'https://platform.deepseek.com', apiFormat: 'OPENAI', authType: 'BEARER' } },
  { id: 'kimi', name: 'Kimi / Moonshot', letter: 'M', color: '#000000', desc: t('adminAiSettings.providerKimiDesc'),
    preset: { baseUrl: 'https://api.moonshot.cn', model: 'moonshot-v1-8k', websiteUrl: 'https://platform.moonshot.cn', apiFormat: 'OPENAI', authType: 'BEARER' } },
  { id: 'qwen', name: t('adminAiSettings.providerQwenName'), letter: 'Q', color: '#615CED', desc: t('adminAiSettings.providerQwenDesc'),
    preset: { baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode', model: 'qwen-plus', websiteUrl: 'https://dashscope.console.aliyun.com', apiFormat: 'OPENAI', authType: 'BEARER' } },
  { id: 'openai', name: 'OpenAI', letter: 'O', color: '#10A37F', desc: t('adminAiSettings.providerOpenaiDesc'),
    preset: { baseUrl: 'https://api.openai.com', model: 'gpt-4o', websiteUrl: 'https://platform.openai.com', apiFormat: 'OPENAI', authType: 'BEARER' } },
  { id: 'anthropic', name: 'Anthropic', letter: 'A', color: '#D97706', desc: t('adminAiSettings.providerAnthropicDesc'),
    preset: { baseUrl: 'https://api.anthropic.com', model: 'claude-sonnet-4-20250514', websiteUrl: 'https://console.anthropic.com', apiFormat: 'ANTHROPIC', authType: 'X_API_KEY' } },
  { id: 'zhipu', name: t('adminAiSettings.providerZhipuName'), letter: 'Z', color: '#3265FF', desc: t('adminAiSettings.providerZhipuDesc'),
    preset: { baseUrl: 'https://open.bigmodel.cn/api/paas', model: 'glm-4-flash', websiteUrl: 'https://open.bigmodel.cn', apiFormat: 'OPENAI', authType: 'BEARER' } },
  { id: 'custom', name: t('adminAiSettings.providerCustomName'), letter: '+', color: '#64748b', desc: t('adminAiSettings.providerCustomDesc'), preset: null }
])

const activeConfig = computed(() => configs.value.find(c => c.isActive) || null)
const validationErrors = ref([])

function validateForm() {
  const errors = []
  if (!form.name?.trim()) errors.push(t('adminAiSettings.fieldConfigName'))
  if (!form.baseUrl?.trim()) errors.push(t('adminAiSettings.fieldApiUrl'))
  if (!form.model?.trim()) errors.push(t('adminAiSettings.fieldModelName'))
  if (!isEdit.value && !form.apiKey?.trim()) errors.push(t('adminAiSettings.fieldApiKey'))
  validationErrors.value = errors
  return errors.length === 0
}

function formatLabel(options, value) {
  const found = options.find(o => o.value === value)
  return found ? found.label : value
}

function maskApiKey(key) {
  if (!key) return '-'
  if (key.length <= 8) return '****'
  return key.slice(0, 4) + '****' + key.slice(-4)
}

function resetForm() {
  Object.assign(form, {
    name: '', apiKey: '', baseUrl: '', model: '',
    maxTokens: 4096, temperature: 0.7,
    apiFormat: 'OPENAI', authType: 'BEARER',
    websiteUrl: '', balanceUrl: '', balanceScript: '',
    description: '', sortOrder: 0
  })
  showApiKey.value = false
  showAdvanced.value = false
  selectedPreset.value = null
}

function showAddDialog() {
  resetForm()
  validationErrors.value = []
  isEdit.value = false
  editingId.value = null
  dialogVisible.value = true
}

function showEditDialog(config) {
  isEdit.value = true
  editingId.value = config.id
  showApiKey.value = false
  validationErrors.value = []
  Object.assign(form, {
    name: config.name || '', apiKey: '',
    baseUrl: config.baseUrl || '', model: config.model || '',
    maxTokens: config.maxTokens || 4096, temperature: config.temperature ?? 0.7,
    apiFormat: config.apiFormat || 'OPENAI', authType: config.authType || 'BEARER',
    websiteUrl: config.websiteUrl || '', balanceUrl: config.balanceUrl || '',
    balanceScript: config.balanceScript || '', description: config.description || '',
    sortOrder: config.sortOrder || 0
  })
  showAdvanced.value = form.apiFormat !== 'OPENAI' || form.authType !== 'BEARER' || !!form.balanceScript
  selectedPreset.value = { preset: {} }
  dialogVisible.value = true
}

function selectProvider(p) {
  selectedPreset.value = p
  if (p.preset) {
    form.name = p.name
    form.baseUrl = p.preset.baseUrl
    form.model = p.preset.model
    form.websiteUrl = p.preset.websiteUrl || ''
    form.apiFormat = p.preset.apiFormat || 'OPENAI'
    form.authType = p.preset.authType || 'BEARER'
    showAdvanced.value = form.apiFormat !== 'OPENAI' || form.authType !== 'BEARER'
  }
}

async function loadConfigs() {
  loading.value = true
  try {
    configs.value = await request.get('/admin/ai/configs')
  } catch (e) {
    console.error('Failed to load AI configs:', e)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!validateForm()) return
  saving.value = true
  try {
    const body = { ...form }
    if (body.apiKey?.startsWith('****')) body.apiKey = ''
    if (isEdit.value) {
      await request.put(`/admin/ai/configs/${editingId.value}`, body)
    } else {
      await request.post('/admin/ai/configs', body)
    }
    ElMessage.success(t('adminAiSettings.saveSuccess'))
    dialogVisible.value = false
    loadConfigs()
  } catch (e) {
    ElMessage.error(t('adminAiSettings.saveFailed', { error: e.message || t('adminAiSettings.unknownError') }))
  } finally {
    saving.value = false
  }
}

async function handleTest(config) {
  testingId.value = config.id
  testResult.value = null
  try {
    const data = await request.post(`/admin/ai/configs/${config.id}/test`)
    testResult.value = data
    testResultVisible.value = true
  } catch (e) {
    testResult.value = { success: false, message: t('adminAiSettings.testFailed', { error: e.message || t('adminAiSettings.unknownError') }) }
    testResultVisible.value = true
  } finally {
    testingId.value = null
  }
}

async function handleBalanceCheck(config) {
  balanceCheckingId.value = config.id
  try {
    const data = await request.get(`/admin/ai/configs/${config.id}/balance`)
    balanceMap.value[config.id] = data
    if (!data.success) ElMessage.warning(data.message || t('adminAiSettings.balanceQueryFailedMsg'))
  } catch (e) {
    balanceMap.value[config.id] = { success: false, message: t('adminAiSettings.queryFailedMsg') }
    ElMessage.error(t('adminAiSettings.balanceQueryFailedMsg'))
  } finally {
    balanceCheckingId.value = null
  }
}

async function handleActivate(config) {
  try {
    await request.put(`/admin/ai/configs/${config.id}/activate`)
    ElMessage.success(t('adminAiSettings.activatedMsg'))
    loadConfigs()
  } catch (e) {
    ElMessage.error(t('adminAiSettings.activateFailed'))
  }
}

async function handleDelete(config) {
  try {
    await ElMessageBox.confirm(t('adminAiSettings.deleteConfirmMsg', { name: config.name }), t('adminAiSettings.deleteConfirmTitle'), { type: 'warning' })
    await request.delete(`/admin/ai/configs/${config.id}`)
    ElMessage.success(t('adminAiSettings.deleteSuccess'))
    loadConfigs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminAiSettings.deleteFailed'))
  }
}

onMounted(loadConfigs)
</script>

<style scoped>
.ai-config-page {
  /* fill available width in admin layout */
}
.provider-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.provider-card:hover {
  border-color: #409eff;
  background: #f0f9ff;
}
</style>
