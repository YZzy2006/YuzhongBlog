<template>
  <div class="search-page">
    <h1 class="page-title fade-in-up">{{ $t('search.title') }}</h1>
    <div class="search-box fade-in-up fade-in-up-delay-1">
      <input v-model="keyword" :placeholder="$t('search.placeholder')" @keyup.enter="doSearch" />
      <button @click="doSearch">{{ $t('search.button') }}</button>
    </div>
    <div v-if="searched" class="results">
      <!-- Search skeleton -->
      <div v-if="searchLoading">
        <div v-for="n in 3" :key="n" class="article-card" style="cursor: default;">
          <div style="display: flex; gap: 12px; margin-bottom: 10px;">
            <div class="skeleton" style="width: 60px; height: 14px;" />
            <div class="skeleton" style="width: 80px; height: 14px;" />
          </div>
          <div class="skeleton" style="width: 70%; height: 20px; margin-bottom: 10px;" />
          <div class="skeleton" style="width: 100%; height: 14px; margin-bottom: 6px;" />
          <div class="skeleton" style="width: 85%; height: 14px;" />
        </div>
      </div>
      <p v-if="!searchLoading && results.length" class="result-count fade-in-up">{{ t('search.resultCount', { n: results.length }) }}</p>
      <div v-if="!searchLoading" v-for="(article, i) in results" :key="article.id"
        class="article-card fade-in-up"
        :class="`fade-in-up-delay-${Math.min(i + 1, 5)}`">
        <div class="article-meta">
          <span v-if="article.categoryName" class="category">{{ article.categoryName }}</span>
          <span class="date">{{ relativeDate(article.createdAt) }}</span>
          <span v-if="article.viewCount" class="views">{{ t('search.views', { n: article.viewCount }) }}</span>
        </div>
        <router-link :to="`/articles/${article.slug || article.id}`" class="article-title">
          {{ articleTitle(article) }}
        </router-link>
        <p class="article-summary">{{ stripMarkdown(articleSummary(article)) }}</p>
        <div class="article-tags" v-if="article.tags?.length">
          <span v-for="tag in article.tags" :key="tag.id" class="tag">{{ tag.name }}</span>
        </div>
      </div>
      <p v-if="!searchLoading && results.length === 0" class="empty fade-in-up">{{ $t('search.empty') }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '../utils/request'
import { relativeDate } from '../utils/date'
import { stripMarkdown } from '../utils/stripMarkdown'

const { t, locale } = useI18n()
const isEn = computed(() => locale.value === 'en-US')
function articleTitle(a) { return a && isEn.value && a.titleEn ? a.titleEn : (a?.title || '') }
function articleSummary(a) { return a && isEn.value && a.summaryEn ? a.summaryEn : (a?.summary || '') }

const keyword = ref('')
const results = ref([])
const searched = ref(false)
const searchLoading = ref(false)
let searchRequestId = 0

watch(keyword, (val) => {
  if (!val.trim()) { searched.value = false; results.value = [] }
})

async function doSearch() {
  if (!keyword.value.trim()) return
  const reqId = ++searchRequestId
  searchLoading.value = true
  searched.value = true
  try {
    const data = await request.get(`/api/articles?keyword=${encodeURIComponent(keyword.value)}`)
    if (reqId !== searchRequestId) return
    results.value = data.content || []
  } catch (e) {
    if (reqId !== searchRequestId) return
    console.error('Search failed:', e)
  } finally {
    if (reqId === searchRequestId) searchLoading.value = false
  }
}
</script>

<style scoped>
.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
}
.search-box {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 2rem;
}
.search-box input {
  flex: 1;
  max-width: 420px;
  padding: 0.6rem 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  outline: none;
  transition: all var(--transition);
  background: var(--color-bg);
}
.search-box input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-glow);
}
.search-box button {
  padding: 0.6rem 1.5rem;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 0.9rem;
  font-weight: 500;
  transition: all var(--transition);
  box-shadow: 0 2px 8px rgba(30, 94, 182, 0.2);
}
.search-box button:hover {
  background: var(--color-primary-hover);
  box-shadow: 0 4px 14px rgba(30, 94, 182, 0.3);
  transform: translateY(-1px);
}
.results { margin-top: 1rem; }
.result-count { color: var(--color-text-secondary); margin-bottom: 1rem; font-size: 0.9rem; }
.article-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-left: 3px solid var(--color-primary);
  border-radius: var(--radius-lg);
  padding: 1.25rem 1.5rem;
  margin-bottom: 0.75rem;
  transition: all var(--transition);
}
.article-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-1px);
}
.article-meta {
  display: flex;
  gap: 0.75rem;
  font-size: 0.8rem;
  color: var(--color-text-tertiary);
  margin-bottom: 0.4rem;
}
.category { color: var(--color-primary); font-weight: 500; }
.views { margin-left: auto; }
.article-title {
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--color-text);
  text-decoration: none;
  display: block;
  margin-bottom: 0.35rem;
  transition: color var(--transition-fast);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.article-title:hover { color: var(--color-primary); }
.article-summary {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.article-tags {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}
.tag {
  font-size: 0.72rem;
  padding: 0.1rem 0.5rem;
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border-light);
  border-radius: 2rem;
  color: var(--color-text-secondary);
  transition: all var(--transition-fast);
}
.tag:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.empty { color: var(--color-text-tertiary); text-align: center; padding: 2rem; }

@media (max-width: 768px) {
  .page-title { font-size: 1.2rem; }
  .search-box { flex-direction: column; gap: 0.5rem; }
  .search-box input { max-width: none; }
  .search-box button { width: 100%; }
  .article-card { padding: 1rem; }
  .article-title { font-size: 0.95rem; white-space: normal; }
  .article-summary { font-size: 0.82rem; }
  .article-meta { font-size: 0.75rem; flex-wrap: wrap; gap: 0.25rem 0.5rem; }
  .views { margin-left: 0; }
}
</style>
