<template>
  <div class="music-page" :class="{ 'is-night': isNight }">
    <!-- Background blur -->
    <div class="music-bg" v-if="currentSong?.coverUrl && !failedCovers.has(currentSong.coverUrl)">
      <div class="music-bg-img" :style="{ backgroundImage: `url(${currentSong.coverUrl})` }"></div>
      <div class="music-bg-overlay"></div>
    </div>

    <!-- Loading -->
    <div v-if="isLoading" class="music-loading">
      <div class="music-loading-disc">♪</div>
      <span class="music-loading-text">{{ $t('music.connecting') }}</span>
    </div>

    <!-- No songs -->
    <div v-else-if="playlist.length === 0" class="music-empty">
      <div class="music-empty-icon">♫</div>
      <span>{{ $t('music.noSongs') }}</span>
    </div>

    <!-- Main content -->
    <div v-else class="music-content">
      <div class="music-header">
        <h1 class="music-title">{{ $t('music.title') }}</h1>
        <p class="music-desc">{{ $t('music.desc') }}</p>
      </div>

      <div class="music-grid">
        <!-- Left: Disc console -->
        <div class="music-disc-panel">
          <div class="disc-info-wrapper">
            <div class="disc-area">
              <div class="disc-glow" :class="{ active: isPlaying }"></div>
              <div class="disc-shadow"></div>
              <div class="disc-rotating" :class="{ spinning: isPlaying }">
                <img
                  v-if="currentSong?.coverUrl && !failedCovers.has(currentSong.coverUrl)"
                  :src="currentSong.coverUrl"
                  @error="onCoverError($event, currentSong.coverUrl)"
                  alt="cover"
                  class="disc-cover"
                />
                <div v-else class="disc-cover-placeholder">♪</div>
                <div class="disc-center"></div>
                <div class="disc-shine"></div>
              </div>
            </div>

            <div class="song-info">
              <h2 class="song-name">{{ currentSong?.name || '' }}</h2>
              <p class="song-artist">{{ currentSong?.artist || '' }}</p>
            </div>
          </div>

          <div class="transport-wrapper">
            <!-- Seek bar -->
            <div class="seek-bar">
              <input
                type="range"
                min="0"
                max="100"
                :value="progress"
                @input="onSeek"
                class="seek-input"
                :style="seekStyle"
              />
              <div class="seek-time">
                <span>{{ formatTime(currentTime) }}</span>
                <span v-if="isBilibiliLoading" class="bilibili-loading">加载中...</span>
                <span v-else>{{ formatTime(duration) }}</span>
              </div>
            </div>
              <a
                v-if="currentSong?.source === 'bilibili' && currentSong?.bvid"
                :href="`https://www.bilibili.com/video/${currentSong.bvid}`"
                target="_blank"
                rel="noopener"
                class="bilibili-link"
              >{{ $t('music.watchOnBilibili') }} ↗</a>

          <!-- Controls -->
          <div class="transport">
            <button class="ctrl-btn mode-btn" @click="cyclePlayMode" :title="playModeLabel">
              <svg v-if="playMode === 'loop'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg>
              <svg v-else-if="playMode === 'single'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/><text x="10" y="15" font-size="8" fill="currentColor" stroke="none" font-weight="bold">1</text></svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="16 3 21 3 21 8"/><line x1="4" y1="20" x2="21" y2="3"/><polyline points="21 16 21 21 16 21"/><line x1="15" y1="15" x2="21" y2="21"/><line x1="4" y1="4" x2="9" y2="9"/></svg>
            </button>
            <button class="ctrl-btn" @click="prev">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/></svg>
            </button>
            <button class="ctrl-btn play-btn" @click="togglePlay">
              <svg v-if="!isPlaying" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
              <svg v-else viewBox="0 0 24 24" fill="currentColor"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
            </button>
            <button class="ctrl-btn" @click="next">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
            </button>
            <div class="volume-area" @mouseenter="onVolumeAreaMouseEnter" @mouseleave="onVolumeAreaMouseLeave">
              <Transition name="vol">
                <div v-if="showVolume" class="volume-slider">
                  <input
                    type="range" min="0" max="1" step="0.01"
                    :value="isMuted ? 0 : volume"
                    @input="onVolumeInput"
                    class="volume-input"
                    :style="volumeStyle"
                  />
                </div>
              </Transition>
              <button class="ctrl-btn volume-btn" :class="{ active: showVolume }" @click="onVolumeBtnClick">
                <svg v-if="isMuted || volume === 0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>
              </button>
              <Transition name="tip">
                <div v-if="showBilibiliVolTip" class="bilibili-vol-tip">{{ t('music.bilibiliVolumeTip') }}</div>
              </Transition>
            </div>
          </div>
          </div>
        </div>

        <!-- Right: Lyrics / Playlist / Video -->
        <div class="music-right-panel">
          <div class="tab-bar-wrapper">
            <div class="tab-bar">
              <button
                class="tab-btn"
                :class="{ active: activeTab === 'lyrics' }"
                @click="activeTab = 'lyrics'"
              >{{ $t('music.lyrics') }}</button>
              <button
                class="tab-btn"
                :class="{ active: activeTab === 'playlist' }"
                @click="activeTab = 'playlist'"
              >{{ $t('music.playlist') }}</button>
              <button
                v-if="videoList.length > 0 && !isMobile"
                class="tab-btn"
                :class="{ active: activeTab === 'video' }"
                @click="activeTab = 'video'"
              >{{ $t('music.video') || '视频' }}</button>
            </div>
          </div>

          <!-- Lyrics Tab -->
          <div v-if="activeTab === 'lyrics'" class="lyrics-panel tab-fade-in">
            <div class="lyrics-fade-top"></div>
            <div class="lyrics-fade-bottom"></div>
            <div ref="lyricContainer" class="lyrics-scroll">
              <div class="lyrics-padding">
                <div
                  v-for="(line, i) in lyrics"
                  :key="i"
                  :ref="el => { if (i === activeLyricIndex) activeLyricEl = el }"
                  class="lyric-line"
                  :class="{ active: i === activeLyricIndex }"
                  @click="onLyricClick(line)"
                >
                  <p>{{ line.text }}</p>
                </div>
                <div v-if="lyrics.length === 0" class="lyrics-empty">
                  <div class="lyrics-empty-icon">♫</div>
                  <p>{{ $t('music.noLyrics') || '暂无歌词' }}</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Video Tab -->
          <div v-if="activeTab === 'video'" class="video-tab-panel tab-fade-in">
            <div v-if="videoList.length === 0" class="lyrics-empty">
              <div class="lyrics-empty-icon">📺</div>
              <p>{{ $t('video.noVideos') || '暂无视频' }}</p>
            </div>
            <template v-else>
              <div class="video-tab-player">
                <div class="video-tab-player-wrap" ref="videoTabContainerRef"></div>
              </div>
              <div class="video-tab-info">
                <span class="video-tab-title">{{ currentVideo?.title }}</span>
                <span class="video-tab-author">{{ currentVideo?.author }}</span>
              </div>
              <div class="video-seek-bar">
                <input
                  type="range" min="0" max="100"
                  :value="videoProgress"
                  @input="onVideoSeek"
                  class="video-seek-input"
                  :style="videoSeekStyle"
                />
                <div class="video-seek-time">
                  <span>{{ formatVideoDuration(videoCurrentTime) }}</span>
                  <span>{{ formatVideoDuration(videoDuration) }}</span>
                </div>
              </div>
              <div class="video-tab-nav">
                <button class="video-tab-nav-btn" @click="setVideoIndex((videoIndex - 1 + videoList.length) % videoList.length)">
                  <svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/></svg>
                </button>
                <span class="video-tab-counter">{{ videoIndex + 1 }} / {{ videoList.length }}</span>
                <button class="video-tab-nav-btn" @click="setVideoIndex((videoIndex + 1) % videoList.length)">
                  <svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
                </button>
              </div>
            </template>
          </div>

          <!-- Playlist Tab -->
          <div v-if="activeTab === 'playlist'" class="playlist-panel tab-fade-in">
            <div class="playlist-search">
              <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
              <input
                v-model="searchQuery"
                :placeholder="$t('music.searchPlaceholder')"
                class="search-input"
              />
              <button v-if="searchQuery" class="search-clear" @click="searchQuery = ''">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="playlist-list">
              <TransitionGroup name="playlist-item">
                <div
                  v-for="song in filteredPlaylist"
                  :key="song.id"
                  class="playlist-item"
                  :class="{ active: song.id === currentSong?.id }"
                  @click="playSong(playlist.indexOf(song))"
                >
                  <div class="playlist-cover">
                    <img v-if="song.coverUrl && !failedCovers.has(song.coverUrl)" :src="song.coverUrl" @error="onCoverError($event, song.coverUrl)" alt="cover" loading="lazy" />
                    <div v-else class="playlist-cover-placeholder">♪</div>
                    <!-- Playing indicator -->
                    <div v-if="song.id === currentSong?.id && isPlaying" class="playlist-playing">
                      <span></span><span></span><span></span>
                    </div>
                  </div>
                  <div class="playlist-info">
                    <span class="playlist-song-name">{{ song.name }}</span>
                    <span class="playlist-song-artist">{{ song.artist }}</span>
                  </div>
                </div>
              </TransitionGroup>
            </div>
          </div>
        </div>
      </div>

      <!-- Bottom: Video Showcase -->
      <div v-if="videoList.length > 0 && !isMobile" class="video-showcase">
        <div class="video-showcase-header">
          <div class="video-showcase-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="23 7 16 12 23 17 23 7"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/></svg>
          </div>
          <div>
            <h2 class="video-showcase-title">{{ $t('video.title') || '云端影厅' }}</h2>
            <p class="video-showcase-desc">{{ $t('video.desc') || '精选B站视频' }}</p>
          </div>
        </div>

        <div class="video-showcase-grid">
          <!-- Player -->
          <div class="video-showcase-player">
            <div class="video-showcase-player-wrap" ref="videoShowcaseContainerRef">
              <div v-if="!videoActiveSrc || activeTab === 'video'" class="video-showcase-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="video-showcase-placeholder-icon"><polygon points="5 3 19 12 5 21 5 3"/></svg>
              </div>
            </div>
            <div class="video-showcase-info">
              <span class="video-showcase-video-title">{{ currentVideo?.title }}</span>
              <span class="video-showcase-video-author">{{ currentVideo?.author }}</span>
            </div>
            <div v-if="videoActiveSrc" class="video-seek-bar">
              <input
                type="range" min="0" max="100"
                :value="videoProgress"
                @input="onVideoSeek"
                class="video-seek-input"
                :style="videoSeekStyle"
              />
              <div class="video-seek-time">
                <span>{{ formatVideoDuration(videoCurrentTime) }}</span>
                <span>{{ formatVideoDuration(videoDuration) }}</span>
              </div>
            </div>
          </div>

          <!-- List -->
          <div class="video-showcase-list">
            <div
              v-for="(video, i) in videoList"
              :key="video.bvid"
              class="video-showcase-item"
              :class="{ active: i === videoIndex }"
              @click="onVideoClick(i)"
            >
              <div class="video-showcase-thumb">
                <img v-if="video.cover && !failedCovers.has(video.cover)" :src="proxyCoverUrl(video.cover)" loading="lazy" @error="onCoverError($event, video.cover)" alt="cover" />
                <div v-else class="video-showcase-thumb-placeholder">▶</div>
                <div v-if="i === videoIndex && videoActive && videoPlaying" class="video-showcase-playing">
                  <span></span><span></span><span></span>
                </div>
                <span v-if="video.duration" class="video-showcase-duration">{{ formatVideoDuration(video.duration) }}</span>
              </div>
              <div class="video-showcase-item-info">
                <span class="video-showcase-item-title">{{ video.title }}</span>
                <span class="video-showcase-item-author">{{ video.author }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMusicPlayer } from '../composables/useMusicPlayer'
import { useVideoPlayer } from '../composables/useVideoPlayer'
import request from '../utils/request'

const { t } = useI18n()

const {
  playlist, currentSong, isPlaying, progress, currentTime, duration,
  currentLyric, lyrics, activeLyricIndex, isLoading, isBilibiliLoading, isMobile, volume, isMuted, playMode,
  initialize, play, pause, togglePlay, next, prev, seek, setVolume, toggleMute,
  cyclePlayMode, playSong, formatTime
} = useMusicPlayer()

const {
  videoList, videoIndex, videoActive, currentVideo,
  videoCurrentTime, videoDuration, videoPlaying,
  setVideoIndex, setVideoActive, stopAllVideos, videoSeek
} = useVideoPlayer()

const activeTab = ref('lyrics')
const searchQuery = ref('')
const showVolume = ref(false)
const showBilibiliVolTip = ref(false)
const isTouchDevice = ref(false)
let bilibiliVolTipTimer = null
const lyricContainer = ref(null)
let activeLyricEl = null
const isNight = ref(document.body.classList.contains('body-night'))
const failedCovers = reactive(new Set())

// Single shared video iframe — moved between tab and showcase, never destroyed
const videoTabContainerRef = ref(null)
const videoShowcaseContainerRef = ref(null)
let iframeEl = null

const videoActiveSrc = computed(() => {
  if (!videoActive.value) return ''
  const v = currentVideo.value
  if (!v?.bvid) return ''
  return `https://player.bilibili.com/player.html?bvid=${v.bvid}&autoplay=0&high_quality=1&danmaku=0`
})

function onCoverError(e, url) {
  e.target.style.display = 'none'
  if (url) failedCovers.add(url)
}

const seekStyle = computed(() => ({
  background: `linear-gradient(to right, #3b82f6 ${progress.value}%, rgba(0,0,0,0.15) 0)`
}))

const volumeStyle = computed(() => ({
  background: `linear-gradient(to right, #3b82f6 ${(isMuted.value ? 0 : volume.value) * 100}%, rgba(0,0,0,0.15) 0)`
}))

const playModeLabel = computed(() => {
  const modes = { loop: 'music.playModeLoop', single: 'music.playModeSingle', random: 'music.playModeRandom' }
  return t(modes[playMode.value] || '')
})

const filteredPlaylist = computed(() => {
  if (!searchQuery.value.trim()) return playlist.value
  const q = searchQuery.value.toLowerCase()
  return playlist.value.filter(s =>
    (s.name || '').toLowerCase().includes(q) ||
    (s.artist || '').toLowerCase().includes(q)
  )
})

function onSeek(e) {
  seek(Number(e.target.value))
}

function onVolumeInput(e) {
  if (currentSong.value?.source === 'bilibili') {
    flashBilibiliVolTip()
    return
  }
  setVolume(Number(e.target.value))
}

function flashBilibiliVolTip() {
  showBilibiliVolTip.value = true
  clearTimeout(bilibiliVolTipTimer)
  bilibiliVolTipTimer = setTimeout(() => { showBilibiliVolTip.value = false }, 2000)
}

function onToggleMute() {
  if (currentSong.value?.source === 'bilibili') {
    flashBilibiliVolTip()
    return
  }
  toggleMute()
}

function onVolumeBtnClick(e) {
  if (isTouchDevice.value) {
    e.stopPropagation()
    showVolume.value = !showVolume.value
  } else {
    onToggleMute()
  }
}

function onVolumeAreaMouseEnter() {
  if (!isTouchDevice.value) showVolume.value = true
}

function onVolumeAreaMouseLeave() {
  if (!isTouchDevice.value) showVolume.value = false
}

function onDocumentClick() {
  if (isTouchDevice.value && showVolume.value) {
    showVolume.value = false
  }
}

function onLyricClick(line) {
  if (duration.value > 0) {
    seek((line.time / duration.value) * 100)
  }
}

function onVideoClick(i) {
  setVideoIndex(i)
  setVideoActive(true)
  if (isPlaying.value) pause()
}

function moveVideoIframe() {
  nextTick(() => {
    requestAnimationFrame(() => {
      const container = activeTab.value === 'video'
        ? videoTabContainerRef.value
        : videoShowcaseContainerRef.value
      if (!container) return

      if (!iframeEl) {
        iframeEl = document.createElement('iframe')

        iframeEl.frameBorder = '0'
        iframeEl.allowFullscreen = true
        iframeEl.scrolling = 'no'
        iframeEl.style.cssText = 'position:absolute;top:0;left:0;width:100%;height:100%;'
      }
      if (iframeEl.parentElement !== container) {
        if (iframeEl.parentElement) iframeEl.remove()
        container.appendChild(iframeEl)
      }
      if (iframeEl.src !== videoActiveSrc.value) {
        iframeEl.src = videoActiveSrc.value
      }
    })
  })
}

function proxyCoverUrl(url) {
  if (!url) return ''
  return `/api/video/cover?url=${encodeURIComponent(url)}`
}

function formatVideoDuration(sec) {
  if (!sec || isNaN(sec)) return ''
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m}:${s.toString().padStart(2, '0')}`
}

const videoProgress = computed(() =>
  videoDuration.value > 0 ? (videoCurrentTime.value / videoDuration.value) * 100 : 0
)

const videoSeekStyle = computed(() => ({
  background: `linear-gradient(to right, #3b82f6 ${videoProgress.value}%, rgba(0,0,0,0.15) 0)`
}))

function onVideoSeek(e) {
  const percent = Number(e.target.value)
  if (videoDuration.value > 0) {
    videoSeek((percent / 100) * videoDuration.value)
  }
}

// Auto-scroll lyrics
watch(activeLyricIndex, async () => {
  await nextTick()
  if (activeLyricEl && lyricContainer.value) {
    const container = lyricContainer.value
    const el = activeLyricEl
    const top = el.offsetTop - container.offsetHeight / 2 + el.offsetHeight / 2
    container.scrollTo({ top, behavior: 'smooth' })
  }
})

// Video ↔ Music mutual exclusion
watch(activeTab, (tab) => {
  if (tab === 'video') {
    setVideoActive(true)
    if (isPlaying.value) pause()
  }
})

// Music starts → pause video (keep iframe visible in showcase)
watch(isPlaying, (playing) => {
  if (playing) stopAllVideos()
})

// Video starts playing → pause music
watch(videoPlaying, (playing) => {
  if (playing && isPlaying.value) pause()
})

// Single iframe management: src change + positioning
watch(videoActiveSrc, (src) => {
  // Reset progress when video changes
  videoCurrentTime.value = 0
  videoDuration.value = 0
  videoPlaying.value = false
  if (src) moveVideoIframe()
  else {
    iframeEl?.remove()
    iframeEl = null
  }
})

watch(activeTab, () => {
  if (videoActiveSrc.value) moveVideoIframe()
})

let observer = null

function onBilibiliMessage(event) {
  try {
    const originHost = new URL(event.origin).hostname
    if (!originHost.endsWith('.bilibili.com') && originHost !== 'bilibili.com') return
  } catch { return }
  try {
    const msg = typeof event.data === 'string' ? JSON.parse(event.data) : event.data
    if (!msg || typeof msg !== 'object') return
    const type = msg.event || msg.type
    const data = msg.data || msg
    if (type === 'info' && data.currentTime != null) {
      videoCurrentTime.value = data.currentTime
      if (data.duration > 0) videoDuration.value = data.duration
    } else if (type === 'timeupdate' || type === 'playing') {
      if (data.currentTime != null) videoCurrentTime.value = data.currentTime
      if (data.duration > 0) videoDuration.value = data.duration
      videoPlaying.value = true
    } else if (type === 'onStateChange') {
      if (data === 1) videoPlaying.value = true
      else if (data === 2) videoPlaying.value = false
      else if (data === 0) { videoPlaying.value = false; videoCurrentTime.value = videoDuration.value }
    } else if (type === 'play') {
      videoPlaying.value = true
    } else if (type === 'pause' || type === 'paused') {
      videoPlaying.value = false
    } else if (type === 'ended') {
      videoPlaying.value = false
      videoCurrentTime.value = videoDuration.value
    }
  } catch {}
}

onMounted(() => {
  initialize()
  isTouchDevice.value = 'ontouchstart' in window || navigator.maxTouchPoints > 0
  if (!isMobile.value) {
    fetchVideos()
  } else {
    // On mobile, ensure we don't land on the video tab
    if (activeTab.value === 'video') activeTab.value = 'lyrics'
  }
  observer = new MutationObserver(() => {
    isNight.value = document.body.classList.contains('body-night')
  })
  observer.observe(document.body, { attributes: true, attributeFilter: ['class'] })
  window.addEventListener('message', onBilibiliMessage)
  document.addEventListener('click', onDocumentClick)
})

async function fetchVideos() {
  try {
    const data = await request.get('/api/video/list')
    if (data && data.length > 0) videoList.value = data
  } catch { /* ignore */ }
}

onUnmounted(() => {
  observer?.disconnect()
  window.removeEventListener('message', onBilibiliMessage)
  document.removeEventListener('click', onDocumentClick)
  iframeEl?.remove()
  iframeEl = null
  clearTimeout(bilibiliVolTipTimer)
})
</script>

<style scoped>
/* ========== Page Root ========== */
.music-page {
  min-height: 100vh;
  position: relative;
  padding: 80px 0 40px;
  display: flex;
  flex-direction: column;
}

/* ========== Background ========== */
.music-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.music-bg-img {
  position: absolute;
  inset: -10%;
  background-size: cover;
  background-position: center;
  filter: blur(50px) saturate(1.5);
  opacity: 0.4;
  transition: opacity 1s ease;
}

.music-bg-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(8px);
}

.is-night .music-bg-img { opacity: 0.2; }
.is-night .music-bg-overlay { background: rgba(0, 0, 0, 0.4); }

/* ========== Loading & Empty ========== */
.music-loading, .music-empty {
  position: relative;
  z-index: 1;
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.music-loading-disc {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: white;
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: spin 2s linear infinite;
}

.music-loading-text, .music-empty span {
  font-size: 14px;
  color: #64748b;
  font-weight: 900;
  letter-spacing: 0.1em;
}

.music-empty-icon { font-size: 48px; opacity: 0.3; }

@keyframes spin { to { transform: rotate(360deg); } }

/* ========== Content Container ========== */
.music-content {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 80rem; /* max-w-7xl */
  margin: 0 auto;
  padding: 0 16px;
}

/* ========== Header ========== */
.music-header {
  text-align: center;
  margin-bottom: 24px;
}

.music-title {
  font-size: 30px;
  font-weight: 900;
  color: #1e293b;
  letter-spacing: 0.1em;
  margin-bottom: 4px;
  transition: color 0.7s ease;
}

.music-desc {
  font-size: 12px;
  color: #475569;
  font-weight: 500;
  letter-spacing: 0.05em;
  transition: color 0.7s ease;
}

.is-night .music-title { color: #e2e8f0; }
.is-night .music-desc { color: #64748b; }

/* ========== Grid ========== */
.music-grid {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
}

/* ========== Disc Panel (Left) ========== */
.music-disc-panel {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 32px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  min-height: 460px;
  position: relative;
  overflow: hidden;
  transition: all 0.5s ease;
  flex-shrink: 0;
}

.is-night .music-disc-panel {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
}

/* ========== Disc Info Wrapper ========== */
.disc-info-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 0;
  width: 100%;
}

/* ========== Transport Wrapper ========== */
.transport-wrapper {
  width: 100%;
  margin-top: auto;
  flex-shrink: 0;
}

/* ========== Disc Area ========== */
.disc-area {
  position: relative;
  width: 160px;
  height: 160px;
  margin-bottom: 24px;
  flex-shrink: 0;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.disc-glow {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 85%;
  height: 85%;
  background: rgba(59, 130, 246, 0.25);
  border-radius: 50%;
  filter: blur(35px);
  opacity: 0.2;
  transition: all 1s ease;
  z-index: 0;
}

.disc-glow.active {
  opacity: 0.9;
  transform: scale(1.05);
}

.disc-shadow {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 90%;
  height: 90%;
  border-radius: 50%;
  box-shadow: 0 0 40px -5px rgba(59, 130, 246, 0.4);
  z-index: 0;
}

.disc-rotating {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  transition: transform 0.7s ease;
  z-index: 10;
}

.disc-rotating.spinning {
  animation: spin 20s linear infinite;
}

.disc-rotating:not(.spinning) {
  transform: scale(0.95);
}

.disc-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.disc-cover-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 40px;
}

.disc-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border-radius: 50%;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(203, 213, 225, 1);
  z-index: 30;
}

.is-night .disc-center {
  background: rgba(30, 41, 59, 0.9);
  border-color: rgba(51, 65, 85, 1);
}

.disc-shine {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  pointer-events: none;
  opacity: 0.2;
  background: conic-gradient(from 0deg, transparent, rgba(255,255,255,0.4), transparent, rgba(255,255,255,0.4), transparent);
  z-index: 20;
}

.is-night .disc-rotating { border-color: rgba(100, 116, 139, 0.8); }

/* ========== Song Info ========== */
.song-info {
  text-align: center;
  margin-bottom: 8px;
  width: 100%;
  padding: 0 8px;
}

.song-name {
  font-size: 18px;
  font-weight: 900;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  letter-spacing: -0.025em;
  filter: drop-shadow(0 1px 1px rgb(0 0 0 / 0.05));
  transition: color 0.7s ease;
}

.song-artist {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  font-weight: 700;
  letter-spacing: 0.1em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.7s ease;
}

.is-night .song-name { color: #e2e8f0; }
.is-night .song-artist { color: #64748b; }

/* ========== Seek Bar ========== */
.seek-bar {
  width: 100%;
  margin-bottom: 24px;
  padding: 0 4px;
}

.seek-input, .volume-input {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 4px;
  border-radius: 999px;
  outline: none;
  cursor: pointer;
}

.seek-input::-webkit-slider-thumb,
.volume-input::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
}

.seek-time {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  font-weight: 700;
  color: #64748b;
  margin-top: 6px;
  font-variant-numeric: tabular-nums;
}

.is-night .seek-time { color: #64748b; }

.bilibili-loading {
  color: #3b82f6;
  animation: bilibiliPulse 1s ease-in-out infinite;
}

@keyframes bilibiliPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* Bilibili link */
.bilibili-link {
  font-size: 11px;
  color: #3b82f6;
  text-decoration: none;
  font-weight: 700;
  transition: color 0.2s;
}

.bilibili-link:hover {
  color: #2563eb;
  text-decoration: underline;
}

/* ========== Transport Controls ========== */
.transport {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-top: auto;
  padding: 0 4px;
}

.ctrl-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: none;
  cursor: pointer;
  color: #334155;
  transition: transform 0.15s ease;
  padding: 8px;
}

.ctrl-btn:hover {
  color: #3b82f6;
  transform: scale(1.1);
}

.is-night .ctrl-btn { color: #cbd5e1; }
.is-night .ctrl-btn:hover { color: #60a5fa; }

.ctrl-btn svg {
  width: 24px;
  height: 24px;
}

.play-btn {
  width: 56px;
  height: 56px;
  background: #3b82f6;
  border-radius: 50%;
  color: white;
  box-shadow: 0 20px 25px -5px rgba(59, 130, 246, 0.4);
  transition: transform 0.15s ease;
  padding: 0;
}

.play-btn:hover {
  background: #3b82f6;
  color: white;
  transform: scale(1.05);
}

.play-btn svg {
  width: 28px;
  height: 28px;
  margin-left: 2px;
}

.mode-btn svg {
  width: 18px;
  height: 18px;
}

/* ========== Volume ========== */
.volume-area {
  position: relative;
  display: flex;
  align-items: center;
}

.volume-slider {
  display: flex;
  overflow: hidden;
  align-items: center;
  margin-right: 8px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 999px;
  padding: 6px 12px;
  width: 80px;
}

.is-night .volume-slider {
  background: rgba(0, 0, 0, 0.2);
  border-color: rgba(255, 255, 255, 0.08);
}

.volume-btn {
  border-radius: 50%;
}

.volume-btn.active {
  background: #3b82f6;
  color: white;
  box-shadow: 0 10px 15px -3px rgba(59, 130, 246, 0.4);
}

.vol-enter-active, .vol-leave-active {
  transition: width 0.2s ease, opacity 0.2s ease;
}
.vol-enter-from, .vol-leave-to {
  width: 0;
  opacity: 0;
}

.bilibili-vol-tip {
  position: absolute;
  bottom: calc(100% + 8px);
  right: 0;
  white-space: nowrap;
  font-size: 12px;
  padding: 6px 12px;
  background: rgba(30, 41, 59, 0.9);
  color: #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  pointer-events: none;
  z-index: 10;
}

.bilibili-vol-tip::after {
  content: '';
  position: absolute;
  top: 100%;
  right: 12px;
  border: 5px solid transparent;
  border-top-color: rgba(30, 41, 59, 0.9);
}

.tip-enter-active, .tip-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.tip-enter-from, .tip-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

/* ========== Right Panel ========== */
.music-right-panel {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 32px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 450px;
  position: relative;
  transition: color 0.7s ease;
  flex-shrink: 0;
}

.is-night .music-right-panel {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
}

/* ========== Tab Bar ========== */
.tab-bar-wrapper {
  display: flex;
  justify-content: center;
  padding: 16px 16px 0;
  flex-shrink: 0;
}

.tab-bar {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  box-shadow: inset 0 2px 4px 0 rgb(0 0 0 / 0.05);
  border: 1px solid rgba(255, 255, 255, 0.4);
  width: 192px;
}

.is-night .tab-bar {
  background: rgba(15, 23, 42, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
}

.tab-btn {
  flex: 1;
  padding: 6px 0;
  border: none;
  background: none;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn.active {
  background: #3b82f6;
  color: white;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.tab-btn:hover:not(.active) {
  color: #3b82f6;
}

/* ========== Lyrics Panel ========== */
.lyrics-panel {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.lyrics-fade-top, .lyrics-fade-bottom {
  position: absolute;
  left: 0;
  right: 0;
  height: 128px;
  pointer-events: none;
  z-index: 10;
}

.lyrics-fade-top {
  top: 0;
  background: linear-gradient(to bottom, rgba(255,255,255,0.4), transparent);
}

.lyrics-fade-bottom {
  bottom: 0;
  background: linear-gradient(to top, rgba(255,255,255,0.4), transparent);
}

.is-night .lyrics-fade-top { background: linear-gradient(to bottom, rgba(30,41,59,0.6), transparent); }
.is-night .lyrics-fade-bottom { background: linear-gradient(to top, rgba(30,41,59,0.6), transparent); }

.lyrics-scroll {
  height: 100%;
  overflow-y: auto;
  scroll-behavior: smooth;
  position: relative;
  padding: 0 16px;
  -webkit-mask-image: linear-gradient(to bottom, transparent 0%, black 15%, black 85%, transparent 100%);
  mask-image: linear-gradient(to bottom, transparent 0%, black 15%, black 85%, transparent 100%);
}

.lyrics-scroll::-webkit-scrollbar { display: none; }
.lyrics-scroll { -ms-overflow-style: none; scrollbar-width: none; }

.lyrics-padding {
  padding: 30vh 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  text-align: center;
}

.lyric-line {
  padding: 6px 8px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.7s ease;
  opacity: 0.2;
}

.lyric-line:hover { opacity: 0.4; }

.lyric-line.active {
  opacity: 1;
  transform: scale(1.05);
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.1);
}

.lyric-line p {
  font-size: 14px;
  font-weight: 900;
  color: #334155;
  transition: all 0.7s ease;
  line-height: 1.625;
  letter-spacing: -0.025em;
}

.lyric-line.active p {
  font-size: 18px;
  color: #2563eb;
  text-shadow: 0 0 20px rgba(59, 130, 246, 0.15);
}

.is-night .lyric-line p { color: #cbd5e1; }
.is-night .lyric-line.active p { color: #60a5fa; }

.lyrics-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
}

.lyrics-empty-icon {
  font-size: 32px;
  opacity: 0.4;
  color: #3b82f6;
  animation: spin 3s linear infinite;
}

.lyrics-empty p {
  font-size: 16px;
  color: #3b82f6;
  font-weight: 900;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ========== Playlist Panel ========== */
.playlist-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 8px 16px 16px;
  position: relative;
}

.playlist-search {
  position: relative;
  margin-bottom: 16px;
  flex-shrink: 0;
  max-width: 448px;
  width: 100%;
  margin-left: auto;
  margin-right: auto;
}

.playlist-search::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(59, 130, 246, 0.05);
  border-radius: 999px;
  filter: blur(20px);
  opacity: 0;
  transition: all 0.3s ease;
}

.playlist-search:focus-within::before {
  opacity: 1;
  background: rgba(59, 130, 246, 0.1);
}

.search-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  color: #94a3b8;
  pointer-events: none;
  transition: color 0.2s ease;
  z-index: 1;
}

.playlist-search:focus-within .search-icon { color: #3b82f6; }

.search-input {
  width: 100%;
  height: 40px;
  padding: 0 40px 0 44px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(12px);
  font-size: 12px;
  font-weight: 500;
  outline: none;
  transition: all 0.2s ease;
  position: relative;
  z-index: 1;
}

.search-input:focus {
  border-color: rgba(59, 130, 246, 0.4);
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
}

.is-night .search-input {
  background: rgba(15, 23, 42, 0.6);
  border-color: rgba(255, 255, 255, 0.08);
  color: #e2e8f0;
}

.search-clear {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  cursor: pointer;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  z-index: 1;
  transition: background 0.15s ease;
}

.search-clear:hover { background: rgba(0, 0, 0, 0.1); }
.search-clear svg { width: 14px; height: 14px; }

.playlist-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 8px;
}

.playlist-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.playlist-item:hover {
  background: rgba(255, 255, 255, 0.3);
}

.is-night .playlist-item:hover {
  background: rgba(30, 41, 59, 0.4);
}

.playlist-item.active {
  background: rgba(255, 255, 255, 0.6);
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
}

.is-night .playlist-item.active {
  background: rgba(30, 41, 59, 0.8);
}

.playlist-cover {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
  box-shadow: 0 1px 2px 0 rgb(0 0 0 / 0.05);
}

.playlist-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.playlist-cover-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 16px;
}

.playlist-playing {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(1px);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
}

.playlist-playing span {
  width: 2px;
  background: white;
  border-radius: 999px;
  animation: barBounce 1s ease-in-out infinite;
}

.playlist-playing span:nth-child(1) { height: 8px; animation-delay: 0ms; }
.playlist-playing span:nth-child(2) { height: 12px; animation-delay: 200ms; }
.playlist-playing span:nth-child(3) { height: 6px; animation-delay: 400ms; }

@keyframes barBounce {
  0%, 100% { transform: scaleY(0.4); }
  50% { transform: scaleY(1); }
}

.playlist-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
  flex: 1;
}

.playlist-song-name {
  font-size: 13px;
  font-weight: 900;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s ease;
}

.playlist-item.active .playlist-song-name { color: #2563eb; }

.playlist-song-artist {
  font-size: 10px;
  font-weight: 500;
  color: #64748b;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.is-night .playlist-song-name { color: #e2e8f0; }
.is-night .playlist-item.active .playlist-song-name { color: #60a5fa; }
.is-night .playlist-song-artist { color: #64748b; }

/* Playlist transition */
.playlist-item-enter-active { transition: all 0.3s ease; }
.playlist-item-leave-active { transition: all 0.2s ease; }
.playlist-item-enter-from { opacity: 0; transform: translateY(8px); }
.playlist-item-leave-to { opacity: 0; transform: scale(0.95); }

/* ========== Video Tab ========== */
.video-tab-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.video-tab-player {
  flex-shrink: 0;
  background: #000;
  border-radius: 8px;
  overflow: hidden;
  margin: 8px 16px 0;
}

.video-tab-player-wrap {
  position: relative;
  width: 100%;
  padding-top: 56.25%;
}

.video-tab-iframe {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.video-tab-info {
  display: flex;
  flex-direction: column;
  padding: 10px 16px 0;
}

.video-tab-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.is-night .video-tab-title { color: #e2e8f0; }

.video-tab-author {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.video-tab-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 8px 16px 12px;
}

.video-tab-nav-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(59, 130, 246, 0.08);
  cursor: pointer;
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.15s ease;
}

.video-tab-nav-btn:hover {
  background: rgba(59, 130, 246, 0.15);
  transform: scale(1.05);
}

.is-night .video-tab-nav-btn {
  background: rgba(96, 165, 250, 0.1);
  color: #60a5fa;
}

.video-tab-nav-btn svg { width: 16px; height: 16px; }

.video-tab-counter {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* ========== Video Seek Bar ========== */
.video-seek-bar {
  width: 100%;
  padding: 0 4px;
  margin: 8px 0;
}

.video-seek-input {
  width: 100%;
  height: 4px;
  border-radius: 2px;
  appearance: none;
  -webkit-appearance: none;
  outline: none;
  cursor: pointer;
  border: none;
}

.video-seek-input::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  box-shadow: 0 0 4px rgba(59, 130, 246, 0.4);
}

.video-seek-input::-moz-range-thumb {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  border: none;
  box-shadow: 0 0 4px rgba(59, 130, 246, 0.4);
}

.video-seek-time {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
  font-variant-numeric: tabular-nums;
}

.is-night .video-seek-time { color: #64748b; }

/* ========== Video Showcase ========== */
.video-showcase {
  margin-top: 32px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 32px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  padding: 24px;
  overflow: hidden;
  animation: fade-in-up 0.8s ease-out 0.2s both;
}

.is-night .video-showcase {
  background: rgba(30, 41, 59, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
}

.video-showcase-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.video-showcase-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(59, 130, 246, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.video-showcase-icon svg {
  width: 20px;
  height: 20px;
  color: #3b82f6;
}

.video-showcase-title {
  font-size: 20px;
  font-weight: 900;
  color: #1e293b;
  letter-spacing: -0.01em;
  transition: color 0.7s ease;
}

.video-showcase-desc {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  margin-top: 2px;
  transition: color 0.7s ease;
}

.is-night .video-showcase-title { color: #e2e8f0; }
.is-night .video-showcase-desc { color: #64748b; }

.video-showcase-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Player */
.video-showcase-player {
  flex-shrink: 0;
}

.video-showcase-player-wrap {
  position: relative;
  width: 100%;
  padding-top: 56.25%;
  border-radius: 16px;
  overflow: hidden;
  background: #000;
}

.video-showcase-iframe {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.video-showcase-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e293b, #334155);
}

.video-showcase-placeholder-icon {
  width: 48px;
  height: 48px;
  color: rgba(255, 255, 255, 0.3);
}

.video-showcase-info {
  display: flex;
  flex-direction: column;
  padding: 12px 4px 0;
}

.video-showcase-video-title {
  font-size: 16px;
  font-weight: 900;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.7s ease;
}

.video-showcase-video-author {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  margin-top: 4px;
}

.is-night .video-showcase-video-title { color: #e2e8f0; }
.is-night .video-showcase-video-author { color: #64748b; }

/* List */
.video-showcase-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
  padding-right: 4px;
}

.video-showcase-list::-webkit-scrollbar { width: 4px; }
.video-showcase-list::-webkit-scrollbar-track { background: transparent; }
.video-showcase-list::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 4px; }
.is-night .video-showcase-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); }

.video-showcase-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}

.video-showcase-item:hover {
  background: rgba(255, 255, 255, 0.3);
}

.is-night .video-showcase-item:hover {
  background: rgba(30, 41, 59, 0.4);
}

.video-showcase-item.active {
  background: rgba(255, 255, 255, 0.6);
  border-color: rgba(59, 130, 246, 0.3);
}

.is-night .video-showcase-item.active {
  background: rgba(30, 41, 59, 0.8);
}

.video-showcase-thumb {
  width: 96px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  flex-shrink: 0;
  background: #e2e8f0;
}

.video-showcase-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-showcase-thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  color: white;
  font-size: 16px;
}

.video-showcase-playing {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(1px);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.video-showcase-playing span {
  width: 2px;
  background: white;
  border-radius: 999px;
  animation: barBounce 1s ease-in-out infinite;
}

.video-showcase-playing span:nth-child(1) { height: 8px; animation-delay: 0ms; }
.video-showcase-playing span:nth-child(2) { height: 12px; animation-delay: 200ms; }
.video-showcase-playing span:nth-child(3) { height: 6px; animation-delay: 400ms; }

.video-showcase-duration {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 4px;
  font-variant-numeric: tabular-nums;
}

.video-showcase-item-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
  flex: 1;
}

.video-showcase-item-title {
  font-size: 13px;
  font-weight: 900;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.2s ease;
}

.video-showcase-item.active .video-showcase-item-title {
  color: #2563eb;
}

.video-showcase-item-author {
  font-size: 10px;
  font-weight: 500;
  color: #64748b;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.is-night .video-showcase-item-title { color: #e2e8f0; }
.is-night .video-showcase-item.active .video-showcase-item-title { color: #60a5fa; }
.is-night .video-showcase-item-author { color: #64748b; }

/* ========== Entrance Animation ========== */
@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.music-header {
  animation: fade-in-up 0.8s ease-out both;
}

/* ========== Tab Cross-fade ========== */
.tab-fade-in {
  animation: tab-fade-in 0.3s ease both;
}

@keyframes tab-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ========== Responsive: sm (640px) ========== */
@media (min-width: 640px) {
  .music-content { padding: 0 24px; }

  .disc-area {
    width: 192px;
    height: 192px;
  }

  .music-disc-panel {
    min-height: 500px;
  }
}

/* ========== Responsive: md (768px) ========== */
@media (min-width: 768px) {
  .music-page { padding: 112px 0 40px; }

  .music-content { padding: 0 40px; }

  .music-header { text-align: left; margin-bottom: 40px; }
  .music-title { font-size: 48px; margin-bottom: 8px; }
  .music-desc { font-size: 16px; }

  .music-grid {
    display: grid;
    grid-template-columns: repeat(12, 1fr);
    gap: 32px;
    align-items: stretch;
    height: calc(100vh - 320px);
    min-height: 600px;
    max-height: 720px;
  }

  .music-disc-panel {
    grid-column: span 5 / span 5;
    min-height: 0;
    padding: 40px;
  }

  .music-right-panel {
    grid-column: span 7 / span 7;
    height: auto;
    min-height: 0;
  }

  .disc-area {
    width: 240px;
    height: 240px;
    margin-bottom: 40px;
  }

  .disc-rotating { border-width: 6px; }

  .disc-center {
    width: 48px;
    height: 48px;
  }

  .song-info { padding: 0 16px; margin-bottom: 24px; }
  .song-name { font-size: 20px; }
  .song-artist { font-size: 14px; margin-top: 8px; }

  .seek-bar { margin-bottom: 32px; padding: 0 12px; }
  .seek-input, .volume-input { height: 6px; }
  .seek-time { font-size: 12px; }

  .transport { padding: 0 8px; }
  .ctrl-btn svg { width: 28px; height: 28px; }
  .mode-btn svg { width: 20px; height: 20px; }

  .play-btn {
    width: 64px;
    height: 64px;
  }

  .play-btn svg { width: 32px; height: 32px; }

  .tab-bar-wrapper { padding: 24px 24px 0; }
  .tab-bar { width: 256px; }
  .tab-btn { font-size: 13px; padding: 8px 0; }

  .lyrics-fade-top, .lyrics-fade-bottom { height: 160px; }
  .lyrics-scroll { padding: 0 24px; }
  .lyrics-padding { padding: 35vh 0; gap: 24px; }
  .lyric-line { padding: 6px 16px; }
  .lyric-line.active { padding: 12px 16px; }
  .lyric-line p { font-size: 16px; }
  .lyric-line.active p { font-size: 24px; }

  .playlist-panel { padding: 16px 32px 32px; }
  .playlist-search { margin-bottom: 32px; }
  .search-input { height: 48px; font-size: 14px; }
  .playlist-list { gap: 10px; }
  .playlist-item { padding: 16px; border-radius: 16px; }
  .playlist-cover { width: 48px; height: 48px; border-radius: 12px; }
  .playlist-song-name { font-size: 15px; }
  .playlist-song-artist { font-size: 11px; }

  .video-showcase { padding: 32px; margin-top: 40px; }
  .video-showcase-header { margin-bottom: 24px; }
  .video-showcase-title { font-size: 24px; }

  .video-showcase-grid {
    flex-direction: row;
    gap: 24px;
  }

  .video-showcase-player {
    flex: 7;
    min-width: 0;
  }

  .video-showcase-list {
    flex: 5;
    max-height: 520px;
  }
}

/* ========== Responsive: lg (1024px) ========== */
@media (min-width: 1024px) {
  .disc-area {
    width: 256px;
    height: 256px;
  }

  .play-btn {
    width: 80px;
    height: 80px;
  }

  .play-btn svg { width: 32px; height: 32px; }

  .song-name { font-size: 24px; }

  .transport { padding: 0 16px; }

  .lyrics-scroll { padding: 0 40px; }
}

/* ========== Scrollbar ========== */
.playlist-list::-webkit-scrollbar {
  width: 4px;
}

.playlist-list::-webkit-scrollbar-track {
  background: transparent;
}

.playlist-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.is-night .playlist-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
}
</style>
