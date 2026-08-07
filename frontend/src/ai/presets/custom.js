/**
 * 自定义预设系统
 * 用户可以创建、编辑、删除自定义预设
 */

const STORAGE_KEY = 'ai-custom-presets'

/**
 * 获取所有自定义预设
 * @returns {Array}
 */
export function getCustomPresets() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

/**
 * 保存自定义预设列表
 * @param {Array} presets
 */
function savePresets(presets) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(presets))
  } catch (e) {
    console.error('Failed to save presets:', e)
  }
}

/**
 * 添加自定义预设
 * @param {Object} preset
 * @returns {Object} 创建的预设
 */
export function addCustomPreset(preset) {
  const presets = getCustomPresets()
  const newPreset = {
    id: `custom-${Date.now()}`,
    icon: preset.icon || ' ',
    name: preset.name,
    prompt: preset.prompt,
    needsTitle: preset.needsTitle || false,
    needsContent: preset.needsContent || false,
    localOp: preset.localOp || false,
    category: preset.category || 'custom',
    isCustom: true,
    createdAt: new Date().toISOString(),
  }

  presets.push(newPreset)
  savePresets(presets)
  return newPreset
}

/**
 * 更新自定义预设
 * @param {string} id
 * @param {Object} updates
 * @returns {Object|null}
 */
export function updateCustomPreset(id, updates) {
  const presets = getCustomPresets()
  const index = presets.findIndex((p) => p.id === id)
  if (index === -1) return null

  presets[index] = { ...presets[index], ...updates, updatedAt: new Date().toISOString() }
  savePresets(presets)
  return presets[index]
}

/**
 * 删除自定义预设
 * @param {string} id
 * @returns {boolean}
 */
export function deleteCustomPreset(id) {
  const presets = getCustomPresets()
  const filtered = presets.filter((p) => p.id !== id)
  if (filtered.length === presets.length) return false

  savePresets(filtered)
  return true
}

/**
 * 获取单个自定义预设
 * @param {string} id
 * @returns {Object|null}
 */
export function getCustomPresetById(id) {
  const presets = getCustomPresets()
  return presets.find((p) => p.id === id) || null
}

/**
 * 导出所有自定义预设
 * @returns {string} JSON 字符串
 */
export function exportCustomPresets() {
  const presets = getCustomPresets()
  return JSON.stringify(presets, null, 2)
}

/**
 * 导入自定义预设
 * @param {string} jsonString
 * @returns {{ success: boolean, count: number, error?: string }}
 */
export function importCustomPresets(jsonString) {
  try {
    const imported = JSON.parse(jsonString)
    if (!Array.isArray(imported)) {
      return { success: false, count: 0, error: 'Invalid format: expected array' }
    }

    const existing = getCustomPresets()
    const existingIds = new Set(existing.map((p) => p.id))

    // 合并，跳过已存在的
    let added = 0
    for (const preset of imported) {
      if (!existingIds.has(preset.id) && preset.name && preset.prompt) {
        existing.push({ ...preset, isCustom: true })
        added++
      }
    }

    savePresets(existing)
    return { success: true, count: added }
  } catch (e) {
    return { success: false, count: 0, error: e.message }
  }
}

/**
 * 预设图标选项
 */
export const presetIconOptions = [
  ' ', ' ', ' ', ' ', ' ', ' ',
  ' ', ' ', ' ', ' ', ' ', ' ',
  ' ', ' ', ' ', ' ', '#', ' ',
]

/**
 * 预设分类选项
 */
export const presetCategoryOptions = [
  { value: 'writing', label: '写作' },
  { value: 'translate', label: '翻译' },
  { value: 'analysis', label: '分析' },
  { value: 'enhance', label: '增强' },
  { value: 'tone', label: '语气' },
  { value: 'custom', label: '自定义' },
]

export default {
  getCustomPresets,
  addCustomPreset,
  updateCustomPreset,
  deleteCustomPreset,
  getCustomPresetById,
  exportCustomPresets,
  importCustomPresets,
  presetIconOptions,
  presetCategoryOptions,
}
