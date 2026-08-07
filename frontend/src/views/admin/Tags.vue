<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>{{ $t('adminTags.title') }}</h2>
      <div class="header-actions">
        <RefreshButton :onRefresh="loadTags" />
        <el-button v-if="authStore.hasPermission('tag:manage')" type="primary" @click="showAdd = true">
          <el-icon><Plus /></el-icon> {{ $t('adminTags.createTag') }}
        </el-button>
      </div>
    </div>

    <el-dialog v-model="showAdd" :title="$t('adminTags.createTag')" width="420px" destroy-on-close>
      <el-form label-position="top" @submit.prevent="handleAdd">
        <el-form-item :label="$t('adminTags.tagName')">
          <el-input v-model="newName" :placeholder="$t('adminTags.tagNamePlaceholder')" clearable @keyup.enter="handleAdd" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">{{ $t('adminTags.cancel') }}</el-button>
        <el-button type="primary" @click="handleAdd">{{ $t('adminTags.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-card>
      <el-table :data="tags" row-key="id" stripe v-loading="loading" :empty-text="$t('adminTags.emptyText')">
        <el-table-column prop="name" :label="$t('adminTags.name')" />
        <el-table-column prop="createdAt" :label="$t('adminTags.createdAt')" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('adminTags.action')" width="200">
          <template #default="{ row }">
            <template v-if="editingId !== row.id">
              <el-button v-if="authStore.hasPermission('tag:manage')" type="primary" size="small" @click="startEdit(row)">{{ $t('adminTags.edit') }}</el-button>
              <el-button v-if="authStore.hasPermission('tag:manage')" type="danger" size="small" :disabled="pendingIds.has(row.id)" @click="handleDelete(row)">{{ $t('adminTags.delete') }}</el-button>
            </template>
            <template v-else>
              <el-input v-model="editName" size="small" style="width: 120px; margin-right: 8px" @keyup.enter="handleUpdate(row)" />
              <el-button type="primary" size="small" :disabled="pendingIds.has(row.id)" @click="handleUpdate(row)">{{ $t('adminTags.save') }}</el-button>
              <el-button size="small" @click="editingId = null">{{ $t('adminTags.cancel') }}</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { Plus } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'
import RefreshButton from '../../components/RefreshButton.vue'
import { cachedFetch, invalidateCache } from '../../utils/cache'

const { t } = useI18n()
const authStore = useAuthStore()

const tags = ref([])
const loading = ref(false)
const showAdd = ref(false)
const newName = ref('')
const editingId = ref(null)
const editName = ref('')
const pendingIds = new Set()
const saving = ref(false)

async function loadTags() {
  loading.value = true
  try {
    tags.value = await cachedFetch('admin:tags', () => request.get('/admin/tags'))
  } catch {
    ElMessage.error(t('adminTags.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleAdd() {
  if (!newName.value.trim()) return
  if (saving.value) return
  saving.value = true
  try {
    await request.post('/admin/tags', { name: newName.value })
    newName.value = ''
    showAdd.value = false
    ElMessage.success(t('adminTags.addSuccess'))
    invalidateCache(['admin:tags'])
    loadTags()
  } catch (e) {
    ElMessage.error(t('adminTags.addFailed', { error: e.message || t('adminTags.unknownError') }))
  } finally {
    saving.value = false
  }
}

function startEdit(tag) {
  editingId.value = tag.id
  editName.value = tag.name
}

async function handleUpdate(tag) {
  if (!editName.value.trim()) return
  if (pendingIds.has(tag.id)) return
  pendingIds.add(tag.id)
  try {
    await request.put(`/admin/tags/${tag.id}`, { name: editName.value })
    editingId.value = null
    ElMessage.success(t('adminTags.updateSuccess'))
    invalidateCache(['admin:tags'])
    loadTags()
  } catch (e) {
    ElMessage.error(t('adminTags.updateFailed', { error: e.message || t('adminTags.unknownError') }))
  } finally {
    pendingIds.delete(tag.id)
  }
}

async function handleDelete(tag) {
  if (pendingIds.has(tag.id)) return
  pendingIds.add(tag.id)
  try {
    await ElMessageBox.confirm(t('adminTags.deleteConfirmMsg', { name: tag.name }), t('adminTags.deleteConfirmTitle'), { type: 'warning' })
    await request.delete(`/admin/tags/${tag.id}`)
    ElMessage.success(t('adminTags.deleteSuccess'))
    invalidateCache(['admin:tags'])
    loadTags()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminTags.deleteFailed', { error: e.message || t('adminTags.unknownError') }))
  } finally {
    pendingIds.delete(tag.id)
  }
}

function formatDate(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 16)
}

onMounted(loadTags)
</script>
