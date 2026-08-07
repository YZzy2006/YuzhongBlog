<template>
  <div class="ai-assistant" v-if="!authStore.isLoggedIn">
    <!-- Toggle Button -->
    <button class="ai-toggle-btn" :class="{ active: open }" @click="open = !open" :title="$t('aiChat.title')">
      <div class="weather-icon">
        <div class="cloud front">
          <span class="left-front"></span>
          <span class="right-front"></span>
        </div>
        <span class="sun sunshine"></span>
        <span class="sun"></span>
        <div class="cloud back">
          <span class="left-back"></span>
          <span class="right-back"></span>
        </div>
      </div>
    </button>

    <!-- Chat Panel -->
    <Transition name="panel-slide">
      <div v-if="open" class="ai-panel">
        <!-- Header -->
        <div class="ai-panel-header">
          <div class="ai-panel-title">
            <span v-if="!streaming" class="title-icon">✨</span>
            <div v-else class="ai-loader">
              <svg width="100" height="100" viewBox="0 0 100 100">
                <defs>
                  <mask id="clipping">
                    <polygon points="0,0 100,0 100,100 0,100" fill="black"></polygon>
                    <polygon points="25,25 75,25 50,75" fill="white"></polygon>
                    <polygon points="50,25 75,75 25,75" fill="white"></polygon>
                    <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                    <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                    <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                    <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                  </mask>
                </defs>
              </svg>
              <div class="ai-loader-box"></div>
            </div>
            <span>{{ $t('aiChat.title') }}</span>
          </div>
          <button class="ai-panel-close" @click="open = false">&times;</button>
        </div>

        <!-- Messages -->
        <div class="ai-panel-messages" ref="messagesRef">
          <!-- Welcome -->
          <div v-if="messages.length === 0" class="ai-welcome">
            <p class="welcome-text">{{ $t('aiChat.welcome') }}</p>
            <div class="quick-questions">
              <button v-for="(q, qi) in quickQuestions" :key="qi" class="quick-btn" @click="handleQuickQuestion(q, qi)">
                {{ q }}
              </button>
            </div>
          </div>

          <!-- Message List -->
          <div v-for="(msg, i) in messages" :key="i" class="ai-msg" :class="msg.role">
            <div v-if="msg.role === 'user'" class="msg-bubble user-bubble">
              {{ msg.content }}
            </div>
            <div v-else class="msg-bubble ai-bubble">
              <div v-if="msg.searchResults" class="search-results">
                <div v-for="article in msg.searchResults" :key="article.id"
                  class="search-result-card" @click="openArticle(article.slug)">
                  <div class="result-title">{{ article.title }}</div>
                  <div v-if="article.summary" class="result-summary">{{ stripMarkdown(article.summary) }}</div>
                  <div class="result-meta">
                    <span v-if="article.categoryName" class="result-category">{{ article.categoryName }}</span>
                    <span class="result-stats">{{ article.viewCount || 0 }} {{ $t('aiChat.reads') }} · {{ article.likeCount || 0 }} {{ $t('aiChat.likes') }}</span>
                  </div>
                </div>
              </div>
              <MdPreview v-else :modelValue="msg.content" previewTheme="github" :codeFoldable="false" />
            </div>

            <!-- Disambiguate options -->
            <div v-if="msg.disambiguate" class="disambiguate-card">
              <p class="disambiguate-hint">{{ $t('aiChat.disambiguateHint') }}</p>
              <div class="disambiguate-options">
                <!-- Site search option -->
                <label class="neon-checkbox" :class="{ 'is-checked': msg.selectedChoice === 'site', 'is-hidden': msg.selectedChoice && msg.selectedChoice !== 'site' }"
                  @click.prevent="!msg.selectedChoice && handleDisambiguate(msg, 'site')">
                  <input type="checkbox" :checked="msg.selectedChoice === 'site'" />
                  <div class="neon-checkbox__frame">
                    <div class="neon-checkbox__box">
                      <div class="neon-checkbox__check-container">
                        <svg viewBox="0 0 24 24" class="neon-checkbox__check">
                          <path d="M3,12.5l7,7L21,5"></path>
                        </svg>
                      </div>
                      <div class="neon-checkbox__glow"></div>
                      <div class="neon-checkbox__borders">
                        <span></span><span></span><span></span><span></span>
                      </div>
                    </div>
                    <div class="neon-checkbox__effects">
                      <div class="neon-checkbox__particles">
                        <span></span><span></span><span></span><span></span>
                        <span></span><span></span><span></span><span></span>
                        <span></span><span></span><span></span><span></span>
                      </div>
                      <div class="neon-checkbox__rings">
                        <div class="ring"></div><div class="ring"></div><div class="ring"></div>
                      </div>
                      <div class="neon-checkbox__sparks">
                        <span></span><span></span><span></span><span></span>
                      </div>
                    </div>
                  </div>
                  <span class="disambiguate-label">{{ msg.disambiguate.site }}</span>
                </label>
                <!-- Knowledge option -->
                <label class="neon-checkbox" :class="{ 'is-checked': msg.selectedChoice === 'knowledge', 'is-hidden': msg.selectedChoice && msg.selectedChoice !== 'knowledge' }"
                  @click.prevent="!msg.selectedChoice && handleDisambiguate(msg, 'knowledge')">
                  <input type="checkbox" :checked="msg.selectedChoice === 'knowledge'" />
                  <div class="neon-checkbox__frame">
                    <div class="neon-checkbox__box">
                      <div class="neon-checkbox__check-container">
                        <svg viewBox="0 0 24 24" class="neon-checkbox__check">
                          <path d="M3,12.5l7,7L21,5"></path>
                        </svg>
                      </div>
                      <div class="neon-checkbox__glow"></div>
                      <div class="neon-checkbox__borders">
                        <span></span><span></span><span></span><span></span>
                      </div>
                    </div>
                    <div class="neon-checkbox__effects">
                      <div class="neon-checkbox__particles">
                        <span></span><span></span><span></span><span></span>
                        <span></span><span></span><span></span><span></span>
                        <span></span><span></span><span></span><span></span>
                      </div>
                      <div class="neon-checkbox__rings">
                        <div class="ring"></div><div class="ring"></div><div class="ring"></div>
                      </div>
                      <div class="neon-checkbox__sparks">
                        <span></span><span></span><span></span><span></span>
                      </div>
                    </div>
                  </div>
                  <span class="disambiguate-label">{{ msg.disambiguate.knowledge }}</span>
                </label>
              </div>
            </div>
          </div>

          <!-- Streaming indicator -->
          <div v-if="streaming" class="ai-msg assistant">
            <div class="msg-bubble ai-bubble">
              <div v-if="!streamContent" class="typing-dots">
                <span></span><span></span><span></span>
              </div>
              <MdPreview v-else :modelValue="streamContent" previewTheme="github" :codeFoldable="false" />
            </div>
          </div>

          <!-- Searching indicator -->
          <div v-if="searching" class="ai-msg assistant">
            <div class="msg-bubble ai-bubble">
              <div class="searching-indicator">
                <span class="search-pulse">🔍</span>
                <span>{{ $t('aiChat.searchingArticles') }}</span>
              </div>
            </div>
          </div>

          <!-- Error -->
          <div v-if="errorMsg" class="ai-error-msg">{{ errorMsg }}</div>
        </div>

        <!-- Input -->
        <div class="ai-panel-input">
          <input v-model="inputText" type="text" :placeholder="$t('aiChat.inputPlaceholder')"
            @keyup.enter="sendMessage()" :disabled="streaming || searching" />
          <button class="search-btn" @click="searchArticles()"
            :disabled="searching || streaming || !inputText.trim()" :title="$t('aiChat.searchArticles')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
          </button>
          <button class="send-btn" @click="sendMessage()" :disabled="streaming || searching || !inputText.trim()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 2 11 13"/><path d="M22 2 15 22 11 13 2 9z"/></svg>
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/auth'
import { aiChatStream, smartSearch } from '../utils/ai'
import { stripMarkdown } from '../utils/stripMarkdown'
import { checkContent } from '../utils/contentFilter'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import '../utils/mdEditorConfig'

const authStore = useAuthStore()
const router = useRouter()
const { t, tm } = useI18n()
const open = ref(false)
const messages = ref([])
const inputText = ref('')
const streaming = ref(false)
const streamContent = ref('')
const searching = ref(false)
const errorMsg = ref('')
const messagesRef = ref(null)
let abortFn = null

const quickQuestions = computed(() => {
  const val = tm('aiChat.quickQuestions')
  return Array.isArray(val) ? val : []
})

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function sendMessage(text) {
  const msg = typeof text === 'string' ? text : inputText.value.trim()
  if (!msg || streaming.value) return

  // 前端即时过滤（用户端）
  const check = checkContent(msg)
  if (check.blocked) {
    errorMsg.value = check.message
    return
  }

  if (abortFn) { abortFn(); abortFn = null }
  messages.value.push({ role: 'user', content: msg })
  inputText.value = ''
  streaming.value = true
  streamContent.value = ''
  errorMsg.value = ''
  scrollToBottom()

  // Build conversation history for multi-turn (exclude search results)
  const apiMessages = messages.value
    .filter(m => (m.role === 'user' || m.role === 'assistant') && !m.searchResults)
    .map(m => ({ role: m.role, content: m.content || '' }))

  abortFn = aiChatStream(apiMessages, {
    onChunk(content) {
      streamContent.value += content
      scrollToBottom()
    },
    onDone() {
      const parsed = parseDisambiguate(streamContent.value)
      let disambiguate = parsed.disambiguate
      // Frontend fallback: if AI didn't emit marker but input looks ambiguous
      if (!disambiguate && isAmbiguousQuery(msg)) {
        const kw = msg.trim()
        disambiguate = { site: t('aiChat.searchSite', { keyword: kw }), knowledge: t('aiChat.searchKnowledge', { keyword: kw }) }
      }
      messages.value.push({ role: 'assistant', content: parsed.content, disambiguate })
      streamContent.value = ''
      streaming.value = false
      scrollToBottom()
    },
    onError(e) {
      errorMsg.value = e.message || t('aiChat.aiUnavailable')
      if (streamContent.value) {
        const parsed = parseDisambiguate(streamContent.value)
        messages.value.push({ role: 'assistant', content: parsed.content, disambiguate: parsed.disambiguate })
        streamContent.value = ''
      }
      streaming.value = false
      scrollToBottom()
    }
  })
}

async function searchArticles(query) {
  const q = query || inputText.value.trim()
  if (!q || searching.value || streaming.value) return

  messages.value.push({ role: 'user', content: `🔍 ${q}` })
  inputText.value = ''
  searching.value = true
  errorMsg.value = ''
  scrollToBottom()

  try {
    const data = await smartSearch(q)
    const articles = data.content || []
    if (articles.length === 0) {
      messages.value.push({ role: 'assistant', content: t('aiChat.noResults', { query: q }) })
    } else {
      messages.value.push({ role: 'assistant', content: '', searchResults: articles })
    }
  } catch (e) {
    errorMsg.value = t('aiChat.searchFailed')
  } finally {
    searching.value = false
    scrollToBottom()
  }
}

function handleQuickQuestion(q, index) {
  if (index === 0) {
    inputText.value = q
    searchArticles(q)
  } else {
    sendMessage(q)
  }
}

function openArticle(slug) {
  router.push(`/articles/${slug}`)
  open.value = false
}

const DISAMBIGUATE_RE = /<!--\s*DISAMBIGUATE:\s*(\{.*?\})\s*-->/

const TECH_KEYWORDS = /^(java|python|vue|react|spring|redis|docker|mysql|javascript|typescript|go\b|rust|c\+\+|kotlin|swift|node|angular|webpack|git|linux|nginx|kafka|rabbitmq|elasticsearch|mongodb|postgresql|css|html|sql|hibernate|mybatis|flask|django|express|nextjs|nuxt|sass|less|tailwind|bootstrap|jquery|php|ruby|scala|haskell|lua|perl|matlab|r语言|flutter|dart|swiftui|android|ios|flutter|unity|unreal|微服务|消息队列|缓存|数据库|前端|后端|全栈|devops|ci\/cd|k8s|kubernetes|terraform|aws|azure|gcp)$/i

function isAmbiguousQuery(text) {
  const t = text.trim()
  if (t.length > 30) return false
  if (/[？?！!。.]/.test(t)) return false
  if (/搜索|查找|找一下|有没有|推荐|怎么|如何|什么是|解释|区别|对比|教程|search|find|how|what|why|explain|compare|tutorial|guide|recommend|show/.test(t)) return false
  if (TECH_KEYWORDS.test(t)) return true
  // Single word or two short words
  const words = t.split(/\s+/)
  return words.length <= 2 && t.length <= 15
}

function parseDisambiguate(content) {
  const match = content.match(DISAMBIGUATE_RE)
  if (!match) return { content, disambiguate: null }
  try {
    const data = JSON.parse(match[1])
    const clean = content.replace(DISAMBIGUATE_RE, '').trim()
    return { content: clean, disambiguate: data }
  } catch {
    return { content, disambiguate: null }
  }
}

function extractKeyword(text) {
  const m = text.match(/「(.+?)」/) || text.match(/"(.+?)"/)
  return m ? m[1] : text
}

function handleDisambiguate(msg, choice) {
  const keyword = extractKeyword(msg.disambiguate.site)
  msg.selectedChoice = choice
  setTimeout(() => {
    if (choice === 'site') {
      searchArticles(keyword)
    } else {
      sendMessage(t('aiChat.introPrompt', { keyword }))
    }
  }, 600)
}

onBeforeUnmount(() => {
  if (abortFn) abortFn()
})
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  top: 80px;
  right: 24px;
  z-index: 10001;
}

/* --- Toggle Button --- */
.ai-toggle-btn {
  width: 112px;
  height: 112px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #e0f2fe, #bae6fd);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(56, 189, 248, 0.3);
  transition: transform 0.3s, box-shadow 0.3s;
  padding: 0;
}
.ai-toggle-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 20px rgba(56, 189, 248, 0.45);
}
.ai-toggle-btn.active {
  transform: scale(0.95);
}

/* Weather icon inside button */
.weather-icon {
  width: 112px;
  height: 112px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: scale(0.44);
  transform-origin: center center;
}
.weather-icon .cloud {
  width: 250px;
  position: absolute;
}
.weather-icon .front {
  padding-top: 45px;
  margin-left: -15px;
  z-index: 11;
  animation: clouds 8s infinite ease-in-out;
}
.weather-icon .back {
  margin-top: -30px;
  margin-left: 100px;
  z-index: 12;
  animation: clouds 12s infinite ease-in-out;
}
.weather-icon .right-front {
  width: 45px;
  height: 45px;
  border-radius: 50% 50% 50% 0%;
  background-color: #4c9beb;
  display: inline-block;
  margin-left: -25px;
  z-index: 5;
}
.weather-icon .left-front {
  width: 65px;
  height: 65px;
  border-radius: 50% 50% 0% 50%;
  background-color: #4c9beb;
  display: inline-block;
  z-index: 5;
}
.weather-icon .right-back {
  width: 50px;
  height: 50px;
  border-radius: 50% 50% 50% 0%;
  background-color: #4c9beb;
  display: inline-block;
  margin-left: -20px;
  z-index: 5;
}
.weather-icon .left-back {
  width: 30px;
  height: 30px;
  border-radius: 50% 50% 0% 50%;
  background-color: #4c9beb;
  display: inline-block;
  z-index: 5;
}
.weather-icon .sun {
  width: 120px;
  height: 120px;
  background: linear-gradient(to right, #fcbb04, #fffc00);
  border-radius: 60px;
  position: absolute;
  left: 20px;
  top: 0;
}
.weather-icon .sunshine {
  animation: sunshines 2s infinite;
}
@keyframes sunshines {
  0% { transform: scale(1); opacity: 0.6; }
  100% { transform: scale(1.4); opacity: 0; }
}
@keyframes clouds {
  0% { transform: translateX(15px); }
  50% { transform: translateX(0px); }
  100% { transform: translateX(15px); }
}

/* --- Chat Panel --- */
.ai-panel {
  position: absolute;
  top: 120px;
  right: 0;
  width: 370px;
  max-height: 520px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12), 0 0 0 1px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
.panel-slide-enter-from,
.panel-slide-leave-to {
  opacity: 0;
  transform: translateY(-12px) scale(0.95);
}

/* Header */
.ai-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border-light);
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.06), rgba(99, 102, 241, 0.06));
}
.ai-panel-title {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text);
  display: flex;
  align-items: center;
  gap: 6px;
}
.title-icon {
  font-size: 0.85rem;
  line-height: 1;
}

/* AI Loader */
.ai-loader {
  --color-one: #ffbf48;
  --color-two: #be4a1d;
  --color-three: #ffbf4780;
  --color-four: #bf4a1d80;
  --color-five: #ffbf4740;
  --time-animation: 2s;
  width: 20px;
  height: 20px;
  position: relative;
  border-radius: 50%;
  box-shadow:
    0 0 8px 0 var(--color-three),
    0 4px 12px 0 var(--color-four);
  animation: ai-colorize calc(var(--time-animation) * 3) ease-in-out infinite;
  flex-shrink: 0;
}
.ai-loader::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border-top: solid 1px var(--color-one);
  border-bottom: solid 1px var(--color-two);
  background: linear-gradient(180deg, var(--color-five), var(--color-four));
  box-shadow:
    inset 0 2px 3px 0 var(--color-three),
    inset 0 -2px 3px 0 var(--color-four);
}
.ai-loader-box {
  width: 20px;
  height: 20px;
  background: linear-gradient(180deg, var(--color-one) 30%, var(--color-two) 70%);
  mask: url(#clipping);
  -webkit-mask: url(#clipping);
}
.ai-loader svg {
  position: absolute;
  width: 20px;
  height: 20px;
}
.ai-loader svg #clipping {
  filter: contrast(15);
  animation: ai-roundness calc(var(--time-animation) / 2) linear infinite;
}
.ai-loader svg #clipping polygon {
  filter: blur(7px);
}
.ai-loader svg #clipping polygon:nth-child(1) {
  transform-origin: 75% 25%;
  transform: rotate(90deg);
}
.ai-loader svg #clipping polygon:nth-child(2) {
  transform-origin: 50% 50%;
  animation: ai-rotation var(--time-animation) linear infinite reverse;
}
.ai-loader svg #clipping polygon:nth-child(3) {
  transform-origin: 50% 60%;
  animation: ai-rotation var(--time-animation) linear infinite;
  animation-delay: calc(var(--time-animation) / -3);
}
.ai-loader svg #clipping polygon:nth-child(4) {
  transform-origin: 40% 40%;
  animation: ai-rotation var(--time-animation) linear infinite reverse;
}
.ai-loader svg #clipping polygon:nth-child(5) {
  transform-origin: 40% 40%;
  animation: ai-rotation var(--time-animation) linear infinite reverse;
  animation-delay: calc(var(--time-animation) / -2);
}
.ai-loader svg #clipping polygon:nth-child(6) {
  transform-origin: 60% 40%;
  animation: ai-rotation var(--time-animation) linear infinite;
}
.ai-loader svg #clipping polygon:nth-child(7) {
  transform-origin: 60% 40%;
  animation: ai-rotation var(--time-animation) linear infinite;
  animation-delay: calc(var(--time-animation) / -1.5);
}
@keyframes ai-rotation {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
@keyframes ai-roundness {
  0% { filter: contrast(15); }
  20% { filter: contrast(3); }
  40% { filter: contrast(3); }
  60% { filter: contrast(15); }
  100% { filter: contrast(15); }
}
@keyframes ai-colorize {
  0% { filter: hue-rotate(0deg); }
  20% { filter: hue-rotate(-30deg); }
  40% { filter: hue-rotate(-60deg); }
  60% { filter: hue-rotate(-90deg); }
  80% { filter: hue-rotate(-45deg); }
  100% { filter: hue-rotate(0deg); }
}
.ai-panel-close {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: var(--color-text-secondary);
  cursor: pointer;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all var(--transition-fast);
}
.ai-panel-close:hover {
  background: var(--color-bg-muted);
  color: var(--color-text);
}

/* Messages */
.ai-panel-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  max-height: 380px;
  scroll-behavior: smooth;
}
.ai-panel-messages::-webkit-scrollbar {
  width: 4px;
}
.ai-panel-messages::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 4px;
}

/* Welcome */
.ai-welcome {
  text-align: center;
}
.welcome-text {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
  margin-bottom: 1rem;
  line-height: 1.6;
}
.quick-questions {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.quick-btn {
  display: block;
  width: 100%;
  padding: 0.55rem 0.85rem;
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  color: var(--color-text);
  cursor: pointer;
  text-align: left;
  transition: all var(--transition-fast);
}
.quick-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(59, 130, 246, 0.04);
}

/* Messages */
.ai-msg {
  margin-bottom: 0.75rem;
}
.ai-msg.user {
  display: flex;
  justify-content: flex-end;
}
.msg-bubble {
  max-width: 85%;
  font-size: 0.82rem;
  line-height: 1.6;
}
.user-bubble {
  background: var(--color-primary);
  color: #fff;
  padding: 0.5rem 0.85rem;
  border-radius: var(--radius-md) var(--radius-md) 4px var(--radius-md);
  word-break: break-word;
}
.ai-bubble {
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border-light);
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-md) var(--radius-md) var(--radius-md) 4px;
  color: var(--color-text);
}
.ai-bubble :deep(.md-editor) {
  background: transparent;
  border: none;
}
.ai-bubble :deep(.md-editor-preview) {
  font-size: 0.82rem;
  line-height: 1.6;
}
.ai-bubble :deep(.md-editor-preview p) {
  margin-bottom: 0.3rem;
}
.ai-bubble :deep(.md-editor-preview pre) {
  font-size: 0.78rem;
  background: rgba(241, 245, 249, 0.6);
  border: 1px solid var(--color-border-light);
}

/* Typing dots */
.typing-dots {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}
.typing-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-text-secondary);
  animation: dotBounce 1.4s ease-in-out infinite;
}
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes dotBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* Error */
.ai-error-msg {
  font-size: 0.8rem;
  color: #dc2626;
  background: rgba(239, 68, 68, 0.06);
  padding: 0.4rem 0.65rem;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(239, 68, 68, 0.15);
  margin-top: 0.5rem;
}

/* Input */
.ai-panel-input {
  display: flex;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-top: 1px solid var(--color-border-light);
  background: var(--color-bg);
}
.ai-panel-input input {
  flex: 1;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0.5rem 0.75rem;
  font-size: 0.82rem;
  background: var(--color-bg);
  color: var(--color-text);
  outline: none;
  transition: border-color var(--transition-fast);
}
.ai-panel-input input:focus {
  border-color: var(--color-primary);
}
.ai-panel-input input:disabled {
  opacity: 0.6;
}
.send-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}
.send-btn svg {
  width: 16px;
  height: 16px;
}
.send-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
}
.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Search button */
.search-btn {
  width: 36px;
  height: 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg);
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}
.search-btn svg {
  width: 16px;
  height: 16px;
}
.search-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.search-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Search results */
.search-results {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.search-result-card {
  padding: 0.6rem 0.75rem;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.search-result-card:hover {
  border-color: var(--color-primary);
  background: rgba(59, 130, 246, 0.04);
}
.result-title {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 0.25rem;
  line-height: 1.4;
}
.result-summary {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.result-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.35rem;
  font-size: 0.7rem;
}
.result-category {
  background: var(--color-primary-light);
  color: var(--color-primary);
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 0.68rem;
}
.result-stats {
  color: var(--color-text-tertiary);
}

/* Searching indicator */
.searching-indicator {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.82rem;
  color: var(--color-text-secondary);
}
.search-pulse {
  animation: searchPulse 1.5s ease-in-out infinite;
}
@keyframes searchPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* Disambiguate card */
.disambiguate-card {
  margin-top: 0.5rem;
  padding: 0.6rem 0.75rem;
  background: rgba(0, 255, 170, 0.04);
  border: 1px solid rgba(0, 255, 170, 0.2);
  border-radius: var(--radius-md);
}
.disambiguate-hint {
  font-size: 0.78rem;
  color: var(--color-text-secondary);
  margin-bottom: 0.6rem;
  line-height: 1.5;
}
.disambiguate-options {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.disambiguate-label {
  font-size: 0.82rem;
  color: var(--color-text);
  cursor: pointer;
  transition: color 0.2s;
}
.neon-checkbox:hover .disambiguate-label {
  color: #00ffaa;
}

/* Neon Checkbox */
.neon-checkbox {
  --primary: #00ffaa;
  --primary-dark: #00cc88;
  --primary-light: #88ffdd;
  --size: 24px;
  position: relative;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  display: flex;
  align-items: center;
  gap: 8px;
}
.neon-checkbox input {
  display: none;
}
.neon-checkbox__frame {
  position: relative;
  width: var(--size);
  height: var(--size);
  flex-shrink: 0;
}
.neon-checkbox__box {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  border-radius: 4px;
  border: 2px solid var(--primary-dark);
  transition: all 0.4s ease;
}
.neon-checkbox__check-container {
  position: absolute;
  inset: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.neon-checkbox__check {
  width: 80%;
  height: 80%;
  fill: none;
  stroke: var(--primary);
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 40;
  stroke-dashoffset: 40;
  transform-origin: center;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.neon-checkbox__glow {
  position: absolute;
  inset: -2px;
  border-radius: 6px;
  background: var(--primary);
  opacity: 0;
  filter: blur(8px);
  transform: scale(1.2);
  transition: all 0.4s ease;
}
.neon-checkbox__borders {
  position: absolute;
  inset: 0;
  border-radius: 4px;
  overflow: hidden;
}
.neon-checkbox__borders span {
  position: absolute;
  width: 40px;
  height: 1px;
  background: var(--primary);
  opacity: 0;
  transition: opacity 0.4s ease;
}
.neon-checkbox__borders span:nth-child(1) { top: 0; left: -100%; animation: borderFlow1 2s linear infinite; }
.neon-checkbox__borders span:nth-child(2) { top: -100%; right: 0; width: 1px; height: 40px; animation: borderFlow2 2s linear infinite; }
.neon-checkbox__borders span:nth-child(3) { bottom: 0; right: -100%; animation: borderFlow3 2s linear infinite; }
.neon-checkbox__borders span:nth-child(4) { bottom: -100%; left: 0; width: 1px; height: 40px; animation: borderFlow4 2s linear infinite; }
.neon-checkbox__particles span {
  position: absolute;
  width: 4px;
  height: 4px;
  background: var(--primary);
  border-radius: 50%;
  opacity: 0;
  pointer-events: none;
  top: 50%;
  left: 50%;
  box-shadow: 0 0 6px var(--primary);
}
.neon-checkbox__rings {
  position: absolute;
  inset: -20px;
  pointer-events: none;
}
.neon-checkbox__rings .ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 1px solid var(--primary);
  opacity: 0;
  transform: scale(0);
}
.neon-checkbox__sparks span {
  position: absolute;
  width: 20px;
  height: 1px;
  background: linear-gradient(90deg, var(--primary), transparent);
  opacity: 0;
}
.neon-checkbox:hover .neon-checkbox__box {
  border-color: var(--primary);
  transform: scale(1.05);
}
.neon-checkbox input:checked ~ .neon-checkbox__frame .neon-checkbox__box {
  border-color: var(--primary);
  background: rgba(0, 255, 170, 0.1);
}
.neon-checkbox input:checked ~ .neon-checkbox__frame .neon-checkbox__check {
  stroke-dashoffset: 0;
  transform: scale(1.1);
}
.neon-checkbox input:checked ~ .neon-checkbox__frame .neon-checkbox__glow {
  opacity: 0.2;
}
.neon-checkbox input:checked ~ .neon-checkbox__frame .neon-checkbox__borders span {
  opacity: 1;
}
.neon-checkbox input:checked ~ .neon-checkbox__frame .neon-checkbox__particles span {
  animation: particleExplosion 0.6s ease-out forwards;
}
.neon-checkbox input:checked ~ .neon-checkbox__frame .neon-checkbox__rings .ring {
  animation: ringPulse 0.6s ease-out forwards;
}
.neon-checkbox input:checked ~ .neon-checkbox__frame .neon-checkbox__sparks span {
  animation: sparkFlash 0.6s ease-out forwards;
}

/* is-checked: same as :checked but driven by class */
.neon-checkbox.is-checked .neon-checkbox__box {
  border-color: var(--primary);
  background: rgba(0, 255, 170, 0.1);
}
.neon-checkbox.is-checked .neon-checkbox__check {
  stroke-dashoffset: 0;
  transform: scale(1.1);
}
.neon-checkbox.is-checked .neon-checkbox__glow {
  opacity: 0.2;
}
.neon-checkbox.is-checked .neon-checkbox__borders span {
  opacity: 1;
}
.neon-checkbox.is-checked .neon-checkbox__particles span {
  animation: particleExplosion 0.6s ease-out forwards;
}
.neon-checkbox.is-checked .neon-checkbox__rings .ring {
  animation: ringPulse 0.6s ease-out forwards;
}
.neon-checkbox.is-checked .neon-checkbox__sparks span {
  animation: sparkFlash 0.6s ease-out forwards;
}

/* is-hidden: dim unselected option after a choice is made */
.neon-checkbox.is-hidden {
  opacity: 0.3;
  pointer-events: none;
  transition: opacity 0.4s ease;
}

@keyframes borderFlow1 { 0% { transform: translateX(0); } 100% { transform: translateX(200%); } }
@keyframes borderFlow2 { 0% { transform: translateY(0); } 100% { transform: translateY(200%); } }
@keyframes borderFlow3 { 0% { transform: translateX(0); } 100% { transform: translateX(-200%); } }
@keyframes borderFlow4 { 0% { transform: translateY(0); } 100% { transform: translateY(-200%); } }

@keyframes particleExplosion {
  0% { transform: translate(-50%, -50%) scale(1); opacity: 0; }
  20% { opacity: 1; }
  100% { transform: translate(calc(-50% + var(--x, 20px)), calc(-50% + var(--y, 20px))) scale(0); opacity: 0; }
}
@keyframes ringPulse {
  0% { transform: scale(0); opacity: 1; }
  100% { transform: scale(2); opacity: 0; }
}
@keyframes sparkFlash {
  0% { transform: rotate(var(--r, 0deg)) translateX(0) scale(1); opacity: 1; }
  100% { transform: rotate(var(--r, 0deg)) translateX(30px) scale(0); opacity: 0; }
}

.neon-checkbox__particles span:nth-child(1) { --x: 25px; --y: -25px; }
.neon-checkbox__particles span:nth-child(2) { --x: -25px; --y: -25px; }
.neon-checkbox__particles span:nth-child(3) { --x: 25px; --y: 25px; }
.neon-checkbox__particles span:nth-child(4) { --x: -25px; --y: 25px; }
.neon-checkbox__particles span:nth-child(5) { --x: 35px; --y: 0px; }
.neon-checkbox__particles span:nth-child(6) { --x: -35px; --y: 0px; }
.neon-checkbox__particles span:nth-child(7) { --x: 0px; --y: 35px; }
.neon-checkbox__particles span:nth-child(8) { --x: 0px; --y: -35px; }
.neon-checkbox__particles span:nth-child(9) { --x: 20px; --y: -30px; }
.neon-checkbox__particles span:nth-child(10) { --x: -20px; --y: 30px; }
.neon-checkbox__particles span:nth-child(11) { --x: 30px; --y: 20px; }
.neon-checkbox__particles span:nth-child(12) { --x: -30px; --y: -20px; }

.neon-checkbox__sparks span:nth-child(1) { --r: 0deg; top: 50%; left: 50%; }
.neon-checkbox__sparks span:nth-child(2) { --r: 90deg; top: 50%; left: 50%; }
.neon-checkbox__sparks span:nth-child(3) { --r: 180deg; top: 50%; left: 50%; }
.neon-checkbox__sparks span:nth-child(4) { --r: 270deg; top: 50%; left: 50%; }

.neon-checkbox__rings .ring:nth-child(1) { animation-delay: 0s; }
.neon-checkbox__rings .ring:nth-child(2) { animation-delay: 0.1s; }
.neon-checkbox__rings .ring:nth-child(3) { animation-delay: 0.2s; }

/* Mobile */
@media (max-width: 768px) {
  .ai-assistant {
    display: none;
  }
}
</style>

<style>
/* Night mode - must be unscoped to match parent .night class */
.night .ai-toggle-btn {
  background: linear-gradient(135deg, #1e3a5f, #2a5298);
  box-shadow: 0 4px 15px rgba(90, 155, 255, 0.3);
}
.night .ai-toggle-btn:hover {
  box-shadow: 0 6px 20px rgba(90, 155, 255, 0.45);
}
.night .ai-panel {
  background: rgba(15, 23, 42, 0.97);
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}
.night .ai-panel-header {
  border-bottom-color: rgba(255, 255, 255, 0.08);
  background: linear-gradient(135deg, rgba(90, 155, 255, 0.08), rgba(99, 102, 241, 0.08));
}
.night .ai-panel-title {
  color: #e0e0e0;
}
.night .ai-panel-close {
  color: #a0aec0;
}
.night .ai-panel-close:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #e0e0e0;
}
.night .ai-panel-messages::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
}
.night .welcome-text {
  color: #a0aec0;
}
.night .quick-btn {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
  color: #e0e0e0;
}
.night .quick-btn:hover {
  border-color: #5a9bff;
  color: #5a9bff;
  background: rgba(90, 155, 255, 0.08);
}
.night .user-bubble {
  background: #2a5298;
  color: #e0e0e0;
}
.night .ai-bubble {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
  color: #e0e0e0;
}
.night .ai-bubble :deep(.md-editor) {
  background: transparent;
}
.night .ai-bubble :deep(.md-editor-preview pre) {
  background: rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 255, 255, 0.1);
}
.night .typing-dots span {
  background: #a0aec0;
}
.night .ai-error-msg {
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.3);
  color: #fca5a5;
}
.night .ai-panel-input {
  border-top-color: rgba(255, 255, 255, 0.08);
  background: rgba(15, 23, 42, 0.8);
}
.night .ai-panel-input input {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #e0e0e0;
}
.night .ai-panel-input input:focus {
  border-color: #5a9bff;
}
.night .ai-panel-input input::placeholder {
  color: #718096;
}
.night .search-btn {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #a0aec0;
}
.night .search-btn:hover:not(:disabled) {
  border-color: #5a9bff;
  color: #5a9bff;
}
.night .search-result-card {
  border-color: rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.03);
}
.night .search-result-card:hover {
  border-color: #5a9bff;
  background: rgba(90, 155, 255, 0.08);
}
.night .result-title {
  color: #e0e0e0;
}
.night .result-summary {
  color: #a0aec0;
}
.night .result-category {
  background: rgba(90, 155, 255, 0.15);
  color: #93c5fd;
}
.night .result-stats {
  color: #718096;
}
.night .searching-indicator {
  color: #a0aec0;
}
.night .disambiguate-card {
  background: rgba(0, 255, 170, 0.06);
  border-color: rgba(0, 255, 170, 0.25);
}
.night .disambiguate-hint {
  color: #a0aec0;
}
.night .disambiguate-label {
  color: #e0e0e0;
}
.night .neon-checkbox:hover .disambiguate-label {
  color: #00ffaa;
}
.night .neon-checkbox__box {
  background: rgba(0, 0, 0, 0.9);
}
</style>
