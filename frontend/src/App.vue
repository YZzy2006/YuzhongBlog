<template>
  <el-config-provider :locale="epLocale">
    <div id="route-progress-bar"></div>
    <router-view />
    <footer class="site-footer" v-if="hasFooterContent">
      <div class="footer-line">
        <span v-if="copyright" @click="onCopyrightClick" style="cursor:default;user-select:none">{{ copyright }}</span>
        <span v-if="copyright && icpNumber" class="footer-sep">|</span>
        <a v-if="icpNumber" href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">{{ icpNumber }}</a>
        <span v-if="icpNumber && policeNumber" class="footer-sep">|</span>
        <a v-if="policeNumber" href="https://beian.mps.gov.cn/" target="_blank" rel="noopener noreferrer">{{ policeNumber }}</a>
      </div>
    </footer>
    <div v-if="!isAdminRoute" class="cyber-cat-desktop">
      <CyberCat v-if="petDeferred" />
      <div v-else class="pet-skeleton" aria-hidden="true"><span class="pet-skeleton-paw">🐾</span></div>
    </div>
    <RippleEffect v-if="!isAdminRoute" />
  </el-config-provider>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { defineAsyncComponent } from 'vue'
const CyberCat = defineAsyncComponent(() => import('./components/CyberCat.vue'))
import RippleEffect from './components/RippleEffect.vue'
import { useRoute, useRouter } from 'vue-router'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import enUs from 'element-plus/dist/locale/en.mjs'
import { initLocale } from './i18n'

const locale = localStorage.getItem('locale') || 'zh-CN'
const epLocale = computed(() => locale === 'en-US' ? enUs : zhCn)

const route = useRoute()
const router = useRouter()
const isAdminRoute = computed(() => route.path.startsWith('/admin'))

// 宠物组件较重（精灵图 2MB+），固定延迟后挂载，首屏优先显示内容
const petDeferred = ref(false)
function deferPetLoad() {
  setTimeout(() => { petDeferred.value = true }, 2500)
}
onMounted(deferPetLoad)

// Secret: 5 rapid clicks on copyright → login page
let copyrightClicks = 0
let copyrightTimer = null
function onCopyrightClick() {
  copyrightClicks++
  clearTimeout(copyrightTimer)
  if (copyrightClicks >= 5) {
    copyrightClicks = 0
    router.push('/admin/login')
    return
  }
  copyrightTimer = setTimeout(() => { copyrightClicks = 0 }, 1500)
}

const icpNumber = ref('粤ICP备2026044688号')
const policeNumber = ref('')
const copyright = ref('© 2026 雨中的研发日志')

const hasFooterContent = computed(() => icpNumber.value || policeNumber.value || copyright.value)

async function fetchSiteInfo() {
  try {
    const res = await fetch('/api/site/info')
    const json = await res.json()
    if (json.code === 200 && json.data?.extraSettings) {
      const s = json.data.extraSettings
      if (s.icp_number) icpNumber.value = s.icp_number
      if (s.police_number) policeNumber.value = s.police_number
      if (s.copyright) copyright.value = s.copyright
    }
  } catch {
    // settings not configured yet, footer stays hidden
  }
}

let siteInfoFetched = false
if (!isAdminRoute.value) {
  fetchSiteInfo()
  siteInfoFetched = true
}

// Switch locale when route changes (admin vs frontend)
watch(() => route.path, (path) => {
  const isAdmin = path.startsWith('/admin')
  initLocale(isAdmin)
  if (!isAdmin && !siteInfoFetched) {
    fetchSiteInfo()
    siteInfoFetched = true
  }
}, { immediate: true })
</script>

<style>
.site-footer {
  text-align: center;
  padding: 1.5rem 1rem;
  font-size: 0.75rem;
  background: var(--color-footer-bg, #1e293b);
  color: var(--color-footer-text, #94a3b8);
  border-top: 1px solid var(--color-footer-border, rgba(255, 255, 255, 0.08));
}
.footer-line {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.footer-sep {
  opacity: 0.4;
}
.site-footer a {
  color: var(--color-footer-text, #94a3b8);
  text-decoration: none;
  transition: color 0.2s;
}
.site-footer a:hover {
  color: var(--color-footer-heading, #e2e8f0);
}
body.body-night .site-footer {
  background: var(--color-footer-bg, rgba(10, 20, 35, 0.95));
  color: var(--color-footer-text, #a0aec0);
}
body.body-night .site-footer a {
  color: var(--color-footer-text, #a0aec0);
}
body.body-night .site-footer a:hover {
  color: var(--color-footer-heading, #e0e0e0);
}

.cyber-cat-desktop {
  display: none;
}

/* 宠物加载前的轻量骨架占位（与宠物容器同位置，避免布局跳动） */
.pet-skeleton {
  position: fixed;
  bottom: 7rem;
  right: 5rem;
  z-index: 9998;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  font-size: 26px;
  animation: petSkeletonPulse 1.8s ease-in-out infinite;
}
.pet-skeleton-paw { opacity: 0.75; }
@keyframes petSkeletonPulse {
  0%, 100% { transform: scale(1); opacity: 0.9; }
  50% { transform: scale(0.9); opacity: 0.55; }
}

@media (min-width: 768px) {
  .cyber-cat-desktop {
    display: block;
  }
}

#route-progress-bar {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  height: 2px;
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
  z-index: 99999;
  transition: width 0.3s ease-out;
  border-radius: 0 2px 2px 0;
}
</style>
