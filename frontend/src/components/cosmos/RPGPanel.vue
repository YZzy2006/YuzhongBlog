<template>
  <div class="rpg-panel" :class="mode">
    <!-- Top-left: Profile card -->
    <div class="rpg-topleft">
      <div class="rpg-profile-card" :class="mode">
        <!-- Diagonal stripe overlay -->
        <div class="rpg-stripes"></div>

        <div class="rpg-profile-main">
          <!-- Level indicator: circle for alchemy, diamond for starship -->
          <div v-if="mode === 'alchemy'" class="rpg-level-circle">
            <span class="rpg-level-lv">Lv.</span>
            <span class="rpg-level-num">{{ level }}</span>
          </div>
          <div v-else class="rpg-level-diamond">
            <span class="rpg-level-num">{{ level }}</span>
          </div>

          <!-- Info -->
          <div class="rpg-info">
            <div class="rpg-title-row">
              <span class="rpg-level-label">{{ $t('cosmos.levelLabel') }} {{ level }}</span>
              <span class="rpg-level-name">{{ levelName }}</span>
            </div>
            <!-- XP bar -->
            <div class="rpg-xp-bar" :class="mode">
              <div class="rpg-xp-fill" :class="mode" :style="{ width: percentage + '%' }"></div>
              <div class="rpg-xp-shimmer" :class="mode"></div>
            </div>
            <span class="rpg-xp-text">{{ currentXP }} / {{ neededXP }} XP</span>
          </div>
        </div>

        <!-- Today stats (desktop) -->
        <div class="rpg-today">
          <span class="rpg-today-exp">+{{ todayXP }} XP</span>
          <span class="rpg-checkin" :class="{ done: isCheckedIn }">
            {{ isCheckedIn ? $t('cosmos.checkedIn') : $t('cosmos.checkIn') }}
          </span>
        </div>
      </div>
    </div>

    <!-- Top-right: Achievement toggle -->
    <div class="rpg-topright">
      <button class="rpg-achieve-btn" :class="mode" @click="$emit('toggleAchievements')">
        <span class="rpg-achieve-icon"> </span>
        <span class="rpg-achieve-label">{{ $t('cosmos.achievements') }}</span>
        <span class="rpg-achieve-count">{{ badgeCount }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  level: { type: Number, default: 1 },
  levelName: { type: String, default: '' },
  currentXP: { type: Number, default: 0 },
  neededXP: { type: Number, default: 150 },
  percentage: { type: Number, default: 0 },
  badgeCount: { type: Number, default: 0 },
  stats: { type: Object, default: () => ({}) },
  isMobile: { type: Boolean, default: false },
  todayXP: { type: Number, default: 0 },
  isCheckedIn: { type: Boolean, default: false },
  mode: { type: String, default: 'alchemy' }
})

defineEmits(['toggleAchievements'])
</script>

<style scoped>
.rpg-panel {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 10;
}

/* Top-left */
.rpg-topleft {
  position: absolute;
  top: 68px;
  left: 12px;
  pointer-events: auto;
}

/* === PROFILE CARD === */
.rpg-profile-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  backdrop-filter: blur(12px);
  border-radius: 1.5rem;
  box-shadow: 0 5px 25px rgba(0, 0, 0, 0.6);
  overflow: hidden;
  transition: background 0.5s, border-color 0.5s;
}
/* Alchemy: wood/gold theme */
.rpg-profile-card.alchemy {
  background: rgba(35, 26, 22, 0.9);
  border: 2px solid rgba(139, 107, 74, 0.6);
  font-family: serif;
}
/* Starship: dark cyberpunk theme */
.rpg-profile-card.starship {
  background: rgba(10, 10, 10, 0.85);
  border: 1px solid #333;
  font-family: monospace;
}

/* Diagonal stripe overlay */
.rpg-stripes {
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    -45deg,
    transparent,
    transparent 10px,
    rgba(255, 255, 255, 0.02) 10px,
    rgba(255, 255, 255, 0.02) 20px
  );
  pointer-events: none;
}

.rpg-profile-main {
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
  z-index: 1;
}

/* === LEVEL INDICATORS === */
/* Alchemy: circular with Lv. label */
.rpg-level-circle {
  width: 56px;
  height: 56px;
  background: #1a110b;
  border: 3px solid #d4af37;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 0 15px rgba(212, 175, 55, 0.4);
  position: relative;
}
.rpg-level-lv {
  font-size: 10px;
  font-weight: 900;
  color: #d4af37;
  position: absolute;
  top: 6px;
  letter-spacing: 1px;
}
/* Starship: diamond */
.rpg-level-diamond {
  width: 64px;
  height: 64px;
  background: #111;
  border: 1.5px solid #eab308;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transform: rotate(45deg);
  box-shadow: 0 0 15px rgba(234, 179, 8, 0.3);
}
.rpg-level-diamond .rpg-level-num {
  transform: rotate(-45deg);
}

.rpg-level-num {
  font-size: 24px;
  font-weight: 900;
  color: #e8e4d9;
  letter-spacing: -0.05em;
  margin-top: 4px;
}

.rpg-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.rpg-title-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.rpg-level-label {
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 2px;
  text-transform: uppercase;
}
.alchemy .rpg-level-label {
  font-family: monospace;
  color: #8b6b4a;
}
.starship .rpg-level-label {
  font-family: monospace;
  color: #64748b;
}
.rpg-level-name {
  font-size: 18px;
  font-weight: 900;
  color: #e8e4d9;
  letter-spacing: 3px;
}

/* === XP BAR === */
.rpg-xp-bar {
  width: 180px;
  height: 10px;
  overflow: hidden;
  position: relative;
  transition: background 0.5s, border-color 0.5s;
}
/* Alchemy: rounded pill with gold border */
.rpg-xp-bar.alchemy {
  background: #1a110b;
  border: 1px solid rgba(139, 107, 74, 0.3);
  border-radius: 9999px;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.5);
}
/* Starship: trapezoid with gray border */
.rpg-xp-bar.starship {
  background: #111;
  border: 1px solid #333;
  clip-path: polygon(0 0, 100% 0, 98% 100%, 2% 100%);
}

/* Fill */
.rpg-xp-fill.alchemy {
  height: 100%;
  background: linear-gradient(90deg, #8b6b4a, #d4af37, #fde047);
  border-radius: 9999px;
  transition: width 0.8s ease;
}
.rpg-xp-fill.starship {
  height: 100%;
  background: #eab308;
  transition: width 0.8s ease;
}

/* Shimmer / stripe overlay */
.rpg-xp-shimmer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
.rpg-xp-shimmer.alchemy {
  background: rgba(255, 255, 255, 0.3);
  animation: pulse-glow 2s ease-in-out infinite;
}
.rpg-xp-shimmer.starship {
  background: repeating-linear-gradient(90deg, transparent, transparent 20px, rgba(0,0,0,0.5) 20px, rgba(0,0,0,0.5) 22px);
}

.rpg-xp-text {
  font-size: 10px;
  font-family: monospace;
  letter-spacing: 1px;
}
.alchemy .rpg-xp-text { color: #8b6b4a; }
.starship .rpg-xp-text { color: #64748b; }

/* === TODAY STATS === */
.rpg-today {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  padding-left: 16px;
  position: relative;
  z-index: 1;
}
.alchemy .rpg-today { border-left: 1px solid rgba(139, 107, 74, 0.3); }
.starship .rpg-today { border-left: 1px solid #333; }

.rpg-today-exp {
  font-size: 14px;
  font-family: monospace;
  font-weight: 900;
  color: #10b981;
}
.rpg-checkin {
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 2px;
  padding: 3px 8px;
  border: 1px solid rgba(100, 116, 139, 0.5);
  border-radius: 6px;
  color: #64748b;
}
.rpg-checkin.done {
  color: #d4af37;
  border-color: rgba(212, 175, 55, 0.5);
  background: rgba(212, 175, 55, 0.1);
  box-shadow: 0 0 10px rgba(212, 175, 55, 0.2);
}

/* === TOP-RIGHT: ACHIEVEMENTS === */
.rpg-topright {
  position: absolute;
  top: 68px;
  right: 12px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  pointer-events: auto;
}
.rpg-achieve-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  backdrop-filter: blur(12px);
  border-radius: 9999px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.rpg-achieve-btn.alchemy {
  background: rgba(35, 26, 22, 0.9);
  border: 1px solid rgba(212, 175, 55, 0.3);
  color: #d4af37;
  font-family: serif;
}
.rpg-achieve-btn.alchemy:hover {
  background: rgba(212, 175, 55, 0.15);
  border-color: rgba(212, 175, 55, 0.5);
}
.rpg-achieve-btn.starship {
  background: rgba(10, 10, 10, 0.85);
  border: 1px solid #333;
  color: #eab308;
  font-family: monospace;
  font-size: 11px;
  letter-spacing: 1px;
}
.rpg-achieve-btn.starship:hover {
  background: rgba(234, 179, 8, 0.1);
  border-color: #eab308;
}
.rpg-achieve-icon { font-size: 16px; }
.rpg-achieve-label { font-weight: 600; }
.rpg-achieve-count {
  padding: 2px 8px;
  border-radius: 9999px;
  font-size: 11px;
  font-family: monospace;
}
.alchemy .rpg-achieve-count {
  background: rgba(139, 107, 74, 0.3);
  color: #e8e4d9;
}
.starship .rpg-achieve-count {
  background: rgba(100, 116, 139, 0.2);
  color: #94a3b8;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
@keyframes pulse-glow {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
}

@media (max-width: 768px) {
  .rpg-topleft { top: 68px; }
  .rpg-profile-card { padding: 10px 14px; gap: 10px; }
  .rpg-level-circle { width: 40px; height: 40px; }
  .rpg-level-diamond { width: 48px; height: 48px; }
  .rpg-level-num { font-size: 18px; }
  .rpg-level-name { font-size: 14px; }
  .rpg-xp-bar { width: 100px; height: 8px; }
  .rpg-today { display: none; }
}
</style>
