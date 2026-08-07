/**
 * AI 历史记录和收藏系统
 * 保存 AI 交互历史，支持收藏和搜索
 */

const HISTORY_KEY = 'ai-history'
const FAVORITES_KEY = 'ai-favorites'
const MAX_HISTORY = 100

/**
 * 获取历史记录
 * @returns {Array}
 */
export function getHistory() {
  try {
    const stored = localStorage.getItem(HISTORY_KEY)
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

/**
 * 保存历史记录
 * @param {Array} history
 */
function saveHistory(history) {
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(history))
  } catch (e) {
    console.error('Failed to save history:', e)
  }
}

/**
 * 添加历史记录
 * @param {Object} entry
 * @returns {Object} saved entry with id
 */
export function addHistory(entry) {
  const history = getHistory()
  const newEntry = {
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 7),
    createdAt: new Date().toISOString(),
    type: entry.type || 'chat',
    preset: entry.preset || '',
    prompt: entry.prompt || '',
    response: entry.response || '',
    context: entry.context || '',
    contentType: entry.contentType || 'article',
    tokenCount: entry.tokenCount || 0,
  }

  history.unshift(newEntry)

  // 限制历史记录数量
  if (history.length > MAX_HISTORY) {
    history.splice(MAX_HISTORY)
  }

  saveHistory(history)
  return newEntry
}

/**
 * 删除历史记录
 * @param {string} id
 */
export function deleteHistory(id) {
  const history = getHistory()
  const filtered = history.filter(h => h.id !== id)
  saveHistory(filtered)
}

/**
 * 清空历史记录
 */
export function clearHistory() {
  saveHistory([])
}

/**
 * 搜索历史记录
 * @param {string} keyword
 * @returns {Array}
 */
export function searchHistory(keyword) {
  if (!keyword || typeof keyword !== 'string') return getHistory()
  const lower = keyword.toLowerCase()
  return getHistory().filter(h =>
    h.prompt.toLowerCase().includes(lower) ||
    h.response.toLowerCase().includes(lower) ||
    h.preset.toLowerCase().includes(lower)
  )
}

/**
 * 获取收藏列表
 * @returns {Array}
 */
export function getFavorites() {
  try {
    const stored = localStorage.getItem(FAVORITES_KEY)
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

/**
 * 保存收藏列表
 * @param {Array} favorites
 */
function saveFavorites(favorites) {
  try {
    localStorage.setItem(FAVORITES_KEY, JSON.stringify(favorites))
  } catch (e) {
    console.error('Failed to save favorites:', e)
  }
}

/**
 * 添加收藏
 * @param {Object} entry
 * @returns {Object}
 */
export function addFavorite(entry) {
  const favorites = getFavorites()

  // 检查是否已收藏
  const exists = favorites.some(f => f.historyId === entry.id)
  if (exists) return null

  const favorite = {
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 7),
    historyId: entry.id,
    createdAt: new Date().toISOString(),
    type: entry.type || 'chat',
    preset: entry.preset || '',
    prompt: entry.prompt || '',
    response: entry.response || '',
    contentType: entry.contentType || 'article',
    note: '',
  }

  favorites.unshift(favorite)
  saveFavorites(favorites)
  return favorite
}

/**
 * 删除收藏
 * @param {string} id
 */
export function deleteFavorite(id) {
  const favorites = getFavorites()
  const filtered = favorites.filter(f => f.id !== id)
  saveFavorites(filtered)
}

/**
 * 更新收藏备注
 * @param {string} id
 * @param {string} note
 */
export function updateFavoriteNote(id, note) {
  const favorites = getFavorites()
  const fav = favorites.find(f => f.id === id)
  if (fav) {
    fav.note = note
    saveFavorites(favorites)
  }
}

/**
 * 检查是否已收藏
 * @param {string} historyId
 * @returns {boolean}
 */
export function isFavorited(historyId) {
  return getFavorites().some(f => f.historyId === historyId)
}

/**
 * 导出历史记录
 * @returns {string}
 */
export function exportHistory() {
  return JSON.stringify({
    history: getHistory(),
    favorites: getFavorites(),
    exportedAt: new Date().toISOString(),
  }, null, 2)
}

/**
 * 导入历史记录
 * @param {string} jsonString
 * @returns {{ success: boolean, error?: string }}
 */
export function importHistory(jsonString) {
  try {
    const imported = JSON.parse(jsonString)
    if (typeof imported !== 'object') {
      return { success: false, error: 'Invalid format' }
    }

    if (imported.history) {
      const existing = getHistory()
      const merged = [...imported.history, ...existing]
      const unique = merged.filter((item, index, self) =>
        index === self.findIndex(t => t.id === item.id)
      )
      saveHistory(unique.slice(0, MAX_HISTORY))
    }

    if (imported.favorites) {
      const existing = getFavorites()
      const merged = [...imported.favorites, ...existing]
      const unique = merged.filter((item, index, self) =>
        index === self.findIndex(t => t.id === item.id)
      )
      saveFavorites(unique)
    }

    return { success: true }
  } catch (e) {
    return { success: false, error: e.message }
  }
}

export default {
  getHistory,
  addHistory,
  deleteHistory,
  clearHistory,
  searchHistory,
  getFavorites,
  addFavorite,
  deleteFavorite,
  updateFavoriteNote,
  isFavorited,
  exportHistory,
  importHistory,
}
