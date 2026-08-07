/**
 * 图片理解系统
 * 提供图片分析、ALT 文本生成、图片建议等功能
 */

/**
 * 从 Markdown 内容中提取图片
 * @param {string} content
 * @returns {Array<{alt: string, url: string, fullMatch: string}>}
 */
export function extractImages(content) {
  if (!content) return []
  const regex = /!\[([^\]]*)\]\(([^)]+)\)/g
  const images = []
  let match

  while ((match = regex.exec(content)) !== null) {
    images.push({
      alt: match[1],
      url: match[2],
      fullMatch: match[0],
      index: match.index,
    })
  }

  return images
}

/**
 * 分析图片使用情况
 * @param {string} content
 * @returns {Object}
 */
export function analyzeImageUsage(content) {
  const images = extractImages(content)
  const issues = []
  const suggestions = []

  // 检查是否有图片
  if (images.length === 0) {
    suggestions.push('建议添加相关图片以增强文章可读性')
    return { images: [], issues, suggestions, score: 60 }
  }

  // 检查 ALT 文本
  const missingAlt = images.filter(img => !img.alt.trim())
  if (missingAlt.length > 0) {
    issues.push({
      type: 'warning',
      message: `${missingAlt.length} 张图片缺少 ALT 文本，影响 SEO 和可访问性`,
      images: missingAlt,
    })
  }

  // 检查图片分布
  const lines = content.split('\n')
  const imageLines = images.map(img => {
    const before = content.slice(0, img.index)
    return before.split('\n').length
  })

  // 检查图片是否过于集中
  for (let i = 1; i < imageLines.length; i++) {
    if (imageLines[i] - imageLines[i - 1] < 3) {
      issues.push({
        type: 'info',
        message: '图片过于集中，建议分散放置以增强阅读体验',
      })
      break
    }
  }

  // 检查第一段和最后一段是否有图片
  const firstParagraph = content.split('\n\n')[0] || ''
  const lastParagraph = content.split('\n\n').pop() || ''
  const hasImageInFirst = images.some(img => firstParagraph.includes(img.fullMatch))
  const hasImageInLast = images.some(img => lastParagraph.includes(img.fullMatch))

  if (!hasImageInFirst && content.length > 1000) {
    suggestions.push('建议在文章开头添加一张特色图片')
  }

  // 计算图片密度
  const wordCount = content.replace(/[#*`\[\](){}]/g, '').length
  const imageDensity = wordCount > 0 ? images.length / (wordCount / 1000) : 0

  if (imageDensity < 0.5 && wordCount > 500) {
    suggestions.push('图片较少，建议每 500-1000 字添加一张图片')
  }

  // 计算分数
  let score = 80
  if (missingAlt.length > 0) score -= missingAlt.length * 5
  if (images.length === 0) score -= 20
  if (imageDensity > 2) score -= 10

  return {
    images,
    issues,
    suggestions,
    score: Math.max(0, Math.min(100, score)),
    stats: {
      total: images.length,
      missingAlt: missingAlt.length,
      density: imageDensity.toFixed(1),
    },
  }
}

/**
 * 生成 ALT 文本建议提示词
 * @param {string} imageUrl
 * @param {string} surroundingText
 * @returns {string}
 */
export function generateAltTextPrompt(imageUrl, surroundingText = '') {
  let prompt = `请为以下图片生成简洁、描述性的 ALT 文本（用于 SEO 和可访问性）。

要求：
- 10-125 个字符
- 准确描述图片内容
- 包含相关关键词（如果上下文允许）
- 避免以"图片"或"图像"开头`

  if (surroundingText) {
    prompt += `\n\n上下文内容：\n${surroundingText.slice(0, 500)}`
  }

  prompt += `\n\n图片 URL：${imageUrl}`

  return prompt
}

/**
 * 生成图片建议提示词
 * @param {string} content
 * @returns {string}
 */
export function generateImageSuggestionPrompt(content) {
  const analysis = analyzeImageUsage(content)

  let prompt = `请根据以下文章内容，建议合适的图片位置和类型。

当前图片情况：
- 已有 ${analysis.stats.total} 张图片
- ${analysis.stats.missingAlt} 张缺少 ALT 文本
- 图片密度：${analysis.stats.density} 张/千字

请建议：
1. 应该添加什么类型的图片（插图、图表、代码截图、示意图等）
2. 建议的图片位置
3. 每张图片的 ALT 文本建议

文章内容：
${content.slice(0, 2000)}${content.length > 2000 ? '...' : ''}`

  return prompt
}

/**
 * 批量生成 ALT 文本提示词
 * @param {Array} images
 * @param {string} content
 * @returns {string}
 */
export function generateBatchAltTextPrompt(images, content) {
  if (!images || images.length === 0) return ''

  let prompt = `请为以下 ${images.length} 张图片生成 ALT 文本。

要求：
- 每张图片的 ALT 文本 10-125 个字符
- 准确描述图片内容
- 与文章上下文相关

文章主题：${(content || '').slice(0, 200)}

图片列表：
${images.map((img, i) => `${i + 1}. URL: ${img.url}${img.alt ? ` (当前 ALT: ${img.alt})` : ''}`).join('\n')}

请以 JSON 格式返回：[{"index": 1, "alt": "建议的 ALT 文本"}, ...]`

  return prompt
}

export default {
  extractImages,
  analyzeImageUsage,
  generateAltTextPrompt,
  generateImageSuggestionPrompt,
  generateBatchAltTextPrompt,
}
