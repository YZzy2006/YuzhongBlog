/**
 * 多语言支持系统
 * 提供语言检测、翻译优化、语言特定提示等功能
 */

/**
 * 支持的语言
 */
export const SUPPORTED_LANGUAGES = {
  zh: { name: '中文', nameEn: 'Chinese', nativeName: '中文' },
  en: { name: '英文', nameEn: 'English', nativeName: 'English' },
  ja: { name: '日文', nameEn: 'Japanese', nativeName: '日本語' },
  ko: { name: '韩文', nameEn: 'Korean', nativeName: '한국어' },
  fr: { name: '法文', nameEn: 'French', nativeName: 'Français' },
  de: { name: '德文', nameEn: 'German', nativeName: 'Deutsch' },
  es: { name: '西班牙文', nameEn: 'Spanish', nativeName: 'Español' },
  ru: { name: '俄文', nameEn: 'Russian', nativeName: 'Русский' },
}

/**
 * 检测文本语言
 * @param {string} text
 * @returns {string} language code
 */
export function detectLanguage(text) {
  if (!text) return 'zh'

  // 统计各语言字符
  const counts = {
    zh: (text.match(/[一-鿿]/g) || []).length,
    ja: (text.match(/[぀-ゟ゠-ヿ]/g) || []).length,
    ko: (text.match(/[가-힯ᄀ-ᇿ]/g) || []).length,
    ru: (text.match(/[Ѐ-ӿ]/g) || []).length,
    en: (text.match(/[a-zA-Z]/g) || []).length,
  }

  // 计算总字符数
  const total = Object.values(counts).reduce((a, b) => a + b, 0)
  if (total === 0) return 'zh'

  // 找出占比最高的语言
  let maxLang = 'zh'
  let maxRatio = 0

  for (const [lang, count] of Object.entries(counts)) {
    const ratio = count / total
    if (ratio > maxRatio) {
      maxRatio = ratio
      maxLang = lang
    }
  }

  // 如果中文和日文都有，根据特殊字符判断
  if (counts.zh > 0 && counts.ja > 0) {
    const jaSpecific = (text.match(/[぀-ゟ゠-ヿ]/g) || []).length
    if (jaSpecific > counts.zh * 0.1) return 'ja'
  }

  return maxLang
}

/**
 * 获取语言信息
 * @param {string} langCode
 * @returns {Object}
 */
export function getLanguageInfo(langCode) {
  return SUPPORTED_LANGUAGES[langCode] || SUPPORTED_LANGUAGES.zh
}

/**
 * 生成翻译提示词
 * @param {string} text
 * @param {string} sourceLang
 * @param {string} targetLang
 * @returns {string}
 */
export function generateTranslationPrompt(text, sourceLang, targetLang) {
  if (!text) return '请提供需要翻译的文本。'
  const sourceInfo = getLanguageInfo(sourceLang)
  const targetInfo = getLanguageInfo(targetLang)

  return `请将以下 ${sourceInfo.name} 文本翻译为 ${targetInfo.name}。

要求：
- 保持原文的语气和风格
- 翻译自然流畅，符合目标语言表达习惯
- 专业术语使用准确
- 保持 Markdown 格式

原文：
${text}`
}

/**
 * 生成多语言摘要提示词
 * @param {string} text
 * @param {string} sourceLang
 * @returns {string}
 */
export function generateMultilingualSummaryPrompt(text, sourceLang) {
  if (!text) return '请提供需要生成摘要的文本。'
  const sourceInfo = getLanguageInfo(sourceLang)

  return `请为以下 ${sourceInfo.name} 文本生成中英双语摘要。

要求：
- 中文摘要约 150 字
- 英文摘要约 100 词
- 保持核心观点一致
- 语言简洁明了

原文：
${text.slice(0, 2000)}${text.length > 2000 ? '...' : ''}`
}

/**
 * 生成语言润色提示词
 * @param {string} text
 * @param {string} lang
 * @returns {string}
 */
export function generatePolishPrompt(text, lang) {
  if (!text) return '请提供需要润色的文本。'
  const langInfo = getLanguageInfo(lang)

  return `请润色以下 ${langInfo.name} 文本，提升其表达质量。

要求：
- 修正语法和拼写错误
- 优化用词和表达
- 提升可读性和流畅度
- 保持原意不变
- 保持 Markdown 格式

原文：
${text}`
}

/**
 * 生成语言检测结果描述
 * @param {string} text
 * @returns {Object}
 */
export function analyzeLanguage(text) {
  const lang = detectLanguage(text)
  const langInfo = getLanguageInfo(lang)

  // 计算语言纯度
  const counts = {
    zh: (text.match(/[一-鿿]/g) || []).length,
    ja: (text.match(/[぀-ゟ゠-ヿ]/g) || []).length,
    ko: (text.match(/[가-힯ᄀ-ᇿ]/g) || []).length,
    ru: (text.match(/[Ѐ-ӿ]/g) || []).length,
    en: (text.match(/[a-zA-Z]+/g) || []).length,
  }
  const total = Object.values(counts).reduce((a, b) => a + b, 0)
  const purity = total > 0 ? Math.round((counts[lang] / total) * 100) : 100

  // 检测是否为混合语言
  const nonDominant = Object.entries(counts)
    .filter(([l, c]) => l !== lang && c > 0)
    .map(([l, c]) => ({ lang: l, count: c, ratio: Math.round((c / total) * 100) }))
    .filter(item => item.ratio > 5)

  const isMixed = nonDominant.length > 0

  return {
    language: lang,
    languageInfo: langInfo,
    purity,
    isMixed,
    mixedLanguages: nonDominant,
    charCount: text.length,
    wordCount: text.match(/[一-鿿]|[a-zA-Z]+/g)?.length || 0,
  }
}

/**
 * 获取语言特定的写作建议
 * @param {string} lang
 * @returns {Array<string>}
 */
export function getLanguageTips(lang) {
  const tips = {
    zh: [
      '使用简洁明了的中文表达',
      '避免过度使用"的"字',
      '适当使用成语增强表达力',
      '注意标点符号的正确使用',
      '段落之间保持逻辑连贯',
    ],
    en: [
      'Use active voice when possible',
      'Vary sentence length for better rhythm',
      'Avoid jargon unless necessary',
      'Use transition words for coherence',
      'Keep paragraphs focused and concise',
    ],
    ja: [
      '敬語の使用に注意する',
      '主語と述語の対応を明確にする',
      '助詞の使い分けに注意する',
      '文末表現を統一する',
      '漢字とひらがなのバランスを保つ',
    ],
    ko: [
      '문장 호응을 명확히 하세요',
      '조사 사용에 주의하세요',
      '문말 표현을 통일하세요',
      '한자어와 고유어의 균형을 맞추세요',
      '문장 간 연결을 자연스럽게 하세요',
    ],
  }

  return tips[lang] || tips.zh
}

export default {
  SUPPORTED_LANGUAGES,
  detectLanguage,
  getLanguageInfo,
  generateTranslationPrompt,
  generateMultilingualSummaryPrompt,
  generatePolishPrompt,
  analyzeLanguage,
  getLanguageTips,
}
