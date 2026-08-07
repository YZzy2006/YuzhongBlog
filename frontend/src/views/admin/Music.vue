<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>{{ $t('adminMusic.pageTitle') }}</h2>
      <el-button type="primary" @click="showAddDialog = true">
        <el-icon><Plus /></el-icon> {{ $t('adminMusic.addSong') }}
      </el-button>
    </div>

    <!-- Stats bar -->
    <div class="music-stats">
      <div class="stat-item">
        <span class="stat-num">{{ songs.length }}</span>
        <span class="stat-label">{{ $t('adminMusic.totalSongs') }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-num">{{ bilibiliSongs.length }}</span>
        <span class="stat-label">{{ $t('adminMusic.totalBilibiliSongs') }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-num">{{ videos.length }}</span>
        <span class="stat-label">{{ $t('adminMusic.totalVideos') }}</span>
      </div>
    </div>

    <!-- NetEase Song list -->
    <el-card v-loading="loading">
      <div v-if="songs.length === 0" class="empty-state">
        <div class="empty-icon">&#127925;</div>
        <p>{{ $t('adminMusic.emptyText') }}</p>
        <el-button type="primary" @click="showAddDialog = true">{{ $t('adminMusic.addFirst') }}</el-button>
      </div>

      <TransitionGroup v-else name="song-list" tag="div" class="song-grid">
        <div
          v-for="(song, index) in songs"
          :key="song.id"
          class="song-card"
        >
          <div class="song-card-cover" @click="openCoverPicker('netease', song)">
            <img v-if="song.coverUrl" :src="song.coverUrl" :alt="song.name" loading="lazy" />
            <div v-else class="song-card-cover-placeholder">&#127925;</div>
            <span class="song-card-index">{{ index + 1 }}</span>
            <div class="cover-overlay">
              <el-icon :size="20"><Camera /></el-icon>
            </div>
          </div>
          <div class="song-card-body">
            <div class="song-card-info">
              <h4 v-if="isEditing('netease', song.id, 'name')" class="song-card-title">
                <input
                  :ref="el => { if (el) el.focus() }"
                  class="inline-edit-input"
                  :value="editing.value"
                  @input="editing.value = $event.target.value"
                  @keydown.enter="saveMeta('netease', song)"
                  @keydown.escape="cancelEdit"
                  @blur="saveMeta('netease', song)"
                />
              </h4>
              <h4 v-else class="song-card-title editable" @click="startEdit('netease', song.id, 'name', song.name)">{{ song.name }}</h4>
              <p v-if="isEditing('netease', song.id, 'artist')" class="song-card-artist">
                <input
                  :ref="el => { if (el) el.focus() }"
                  class="inline-edit-input"
                  :value="editing.value"
                  @input="editing.value = $event.target.value"
                  @keydown.enter="saveMeta('netease', song)"
                  @keydown.escape="cancelEdit"
                  @blur="saveMeta('netease', song)"
                />
              </p>
              <p v-else class="song-card-artist editable" @click="startEdit('netease', song.id, 'artist', song.artist)">{{ song.artist }}</p>
            </div>
            <div class="song-card-actions">
              <el-tooltip :content="$t('adminMusic.moveUp')" placement="top" :show-after="300">
                <el-button
                  :icon="ArrowUp"
                  size="small"
                  circle
                  :disabled="index === 0"
                  @click="moveSong(index, -1)"
                />
              </el-tooltip>
              <el-tooltip :content="$t('adminMusic.moveDown')" placement="top" :show-after="300">
                <el-button
                  :icon="ArrowDown"
                  size="small"
                  circle
                  :disabled="index === songs.length - 1"
                  @click="moveSong(index, 1)"
                />
              </el-tooltip>
              <el-tooltip :content="$t('adminMusic.remove')" placement="top" :show-after="300">
                <el-button
                  :icon="Delete"
                  size="small"
                  circle
                  type="danger"
                  @click="removeSong(song)"
                />
              </el-tooltip>
            </div>
          </div>
        </div>
      </TransitionGroup>
    </el-card>

    <!-- Bilibili Songs list -->
    <el-card v-loading="bilibiliSongLoading" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>{{ $t('adminMusic.bilibiliSongsSection') }}</span>
          <el-button size="small" :loading="refreshing" @click="refreshBilibiliMeta">{{ $t('adminMusic.refreshMeta') }}</el-button>
        </div>
      </template>
      <div v-if="bilibiliSongs.length === 0" class="empty-state">
        <div class="empty-icon">&#127925;</div>
        <p>{{ $t('adminMusic.noBilibiliSongs') }}</p>
      </div>

      <TransitionGroup v-else name="song-list" tag="div" class="song-grid">
        <div
          v-for="(song, index) in bilibiliSongs"
          :key="song.id"
          class="song-card"
        >
          <div class="song-card-cover" @click="openCoverPicker('bilibili', song)">
            <img v-if="song.customCoverUrl || song.coverUrl" :src="song.customCoverUrl || song.coverUrl" :alt="song.title" loading="lazy" />
            <div v-else class="song-card-cover-placeholder">&#127916;</div>
            <span class="song-card-index">{{ index + 1 }}</span>
            <div class="cover-overlay">
              <el-icon :size="20"><Camera /></el-icon>
            </div>
          </div>
          <div class="song-card-body">
            <div class="song-card-info">
              <h4 v-if="isEditing('bilibili', song.id, 'title')" class="song-card-title">
                <input
                  :ref="el => { if (el) el.focus() }"
                  class="inline-edit-input"
                  :value="editing.value"
                  @input="editing.value = $event.target.value"
                  @keydown.enter="saveMeta('bilibili', song)"
                  @keydown.escape="cancelEdit"
                  @blur="saveMeta('bilibili', song)"
                />
              </h4>
              <h4 v-else class="song-card-title editable" @click="startEdit('bilibili', song.id, 'title', song.title)">{{ song.title }}</h4>
              <p v-if="isEditing('bilibili', song.id, 'artist')" class="song-card-artist">
                <input
                  :ref="el => { if (el) el.focus() }"
                  class="inline-edit-input"
                  :value="editing.value"
                  @input="editing.value = $event.target.value"
                  @keydown.enter="saveMeta('bilibili', song)"
                  @keydown.escape="cancelEdit"
                  @blur="saveMeta('bilibili', song)"
                />
              </p>
              <p v-else class="song-card-artist editable" @click="startEdit('bilibili', song.id, 'artist', song.artist)">{{ song.artist }}</p>
            </div>
            <div class="song-card-actions">
              <el-tooltip :content="$t('adminMusic.moveUp')" placement="top" :show-after="300">
                <el-button
                  :icon="ArrowUp"
                  size="small"
                  circle
                  :disabled="index === 0"
                  @click="moveBilibiliSong(index, -1)"
                />
              </el-tooltip>
              <el-tooltip :content="$t('adminMusic.moveDown')" placement="top" :show-after="300">
                <el-button
                  :icon="ArrowDown"
                  size="small"
                  circle
                  :disabled="index === bilibiliSongs.length - 1"
                  @click="moveBilibiliSong(index, 1)"
                />
              </el-tooltip>
              <el-tooltip :content="$t('adminMusic.removeBilibiliSong')" placement="top" :show-after="300">
                <el-button
                  :icon="Delete"
                  size="small"
                  circle
                  type="danger"
                  @click="removeBilibiliSong(song)"
                />
              </el-tooltip>
            </div>
          </div>
        </div>
      </TransitionGroup>
    </el-card>

    <!-- Video list -->
    <el-card v-loading="videoLoading" style="margin-top: 16px">
      <template #header>
        <div class="card-header"><span>{{ $t('adminMusic.videoSection') }}</span></div>
      </template>
      <div v-if="videos.length === 0" class="empty-state">
        <div class="empty-icon">&#127916;</div>
        <p>{{ $t('adminMusic.noVideos') }}</p>
      </div>

      <TransitionGroup v-else name="song-list" tag="div" class="song-grid">
        <div
          v-for="(video, index) in videos"
          :key="video.bvid"
          class="song-card"
        >
          <div class="song-card-cover">
            <img v-if="video.cover" :src="proxyCoverUrl(video.cover)" :alt="video.title" loading="lazy" />
            <div v-else class="song-card-cover-placeholder">&#127916;</div>
            <span class="song-card-index">{{ index + 1 }}</span>
          </div>
          <div class="song-card-body">
            <div class="song-card-info">
              <h4 v-if="isEditing('video', video.bvid, 'title')" class="song-card-title">
                <input
                  :ref="el => { if (el) el.focus() }"
                  class="inline-edit-input"
                  :value="editing.value"
                  @input="editing.value = $event.target.value"
                  @keydown.enter="saveVideoMeta(video)"
                  @keydown.escape="cancelEdit"
                  @blur="saveVideoMeta(video)"
                />
              </h4>
              <h4 v-else class="song-card-title editable" @click="startEdit('video', video.bvid, 'title', video.title)">{{ video.title }}</h4>
              <p v-if="isEditing('video', video.bvid, 'author')" class="song-card-artist">
                <input
                  :ref="el => { if (el) el.focus() }"
                  class="inline-edit-input"
                  :value="editing.value"
                  @input="editing.value = $event.target.value"
                  @keydown.enter="saveVideoMeta(video)"
                  @keydown.escape="cancelEdit"
                  @blur="saveVideoMeta(video)"
                />
              </p>
              <p v-else class="song-card-artist editable" @click="startEdit('video', video.bvid, 'author', video.author)">{{ video.author }}</p>
            </div>
            <div class="song-card-actions">
              <el-tooltip :content="$t('adminMusic.removeVideo')" placement="top" :show-after="300">
                <el-button
                  :icon="Delete"
                  size="small"
                  circle
                  type="danger"
                  @click="removeVideo(video)"
                />
              </el-tooltip>
            </div>
          </div>
        </div>
      </TransitionGroup>
    </el-card>

    <!-- Add song dialog -->
    <el-dialog
      v-model="showAddDialog"
      :title="$t('adminMusic.addDialogTitle')"
      width="480px"
      destroy-on-close
      @close="resetAddDialog"
    >
      <el-form @submit.prevent="handleQuery">
        <el-form-item :label="$t('adminMusic.linkLabel')">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-input
              v-model="addForm.id"
              :placeholder="$t('adminMusic.linkPlaceholder')"
              clearable
              @input="addPreview = null; queryError = ''"
            />
            <el-button
              v-if="!isBilibiliInput"
              type="primary"
              @click="handleQuery"
              :loading="querying"
              :disabled="!addForm.id.trim()"
            >{{ $t('adminMusic.query') || '查询' }}</el-button>
          </div>
          <div class="form-hint">{{ $t('adminMusic.idHint') }}</div>
          <div class="form-hint">{{ $t('adminMusic.bilibiliHint') }}</div>
        </el-form-item>
      </el-form>

      <!-- Query error -->
      <div v-if="queryError" class="query-error">
        <span>&#9888; {{ queryError }}</span>
      </div>

      <!-- Preview -->
      <div v-if="addPreview" class="add-preview">
        <div class="add-preview-cover">
          <img v-if="addPreview.coverUrl" :src="addPreview.coverUrl" alt="cover" />
          <div v-else class="add-preview-placeholder">&#127925;</div>
        </div>
        <div class="add-preview-info">
          <h4>{{ addPreview.name }}</h4>
          <p>{{ addPreview.artist }}</p>
          <span v-if="albumSongs.length > 0" class="add-preview-badge" style="color: #3b82f6;">
            &#127925; {{ albumSongs.length }} {{ $t('adminMusic.songsCount') || '首歌曲' }}
          </span>
          <span v-else class="add-preview-badge">&#10003; {{ $t('adminMusic.verified') || '已验证' }}</span>
        </div>
      </div>

      <template #footer>
        <el-button @click="showAddDialog = false">{{ $t('adminMusic.cancel') }}</el-button>
        <el-button
          v-if="isBilibiliInput"
          type="primary"
          @click="showBilibiliChoice = true"
          :loading="adding"
          :disabled="!addForm.id.trim()"
        >{{ $t('adminMusic.confirmAdd') }}</el-button>
        <el-button
          v-else
          type="primary"
          @click="handleAdd"
          :loading="adding"
          :disabled="!addPreview"
        >{{ $t('adminMusic.confirmAdd') }}</el-button>
      </template>
    </el-dialog>

    <!-- Bilibili add type choice dialog -->
    <el-dialog
      v-model="showBilibiliChoice"
      :title="$t('adminMusic.chooseAddType')"
      width="360px"
      :close-on-click-modal="false"
      append-to-body
    >
      <div style="display: flex; gap: 12px; justify-content: center; padding: 12px 0">
        <el-button size="large" @click="handleAddBilibili('video')">
          &#127916; {{ $t('adminMusic.addToVideo') }}
        </el-button>
        <el-button size="large" type="primary" @click="handleAddBilibili('song')">
          &#127925; {{ $t('adminMusic.addToSong') }}
        </el-button>
      </div>
    </el-dialog>

    <!-- Cover crop dialog -->
    <CircularCropDialog
      v-model="showCropDialog"
      :image-file="cropImageFile"
      @cropped="onCoverCropped"
    />

    <!-- Hidden file input for cover upload -->
    <input
      ref="coverFileInput"
      type="file"
      accept="image/*"
      style="display: none"
      @change="onCoverFileSelected"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { Plus, Delete, ArrowUp, ArrowDown, Camera } from '@element-plus/icons-vue'
import request from '../../utils/request'
import CircularCropDialog from '../../components/CircularCropDialog.vue'

const { t } = useI18n()

const songs = ref([])
const loading = ref(false)
const videos = ref([])
const videoLoading = ref(false)
const bilibiliSongs = ref([])
const bilibiliSongLoading = ref(false)
const refreshing = ref(false)
const showAddDialog = ref(false)
const addForm = ref({ id: '' })
const addPreview = ref(null)
const adding = ref(false)
const querying = ref(false)
const queryError = ref('')
const albumSongs = ref([])
const showBilibiliChoice = ref(false)

// Cover upload state
const showCropDialog = ref(false)
const cropImageFile = ref(null)
const coverFileInput = ref(null)
let coverTarget = null // { type: 'netease'|'bilibili', song: {...} }

// Inline edit state
const editing = ref({ type: null, songId: null, field: null, value: '' })
let savingMeta = false

function isEditing(type, songId, field) {
  return editing.value.type === type && editing.value.songId === songId && editing.value.field === field
}

function startEdit(type, songId, field, currentValue) {
  editing.value = { type, songId, field, value: currentValue || '' }
}

function cancelEdit() {
  editing.value = { type: null, songId: null, field: null, value: '' }
}

async function saveMeta(type, song) {
  if (savingMeta) return
  savingMeta = true
  const { field, value } = editing.value
  const trimmed = value.trim()
  if (!trimmed) {
    cancelEdit()
    savingMeta = false
    return
  }
  // Check if changed
  const original = type === 'netease' ? song[field] : song[field]
  if (trimmed === original) {
    cancelEdit()
    savingMeta = false
    return
  }
  try {
    if (type === 'netease') {
      const body = { name: song.name, artist: song.artist }
      body[field] = trimmed
      await request.put(`/admin/music/songs/${song.id}/meta`, body)
      song[field] = trimmed
    } else {
      const body = { name: song.title, artist: song.artist }
      if (field === 'title') body.name = trimmed
      else body[field] = trimmed
      await request.put(`/admin/music/bilibili-songs/${song.id}/meta`, body)
      song[field] = trimmed
    }
    ElMessage.success(t('adminMusic.metaUpdateSuccess') || '已更新')
  } catch (e) {
    ElMessage.error(e.message || t('adminMusic.metaUpdateFailed') || '更新失败')
  }
  cancelEdit()
  savingMeta = false
}

async function saveVideoMeta(video) {
  if (savingMeta) return
  savingMeta = true
  const { field, value } = editing.value
  const trimmed = value.trim()
  if (!trimmed) {
    cancelEdit()
    savingMeta = false
    return
  }
  const original = video[field]
  if (trimmed === original) {
    cancelEdit()
    savingMeta = false
    return
  }
  try {
    const body = { name: video.title, author: video.author }
    if (field === 'title') body.name = trimmed
    else body.author = trimmed
    await request.put(`/admin/music/videos/${video.bvid}/meta`, body)
    video[field] = trimmed
    ElMessage.success(t('adminMusic.metaUpdateSuccess') || '已更新')
  } catch (e) {
    ElMessage.error(e.message || t('adminMusic.metaUpdateFailed') || '更新失败')
  }
  cancelEdit()
  savingMeta = false
}

const isBilibiliInput = computed(() => /BV[a-zA-Z0-9]+/.test(addForm.value.id))

function resetAddDialog() {
  addForm.value.id = ''
  addPreview.value = null
  queryError.value = ''
  albumSongs.value = []
}

async function handleQuery() {
  const idStr = addForm.value.id.trim()
  if (!idStr) return
  if (isBilibiliInput.value) {
    showBilibiliChoice.value = true
    return
  }
  const id = Number(idStr)
  if (isNaN(id) || id <= 0) {
    ElMessage.warning(t('adminMusic.invalidId'))
    return
  }

  queryError.value = ''
  addPreview.value = null
  albumSongs.value = []
  querying.value = true
  try {
    const result = await request.get(`/admin/music/songs/query/${id}`)
    if (result && result.type === 'song') {
      addPreview.value = result.data
    } else if (result && result.type === 'album') {
      albumSongs.value = result.songs || []
      addPreview.value = {
        name: result.albumName || t('adminMusic.album'),
        artist: result.artistName || '',
        coverUrl: result.coverUrl || ''
      }
    } else {
      queryError.value = t('adminMusic.songNotFound')
    }
  } catch (e) {
    const msg = e.message || ''
    if (msg.includes('NOT_FOUND')) {
      queryError.value = t('adminMusic.songNotFound')
    } else {
      queryError.value = msg || t('adminMusic.songNotFound')
    }
  } finally {
    querying.value = false
  }
}

async function loadSongs() {
  loading.value = true
  try {
    const data = await request.get('/admin/music/songs')
    songs.value = data || []
  } catch (e) {
    ElMessage.error(e.message || t('adminMusic.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function loadBilibiliSongs() {
  bilibiliSongLoading.value = true
  try {
    const data = await request.get('/admin/music/bilibili-songs')
    bilibiliSongs.value = data || []
  } catch {
    bilibiliSongs.value = []
    ElMessage.error(t('adminMusic.loadBilibiliFailed'))
  } finally {
    bilibiliSongLoading.value = false
  }
}

async function refreshBilibiliMeta() {
  refreshing.value = true
  try {
    const data = await request.post('/admin/music/bilibili-songs/refresh')
    ElMessage.success(t('adminMusic.refreshSuccess', { count: data.updated }))
    await loadBilibiliSongs()
  } catch {
    ElMessage.error(t('adminMusic.refreshFailed'))
  } finally {
    refreshing.value = false
  }
}

function extractBvid(input) {
  const bvMatch = input.match(/(BV[a-zA-Z0-9]+)/)
  return bvMatch ? bvMatch[1] : null
}

async function handleAdd() {
  const idStr = addForm.value.id.trim()
  if (!idStr) return

  const id = Number(idStr)
  if (isNaN(id) || id <= 0) {
    ElMessage.warning(t('adminMusic.invalidId'))
    return
  }
  if (!addPreview.value) {
    ElMessage.warning(t('adminMusic.queryFirst'))
    return
  }

  adding.value = true
  try {
    if (albumSongs.value.length > 0) {
      const existingIds = new Set(songs.value.map(s => s.id))
      const newIds = albumSongs.value.map(s => s.id).filter(sid => !existingIds.has(sid))
      if (newIds.length === 0) {
        ElMessage.warning(t('adminMusic.allAlreadyExist') || '所有歌曲都已在列表中')
        return
      }
      await request.post('/admin/music/songs/batch', newIds)
      ElMessage.success(t('adminMusic.batchAddSuccess', { count: newIds.length }))
    } else {
      if (songs.value.some(s => s.id === id)) {
        ElMessage.warning(t('adminMusic.alreadyExists'))
        return
      }
      await request.post(`/admin/music/songs?id=${id}`)
      ElMessage.success(t('adminMusic.addSuccess'))
    }
    showAddDialog.value = false
    addForm.value.id = ''
    addPreview.value = null
    albumSongs.value = []
    await loadSongs()
  } catch (e) {
    ElMessage.error(e.message || t('adminMusic.addFailed'))
  } finally {
    adding.value = false
  }
}

async function handleAddBilibili(type) {
  const idStr = addForm.value.id.trim()
  if (!idStr) return
  const bvid = extractBvid(idStr)
  if (!bvid) return

  showBilibiliChoice.value = false
  adding.value = true

  if (type === 'video') {
    try {
      const allSettings = await request.get('/admin/settings')
      const currentIds = (allSettings.video_ids || '').split(',').map(s => s.trim()).filter(Boolean)
      if (currentIds.includes(bvid)) {
        ElMessage.warning(t('adminMusic.videoAlreadyExists'))
        return
      }
      currentIds.push(bvid)
      await request.put('/admin/settings', { settings: { video_ids: currentIds.join(',') } })
      ElMessage.success(t('adminMusic.videoAddSuccess'))
      showAddDialog.value = false
      addForm.value.id = ''
      await loadVideos()
    } catch (e) {
      ElMessage.error(e.message || t('adminMusic.addFailed'))
    } finally {
      adding.value = false
    }
  } else {
    try {
      await request.post(`/admin/music/bilibili-songs?bvid=${bvid}`)
      ElMessage.success(t('adminMusic.bilibiliSongAddSuccess'))
      showAddDialog.value = false
      addForm.value.id = ''
      await loadBilibiliSongs()
    } catch (e) {
      ElMessage.error(e.message || t('adminMusic.addFailed'))
    } finally {
      adding.value = false
    }
  }
}

async function removeSong(song) {
  try {
    await ElMessageBox.confirm(
      t('adminMusic.removeConfirm', { name: song.name }),
      t('adminMusic.removeTitle'),
      { type: 'warning' }
    )
    await request.delete(`/admin/music/songs/${song.id}`)
    ElMessage.success(t('adminMusic.removeSuccess'))
    await loadSongs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminMusic.removeFailed'))
  }
}

async function moveSong(index, direction) {
  const newIndex = index + direction
  if (newIndex < 0 || newIndex >= songs.value.length) return
  const arr = [...songs.value]
  const temp = arr[index]
  arr[index] = arr[newIndex]
  arr[newIndex] = temp
  songs.value = arr
  try {
    await request.put('/admin/music/songs/reorder', arr.map(s => s.id))
  } catch (e) {
    ElMessage.error(t('adminMusic.reorderFailed'))
    await loadSongs()
  }
}

async function removeBilibiliSong(song) {
  try {
    await ElMessageBox.confirm(
      t('adminMusic.removeBilibiliSongConfirm', { title: song.title }),
      t('adminMusic.removeTitle'),
      { type: 'warning' }
    )
    await request.delete(`/admin/music/bilibili-songs/${song.id}`)
    ElMessage.success(t('adminMusic.removeBilibiliSongSuccess'))
    await loadBilibiliSongs()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminMusic.removeFailed'))
  }
}

async function moveBilibiliSong(index, direction) {
  const newIndex = index + direction
  if (newIndex < 0 || newIndex >= bilibiliSongs.value.length) return
  const arr = [...bilibiliSongs.value]
  const temp = arr[index]
  arr[index] = arr[newIndex]
  arr[newIndex] = temp
  bilibiliSongs.value = arr
  try {
    await request.put('/admin/music/bilibili-songs/reorder', arr.map(s => s.id))
  } catch {
    ElMessage.error(t('adminMusic.reorderFailed'))
    await loadBilibiliSongs()
  }
}

async function loadVideos() {
  videoLoading.value = true
  try {
    const data = await request.get('/api/video/list')
    videos.value = data || []
  } catch {
    videos.value = []
  } finally {
    videoLoading.value = false
  }
}

function proxyCoverUrl(url) {
  if (!url) return ''
  return `/api/video/cover?url=${encodeURIComponent(url)}`
}

async function removeVideo(video) {
  try {
    await ElMessageBox.confirm(
      t('adminMusic.removeVideoConfirm', { title: video.title }),
      t('adminMusic.removeTitle'),
      { type: 'warning' }
    )
    const allSettings = await request.get('/admin/settings')
    const currentIds = (allSettings.video_ids || '').split(',').map(s => s.trim()).filter(s => s !== video.bvid)
    await request.put('/admin/settings', { settings: { video_ids: currentIds.join(',') } })
    ElMessage.success(t('adminMusic.removeVideoSuccess'))
    await loadVideos()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(t('adminMusic.removeFailed'))
  }
}

// Cover upload
function openCoverPicker(type, song) {
  coverTarget = { type, song }
  coverFileInput.value.click()
}

const MAX_COVER_SIZE = 10 * 1024 * 1024 // 10MB

function onCoverFileSelected(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > MAX_COVER_SIZE) {
    ElMessage.warning(t('fileUpload.tooLarge', { size: 10 }))
    e.target.value = ''
    return
  }
  cropImageFile.value = file
  showCropDialog.value = true
  e.target.value = ''
}

async function onCoverCropped(blob) {
  if (!coverTarget) return
  const { type, song } = coverTarget

  try {
    const formData = new FormData()
    formData.append('file', blob, 'cover.png')
    const uploadResult = await request.post('/admin/upload/music-cover', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const coverUrl = uploadResult.url

    if (type === 'netease') {
      await request.put(`/admin/music/songs/${song.id}/cover`, { coverUrl })
      song.coverUrl = coverUrl
    } else {
      await request.put(`/admin/music/bilibili-songs/${song.id}/cover`, { coverUrl })
      song.customCoverUrl = coverUrl
    }
    ElMessage.success(t('adminMusic.coverUploadSuccess'))
  } catch (e) {
    ElMessage.error(e.message || t('adminMusic.coverUploadFailed'))
  }
  coverTarget = null
}

onMounted(() => {
  loadSongs()
  loadBilibiliSongs()
  loadVideos()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.music-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-item {
  background: var(--color-bg-muted, #f8fafc);
  border: 1px solid var(--color-border-light, #e2e8f0);
  border-radius: 12px;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-num {
  font-size: 28px;
  font-weight: 800;
  color: var(--color-primary, #3b82f6);
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-secondary, #64748b);
  font-weight: 500;
}

/* Empty state */
.empty-state {
  text-align: center;
  padding: 48px 24px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.4;
}

.empty-state p {
  color: var(--color-text-secondary, #94a3b8);
  margin-bottom: 16px;
  font-size: 14px;
}

/* Song grid */
.song-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 12px;
  max-height: 480px;
  overflow-y: auto;
  padding-right: 4px;
}

.song-grid::-webkit-scrollbar {
  width: 6px;
}

.song-grid::-webkit-scrollbar-thumb {
  background: var(--color-border-light, #cbd5e1);
  border-radius: 3px;
}

.song-grid::-webkit-scrollbar-thumb:hover {
  background: var(--color-text-secondary, #94a3b8);
}

.song-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border: 1px solid var(--color-border-light, #e2e8f0);
  border-radius: 12px;
  background: var(--color-bg-muted, #fafbfc);
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.song-card:hover {
  border-color: var(--color-primary, #3b82f6);
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.08);
}

.song-card-cover {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  cursor: pointer;
}

.song-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.song-card-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  font-size: 24px;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.2s;
  border-radius: 10px;
}

.song-card-cover:hover .cover-overlay {
  opacity: 1;
}

.song-card-index {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.song-card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.song-card-info {
  min-width: 0;
}

.song-card-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text, #1e293b);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin: 0;
}

.song-card-artist {
  font-size: 12px;
  color: var(--color-text-secondary, #94a3b8);
  margin: 2px 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.editable {
  cursor: pointer;
  border-radius: 4px;
  padding: 1px 4px;
  margin: -1px -4px;
  transition: background 0.15s;
}

.editable:hover {
  background: var(--color-bg-muted, #f1f5f9);
}

.inline-edit-input {
  width: 100%;
  border: 1px solid var(--color-primary, #3b82f6);
  border-radius: 4px;
  padding: 2px 4px;
  font-size: inherit;
  font-weight: inherit;
  color: inherit;
  background: var(--color-bg, #fff);
  outline: none;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.song-card-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

/* Add dialog */
.form-hint {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.query-error {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  margin-top: 12px;
  font-size: 13px;
  color: #dc2626;
}

.add-preview-badge {
  display: inline-block;
  font-size: 11px;
  color: #16a34a;
  font-weight: 600;
  margin-top: 4px;
}

.add-preview {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  margin-top: 12px;
}

.add-preview-cover {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.add-preview-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.add-preview-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  font-size: 20px;
}

.add-preview-info h4 {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.add-preview-info p {
  font-size: 12px;
  color: #94a3b8;
  margin: 2px 0 0;
}

/* Transition */
.song-list-enter-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.song-list-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.song-list-enter-from { opacity: 0; transform: translateY(8px); }
.song-list-leave-to { opacity: 0; transform: scale(0.95); }

@media (max-width: 768px) {
  .song-grid {
    grid-template-columns: 1fr;
  }
}
</style>
