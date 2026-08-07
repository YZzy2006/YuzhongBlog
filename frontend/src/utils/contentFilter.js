// ===== 敏感词前端预过滤引擎 =====
// 技术：DFA 字典树 + Unicode 全归一化 + 跳符匹配 + 谐音检测
// 用途：即时反馈，后端仍为权威过滤

// ===== 词库（分类管理）=====
const WORD_GROUPS = {
  profanity: [
    '操','操你','我操','操你妈','操蛋','我操你','草泥马','草拟吗','草拟马','草你妈','草他妈',
    '傻逼','傻b','傻B','傻杯','煞笔','沙比','沙币','s逼',
    '妈的','他妈','你妈','你麻痹','尼玛','尼玛币','你妈的','他妈的','妈了个逼',
    '日你','日尼','日死','日了','狗日的','狗日','王八','王八蛋','龟儿子','龟孙','去死','去屎',
    '艹','我艹','艹你','艹尼','艹他妈','艹蛋','卧槽','我操','我擦',
    'fuck','f**k','fuuck','fck','fuk','shit','sh1t','sh!t','damn','d4mn','bitch','btch','ahole',
    '肏','屌','婊','婊子','婊子养的','贱','贱人','贱货','烂货','烂人','垃圾','废物','废柴','智障','脑残',
    '畜生','杂种','狗逼','狗比','狗东西','狗娘养的','狗杂种','狗屎','死全家','死妈','死爹',
    'cnm','cnmb','nmsl','nmb','wqnmlgb','rnm','mlgb','tmd',
  ],
  pinyin: ['cao','shabi','sabi','nimabi','caonima','ganni','rini','tamade','wocaonima','wdnmd'],
  contact: [
    '加微信','加我微信','加v信','加V信','加我v','加我V','加vx','加VX',
    '扫码','加群','看片','约炮','约p','一夜情','裸聊','色情','黄片',
    '加qq','加我QQ','加扣扣','扣扣号','qq号','私聊我','联系我','加我私聊',
    '代办','代开发票','刻章','办证','贷款找我','信用卡','套现','花呗',
    '刷单','传销','中奖','免费领取','兼职','日结','日赚','洗钱','跑分',
  ],
  gambling: ['赌博','赌场','赌钱','赌球','博彩','老虎机','百家乐','捕鱼','下注','投注','棋牌','彩票','六合彩'],
  political: ['反党','反华','台独','港独','藏独','疆独','法轮','六四','天安门','卖国','精日'],
}

// 合并为扁平数组
const BAD_RAW = Object.values(WORD_GROUPS).flat()

// ===== Unicode 字符归一化映射表 =====
const UNICODE_NORM = {
  'ａ':'a','ｂ':'b','ｃ':'c','ｄ':'d','ｅ':'e','ｆ':'f','ｇ':'g','ｈ':'h','ｉ':'i','ｊ':'j','ｋ':'k','ｌ':'l','ｍ':'m',
  'ｎ':'n','ｏ':'o','ｐ':'p','ｑ':'q','ｒ':'r','ｓ':'s','ｔ':'t','ｕ':'u','ｖ':'v','ｗ':'w','ｘ':'x','ｙ':'y','ｚ':'z',
  'Ａ':'a','Ｂ':'b','Ｃ':'c','Ｄ':'d','Ｅ':'e','Ｆ':'f','Ｇ':'g','Ｈ':'h','Ｉ':'i','Ｊ':'j','Ｋ':'k','Ｌ':'l','Ｍ':'m',
  'Ｎ':'n','Ｏ':'o','Ｐ':'p','Ｑ':'q','Ｒ':'r','Ｓ':'s','Ｔ':'t','Ｕ':'u','Ｖ':'v','Ｗ':'w','Ｘ':'x','Ｙ':'y','Ｚ':'z',
  '０':'0','１':'1','２':'2','３':'3','４':'4','５':'5','６':'6','７':'7','８':'8','９':'9',
  '⓪':'0','①':'1','②':'2','③':'3','④':'4','⑤':'5','⑥':'6','⑦':'7','⑧':'8','⑨':'9',
  'ⓐ':'a','ⓑ':'b','ⓒ':'c','ⓓ':'d','ⓔ':'e','ⓕ':'f','ⓖ':'g','ⓗ':'h','ⓘ':'i','ⓙ':'j','ⓚ':'k','ⓛ':'l','ⓜ':'m',
  'ⓝ':'n','ⓞ':'o','ⓟ':'p','ⓠ':'q','ⓡ':'r','ⓢ':'s','ⓣ':'t','ⓤ':'u','ⓥ':'v','ⓦ':'w','ⓧ':'x','ⓨ':'y','ⓩ':'z',
  'а':'a','е':'e','о':'o','р':'p','с':'c','у':'y','х':'x','і':'i','ɑ':'a','ο':'o',
  '@':'a','$':'s','!':'i','¡':'i','|':'l',
}

// 跳符字符（buildSkipPattern 复用此集合）
const SKIP_WORDS = new Set(' \r\n\t　.,;:!?()（）【】[]{}《》<>＂\'"`~@#$%^&*-+=_/\\|~·…×'.split(''))

// 输入长度上限（防 ReDoS / 性能）
const MAX_INPUT_LENGTH = 5000

// 繁简映射
const TRAD_SIMP = {
  '幹':'干','媽':'妈','嗎':'吗','們':'们','門':'门','麼':'么','說':'说','來':'来','對':'对',
  '時':'时','會':'会','個':'个','為':'为','國':'国','這':'这','軍':'军','發':'发','現':'现',
  '後':'后','開':'开','關':'关','長':'长','書':'书','兒':'儿','頭':'头','無':'无','體':'体',
  '機':'机','氣':'气','愛':'爱','電':'电','實':'实','學':'学','點':'点','當':'当','過':'过',
}

// ===== 谐音/拼音映射 =====
const PINYIN_MAP = {
  'cao': '操', 'kao': '靠', 'ri': '日', 'gan': '干', 'diao': '屌', 'bi': '逼', 'sha': '傻',
  'biao': '婊', 'jian': '贱', 'ma': '妈', 'gou': '狗', 'wang': '王', 'gui': '龟',
  'shabi': '傻逼', 'sabi': '傻逼', 'caonima': '操你妈', 'nimabi': '尼玛逼',
  'wocao': '我操', 'tama': '他妈', 'nima': '尼玛', 'gouri': '狗日',
}

/** 归一化单个字符 */
function normChar(ch) {
  if (TRAD_SIMP[ch]) return TRAD_SIMP[ch]
  if (UNICODE_NORM[ch]) return UNICODE_NORM[ch]
  return ch
}

/** 归一化整个字符串 */
function normalize(text) {
  let result = ''
  let prevNorm = ''
  for (const ch of text) {
    if (SKIP_WORDS.has(ch)) continue
    const nc = normChar(ch).toLowerCase()
    if (nc === prevNorm) continue
    result += nc
    prevNorm = nc
  }
  return result
}

/** 构建包含跳符的正则（复用 SKIP_WORDS 集合） */
function buildSkipPattern(word) {
  if (word.length < 2) return word
  const escaped = [...SKIP_WORDS].map(ch => {
    // 正则特殊字符转义
    return ch.replace(/[[\]{}()*+?.\\^$|]/g, '\\$&')
  }).join('')
  const skips = `[${escaped}]{0,2}`
  return word.split('').join(skips)
}

// ===== DFA 字典树 =====
class TrieNode {
  constructor() { this.children = {}; this.isEnd = false; this.word = '' }
}
const trieRoot = new TrieNode()
for (const w of BAD_RAW) {
  let node = trieRoot
  const nw = normalize(w)
  for (const ch of nw) {
    if (!node.children[ch]) node.children[ch] = new TrieNode()
    node = node.children[ch]
  }
  node.isEnd = true; node.word = w
}

/** DFA 匹配 */
function matchDFA(normalizedText) {
  for (let i = 0; i < normalizedText.length; i++) {
    let node = trieRoot
    for (let j = i; j < normalizedText.length; j++) {
      const ch = normalizedText[j]
      if (!node.children[ch]) break
      node = node.children[ch]
      if (node.isEnd) return node.word
    }
  }
  return null
}

// ===== 对外接口 =====

/**
 * 检测敏感内容（7层防御）
 * @param {string} text 用户输入
 * @returns {{ blocked: boolean, message: string, word: string|null }}
 */
export function checkContent(text) {
  if (!text || !text.trim()) return { blocked: false, message: '', word: null }
  if (text.length > MAX_INPUT_LENGTH) return { blocked: true, message: '输入内容过长', word: 'length' }

  const raw = text.toLowerCase()
  const rawNoSpace = raw.replace(/\s/g, '')

  // 1. URL / 邮箱 / 长数字
  if (/https?:\/\/|www\.|\.com|\.cn|\.net|\.top|\.xyz|\.cc|\.vip|\.org|\.io/.test(rawNoSpace))
    return { blocked: true, message: '请勿包含网址链接', word: 'url' }
  if (/@\w+\./.test(rawNoSpace))
    return { blocked: true, message: '请勿包含邮箱地址', word: 'email' }
  if (/\d{13,}/.test(rawNoSpace.replace(/\D/g, '')))
    return { blocked: true, message: '请勿包含长数字串', word: 'number' }

  // 2. 拼音缩写匹配
  for (const w of WORD_GROUPS.pinyin) {
    if (rawNoSpace.includes(w)) return { blocked: true, message: '请文明交流', word: w }
  }

  // 3. 原始文本直接匹配
  for (const w of BAD_RAW) {
    if (rawNoSpace.includes(w.toLowerCase())) return { blocked: true, message: classifyWord(w), word: w }
  }

  // 4. 归一化 + DFA 匹配
  const norm = normalize(text)
  const dfaHit = matchDFA(norm)
  if (dfaHit) return { blocked: true, message: classifyWord(dfaHit), word: dfaHit }

  // 5. 跳符匹配（防 "操@你 妈" 绕过）
  for (const w of BAD_RAW) {
    if (w.length < 2) continue
    const pat = buildSkipPattern(w)
    try { if (new RegExp(pat, 'i').test(rawNoSpace)) return { blocked: true, message: classifyWord(w), word: w } } catch {}
  }

  // 6. 谐音检测
  const letterOnly = rawNoSpace.replace(/[^a-z]/g, '')
  if (letterOnly.length >= 3) {
    for (const [py, ch] of Object.entries(PINYIN_MAP)) {
      if (letterOnly.includes(py) && norm.includes(ch)) return { blocked: true, message: '请文明交流', word: py }
    }
  }

  // 7. 倒写检测
  const reversed = norm.split('').reverse().join('')
  const reverseHit = matchDFA(reversed)
  if (reverseHit) return { blocked: true, message: '请文明交流', word: reverseHit }

  return { blocked: false, message: '', word: null }
}

/** 根据词分类返回提示 */
function classifyWord(word) {
  const w = word.toLowerCase()
  if (WORD_GROUPS.gambling.some(g => w.includes(g))) return '请勿发布赌博相关信息'
  if (WORD_GROUPS.political.some(p => w.includes(p))) return '请遵守法律法规'
  if (WORD_GROUPS.contact.some(c => w.includes(c))) return '请勿发布引流信息'
  return '请文明交流'
}

/** 格式化提示消息 */
export function formatFilterMsg(result) {
  if (!result || !result.blocked) return null
  return result.message
}
