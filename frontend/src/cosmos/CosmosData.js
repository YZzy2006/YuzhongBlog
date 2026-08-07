import request from '../utils/request'

// Seeded PRNG for deterministic orbit positions (mulberry32)
function seededRandom(seed) {
  let t = seed + 0x6d2b79f5
  t = Math.imul(t ^ (t >>> 15), t | 1)
  t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296
}

const PLANET_CONFIG = [
  { id: 'articles', key: 'planetArticles', type: 'article', color: '#3b82f6', endpoint: '/api/articles', isPaginated: true, linkPrefix: '/articles', slugField: 'slug' },
  { id: 'projects', key: 'planetProjects', type: 'project', color: '#22c55e', endpoint: '/api/projects', isPaginated: false, linkPrefix: '/projects', slugField: 'id' },
  { id: 'moments', key: 'planetMoments', type: 'moment', color: '#f59e0b', endpoint: '/api/timeline-entries', isPaginated: false, linkPrefix: '/moments', slugField: 'id' },
  { id: 'albums', key: 'planetAlbums', type: 'album', color: '#ec4899', endpoint: '/api/photowall/albums', isPaginated: true, linkPrefix: '/photowall', slugField: 'id' },
  { id: 'songs', key: 'planetSongs', type: 'song', color: '#3b82f6', endpoint: '/api/music/songs', isPaginated: false, linkPrefix: '/music', slugField: 'id' },
  { id: 'notices', key: 'planetNotices', type: 'notice', color: '#06b6d4', endpoint: '/api/announcements', isPaginated: false, linkPrefix: '/announcements', slugField: 'id' }
]

export function getPlanetConfig() {
  return PLANET_CONFIG
}

export async function fetchAllContent() {
  const results = await Promise.allSettled(
    PLANET_CONFIG.map(async (config) => {
      try {
        let items
        if (config.isPaginated) {
          const res = await request.get(config.endpoint, { params: { page: 0, size: 200 } })
          items = res?.content || res || []
        } else {
          const res = await request.get(config.endpoint)
          items = Array.isArray(res) ? res : []
        }
        return { config, items: Array.isArray(items) ? items : [] }
      } catch {
        return { config, items: [] }
      }
    })
  )

  // Also fetch categories and tags
  let categories = []
  let tags = []
  try {
    const [catRes, tagRes] = await Promise.allSettled([
      request.get('/api/categories'),
      request.get('/api/tags')
    ])
    if (catRes.status === 'fulfilled') categories = Array.isArray(catRes.value) ? catRes.value : []
    if (tagRes.status === 'fulfilled') tags = Array.isArray(tagRes.value) ? tagRes.value : []
  } catch { /* ignore */ }

  const planets = []
  const allNodes = []
  let totalXP = 0

  results.forEach((result, index) => {
    if (result.status !== 'fulfilled') return
    const { config, items } = result.value
    const planetIndex = index

    // Build planet
    const angle = (index / PLANET_CONFIG.length) * Math.PI * 2
    const radius = 12
    const yPos = Math.sin(angle * 2) * 2

    planets.push({
      id: config.id,
      index: planetIndex,
      color: config.color,
      count: items.length,
      position: {
        x: Math.cos(angle) * radius,
        y: yPos,
        z: Math.sin(angle) * radius
      }
    })

    // Build content nodes orbiting this planet
    items.forEach((item, i) => {
      const seed = planetIndex * 1000 + i
      const orbitAngle = (i / Math.max(items.length, 1)) * Math.PI * 2
      const orbitRadius = 2.5 + seededRandom(seed) * 1.5
      const orbitY = (seededRandom(seed + 500) - 0.5) * 1.5

      const node = {
        id: `${config.id}-${item.id || item.slug || i}`,
        type: config.type,
        planetIndex,
        title: item.title || item.name || item.content?.substring(0, 30) || '',
        subtitle: item.subtitle || item.description || item.category?.name || '',
        date: item.createdAt || item.date || item.entryDate || '',
        coverUrl: item.coverImage || item.cover || item.thumbnail || '',
        tags: Array.isArray(item.tags) ? item.tags.map(t => typeof t === 'string' ? t : t.name) : typeof item.tags === 'string' && item.tags ? item.tags.split(',').map(s => s.trim()).filter(Boolean) : [],
        views: item.viewCount || item.views || 0,
        apiType: item.type || '',
        apiTag: item.tag || '',
        link: `${config.linkPrefix}/${item[config.slugField] || item.id || ''}`,
        position: {
          x: planets[planetIndex].position.x + Math.cos(orbitAngle) * orbitRadius,
          y: planets[planetIndex].position.y + orbitY,
          z: planets[planetIndex].position.z + Math.sin(orbitAngle) * orbitRadius
        }
      }
      allNodes.push(node)
    })

    // XP from content
    const xpMap = { articles: 10, projects: 25, moments: 5, albums: 15, songs: 8, notices: 3 }
    totalXP += items.length * (xpMap[config.id] || 5)
  })

  // XP from categories and tags
  totalXP += categories.length * 12
  totalXP += tags.length * 2

  return { planets, nodes: allNodes, categories, tags, totalXP }
}
