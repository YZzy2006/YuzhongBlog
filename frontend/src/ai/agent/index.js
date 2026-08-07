/**
 * Agent 自主写作系统
 * 提供多步骤自主写作能力
 */

/**
 * Agent 任务状态
 */
export const AGENT_STATUS = {
  IDLE: 'idle',
  PLANNING: 'planning',
  WRITING: 'writing',
  REVIEWING: 'reviewing',
  COMPLETED: 'completed',
  ERROR: 'error',
}

/**
 * 创建写作任务计划
 * @param {string} topic
 * @param {Object} options
 * @returns {Object}
 */
export function createWritingPlan(topic, options = {}) {
  const {
    type = 'article',
    language = 'zh',
    targetLength = 'medium',
    style = 'professional',
    includeCode = false,
  } = options

  const steps = []

  // Step 1: Research and outline
  steps.push({
    id: 'outline',
    name: '生成大纲',
    nameEn: 'Generate Outline',
    prompt: language === 'zh'
      ? `请为以下主题生成一个详细的文章大纲：

主题：${topic}

要求：
- 包含 3-5 个主要章节
- 每个章节包含 2-3 个子主题
- 考虑读者的知识水平
- 逻辑清晰，层次分明

请以 Markdown 格式输出大纲。`
      : `Please generate a detailed outline for the following topic:

Topic: ${topic}

Requirements:
- Include 3-5 main sections
- Each section has 2-3 subtopics
- Consider the reader's knowledge level
- Clear logic and structure

Output the outline in Markdown format.`,
    status: 'pending',
    result: null,
  })

  // Step 2: Introduction
  steps.push({
    id: 'introduction',
    name: '撰写引言',
    nameEn: 'Write Introduction',
    prompt: language === 'zh'
      ? `根据以下大纲，撰写一个引人入胜的引言：

{{outline}}

要求：
- 吸引读者注意力
- 概述文章主题
- 预告主要观点
- 约 150-200 字`
      : `Based on the following outline, write an engaging introduction:

{{outline}}

Requirements:
- Capture reader's attention
- Overview the article topic
- Preview main points
- About 150-200 words`,
    status: 'pending',
    result: null,
  })

  // Step 3: Main content sections
  steps.push({
    id: 'content',
    name: '撰写正文',
    nameEn: 'Write Main Content',
    prompt: language === 'zh'
      ? `根据以下大纲，撰写详细的正文内容：

{{outline}}

引言：
{{introduction}}

要求：
- 详细解释每个要点
- ${includeCode ? '包含代码示例' : '使用清晰的说明'}
- 适当使用标题和列表
- 目标长度：${getTargetLength(targetLength, language)}`
      : `Based on the following outline, write detailed main content:

{{outline}}

Introduction:
{{introduction}}

Requirements:
- Explain each point in detail
- ${includeCode ? 'Include code examples' : 'Use clear explanations'}
- Use headings and lists appropriately
- Target length: ${getTargetLength(targetLength, language)}`,
    status: 'pending',
    result: null,
  })

  // Step 4: Conclusion
  steps.push({
    id: 'conclusion',
    name: '撰写结论',
    nameEn: 'Write Conclusion',
    prompt: language === 'zh'
      ? `根据以下内容，撰写一个有力的结论：

大纲：
{{outline}}

正文：
{{content}}

要求：
- 总结主要观点
- 提供行动建议或思考
- 约 150-200 字`
      : `Based on the following content, write a strong conclusion:

Outline:
{{outline}}

Content:
{{content}}

Requirements:
- Summarize main points
- Provide action suggestions or reflections
- About 150-200 words`,
    status: 'pending',
    result: null,
  })

  // Step 5: Review and polish
  steps.push({
    id: 'review',
    name: '审校润色',
    nameEn: 'Review and Polish',
    prompt: language === 'zh'
      ? `请审阅并润色以下文章：

{{fullContent}}

要求：
- 检查语法和拼写
- 优化表达和用词
- 确保逻辑连贯
- 改善可读性
- 输出完整的润色后文章`
      : `Please review and polish the following article:

{{fullContent}}

Requirements:
- Check grammar and spelling
- Optimize expressions and word choice
- Ensure logical coherence
- Improve readability
- Output the complete polished article`,
    status: 'pending',
    result: null,
  })

  return {
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 7),
    topic,
    type,
    language,
    style,
    steps,
    currentStep: 0,
    status: AGENT_STATUS.IDLE,
    createdAt: new Date().toISOString(),
  }
}

/**
 * 获取目标长度描述
 * @param {string} length
 * @param {string} language
 * @returns {string}
 */
function getTargetLength(length, language) {
  const lengths = {
    zh: {
      short: '500-1000 字',
      medium: '1000-2000 字',
      long: '2000-3000 字',
      comprehensive: '3000 字以上',
    },
    en: {
      short: '500-1000 words',
      medium: '1000-2000 words',
      long: '2000-3000 words',
      comprehensive: '3000+ words',
    },
  }
  return lengths[language]?.[length] || lengths.zh[length] || '1000-2000 字'
}

/**
 * 准备步骤提示词（替换占位符）
 * @param {Object} plan
 * @param {number} stepIndex
 * @returns {string}
 */
export function prepareStepPrompt(plan, stepIndex) {
  const step = plan.steps[stepIndex]
  if (!step) return ''

  let prompt = step.prompt

  // 替换占位符
  const outlineStep = plan.steps.find(s => s.id === 'outline')
  const introStep = plan.steps.find(s => s.id === 'introduction')
  const contentStep = plan.steps.find(s => s.id === 'content')

  if (outlineStep?.result) {
    prompt = prompt.replace(/\{\{outline\}\}/g, outlineStep.result)
  }
  if (introStep?.result) {
    prompt = prompt.replace(/\{\{introduction\}\}/g, introStep.result)
  }
  if (contentStep?.result) {
    prompt = prompt.replace(/\{\{content\}\}/g, contentStep.result)
  }

  // 替换完整内容
  const fullContent = plan.steps
    .filter(s => s.result && s.id !== 'review')
    .map(s => s.result)
    .join('\n\n')
  prompt = prompt.replace(/\{\{fullContent\}\}/g, fullContent)

  return prompt
}

/**
 * 获取 Agent 进度
 * @param {Object} plan
 * @returns {Object}
 */
export function getAgentProgress(plan) {
  const completed = plan.steps.filter(s => s.status === 'completed').length
  const total = plan.steps.length
  const percentage = Math.round((completed / total) * 100)

  return {
    completed,
    total,
    percentage,
    currentStep: plan.steps[plan.currentStep]?.name || '',
    status: plan.status,
  }
}

/**
 * 导出 Agent 结果
 * @param {Object} plan
 * @returns {string}
 */
export function exportAgentResult(plan) {
  // 优先返回审校润色结果，其次返回正文，最后返回任意完成步骤
  const reviewStep = plan.steps.find(s => s.id === 'review' && s.result)
  if (reviewStep) return reviewStep.result

  const contentStep = plan.steps.find(s => s.id === 'content' && s.result)
  if (contentStep) return contentStep.result

  const parts = plan.steps.filter(s => s.result).map(s => s.result)
  return parts.join('\n\n---\n\n')
}

export default {
  AGENT_STATUS,
  createWritingPlan,
  prepareStepPrompt,
  getAgentProgress,
  exportAgentResult,
}
