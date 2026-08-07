<template>
  <Transition name="card-pop">
    <div v-if="node" class="cosmos-card-overlay" @click="$emit('close')">
    <div class="cosmos-card" @click.stop>
      <button class="cosmos-card-close" @click="$emit('close')">&times;</button>
      <div v-if="node.coverUrl" class="cosmos-card-cover">
        <img :src="node.coverUrl" :alt="node.title" loading="lazy" />
      </div>
      <div class="cosmos-card-body">
        <h3 class="cosmos-card-title">{{ node.title }}</h3>
        <p v-if="node.subtitle" class="cosmos-card-subtitle">{{ node.subtitle }}</p>
        <div class="cosmos-card-meta">
          <span v-if="node.date" class="cosmos-card-date">{{ formatDate(node.date) }}</span>
          <span v-if="node.views" class="cosmos-card-views">{{ node.views }} {{ $t('cosmos.views') }}</span>
        </div>
        <div v-if="node.tags && node.tags.length" class="cosmos-card-tags">
          <span v-for="tag in node.tags.slice(0, 5)" :key="tag" class="cosmos-card-tag">{{ tag }}</span>
        </div>
        <button v-if="node.link" class="cosmos-card-btn" @click="goToDetail">
          {{ $t('cosmos.viewDetail') }}
        </button>
      </div>
      <!-- Bottom accent line -->
      <div class="cosmos-card-accent"></div>
    </div>
    </div>
  </Transition>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  node: { type: Object, default: null }
})

const emit = defineEmits(['close'])
const router = useRouter()
const { locale } = useI18n()

function formatDate(d) {
  if (!d) return ''
  const date = new Date(d)
  if (isNaN(date.getTime())) return ''
  return date.toLocaleDateString(locale.value.startsWith('zh') ? 'zh-CN' : 'en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

function goToDetail() {
  if (props.node.link) {
    router.push(props.node.link)
    emit('close')
  }
}
</script>

<style scoped>
.cosmos-card-overlay {
  position: fixed;
  inset: 0;
  z-index: 39;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  pointer-events: auto;
}
.cosmos-card {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 40;
  width: 320px;
  max-height: 80vh;
  overflow-y: auto;
  background: rgba(35, 26, 22, 0.92);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: 16px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6), 0 0 30px rgba(212, 175, 55, 0.1);
  pointer-events: auto;
  font-family: serif;
}
.cosmos-card-close {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border: 1px solid rgba(139, 107, 74, 0.4);
  background: rgba(26, 17, 11, 0.8);
  color: #8b6b4a;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  z-index: 1;
  transition: all 0.2s;
}
.cosmos-card-close:hover {
  background: rgba(139, 107, 74, 0.3);
  color: #d4af37;
}
.cosmos-card-cover {
  width: 100%;
  height: 160px;
  overflow: hidden;
  border-radius: 16px 16px 0 0;
}
.cosmos-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cosmos-card-body {
  padding: 16px;
}
.cosmos-card-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 700;
  color: #e8e4d9;
  line-height: 1.3;
}
.cosmos-card-subtitle {
  margin: 0 0 10px;
  font-size: 12px;
  color: #8b6b4a;
}
.cosmos-card-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #6b5b3e;
  margin-bottom: 10px;
}
.cosmos-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}
.cosmos-card-tag {
  padding: 2px 8px;
  border: 1px solid rgba(139, 107, 74, 0.4);
  background: rgba(139, 107, 74, 0.15);
  color: #d4af37;
  font-size: 10px;
  border-radius: 9999px;
  font-family: monospace;
}
.cosmos-card-btn {
  width: 100%;
  padding: 10px;
  border: 1px solid rgba(212, 175, 55, 0.5);
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.15), rgba(184, 134, 11, 0.1));
  color: #d4af37;
  font-family: serif;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
}
.cosmos-card-btn:hover {
  background: linear-gradient(135deg, rgba(212, 175, 55, 0.25), rgba(184, 134, 11, 0.2));
  box-shadow: 0 0 15px rgba(212, 175, 55, 0.2);
}
.cosmos-card-accent {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(212, 175, 55, 0.3), transparent);
}
.cosmos-card::-webkit-scrollbar {
  width: 4px;
}
.cosmos-card::-webkit-scrollbar-track {
  background: transparent;
}
.cosmos-card::-webkit-scrollbar-thumb {
  background: rgba(234, 179, 8, 0.5);
  border-radius: 0;
}
.cosmos-card::-webkit-scrollbar-thumb:hover {
  background: rgba(234, 179, 8, 0.8);
}

.card-pop-enter-active { transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.card-pop-leave-active { transition: all 0.2s ease; }
.card-pop-enter-from { opacity: 0; transform: translate(-50%, -50%) scale(0.9); }
.card-pop-leave-to { opacity: 0; transform: translate(-50%, -50%) scale(0.95); }

@media (max-width: 768px) {
  .cosmos-card {
    width: calc(100% - 32px);
  }
}
</style>
