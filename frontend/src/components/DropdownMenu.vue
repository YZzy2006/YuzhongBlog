<template>
  <div class="dropdown-menu" :class="{ open: isOpen }" ref="menuRef">
    <div class="dropdown-trigger" @click="isOpen = !isOpen">
      <span class="dropdown-label">{{ selectedLabel }}</span>
      <svg class="dropdown-arrow" xmlns="http://www.w3.org/2000/svg" height="1em" viewBox="0 0 512 512">
        <path d="M233.4 406.6c12.5 12.5 32.8 12.5 45.3 0l192-192c12.5-12.5 12.5-32.8 0-45.3s-32.8-12.5-45.3 0L256 338.7 86.6 169.4c-12.5-12.5-32.8-12.5-45.3 0s-12.5 32.8 0 45.3l192 192z"/>
      </svg>
    </div>
    <div class="dropdown-options" v-show="isOpen">
      <div class="dropdown-item" v-for="item in items" :key="item.value"
        :class="{ active: item.value === modelValue }"
        @click="select(item)">
        <span class="dropdown-item-text">{{ item.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps({
  items: { type: Array, required: true },
  modelValue: { type: [String, Number], default: '' },
  placeholder: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'change'])
const isOpen = ref(false)
const menuRef = ref(null)

const selectedLabel = computed(() => {
  const found = props.items.find(i => i.value === props.modelValue)
  return found ? found.label : (props.placeholder || t('common.pleaseSelect'))
})

function select(item) {
  emit('update:modelValue', item.value)
  emit('change', item.value)
  isOpen.value = false
}

function onClickOutside(e) {
  if (menuRef.value && !menuRef.value.contains(e.target)) {
    isOpen.value = false
  }
}

onMounted(() => document.addEventListener('click', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside))
</script>

<style scoped>
.dropdown-menu {
  width: fit-content;
  cursor: pointer;
  position: relative;
  transition: 300ms;
  color: #1a202c;
}

.dropdown-trigger {
  background-color: #f1f5f9;
  padding: 6px 12px;
  margin-bottom: 3px;
  border-radius: 8px;
  position: relative;
  z-index: 100;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 1px solid #e2e8f0;
  transition: 300ms;
  user-select: none;
  white-space: nowrap;
}

.dropdown-arrow {
  position: relative;
  height: 10px;
  width: 20px;
  fill: #64748b;
  z-index: 100;
  transition: 300ms;
  transform: rotate(-90deg);
}

.dropdown-options {
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  padding: 4px;
  background-color: #f1f5f9;
  border: 1px solid #e2e8f0;
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  z-index: 99;
  max-height: calc(6 * 36px);
  overflow-y: auto;
  overflow-x: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  scrollbar-width: thin;
  scrollbar-color: #94a3b8 transparent;
}
.dropdown-options::-webkit-scrollbar {
  width: 4px;
}
.dropdown-options::-webkit-scrollbar-track {
  background: transparent;
}
.dropdown-options::-webkit-scrollbar-thumb {
  background: #94a3b8;
  border-radius: 4px;
}

.dropdown-item {
  border-radius: 6px;
  transition: 300ms;
  background-color: transparent;
}

.dropdown-item-text {
  display: block;
  padding: 8px 12px;
  font-size: 14px;
  color: #334155;
  transition: 300ms;
  border-radius: 6px;
}

.dropdown-item:hover .dropdown-item-text {
  background-color: #e2e8f0;
}

.dropdown-item.active .dropdown-item-text {
  background-color: #dbeafe;
  color: #2563eb;
  font-weight: 600;
}

.dropdown-item.active:hover .dropdown-item-text {
  background-color: #bfdbfe;
}

/* Open state */
.dropdown-menu.open .dropdown-arrow {
  transform: rotate(0deg);
}

/* Transition */
.dropdown-options {
  animation: dropdown-slide 200ms ease;
}
@keyframes dropdown-slide {
  from { opacity: 0; transform: translateY(-6px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

<style>
/* Night mode */
body.body-night .dropdown-menu {
  color: #e2e8f0;
}
body.body-night .dropdown-menu .dropdown-trigger {
  background-color: #2a2f3b;
  border-color: #3a3f4b;
  color: #e2e8f0;
}
body.body-night .dropdown-menu .dropdown-arrow {
  fill: #94a3b8;
}
body.body-night .dropdown-menu .dropdown-options {
  background-color: #2a2f3b;
  border-color: #3a3f4b;
  box-shadow: 0 4px 16px rgba(0,0,0,0.4);
  scrollbar-color: #475569 transparent;
}
body.body-night .dropdown-menu .dropdown-options::-webkit-scrollbar-thumb {
  background: #475569;
}
body.body-night .dropdown-menu .dropdown-item-text {
  color: #e2e8f0;
}
body.body-night .dropdown-menu .dropdown-item:hover .dropdown-item-text {
  background-color: #323741;
}
body.body-night .dropdown-menu .dropdown-item.active .dropdown-item-text {
  background-color: #334155;
  color: #93c5fd;
  font-weight: 600;
}
body.body-night .dropdown-menu .dropdown-item.active:hover .dropdown-item-text {
  background-color: #3e4657;
}
</style>
