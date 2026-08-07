<template>
  <el-dialog
    v-model="visible"
    :title="$t('kick.title')"
    width="420px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="kick-dialog"
  >
    <div class="kick-content">
      <div class="kick-icon">⚠️</div>
      <div class="kick-message">{{ $t('kick.message') }}</div>
      <div class="kick-device">
        <div class="kick-device-row">
          <span class="kick-label">{{ $t('kick.device') }}</span>
          <span class="kick-value">{{ notification?.deviceInfo }}</span>
        </div>
        <div class="kick-device-row">
          <span class="kick-label">{{ $t('kick.time') }}</span>
          <span class="kick-value">{{ notification?.loginTime }}</span>
        </div>
        <div v-if="notification?.ip" class="kick-device-row">
          <span class="kick-label">{{ $t('kick.ip') }}</span>
          <span class="kick-value">{{ notification?.ip }}</span>
        </div>
        <div v-if="notification?.loginMethod" class="kick-device-row">
          <span class="kick-label">{{ $t('kick.loginMethod') }}</span>
          <span class="kick-value">{{ notification?.loginMethod }}</span>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="kick-footer">
        <button
          v-if="!isSuperAdmin"
          class="kick-btn freeze-btn"
          :disabled="freezing"
          @click="onFreeze"
        >
          {{ freezing ? $t('kick.freezing') : $t('kick.freeze') }}
        </button>
        <button class="kick-btn confirm-btn" @click="onConfirm">
          {{ $t('kick.confirm') }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const freezing = ref(false)

const notification = computed(() => authStore.kickNotification)
const isSuperAdmin = computed(() => notification.value?.role === 'super_admin')
const visible = computed(() => !!notification.value)

async function onFreeze() {
  freezing.value = true
  try {
    await authStore.freezeAccount()
    authStore.kickNotification = null
    ElMessage.success(t('kick.freezeSuccess'))
    router.push('/admin/login')
  } catch {
    ElMessage.error(t('kick.freezeFailed'))
  } finally {
    freezing.value = false
  }
}

function onConfirm() {
  authStore.kickNotification = null
  router.push('/admin/login')
}
</script>

<style scoped>
.kick-content {
  text-align: center;
  padding: 8px 0;
}

.kick-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.kick-message {
  font-size: 15px;
  color: #333;
  margin-bottom: 16px;
  line-height: 1.5;
}

.kick-device {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px 16px;
  text-align: left;
}

.kick-device-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}

.kick-device-row + .kick-device-row {
  border-top: 1px solid #eee;
  margin-top: 4px;
  padding-top: 8px;
}

.kick-label {
  font-size: 13px;
  color: #999;
}

.kick-value {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.kick-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.kick-btn {
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid #ddd;
  transition: all 0.2s;
}

.freeze-btn {
  background: #fff;
  color: #e74c3c;
  border-color: #e74c3c;
}

.freeze-btn:hover:not(:disabled) {
  background: #e74c3c;
  color: #fff;
}

.freeze-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.confirm-btn {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.confirm-btn:hover {
  background: #3a8ee6;
  border-color: #3a8ee6;
}
</style>
