<template>
  <div class="login-page" @mousemove="onMouseMove">
    <!-- Grid Pattern -->
    <div class="grid-pattern"></div>

    <!-- Noise Texture -->
    <div class="noise-overlay"></div>

    <!-- Code Rain Background -->
    <div class="code-rain" ref="rainRef"></div>

    <!-- Floating Particles -->
    <div class="particles" ref="particlesRef"></div>

    <!-- Mouse Spotlight -->
    <div class="mouse-spotlight" :style="spotlightStyle"></div>

    <!-- Floating Orbs -->
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>
    <div class="orb orb-3"></div>

    <!-- Back to Home -->
    <button class="back-btn" @click="router.push('/')" :title="$t('login.backHome')">
      <div class="back-btn-box">
        <span class="back-btn-elem">
          <svg viewBox="0 0 46 40" xmlns="http://www.w3.org/2000/svg"><path d="M46 20.038c0-.7-.3-1.5-.8-2.1l-16-17c-1.1-1-3.2-1.4-4.4-.3-1.2 1.1-1.2 3.3 0 4.4l11.3 11.9H3c-1.7 0-3 1.3-3 3s1.3 3 3 3h33.1l-11.3 11.9c-1 1-1.2 3.3 0 4.4 1.2 1.1 3.3.8 4.4-.3l16-17c.5-.5.8-1.1.8-1.9z"/></svg>
        </span>
        <span class="back-btn-elem">
          <svg viewBox="0 0 46 40" xmlns="http://www.w3.org/2000/svg"><path d="M46 20.038c0-.7-.3-1.5-.8-2.1l-16-17c-1.1-1-3.2-1.4-4.4-.3-1.2 1.1-1.2 3.3 0 4.4l11.3 11.9H3c-1.7 0-3 1.3-3 3s1.3 3 3 3h33.1l-11.3 11.9c-1 1-1.2 3.3 0 4.4 1.2 1.1 3.3.8 4.4-.3l16-17c.5-.5.8-1.1.8-1.9z"/></svg>
        </span>
      </div>
    </button>

    <!-- Main Content -->
    <div class="login-container">
      <!-- Left: Brand -->
      <div class="brand-side animate-item" :class="{ 'show': mounted }">
        <div class="brand-content">
          <h1 class="brand-title">
            <span class="typewriter-text">{{ displayedTitle }}</span>
            <span class="cursor">|</span>
          </h1>
          <p class="brand-subtitle">{{ displayedSubtitle }}</p>
          <div class="brand-tags">
            <span class="tag" v-for="(tag, i) in tags" :key="tag" :style="{ animationDelay: `${0.8 + i * 0.1}s` }">{{ tag }}</span>
          </div>
          <!-- Stats -->
          <div class="brand-stats">
            <div class="stat-item">
              <span class="stat-value">10+</span>
              <span class="stat-label">{{ $t('login.statTech') }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">100%</span>
              <span class="stat-label">{{ $t('login.statSecure') }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">24/7</span>
              <span class="stat-label">{{ $t('login.statOnline') }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Login Card -->
      <div class="form-container animate-item delay-1" :class="{ 'show': mounted }">
        <!-- Animated border -->
        <div class="card-border-glow"></div>

        <!-- Tab Switcher -->
        <div class="login-tabs">
          <button class="login-tab" :class="{ active: loginMode === 'password' }" @click="loginMode = 'password'">
            <svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            {{ $t('login.passwordLogin') }}
          </button>
          <button class="login-tab" :class="{ active: loginMode === 'phone' }" @click="loginMode = 'phone'">
            <svg class="tab-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
            {{ $t('login.phoneLogin') }}
          </button>
          <div class="tab-indicator" :class="{ 'phone-active': loginMode === 'phone' }"></div>
        </div>

        <el-alert v-if="kickedMsg" :title="$t('login.kickedTitle')" :description="kickedMsg" type="warning" show-icon :closable="false" style="margin-bottom: 16px" />

        <!-- Password Login -->
        <form v-if="loginMode === 'password'" class="form" @submit.prevent="handleLogin">
          <div class="input-group">
            <label for="username">{{ $t('login.username') }}</label>
            <div class="input-wrapper">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <input v-model="form.username" type="text" id="username" :placeholder="$t('login.enterUsername')" autocomplete="username" />
            </div>
          </div>
          <div class="input-group">
            <label for="password">{{ $t('login.password') }}</label>
            <div class="input-wrapper">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input v-model="form.password" type="password" id="password" :placeholder="$t('login.enterPassword')" autocomplete="current-password" @keyup.enter="handleLogin" />
            </div>
          </div>
          <div v-if="captchaRequired" class="input-group">
            <label for="captcha">{{ $t('login.captcha') }}</label>
            <div class="captcha-row">
              <div class="input-wrapper" style="flex:1">
                <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M7 7h.01M7 12h.01M7 17h.01M12 7h.01M12 12h.01M12 17h.01M17 7h.01M17 12h.01M17 17h.01"/></svg>
                <input v-model="form.captchaCode" type="text" id="captcha" :placeholder="$t('login.enterCaptcha')" @keyup.enter="handleLogin" />
              </div>
              <img v-if="captchaImg" :src="captchaImg" class="captcha-img" @click="loadCaptcha" :title="$t('login.captchaClick')" />
              <span v-else class="captcha-loading">{{ $t('login.captchaLoading') }}</span>
            </div>
          </div>
          <div v-if="error" class="error-msg">
            <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            {{ error }}
          </div>
          <button type="submit" class="sign" :disabled="loading">
            <span class="sign-text">{{ loading ? $t('login.verifying') : $t('login.signIn') }}</span>
            <span class="sign-shine"></span>
          </button>
        </form>

        <!-- Phone Login -->
        <form v-else class="form" @submit.prevent="handlePhoneLogin">
          <div class="input-group">
            <label for="phone">{{ $t('login.phoneLogin') }}</label>
            <div class="input-wrapper">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>
              <input v-model="phoneForm.phone" type="tel" id="phone" :placeholder="$t('login.phonePlaceholder')" maxlength="11" />
            </div>
          </div>
          <div class="input-group">
            <label for="unlockPwd">{{ $t('login.unlockPassword') }}</label>
            <div class="input-wrapper">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
              <input v-model="phoneForm.unlockPassword" type="password" id="unlockPwd" :placeholder="$t('login.unlockHint')" maxlength="4" />
            </div>
          </div>
          <div class="input-group">
            <button type="button" class="code-btn" @click="requestPhoneCode" :disabled="codeCooldown > 0 || phoneLoading">
              <svg class="code-btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 2l-7 20-4-9-9-4z"/></svg>
              {{ codeCooldown > 0 ? $t('login.retryAfter', { n: codeCooldown }) : $t('login.getCode') }}
            </button>
          </div>
          <div v-if="phoneError" class="error-msg">
            <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            {{ phoneError }}
          </div>
          <button type="submit" class="sign" :disabled="phoneLoading">
            <span class="sign-text">{{ phoneLoading ? $t('login.verifying') : $t('login.loginWithPhone') }}</span>
            <span class="sign-shine"></span>
          </button>
        </form>

        <div class="social-message">
          <div class="line"></div>
          <p class="message">{{ $t('login.socialLogin') }}</p>
          <div class="line"></div>
        </div>
        <div class="social-icons">
          <button aria-label="Log in with Google" class="icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><path d="M16.318 13.714v5.484h9.078c-0.37 2.354-2.745 6.901-9.078 6.901-5.458 0-9.917-4.521-9.917-10.099s4.458-10.099 9.917-10.099c3.109 0 5.193 1.318 6.38 2.464l4.339-4.182c-2.786-2.599-6.396-4.182-10.719-4.182-8.844 0-16 7.151-16 16s7.156 16 16 16c9.234 0 15.365-6.49 15.365-15.635 0-1.052-0.115-1.854-0.255-2.651z"/></svg>
          </button>
          <button aria-label="Log in with GitHub" class="icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><path d="M16 0.396c-8.839 0-16 7.167-16 16 0 7.073 4.584 13.068 10.937 15.183 0.803 0.151 1.093-0.344 1.093-0.772 0-0.38-0.009-1.385-0.015-2.719-4.453 0.964-5.391-2.151-5.391-2.151-0.729-1.844-1.781-2.339-1.781-2.339-1.448-0.989 0.115-0.968 0.115-0.968 1.604 0.109 2.448 1.645 2.448 1.645 1.427 2.448 3.744 1.74 4.661 1.328 0.14-1.031 0.557-1.74 1.011-2.135-3.552-0.401-7.287-1.776-7.287-7.907 0-1.751 0.62-3.177 1.645-4.297-0.177-0.401-0.719-2.031 0.141-4.235 0 0 1.339-0.427 4.4 1.641 1.281-0.355 2.641-0.532 4-0.541 1.36 0.009 2.719 0.187 4 0.541 3.043-2.068 4.381-1.641 4.381-1.641 0.859 2.204 0.317 3.833 0.161 4.235 1.015 1.12 1.635 2.547 1.635 4.297 0 6.145-3.74 7.5-7.296 7.891 0.556 0.479 1.077 1.464 1.077 2.959 0 2.14-0.020 3.864-0.020 4.385 0 0.416 0.28 0.916 1.104 0.755 6.4-2.093 10.979-8.093 10.979-15.156 0-8.833-7.161-16-16-16z"/></svg>
          </button>
        </div>
      </div>

      <!-- Simulated Phone Overlay -->
      <Teleport to="body">
        <div v-if="showPhoneDialog" class="phone-overlay" @click.self="closePhoneDialog">
          <div class="phone-dialog">
            <div class="phone-device">
              <div class="phone-body">
                <span class="phone-notch"></span>
                <div class="phone-screen">
                  <div class="phone-screen-title">{{ $t('login.codeLoginTitle') }}</div>
                  <div class="phone-screen-hint">{{ $t('login.codeSent') }}</div>
                  <div v-if="simCode" class="phone-code-display">
                    <span class="phone-code-label">{{ $t('login.codeLabel') }}</span>
                    <span class="phone-code-value">{{ simCode }}</span>
                  </div>
                  <div class="phone-code-inputs">
                    <input v-for="(_, i) in 6" :key="i" :ref="el => { if (el) codeInputRefs[i] = el }"
                      type="text" maxlength="1" class="code-digit"
                      :value="codeDigits[i] || ''"
                      @input="onCodeInput(i, $event)"
                      @keydown="onCodeKeydown(i, $event)"
                      @focus="$event.target.select()" />
                  </div>
                  <div v-if="phoneLoginError" class="phone-screen-error">{{ phoneLoginError }}</div>
                  <button class="phone-confirm-btn" @click="confirmPhoneLogin" :disabled="codeDigits.join('').length < 6 || phoneLoginLoading">
                    {{ phoneLoginLoading ? $t('login.phoneVerifying') : $t('login.confirmLogin') }}
                  </button>
                </div>
                <span class="phone-btn-side-right"></span>
                <span class="phone-btn-side-left"></span>
              </div>
            </div>
            <button class="phone-close-btn" @click="closePhoneDialog">&times;</button>
          </div>
        </div>
      </Teleport>
    </div>

    <!-- Footer -->
    <div class="login-footer animate-item delay-2" :class="{ 'show': mounted }">
      <span>Powered by Spring Boot & Vue 3</span>
    </div>

    <!-- 地球加载动画 -->
    <div class="earth-overlay" v-if="loading">
      <div class="earth">
        <div class="earth-loader">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><path transform="translate(100 100)" d="M29.4,-17.4C33.1,1.8,27.6,16.1,11.5,31.6C-4.7,47,-31.5,63.6,-43,56C-54.5,48.4,-50.7,16.6,-41,-10.9C-31.3,-38.4,-15.6,-61.5,-1.4,-61C12.8,-60.5,25.7,-36.5,29.4,-17.4Z" fill="#60a5fa"/></svg>
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><path transform="translate(100 100)" d="M31.7,-55.8C40.3,-50,45.9,-39.9,49.7,-29.8C53.5,-19.8,55.5,-9.9,53.1,-1.4C50.6,7.1,43.6,14.1,41.8,27.6C40.1,41.1,43.4,61.1,37.3,67C31.2,72.9,15.6,64.8,1.5,62.2C-12.5,59.5,-25,62.3,-31.8,56.7C-38.5,51.1,-39.4,37.2,-49.3,26.3C-59.1,15.5,-78,7.7,-77.6,0.2C-77.2,-7.2,-57.4,-14.5,-49.3,-28.4C-41.2,-42.4,-44.7,-63,-38.5,-70.1C-32.2,-77.2,-16.1,-70.8,-2.3,-66.9C11.6,-63,23.1,-61.5,31.7,-55.8Z" fill="#60a5fa"/></svg>
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><path transform="translate(100 100)" d="M30.6,-49.2C42.5,-46.1,57.1,-43.7,67.6,-35.7C78.1,-27.6,84.6,-13.8,80.3,-2.4C76.1,8.9,61.2,17.8,52.5,29.1C43.8,40.3,41.4,53.9,33.7,64C26,74.1,13,80.6,2.2,76.9C-8.6,73.1,-17.3,59,-30.6,52.1C-43.9,45.3,-61.9,45.7,-74.1,38.2C-86.4,30.7,-92.9,15.4,-88.6,2.5C-84.4,-10.5,-69.4,-20.9,-60.7,-34.6C-52.1,-48.3,-49.8,-65.3,-40.7,-70C-31.6,-74.8,-15.8,-67.4,-3.2,-61.8C9.3,-56.1,18.6,-52.3,30.6,-49.2Z" fill="#60a5fa"/></svg>
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><path transform="translate(100 100)" d="M39.4,-66C48.6,-62.9,51.9,-47.4,52.9,-34.3C53.8,-21.3,52.4,-10.6,54.4,1.1C56.3,12.9,61.7,25.8,57.5,33.2C53.2,40.5,39.3,42.3,28.2,46C17,49.6,8.5,55.1,1.3,52.8C-5.9,50.5,-11.7,40.5,-23.6,37.2C-35.4,34,-53.3,37.5,-62,32.4C-70.7,27.4,-70.4,13.7,-72.4,-1.1C-74.3,-15.9,-78.6,-31.9,-73.3,-43C-68.1,-54.2,-53.3,-60.5,-39.5,-60.9C-25.7,-61.4,-12.9,-56,1.1,-58C15.1,-59.9,30.2,-69.2,39.4,-66Z" fill="#60a5fa"/></svg>
        </div>
        <p>{{ $t('login.verifying') }}</p>
      </div>
    </div>

    <!-- 登录成功地球动画 -->
    <div class="earth-success-overlay" v-if="loginSuccess">
      <div class="earth-success">
        <div class="earth-success-glow"></div>
        <div class="earth-success-loader">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><path transform="translate(100 100)" d="M29.4,-17.4C33.1,1.8,27.6,16.1,11.5,31.6C-4.7,47,-31.5,63.6,-43,56C-54.5,48.4,-50.7,16.6,-41,-10.9C-31.3,-38.4,-15.6,-61.5,-1.4,-61C12.8,-60.5,25.7,-36.5,29.4,-17.4Z" fill="#4ade80"/></svg>
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><path transform="translate(100 100)" d="M31.7,-55.8C40.3,-50,45.9,-39.9,49.7,-29.8C53.5,-19.8,55.5,-9.9,53.1,-1.4C50.6,7.1,43.6,14.1,41.8,27.6C40.1,41.1,43.4,61.1,37.3,67C31.2,72.9,15.6,64.8,1.5,62.2C-12.5,59.5,-25,62.3,-31.8,56.7C-38.5,51.1,-39.4,37.2,-49.3,26.3C-59.1,15.5,-78,7.7,-77.6,0.2C-77.2,-7.2,-57.4,-14.5,-49.3,-28.4C-41.2,-42.4,-44.7,-63,-38.5,-70.1C-32.2,-77.2,-16.1,-70.8,-2.3,-66.9C11.6,-63,23.1,-61.5,31.7,-55.8Z" fill="#4ade80"/></svg>
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><path transform="translate(100 100)" d="M30.6,-49.2C42.5,-46.1,57.1,-43.7,67.6,-35.7C78.1,-27.6,84.6,-13.8,80.3,-2.4C76.1,8.9,61.2,17.8,52.5,29.1C43.8,40.3,41.4,53.9,33.7,64C26,74.1,13,80.6,2.2,76.9C-8.6,73.1,-17.3,59,-30.6,52.1C-43.9,45.3,-61.9,45.7,-74.1,38.2C-86.4,30.7,-92.9,15.4,-88.6,2.5C-84.4,-10.5,-69.4,-20.9,-60.7,-34.6C-52.1,-48.3,-49.8,-65.3,-40.7,-70C-31.6,-74.8,-15.8,-67.4,-3.2,-61.8C9.3,-56.1,18.6,-52.3,30.6,-49.2Z" fill="#4ade80"/></svg>
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><path transform="translate(100 100)" d="M39.4,-66C48.6,-62.9,51.9,-47.4,52.9,-34.3C53.8,-21.3,52.4,-10.6,54.4,1.1C56.3,12.9,61.7,25.8,57.5,33.2C53.2,40.5,39.3,42.3,28.2,46C17,49.6,8.5,55.1,1.3,52.8C-5.9,50.5,-11.7,40.5,-23.6,37.2C-35.4,34,-53.3,37.5,-62,32.4C-70.7,27.4,-70.4,13.7,-72.4,-1.1C-74.3,-15.9,-78.6,-31.9,-73.3,-43C-68.1,-54.2,-53.3,-60.5,-39.5,-60.9C-25.7,-61.4,-12.9,-56,1.1,-58C15.1,-59.9,30.2,-69.2,39.4,-66Z" fill="#4ade80"/></svg>
        </div>
        <div class="earth-success-check">
          <svg viewBox="0 0 24 24" fill="none" width="40" height="40">
            <path d="M5 13l4 4L19 7" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" class="check-path"/>
          </svg>
        </div>
        <p class="earth-success-text">{{ $t('login.loginSuccess') }}</p>
      </div>
    </div>
    <KickNotification />
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'
import KickNotification from '../../components/KickNotification.vue'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const authStore = useAuthStore()

const form = reactive({ username: '', password: '', captchaCode: '' })
const loading = ref(false)
const error = ref('')
const kickedMsg = ref('')
const loginSuccess = ref(false)
const captchaRequired = ref(false)
const captchaImg = ref('')
const captchaId = ref('')

// --- Phone Login ---
const loginMode = ref('password')
const phoneForm = reactive({ phone: '', unlockPassword: '' })
const phoneLoading = ref(false)
const phoneError = ref('')
const showPhoneDialog = ref(false)
const simCode = ref('')
const codeDigits = ref(Array(6).fill(''))
const codeInputRefs = ref([])
const phoneLoginLoading = ref(false)
const phoneLoginError = ref('')
const codeCooldown = ref(0)
let cooldownTimer = null

// --- Enhanced: Mount animation ---
const mounted = ref(false)
const tags = ['Java', 'Spring Boot', 'Vue', 'Full Stack']

// --- Enhanced: Mouse spotlight ---
const mouseX = ref(0)
const mouseY = ref(0)
const spotlightStyle = computed(() => ({
  left: `${mouseX.value}px`,
  top: `${mouseY.value}px`
}))
function onMouseMove(e) {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

// --- Typewriter Effect ---
const fullTitle = t('nav.brand')
const fullSubtitle = t('home.heroDesc')
const displayedTitle = ref('')
const displayedSubtitle = ref('')
let typeTimer = null

function startTypewriter() {
  let i = 0
  typeTimer = setInterval(() => {
    if (i < fullTitle.length) {
      displayedTitle.value += fullTitle[i]
      i++
    } else {
      clearInterval(typeTimer)
      let j = 0
      typeTimer = setInterval(() => {
        if (j < fullSubtitle.length) {
          displayedSubtitle.value += fullSubtitle[j]
          j++
        } else {
          clearInterval(typeTimer)
        }
      }, 60)
    }
  }, 120)
}

// --- Code Rain ---
const rainRef = ref(null)
const CHARS = 'const let var function return if else for while class extends import export default async await try catch throw new this super static void null true false public private protected get set => {} [] () ; : . , + - * / = ! < > & | ? ~ ^ % # @ $'.split(' ')
let rainColumns = []

function createRain() {
  const container = rainRef.value
  if (!container) return
  const cols = Math.floor(window.innerWidth / 28)
  for (let i = 0; i < cols; i++) {
    const col = document.createElement('div')
    col.className = 'rain-column'
    col.style.left = `${(i / cols) * 100}%`
    col.style.animationDuration = `${8 + Math.random() * 12}s`
    col.style.animationDelay = `${-Math.random() * 10}s`
    const charCount = 8 + Math.floor(Math.random() * 15)
    let text = ''
    for (let j = 0; j < charCount; j++) {
      text += CHARS[Math.floor(Math.random() * CHARS.length)] + '\n'
    }
    col.textContent = text
    col.style.opacity = 0.06 + Math.random() * 0.12
    container.appendChild(col)
    rainColumns.push(col)
  }
}

// --- Enhanced: Floating Particles ---
const particlesRef = ref(null)
let particleElements = []

function createParticles() {
  const container = particlesRef.value
  if (!container) return
  for (let i = 0; i < 30; i++) {
    const p = document.createElement('div')
    p.className = 'particle'
    const size = 2 + Math.random() * 3
    p.style.width = `${size}px`
    p.style.height = `${size}px`
    p.style.left = `${Math.random() * 100}%`
    p.style.top = `${Math.random() * 100}%`
    p.style.animationDuration = `${15 + Math.random() * 25}s`
    p.style.animationDelay = `${-Math.random() * 20}s`
    p.style.opacity = 0.15 + Math.random() * 0.3
    container.appendChild(p)
    particleElements.push(p)
  }
}

// --- Login Logic ---
async function loadCaptcha() {
  try {
    const data = await request.get('/admin/auth/captcha')
    captchaId.value = data.id
    captchaImg.value = data.image
    form.captchaCode = ''
  } catch {
    captchaImg.value = ''
  }
}

let loginLock = false
async function handleLogin() {
  if (loginLock) return
  if (!form.username.trim() || !form.password.trim()) {
    error.value = t('login.enterUsernameAndPassword')
    return
  }
  loginLock = true
  loading.value = true
  error.value = ''
  try {
    const payload = { username: form.username, password: form.password }
    if (captchaRequired.value) {
      payload.captchaId = captchaId.value
      payload.captchaCode = form.captchaCode
    }
    const data = await request.post('/admin/auth/login', payload)
    handleLoginSuccess(data)
  } catch (e) {
    const msg = e.message || t('login.loginFailed')
    if (msg.includes('验证码') || msg.includes('captcha') || msg.includes('1007')) {
      captchaRequired.value = true
      loadCaptcha()
      error.value = t('login.enterCaptchaRequired')
    } else {
      error.value = msg
      if (!captchaRequired.value && (msg.includes('密码') || msg.includes('password') || msg.includes('1001'))) {
        captchaRequired.value = true
        loadCaptcha()
      }
    }
  } finally {
    loading.value = false
    loginLock = false
  }
}

// --- Phone Login Functions ---
let phoneCodeLock = false
async function requestPhoneCode() {
  if (phoneCodeLock) return
  if (!phoneForm.phone.trim() || !phoneForm.unlockPassword.trim()) {
    phoneError.value = t('login.enterPhoneAndPassword')
    return
  }
  phoneCodeLock = true
  phoneLoading.value = true
  phoneError.value = ''
  try {
    const res = await request.post('/admin/phone/request-code', {
      phone: phoneForm.phone,
      unlockPassword: phoneForm.unlockPassword
    })
    simCode.value = res.code || ''
    codeDigits.value = Array(6).fill('')
    phoneLoginError.value = ''
    showPhoneDialog.value = true
    codeCooldown.value = 60
    cooldownTimer = setInterval(() => {
      codeCooldown.value--
      if (codeCooldown.value <= 0) clearInterval(cooldownTimer)
    }, 1000)
    await nextTick()
    if (codeInputRefs.value[0]) codeInputRefs.value[0].focus()
  } catch (e) {
    phoneError.value = e.message || t('login.getCodeFailed')
  } finally {
    phoneLoading.value = false
    phoneCodeLock = false
  }
}

function onCodeInput(index, event) {
  const val = event.target.value.toUpperCase().replace(/[^0-9A-Z]/g, '')
  codeDigits.value[index] = val
  if (val && index < 5) {
    nextTick(() => codeInputRefs.value[index + 1]?.focus())
  }
}

function onCodeKeydown(index, event) {
  if (event.key === 'Backspace' && !codeDigits.value[index] && index > 0) {
    codeDigits.value[index - 1] = ''
    nextTick(() => codeInputRefs.value[index - 1]?.focus())
  }
}

let phoneLoginLock = false
async function confirmPhoneLogin() {
  if (phoneLoginLock) return
  const code = codeDigits.value.join('')
  if (code.length < 6) return
  phoneLoginLock = true
  phoneLoginLoading.value = true
  phoneLoginError.value = ''
  try {
    const data = await request.post('/admin/phone/login', {
      phone: phoneForm.phone,
      code
    })
    handleLoginSuccess(data)
  } catch (e) {
    phoneLoginError.value = e.message || t('login.loginFailed')
    codeDigits.value = Array(6).fill('')
    nextTick(() => codeInputRefs.value[0]?.focus())
  } finally {
    phoneLoginLoading.value = false
    phoneLoginLock = false
  }
}

async function handlePhoneLogin() {
  if (!phoneForm.phone.trim() || !phoneForm.unlockPassword.trim()) {
    phoneError.value = t('login.enterPhoneAndPassword')
    return
  }
  if (!showPhoneDialog.value) {
    await requestPhoneCode()
    return
  }
  await confirmPhoneLogin()
}

function closePhoneDialog() {
  showPhoneDialog.value = false
  codeDigits.value = Array(6).fill('')
  phoneLoginError.value = ''
}

function handleLoginSuccess(data) {
  // Stop any stale polling from previous session before setting new tokens
  authStore.clearTokensAndStopPolling()
  authStore.setLoginData(data)
  loginSuccess.value = true
  const redirect = route.query.redirect
  const safeRedirect = redirect && typeof redirect === 'string' && redirect.startsWith('/admin') ? redirect : '/admin'
  setTimeout(() => {
    router.push(safeRedirect)
  }, 1800)
}

onMounted(() => {
  if (route.query.kicked) {
    kickedMsg.value = route.query.kicked
  }
  if (authStore.isLoggedIn) {
    const redirect = route.query.redirect
    const safeRedirect = redirect && typeof redirect === 'string' && redirect.startsWith('/admin') ? redirect : '/admin'
    router.replace(safeRedirect)
    return
  }
  createRain()
  createParticles()
  startTypewriter()
  requestAnimationFrame(() => { mounted.value = true })
})

onUnmounted(() => {
  if (typeTimer) clearInterval(typeTimer)
  if (cooldownTimer) clearInterval(cooldownTimer)
  rainColumns.forEach(col => col.remove())
  rainColumns = []
  particleElements.forEach(p => p.remove())
  particleElements = []
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  overflow: hidden;
  position: relative;
  background: linear-gradient(135deg, #0c1929 0%, #0f2847 40%, #0a1628 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* --- Grid Pattern --- */
.grid-pattern {
  position: fixed;
  inset: 0;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
  z-index: 0;
}

/* --- Noise Texture --- */
.noise-overlay {
  position: fixed;
  inset: 0;
  opacity: 0.015;
  pointer-events: none;
  z-index: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  background-repeat: repeat;
  background-size: 256px 256px;
}

/* --- Mouse Spotlight --- */
.mouse-spotlight {
  position: fixed;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.06) 0%, transparent 70%);
  pointer-events: none;
  transform: translate(-50%, -50%);
  z-index: 0;
  transition: left 0.3s ease-out, top 0.3s ease-out;
}

/* --- Code Rain --- */
.code-rain {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}
:deep(.rain-column) {
  position: absolute;
  top: -100%;
  font-family: 'Courier New', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.6;
  color: #3b8bf6;
  white-space: pre;
  writing-mode: vertical-lr;
  animation: rainFall linear infinite;
  user-select: none;
}
@keyframes rainFall {
  0% { transform: translateY(-100%); }
  100% { transform: translateY(calc(100vh + 100%)); }
}

/* --- Particles --- */
.particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}
:deep(.particle) {
  position: absolute;
  border-radius: 50%;
  background: #60a5fa;
  animation: particleFloat linear infinite;
}
@keyframes particleFloat {
  0% { transform: translate(0, 0) scale(1); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { transform: translate(calc(var(--dx, 80px) - 160px), -100vh) scale(0.3); opacity: 0; }
}
:deep(.particle:nth-child(odd)) { --dx: 40px; }
:deep(.particle:nth-child(even)) { --dx: 120px; }
:deep(.particle:nth-child(3n)) { --dx: 200px; }

/* --- Orbs --- */
.orb {
  position: fixed;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
  z-index: 0;
}
.orb-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(59,130,246,0.18), transparent 70%);
  top: -15%;
  right: -8%;
  animation: orbFloat1 20s ease-in-out infinite;
}
.orb-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(16,185,129,0.12), transparent 70%);
  bottom: -12%;
  left: -8%;
  animation: orbFloat2 18s ease-in-out infinite;
}
.orb-3 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(168,85,247,0.1), transparent 70%);
  top: 40%;
  left: 50%;
  animation: orbFloat3 22s ease-in-out infinite;
}
@keyframes orbFloat1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-40px, 30px) scale(1.08); }
}
@keyframes orbFloat2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(30px, -40px) scale(1.05); }
}
@keyframes orbFloat3 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-30px, 20px) scale(1.1); }
  66% { transform: translate(20px, -30px) scale(0.95); }
}

/* --- Staggered Entrance --- */
.animate-item {
  opacity: 0;
  transform: translateY(30px);
  transition: opacity 0.8s cubic-bezier(0.16, 1, 0.3, 1), transform 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}
.animate-item.delay-1 { transition-delay: 0.2s; }
.animate-item.delay-2 { transition-delay: 0.4s; }
.animate-item.show {
  opacity: 1;
  transform: translateY(0);
}

/* --- Back Button --- */
.back-btn {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 100;
  display: block;
  width: 48px;
  height: 48px;
  overflow: hidden;
  outline: none;
  background-color: transparent;
  cursor: pointer;
  border: 0;
  padding: 0;
}
.back-btn:before,
.back-btn:after {
  content: "";
  position: absolute;
  border-radius: 50%;
  inset: 6px;
}
.back-btn:before {
  border: 3px solid rgba(148, 163, 184, 0.25);
  transition: opacity 0.4s cubic-bezier(0.77, 0, 0.175, 1) 80ms,
    transform 0.5s cubic-bezier(0.455, 0.03, 0.515, 0.955) 80ms;
}
.back-btn:after {
  border: 3px solid #60a5fa;
  transform: scale(1.3);
  transition: opacity 0.4s cubic-bezier(0.165, 0.84, 0.44, 1),
    transform 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  opacity: 0;
}
.back-btn:hover:before,
.back-btn:focus:before {
  opacity: 0;
  transform: scale(0.7);
}
.back-btn:hover:after,
.back-btn:focus:after {
  opacity: 1;
  transform: scale(1);
}
.back-btn-box {
  display: flex;
  position: absolute;
  top: 0;
  left: 0;
}
.back-btn-elem {
  display: block;
  width: 18px;
  height: 18px;
  margin: 15px 15px 0 15px;
  transform: rotate(180deg);
  fill: #94a3b8;
  transition: fill 0.2s;
}
.back-btn:hover .back-btn-elem {
  fill: #60a5fa;
}
.back-btn:hover .back-btn-box,
.back-btn:focus .back-btn-box {
  transition: 0.4s;
  transform: translateX(-48px);
}

/* --- Container --- */
.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 60px;
  max-width: 900px;
  width: 100%;
  padding: 20px;
}

/* --- Brand Side --- */
.brand-side {
  flex: 1;
  min-width: 0;
}
.brand-content {
  padding: 20px 0;
}
.brand-title {
  font-size: 2.2rem;
  font-weight: 800;
  color: #f1f5f9;
  margin: 0 0 16px;
  letter-spacing: -0.02em;
  line-height: 1.3;
  min-height: 2.6em;
}
.typewriter-text {
  background: linear-gradient(135deg, #60a5fa, #38bdf8, #34d399);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.cursor {
  display: inline-block;
  color: #60a5fa;
  animation: blink 0.8s step-end infinite;
  font-weight: 300;
  margin-left: 2px;
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
.brand-subtitle {
  font-size: 0.95rem;
  color: #94a3b8;
  margin: 0 0 28px;
  line-height: 1.7;
  min-height: 1.6em;
}
.brand-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag {
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 500;
  color: #7dd3fc;
  background: rgba(59,130,246,0.08);
  border: 1px solid rgba(59,130,246,0.18);
  transition: background 0.25s, border-color 0.25s, color 0.25s;
  animation: tagFadeIn 0.5s ease backwards;
}
@keyframes tagFadeIn {
  from { opacity: 0; transform: translateY(8px) scale(0.9); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.tag:hover {
  background: rgba(59,130,246,0.15);
  border-color: rgba(59,130,246,0.3);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59,130,246,0.15);
}

/* --- Brand Stats --- */
.brand-stats {
  display: flex;
  gap: 24px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid rgba(148, 163, 184, 0.08);
}
.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.stat-value {
  font-size: 1.3rem;
  font-weight: 700;
  background: linear-gradient(135deg, #60a5fa, #34d399);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.stat-label {
  font-size: 0.7rem;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* --- Form Container --- */
.form-container {
  width: 380px;
  border-radius: 20px;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(16px) saturate(1.2);
  -webkit-backdrop-filter: blur(16px) saturate(1.2);
  padding: 2rem;
  color: #f1f5f9;
  border: 1px solid rgba(148, 163, 184, 0.08);
  flex-shrink: 0;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(255, 255, 255, 0.03) inset,
    0 1px 0 rgba(255, 255, 255, 0.04) inset;
  position: relative;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.form-container:hover {
  box-shadow:
    0 12px 48px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(255, 255, 255, 0.05) inset,
    0 1px 0 rgba(255, 255, 255, 0.06) inset,
    0 0 30px rgba(59, 130, 246, 0.06);
}

/* --- Card Border Glow --- */
.card-border-glow {
  position: absolute;
  inset: -1px;
  border-radius: 20px;
  background: conic-gradient(from 0deg, transparent 0%, rgba(59,130,246,0.15) 10%, transparent 20%, rgba(52,211,153,0.1) 30%, transparent 40%);
  animation: borderRotate 8s linear infinite;
  z-index: -1;
  opacity: 0;
  transition: opacity 0.5s;
}
.form-container:hover .card-border-glow {
  opacity: 1;
}
@keyframes borderRotate {
  100% { transform: rotate(360deg); }
}

.form {
  margin-top: 0.25rem;
}

/* --- Login Tabs --- */
.login-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 1.25rem;
  border-bottom: 1px solid rgba(148, 163, 184, 0.08);
  position: relative;
}
.login-tab {
  flex: 1;
  padding: 0.7rem 0;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: #64748b;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.25s, border-color 0.25s, color 0.25s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.login-tab:hover {
  color: #94a3b8;
}
.login-tab.active {
  color: #60a5fa;
}
.tab-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}
.tab-indicator {
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 50%;
  height: 2px;
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
  border-radius: 1px;
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 0 8px rgba(59,130,246,0.4);
}
.tab-indicator.phone-active {
  transform: translateX(100%);
}

/* --- Input Group --- */
.input-group {
  margin-top: 0.6rem;
  font-size: 0.875rem;
  line-height: 1.25rem;
}
.input-group label {
  display: block;
  color: #94a3b8;
  margin-bottom: 6px;
  font-size: 0.8rem;
  font-weight: 500;
  letter-spacing: 0.02em;
}
.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}
.input-icon {
  position: absolute;
  left: 12px;
  width: 16px;
  height: 16px;
  color: #475569;
  pointer-events: none;
  transition: color 0.25s;
}
.input-group input {
  width: 100%;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.1);
  outline: 0;
  background: rgba(15, 23, 42, 0.4);
  padding: 0.75rem 1rem 0.75rem 2.5rem;
  color: #f1f5f9;
  font-size: 0.875rem;
  box-sizing: border-box;
  transition: background 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s cubic-bezier(0.16, 1, 0.3, 1), transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.input-group input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1), 0 0 20px rgba(59, 130, 246, 0.05);
  background: rgba(15, 23, 42, 0.6);
}
.input-group input:focus ~ .input-icon,
.input-wrapper:focus-within .input-icon {
  color: #60a5fa;
}
.input-group input::placeholder {
  color: #3a4a5e;
}

/* --- Captcha --- */
.captcha-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.captcha-row .input-wrapper {
  flex: 1;
}
.captcha-img {
  height: 42px;
  cursor: pointer;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.15);
  transition: border-color 0.2s;
}
.captcha-img:hover {
  border-color: rgba(59, 130, 246, 0.3);
}
.captcha-loading {
  font-size: 0.75rem;
  color: #64748b;
}

/* --- Error --- */
.error-msg {
  margin-top: 0.5rem;
  font-size: 0.8rem;
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.06);
  padding: 0.5rem 0.75rem;
  border-radius: 8px;
  border: 1px solid rgba(239, 68, 68, 0.1);
  display: flex;
  align-items: center;
  gap: 8px;
  animation: errorShake 0.4s ease;
}
.error-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}
@keyframes errorShake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

/* --- Sign In Button --- */
.sign {
  display: block;
  width: 100%;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  padding: 0.8rem;
  text-align: center;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  margin-top: 1.25rem;
  transition: background 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s cubic-bezier(0.16, 1, 0.3, 1), transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
  position: relative;
  overflow: hidden;
}
.sign:hover:not(:disabled) {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.35);
}
.sign:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
}
.sign:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.sign-text {
  position: relative;
  z-index: 1;
}
.sign-shine {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  animation: btnShine 3s ease-in-out infinite;
}
@keyframes btnShine {
  0% { left: -100%; }
  50% { left: 100%; }
  100% { left: 100%; }
}

/* --- Social --- */
.social-message {
  display: flex;
  align-items: center;
  padding-top: 1.25rem;
}
.line {
  height: 1px;
  flex: 1 1 0%;
  background: rgba(148, 163, 184, 0.08);
}
.social-message .message {
  padding-left: 0.75rem;
  padding-right: 0.75rem;
  font-size: 0.75rem;
  line-height: 1.25rem;
  color: #4a5568;
}
.social-icons {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 8px;
}
.social-icons .icon {
  border-radius: 10px;
  padding: 0.7rem;
  border: 1px solid rgba(148, 163, 184, 0.06);
  background: rgba(15, 23, 42, 0.25);
  cursor: default;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s cubic-bezier(0.16, 1, 0.3, 1), transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.social-icons .icon svg {
  height: 1.15rem;
  width: 1.15rem;
  fill: #64748b;
  transition: fill 0.2s;
}
.social-icons .icon:hover {
  background: rgba(59, 130, 246, 0.06);
  border-color: rgba(59, 130, 246, 0.12);
  transform: translateY(-2px);
}
.social-icons .icon:hover svg {
  fill: #60a5fa;
}

/* --- Code Button --- */
.code-btn {
  width: 100%;
  padding: 0.7rem;
  background: rgba(59, 130, 246, 0.06);
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 10px;
  color: #60a5fa;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.3s, border-color 0.3s, color 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.code-btn-icon {
  width: 16px;
  height: 16px;
}
.code-btn:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.3);
  transform: translateY(-1px);
}
.code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* --- Footer --- */
.login-footer {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1;
  font-size: 0.7rem;
  color: #334155;
  letter-spacing: 0.05em;
}

/* --- Alert Override --- */
:deep(.el-alert) {
  border-radius: 10px;
}

/* --- Phone Overlay --- */
.phone-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 4000;
  animation: fadeIn 0.3s ease;
}
.phone-dialog {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.phone-close-btn {
  position: absolute;
  top: -40px;
  right: -20px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  font-size: 1.2rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, border-color 0.2s, color 0.2s, box-shadow 0.2s;
}
.phone-close-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

/* --- Phone Device --- */
.phone-device {
  width: 220px;
  height: 400px;
  position: relative;
  display: flex;
  justify-content: center;
  transform: scale(2);
  transform-origin: center center;
}
.phone-body {
  position: relative;
  width: 160px;
  height: 300px;
  border: 3px solid #1e293b;
  border-radius: 1.25rem;
  background: #f8fafc;
  box-shadow: 0 8px 30px rgba(0,0,0,0.4), inset 0 1px 0 rgba(255,255,255,0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
}
.phone-notch {
  display: block;
  width: 80px;
  height: 8px;
  margin-top: 8px;
  background: #1e293b;
  border-radius: 0 0 0.75rem 0.75rem;
}
.phone-btn-side-right {
  position: absolute;
  right: -10px;
  top: 56px;
  width: 4px;
  height: 28px;
  border: 3px solid #1e293b;
  border-radius: 0.375rem;
}
.phone-btn-side-left {
  position: absolute;
  right: -10px;
  bottom: 72px;
  width: 4px;
  height: 40px;
  border: 3px solid #1e293b;
  border-radius: 0.375rem;
}

/* --- Phone Screen --- */
.phone-screen {
  flex: 1;
  width: 100%;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  overflow: hidden;
}
.phone-screen-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin-top: 8px;
}
.phone-screen-hint {
  font-size: 11px;
  color: #64748b;
  text-align: center;
}
.phone-code-display {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  background: rgba(59, 130, 246, 0.06);
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 8px;
  width: 100%;
}
.phone-code-label {
  font-size: 10px;
  color: #64748b;
}
.phone-code-value {
  font-size: 13px;
  font-weight: 700;
  color: #2563eb;
  letter-spacing: 2px;
  font-family: 'Courier New', monospace;
}
.phone-code-inputs {
  display: flex;
  gap: 3px;
  margin-top: 4px;
}
.code-digit {
  width: 18px;
  height: 24px;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
  background: #fff;
  outline: none;
  transition: background 0.2s, border-color 0.2s, color 0.2s, box-shadow 0.2s;
  font-family: 'Courier New', monospace;
  padding: 0;
}
.code-digit:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12);
}
.phone-screen-error {
  font-size: 10px;
  color: #ef4444;
  text-align: center;
  padding: 3px 6px;
  background: rgba(239, 68, 68, 0.06);
  border-radius: 6px;
  width: 100%;
}
.phone-confirm-btn {
  width: 100%;
  padding: 7px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s, box-shadow 0.2s;
  margin-top: auto;
}
.phone-confirm-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
}
.phone-confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ====== 地球动画 ====== */
.earth-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.earth {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}
.earth p {
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  padding-top: 0.25em;
  font-size: 1.25em;
  letter-spacing: 2px;
}
.earth-loader {
  --watercolor: #0c2d4a;
  --landcolor: #60a5fa;
  width: 7.5em;
  height: 7.5em;
  background-color: var(--watercolor);
  position: relative;
  overflow: hidden;
  border-radius: 50%;
  box-shadow:
    inset 0em 0.5em rgb(255, 255, 255, 0.2),
    inset 0em -0.5em rgb(0, 0, 0, 0.2);
  border: solid 0.15em rgba(255,255,255,0.6);
  animation: startround 1s;
  animation-iteration-count: 1;
}
.earth-loader svg:nth-child(1) {
  position: absolute;
  bottom: -2em;
  width: 7em;
  height: auto;
  animation: round1 5s infinite linear 0.75s;
}
.earth-loader svg:nth-child(2) {
  position: absolute;
  top: -3em;
  width: 7em;
  height: auto;
  animation: round1 5s infinite linear;
}
.earth-loader svg:nth-child(3) {
  position: absolute;
  top: -2.5em;
  width: 7em;
  height: auto;
  animation: round2 5s infinite linear;
}
.earth-loader svg:nth-child(4) {
  position: absolute;
  bottom: -2.2em;
  width: 7em;
  height: auto;
  animation: round2 5s infinite linear 0.75s;
}
@keyframes startround {
  0% { filter: brightness(500%); box-shadow: none; }
  75% { filter: brightness(500%); box-shadow: none; }
  100% {
    filter: brightness(100%);
    box-shadow: inset 0em 0.5em rgb(255, 255, 255, 0.2), inset 0em -0.5em rgb(0, 0, 0, 0.2);
  }
}

/* --- Success Overlay --- */
.earth-success-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  animation: fadeIn 0.3s ease;
}
.earth-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  position: relative;
}
.earth-success-glow {
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(52, 211, 153, 0.3) 0%, transparent 70%);
  animation: successGlow 1.8s ease-in-out infinite;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -60%);
}
@keyframes successGlow {
  0%, 100% { opacity: 0.5; transform: translate(-50%, -60%) scale(0.8); }
  50% { opacity: 1; transform: translate(-50%, -60%) scale(1.2); }
}
.earth-success-loader {
  --watercolor: #0d9488;
  --landcolor: #34d399;
  width: 10em;
  height: 10em;
  background-color: var(--watercolor);
  position: relative;
  overflow: hidden;
  border-radius: 50%;
  box-shadow:
    inset 0 0.5em rgba(255, 255, 255, 0.2),
    inset 0 -0.5em rgba(0, 0, 0, 0.25),
    0 0 30px rgba(52, 211, 153, 0.4),
    0 0 60px rgba(52, 211, 153, 0.15);
  animation: earthZoomIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}
@keyframes earthZoomIn {
  0% { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
.earth-success-loader svg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
.earth-success-loader svg:nth-child(1) {
  position: absolute;
  bottom: -2em;
  width: 10em;
  height: auto;
  animation: round1 5s infinite linear 0.75s;
}
.earth-success-loader svg:nth-child(2) {
  position: absolute;
  top: -3em;
  width: 10em;
  height: auto;
  animation: round1 5s infinite linear;
}
.earth-success-loader svg:nth-child(3) {
  position: absolute;
  top: -2.5em;
  width: 10em;
  height: auto;
  animation: round2 5s infinite linear;
}
.earth-success-loader svg:nth-child(4) {
  position: absolute;
  bottom: -2.2em;
  width: 10em;
  height: auto;
  animation: round2 5s infinite linear 0.75s;
}
.earth-success-check {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -60%);
  animation: checkPop 0.4s 0.3s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}
.check-path {
  stroke-dasharray: 30;
  stroke-dashoffset: 30;
  animation: drawCheck 0.5s 0.5s ease forwards;
}
@keyframes drawCheck {
  to { stroke-dashoffset: 0; }
}
@keyframes checkPop {
  0% { transform: translate(-50%, -60%) scale(0); }
  100% { transform: translate(-50%, -60%) scale(1); }
}
.earth-success-text {
  color: #34d399;
  font-size: 1.4em;
  font-weight: 600;
  letter-spacing: 3px;
  animation: textSlideUp 0.5s 0.6s ease both;
}
@keyframes textSlideUp {
  0% { opacity: 0; transform: translateY(15px); }
  100% { opacity: 1; transform: translateY(0); }
}

@keyframes round1 {
  0% { left: -2em; opacity: 100%; transform: skewX(0deg) rotate(0deg); }
  30% { left: -6em; opacity: 100%; transform: skewX(-25deg) rotate(25deg); }
  31% { left: -6em; opacity: 0%; transform: skewX(-25deg) rotate(25deg); }
  35% { left: 7em; opacity: 0%; transform: skewX(25deg) rotate(-25deg); }
  45% { left: 7em; opacity: 100%; transform: skewX(25deg) rotate(-25deg); }
  100% { left: -2em; opacity: 100%; transform: skewX(0deg) rotate(0deg); }
}
@keyframes round2 {
  0% { left: 5em; opacity: 100%; transform: skewX(0deg) rotate(0deg); }
  75% { left: -7em; opacity: 100%; transform: skewX(-25deg) rotate(25deg); }
  76% { left: -7em; opacity: 0%; transform: skewX(-25deg) rotate(25deg); }
  77% { left: 8em; opacity: 0%; transform: skewX(25deg) rotate(-25deg); }
  80% { left: 8em; opacity: 100%; transform: skewX(25deg) rotate(-25deg); }
  100% { left: 5em; opacity: 100%; transform: skewX(0deg) rotate(0deg); }
}

/* --- Responsive --- */
@media (max-width: 768px) {
  .login-container {
    flex-direction: column;
    gap: 32px;
  }
  .brand-side {
    text-align: center;
  }
  .brand-tags {
    justify-content: center;
  }
  .brand-stats {
    justify-content: center;
  }
  .brand-title {
    font-size: 1.5rem;
  }
  .form-container {
    width: 100%;
    max-width: 380px;
  }
  .phone-device {
    transform: scale(0.9);
  }
  .mouse-spotlight {
    display: none;
  }
}
</style>
