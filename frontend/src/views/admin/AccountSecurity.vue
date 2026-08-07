<template>
  <div class="admin-page account-security-page" v-loading="pageLoading">
    <div class="page-header">
      <h2>{{ $t('adminAccountSecurity.title') }}</h2>
      <RefreshButton :onRefresh="loadData" />
    </div>

    <!-- Current Session -->
    <el-card class="section-card">
      <template #header>
        <div class="card-header">
          <el-icon><Monitor /></el-icon>
          <span>{{ $t('adminAccountSecurity.currentSession') }}</span>
        </div>
      </template>
      <div class="session-info" v-if="data">
        <div class="session-item">
          <span class="session-label">{{ $t('adminAccountSecurity.loginDevice') }}</span>
          <span class="session-value">{{ data.currentSession?.deviceInfo || $t('adminAccountSecurity.unknownDevice') }}</span>
        </div>
        <div class="session-item">
          <span class="session-label">{{ $t('adminAccountSecurity.loginTime') }}</span>
          <span class="session-value">{{ data.currentSession?.loginTime || $t('adminAccountSecurity.unknown') }}</span>
        </div>
        <div class="session-item">
          <span class="session-label">{{ $t('adminAccountSecurity.ipAddress') }}</span>
          <span class="session-value mono">{{ data.currentSession?.loginIp || $t('adminAccountSecurity.unknown') }}</span>
        </div>
        <div class="session-item">
          <span class="session-label">{{ $t('adminAccountSecurity.username') }}</span>
          <span class="session-value">{{ data.username }}</span>
        </div>
        <div class="session-item">
          <span class="session-label">{{ $t('adminAccountSecurity.role') }}</span>
          <span class="session-value">
            <el-tag :type="data.role === 'super_admin' ? 'danger' : 'primary'" effect="plain" size="small">
              {{ data.role === 'super_admin' ? $t('adminAccountSecurity.superAdmin') : $t('adminAccountSecurity.admin') }}
            </el-tag>
          </span>
        </div>
      </div>
    </el-card>

    <!-- Activity Heatmap -->
    <el-card class="section-card">
      <template #header>
        <div class="card-header">
          <el-icon><DataLine /></el-icon>
          <span>{{ $t('adminAccountSecurity.loginActivity') }}</span>
          <span class="heatmap-legend">
            <span class="legend-label">{{ $t('adminAccountSecurity.legendLess') }}</span>
            <span class="legend-cell level-0"></span>
            <span class="legend-cell level-1"></span>
            <span class="legend-cell level-2"></span>
            <span class="legend-cell level-3"></span>
            <span class="legend-label">{{ $t('adminAccountSecurity.legendMore') }}</span>
          </span>
        </div>
      </template>
      <div class="heatmap-wrapper" v-if="data">
        <div class="heatmap-months">
          <span v-for="m in monthLabels" :key="m.label" :style="{ gridColumn: m.col }">{{ m.label }}</span>
        </div>
        <div class="heatmap-grid">
          <div v-for="(day, i) in heatmapDays" :key="i"
               class="heatmap-cell" :class="getCellClass(day.count)"
               :title="$t('adminAccountSecurity.loginCount', { date: day.date, count: day.count })">
          </div>
        </div>
      </div>
      <el-empty v-else :description="$t('adminAccountSecurity.noData')" :image-size="60" />
    </el-card>

    <!-- Security Alerts (superAdmin only) -->
    <el-card v-if="authStore.isSuperAdmin && data?.securityAlerts?.length" class="section-card alert-card">
      <template #header>
        <div class="card-header">
          <el-icon color="#ef4444"><WarningFilled /></el-icon>
          <span>{{ $t('adminAccountSecurity.securityAlerts') }}</span>
          <el-tag type="danger" effect="dark" size="small" style="margin-left: 8px">
            {{ $t('adminAccountSecurity.anomalyCount', { count: data.securityAlerts.length }) }}
          </el-tag>
        </div>
      </template>
      <div class="alert-list">
        <div v-for="alert in data.securityAlerts" :key="alert.username" class="alert-item">
          <div class="alert-icon">
            <el-icon color="#ef4444"><CircleCloseFilled /></el-icon>
          </div>
          <div class="alert-info">
            <span class="alert-user">{{ alert.username }}</span>
            <span class="alert-detail">
              {{ $t('adminAccountSecurity.loginFailDetail', { count: alert.failCount }) }}
              <span v-if="alert.lastFailTime">{{ $t('adminAccountSecurity.recentFail') }}{{ alert.lastFailTime }}</span>
            </span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- Password Change -->
    <el-card class="section-card">
      <template #header>
        <div class="card-header">
          <el-icon><Lock /></el-icon>
          <span>{{ $t('adminAccountSecurity.changePassword') }}</span>
        </div>
      </template>
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px" style="max-width: 400px">
        <el-form-item :label="$t('adminAccountSecurity.oldPassword')" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password :placeholder="$t('adminAccountSecurity.oldPasswordPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminAccountSecurity.newPassword')" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password :placeholder="$t('adminAccountSecurity.newPasswordPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminAccountSecurity.confirmPassword')" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password :placeholder="$t('adminAccountSecurity.confirmPasswordPlaceholder')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="changePassword" :loading="passwordLoading">{{ $t('adminAccountSecurity.changePasswordBtn') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Phone Management -->
    <el-card class="section-card">
      <template #header>
        <div class="card-header">
          <el-icon><Iphone /></el-icon>
          <span>{{ $t('adminAccountSecurity.phoneManagement') }}</span>
        </div>
      </template>
      <div v-if="data" class="phone-section">
        <div class="phone-status">
          <span class="phone-label">{{ $t('adminAccountSecurity.bindStatus') }}</span>
          <el-tag v-if="data.phoneBound" type="success" effect="plain" size="small">{{ $t('adminAccountSecurity.bound') }}</el-tag>
          <el-tag v-else type="info" effect="plain" size="small">{{ $t('adminAccountSecurity.unbound') }}</el-tag>
        </div>
        <div v-if="data.phoneBound" class="phone-number">
          <span class="phone-label">{{ $t('adminAccountSecurity.phoneNumber') }}</span>
          <span class="mono">{{ data.maskedPhone }}</span>
        </div>
        <el-button v-if="data.phoneBound" type="primary" size="small" style="margin-top: 12px"
          @click="openPhoneDialog('change')">
          {{ $t('adminAccountSecurity.changePhone') }}
        </el-button>
        <el-button v-else type="primary" size="small" @click="openPhoneDialog('bind')">
          {{ $t('adminAccountSecurity.bindPhone') }}
        </el-button>
      </div>
    </el-card>

    <!-- Phone Dialog -->
    <el-dialog v-model="phoneDialogVisible" :title="phoneMode === 'bind' ? $t('adminAccountSecurity.phoneDialogTitleBind') : $t('adminAccountSecurity.phoneDialogTitleChange')" width="420px" destroy-on-close>
      <el-form :model="phoneForm" :rules="phoneRules" ref="phoneFormRef" label-width="80px">
        <el-form-item :label="$t('adminAccountSecurity.phoneLabel')" prop="phone">
          <el-input v-model="phoneForm.phone" :placeholder="$t('adminAccountSecurity.phonePlaceholder')" maxlength="11" />
        </el-form-item>
        <el-form-item :label="$t('adminAccountSecurity.unlockPassword')" prop="unlockPassword">
          <el-input v-model="phoneForm.unlockPassword" type="password" show-password :placeholder="$t('adminAccountSecurity.unlockPasswordPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="phoneDialogVisible = false">{{ $t('adminAccountSecurity.cancel') }}</el-button>
        <el-button type="primary" :loading="phoneLoading" @click="submitPhone">
          {{ phoneMode === 'bind' ? $t('adminAccountSecurity.bind') : $t('adminAccountSecurity.modify') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { Monitor, DataLine, WarningFilled, CircleCloseFilled, Lock, Iphone } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'
import RefreshButton from '../../components/RefreshButton.vue'

const { t } = useI18n()

const router = useRouter()
const authStore = useAuthStore()
const data = ref(null)
const pageLoading = ref(true)
const passwordFormRef = ref(null)
const passwordLoading = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error(t('adminAccountSecurity.passwordMismatch')))
  } else {
    callback()
  }
}

const passwordRules = computed(() => ({
  oldPassword: [{ required: true, message: t('adminAccountSecurity.enterOldPassword'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('adminAccountSecurity.enterNewPassword'), trigger: 'blur' },
    { min: 6, message: t('adminAccountSecurity.passwordMinLength'), trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: t('adminAccountSecurity.enterConfirmPassword'), trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}))

// Heatmap
const heatmapDays = computed(() => {
  if (!data.value?.heatmap) return []
  return data.value.heatmap
})

const monthLabels = computed(() => {
  if (!data.value?.heatmap?.length) return []
  const months = []
  const seen = new Set()
  const labels = ['', 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  data.value.heatmap.forEach((day, i) => {
    const month = day.date.substring(0, 7) // yyyy-MM
    if (!seen.has(month)) {
      seen.add(month)
      const mon = parseInt(month.split('-')[1])
      // Grid fills column-first (7 rows per column), so column = floor(index / 7) + 1
      const col = Math.floor(i / 7) + 1
      months.push({ label: labels[mon], col })
    }
  })
  return months
})

function getCellClass(count) {
  if (count === 0) return 'level-0'
  if (count === 1) return 'level-1'
  if (count <= 3) return 'level-2'
  return 'level-3'
}

// Data loading
async function loadData() {
  pageLoading.value = true
  try {
    const res = await request.get('/admin/account-security')
    data.value = res
  } catch (e) {
    console.error('Failed to load account security data:', e)
  } finally {
    pageLoading.value = false
  }
}

async function changePassword() {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    passwordLoading.value = true
    try {
      await request.put('/admin/users/me/password', {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      ElMessage.success(t('adminAccountSecurity.passwordChangeSuccess'))
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      await authStore.logout()
      router.push('/admin/login')
    } catch (e) {
      ElMessage.error(e.message || t('adminAccountSecurity.passwordChangeFailed'))
    } finally {
      passwordLoading.value = false
    }
  })
}

// Phone dialog
const phoneDialogVisible = ref(false)
const phoneMode = ref('bind') // 'bind' or 'change'
const phoneLoading = ref(false)
const phoneFormRef = ref(null)
const phoneForm = reactive({ phone: '', unlockPassword: '' })

const phoneRules = computed(() => ({
  phone: [
    { required: true, message: t('adminAccountSecurity.enterPhone'), trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: t('adminAccountSecurity.phoneFormatError'), trigger: 'blur' }
  ],
  unlockPassword: [
    { required: true, message: t('adminAccountSecurity.enterUnlockPassword'), trigger: 'blur' },
    { min: 4, message: t('adminAccountSecurity.unlockPasswordMinLength'), trigger: 'blur' }
  ]
}))

function openPhoneDialog(mode) {
  phoneMode.value = mode
  phoneForm.phone = ''
  phoneForm.unlockPassword = ''
  phoneDialogVisible.value = true
}

async function submitPhone() {
  if (!phoneFormRef.value) return
  await phoneFormRef.value.validate(async (valid) => {
    if (!valid) return
    phoneLoading.value = true
    try {
      const url = phoneMode.value === 'bind'
        ? '/admin/phone/bind'
        : '/admin/phone/change'
      const body = phoneMode.value === 'bind'
        ? { phone: phoneForm.phone, unlockPassword: phoneForm.unlockPassword }
        : { newPhone: phoneForm.phone, unlockPassword: phoneForm.unlockPassword }
      await request.post(url, body)
      ElMessage.success(phoneMode.value === 'bind' ? t('adminAccountSecurity.bindSuccess') : t('adminAccountSecurity.modifySuccess'))
      phoneDialogVisible.value = false
      loadData()
    } catch (e) {
      ElMessage.error(e.message || t('adminAccountSecurity.actionFailed'))
    } finally {
      phoneLoading.value = false
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  font-size: 1.25rem;
}
.section-card {
  margin-bottom: 16px;
}
.section-card:last-child {
  margin-bottom: 0;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

/* Session */
.session-info {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.session-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.session-label {
  font-size: 12px;
  color: #94a3b8;
}
.session-value {
  font-size: 14px;
  color: #1e293b;
}
.session-value.mono {
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
}

/* Heatmap */
.heatmap-wrapper {
  overflow-x: auto;
  padding-bottom: 8px;
}
.heatmap-months {
  display: grid;
  grid-template-columns: repeat(54, 14px);
  gap: 2px;
  margin-bottom: 4px;
  font-size: 10px;
  color: #94a3b8;
}
.heatmap-grid {
  display: grid;
  grid-template-rows: repeat(7, 14px);
  grid-auto-flow: column;
  grid-auto-columns: 14px;
  gap: 2px;
  width: max-content;
}
.heatmap-cell {
  width: 14px;
  height: 14px;
  border-radius: 2px;
  cursor: pointer;
  transition: outline 0.1s;
}
.heatmap-cell:hover {
  outline: 2px solid #3b82f6;
  outline-offset: 1px;
}
.heatmap-cell.level-0 { background: #e2e8f0; }
.heatmap-cell.level-1 { background: #9be9a8; }
.heatmap-cell.level-2 { background: #40c463; }
.heatmap-cell.level-3 { background: #30a14e; }
.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-left: auto;
  font-size: 11px;
  font-weight: 400;
  color: #94a3b8;
}
.legend-label {
  font-size: 10px;
}
.legend-cell {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

/* Alerts */
.alert-card :deep(.el-card__header) {
  background: rgba(239, 68, 68, 0.04);
}
.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(239, 68, 68, 0.04);
  border-radius: 8px;
  border: 1px solid rgba(239, 68, 68, 0.15);
}
.alert-icon {
  margin-top: 2px;
}
.alert-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.alert-user {
  font-weight: 600;
  font-size: 14px;
  color: #1e293b;
}
.alert-detail {
  font-size: 13px;
  color: #64748b;
}
.alert-detail strong {
  color: #ef4444;
}

/* Phone */
.phone-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.phone-status,
.phone-number {
  display: flex;
  align-items: center;
  gap: 8px;
}
.phone-label {
  font-size: 13px;
  color: #64748b;
}
.mono {
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
}
</style>
