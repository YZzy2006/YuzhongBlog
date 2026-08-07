<template>
  <div class="admin-page profile-page" v-loading="pageLoading">
    <div class="page-header">
      <h2>{{ $t('adminProfile.title') }}</h2>
      <RefreshButton :onRefresh="loadProfile" />
    </div>

    <!-- Card 1: Avatar + Basic Info -->
    <el-card class="section-card">
      <div class="profile-top">
        <div class="profile-avatar-section">
          <div class="profile-avatar-wrapper" @click="showAvatarDialog = true" :title="$t('adminProfile.avatarTip')">
            <div class="profile-avatar" :style="profile.avatarUrl ? { background: `url(${profile.avatarUrl}) center/cover` } : {}">
              <span v-if="!profile.avatarUrl">{{ avatarLetter }}</span>
            </div>
            <div class="avatar-overlay">
              <el-icon :size="20"><Camera /></el-icon>
            </div>
          </div>
          <div class="avatar-hint">{{ $t('adminProfile.avatarTip') }}</div>
        </div>
        <div class="profile-basic">
          <div class="profile-username">{{ profile.username }}</div>
          <div class="profile-tags">
            <el-tag size="small" :type="profile.role === 'super_admin' ? 'danger' : 'primary'" effect="plain">
              {{ profile.role === 'super_admin' ? $t('adminProfile.superAdmin') : $t('adminProfile.adminRole') }}
            </el-tag>
            <el-tag v-if="profile.realName" size="small" type="info" effect="plain">{{ profile.realName }}</el-tag>
          </div>
          <div class="profile-meta">
            <span v-if="profile.email" class="meta-item">
              <el-icon><Message /></el-icon> {{ profile.email }}
            </span>
            <span v-if="profile.createdAt" class="meta-item">
              <el-icon><Calendar /></el-icon> {{ $t('adminProfile.joined') }} {{ profile.createdAt }}
            </span>
          </div>
          <div class="profile-bio" v-if="profile.bio">{{ profile.bio }}</div>
          <div class="profile-bio empty" v-else>{{ $t('adminProfile.noBio') }}</div>
        </div>
      </div>
    </el-card>

    <!-- Card 2: Edit Profile -->
    <el-card class="section-card">
      <template #header>
        <div class="card-header">
          <span>{{ $t('adminProfile.editProfile') }}</span>
        </div>
      </template>
      <el-form :model="form" label-position="top" style="max-width: 500px">
        <el-form-item :label="$t('adminProfile.name')">
          <el-input v-model="form.name" :placeholder="$t('adminProfile.namePlaceholder')" maxlength="50" />
        </el-form-item>
        <el-form-item :label="$t('adminProfile.realName')">
          <el-input v-model="form.realName" :placeholder="$t('adminProfile.realNamePlaceholder')" maxlength="50" />
        </el-form-item>
        <el-form-item :label="$t('adminProfile.email')">
          <el-input v-model="form.email" :placeholder="$t('adminProfile.emailPlaceholder')" maxlength="100" />
        </el-form-item>
        <el-form-item :label="$t('adminProfile.bio')">
          <el-input v-model="form.bio" type="textarea" :rows="4" :placeholder="$t('adminProfile.bioPlaceholder')" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">{{ $t('adminProfile.save') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Card 3: Account Security -->
    <el-card class="section-card">
      <template #header>
        <div class="card-header">
          <span>{{ $t('adminProfile.accountSecurity') }}</span>
        </div>
      </template>
      <div class="security-info">
        <div class="info-row">
          <span class="info-label">{{ $t('adminProfile.username') }}</span>
          <span class="info-value">{{ profile.username }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">{{ $t('adminProfile.phoneNumber') }}</span>
          <span class="info-value">
            <template v-if="profile.phone">{{ maskPhone(profile.phone) }}</template>
            <el-tag v-else size="small" type="info">{{ $t('adminProfile.phoneUnbound') }}</el-tag>
          </span>
        </div>
        <div class="info-row">
          <span class="info-label">{{ $t('adminProfile.accountRole') }}</span>
          <span class="info-value">
            <el-tag size="small" :type="profile.role === 'super_admin' ? 'danger' : 'primary'" effect="plain">
              {{ profile.role === 'super_admin' ? $t('adminProfile.superAdmin') : $t('adminProfile.adminRole') }}
            </el-tag>
          </span>
        </div>
        <el-button type="primary" plain size="small" @click="router.push('/admin/account-security')" style="margin-top: 8px">
          {{ $t('adminProfile.goToSecurity') }}
        </el-button>
      </div>
    </el-card>

    <!-- Card 4: Change Password -->
    <el-card class="section-card">
      <template #header>
        <div class="card-header">
          <span>{{ $t('adminProfile.changePassword') }}</span>
        </div>
      </template>
      <el-form :model="pwForm" label-position="top" style="max-width: 400px" @submit.prevent="handleChangePassword">
        <el-form-item :label="$t('adminProfile.oldPassword')">
          <el-input v-model="pwForm.oldPassword" type="password" show-password :placeholder="$t('adminProfile.oldPasswordPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminProfile.newPassword')">
          <el-input v-model="pwForm.newPassword" type="password" show-password :placeholder="$t('adminProfile.newPasswordPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminProfile.confirmPassword')">
          <el-input v-model="pwForm.confirmPassword" type="password" show-password :placeholder="$t('adminProfile.confirmPasswordPlaceholder')" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="handleChangePassword" :loading="changingPassword">
            {{ $t('adminProfile.changePasswordBtn') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Card 5: Account Info (read-only) -->
    <el-card class="section-card">
      <template #header>
        <div class="card-header">
          <span>{{ $t('adminProfile.accountInfo') }}</span>
        </div>
      </template>
      <div class="account-info-grid">
        <div class="info-item">
          <div class="info-item-label">{{ $t('adminProfile.accountId') }}</div>
          <div class="info-item-value">#{{ profile.id }}</div>
        </div>
        <div class="info-item">
          <div class="info-item-label">{{ $t('adminProfile.username') }}</div>
          <div class="info-item-value">{{ profile.username }}</div>
        </div>
        <div class="info-item">
          <div class="info-item-label">{{ $t('adminProfile.accountRole') }}</div>
          <div class="info-item-value">
            <el-tag size="small" :type="profile.role === 'super_admin' ? 'danger' : 'primary'" effect="plain">
              {{ profile.role === 'super_admin' ? $t('adminProfile.superAdmin') : $t('adminProfile.adminRole') }}
            </el-tag>
          </div>
        </div>
        <div class="info-item">
          <div class="info-item-label">{{ $t('adminProfile.permissionsCount') }}</div>
          <div class="info-item-value">{{ authStore.permissions?.length || 0 }}</div>
        </div>
        <div class="info-item">
          <div class="info-item-label">{{ $t('adminProfile.joined') }}</div>
          <div class="info-item-value">{{ profile.createdAt || '-' }}</div>
        </div>
        <div class="info-item">
          <div class="info-item-label">{{ $t('adminProfile.email') }}</div>
          <div class="info-item-value">{{ profile.email || '-' }}</div>
        </div>
      </div>
    </el-card>

    <!-- Avatar Upload Dialog -->
    <el-dialog v-model="showAvatarDialog" :title="$t('adminProfile.avatarUpload')" width="360px" :close-on-click-modal="false">
      <FileUpload endpoint="/admin/profile/avatar" @uploaded="onAvatarUploaded" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { Camera, Message, Calendar } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'
import RefreshButton from '../../components/RefreshButton.vue'
import FileUpload from '../../components/FileUpload.vue'

const router = useRouter()
const { t } = useI18n()
const authStore = useAuthStore()

const pageLoading = ref(true)
const saving = ref(false)
const changingPassword = ref(false)
const profile = ref({ id: null, username: '', name: '', realName: '', bio: '', avatarUrl: '', email: '', phone: '', role: '', createdAt: '' })
const form = reactive({ name: '', realName: '', email: '', bio: '' })
const pwForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const showAvatarDialog = ref(false)

const avatarLetter = computed(() => {
  const display = profile.value.name || profile.value.username || '?'
  return display.charAt(0).toUpperCase()
})

function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone || '-'
  return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
}

async function loadProfile() {
  pageLoading.value = true
  try {
    const data = await request.get('/admin/profile')
    profile.value = data
    form.name = data.name || ''
    form.realName = data.realName || ''
    form.email = data.email || ''
    form.bio = data.bio || ''
  } catch (e) {
    console.error('Failed to load profile:', e)
  } finally {
    pageLoading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const data = await request.put('/admin/profile', {
      name: form.name,
      realName: form.realName,
      email: form.email,
      bio: form.bio
    })
    profile.value = data
    if (data.name) {
      authStore.name = data.name
      localStorage.setItem('name', data.name)
    }
    if (data.avatarUrl !== undefined) {
      authStore.avatarUrl = data.avatarUrl || ''
      localStorage.setItem('avatarUrl', data.avatarUrl || '')
    }
    authStore.bio = form.bio || ''
    localStorage.setItem('bio', form.bio || '')
    ElMessage.success(t('adminProfile.saveSuccess'))
  } catch (e) {
    ElMessage.error(e.message || t('adminProfile.saveFailed'))
  } finally {
    saving.value = false
  }
}

// --- Avatar Upload ---
function onAvatarUploaded(res) {
  profile.value = res
  if (res.avatarUrl) {
    authStore.avatarUrl = res.avatarUrl
    localStorage.setItem('avatarUrl', res.avatarUrl)
  }
  showAvatarDialog.value = false
  ElMessage.success(t('adminProfile.avatarSuccess'))
}

// --- Password Change ---
async function handleChangePassword() {
  if (!pwForm.oldPassword || !pwForm.newPassword || !pwForm.confirmPassword) {
    ElMessage.warning(t('adminProfile.oldPasswordPlaceholder'))
    return
  }
  if (pwForm.newPassword.length < 6) {
    ElMessage.warning(t('adminProfile.passwordMinLength'))
    return
  }
  if (pwForm.newPassword !== pwForm.confirmPassword) {
    ElMessage.warning(t('adminProfile.passwordMismatch'))
    return
  }
  changingPassword.value = true
  try {
    await request.put('/admin/users/me/password', {
      oldPassword: pwForm.oldPassword,
      newPassword: pwForm.newPassword
    })
    ElMessage.success(t('adminProfile.passwordSuccess'))
    pwForm.oldPassword = ''
    pwForm.newPassword = ''
    pwForm.confirmPassword = ''
    authStore.logout()
    router.push('/admin/login')
  } catch (e) {
    ElMessage.error(e.message || t('adminProfile.passwordFailed'))
  } finally {
    changingPassword.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-top {
  display: flex;
  align-items: flex-start;
  gap: 28px;
}
.profile-avatar-section {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.profile-avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}
.profile-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.4rem;
  font-weight: 600;
  transition: filter 0.2s;
}
.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s;
  border-radius: 50%;
}
.profile-avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}
.profile-avatar-wrapper:hover .profile-avatar {
  filter: brightness(0.7);
}
.avatar-hint {
  font-size: 12px;
  color: #94a3b8;
}
.profile-basic {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}
.profile-username {
  font-size: 1.2rem;
  font-weight: 600;
  color: #1e293b;
}
.profile-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.profile-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #64748b;
}
.profile-bio {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
}
.profile-bio.empty {
  color: #94a3b8;
  font-style: italic;
}
.card-header {
  font-weight: 600;
}
.security-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.info-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.info-label {
  font-size: 13px;
  color: #94a3b8;
  width: 70px;
  flex-shrink: 0;
}
.info-value {
  font-size: 14px;
  color: #334155;
}
.account-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.info-item-label {
  font-size: 12px;
  color: #94a3b8;
}
.info-item-value {
  font-size: 14px;
  color: #334155;
  font-weight: 500;
}
</style>
