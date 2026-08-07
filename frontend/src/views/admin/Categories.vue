<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>{{ $t('adminCategories.title') }}</h2>
      <div class="header-actions">
        <RefreshButton :onRefresh="loadCategories" />
        <el-button v-if="authStore.hasPermission('category:manage')" type="primary" @click="showAdd = true">
          <el-icon><Plus /></el-icon> {{ $t('adminCategories.createCategory') }}
        </el-button>
      </div>
    </div>

    <el-dialog v-model="showAdd" :title="$t('adminCategories.createCategory')" width="420px" destroy-on-close>
      <el-form label-position="top" @submit.prevent="handleAdd">
        <el-form-item :label="$t('adminCategories.categoryName')">
          <el-input v-model="newName" :placeholder="$t('adminCategories.categoryNamePlaceholder')" clearable @keyup.enter="handleAdd" />
        </el-form-item>
        <el-form-item :label="$t('adminCategories.sortOrder')">
          <el-input-number v-model="newSort" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">{{ $t('adminCategories.cancel') }}</el-button>
        <el-button type="primary" @click="handleAdd">{{ $t('adminCategories.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-card>
      <el-table :data="categories" row-key="id" stripe v-loading="loading" :empty-text="$t('adminCategories.emptyText')">
        <el-table-column prop="name" :label="$t('adminCategories.name')">
          <template #default="{ row }">
            <span v-if="editingId !== row.id">{{ row.name }}</span>
            <el-input v-else v-model="editForm.name" size="small" style="width: 160px" @keyup.enter="handleUpdate(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" :label="$t('adminCategories.sortOrder')" width="120">
          <template #default="{ row }">
            <span v-if="editingId !== row.id">{{ row.sortOrder }}</span>
            <el-input-number v-else v-model="editForm.sortOrder" :min="0" controls-position="right" size="small" style="width: 80px" @keyup.enter="handleUpdate(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="$t('adminCategories.createdAt')" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('adminCategories.action')" width="200">
          <template #default="{ row }">
            <template v-if="editingId !== row.id">
              <el-button v-if="authStore.hasPermission('category:manage')" type="primary" size="small" @click="startEdit(row)">{{ $t('adminCategories.edit') }}</el-button>
              <el-button v-if="authStore.hasPermission('category:manage')" type="danger" size="small" :disabled="pendingIds.has(row.id)" @click="handleDelete(row)">{{ $t('adminCategories.delete') }}</el-button>
            </template>
            <template v-else>
              <el-button type="primary" size="small" :disabled="pendingIds.has(row.id)" @click="handleUpdate(row)">{{ $t('adminCategories.save') }}</el-button>
              <el-button size="small" @click="editingId = null">{{ $t('adminCategories.cancel') }}</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
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

const categories = ref([])
const loading = ref(false)
const showAdd = ref(false)
const newName = ref('')
const newSort = ref(0)
const editingId = ref(null)
const editForm = reactive({ name: '', sortOrder: 0 })
const pendingIds = new Set()
const saving = ref(false)

async function loadCategories() {
  loading.value = true
  try {
    categories.value = await cachedFetch('admin:categories', () => request.get('/admin/categories'))
  } finally {
    loading.value = false
  }
}

async function handleAdd() {
  if (!newName.value.trim()) return
  if (saving.value) return
  saving.value = true
  try {
    await request.post('/admin/categories', { name: newName.value, sortOrder: newSort.value || 0 })
    newName.value = ''
    newSort.value = 0
    showAdd.value = false
    ElMessage.success(t('adminCategories.addSuccess'))
    invalidateCache(['admin:categories'])
    loadCategories()
  } catch (e) {
    ElMessage.error(t('adminCategories.addFailed', { error: e.message || t('adminCategories.unknownError') }))
  } finally {
    saving.value = false
  }
}

function startEdit(cat) {
  editingId.value = cat.id
  editForm.name = cat.name
  editForm.sortOrder = cat.sortOrder
}

async function handleUpdate(cat) {
  if (!editForm.name.trim()) return
  if (pendingIds.has(cat.id)) return
  pendingIds.add(cat.id)
  try {
    await request.put(`/admin/categories/${cat.id}`, { name: editForm.name, sortOrder: editForm.sortOrder })
    editingId.value = null
    ElMessage.success(t('adminCategories.updateSuccess'))
    invalidateCache(['admin:categories'])
    loadCategories()
  } catch (e) {
    ElMessage.error(t('adminCategories.updateFailed', { error: e.message || t('adminCategories.unknownError') }))
  } finally {
    pendingIds.delete(cat.id)
  }
}

async function handleDelete(cat) {
  if (pendingIds.has(cat.id)) return
  pendingIds.add(cat.id)
  try {
    await ElMessageBox.confirm(t('adminCategories.deleteConfirmMsg', { name: cat.name }), t('adminCategories.deleteConfirmTitle'), { type: 'warning' })
    await request.delete(`/admin/categories/${cat.id}`)
    ElMessage.success(t('adminCategories.deleteSuccess'))
    invalidateCache(['admin:categories'])
    loadCategories()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminCategories.deleteFailed', { error: e.message || t('adminCategories.unknownError') }))
  } finally {
    pendingIds.delete(cat.id)
  }
}

function formatDate(d) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 16)
}

onMounted(loadCategories)
</script>
