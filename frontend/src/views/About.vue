<template>
  <div class="about-page">
    <h1 class="page-title fade-in-up">{{ $t('about.title') }}</h1>

    <AboutProfileCard class="fade-in-up" @open="profileVisible = true" />
    <AboutProfileModal v-model="profileVisible" />

    <div class="about-card fade-in-up fade-in-up-delay-1">
      <p class="intro">{{ $t('about.intro') }}</p>
      <p>{{ $t('about.description') }}</p>
      <ul>
        <li>{{ $t('about.item1') }}</li>
        <li>{{ $t('about.item2') }}</li>
        <li>{{ $t('about.item3') }}</li>
      </ul>
    </div>

    <div class="about-card fade-in-up fade-in-up-delay-2">
      <h2>{{ $t('about.techStack') }}</h2>
      <div class="tech-grid">
        <div class="tech-group">
          <h3>{{ $t('about.backend') }}</h3>
          <div class="tech-tags">
            <span class="tech-tag">Java</span>
            <span class="tech-tag">Spring Boot</span>
            <span class="tech-tag">JPA / Hibernate</span>
            <span class="tech-tag">MySQL</span>
            <span class="tech-tag">Redis</span>
          </div>
        </div>
        <div class="tech-group">
          <h3>{{ $t('about.frontend') }}</h3>
          <div class="tech-tags">
            <span class="tech-tag">Vue 3</span>
            <span class="tech-tag">Vite</span>
            <span class="tech-tag">JavaScript</span>
            <span class="tech-tag">CSS3</span>
          </div>
        </div>
        <div class="tech-group">
          <h3>{{ $t('about.tools') }}</h3>
          <div class="tech-tags">
            <span class="tech-tag">Git</span>
            <span class="tech-tag">Docker</span>
            <span class="tech-tag">Maven</span>
            <span class="tech-tag">npm</span>
          </div>
        </div>
      </div>
    </div>

    <div class="about-card fade-in-up fade-in-up-delay-3">
      <h2>{{ $t('about.contact') }}</h2>
      <p class="contact-text">{{ $t('about.contactText') }}</p>
      <div class="contact-list">
        <div class="contact-item" v-if="contactGithub">
          <span class="contact-label">{{ $t('about.github') }}</span>
          <a :href="contactGithub" target="_blank" rel="noopener" class="contact-value contact-link">{{ contactGithub }}</a>
        </div>
        <div class="contact-item" v-if="contactEmail">
          <span class="contact-label">{{ $t('about.email') }}</span>
          <a :href="'mailto:' + contactEmail" class="contact-value contact-link">{{ contactEmail }}</a>
        </div>
        <div class="contact-item" v-if="!contactGithub && !contactEmail">
          <span class="contact-value">{{ $t('about.tbd') }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AboutProfileCard from '../components/AboutProfileCard.vue'
import AboutProfileModal from '../components/AboutProfileModal.vue'
import request from '../utils/request'

const profileVisible = ref(false)
const contactGithub = ref('')
const contactEmail = ref('')

onMounted(async () => {
  try {
    const data = await request.get('/api/site/info')
    const extra = data.extraSettings || {}
    contactGithub.value = extra.contact_github || ''
    contactEmail.value = extra.contact_email || ''
  } catch { /* ignore */ }
})
</script>

<style scoped>
.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
}
.about-card {
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: 1.75rem 2rem;
  margin-bottom: 1rem;
  transition: box-shadow var(--transition);
}
.about-card:hover {
  box-shadow: var(--shadow-sm);
}
.about-card h2 {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 1rem;
}
.intro {
  font-size: 1rem;
  font-weight: 500;
  color: #24292f;
  margin-bottom: 1rem;
}
.about-card p {
  color: #3d4551;
  font-size: 0.925rem;
  line-height: 1.7;
  margin-bottom: 0.75rem;
}
.about-card ul {
  margin: 0.5rem 0 0 1.5rem;
}
.about-card li {
  margin-bottom: 0.4rem;
  color: #3d4551;
  font-size: 0.925rem;
}
.tech-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1.25rem;
}
.tech-group h3 {
  font-size: 0.85rem;
  font-weight: 600;
  color: #3d4551;
  margin-bottom: 0.5rem;
}
.tech-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}
.tech-tag {
  font-size: 0.78rem;
  padding: 0.2rem 0.6rem;
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border-light);
  border-radius: 2rem;
  color: #3d4551;
}
.contact-text {
  margin-bottom: 1rem;
}
.contact-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.contact-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.9rem;
}
.contact-label {
  font-weight: 500;
  color: #24292f;
  min-width: 60px;
}
.contact-value {
  color: #57606a;
}
.contact-link {
  color: var(--color-primary, #3b82f6);
  text-decoration: none;
}
.contact-link:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .about-page {
    padding: 0 0 2rem;
  }
  .page-title {
    font-size: 1.3rem;
  }
  .about-card {
    padding: 1.25rem 1rem;
    border-radius: 10px;
  }
  .about-card h2 {
    font-size: 1rem;
  }
  .about-card p, .about-card li {
    font-size: 0.85rem;
  }
  .tech-grid {
    grid-template-columns: 1fr 1fr;
    gap: 0.5rem;
  }
  .tech-tag {
    font-size: 0.75rem;
    padding: 0.2rem 0.5rem;
  }
  .contact-item {
    font-size: 0.82rem;
    flex-wrap: wrap;
    gap: 0.15rem 0.5rem;
  }
  .contact-label {
    min-width: 50px;
    font-size: 0.82rem;
  }
}
@media (max-width: 480px) {
  .tech-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
/* Night mode */
body.body-night .about-page .page-title { color: #f0f6fc; }
body.body-night .about-card h2 { color: #f0f6fc; }
body.body-night .about-card .intro { color: #e6edf3; }
body.body-night .about-card p { color: #c9d1d9; }
body.body-night .about-card li { color: #c9d1d9; }
body.body-night .about-card .tech-group h3 { color: #c9d1d9; }
body.body-night .about-card .tech-tag { color: #c9d1d9; background: rgba(255,255,255,0.06); border-color: rgba(255,255,255,0.12); }
body.body-night .about-card .contact-label { color: #f0f6fc; }
body.body-night .about-card .contact-value { color: #8b949e; }
</style>
