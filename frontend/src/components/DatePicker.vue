<template>
  <div class="date-picker" ref="pickerRef">
    <div class="date-trigger" @click="toggle">
      <svg class="date-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
      </svg>
      <span class="date-text" :class="{ placeholder: !modelValue }">{{ displayText }}</span>
      <span v-if="modelValue" class="date-clear" @click.stop="clear">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </span>
    </div>

    <Teleport to="body">
      <Transition name="panel-pop">
        <div class="date-panel" v-if="isOpen" ref="panelRef" :style="panelStyle">
          <!-- Days view -->
          <template v-if="panelMode === 'days'">
            <div class="panel-header">
              <button class="nav-btn" @click="prevMonth">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
              </button>
              <span class="panel-title">
                <button class="title-btn" @click="switchingMode = true; panelMode = 'years'">{{ viewYear }}{{ $t('datePicker.yearSuffix') }}</button>
                <button class="title-btn" @click="switchingMode = true; panelMode = 'months'">{{ $t('datePicker.monthSuffix') ? (viewMonth + 1) + $t('datePicker.monthSuffix') : months[viewMonth] }}</button>
              </span>
              <button class="nav-btn" @click="nextMonth">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
              </button>
            </div>
            <div class="panel-weekdays">
              <span v-for="w in weekdays" :key="w" class="weekday">{{ w }}</span>
            </div>
            <div class="panel-days">
              <div v-for="(cell, i) in cells" :key="i"
                class="day-cell"
                :class="{
                  today: cell.isToday,
                  selected: cell.isSelected,
                  'other-month': cell.isOtherMonth,
                  disabled: cell.disabled
                }"
                @click="selectDay(cell)">
                <span class="day-num">{{ cell.day }}</span>
              </div>
            </div>
          </template>

          <!-- Months view -->
          <template v-if="panelMode === 'months'">
            <div class="panel-header">
              <button class="nav-btn" @click="viewYear--">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
              </button>
              <button class="title-btn" @click="switchingMode = true; panelMode = 'years'">{{ viewYear }}{{ $t('datePicker.yearSuffix') }}</button>
              <button class="nav-btn" @click="viewYear++">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
              </button>
            </div>
            <div class="month-grid">
              <div v-for="(m, i) in months" :key="i"
                class="month-cell"
                :class="{ selected: i === selectedMonth && viewYear === selectedYear }"
                @click="selectMonth(i)">
                {{ m }}
              </div>
            </div>
          </template>

          <!-- Years view -->
          <template v-if="panelMode === 'years'">
            <div class="panel-header">
              <button class="nav-btn" @click="yearRangeStart -= 12">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
              </button>
              <span class="panel-title-text">{{ yearRangeStart }}–{{ yearRangeStart + 11 }}</span>
              <button class="nav-btn" @click="yearRangeStart += 12">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
              </button>
            </div>
            <div class="month-grid">
              <div v-for="y in yearRange" :key="y"
                class="month-cell"
                :class="{ selected: y === viewYear }"
                @click="selectYear(y)">
                {{ y }}
              </div>
            </div>
          </template>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'

const { t, locale, tm } = useI18n()

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  min: { type: String, default: '' },
  max: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'change'])

const isOpen = ref(false)
const pickerRef = ref(null)
const panelRef = ref(null)
const panelMode = ref('days')
let switchingMode = false

const weekdays = computed(() => { const v = tm('datePicker.weekdays'); return Array.isArray(v) ? v : [] })
const months = computed(() => { const v = tm('datePicker.months'); return Array.isArray(v) ? v : [] })

const today = new Date()
const currentYear = today.getFullYear()
const viewYear = ref(currentYear)
const viewMonth = ref(today.getMonth())
const yearRangeStart = ref(Math.floor(currentYear / 12) * 12)

const yearRange = computed(() => {
  const start = yearRangeStart.value
  return Array.from({ length: 12 }, (_, i) => start + i)
})

const displayText = computed(() => {
  if (!props.modelValue) return props.placeholder || t('common.selectDate')
  return props.modelValue
})

const selectedDate = computed(() => parseDate(props.modelValue))
const selectedYear = computed(() => selectedDate.value ? selectedDate.value.getFullYear() : null)
const selectedMonth = computed(() => selectedDate.value ? selectedDate.value.getMonth() : null)

// Position the panel relative to the trigger
const panelStyle = ref({})

function updatePanelPosition() {
  if (!pickerRef.value || !isOpen.value) return
  const rect = pickerRef.value.getBoundingClientRect()
  panelStyle.value = {
    position: 'fixed',
    top: `${rect.bottom + 6}px`,
    left: `${rect.left}px`,
    zIndex: 99999
  }
}

function toggle() {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    panelMode.value = 'days'
    if (props.modelValue) {
      const d = parseDate(props.modelValue)
      if (d) {
        viewYear.value = d.getFullYear()
        viewMonth.value = d.getMonth()
      }
    }
    nextTick(updatePanelPosition)
  }
}

function clear() {
  emit('update:modelValue', '')
  emit('change', '')
  isOpen.value = false
}

function prevMonth() {
  if (viewMonth.value === 0) {
    viewMonth.value = 11
    viewYear.value--
  } else {
    viewMonth.value--
  }
}

function nextMonth() {
  if (viewMonth.value === 11) {
    viewMonth.value = 0
    viewYear.value++
  } else {
    viewMonth.value++
  }
}

function selectMonth(i) {
  switchingMode = true
  viewMonth.value = i
  panelMode.value = 'days'
}

function selectYear(y) {
  switchingMode = true
  viewYear.value = y
  yearRangeStart.value = Math.floor(y / 12) * 12
  panelMode.value = 'months'
}

function parseDate(str) {
  if (!str) return null
  const parts = str.split('-')
  if (parts.length !== 3) return null
  return new Date(+parts[0], +parts[1] - 1, +parts[2])
}

function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const todayStr = formatDate(today)

const cells = computed(() => {
  const year = viewYear.value
  const month = viewMonth.value
  const firstDay = new Date(year, month, 1).getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const daysInPrev = new Date(year, month, 0).getDate()
  const minDate = props.min ? parseDate(props.min) : null
  const maxDate = props.max ? parseDate(props.max) : null

  function isDisabled(dateStr) {
    if (minDate && dateStr < props.min) return true
    if (maxDate && dateStr > props.max) return true
    return false
  }

  const result = []
  for (let i = firstDay - 1; i >= 0; i--) {
    const d = daysInPrev - i
    const date = new Date(year, month - 1, d)
    const dateStr = formatDate(date)
    result.push({ day: d, dateStr, isToday: dateStr === todayStr, isSelected: dateStr === props.modelValue, isOtherMonth: true, disabled: isDisabled(dateStr) })
  }
  for (let d = 1; d <= daysInMonth; d++) {
    const date = new Date(year, month, d)
    const dateStr = formatDate(date)
    result.push({ day: d, dateStr, isToday: dateStr === todayStr, isSelected: dateStr === props.modelValue, isOtherMonth: false, disabled: isDisabled(dateStr) })
  }
  const remaining = 42 - result.length
  for (let d = 1; d <= remaining; d++) {
    const date = new Date(year, month + 1, d)
    const dateStr = formatDate(date)
    result.push({ day: d, dateStr, isToday: dateStr === todayStr, isSelected: dateStr === props.modelValue, isOtherMonth: true, disabled: isDisabled(dateStr) })
  }
  return result
})

function selectDay(cell) {
  if (cell.disabled) return
  emit('update:modelValue', cell.dateStr)
  emit('change', cell.dateStr)
  isOpen.value = false
}

function onClickOutside(e) {
  if (switchingMode) {
    switchingMode = false
    return
  }
  const clickedInTrigger = pickerRef.value && pickerRef.value.contains(e.target)
  const clickedInPanel = panelRef.value && panelRef.value.contains(e.target)
  if (!clickedInTrigger && !clickedInPanel) {
    isOpen.value = false
  }
}

function onScroll() {
  if (isOpen.value) updatePanelPosition()
}

onMounted(() => {
  document.addEventListener('click', onClickOutside)
  window.addEventListener('scroll', onScroll, { passive: true, capture: true })
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onClickOutside)
  window.removeEventListener('scroll', onScroll, { capture: true })
})
</script>

<style scoped>
.date-picker {
  position: relative;
  width: fit-content;
}

.date-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 300ms;
  user-select: none;
  white-space: nowrap;
  min-width: 120px;
}

.date-trigger:hover {
  border-color: #cbd5e1;
}

.date-icon {
  color: #64748b;
  flex-shrink: 0;
}

.date-text {
  font-size: 14px;
  color: #334155;
  flex: 1;
}

.date-text.placeholder {
  color: #94a3b8;
}

.date-clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  color: #94a3b8;
  cursor: pointer;
  transition: all 200ms;
  flex-shrink: 0;
}

.date-clear:hover {
  background: #e2e8f0;
  color: #475569;
}

</style>

<style>
/* Panel (global — teleported to body, scoped styles won't apply) */
.date-panel {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  padding: 12px;
  width: 280px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 4px;
}

.title-btn {
  background: none;
  border: none;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 200ms;
}

.title-btn:hover {
  background: #f1f5f9;
}

.panel-title-text {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: #64748b;
  transition: all 200ms;
}

.nav-btn:hover {
  background: #f1f5f9;
  color: #334155;
}

/* Weekdays */
.panel-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  margin-bottom: 4px;
}

.weekday {
  text-align: center;
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
  padding: 4px 0;
}

/* Days */
.panel-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.day-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  margin: 0 auto;
  border-radius: 50%;
  cursor: pointer;
  transition: all 200ms;
}

.day-num {
  font-size: 13px;
  color: #334155;
  font-weight: 500;
}

.day-cell:hover {
  background: #f1f5f9;
}

.day-cell.other-month .day-num {
  color: #cbd5e1;
}

.day-cell.disabled {
  opacity: 0.35;
  cursor: not-allowed;
  pointer-events: none;
}

.day-cell.today {
  position: relative;
}

.day-cell.today::after {
  content: '';
  position: absolute;
  inset: 2px;
  border-radius: 50%;
  border: 2px solid #2563eb;
  pointer-events: none;
}

.day-cell.selected {
  background: #2563eb;
}

.day-cell.selected .day-num {
  color: #fff;
  font-weight: 700;
}

.day-cell.selected:hover {
  background: #1d4ed8;
}

/* Month / Year grid */
.month-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.month-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 0;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  cursor: pointer;
  transition: all 200ms;
}

.month-cell:hover {
  background: #f1f5f9;
}

.month-cell.selected {
  background: #2563eb;
  color: #fff;
  font-weight: 700;
}

.month-cell.selected:hover {
  background: #1d4ed8;
}

/* Transition */
.panel-pop-enter-active,
.panel-pop-leave-active {
  transition: all 200ms cubic-bezier(0.23, 1, 0.32, 1);
}
.panel-pop-enter-from,
.panel-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* Night mode */
body.body-night .date-picker .date-trigger {
  background: #2a2f3b;
  border-color: #3a3f4b;
}
body.body-night .date-picker .date-trigger:hover {
  border-color: #4a5568;
}
body.body-night .date-picker .date-icon {
  color: #94a3b8;
}
body.body-night .date-picker .date-text {
  color: #e2e8f0;
}
body.body-night .date-picker .date-text.placeholder {
  color: #64748b;
}
body.body-night .date-picker .date-clear {
  color: #64748b;
}
body.body-night .date-picker .date-clear:hover {
  background: #3a3f4b;
  color: #94a3b8;
}
body.body-night .date-panel {
  background: #2a2f3b;
  border-color: #3a3f4b;
  box-shadow: 0 4px 16px rgba(0,0,0,0.4);
}
body.body-night .date-panel .title-btn {
  color: #e2e8f0;
}
body.body-night .date-panel .title-btn:hover {
  background: #3a3f4b;
}
body.body-night .date-panel .panel-title-text {
  color: #e2e8f0;
}
body.body-night .date-panel .nav-btn {
  color: #94a3b8;
}
body.body-night .date-panel .nav-btn:hover {
  background: #3a3f4b;
  color: #e2e8f0;
}
body.body-night .date-panel .weekday {
  color: #64748b;
}
body.body-night .date-panel .day-num {
  color: #e2e8f0;
}
body.body-night .date-panel .day-cell:hover {
  background: #3a3f4b;
}
body.body-night .date-panel .day-cell.other-month .day-num {
  color: #4a5568;
}
body.body-night .date-panel .day-cell.disabled {
  opacity: 0.3;
}
body.body-night .date-panel .day-cell.today::after {
  border-color: #60a5fa;
}
body.body-night .date-panel .day-cell.selected {
  background: #2563eb;
}
body.body-night .date-panel .day-cell.selected:hover {
  background: #1d4ed8;
}
body.body-night .date-panel .month-cell {
  color: #e2e8f0;
}
body.body-night .date-panel .month-cell:hover {
  background: #3a3f4b;
}
body.body-night .date-panel .month-cell.selected {
  background: #2563eb;
  color: #fff;
}
body.body-night .date-panel .month-cell.selected:hover {
  background: #1d4ed8;
}
</style>
