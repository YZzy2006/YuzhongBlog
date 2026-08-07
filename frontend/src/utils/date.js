const i18n = {
  'en-US': {
    justNow: 'just now',
    minutesAgo: (n) => `${n}m ago`,
    hoursAgo: (n) => `${n}h ago`,
    daysAgo: (n) => `${n}d ago`,
    weeksAgo: (n) => `${n}w ago`,
  },
  'zh-CN': {
    justNow: '刚刚',
    minutesAgo: (n) => `${n}分钟前`,
    hoursAgo: (n) => `${n}小时前`,
    daysAgo: (n) => `${n}天前`,
    weeksAgo: (n) => `${n}周前`,
  }
}

/**
 * Format a date string as a relative time (e.g., "3天前" / "3d ago").
 * Falls back to absolute date for older dates.
 */
export function relativeDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  const lang = localStorage.getItem('locale') || 'zh-CN'
  const t = i18n[lang] || i18n['zh-CN']

  if (seconds < 60) return t.justNow
  if (minutes < 60) return t.minutesAgo(minutes)
  if (hours < 24) return t.hoursAgo(hours)
  if (days < 7) return t.daysAgo(days)
  if (days < 30) return t.weeksAgo(Math.floor(days / 7))
  // Fall back to absolute date
  return date.toLocaleDateString(lang === 'en-US' ? 'en-US' : 'zh-CN')
}

/**
 * Estimate reading time in minutes based on content length.
 * Assumes ~400 Chinese characters per minute.
 */
export function readingTime(text) {
  if (!text) return 1
  const stripped = text.replace(/<[^>]+>/g, '').replace(/[#*`>\-\[\]()!]/g, '')
  const chars = stripped.length
  return Math.max(1, Math.round(chars / 400))
}
