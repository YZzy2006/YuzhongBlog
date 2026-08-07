<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>{{ $t('adminUsers.pageTitle') }}</h2>
      <div class="header-actions">
        <RefreshButton :onRefresh="loadUsers" />
        <el-button @click="showPasswordDialog = true">
          <el-icon><Lock /></el-icon> {{ $t('adminUsers.changePassword') }}
        </el-button>
        <el-button v-if="authStore.isSuperAdmin" type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> {{ $t('adminUsers.createUser') }}
        </el-button>
      </div>
    </div>

    <!-- 搜索 -->
    <el-card style="margin-bottom: 16px">
      <el-input v-model="keyword" :placeholder="$t('adminUsers.searchPlaceholder')" clearable
                @input="onKeywordInput" style="width: 300px; margin-right: 8px">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="loadUsers(1)">{{ $t('adminUsers.search') }}</el-button>
    </el-card>

    <!-- 用户列表 -->
    <el-card>
      <el-table :data="users" row-key="id" stripe v-loading="loading" :empty-text="$t('adminUsers.emptyText')">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" :label="$t('adminUsers.username')" min-width="80" show-overflow-tooltip />
        <el-table-column prop="realName" :label="$t('adminUsers.realName')" min-width="70">
          <template #default="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="email" :label="$t('adminUsers.email')" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column :label="$t('adminUsers.role')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.role === 'super_admin' ? 'danger' : row.role === 'visitor' ? 'info' : 'primary'" size="small">
              {{ row.role === 'super_admin' ? $t('adminUsers.roleSuperAdmin') : row.role === 'visitor' ? $t('adminUsers.roleVisitor') : $t('adminUsers.roleAdmin') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('adminUsers.status')" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? $t('adminUsers.statusNormal') : $t('adminUsers.statusDisabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="$t('adminUsers.createdAt')" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="$t('adminUsers.action')" width="280">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button v-if="canEdit(row)" type="primary" size="small" @click="openEdit(row)">{{ $t('adminUsers.edit') }}</el-button>
              <el-button v-if="canEditPermissions(row)" type="success" size="small" @click="openPermissions(row)">{{ $t('adminUsers.permissions') }}</el-button>
              <el-button v-if="canToggle(row)" :type="row.enabled ? 'warning' : 'success'" size="small" @click="handleToggle(row)">{{ row.enabled ? $t('adminUsers.disable') : $t('adminUsers.enable') }}</el-button>
              <el-button v-if="canResetPassword(row)" size="small" @click="openResetPassword(row)">{{ $t('adminUsers.reset') }}</el-button>
              <el-button v-if="canDelete(row)" type="danger" size="small" @click="handleDelete(row)">{{ $t('adminUsers.delete') }}</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination background layout="total, prev, pager, next, sizes"
                       :total="total" :page-size="pageSize" :current-page="currentPage"
                       :page-sizes="[10, 20, 50]"
                       @current-change="p => loadUsers(p)"
                       @size-change="s => { pageSize = s; loadUsers(1) }" />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="isEditing ? $t('adminUsers.editUser') : $t('adminUsers.createUserTitle')" width="460px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item :label="$t('adminUsers.username')" required>
          <el-input v-model="form.username" :disabled="isEditing" :placeholder="$t('adminUsers.usernamePlaceholder')" />
        </el-form-item>
        <template v-if="!isEditing">
          <el-form-item :label="$t('adminUsers.password')" required>
            <el-input v-model="form.password" type="password" :placeholder="$t('adminUsers.passwordPlaceholder')" show-password />
          </el-form-item>
        </template>
        <el-form-item :label="$t('adminUsers.role')" required>
          <el-select v-model="form.role" style="width: 100%">
            <el-option :label="$t('adminUsers.roleAdminOption')" value="admin" />
            <el-option :label="$t('adminUsers.roleSuperAdminOption')" value="super_admin"
                       :disabled="!authStore.isSuperAdmin" />
            <el-option :label="$t('adminUsers.roleVisitorOption')" value="visitor" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('adminUsers.realName')">
          <el-input v-model="form.realName" :placeholder="$t('adminUsers.realNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminUsers.email')">
          <el-input v-model="form.email" :placeholder="$t('adminUsers.emailPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">{{ $t('adminUsers.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEditing ? $t('adminUsers.save') : $t('adminUsers.create') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="showResetDialog" :title="$t('adminUsers.resetPasswordTitle')" width="400px" destroy-on-close>
      <p style="margin-bottom: 16px; color: var(--color-text-secondary);">
        {{ $t('adminUsers.resetPasswordDescPrefix') }} <strong>{{ resetTarget?.username }}</strong> {{ $t('adminUsers.resetPasswordDescSuffix') }}
      </p>
      <el-form :model="resetForm" label-width="80px">
        <el-form-item :label="$t('adminUsers.newPassword')" required>
          <el-input v-model="resetForm.newPassword" type="password" :placeholder="$t('adminUsers.passwordPlaceholder')" show-password />
        </el-form-item>
        <el-form-item :label="$t('adminUsers.confirmPassword')" required>
          <el-input v-model="resetForm.confirmPassword" type="password" :placeholder="$t('adminUsers.confirmPasswordPlaceholder')" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showResetDialog = false">{{ $t('adminUsers.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleResetPassword">{{ $t('adminUsers.confirmReset') }}</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPasswordDialog" :title="$t('adminUsers.changePasswordTitle')" width="400px" destroy-on-close>
      <el-form :model="passwordForm" label-width="80px">
        <el-form-item :label="$t('adminUsers.oldPassword')" required>
          <el-input v-model="passwordForm.oldPassword" type="password" :placeholder="$t('adminUsers.oldPasswordPlaceholder')" show-password />
        </el-form-item>
        <el-form-item :label="$t('adminUsers.newPassword')" required>
          <el-input v-model="passwordForm.newPassword" type="password" :placeholder="$t('adminUsers.passwordPlaceholder')" show-password />
        </el-form-item>
        <el-form-item :label="$t('adminUsers.confirmPassword')" required>
          <el-input v-model="passwordForm.confirmPassword" type="password" :placeholder="$t('adminUsers.confirmPasswordPlaceholder')" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">{{ $t('adminUsers.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="handleChangePassword">{{ $t('adminUsers.confirmChange') }}</el-button>
      </template>
    </el-dialog>

    <!-- 权限编辑对话框 -->
    <el-dialog v-model="showPermissionDialog" :title="$t('adminUsers.editPermissionTitle', { username: permissionTarget?.username })" width="560px" destroy-on-close>
      <div v-if="permissionTarget" class="permission-header">
        <el-tag :type="permissionTarget.role === 'super_admin' ? 'danger' : permissionTarget.role === 'visitor' ? 'info' : 'primary'" size="small">
          {{ permissionTarget.role === 'super_admin' ? $t('adminUsers.roleSuperAdmin') : permissionTarget.role === 'visitor' ? $t('adminUsers.roleVisitor') : $t('adminUsers.roleAdmin') }}
        </el-tag>
        <span v-if="permissionTarget.role === 'super_admin'" class="perm-hint">{{ $t('adminUsers.superAdminFullPerm') }}</span>
        <span v-else-if="permissionTarget.role === 'visitor'" class="perm-hint">{{ $t('adminUsers.visitorReadOnlyPerm') }}</span>
        <span v-else class="perm-hint">{{ $t('adminUsers.adminPermHint') }}</span>
      </div>

      <div v-loading="permissionsLoading" class="permission-groups">
        <div v-for="(items, group) in groupedPermissions" :key="group" class="permission-group">
          <div class="group-title">{{ group }}</div>
          <div v-for="item in items" :key="item.code" class="permission-item">
            <span class="perm-label">{{ item.label }}</span>
            <label class="switch" :class="{ disabled: item.locked }"
                   @click.prevent="if (!item.locked) { item.enabled = !item.enabled; onPermissionChange(item) }">
              <input type="checkbox" :checked="item.enabled" :disabled="item.locked">
              <div class="slider">
                <div class="circle">
                  <svg class="cross" xml:space="preserve" style="enable-background:new 0 0 512 512" viewBox="0 0 365.696 365.696" y="0" x="0" height="6" width="6" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1" xmlns="http://www.w3.org/2000/svg">
                    <g>
                      <path data-original="#000000" fill="currentColor" d="M243.188 182.86 356.32 69.726c12.5-12.5 12.5-32.766 0-45.247L341.238 9.398c-12.504-12.503-32.77-12.503-45.25 0L182.86 122.528 69.727 9.374c-12.5-12.5-32.766-12.5-45.247 0L9.375 24.457c-12.5 12.504-12.5 32.77 0 45.25l113.152 113.152L9.398 295.99c-12.503 12.503-12.503 32.769 0 45.25L24.48 356.32c12.5 12.5 32.766 12.5 45.247 0l113.132-113.132L295.99 356.32c12.503 12.5 32.769 12.5 45.25 0l15.081-15.082c12.5-12.504 12.5-32.77 0-45.25zm0 0"></path>
                    </g>
                  </svg>
                  <svg class="checkmark" xml:space="preserve" style="enable-background:new 0 0 512 512" viewBox="0 0 24 24" y="0" x="0" height="10" width="10" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1" xmlns="http://www.w3.org/2000/svg">
                    <g>
                      <path class="" data-original="#000000" fill="currentColor" d="M9.707 19.121a.997.997 0 0 1-1.414 0l-5.646-5.647a1.5 1.5 0 0 1 0-2.121l.707-.707a1.5 1.5 0 0 1 2.121 0L9 14.171l9.525-9.525a1.5 1.5 0 0 1 2.121 0l.707.707a1.5 1.5 0 0 1 0 2.121z"></path>
                    </g>
                  </svg>
                </div>
              </div>
            </label>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showPermissionDialog = false">{{ $t('adminUsers.close') }}</el-button>
        <el-button v-if="permissionTarget?.role === 'admin'" type="primary" :loading="permissionSaving" @click="savePermissions">
          {{ $t('adminUsers.savePermissions') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { Plus, Search, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'
import RefreshButton from '../../components/RefreshButton.vue'

const { t } = useI18n()

const authStore = useAuthStore()

const users = ref([])
const loading = ref(false)
const keyword = ref('')
let searchDebounceTimer = null
function onKeywordInput() {
  clearTimeout(searchDebounceTimer)
  searchDebounceTimer = setTimeout(() => { loadUsers(1) }, 300)
}
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const submitting = ref(false)

// 新增/编辑
const showDialog = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const form = reactive({ username: '', password: '', role: 'admin', realName: '', email: '' })

// 重置密码
const showResetDialog = ref(false)
const resetTarget = ref(null)
const resetForm = reactive({ newPassword: '', confirmPassword: '' })

// 修改密码
const showPasswordDialog = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 权限编辑
const showPermissionDialog = ref(false)
const permissionTarget = ref(null)
const permissionsLoading = ref(false)
const permissionSaving = ref(false)
const permissionMatrix = ref([])
const groupedPermissions = ref({})

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 权限判断
function canEdit(row) {
  if (!authStore.isSuperAdmin && row.role === 'super_admin') return false
  return authStore.isSuperAdmin
}

function canToggle(row) {
  if (row.username === authStore.username) return false
  if (!authStore.isSuperAdmin && row.role === 'super_admin') return false
  return authStore.isSuperAdmin
}

function canDelete(row) {
  if (row.username === authStore.username) return false
  if (!authStore.isSuperAdmin && row.role === 'super_admin') return false
  return authStore.isSuperAdmin
}

function canResetPassword(row) {
  return authStore.isSuperAdmin
}

function canEditPermissions(row) {
  return authStore.isSuperAdmin && row.role === 'admin' && row.username !== authStore.username
}

// 加载用户列表
async function loadUsers(page = 1) {
  currentPage.value = page
  loading.value = true
  try {
    const params = { page: page - 1, size: pageSize.value }
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const data = await request.get('/admin/users', { params })
    users.value = data.content
    total.value = data.totalElements
  } catch {
    ElMessage.error(t('adminUsers.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 新增
function openCreate() {
  isEditing.value = false
  editingId.value = null
  form.username = ''
  form.password = ''
  form.role = 'admin'
  form.realName = ''
  form.email = ''
  showDialog.value = true
}

// 编辑
function openEdit(row) {
  isEditing.value = true
  editingId.value = row.id
  form.username = row.username
  form.password = ''
  form.role = row.role
  form.realName = row.realName || ''
  form.email = row.email || ''
  showDialog.value = true
}

// 提交新增/编辑
async function handleSubmit() {
  if (!isEditing.value) {
    if (!form.username.trim() || form.username.length < 3) {
      ElMessage.warning(t('adminUsers.usernameMinChars'))
      return
    }
    if (!form.password || form.password.length < 6) {
      ElMessage.warning(t('adminUsers.passwordMinChars'))
      return
    }
  }

  submitting.value = true
  try {
    if (isEditing.value) {
      await request.put(`/admin/users/${editingId.value}`, {
        realName: form.realName || null,
        email: form.email || null,
        role: form.role
      })
      ElMessage.success(t('adminUsers.updateSuccess'))
    } else {
      await request.post('/admin/users', {
        username: form.username,
        password: form.password,
        role: form.role,
        realName: form.realName || null,
        email: form.email || null
      })
      ElMessage.success(t('adminUsers.createSuccess'))
    }
    showDialog.value = false
    loadUsers(currentPage.value)
  } catch (e) {
    ElMessage.error(e.message || t('adminUsers.operationFailed'))
  } finally {
    submitting.value = false
  }
}

// 启用/禁用
async function handleToggle(row) {
  const action = row.enabled ? t('adminUsers.disable') : t('adminUsers.enable')
  try {
    await ElMessageBox.confirm(t('adminUsers.toggleConfirmMsg', { action, username: row.username }), t('adminUsers.toggleConfirmTitle'), { type: 'warning' })
    await request.put(`/admin/users/${row.id}/toggle-enabled`)
    ElMessage.success(t('adminUsers.toggleSuccess', { action }))
    loadUsers(currentPage.value)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || t('adminUsers.operationFailed'))
  }
}

// 删除
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(t('adminUsers.deleteConfirmMsg', { username: row.username }), t('adminUsers.deleteConfirmTitle'), { type: 'error' })
    await request.delete(`/admin/users/${row.id}`)
    ElMessage.success(t('adminUsers.deleteSuccess'))
    loadUsers(currentPage.value)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || t('adminUsers.operationFailed'))
  }
}

// 重置密码
function openResetPassword(row) {
  resetTarget.value = row
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  showResetDialog.value = true
}

async function handleResetPassword() {
  if (resetForm.newPassword.length < 6) {
    ElMessage.warning(t('adminUsers.passwordMinChars'))
    return
  }
  if (resetForm.newPassword !== resetForm.confirmPassword) {
    ElMessage.warning(t('adminUsers.passwordsNotMatch'))
    return
  }

  submitting.value = true
  try {
    await request.put(`/admin/users/${resetTarget.value.id}/reset-password`, {
      newPassword: resetForm.newPassword
    })
    ElMessage.success(t('adminUsers.passwordResetSuccess'))
    showResetDialog.value = false
  } catch (e) {
    ElMessage.error(e.message || t('adminUsers.operationFailed'))
  } finally {
    submitting.value = false
  }
}

// 修改自己的密码
async function handleChangePassword() {
  if (!passwordForm.oldPassword) {
    ElMessage.warning(t('adminUsers.enterOldPassword'))
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning(t('adminUsers.newPasswordMinChars'))
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning(t('adminUsers.passwordsNotMatch'))
    return
  }

  submitting.value = true
  try {
    await request.put('/admin/users/me/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success(t('adminUsers.passwordChangeSuccess'))
    showPasswordDialog.value = false
    await authStore.logout()
    window.location.href = '/admin/login'
  } catch (e) {
    ElMessage.error(e.message || t('adminUsers.operationFailed'))
  } finally {
    submitting.value = false
  }
}

// 权限编辑
async function openPermissions(row) {
  permissionTarget.value = row
  showPermissionDialog.value = true
  permissionsLoading.value = true
  permissionMatrix.value = []
  groupedPermissions.value = {}
  try {
    const data = await request.get(`/admin/permissions/users/${row.id}`)
    permissionMatrix.value = data
    // 按 group 分组
    const groups = {}
    for (const item of data) {
      if (!groups[item.group]) groups[item.group] = []
      groups[item.group].push(item)
    }
    groupedPermissions.value = groups
  } catch (e) {
    ElMessage.error(e.message || t('adminUsers.loadPermissionsFailed'))
    showPermissionDialog.value = false
  } finally {
    permissionsLoading.value = false
  }
}

function onPermissionChange(item) {
  // Track changed items (optimistic UI)
}

async function savePermissions() {
  permissionSaving.value = true
  try {
    const permissions = permissionMatrix.value.map(item => ({
      permission: item.code,
      enabled: item.enabled
    }))
    await request.put(`/admin/permissions/users/${permissionTarget.value.id}`, { permissions })
    ElMessage.success(t('adminUsers.permissionsUpdated'))
    showPermissionDialog.value = false
  } catch (e) {
    ElMessage.error(e.message || t('adminUsers.savePermissionsFailed'))
  } finally {
    permissionSaving.value = false
  }
}

onMounted(() => loadUsers())

onBeforeUnmount(() => { clearTimeout(searchDebounceTimer) })
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

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.action-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.action-btns .el-button {
  margin-left: 0;
}

.permission-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-light);
}

.perm-hint {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.permission-groups {
  max-height: 400px;
  overflow-y: auto;
}

.permission-group {
  margin-bottom: 16px;
}

.group-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
  color: var(--color-text);
  padding: 4px 0;
  border-bottom: 1px solid var(--color-border-light);
}

.permission-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}

.perm-label {
  font-size: 13px;
  color: var(--color-text);
}

/* Custom Switch */
.switch {
  --switch-width: 46px;
  --switch-height: 24px;
  --switch-bg: rgb(131, 131, 131);
  --switch-checked-bg: rgb(0, 218, 80);
  --switch-offset: calc((var(--switch-height) - var(--circle-diameter)) / 2);
  --switch-transition: all .2s cubic-bezier(0.27, 0.2, 0.25, 1.51);
  --circle-diameter: 18px;
  --circle-bg: #fff;
  --circle-shadow: 1px 1px 2px rgba(146, 146, 146, 0.45);
  --circle-checked-shadow: -1px 1px 2px rgba(163, 163, 163, 0.45);
  --circle-transition: var(--switch-transition);
  --icon-transition: all .2s cubic-bezier(0.27, 0.2, 0.25, 1.51);
  --icon-cross-color: var(--switch-bg);
  --icon-cross-size: 6px;
  --icon-checkmark-color: var(--switch-checked-bg);
  --icon-checkmark-size: 10px;
  --effect-width: calc(var(--circle-diameter) / 2);
  --effect-height: calc(var(--effect-width) / 2 - 1px);
  --effect-bg: var(--circle-bg);
  --effect-border-radius: 1px;
  --effect-transition: all .2s ease-in-out;
  display: inline-block;
}

.switch input {
  display: none;
}

.switch svg {
  transition: var(--icon-transition);
  position: absolute;
  height: auto;
}

.switch .checkmark {
  width: var(--icon-checkmark-size);
  color: var(--icon-checkmark-color);
  transform: scale(0);
}

.switch .cross {
  width: var(--icon-cross-size);
  color: var(--icon-cross-color);
}

.slider {
  box-sizing: border-box;
  width: var(--switch-width);
  height: var(--switch-height);
  background: var(--switch-bg);
  border-radius: 999px;
  display: flex;
  align-items: center;
  position: relative;
  transition: var(--switch-transition);
  cursor: pointer;
}

.circle {
  width: var(--circle-diameter);
  height: var(--circle-diameter);
  background: var(--circle-bg);
  border-radius: inherit;
  box-shadow: var(--circle-shadow);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--circle-transition);
  z-index: 1;
  position: absolute;
  left: var(--switch-offset);
}

.slider::before {
  content: "";
  position: absolute;
  width: var(--effect-width);
  height: var(--effect-height);
  left: calc(var(--switch-offset) + (var(--effect-width) / 2));
  background: var(--effect-bg);
  border-radius: var(--effect-border-radius);
  transition: var(--effect-transition);
}

.switch input:checked + .slider {
  background: var(--switch-checked-bg);
}

.switch input:checked + .slider .checkmark {
  transform: scale(1);
}

.switch input:checked + .slider .cross {
  transform: scale(0);
}

.switch input:checked + .slider::before {
  left: calc(100% - var(--effect-width) - (var(--effect-width) / 2) - var(--switch-offset));
}

.switch input:checked + .slider .circle {
  left: calc(100% - var(--circle-diameter) - var(--switch-offset));
  box-shadow: var(--circle-checked-shadow);
}

.switch.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.switch.disabled .slider {
  cursor: not-allowed;
}
</style>
