import request from '../utils/request'

export function getDashboardStats() {
  return Promise.all([
    request.get('/admin/articles?size=1'),
    request.get('/admin/articles?size=1&status=1'),
    request.get('/admin/articles?size=1&status=0'),
    request.get('/admin/categories'),
    request.get('/admin/tags'),
    request.get('/admin/projects'),
  ]).then(([all, published, drafts, categories, tags, projects]) => ({
    articleCount: all.totalElements || 0,
    publishedCount: published.totalElements || 0,
    draftCount: drafts.totalElements || 0,
    categoryCount: categories?.length || 0,
    tagCount: tags?.length || 0,
    projectCount: projects?.length || 0,
    categories: categories || [],
    tags: tags || [],
  }))
}

export function getRecentArticles(size = 5) {
  return request.get(`/admin/articles?size=${size}`)
}

export function getRecentDrafts(size = 5) {
  return request.get(`/admin/articles?size=${size}&status=0`)
}

export function getHotArticles(size = 5) {
  return request.get(`/admin/articles?size=${size}&status=1`)
}

export function getAnnouncements() {
  return request.get('/admin/announcements')
}

export function getAllArticlesForCharts(size = 100) {
  return request.get(`/admin/articles?size=${size}`)
}

// Report APIs
export function saveReport(report) {
  return request.post('/admin/reports', report)
}

export function getReports(params = {}) {
  return request.get('/admin/reports', { params })
}

export function getReport(id) {
  return request.get(`/admin/reports/${id}`)
}

export function deleteReport(id) {
  return request.delete(`/admin/reports/${id}`)
}
