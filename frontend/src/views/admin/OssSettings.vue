<template>
  <div class="admin-page oss-config-page">
    <!-- Status bar -->
    <el-alert v-if="isConfigured" type="success" :closable="false" style="margin-bottom: 16px">
      <template #title>
        <span style="font-weight: 600">{{ $t('adminOss.currentConfig') }}</span>
        {{ form.endpoint || '-' }} / {{ form.bucketName || '-' }}
      </template>
    </el-alert>
    <el-alert v-else :title="$t('adminOss.notConfigured')" type="warning" :closable="false" style="margin-bottom: 16px" />

    <!-- Config form -->
    <el-card>
      <el-form :model="form" label-position="top" v-loading="loading">
        <el-form-item :label="$t('adminOss.endpoint')" required>
          <el-input v-model="form.endpoint" placeholder="oss-cn-guangzhou.aliyuncs.com" maxlength="200" />
          <div style="font-size: 12px; color: #999; margin-top: 4px">{{ $t('adminOss.endpointHint') }}</div>
        </el-form-item>

        <el-form-item :label="$t('adminOss.bucketName')" required>
          <el-input v-model="form.bucketName" placeholder="my-bucket" maxlength="100" />
        </el-form-item>

        <el-form-item :label="$t('adminOss.accessKeyId')" required>
          <el-input v-model="form.accessKeyId" placeholder="LTAI5t..." maxlength="100" />
        </el-form-item>

        <el-form-item :label="$t('adminOss.accessKeySecret')" required>
          <el-input v-model="form.accessKeySecret" :type="showSecret ? 'text' : 'password'"
            :placeholder="$t('adminOss.accessKeySecretPlaceholder')" maxlength="200">
            <template #suffix>
              <el-icon style="cursor: pointer" @click="showSecret = !showSecret">
                <View v-if="showSecret" />
                <Hide v-else />
              </el-icon>
            </template>
          </el-input>
          <div style="font-size: 12px; color: #999; margin-top: 4px">{{ $t('adminOss.accessKeySecretHint') }}</div>
        </el-form-item>

        <el-form-item :label="$t('adminOss.customDomain')">
          <el-input v-model="form.customDomain" placeholder="https://cdn.example.com" maxlength="300" />
          <div style="font-size: 12px; color: #999; margin-top: 4px">{{ $t('adminOss.customDomainHint') }}</div>
        </el-form-item>

        <!-- Actions -->
        <el-form-item>
          <div style="display: flex; gap: 8px">
            <el-button type="primary" @click="handleSave" :loading="saving">{{ $t('adminOss.save') }}</el-button>
            <el-button @click="handleTest" :loading="testing">{{ $t('adminOss.testConnection') }}</el-button>
            <el-button @click="loadConfig">{{ $t('adminOss.refresh') }}</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Test result dialog -->
    <el-dialog v-model="testResultVisible" :title="$t('adminOss.testResult')" width="400px">
      <div style="display: flex; flex-direction: column; align-items: center; padding: 20px 0; gap: 12px">
        <div :style="{ width: '72px', height: '72px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '32px', fontWeight: 700, background: testResult?.success ? '#f0fdf4' : '#fef2f2', color: testResult?.success ? '#22c55e' : '#ef4444' }">
          {{ testResult?.success ? '✓' : '✗' }}
        </div>
        <div style="font-size: 16px; font-weight: 500">{{ testResult?.message }}</div>
        <div v-if="testResult?.bucketName" style="font-size: 13px; color: #999">{{ $t('adminOss.bucket') }}: {{ testResult.bucketName }}</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="testResultVisible = false">{{ $t('adminOss.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { View, Hide } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const showSecret = ref(false)
const testResultVisible = ref(false)
const testResult = ref(null)

const form = reactive({
  endpoint: '',
  bucketName: '',
  accessKeyId: '',
  accessKeySecret: '',
  customDomain: ''
})

const isConfigured = computed(() => {
  return form.endpoint && form.bucketName && form.accessKeyId
})

async function loadConfig() {
  loading.value = true
  try {
    const data = await request.get('/admin/oss/config')
    if (data) {
      form.endpoint = data.endpoint || ''
      form.bucketName = data.bucketName || ''
      form.accessKeyId = data.accessKeyId || ''
      form.accessKeySecret = data.accessKeySecret || ''
      form.customDomain = data.customDomain || ''
    }
  } catch (e) {
    ElMessage.error(t('adminOss.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.endpoint || !form.bucketName || !form.accessKeyId) {
    ElMessage.warning(t('adminOss.fillRequired'))
    return
  }
  saving.value = true
  try {
    await request.put('/admin/oss/config', { ...form })
    ElMessage.success(t('adminOss.saveSuccess'))
    await loadConfig()
  } catch (e) {
    ElMessage.error(t('adminOss.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function handleTest() {
  testing.value = true
  try {
    const data = await request.post('/admin/oss/test')
    testResult.value = data
    testResultVisible.value = true
  } catch (e) {
    ElMessage.error(t('adminOss.testFailed'))
  } finally {
    testing.value = false
  }
}

onMounted(loadConfig)
</script>

<style scoped>
.oss-config-page {
  max-width: 700px;
  margin: 0 auto;
}
</style>
