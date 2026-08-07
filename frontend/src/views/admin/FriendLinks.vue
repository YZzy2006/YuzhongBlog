<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>{{ $t('adminFriendLinks.title') }}</h2>
      <div class="header-actions">
        <RefreshButton :onRefresh="loadLinks" />
        <el-button v-if="authStore.hasPermission('project:manage')" type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> {{ $t('adminFriendLinks.create') }}
        </el-button>
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="showForm" :title="editingId ? $t('adminFriendLinks.edit') : $t('adminFriendLinks.create')" width="520px" destroy-on-close>
      <el-form label-position="top" @submit.prevent="handleSubmit">
        <el-form-item :label="$t('adminFriendLinks.nameLabel')">
          <el-input v-model="form.name" :placeholder="$t('adminFriendLinks.namePlaceholder')" clearable />
        </el-form-item>
        <el-form-item :label="$t('adminFriendLinks.urlLabel')">
          <el-input v-model="form.url" placeholder="https://example.com" clearable />
        </el-form-item>
        <el-form-item :label="$t('adminFriendLinks.avatarLabel')">
          <div class="avatar-upload-area">
            <div v-if="form.avatar" class="avatar-preview">
              <img :src="form.avatar" alt="avatar" />
              <el-button size="small" type="danger" text @click="form.avatar = ''">{{ $t('adminSettings.removeImage') }}</el-button>
            </div>
            <FileUpload v-else endpoint="/admin/upload/image" @uploaded="res => form.avatar = res.url" style="height: 120px" />
          </div>
        </el-form-item>
        <el-form-item :label="$t('adminFriendLinks.descLabel')">
          <el-input v-model="form.description" type="textarea" :rows="2" :placeholder="$t('adminFriendLinks.descPlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('adminFriendLinks.themeColorLabel')">
          <div class="color-picker-row">
            <input type="color" v-model="colorPicker" class="color-input" />
            <el-input v-model="form.themeColor" :placeholder="$t('adminFriendLinks.themeColorPlaceholder')" clearable />
            <div class="color-preview" :style="{ background: form.themeColor || '#3b82f6' }"></div>
          </div>
        </el-form-item>
        <el-form-item :label="$t('adminFriendLinks.sortOrderLabel')">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">{{ $t('adminFriendLinks.cancel') }}</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">{{ $t('adminFriendLinks.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- Table -->
    <el-card>
      <el-table :data="links" row-key="id" stripe v-loading="loading" :empty-text="$t('adminFriendLinks.emptyText')">
        <el-table-column :label="$t('adminFriendLinks.avatarCol')" width="60">
          <template #default="{ row }">
            <img v-if="row.avatar" :src="row.avatar" loading="lazy" class="table-avatar" />
            <span v-else class="table-avatar-fallback">{{ row.name?.charAt(0) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('adminFriendLinks.nameCol')" min-width="120" />
        <el-table-column prop="url" :label="$t('adminFriendLinks.urlCol')" min-width="200">
          <template #default="{ row }">
            <a :href="row.url" target="_blank" rel="noopener noreferrer" class="table-link">{{ row.url }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('adminFriendLinks.descCol')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="$t('adminFriendLinks.colorCol')" width="80">
          <template #default="{ row }">
            <span v-if="row.themeColor" class="color-dot" :style="{ background: row.themeColor }"></span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" :label="$t('adminFriendLinks.sortCol')" width="80" />
        <el-table-column :label="$t('adminFriendLinks.actionCol')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="authStore.hasPermission('project:manage')" type="primary" size="small" @click="openEdit(row)">{{ $t('adminFriendLinks.editBtn') }}</el-button>
            <el-button v-if="authStore.hasPermission('project:manage')" type="danger" size="small" :disabled="pendingIds.has(row.id)" @click="handleDelete(row)">{{ $t('adminFriendLinks.deleteBtn') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { Plus } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'
import RefreshButton from '../../components/RefreshButton.vue'
import FileUpload from '../../components/FileUpload.vue'
import { cachedFetch, invalidateCache } from '../../utils/cache'

const { t } = useI18n()
const authStore = useAuthStore()

const links = ref([])
const loading = ref(false)
const showForm = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const pendingIds = new Set()

const defaultForm = {
  name: '',
  url: '',
  avatar: '',
  description: '',
  themeColor: 'rgba(59, 130, 246, 0.5)',
  sortOrder: 0
}
const form = ref({ ...defaultForm })

const colorPicker = computed({
  get() {
    const c = form.value.themeColor || '#3b82f6'
    if (c.startsWith('#')) return c
    // Try to extract hex from rgba
    const m = c.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/)
    if (m) {
      const hex = '#' + [m[1], m[2], m[3]].map(n => parseInt(n).toString(16).padStart(2, '0')).join('')
      return hex
    }
    return '#3b82f6'
  },
  set(val) {
    form.value.themeColor = val
  }
})

async function loadLinks() {
  loading.value = true
  try {
    links.value = await cachedFetch('admin:friend-links', () => request.get('/admin/friend-links'))
  } catch {
    ElMessage.error(t('adminFriendLinks.loadFailed'))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { ...defaultForm }
  showForm.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.value = {
    name: row.name || '',
    url: row.url || '',
    avatar: row.avatar || '',
    description: row.description || '',
    themeColor: row.themeColor || 'rgba(59, 130, 246, 0.5)',
    sortOrder: row.sortOrder || 0
  }
  showForm.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) { ElMessage.warning(t('adminFriendLinks.nameRequired')); return }
  if (!form.value.url.trim()) { ElMessage.warning(t('adminFriendLinks.urlRequired')); return }
  try { new URL(form.value.url) } catch { ElMessage.warning(t('adminFriendLinks.invalidUrl')); return }
  submitting.value = true
  try {
    if (editingId.value) {
      await request.put(`/admin/friend-links/${editingId.value}`, form.value)
      ElMessage.success(t('adminFriendLinks.updateSuccess'))
    } else {
      await request.post('/admin/friend-links', form.value)
      ElMessage.success(t('adminFriendLinks.createSuccess'))
    }
    showForm.value = false
    invalidateCache(['admin:friend-links'])
    loadLinks()
  } catch (e) {
    ElMessage.error(e.message || t('adminFriendLinks.error'))
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  if (pendingIds.has(row.id)) return
  pendingIds.add(row.id)
  try {
    await ElMessageBox.confirm(
      t('adminFriendLinks.deleteConfirmMsg', { name: row.name }),
      t('adminFriendLinks.deleteConfirmTitle'),
      { type: 'warning' }
    )
    await request.delete(`/admin/friend-links/${row.id}`)
    ElMessage.success(t('adminFriendLinks.deleteSuccess'))
    invalidateCache(['admin:friend-links'])
    loadLinks()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || t('adminFriendLinks.error'))
  } finally {
    pendingIds.delete(row.id)
  }
}

onMounted(loadLinks)
</script>

<style scoped>
.table-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.table-avatar-fallback {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #3b82f6;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 700;
}

.table-link {
  color: #3b82f6;
  text-decoration: none;
  font-size: 0.8rem;
}

.table-link:hover {
  text-decoration: underline;
}

.color-dot {
  display: inline-block;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid rgba(0, 0, 0, 0.1);
}

.color-picker-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.color-input {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  padding: 0;
  background: none;
}

.color-preview {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 2px solid rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.avatar-upload-area {
  width: 100%;
}

.avatar-preview {
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
</style>
