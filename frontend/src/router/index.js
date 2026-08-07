import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  // Public
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'articles', name: 'Articles', component: () => import('../views/Articles.vue') },
      { path: 'articles/:slug', name: 'ArticleDetail', component: () => import('../views/ArticleDetail.vue') },
      { path: 'projects', name: 'Projects', component: () => import('../views/Projects.vue') },
      { path: 'projects/:id', name: 'ProjectDetail', component: () => import('../views/ProjectDetail.vue') },
      { path: 'about', name: 'About', component: () => import('../views/About.vue') },
      { path: 'archive', name: 'Archive', component: () => import('../views/Archive.vue') },
      { path: 'moments', name: 'Moments', component: () => import('../views/Moments.vue') },
      { path: 'friends', name: 'Friends', component: () => import('../views/Friends.vue') },
      { path: 'moments/:id', name: 'MomentDetail', component: () => import('../views/MomentDetail.vue') },
      { path: 'announcements', name: 'Announcements', component: () => import('../views/Announcements.vue') },
      { path: 'search', name: 'Search', component: () => import('../views/Search.vue') },
      { path: 'games', name: 'Games', component: () => import('../views/Games.vue') },
      { path: 'photowall', name: 'PhotoWall', component: () => import('../views/PhotoWall.vue') },
      { path: 'music', name: 'Music', component: () => import('../views/Music.vue') },
      { path: 'cosmos', name: 'Cosmos', component: () => import('../views/Cosmos.vue') },
      { path: ':pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFound.vue') },
    ]
  },
  // Admin
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/admin/Login.vue')
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'articles', name: 'AdminArticles', component: () => import('../views/admin/Articles.vue') },
      { path: 'articles/create', name: 'AdminArticleCreate', component: () => import('../views/admin/ArticleEdit.vue') },
      { path: 'articles/edit/:id', name: 'AdminArticleEdit', component: () => import('../views/admin/ArticleEdit.vue') },
      { path: 'categories', name: 'AdminCategories', component: () => import('../views/admin/Categories.vue') },
      { path: 'tags', name: 'AdminTags', component: () => import('../views/admin/Tags.vue') },
      { path: 'projects', name: 'AdminProjects', component: () => import('../views/admin/Projects.vue') },
      { path: 'settings', name: 'AdminSettings', component: () => import('../views/admin/Settings.vue'), meta: { requiresSuperAdmin: true } },
      { path: 'ai-settings', name: 'AdminAiSettings', component: () => import('../views/admin/AiSettings.vue'), meta: { requiresSuperAdmin: true } },
      { path: 'weather-settings', name: 'AdminWeatherSettings', component: () => import('../views/admin/WeatherSettings.vue'), meta: { requiresSuperAdmin: true } },
      { path: 'oss-settings', name: 'AdminOssSettings', component: () => import('../views/admin/OssSettings.vue'), meta: { requiresSuperAdmin: true } },
      { path: 'music', name: 'AdminMusic', component: () => import('../views/admin/Music.vue'), meta: { requiresSuperAdmin: true } },
      { path: 'announcements', name: 'AdminAnnouncements', component: () => import('../views/admin/Announcements.vue'), meta: { requiresPermission: 'announcement:view' } },
      { path: 'timeline', name: 'AdminTimeline', component: () => import('../views/admin/TimelineEntries.vue'), meta: { requiresPermission: 'project:view' } },
      { path: 'photowall', name: 'AdminPhotoWall', component: () => import('../views/admin/PhotoWall.vue'), meta: { requiresPermission: 'photowall:view' } },
      { path: 'friend-links', name: 'AdminFriendLinks', component: () => import('../views/admin/FriendLinks.vue'), meta: { requiresPermission: 'project:view' } },
      { path: 'reviews', name: 'AdminReviews', component: () => import('../views/admin/Reviews.vue'), meta: { requiresSuperAdmin: true } },
      { path: 'users', name: 'AdminUsers', component: () => import('../views/admin/Users.vue'), meta: { requiresPermission: 'user:view' } },
      { path: 'login-logs', name: 'AdminLoginLogs', component: () => import('../views/admin/LoginLogs.vue'), meta: { requiresPermission: 'log:view' } },
      { path: 'account-security', name: 'AdminAccountSecurity', component: () => import('../views/admin/AccountSecurity.vue') },
      { path: 'profile', name: 'AdminProfile', component: () => import('../views/admin/Profile.vue') },
      { path: 'backup', name: 'AdminBackup', component: () => import('../views/admin/Backup.vue'), meta: { requiresSuperAdmin: true } },
      { path: ':pathMatch(.*)*', name: 'AdminNotFound', component: () => import('../views/NotFound.vue') },
    ]
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

let progressBarTimer = null
let sessionValidated = false

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // Show progress bar
  const bar = document.getElementById('route-progress-bar')
  if (bar) {
    bar.style.display = 'block'
    bar.style.width = '0%'
    requestAnimationFrame(() => { bar.style.width = '70%' })
  }

  // 已登录用户访问登录页 → 跳转管理后台
  if (to.path === '/admin/login') {
    if (authStore.isLoggedIn) return next({ name: 'AdminDashboard' })
    return next()
  }
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!authStore.isLoggedIn) {
      next({ name: 'AdminLogin', query: { redirect: to.fullPath } })
    } else {
      // First admin navigation: validate session against server
      // (token in localStorage may be valid JWT but server session cleared)
      if (!sessionValidated) {
        sessionValidated = true
        await authStore.validateSession()
        if (!authStore.isLoggedIn) {
          return next({ name: 'AdminLogin', query: { redirect: to.fullPath } })
        }
      }
      if (to.matched.some(record => record.meta.requiresSuperAdmin) && !authStore.isSuperAdmin) {
        next({ name: 'AdminDashboard' })
      } else {
        const permMeta = to.matched.find(r => r.meta.requiresPermission)?.meta.requiresPermission
        if (permMeta && !authStore.hasPermission(permMeta)) {
          next({ name: 'AdminDashboard' })
        } else {
          next()
        }
      }
    }
  } else {
    next()
  }
})

router.afterEach((to) => {
  document.title = to.meta?.title || '雨中的研发日志'
  const bar = document.getElementById('route-progress-bar')
  if (bar) {
    bar.style.width = '100%'
    clearTimeout(progressBarTimer)
    progressBarTimer = setTimeout(() => {
      bar.style.display = 'none'
      bar.style.width = '0%'
    }, 300)
  }
})

export default router
