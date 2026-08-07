<template>
  <div class="project-detail-page" :class="{ 'is-night': isNight }">
    <div class="project-detail-container">
      <!-- Back button -->
      <button class="back-btn" @click="goBack" :title="$t('projects.title')">
        &larr; <span>{{ $t('projects.title') }}</span>
      </button>

      <!-- Loading -->
      <div v-if="loading" class="detail-loading">
        <div class="skeleton skeleton-title"></div>
        <div class="skeleton skeleton-line"></div>
        <div class="skeleton skeleton-line short"></div>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="detail-error">
        <p>{{ $t('projects.loadError') }}</p>
        <button class="retry-btn" @click="loadProject">重试</button>
      </div>

      <!-- Detail -->
      <article v-else-if="project" class="project-detail">
        <!-- Cover -->
        <div v-if="project.coverImage" class="detail-cover">
          <img :src="ossImg(project.coverImage)" :alt="project.name" />
        </div>

        <!-- Header -->
        <header class="detail-header">
          <h1>{{ project.name }}</h1>
          <p v-if="project.subtitle" class="detail-subtitle">{{ project.subtitle }}</p>
          <div v-if="project.techStack" class="detail-tech">
            <span v-for="t in splitTech(project.techStack)" :key="t" class="tech-tag">{{ t }}</span>
          </div>
          <div v-if="hasLinks" class="detail-links">
            <a v-if="project.githubUrl" :href="project.githubUrl" target="_blank" rel="noopener noreferrer" class="link-btn">
              <svg viewBox="0 0 16 16" width="14" height="14" fill="currentColor"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/></svg>
              {{ $t('projects.github') }}
            </a>
            <a v-if="project.demoUrl" :href="project.demoUrl" target="_blank" rel="noopener noreferrer" class="link-btn">{{ $t('projects.demo') }}</a>
            <a v-if="project.subdomainUrl" :href="project.subdomainUrl" target="_blank" rel="noopener noreferrer" class="link-btn primary">{{ $t('projects.visit') }} →</a>
          </div>
        </header>

        <!-- Screenshots gallery -->
        <section v-if="screenshots.length" class="detail-shots">
          <img v-for="(img, i) in screenshots" :key="i" :src="ossImg(img)" :alt="project.name"
            loading="lazy" class="detail-shot" @click="openLightbox(i)" />
        </section>

        <!-- Description -->
        <section v-if="project.description" class="detail-section">
          <h2>{{ $t('projects.title') }} · {{ $t('projects.desc') }}</h2>
          <MdPreview v-if="project.description" :modelValue="project.description" previewTheme="github" :codeFoldable="false" />
        </section>

        <!-- Features -->
        <section v-if="project.features" class="detail-section">
          <h2>项目亮点</h2>
          <MdPreview :modelValue="project.features" previewTheme="github" :codeFoldable="false" />
        </section>
      </article>
    </div>

    <ImageLightbox v-model="lightboxVisible" :images="screenshots" :initial-index="lightboxIndex" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import request from '../utils/request'
import { ossImg } from '../utils/oss'
import ImageLightbox from '../components/ImageLightbox.vue'

const MdPreview = defineAsyncComponent(() =>
  import('md-editor-v3').then(m => m.MdPreview)
)

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const project = ref(null)
const loading = ref(true)
const error = ref(false)
const lightboxVisible = ref(false)
const lightboxIndex = ref(0)

const isNight = ref(document.body.classList.contains('body-night'))
let nightObserver = null

const screenshots = computed(() => {
  if (!project.value?.screenshots) return []
  try {
    const arr = JSON.parse(project.value.screenshots)
    return Array.isArray(arr) ? arr.filter(Boolean) : []
  } catch {
    return []
  }
})

const hasLinks = computed(() =>
  project.value && (project.value.githubUrl || project.value.demoUrl || project.value.subdomainUrl)
)

function splitTech(str) {
  return (str || '').split(',').map(s => s.trim()).filter(Boolean)
}

function openLightbox(i) {
  lightboxIndex.value = i
  lightboxVisible.value = true
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/projects')
}

async function loadProject() {
  loading.value = true
  error.value = false
  try {
    project.value = await request.get(`/api/projects/${route.params.id}`)
    document.title = (project.value?.name ? project.value.name + ' - ' : '') + '雨中的研发日志'
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProject()
  nightObserver = new MutationObserver(() => {
    isNight.value = document.body.classList.contains('body-night')
  })
  nightObserver.observe(document.body, { attributes: true, attributeFilter: ['class'] })
})
onBeforeUnmount(() => {
  if (nightObserver) { nightObserver.disconnect(); nightObserver = null }
})
</script>

<style scoped>
.project-detail-page {
  min-height: 100vh;
  padding: 80px 16px 60px;
}
.project-detail-container {
  max-width: 860px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 20px;
  background: transparent;
  color: #3b82f6;
  font-size: 13px;
  cursor: pointer;
  margin-bottom: 20px;
  transition: all 0.2s;
}
.back-btn:hover {
  background: rgba(59, 130, 246, 0.08);
}

.detail-cover {
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 24px;
}
.detail-cover img {
  width: 100%;
  max-height: 320px;
  object-fit: cover;
  display: block;
}

.detail-header {
  margin-bottom: 24px;
}
.detail-header h1 {
  font-size: 1.8rem;
  font-weight: 800;
  margin: 0 0 8px;
  color: #0f172a;
}
.detail-subtitle {
  font-size: 1rem;
  color: #64748b;
  margin: 0 0 12px;
}
.detail-tech {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}
.tech-tag {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  font-weight: 500;
}
.detail-links {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 13px;
  color: #475569;
  text-decoration: none;
  transition: all 0.2s;
}
.link-btn:hover {
  border-color: #3b82f6;
  color: #2563eb;
}
.link-btn.primary {
  background: #3b82f6;
  color: #fff;
  border-color: #3b82f6;
}
.link-btn.primary:hover {
  background: #2563eb;
}

.detail-shots {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 28px;
}
.detail-shot {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  border-radius: 10px;
  cursor: zoom-in;
  transition: transform 0.25s;
  border: 1px solid rgba(0, 0, 0, 0.06);
}
.detail-shot:hover { transform: scale(1.02); }

.detail-section {
  margin-bottom: 28px;
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  border: 1px solid #f0f0f0;
}
.detail-section h2 {
  font-size: 1.1rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-loading {
  padding: 40px 0;
}
.skeleton {
  border-radius: 8px;
  background: linear-gradient(120deg, #e2e8f0 30%, #f1f5f9 38%, #f1f5f9 40%, #e2e8f0 48%);
  background-size: 200% 100%;
  animation: shimmer 2s infinite;
}
.skeleton-title { height: 32px; width: 60%; margin-bottom: 16px; }
.skeleton-line { height: 14px; margin-bottom: 10px; }
.skeleton-line.short { width: 80%; }

.detail-error {
  text-align: center;
  padding: 60px 0;
  color: #64748b;
}
.retry-btn {
  padding: 8px 20px;
  border: 1px solid #3b82f6;
  border-radius: 20px;
  background: transparent;
  color: #3b82f6;
  cursor: pointer;
}
@keyframes shimmer { 100% { background-position: -200% 0; } }

/* Night mode */
.is-night .detail-header h1 { color: #f1f5f9; }
.is-night .detail-subtitle { color: #94a3b8; }
.is-night .detail-section {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.08);
}
.is-night .detail-section h2 {
  color: #e2e8f0;
  border-bottom-color: rgba(255, 255, 255, 0.08);
}
.is-night .back-btn { border-color: rgba(90, 155, 255, 0.4); color: #5a9bff; }

@media (max-width: 768px) {
  .project-detail-page { padding: 70px 12px 40px; }
  .detail-header h1 { font-size: 1.4rem; }
  .detail-shots { grid-template-columns: repeat(2, 1fr); }
}
</style>
