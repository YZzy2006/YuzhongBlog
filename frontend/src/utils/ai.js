import request from './request'

export function checkAiStatus() {
  return request.get('/api/ai/status')
}

export function aiChat(message) {
  return request.post('/api/ai/chat', { message })
}

/**
 * Smart search: parse natural language → structured params → search articles
 * @param {string} query - natural language search query
 * @returns {Promise<object>} - PageResult of articles
 */
export async function smartSearch(query) {
  const parseResult = await request.post('/api/ai/search/parse', { message: query })
  if (!parseResult) return request.get('/api/articles?page=0&size=5&sort=latest')
  const params = new URLSearchParams({ page: 0, size: 5 })
  if (parseResult.keyword) params.set('keyword', parseResult.keyword)
  if (parseResult.categoryId) params.set('categoryId', parseResult.categoryId)
  if (parseResult.tagId) params.set('tagId', parseResult.tagId)
  if (parseResult.sortBy) {
    const sortMap = { newest: 'latest', views: 'popular', likes: 'featured' }
    params.set('sort', sortMap[parseResult.sortBy] || 'latest')
  }
  return request.get(`/api/articles?${params}`)
}

/**
 * Shared SSE stream helper
 * @param {string} url - endpoint URL
 * @param {object} body - request body (will be JSON.stringify)
 * @param {object} callbacks - { onChunk(content), onThinking(content), onDone(), onError(error) }
 * @param {object} [fetchOptions] - extra fetch options (e.g. headers)
 * @returns {function} abort function
 */
function sseStream(url, body, callbacks, fetchOptions = {}) {
  const { onChunk, onDone, onError } = callbacks
  const controller = new AbortController()
  let doneCalled = false

  fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...fetchOptions.headers },
    body: JSON.stringify(body),
    signal: controller.signal
  }).then(async response => {
    if (!response.ok) {
      const text = await response.text()
      throw new Error(text || `HTTP ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed || !trimmed.startsWith('data:')) continue
        const jsonStr = trimmed.slice(5).trim()
        if (!jsonStr) continue

        try {
          const event = JSON.parse(jsonStr)
          if (event.type === 'chunk' && event.content) {
            onChunk(event.content)
          } else if (event.type === 'thinking' && event.content) {
            if (callbacks.onThinking) callbacks.onThinking(event.content)
          } else if (event.type === 'done') {
            doneCalled = true
            onDone()
          } else if (event.type === 'error') {
            doneCalled = true
            onError(new Error(event.content))
          }
        } catch {
          // ignore parse errors
        }
      }
    }

    if (!doneCalled) {
      doneCalled = true
      onDone()
    }
  }).catch(err => {
    if (err.name !== 'AbortError' && !doneCalled) {
      onError(err)
    }
  })

  return () => controller.abort()
}

/**
 * SSE streaming chat, returns abort function
 * @param {string|Array} messageOrMessages - single message string or messages array [{role, content}]
 * @param {object} callbacks - { onChunk(content), onDone(), onError(error) }
 * @returns {function} abort function
 */
export function aiChatStream(messageOrMessages, callbacks) {
  const body = Array.isArray(messageOrMessages)
    ? { messages: messageOrMessages }
    : { message: messageOrMessages }
  return sseStream('/api/ai/chat/stream', body, callbacks)
}

/**
 * SSE streaming for pet chat (multi-turn), returns abort function
 * @param {Array} messages - [{role, content}]
 * @param {object} callbacks - { onChunk(content), onDone(), onError(error) }
 * @returns {function} abort function
 */
export function petChatStream(messages, callbacks) {
  return sseStream('/api/ai/pet-chat/stream', { messages }, callbacks)
}

/**
 * SSE streaming for editor AI, supports multi-turn + custom system prompt + maxTokens
 * @param {object} options - { messages: [{role, content}], systemPrompt: string, maxTokens: number }
 * @param {object} callbacks - { onChunk(content), onThinking(content), onDone(), onError(error) }
 * @returns {function} abort function
 */
export function aiEditorStream({ messages, systemPrompt, maxTokens }, callbacks) {
  const token = localStorage.getItem('accessToken') || ''
  return sseStream('/admin/ai/editor/stream', { messages, systemPrompt, maxTokens }, callbacks, {
    headers: { Authorization: `Bearer ${token}` }
  })
}
