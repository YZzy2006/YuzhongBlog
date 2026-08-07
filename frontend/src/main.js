import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import { useAuthStore } from './stores/auth'
import './styles/global.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(i18n)

// 立即挂载，不等待 token 刷新——登录用户的静默刷新在后台进行，不阻塞首屏
app.mount('#app')

// 标记已加载（同会话刷新时跳过 loading）
sessionStorage.setItem('app-loaded', 'true')

// 显示主内容
document.documentElement.classList.add('app-ready')

// 淡出 loading screen
const loadingScreen = document.getElementById('loading-screen')
if (loadingScreen) {
  loadingScreen.classList.add('fade-out')
  loadingScreen.addEventListener('transitionend', () => loadingScreen.remove())
}

// 后台静默刷新 token（不再阻塞挂载）
const authStore = useAuthStore()
authStore.trySilentRefresh()
