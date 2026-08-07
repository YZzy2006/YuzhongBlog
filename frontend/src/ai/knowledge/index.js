/**
 * 站点知识库系统
 * 存储站点特定信息，为 AI 提供上下文
 */

const STORAGE_KEY = 'ai-knowledge-base'

/**
 * 获取知识库
 * @returns {Object}
 */
export function getKnowledgeBase() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : getDefaultKnowledgeBase()
  } catch {
    return getDefaultKnowledgeBase()
  }
}

/**
 * 保存知识库
 * @param {Object} kb
 */
function saveKnowledgeBase(kb) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(kb))
  } catch (e) {
    console.error('Failed to save knowledge base:', e)
  }
}

/**
 * 获取默认知识库
 * @returns {Object}
 */
function getDefaultKnowledgeBase() {
  return {
    site: {
      name: '',
      description: '',
      author: '',
      language: 'zh',
      topics: [],
    },
    style: {
      tone: 'professional',
      formality: 'formal',
      technicalLevel: 'intermediate',
      preferredLength: 'medium',
      avoidWords: [],
      preferredPhrases: [],
    },
    terminology: {},
    patterns: {
      articleStructure: [],
      commonSections: [],
      writingTips: [],
    },
    lastUpdated: new Date().toISOString(),
  }
}

/**
 * 更新站点信息
 * @param {Object} siteInfo
 */
export function updateSiteInfo(siteInfo) {
  const kb = getKnowledgeBase()
  kb.site = { ...kb.site, ...siteInfo }
  kb.lastUpdated = new Date().toISOString()
  saveKnowledgeBase(kb)
  return kb.site
}

/**
 * 更新写作风格
 * @param {Object} style
 */
export function updateStyle(style) {
  const kb = getKnowledgeBase()
  kb.style = { ...kb.style, ...style }
  kb.lastUpdated = new Date().toISOString()
  saveKnowledgeBase(kb)
  return kb.style
}

/**
 * 添加术语
 * @param {string} term
 * @param {string} definition
 */
export function addTerm(term, definition) {
  const kb = getKnowledgeBase()
  kb.terminology[term] = {
    definition,
    addedAt: new Date().toISOString(),
  }
  kb.lastUpdated = new Date().toISOString()
  saveKnowledgeBase(kb)
  return kb.terminology
}

/**
 * 删除术语
 * @param {string} term
 */
export function removeTerm(term) {
  const kb = getKnowledgeBase()
  delete kb.terminology[term]
  kb.lastUpdated = new Date().toISOString()
  saveKnowledgeBase(kb)
  return kb.terminology
}

/**
 * 添加写作模式
 * @param {Object} pattern
 */
export function addPattern(pattern) {
  const kb = getKnowledgeBase()
  kb.patterns.writingTips.push({
    ...pattern,
    addedAt: new Date().toISOString(),
  })
  kb.lastUpdated = new Date().toISOString()
  saveKnowledgeBase(kb)
  return kb.patterns
}

/**
 * 构建知识库上下文字符串
 * @returns {string}
 */
export function buildKnowledgeContext() {
  const kb = getKnowledgeBase()
  const parts = []

  // 站点信息
  if (kb.site.name) {
    const topics = kb.site.topics || []
    parts.push(`【站点信息】
站点名称: ${kb.site.name}
站点描述: ${kb.site.description || '无'}
作者: ${kb.site.author || '未知'}
主要话题: ${topics.length > 0 ? topics.join('、') : '未设置'}`)
  }

  // 写作风格
  parts.push(`【写作风格要求】
语气: ${getToneLabel(kb.style.tone)}
正式程度: ${getFormalityLabel(kb.style.formality)}
技术深度: ${getTechnicalLevelLabel(kb.style.technicalLevel)}
内容长度: ${getLengthLabel(kb.style.preferredLength)}`)

  const avoidWords = kb.style.avoidWords || []
  if (avoidWords.length > 0) {
    parts.push(`避免使用: ${avoidWords.join('、')}`)
  }

  const preferredPhrases = kb.style.preferredPhrases || []
  if (preferredPhrases.length > 0) {
    parts.push(`推荐用语: ${preferredPhrases.join('、')}`)
  }

  // 术语表
  const terms = Object.entries(kb.terminology)
  if (terms.length > 0) {
    parts.push(`【专业术语】
${terms.map(([term, data]) => `${term}: ${data.definition}`).join('\n')}`)
  }

  // 写作建议
  if (kb.patterns.writingTips.length > 0) {
    parts.push(`【写作建议】
${kb.patterns.writingTips.map(tip => `- ${tip.content}`).join('\n')}`)
  }

  return parts.join('\n\n')
}

/**
 * 获取语气标签
 * @param {string} tone
 * @returns {string}
 */
function getToneLabel(tone) {
  const labels = {
    professional: '专业严谨',
    friendly: '友好亲切',
    casual: '轻松随意',
    humorous: '幽默风趣',
    academic: '学术正式',
  }
  return labels[tone] || tone
}

/**
 * 获取正式程度标签
 * @param {string} formality
 * @returns {string}
 */
function getFormalityLabel(formality) {
  const labels = {
    formal: '正式',
    'semi-formal': '半正式',
    informal: '非正式',
  }
  return labels[formality] || formality
}

/**
 * 获取技术深度标签
 * @param {string} level
 * @returns {string}
 */
function getTechnicalLevelLabel(level) {
  const labels = {
    beginner: '入门级',
    intermediate: '中级',
    advanced: '高级',
    expert: '专家级',
  }
  return labels[level] || level
}

/**
 * 获取内容长度标签
 * @param {string} length
 * @returns {string}
 */
function getLengthLabel(length) {
  const labels = {
    short: '简短(500-1000字)',
    medium: '中等(1000-2000字)',
    long: '详细(2000-3000字)',
    comprehensive: '全面(3000字以上)',
  }
  return labels[length] || length
}

/**
 * 从现有文章中学习写作模式
 * @param {string} content
 */
export function learnFromContent(content) {
  const kb = getKnowledgeBase()

  // 学习文章结构
  const headings = content.match(/^#{1,6}\s+.+$/gm) || []
  if (headings.length > 0) {
    const structure = headings.map(h => h.replace(/^#+\s+/, ''))
    kb.patterns.articleStructure = [...new Set([...kb.patterns.articleStructure, ...structure])].slice(0, 20)
  }

  // 学习常用短语
  const phrases = content.match(/[一-龥]{2,4}/g) || []
  const phraseFreq = {}
  phrases.forEach(phrase => {
    phraseFreq[phrase] = (phraseFreq[phrase] || 0) + 1
  })
  const commonPhrases = Object.entries(phraseFreq)
    .filter(([_, count]) => count >= 3)
    .map(([phrase]) => phrase)
    .slice(0, 10)
  kb.style.preferredPhrases = [...new Set([...kb.style.preferredPhrases, ...commonPhrases])].slice(0, 20)

  kb.lastUpdated = new Date().toISOString()
  saveKnowledgeBase(kb)
  return kb
}

/**
 * 导出知识库
 * @returns {string}
 */
export function exportKnowledgeBase() {
  const kb = getKnowledgeBase()
  return JSON.stringify(kb, null, 2)
}

/**
 * 导入知识库
 * @param {string} jsonString
 * @returns {{ success: boolean, error?: string }}
 */
export function importKnowledgeBase(jsonString) {
  try {
    const imported = JSON.parse(jsonString)
    if (typeof imported !== 'object') {
      return { success: false, error: 'Invalid format: expected object' }
    }

    const kb = getKnowledgeBase()
    const merged = {
      ...kb,
      ...imported,
      site: { ...kb.site, ...imported.site },
      style: { ...kb.style, ...imported.style },
      terminology: { ...kb.terminology, ...imported.terminology },
      patterns: {
        articleStructure: [...new Set([
          ...(kb.patterns.articleStructure || []),
          ...(imported.patterns?.articleStructure || [])
        ])].slice(0, 20),
        commonSections: [...new Set([
          ...(kb.patterns.commonSections || []),
          ...(imported.patterns?.commonSections || [])
        ])],
        writingTips: [...(kb.patterns.writingTips || []), ...(imported.patterns?.writingTips || [])],
      },
      lastUpdated: new Date().toISOString(),
    }

    saveKnowledgeBase(merged)
    return { success: true }
  } catch (e) {
    return { success: false, error: e.message }
  }
}

export default {
  getKnowledgeBase,
  updateSiteInfo,
  updateStyle,
  addTerm,
  removeTerm,
  addPattern,
  buildKnowledgeContext,
  learnFromContent,
  exportKnowledgeBase,
  importKnowledgeBase,
}
