<template>
  <div class="admin-page">
    <h2 style="margin-bottom: 20px; font-size: 18px; font-weight: 600">{{ $t('adminSettings.pageTitle') }}</h2>

    <!-- Site Info -->
    <el-card class="settings-card">
      <template #header>
        <div class="card-header"><span>{{ $t('adminSettings.basicInfo') }}</span></div>
      </template>
      <el-form :model="settings" label-position="top">
        <el-form-item :label="$t('adminSettings.siteName')">
          <el-input v-model="settings.site_name" :placeholder="$t('adminSettings.siteNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminSettings.siteDescription')">
          <el-input v-model="settings.site_description" type="textarea" :rows="3" :placeholder="$t('adminSettings.siteDescriptionPlaceholder')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveBasicInfo" :loading="savingBasicInfo">{{ $t('adminSettings.save') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Filing Info -->
    <el-card class="settings-card">
      <template #header>
        <div class="card-header"><span>{{ $t('adminSettings.filingInfo') }}</span></div>
      </template>
      <el-form :model="settings" label-position="top">
        <el-form-item :label="$t('adminSettings.icpNumber')">
          <el-input v-model="settings.icp_number" :placeholder="$t('adminSettings.icpPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminSettings.policeNumber')">
          <el-input v-model="settings.police_number" :placeholder="$t('adminSettings.policePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminSettings.copyright')">
          <el-input v-model="settings.copyright" :placeholder="$t('adminSettings.copyrightPlaceholder')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveFiling" :loading="savingFiling">{{ $t('adminSettings.save') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Video Settings -->
    <el-card class="settings-card">
      <template #header>
        <div class="card-header"><span>{{ $t('adminSettings.videoSettings') }}</span></div>
      </template>
      <el-form :model="settings" label-position="top">
        <el-form-item :label="$t('adminSettings.videoIds')">
          <el-input v-model="settings.video_ids" :placeholder="$t('adminSettings.videoIdsPlaceholder')" />
          <div class="page-desc" style="margin-top: 4px">{{ $t('adminSettings.videoIdsHint') }}</div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveVideo" :loading="savingVideo">{{ $t('adminSettings.save') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- About Page -->
    <el-card class="settings-card">
      <template #header>
        <div class="card-header"><span>{{ $t('adminSettings.aboutPage') }}</span></div>
      </template>
      <el-form :model="settings" label-position="top">
        <el-form-item :label="$t('adminSettings.siteAvatar')">
          <div class="url-input-row">
            <el-input v-model="settings.site_avatar" :placeholder="$t('adminSettings.siteAvatarPlaceholder')" />
          </div>
          <div class="upload-row">
            <div v-if="settings.site_avatar" class="avatar-preview">
              <img :src="settings.site_avatar" alt="avatar" />
              <el-button size="small" type="danger" text @click="settings.site_avatar = ''">{{ $t('adminSettings.removeImage') }}</el-button>
            </div>
            <FileUpload v-else endpoint="/admin/upload/image" @uploaded="res => settings.site_avatar = res.url" style="height: 140px" />
          </div>
        </el-form-item>
        <el-form-item :label="$t('adminSettings.siteTagline')">
          <el-input v-model="settings.site_tagline" :placeholder="$t('adminSettings.siteTaglinePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminSettings.aboutCover')">
          <div class="url-input-row">
            <el-input v-model="settings.about_cover" :placeholder="$t('adminSettings.aboutCoverPlaceholder')" />
          </div>
          <div class="upload-row">
            <div v-if="settings.about_cover" class="cover-preview">
              <img :src="settings.about_cover" alt="cover" />
              <el-button size="small" type="danger" text @click="settings.about_cover = ''">{{ $t('adminSettings.removeImage') }}</el-button>
            </div>
            <FileUpload v-else endpoint="/admin/upload/image" @uploaded="res => settings.about_cover = res.url" style="height: 140px" />
          </div>
        </el-form-item>
        <el-form-item :label="$t('adminSettings.aboutSubtitle')">
          <el-input v-model="settings.about_subtitle" :placeholder="$t('adminSettings.aboutSubtitlePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminSettings.aboutBio')">
          <el-input v-model="settings.about_bio" type="textarea" :rows="6" :placeholder="$t('adminSettings.aboutBioPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminSettings.contactGithub')">
          <el-input v-model="settings.contact_github" :placeholder="$t('adminSettings.contactGithubPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminSettings.contactEmail')">
          <el-input v-model="settings.contact_email" :placeholder="$t('adminSettings.contactEmailPlaceholder')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSaveAbout" :loading="savingAbout">{{ $t('adminSettings.save') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Password -->
    <el-card class="settings-card">
      <template #header>
        <div class="card-header"><span>{{ $t('adminSettings.changePassword') }}</span></div>
      </template>
      <el-form :model="passwordForm" label-position="top" ref="passwordFormRef">
        <el-form-item :label="$t('adminSettings.oldPassword')" prop="oldPassword" :rules="oldPasswordRules">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password :placeholder="$t('adminSettings.enterOldPassword')" style="max-width: 400px" />
        </el-form-item>
        <el-form-item :label="$t('adminSettings.newPassword')" prop="newPassword" :rules="newPasswordRules">
          <el-input v-model="passwordForm.newPassword" type="password" show-password :placeholder="$t('adminSettings.enterNewPasswordPlaceholder')" style="max-width: 400px" />
        </el-form-item>
        <el-form-item :label="$t('adminSettings.confirmNewPassword')" prop="confirmPassword" :rules="confirmPasswordRules">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password :placeholder="$t('adminSettings.enterConfirmPasswordPlaceholder')" style="max-width: 400px" />
        </el-form-item>
        <el-button type="primary" @click="handleChangePassword" :loading="changingPassword">{{ $t('adminSettings.changePasswordBtn') }}</el-button>
      </el-form>
    </el-card>

    <!-- Phone Binding -->
    <el-card class="settings-card">
      <template #header>
        <div class="card-header"><span>{{ $t('adminSettings.phoneBinding') }}</span></div>
      </template>
      <div v-if="phoneStatus.bound" class="phone-bound-info">
        <p>{{ $t('adminSettings.phoneBound') }}<strong>{{ phoneStatus.phone }}</strong></p>
        <el-button size="small" @click="showChangePhone = true">{{ $t('adminSettings.changePhone') }}</el-button>
      </div>
      <div v-else>
        <p style="color: var(--color-text-secondary); margin-bottom: 12px;">{{ $t('adminSettings.phoneNotBound') }}</p>
        <el-button type="primary" size="small" @click="showBindPhone = true">{{ $t('adminSettings.bindPhone') }}</el-button>
      </div>
    </el-card>

    <!-- Theme -->
    <el-card class="settings-card">
      <template #header>
        <div class="card-header"><span>{{ $t('adminSettings.themeSettings') }}</span></div>
      </template>
      <el-form label-position="top">
        <el-form-item :label="$t('adminSettings.themeMode')">
          <el-radio-group v-model="themeMode" @change="handleThemeChange">
            <el-radio value="light">{{ $t('adminSettings.lightMode') }}</el-radio>
            <el-radio value="dark">{{ $t('adminSettings.darkMode') }}</el-radio>
            <el-radio value="auto">{{ $t('adminSettings.autoMode') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Notifications -->
    <el-card class="settings-card">
      <template #header>
        <div class="card-header"><span>{{ $t('adminSettings.notificationPrefs') }}</span></div>
      </template>
      <el-form label-position="top">
        <el-form-item :label="$t('adminSettings.systemNotify')">
          <el-switch v-model="notifySettings.system" />
          <span class="notify-desc">{{ $t('adminSettings.systemNotifyDesc') }}</span>
        </el-form-item>
        <el-form-item :label="$t('adminSettings.commentNotify')">
          <el-switch v-model="notifySettings.comment" />
          <span class="notify-desc">{{ $t('adminSettings.commentNotifyDesc') }}</span>
        </el-form-item>
        <el-form-item :label="$t('adminSettings.loginNotify')">
          <el-switch v-model="notifySettings.login" />
          <span class="notify-desc">{{ $t('adminSettings.loginNotifyDesc') }}</span>
        </el-form-item>
      </el-form>
      <el-button type="primary" @click="handleSaveNotify" :loading="savingNotify">{{ $t('adminSettings.saveNotifySettings') }}</el-button>
    </el-card>

    <!-- Bind Phone Dialog -->
    <el-dialog v-model="showBindPhone" :title="$t('adminSettings.bindPhoneTitle')" width="400px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item :label="$t('adminSettings.phoneNumber')">
          <el-input v-model="bindPhoneForm.phone" :placeholder="$t('adminSettings.enterPhoneNumber')" maxlength="11" />
        </el-form-item>
        <el-form-item :label="$t('adminSettings.unlockPassword')">
          <el-input v-model="bindPhoneForm.unlockPassword" type="password" show-password :placeholder="$t('adminSettings.unlockPasswordPlaceholder')" maxlength="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBindPhone = false">{{ $t('adminSettings.cancel') }}</el-button>
        <el-button type="primary" @click="handleBindPhone" :loading="bindingPhone">{{ $t('adminSettings.confirmBind') }}</el-button>
      </template>
    </el-dialog>

    <!-- Change Phone Dialog -->
    <el-dialog v-model="showChangePhone" :title="$t('adminSettings.changePhoneTitle')" width="400px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item :label="$t('adminSettings.newPhoneNumber')">
          <el-input v-model="changePhoneForm.newPhone" :placeholder="$t('adminSettings.enterNewPhoneNumber')" maxlength="11" />
        </el-form-item>
        <el-form-item :label="$t('adminSettings.unlockPasswordLabel')">
          <el-input v-model="changePhoneForm.unlockPassword" type="password" show-password :placeholder="$t('adminSettings.enterUnlockPassword')" maxlength="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showChangePhone = false">{{ $t('adminSettings.cancel') }}</el-button>
        <el-button type="primary" @click="handleChangePhone" :loading="changingPhone">{{ $t('adminSettings.confirmChange') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import request from '../../utils/request'
import FileUpload from '../../components/FileUpload.vue'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const settings = reactive({
  site_name: '',
  site_description: '',
  icp_number: '',
  police_number: '',
  copyright: '',
  video_ids: '',
  site_avatar: '',
  site_tagline: '',
  about_cover: '',
  about_subtitle: '',
  about_bio: '',
  contact_github: '',
  contact_email: ''
})
const savingBasicInfo = ref(false)
const savingFiling = ref(false)
const savingVideo = ref(false)
const savingAbout = ref(false)

// --- Password ---
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const changingPassword = ref(false)
const passwordFormRef = ref(null)

const oldPasswordRules = computed(() => [{ required: true, message: t('adminSettings.enterOldPassword') }])
const newPasswordRules = computed(() => [
  { required: true, message: t('adminSettings.enterNewPassword') },
  { min: 6, message: t('adminSettings.passwordMin6') }
])
const confirmPasswordRules = computed(() => [
  { required: true, message: t('adminSettings.enterConfirmPassword') },
  { validator: validateConfirmPassword }
])

function validateConfirmPassword(rule, value, callback) {
  if (value !== passwordForm.newPassword) {
    callback(new Error(t('adminSettings.passwordsNotMatch')))
  } else {
    callback()
  }
}

async function handleChangePassword() {
  if (passwordFormRef.value) {
    try {
      await passwordFormRef.value.validate()
    } catch { return }
  }
  changingPassword.value = true
  try {
    await request.put('/admin/users/me/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success(t('adminSettings.passwordChangeSuccess'))
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    await authStore.logout()
    router.push('/admin/login')
  } catch (e) {
    ElMessage.error(e.message || t('adminSettings.passwordChangeFailed'))
  } finally {
    changingPassword.value = false
  }
}

// --- Phone ---
const phoneStatus = reactive({ bound: false, phone: '' })
const showBindPhone = ref(false)
const showChangePhone = ref(false)
const bindPhoneForm = reactive({ phone: '', unlockPassword: '' })
const changePhoneForm = reactive({ newPhone: '', unlockPassword: '' })
const bindingPhone = ref(false)
const changingPhone = ref(false)

async function loadPhoneStatus() {
  try {
    const data = await request.get('/admin/phone/status')
    phoneStatus.bound = data.bound
    phoneStatus.phone = data.phone || ''
  } catch { /* ignore */ }
}

async function handleBindPhone() {
  if (!bindPhoneForm.phone || !bindPhoneForm.unlockPassword) {
    ElMessage.warning(t('adminSettings.fillPhoneAndPassword'))
    return
  }
  bindingPhone.value = true
  try {
    await request.post('/admin/phone/bind', {
      phone: bindPhoneForm.phone,
      unlockPassword: bindPhoneForm.unlockPassword
    })
    ElMessage.success(t('adminSettings.bindSuccess'))
    showBindPhone.value = false
    bindPhoneForm.phone = ''
    bindPhoneForm.unlockPassword = ''
    await loadPhoneStatus()
  } catch (e) {
    ElMessage.error(e.message || t('adminSettings.bindFailed'))
  } finally {
    bindingPhone.value = false
  }
}

async function handleChangePhone() {
  if (!changePhoneForm.newPhone || !changePhoneForm.unlockPassword) {
    ElMessage.warning(t('adminSettings.fillNewPhoneAndPassword'))
    return
  }
  changingPhone.value = true
  try {
    await request.post('/admin/phone/change', {
      newPhone: changePhoneForm.newPhone,
      unlockPassword: changePhoneForm.unlockPassword
    })
    ElMessage.success(t('adminSettings.changeSuccess'))
    showChangePhone.value = false
    changePhoneForm.newPhone = ''
    changePhoneForm.unlockPassword = ''
    await loadPhoneStatus()
  } catch (e) {
    ElMessage.error(e.message || t('adminSettings.changeFailed'))
  } finally {
    changingPhone.value = false
  }
}

// --- Theme ---
const themeMode = ref(localStorage.getItem('theme') || 'light')

function handleThemeChange(val) {
  localStorage.setItem('theme', val)
  if (val === 'dark') {
    document.body.classList.add('night')
  } else if (val === 'light') {
    document.body.classList.remove('night')
  } else {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    document.body.classList.toggle('night', prefersDark)
  }
}

// --- Notifications ---
const notifySettings = reactive({ system: true, comment: true, login: true })
const savingNotify = ref(false)

async function handleSaveNotify() {
  savingNotify.value = true
  try {
    await request.put('/admin/settings', { settings: {
      notify_system: String(notifySettings.system),
      notify_comment: String(notifySettings.comment),
      notify_login: String(notifySettings.login)
    }})
    ElMessage.success(t('adminSettings.notifySettingsSaved'))
  } catch {
    ElMessage.error(t('adminSettings.settingsSaveFailed'))
  } finally {
    savingNotify.value = false
  }
}

// --- Load Settings ---
async function loadSettings() {
  try {
    const data = await request.get('/admin/settings')
    Object.keys(settings).forEach(key => {
      if (data[key] !== undefined) {
        settings[key] = data[key]
      }
    })
    // Load notification settings
    if (data.notify_system !== undefined) notifySettings.system = data.notify_system === 'true'
    if (data.notify_comment !== undefined) notifySettings.comment = data.notify_comment === 'true'
    if (data.notify_login !== undefined) notifySettings.login = data.notify_login === 'true'
  } catch {
    ElMessage.error(t('adminSettings.loadFailed'))
  }
}

async function saveSettings(partial, loadingRef) {
  loadingRef.value = true
  try {
    await request.put('/admin/settings', { settings: partial })
    ElMessage.success(t('adminSettings.settingsSaved'))
  } catch (e) {
    ElMessage.error(t('adminSettings.saveFailed') + ': ' + (e.message || t('adminSettings.unknownError')))
  } finally {
    loadingRef.value = false
  }
}

async function handleSaveBasicInfo() {
  await saveSettings({ site_name: settings.site_name, site_description: settings.site_description }, savingBasicInfo)
}

async function handleSaveFiling() {
  await saveSettings({ icp_number: settings.icp_number, police_number: settings.police_number, copyright: settings.copyright }, savingFiling)
}

async function handleSaveVideo() {
  await saveSettings({ video_ids: settings.video_ids }, savingVideo)
}

async function handleSaveAbout() {
  await saveSettings({
    site_avatar: settings.site_avatar,
    site_tagline: settings.site_tagline,
    about_cover: settings.about_cover,
    about_subtitle: settings.about_subtitle,
    about_bio: settings.about_bio,
    contact_github: settings.contact_github,
    contact_email: settings.contact_email
  }, savingAbout)
}

onMounted(() => {
  loadSettings()
  loadPhoneStatus()
})
</script>

<style scoped>
.settings-card {
  margin-bottom: 16px;
}

.settings-card :deep(.el-form-item) {
  max-width: 600px;
}

.settings-card :deep(.el-form-item__label) {
  font-weight: 600;
  color: #475569;
}

.phone-bound-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.phone-bound-info p {
  margin: 0;
  font-size: 14px;
}

.notify-desc {
  margin-left: 8px;
  font-size: 12px;
  color: #8b949e;
}

body.body-night .settings-card :deep(.el-form-item__label) {
  color: #cbd5e1;
}

.url-input-row {
  width: 100%;
}

.upload-row {
  width: 100%;
  margin-top: 8px;
}

.avatar-preview,
.cover-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.avatar-preview img {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
}

.cover-preview img {
  width: 120px;
  height: 68px;
  border-radius: 8px;
  object-fit: cover;
}

body.body-night .avatar-preview,
body.body-night .cover-preview {
  border-color: #334155;
  background: #1e293b;
}
</style>
