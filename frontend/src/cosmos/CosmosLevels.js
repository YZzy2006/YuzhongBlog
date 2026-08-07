/**
 * RPG Level / XP system for the Cosmos page
 */

const STORAGE_KEY = 'cosmos-profile'

// Level names by tier (10 tiers, 3 levels each)
export const LEVEL_TIERS = [
  { min: 1, max: 3, tier: 0 },
  { min: 4, max: 6, tier: 1 },
  { min: 7, max: 9, tier: 2 },
  { min: 10, max: 12, tier: 3 },
  { min: 13, max: 15, tier: 4 },
  { min: 16, max: 18, tier: 5 },
  { min: 19, max: 21, tier: 6 },
  { min: 22, max: 24, tier: 7 },
  { min: 25, max: 27, tier: 8 },
  { min: 28, max: 30, tier: 9 }
]

// XP needed for a given level
export function xpForLevel(lvl) {
  if (lvl <= 1) return 150
  return 150 + Math.floor((2000 * (lvl - 1)) / ((lvl - 1) + 10))
}

// Total XP to reach level N
export function totalXpForLevel(lvl) {
  let total = 0
  for (let i = 1; i < lvl; i++) {
    total += xpForLevel(i)
  }
  return total
}

// Calculate level from total XP
export function levelFromXP(totalXP) {
  let level = 1
  let remaining = totalXP
  while (remaining >= xpForLevel(level) && level < 30) {
    remaining -= xpForLevel(level)
    level++
  }
  return {
    level,
    currentXP: remaining,
    neededXP: xpForLevel(level),
    percentage: Math.min(100, Math.floor((remaining / xpForLevel(level)) * 100))
  }
}

// Get tier index for a level
export function getTier(level) {
  for (const t of LEVEL_TIERS) {
    if (level >= t.min && level <= t.max) return t.tier
  }
  return 0
}

// ─── Rarity Configuration ───────────────────────────────
export const RARITY_CONFIG = {
  common:    { color: '#9ca3af', glow: 'rgba(156,163,175,0.3)', i18nKey: 'rarityCommon' },
  uncommon:  { color: '#22c55e', glow: 'rgba(34,197,94,0.4)', i18nKey: 'rarityUncommon' },
  rare:      { color: '#3b82f6', glow: 'rgba(59,130,246,0.5)', i18nKey: 'rarityRare' },
  epic:      { color: '#a855f7', glow: 'rgba(168,85,247,0.5)', i18nKey: 'rarityEpic' },
  legendary: { color: '#eab308', glow: 'rgba(234,179,8,0.6)', i18nKey: 'rarityLegendary' }
}

// Badge categories for Memory Codex
export const BADGE_CATEGORIES = [
  { id: 'level', icon: '⭐', i18nKey: 'catLevel' },
  { id: 'article', icon: '📖', i18nKey: 'catArticle' },
  { id: 'project', icon: '🚀', i18nKey: 'catProject' },
  { id: 'moment', icon: '📸', i18nKey: 'catMoment' },
  { id: 'album', icon: '🖼️', i18nKey: 'catAlbum' },
  { id: 'song', icon: '🎵', i18nKey: 'catSong' },
  { id: 'notice', icon: '📢', i18nKey: 'catNotice' },
  { id: 'special', icon: '✨', i18nKey: 'catSpecial' }
]

// Badge definitions — 88 achievements across 8 categories
export const BADGE_DEFS = [
  // ─── Level (10) ─────────────────────────────────────
  { id: 'lvl-2', icon: '⭐', group: 'level', rarity: 'common', chain: 'level', condition: (s) => s.level >= 2 },
  { id: 'lvl-5', icon: '🌟', group: 'level', rarity: 'uncommon', chain: 'level', condition: (s) => s.level >= 5 },
  { id: 'lvl-8', icon: '💫', group: 'level', rarity: 'uncommon', chain: 'level', condition: (s) => s.level >= 8 },
  { id: 'lvl-10', icon: '🔥', group: 'level', rarity: 'rare', chain: 'level', condition: (s) => s.level >= 10 },
  { id: 'lvl-13', icon: '⚔️', group: 'level', rarity: 'rare', chain: 'level', condition: (s) => s.level >= 13 },
  { id: 'lvl-15', icon: '🛡️', group: 'level', rarity: 'rare', chain: 'level', condition: (s) => s.level >= 15 },
  { id: 'lvl-18', icon: '💎', group: 'level', rarity: 'epic', chain: 'level', condition: (s) => s.level >= 18 },
  { id: 'lvl-20', icon: '👑', group: 'level', rarity: 'epic', chain: 'level', condition: (s) => s.level >= 20 },
  { id: 'lvl-25', icon: '🌌', group: 'level', rarity: 'epic', chain: 'level', condition: (s) => s.level >= 25 },
  { id: 'lvl-30', icon: '🏆', group: 'level', rarity: 'legendary', chain: 'level', condition: (s) => s.level >= 30 },

  // ─── Articles (12) ──────────────────────────────────
  { id: 'art-1', icon: '✒️', group: 'article', rarity: 'common', chain: 'article', condition: (s) => s.articles >= 1 },
  { id: 'art-5', icon: '📝', group: 'article', rarity: 'uncommon', chain: 'article', condition: (s) => s.articles >= 5 },
  { id: 'art-10', icon: '📜', group: 'article', rarity: 'uncommon', chain: 'article', condition: (s) => s.articles >= 10 },
  { id: 'art-20', icon: '📖', group: 'article', rarity: 'rare', chain: 'article', condition: (s) => s.articles >= 20 },
  { id: 'art-30', icon: '📚', group: 'article', rarity: 'rare', chain: 'article', condition: (s) => s.articles >= 30 },
  { id: 'art-50', icon: '🎓', group: 'article', rarity: 'epic', chain: 'article', condition: (s) => s.articles >= 50 },
  { id: 'art-100', icon: '📕', group: 'article', rarity: 'epic', chain: 'article', condition: (s) => s.articles >= 100 },
  { id: 'art-200', icon: '✍️', group: 'article', rarity: 'epic', chain: 'article', condition: (s) => s.articles >= 200 },
  { id: 'art-300', icon: '📘', group: 'article', rarity: 'legendary', chain: 'article', condition: (s) => s.articles >= 300 },
  { id: 'art-500', icon: '🏛️', group: 'article', rarity: 'legendary', chain: 'article', condition: (s) => s.articles >= 500 },
  { id: 'art-1000', icon: '🏴‍☠️', group: 'article', rarity: 'legendary', chain: 'article', condition: (s) => s.articles >= 1000 },


  // ─── Projects (10) ──────────────────────────────────
  { id: 'proj-1', icon: '🔧', group: 'project', rarity: 'common', chain: 'project', condition: (s) => s.projects >= 1 },
  { id: 'proj-3', icon: '⚙️', group: 'project', rarity: 'uncommon', chain: 'project', condition: (s) => s.projects >= 3 },
  { id: 'proj-5', icon: '🏗️', group: 'project', rarity: 'uncommon', chain: 'project', condition: (s) => s.projects >= 5 },
  { id: 'proj-8', icon: '🖥️', group: 'project', rarity: 'rare', chain: 'project', condition: (s) => s.projects >= 8 },
  { id: 'proj-10', icon: '🚀', group: 'project', rarity: 'rare', chain: 'project', condition: (s) => s.projects >= 10 },
  { id: 'proj-15', icon: '🛸', group: 'project', rarity: 'rare', chain: 'project', condition: (s) => s.projects >= 15 },
  { id: 'proj-20', icon: '🏰', group: 'project', rarity: 'epic', chain: 'project', condition: (s) => s.projects >= 20 },
  { id: 'proj-30', icon: '🌍', group: 'project', rarity: 'epic', chain: 'project', condition: (s) => s.projects >= 30 },
  { id: 'proj-50', icon: '🌌', group: 'project', rarity: 'legendary', chain: 'project', condition: (s) => s.projects >= 50 },
  { id: 'proj-100', icon: '🏆', group: 'project', rarity: 'legendary', chain: 'project', condition: (s) => s.projects >= 100 },

  // ─── Moments (12) ───────────────────────────────────
  { id: 'mom-1', icon: '📸', group: 'moment', rarity: 'common', chain: 'moment', condition: (s) => s.moments >= 1 },
  { id: 'mom-5', icon: '🎬', group: 'moment', rarity: 'uncommon', chain: 'moment', condition: (s) => s.moments >= 5 },
  { id: 'mom-10', icon: '🌅', group: 'moment', rarity: 'uncommon', chain: 'moment', condition: (s) => s.moments >= 10 },
  { id: 'mom-20', icon: '🎨', group: 'moment', rarity: 'rare', chain: 'moment', condition: (s) => s.moments >= 20 },
  { id: 'mom-50', icon: '🌈', group: 'moment', rarity: 'rare', chain: 'moment', condition: (s) => s.moments >= 50 },
  { id: 'mom-100', icon: '🎪', group: 'moment', rarity: 'epic', chain: 'moment', condition: (s) => s.moments >= 100 },
  { id: 'mom-200', icon: '🎭', group: 'moment', rarity: 'epic', chain: 'moment', condition: (s) => s.moments >= 200 },
  { id: 'mom-500', icon: '🎬', group: 'moment', rarity: 'epic', chain: 'moment', condition: (s) => s.moments >= 500 },
  { id: 'mom-1000', icon: '🌟', group: 'moment', rarity: 'legendary', chain: 'moment', condition: (s) => s.moments >= 1000 },
  { id: 'mom-night', icon: '🌙', group: 'moment', rarity: 'uncommon', chain: null, condition: (s) => s.nightMoments >= 10 },
  { id: 'mom-weekend', icon: '🎉', group: 'moment', rarity: 'uncommon', chain: null, condition: (s) => s.weekendMoments >= 20 },
  { id: 'mom-hot', icon: '🔥', group: 'moment', rarity: 'rare', chain: null, condition: (s) => s.hotMoments >= 5 },

  // ─── Albums (8) ─────────────────────────────────────
  { id: 'alb-1', icon: '🖼️', group: 'album', rarity: 'common', chain: 'album', condition: (s) => s.albums >= 1 },
  { id: 'alb-3', icon: '🎨', group: 'album', rarity: 'uncommon', chain: 'album', condition: (s) => s.albums >= 3 },
  { id: 'alb-5', icon: '📷', group: 'album', rarity: 'uncommon', chain: 'album', condition: (s) => s.albums >= 5 },
  { id: 'alb-10', icon: '🏞️', group: 'album', rarity: 'rare', chain: 'album', condition: (s) => s.albums >= 10 },
  { id: 'alb-20', icon: '🌄', group: 'album', rarity: 'rare', chain: 'album', condition: (s) => s.albums >= 20 },
  { id: 'alb-30', icon: '🎪', group: 'album', rarity: 'epic', chain: 'album', condition: (s) => s.albums >= 30 },
  { id: 'alb-50', icon: '🌍', group: 'album', rarity: 'epic', chain: 'album', condition: (s) => s.albums >= 50 },
  { id: 'alb-100', icon: '🏆', group: 'album', rarity: 'legendary', chain: 'album', condition: (s) => s.albums >= 100 },

  // ─── Songs (8) ──────────────────────────────────────
  { id: 'song-1', icon: '🎵', group: 'song', rarity: 'common', chain: 'song', condition: (s) => s.songs >= 1 },
  { id: 'song-5', icon: '🎶', group: 'song', rarity: 'uncommon', chain: 'song', condition: (s) => s.songs >= 5 },
  { id: 'song-10', icon: '🎸', group: 'song', rarity: 'uncommon', chain: 'song', condition: (s) => s.songs >= 10 },
  { id: 'song-20', icon: '🎹', group: 'song', rarity: 'rare', chain: 'song', condition: (s) => s.songs >= 20 },
  { id: 'song-30', icon: '🎷', group: 'song', rarity: 'rare', chain: 'song', condition: (s) => s.songs >= 30 },
  { id: 'song-50', icon: '🎺', group: 'song', rarity: 'epic', chain: 'song', condition: (s) => s.songs >= 50 },
  { id: 'song-100', icon: '🎻', group: 'song', rarity: 'epic', chain: 'song', condition: (s) => s.songs >= 100 },
  { id: 'song-200', icon: '🎼', group: 'song', rarity: 'legendary', chain: 'song', condition: (s) => s.songs >= 200 },

  // ─── Notices (8) ────────────────────────────────────
  { id: 'ntc-1', icon: '📢', group: 'notice', rarity: 'common', chain: 'notice', condition: (s) => s.notices >= 1 },
  { id: 'ntc-3', icon: '📣', group: 'notice', rarity: 'uncommon', chain: 'notice', condition: (s) => s.notices >= 3 },
  { id: 'ntc-5', icon: '📬', group: 'notice', rarity: 'uncommon', chain: 'notice', condition: (s) => s.notices >= 5 },
  { id: 'ntc-10', icon: '🔔', group: 'notice', rarity: 'rare', chain: 'notice', condition: (s) => s.notices >= 10 },
  { id: 'ntc-15', icon: '📡', group: 'notice', rarity: 'rare', chain: 'notice', condition: (s) => s.notices >= 15 },
  { id: 'ntc-20', icon: '📺', group: 'notice', rarity: 'epic', chain: 'notice', condition: (s) => s.notices >= 20 },
  { id: 'ntc-30', icon: '🌐', group: 'notice', rarity: 'epic', chain: 'notice', condition: (s) => s.notices >= 30 },
  { id: 'ntc-50', icon: '🏆', group: 'notice', rarity: 'legendary', chain: 'notice', condition: (s) => s.notices >= 50 },

  // ─── Special (20) ───────────────────────────────────
  { id: 'visit-7', icon: '🗓️', group: 'special', rarity: 'common', chain: 'visit', condition: (s) => s.visitDays >= 7 },
  { id: 'visit-30', icon: '📅', group: 'special', rarity: 'uncommon', chain: 'visit', condition: (s) => s.visitDays >= 30 },
  { id: 'visit-100', icon: '🎯', group: 'special', rarity: 'rare', chain: 'visit', condition: (s) => s.visitDays >= 100 },
  { id: 'visit-365', icon: '🏅', group: 'special', rarity: 'epic', chain: 'visit', condition: (s) => s.visitDays >= 365 },
  { id: 'views-1k', icon: '👁️', group: 'special', rarity: 'rare', chain: 'views', condition: (s) => s.totalViews >= 1000 },
  { id: 'views-10k', icon: '👀', group: 'special', rarity: 'epic', chain: 'views', condition: (s) => s.totalViews >= 10000 },
  { id: 'views-100k', icon: '🔭', group: 'special', rarity: 'legendary', chain: 'views', condition: (s) => s.totalViews >= 100000 },
  { id: 'views-1m', icon: '🌌', group: 'special', rarity: 'legendary', chain: 'views', condition: (s) => s.totalViews >= 1000000 },
  { id: 'cat-5', icon: '📂', group: 'special', rarity: 'uncommon', chain: 'cat', condition: (s) => s.categories >= 5 },
  { id: 'cat-10', icon: '📁', group: 'special', rarity: 'rare', chain: 'cat', condition: (s) => s.categories >= 10 },
  { id: 'tag-10', icon: '🏷️', group: 'special', rarity: 'uncommon', chain: 'tag', condition: (s) => s.tags >= 10 },
  { id: 'tag-30', icon: '🔖', group: 'special', rarity: 'rare', chain: 'tag', condition: (s) => s.tags >= 30 },
  { id: 'tag-50', icon: '📌', group: 'special', rarity: 'epic', chain: 'tag', condition: (s) => s.tags >= 50 },
  { id: 'full-stack', icon: '🧩', group: 'special', rarity: 'legendary', chain: null, condition: (s) => s.articles > 0 && s.projects > 0 && s.moments > 0 && s.albums > 0 && s.songs > 0 && s.notices > 0 },
  { id: 'century', icon: '💯', group: 'special', rarity: 'legendary', chain: null, condition: (s) => (s.articles + s.projects + s.moments + s.albums + s.songs + s.notices) >= 100 },
  { id: 'millennium', icon: '🎆', group: 'special', rarity: 'legendary', chain: null, condition: (s) => (s.articles + s.projects + s.moments + s.albums + s.songs + s.notices) >= 1000 },
  { id: 'early-bird', icon: '🐦', group: 'special', rarity: 'epic', chain: null, condition: (s) => s.earlyPosts >= 10 },
  { id: 'night-owl', icon: '🦉', group: 'special', rarity: 'epic', chain: null, condition: (s) => s.latePosts >= 10 },
  { id: 'weekend-warrior', icon: '⚔️', group: 'special', rarity: 'epic', chain: null, condition: (s) => s.weekendPosts >= 20 },
  { id: 'collector', icon: '🧲', group: 'special', rarity: 'legendary', chain: null, condition: (s) => s.categories >= 5 && s.tags >= 20 && s.articles >= 30 },

  // ─── Streak (10) ────────────────────────────────────
  { id: 'art-streak-3', icon: '🕯️', group: 'special', rarity: 'common', chain: 'art-streak', condition: (s) => s.articleStreak >= 3 },
  { id: 'art-streak-7', icon: '🔥', group: 'special', rarity: 'uncommon', chain: 'art-streak', condition: (s) => s.articleStreak >= 7 },
  { id: 'art-streak-14', icon: '⚡', group: 'special', rarity: 'rare', chain: 'art-streak', condition: (s) => s.articleStreak >= 14 },
  { id: 'art-streak-30', icon: '🌊', group: 'special', rarity: 'epic', chain: 'art-streak', condition: (s) => s.articleStreak >= 30 },
  { id: 'art-streak-100', icon: '☀️', group: 'special', rarity: 'legendary', chain: 'art-streak', condition: (s) => s.articleStreak >= 100 },
  { id: 'post-streak-3', icon: '✨', group: 'special', rarity: 'common', chain: 'post-streak', condition: (s) => s.postStreak >= 3 },
  { id: 'post-streak-7', icon: '☄️', group: 'special', rarity: 'uncommon', chain: 'post-streak', condition: (s) => s.postStreak >= 7 },
  { id: 'post-streak-14', icon: '⚡', group: 'special', rarity: 'rare', chain: 'post-streak', condition: (s) => s.postStreak >= 14 },
  { id: 'post-streak-30', icon: '🌊', group: 'special', rarity: 'epic', chain: 'post-streak', condition: (s) => s.postStreak >= 30 },
  { id: 'post-streak-100', icon: '🌟', group: 'special', rarity: 'legendary', chain: 'post-streak', condition: (s) => s.postStreak >= 100 },

  // ─── Exploration (5) ──────────────────────────────────
  { id: 'explore-10', icon: '🧭', group: 'special', rarity: 'common', chain: 'explore', condition: (s) => s.uniqueNodesVisited >= 10 },
  { id: 'explore-50', icon: '🗺️', group: 'special', rarity: 'uncommon', chain: 'explore', condition: (s) => s.uniqueNodesVisited >= 50 },
  { id: 'explore-100', icon: '🌍', group: 'special', rarity: 'rare', chain: 'explore', condition: (s) => s.uniqueNodesVisited >= 100 },
  { id: 'explore-all', icon: '🗺️', group: 'special', rarity: 'epic', chain: null, condition: (s) => s.typesVisited >= 6 },
  { id: 'deep-reader', icon: '🔬', group: 'special', rarity: 'rare', chain: null, condition: (s) => s.uniqueNodesVisited >= 30 && s.totalViews >= 5000 },

  // ─── Combo (2) ─────────────────────────────────────
  { id: 'triple', icon: '🔗', group: 'special', rarity: 'uncommon', chain: null, condition: (s) => s.maxTypesPerDay >= 3 },
  { id: 'penta', icon: '⭐', group: 'special', rarity: 'rare', chain: null, condition: (s) => s.maxTypesPerDay >= 5 },

  // ─── Holiday (4) ───────────────────────────────────
  { id: 'new-year', icon: '🎇', group: 'special', rarity: 'epic', chain: null, condition: (s) => s.holidayPost?.newYear },
  { id: 'valentine', icon: '❤️', group: 'special', rarity: 'rare', chain: null, condition: (s) => s.holidayPost?.valentine },
  { id: 'halloween', icon: '🎃', group: 'special', rarity: 'rare', chain: null, condition: (s) => s.holidayPost?.halloween },
  { id: 'christmas', icon: '🎄', group: 'special', rarity: 'rare', chain: null, condition: (s) => s.holidayPost?.christmas },

  // ─── Milestone (2) ─────────────────────────────────
  { id: 'first-blood', icon: '⚔️', group: 'special', rarity: 'common', chain: null, condition: (s) => s.articles >= 1 || s.projects >= 1 || s.moments >= 1 },
  { id: 'comeback', icon: '🔄', group: 'special', rarity: 'uncommon', chain: null, condition: (s) => s.comeback }
]

// Pre-computed evolution chains: chainId → [badgeIds] in order
export const EVOLUTION_CHAINS = {}
for (const def of BADGE_DEFS) {
  if (def.chain) {
    if (!EVOLUTION_CHAINS[def.chain]) EVOLUTION_CHAINS[def.chain] = []
    EVOLUTION_CHAINS[def.chain].push(def.id)
  }
}

// Get evolution info for a badge within its chain
export function getEvolutionInfo(badgeId, unlockedBadges) {
  const def = BADGE_DEFS.find(b => b.id === badgeId)
  if (!def || !def.chain) return null
  const chain = EVOLUTION_CHAINS[def.chain]
  if (!chain) return null
  const total = chain.length
  const current = chain.indexOf(badgeId) + 1
  // Find next unearned badge in chain
  const nextIdx = chain.findIndex((id, i) => i >= current && !unlockedBadges.includes(id))
  const nextId = nextIdx >= 0 ? chain[nextIdx] : null
  return { current, total, nextId, chainId: def.chain }
}

// Get the highest unlocked badge in a chain
export function getHighestInChain(chainId, unlockedBadges) {
  const chain = EVOLUTION_CHAINS[chainId]
  if (!chain) return null
  let highest = null
  for (const id of chain) {
    if (unlockedBadges.includes(id)) highest = id
  }
  return highest
}

// Get all badge IDs in a chain that are "absorbed" (not the highest)
export function getAbsorbedBadges(chainId, unlockedBadges) {
  const highest = getHighestInChain(chainId, unlockedBadges)
  if (!highest) return []
  const chain = EVOLUTION_CHAINS[chainId]
  const highestIdx = chain.indexOf(highest)
  return chain.slice(0, highestIdx).filter(id => unlockedBadges.includes(id))
}

// Load profile from localStorage
export function loadProfile() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) return JSON.parse(stored)
  } catch { /* ignore */ }
  return { xp: 0, badges: [], lastVisit: null, visitedNodes: [], visitDays: [] }
}

// Save profile to localStorage
export function saveProfile(profile) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(profile))
  } catch { /* ignore */ }
}

// Check and award badges
export function checkBadges(profile, stats) {
  const newBadges = []
  for (const def of BADGE_DEFS) {
    if (!profile.badges.includes(def.id) && def.condition(stats)) {
      profile.badges.push(def.id)
      newBadges.push(def)
    }
  }
  return newBadges
}

// Record daily visit
export function recordVisit(profile) {
  const today = new Date().toISOString().slice(0, 10)
  if (!Array.isArray(profile.visitDays)) profile.visitDays = []
  if (!profile.visitDays.includes(today)) {
    profile.visitDays.push(today)
    if (profile.visitDays.length > 365) {
      profile.visitDays = profile.visitDays.slice(-365)
    }
    profile.xp += 20
  }
  profile.lastVisit = today
  saveProfile(profile)
}
