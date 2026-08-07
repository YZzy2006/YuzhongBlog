/**
 * 智能续写系统
 * 分析内容结构和风格，生成续写提示词
 */

/**
 * 分析内容结构
 * @param {string} content
 * @returns {Object}
 */
export function analyzeContent(content) {
  if (!content) return { type: 'empty', sections: [], style: 'general' }

  const lines = content.split('\n')
  const sections = []
  let currentSection = null

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim()

    // 检测标题
    if (line.match(/^#{1,6}\s+/)) {
      if (currentSection) sections.push(currentSection)
      currentSection = {
        title: line.replace(/^#+\s+/, ''),
        level: line.match(/^(#{1,6})/)[1].length,
        startLine: i,
        content: [],
      }
    } else {
      // 没有标题的内容归入当前章节，若无章节则创建默认章节
      if (!currentSection) {
        currentSection = { title: null, level: 0, startLine: 0, content: [] }
      }
      currentSection.content.push(line)
    }
  }

  if (currentSection) sections.push(currentSection)

  // 分析内容类型
  const type = analyzeContentType(content)

  // 分析写作风格
  const style = analyzeWritingStyle(content)

  // 分析代码密度
  const codeBlocks = (content.match(/```[\s\S]*?```/g) || []).length
  const codeDensity = codeBlocks > 0 ? codeBlocks / sections.length : 0

  return {
    type,
    sections,
    style,
    codeDensity,
    wordCount: content.length,
    lineCount: lines.length,
    lastSection: sections[sections.length - 1] || null,
  }
}

/**
 * 分析内容类型
 * @param {string} content
 * @returns {string}
 */
function analyzeContentType(content) {
  const codeBlocks = (content.match(/```[\s\S]*?```/g) || []).length
  const headings = (content.match(/^#{1,6}\s+/gm) || []).length
  const lists = (content.match(/^[\s]*[-*+]\s+/gm) || []).length
  const paragraphs = content.split(/\n\s*\n/).filter(p => p.trim()).length

  if (codeBlocks > 3) return 'tutorial'
  if (headings > 5) return 'structured'
  if (lists > 5) return 'guide'
  if (paragraphs > 3 && codeBlocks === 0) return 'article'
  return 'general'
}

/**
 * 分析写作风格
 * @param {string} content
 * @returns {string}
 */
function analyzeWritingStyle(content) {
  // 检测正式程度
  const formalPatterns = /因此|然而|此外|综上所述|由此可见/g
  const casualPatterns = /哈哈|嗯|哦|啦|吧|呀/g
  const formalCount = (content.match(formalPatterns) || []).length
  const casualCount = (content.match(casualPatterns) || []).length

  if (formalCount > casualCount * 2) return 'formal'
  if (casualCount > formalCount * 2) return 'casual'

  // 检测技术性
  const techPatterns = /API|SDK|HTTP|JSON|XML|SQL|Git|Docker|Kubernetes/gi
  const techCount = (content.match(techPatterns) || []).length
  if (techCount > 5) return 'technical'

  return 'neutral'
}

/**
 * 生成智能续写提示词
 * @param {string} content - 现有内容
 * @param {Object} options - 选项
 * @returns {string}
 */
export function buildContinuePrompt(content, options = {}) {
  const analysis = analyzeContent(content)
  const {
    language = detectLanguage(content),
    targetLength = 'medium',
    focus = 'auto',
  } = options

  const langHint = language === 'zh' ? '请用中文续写。' : 'Please continue in English.'
  const lengthHint = {
    short: '续写约 100-200 字。',
    medium: '续写约 300-500 字。',
    long: '续写约 500-1000 字。',
  }[targetLength] || '续写约 300-500 字。'

  let focusHint = ''
  if (focus === 'auto') {
    if (analysis.type === 'tutorial') {
      focusHint = '继续讲解下一个知识点或步骤，保持教程的连贯性。'
    } else if (analysis.type === 'structured') {
      focusHint = '继续完善当前章节，或开始下一个相关主题。'
    } else if (analysis.type === 'guide') {
      focusHint = '继续补充更多实用建议或注意事项。'
    } else {
      focusHint = '自然地延续当前内容，保持风格一致。'
    }
  } else {
    focusHint = focus
  }

  // 分析最后的章节
  const lastSection = analysis.lastSection
  let contextHint = ''
  if (lastSection && lastSection.title) {
    contextHint = `\n\n当前正在写的章节: "${lastSection.title}"\n章节内容概要: ${lastSection.content.slice(-3).join(' ').slice(0, 200)}...`
  }

  const prompt = `你是一个专业的技术写作助手。请根据以下内容进行智能续写。

要求：
${langHint}
${lengthHint}
${focusHint}

续写规则：
1. 保持与现有内容相同的写作风格和语气
2. 如果内容中有代码，继续使用相同的编程语言和代码风格
3. 如果正在写教程，继续讲解下一个步骤
4. 如果正在写文章，自然地过渡到下一个论点
5. 不要重复已有内容，直接从断点处继续
6. 保持 Markdown 格式一致性

当前内容：
${content}${contextHint}

请从上述内容的末尾继续写：`

  return prompt
}

/**
 * 检测语言
 * @param {string} text
 * @returns {string}
 */
function detectLanguage(text) {
  if (!text) return 'zh'
  const chineseChars = (text.match(/[一-鿿]/g) || []).length
  return chineseChars > text.length * 0.15 ? 'zh' : 'en'
}

/**
 * 获取续写建议
 * @param {string} content
 * @returns {Array<string>}
 */
export function getContinueSuggestions(content) {
  const analysis = analyzeContent(content)
  const suggestions = []

  if (analysis.type === 'tutorial') {
    suggestions.push('继续下一步操作')
    suggestions.push('添加注意事项')
    suggestions.push('补充代码示例')
    suggestions.push('添加常见问题')
  } else if (analysis.type === 'structured') {
    suggestions.push('继续下一章节')
    suggestions.push('补充当前章节')
    suggestions.push('添加总结')
    suggestions.push('添加参考资料')
  } else if (analysis.type === 'guide') {
    suggestions.push('添加更多建议')
    suggestions.push('补充注意事项')
    suggestions.push('添加示例')
    suggestions.push('添加总结')
  } else {
    suggestions.push('继续展开论述')
    suggestions.push('添加例子')
    suggestions.push('补充说明')
    suggestions.push('添加结论')
  }

  return suggestions
}

export default {
  analyzeContent,
  buildContinuePrompt,
  getContinueSuggestions,
}
