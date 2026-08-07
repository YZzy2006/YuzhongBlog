<template>
  <div class="month-nav">
    <button class="month-arrow" :disabled="!canPrev" @click="prev">
      <span>&lsaquo;</span>
    </button>
    <div class="month-display">
      <span class="month-text">{{ formattedMonth }}</span>
    </div>
    <button class="month-arrow" :disabled="!canNext" @click="next">
      <span>&rsaquo;</span>
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  months: { type: Array, default: () => [] },
  modelValue: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])
const { t, locale } = useI18n()

const CN_MONTHS = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十', '十一', '十二']
const EN_MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

const currentIndex = computed(() => {
  const idx = props.months.indexOf(props.modelValue)
  return idx >= 0 ? idx : props.months.length - 1
})

const canPrev = computed(() => currentIndex.value > 0)
const canNext = computed(() => currentIndex.value < props.months.length - 1)

const formattedMonth = computed(() => {
  const m = props.months[currentIndex.value]
  if (!m) return ''
  const [year, month] = m.split('-')
  const mi = parseInt(month, 10)
  const isZh = locale.value.startsWith('zh')
  const monthName = isZh ? (CN_MONTHS[mi - 1] || month) : (EN_MONTHS[mi - 1] || month)
  return `${year} ${t('cosmos.monthNav')}${monthName}`
})

function prev() {
  if (canPrev.value) {
    emit('update:modelValue', props.months[currentIndex.value - 1])
  }
}
function next() {
  if (canNext.value) {
    emit('update:modelValue', props.months[currentIndex.value + 1])
  }
}
</script>

<style scoped>
.month-nav {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(35, 26, 22, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(139, 107, 74, 0.4);
  border-radius: 9999px;
  padding: 6px 16px;
}
.month-arrow {
  background: none;
  border: none;
  color: #8b6b4a;
  font-size: 20px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 50%;
  transition: all 0.2s;
  line-height: 1;
}
.month-arrow:hover:not(:disabled) {
  background: rgba(212, 175, 55, 0.15);
  color: #d4af37;
}
.month-arrow:disabled {
  opacity: 0.2;
  cursor: not-allowed;
}
.month-display {
  min-width: 120px;
  text-align: center;
}
.month-text {
  font-family: serif;
  font-size: 14px;
  font-weight: 600;
  color: #d4af37;
  letter-spacing: 1px;
}
</style>
