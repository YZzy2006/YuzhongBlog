import { ref, computed } from 'vue'

// ─── Module-level singleton state (shared across all components) ───
const videoList = ref([])
const videoIndex = ref(0)
const videoActive = ref(false) // true when video is the active media (music paused for video)

// Video playback progress (updated via postMessage from bilibili iframe)
const videoCurrentTime = ref(0)
const videoDuration = ref(0)
const videoPlaying = ref(false)

// ─── Public composable ───

export function useVideoPlayer() {
  const currentVideo = computed(() => videoList.value[videoIndex.value] || null)

  const currentVideoSrc = computed(() => {
    if (!videoActive.value) return ''
    const v = currentVideo.value
    if (!v?.bvid) return ''
    return `https://player.bilibili.com/player.html?bvid=${v.bvid}&autoplay=0&high_quality=1&danmaku=0`
  })

  function setVideoIndex(i) {
    if (i >= 0 && i < videoList.value.length) {
      videoIndex.value = i
    }
  }

  function nextVideo() {
    if (videoList.value.length > 0) {
      videoIndex.value = (videoIndex.value + 1) % videoList.value.length
    }
  }

  function prevVideo() {
    if (videoList.value.length > 0) {
      videoIndex.value = (videoIndex.value - 1 + videoList.value.length) % videoList.value.length
    }
  }

  function setVideoActive(active) {
    videoActive.value = active
  }

  // Pause video playback (called when music starts) — keeps iframe visible
  function stopAllVideos() {
    const iframe = document.querySelector('iframe[src*="bilibili.com"]')
    if (iframe) {
      try {
        iframe.contentWindow.postMessage(JSON.stringify({ event: 'command', func: 'pauseVideo', args: '' }), '*')
      } catch {}
    }
    videoPlaying.value = false
  }

  // Seek video via postMessage to bilibili iframe
  function videoSeek(seconds) {
    const iframe = document.querySelector('iframe[src*="bilibili.com"]')
    if (!iframe) return
    try {
      iframe.contentWindow.postMessage(JSON.stringify({ event: 'command', func: 'seekTo', args: [seconds, true] }), '*')
      videoCurrentTime.value = seconds
    } catch {}
  }

  return {
    videoList,
    videoIndex,
    videoActive,
    currentVideo,
    currentVideoSrc,
    videoCurrentTime,
    videoDuration,
    videoPlaying,
    setVideoIndex,
    nextVideo,
    prevVideo,
    setVideoActive,
    stopAllVideos,
    videoSeek
  }
}
