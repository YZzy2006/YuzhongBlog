<template>
  <div class="admin-page weather-config-page">
    <!-- Active provider bar -->
    <el-alert v-if="activeConfig" type="success" :closable="false" style="margin-bottom: 16px">
      <template #title>
        <span style="font-weight: 600">{{ $t('adminWeather.currentProvider') }}</span>
        {{ activeConfig.name }} — {{ activeConfig.provider }}
        <el-tag size="small" style="margin-left: 8px">{{ activeConfig.language }}</el-tag>
        <el-tag size="small" type="info" style="margin-left: 4px">{{ activeConfig.units }}</el-tag>
      </template>
    </el-alert>
    <el-alert v-else :title="$t('adminWeather.noProviderAlert')" type="info" :closable="false" style="margin-bottom: 16px" />

    <!-- Toolbar -->
    <div style="display: flex; gap: 8px; margin-bottom: 16px">
      <el-button type="primary" @click="showAddDialog">
        <el-icon><Plus /></el-icon> {{ $t('adminWeather.addProvider') }}
      </el-button>
      <el-button @click="loadConfigs" :loading="loading">
        <el-icon><Refresh /></el-icon> {{ $t('adminWeather.refresh') }}
      </el-button>
    </div>

    <!-- Config table -->
    <el-card>
      <el-table :data="configs" row-key="id" stripe v-loading="loading" :empty-text="$t('adminWeather.emptyTable')">
        <el-table-column prop="name" :label="$t('adminWeather.configName')" min-width="100" />
        <el-table-column prop="provider" :label="$t('adminWeather.provider')" width="130">
          <template #default="{ row }">
            <el-tag :type="providerTagType(row.provider)" effect="plain" size="small">{{ providerLabel(row.provider) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="apiFormat" :label="$t('adminWeather.apiFormat')" width="90">
          <template #default="{ row }">
            <span style="font-size: 12px; color: #666">{{ row.apiFormat || 'json' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="baseUrl" :label="$t('adminWeather.apiUrl')" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-size: 12px; color: #666">{{ row.baseUrl }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="language" :label="$t('adminWeather.language')" width="80" />
        <el-table-column prop="units" :label="$t('adminWeather.units')" width="80">
          <template #default="{ row }">
            <span>{{ row.units === 'f' ? '°F' : '°C' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="location" :label="$t('adminWeather.location')" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-size: 12px; color: #666">{{ row.location || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="isActive" :label="$t('adminWeather.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'" effect="plain" size="small">
              {{ row.isActive ? $t('adminWeather.activated') : $t('adminWeather.notActivated') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('adminWeather.action')" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="showEditDialog(row)">{{ $t('adminWeather.edit') }}</el-button>
            <el-button size="small" @click="handleTest(row)" :loading="testingId === row.id">{{ $t('adminWeather.test') }}</el-button>
            <el-button v-if="!row.isActive" type="success" size="small" @click="handleActivate(row)">{{ $t('adminWeather.activate') }}</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">{{ $t('adminWeather.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Test result dialog -->
    <el-dialog v-model="testResultVisible" :title="$t('adminWeather.testResult')" width="400px">
      <div style="display: flex; flex-direction: column; align-items: center; padding: 20px 0; gap: 12px">
        <div :style="{ width: '72px', height: '72px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '32px', fontWeight: 700, background: testResult?.success ? '#f0fdf4' : '#fef2f2', color: testResult?.success ? '#22c55e' : '#ef4444' }">
          {{ testResult?.success ? '✓' : '✗' }}
        </div>
        <div style="font-size: 16px; font-weight: 500">{{ testResult?.message }}</div>
        <div v-if="testResult?.latency > 0" style="font-size: 13px; color: #999">{{ $t('adminWeather.responseTime') }}: {{ testResult.latency }}ms</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="testResultVisible = false">{{ $t('adminWeather.confirm') }}</el-button>
      </template>
    </el-dialog>

    <!-- Add/Edit dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? $t('adminWeather.editConfig') : (selectedPreset ? $t('adminWeather.addConfig') : $t('adminWeather.selectProvider'))" :width="!isEdit && !selectedPreset ? '640px' : '560px'">
      <!-- Provider selection grid -->
      <template v-if="!isEdit && !selectedPreset">
        <div class="provider-grid">
          <div v-for="p in providerPresets" :key="p.id"
            class="provider-card"
            @click="selectProvider(p)">
            <span class="provider-letter" :style="{ background: p.color }">{{ p.letter }}</span>
            <div>
              <div class="provider-name">{{ p.name }}</div>
              <div class="provider-desc">{{ p.desc }}</div>
            </div>
          </div>
        </div>
      </template>

      <!-- Form -->
      <template v-if="isEdit || selectedPreset">
        <el-link v-if="!isEdit && selectedPreset" type="primary" :underline="false" style="margin-bottom: 16px" @click="selectedPreset = null">
          {{ $t('adminWeather.backToProviders') }}
        </el-link>
        <el-form :model="form" label-position="top">
          <el-form-item :label="$t('adminWeather.configName')" required>
            <el-input v-model="form.name" :placeholder="$t('adminWeather.configNamePlaceholder')" maxlength="100" />
          </el-form-item>
          <el-form-item :label="$t('adminWeather.apiKeyLabel')" required>
            <el-input v-model="form.apiKey" :type="showApiKey ? 'text' : 'password'" :placeholder="isEdit ? $t('adminWeather.apiKeyPlaceholderEdit') : $t('adminWeather.apiKeyPlaceholderAdd')" maxlength="200">
              <template #suffix>
                <el-icon style="cursor: pointer" @click="showApiKey = !showApiKey">
                  <View v-if="showApiKey" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item :label="$t('adminWeather.apiUrl')" required>
            <el-input v-model="form.baseUrl" placeholder="https://api.example.com/v7/" maxlength="500" />
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item :label="$t('adminWeather.language')">
                <el-select v-model="form.language" style="width: 100%">
                  <el-option :label="$t('adminWeather.languageZh')" value="zh" />
                  <el-option :label="$t('adminWeather.languageEn')" value="en" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="$t('adminWeather.units')">
                <el-select v-model="form.units" style="width: 100%">
                  <el-option :label="$t('adminWeather.unitsCelsius')" value="c" />
                  <el-option :label="$t('adminWeather.unitsFahrenheit')" value="f" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- Advanced toggle -->
          <el-divider content-position="left" style="cursor: pointer" @click="showAdvanced = !showAdvanced">
            <span style="font-size: 13px; color: #999">
              {{ $t('adminWeather.advancedOptions') }}
              <el-icon style="margin-left: 4px; transition: transform 0.2s" :style="{ transform: showAdvanced ? 'rotate(180deg)' : '' }"><ArrowDown /></el-icon>
            </span>
          </el-divider>

          <template v-if="showAdvanced">
            <el-form-item :label="$t('adminWeather.authField')">
              <el-select v-model="form.authType" style="width: 100%">
                <el-option :label="$t('adminWeather.authQueryParam')" value="query_param" />
                <el-option :label="$t('adminWeather.authHeader')" value="header" />
              </el-select>
            </el-form-item>
            <el-form-item :label="$t('adminWeather.location')">
              <el-input v-model="form.location" :placeholder="$t('adminWeather.locationPlaceholder')" maxlength="100" />
            </el-form-item>
            <el-form-item :label="$t('adminWeather.extraParams')">
              <el-input v-model="form.extraParams" type="textarea" :rows="3" :placeholder="$t('adminWeather.extraParamsPlaceholder')" maxlength="2000" />
            </el-form-item>
          </template>

          <el-form-item :label="$t('adminWeather.remark')">
            <el-input v-model="form.description" type="textarea" :rows="2" :placeholder="$t('adminWeather.remarkPlaceholder')" maxlength="500" />
          </el-form-item>
        </el-form>
        <el-alert v-if="validationErrors.length" :title="$t('adminWeather.validationPrefix') + validationErrors.join(locale === 'zh-CN' ? '、' : ', ')" type="error" show-icon :closable="false" style="margin-top: 12px" />
      </template>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('adminWeather.cancel') }}</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          {{ isEdit ? $t('adminWeather.save') : $t('adminWeather.create') }}
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

const form = reactive({
  name: '', provider: '', apiKey: '', baseUrl: '',
  apiFormat: 'json', authType: 'query_param',
  language: 'zh', units: 'c',
  location: '', extraParams: '', description: ''
})

const providerPresets = computed(() => [
  { id: 'qweather', name: t('adminWeather.providerQweatherName'), letter: '和', color: '#0091FF', desc: t('adminWeather.providerQweatherDesc'),
    preset: { baseUrl: 'https://devapi.qweather.com/v7/', language: 'zh', units: 'c' } },
  { id: 'openweathermap', name: t('adminWeather.providerOpenweathermapName'), letter: 'O', color: '#E67E22', desc: t('adminWeather.providerOpenweathermapDesc'),
    preset: { baseUrl: 'https://api.openweathermap.org/data/2.5/', language: 'en', units: 'c' } },
  { id: 'seniverse', name: t('adminWeather.providerSeniverseName'), letter: '心', color: '#2ECC71', desc: t('adminWeather.providerSeniverseDesc'),
    preset: { baseUrl: 'https://api.seniverse.com/v3/', language: 'zh', units: 'c' } },
  { id: 'custom', name: t('adminWeather.providerCustomName'), letter: '+', color: '#64748b', desc: t('adminWeather.providerCustomDesc'), preset: null }
])

const activeConfig = computed(() => configs.value.find(c => c.isActive) || null)
const validationErrors = ref([])

const providerLabelMap = computed(() => ({
  'qweather': t('adminWeather.providerQweatherName'),
  'openweathermap': t('adminWeather.providerOpenweathermapName'),
  'seniverse': t('adminWeather.providerSeniverseName'),
  'custom': t('adminWeather.providerCustomName')
}))

function providerLabel(provider) {
  return providerLabelMap.value[provider] || provider
}

function providerTagType(provider) {
  const map = { 'qweather': '', 'openweathermap': 'warning', 'seniverse': 'success', 'custom': 'info' }
  return map[provider] || 'info'
}

function validateForm() {
  const errors = []
  if (!form.name?.trim()) errors.push(t('adminWeather.fieldConfigName'))
  if (!form.provider) errors.push(t('adminWeather.fieldProvider'))
  if (!form.baseUrl?.trim()) errors.push(t('adminWeather.fieldApiUrl'))
  if (!isEdit.value && !form.apiKey?.trim()) errors.push(t('adminWeather.fieldApiKey'))
  validationErrors.value = errors
  return errors.length === 0
}

function resetForm() {
  Object.assign(form, {
    name: '', provider: '', apiKey: '', baseUrl: '',
    apiFormat: 'json', authType: 'query_param',
    language: 'zh', units: 'c',
    location: '', extraParams: '', description: ''
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
    name: config.name || '', provider: config.provider || '', apiKey: '',
    baseUrl: config.baseUrl || '',
    apiFormat: config.apiFormat || 'json', authType: config.authType || 'query_param',
    language: config.language || 'zh', units: config.units || 'c',
    location: config.location || '', extraParams: config.extraParams || '',
    description: config.description || ''
  })
  showAdvanced.value = form.authType !== 'query_param' || !!form.location || !!form.extraParams
  selectedPreset.value = { preset: {} }
  dialogVisible.value = true
}

function selectProvider(p) {
  selectedPreset.value = p
  form.provider = p.id
  if (p.preset) {
    form.name = p.name
    form.baseUrl = p.preset.baseUrl
    form.language = p.preset.language
    form.units = p.preset.units
  }
}

async function loadConfigs() {
  loading.value = true
  try {
    configs.value = await request.get('/admin/weather/configs')
  } catch (e) {
    console.error('Failed to load weather configs:', e)
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
      await request.put(`/admin/weather/configs/${editingId.value}`, body)
    } else {
      await request.post('/admin/weather/configs', body)
    }
    ElMessage.success(t('adminWeather.saveSuccess'))
    dialogVisible.value = false
    loadConfigs()
  } catch (e) {
    ElMessage.error(t('adminWeather.saveFailed', { error: e.message || t('adminWeather.unknownError') }))
  } finally {
    saving.value = false
  }
}

async function handleTest(config) {
  testingId.value = config.id
  testResult.value = null
  try {
    const data = await request.post(`/admin/weather/configs/${config.id}/test`)
    testResult.value = data
    testResultVisible.value = true
  } catch (e) {
    testResult.value = { success: false, message: t('adminWeather.testFailed', { error: e.message || t('adminWeather.unknownError') }) }
    testResultVisible.value = true
  } finally {
    testingId.value = null
  }
}

async function handleActivate(config) {
  try {
    await request.put(`/admin/weather/configs/${config.id}/activate`)
    ElMessage.success(t('adminWeather.activatedMsg'))
    loadConfigs()
  } catch (e) {
    ElMessage.error(t('adminWeather.activateFailed'))
  }
}

async function handleDelete(config) {
  try {
    await ElMessageBox.confirm(t('adminWeather.deleteConfirmMsg', { name: config.name }), t('adminWeather.deleteConfirmTitle'), { type: 'warning' })
    await request.delete(`/admin/weather/configs/${config.id}`)
    ElMessage.success(t('adminWeather.deleteSuccess'))
    loadConfigs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminWeather.deleteFailed'))
  }
}

onMounted(loadConfigs)
</script>

<style scoped>
.weather-config-page {
  /* fill available width in admin layout */
}

.provider-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.provider-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s;
}

.provider-card:hover {
  border-color: #409eff;
  background: #f0f9ff;
}

.provider-letter {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.provider-name {
  font-size: 14px;
  font-weight: 600;
}

.provider-desc {
  font-size: 12px;
  color: #999;
}
</style>
