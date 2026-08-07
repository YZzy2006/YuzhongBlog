import { ref, reactive } from 'vue'
import { fetchAllContent, getPlanetConfig } from '../cosmos/CosmosData'

export function useCosmosData() {
  const nodes = ref([])
  const planets = ref([])
  const categories = ref([])
  const tags = ref([])
  const loading = ref(true)
  const error = ref(false)
  const totalXP = ref(0)

  const stats = reactive({
    articles: 0,
    projects: 0,
    moments: 0,
    albums: 0,
    songs: 0,
    notices: 0,
    categories: 0,
    tags: 0,
    level: 1,
    totalViews: 0,
    nightMoments: 0,
    weekendMoments: 0,
    hotMoments: 0,
    earlyPosts: 0,
    latePosts: 0,
    weekendPosts: 0,
    articleStreak: 0,
    postStreak: 0,
    maxTypesPerDay: 0,
    holidayPost: false
  })

  const planetConfig = getPlanetConfig()

  async function fetchAll() {
    loading.value = true
    error.value = false
    try {
      const data = await fetchAllContent()
      nodes.value = data.nodes
      planets.value = data.planets
      categories.value = data.categories
      tags.value = data.tags
      totalXP.value = data.totalXP

      // Compute stats
      const typeCount = {}
      for (const node of data.nodes) {
        typeCount[node.type] = (typeCount[node.type] || 0) + 1
      }
      stats.articles = typeCount.article || 0
      stats.projects = typeCount.project || 0
      stats.moments = typeCount.moment || 0
      stats.albums = typeCount.album || 0
      stats.songs = typeCount.song || 0
      stats.notices = typeCount.notice || 0
      stats.categories = data.categories.length
      stats.tags = data.tags.length

      // Time-based stats
      let totalViews = 0
      let nightMoments = 0, weekendMoments = 0, hotMoments = 0
      let earlyPosts = 0, latePosts = 0, weekendPosts = 0
      const articleDates = new Set()
      const allPostDates = new Set()

      for (const node of data.nodes) {
        totalViews += node.views || 0
        const d = node.date ? new Date(node.date) : null
        if (!d || isNaN(d)) continue
        const hour = d.getHours()
        const day = d.getDay()

        if (node.type === 'moment') {
          if (hour >= 0 && hour < 6) nightMoments++
          if (day === 0 || day === 6) weekendMoments++
          if ((node.views || 0) >= 100) hotMoments++
        }
        if (node.type === 'article') {
          if (hour >= 5 && hour < 9) earlyPosts++
          if (hour >= 22 || hour < 2) latePosts++
          if (day === 0 || day === 6) weekendPosts++
          articleDates.add(d.toISOString().slice(0, 10))
        }
        allPostDates.add(d.toISOString().slice(0, 10))
      }

      // Article streak: consecutive days with at least one article
      let streak = 0
      const sorted = [...articleDates].sort().reverse()
      if (sorted.length > 0) {
        const today = new Date().toISOString().slice(0, 10)
        let check = new Date(today)
        // Allow today or yesterday as start
        if (!articleDates.has(today)) {
          check.setDate(check.getDate() - 1)
        }
        for (let i = 0; i < 365; i++) {
          const key = check.toISOString().slice(0, 10)
          if (articleDates.has(key)) {
            streak++
            check.setDate(check.getDate() - 1)
          } else {
            break
          }
        }
      }

      // Post streak: consecutive days with any content
      let postStreakCount = 0
      if (allPostDates.size > 0) {
        const today = new Date().toISOString().slice(0, 10)
        let check = new Date(today)
        if (!allPostDates.has(today)) {
          check.setDate(check.getDate() - 1)
        }
        for (let i = 0; i < 365; i++) {
          const key = check.toISOString().slice(0, 10)
          if (allPostDates.has(key)) {
            postStreakCount++
            check.setDate(check.getDate() - 1)
          } else {
            break
          }
        }
      }

      stats.totalViews = totalViews
      stats.nightMoments = nightMoments
      stats.weekendMoments = weekendMoments
      stats.hotMoments = hotMoments
      stats.earlyPosts = earlyPosts
      stats.latePosts = latePosts
      stats.weekendPosts = weekendPosts
      stats.articleStreak = streak
      stats.postStreak = postStreakCount

      // Combo: max different content types posted on the same day
      const dayTypes = {}
      for (const node of data.nodes) {
        if (!node.date) continue
        const day = new Date(node.date).toISOString().slice(0, 10)
        if (!dayTypes[day]) dayTypes[day] = new Set()
        dayTypes[day].add(node.type)
      }
      let maxTypes = 0
      for (const types of Object.values(dayTypes)) {
        if (types.size > maxTypes) maxTypes = types.size
      }
      stats.maxTypesPerDay = maxTypes

      // Holiday posts
      const holidays = { '01-01': 'newYear', '02-14': 'valentine', '10-31': 'halloween', '12-25': 'christmas' }
      const holidayPosts = { newYear: false, valentine: false, halloween: false, christmas: false }
      for (const node of data.nodes) {
        if (!node.date) continue
        const mmdd = new Date(node.date).toISOString().slice(5, 10)
        if (holidays[mmdd]) holidayPosts[holidays[mmdd]] = true
      }
      stats.holidayPost = holidayPosts
    } catch (e) {
      console.error('Failed to load cosmos data:', e)
      error.value = true
    } finally {
      loading.value = false
    }
  }

  return { nodes, planets, categories, tags, loading, error, stats, totalXP, planetConfig, fetchAll }
}
