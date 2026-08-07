<template>
  <div class="file-upload-container"
    @dragover.prevent="onDragOver"
    @dragleave.prevent="onDragLeave"
    @drop.prevent="onDrop"
    :class="{ 'drag-active': isDragging }"
  >
    <div class="file-upload-header">
      <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M7 10V9C7 6.23858 9.23858 4 12 4C14.7614 4 17 6.23858 17 9V10C19.2091 10 21 11.7909 21 14C21 15.4806 20.1956 16.8084 19 17.5M7 10C4.79086 10 3 11.7909 3 14C3 15.4806 3.8044 16.8084 5 17.5M7 10C7.43285 10 7.84965 10.0688 8.24006 10.1959M12 12V21M12 12L15 15M12 12L9 15" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <p>{{ isDragging ? $t('fileUpload.dropHere') : (multiple ? $t('fileUpload.browseHintMulti') : $t('fileUpload.browseHint')) }}</p>
    </div>
    <label class="file-upload-footer" @click.prevent="triggerSelect">
      <svg fill="currentColor" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <path d="M15.331 6H8.5v20h15V14.154h-8.169z"/><path d="M18.153 6h-.009v5.342H23.5v-.002z"/>
      </svg>
      <p>{{ displayText }}</p>
      <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M5.16565 10.1534C5.07629 8.99181 5.99473 8 7.15975 8H16.8402C18.0053 8 18.9237 8.9918 18.8344 10.1534L18.142 19.1534C18.0619 20.1954 17.193 21 16.1479 21H7.85206C6.80699 21 5.93811 20.1954 5.85795 19.1534L5.16565 10.1534Z" stroke="currentColor" stroke-width="2"/>
        <path d="M19.5 5H4.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <path d="M10 3C10 2.44772 10.4477 2 11 2H13C13.5523 2 14 2.44772 14 3V5H10V3Z" stroke="currentColor" stroke-width="2"/>
      </svg>
    </label>
    <input
      ref="fileInput"
      type="file"
      :accept="accept"
      :multiple="multiple"
      style="display: none"
      @change="onFileChange"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import request from '../utils/request'

const props = defineProps({
  endpoint: { type: String, required: true },
  accept: { type: String, default: 'image/jpeg,image/png,image/gif,image/webp' },
  maxSize: { type: Number, default: 5 * 1024 * 1024 },
  fieldName: { type: String, default: 'file' },
  multiple: { type: Boolean, default: false }
})

const emit = defineEmits(['uploaded', 'error', 'all-uploaded'])

const { t } = useI18n()
const fileInput = ref(null)
const isDragging = ref(false)
const fileNames = ref([])
const uploading = ref(false)
const uploadProgress = ref(0)

const displayText = computed(() => {
  if (uploading.value) {
    return props.multiple
      ? `${t('fileUpload.uploading')} (${uploadProgress.value}/${fileNames.value.length})`
      : t('fileUpload.uploading')
  }
  if (fileNames.value.length > 0) {
    return props.multiple && fileNames.value.length > 1
      ? `${fileNames.value.length} ${t('fileUpload.filesSelected')}`
      : fileNames.value[0]
  }
  return t('fileUpload.notSelected')
})

function triggerSelect() {
  fileInput.value?.click()
}

function onDragOver() {
  isDragging.value = true
}

function onDragLeave() {
  isDragging.value = false
}

function onDrop(e) {
  isDragging.value = false
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    if (props.multiple) {
      processFiles(Array.from(files))
    } else {
      processFile(files[0])
    }
  }
}

function onFileChange(e) {
  const files = e.target.files
  if (files && files.length > 0) {
    if (props.multiple) {
      processFiles(Array.from(files))
    } else {
      processFile(files[0])
    }
  }
  e.target.value = ''
}

function validateFile(file) {
  if (props.accept && !file.type.match(props.accept.replace(/,/g, '|').replace(/\*/g, '.*'))) {
    ElMessage.warning(t('fileUpload.invalidType'))
    return false
  }
  if (file.size > props.maxSize) {
    ElMessage.warning(t('fileUpload.tooLarge', { size: Math.round(props.maxSize / 1024 / 1024) }))
    return false
  }
  return true
}

async function processFiles(files) {
  const validFiles = files.filter(validateFile)
  if (validFiles.length === 0) return

  fileNames.value = validFiles.map(f => f.name)
  uploading.value = true
  uploadProgress.value = 0

  const results = []
  for (const file of validFiles) {
    try {
      const fd = new FormData()
      fd.append(props.fieldName, file)
      const res = await request.post(props.endpoint, fd, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      emit('uploaded', res)
      results.push(res)
    } catch (err) {
      ElMessage.error(err.message || t('fileUpload.uploadFailed'))
      emit('error', err)
    }
    uploadProgress.value++
  }

  uploading.value = false
  emit('all-uploaded', results)
}

async function processFile(file) {
  if (!validateFile(file)) return

  fileNames.value = [file.name]
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append(props.fieldName, file)
    const res = await request.post(props.endpoint, fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    emit('uploaded', res)
  } catch (err) {
    ElMessage.error(err.message || t('fileUpload.uploadFailed'))
    emit('error', err)
    fileNames.value = []
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.file-upload-container {
  height: 260px;
  width: 100%;
  border-radius: 10px;
  box-shadow: 4px 4px 30px rgba(0, 0, 0, .12);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding: 10px;
  gap: 5px;
  background-color: rgba(0, 110, 255, 0.041);
  transition: all 0.3s ease;
}
.file-upload-container.drag-active {
  box-shadow: 4px 4px 30px rgba(59, 130, 246, 0.3);
  background-color: rgba(0, 110, 255, 0.08);
}
.file-upload-container.drag-active .file-upload-header {
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.04);
}

.file-upload-header {
  flex: 1;
  width: 100%;
  border: 2px dashed royalblue;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  transition: all 0.3s ease;
}
.file-upload-header svg {
  height: 80px;
  color: royalblue;
}
.file-upload-header p {
  text-align: center;
  color: #333;
  font-size: 14px;
  margin: 0;
}

.file-upload-footer {
  background-color: rgba(0, 110, 255, 0.075);
  width: 100%;
  height: 40px;
  padding: 8px;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  color: #333;
  border: none;
  transition: background-color 0.2s;
}
.file-upload-footer:hover {
  background-color: rgba(0, 110, 255, 0.12);
}
.file-upload-footer svg {
  height: 130%;
  fill: royalblue;
  background-color: rgba(70, 66, 66, 0.103);
  border-radius: 50%;
  padding: 2px;
  cursor: pointer;
  box-shadow: 0 2px 30px rgba(0, 0, 0, 0.205);
}
.file-upload-footer p {
  flex: 1;
  text-align: center;
  font-size: 13px;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
