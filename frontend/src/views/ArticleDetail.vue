<template>
  <div class="article-detail-wrapper" :style="readingStyles"
    @touchstart.passive="onTouchStart"
    @touchmove="onTouchMove"
    @touchend.passive="onTouchEnd"
  >
    <!-- Reading progress bar -->
    <div class="reading-progress-bar" :style="{ width: readProgress + '%' }">
      <span v-if="readProgress > 3" class="reading-progress-pct">{{ Math.round(readProgress) }}%</span>
    </div>

    <div class="article-detail" v-if="article"
      :style="{ transform: swipeOffset ? `translateX(${swipeOffset}px)` : undefined, transition: swipeAnimating ? 'transform 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94)' : 'none' }"
    >
      <div class="article-layout">
        <!-- Main article card -->
        <div class="article-card">
          <div v-if="article.coverImage" class="article-cover">
            <img :src="ossImg(article.coverImage)" :alt="currentTitle" loading="lazy" decoding="async" @error="$event.target.parentElement.style.display='none'" />
          </div>

          <header class="article-header">
            <h1>{{ currentTitle }}</h1>
            <div class="meta">
              <span v-if="article.categoryName" class="meta-category">{{ article.categoryName }}</span>
              <span class="meta-dot">&middot;</span>
              <span>{{ relativeDate(article.createdAt) }}</span>
              <span class="meta-dot">&middot;</span>
              <span>{{ article.viewCount }} {{ $t('article.reads') }}</span>
              <span class="meta-dot">&middot;</span>
              <span>{{ currentReadingTime }} {{ $t('article.minRead') }}</span>
            </div>
            <div class="tags" v-if="article.tags?.length">
              <span v-for="tag in article.tags" :key="tag.id" class="tag">{{ tag.name }}</span>
            </div>
          </header>

          <div class="article-body" ref="bodyRef">
            <MdPreview v-if="articleContentMd" :modelValue="articleContentMd" />
            <div v-else-if="article.contentHtml" class="content" ref="contentRef" v-html="safeContentHtml"></div>
          </div>

          <footer class="article-footer">
            <div class="footer-actions">
              <div class="like-section">
                <div class="heart-container" :title="$t('article.like')" @click="handleLike">
                  <input type="checkbox" class="checkbox" :checked="liked" readonly>
                  <div class="svg-container">
                    <svg viewBox="0 0 24 24" class="svg-outline" xmlns="http://www.w3.org/2000/svg">
                      <path d="M17.5,1.917a6.4,6.4,0,0,0-5.5,3.3,6.4,6.4,0,0,0-5.5-3.3A6.8,6.8,0,0,0,0,8.967c0,4.547,4.786,9.513,8.8,12.88a4.974,4.974,0,0,0,6.4,0C19.214,18.48,24,13.514,24,8.967A6.8,6.8,0,0,0,17.5,1.917Zm-3.585,18.4a2.973,2.973,0,0,1-3.83,0C4.947,16.006,2,11.87,2,8.967a4.8,4.8,0,0,1,4.5-5.05A4.8,4.8,0,0,1,11,8.967a1,1,0,0,0,2,0,4.8,4.8,0,0,1,4.5-5.05A4.8,4.8,0,0,1,22,8.967C22,11.87,19.053,16.006,13.915,20.313Z"></path>
                    </svg>
                    <svg viewBox="0 0 24 24" class="svg-filled" xmlns="http://www.w3.org/2000/svg">
                      <path d="M17.5,1.917a6.4,6.4,0,0,0-5.5,3.3,6.4,6.4,0,0,0-5.5-3.3A6.8,6.8,0,0,0,0,8.967c0,4.547,4.786,9.513,8.8,12.88a4.974,4.974,0,0,0,6.4,0C19.214,18.48,24,13.514,24,8.967A6.8,6.8,0,0,0,17.5,1.917Z"></path>
                    </svg>
                    <svg class="svg-celebrate" width="100" height="100" xmlns="http://www.w3.org/2000/svg">
                      <polygon points="10,10 20,20"></polygon>
                      <polygon points="10,50 20,50"></polygon>
                      <polygon points="20,80 30,70"></polygon>
                      <polygon points="90,10 80,20"></polygon>
                      <polygon points="90,50 80,50"></polygon>
                      <polygon points="80,80 70,70"></polygon>
                    </svg>
                  </div>
                </div>
                <span class="like-count-text">{{ article.likeCount || 0 }}</span>
              </div>
              <button class="share-btn" @click="shareArticle" :title="$t('article.share')" :aria-label="$t('article.share')">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>
                <span v-if="shareCopied" class="share-copied-text">{{ $t('article.linkCopied') }}</span>
              </button>
              <button v-if="article.authorNotes" class="mobile-notes-btn" @click="authorNotesVisible = true">
                <span>📝</span> {{ $t('article.authorNotes') }}
              </button>
            </div>
          </footer>
        </div>

        <!-- TOC sidebar (desktop only) -->
        <aside class="toc-sidebar" v-if="headings.length > 0">
          <div class="toc-card">
            <h4 class="toc-title">{{ $t('article.toc') }}</h4>
            <nav class="toc-list">
              <a v-for="h in headings" :key="h.id"
                :href="'#' + h.id"
                :class="['toc-item', `toc-${h.level}`, { active: activeHeading === h.id }]"
                @click.prevent="scrollToHeading(h.id)">
                {{ h.text }}
              </a>
            </nav>
          </div>
        </aside>
      </div>

      <!-- Prev / Next navigation -->
      <nav class="article-nav" v-if="article.prevArticle || article.nextArticle">
        <router-link v-if="article.prevArticle" :to="`/articles/${article.prevArticle.slug}`" class="nav-item nav-prev">
          <span class="nav-label">&larr; {{ $t('article.prevArticle') }}</span>
          <span class="nav-title">{{ isEn && article.prevArticle.titleEn ? article.prevArticle.titleEn : article.prevArticle.title }}</span>
        </router-link>
        <div v-else></div>
        <router-link v-if="article.nextArticle" :to="`/articles/${article.nextArticle.slug}`" class="nav-item nav-next">
          <span class="nav-label">{{ $t('article.nextArticle') }} &rarr;</span>
          <span class="nav-title">{{ isEn && article.nextArticle.titleEn ? article.nextArticle.titleEn : article.nextArticle.title }}</span>
        </router-link>
      </nav>

      <!-- Related articles -->
      <section class="related-section" v-if="article.relatedArticles?.length">
        <h3 class="related-title">{{ $t('article.relatedArticles') }}</h3>
        <div class="related-grid">
          <router-link v-for="ra in article.relatedArticles" :key="ra.id"
            :to="`/articles/${ra.slug}`" class="related-card">
            <h4>{{ isEn && ra.titleEn ? ra.titleEn : ra.title }}</h4>
            <p>{{ stripMarkdown(isEn && ra.summaryEn ? ra.summaryEn : ra.summary) }}</p>
            <div class="related-meta">
              <span v-if="ra.categoryName">{{ ra.categoryName }}</span>
              <span>{{ ra.viewCount }} {{ $t('article.reads') }}</span>
            </div>
          </router-link>
        </div>
      </section>

      <!-- Author's Notes floating button -->
      <button v-if="article.authorNotes" class="author-notes-btn" @click="authorNotesVisible = true">
        <span class="author-notes-btn-icon">📝</span>
        <span class="author-notes-btn-text">{{ $t('article.authorNotes') }}</span>
      </button>

      <!-- Reading settings button -->
      <button class="settings-btn" @click="settingsOpen = !settingsOpen" :title="$t('article.readingSettings')" :aria-label="$t('article.readingSettings')">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
      </button>
      <!-- Reading settings panel -->
      <div v-if="settingsOpen" class="settings-panel">
        <div class="settings-panel-header">
          <span>{{ $t('article.readingSettings') }}</span>
          <button class="settings-reset" @click="resetReadingPrefs">{{ $t('article.resetSettings') }}</button>
        </div>
        <div class="settings-item">
          <label>{{ $t('article.fontSize') }} <span class="settings-val">{{ readingPrefs.fontSize }}px</span></label>
          <input type="range" v-model.number="readingPrefs.fontSize" min="14" max="24" step="1">
        </div>
        <div class="settings-item">
          <label>{{ $t('article.lineHeight') }} <span class="settings-val">{{ readingPrefs.lineHeight }}</span></label>
          <input type="range" v-model.number="readingPrefs.lineHeight" min="1.4" max="2.2" step="0.05">
        </div>
        <div class="settings-item">
          <label>{{ $t('article.letterSpacing') }} <span class="settings-val">{{ readingPrefs.letterSpacing }}px</span></label>
          <input type="range" v-model.number="readingPrefs.letterSpacing" min="0" max="2" step="0.1">
        </div>
        <div class="settings-item">
          <label>{{ $t('article.paragraphSpacing') }} <span class="settings-val">{{ readingPrefs.paragraphSpacing }}rem</span></label>
          <input type="range" v-model.number="readingPrefs.paragraphSpacing" min="0.5" max="2" step="0.1">
        </div>
        <div class="settings-item">
          <label>{{ $t('article.contentWidth') }} <span class="settings-val">{{ readingPrefs.contentWidth }}px</span></label>
          <input type="range" v-model.number="readingPrefs.contentWidth" min="700" max="1600" step="50">
        </div>
        <div class="settings-item">
          <label>{{ $t('article.fontFamily') }}</label>
          <select v-model="readingPrefs.fontFamily">
            <option value="system">{{ $t('article.fontDefault') }}</option>
            <option value="song">{{ $t('article.fontSong') }}</option>
            <option value="kai">{{ $t('article.fontKai') }}</option>
            <option value="serif">{{ $t('article.fontSerif') }}</option>
          </select>
        </div>
      </div>

      <!-- Print button (typewriter animation) -->
      <button class="print-btn" @click="printArticle" :title="$t('article.print')" :aria-label="$t('article.print')">
        <div class="typewriter">
          <div class="slide"><i></i></div>
          <div class="paper"></div>
          <div class="keyboard"></div>
        </div>
      </button>

      <!-- Download button -->
      <div class="download-wrapper">
        <button class="download-btn" @click.stop="downloadMenuOpen = !downloadMenuOpen" :disabled="exporting">
          <span class="download-btn-content">
            <svg v-if="!exporting" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            <span v-else class="download-spinner"></span>
            {{ exporting ? $t('article.downloading') : $t('article.download') }}
          </span>
        </button>
        <div v-if="downloadMenuOpen" class="download-menu">
          <button class="download-menu-item" @click="exportPdf">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
            {{ $t('article.downloadPdf') }}
          </button>
          <button class="download-menu-item" @click="exportWord">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><path d="M9 15l2 2 4-4"/></svg>
            {{ $t('article.downloadWord') }}
          </button>
        </div>
      </div>

      <!-- Author's Notes popup -->
      <Teleport to="body">
        <div v-if="authorNotesVisible" class="author-notes-overlay" @click.self="authorNotesVisible = false" role="dialog" aria-modal="true" :aria-label="$t('article.authorNotesTitle')">
          <div class="author-notes-popup" :class="{ visible: authorNotesVisible }">
            <div class="author-notes-popup-header">
              <span class="author-notes-popup-title">{{ $t('article.authorNotesTitle') }}</span>
              <button class="author-notes-close" @click="authorNotesVisible = false">&times;</button>
            </div>
            <div class="author-notes-popup-body">
              <MdPreview :modelValue="article.authorNotes" previewTheme="github" :codeFoldable="false" />
            </div>
          </div>
        </div>
      </Teleport>
    </div>

    <!-- Error state -->
    <div v-else-if="loadError" class="article-detail error-state">
      <div class="error-card">
        <div class="error-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        </div>
        <h2>{{ $t('article.loadError') }}</h2>
        <p>{{ $t('article.loadErrorDesc') }}</p>
        <div class="error-actions">
          <button class="error-retry-btn" @click="loadArticle(route.params.slug)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
            {{ $t('article.retry') }}
          </button>
          <router-link to="/articles" class="error-back-btn">{{ $t('article.backToList') }}</router-link>
        </div>
      </div>
    </div>

    <!-- Loading skeleton -->
    <div v-else class="article-detail">
      <div class="article-card skeleton-card">
        <div class="skeleton" style="width: 80%; height: 32px; margin-bottom: 12px;" />
        <div class="skeleton" style="width: 50%; height: 32px; margin-bottom: 20px;" />
        <div style="display: flex; gap: 16px; margin-bottom: 24px;">
          <div class="skeleton" style="width: 80px; height: 14px;" />
          <div class="skeleton" style="width: 100px; height: 14px;" />
          <div class="skeleton" style="width: 60px; height: 14px;" />
        </div>
        <div style="display: flex; gap: 8px; margin-bottom: 32px;">
          <div class="skeleton" style="width: 48px; height: 22px; border-radius: 12px;" />
          <div class="skeleton" style="width: 56px; height: 22px; border-radius: 12px;" />
        </div>
        <div class="skeleton" style="width: 100%; height: 16px; margin-bottom: 12px;" />
        <div class="skeleton" style="width: 95%; height: 16px; margin-bottom: 12px;" />
        <div class="skeleton" style="width: 88%; height: 16px; margin-bottom: 12px;" />
        <div class="skeleton" style="width: 100%; height: 16px; margin-bottom: 12px;" />
        <div class="skeleton" style="width: 70%; height: 16px; margin-bottom: 28px;" />
        <div class="skeleton" style="width: 40%; height: 22px; margin-bottom: 16px;" />
        <div class="skeleton" style="width: 100%; height: 16px; margin-bottom: 12px;" />
        <div class="skeleton" style="width: 92%; height: 16px; margin-bottom: 12px;" />
        <div class="skeleton" style="width: 85%; height: 16px; margin-bottom: 12px;" />
        <div class="skeleton" style="width: 100%; height: 16px; margin-bottom: 28px;" />
        <div class="skeleton" style="width: 100%; height: 120px; border-radius: 8px; margin-bottom: 28px;" />
        <div class="skeleton" style="width: 100%; height: 16px; margin-bottom: 12px;" />
        <div class="skeleton" style="width: 78%; height: 16px; margin-bottom: 12px;" />
        <div class="skeleton" style="width: 90%; height: 16px;" />
      </div>
    </div>

    <!-- Fan Actions -->
    <FanActions :liked="liked" @like="handleLike" @scroll-top="scrollToTop" />

    <!-- Mobile TOC button -->
    <button v-if="headings.length > 0" class="mobile-toc-btn" @click="mobileTocOpen = true" :title="$t('article.toc')" :aria-label="$t('article.toc')">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
    </button>

    <!-- Mobile TOC drawer -->
    <Teleport to="body">
      <Transition name="mobile-toc-fade">
        <div v-if="mobileTocOpen" class="mobile-toc-overlay" @click.self="mobileTocOpen = false" role="dialog" aria-modal="true" :aria-label="$t('article.toc')">
          <div class="mobile-toc-drawer">
            <div class="mobile-toc-header">
              <span>{{ $t('article.toc') }}</span>
              <button class="mobile-toc-close" @click="mobileTocOpen = false">&times;</button>
            </div>
            <nav class="mobile-toc-list">
              <a v-for="h in headings" :key="h.id"
                :href="'#' + h.id"
                :class="['mobile-toc-item', `mobile-toc-${h.level}`, { active: activeHeading === h.id }]"
                @click.prevent="scrollToHeading(h.id); mobileTocOpen = false">
                {{ h.text }}
              </a>
            </nav>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import 'md-editor-v3/lib/preview.css'
import '../utils/mdEditorConfig'
import { ossImg } from '../utils/oss'

const MdPreview = defineAsyncComponent(() =>
  import('md-editor-v3').then(m => m.MdPreview)
)
import DOMPurify from 'dompurify'
import request from '../utils/request'
import { relativeDate, readingTime } from '../utils/date'
import { stripMarkdown } from '../utils/stripMarkdown'
import FanActions from '../components/FanActions.vue'

const route = useRoute()
const { t, locale } = useI18n()
const isEn = computed(() => locale.value === 'en-US')
const article = ref(null)
const liked = ref(false)
const likeLoading = ref(false)
const authorNotesVisible = ref(false)
const settingsOpen = ref(false)
const bodyRef = ref(null)
const loadError = ref(false)
const mobileTocOpen = ref(false)
const shareCopied = ref(false)

// Mobile swipe navigation
const touchState = {
  startX: 0,
  startY: 0,
  startTime: 0,
  swiping: false,
  swipingVertically: false,
  insideCodeBlock: false
}
const swipeOffset = ref(0)
const swipeAnimating = ref(false)
const SWIPE_THRESHOLD = 80
const SWIPE_TIME_LIMIT = 300

function isMobile() {
  return window.innerWidth <= 768
}

function onTouchStart(e) {
  if (!isMobile()) return
  const touch = e.touches[0]
  touchState.startX = touch.clientX
  touchState.startY = touch.clientY
  touchState.startTime = Date.now()
  touchState.swiping = false
  touchState.swipingVertically = false
  touchState.insideCodeBlock = !!e.target.closest('pre')
  swipeOffset.value = 0
}

function onTouchMove(e) {
  if (!isMobile() || touchState.swipingVertically || touchState.insideCodeBlock) return
  const touch = e.touches[0]
  const dx = touch.clientX - touchState.startX
  const dy = touch.clientY - touchState.startY

  // Determine swipe direction on first significant move
  if (!touchState.swiping && Math.abs(dx) < 10 && Math.abs(dy) < 10) return
  if (!touchState.swiping) {
    if (Math.abs(dy) > Math.abs(dx)) {
      touchState.swipingVertically = true
      return
    }
    touchState.swiping = true
  }

  // Only prevent default for horizontal swipe (check cancelable to avoid browser warning)
  if (touchState.swiping) {
    if (e.cancelable) {
      e.preventDefault()
    }
    // Apply resistance at edges
    const hasTarget = dx > 0 ? article.value?.prevArticle : article.value?.nextArticle
    swipeOffset.value = hasTarget ? dx * 0.6 : dx * 0.15
  }
}

function onTouchEnd() {
  if (!isMobile() || !touchState.swiping) {
    swipeOffset.value = 0
    return
  }
  const elapsed = Date.now() - touchState.startTime
  const fastSwipe = elapsed < SWIPE_TIME_LIMIT && Math.abs(swipeOffset.value) > 30

  if (swipeOffset.value > SWIPE_THRESHOLD || (fastSwipe && swipeOffset.value > 0)) {
    // Swipe right -> prev article
    if (article.value?.prevArticle) {
      swipeAnimating.value = true
      swipeOffset.value = window.innerWidth
      setTimeout(() => {
        window.location.href = `/articles/${article.value.prevArticle.slug}`
      }, 200)
      return
    }
  } else if (swipeOffset.value < -SWIPE_THRESHOLD || (fastSwipe && swipeOffset.value < 0)) {
    // Swipe left -> next article
    if (article.value?.nextArticle) {
      swipeAnimating.value = true
      swipeOffset.value = -window.innerWidth
      setTimeout(() => {
        window.location.href = `/articles/${article.value.nextArticle.slug}`
      }, 200)
      return
    }
  }

  // Spring back
  swipeAnimating.value = true
  swipeOffset.value = 0
  setTimeout(() => { swipeAnimating.value = false }, 300)
}

// Reading preferences
const DEFAULT_PREFS = { fontSize: 16, lineHeight: 1.75, letterSpacing: 0, paragraphSpacing: 1, contentWidth: 1450, fontFamily: 'system' }
const readingPrefs = ref({ ...DEFAULT_PREFS })

function loadReadingPrefs() {
  try {
    const saved = localStorage.getItem('article_reading_prefs')
    if (saved) readingPrefs.value = { ...DEFAULT_PREFS, ...JSON.parse(saved) }
  } catch {}
}
let savePrefsTimer = null
function saveReadingPrefs() {
  clearTimeout(savePrefsTimer)
  savePrefsTimer = setTimeout(() => {
    localStorage.setItem('article_reading_prefs', JSON.stringify(readingPrefs.value))
  }, 300)
}
function resetReadingPrefs() {
  readingPrefs.value = { ...DEFAULT_PREFS }
  saveReadingPrefs()
}

const FONT_MAP = {
  system: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  song: '"SimSun", "宋体", serif',
  kai: '"KaiTi", "楷体", serif',
  serif: 'Georgia, "Times New Roman", serif'
}
const readingStyles = computed(() => ({
  '--reading-font-size': readingPrefs.value.fontSize + 'px',
  '--reading-line-height': readingPrefs.value.lineHeight,
  '--reading-letter-spacing': readingPrefs.value.letterSpacing + 'px',
  '--reading-paragraph-spacing': readingPrefs.value.paragraphSpacing + 'rem',
  '--reading-max-width': readingPrefs.value.contentWidth + 'px',
  '--reading-font-family': FONT_MAP[readingPrefs.value.fontFamily] || FONT_MAP.system
}))

watch(readingPrefs, saveReadingPrefs, { deep: true })

function printArticle() { window.print() }

// Download / Export
const downloadMenuOpen = ref(false)
const exporting = ref(false)

function onDownloadClickOutside(e) {
  if (downloadMenuOpen.value && !e.target.closest('.download-wrapper')) {
    downloadMenuOpen.value = false
  }
}

function loadScript(src) {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) { resolve(); return }
    const s = document.createElement('script')
    s.src = src
    s.onload = resolve
    s.onerror = () => reject(new Error(`Failed to load ${src}`))
    document.head.appendChild(s)
  })
}

async function exportPdf() {
  downloadMenuOpen.value = false
  if (exporting.value) return
  exporting.value = true
  try {
    await loadScript('/vendor/jspdf.umd.min.js')
    await loadScript('/vendor/html2canvas.min.js')
    const JsPDF = window.jspdf?.jsPDF
    if (!JsPDF) throw new Error('jsPDF not loaded.')
    const h2c = window.html2canvas
    if (!h2c) throw new Error('html2canvas not loaded.')
    const el = bodyRef.value || document.querySelector('.article-body')
    if (!el) throw new Error('Article body not found')

    const title = articleTitle(article.value) || 'article'
    const author = article.value?.author || ''
    const date = article.value?.createdAt ? new Date(article.value.createdAt).toLocaleDateString('zh-CN') : ''

    const canvas = await h2c(el, { scale: 2, useCORS: true, logging: false })
    const pdf = new JsPDF('p', 'mm', 'a4')
    const pdfW = pdf.internal.pageSize.getWidth()
    const pdfH = pdf.internal.pageSize.getHeight()
    const margin = 15
    const contentW = pdfW - margin * 2

    // Header: title + meta
    pdf.setFontSize(18)
    pdf.setFont('helvetica', 'bold')
    const titleLines = pdf.splitTextToSize(title, contentW)
    pdf.text(titleLines, margin, margin + 6)
    const headerEnd = margin + titleLines.length * 8 + 2

    if (author || date) {
      pdf.setFontSize(9)
      pdf.setFont('helvetica', 'normal')
      pdf.setTextColor(120, 120, 120)
      pdf.text(`${author}${author && date ? '  ·  ' : ''}${date}`, margin, headerEnd + 2)
      pdf.setTextColor(0, 0, 0)
    }

    // Separator line
    const contentStart = headerEnd + 8
    pdf.setDrawColor(200, 200, 200)
    pdf.setLineWidth(0.3)
    pdf.line(margin, contentStart - 3, pdfW - margin, contentStart - 3)

    // Content image
    const contentH = (canvas.height * contentW) / canvas.width
    const availH = pdfH - contentStart - margin - 8 // reserve space for page number

    if (contentH <= availH) {
      pdf.addImage(canvas.toDataURL('image/png'), 'PNG', margin, contentStart, contentW, contentH)
    } else {
      let remaining = contentH
      let srcY = 0
      let page = 1
      while (remaining > 0) {
        const isFirstPage = page === 1
        const pageTop = isFirstPage ? contentStart : margin
        const pageAvail = isFirstPage ? availH : pdfH - margin * 2 - 8
        const sliceH = Math.min(remaining, pageAvail)
        const slicePxH = (sliceH / contentW) * canvas.width

        const sliceCanvas = document.createElement('canvas')
        sliceCanvas.width = canvas.width
        sliceCanvas.height = Math.ceil(slicePxH)
        const ctx = sliceCanvas.getContext('2d')
        ctx.drawImage(canvas, 0, srcY, canvas.width, sliceCanvas.height, 0, 0, canvas.width, sliceCanvas.height)

        if (!isFirstPage) pdf.addPage()
        pdf.addImage(sliceCanvas.toDataURL('image/png'), 'PNG', margin, pageTop, contentW, sliceH)

        srcY += sliceCanvas.height
        remaining -= sliceH
        page++
      }
    }

    // Page numbers
    const totalPages = pdf.internal.getNumberOfPages()
    for (let i = 1; i <= totalPages; i++) {
      pdf.setPage(i)
      pdf.setFontSize(8)
      pdf.setTextColor(150, 150, 150)
      pdf.text(`${i} / ${totalPages}`, pdfW / 2, pdfH - 6, { align: 'center' })
    }

    pdf.save(`${title}.pdf`)
  } catch (e) {
    console.error('[exportPdf] FAILED:', e)
    alert('PDF export failed: ' + e.message)
  } finally {
    exporting.value = false
  }
}

async function exportWord() {
  downloadMenuOpen.value = false
  if (exporting.value) return
  exporting.value = true
  try {
    const el = bodyRef.value || document.querySelector('.article-body')
    if (!el) throw new Error('Article body not found')
    const title = articleTitle(article.value) || 'article'
    const author = article.value?.author || ''
    const date = article.value?.createdAt ? new Date(article.value.createdAt).toLocaleDateString('zh-CN') : ''
    const clone = el.cloneNode(true)
    clone.querySelectorAll('.heading-anchor, .copy-btn').forEach(e => e.remove())
    const content = clone.innerHTML
    const escHtml = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')

    const wordStyles = `
      @page { size: A4; margin: 2.54cm; }
      body { font-family: "Microsoft YaHei", "PingFang SC", sans-serif; font-size: 11pt; line-height: 1.8; color: #333; }
      h1 { font-size: 22pt; font-weight: bold; color: #1a1a2e; margin: 0 0 6pt; }
      h2 { font-size: 16pt; font-weight: bold; color: #1a1a2e; margin: 18pt 0 6pt; border-bottom: 1px solid #e0e0e0; padding-bottom: 4pt; }
      h3 { font-size: 13pt; font-weight: bold; color: #333; margin: 14pt 0 4pt; }
      h4, h5, h6 { font-size: 11pt; font-weight: bold; color: #444; margin: 10pt 0 4pt; }
      p { margin: 0 0 8pt; text-align: justify; }
      .word-meta { color: #888; font-size: 9pt; margin-bottom: 16pt; border-bottom: 1px solid #ddd; padding-bottom: 8pt; }
      pre { background: #f5f5f5; border: 1px solid #ddd; border-radius: 4pt; padding: 10pt; font-family: "Consolas", "Courier New", monospace; font-size: 9pt; line-height: 1.5; white-space: pre-wrap; word-wrap: break-word; margin: 8pt 0; }
      code { font-family: "Consolas", "Courier New", monospace; font-size: 9.5pt; }
      p > code, li > code, td > code { background: #f0f0f0; padding: 1pt 4pt; border-radius: 2pt; }
      pre code { background: none; padding: 0; }
      blockquote { border-left: 3pt solid #3b82f6; margin: 8pt 0; padding: 6pt 12pt; background: #f8f9fa; color: #555; font-style: italic; }
      table { border-collapse: collapse; width: 100%; margin: 8pt 0; }
      th, td { border: 1px solid #d0d0d0; padding: 6pt 8pt; font-size: 10pt; }
      th { background: #f0f0f0; font-weight: bold; text-align: left; }
      tr:nth-child(even) { background: #fafafa; }
      img { max-width: 100%; height: auto; }
      ul, ol { margin: 4pt 0 8pt 18pt; }
      li { margin: 2pt 0; }
      a { color: #2563eb; text-decoration: underline; }
      hr { border: none; border-top: 1px solid #ddd; margin: 16pt 0; }
      .katex, .katex-display { font-family: "Cambria Math", "Times New Roman", serif; }
    `

    const safeTitle = escHtml(title)
    const safeAuthor = escHtml(author)
    const html = `<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" xmlns="http://www.w3.org/TR/REC-html40">
<head><meta charset="utf-8"><title>${safeTitle}</title>
<!--[if gte mso 9]><xml><w:WordDocument><w:View>Print</w:View></w:WordDocument></xml><![endif]-->
<style>${wordStyles}</style></head>
<body>
<h1>${safeTitle}</h1>
<div class="word-meta">${safeAuthor}${author && date ? ' · ' : ''}${date}</div>
${content}
</body></html>`

    const blob = new Blob(['﻿', html], { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${title}.docx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (e) {
    console.error('[exportWord] FAILED:', e)
    alert('Word export failed: ' + e.message)
  } finally {
    exporting.value = false
  }
}

function articleTitle(a) { return a && isEn.value && a.titleEn ? a.titleEn : (a?.title || '') }
function articleSummary(a) { return a && isEn.value && a.summaryEn ? a.summaryEn : (a?.summary || '') }

const safeContentHtml = computed(() => {
  const a = article.value
  if (!a) return ''
  const html = isEn.value && a.contentHtmlEn ? a.contentHtmlEn : a.contentHtml
  if (!html) return ''
  const clean = DOMPurify.sanitize(html)
  return clean.replace(/<img /g, '<img loading="lazy" decoding="async" ')
})

const articleContentMd = computed(() => {
  const a = article.value
  if (!a) return ''
  return isEn.value && a.contentMdEn ? a.contentMdEn : (a.contentMd || '')
})

const currentTitle = computed(() => articleTitle(article.value))

// 动态浏览器标签页标题（配合 OG 分享卡片）
watch(currentTitle, (title) => {
  if (title) document.title = `${title} - 雨中的研发日志`
}, { immediate: true })

const currentReadingTime = computed(() => {
  const content = articleContentMd.value || (isEn.value && article.value?.contentHtmlEn ? article.value.contentHtmlEn : article.value?.contentHtml)
  return readingTime(content)
})

// === 1. Reading progress ===
const readProgress = ref(0)
function updateProgress() {
  const scrollTop = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  readProgress.value = docHeight > 0 ? Math.min(100, (scrollTop / docHeight) * 100) : 0
}

// === 7. Back to top ===
function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// === 2. TOC & 5. Heading anchors ===
const headings = ref([])
const activeHeading = ref('')
const contentRef = ref(null)
const headingElements = [] // cached DOM elements
const injectedElements = [] // track injected DOM for cleanup
const copyTimers = [] // track setTimeout for cleanup

function getContentContainer() {
  // For HTML articles, use the ref; for MdPreview articles, query the rendered container
  return contentRef.value || document.querySelector('.md-editor-preview')
}

function waitForContentContainer(maxWait = 5000) {
  return new Promise((resolve) => {
    const existing = getContentContainer()
    if (existing) { resolve(existing); return }
    const observer = new MutationObserver(() => {
      const el = getContentContainer()
      if (el) { observer.disconnect(); resolve(el) }
    })
    observer.observe(document.body, { childList: true, subtree: true })
    setTimeout(() => { observer.disconnect(); resolve(null) }, maxWait)
  })
}

let tocRetryTimer = null
const MAX_TOC_RETRIES = 50
async function buildToc() {
  // Clean up previously injected elements
  cleanupInjected()
  clearTimeout(tocRetryTimer)
  const container = await waitForContentContainer()
  if (!container) return
  const elements = container.querySelectorAll('h2, h3')
  const result = []
  headingElements.length = 0
  elements.forEach((el, i) => {
    const id = el.id || `heading-${i}`
    el.id = id
    // Add anchor link
    if (!el.querySelector('.heading-anchor')) {
      const anchor = document.createElement('a')
      anchor.className = 'heading-anchor'
      anchor.href = '#' + id
      anchor.textContent = '#'
      anchor.addEventListener('click', (e) => {
        e.preventDefault()
        const url = window.location.origin + window.location.pathname + '#' + id
        if (navigator.clipboard) {
          navigator.clipboard.writeText(url).catch(() => fallbackCopy(url, () => {}))
        } else {
          fallbackCopy(url, () => {})
        }
        scrollToHeading(id)
      })
      el.appendChild(anchor)
      injectedElements.push(anchor)
    }
    headingElements.push(el) // cache DOM reference
    result.push({ id, text: el.textContent.replace('#', '').trim(), level: el.tagName.toLowerCase() })
  })
  headings.value = result
  updateActiveHeading()
}

function scrollToHeading(id) {
  const el = document.getElementById(id)
  if (el) {
    const offset = 80
    const y = el.getBoundingClientRect().top + window.scrollY - offset
    window.scrollTo({ top: y, behavior: 'smooth' })
  }
}

function updateActiveHeading() {
  const scrollY = window.scrollY + 100
  let current = ''
  for (const el of headingElements) {
    if (el.offsetTop <= scrollY) {
      current = el.id
    }
  }
  activeHeading.value = current
}

// === 3. Code block enhancements ===
let copyRetryTimer = null
let codeBlockStyleInjected = false
function ensureCodeBlockScrollStyle() {
  if (codeBlockStyleInjected) return
  const style = document.createElement('style')
  style.id = 'code-block-scroll-fix'
  style.textContent = '.article-body .md-editor-preview pre code,.article-body .content pre code,.article-body .md-editor-preview pre code *,.article-body .content pre code *{overflow-x:visible!important;overflow-y:visible!important}'
  document.head.appendChild(style)
  codeBlockStyleInjected = true
}
async function addCopyButtons() {
  clearTimeout(copyRetryTimer)
  ensureCodeBlockScrollStyle()
  const container = await waitForContentContainer()
  if (!container) return
  container.querySelectorAll('pre').forEach(pre => {
    // Add vertical scroll for long code blocks (>10 lines)
    const codeEl = pre.querySelector('code')
    const lineCount = (codeEl?.textContent || pre.textContent || '').split('\n').length
    if (lineCount > 10) {
      pre.classList.add('code-long')
    }
  })
}

// === Cleanup injected DOM elements ===
function cleanupInjected() {
  const fixStyle = document.getElementById('code-block-scroll-fix')
  if (fixStyle) fixStyle.remove()
  codeBlockStyleInjected = false
  injectedElements.forEach(el => {
    if (typeof el.remove === 'function') el.remove()
  })
  injectedElements.length = 0
  copyTimers.forEach(t => clearTimeout(t))
  copyTimers.length = 0
  headingElements.length = 0
}

// === Scroll handler (throttled via rAF) ===
let scrollRaf = null
function onScroll() {
  if (scrollRaf) return
  scrollRaf = requestAnimationFrame(() => {
    scrollRaf = null
    updateProgress()
    updateActiveHeading()
  })
}

function onKeydown(e) {
  if (e.key === 'Escape') {
    if (authorNotesVisible.value) { authorNotesVisible.value = false }
    else if (mobileTocOpen.value) { mobileTocOpen.value = false }
    else if (settingsOpen.value) { settingsOpen.value = false }
  }
}

watch(mobileTocOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})
watch(authorNotesVisible, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})

// === Load article ===
let loadRequestId = 0
async function loadArticle(slug) {
  const reqId = ++loadRequestId
  clearTimeout(tocRetryTimer)
  clearTimeout(copyRetryTimer)
  try {
    article.value = null
    loadError.value = false
    headings.value = []
    const data = await request.get(`/api/articles/${slug}`)
    if (reqId !== loadRequestId) return
    article.value = data
    liked.value = !!data.liked
    buildToc()
    addCopyButtons()
  } catch (e) {
    if (reqId !== loadRequestId) return
    console.error('Failed to load article:', e)
    loadError.value = true
  }
}

function shareArticle() {
  // 带时间戳参数：微信/QQ 对同 URL 有预览缓存，新参数让爬虫重新抓取 OG 卡片
  const base = window.location.href
  const sep = base.includes('?') ? '&' : '?'
  const url = `${base}${sep}from=${Date.now()}`
  const showCopied = () => {
    shareCopied.value = true
    setTimeout(() => { shareCopied.value = false }, 2000)
  }
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url).then(showCopied).catch(() => fallbackCopy(url, showCopied))
  } else {
    fallbackCopy(url, showCopied)
  }
}
function fallbackCopy(text, onDone) {
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.cssText = 'position:fixed;opacity:0;top:0;left:0'
  document.body.appendChild(ta)
  ta.select()
  try { document.execCommand('copy') } catch {}
  document.body.removeChild(ta)
  onDone()
}

function onSettingsClickOutside(e) {
  if (settingsOpen.value && !e.target.closest('.settings-panel') && !e.target.closest('.settings-btn')) {
    settingsOpen.value = false
  }
}

onMounted(() => {
  loadReadingPrefs()
  loadArticle(route.params.slug)
  window.addEventListener('scroll', onScroll, { passive: true })
  window.addEventListener('keydown', onKeydown)
  document.addEventListener('click', onSettingsClickOutside)
  document.addEventListener('click', onDownloadClickOutside)
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  window.removeEventListener('keydown', onKeydown)
  document.removeEventListener('click', onSettingsClickOutside)
  document.removeEventListener('click', onDownloadClickOutside)
  document.body.style.overflow = ''
  if (scrollRaf) {
    cancelAnimationFrame(scrollRaf)
    scrollRaf = null
  }
  clearTimeout(savePrefsTimer)
  clearTimeout(tocRetryTimer)
  clearTimeout(copyRetryTimer)
  cleanupInjected()
})

// Watch for route changes (prev/next navigation)
watch(() => route.params.slug, (newSlug) => {
  if (newSlug) {
    window.scrollTo({ top: 0 })
    loadArticle(newSlug)
  }
})

// Like/Unlike toggle
async function handleLike() {
  if (likeLoading.value) return
  likeLoading.value = true
  try {
    const result = await request.post(`/api/articles/${route.params.slug}/like`)
    Object.assign(article.value, { likeCount: result.likeCount })
    liked.value = result.liked
  } catch (e) {
    console.error('Failed to like:', e)
  } finally {
    likeLoading.value = false
  }
}
</script>

<style>
/* Reading progress bar */
.reading-progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary), #3b82f6);
  z-index: 999;
  transition: width 0.1s linear;
  border-radius: 0 2px 2px 0;
}

.article-detail-wrapper {
  overflow-x: clip;
}
.article-detail {
  max-width: min(var(--reading-max-width, 1450px), 100%);
  margin: 0 auto;
}

/* Swipe indicators */
.swipe-indicator {
  position: fixed;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--article-card-bg, rgba(255, 255, 255, 0.9));
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s;
}
.swipe-indicator.left { left: 8px; }
.swipe-indicator.right { right: 8px; }
.swipe-indicator.visible { opacity: 1; }
.swipe-indicator svg {
  width: 18px;
  height: 18px;
  stroke: var(--color-text, #333);
  fill: none;
  stroke-width: 2;
}

/* Layout: article + toc sidebar */
.article-layout {
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
  min-width: 0;
}
.article-card {
  flex: 1;
  min-width: 0;
  background: var(--article-card-bg, #fff);
  border: 1px solid var(--article-card-border, #e8e8e8);
  border-radius: 16px;
  overflow: clip;
}
.skeleton-card {
  max-width: 900px;
  margin: 0 auto;
  padding: 2rem;
}

/* Header */
.article-header {
  padding: 2.5rem 3rem 1.5rem;
  border-bottom: 1px solid var(--article-header-border, #f0f0f0);
}
.article-header h1 {
  font-size: 1.85rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.35;
  color: var(--article-title-color, #1a1a1a);
  margin: 0 0 1rem;
}
.meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.82rem;
  color: var(--meta-color, #999);
  margin-bottom: 1rem;
  flex-wrap: wrap;
}
.meta-category {
  color: var(--meta-category-color, var(--color-primary));
  font-weight: 500;
}
.meta-dot { color: var(--meta-dot-color, #ddd); }
.tags {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}
.tag {
  font-size: 0.72rem;
  padding: 0.15rem 0.6rem;
  background: var(--tag-bg, #f6f8fa);
  border: 1px solid var(--tag-border, #eee);
  border-radius: 6px;
  color: var(--tag-color, #666);
  transition: all 0.2s;
}
.tag:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* Body */
.article-body {
  padding: 2rem 3rem 2.5rem;
  font-family: var(--reading-font-family, inherit);
  overflow-wrap: break-word;
  word-break: break-word;
}
.content {
  line-height: var(--reading-line-height, 1.9);
  letter-spacing: var(--reading-letter-spacing, 0);
  font-size: var(--reading-font-size, 16px);
  color: var(--content-color, #333);
  overflow-wrap: break-word;
  word-break: break-word;
}
/* MdPreview (markdown articles) — same reading settings */
.article-body .md-editor-preview {
  font-size: var(--reading-font-size, 16px);
  line-height: var(--reading-line-height, 1.9);
  letter-spacing: var(--reading-letter-spacing, 0);
  overflow-wrap: break-word;
  word-break: break-word;
}
.article-body .md-editor-preview p {
  margin-bottom: var(--reading-paragraph-spacing, 1.1rem);
}
.article-body .md-editor-preview pre {
  overflow-x: auto;
  max-width: 100%;
}
.article-body .md-editor-preview img {
  max-width: 100%;
  height: auto;
}
.article-body .md-editor-preview table {
  overflow-x: auto;
  max-width: 100%;
}
.article-body .md-editor-preview .katex-display {
  overflow-x: auto;
  max-width: 100%;
}
:is(.content, .md-editor-preview) h1 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--content-heading-color, #1a1a1a);
  margin: 2.5rem 0 1rem;
}
:is(.content, .md-editor-preview) h2 {
  font-size: 1.3rem;
  font-weight: 600;
  color: var(--content-heading-color, #1a1a1a);
  margin: 2.5rem 0 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #eee;
  position: relative;
}
:is(.content, .md-editor-preview) h3 {
  font-size: 1.12rem;
  font-weight: 600;
  color: var(--content-heading-color, #1a1a1a);
  margin: 2rem 0 0.75rem;
  position: relative;
}
/* Heading anchor (#) */
.content .heading-anchor,
.md-editor-preview .heading-anchor {
  display: inline-block;
  margin-left: 0.4rem;
  color: var(--color-primary);
  opacity: 0;
  text-decoration: none;
  font-weight: 400;
  transition: opacity 0.2s;
  cursor: pointer;
}
.content h2:hover .heading-anchor,
.content h3:hover .heading-anchor,
.md-editor-preview h2:hover .heading-anchor,
.md-editor-preview h3:hover .heading-anchor {
  opacity: 0.6;
}
.content .heading-anchor:hover,
.md-editor-preview .heading-anchor:hover {
  opacity: 1;
}
:is(.content, .md-editor-preview) p {
  margin-bottom: var(--reading-paragraph-spacing, 1.1rem);
  color: var(--content-color, #444);
}
:is(.content, .md-editor-preview) code:not(.md-editor-code pre code) {
  background: var(--content-code-bg, #f4f5f7);
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
  font-size: 0.88em;
  color: var(--content-code-color, #d63384);
}
:is(.content, .md-editor-preview) pre {
  max-width: 100%;
  background: transparent;
}
/* Ensure code block fills width and no gap with header */
.md-editor-preview .md-editor-code pre {
  margin: 0 !important;
  padding: 0 !important;
  overflow: visible !important;
  background: transparent !important;
}
.md-editor-preview .md-editor-code pre code {
  margin: 0 !important;
  min-width: 100% !important;
}
/* Copy button inside pre blocks */
.content .copy-btn,
.md-editor-preview .copy-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 0.25rem 0.65rem;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 6px;
  color: #a6adc8;
  font-size: 0.72rem;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s;
  z-index: 1;
}
.content pre:hover .copy-btn,
.md-editor-preview pre:hover .copy-btn {
  opacity: 1;
}
.content .copy-btn:hover,
.md-editor-preview .copy-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #cdd6f4;
}
.content .copy-btn.copied,
.md-editor-preview .copy-btn.copied {
  background: rgba(52, 211, 153, 0.2);
  border-color: rgba(52, 211, 153, 0.3);
  color: #34d399;
  opacity: 1;
}
:is(.content, .md-editor-preview) blockquote {
  border-left: 3px solid var(--color-primary);
  padding: 0.75rem 1.25rem;
  margin: 1.25rem 0;
  color: var(--content-blockquote-color, #666);
  background: var(--content-blockquote-bg, #f8f9fb);
  border-radius: 0 8px 8px 0;
}
:is(.content, .md-editor-preview) img {
  max-width: 100%;
  border-radius: 12px;
  margin: 1rem 0;
}
:is(.content, .md-editor-preview) ul,
:is(.content, .md-editor-preview) ol {
  padding-left: 1.5rem;
  margin-bottom: 1rem;
}
:is(.content, .md-editor-preview) li {
  margin-bottom: 0.4rem;
  color: var(--content-li-color, #444);
}
:is(.content, .md-editor-preview) strong {
  color: var(--content-strong-color, #1a1a1a);
  font-weight: 600;
}
:is(.content, .md-editor-preview) table {
  width: 100%;
  border-collapse: collapse;
  margin: 1.25rem 0;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid var(--content-table-border, #eee);
}
:is(.content, .md-editor-preview) th {
  background: var(--content-th-bg, linear-gradient(135deg, #1e5eb6, #2b78c8));
  color: var(--content-th-color, #fff);
  font-weight: 600;
  font-size: 0.85rem;
  padding: 0.75rem 1rem;
  text-align: left;
}
:is(.content, .md-editor-preview) td {
  padding: 0.75rem 1rem;
  font-size: 0.9rem;
  color: var(--content-td-color, #444);
  border-bottom: 1px solid var(--content-td-border, #f0f0f0);
}
:is(.content, .md-editor-preview) tr:last-child td { border-bottom: none; }
:is(.content, .md-editor-preview) tr:nth-child(even) td { background: #f9fafb; }
:is(.content, .md-editor-preview) tr:hover td { background: #f0f7ff; }

/* Footer */
.article-footer {
  padding: 1.5rem 3rem;
  border-top: 1px solid var(--footer-border, #f0f0f0);
  background: var(--footer-bg, #fafbfc);
  text-align: center;
}
.like-section {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}
.like-count-text {
  font-size: 1rem;
  font-weight: 500;
  color: var(--like-btn-color, #666);
  font-variant-numeric: tabular-nums;
}
/* Heart animation */
.heart-container {
  --heart-color: rgb(255, 91, 137);
  position: relative;
  width: 40px;
  height: 40px;
  transition: .3s;
  cursor: pointer;
}
.heart-container .checkbox {
  position: absolute;
  width: 100%;
  height: 100%;
  opacity: 0;
  z-index: 20;
  cursor: pointer;
}
.heart-container .svg-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}
.heart-container .svg-outline,
.heart-container .svg-filled {
  fill: var(--heart-color);
  position: absolute;
}
.heart-container .svg-filled {
  animation: keyframes-svg-filled 1s;
  display: none;
}
.heart-container .svg-celebrate {
  position: absolute;
  animation: keyframes-svg-celebrate .5s;
  animation-fill-mode: forwards;
  display: none;
  stroke: var(--heart-color);
  fill: var(--heart-color);
  stroke-width: 2px;
}
.heart-container .checkbox:checked~.svg-container .svg-filled {
  display: block;
}
.heart-container .checkbox:checked~.svg-container .svg-celebrate {
  display: block;
}
@keyframes keyframes-svg-filled {
  0% { transform: scale(0); }
  25% { transform: scale(1.2); }
  50% { transform: scale(1); filter: brightness(1.5); }
}
@keyframes keyframes-svg-celebrate {
  0% { transform: scale(0); }
  50% { opacity: 1; filter: brightness(1.5); }
  100% { transform: scale(1.4); opacity: 0; display: none; }
}

/* TOC sidebar */
.toc-sidebar {
  width: 220px;
  flex-shrink: 0;
  position: sticky;
  top: 72px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}
.toc-card {
  background: var(--toc-card-bg, #fff);
  border: 1px solid var(--toc-card-border, #e8e8e8);
  border-radius: 12px;
  padding: 1rem 0;
}
.toc-title {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--toc-title-color, #999);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0 1rem 0.6rem;
  border-bottom: 1px solid var(--toc-title-border, #f0f0f0);
  margin: 0 0 0.3rem;
}
.toc-list {
  display: flex;
  flex-direction: column;
}
.toc-item {
  display: block;
  padding: 0.3rem 1rem;
  font-size: 0.78rem;
  color: var(--toc-item-color, #666);
  text-decoration: none;
  border-left: 2px solid transparent;
  transition: all 0.2s;
  line-height: 1.5;
}
.toc-item:hover {
  color: var(--color-primary);
  background: #f8f9fb;
}
.toc-item.active {
  color: var(--color-primary);
  border-left-color: var(--color-primary);
  background: rgba(30, 94, 182, 0.04);
  font-weight: 500;
}
.toc-h3 {
  padding-left: 1.8rem;
  font-size: 0.74rem;
}

/* Prev / Next navigation */
.article-nav {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1.5rem;
}
.nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  padding: 1rem 1.25rem;
  background: var(--nav-item-bg, #fff);
  border: 1px solid var(--nav-item-border, #e8e8e8);
  border-radius: 12px;
  text-decoration: none;
  transition: all 0.25s;
  max-width: 50%;
}
.nav-item:hover {
  border-color: rgba(30, 94, 182, 0.3);
  box-shadow: 0 2px 12px rgba(30, 94, 182, 0.08);
  transform: translateY(-2px);
}
.nav-next {
  text-align: right;
}
.nav-label {
  font-size: 0.75rem;
  color: var(--nav-label-color, #999);
}
.nav-title {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--nav-title-color, #333);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nav-item:hover .nav-title {
  color: var(--color-primary);
}

/* Related articles */
.related-section {
  margin-top: 1.5rem;
}
.related-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--related-title-color, #333);
  margin-bottom: 1rem;
}
.related-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}
.related-card {
  background: var(--related-card-bg, #fff);
  border: 1px solid var(--related-card-border, #e8e8e8);
  border-radius: 12px;
  padding: 1.25rem;
  text-decoration: none;
  transition: all 0.25s;
}
.related-card:hover {
  border-color: rgba(30, 94, 182, 0.25);
  box-shadow: 0 4px 16px rgba(30, 94, 182, 0.08);
  transform: translateY(-2px);
}
.related-card h4 {
  font-size: 0.92rem;
  font-weight: 600;
  color: var(--related-card-h4-color, #333);
  margin: 0 0 0.4rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s;
}
.related-card:hover h4 {
  color: var(--color-primary);
}
.related-card p {
  font-size: 0.8rem;
  color: var(--related-card-p-color, #888);
  line-height: 1.5;
  margin: 0 0 0.6rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.related-meta {
  display: flex;
  gap: 0.75rem;
  font-size: 0.72rem;
  color: var(--related-meta-color, #bbb);
}

/* Responsive */
@media (max-width: 900px) {
  .toc-sidebar { display: none; }
  .article-layout { flex-direction: column; }
}
@media (max-width: 768px) {
  /* Edge-to-edge card on mobile */
  .article-card {
    border-radius: 0;
    border-left: none;
    border-right: none;
    box-shadow: none;
    max-width: 100% !important;
  }
  .skeleton-card {
    padding: 0.75rem;
  }
  /* Text wrapping for mobile */
  .article-body {
    overflow-wrap: break-word !important;
    word-break: break-word !important;
    overflow-x: hidden !important;
    max-width: 100% !important;
  }
  .article-body .md-editor-preview,
  .article-body .content {
    overflow-wrap: anywhere !important;
    word-break: break-word !important;
    max-width: 100% !important;
    width: 100% !important;
    min-width: 0 !important;
    overflow-x: hidden !important;
  }
  /* All text wraps — catch-all covers every element inside content */
  .article-body .md-editor-preview *,
  .article-body .content * {
    word-break: break-word !important;
    overflow-wrap: anywhere !important;
  }
  .article-body .md-editor-preview code:not(pre code),
  .article-body .content code:not(pre code) {
    word-break: break-word !important;
    overflow-wrap: anywhere !important;
    max-width: 100%;
  }
  /* Compact header */
  .article-header { padding: 1rem 0.75rem 0.75rem; }
  .article-header h1 { font-size: 1.35rem; line-height: 1.4; }
  .meta { font-size: 0.78rem; flex-wrap: wrap; gap: 0.25rem 0.5rem; }
  /* Cover image full-bleed */
  .article-cover { border-radius: 0; max-height: 200px; }
  /* Body */
  .article-body { padding: 0.75rem; }
  .article-body .content,
  .article-body .md-editor-preview { font-size: max(var(--reading-font-size, 16px), 16px); }
  /* Footer: larger touch targets */
  .article-footer { padding: 1rem 0.75rem; }
  .footer-actions { gap: 1.25rem; }
  .share-btn { padding: 10px 20px; font-size: 14px; }
  /* Navigation */
  .article-nav { flex-direction: column; }
  .nav-item { max-width: 100%; }
  .related-grid { grid-template-columns: 1fr; }
  .related-card h4 { white-space: normal; }
  /* Content responsive */
  :is(.content, .md-editor-preview) table { display: block; overflow-x: auto; }
  :is(.content, .md-editor-preview) th,
  :is(.content, .md-editor-preview) td { padding: 0.5rem 0.65rem; font-size: 0.8rem; }
  /* Code blocks: independent scrolling to preserve formatting */
  .article-body .md-editor-preview pre,
  .article-body .content pre {
    white-space: pre !important;
    word-break: normal !important;
    overflow-wrap: normal !important;
    overflow-x: auto !important;
    overflow-y: auto !important;
    max-height: 24em !important;
    max-width: 100% !important;
  }
  /* All descendants inside pre: preserve formatting, no wrapping */
  .article-body .md-editor-preview pre *,
  .article-body .content pre * {
    white-space: pre !important;
    word-break: normal !important;
    overflow-wrap: normal !important;
    overflow-x: visible !important;
  }
  .article-body .md-editor-preview pre code,
  .article-body .content pre code {
    display: table !important;
    min-width: 100% !important;
    width: auto !important;
    white-space: pre !important;
    word-break: normal !important;
    overflow-wrap: normal !important;
    overflow-x: visible !important;
  }
  .article-body .md-editor-preview .md-editor-code pre code .md-editor-code-block {
    white-space: pre !important;
    word-break: normal !important;
    overflow-wrap: normal !important;
    width: auto !important;
    overflow-x: visible !important;
  }
  /* Long code blocks (>10 lines): taller max-height */
  .article-body .md-editor-preview pre.code-long,
  .article-body .content pre.code-long {
    max-height: 28em !important;
  }
  :is(.content, .md-editor-preview) img { max-width: 100%; height: auto; }
  :is(.content, .md-editor-preview) blockquote { padding: 0.5rem 0.75rem; }
  :is(.content, .md-editor-preview) ul,
  :is(.content, .md-editor-preview) ol { padding-left: 1.2rem; }
  :is(.content, .md-editor-preview) h1 { font-size: 1.3rem; }
  :is(.content, .md-editor-preview) h2 { font-size: 1.15rem; }
  :is(.content, .md-editor-preview) h3 { font-size: 1.05rem; }
  .article-body .md-editor-preview .katex-display { overflow-x: auto; }
  .toc-sidebar { display: none; }
}
@media (max-width: 400px) {
  .article-header { padding: 0.75rem 0.5rem 0.5rem; }
  .article-body { padding: 0.5rem; }
  .article-footer { padding: 0.75rem 0.5rem; }
  .article-header h1 { font-size: 1.15rem; }
  :is(.content, .md-editor-preview) h2 { font-size: 1.05rem; }
  :is(.content, .md-editor-preview) h3 { font-size: 0.98rem; }
  .article-body .md-editor-preview pre,
  .article-body .md-editor-preview pre code { padding: 0.65rem !important; font-size: 0.72rem !important; }
  .article-body .md-editor-preview .md-editor-code pre { padding: 0 !important; }
  .article-body .md-editor-preview pre.code-long { max-height: 18em !important; }
  :is(.content, .md-editor-preview) blockquote { padding: 0.4rem 0.6rem; }
  :is(.content, .md-editor-preview) ul,
  :is(.content, .md-editor-preview) ol { padding-left: 1rem; }
  .nav-item { padding: 0.75rem 1rem; }
  .tag { font-size: 0.72rem; padding: 0.1rem 0.45rem; }
}

/* ===== Night Mode ===== */
.night .article-detail-wrapper { background: transparent; }
.night {
  --article-card-bg: rgba(15, 23, 42, 0.95);
  --article-card-border: rgba(255, 255, 255, 0.08);
  --article-header-border: rgba(255, 255, 255, 0.08);
  --article-title-color: #e0e0e0;
  --meta-color: #a0aec0;
  --meta-category-color: #93c5fd;
  --meta-dot-color: #4a5568;
  --tag-bg: rgba(255, 255, 255, 0.05);
  --tag-border: rgba(255, 255, 255, 0.12);
  --tag-color: #a0aec0;
  --content-color: #d1d5db;
  --content-heading-color: #e0e0e0;
  --content-strong-color: #e0e0e0;
  --content-blockquote-color: #a0aec0;
  --content-blockquote-bg: rgba(255, 255, 255, 0.03);
  --content-blockquote-border: #4a5568;
  --content-code-color: #f687b3;
  --content-code-bg: rgba(255, 255, 255, 0.08);
  --content-pre-bg: #1a1b2e;
  --content-table-border: rgba(255, 255, 255, 0.08);
  --content-th-bg: rgba(30, 94, 182, 0.3);
  --content-th-color: #e0e0e0;
  --content-td-color: #d1d5db;
  --content-td-border: rgba(255, 255, 255, 0.08);
  --content-li-color: #d1d5db;
  --footer-bg: rgba(15, 23, 42, 0.6);
  --footer-border: rgba(255, 255, 255, 0.08);
  --like-btn-bg: rgba(255, 255, 255, 0.05);
  --like-btn-border: rgba(255, 255, 255, 0.15);
  --like-btn-color: #a0aec0;
  --toc-card-bg: rgba(15, 23, 42, 0.95);
  --toc-card-border: rgba(255, 255, 255, 0.08);
  --toc-title-color: #e0e0e0;
  --toc-title-border: rgba(255, 255, 255, 0.08);
  --toc-item-color: #a0aec0;
  --nav-item-bg: rgba(15, 23, 42, 0.95);
  --nav-item-border: rgba(255, 255, 255, 0.08);
  --nav-label-color: #718096;
  --nav-title-color: #e0e0e0;
  --related-title-color: #e0e0e0;
  --related-card-bg: rgba(15, 23, 42, 0.95);
  --related-card-border: rgba(255, 255, 255, 0.08);
  --related-card-h4-color: #e0e0e0;
  --related-card-p-color: #a0aec0;
  --related-meta-color: #718096;
}
.night .article-card {
  background: rgba(15, 23, 42, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.3);
}
.night .article-header { border-bottom-color: rgba(255, 255, 255, 0.08); }
.night .article-header h1 { color: #e0e0e0; }
.night .meta { color: #a0aec0; }
.night .meta-category { color: #93c5fd; }
.night .meta-dot { color: #4a5568; }
.night .tag {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #a0aec0;
}
.night .tag:hover { border-color: #5a9bff; color: #5a9bff; }

/* Content night */
:is(.night .content, .night .md-editor-preview) { color: #d1d5db; }
:is(.night .content, .night .md-editor-preview) p { color: #d1d5db; }
:is(.night .content, .night .md-editor-preview) strong { color: #e0e0e0; }
:is(.night .content, .night .md-editor-preview) em { color: #cbd5e1; }
:is(.night .content, .night .md-editor-preview) h1,
:is(.night .content, .night .md-editor-preview) h2,
:is(.night .content, .night .md-editor-preview) h3,
:is(.night .content, .night .md-editor-preview) h4,
:is(.night .content, .night .md-editor-preview) h5,
:is(.night .content, .night .md-editor-preview) h6 { color: #e0e0e0; }
:is(.night .content, .night .md-editor-preview) a { color: #93c5fd; }
:is(.night .content, .night .md-editor-preview) a:hover { color: #bfdbfe; }
:is(.night .content, .night .md-editor-preview) blockquote {
  color: #a0aec0;
  border-left-color: #4a5568;
  background: rgba(255, 255, 255, 0.03);
}
:is(.night .content, .night .md-editor-preview) code:not(pre code) {
  color: #f687b3;
  background: rgba(255, 255, 255, 0.08);
}
:is(.night .content, .night .md-editor-preview) pre {
  /* background handled by md-editor-v3 library (.md-editor-code-head + pre code) */
}
.night .content pre code { color: #cdd6f4; }
:is(.night .content, .night .md-editor-preview) img { opacity: 0.9; }
:is(.night .content, .night .md-editor-preview) hr { border-color: rgba(255, 255, 255, 0.1); }
:is(.night .content, .night .md-editor-preview) table { border-color: rgba(255, 255, 255, 0.1); }
:is(.night .content, .night .md-editor-preview) th {
  background: rgba(30, 94, 182, 0.3);
  color: #e0e0e0;
  border-color: rgba(255, 255, 255, 0.1);
}
:is(.night .content, .night .md-editor-preview) td {
  color: #d1d5db;
  border-color: rgba(255, 255, 255, 0.08);
}
:is(.night .content, .night .md-editor-preview) tr:nth-child(even) td { background: rgba(255, 255, 255, 0.02); }
:is(.night .content, .night .md-editor-preview) tr:hover td { background: rgba(90, 155, 255, 0.06); }
:is(.night .content, .night .md-editor-preview) li { color: #d1d5db; }
:is(.night .content, .night .md-editor-preview) li::marker { color: #6b7280; }
.night .heading-anchor { color: #4a5568; }
.night .heading-anchor:hover { color: #93c5fd; }

/* Footer night */
.night .article-footer { background: rgba(15, 23, 42, 0.6); border-top-color: rgba(255, 255, 255, 0.08); }
.night .like-count-text { color: #a0aec0; }

/* TOC night */
.night .toc-card {
  background: rgba(15, 23, 42, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
}
.night .toc-title { color: #e0e0e0; border-bottom-color: rgba(255, 255, 255, 0.08); }
.night .toc-item { color: #a0aec0; }
.night .toc-item:hover { color: #93c5fd; background: rgba(255, 255, 255, 0.04); }
.night .toc-item.active {
  color: #93c5fd;
  border-left-color: #93c5fd;
  background: rgba(90, 155, 255, 0.08);
}

/* Nav night */
.night .nav-item {
  background: rgba(15, 23, 42, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
}
.night .nav-item:hover { border-color: rgba(90, 155, 255, 0.3); box-shadow: 0 2px 12px rgba(90, 155, 255, 0.1); }
.night .nav-label { color: #718096; }
.night .nav-title { color: #e0e0e0; }
.night .nav-item:hover .nav-title { color: #93c5fd; }

/* Related night */
.night .related-title { color: #e0e0e0; }
.night .related-card {
  background: rgba(15, 23, 42, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
}
.night .related-card:hover { border-color: rgba(90, 155, 255, 0.25); box-shadow: 0 4px 16px rgba(90, 155, 255, 0.08); }
.night .related-card h4 { color: #e0e0e0; }
.night .related-card:hover h4 { color: #93c5fd; }
.night .related-card p { color: #a0aec0; }
.night .related-meta { color: #718096; }

/* Author's Notes floating button */
.author-notes-btn {
  position: fixed;
  right: 32px;
  bottom: 160px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border: none;
  border-radius: 24px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.4);
  transition: all 0.3s;
  z-index: 100;
  white-space: nowrap;
}
.author-notes-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}
.author-notes-btn-icon { font-size: 16px; }
.author-notes-btn-text { line-height: 1; }

/* Author's Notes popup overlay */
.author-notes-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: author-notes-fade-in 0.2s ease;
}
@keyframes author-notes-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.author-notes-popup {
  background: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 560px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: author-notes-scale-in 0.25s ease;
}
@keyframes author-notes-scale-in {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}

.author-notes-popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f2f5;
}
.author-notes-popup-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}
.author-notes-close {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  font-size: 20px;
  color: #999;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s;
}
.author-notes-close:hover {
  background: #f5f5f5;
  color: #333;
}

.author-notes-popup-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 20px;
}

/* Author's Notes night mode */
.night .author-notes-popup {
  background: #1e1e1e;
  border: 1px solid rgba(255, 255, 255, 0.08);
}
.night .author-notes-popup-header { border-bottom-color: rgba(255, 255, 255, 0.06); }
.night .author-notes-popup-title { color: #e0e0e0; }
.night .author-notes-close { color: #888; }
.night .author-notes-close:hover { background: rgba(255, 255, 255, 0.08); color: #e0e0e0; }

/* ========== Download Button ========== */
.download-wrapper {
  position: fixed;
  left: 32px;
  bottom: 40px;
  z-index: 100;
}
.download-btn {
  position: relative;
  overflow: hidden;
  height: 3rem;
  padding: 0 2rem;
  border-radius: 1.5rem;
  background: #3d3a4e;
  color: #fff;
  border: none;
  cursor: pointer;
  transition: opacity 0.2s;
}
.download-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.download-btn::before {
  content: "";
  position: absolute;
  top: 0; left: 0;
  transform: scaleX(0);
  transform-origin: 0 50%;
  width: 100%;
  height: inherit;
  border-radius: inherit;
  background: linear-gradient(82.3deg, rgba(34, 197, 94, 1) 10.8%, rgba(16, 185, 129, 1) 94.3%);
  transition: all 0.475s;
  z-index: 0;
}
.download-btn:hover::before { transform: scaleX(1); }
.download-btn-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
}
.download-spinner {
  width: 14px; height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.download-menu {
  position: absolute;
  bottom: 100%;
  left: 0;
  margin-bottom: 8px;
  background: var(--article-card-bg, #fff);
  border: 1px solid var(--article-card-border, #e8e8e8);
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  min-width: 170px;
  overflow: hidden;
  animation: panel-in 0.2s ease-out;
}
.download-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 16px;
  border: none;
  background: none;
  color: var(--content-color, #333);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
  text-align: left;
}
.download-menu-item:hover {
  background: rgba(34, 197, 94, 0.06);
  color: #16a34a;
}
.download-menu-item svg { flex-shrink: 0; }

/* ========== Print Button (Typewriter) ========== */
.print-btn {
  position: fixed;
  left: 32px;
  bottom: 100px;
  z-index: 100;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  transition: transform 0.2s;
}
.print-btn:hover { transform: scale(1.15); }

.typewriter {
  --blue: #5C86FF;
  --blue-dark: #275EFE;
  --key: #fff;
  --paper: #EEF0FD;
  --text: #D3D4EC;
  --tool: #FBC56C;
  --duration: 3s;
  position: relative;
  animation: bounce05 var(--duration) linear infinite;
}
.typewriter .slide {
  width: 92px;
  height: 20px;
  border-radius: 3px;
  margin-left: 14px;
  transform: translateX(14px);
  background: linear-gradient(var(--blue), var(--blue-dark));
  animation: slide05 var(--duration) ease infinite;
}
.typewriter .slide:before, .typewriter .slide:after,
.typewriter .slide i:before {
  content: "";
  position: absolute;
  background: var(--tool);
}
.typewriter .slide:before {
  width: 2px;
  height: 8px;
  top: 6px;
  left: 100%;
}
.typewriter .slide:after {
  left: 94px;
  top: 3px;
  height: 14px;
  width: 6px;
  border-radius: 3px;
}
.typewriter .slide i {
  display: block;
  position: absolute;
  right: 100%;
  width: 6px;
  height: 4px;
  top: 4px;
  background: var(--tool);
}
.typewriter .slide i:before {
  right: 100%;
  top: -2px;
  width: 4px;
  border-radius: 2px;
  height: 14px;
}
.typewriter .paper {
  position: absolute;
  left: 24px;
  top: -26px;
  width: 40px;
  height: 46px;
  border-radius: 5px;
  background: var(--paper);
  transform: translateY(46px);
  animation: paper05 var(--duration) linear infinite;
}
.typewriter .paper:before {
  content: "";
  position: absolute;
  left: 6px;
  right: 6px;
  top: 7px;
  border-radius: 2px;
  height: 4px;
  transform: scaleY(0.8);
  background: var(--text);
  box-shadow: 0 12px 0 var(--text), 0 24px 0 var(--text), 0 36px 0 var(--text);
}
.typewriter .keyboard {
  width: 120px;
  height: 56px;
  margin-top: -10px;
  z-index: 1;
  position: relative;
}
.typewriter .keyboard:before, .typewriter .keyboard:after {
  content: "";
  position: absolute;
}
.typewriter .keyboard:before {
  top: 0; left: 0; right: 0; bottom: 0;
  border-radius: 7px;
  background: linear-gradient(135deg, var(--blue), var(--blue-dark));
  transform: perspective(10px) rotateX(2deg);
  transform-origin: 50% 100%;
}
.typewriter .keyboard:after {
  left: 2px; top: 25px;
  width: 11px; height: 4px;
  border-radius: 2px;
  box-shadow: 15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key), 60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key), 22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key), 60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key);
  animation: keyboard05 var(--duration) linear infinite;
}
@keyframes bounce05 {
  85%, 92%, 100% { transform: translateY(0); }
  89% { transform: translateY(-4px); }
  95% { transform: translateY(2px); }
}
@keyframes slide05 {
  5% { transform: translateX(14px); }
  15%, 30% { transform: translateX(6px); }
  40%, 55% { transform: translateX(0); }
  65%, 70% { transform: translateX(-4px); }
  80%, 89% { transform: translateX(-12px); }
  100% { transform: translateX(14px); }
}
@keyframes paper05 {
  5% { transform: translateY(46px); }
  20%, 30% { transform: translateY(34px); }
  40%, 55% { transform: translateY(22px); }
  65%, 70% { transform: translateY(10px); }
  80%, 85% { transform: translateY(0); }
  92%, 100% { transform: translateY(46px); }
}
@keyframes keyboard05 {
  5%, 12%, 21%, 30%, 39%, 48%, 57%, 66%, 75%, 84% {
    box-shadow: 15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key), 60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key), 22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key), 60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key);
  }
  9% { box-shadow: 15px 2px 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key), 60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key), 22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key), 60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key); }
  18% { box-shadow: 15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key), 60px 2px 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key), 22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key), 60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key); }
  27% { box-shadow: 15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key), 60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key), 22px 12px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key), 60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key); }
  36% { box-shadow: 15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key), 60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key), 22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 12px 0 var(--key), 60px 12px 0 var(--key), 68px 12px 0 var(--key), 83px 10px 0 var(--key); }
  45% { box-shadow: 15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key), 60px 0 0 var(--key), 75px 0 0 var(--key), 90px 2px 0 var(--key), 22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key), 60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key); }
  54% { box-shadow: 15px 0 0 var(--key), 30px 2px 0 var(--key), 45px 0 0 var(--key), 60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key), 22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key), 60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key); }
  63% { box-shadow: 15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key), 60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key), 22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key), 60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 12px 0 var(--key); }
  72% { box-shadow: 15px 0 0 var(--key), 30px 0 0 var(--key), 45px 2px 0 var(--key), 60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key), 22px 10px 0 var(--key), 37px 10px 0 var(--key), 52px 10px 0 var(--key), 60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key); }
  81% { box-shadow: 15px 0 0 var(--key), 30px 0 0 var(--key), 45px 0 0 var(--key), 60px 0 0 var(--key), 75px 0 0 var(--key), 90px 0 0 var(--key), 22px 10px 0 var(--key), 37px 12px 0 var(--key), 52px 10px 0 var(--key), 60px 10px 0 var(--key), 68px 10px 0 var(--key), 83px 10px 0 var(--key); }
}

/* ========== Settings Button & Panel ========== */
.settings-btn {
  position: fixed;
  right: 32px;
  bottom: 200px;
  z-index: 100;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--article-card-bg, #fff);
  border: 1px solid var(--article-card-border, #e8e8e8);
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--content-color, #555);
  transition: all 0.2s;
}
.settings-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  color: var(--color-primary, #3b82f6);
}

.settings-panel {
  position: fixed;
  right: 32px;
  bottom: 250px;
  z-index: 200;
  width: 280px;
  background: var(--article-card-bg, #fff);
  border: 1px solid var(--article-card-border, #e8e8e8);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.12);
  padding: 16px;
  animation: panel-in 0.2s ease-out;
}
@keyframes panel-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.settings-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--content-heading-color, #1a1a1a);
}
.settings-reset {
  font-size: 12px;
  font-weight: 400;
  color: var(--color-primary, #3b82f6);
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: background 0.15s;
}
.settings-reset:hover { background: rgba(59, 130, 246, 0.08); }

.settings-item {
  margin-bottom: 12px;
}
.settings-item:last-child { margin-bottom: 0; }
.settings-item label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--meta-color, #888);
  margin-bottom: 6px;
}
.settings-val {
  font-variant-numeric: tabular-nums;
  color: var(--content-color, #555);
  font-weight: 500;
}
.settings-item input[type="range"] {
  width: 100%;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: var(--article-card-border, #e0e0e0);
  border-radius: 2px;
  outline: none;
}
.settings-item input[type="range"]::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--color-primary, #3b82f6);
  cursor: pointer;
  border: 2px solid #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.15);
}
.settings-item select {
  width: 100%;
  padding: 6px 8px;
  border-radius: 6px;
  border: 1px solid var(--article-card-border, #e0e0e0);
  background: var(--article-card-bg, #fff);
  color: var(--content-color, #333);
  font-size: 13px;
  outline: none;
  cursor: pointer;
}
.settings-item select:focus { border-color: var(--color-primary, #3b82f6); }

/* ========== Print Styles ========== */
@media print {
  /* ===== 1. 隐藏所有非文章元素 ===== */
  /* 网站级组件 */
  .header,
  .back-btn,
  .footer,
  .site-footer,
  .cyber-cat-desktop,
  .cyber-cat-wrapper,
  .floating-player,
  .ai-assistant,
  .midnight-sky,
  .cmd-overlay,
  .kick-dialog,
  /* 文章页浮动按钮 */
  .fan-actions,
  .author-notes-btn,
  .settings-btn,
  .settings-panel,
  .download-wrapper,
  .print-btn,
  .mobile-toc-btn,
  .mobile-toc-overlay,
  /* 非文章内容 */
  .article-nav,
  .related-section,
  .toc-sidebar,
  .article-footer,
  .reading-progress-bar,
  .copy-btn {
    display: none !important;
  }

  /* ===== 2. 打印页面基础设置 ===== */
  @page {
    margin: 1.5cm;
    size: A4;
  }

  /* ===== 3. 打印布局优化 ===== */
  .article-detail-wrapper {
    max-width: none !important;
    padding: 0 !important;
    margin: 0 !important;
  }
  .article-detail {
    max-width: none !important;
    padding: 0 !important;
  }
  .article-card {
    border: none !important;
    box-shadow: none !important;
    background: #fff !important;
    border-radius: 0 !important;
    padding: 0 !important;
  }
  .article-body { padding: 0; }
  .article-header { page-break-after: avoid; }

  /* ===== 4. 字体设置适配打印 ===== */
  .article-body {
    font-family: var(--reading-font-family, inherit) !important;
  }
  .article-body .content,
  .article-body .md-editor-preview {
    font-size: var(--reading-font-size, 16px) !important;
    line-height: var(--reading-line-height, 1.75) !important;
    letter-spacing: var(--reading-letter-spacing, 0) !important;
  }
  .article-body .content p,
  .article-body .md-editor-preview p {
    margin-bottom: var(--reading-paragraph-spacing, 1rem) !important;
  }

  /* ===== 5. 代码块/表格/引用/图片 ===== */
  .content pre,
  .md-editor-preview pre {
    page-break-inside: avoid;
    background: #1e1e2e !important;
    color: #cdd6f4 !important;
    border: 1px solid #333 !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  .content pre code,
  .md-editor-preview pre code {
    color: #cdd6f4 !important;
    background: none !important;
  }
  .content code:not(pre code),
  .md-editor-preview code:not(pre code) {
    background: #f4f5f7 !important;
    color: #d63384 !important;
    padding: 0.15rem 0.4rem !important;
    border-radius: 4px !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  .content blockquote,
  .md-editor-preview blockquote {
    border-left: 3px solid #3b82f6 !important;
    background: #f8f9fb !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  .content th,
  .md-editor-preview th {
    background: #1e5eb6 !important;
    color: #fff !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  .content tr:nth-child(even) td,
  .md-editor-preview tr:nth-child(even) td {
    background: #f9fafb !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
  .content img,
  .md-editor-preview img { max-width: 100%; }
}

/* ========== Night Mode: New Elements ========== */
.night .settings-btn {
  background: rgba(30, 41, 59, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
  color: #a0aec0;
}
.night .settings-btn:hover { color: #93c5fd; }
.night .settings-panel {
  background: rgba(30, 41, 59, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 32px rgba(0,0,0,0.4);
}
.night .settings-panel-header { color: #e0e0e0; }
.night .settings-item select {
  background: rgba(15, 23, 42, 0.8);
  border-color: rgba(255, 255, 255, 0.08);
  color: #d1d5db;
}
.night .settings-item input[type="range"] {
  background: rgba(255, 255, 255, 0.1);
}

/* Night mode: Download button */
.night .download-menu {
  background: rgba(30, 41, 59, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 24px rgba(0,0,0,0.4);
}
.night .download-menu-item { color: #d1d5db; }
.night .download-menu-item:hover { background: rgba(34, 197, 94, 0.12); color: #4ade80; }

/* ========== Reading Progress Percentage ========== */
.reading-progress-pct {
  position: absolute;
  right: 0;
  top: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: var(--color-primary, #3b82f6);
  padding: 1px 6px 2px;
  border-radius: 0 4px 4px 0;
  line-height: 1.4;
  font-variant-numeric: tabular-nums;
  pointer-events: none;
  white-space: nowrap;
}

/* ========== Article Cover Image ========== */
.article-cover {
  width: 100%;
  max-height: 400px;
  overflow: hidden;
  border-radius: 16px 16px 0 0;
}
.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
@media (max-width: 768px) {
  .article-cover { max-height: 220px; border-radius: 12px 12px 0 0; }
}

/* ========== Footer Actions (Like + Share) ========== */
.footer-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1.5rem;
}
.share-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: none;
  border: 1px solid var(--article-card-border, #e0e0e0);
  border-radius: 20px;
  color: var(--meta-color, #888);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.share-btn:hover {
  border-color: var(--color-primary, #3b82f6);
  color: var(--color-primary, #3b82f6);
  background: rgba(59, 130, 246, 0.04);
}
.share-copied-text {
  color: #22c55e;
  font-weight: 500;
  font-size: 12px;
}
.night .share-btn {
  border-color: rgba(255, 255, 255, 0.12);
  color: #a0aec0;
}
.night .share-btn:hover {
  border-color: #5a9bff;
  color: #5a9bff;
  background: rgba(90, 155, 255, 0.08);
}

/* ========== Error State ========== */
.error-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 50vh;
}
.error-card {
  text-align: center;
  padding: 3rem 2rem;
  max-width: 440px;
}
.error-icon {
  color: var(--color-error, #ef4444);
  margin-bottom: 1.25rem;
  opacity: 0.7;
}
.error-card h2 {
  font-size: 1.3rem;
  font-weight: 600;
  color: var(--content-heading-color, #1a1a1a);
  margin: 0 0 0.5rem;
}
.error-card p {
  font-size: 0.9rem;
  color: var(--meta-color, #888);
  margin: 0 0 1.5rem;
  line-height: 1.6;
}
.error-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
  flex-wrap: wrap;
}
.error-retry-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: var(--color-primary, #3b82f6);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}
.error-retry-btn:hover { background: var(--color-primary-hover, #2563eb); }
.error-back-btn {
  display: inline-flex;
  align-items: center;
  padding: 10px 20px;
  background: none;
  border: 1px solid var(--article-card-border, #e0e0e0);
  border-radius: 8px;
  color: var(--content-color, #555);
  font-size: 14px;
  text-decoration: none;
  transition: all 0.2s;
}
.error-back-btn:hover {
  border-color: var(--color-primary, #3b82f6);
  color: var(--color-primary, #3b82f6);
}
.night .error-card h2 { color: #e0e0e0; }
.night .error-card p { color: #a0aec0; }
.night .error-icon { color: #f87171; }
.night .error-back-btn {
  border-color: rgba(255, 255, 255, 0.12);
  color: #a0aec0;
}
.night .error-back-btn:hover {
  border-color: #5a9bff;
  color: #5a9bff;
}

/* ========== Mobile TOC Button ========== */
.mobile-toc-btn {
  display: none;
  position: fixed;
  right: 16px;
  bottom: 80px;
  z-index: 100;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--article-card-bg, #fff);
  border: 1px solid var(--article-card-border, #e8e8e8);
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--content-color, #555);
  transition: all 0.2s;
}
.mobile-toc-btn:hover {
  transform: scale(1.1);
  color: var(--color-primary, #3b82f6);
}
@media (max-width: 900px) {
  .mobile-toc-btn { display: flex; }
}

/* Mobile TOC Drawer */
.mobile-toc-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 3000;
}
.mobile-toc-drawer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  max-height: 60vh;
  background: var(--article-card-bg, #fff);
  border-radius: 16px 16px 0 0;
  box-shadow: 0 -8px 32px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  animation: slide-up 0.25s ease-out;
}
@keyframes slide-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
.mobile-toc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--article-card-border, #f0f0f0);
  font-size: 15px;
  font-weight: 600;
  color: var(--content-heading-color, #1a1a1a);
  flex-shrink: 0;
}
.mobile-toc-close {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  font-size: 22px;
  color: #999;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s;
}
.mobile-toc-close:hover { background: #f5f5f5; color: #333; }
.mobile-toc-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}
.mobile-toc-item {
  display: block;
  padding: 10px 20px;
  font-size: 14px;
  color: var(--content-color, #555);
  text-decoration: none;
  border-left: 3px solid transparent;
  transition: all 0.15s;
}
.mobile-toc-item:hover {
  background: #f8f9fb;
  color: var(--color-primary, #3b82f6);
}
.mobile-toc-item.active {
  color: var(--color-primary, #3b82f6);
  border-left-color: var(--color-primary, #3b82f6);
  background: rgba(59, 130, 246, 0.04);
  font-weight: 500;
}
.mobile-toc-h3 {
  padding-left: 36px;
  font-size: 13px;
}
.mobile-toc-fade-enter-active,
.mobile-toc-fade-leave-active {
  transition: opacity 0.2s;
}
.mobile-toc-fade-enter-from,
.mobile-toc-fade-leave-to {
  opacity: 0;
}

/* Night: Mobile TOC */
.night .mobile-toc-btn {
  background: rgba(30, 41, 59, 0.95);
  border-color: rgba(255, 255, 255, 0.08);
  color: #a0aec0;
}
.night .mobile-toc-btn:hover { color: #93c5fd; }
.night .mobile-toc-overlay { background: rgba(0, 0, 0, 0.6); }
.night .mobile-toc-drawer {
  background: rgba(15, 23, 42, 0.98);
}
.night .mobile-toc-header {
  color: #e0e0e0;
  border-bottom-color: rgba(255, 255, 255, 0.08);
}
.night .mobile-toc-close { color: #718096; }
.night .mobile-toc-close:hover { background: rgba(255, 255, 255, 0.08); color: #e0e0e0; }
.night .mobile-toc-item { color: #a0aec0; }
.night .mobile-toc-item:hover { background: rgba(255, 255, 255, 0.04); color: #93c5fd; }
.night .mobile-toc-item.active { color: #93c5fd; border-left-color: #93c5fd; background: rgba(90, 155, 255, 0.08); }

/* ========== Mobile: Hide non-essential floating buttons ========== */
@media (max-width: 768px) {
  /* Hide desktop-only floating actions */
  .settings-btn,
  .settings-panel,
  .print-btn,
  .download-wrapper,
  .author-notes-btn,
  .fan-actions { display: none !important; }
  .author-notes-popup {
    width: 95%;
    max-height: 80vh;
  }
  .mobile-toc-btn { right: 16px; bottom: 16px; }
  /* Mobile author notes button in footer */
  .mobile-notes-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 14px;
    background: none;
    border: 1px solid var(--article-card-border, #e0e0e0);
    border-radius: 20px;
    color: var(--meta-color, #888);
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;
  }
  .mobile-notes-btn:hover {
    border-color: var(--color-primary, #3b82f6);
    color: var(--color-primary, #3b82f6);
  }
}
@media (min-width: 769px) {
  .mobile-notes-btn { display: none; }
}
</style>
