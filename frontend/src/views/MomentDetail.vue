<template>
  <div class="moment-detail-page" :class="{ 'is-night': isNight }">
    <!-- Loading skeleton -->
    <div v-if="loading" class="detail-container">
      <div class="skeleton" style="width: 100%; height: 280px; border-radius: 16px; margin-bottom: 24px;" />
      <div class="skeleton" style="width: 60%; height: 28px; margin-bottom: 12px;" />
      <div class="skeleton" style="width: 100%; height: 16px; margin-bottom: 8px;" />
      <div class="skeleton" style="width: 90%; height: 16px; margin-bottom: 8px;" />
      <div class="skeleton" style="width: 80%; height: 16px;" />
    </div>

    <ResourceError v-if="loadError" :message="$t('moments.loadError')" @retry="loadEntry()" />

    <!-- Detail content -->
    <div v-if="!loading && entry" class="detail-container fade-in-up">
      <!-- Back button -->
      <button class="back-btn" @click="$router.push('/moments')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><polyline points="15 18 9 12 15 6"/></svg>
        <span>{{ $t('moments.backToList') }}</span>
      </button>

      <!-- Images grid -->
      <div v-if="allImages.length" class="detail-images" :class="imgsClass(allImages.length)">
        <div
          v-for="(img, i) in allImages.slice(0, 9)"
          :key="i"
          class="detail-img-cell"
          @click="openLightbox(i)"
        >
          <img :src="ossImg(img)" :alt="entry.title" loading="lazy" />
          <div v-if="i === 8 && allImages.length > 9" class="img-more-overlay">
            +{{ allImages.length - 9 }}
          </div>
        </div>
      </div>

      <!-- Title area -->
      <div class="detail-header">
        <h1 class="detail-title">{{ entry.title }}</h1>
        <div class="detail-meta">
          <span class="detail-date">{{ formatDate(entry.entryDate) }}</span>
          <span v-if="entry.mood" class="detail-mood">{{ entry.mood }}</span>
          <span v-if="entry.category" class="detail-category">{{ entry.category }}</span>
        </div>
        <div v-if="tagsArray.length" class="detail-tags">
          <span v-for="tag in tagsArray" :key="tag" class="detail-tag">#{{ tag }}</span>
        </div>
      </div>

      <!-- Markdown content -->
      <div class="detail-content markdown-body" v-html="renderedContent"></div>

      <!-- Link -->
      <div v-if="entry.linkUrl" class="detail-link">
        <a :href="entry.linkUrl" target="_blank" rel="noopener">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
          <span>{{ $t('moments.viewLink') }}</span>
        </a>
      </div>
    </div>

    <!-- Lightbox -->
    <ImageLightbox v-model="lightboxVisible" :images="lightboxImages" :initial-index="lightboxIndex" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import { ossImg } from '../utils/oss'
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import css from 'highlight.js/lib/languages/css'
import xml from 'highlight.js/lib/languages/xml'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import sql from 'highlight.js/lib/languages/sql'
import markdown from 'highlight.js/lib/languages/markdown'
import yaml from 'highlight.js/lib/languages/yaml'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'
import cpp from 'highlight.js/lib/languages/cpp'
import csharp from 'highlight.js/lib/languages/csharp'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('python', python)
hljs.registerLanguage('java', java)
hljs.registerLanguage('css', css)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('json', json)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('yaml', yaml)
hljs.registerLanguage('go', go)
hljs.registerLanguage('rust', rust)
hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('csharp', csharp)
import DOMPurify from 'dompurify'
import request from '../utils/request'
import ResourceError from '../components/ResourceError.vue'
import ImageLightbox from '../components/ImageLightbox.vue'

const route = useRoute()
const { t } = useI18n()

const entry = ref(null)
const loading = ref(true)
const loadError = ref(false)
const isNight = ref(document.body.classList.contains('body-night'))

// Lightbox
const lightboxVisible = ref(false)
const lightboxImages = ref([])
const lightboxIndex = ref(0)

const allImages = computed(() => {
  if (!entry.value) return []
  const imgs = []
  if (entry.value.coverImage) imgs.push(entry.value.coverImage)
  if (entry.value.images) {
    try {
      const arr = JSON.parse(entry.value.images)
      if (Array.isArray(arr)) arr.forEach(u => { if (u && !imgs.includes(u)) imgs.push(u) })
    } catch {}
  }
  return imgs
})

function openLightbox(idx) {
  lightboxImages.value = allImages.value
  lightboxIndex.value = idx
  lightboxVisible.value = true
}

function imgsClass(count) {
  if (count <= 1) return 'imgs-1'
  if (count === 2) return 'imgs-2'
  if (count === 3) return 'imgs-3'
  if (count === 4) return 'imgs-4'
  return 'imgs-many'
}

// Configure marked (guard against HMR accumulation)
if (!marked.__configured) {
marked.use(markedHighlight({
  langPrefix: 'hljs language-',
  highlight(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  }
}))
marked.use({ breaks: true, gfm: true })
marked.__configured = true
}

const renderedContent = computed(() => {
  if (!entry.value?.description) return ''
  return DOMPurify.sanitize(marked.parse(entry.value.description))
})

const tagsArray = computed(() => {
  if (!entry.value?.tags) return []
  try {
    const arr = JSON.parse(entry.value.tags)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
})

function formatDate(d) {
  if (!d) return ''
  return d.substring(0, 10)
}

let loadRequestId = 0
async function loadEntry() {
  const reqId = ++loadRequestId
  loadError.value = false
  loading.value = true
  try {
    const data = await request.get(`/api/timeline-entries/${route.params.id}`)
    if (reqId !== loadRequestId) return
    entry.value = data
  } catch (e) {
    if (reqId !== loadRequestId) return
    console.error('Failed to load moment:', e)
    loadError.value = true
  } finally {
    if (reqId === loadRequestId) loading.value = false
  }
}

watch(() => route.params.id, (newId) => {
  if (newId) loadEntry()
})

let nightObserver = null
onMounted(() => {
  loadEntry()
  nightObserver = new MutationObserver(() => {
    isNight.value = document.body.classList.contains('body-night')
  })
  nightObserver.observe(document.body, { attributes: true, attributeFilter: ['class'] })
})
onUnmounted(() => {
  if (nightObserver) { nightObserver.disconnect(); nightObserver = null }
})
</script>

<style scoped>
.moment-detail-page {
  min-height: 100vh;
  padding: 0 24px 60px;
  max-width: 720px;
  margin: 0 auto;
}

.detail-container {
  padding-top: 24px;
}

/* Back button */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #3b82f6;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px 4px;
  margin-bottom: 20px;
  transition: color 0.2s;
}
.back-btn:hover { color: #2563eb; }
.is-night .back-btn { color: #60a5fa; }
.is-night .back-btn:hover { color: #93c5fd; }

/* Images grid */
.detail-images {
  display: grid;
  gap: 6px;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 24px;
}
.detail-images.imgs-1 { grid-template-columns: 1fr; }
.detail-images.imgs-2 { grid-template-columns: 1fr 1fr; }
.detail-images.imgs-3 { grid-template-columns: 1fr 1fr 1fr; }
.detail-images.imgs-4 { grid-template-columns: 1fr 1fr; }
.detail-images.imgs-many { grid-template-columns: 1fr 1fr 1fr; }
.detail-img-cell {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  cursor: zoom-in;
}
.detail-images.imgs-1 .detail-img-cell { aspect-ratio: auto; max-height: 400px; }
.detail-img-cell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s;
}
.detail-img-cell:hover img { transform: scale(1.05); }
.img-more-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 28px;
  font-weight: 700;
}

/* Header */
.detail-header {
  margin-bottom: 28px;
}
.detail-title {
  font-size: 1.8rem;
  font-weight: 900;
  color: #1e293b;
  margin: 0 0 12px;
  line-height: 1.3;
  word-break: break-word;
}
.is-night .detail-title { color: #e2e8f0; }

.detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.detail-date {
  font-family: monospace;
  font-size: 12px;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
  padding: 2px 10px;
  border-radius: 4px;
}
.is-night .detail-date { background: rgba(59, 130, 246, 0.15); color: #60a5fa; }
.detail-mood {
  font-size: 12px;
  color: #ec4899;
  background: rgba(236, 72, 153, 0.08);
  padding: 2px 10px;
  border-radius: 4px;
}
.detail-category {
  font-size: 12px;
  color: #94a3b8;
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 10px;
  border-radius: 4px;
}
.is-night .detail-category { background: rgba(255, 255, 255, 0.06); }

.detail-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.detail-tag {
  font-size: 13px;
  color: #3b82f6;
}
.is-night .detail-tag { color: #60a5fa; }

/* Markdown content */
.detail-content {
  font-size: 15px;
  line-height: 1.8;
  color: #334155;
}
.is-night .detail-content { color: #cbd5e1; }

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  font-weight: 700;
  color: #1e293b;
}
.is-night .markdown-body :deep(h1),
.is-night .markdown-body :deep(h2),
.is-night .markdown-body :deep(h3) { color: #e2e8f0; }

.markdown-body :deep(p) {
  margin: 0 0 1em;
}

.markdown-body :deep(code) {
  font-family: 'Fira Code', monospace;
  font-size: 0.9em;
  background: rgba(59, 130, 246, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  color: #3b82f6;
}
.is-night .markdown-body :deep(code) {
  background: rgba(59, 130, 246, 0.12);
  color: #60a5fa;
}

.markdown-body :deep(pre) {
  background: #1e293b;
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;
  margin: 1em 0;
}
.markdown-body :deep(pre code) {
  background: none;
  color: #e2e8f0;
  padding: 0;
  font-size: 13px;
}

.markdown-body :deep(blockquote) {
  border-left: 3px solid #3b82f6;
  margin: 1em 0;
  padding: 8px 16px;
  background: rgba(59, 130, 246, 0.04);
  border-radius: 0 8px 8px 0;
  color: #64748b;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.markdown-body :deep(a) {
  color: #3b82f6;
  text-decoration: none;
  border-bottom: 1px solid rgba(59, 130, 246, 0.3);
  transition: border-color 0.2s;
}
.markdown-body :deep(a:hover) {
  border-bottom-color: #3b82f6;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 1.5em;
  margin: 0.5em 0;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid rgba(0, 0, 0, 0.08);
  padding: 8px 12px;
  text-align: left;
}
.markdown-body :deep(th) {
  background: rgba(59, 130, 246, 0.06);
  font-weight: 600;
}
.is-night .markdown-body :deep(th),
.is-night .markdown-body :deep(td) {
  border-color: rgba(255, 255, 255, 0.08);
}
.is-night .markdown-body :deep(blockquote) {
  color: #94a3b8;
  background: rgba(59, 130, 246, 0.08);
}

/* Link */
.detail-link {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}
.is-night .detail-link { border-top-color: rgba(255, 255, 255, 0.06); }
.detail-link a {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #3b82f6;
  text-decoration: none;
  transition: color 0.2s;
}
.detail-link a:hover { color: #2563eb; }
.is-night .detail-link a { color: #60a5fa; }

/* Skeleton */
.skeleton {
  background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  border-radius: 6px;
}
.is-night .skeleton {
  background: linear-gradient(90deg, #334155 25%, #475569 50%, #334155 75%);
  background-size: 200% 100%;
}
@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@keyframes fade-in-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
.fade-in-up { animation: fade-in-up 0.6s ease both; }

@media (max-width: 768px) {
  .moment-detail-page { padding: 0 12px 30px; }
  .detail-title { font-size: 1.3rem; }
  .detail-content {
    font-size: 14px;
    word-break: break-word;
    overflow-wrap: anywhere;
  }
  .detail-content :deep(pre) {
    font-size: 0.78rem;
    padding: 0.75rem;
    white-space: pre;
    word-break: normal;
    overflow-wrap: normal;
    overflow-x: auto;
    max-height: 24em;
    overflow-y: auto;
  }
  .detail-content :deep(pre code) {
    display: table;
    min-width: 100%;
    width: auto;
    white-space: pre;
    word-break: normal;
    overflow-wrap: normal;
  }
  .detail-content table { display: block; overflow-x: auto; }
  .detail-images.imgs-3 { grid-template-columns: 1fr 1fr; }
  .detail-images.imgs-many { grid-template-columns: 1fr 1fr; }
  .detail-images.imgs-1 .detail-img-cell { max-height: 280px; }
  .detail-meta { font-size: 0.78rem; flex-wrap: wrap; gap: 0.25rem 0.75rem; }
  .detail-back { font-size: 0.82rem; }
}
@media (max-width: 480px) {
  .detail-images.imgs-2 { grid-template-columns: 1fr; }
  .detail-images.imgs-3 { grid-template-columns: 1fr; }
  .detail-images.imgs-4 { grid-template-columns: 1fr; }
  .detail-images.imgs-many { grid-template-columns: 1fr 1fr; }
}
</style>
