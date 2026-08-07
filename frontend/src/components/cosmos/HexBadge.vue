<template>
  <div class="hex-badge-wrapper" :class="{ locked: !unlocked }">
    <div class="hex-badge" :style="badgeStyle">
      <span class="hex-icon">{{ badge.icon }}</span>
      <span v-if="!unlocked" class="hex-lock">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
          <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
        </svg>
      </span>
      <span v-if="evolution" class="hex-evo-badge">{{ evolution.current }}/{{ evolution.total }}</span>
    </div>
    <div class="hex-tooltip">
      <div class="hex-tooltip-arrow"></div>
      <span class="hex-tooltip-type">{{ badge.id }}</span>
      <span class="hex-tooltip-rarity" :style="{ color: rarityColor }">{{ rarityLabel }}</span>
      <span class="hex-tooltip-name">{{ $t(`cosmos.badges.${badge.id}.name`) }}</span>
      <span class="hex-tooltip-desc" :style="{ color: unlocked ? rarityColor : '#6b5b3e' }">{{ $t(`cosmos.badges.${badge.id}.desc`) }}</span>
      <span v-if="evolution && evolution.nextId" class="hex-tooltip-evo">
        {{ $t('cosmos.evolution') }} {{ evolution.current }}/{{ evolution.total }}
        &middot; {{ $t('cosmos.evolutionNext') }}: {{ $t(`cosmos.badges.${evolution.nextId}.name`) }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { RARITY_CONFIG } from '../../cosmos/CosmosLevels'

const { t } = useI18n()

const props = defineProps({
  badge: { type: Object, required: true },
  unlocked: { type: Boolean, default: false },
  tier: { type: Number, default: 0 },
  evolution: { type: Object, default: null }
})

const RARITY_STYLES = {
  common:    { bg: 'linear-gradient(135deg, #4b5563, #6b7280)', border: '#6b7280', shadow: '0 0 6px rgba(107,114,128,0.3)' },
  uncommon:  { bg: 'linear-gradient(135deg, #15803d, #22c55e)', border: '#16a34a', shadow: '0 0 10px rgba(34,197,94,0.4)' },
  rare:      { bg: 'linear-gradient(135deg, #1d4ed8, #3b82f6)', border: '#2563eb', shadow: '0 0 14px rgba(59,130,246,0.5)' },
  epic:      { bg: 'linear-gradient(135deg, #2563eb, #a855f7)', border: '#2563eb', shadow: '0 0 18px rgba(59,130,246,0.5)' },
  legendary: { bg: 'linear-gradient(135deg, #b45309, #eab308)', border: '#d4af37', shadow: '0 0 24px rgba(234,179,8,0.6)' }
}

const RARITY_LOCKED_BORDER = {
  common:    'rgba(107,114,128,0.35)',
  uncommon:  'rgba(34,197,94,0.35)',
  rare:      'rgba(59,130,246,0.35)',
  epic:      'rgba(59,130,246,0.35)',
  legendary: 'rgba(234,179,8,0.35)'
}

const rarity = computed(() => props.badge?.rarity || 'common')
const rarityColor = computed(() => RARITY_CONFIG[rarity.value]?.color || '#9ca3af')
const rarityLabel = computed(() => {
  const key = RARITY_CONFIG[rarity.value]?.i18nKey || 'rarityCommon'
  return `★ ${t(`cosmos.${key}`)}`
})

const badgeStyle = computed(() => {
  const r = rarity.value
  if (!props.unlocked) {
    // Locked: show rarity border hint, keep grayscale icon
    const borderColor = RARITY_LOCKED_BORDER[r] || 'rgba(107,114,128,0.35)'
    return {
      borderColor,
      boxShadow: `inset 0 0 8px ${RARITY_CONFIG[r]?.glow || 'rgba(107,114,128,0.15)'}`
    }
  }
  const s = RARITY_STYLES[r]
  const style = {
    background: s.bg,
    borderColor: s.border,
    boxShadow: s.shadow
  }
  // Animation speed based on rarity (higher rarity = more intense)
  if (r === 'legendary') {
    style.backgroundSize = '400%'
    style.animation = 'rainbow 3s linear infinite'
  } else if (r === 'epic') {
    style.backgroundSize = '200% 200%'
    style.animation = 'hex-shine 2.5s ease-in-out infinite'
  } else if (r === 'rare') {
    style.backgroundSize = '200% 200%'
    style.animation = 'hex-shine 4s ease-in-out infinite'
  } else if (r === 'uncommon') {
    style.backgroundSize = '200% 200%'
    style.animation = 'hex-shine 6s ease-in-out infinite'
  }
  // common: no animation
  return style
})
</script>

<style scoped>
.hex-badge-wrapper {
  position: relative;
  display: inline-flex;
  cursor: pointer;
}
.hex-badge-wrapper:hover .hex-tooltip {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
  pointer-events: auto;
}
.hex-badge-wrapper:hover .hex-badge {
  transform: scale(1.1) translateY(-4px);
  filter: brightness(1.1);
}
.hex-badge-wrapper.locked {
  filter: grayscale(1) brightness(0.5);
  opacity: 0.6;
  cursor: default;
}
.hex-badge {
  width: 52px;
  height: 60px;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #231a16;
  border: 2px solid #8b6b4a;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), filter 0.3s;
  position: relative;
}
.hex-icon {
  font-size: 18px;
  filter: drop-shadow(0 1px 2px rgba(0,0,0,0.5));
}
.hex-lock {
  position: absolute;
  bottom: 4px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255,255,255,0.5);
  filter: drop-shadow(0 1px 2px rgba(0,0,0,0.6));
  pointer-events: none;
}
.hex-evo-badge {
  position: absolute;
  bottom: 6px;
  right: 4px;
  font-size: 7px;
  font-family: monospace;
  color: rgba(255,255,255,0.85);
  background: rgba(0,0,0,0.55);
  padding: 0 3px;
  border-radius: 3px;
  line-height: 1.4;
  pointer-events: none;
}
.hex-tooltip {
  position: absolute;
  top: 115%;
  left: 50%;
  transform: translateX(-50%) translateY(-8px);
  opacity: 0;
  pointer-events: none;
  transition: all 0.2s ease;
  background: rgba(26, 17, 11, 0.95);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(139, 107, 74, 0.6);
  border-radius: 12px;
  padding: 10px 14px;
  min-width: 120px;
  max-width: 200px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.8);
  z-index: 50;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}
.hex-tooltip-arrow {
  position: absolute;
  top: -6px;
  left: 50%;
  transform: translateX(-50%) rotate(45deg);
  width: 12px;
  height: 12px;
  background: rgba(26, 17, 11, 0.95);
  border-top: 1px solid rgba(139, 107, 74, 0.6);
  border-left: 1px solid rgba(139, 107, 74, 0.6);
}
.hex-tooltip-type {
  font-size: 9px;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: #8b6b4a;
}
.hex-tooltip-rarity {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 1px;
}
.hex-tooltip-name {
  font-size: 13px;
  font-weight: 700;
  color: #e8e4d9;
}
.hex-tooltip-desc {
  font-size: 11px;
}
.hex-tooltip-evo {
  font-size: 10px;
  color: #8b6b4a;
  margin-top: 2px;
  padding-top: 4px;
  border-top: 1px solid rgba(139, 107, 74, 0.2);
  text-align: center;
  line-height: 1.4;
}

@keyframes hex-shine {
  0% { background-position: -200% -200%; }
  100% { background-position: 200% 200%; }
}
@keyframes rainbow {
  0% { background-position: 0% 50%; }
  100% { background-position: 100% 50%; }
}
</style>

<!-- Unscoped keyframes for inline style animation references -->
<style>
@keyframes hex-shine {
  0% { background-position: -200% -200%; }
  100% { background-position: 200% 200%; }
}
@keyframes rainbow {
  0% { background-position: 0% 50%; }
  100% { background-position: 100% 50%; }
}
</style>
