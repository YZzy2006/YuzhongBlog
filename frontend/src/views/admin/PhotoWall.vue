<template>
  <div class="admin-page">
    <!-- Album list view -->
    <template v-if="!managingAlbum">
      <div class="page-toolbar">
        <h2 class="toolbar-title">{{ $t('adminPhotowall.title') }}</h2>
        <div class="header-actions">
          <RefreshButton :onRefresh="fetchAlbums" />
          <el-button type="primary" @click="openCreateDialog" :icon="Plus">{{ $t('adminPhotowall.createAlbum') }}</el-button>
        </div>
      </div>

      <el-card shadow="never">
        <el-table :data="albums" row-key="id" v-loading="loading" stripe style="width: 100%">
          <el-table-column :label="$t('adminPhotowall.cover')" width="100">
            <template #default="{ row }">
              <el-image v-if="row.coverUrl" :src="row.coverUrl" lazy
                style="width: 60px; height: 45px; border-radius: 6px" fit="cover" />
              <span v-else class="no-cover">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" :label="$t('adminPhotowall.name')" min-width="150" />
          <el-table-column :label="$t('adminPhotowall.photoCount')" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.photoCount || 0 }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('adminPhotowall.visible')" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.visible ? 'success' : 'info'" size="small">
                {{ row.visible ? $t('adminPhotowall.yes') : $t('adminPhotowall.no') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" :label="$t('adminPhotowall.sort')" width="80" align="center" />
          <el-table-column :label="$t('adminPhotowall.createdAt')" width="170">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column :label="$t('adminPhotowall.actions')" width="260" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="managePhotos(row)">
                {{ $t('adminPhotowall.managePhotos') }}
              </el-button>
              <el-button size="small" @click="openEditDialog(row)">
                {{ $t('adminPhotowall.edit') }}
              </el-button>
              <el-button size="small" type="danger" :disabled="pendingAlbumIds.has(row.id)" @click="deleteAlbum(row)">
                {{ $t('adminPhotowall.delete') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="totalPages > 1" class="pagination-wrap">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="totalElements"
            layout="prev, pager, next"
            @current-change="fetchAlbums"
          />
        </div>
      </el-card>
    </template>

    <!-- Photo management view -->
    <template v-else>
      <div class="page-toolbar">
        <div class="toolbar-left">
          <el-button @click="backToAlbums" :icon="ArrowLeft">{{ $t('adminPhotowall.backToList') }}</el-button>
          <h2 class="toolbar-title">{{ managingAlbum.name }} — {{ $t('adminPhotowall.photoManagement') }}</h2>
        </div>
      </div>

      <!-- Upload area -->
      <el-card shadow="never" style="margin-bottom: 16px">
        <template #header><span style="font-weight: 600; font-size: 14px">{{ $t('adminPhotowall.uploadPhotos') }}</span></template>
        <FileUpload endpoint="/admin/upload/image" accept="image/jpeg,image/png,image/gif,image/webp"
          multiple @uploaded="onPhotoUploaded" @all-uploaded="onAllUploaded" />
      </el-card>

      <!-- Photo grid -->
      <el-card shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span style="font-weight: 600; font-size: 14px">{{ $t('adminPhotowall.photoList') }} ({{ photos.length }})</span>
          </div>
        </template>

        <div v-if="photos.length > 0" class="photo-grid">
          <div v-for="photo in photos" :key="photo.id" class="photo-card">
            <el-image :src="photo.url" lazy fit="cover" class="photo-thumb" />
            <div class="photo-card-info">
              <el-input v-model="photo.caption" size="small" :placeholder="$t('adminPhotowall.captionPlaceholder')"
                @blur="updateCaption(photo)" />
            </div>
            <el-button class="photo-delete-btn" type="danger" size="small" circle
              :disabled="pendingPhotoIds.has(photo.id)" @click="deletePhoto(photo)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>

        <el-empty v-else :description="$t('adminPhotowall.noPhotos')" />
      </el-card>
    </template>

    <!-- Create/Edit Album Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingAlbum ? $t('adminPhotowall.editAlbum') : $t('adminPhotowall.createAlbum')"
      width="520px" destroy-on-close>
      <el-form :model="albumForm" label-position="top">
        <el-form-item :label="$t('adminPhotowall.name')" required>
          <el-input v-model="albumForm.name" :placeholder="$t('adminPhotowall.namePlaceholder')" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('adminPhotowall.description')">
          <el-input v-model="albumForm.description" type="textarea" :rows="3"
            :placeholder="$t('adminPhotowall.descPlaceholder')" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('adminPhotowall.coverImage')">
          <div v-if="albumForm.coverUrl" style="margin-bottom: 8px">
            <el-image :src="albumForm.coverUrl" style="width: 100%; max-height: 160px; border-radius: 6px" fit="cover" />
            <div style="margin-top: 8px">
              <el-button size="small" @click="albumForm.coverUrl = ''">{{ $t('adminPhotowall.changeCover') }}</el-button>
            </div>
          </div>
          <FileUpload v-else endpoint="/admin/upload/cover" accept="image/*" @uploaded="onCoverUploaded" style="height: 180px" />
        </el-form-item>
        <el-form-item :label="$t('adminPhotowall.sort')">
          <el-input-number v-model="albumForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item :label="$t('adminPhotowall.visible')">
          <el-switch v-model="albumForm.visible" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('adminPhotowall.cancel') }}</el-button>
        <el-button type="primary" @click="saveAlbum" :loading="saving">{{ $t('adminPhotowall.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { Plus, ArrowLeft, Delete } from '@element-plus/icons-vue'
import request from '../../utils/request'
import FileUpload from '../../components/FileUpload.vue'
import RefreshButton from '../../components/RefreshButton.vue'

const { t } = useI18n()

// Album list state
const loading = ref(false)
const albums = ref([])
const currentPage = ref(1)
const pageSize = 20
const totalElements = ref(0)
const totalPages = ref(0)

// Photo management state
const managingAlbum = ref(null)
const photos = ref([])

// Dialog state
const dialogVisible = ref(false)
const editingAlbum = ref(null)
const saving = ref(false)
const pendingAlbumIds = new Set()
const pendingPhotoIds = new Set()
const albumForm = ref({
  name: '',
  description: '',
  coverUrl: '',
  sortOrder: 0,
  visible: true
})

// ==================== Album CRUD ====================

async function fetchAlbums() {
  loading.value = true
  try {
    const data = await request.get('/admin/photowall/albums', {
      params: { page: currentPage.value - 1, size: pageSize }
    })
    albums.value = data.content || []
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages
  } catch (e) {
    ElMessage.error(t('adminPhotowall.fetchFailed'))
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingAlbum.value = null
  albumForm.value = { name: '', description: '', coverUrl: '', sortOrder: 0, visible: true }
  dialogVisible.value = true
}

function openEditDialog(album) {
  editingAlbum.value = album
  albumForm.value = {
    name: album.name,
    description: album.description || '',
    coverUrl: album.coverUrl || '',
    sortOrder: album.sortOrder || 0,
    visible: album.visible
  }
  dialogVisible.value = true
}

async function saveAlbum() {
  if (!albumForm.value.name.trim()) {
    ElMessage.warning(t('adminPhotowall.nameRequired'))
    return
  }
  saving.value = true
  try {
    if (editingAlbum.value) {
      await request.put(`/admin/photowall/albums/${editingAlbum.value.id}`, albumForm.value)
      ElMessage.success(t('adminPhotowall.albumUpdated'))
    } else {
      await request.post('/admin/photowall/albums', albumForm.value)
      ElMessage.success(t('adminPhotowall.albumCreated'))
    }
    dialogVisible.value = false
    fetchAlbums()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('adminPhotowall.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function deleteAlbum(album) {
  if (pendingAlbumIds.has(album.id)) return
  pendingAlbumIds.add(album.id)
  try {
    await ElMessageBox.confirm(
      t('adminPhotowall.deleteAlbumConfirm', { name: album.name }),
      t('adminPhotowall.deleteConfirmTitle'),
      { type: 'warning' }
    )
    await request.delete(`/admin/photowall/albums/${album.id}`)
    ElMessage.success(t('adminPhotowall.albumDeleted'))
    fetchAlbums()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminPhotowall.deleteFailed'))
  } finally {
    pendingAlbumIds.delete(album.id)
  }
}

// ==================== Photo Management ====================

async function managePhotos(album) {
  managingAlbum.value = album
  await fetchPhotos(album.id)
}

function backToAlbums() {
  managingAlbum.value = null
  photos.value = []
  fetchAlbums()
}

async function fetchPhotos(albumId) {
  try {
    const data = await request.get(`/admin/photowall/albums/${albumId}`)
    photos.value = data.photos || []
  } catch (e) {
    ElMessage.error(t('adminPhotowall.fetchPhotosFailed'))
  }
}

async function onPhotoUploaded(res) {
  const url = res?.url
  if (!url) return
  try {
    await request.post('/admin/photowall/photos', {
      albumId: managingAlbum.value.id,
      url: url,
      sortOrder: 0
    })
  } catch (e) {
    ElMessage.error(t('adminPhotowall.photoAddFailed'))
  }
}

function onAllUploaded(results) {
  if (results.length > 0) {
    ElMessage.success(t('adminPhotowall.photosUploaded', { count: results.length }))
    fetchPhotos(managingAlbum.value.id)
  }
}

async function deletePhoto(photo) {
  if (pendingPhotoIds.has(photo.id)) return
  pendingPhotoIds.add(photo.id)
  try {
    await ElMessageBox.confirm(
      t('adminPhotowall.deletePhotoConfirm'),
      t('adminPhotowall.deleteConfirmTitle'),
      { type: 'warning' }
    )
    await request.delete(`/admin/photowall/photos/${photo.id}`)
    ElMessage.success(t('adminPhotowall.photoDeleted'))
    await fetchPhotos(managingAlbum.value.id)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminPhotowall.deleteFailed'))
  } finally {
    pendingPhotoIds.delete(photo.id)
  }
}

async function updateCaption(photo) {
  try {
    await request.put(`/admin/photowall/photos/${photo.id}/caption`, { caption: photo.caption || '' })
  } catch (e) {
    ElMessage.error(t('adminPhotowall.saveFailed'))
  }
}

function onCoverUploaded(res) {
  const url = res?.url
  if (url) {
    albumForm.value.coverUrl = url
  }
}

// ==================== Helpers ====================

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

onMounted(() => {
  fetchAlbums()
})
</script>

<style scoped>
.admin-page {
  padding: 0;
}

.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  color: #1e293b;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.no-cover {
  color: #94a3b8;
  font-size: 12px;
}

/* Photo grid */
.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.photo-card {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.4);
  transition: box-shadow 0.3s, transform 0.3s;
}

.photo-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.photo-thumb {
  width: 100%;
  height: 140px;
}

.photo-card-info {
  padding: 8px;
}

.photo-delete-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  opacity: 0;
  transition: opacity 0.2s;
}

.photo-card:hover .photo-delete-btn {
  opacity: 1;
}
</style>
