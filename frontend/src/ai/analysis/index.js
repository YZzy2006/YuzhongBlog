/**
 * 内容分析系统
 * 提供 SEO 优化和可读性分析
 */

/**
 * 分析内容
 * @param {string} content
 * @param {Object} options
 * @returns {Object}
 */
export function analyzeContent(content, options = {}) {
  if (!content) return { seo: null, readability: null, quality: null }

  return {
    seo: analyzeSeo(content, options),
    readability: analyzeReadability(content),
    quality: analyzeQuality(content),
  }
}

/**
 * SEO 分析
 * @param {string} content
 * @param {Object} options
 * @returns {Object}
 */
function analyzeSeo(content, options = {}) {
  const { keyword = '', title = '' } = options
  const issues = []
  const suggestions = []

  // 标题分析
  const headings = extractHeadings(content)
  if (headings.length === 0) {
    issues.push({ type: 'warning', message: '缺少标题，建议添加 H1 标题' })
  }

  // H1 标题检查
  const h1Count = headings.filter(h => h.level === 1).length
  if (h1Count === 0) {
    issues.push({ type: 'error', message: '缺少 H1 标题' })
  } else if (h1Count > 1) {
    issues.push({ type: 'warning', message: '存在多个 H1 标题，建议只使用一个' })
  }

  // 关键词分析
  if (keyword) {
    const keywordLower = keyword.toLowerCase()
    const contentLower = content.toLowerCase()
    const escapedKeyword = keywordLower.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const keywordCount = (contentLower.match(new RegExp(escapedKeyword, 'g')) || []).length
    const wordCount = content.split(/\s+/).length
    const density = (keywordCount / wordCount) * 100

    if (keywordCount === 0) {
      issues.push({ type: 'error', message: `内容中未找到关键词"${keyword}"` })
    } else if (density < 0.5) {
      suggestions.push(`关键词密度较低(${density.toFixed(1)}%)，建议增加到 1-2%`)
    } else if (density > 3) {
      suggestions.push(`关键词密度过高(${density.toFixed(1)}%)，建议降低到 1-2%`)
    }

    // 标题中是否包含关键词
    if (title && !title.toLowerCase().includes(keywordLower)) {
      suggestions.push('标题中未包含关键词，建议添加')
    }

    // 第一段是否包含关键词
    const firstParagraph = content.split('\n\n')[0] || ''
    if (!firstParagraph.toLowerCase().includes(keywordLower)) {
      suggestions.push('第一段未包含关键词，建议添加')
    }
  }

  // 链接分析
  const internalLinks = (content.match(/\[.*?\]\(\/.*?\)/g) || []).length
  const externalLinks = (content.match(/\[.*?\]\(https?:\/\/.*?\)/g) || []).length
  if (internalLinks === 0 && externalLinks === 0) {
    suggestions.push('未发现链接，建议添加内部或外部链接')
  }

  // 图片分析
  const images = (content.match(/!\[.*?\]\(.*?\)/g) || []).length
  if (images === 0) {
    suggestions.push('未发现图片，建议添加相关图片')
  }

  // Alt 文本检查
  const imagesWithoutAlt = (content.match(/!\[\]\(.*?\)/g) || []).length
  if (imagesWithoutAlt > 0) {
    issues.push({ type: 'warning', message: `${imagesWithoutAlt} 张图片缺少 Alt 文本` })
  }

  // Meta description 建议
  const firstSentences = content.replace(/[#*`\[\]]/g, '').split(/[。！？.!?]/).slice(0, 2).join('。')
  if (firstSentences.length > 0) {
    suggestions.push(`Meta Description 建议: "${firstSentences.slice(0, 150)}..."`)
  }

  return {
    headings,
    keywordDensity: keyword ? calculateKeywordDensity(content, keyword) : null,
    internalLinks,
    externalLinks,
    images,
    issues,
    suggestions,
    score: calculateSeoScore(issues, suggestions),
  }
}

/**
 * 可读性分析
 * @param {string} content
 * @returns {Object}
 */
function analyzeReadability(content) {
  const sentences = splitSentences(content)
  const words = splitWords(content)
  const paragraphs = content.split(/\n\s*\n/).filter(p => p.trim())

  // 句子长度分析
  const sentenceLengths = sentences.map(s => splitWords(s).length)
  const avgSentenceLength = sentenceLengths.length > 0
    ? sentenceLengths.reduce((a, b) => a + b, 0) / sentenceLengths.length
    : 0

  // 段落长度分析
  const paragraphLengths = paragraphs.map(p => p.length)
  const avgParagraphLength = paragraphLengths.length > 0
    ? paragraphLengths.reduce((a, b) => a + b, 0) / paragraphLengths.length
    : 0

  // 阅读时间（中文约 300 字/分钟，英文约 200 词/分钟）
  const chineseChars = (content.match(/[一-鿿]/g) || []).length
  const isChinese = chineseChars > content.length * 0.3
  const readingTime = isChinese
    ? Math.ceil(chineseChars / 300)
    : Math.ceil(words.length / 200)

  // Flesch-Kincaid 可读性分数（简化版）
  const syllables = words.reduce((count, word) => count + countSyllables(word), 0)
  const fleschKincaid = (sentences.length > 0 && words.length > 0)
    ? 206.835 - 1.015 * (words.length / sentences.length) - 84.6 * (syllables / words.length)
    : 60

  // 可读性等级
  let readabilityLevel = 'easy'
  if (fleschKincaid < 30) readabilityLevel = 'very-difficult'
  else if (fleschKincaid < 50) readabilityLevel = 'difficult'
  else if (fleschKincaid < 60) readabilityLevel = 'standard'
  else if (fleschKincaid < 70) readabilityLevel = 'fairly-easy'

  const issues = []

  // 长句子检查
  const longSentences = sentenceLengths.filter(l => l > 40).length
  if (longSentences > 0) {
    issues.push({ type: 'warning', message: `${longSentences} 个句子过长(超过40词)，建议拆分` })
  }

  // 长段落检查
  const longParagraphs = paragraphLengths.filter(l => l > 500).length
  if (longParagraphs > 0) {
    issues.push({ type: 'warning', message: `${longParagraphs} 个段落过长(超过500字)，建议拆分` })
  }

  return {
    sentenceCount: sentences.length,
    wordCount: words.length,
    paragraphCount: paragraphs.length,
    avgSentenceLength: Math.round(avgSentenceLength),
    avgParagraphLength: Math.round(avgParagraphLength),
    readingTime,
    fleschKincaid: Math.round(fleschKincaid),
    readabilityLevel,
    issues,
  }
}

/**
 * 内容质量分析
 * @param {string} content
 * @returns {Object}
 */
function analyzeQuality(content) {
  const words = splitWords(content)
  const sentences = splitSentences(content)

  // 代码块分析
  const codeBlocks = content.match(/```[\s\S]*?```/g) || []
  const codeLines = codeBlocks.reduce((count, block) => {
    return count + block.split('\n').length - 2
  }, 0)

  // 重复词分析
  const wordFreq = {}
  words.forEach(word => {
    const lower = word.toLowerCase()
    if (lower.length > 3) {
      wordFreq[lower] = (wordFreq[lower] || 0) + 1
    }
  })
  const repeatedWords = Object.entries(wordFreq)
    .filter(([_, count]) => count > 5)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)

  // 内容结构分析
  const hasIntroduction = content.split('\n\n')[0]?.length > 100
  const hasConclusion = content.split('\n\n').pop()?.length > 50
  const hasCodeExamples = codeBlocks.length > 0

  // 评分
  let score = 70
  if (words.length > 300) score += 10
  if (words.length > 1000) score += 10
  if (hasIntroduction) score += 5
  if (hasConclusion) score += 5
  if (hasCodeExamples) score += 5
  if (repeatedWords.length > 3) score -= 10

  return {
    wordCount: words.length,
    sentenceCount: sentences.length,
    codeBlockCount: codeBlocks.length,
    codeLineCount: codeLines,
    repeatedWords,
    hasIntroduction,
    hasConclusion,
    hasCodeExamples,
    score: Math.min(100, Math.max(0, score)),
  }
}

/**
 * 提取标题
 * @param {string} content
 * @returns {Array}
 */
function extractHeadings(content) {
  const headings = []
  const lines = content.split('\n')

  for (let i = 0; i < lines.length; i++) {
    const match = lines[i].match(/^(#{1,6})\s+(.+)/)
    if (match) {
      headings.push({
        level: match[1].length,
        text: match[2].trim(),
        line: i + 1,
      })
    }
  }

  return headings
}

/**
 * 分割句子
 * @param {string} text
 * @returns {Array}
 */
function splitSentences(text) {
  return text
    .replace(/[#*`\[\]]/g, '')
    .split(/[。！？.!?]+/)
    .map(s => s.trim())
    .filter(s => s.length > 0)
}

/**
 * 分割单词
 * @param {string} text
 * @returns {Array}
 */
function splitWords(text) {
  const cleanText = text.replace(/[#*`\[\](){}]/g, '')
  const chineseChars = cleanText.match(/[一-鿿]/g) || []
  const englishWords = cleanText.match(/[a-zA-Z]+/g) || []
  return [...chineseChars, ...englishWords]
}

/**
 * 计算音节数（英文单词）
 * @param {string} word
 * @returns {number}
 */
function countSyllables(word) {
  word = word.toLowerCase()
  if (word.length <= 3) return 1
  word = word.replace(/(?:[^laeiouy]es|ed|[^laeiouy]e)$/, '')
  word = word.replace(/^y/, '')
  const matches = word.match(/[aeiouy]{1,2}/g)
  return matches ? matches.length : 1
}

/**
 * 计算关键词密度
 * @param {string} content
 * @param {string} keyword
 * @returns {number}
 */
function calculateKeywordDensity(content, keyword) {
  const contentLower = content.toLowerCase()
  const keywordLower = keyword.toLowerCase()
  const escapedKeyword = keywordLower.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const keywordCount = (contentLower.match(new RegExp(escapedKeyword, 'g')) || []).length
  const wordCount = splitWords(content).length
  return wordCount > 0 ? (keywordCount / wordCount) * 100 : 0
}

/**
 * 计算 SEO 分数
 * @param {Array} issues
 * @param {Array} suggestions
 * @returns {number}
 */
function calculateSeoScore(issues, suggestions) {
  let score = 100
  issues.forEach(issue => {
    if (issue.type === 'error') score -= 15
    else if (issue.type === 'warning') score -= 5
  })
  score -= suggestions.length * 2
  return Math.max(0, Math.min(100, score))
}

/**
 * 生成 SEO 优化建议
 * @param {string} content
 * @param {Object} options
 * @returns {string}
 */
export function generateSeoPrompt(content, options = {}) {
  if (!content) return '请提供内容以进行 SEO 分析。'

  const analysis = analyzeContent(content, options)
  const { keyword = '' } = options

  const seo = analysis.seo || {}
  const readability = analysis.readability || {}

  return `请根据以下内容分析结果，提供 SEO 优化建议：

## 当前分析
- 标题结构: ${(seo.headings || []).length} 个标题
- 关键词: ${keyword || '未指定'}
- 关键词密度: ${seo.keywordDensity ? seo.keywordDensity.toFixed(1) + '%' : '未计算'}
- 内部链接: ${seo.internalLinks || 0} 个
- 外部链接: ${seo.externalLinks || 0} 个
- 图片: ${seo.images || 0} 张
- 可读性分数: ${readability.fleschKincaid || 'N/A'}
- 阅读时间: ${readability.readingTime || 0} 分钟

## 发现的问题
${(seo.issues || []).map(i => `- ${i.message}`).join('\n') || '无'}

## 请提供：
1. 标题优化建议（3个备选标题）
2. Meta Description 建议（150字以内）
3. 关键词优化建议
4. 内容结构优化建议
5. 内部链接建议

当前内容：
${content.slice(0, 2000)}${content.length > 2000 ? '...' : ''}`
}

export default {
  analyzeContent,
  generateSeoPrompt,
}
