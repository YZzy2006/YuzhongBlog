import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const LOCALES_DIR = path.resolve(__dirname, '../src/i18n/locales')
const ZH_PATH = path.join(LOCALES_DIR, 'zh-CN.js')
const EN_PATH = path.join(LOCALES_DIR, 'en-US.js')

// --- env ---
function loadEnv() {
  const envPath = path.resolve(__dirname, '../.env')
  if (!fs.existsSync(envPath)) return {}
  const env = {}
  for (const line of fs.readFileSync(envPath, 'utf-8').split('\n')) {
    const m = line.match(/^\s*([\w]+)\s*=\s*(.*?)\s*$/)
    if (m) env[m[1]] = m[2]
  }
  return env
}

const env = loadEnv()
// Backend AI endpoint (uses admin-configured AI provider)
const BACKEND_URL = process.env.BACKEND_URL || env.BACKEND_URL || 'http://localhost:8080'
// Fallback: direct API call
const API_KEY = process.env.XIAOMIMIMO_API_KEY || env.XIAOMIMIMO_API_KEY
const BASE_URL = process.env.AI_BASE_URL || env.AI_BASE_URL || 'https://token-plan-cn.xiaomimimo.com/v1'
const MODEL = process.env.AI_MODEL || env.AI_MODEL || 'mimo-v2.5'

// --- locale file helpers ---

async function loadLocale(filePath) {
  const content = fs.readFileSync(filePath, 'utf-8')
  const match = content.match(/export\s+default\s+(\{[\s\S]*\})\s*;?\s*$/)
  if (!match) throw new Error(`Cannot parse locale file: ${filePath}`)
  const fn = new Function('return ' + match[1])
  return fn()
}

function writeLocale(filePath, obj) {
  const lines = []
  lines.push('export default {')
  serializeObject(obj, lines, 1)
  lines.push('}')
  fs.writeFileSync(filePath, lines.join('\n') + '\n', 'utf-8')
}

function quoteKey(key) {
  // Quote keys that contain hyphens, spaces, or start with digits
  if (/[^a-zA-Z0-9_$]/.test(key) || /^\d/.test(key)) {
    return JSON.stringify(key)
  }
  return key
}

function serializeObject(obj, lines, depth) {
  const indent = '  '.repeat(depth)
  const keys = Object.keys(obj)
  keys.forEach((key, i) => {
    const val = obj[key]
    const comma = i < keys.length - 1 ? ',' : ''
    const k = quoteKey(key)
    if (typeof val === 'object' && val !== null && !Array.isArray(val)) {
      lines.push(`${indent}${k}: {`)
      serializeObject(val, lines, depth + 1)
      lines.push(`${indent}}${comma}`)
    } else if (typeof val === 'string') {
      const escaped = JSON.stringify(val)
      lines.push(`${indent}${k}: ${escaped}${comma}`)
    } else {
      lines.push(`${indent}${k}: ${JSON.stringify(val)}${comma}`)
    }
  })
}

// --- translation source tracking ---

const META_PATH = path.join(LOCALES_DIR, '.translation-meta.json')

function loadTranslationMeta() {
  if (!fs.existsSync(META_PATH)) return {}
  try {
    return JSON.parse(fs.readFileSync(META_PATH, 'utf-8'))
  } catch {
    return {}
  }
}

function saveTranslationMeta(meta) {
  fs.writeFileSync(META_PATH, JSON.stringify(meta, null, 2) + '\n', 'utf-8')
}

function seedMeta(zhObj, enObj, prefix = '') {
  const meta = {}
  for (const key of Object.keys(zhObj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key
    const zhVal = zhObj[key]
    const enVal = enObj?.[key]
    if (typeof zhVal === 'object' && zhVal !== null && !Array.isArray(zhVal)) {
      Object.assign(meta, seedMeta(zhVal, enVal || {}, fullKey))
    } else if (typeof zhVal === 'string' && typeof enVal === 'string' && zhVal !== enVal) {
      meta[fullKey] = zhVal
    }
  }
  return meta
}

// --- diff: find keys that need translation ---

function needsTranslation(zhVal, enVal, key, meta) {
  if (enVal === undefined || enVal === null) return true
  if (typeof zhVal === 'string' && typeof enVal === 'string') {
    if (zhVal === enVal) return true
    const prevZh = meta[key]
    if (prevZh !== undefined && prevZh !== zhVal) return true
    return false
  }
  return false
}

function collectMissingKeys(zhObj, enObj, meta, prefix = '') {
  const missing = []
  for (const key of Object.keys(zhObj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key
    const zhVal = zhObj[key]
    const enVal = enObj?.[key]
    if (typeof zhVal === 'object' && zhVal !== null && !Array.isArray(zhVal)) {
      missing.push(...collectMissingKeys(zhVal, enVal || {}, meta, fullKey))
    } else if (typeof zhVal === 'string') {
      if (needsTranslation(zhVal, enVal, fullKey, meta)) {
        missing.push({ key: fullKey, zh: zhVal })
      }
    }
  }
  return missing
}

function setNestedValue(obj, keyPath, value) {
  const parts = keyPath.split('.')
  let cur = obj
  for (let i = 0; i < parts.length - 1; i++) {
    if (!cur[parts[i]] || typeof cur[parts[i]] !== 'object') cur[parts[i]] = {}
    cur = cur[parts[i]]
  }
  cur[parts[parts.length - 1]] = value
}

// --- skip logic ---

function shouldSkip(val) {
  if (!val || val.length < 2) return true
  if (/^#[0-9a-f]{3,8}$/i.test(val)) return true
  if (/^\d+(\.\d+)?(%|px|rem|em)?$/i.test(val)) return true
  if (/^[A-Z][a-zA-Z]+$/.test(val) && val.length < 20) return true
  return false
}

// --- AI translation ---

function buildTranslatePrompt(items) {
  const lines = items.map((item, i) => `${i + 1}. ${item.zh}`)
  return {
    user: `Translate these Chinese strings to English. Output ONLY a JSON object mapping the number to the translation. Keep HTML tags, {placeholders}, and line breaks unchanged. Do not add quotes or explanations.\n\n${lines.join('\n')}`,
    system: 'You are a professional translator. Output only valid JSON. Keep HTML tags and {interpolation} placeholders exactly as-is.'
  }
}

function parseTranslationResponse(content, items) {
  const jsonMatch = content.match(/\{[\s\S]*\}/)
  if (!jsonMatch) {
    console.log('[i18n-sync] Could not parse translation response:', content.slice(0, 200))
    return {}
  }

  let translations
  try {
    translations = JSON.parse(jsonMatch[0])
  } catch {
    console.log('[i18n-sync] Invalid JSON in translation response')
    return {}
  }

  const result = {}
  items.forEach((item, i) => {
    const translated = translations[String(i + 1)]
    if (translated && typeof translated === 'string') {
      result[item.key] = translated
    }
  })
  return result
}

// Try backend AI endpoint first (uses admin-configured provider)
async function translateViaBackend(items) {
  const prompt = buildTranslatePrompt(items)
  const msg = `${prompt.system}\n\n${prompt.user}`

  const resp = await fetch(`${BACKEND_URL}/api/ai/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message: msg })
  })

  if (!resp.ok) {
    const text = await resp.text()
    throw new Error(`Backend AI error ${resp.status}: ${text}`)
  }

  const data = await resp.json()
  const content = data.data?.response || data.response || ''
  return parseTranslationResponse(content, items)
}

// Fallback: direct API call
async function translateViaDirectAPI(items) {
  if (!API_KEY) return null

  const prompt = buildTranslatePrompt(items)
  const body = {
    model: MODEL,
    messages: [
      { role: 'system', content: prompt.system },
      { role: 'user', content: prompt.user }
    ],
    temperature: 0.1,
    max_tokens: 4096
  }

  const url = `${BASE_URL.replace(/\/+$/, '')}/chat/completions`

  const resp = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${API_KEY}`
    },
    body: JSON.stringify(body)
  })

  if (!resp.ok) {
    const text = await resp.text()
    throw new Error(`Direct API error ${resp.status}: ${text}`)
  }

  const data = await resp.json()
  const content = data.choices?.[0]?.message?.content || ''
  return parseTranslationResponse(content, items)
}

async function translateBatch(items) {
  if (!items.length) return {}

  console.log(`[i18n-sync] Translating ${items.length} keys...`)

  // Strategy 1: Use backend AI (supports all configured providers)
  try {
    const result = await translateViaBackend(items)
    if (Object.keys(result).length > 0) {
      console.log(`[i18n-sync] Translated via backend AI`)
      return result
    }
  } catch (e) {
    console.log(`[i18n-sync] Backend AI unavailable: ${e.message}`)
  }

  // Strategy 2: Direct API fallback
  try {
    const result = await translateViaDirectAPI(items)
    if (result && Object.keys(result).length > 0) {
      console.log(`[i18n-sync] Translated via direct API (${MODEL})`)
      return result
    }
  } catch (e) {
    console.log(`[i18n-sync] Direct API failed: ${e.message}`)
  }

  console.log('[i18n-sync] No translation source available. Start backend or set XIAOMIMIMO_API_KEY in .env')
  return {}
}

// --- main ---

export async function syncI18n() {
  console.log('[i18n-sync] Checking zh-CN vs en-US...')

  const zh = await loadLocale(ZH_PATH)
  const en = await loadLocale(EN_PATH)
  let meta = loadTranslationMeta()

  if (Object.keys(meta).length === 0) {
    console.log('[i18n-sync] First run, seeding translation meta...')
    meta = seedMeta(zh, en)
    saveTranslationMeta(meta)
  }

  const missing = collectMissingKeys(zh, en, meta).filter(item => !shouldSkip(item.zh))

  if (missing.length === 0) {
    console.log('[i18n-sync] en-US is up to date, no translation needed')
    return
  }

  console.log(`[i18n-sync] Found ${missing.length} keys needing translation`)

  const BATCH_SIZE = 30
  const allTranslations = {}

  for (let i = 0; i < missing.length; i += BATCH_SIZE) {
    const batch = missing.slice(i, i + BATCH_SIZE)
    try {
      const result = await translateBatch(batch)
      Object.assign(allTranslations, result)
    } catch (e) {
      console.error(`[i18n-sync] Batch translation failed:`, e.message)
    }
  }

  if (Object.keys(allTranslations).length === 0) {
    console.log('[i18n-sync] No translations produced')
    return
  }

  for (const [keyPath, translated] of Object.entries(allTranslations)) {
    setNestedValue(en, keyPath, translated)
    const zhItem = missing.find(m => m.key === keyPath)
    if (zhItem) meta[keyPath] = zhItem.zh
  }

  writeLocale(EN_PATH, en)
  saveTranslationMeta(meta)

  console.log(`[i18n-sync] Updated en-US.js with ${Object.keys(allTranslations).length} translations`)
}

// Run if called directly
if (process.argv[1] && process.argv[1].includes('sync-i18n')) {
  syncI18n().catch(e => {
    console.error('[i18n-sync] Fatal error:', e)
    process.exit(1)
  })
}
