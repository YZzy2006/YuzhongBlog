<template>
  <div class="mode-switcher">
    <div class="mode-track">
      <div class="mode-indicator" :class="{ starship: modelValue === 'starship' }"></div>
      <button
        class="mode-btn"
        :class="{ active: modelValue === 'alchemy' }"
        @click="$emit('update:modelValue', 'alchemy')"
      >
        <span class="mode-icon"> </span>
        <span class="mode-label">{{ $t('cosmos.alchemyLab') }}</span>
      </button>
      <button
        class="mode-btn"
        :class="{ active: modelValue === 'starship' }"
        @click="$emit('update:modelValue', 'starship')"
      >
        <span class="mode-icon"> </span>
        <span class="mode-label">{{ $t('cosmos.starship') }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  modelValue: { type: String, default: 'alchemy' }
})
defineEmits(['update:modelValue'])
</script>

<style scoped>
.mode-switcher {
  position: fixed;
  top: 68px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 110;
}
.mode-track {
  display: flex;
  position: relative;
  background: rgba(30, 30, 30, 0.6);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 9999px;
  padding: 4px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
}
.mode-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 9999px;
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 0;
}
.mode-indicator.starship {
  transform: translateX(100%);
}
.mode-btn {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 9999px;
  transition: color 0.3s;
  white-space: nowrap;
}
.mode-btn .mode-label {
  font-family: monospace;
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  letter-spacing: 1px;
  transition: color 0.3s;
}
.mode-btn .mode-icon {
  font-size: 14px;
}
.mode-btn.active .mode-label {
  color: #e2e8f0;
}

@media (max-width: 768px) {
  .mode-btn {
    padding: 6px 12px;
  }
  .mode-btn .mode-label {
    font-size: 10px;
  }
}
</style>
