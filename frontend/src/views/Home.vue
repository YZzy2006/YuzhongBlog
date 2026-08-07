<template>
  <div class="home">
    <!-- Notice Banner -->
    <template v-if="topNotice">
      <!-- Banner style -->
      <div v-if="topNotice.displayStyle !== 'alert'" class="notice-banner" :class="[`notice-banner--${topNotice.level || 'info'}`, ready ? 'fade-in-up' : '']" @click="showNotice(topNotice)">
        <span class="notice-icon">&#128161;</span>
        <div class="notice-banner-body">
          <span class="notice-banner-title">{{ noticeTitle(topNotice) }}</span>
          <span class="notice-banner-text">{{ noticeContent(topNotice) }}</span>
        </div>
        <span class="notice-banner-arrow">&rsaquo;</span>
      </div>
      <!-- Alert style -->
      <div v-else class="notice-alert" :class="[`notice-alert--${topNotice.level || 'info'}`, ready ? 'fade-in-up' : '']" @click="showNotice(topNotice)">
        <svg class="notice-alert-icon" stroke="currentColor" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M13 16h-1v-4h1m0-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke-width="2" stroke-linejoin="round" stroke-linecap="round"></path>
        </svg>
        <div class="notice-alert-body">
          <span class="notice-alert-title">{{ noticeTitle(topNotice) }}</span>
          <span class="notice-alert-text">{{ noticeContent(topNotice) }}</span>
        </div>
      </div>
    </template>

    <!-- Hero -->
    <section class="hero" :class="heroTheme">
      <div class="hero-bg"></div>
      <!-- Weather info chip -->
      <div class="hero-weather" v-if="weatherData" @click="showCityPicker = true" :title="weatherData.city">
        <span class="hero-weather-icon">{{ weatherData.icon }}</span>
        <span class="hero-weather-desc">{{ weatherData.desc }}</span>
        <span class="hero-weather-temp">{{ weatherData.temp }}°</span>
      </div>
      <!-- Soft light orbs -->
      <div class="hero-orbs">
        <div class="orb"></div><div class="orb"></div><div class="orb"></div><div class="orb"></div>
      </div>
      <!-- Sun (sunny/default) -->
      <div class="hero-sun">
        <div class="sun-core"></div>
        <div class="sun-glow"></div>
        <div class="sun-rays">
          <div class="ray"></div><div class="ray"></div><div class="ray"></div>
          <div class="ray"></div><div class="ray"></div><div class="ray"></div>
          <div class="ray"></div><div class="ray"></div>
        </div>
      </div>
      <!-- Clouds -->
      <div class="hero-clouds">
        <div class="cloud cloud-1"></div>
        <div class="cloud cloud-2"></div>
        <div class="cloud cloud-3"></div>
        <div class="cloud cloud-4"></div>
        <div class="cloud cloud-5"></div>
        <div class="cloud cloud-6"></div>
        <div class="cloud cloud-7"></div>
        <div class="cloud cloud-8"></div>
      </div>
      <!-- Fog layer -->
      <div class="hero-fog">
        <div class="fog-layer fog-1"></div>
        <div class="fog-layer fog-2"></div>
      </div>
      <!-- Lightning (storm) -->
      <div class="hero-lightning"></div>
      <div class="hero-bubbles">
        <div class="bubble"></div>
        <div class="bubble"></div>
        <div class="bubble"></div>
        <div class="bubble"></div>
        <div class="bubble"></div>
        <div class="bubble"></div>
        <div class="bubble"></div>
        <div class="bubble"></div>
      </div>
      <div class="hero-particles">
        <div class="particle"></div>
        <div class="particle"></div>
        <div class="particle"></div>
        <div class="particle"></div>
        <div class="particle"></div>
        <div class="particle"></div>
      </div>
      <!-- Rain drops -->
      <div class="hero-rain">
        <div class="raindrop"></div><div class="raindrop"></div><div class="raindrop"></div>
        <div class="raindrop"></div><div class="raindrop"></div><div class="raindrop"></div>
        <div class="raindrop"></div><div class="raindrop"></div><div class="raindrop"></div>
        <div class="raindrop"></div><div class="raindrop"></div><div class="raindrop"></div>
        <div class="raindrop"></div><div class="raindrop"></div><div class="raindrop"></div>
        <div class="raindrop"></div><div class="raindrop"></div><div class="raindrop"></div>
        <div class="raindrop"></div><div class="raindrop"></div>
      </div>
      <!-- Snowflakes -->
      <div class="hero-snow">
        <div class="snowflake">&#10052;</div><div class="snowflake">&#10052;</div>
        <div class="snowflake">&#10052;</div><div class="snowflake">&#10052;</div>
        <div class="snowflake">&#10052;</div><div class="snowflake">&#10052;</div>
        <div class="snowflake">&#10052;</div><div class="snowflake">&#10052;</div>
        <div class="snowflake">&#10052;</div><div class="snowflake">&#10052;</div>
        <div class="snowflake">&#10052;</div><div class="snowflake">&#10052;</div>
        <div class="snowflake">&#10052;</div><div class="snowflake">&#10052;</div>
        <div class="snowflake">&#10052;</div>
      </div>
      <div class="hero-inner">
        <h1>
          <span v-for="(ch, i) in heroTitleChars" :key="i" class="hero-char hero-char-title" :style="{ animationDelay: (i * 45) + 'ms' }">{{ ch === ' ' ? ' ' : ch }}</span>
        </h1>
        <p class="hero-desc">
          <span v-for="(ch, i) in heroDescChars" :key="i" class="hero-char hero-char-desc" :style="{ animationDelay: (350 + i * 30) + 'ms' }">{{ ch === ' ' ? ' ' : ch }}</span>
        </p>
        <div class="hero-links">
          <router-link to="/articles" class="btn-primary">{{ $t('home.readArticles') }}</router-link>
          <router-link to="/about" class="btn-ghost">{{ $t('home.learnMore') }}</router-link>
        </div>
      </div>
    </section>

    <!-- Guide Cards -->
    <section class="guide-section">
      <div class="guide-grid">
        <router-link v-for="(g, i) in guides" :key="g.title" :to="g.to" class="guide-item" :class="ready ? `fade-in-up fade-in-up-delay-${i + 1}` : ''">
          <span class="guide-icon" v-html="g.svg"></span>
          <h4>{{ g.title }}</h4>
          <p>{{ g.desc }}</p>
        </router-link>
      </div>
    </section>

    <!-- Main + Sidebar -->
    <div class="content-grid">
      <!-- Left: Articles -->
      <div class="main-col">
        <!-- AI Search -->
        <section v-if="aiEnabled" class="ai-search-section" :class="ready ? 'fade-in-up fade-in-up-delay-2' : ''">
          <div class="ai-search-header">
            <div class="ai-header-left">
              <div class="ai-loader">
                <svg width="50" height="50" viewBox="0 0 100 100">
                  <defs>
                    <mask id="ai-clipping">
                      <polygon points="0,0 100,0 100,100 0,100" fill="black"></polygon>
                      <polygon points="25,25 75,25 50,75" fill="white"></polygon>
                      <polygon points="50,25 75,75 25,75" fill="white"></polygon>
                      <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                      <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                      <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                      <polygon points="35,35 65,35 50,65" fill="white"></polygon>
                    </mask>
                  </defs>
                </svg>
                <div class="ai-loader-box"></div>
              </div>
              <span>{{ $t('home.aiAssistant') }}</span>
            </div>
            <div class="ai-mode-toggle">
              <button class="ai-mode-btn" :class="{ active: aiMode === 'chat' }" @click="switchAiMode('chat')">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                {{ $t('home.chat') }}
              </button>
              <button class="ai-mode-btn" :class="{ active: aiMode === 'search' }" @click="switchAiMode('search')">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
                {{ $t('home.searchMode') }}
              </button>
            </div>
          </div>
          <div class="ask-ai-wrapper">
            <div class="ai-input-container">
              <span class="underline-effect"></span>
              <span class="ripple-circle"></span>
              <span class="bg-fade"></span>
              <span class="floating-dots">
                <span></span><span></span><span></span><span></span>
              </span>
              <input v-model="aiQuery" @keyup.enter="handleAiSubmit"
                :placeholder="aiMode === 'search' ? $t('home.searchArticles') : (aiLoading ? $t('home.thinking') : $t('home.askAi'))"
                :disabled="aiLoading || searchLoading" class="ai-input" type="text" />
              <span class="icon-container" @click="handleAiSubmit">
                <div v-if="aiLoading || searchLoading" class="ai-loading-bars">
                  <span></span><span></span><span></span><span></span><span></span>
                </div>
                <svg v-else viewBox="0 0 24 24" height="24" width="24" xmlns="http://www.w3.org/2000/svg" class="ai-icon-svg">
                  <path color="currentColor" d="M7.94 3.078h8.11c1.37 0 2.47 0 3.34.12c.9.12 1.66.38 2.26.98s.86 1.36.98 2.26c.12.87.12 1.97.12 3.34v2.05c0 .41-.34.75-.75.75s-.75-.34-.75-.75v-2c0-1.43 0-2.44-.1-3.19c-.1-.73-.28-1.12-.56-1.4s-.66-.46-1.4-.56c-.76-.1-1.76-.1-3.19-.1H8c-1.43 0-2.44 0-3.19.1c-.73.1-1.12.28-1.4.56s-.46.67-.56 1.4c-.1.76-.1 1.76-.1 3.19s0 2.44.1 3.19c.1.73.28 1.12.56 1.4s.66.46 1.4.56c.76.1 1.76.1 3.19.1h3c.41 0 .75.34.75.75s-.34.75-.75.75H7.95c-1.37 0-2.47 0-3.34-.12c-.9-.12-1.66-.38-2.26-.98s-.86-1.36-.98-2.26c-.12-.87-.12-1.97-.12-3.34v-.11c0-1.37 0-2.47.12-3.34c.12-.9.38-1.66.98-2.26s1.36-.86 2.26-.98c.87-.12 1.97-.12 3.34-.12zm8.76 10.88l-.04.09a4.34 4.34 0 0 1-2.45 2.45l-.09.04c-1.17.46-1.17 2.12 0 2.58l.09.04c1.12.44 2.01 1.33 2.45 2.45l.04.09c.46 1.17 2.12 1.17 2.58 0l.04-.09a4.34 4.34 0 0 1 2.45-2.45l.09-.04c1.17-.46 1.17-2.12 0-2.58l-.09-.04a4.34 4.34 0 0 1-2.45-2.45l-.04-.09c-.46-1.17-2.12-1.17-2.58 0m1.29.81a5.83 5.83 0 0 0 3.06 3.06a5.83 5.83 0 0 0-3.06 3.06a5.83 5.83 0 0 0-3.06-3.06a5.83 5.83 0 0 0 3.06-3.06M6.74 8.828c0-.41-.34-.75-.75-.75s-.75.34-.75.75v2c0 .41.34.75.75.75s.75-.34.75-.75zm8.25-1.75c.41 0 .75.34.75.75v4c0 .41-.34.75-.75.75s-.75-.34-.75-.75v-4c0-.41.34-.75.75-.75m-2.25 2.25c0-.41-.34-.75-.75-.75s-.75.34-.75.75v1c0 .41.34.75.75.75s.75-.34.75-.75zm5.25-.75c.41 0 .75.34.75.75v1c0 .41-.34.75-.75.75s-.75-.34-.75-.75v-1c0-.41.34-.75.75-.75m-8.25-.75c0-.41-.34-.75-.75-.75s-.75.34-.75.75v4c0 .41.34.75.75.75s.75-.34.75-.75z" fill-rule="evenodd" fill="currentColor"></path>
                </svg>
              </span>
            </div>
          </div>

          <!-- Intent Navigation Cards -->
          <div v-if="aiNavIntents.length" class="ai-nav-cards">
            <router-link v-for="nav in aiNavIntents" :key="nav.to" :to="nav.to" class="ai-nav-card">
              <span class="ai-nav-icon">{{ nav.icon }}</span>
              <span class="ai-nav-label">{{ nav.label }}</span>
              <svg class="ai-nav-arrow" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
            </router-link>
          </div>

          <!-- Chat Thinking -->
          <div v-if="aiMode === 'chat' && aiThinking" class="ai-thinking">
            <div class="ai-thinking-header" @click="thinkingExpanded = !thinkingExpanded">
              <span class="thinking-icon">&#129504;</span>
              <span class="thinking-label">{{ $t('home.thinkingProcess') }}</span>
              <span class="thinking-pulse"></span>
              <span class="thinking-toggle">{{ thinkingExpanded ? $t('home.collapse') : $t('home.expand') }}</span>
            </div>
            <Transition name="thinking-slide">
              <div v-show="thinkingExpanded" class="ai-thinking-content">
                <MdPreview :modelValue="aiThinking" previewTheme="github" :codeFoldable="false" />
              </div>
            </Transition>
          </div>

          <!-- Chat Response -->
          <div v-if="aiMode === 'chat' && aiResponse" class="ai-response">
            <div class="ai-response-header">
              <svg class="ai-response-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a4 4 0 0 1 4 4v1a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V6a4 4 0 0 1 4-4z"/><path d="M10 10v4"/><path d="M14 10v4"/><path d="M8 18h8"/><path d="M7 22h10"/><path d="M9 14h6v4H9z"/></svg>
              <span>{{ $t('home.aiAnswer') }}</span>
            </div>
            <div class="ai-response-body">
              <MdPreview :modelValue="aiResponse" previewTheme="github" :codeFoldable="false" />
            </div>
          </div>

          <div v-if="aiError" class="ai-error">
            <svg class="ai-error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 8v4"/><path d="M12 16h.01"/></svg>
            <span>{{ aiError }}</span>
          </div>
        </section>

        <section class="section">
          <div class="section-header" :class="ready ? 'fade-in-up fade-in-up-delay-2' : ''">
            <div class="glass-radio-group">
              <input type="radio" id="tab-latest" name="article-tabs" value="latest" v-model="articleSort" checked />
              <label for="tab-latest">{{ $t('home.latest') }}</label>
              <input type="radio" id="tab-popular" name="article-tabs" value="popular" v-model="articleSort" />
              <label for="tab-popular">{{ $t('home.popular') }}</label>
              <input type="radio" id="tab-featured" name="article-tabs" value="featured" v-model="articleSort" />
              <label for="tab-featured">{{ $t('home.featured') }}</label>
              <input type="radio" id="tab-search" name="article-tabs" value="search" v-model="articleSort" />
              <label for="tab-search">{{ $t('home.searchTab') }}</label>
              <div class="glass-glider"></div>
            </div>
            <router-link v-if="articleSort !== 'search'" to="/articles" class="more-link">{{ $t('home.viewAll') }} &rarr;</router-link>
          </div>
          <!-- Search Tab Content -->
          <div v-if="articleSort === 'search'" class="article-list">
            <div v-if="!searchResults.length" class="search-empty-state">
              <svg class="search-empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
              </svg>
              <template v-if="aiError">
                <p class="search-empty-text">{{ aiError }}</p>
                <p class="search-empty-hint">{{ $t('home.searchEmptyHint') }}</p>
              </template>
              <template v-else>
                <p class="search-empty-text">{{ $t('home.searchEmptyText') }}</p>
                <p class="search-empty-hint">{{ $t('home.searchEmptyHint') }}</p>
              </template>
            </div>
            <template v-else>
              <div class="search-results-header">
                <span>{{ $t('home.foundArticles', { count: searchResults.length }) }}</span>
                <button class="search-clear-btn" @click="clearSearchResults">{{ $t('home.clearResults') }}</button>
              </div>
              <router-link v-for="(article, i) in searchResults" :key="article.id"
                :to="`/articles/${article.slug || article.id}`"
                class="article-card-item"
                :class="ready ? `fade-in-up fade-in-up-delay-${Math.min(i + 3, 5)}` : ''">
                <svg class="article-card-icon" stroke-linejoin="round" stroke-linecap="round" stroke-width="1.5" fill="none" stroke="#374151" viewBox="0 0 24 24">
                  <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
                <span v-if="article.categoryName" class="article-card-cat">{{ article.categoryName }}</span>
                <p class="article-card-title">{{ articleTitle(article) }}</p>
                <p class="article-card-summary">{{ stripMarkdown(articleSummary(article)) }}</p>
                <div class="article-card-meta">
                  <span class="article-card-date">{{ relativeDate(article.createdAt) }}</span>
                  <span v-if="article.viewCount" class="article-card-views">{{ article.viewCount }} {{ $t('home.reads') }}</span>
                </div>
                <p class="article-card-number">{{ String(i + 1).padStart(2, '0') }}</p>
              </router-link>
            </template>
          </div>

          <!-- Normal Tabs Content (latest/popular/featured) -->
          <template v-else>
            <!-- Skeleton loading -->
            <div v-if="loading" class="article-list">
              <div v-for="n in 4" :key="n" class="article-card-item skeleton-card">
                <div class="skeleton" style="width: 28px; height: 28px; border-radius: 6px;" />
                <div class="skeleton" style="width: 50px; height: 14px; border-radius: 4px;" />
                <div class="skeleton" style="width: 75%; height: 16px;" />
                <div style="display: flex; flex-direction: column; gap: 6px; width: 100%;">
                  <div class="skeleton" style="width: 100%; height: 13px;" />
                  <div class="skeleton" style="width: 60%; height: 13px;" />
                </div>
                <div style="display: flex; gap: 12px;">
                  <div class="skeleton" style="width: 60px; height: 12px;" />
                  <div class="skeleton" style="width: 40px; height: 12px;" />
                </div>
                <div class="skeleton" style="width: 48px; height: 36px; align-self: flex-end; border-radius: 6px;" />
              </div>
            </div>
            <div v-if="!loading" class="article-list">
              <router-link v-for="(article, i) in articles" :key="article.id"
                :to="`/articles/${article.slug || article.id}`"
                class="article-card-item"
                :class="ready ? `fade-in-up fade-in-up-delay-${Math.min(i + 3, 5)}` : ''">
                <svg class="article-card-icon" stroke-linejoin="round" stroke-linecap="round" stroke-width="1.5" fill="none" stroke="#374151" viewBox="0 0 24 24">
                  <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
                <span v-if="article.categoryName" class="article-card-cat">{{ article.categoryName }}</span>
                <p class="article-card-title">{{ articleTitle(article) }}</p>
                <p class="article-card-summary">{{ stripMarkdown(articleSummary(article)) }}</p>
                <div class="article-card-meta">
                  <span class="article-card-date">{{ relativeDate(article.createdAt) }}</span>
                  <span v-if="article.viewCount" class="article-card-views">{{ article.viewCount }} {{ $t('home.reads') }}</span>
                </div>
                <p class="article-card-number">{{ String(i + 1).padStart(2, '0') }}</p>
              </router-link>
              <p v-if="articles.length === 0" class="empty">{{ $t('home.noArticles') }}</p>
            </div>
            <!-- Pagination -->
            <div class="home-pagination" v-if="articles.length > 0">
              <div class="page-size-selector">
                <span class="page-size-label">{{ $t('home.perPage') }}</span>
                <DropdownMenu :modelValue="homePageSize" :items="pageSizeItems" @change="v => setHomePageSize(v)" />
              </div>
              <div class="page-nav" v-if="homeTotalPages > 1">
                <button class="page-nav-btn" :disabled="homePage === 0" @click="goHomePrev">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
                </button>
                <button v-for="p in homePageNumbers" :key="p"
                  class="page-num-btn" :class="{ active: p === homePage + 1 }"
                  @click="goHomePage(p)">{{ p }}</button>
                <button class="page-nav-btn" :disabled="homePage >= homeTotalPages - 1" @click="goHomeNext">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
                </button>
              </div>
              <span class="page-total">{{ $t('home.total', { count: homeTotalElements }) }}</span>
            </div>
          </template>
        </section>
      </div>

      <!-- Right: Sidebar -->
      <aside class="sidebar-col">
        <!-- Weather Card -->
        <div class="weather-card" :class="ready ? 'fade-in-up fade-in-up-delay-2' : ''" v-if="weatherData">
          <section class="weather-info" :style="{ background: getWeatherBg(weatherData.code) }">
            <div class="weather-bg-design">
              <div class="weather-circle"></div>
              <div class="weather-circle"></div>
              <div class="weather-circle"></div>
            </div>
            <div class="weather-left">
              <div class="weather-condition">
                <span class="weather-icon-lg">{{ weatherData.icon }}</span>
                <span class="weather-desc">{{ weatherData.desc }}</span>
              </div>
              <div class="weather-temp">{{ weatherData.temp }}°</div>
              <div class="weather-range">{{ weatherData.max }}° / {{ weatherData.min }}°</div>
            </div>
            <div class="weather-right">
              <div class="weather-time-wrap">
                <div class="weather-time">{{ nowTime }}</div>
                <div class="weather-date">{{ nowDate }}</div>
              </div>
              <div class="weather-city" @click="showCityPicker = true">
                {{ cityName(weatherCity) }}
                <svg width="10" height="10" viewBox="0 0 10 10" fill="currentColor"><path d="M2 4l3 3 3-3z"/></svg>
              </div>
            </div>
          </section>
          <section class="weather-forecast">
            <div v-for="day in weatherData.forecast" :key="day.day" class="forecast-item">
              <span class="forecast-day">{{ day.day }}</span>
              <span class="forecast-icon">{{ day.icon }}</span>
              <span class="forecast-temp">{{ day.max }}°/{{ day.min }}°</span>
            </div>
          </section>
          <div class="weather-tip">
            <span class="weather-tip-icon">💡</span>
            <span>{{ weatherData.tip }}</span>
          </div>
        </div>

        <!-- Weather Loading -->
        <div class="weather-card weather-loading" :class="ready ? 'fade-in-up fade-in-up-delay-2' : ''" v-else-if="weatherLoading">
          <div class="weather-loading-text">{{ $t('home.weatherLoading') }}</div>
        </div>

        <!-- Weather Failed -->
        <div class="weather-card weather-failed" :class="ready ? 'fade-in-up fade-in-up-delay-2' : ''" v-else-if="weatherFail">
          <ResourceError :message="$t('home.weatherError')" @retry="fetchWeather" />
        </div>

        <!-- Weather Placeholder (initial) -->
        <div class="weather-card weather-loading" :class="ready ? 'fade-in-up fade-in-up-delay-2' : ''" v-else>
          <div class="weather-loading-text">{{ $t('home.weatherLoading') }}</div>
        </div>

        <!-- City Picker Modal -->
        <Teleport to="body">
          <Transition name="modal">
            <div v-if="showCityPicker" class="city-picker-mask" @mousedown.self="showCityPicker = false">
              <div class="city-picker-modal">
                <div class="city-picker-header">
                  <h3>{{ $t('home.selectCity') }}</h3>
                  <button class="city-picker-close" @click="showCityPicker = false">&times;</button>
                </div>
                <div class="city-picker-search">
                  <input v-model="citySearch" :placeholder="$t('home.searchCity')" autofocus />
                </div>
                <div class="city-picker-grid">
                  <button v-for="city in filteredCities" :key="city.name + city.admin"
                    class="city-tag" :class="{ active: city.name === weatherCity.name && city.admin === weatherCity.admin }"
                    @click="selectCity(city)">
                    <span class="city-tag-name">{{ cityName(city) }}</span>
                    <span class="city-tag-province">{{ cityAdmin(city) || (locale === 'en-US' ? city.countryEn : city.country) || '' }}</span>
                  </button>
                </div>
                <div class="city-picker-footer">
                  <span>{{ $t('home.weatherAttribution') }} <a :href="weatherProvider.url" target="_blank" rel="noopener">{{ weatherProvider.name }}</a></span>
                </div>
              </div>
            </div>
          </Transition>
        </Teleport>

        <!-- Announcements Card -->
        <div class="sidebar-card notice-card-section" :class="ready ? 'fade-in-up fade-in-up-delay-2' : ''">
          <div class="notice-card-header">
            <h3><span class="card-icon" style="color:#f59e0b">&#128227;</span> {{ $t('home.siteAnnouncements') }}</h3>
            <router-link to="/announcements" class="notice-view-all">{{ $t('home.noticeViewAll') }} →</router-link>
          </div>
          <div class="notice-card-list">
            <div v-for="n in notices" :key="n.id || n.title"
              class="notice-card-item" :class="`notice-card-item--${noticeColorKey(n)}`"
              @click="showNotice(n)">
              <svg class="notice-card-wave" viewBox="0 0 80 80" xmlns="http://www.w3.org/2000/svg">
                <path d="M0 40 Q10 20 20 40 T40 40 T60 40 T80 40 V80 H0 Z" :fill="waveColor(n)" />
              </svg>
              <div class="notice-card-icon-wrap">
                <svg class="notice-card-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path v-if="n.type === 'feature'" d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" :fill="iconColor(n)" stroke="none"/>
                  <path v-else-if="n.type === 'update'" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" :fill="iconColor(n)" stroke="none"/>
                  <path v-else d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" :fill="iconColor(n)" stroke="none"/>
                </svg>
              </div>
              <div class="notice-card-text">
                <span class="notice-card-title">{{ noticeTitle(n) }}</span>
                <span class="notice-card-sub">{{ noticeTag(n) }} · {{ relativeDate(n.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- About Card -->
        <div class="sidebar-card about-card" :class="ready ? 'fade-in-up fade-in-up-delay-2' : ''">
          <div class="candle-wrapper">
            <div class="candles">
              <div class="light__wave"></div>
              <div class="candle1">
                <div class="candle1__body">
                  <div class="candle1__eyes">
                    <span class="candle1__eyes-one"></span>
                    <span class="candle1__eyes-two"></span>
                  </div>
                  <div class="candle1__mouth"></div>
                </div>
                <div class="candle1__stick"></div>
              </div>
              <div class="candle2">
                <div class="candle2__body">
                  <div class="candle2__eyes">
                    <div class="candle2__eyes-one"></div>
                    <div class="candle2__eyes-two"></div>
                  </div>
                </div>
                <div class="candle2__stick"></div>
              </div>
              <div class="candle2__fire"></div>
              <div class="sparkles-one"></div>
              <div class="sparkles-two"></div>
              <div class="candle__smoke-one"></div>
              <div class="candle__smoke-two"></div>
            </div>
            <div class="floor"></div>
          </div>
          <h3 @click="$router.push('/about')" class="about-title-clickable"><span class="card-icon" style="color:#3b82f6">&#9733;</span> {{ $t('home.aboutSite') }}</h3>
          <p class="about-text">
            {{ $t('home.aboutSiteDesc') }}
          </p>
          <div class="about-stats">
            <div class="stat-item" @click="$router.push('/articles')">
              <span class="stat-num" style="color:#3b82f6">{{ stats.articleCount }}</span>
              <span class="stat-label">{{ $t('home.statArticles') }}</span>
            </div>
            <div class="stat-item" @click="$router.push('/articles')">
              <span class="stat-num" style="color:#10b981">{{ stats.categoryCount }}</span>
              <span class="stat-label">{{ $t('home.statCategories') }}</span>
            </div>
            <div class="stat-item" @click="$router.push('/articles')">
              <span class="stat-num" style="color:#f59e0b">{{ stats.tagCount }}</span>
              <span class="stat-label">{{ $t('home.statTags') }}</span>
            </div>
          </div>
        </div>
      </aside>
    </div>
    <!-- Notice Detail Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="noticeDetail" class="notice-modal-mask" @mousedown.self="onNoticeMaskMouseDown" @keydown.esc="noticeDetail = null" tabindex="0" ref="modalRef">
          <div class="notice-modal" role="dialog" aria-modal="true">
            <button class="notice-modal-close" @click="noticeDetail = null">&times;</button>
            <div class="notice-modal-header">
              <div class="notice-modal-icon" :class="noticeDetail.type">
                {{ noticeDetail.type === 'feature' ? '&#10024;' : noticeDetail.type === 'update' ? '&#128640;' : '&#128161;' }}
              </div>
              <div class="notice-modal-title-wrap">
                <h3 class="notice-modal-title">{{ noticeTitle(noticeDetail) }}</h3>
                <p class="notice-modal-subtitle">
                  <span class="notice-modal-tag" :class="noticeDetail.type">{{ noticeTag(noticeDetail) }}</span>
                  <span v-if="noticeDetail.createdAt" class="notice-modal-date">{{ relativeDate(noticeDetail.createdAt) }}</span>
                </p>
              </div>
            </div>
            <div class="notice-modal-body">
              <p>{{ noticeContent(noticeDetail) }}</p>
            </div>
            <div class="notice-modal-footer">
              <button class="notice-modal-btn" @click="noticeDetail = null">{{ $t('home.noticeClose') }}</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Lyric Bar -->
    <LyricBar />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick, defineAsyncComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '../utils/request'
import { cachedFetch } from '../utils/cache'
import { checkAiStatus, aiChatStream, smartSearch } from '../utils/ai'
import { useRouter } from 'vue-router'
import { relativeDate } from '../utils/date'
import { stripMarkdown } from '../utils/stripMarkdown'
import 'md-editor-v3/lib/preview.css'
import '../utils/mdEditorConfig'

const MdPreview = defineAsyncComponent(() =>
  import('md-editor-v3').then(m => m.MdPreview)
)
import ResourceError from '../components/ResourceError.vue'
import DropdownMenu from '../components/DropdownMenu.vue'
import LyricBar from '../components/LyricBar.vue'

const { t, locale } = useI18n()

const heroTitleChars = computed(() => [...(t('home.heroTitle') || '')])
const heroDescChars = computed(() => [...(t('home.heroDesc') || '')])

const articles = ref([])
const homePage = ref(0)
const homePageSize = ref(Number(localStorage.getItem('home_page_size')) || 4)
const homeTotalPages = ref(0)
const homeTotalElements = ref(0)
const categories = ref([])
const tags = ref([])
const stats = ref({ articleCount: 0, categoryCount: 0, tagCount: 0 })
const ready = ref(false)
const loading = ref(true)

// Hero theme based on weather
const heroTheme = computed(() => {
  if (!weatherData.value) return 'theme-default'
  const code = weatherData.value.code
  if (code <= 1) return 'theme-sunny'
  if (code <= 3) return 'theme-cloudy'
  if (code >= 45 && code <= 48) return 'theme-fog'
  if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return 'theme-rain'
  if (code >= 71 && code <= 86) return 'theme-snow'
  if (code >= 95) return 'theme-storm'
  return 'theme-default'
})

// Article sort tabs
const articleSort = ref('latest')

// watch sort change -> reload (skip initial fire, onMounted handles it)
watch(articleSort, (val) => {
  if (!ready.value) return
  if (val === 'search') {
    // Switch AI to search mode when search tab is selected
    aiMode.value = 'search'
    aiResponse.value = ''
    aiThinking.value = ''
    aiError.value = ''
    aiNavIntents.value = []
    return
  }
  // Switch AI back to chat mode when leaving search tab
  if (aiMode.value === 'search') {
    aiMode.value = 'chat'
  }
  homePage.value = 0
  loadHomeArticles()
})

const pageSizeItems = computed(() => [4, 8, 12, 16, 20].map(s => ({ value: s, label: `${s} ${t('home.articles')}` })))

let homeLoadRequestId = 0
async function loadHomeArticles() {
  const reqId = ++homeLoadRequestId
  try {
    const data = await request.get(`/api/articles?page=${homePage.value}&size=${homePageSize.value}&sort=${articleSort.value}`)
    if (reqId !== homeLoadRequestId) return
    articles.value = data.content || []
    homeTotalPages.value = data.totalPages || 0
    homeTotalElements.value = data.totalElements || 0
    stats.value.articleCount = data.totalElements || 0
  } catch (e) {
    if (reqId !== homeLoadRequestId) return
    console.error('Failed to load articles:', e)
  }
}

function setHomePageSize(size) {
  homePageSize.value = size
  homePage.value = 0
  localStorage.setItem('home_page_size', size)
  loadHomeArticles()
}

// Page number list with ellipsis logic
const homePageNumbers = computed(() => {
  const total = homeTotalPages.value
  const current = homePage.value + 1
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = []
  pages.push(1)
  if (current > 3) pages.push('...')
  for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) {
    pages.push(i)
  }
  if (current < total - 2) pages.push('...')
  pages.push(total)
  return pages
})

function goHomePage(p) {
  if (p === '...' || p === homePage.value + 1) return
  homePage.value = p - 1
  loadHomeArticles()
}
function goHomePrev() {
  if (homePage.value > 0) {
    homePage.value--
    loadHomeArticles()
  }
}
function goHomeNext() {
  if (homePage.value < homeTotalPages.value - 1) {
    homePage.value++
    loadHomeArticles()
  }
}

// Weather
const weatherData = ref(null)
const weatherProvider = ref({ name: 'Open-Meteo', url: 'https://open-meteo.com' })
const weatherLoading = ref(false)
const weatherFail = ref(false)
const DEFAULT_CITY = '{"name":"广州","nameEn":"Guangzhou","lat":23.13,"lon":113.26,"admin":"广东","adminEn":"Guangdong"}'
let parsedCity
try { parsedCity = JSON.parse(localStorage.getItem('weatherCity') || DEFAULT_CITY) } catch { parsedCity = JSON.parse(DEFAULT_CITY) }
const weatherCity = ref(parsedCity)
const showCityPicker = ref(false)
const citySearch = ref('')

import CITIES from '../data/cities'

function cityName(city) {
  if (locale.value === 'en-US') return city.nameEn || city.name
  return city.name
}
function cityAdmin(city) {
  if (locale.value === 'en-US') return city.adminEn || city.admin
  return city.admin
}

const WMO_CODE_KEYS = {
  0: 'clear', 1: 'mostlyClear', 2: 'partlyCloudy', 3: 'overcast',
  45: 'fog', 48: 'rime',
  51: 'drizzleLight', 53: 'drizzleModerate', 55: 'drizzleHeavy',
  56: 'freezingDrizzleLight', 57: 'freezingDrizzleHeavy',
  61: 'rainLight', 63: 'rainModerate', 65: 'rainHeavy',
  66: 'freezingRainLight', 67: 'freezingRainHeavy',
  71: 'snowLight', 73: 'snowModerate', 75: 'snowHeavy', 77: 'snowGrains',
  80: 'showersLight', 81: 'showersModerate', 82: 'showersHeavy',
  85: 'snowShowersLight', 86: 'snowShowersHeavy',
  95: 'thunderstorm', 96: 'thunderstormHailLight', 99: 'thunderstormHailHeavy',
}

function getWeatherDesc(code) {
  const key = WMO_CODE_KEYS[code]
  return key ? t(`home.weatherDesc.${key}`) : t('home.weatherDesc.unknown')
}

function getWeatherIcon(code) {
  if (code <= 1) return '☀️'
  if (code <= 3) return '⛅'
  if (code <= 48) return '🌫️'
  if (code <= 57) return '🌦️'
  if (code <= 67) return '🌧️'
  if (code <= 77) return '🌨️'
  if (code <= 82) return '🌧️'
  if (code <= 86) return '❄️'
  if (code >= 95) return '⛈️'
  return '🌤️'
}

function getWeatherTip(code, temp) {
  if (code >= 95) return t('home.weatherTip.thunderstorm')
  if (code >= 80 || (code >= 61 && code <= 67)) return t('home.weatherTip.rain')
  if (code >= 71 && code <= 86) return t('home.weatherTip.wet')
  if (code >= 45 && code <= 48) return t('home.weatherTip.fog')
  if (temp >= 35) return t('home.weatherTip.hot')
  if (temp <= 0) return t('home.weatherTip.cold')
  return t('home.weatherTip.nice')
}

function getWeatherBg(code) {
  if (code <= 1) return 'linear-gradient(135deg, #1e6cb6, #3b9edd)'
  if (code <= 3) return 'linear-gradient(135deg, #4a6fa1, #7ba3c9)'
  if (code <= 48) return 'linear-gradient(135deg, #7a8a9a, #a0b0c0)'
  if (code <= 67) return 'linear-gradient(135deg, #3a5a8a, #5a7aaa)'
  if (code <= 86) return 'linear-gradient(135deg, #4a6080, #6a8aaa)'
  if (code >= 95) return 'linear-gradient(135deg, #2a3a5a, #4a5a7a)'
  return 'linear-gradient(135deg, #1e6cb6, #3b9edd)'
}

const filteredCities = computed(() => {
  const q = citySearch.value.trim().toLowerCase()
  if (!q) return CITIES
  return CITIES.filter(c =>
    c.name.toLowerCase().includes(q) ||
    (c.nameEn && c.nameEn.toLowerCase().includes(q)) ||
    (c.admin && c.admin.toLowerCase().includes(q)) ||
    (c.adminEn && c.adminEn.toLowerCase().includes(q)) ||
    (c.country && c.country.toLowerCase().includes(q)) ||
    (c.countryEn && c.countryEn.toLowerCase().includes(q))
  )
})

async function fetchWeather() {
  weatherLoading.value = true
  weatherFail.value = false
  try {
    const city = weatherCity.value
    const url = `/api/site/weather?lat=${city.lat}&lon=${city.lon}`
    const res = await fetch(url)
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const data = await res.json()

    // 来源信息
    weatherProvider.value = {
      name: data.providerName || 'Open-Meteo',
      url: data.providerUrl || 'https://open-meteo.com'
    }

    // 统一数据源：优先用 current/daily，其次解析 raw
    let current, daily
    if (data.current && data.daily) {
      current = data.current
      daily = data.daily
    } else if (data.raw) {
      const raw = typeof data.raw === 'string' ? JSON.parse(data.raw) : data.raw
      current = raw.current
      daily = raw.daily
    } else {
      throw new Error('无法解析天气数据')
    }

    weatherData.value = {
      temp: Math.round(current.temperature_2m),
      code: current.weather_code,
      desc: getWeatherDesc(current.weather_code),
      icon: getWeatherIcon(current.weather_code),
      tip: getWeatherTip(current.weather_code, Math.round(current.temperature_2m)),
      min: Math.round(daily.temperature_2m_min[0]),
      max: Math.round(daily.temperature_2m_max[0]),
      forecast: daily.time.slice(1).map((d, i) => ({
        day: new Date(d).toLocaleDateString(locale.value, { weekday: 'short' }),
        icon: getWeatherIcon(daily.weather_code[i + 1]),
        min: Math.round(daily.temperature_2m_min[i + 1]),
        max: Math.round(daily.temperature_2m_max[i + 1]),
      })),
    }
  } catch {
    weatherFail.value = true
  } finally {
    weatherLoading.value = false
  }
}

function selectCity(city) {
  weatherCity.value = city
  localStorage.setItem('weatherCity', JSON.stringify(city))
  showCityPicker.value = false
  citySearch.value = ''
  fetchWeather()
}

// Update time every minute
const nowTime = ref(new Date().toLocaleTimeString(locale.value, { hour: '2-digit', minute: '2-digit' }))
const nowDate = ref(new Date().toLocaleDateString(locale.value, { weekday: 'short', month: '2-digit', day: '2-digit' }))
let timeTimer = null

// Guide cards
const guideSvgs = [
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" fill="none"><rect x="6" y="4" width="28" height="36" rx="3" stroke="#3b82f6" stroke-width="2.5" fill="none"/><path d="M12 14h16M12 20h12M12 26h14" stroke="#3b82f6" stroke-width="2" stroke-linecap="round"/><path d="M34 12c6 0 10 4 10 10s-4 10-10 10" stroke="#93c5fd" stroke-width="2.5" stroke-linecap="round" fill="none"/><circle cx="34" cy="32" r="8" fill="#3b82f6" opacity="0.15"/><path d="M31 32l2 2 4-4" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" fill="none"><rect x="4" y="8" width="40" height="30" rx="4" stroke="#ec4899" stroke-width="2.5" fill="none"/><path d="M4 18h40" stroke="#ec4899" stroke-width="2.5"/><circle cx="10" cy="13" r="2" fill="#f472b6"/><circle cx="16" cy="13" r="2" fill="#fbbf24"/><circle cx="22" cy="13" r="2" fill="#34d399"/><path d="M14 26l-4 6h8l-4-6z" fill="#ec4899" opacity="0.3"/><path d="M24 24l6 8h-12l6-8z" fill="#ec4899" opacity="0.5"/><path d="M34 22l4 10h-8l4-10z" fill="#ec4899" opacity="0.3"/><rect x="16" y="40" width="16" height="3" rx="1.5" fill="#e5e7eb"/></svg>`,
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" fill="none"><circle cx="24" cy="24" r="18" stroke="#0ea5e9" stroke-width="2.5" fill="none"/><circle cx="24" cy="24" r="14" stroke="#0ea5e9" stroke-width="1" opacity="0.3" fill="none"/><path d="M24 10v14l8 8" stroke="#0ea5e9" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="24" cy="24" r="3" fill="#0ea5e9" opacity="0.2"/><circle cx="24" cy="24" r="1.5" fill="#0ea5e9"/><path d="M8 6l4 4M40 6l-4 4" stroke="#7dd3fc" stroke-width="2" stroke-linecap="round"/></svg>`,
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" fill="none"><circle cx="24" cy="16" r="8" stroke="#10b981" stroke-width="2.5" fill="none"/><path d="M10 40c0-8 6-14 14-14s14 6 14 14" stroke="#10b981" stroke-width="2.5" stroke-linecap="round" fill="none"/><circle cx="24" cy="16" r="3" fill="#10b981" opacity="0.2"/><path d="M36 14c2 0 4 2 4 4s-2 4-4 4" stroke="#6ee7b7" stroke-width="2" stroke-linecap="round" fill="none"/><circle cx="38" cy="12" r="3" fill="#10b981" opacity="0.15"/></svg>`
]
const guideKeys = ['guideArticles', 'guideProjects', 'guideArchive', 'guideAbout']
const guideDescKeys = ['guideArticlesDesc', 'guideProjectsDesc', 'guideArchiveDesc', 'guideAboutDesc']
const guideTos = ['/articles', '/projects', '/archive', '/about']
const guides = computed(() => guideKeys.map((key, i) => ({
  title: t(`home.${key}`), desc: t(`home.${guideDescKeys[i]}`), to: guideTos[i], svg: guideSvgs[i]
})))

// Announcements
const notices = ref([])
const topNotice = computed(() => notices.value?.[0] || null)
const isEn = computed(() => locale.value === 'en-US')
function noticeTitle(n) { return n && isEn.value && n.titleEn ? n.titleEn : (n?.title || '') }
function noticeContent(n) { return n && isEn.value && n.contentEn ? n.contentEn : (n?.content || '') }
function noticeTag(n) { return n && isEn.value && n.tagEn ? n.tagEn : (n?.tag || '') }
function articleTitle(a) { return a && isEn.value && a.titleEn ? a.titleEn : (a?.title || '') }
function articleSummary(a) { return a && isEn.value && a.summaryEn ? a.summaryEn : (a?.summary || '') }

// AI state
const aiEnabled = ref(false)
const aiQuery = ref('')
const aiResponse = ref('')
const aiThinking = ref('')
const thinkingExpanded = ref(false)
const aiError = ref('')
const aiLoading = ref(false)
const aiMode = ref('chat') // 'chat' | 'search'
const aiNavIntents = ref([])
const searchResults = ref([])
const searchLoading = ref(false)
const router = useRouter()

onMounted(async () => {
  // Trigger animations after mount
  ready.value = true

  const [catRes, tagRes, announcementRes, aiStatusRes] = await Promise.allSettled([
    cachedFetch('home:categories', () => request.get('/api/categories'), 300000),
    cachedFetch('home:tags', () => request.get('/api/tags'), 300000),
    cachedFetch('home:announcements', () => request.get('/api/announcements'), 120000),
    checkAiStatus(),
  ])
  await loadHomeArticles()
  loading.value = false
  if (catRes.status === 'fulfilled') {
    categories.value = catRes.value || []
    stats.value.categoryCount = catRes.value?.length || 0
  }
  if (tagRes.status === 'fulfilled') {
    tags.value = tagRes.value || []
    stats.value.tagCount = tagRes.value?.length || 0
  }
  if (announcementRes.status === 'fulfilled') {
    notices.value = announcementRes.value || []
  }
  if (aiStatusRes.status === 'fulfilled') {
    aiEnabled.value = aiStatusRes.value?.configured === true
  }

  // Fetch weather
  fetchWeather()
  timeTimer = setInterval(() => {
    nowTime.value = new Date().toLocaleTimeString(locale.value, { hour: '2-digit', minute: '2-digit' })
    nowDate.value = new Date().toLocaleDateString(locale.value, { weekday: 'short', month: '2-digit', day: '2-digit' })
  }, 60000)
})

let abortAi = null

onBeforeUnmount(() => {
  if (abortAi) {
    abortAi()
    abortAi = null
  }
  if (timeTimer) {
    clearInterval(timeTimer)
    timeTimer = null
  }
})

// Intent detection
const intentPatterns = computed(() => [
  { to: '/articles', icon: '📖', label: t('home.viewArticlesLabel'), keywords: ['文章', '博客', '笔记', '帖子', '阅读', 'article', 'blog', 'post', 'read'] },
  { to: '/projects', icon: '💻', label: t('home.viewProjectsLabel'), keywords: ['作品集', '项目', '作品', 'demo', 'portfolio', 'project'] },
  { to: '/archive', icon: '🔍', label: t('home.archiveLabel'), keywords: ['归档', '存档', '历史', '时间线', 'archive', 'timeline', 'history'] },
  { to: '/about', icon: '💡', label: t('home.aboutLabel'), keywords: ['关于', '联系', '博主', '联系方式', '站长', '介绍', 'about', 'contact', 'author'] }
])

function detectIntent(query) {
  const q = query.toLowerCase()
  const matched = []
  for (const p of intentPatterns.value) {
    if (p.keywords.some(kw => q.includes(kw))) {
      matched.push({ to: p.to, icon: p.icon, label: p.label })
    }
  }
  return matched
}

function switchAiMode(mode) {
  aiMode.value = mode
  aiResponse.value = ''
  aiThinking.value = ''
  aiError.value = ''
  aiNavIntents.value = []
  if (mode === 'search') {
    articleSort.value = 'search'
  } else if (articleSort.value === 'search') {
    articleSort.value = 'latest'
  }
}

function handleAiSubmit() {
  if (!aiQuery.value.trim()) return
  if (aiMode.value === 'search') {
    handleArticleSearch()
  } else {
    handleAiChat()
  }
}

async function handleAiChat() {
  if (!aiQuery.value.trim() || aiLoading.value) return

  // Detect intent before sending to AI
  const intents = detectIntent(aiQuery.value)
  if (intents.length) {
    aiNavIntents.value = intents
  } else {
    aiNavIntents.value = []
  }

  aiLoading.value = true
  aiResponse.value = ''
  aiThinking.value = ''
  thinkingExpanded.value = false
  aiError.value = ''
  searchResults.value = []
  abortAi = aiChatStream(aiQuery.value, {
    onThinking(content) {
      aiThinking.value += content
      thinkingExpanded.value = true
    },
    onChunk(content) {
      aiResponse.value += content
    },
    onDone() {
      aiLoading.value = false
    },
    onError(e) {
      aiError.value = e.message || t('home.aiServiceUnavailable')
      aiLoading.value = false
    }
  })
}

async function handleArticleSearch() {
  if (!aiQuery.value.trim() || searchLoading.value) return
  searchLoading.value = true
  aiError.value = ''
  aiResponse.value = ''
  aiThinking.value = ''
  aiNavIntents.value = []
  // Auto-switch to search tab
  articleSort.value = 'search'
  try {
    const data = await smartSearch(aiQuery.value)
    searchResults.value = data?.content || []
    if (!searchResults.value.length) {
      aiError.value = t('home.noRelatedArticles')
    }
  } catch (e) {
    aiError.value = e.message || t('home.searchFailed')
  } finally {
    searchLoading.value = false
  }
}

function clearSearchResults() {
  searchResults.value = []
  aiError.value = ''
}

// Notice helpers
function isRecent(dateStr) {
  if (!dateStr) return false
  const d = new Date(dateStr)
  return Date.now() - d.getTime() < 7 * 24 * 60 * 60 * 1000
}
function noticeColorKey(n) {
  if (n.level && n.level !== 'info') return n.level
  const type = (n.type || '').toLowerCase()
  if (type === 'feature') return 'success'
  if (type === 'update') return 'warning'
  return 'info'
}
function waveColor(n) {
  const m = { info: '#3b82f63a', success: '#22c55e3a', warning: '#f59e0b3a', error: '#ef44443a' }
  return m[noticeColorKey(n)]
}
function iconColor(n) {
  const m = { info: '#3b82f6', success: '#22c55e', warning: '#f59e0b', error: '#ef4444' }
  return m[noticeColorKey(n)]
}

// Notice detail
const noticeDetail = ref(null)
const modalRef = ref(null)
function showNotice(n) {
  noticeDetail.value = n
}
function onNoticeMaskMouseDown(e) {
  if (e.button === 0 && !e.ctrlKey && !e.metaKey && !e.shiftKey && !e.altKey) {
    noticeDetail.value = null
  }
}
watch(noticeDetail, (val) => {
  if (val) nextTick(() => modalRef.value?.focus())
})
</script>

<style scoped>
/* Notice Banner */
.notice-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1.1rem;
  border-radius: 12px;
  margin-bottom: 1.25rem;
  cursor: pointer;
  transition: all 0.25s;
}
.notice-banner:hover {
  transform: translateY(-1px);
}
/* Level colors — banner */
.notice-banner--info { background: linear-gradient(135deg, #eff6ff, #dbeafe); border: 1px solid #93c5fd; }
.notice-banner--info:hover { box-shadow: 0 2px 12px rgba(59, 130, 246, 0.15); }
.notice-banner--info .notice-icon { background: rgba(59, 130, 246, 0.1); }
.notice-banner--info .notice-banner-title { color: #1e40af; }
.notice-banner--info .notice-banner-text { color: #3b82f6; }
.notice-banner--info .notice-banner-arrow { color: #3b82f6; }

.notice-banner--success { background: linear-gradient(135deg, #ecfdf5, #d1fae5); border: 1px solid #6ee7b7; }
.notice-banner--success:hover { box-shadow: 0 2px 12px rgba(16, 185, 129, 0.15); }
.notice-banner--success .notice-icon { background: rgba(16, 185, 129, 0.1); }
.notice-banner--success .notice-banner-title { color: #065f46; }
.notice-banner--success .notice-banner-text { color: #10b981; }
.notice-banner--success .notice-banner-arrow { color: #10b981; }

.notice-banner--warning { background: linear-gradient(135deg, #fffbeb, #fef3c7); border: 1px solid #fcd34d; }
.notice-banner--warning:hover { box-shadow: 0 2px 12px rgba(245, 158, 11, 0.15); }
.notice-banner--warning .notice-icon { background: rgba(245, 158, 11, 0.1); }
.notice-banner--warning .notice-banner-title { color: #92400e; }
.notice-banner--warning .notice-banner-text { color: #a16207; }
.notice-banner--warning .notice-banner-arrow { color: #d97706; }

.notice-banner--error { background: linear-gradient(135deg, #fef2f2, #fee2e2); border: 1px solid #fca5a5; }
.notice-banner--error:hover { box-shadow: 0 2px 12px rgba(239, 68, 68, 0.15); }
.notice-banner--error .notice-icon { background: rgba(239, 68, 68, 0.1); }
.notice-banner--error .notice-banner-title { color: #991b1b; }
.notice-banner--error .notice-banner-text { color: #ef4444; }
.notice-banner--error .notice-banner-arrow { color: #ef4444; }

.notice-icon {
  font-size: 1.3rem;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
}
.notice-banner-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}
.notice-banner-title {
  font-size: 0.88rem;
  font-weight: 600;
}
.notice-banner-text {
  font-size: 0.8rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-banner-arrow {
  font-size: 1.2rem;
  flex-shrink: 0;
  opacity: 0.5;
  transition: opacity 0.2s;
}
.notice-banner:hover .notice-banner-arrow {
  opacity: 1;
}

/* Notice Alert style */
.notice-alert {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  border-left: 4px solid;
  margin-bottom: 1.25rem;
  cursor: pointer;
  transition: all 0.3s;
}
.notice-alert:hover { transform: translateX(2px); }
.notice-alert--info { background: #eff6ff; border-color: #3b82f6; }
.notice-alert--info:hover { background: #dbeafe; }
.notice-alert--info .notice-alert-icon { color: #3b82f6; }
.notice-alert--info .notice-alert-title { color: #1e40af; }
.notice-alert--info .notice-alert-text { color: #3b82f6; }

.notice-alert--success { background: #ecfdf5; border-color: #10b981; }
.notice-alert--success:hover { background: #d1fae5; }
.notice-alert--success .notice-alert-icon { color: #10b981; }
.notice-alert--success .notice-alert-title { color: #065f46; }
.notice-alert--success .notice-alert-text { color: #10b981; }

.notice-alert--warning { background: #fffbeb; border-color: #f59e0b; }
.notice-alert--warning:hover { background: #fef3c7; }
.notice-alert--warning .notice-alert-icon { color: #f59e0b; }
.notice-alert--warning .notice-alert-title { color: #92400e; }
.notice-alert--warning .notice-alert-text { color: #a16207; }

.notice-alert--error { background: #fef2f2; border-color: #ef4444; }
.notice-alert--error:hover { background: #fee2e2; }
.notice-alert--error .notice-alert-icon { color: #ef4444; }
.notice-alert--error .notice-alert-title { color: #991b1b; }
.notice-alert--error .notice-alert-text { color: #ef4444; }

.notice-alert-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}
.notice-alert-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}
.notice-alert-title {
  font-size: 0.88rem;
  font-weight: 600;
}
.notice-alert-text {
  font-size: 0.78rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Hero */
.hero {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 2rem;
  padding: 48px 40px 42px;
  transition: background 1s ease;
  /* Default blue sky */
  background: linear-gradient(180deg, #4a90d9 0%, #6ba3e0 40%, #87bcf0 100%);
}
/* Weather themes */
.hero.theme-sunny {
  background: linear-gradient(180deg, #1a6bc4 0%, #3a8ee0 30%, #5ba8e8 60%, #87bcf0 100%);
}
.hero.theme-cloudy {
  background: linear-gradient(180deg, #5a7a94 0%, #7a9ab4 40%, #9ab8d0 100%);
}
.hero.theme-rain {
  background: linear-gradient(180deg, #2a3a4c 0%, #3a4f65 40%, #4a6379 100%);
}
.hero.theme-snow {
  background: linear-gradient(180deg, #7a94ac 0%, #98b0cc 40%, #b5c8e0 100%);
}
.hero.theme-fog {
  background: linear-gradient(180deg, #6a7a8a 0%, #8595a5 40%, #a0b0c0 100%);
}
.hero.theme-storm {
  background: linear-gradient(180deg, #1a2530 0%, #2a3545 40%, #3a4555 100%);
}
.hero.theme-default {
  background: linear-gradient(180deg, #1a6bc4 0%, #3a8ee0 30%, #5ba8e8 60%, #87bcf0 100%);
}
/* SVG cross pattern */
.hero-bg {
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  pointer-events: none;
}
/* Weather info chip */
.hero-weather {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 3;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.22);
  color: #fff;
  font-size: 0.8rem;
  cursor: pointer;
  transition: background 0.25s, transform 0.25s;
}
.hero-weather:hover {
  background: rgba(255, 255, 255, 0.26);
  transform: translateY(-1px);
}
.hero-weather-icon { font-size: 1.05rem; line-height: 1; }
.hero-weather-desc { opacity: 0.92; }
.hero-weather-temp { font-weight: 700; margin-left: 2px; }

/* Soft light orbs (bokeh depth) */
.hero-orbs {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(44px);
  opacity: 0.32;
  animation: orbFloat 18s ease-in-out infinite;
}
.orb:nth-child(1) { width: 190px; height: 190px; background: rgba(255, 255, 255, 0.2); top: -50px; left: -50px; }
.orb:nth-child(2) { width: 150px; height: 150px; background: rgba(255, 216, 130, 0.16); bottom: -40px; left: 30%; animation-delay: -6s; }
.orb:nth-child(3) { width: 210px; height: 210px; background: rgba(130, 205, 255, 0.18); top: 22%; right: -70px; animation-delay: -12s; }
.orb:nth-child(4) { width: 110px; height: 110px; background: rgba(255, 255, 255, 0.18); bottom: 12%; right: 24%; animation-delay: -9s; }
@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(18px, 26px); }
}

/* Sun */
.hero-sun {
  position: absolute;
  top: 18%;
  right: 12%;
  width: 80px;
  height: 80px;
  pointer-events: none;
  opacity: 0;
  transition: opacity 1s ease;
}
.hero.theme-sunny .hero-sun,
.hero.theme-default .hero-sun { opacity: 1; }
.sun-core {
  position: absolute;
  inset: 15px;
  border-radius: 50%;
  background: radial-gradient(circle, #fff6d5 0%, #ffdd57 40%, #ffaa00 100%);
  box-shadow: 0 0 40px rgba(255, 200, 0, 0.6), 0 0 80px rgba(255, 170, 0, 0.3);
}
.sun-glow {
  position: absolute;
  inset: -10px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 220, 100, 0.3) 0%, transparent 70%);
  animation: sunPulse 3s ease-in-out infinite;
}
@keyframes sunPulse {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.15); opacity: 1; }
}
.sun-rays {
  position: absolute;
  inset: -20px;
  animation: sunRaysSpin 30s linear infinite;
}
.ray {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 2px;
  height: 18px;
  background: linear-gradient(to top, rgba(255, 200, 0, 0.5), transparent);
  transform-origin: 50% 0;
  border-radius: 1px;
}
.ray:nth-child(1) { transform: rotate(0deg) translateY(-42px); }
.ray:nth-child(2) { transform: rotate(45deg) translateY(-42px); }
.ray:nth-child(3) { transform: rotate(90deg) translateY(-42px); }
.ray:nth-child(4) { transform: rotate(135deg) translateY(-42px); }
.ray:nth-child(5) { transform: rotate(180deg) translateY(-42px); }
.ray:nth-child(6) { transform: rotate(225deg) translateY(-42px); }
.ray:nth-child(7) { transform: rotate(270deg) translateY(-42px); }
.ray:nth-child(8) { transform: rotate(315deg) translateY(-42px); }
@keyframes sunRaysSpin {
  to { transform: rotate(360deg); }
}

/* Clouds */
.hero-clouds {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.cloud {
  position: absolute;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 50%;
  filter: blur(1px);
}
.cloud::before,
.cloud::after {
  content: '';
  position: absolute;
  background: inherit;
  border-radius: 50%;
}
.cloud-1 {
  width: 90px; height: 30px;
  top: 15%; left: -90px;
  animation: cloudDrift 22s linear infinite;
}
.cloud-1::before { width: 44px; height: 44px; top: -20px; left: 16px; }
.cloud-1::after { width: 34px; height: 34px; top: -14px; left: 46px; }
.cloud-2 {
  width: 65px; height: 22px;
  top: 30%; left: -65px;
  animation: cloudDrift 30s linear infinite;
  animation-delay: -8s;
  opacity: 0.7;
}
.cloud-2::before { width: 32px; height: 32px; top: -15px; left: 10px; }
.cloud-2::after { width: 26px; height: 26px; top: -11px; left: 32px; }
.cloud-3 {
  width: 110px; height: 36px;
  top: 50%; left: -110px;
  animation: cloudDrift 36s linear infinite;
  animation-delay: -18s;
  opacity: 0.5;
}
.cloud-3::before { width: 55px; height: 55px; top: -24px; left: 22px; }
.cloud-3::after { width: 40px; height: 40px; top: -16px; left: 60px; }
.cloud-4 {
  width: 70px; height: 24px;
  top: 70%; left: -70px;
  animation: cloudDrift 28s linear infinite;
  animation-delay: -14s;
  opacity: 0.6;
}
.cloud-4::before { width: 35px; height: 35px; top: -16px; left: 12px; }
.cloud-4::after { width: 28px; height: 28px; top: -12px; left: 35px; }
.cloud-5 {
  width: 50px; height: 18px;
  top: 25%; left: -50px;
  animation: cloudDrift 38s linear infinite;
  animation-delay: -25s;
  opacity: 0.4;
}
.cloud-5::before { width: 25px; height: 25px; top: -12px; left: 8px; }
.cloud-5::after { width: 20px; height: 20px; top: -8px; left: 25px; }
.cloud-6 {
  width: 95px; height: 32px;
  top: 42%; left: -95px;
  animation: cloudDrift 33s linear infinite;
  animation-delay: -5s;
  opacity: 0.55;
}
.cloud-6::before { width: 48px; height: 48px; top: -22px; left: 18px; }
.cloud-6::after { width: 36px; height: 36px; top: -14px; left: 52px; }
.cloud-7 {
  width: 55px; height: 20px;
  top: 60%; left: -55px;
  animation: cloudDrift 26s linear infinite;
  animation-delay: -22s;
  opacity: 0.45;
}
.cloud-7::before { width: 28px; height: 28px; top: -13px; left: 9px; }
.cloud-7::after { width: 22px; height: 22px; top: -9px; left: 28px; }
.cloud-8 {
  width: 75px; height: 26px;
  top: 80%; left: -75px;
  animation: cloudDrift 42s linear infinite;
  animation-delay: -30s;
  opacity: 0.35;
}
.cloud-8::before { width: 38px; height: 38px; top: -17px; left: 14px; }
.cloud-8::after { width: 30px; height: 30px; top: -12px; left: 40px; }
@keyframes cloudDrift {
  0% { transform: translateX(0); }
  100% { transform: translateX(calc(100vw + 200px)); }
}

/* Fog */
.hero-fog {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  opacity: 0;
  transition: opacity 1s;
}
.hero.theme-fog .hero-fog { opacity: 1; }
.fog-layer {
  position: absolute;
  width: 200%;
  height: 100%;
  background: repeating-linear-gradient(
    90deg,
    rgba(255, 255, 255, 0) 0%,
    rgba(255, 255, 255, 0.08) 20%,
    rgba(255, 255, 255, 0.15) 40%,
    rgba(255, 255, 255, 0.08) 60%,
    rgba(255, 255, 255, 0) 80%
  );
}
.fog-1 {
  top: 30%;
  animation: fogDrift 20s ease-in-out infinite;
  opacity: 0.6;
}
.fog-2 {
  top: 55%;
  animation: fogDrift 28s ease-in-out infinite reverse;
  opacity: 0.4;
}
@keyframes fogDrift {
  0%, 100% { transform: translateX(-25%); }
  50% { transform: translateX(0%); }
}

/* Lightning */
.hero-lightning {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0;
  background: rgba(255, 255, 255, 0.15);
  transition: opacity 0.05s;
}
.hero.theme-storm .hero-lightning {
  animation: lightningFlash 4s ease-in-out infinite;
}
@keyframes lightningFlash {
  0%, 89%, 91%, 93%, 100% { opacity: 0; }
  90% { opacity: 1; }
  92% { opacity: 0.6; }
}
/* Bubbles */
.hero-bubbles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.bubble {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.3), rgba(255, 255, 255, 0.05));
  border: 1px solid rgba(255, 255, 255, 0.15);
  animation: bubbleFloat linear infinite;
}
.bubble:nth-child(1) { width: 8px; height: 8px; left: 5%; animation-duration: 12s; animation-delay: 0s; }
.bubble:nth-child(2) { width: 12px; height: 12px; left: 15%; animation-duration: 10s; animation-delay: -2s; }
.bubble:nth-child(3) { width: 6px; height: 6px; left: 30%; animation-duration: 14s; animation-delay: -4s; }
.bubble:nth-child(4) { width: 10px; height: 10px; left: 45%; animation-duration: 11s; animation-delay: -1s; }
.bubble:nth-child(5) { width: 14px; height: 14px; left: 60%; animation-duration: 13s; animation-delay: -6s; }
.bubble:nth-child(6) { width: 7px; height: 7px; left: 72%; animation-duration: 9s; animation-delay: -3s; }
.bubble:nth-child(7) { width: 11px; height: 11px; left: 85%; animation-duration: 15s; animation-delay: -8s; }
.bubble:nth-child(8) { width: 9px; height: 9px; left: 95%; animation-duration: 10s; animation-delay: -5s; }
@keyframes bubbleFloat {
  0% { bottom: -5%; opacity: 0; transform: translateX(0) scale(1); }
  10% { opacity: 0.7; }
  50% { transform: translateX(15px) scale(1.05); }
  90% { opacity: 0.2; }
  100% { bottom: 105%; opacity: 0; transform: translateX(-10px) scale(0.9); }
}
/* Particles */
.hero-particles {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  animation: floatUp linear infinite;
}
.particle:nth-child(1) { left: 8%; animation-duration: 8s; animation-delay: 0s; width: 3px; height: 3px; }
.particle:nth-child(2) { left: 22%; animation-duration: 12s; animation-delay: 2s; width: 5px; height: 5px; }
.particle:nth-child(3) { left: 40%; animation-duration: 9s; animation-delay: 4s; }
.particle:nth-child(4) { left: 58%; animation-duration: 11s; animation-delay: 1s; width: 3px; height: 3px; }
.particle:nth-child(5) { left: 75%; animation-duration: 10s; animation-delay: 3s; width: 5px; height: 5px; }
.particle:nth-child(6) { left: 90%; animation-duration: 7s; animation-delay: 5s; }
@keyframes floatUp {
  0% { bottom: -5%; opacity: 0; }
  10% { opacity: 0.5; }
  90% { opacity: 0.2; }
  100% { bottom: 105%; opacity: 0; }
}
/* Rain theme extras */
.hero.theme-rain .bubble { background: radial-gradient(circle at 30% 30%, rgba(200, 220, 255, 0.3), rgba(200, 220, 255, 0.05)); }
.hero.theme-rain .cloud { background: rgba(255, 255, 255, 0.1); }
.hero.theme-rain .cloud::before, .hero.theme-rain .cloud::after { background: rgba(255, 255, 255, 0.1); }
.hero.theme-snow .bubble { background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.5), rgba(255, 255, 255, 0.15)); border-color: rgba(255, 255, 255, 0.3); }
.hero.theme-storm .cloud { opacity: 0.3; background: rgba(200, 200, 200, 0.15); }
.hero.theme-storm .cloud::before, .hero.theme-storm .cloud::after { background: rgba(200, 200, 200, 0.15); }
.hero.theme-fog .cloud { opacity: 0.8; background: rgba(255, 255, 255, 0.25); }
.hero.theme-fog .cloud::before, .hero.theme-fog .cloud::after { background: rgba(255, 255, 255, 0.25); }
/* Rain drops */
.hero-rain {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  opacity: 0;
  transition: opacity 0.8s;
}
.hero.theme-rain .hero-rain,
.hero.theme-storm .hero-rain { opacity: 1; }
.raindrop {
  position: absolute;
  width: 2px;
  background: linear-gradient(to bottom, transparent, rgba(200, 220, 255, 0.5));
  border-radius: 0 0 2px 2px;
  animation: rainFall linear infinite;
}
.raindrop:nth-child(1) { left: 5%; height: 20px; animation-duration: 0.6s; animation-delay: 0s; }
.raindrop:nth-child(2) { left: 15%; height: 28px; animation-duration: 0.5s; animation-delay: -0.2s; }
.raindrop:nth-child(3) { left: 25%; height: 18px; animation-duration: 0.7s; animation-delay: -0.4s; }
.raindrop:nth-child(4) { left: 35%; height: 24px; animation-duration: 0.55s; animation-delay: -0.1s; }
.raindrop:nth-child(5) { left: 45%; height: 22px; animation-duration: 0.65s; animation-delay: -0.5s; }
.raindrop:nth-child(6) { left: 55%; height: 26px; animation-duration: 0.5s; animation-delay: -0.3s; }
.raindrop:nth-child(7) { left: 65%; height: 20px; animation-duration: 0.7s; animation-delay: -0.6s; }
.raindrop:nth-child(8) { left: 75%; height: 30px; animation-duration: 0.45s; animation-delay: -0.15s; }
.raindrop:nth-child(9) { left: 85%; height: 18px; animation-duration: 0.6s; animation-delay: -0.45s; }
.raindrop:nth-child(10) { left: 93%; height: 24px; animation-duration: 0.55s; animation-delay: -0.35s; }
.raindrop:nth-child(11) { left: 10%; height: 22px; animation-duration: 0.62s; animation-delay: -0.18s; }
.raindrop:nth-child(12) { left: 22%; height: 26px; animation-duration: 0.48s; animation-delay: -0.55s; }
.raindrop:nth-child(13) { left: 33%; height: 19px; animation-duration: 0.72s; animation-delay: -0.08s; }
.raindrop:nth-child(14) { left: 42%; height: 28px; animation-duration: 0.52s; animation-delay: -0.42s; }
.raindrop:nth-child(15) { left: 50%; height: 21px; animation-duration: 0.58s; animation-delay: -0.28s; }
.raindrop:nth-child(16) { left: 58%; height: 25px; animation-duration: 0.66s; animation-delay: -0.52s; }
.raindrop:nth-child(17) { left: 68%; height: 17px; animation-duration: 0.54s; animation-delay: -0.12s; }
.raindrop:nth-child(18) { left: 78%; height: 30px; animation-duration: 0.46s; animation-delay: -0.38s; }
.raindrop:nth-child(19) { left: 88%; height: 20px; animation-duration: 0.68s; animation-delay: -0.62s; }
.raindrop:nth-child(20) { left: 96%; height: 23px; animation-duration: 0.56s; animation-delay: -0.22s; }
@keyframes rainFall {
  0% { top: -10%; opacity: 0; transform: translateX(0); }
  10% { opacity: 1; }
  90% { opacity: 0.6; }
  100% { top: 105%; opacity: 0; transform: translateX(-38px); }
}
/* Snowflakes */
.hero-snow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  opacity: 0;
  transition: opacity 0.8s;
}
.hero.theme-snow .hero-snow { opacity: 1; }
.snowflake {
  position: absolute;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  animation: snowFall linear infinite;
}
.snowflake:nth-child(1) { left: 8%; animation-duration: 8s; animation-delay: 0s; font-size: 10px; }
.snowflake:nth-child(2) { left: 20%; animation-duration: 10s; animation-delay: -2s; font-size: 14px; }
.snowflake:nth-child(3) { left: 35%; animation-duration: 9s; animation-delay: -4s; font-size: 8px; }
.snowflake:nth-child(4) { left: 48%; animation-duration: 11s; animation-delay: -1s; font-size: 12px; }
.snowflake:nth-child(5) { left: 60%; animation-duration: 7s; animation-delay: -5s; font-size: 16px; }
.snowflake:nth-child(6) { left: 72%; animation-duration: 12s; animation-delay: -3s; font-size: 10px; }
.snowflake:nth-child(7) { left: 82%; animation-duration: 8s; animation-delay: -6s; font-size: 14px; }
.snowflake:nth-child(8) { left: 95%; animation-duration: 10s; animation-delay: -7s; font-size: 9px; }
.snowflake:nth-child(9) { left: 14%; animation-duration: 11s; animation-delay: -3s; font-size: 11px; }
.snowflake:nth-child(10) { left: 28%; animation-duration: 8s; animation-delay: -8s; font-size: 13px; }
.snowflake:nth-child(11) { left: 42%; animation-duration: 9s; animation-delay: -1.5s; font-size: 7px; }
.snowflake:nth-child(12) { left: 55%; animation-duration: 12s; animation-delay: -4.5s; font-size: 15px; }
.snowflake:nth-child(13) { left: 67%; animation-duration: 7s; animation-delay: -6.5s; font-size: 10px; }
.snowflake:nth-child(14) { left: 78%; animation-duration: 10s; animation-delay: -2.5s; font-size: 8px; }
.snowflake:nth-child(15) { left: 90%; animation-duration: 9s; animation-delay: -5.5s; font-size: 12px; }
@keyframes snowFall {
  0% { top: -5%; opacity: 0; transform: translateX(0) rotate(0deg); }
  10% { opacity: 0.8; }
  50% { transform: translateX(20px) rotate(180deg); }
  90% { opacity: 0.4; }
  100% { top: 105%; opacity: 0; transform: translateX(-15px) rotate(360deg); }
}
/* Content */
.hero-inner {
  position: relative;
  z-index: 2;
  text-align: center;
}
.hero-inner h1 {
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin: 0 0 0.5rem;
  color: #fff;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.1);
}
.hero-desc {
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.95rem;
  margin: 0 0 1.5rem;
}
.hero-char {
  display: inline-block;
  opacity: 0;
  filter: blur(8px);
  transform: translateY(16px);
  animation: heroCharIn 0.7s cubic-bezier(0.22, 1, 0.36, 1) forwards;
  will-change: transform, opacity, filter;
}
@keyframes heroCharIn {
  to { opacity: 1; transform: translateY(0); filter: blur(0); }
}
.hero-links {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}
.btn-primary {
  display: inline-block;
  padding: 0.55rem 1.6rem;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.3s;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
  border: none;
  cursor: pointer;
}
.btn-primary:hover {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.45);
  transform: translateY(-2px);
}
.btn-ghost {
  display: inline-block;
  padding: 0.55rem 1.6rem;
  color: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.08);
  transition: all 0.3s;
  cursor: pointer;
}
.btn-ghost:hover {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.18);
  transform: translateY(-2px);
}

/* Guide Section - Glassmorphism cards */
.guide-section {
  margin-bottom: 1.5rem;
}
.guide-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}
.guide-item {
  box-sizing: border-box;
  background: rgba(217, 217, 217, 0.58);
  border: 1px solid white;
  box-shadow: 12px 17px 51px rgba(0, 0, 0, 0.22);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  border-radius: 17px;
  text-align: center;
  cursor: pointer;
  transition: all 0.5s;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1.6rem 0.75rem;
  user-select: none;
}
.guide-item:hover {
  border: 1px solid black;
  transform: scale(1.05);
}
.guide-item:active {
  transform: scale(0.95) rotateZ(1.7deg);
}
.guide-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-bottom: 0.7rem;
}
.guide-icon svg {
  width: 48px;
  height: 48px;
}
.guide-item h4 {
  font-size: 0.88rem;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 0.2rem;
}
.guide-item p {
  font-size: 0.72rem;
  color: #666;
  margin: 0;
}

/* Notice List (sidebar) */
.notice-card-section {
  padding: 1rem 1.25rem;
  overflow: hidden;
}
.notice-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}
.notice-card-header h3 {
  margin: 0;
  font-size: 0.92rem;
  font-weight: 700;
}
.notice-view-all {
  font-size: 0.72rem;
  color: var(--color-primary);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}
.notice-view-all:hover {
  color: var(--color-primary-hover);
}
.notice-card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 280px;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
}
.notice-card-item {
  width: 100%;
  height: 80px;
  border-radius: 8px;
  box-sizing: border-box;
  padding: 10px 15px;
  background-color: #ffffff;
  box-shadow: rgba(149, 157, 165, 0.2) 0px 8px 24px;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s;
}
.notice-card-item:hover {
  transform: scale(1.02);
  box-shadow: rgba(149, 157, 165, 0.3) 0px 12px 32px;
}
.notice-card-wave {
  position: absolute;
  transform: rotate(90deg);
  left: -31px;
  top: 32px;
  width: 80px;
  pointer-events: none;
}
.notice-card-icon-wrap {
  width: 35px;
  height: 35px;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;
  flex-shrink: 0;
}
.notice-card-icon {
  width: 17px;
  height: 17px;
}
.notice-card-text {
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex: 1;
  min-width: 0;
}
.notice-card-title {
  font-size: 0.82rem;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-card-sub {
  font-size: 0.75rem;
  color: #888;
  margin-top: 2px;
}
/* Level colors - info (blue) */
.notice-card-item--info { background-color: #dbeafe; }
.notice-card-item--info .notice-card-icon-wrap { background-color: #bfdbfe; }
.notice-card-item--info .notice-card-title { color: #2563eb; }
/* Level colors - success (green) */
.notice-card-item--success { background-color: #dcfce7; }
.notice-card-item--success .notice-card-icon-wrap { background-color: #bbf7d0; }
.notice-card-item--success .notice-card-title { color: #16a34a; }
/* Level colors - warning (yellow) */
.notice-card-item--warning { background-color: #fef3c7; }
.notice-card-item--warning .notice-card-icon-wrap { background-color: #fde68a; }
.notice-card-item--warning .notice-card-title { color: #d97706; }
/* Level colors - error (red) */
.notice-card-item--error { background-color: #fee2e2; }
.notice-card-item--error .notice-card-icon-wrap { background-color: #fecaca; }
.notice-card-item--error .notice-card-title { color: #dc2626; }

/* Notice Modal */
.notice-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}
.modal-enter-active { transition: all 0.25s ease-out; }
.modal-leave-active { transition: all 0.2s ease-in; }
.modal-enter-from { opacity: 0; }
.modal-leave-to { opacity: 0; }
.modal-enter-from .notice-modal { transform: translateY(20px) scale(0.96); opacity: 0; }
.modal-leave-to .notice-modal { transform: translateY(10px) scale(0.98); opacity: 0; }
.notice-modal {
  background: #fff;
  border-radius: 16px;
  width: 90%;
  max-width: 460px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  position: relative;
  transition: all 0.25s ease-out;
}
/* Notice Modal - TicketingSystem style */
.notice-modal {
  background: #fff;
  border-radius: 16px;
  width: 90%;
  max-width: 620px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  position: relative;
  transition: all 0.25s ease-out;
}
.notice-modal-close {
  position: absolute;
  top: 18px;
  right: 16px;
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 8px;
  font-size: 1.2rem;
  color: #94a3b8;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  line-height: 1;
  z-index: 1;
}
.notice-modal-close:hover {
  background: rgba(0, 0, 0, 0.08);
  color: #334155;
}
.notice-modal-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 28px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
}
.notice-modal-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
  flex-shrink: 0;
}
.notice-modal-icon.info { background: #e0f2fe; }
.notice-modal-icon.feature { background: #ecfdf5; }
.notice-modal-icon.update { background: #fef3c7; }
.notice-modal-title-wrap {
  flex: 1;
  min-width: 0;
}
.notice-modal-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px;
  line-height: 1.4;
  padding-right: 2rem;
}
.notice-modal-subtitle {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 0.8rem;
  color: #94a3b8;
}
.notice-modal-tag {
  font-size: 0.75rem;
  padding: 0.12rem 0.5rem;
  border-radius: 4px;
  font-weight: 600;
  letter-spacing: 0.02em;
}
.notice-modal-tag.info { background: #e0f2fe; color: #0369a1; }
.notice-modal-tag.feature { background: #ecfdf5; color: #047857; }
.notice-modal-tag.update { background: #fef3c7; color: #b45309; }
.notice-modal-date {
  font-size: 0.75rem;
  color: #94a3b8;
}
.notice-modal-body {
  padding: 28px;
  max-height: 60vh;
  overflow-y: auto;
}
.notice-modal-body p {
  font-size: 0.9rem;
  color: #475569;
  line-height: 1.85;
  margin: 0;
}
.notice-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 28px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}
.notice-modal-btn {
  padding: 0.5rem 1.5rem;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.notice-modal-btn:hover {
  background: var(--color-primary-hover);
  box-shadow: 0 2px 8px rgba(30, 94, 182, 0.25);
}

/* Content Grid */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 210px;
  gap: 1rem;
  align-items: start;
}

/* AI Search - Purple gradient style */
.ai-search-section {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.04), rgba(139, 92, 246, 0.04));
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: var(--radius-lg);
  padding: 1.25rem 1.5rem;
  margin-bottom: 1.25rem;
}
.ai-search-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.9rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
  color: #3b82f6;
}
.ai-header-left {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.ai-mode-toggle {
  display: flex;
  gap: 2px;
  background: rgba(59, 130, 246, 0.08);
  border-radius: 8px;
  padding: 2px;
}
.ai-mode-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #3b82f6;
  font-size: 0.78rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.ai-mode-btn:hover {
  background: rgba(59, 130, 246, 0.12);
}
.ai-mode-btn.active {
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  color: #2563eb;
}
/* Nav Cards */
.ai-nav-cards {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.ai-nav-card {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: rgba(59, 130, 246, 0.06);
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 8px;
  text-decoration: none;
  color: #2563eb;
  font-size: 0.82rem;
  font-weight: 500;
  transition: all 0.2s;
}
.ai-nav-card:hover {
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.3);
  transform: translateY(-1px);
}
.ai-nav-icon { font-size: 1rem; }
.ai-nav-arrow {
  opacity: 0.5;
  transition: transform 0.2s;
}
.ai-nav-card:hover .ai-nav-arrow {
  transform: translateX(2px);
  opacity: 0.8;
}
/* AI Loader Icon */
.ai-loader {
  --color-one: #ffbf48;
  --color-two: #be4a1d;
  --color-three: #ffbf4780;
  --color-four: #bf4a1d80;
  --color-five: #ffbf4740;
  --time-animation: 2s;
  --size: 0.2;
  position: relative;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  transform: scale(var(--size));
  box-shadow:
    0 0 25px 0 var(--color-three),
    0 20px 50px 0 var(--color-four);
  animation: ai-colorize calc(var(--time-animation) * 3) ease-in-out infinite;
  flex-shrink: 0;
}
.ai-loader::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border-top: solid 1px var(--color-one);
  border-bottom: solid 1px var(--color-two);
  background: linear-gradient(180deg, var(--color-five), var(--color-four));
  box-shadow:
    inset 0 10px 10px 0 var(--color-three),
    inset 0 -10px 10px 0 var(--color-four);
}
.ai-loader-box {
  width: 100px;
  height: 100px;
  background: linear-gradient(180deg, var(--color-one) 30%, var(--color-two) 70%);
  mask: url(#ai-clipping);
  -webkit-mask: url(#ai-clipping);
}
.ai-loader svg {
  position: absolute;
}
.ai-loader svg #ai-clipping {
  filter: contrast(15);
  animation: ai-roundness calc(var(--time-animation) / 2) linear infinite;
}
.ai-loader svg #ai-clipping polygon {
  filter: blur(7px);
}
.ai-loader svg #ai-clipping polygon:nth-child(1) {
  transform-origin: 75% 25%;
  transform: rotate(90deg);
}
.ai-loader svg #ai-clipping polygon:nth-child(2) {
  transform-origin: 50% 50%;
  animation: ai-rotation var(--time-animation) linear infinite reverse;
}
.ai-loader svg #ai-clipping polygon:nth-child(3) {
  transform-origin: 50% 60%;
  animation: ai-rotation var(--time-animation) linear infinite;
  animation-delay: calc(var(--time-animation) / -3);
}
.ai-loader svg #ai-clipping polygon:nth-child(4) {
  transform-origin: 40% 40%;
  animation: ai-rotation var(--time-animation) linear infinite reverse;
}
.ai-loader svg #ai-clipping polygon:nth-child(5) {
  transform-origin: 40% 40%;
  animation: ai-rotation var(--time-animation) linear infinite reverse;
  animation-delay: calc(var(--time-animation) / -2);
}
.ai-loader svg #ai-clipping polygon:nth-child(6) {
  transform-origin: 60% 40%;
  animation: ai-rotation var(--time-animation) linear infinite;
}
.ai-loader svg #ai-clipping polygon:nth-child(7) {
  transform-origin: 60% 40%;
  animation: ai-rotation var(--time-animation) linear infinite;
  animation-delay: calc(var(--time-animation) / -1.5);
}
@keyframes ai-rotation {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
@keyframes ai-roundness {
  0% { filter: contrast(15); }
  20% { filter: contrast(3); }
  40% { filter: contrast(3); }
  60% { filter: contrast(15); }
  100% { filter: contrast(15); }
}
@keyframes ai-colorize {
  0% { filter: hue-rotate(0deg); }
  20% { filter: hue-rotate(-30deg); }
  40% { filter: hue-rotate(-60deg); }
  60% { filter: hue-rotate(-90deg); }
  80% { filter: hue-rotate(-45deg); }
  100% { filter: hue-rotate(0deg); }
}
/* Ask AI Input */
.ask-ai-wrapper {
  width: 100%;
  position: relative;
}
.ask-ai-wrapper .ai-input-container {
  position: relative;
  display: flex;
  align-items: center;
  background: #fff;
  border: 2px solid #d0d5dd;
  border-radius: 12px;
  padding: 10px 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.5s cubic-bezier(0.65, 0, 0.35, 1);
  overflow: hidden;
}
.ask-ai-wrapper .ai-input {
  flex-grow: 1;
  background: transparent;
  border: none;
  outline: none;
  font-family: inherit;
  font-size: 0.9rem;
  color: #333;
  padding: 6px 10px;
  width: 100%;
  position: relative;
  z-index: 2;
  transition: color 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.ask-ai-wrapper .ai-input::placeholder {
  color: #999;
  font-style: normal;
}
.ask-ai-wrapper .ai-input:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}
.ask-ai-wrapper .icon-container {
  position: relative;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  transition: all 0.5s cubic-bezier(0.7, -0.5, 0.3, 1.5);
  cursor: pointer;
  z-index: 2;
}
.ask-ai-wrapper .ai-icon-svg path {
  fill: #4a4a4a;
  transform-origin: center;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}
.ask-ai-wrapper .underline-effect {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background: var(--color-primary);
  transition: all 0.6s cubic-bezier(0.25, 0.8, 0.25, 1);
  z-index: 1;
}
.ask-ai-wrapper .ripple-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: rgba(9, 105, 218, 0.08);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: all 0.7s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 0;
}
.ask-ai-wrapper .floating-dots {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}
.ask-ai-wrapper .floating-dots span {
  position: absolute;
  width: 3px;
  height: 3px;
  background: var(--color-primary);
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.3s ease, transform 0.6s cubic-bezier(0.7, -0.5, 0.3, 1.5);
}
.ask-ai-wrapper .bg-fade {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(9, 105, 218, 0.03), rgba(9, 105, 218, 0.06));
  opacity: 0;
  transition: all 0.5s cubic-bezier(0.25, 0.8, 0.25, 1);
  z-index: 0;
}
/* Hover & Focus states */
.ask-ai-wrapper .ai-input-container:hover,
.ask-ai-wrapper .ai-input-container:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 4px 16px rgba(9, 105, 218, 0.15);
}
.ask-ai-wrapper .ai-input-container:hover .icon-container,
.ask-ai-wrapper .ai-input-container:focus-within .icon-container {
  transform: translateY(-2px) scale(1.1);
}
.ask-ai-wrapper .ai-input-container:hover .ai-icon-svg path,
.ask-ai-wrapper .ai-input-container:focus-within .ai-icon-svg path {
  fill: var(--color-primary);
  transform: scale(1.1) rotate(5deg);
  animation: aiIconBounce 0.5s infinite alternate cubic-bezier(0.7, -0.5, 0.3, 1.5);
}
.ask-ai-wrapper .ai-input-container:hover .underline-effect,
.ask-ai-wrapper .ai-input-container:focus-within .underline-effect {
  width: 100%;
}
.ask-ai-wrapper .ai-input-container:hover .ripple-circle,
.ask-ai-wrapper .ai-input-container:focus-within .ripple-circle {
  width: 200px;
  height: 200px;
}
.ask-ai-wrapper .ai-input-container:hover .floating-dots span,
.ask-ai-wrapper .ai-input-container:focus-within .floating-dots span {
  opacity: 1;
  animation: aiFloatUp 1.2s infinite cubic-bezier(0.65, 0, 0.35, 1);
}
.ask-ai-wrapper .ai-input-container:hover .bg-fade,
.ask-ai-wrapper .ai-input-container:focus-within .bg-fade {
  opacity: 1;
}
.ask-ai-wrapper .ai-input:focus {
  color: var(--color-primary);
}
@keyframes aiIconBounce {
  0% { transform: scale(1.1) translateY(1px); }
  100% { transform: scale(1.1) translateY(-1px); }
}
@keyframes aiFloatUp {
  0% { transform: translate(var(--x), var(--y)) scale(1); opacity: 0; }
  50% { transform: translate(var(--x), calc(var(--y) - 15px)) scale(1.3); opacity: 1; }
  100% { transform: translate(var(--x), calc(var(--y) - 30px)) scale(1); opacity: 0; }
}
.ask-ai-wrapper .floating-dots span:nth-child(1) { --x: 10px; --y: 10px; animation-delay: 0s; }
.ask-ai-wrapper .floating-dots span:nth-child(2) { --x: -5px; --y: 5px; animation-delay: 0.2s; }
.ask-ai-wrapper .floating-dots span:nth-child(3) { --x: 15px; --y: 0px; animation-delay: 0.4s; }
.ask-ai-wrapper .floating-dots span:nth-child(4) { --x: -10px; --y: 15px; animation-delay: 0.6s; }

/* AI Loading Bars */
.ai-loading-bars {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: 24px;
  height: 24px;
}
.ai-loading-bars span {
  width: 3px;
  height: 16px;
  background: var(--color-primary);
  border-radius: 2px;
  animation: aiBarScale 0.9s ease-in-out infinite;
}
.ai-loading-bars span:nth-child(2) { animation-delay: -0.8s; background: #49a84c; }
.ai-loading-bars span:nth-child(3) { animation-delay: -0.7s; background: #f6bb02; }
.ai-loading-bars span:nth-child(4) { animation-delay: -0.6s; background: #f6bb02; }
.ai-loading-bars span:nth-child(5) { animation-delay: -0.5s; background: #2196f3; }
@keyframes aiBarScale {
  0%, 40%, 100% { transform: scaleY(0.15); }
  20% { transform: scaleY(1); }
}
.ai-response {
  margin-top: 1rem;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(59, 130, 246, 0.15);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.03), rgba(59, 130, 246, 0.03));
  backdrop-filter: blur(8px);
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.06), 0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  animation: aiResponseIn 0.4s cubic-bezier(0.22, 1, 0.36, 1);
}
@keyframes aiResponseIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.ai-response-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1rem;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.08), rgba(59, 130, 246, 0.08));
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
  font-size: 0.82rem;
  font-weight: 600;
  color: #3b82f6;
  letter-spacing: 0.02em;
}
.ai-response-icon {
  width: 15px;
  height: 15px;
  color: #3b82f6;
  flex-shrink: 0;
}
.ai-response-body {
  padding: 0.75rem 1rem;
}
.ai-response-body :deep(.md-editor) {
  background: transparent;
  border: none;
}
.ai-response-body :deep(.md-editor-preview) {
  font-size: 0.875rem;
  line-height: 1.75;
}
.ai-response-body :deep(.md-editor-preview h1),
.ai-response-body :deep(.md-editor-preview h2),
.ai-response-body :deep(.md-editor-preview h3) {
  margin-top: 0.85rem;
  margin-bottom: 0.5rem;
  padding-bottom: 0.3rem;
  border-bottom: 1px solid rgba(59, 130, 246, 0.08);
}
.ai-response-body :deep(.md-editor-preview pre) {
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  border: 1px solid rgba(59, 130, 246, 0.1);
  background: rgba(241, 245, 249, 0.6);
}
.ai-response-body :deep(.md-editor-preview code) {
  font-size: 0.82rem;
}
.ai-response-body :deep(.md-editor-preview p) {
  margin-bottom: 0.6rem;
}
.ai-response-body :deep(.md-editor-preview ul),
.ai-response-body :deep(.md-editor-preview ol) {
  padding-left: 1.25rem;
  margin-bottom: 0.6rem;
}
.ai-response-body :deep(.md-editor-preview li) {
  margin-bottom: 0.2rem;
}
.ai-response-body :deep(.md-editor-preview blockquote) {
  border-left: 3px solid #3b82f6;
  padding: 0.35rem 0.85rem;
  margin: 0.6rem 0;
  background: rgba(59, 130, 246, 0.04);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  color: var(--color-text-secondary);
}
.ai-response-body :deep(.md-editor-preview table) {
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.ai-response-body :deep(.md-editor-preview th) {
  background: rgba(59, 130, 246, 0.06);
  font-weight: 600;
}
/* Thinking section */
.ai-thinking {
  margin-top: 1rem;
  border: 1px solid rgba(139, 92, 246, 0.18);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.02), rgba(59, 130, 246, 0.02));
  box-shadow: 0 1px 8px rgba(139, 92, 246, 0.05);
  animation: aiResponseIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}
.ai-thinking-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.06), rgba(59, 130, 246, 0.06));
  font-size: 0.82rem;
  font-weight: 600;
  color: #2563eb;
  cursor: pointer;
  user-select: none;
  transition: background var(--transition-fast);
}
.ai-thinking-header:hover {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.1), rgba(59, 130, 246, 0.1));
}
.thinking-icon {
  font-size: 1rem;
}
.thinking-label {
  letter-spacing: 0.02em;
}
.thinking-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3b82f6;
  animation: pulse-dot 1.5s ease-in-out infinite;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 0.4; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}
.thinking-toggle {
  margin-left: auto;
  font-size: 0.72rem;
  font-weight: 500;
  color: var(--color-text-tertiary);
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  background: rgba(139, 92, 246, 0.06);
  transition: all var(--transition-fast);
}
.thinking-toggle:hover {
  background: rgba(139, 92, 246, 0.12);
  color: #2563eb;
}
.thinking-slide-enter-active,
.thinking-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  overflow: hidden;
}
.thinking-slide-enter-from,
.thinking-slide-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}
.thinking-slide-enter-to,
.thinking-slide-leave-from {
  opacity: 1;
  max-height: 500px;
}
.ai-thinking-content {
  padding: 0.6rem 1rem;
}
.ai-thinking-content :deep(.md-editor) {
  background: transparent;
  border: none;
}
.ai-thinking-content :deep(.md-editor-preview) {
  font-size: 0.82rem;
  line-height: 1.7;
  color: var(--color-text-secondary);
}
.ai-thinking-content :deep(.md-editor-preview p) {
  margin-bottom: 0.4rem;
}
.ai-thinking-content :deep(.md-editor-preview pre) {
  font-size: 0.78rem;
  background: rgba(241, 245, 249, 0.5);
  border: 1px solid rgba(139, 92, 246, 0.08);
}

/* Error section */
.ai-error {
  margin-top: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.82rem;
  color: #dc2626;
  padding: 0.6rem 1rem;
  background: linear-gradient(135deg, rgba(207, 34, 46, 0.04), rgba(239, 68, 68, 0.04));
  border-radius: var(--radius-lg);
  border: 1px solid rgba(207, 34, 46, 0.15);
  box-shadow: 0 1px 6px rgba(207, 34, 46, 0.05);
  animation: aiResponseIn 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}
.ai-error-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: #dc2626;
}

/* Section Header */
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}
.section-header h2 {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
}

/* Article Tabs — Glass Radio Group */
.glass-radio-group {
  --bg: rgba(0, 0, 0, 0.04);
  --text: #555;
  display: flex;
  position: relative;
  background: var(--bg);
  border-radius: 1rem;
  backdrop-filter: blur(12px);
  box-shadow:
    inset 1px 1px 3px rgba(255, 255, 255, 0.6),
    inset -1px -1px 3px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  width: fit-content;
}
.glass-radio-group input {
  display: none;
}
.glass-radio-group label {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  font-size: 13px;
  padding: 0.55rem 1.2rem;
  cursor: pointer;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: var(--text);
  position: relative;
  z-index: 2;
  transition: color 0.3s ease-in-out;
  user-select: none;
}
.glass-radio-group label:hover {
  color: #222;
}
.glass-radio-group input:checked + label {
  color: #1a1a1a;
}
.glass-glider {
  position: absolute;
  top: 0;
  bottom: 0;
  width: calc(100% / 4);
  border-radius: 1rem;
  z-index: 1;
  transition:
    transform 0.5s cubic-bezier(0.37, 1.95, 0.66, 0.56),
    background 0.4s ease-in-out,
    box-shadow 0.4s ease-in-out;
}
/* Latest — Green */
#tab-latest:checked ~ .glass-glider {
  transform: translateX(0%);
  background: linear-gradient(135deg, #34d39955, #6ee7b7);
  box-shadow:
    0 0 18px rgba(52, 211, 153, 0.5),
    0 0 10px rgba(110, 231, 183, 0.4) inset;
}
#tab-latest:checked + label {
  color: #065f46;
}
/* Popular — Gold */
#tab-popular:checked ~ .glass-glider {
  transform: translateX(100%);
  background: linear-gradient(135deg, #ffd70055, #ffcc00);
  box-shadow:
    0 0 18px rgba(255, 215, 0, 0.5),
    0 0 10px rgba(255, 235, 150, 0.4) inset;
}
#tab-popular:checked + label {
  color: #92400e;
}
/* Featured — Platinum */
#tab-featured:checked ~ .glass-glider {
  transform: translateX(200%);
  background: linear-gradient(135deg, #d0e7ff55, #a0d8ff);
  box-shadow:
    0 0 18px rgba(160, 216, 255, 0.5),
    0 0 10px rgba(200, 240, 255, 0.4) inset;
}
#tab-featured:checked + label {
  color: #1e40af;
}
/* Search — Purple */
#tab-search:checked ~ .glass-glider {
  transform: translateX(300%);
  background: linear-gradient(135deg, #93c5fd55, #60a5fa);
  box-shadow:
    0 0 18px rgba(192, 132, 252, 0.5),
    0 0 10px rgba(167, 139, 250, 0.4) inset;
}
#tab-search:checked + label {
  color: #6b21a8;
}
.more-link {
  font-size: 0.85rem;
  color: #999;
  transition: color 0.2s;
}
.more-link:hover {
  color: var(--color-primary);
}

/* Search Empty State */
.search-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}
.search-empty-icon {
  width: 48px;
  height: 48px;
  color: #d1d5db;
  margin-bottom: 1rem;
}
.search-empty-text {
  font-size: 1rem;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 0.5rem;
}
.search-empty-hint {
  font-size: 0.85rem;
  color: #9ca3af;
}
.search-results-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
  font-size: 0.88rem;
  color: #6b7280;
}
.search-clear-btn {
  background: none;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 4px 12px;
  font-size: 0.8rem;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.2s;
}
.search-clear-btn:hover {
  color: #ef4444;
  border-color: #fca5a5;
  background: #fef2f2;
}

/* Article Cards — Service Card Style */
.article-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.article-card-item {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 1.15rem 1.25rem;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  gap: 5px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  text-decoration: none;
  color: inherit;
}
.article-card-item:hover {
  background: #202127;
  border-color: transparent;
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
  transform: translateY(-2px);
}
.article-card-icon {
  width: 28px;
  height: 28px;
  stroke: #374151;
  transition: stroke 0.3s;
  flex-shrink: 0;
}
.article-card-item:hover .article-card-icon { stroke: #9ca3af; }
.article-card-cat {
  font-size: 0.72rem;
  padding: 0.1rem 0.45rem;
  border-radius: 4px;
  font-weight: 600;
  background: #e0f2fe;
  color: #0369a1;
  width: fit-content;
  transition: all 0.3s;
}
.article-card-item:hover .article-card-cat {
  background: rgba(59,130,246,0.2);
  color: #93c5fd;
}
.article-card-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: rgba(0,0,0,0.8);
  margin: 0;
  transition: color 0.3s;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}
.article-card-item:hover .article-card-title { color: #fff; }
.article-card-summary {
  font-size: 0.78rem;
  color: #9ca3af;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}
.article-card-item:hover .article-card-summary { color: #6b7280; }
.article-card-meta {
  display: flex;
  gap: 0.75rem;
  font-size: 0.72rem;
  color: #bbb;
  transition: color 0.3s;
}
.article-card-item:hover .article-card-meta { color: #6b7280; }
.article-card-views {
  margin-left: auto;
}
.article-card-number {
  font-size: 2rem;
  font-weight: 700;
  align-self: flex-end;
  margin: 0;
  -webkit-text-stroke: 1px #d1d5db;
  -webkit-text-fill-color: transparent;
  transition: all 0.3s;
  line-height: 1;
}
.article-card-item:hover .article-card-number {
  -webkit-text-stroke: 1px #4b5563;
}
.skeleton-card {
  cursor: default;
  pointer-events: none;
}
.skeleton-card:hover {
  transform: none;
  background: #fff;
  border-color: #f0f0f0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.night .skeleton-card:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: none;
}
.latest-tag {
  display: none;
}
.empty {
  color: var(--color-text-tertiary);
  text-align: center;
  padding: 2rem;
}

/* Home Pagination */
.home-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 1rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-light);
  gap: 0.75rem;
  flex-wrap: wrap;
}
.page-size-selector {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.page-size-label {
  font-size: 0.78rem;
  color: var(--color-text-tertiary);
}
.page-nav {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}
.page-nav-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-bg);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
}
.page-nav-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.page-nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.page-num-btn {
  min-width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 0.78rem;
  font-variant-numeric: tabular-nums;
  transition: all 0.15s;
}
.page-num-btn:hover {
  background: var(--color-bg-muted);
}
.page-num-btn.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
  font-weight: 600;
}
.page-total {
  font-size: 0.75rem;
  color: var(--color-text-tertiary);
}

/* Sidebar */
.sidebar-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  position: sticky;
  top: 72px;
}
.sidebar-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 1.25rem;
  transition: all 0.25s;
}
.sidebar-card:hover {
  border-color: rgba(30, 94, 182, 0.15);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.sidebar-card h3 {
  font-size: 0.9rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.card-icon {
  font-size: 0.95rem;
}

/* About Card */
.about-card {
  overflow: hidden;
}
.about-title-clickable {
  cursor: pointer;
  transition: color 0.2s;
}
.about-title-clickable:hover {
  color: #3b82f6;
}
.candle-wrapper {
  position: relative;
  height: 90px;
  margin-bottom: 0.75rem;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.candle-wrapper .floor {
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 200px;
  height: 3px;
  background: #673c63;
  transform: translateX(-50%);
  box-shadow: 0px 1px 3px #111;
  z-index: 2;
}
.candle-wrapper .candles {
  position: absolute;
  left: 50%;
  bottom: 3px;
  width: 140px;
  height: 85px;
  transform: translateX(-50%);
  z-index: 1;
}
.candle-wrapper .light__wave {
  position: absolute;
  top: 20%;
  left: 35%;
  width: 45px;
  height: 45px;
  border-radius: 100%;
  z-index: 0;
  transform: translate(-25%, -50%) scale(2.5, 2.5);
  border: 2px solid rgba(255, 255, 255, 0.2);
  animation: expand-light 3s infinite linear;
}
.candle-wrapper .candle1 {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 22px;
  height: 55px;
  background: #fff;
  border: 2px solid #673c63;
  border-bottom: 0;
  border-radius: 3px;
  transform-origin: center right;
  transform: translate(60%, -25%);
  box-shadow: -2px 0px 0px #95c6f2 inset;
  animation: expand-body 3s infinite linear;
}
.candle-wrapper .candle1__stick,
.candle-wrapper .candle2__stick {
  position: absolute;
  left: 50%;
  top: 0;
  width: 2px;
  height: 10px;
  background: #673c63;
  border-radius: 8px;
  transform: translate(-50%, -100%);
}
.candle-wrapper .candle2__stick {
  height: 8px;
  transform-origin: bottom center;
  animation: stick-animation 3s infinite linear;
}
.candle-wrapper .candle1__eyes,
.candle-wrapper .candle2__eyes {
  position: absolute;
  left: 50%;
  top: 0;
  width: 22px;
  height: 18px;
  transform: translate(-50%, 0);
}
.candle-wrapper .candle1__eyes-one {
  position: absolute;
  left: 30%;
  top: 20%;
  width: 3px;
  height: 3px;
  border-radius: 100%;
  background: #673c63;
  transform: translate(-70%, 0);
  animation: blink-eyes 3s infinite linear;
}
.candle-wrapper .candle1__eyes-two {
  position: absolute;
  left: 70%;
  top: 20%;
  width: 3px;
  height: 3px;
  border-radius: 100%;
  background: #673c63;
  transform: translate(-70%, 0);
  animation: blink-eyes 3s infinite linear;
}
.candle-wrapper .candle1__mouth {
  position: absolute;
  left: 40%;
  top: 20%;
  width: 0;
  height: 0;
  border-radius: 20px;
  background: #673c63;
  transform: translate(-50%, -50%);
  animation: uff 3s infinite linear;
}
.candle-wrapper .candle__smoke-one {
  position: absolute;
  left: 30%;
  top: 50%;
  width: 20px;
  height: 2px;
  background: grey;
  transform: translate(-50%, -50%);
  animation: move-left 3s infinite linear;
}
.candle-wrapper .candle__smoke-two {
  position: absolute;
  left: 30%;
  top: 40%;
  width: 7px;
  height: 7px;
  border-radius: 7px;
  background: grey;
  transform: translate(-50%, -50%);
  animation: move-top 3s infinite linear;
}
.candle-wrapper .candle2 {
  position: absolute;
  left: 20%;
  top: 65%;
  width: 26px;
  height: 35px;
  background: #fff;
  border: 2px solid #673c63;
  border-bottom: 0;
  border-radius: 3px;
  transform: translate(60%, -15%);
  transform-origin: center right;
  box-shadow: -2px 0px 0px #95c6f2 inset;
  animation: shake-left 3s infinite linear;
}
.candle-wrapper .candle2__eyes-one {
  position: absolute;
  left: 30%;
  top: 50%;
  width: 3px;
  height: 3px;
  display: inline-block;
  border: 0 solid #673c63;
  border-radius: 100%;
  background: #673c63;
  transform: translate(-80%, 0);
  animation: changeto-lower 3s infinite linear;
}
.candle-wrapper .candle2__eyes-two {
  position: absolute;
  left: 70%;
  top: 50%;
  width: 3px;
  height: 3px;
  display: inline-block;
  border: 0 solid #673c63;
  border-radius: 100%;
  background: #673c63;
  transform: translate(-80%, 0);
  animation: changeto-greater 3s infinite linear;
}
.candle-wrapper .candle2__fire {
  position: absolute;
  top: 50%;
  left: 40%;
  display: block;
  width: 10px;
  height: 14px;
  background-color: #ff9800;
  border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
  transform: translate(-50%, -50%);
  animation: dance-fire 3s infinite linear;
}
.candle-wrapper .sparkles-one,
.candle-wrapper .sparkles-two {
  display: none;
}

@keyframes blink-eyes {
  0%, 35% { opacity: 1; transform: translate(-70%, 0); }
  36%, 39% { opacity: 0; transform: translate(-70%, 0); }
  40% { opacity: 1; transform: translate(-70%, 0); }
  50%, 65% { transform: translate(-140%, 0); }
  66% { transform: translate(-70%, 0); }
}
@keyframes expand-body {
  0%, 40% { transform: scale(1, 1) translate(60%, -25%); }
  45%, 55% { transform: scale(1.1, 1.1) translate(60%, -28%); }
  60% { transform: scale(0.89, 0.89) translate(60%, -25%); }
  65% { transform: scale(1, 1) translate(60%, -25%); }
  70% { transform: scale(0.95, 0.95) translate(60%, -25%); }
  75% { transform: scale(1, 1) translate(60%, -25%); }
}
@keyframes uff {
  0%, 40% { width: 0; height: 0; }
  50%, 54% { width: 10px; height: 10px; left: 30%; }
  59% { width: 4px; height: 4px; left: 20%; }
  62% { width: 2px; height: 2px; left: 20%; }
  67% { width: 0; height: 0; left: 30%; }
}
@keyframes move-left {
  0%, 59%, 100% { width: 0; left: 40%; }
  60% { width: 20px; left: 30%; }
  68% { width: 0; left: 20%; }
}
@keyframes move-top {
  0%, 64%, 100% { width: 0; height: 0; top: 0; }
  65% { width: 7px; height: 7px; top: 40%; left: 40%; }
  80% { width: 0; height: 0; top: 20%; }
}
@keyframes shake-left {
  0%, 40% { left: 20%; transform: translate(60%, -15%); }
  50%, 54% { left: 20%; transform: translate(60%, -15%); }
  59% { left: 20%; transform: translate(60%, -15%); }
  62% { left: 18%; transform: translate(60%, -15%); }
  65% { left: 21%; transform: translate(60%, -15%); }
  67% { left: 20%; transform: translate(60%, -15%); }
  75% { left: 20%; transform: scale(1.15, 0.85) translate(60%, -15%); background: #fff; border-color: #673c63; }
  91% { left: 20%; transform: scale(1.18, 0.82) translate(60%, -10%); background: #f44336; border-color: #f44336; box-shadow: -2px 0px 0px #f44336 inset; }
  92% { left: 20%; transform: scale(0.85, 1.15) translate(60%, -15%); }
  95% { left: 20%; transform: scale(1.05, 0.95) translate(60%, -15%); }
  97% { left: 20%; transform: scale(1, 1) translate(60%, -15%); }
}
@keyframes stick-animation {
  0%, 40% { left: 50%; top: 0; transform: translate(-50%, -100%); }
  50%, 54% { left: 50%; top: 0; transform: translate(-50%, -100%); }
  59% { left: 50%; top: 0; transform: translate(-50%, -100%); }
  62% { left: 50%; top: 0; transform: rotateZ(-15deg) translate(-50%, -100%); }
  65% { left: 50%; top: 0; transform: rotateZ(15deg) translate(-50%, -100%); }
  70% { left: 50%; top: 0; transform: rotateZ(-5deg) translate(-50%, -100%); }
  72% { left: 50%; top: 0; transform: rotateZ(5deg) translate(-50%, -100%); }
  74%, 84% { left: 50%; top: 0; transform: rotateZ(0deg) translate(-50%, -100%); }
  85% { transform: rotateZ(180deg) translate(0%, 120%); }
  92% { left: 50%; top: 0; transform: translate(-50%, -100%); }
}
@keyframes expand-light {
  10%, 29%, 59%, 89% { transform: translate(-25%, -50%) scale(0, 0); border: 2px solid rgba(255, 255, 255, 0); }
  90%, 20%, 50% { transform: translate(-25%, -50%) scale(1, 1); }
  95%, 96%, 26%, 27%, 56%, 57% { transform: translate(-25%, -50%) scale(2, 2); border: 2px solid rgba(255, 255, 255, 0.5); }
  0%, 28%, 58%, 100% { transform: translate(-25%, -50%) scale(2.5, 2.5); border: 2px solid rgba(255, 255, 255, 0.2); }
}
@keyframes dance-fire {
  59%, 89% { left: 40%; width: 0; height: 0; }
  90%, 0%, 7%, 15%, 23%, 31%, 39%, 47%, 55% { left: 40.8%; width: 10px; height: 14px; background: #ffc107; }
  94%, 3%, 11%, 19%, 27%, 35%, 43%, 51%, 58% { left: 41.2%; width: 10px; height: 14px; background: #ff9800; }
}
@keyframes changeto-lower {
  0%, 70%, 90% { padding: 0; display: inline-block; border-radius: 100%; background: #673c63; border-width: 0; border: 0 solid #673c63; transform: translate(-90%, 0); }
  71%, 89% { background: none; border: solid #673c63; border-radius: 0; border-width: 0 2px 2px 0; display: inline-block; padding: 1px; float: left; transform-origin: bottom left; transform: rotate(-45deg) translate(-50%, -65%); }
}
@keyframes changeto-greater {
  0%, 70%, 90% { top: 50%; padding: 0; display: inline-block; border-radius: 100%; background: #673c63; border-width: 0; border: 0 solid #673c63; transform: translate(-80%, 0); }
  71%, 89% { top: 30%; background: none; border: solid #673c63; border-radius: 0; border-width: 0 2px 2px 0; display: inline-block; padding: 1px; float: left; transform-origin: bottom left; transform: rotate(135deg) translate(-80%, 20%); }
}

.about-text {
  font-size: 0.85rem;
  color: #666;
  line-height: 1.6;
  margin-bottom: 1rem;
}
.about-stats {
  display: flex;
  gap: 0;
  text-align: center;
}
.stat-item {
  flex: 1;
  padding: 0.5rem 0;
  cursor: pointer;
  transition: background 0.2s;
  border-radius: 6px;
}
.stat-item:hover {
  background: rgba(59, 130, 246, 0.08);
}
.stat-item:not(:last-child) {
  border-right: 1px solid var(--color-border-light);
}
.stat-num {
  display: block;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-primary);
  font-variant-numeric: tabular-nums;
}
.stat-label {
  font-size: 0.75rem;
  color: #999;
}

/* Category List */
.category-list {
  display: flex;
  flex-direction: column;
}
.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.45rem 0.5rem;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.2s;
  font-size: 0.875rem;
}
.category-item:hover {
  background: #f6f8fa;
  transform: translateX(2px);
}
.cat-name {
  color: #333;
  transition: color 0.2s;
}
.category-item:hover .cat-name {
  color: var(--color-primary);
}
.cat-count {
  font-size: 0.72rem;
  color: #999;
  background: #f6f8fa;
  padding: 0.1rem 0.5rem;
  border-radius: 6px;
  font-variant-numeric: tabular-nums;
}

/* Tag Cloud */
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}
.tag-item {
  font-size: 0.78rem;
  padding: 0.2rem 0.6rem;
  background: #f6f8fa;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  color: #666;
  text-decoration: none;
  transition: all 0.2s;
}
.tag-item:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: #f0f7ff;
}

/* Weather Card */
.weather-card {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.25s;
}
.weather-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}
.weather-info {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 18px 14px;
  color: #fff;
  min-height: 120px;
}
.weather-bg-design {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
.weather-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}
.weather-circle:nth-child(1) {
  width: 120px; height: 120px;
  top: -60px; right: -30px;
}
.weather-circle:nth-child(2) {
  width: 80px; height: 80px;
  top: -40px; right: 10px;
  opacity: 0.7;
}
.weather-circle:nth-child(3) {
  width: 40px; height: 40px;
  top: 5px; right: 30px;
}
.weather-left {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  z-index: 1;
  gap: 4px;
}
.weather-condition {
  display: flex;
  align-items: center;
  gap: 6px;
}
.weather-icon-lg {
  font-size: 1.5rem;
  line-height: 1;
}
.weather-desc {
  font-size: 0.82rem;
  opacity: 0.9;
}
.weather-temp {
  font-size: 2.2rem;
  font-weight: 600;
  line-height: 1;
}
.weather-range {
  font-size: 0.78rem;
  opacity: 0.75;
}
.weather-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  height: 100%;
  z-index: 1;
  gap: 4px;
}
.weather-time-wrap {
  text-align: right;
}
.weather-time {
  font-size: 1.15rem;
  font-weight: 600;
  line-height: 1.1;
}
.weather-date {
  font-size: 0.72rem;
  opacity: 0.75;
}
.weather-city {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 0.82rem;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(4px);
  transition: background 0.2s;
}
.weather-city:hover {
  background: rgba(255, 255, 255, 0.25);
}
.weather-forecast {
  display: flex;
  background: rgba(0, 0, 0, 0.06);
}
.forecast-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 8px 4px;
  font-size: 0.72rem;
  color: #666;
  transition: background 0.2s;
}
.forecast-item:hover {
  background: rgba(0, 0, 0, 0.04);
}
.forecast-day {
  font-weight: 500;
  color: #555;
}
.forecast-icon {
  font-size: 1rem;
  line-height: 1;
}
.forecast-temp {
  font-variant-numeric: tabular-nums;
  color: #888;
}
.weather-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 0.75rem;
  color: #666;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
.weather-tip-icon {
  font-size: 0.85rem;
}
.weather-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  background: linear-gradient(135deg, #1e6cb6, #3b9edd);
}
.weather-loading-text {
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.85rem;
}
/* Weather Failed */
.weather-failed {
  padding: 0;
  background: #fff;
}

/* City Picker Modal */
.city-picker-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}
.city-picker-modal {
  background: #fff;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}
.city-picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.city-picker-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #333;
}
.city-picker-close {
  width: 30px;
  height: 30px;
  border: none;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 8px;
  font-size: 1.1rem;
  color: #999;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.city-picker-close:hover {
  background: rgba(0, 0, 0, 0.08);
  color: #333;
}
.city-picker-search {
  padding: 12px 20px;
}
.city-picker-search input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 0.88rem;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.city-picker-search input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(9, 105, 218, 0.1);
}
.city-picker-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 0 20px 16px;
  max-height: 260px;
  overflow-y: auto;
}
.city-tag {
  padding: 7px 4px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background: #fafafa;
  font-size: 0.82rem;
  color: #555;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
}
.city-tag-name {
  font-weight: 500;
}
.city-tag-province {
  font-size: 0.72rem;
  color: #999;
  font-weight: 400;
}
.city-tag:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}
.city-tag.active {
  border-color: var(--color-primary);
  color: #fff;
  background: var(--color-primary);
}
.city-picker-footer {
  padding: 10px 20px;
  text-align: center;
  font-size: 0.72rem;
  color: #bbb;
  border-top: 1px solid #f0f0f0;
}
.city-picker-footer a {
  color: var(--color-primary);
  text-decoration: none;
}
.city-picker-footer a:hover {
  text-decoration: underline;
}

/* Night mode */
.night .guide-item {
  background: rgba(30, 41, 59, 0.6);
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: 12px 17px 51px rgba(0, 0, 0, 0.5);
}
.night .guide-item:hover {
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 12px 17px 51px rgba(0, 0, 0, 0.6);
}
.night .guide-item h4 {
  color: #e0e0e0;
}
.night .guide-item p {
  color: #a0aec0;
}
.night .section-header h2 {
  color: #e0e0e0;
}
.night .section-header p {
  color: #a0aec0;
}
.night .article-card-item {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
}
.night .article-card-item:hover {
  border-color: rgba(90, 155, 255, 0.3);
  box-shadow: 0 8px 24px rgba(90, 155, 255, 0.12);
}
.night .article-card-title {
  color: #e0e0e0;
}
.night .article-card-summary {
  color: #a0aec0;
}
.night .article-card-meta {
  color: #718096;
}
.night .article-card-cat {
  background: rgba(90, 155, 255, 0.15);
  color: #5a9bff;
}
.night .article-card-number {
  color: rgba(255, 255, 255, 0.06);
}
.night .article-card-icon {
  stroke: #a0aec0;
}
/* Pagination night */
.night .home-pagination {
  border-top-color: rgba(255, 255, 255, 0.08);
}
.night .page-nav-btn {
  border-color: rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.05);
  color: #a0aec0;
}
.night .page-nav-btn:hover:not(:disabled) {
  border-color: #5a9bff;
  color: #5a9bff;
}
.night .page-num-btn {
  color: #a0aec0;
}
.night .page-num-btn:hover {
  background: rgba(255, 255, 255, 0.08);
}
.night .page-num-btn.active {
  background: #5a9bff;
  border-color: #5a9bff;
  color: #fff;
}
.night .page-total {
  color: #718096;
}
/* Sidebar cards */
.night .sidebar-card {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
}
.night .sidebar-card h3 {
  color: #e0e0e0;
  border-bottom-color: rgba(255, 255, 255, 0.1);
}
/* Notice */
.night .notice-card-section {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
}
.night .notice-card-header h3 { color: #e0e0e0; }
.night .notice-card-item {
  background-color: rgba(30, 41, 59, 0.8);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}
.night .notice-card-item--info { background-color: rgba(30, 58, 94, 0.6); }
.night .notice-card-item--info .notice-card-icon-wrap { background-color: rgba(59, 130, 246, 0.3); }
.night .notice-card-item--success { background-color: rgba(22, 63, 38, 0.6); }
.night .notice-card-item--success .notice-card-icon-wrap { background-color: rgba(34, 197, 94, 0.3); }
.night .notice-card-item--warning { background-color: rgba(71, 54, 20, 0.6); }
.night .notice-card-item--warning .notice-card-icon-wrap { background-color: rgba(245, 158, 11, 0.3); }
.night .notice-card-item--error { background-color: rgba(69, 26, 26, 0.6); }
.night .notice-card-item--error .notice-card-icon-wrap { background-color: rgba(239, 68, 68, 0.3); }
.night .notice-card-title { color: #e0e0e0; }
.night .notice-card-sub { color: #718096; }
/* About */
.night .about-text {
  color: #a0aec0;
}
.night .stat-label {
  color: #a0aec0;
}
.night .stat-item:not(:last-child) {
  border-right-color: rgba(255, 255, 255, 0.1);
}
.night .stat-item:hover {
  background: rgba(59, 130, 246, 0.15);
}
.night .about-title-clickable:hover {
  color: #60a5fa;
}
/* Weather */
.night .weather-card {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
}
.night .weather-temp {
  color: #e0e0e0;
}
.night .weather-desc {
  color: #a0aec0;
}
.night .forecast-item {
  color: #a0aec0;
}
.night .forecast-item:hover {
  background: rgba(255, 255, 255, 0.06);
}
.night .forecast-day {
  color: #e0e0e0;
}
.night .forecast-temp {
  color: #a0aec0;
}
.night .weather-tip {
  color: #a0aec0;
  background: rgba(255, 255, 255, 0.04);
  border-top-color: rgba(255, 255, 255, 0.08);
}
/* Glass radio tabs */
.night .glass-radio-group {
  --bg: rgba(255, 255, 255, 0.06);
  --text: #a0aec0;
  box-shadow:
    inset 1px 1px 3px rgba(255, 255, 255, 0.05),
    inset -1px -1px 3px rgba(0, 0, 0, 0.2),
    0 2px 8px rgba(0, 0, 0, 0.2);
}
.night .glass-radio-group label:hover {
  color: #e0e0e0;
}
.night .glass-radio-group input:checked + label {
  color: #e0e0e0;
}
.night #tab-latest:checked + label {
  color: #6ee7b7;
}
.night #tab-popular:checked + label {
  color: #fbbf24;
}
.night #tab-featured:checked + label {
  color: #93c5fd;
}
.night #tab-search:checked + label {
  color: #bfdbfe;
}
.night .search-empty-icon {
  color: #4a5568;
}
.night .search-empty-text {
  color: #a0aec0;
}
.night .search-empty-hint {
  color: #718096;
}
.night .search-results-header {
  color: #a0aec0;
}
.night .search-clear-btn {
  border-color: rgba(255, 255, 255, 0.15);
  color: #718096;
}
.night .search-clear-btn:hover {
  color: #fc8181;
  border-color: rgba(252, 129, 129, 0.3);
  background: rgba(252, 129, 129, 0.1);
}
.night .more-link {
  color: #718096;
}
.night .more-link:hover {
  color: #5a9bff;
}
/* Notice Banner variants */
.night .notice-banner--info {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15), rgba(37, 99, 235, 0.1));
  border-color: rgba(59, 130, 246, 0.3);
}
.night .notice-banner--info .notice-banner-title { color: #93c5fd; }
.night .notice-banner--info .notice-banner-text { color: #a0aec0; }
.night .notice-banner--success {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.15), rgba(5, 150, 105, 0.1));
  border-color: rgba(16, 185, 129, 0.3);
}
.night .notice-banner--success .notice-banner-title { color: #6ee7b7; }
.night .notice-banner--success .notice-banner-text { color: #a0aec0; }
.night .notice-banner--warning {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.15), rgba(217, 119, 6, 0.1));
  border-color: rgba(245, 158, 11, 0.3);
}
.night .notice-banner--warning .notice-banner-title { color: #fcd34d; }
.night .notice-banner--warning .notice-banner-text { color: #a0aec0; }
.night .notice-banner--error {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.15), rgba(220, 38, 38, 0.1));
  border-color: rgba(239, 68, 68, 0.3);
}
.night .notice-banner--error .notice-banner-title { color: #fca5a5; }
.night .notice-banner--error .notice-banner-text { color: #a0aec0; }
.night .notice-banner-arrow { color: #718096; }
/* Notice Alert variants */
.night .notice-alert--info { background: rgba(59, 130, 246, 0.12); border-color: #3b82f6; }
.night .notice-alert--success { background: rgba(16, 185, 129, 0.12); border-color: #10b981; }
.night .notice-alert--warning { background: rgba(245, 158, 11, 0.12); border-color: #f59e0b; }
.night .notice-alert--error { background: rgba(239, 68, 68, 0.12); border-color: #ef4444; }
.night .notice-alert-title { color: #e0e0e0; }
.night .notice-alert-text { color: #a0aec0; }
/* Minor elements */
.night .weather-forecast { background: rgba(255, 255, 255, 0.04); }
.night .weather-failed { background: rgba(255, 255, 255, 0.06); color: #a0aec0; }
.night .city-tag { background: rgba(255, 255, 255, 0.06); border-color: rgba(255, 255, 255, 0.12); color: #cbd5e1; }
.night .city-tag:hover { background: rgba(59, 130, 246, 0.15); border-color: #3b82f6; color: #93c5fd; }
.night .city-tag.active { background: #3b82f6; border-color: #3b82f6; color: #fff; }
.night .city-tag-province { color: #718096; }
.night .city-tag.active .city-tag-province { color: rgba(255, 255, 255, 0.8); }
/* City Picker Modal night mode (Teleported to body) */
body.body-night .city-picker-modal { background: #1e293b; box-shadow: 0 24px 48px rgba(0, 0, 0, 0.4); }
body.body-night .city-picker-header { border-bottom-color: rgba(255, 255, 255, 0.1); }
body.body-night .city-picker-header h3 { color: #e2e8f0; }
body.body-night .city-picker-close { background: rgba(255, 255, 255, 0.08); color: #94a3b8; }
body.body-night .city-picker-close:hover { background: rgba(255, 255, 255, 0.15); color: #e2e8f0; }
body.body-night .city-picker-search input { background: rgba(255, 255, 255, 0.06); border-color: rgba(255, 255, 255, 0.12); color: #e2e8f0; }
body.body-night .city-picker-search input:focus { border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15); }
body.body-night .city-picker-search input::placeholder { color: #64748b; }
body.body-night .city-picker-footer { color: #64748b; }
body.body-night .city-picker-footer a { color: #60a5fa; }
.night .notice-banner { color: #e0e0e0; }
.night .notice-icon { opacity: 0.9; }

/* Responsive */
@media (max-width: 768px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  .sidebar-col {
    position: static;
  }
  .weather-info {
    padding: 14px 14px 10px;
    min-height: 100px;
  }
  .weather-temp {
    font-size: 1.8rem;
  }
  .city-picker-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .city-picker-modal {
    width: 95%;
    max-height: 80vh;
  }
  .city-picker-close {
    width: 36px;
    height: 36px;
    font-size: 18px;
  }
  .city-tag {
    padding: 0.5rem;
    min-height: 44px;
  }
  .hero {
    padding: 2rem 1rem 1.5rem;
  }
  .hero h1 {
    font-size: 1.4rem;
  }
  .hero-desc {
    font-size: 0.85rem;
  }
  .hero-links {
    flex-wrap: wrap;
    gap: 0.5rem;
  }
  .btn-primary, .btn-ghost {
    padding: 0.5rem 1.2rem;
    font-size: 0.85rem;
  }
  .notice-banner {
    font-size: 0.825rem;
    padding: 0.6rem 0.85rem;
  }
  .notice-banner-text { white-space: normal; overflow: visible; text-overflow: unset; }
  .notice-alert-text { white-space: normal; overflow: visible; text-overflow: unset; }
  .notice-card-title { white-space: normal; overflow: visible; text-overflow: unset; }
  .guide-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 0.6rem;
  }
  .guide-item {
    padding: 0.85rem 0.4rem;
  }
  .guide-item h4 {
    font-size: 0.78rem;
  }
  .guide-item p {
    font-size: 0.75rem;
  }
  .glass-radio-group {
    width: 100%;
  }
  .glass-radio-group label {
    padding: 0.45rem 0.6rem;
    font-size: 12px;
    min-width: 0;
  }
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }
  .ai-search-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  .ai-mode-toggle {
    width: 100%;
  }
  .ai-mode-btn {
    flex: 1;
    justify-content: center;
  }
  .article-card-item {
    padding: 0.85rem;
  }
  .article-card-number {
    font-size: 1.4rem;
  }
}

</style>
