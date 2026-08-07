<template>
  <div
    class="tactical-point"
    :style="{ left: x + 'px', top: y + 'px' }"
    @click.stop="$emit('select')"
  >
    <div class="tp-ring" :style="{ borderColor: color }"></div>
    <div class="tp-diamond" :style="diamondStyle"></div>
    <div v-if="active" class="tp-orbit" :style="{ borderColor: color }"></div>
    <span class="tp-label" :style="{ color }">{{ label }}</span>
    <span class="tp-count">{{ count }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  x: { type: Number, default: 0 },
  y: { type: Number, default: 0 },
  color: { type: String, default: '#eab308' },
  label: { type: String, default: '' },
  count: { type: Number, default: 0 },
  active: { type: Boolean, default: false }
})

defineEmits(['select'])

const diamondStyle = computed(() => ({
  backgroundColor: props.color,
  color: props.color,
  boxShadow: `0 0 15px ${props.color}`,
  opacity: props.active ? 1 : 0.6,
  transform: props.active ? 'scale(1.5) rotate(45deg)' : 'scale(1) rotate(45deg)'
}))
</script>

<style scoped>
.tactical-point {
  position: absolute;
  transform: translate(-50%, -50%);
  pointer-events: auto;
  cursor: pointer;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.tp-ring {
  position: absolute;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transform: rotate(45deg);
  pointer-events: none;
}
.tp-diamond {
  width: 12px;
  height: 12px;
  transform: rotate(45deg);
  transition: all 0.5s ease;
}
.tp-orbit {
  position: absolute;
  width: 48px;
  height: 48px;
  border: 1px dashed;
  transform: rotate(45deg);
  animation: spin 4s linear infinite;
  opacity: 0.5;
  pointer-events: none;
}
.tp-label {
  font-size: 9px;
  font-family: monospace;
  font-weight: 700;
  letter-spacing: 2px;
  text-transform: uppercase;
  white-space: nowrap;
  text-shadow: 0 0 8px currentColor;
  pointer-events: none;
}
.tp-count {
  font-size: 10px;
  font-family: monospace;
  color: #94a3b8;
  pointer-events: none;
}

@keyframes spin {
  from { transform: rotate(45deg); }
  to { transform: rotate(405deg); }
}
</style>
