/**
 * AI 提示词模板系统
 * 支持带占位符的模板，用户填写占位符后生成完整提示词
 */

const STORAGE_KEY = 'ai-prompt-templates'

/**
 * 获取所有模板
 * @returns {Array}
 */
export function getTemplates() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

/**
 * 保存模板列表
 * @param {Array} templates
 */
function saveTemplates(templates) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(templates))
  } catch (e) {
    console.error('Failed to save templates:', e)
  }
}

/**
 * 添加模板
 * @param {Object} template
 * @returns {Object}
 */
export function addTemplate(template) {
  const templates = getTemplates()
  const newTemplate = {
    id: `tpl-${Date.now()}`,
    name: template.name,
    icon: template.icon || ' ',
    prompt: template.prompt,
    category: template.category || 'custom',
    placeholders: extractPlaceholders(template.prompt),
    createdAt: new Date().toISOString(),
  }

  templates.push(newTemplate)
  saveTemplates(templates)
  return newTemplate
}

/**
 * 更新模板
 * @param {string} id
 * @param {Object} updates
 * @returns {Object|null}
 */
export function updateTemplate(id, updates) {
  const templates = getTemplates()
  const index = templates.findIndex(t => t.id === id)
  if (index === -1) return null

  if (updates.prompt) {
    updates.placeholders = extractPlaceholders(updates.prompt)
  }

  templates[index] = { ...templates[index], ...updates, updatedAt: new Date().toISOString() }
  saveTemplates(templates)
  return templates[index]
}

/**
 * 删除模板
 * @param {string} id
 * @returns {boolean}
 */
export function deleteTemplate(id) {
  const templates = getTemplates()
  const filtered = templates.filter(t => t.id !== id)
  if (filtered.length === templates.length) return false

  saveTemplates(filtered)
  return true
}

/**
 * 获取单个模板
 * @param {string} id
 * @returns {Object|null}
 */
export function getTemplateById(id) {
  const templates = getTemplates()
  return templates.find(t => t.id === id) || null
}

/**
 * 从提示词中提取占位符
 * 占位符格式: {name} 或 {name:default}
 * @param {string} prompt
 * @returns {Array<{name: string, default: string}>}
 */
export function extractPlaceholders(prompt) {
  if (!prompt) return []
  const regex = /\{([^}:]+)(?::([^}]*))?\}/g
  const placeholders = []
  const seen = new Set()
  let match

  while ((match = regex.exec(prompt)) !== null) {
    const name = match[1].trim()
    if (!seen.has(name)) {
      seen.add(name)
      placeholders.push({
        name,
        default: match[2]?.trim() || '',
      })
    }
  }

  return placeholders
}

/**
 * 填充模板占位符
 * @param {string} prompt
 * @param {Object} values - { placeholderName: value }
 * @returns {string}
 */
export function fillTemplate(prompt, values) {
  if (!prompt) return ''
  return prompt.replace(/\{([^}:]+)(?::[^}]*)?\}/g, (match, name) => {
    const key = name.trim()
    return values[key] !== undefined ? values[key] : match
  })
}

/**
 * 导出所有模板
 * @returns {string}
 */
export function exportTemplates() {
  const templates = getTemplates()
  return JSON.stringify(templates, null, 2)
}

/**
 * 导入模板
 * @param {string} jsonString
 * @returns {{ success: boolean, count: number, error?: string }}
 */
export function importTemplates(jsonString) {
  try {
    const imported = JSON.parse(jsonString)
    if (!Array.isArray(imported)) {
      return { success: false, count: 0, error: 'Invalid format: expected array' }
    }

    const existing = getTemplates()
    const existingIds = new Set(existing.map(t => t.id))

    let added = 0
    for (const template of imported) {
      if (!existingIds.has(template.id) && template.name && template.prompt) {
        template.placeholders = extractPlaceholders(template.prompt)
        existing.push(template)
        added++
      }
    }

    saveTemplates(existing)
    return { success: true, count: added }
  } catch (e) {
    return { success: false, count: 0, error: e.message }
  }
}

/**
 * 内置模板
 */
export const builtinTemplates = [
  {
    id: 'tpl-tutorial',
    name: '技术教程',
    icon: ' ',
    prompt: '请写一篇关于 {topic} 的技术教程，目标读者是 {audience:中级开发者}，字数约 {wordCount:2000} 字。',
    category: 'writing',
    isBuiltin: true,
  },
  {
    id: 'tpl-review',
    name: '技术评测',
    icon: '⭐',
    prompt: '请对 {tool} 进行技术评测，从 {aspects:性能、易用性、生态系统} 三个方面分析，给出 1-10 分的评分。',
    category: 'analysis',
    isBuiltin: true,
  },
  {
    id: 'tpl-compare',
    name: '技术对比',
    icon: '⚖️',
    prompt: '请对比 {tech1} 和 {tech2}，从 {aspects:性能、学习曲线、社区支持} 方面分析各自的优缺点，给出使用建议。',
    category: 'analysis',
    isBuiltin: true,
  },
  {
    id: 'tpl-troubleshoot',
    name: '问题排查',
    icon: ' ',
    prompt: '我在使用 {tech} 时遇到了以下问题：\n{problem}\n\n请帮我分析可能的原因并给出解决方案。',
    category: 'analysis',
    isBuiltin: true,
  },
  {
    id: 'tpl-translate-article',
    name: '文章翻译',
    icon: ' ',
    prompt: '请将以下文章翻译成 {targetLang:英文}，保持技术术语准确，行文流畅自然：\n\n{content}',
    category: 'translate',
    isBuiltin: true,
  },
  {
    id: 'tpl-seo',
    name: 'SEO 优化',
    icon: ' ',
    prompt: '请为以下文章生成 SEO 优化建议，包括：标题建议（3个）、meta description、关键词（5个）、内部链接建议：\n\n{content}',
    category: 'enhance',
    isBuiltin: true,
  },
]

export default {
  getTemplates,
  addTemplate,
  updateTemplate,
  deleteTemplate,
  getTemplateById,
  extractPlaceholders,
  fillTemplate,
  exportTemplates,
  importTemplates,
  builtinTemplates,
}
