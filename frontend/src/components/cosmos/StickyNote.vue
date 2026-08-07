<template>
  <div class="sticky-note" :style="noteStyle">
    <div class="sticky-pin"></div>
    <div class="sticky-tape"></div>
    <p class="sticky-text">{{ text }}</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  text: { type: String, default: '' },
  color: { type: String, default: '#fde68a' },
  rotation: { type: Number, default: 0 }
})

const COLORS = ['#fde68a', '#fbcfe8', '#bfdbfe', '#a7f3d0']

const noteStyle = computed(() => {
  const c = props.color || COLORS[Math.abs(props.text.length) % COLORS.length]
  return {
    '--note-color': c,
    transform: `rotate(${props.rotation}deg)`
  }
})
</script>

<style scoped>
.sticky-note {
  position: relative;
  width: 110px;
  height: 110px;
  padding: 16px 12px 12px;
  background: var(--note-color, #fde68a);
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 2px 3px 6px rgba(0, 0, 0, 0.12);
  transition: transform 0.3s, box-shadow 0.3s;
}
.sticky-note:hover {
  transform: rotate(0deg) scale(1.2) !important;
  box-shadow: 2px 3px 10px rgba(0, 0, 0, 0.18);
  z-index: 100;
}
.sticky-pin {
  position: absolute;
  top: -5px;
  left: 50%;
  transform: translateX(-50%);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, #f87171, #b91c1c);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}
.sticky-tape {
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%) rotate(3deg);
  width: 48px;
  height: 20px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(4px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  clip-path: polygon(2% 10%, 98% 5%, 95% 90%, 5% 95%);
}
.sticky-text {
  margin: 0;
  font-family: cursive, sans-serif;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  line-height: 1.4;
  opacity: 0.8;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}
</style>
