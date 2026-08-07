<template>
  <Teleport to="body">
    <div v-if="visible" class="cmd-overlay" @click="visible = false">
      <div class="cmd-box" @click.stop>
        <div class="cmd-input-wrap">
          <svg class="cmd-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
          <input
            ref="inputRef"
            v-model="query"
            class="cmd-input"
            :placeholder="$t('cmd.placeholder')"
            @keydown.down.prevent="move(1)"
            @keydown.up.prevent="move(-1)"
            @keydown.enter.prevent="go(filtered[activeIndex])"
            @keydown.escape.prevent="visible = false"
          />
          <span class="cmd-hint">ESC</span>
        </div>
        <div class="cmd-list" v-if="filtered.length">
          <template v-for="(group, gi) in grouped" :key="gi">
            <div class="cmd-group-label">{{ group.label }}</div>
            <div
              v-for="(item, ii) in group.items"
              :key="item.path"
              class="cmd-item"
              :class="{ active: flatIndex(gi, ii) === activeIndex }"
              @click="go(item)"
              @mouseenter="activeIndex = flatIndex(gi, ii)"
            >
              <span class="cmd-item-icon" v-html="item.icon"></span>
              <span class="cmd-item-label">{{ item.label }}</span>
              <span class="cmd-item-path">{{ item.shortcut }}</span>
            </div>
          </template>
        </div>
        <div v-else class="cmd-empty">{{ $t('cmd.noResults') }}</div>
        <div class="cmd-footer">
          <span>{{ $t('cmd.hint') }}</span>
          <span>{{ $t('cmd.nav') }}</span>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const { t } = useI18n()
const authStore = useAuthStore()

const visible = ref(false)
const query = ref('')
const activeIndex = ref(0)
const inputRef = ref(null)

const commands = computed(() => {
  const list = [
    { icon: '&#x1F3E0;', label: t('cmd.home'), path: '/', group: t('cmd.groupPublic'), shortcut: '', keywords: 'home 首页' },
    { icon: '&#x1F4DD;', label: t('cmd.articles'), path: '/articles', group: t('cmd.groupPublic'), shortcut: '', keywords: 'articles 文章 博客 blog' },
    { icon: '&#x1F4BC;', label: t('cmd.projects'), path: '/projects', group: t('cmd.groupPublic'), shortcut: '', keywords: 'projects 作品 项目 portfolio' },
    { icon: '&#x1F5BC;', label: t('cmd.photowall'), path: '/photowall', group: t('cmd.groupPublic'), shortcut: '', keywords: 'photowall 照片墙 相册 gallery 图库' },
    { icon: '&#x1F4D6;', label: t('cmd.archive'), path: '/archive', group: t('cmd.groupPublic'), shortcut: '', keywords: 'archive 归档 时间线 timeline' },
    { icon: '&#x1F464;', label: t('cmd.about'), path: '/about', group: t('cmd.groupPublic'), shortcut: '', keywords: 'about 关于' },
  ]

  if (authStore.isLoggedIn) {
    list.push(
      { icon: '&#x1F4CA;', label: t('cmd.dashboard'), path: '/admin', group: t('cmd.groupAdmin'), shortcut: '', keywords: 'dashboard 仪表盘 控制台' },
      { icon: '&#x270F;', label: t('cmd.writeArticle'), path: '/admin/articles/create', group: t('cmd.groupAdmin'), shortcut: '', keywords: 'create 写文章 新建 new article' },
      { icon: '&#x1F4DD;', label: t('cmd.articleManage'), path: '/admin/articles', group: t('cmd.groupAdmin'), shortcut: '', keywords: 'articles 文章管理' },
      { icon: '&#x1F3F7;', label: t('cmd.categoryManage'), path: '/admin/categories', group: t('cmd.groupAdmin'), shortcut: '', keywords: 'categories 分类' },
      { icon: '&#x1F3F7;', label: t('cmd.tagManage'), path: '/admin/tags', group: t('cmd.groupAdmin'), shortcut: '', keywords: 'tags 标签' },
      { icon: '&#x1F4BC;', label: t('cmd.projectManage'), path: '/admin/projects', group: t('cmd.groupAdmin'), shortcut: '', keywords: 'projects 项目管理' },
      { icon: '&#x1F4E2;', label: t('cmd.announcementManage'), path: '/admin/announcements', group: t('cmd.groupAdmin'), shortcut: '', keywords: 'announcements 公告' },
      { icon: '&#x1F5BC;', label: t('cmd.photowallManage'), path: '/admin/photowall', group: t('cmd.groupAdmin'), shortcut: '', keywords: 'photowall 照片墙 相册 gallery' },
      { icon: '&#x2699;', label: t('cmd.siteSettings'), path: '/admin/settings', group: t('cmd.groupAdmin'), shortcut: '', keywords: 'settings 设置 配置' },
    )
    if (authStore.isSuperAdmin) {
      list.push(
        { icon: '&#x1F916;', label: t('cmd.aiSettings'), path: '/admin/ai-settings', group: t('cmd.groupAdmin'), shortcut: '', keywords: 'ai 人工智能 设置 config' },
        { icon: '&#9729;', label: t('cmd.ossSettings'), path: '/admin/oss-settings', group: t('cmd.groupAdmin'), shortcut: '', keywords: 'oss 阿里云 存储 配置 upload' },
      )
    }
  }
  return list
})

const filtered = computed(() => {
  if (!query.value) return commands.value
  const q = query.value.toLowerCase()
  return commands.value.filter(c =>
    c.label.toLowerCase().includes(q) ||
    c.group.toLowerCase().includes(q) ||
    c.keywords.toLowerCase().includes(q)
  )
})

const grouped = computed(() => {
  const map = new Map()
  for (const item of filtered.value) {
    if (!map.has(item.group)) map.set(item.group, [])
    map.get(item.group).push(item)
  }
  return Array.from(map.entries()).map(([label, items]) => ({ label, items }))
})

function flatIndex(groupIdx, itemIdx) {
  let idx = 0
  for (let i = 0; i < groupIdx; i++) {
    idx += grouped.value[i].items.length
  }
  return idx + itemIdx
}

function move(delta) {
  const total = filtered.value.length
  if (!total) return
  activeIndex.value = (activeIndex.value + delta + total) % total
}

function go(item) {
  if (!item) return
  visible.value = false
  router.push(item.path)
}

function globalKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    visible.value = !visible.value
    return
  }
  if (e.key === '/' && !visible.value) {
    const tag = document.activeElement?.tagName
    if (tag === 'INPUT' || tag === 'TEXTAREA' || document.activeElement?.isContentEditable) return
    e.preventDefault()
    visible.value = true
  }
}

watch(visible, (v) => {
  if (v) {
    query.value = ''
    activeIndex.value = 0
    nextTick(() => inputRef.value?.focus())
  }
})

onMounted(() => document.addEventListener('keydown', globalKeydown))
onUnmounted(() => document.removeEventListener('keydown', globalKeydown))
</script>

<style scoped>
.cmd-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 9999;
  display: flex;
  justify-content: center;
  padding-top: 15vh;
}
.cmd-box {
  width: 560px;
  max-height: 480px;
  background: var(--color-bg, #fff);
  border-radius: 12px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: cmdSlideIn 0.15s ease-out;
}
@keyframes cmdSlideIn {
  from { opacity: 0; transform: translateY(-12px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.cmd-input-wrap {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border-light, #eee);
  gap: 0.5rem;
}
.cmd-icon {
  color: var(--color-text-secondary, #999);
  flex-shrink: 0;
}
.cmd-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 0.95rem;
  background: transparent;
  color: var(--color-text, #333);
}
.cmd-input::placeholder {
  color: var(--color-text-secondary, #aaa);
}
.cmd-hint {
  font-size: 0.7rem;
  padding: 0.15rem 0.4rem;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 4px;
  color: var(--color-text-secondary, #999);
  flex-shrink: 0;
}
.cmd-list {
  overflow-y: auto;
  padding: 0.4rem 0;
}
.cmd-group-label {
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-secondary, #999);
  padding: 0.5rem 1rem 0.25rem;
}
.cmd-item {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.5rem 1rem;
  cursor: pointer;
  transition: background 0.1s;
}
.cmd-item:hover,
.cmd-item.active {
  background: var(--color-bg-muted, #f5f5f5);
}
.cmd-item-icon {
  font-size: 1rem;
  width: 1.5rem;
  text-align: center;
  flex-shrink: 0;
}
.cmd-item-label {
  flex: 1;
  font-size: 0.875rem;
  color: var(--color-text, #333);
}
.cmd-item-path {
  font-size: 0.72rem;
  color: var(--color-text-secondary, #aaa);
  font-family: monospace;
}
.cmd-empty {
  padding: 2rem 1rem;
  text-align: center;
  color: var(--color-text-secondary, #999);
  font-size: 0.85rem;
}
.cmd-footer {
  display: flex;
  justify-content: space-between;
  padding: 0.45rem 1rem;
  border-top: 1px solid var(--color-border-light, #eee);
  font-size: 0.7rem;
  color: var(--color-text-secondary, #aaa);
}
@media (max-width: 768px) {
  .cmd-box {
    width: calc(100vw - 2rem);
    max-height: 70vh;
  }
}
</style>
