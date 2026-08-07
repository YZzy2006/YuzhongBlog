<template>
  <div class="games-page">
    <el-card shadow="hover" class="game-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="page-title">{{ $t('games.title') }}</span>
            <span class="header-sub">{{ $t('games.subtitle') }}</span>
          </div>
          <div class="tab-bar-outer">
            <button v-show="tabCanScrollLeft" class="tab-arrow tab-arrow-left" @click="scrollTabs(-120)">‹</button>
            <div :class="['tab-bar-wrap', { 'scrolled-end': !tabCanScrollRight }]" ref="tabBarWrapRef" @scroll="onTabScroll">
              <div class="tab-bar">
                <button :class="['tab-btn', { active: currentGame === 'snake' }]" @click="switchGame('snake')">
                  🐍 <span class="tab-label">{{ $t('games.snake') }}</span>
                </button>
                <button :class="['tab-btn', { active: currentGame === 'tetris' }]" @click="switchGame('tetris')">
                  🧱 <span class="tab-label">{{ $t('games.tetris') }}</span>
                </button>
                <button :class="['tab-btn', { active: currentGame === 'game2048' }]" @click="switchGame('game2048')">
                  🔢 <span class="tab-label">{{ $t('games.game2048') }}</span>
                </button>
                <button :class="['tab-btn', { active: currentGame === 'flappy' }]" @click="switchGame('flappy')">
                  🐤 <span class="tab-label">{{ $t('games.flappy') }}</span>
                </button>
                <button :class="['tab-btn', { active: currentGame === 'breakout' }]" @click="switchGame('breakout')">
                  🧩 <span class="tab-label">{{ $t('games.breakout') }}</span>
                </button>
                <button :class="['tab-btn', { active: currentGame === 'shooter' }]" @click="switchGame('shooter')">
                  🚀 <span class="tab-label">{{ $t('games.shooter') }}</span>
                </button>
                <button :class="['tab-btn', { active: currentGame === 'chineseChess' }]" @click="switchGame('chineseChess')">
                  ♟ <span class="tab-label">{{ $t('games.chineseChess') }}</span>
                </button>
                <button :class="['tab-btn', { active: currentGame === 'minesweeper' }]" @click="switchGame('minesweeper')">
                  💣 <span class="tab-label">{{ $t('games.minesweeper') }}</span>
                </button>

                <button :class="['tab-btn', { active: currentGame === 'fruitninja' }]" @click="switchGame('fruitninja')">
                  🍉 <span class="tab-label">{{ $t('games.fruitninja') }}</span>
                </button>
              </div>
            </div>
            <button v-show="tabCanScrollRight" class="tab-arrow tab-arrow-right" @click="scrollTabs(120)">›</button>
          </div>
        </div>
      </template>

      <div class="game-area">
        <div :class="['game-canvas-wrap', { 'touch-locked': canvasTouchLocked }]">
          <canvas ref="canvasRef" :width="canvasWidth" :height="canvasHeight" class="game-canvas"
            @click="onCanvasClick"
            @contextmenu.prevent="onCanvasRightClick"
            @touchmove.prevent="onCanvasTouchMove"
            @touchstart="onCanvasTouchStart"
            @touchend="onCanvasTouchEnd" />
          <div v-if="gameState === 'idle'" class="game-overlay">
            <div class="overlay-content">
              <span class="overlay-icon">{{ gameIcon }}</span>
              <span class="overlay-text">{{ currentGame === 'chineseChess' ? $t('games.chessDifficulty') : currentGame === 'minesweeper' ? $t('games.msSelectDifficulty') : $t('games.clickStart') }}</span>
              <div v-if="currentGame === 'chineseChess'" class="chess-difficulty-panel">
                <button v-for="d in [1,2,3,4]" :key="d"
                  :class="['chess-diff-btn', { active: chessDifficulty === d }]"
                  @click.stop="chessDifficulty = d">
                  {{ $t('games.chessDiff' + d) }}
                </button>
              </div>
              <div v-if="currentGame === 'minesweeper'" class="chess-difficulty-panel">
                <button v-for="d in ['easy','medium','hard']" :key="d"
                  :class="['chess-diff-btn', { active: msDifficulty === d }]"
                  @click.stop="msDifficulty = d">
                  {{ $t('games.ms' + d.charAt(0).toUpperCase() + d.slice(1)) }}
                </button>
              </div>
              <button v-if="currentGame === 'shooter'" class="shop-btn" @click.stop="shooterShopOpen = true">
                🛒 {{ $t('games.shop') }}
              </button>
            </div>
          </div>
          <div v-if="gameState === 'over'" class="game-overlay game-over-overlay">
            <div class="overlay-content">
              <span class="overlay-icon">{{ currentGame === 'chineseChess' ? (chessGameResult === 'win' ? '🎉' : '💭') : '💥' }}</span>
              <span class="overlay-title">{{ currentGame === 'chineseChess' ? $t('games.chess' + (chessGameResult === 'win' ? 'Win' : chessGameResult === 'draw' ? 'Draw' : 'Lose')) : $t('games.gameOver') }}</span>
              <span class="overlay-score">{{ $t('games.finalScore') }}: {{ score }}</span>
              <span v-if="currentGame === 'chineseChess'" class="overlay-combo">
                {{ $t('games.chessPiecesLeft') }}: {{ chessPlayerPieces }} | {{ $t('games.chessTime') }}: {{ chessElapsedMin }}{{ $t('games.chessMin') }}
              </span>
              <span v-if="currentGame === 'shooter' && shooterMaxCombo > 0" class="overlay-combo">
                {{ $t('games.maxCombo') }}: {{ shooterMaxCombo }}
                <span class="combo-multiplier">x{{ shooterMaxCombo >= 20 ? 5 : shooterMaxCombo >= 10 ? 3 : shooterMaxCombo >= 5 ? 2 : 1 }}</span>
              </span>
            </div>
          </div>
          <div v-if="gameState === 'won'" class="game-overlay game-won-overlay">
            <div class="overlay-content">
              <span class="overlay-icon">🎉</span>
              <span class="overlay-title">{{ $t('games.youWin') }}</span>
              <span class="overlay-score">{{ $t('games.finalScore') }}: {{ score }}</span>
            </div>
          </div>
          <!-- Pause menu -->
          <div v-if="gameState === 'paused'" class="game-overlay pause-overlay" @touchstart.prevent.stop>
            <div class="pause-content">
              <span class="pause-icon">⏸</span>
              <span class="pause-title">{{ $t('games.pauseTitle') }}</span>
              <div class="pause-actions">
                <button class="pause-btn pause-btn-resume" @click="startGame" @touchstart.prevent.stop="startGame">
                  ▶ {{ $t('games.pauseResume') }}
                </button>
                <button class="pause-btn pause-btn-restart" @click="resetGame(); startGame()" @touchstart.prevent.stop="resetGame(); startGame()">
                  🔄 {{ $t('games.pauseRestart') }}
                </button>
                <button class="pause-btn pause-btn-quit" @click="resetGame" @touchstart.prevent.stop="resetGame">
                  🚪 {{ $t('games.pauseQuit') }}
                </button>
              </div>
            </div>
          </div>
          <!-- Revival dialog -->
          <div v-if="shooterRevivalDialog" class="game-overlay revival-overlay" @touchstart.prevent.stop>
            <div class="revival-content">
              <span class="revival-icon">💀</span>
              <span class="revival-title">{{ $t('games.revivalTitle') }}</span>
              <span class="revival-desc">{{ $t('games.revivalDesc') }}</span>
              <div class="revival-actions">
                <button class="revival-btn revival-btn-accept" @click="revivePlayer" @touchstart.prevent.stop="revivePlayer">
                  💰 {{ $t('games.revivalAccept') }}
                </button>
                <button class="revival-btn revival-btn-decline" @click="declineRevival" @touchstart.prevent.stop="declineRevival">
                  {{ $t('games.revivalDecline') }}
                </button>
              </div>
            </div>
          </div>

          <!-- Achievement toast -->
          <transition name="achievement-fade">
            <div v-if="achievementToast" class="achievement-toast">
              <span class="achievement-toast-icon">{{ achievementToast.icon }}</span>
              <div class="achievement-toast-text">
                <span class="achievement-toast-title">{{ $t('games.achievementUnlockedToast') }}</span>
                <span class="achievement-toast-name">{{ $t('games.ach_' + achievementToast.id) }}</span>
              </div>
            </div>
          </transition>
        </div>

        <!-- Achievement button -->
        <button v-if="currentGame === 'shooter'" class="achievement-btn" @click="showAchievements = true" :title="$t('games.achievements')">
          🏆 {{ unlockedAchievements.length }}/{{ ACHIEVEMENTS.length }}
        </button>

        <!-- Achievement panel dialog -->
        <el-dialog v-model="showAchievements" :title="$t('games.achievements')" width="420px" class="achievement-dialog">
          <div class="achievement-grid">
            <div v-for="a in ACHIEVEMENTS" :key="a.id" class="achievement-item" :class="{ 'achievement-locked': !unlockedAchievements.includes(a.id) }">
              <span class="achievement-icon">{{ a.icon }}</span>
              <span class="achievement-name">{{ $t('games.ach_' + a.id) }}</span>
              <span v-if="unlockedAchievements.includes(a.id)" class="achievement-check">✓</span>
            </div>
          </div>
          <div class="achievement-footer">
            {{ $t('games.achProgress') }}: {{ unlockedAchievements.length }} / {{ ACHIEVEMENTS.length }}
          </div>
        </el-dialog>

        <!-- Tetris next piece preview -->
        <div v-if="currentGame === 'tetris'" class="next-preview">
          <span class="preview-label">{{ $t('games.nextPiece') }}</span>
          <canvas ref="nextCanvasRef" :width="100" :height="100" class="next-canvas" />
        </div>
      </div>

      <div class="game-stats">
        <div class="stat-item">
          <span class="stat-label">{{ $t('games.score') }}</span>
          <span class="stat-value">{{ score }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">{{ $t('games.highScore') }}</span>
          <span class="stat-value best">{{ highScore }}</span>
        </div>
        <div v-if="currentGame === 'tetris'" class="stat-item">
          <span class="stat-label">{{ $t('games.level') }}</span>
          <span class="stat-value">{{ level }}</span>
        </div>
        <div v-if="currentGame === 'tetris'" class="stat-item">
          <span class="stat-label">{{ $t('games.lines') }}</span>
          <span class="stat-value">{{ linesCleared }}</span>
        </div>
        <div v-if="currentGame === 'breakout'" class="stat-item">
          <span class="stat-label">{{ $t('games.bricksLeft') }}</span>
          <span class="stat-value">{{ bricksLeft }}</span>
        </div>
      </div>

      <div class="game-controls">
        <el-button type="primary" size="large" @click="startGame" :disabled="gameState === 'playing'">
          {{ gameState === 'paused' ? $t('games.resume') : $t('games.start') }}
        </el-button>
        <el-button size="large" @click="pauseGame" :disabled="gameState !== 'playing'">
          {{ $t('games.pause') }}
        </el-button>
        <el-button size="large" @click="resetGame">
          {{ $t('games.reset') }}
        </el-button>
        <el-button size="large" :type="sfxMuted ? 'info' : 'success'" @click="toggleSfx" :title="sfxMuted ? 'Unmute' : 'Mute'">
          {{ sfxMuted ? '🔇' : '🔊' }}
        </el-button>
      </div>

      <!-- Snake mobile controls -->
      <div class="mobile-controls" v-if="currentGame === 'snake'">
        <div class="dpad">
          <button class="dpad-btn up" @touchstart.prevent="setDirection('up')" @click="setDirection('up')">▲</button>
          <div class="dpad-row">
            <button class="dpad-btn left" @touchstart.prevent="setDirection('left')" @click="setDirection('left')">◀</button>
            <button class="dpad-btn center"></button>
            <button class="dpad-btn right" @touchstart.prevent="setDirection('right')" @click="setDirection('right')">▶</button>
          </div>
          <button class="dpad-btn down" @touchstart.prevent="setDirection('down')" @click="setDirection('down')">▼</button>
        </div>
      </div>

      <!-- Tetris mobile controls -->
      <div class="mobile-controls" v-if="currentGame === 'tetris'">
        <div class="tetris-pad">
          <button class="dpad-btn" @touchstart.prevent="tetrisAction('left')" @click="tetrisAction('left')">◀</button>
          <button class="dpad-btn" @touchstart.prevent="tetrisAction('rotate')" @click="tetrisAction('rotate')">↻</button>
          <button class="dpad-btn" @touchstart.prevent="tetrisAction('right')" @click="tetrisAction('right')">▶</button>
          <button class="dpad-btn" @touchstart.prevent="tetrisAction('down')" @touchend="tetrisActionRelease('down')"
            @mousedown="tetrisAction('down')" @mouseup="tetrisActionRelease('down')" @mouseleave="tetrisActionRelease('down')">▼</button>
          <button class="dpad-btn hard-drop" @touchstart.prevent="tetrisAction('drop')" @click="tetrisAction('drop')">⏬</button>
        </div>
      </div>

      <!-- 2048 mobile controls -->
      <div class="mobile-controls" v-if="currentGame === 'game2048'">
        <div class="dpad">
          <button class="dpad-btn up" @touchstart.prevent="move2048('up')" @click="move2048('up')">▲</button>
          <div class="dpad-row">
            <button class="dpad-btn left" @touchstart.prevent="move2048('left')" @click="move2048('left')">◀</button>
            <button class="dpad-btn center"></button>
            <button class="dpad-btn right" @touchstart.prevent="move2048('right')" @click="move2048('right')">▶</button>
          </div>
          <button class="dpad-btn down" @touchstart.prevent="move2048('down')" @click="move2048('down')">▼</button>
        </div>
      </div>

      <!-- Breakout mobile controls -->
      <div class="mobile-controls" v-if="currentGame === 'breakout'">
        <div class="tetris-pad">
          <button class="dpad-btn" @touchstart.prevent="breakoutLeft = true" @touchend="breakoutLeft = false"
            @touchcancel="breakoutLeft = false" @mousedown="breakoutLeft = true"
            @mouseup="breakoutLeft = false" @mouseleave="breakoutLeft = false">◀</button>
          <button class="dpad-btn hard-drop" @touchstart.prevent="launchBall" @click="launchBall">🚀</button>
          <button class="dpad-btn" @touchstart.prevent="breakoutRight = true" @touchend="breakoutRight = false"
            @touchcancel="breakoutRight = false" @mousedown="breakoutRight = true"
            @mouseup="breakoutRight = false" @mouseleave="breakoutRight = false">▶</button>
        </div>
      </div>

      <!-- Shooter mobile controls -->
      <div class="mobile-controls" v-if="currentGame === 'shooter'">
        <div class="tetris-pad">
          <button class="dpad-btn" @touchstart.prevent="shooterLeft = true" @touchend="shooterLeft = false"
            @touchcancel="shooterLeft = false" @mousedown="shooterLeft = true"
            @mouseup="shooterLeft = false" @mouseleave="shooterLeft = false">◀</button>
          <button class="dpad-btn hard-drop" @touchstart.prevent="shooterFire" @click="shooterFire">🔥</button>
          <button class="dpad-btn" @touchstart.prevent="shooterRight = true" @touchend="shooterRight = false"
            @touchcancel="shooterRight = false" @mousedown="shooterRight = true"
            @mouseup="shooterRight = false" @mouseleave="shooterRight = false">▶</button>
        </div>
      </div>

      <!-- Minesweeper mobile controls -->
      <div class="mobile-controls" v-if="currentGame === 'minesweeper'">
        <div class="dpad">
          <button class="dpad-btn up" @touchstart.prevent="msMove('up')" @click="msMove('up')">▲</button>
          <div class="dpad-row">
            <button class="dpad-btn left" @touchstart.prevent="msMove('left')" @click="msMove('left')">◀</button>
            <button class="dpad-btn center" @touchstart.prevent="msActionReveal" @click="msActionReveal">⬜</button>
            <button class="dpad-btn right" @touchstart.prevent="msMove('right')" @click="msMove('right')">▶</button>
          </div>
          <button class="dpad-btn down" @touchstart.prevent="msMove('down')" @click="msMove('down')">▼</button>
        </div>
        <div class="tetris-pad" style="margin-top: 8px;">
          <button class="dpad-btn hard-drop" @touchstart.prevent="msActionReveal" @click="msActionReveal">🔍</button>
          <button class="dpad-btn" @touchstart.prevent="msActionFlag" @click="msActionFlag">🚩</button>
        </div>
      </div>

      <div class="game-tips">
        <span v-if="currentGame === 'snake'">{{ $t('games.snakeTip') }}</span>
        <span v-else-if="currentGame === 'tetris'">{{ $t('games.tetrisTip') }}</span>
        <span v-else-if="currentGame === 'game2048'">{{ $t('games.game2048Tip') }}</span>
        <span v-else-if="currentGame === 'flappy'">{{ $t('games.flappyTip') }}</span>
        <span v-else-if="currentGame === 'breakout'">{{ $t('games.breakoutTip') }}</span>
        <span v-else-if="currentGame === 'shooter'">{{ $t('games.shooterTip') }}</span>
        <span v-else-if="currentGame === 'minesweeper'">{{ $t('games.minesweeperTip') }}</span>

        <span v-else-if="currentGame === 'fruitninja'">{{ $t('games.fruitninjaTip') }}</span>
        <span v-else>{{ $t('games.chineseChessTip') }}</span>
      </div>
    </el-card>

    <!-- Shooter Shop Dialog -->
    <el-dialog v-model="shooterShopOpen" :title="$t('games.shop')" width="540px" class="shop-dialog" :close-on-click-modal="true">
      <div class="shop-content">
        <div class="shop-top-bar">
          <button class="shop-close-btn" @click="shooterShopOpen = false">{{ $t('games.close') }}</button>
        </div>
        <div class="shop-balance">
          <span class="coin-icon">💰</span>
          <span class="coin-amount">{{ shooterCosmetics.coins || 0 }}</span>
          <span class="coin-label">{{ $t('games.coins') }}</span>
          <div class="tier-legend">
            <span class="tier-tag tier-basic">{{ $t(TIER_COLORS.basic.labelKey) }}</span>
            <span class="tier-tag tier-rare">{{ $t(TIER_COLORS.rare.labelKey) }}</span>
            <span class="tier-tag tier-epic">{{ $t(TIER_COLORS.epic.labelKey) }}</span>
            <span class="tier-tag tier-legendary">{{ $t(TIER_COLORS.legendary.labelKey) }}</span>
          </div>
        </div>
        <el-tabs v-model="shopTab" class="shop-tabs">
          <el-tab-pane :label="$t('games.playerSkin')" name="player_skin">
            <div class="shop-grid">
              <div v-for="(skin, id) in PLAYER_SKINS" :key="id" :class="['shop-item', 'tier-border-' + skin.tier, { equipped: shooterCosmetics.equippedPlayerSkin === id }]">
                <span class="tier-badge" :class="'tier-' + skin.tier">{{ $t(TIER_COLORS[skin.tier].labelKey) }}</span>
                <canvas v-preview-canvas="{ type: 'player', id }" class="item-preview" width="104" height="104" :style="{ boxShadow: skin.glow ? '0 0 12px ' + skin.glow : 'none' }"></canvas>
                <div class="item-name">{{ skin.name }}</div>
                <div class="item-action">
                  <button v-if="shooterCosmetics.equippedPlayerSkin === id" class="btn-equipped" disabled>{{ $t('games.equipped') }}</button>
                  <button v-else class="btn-equip" @click="doEquip('player_skin', id)">{{ $t('games.equip') }}</button>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane :label="$t('games.enemySkin')" name="enemy_skin">
            <div class="shop-grid">
              <div v-for="(skin, id) in ENEMY_SKINS" :key="id" :class="['shop-item', 'tier-border-' + skin.tier, { equipped: shooterCosmetics.equippedEnemySkin === id }]">
                <span class="tier-badge" :class="'tier-' + skin.tier">{{ $t(TIER_COLORS[skin.tier].labelKey) }}</span>
                <canvas v-preview-canvas="{ type: 'enemy', id }" class="item-preview" width="104" height="104" :style="{ boxShadow: skin.tier !== 'basic' ? '0 0 10px ' + skin.normal : 'none' }"></canvas>
                <div class="item-name">{{ skin.name }}</div>
                <div class="item-action">
                  <button v-if="shooterCosmetics.equippedEnemySkin === id" class="btn-equipped" disabled>{{ $t('games.equipped') }}</button>
                  <button v-else class="btn-equip" @click="doEquip('enemy_skin', id)">{{ $t('games.equip') }}</button>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane :label="$t('games.bulletStyle')" name="bullet_style">
            <div class="shop-grid">
              <div v-for="(style, id) in BULLET_STYLES" :key="id" :class="['shop-item', 'tier-border-' + style.tier, { equipped: shooterCosmetics.equippedBulletStyle === id }]">
                <span class="tier-badge" :class="'tier-' + style.tier">{{ $t(TIER_COLORS[style.tier].labelKey) }}</span>
                <canvas v-preview-canvas="{ type: 'bullet', id }" class="item-preview" width="104" height="104" :style="{ boxShadow: style.glow ? '0 0 10px ' + style.glow : 'none' }"></canvas>
                <div class="item-name">{{ style.name }}</div>
                <div class="item-action">
                  <button v-if="shooterCosmetics.equippedBulletStyle === id" class="btn-equipped" disabled>{{ $t('games.equipped') }}</button>
                  <button v-else class="btn-equip" @click="doEquip('bullet_style', id)">{{ $t('games.equip') }}</button>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane :label="$t('games.powerupSkin')" name="powerup_skin">
            <div class="shop-grid">
              <div v-for="(skin, id) in POWERUP_SKINS" :key="id" :class="['shop-item', 'tier-border-' + skin.tier, { equipped: shooterCosmetics.equippedPowerupSkin === id }]">
                <span class="tier-badge" :class="'tier-' + skin.tier">{{ $t(TIER_COLORS[skin.tier].labelKey) }}</span>
                <canvas v-preview-canvas="{ type: 'powerup', id }" class="item-preview" width="104" height="104" :style="{ boxShadow: skin.glow ? '0 0 12px ' + skin.glow : 'none' }"></canvas>
                <div class="item-name">{{ skin.name }}</div>
                <div class="item-action">
                  <button v-if="shooterCosmetics.equippedPowerupSkin === id" class="btn-equipped" disabled>{{ $t('games.equipped') }}</button>
                  <button v-else class="btn-equip" @click="doEquip('powerup_skin', id)">{{ $t('games.equip') }}</button>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane :label="$t('games.shopSkills')" name="skill">
            <div class="shop-grid">
              <div v-for="(skill, id) in SHOP_SKILLS" :key="id" :class="['shop-item', 'tier-border-rare', { equipped: shooterCosmetics.equippedSkill1 === id || shooterCosmetics.equippedSkill2 === id || shooterCosmetics.equippedSkill3 === id }]">
                <span class="tier-badge tier-rare">{{ $t('games.shopSkills') }}</span>
                <div class="item-preview skill-preview">{{ skill.icon }}</div>
                <div class="item-name">{{ $t('games.skill' + id.charAt(0).toUpperCase() + id.slice(1).replace('_skill', '')) }}</div>
                <div class="item-desc">{{ $t('games.skill' + id.charAt(0).toUpperCase() + id.slice(1).replace('_skill', '') + 'Desc') }}</div>
                <div class="item-action">
                  <button class="btn-equip" @click="toggleEquipSkill(id)">{{ shooterCosmetics.equippedSkill1 === id || shooterCosmetics.equippedSkill2 === id || shooterCosmetics.equippedSkill3 === id ? $t('games.unequipSkill') : $t('games.equipSkill') }}</button>
                </div>
              </div>
            </div>
            <!-- Skill slot equip UI -->
            <div class="skill-slots">
              <div class="skill-slot-title">{{ $t('games.skillSlot') }}</div>
              <div class="skill-slot-row">
                <div v-for="si in 3" :key="si" class="skill-slot" @click="clearSkillSlot(si - 1)">
                  <span v-if="shooterCosmetics['equippedSkill' + si]">{{ SHOP_SKILLS[shooterCosmetics['equippedSkill' + si]]?.icon || '?' }} {{ $t('games.skill' + shooterCosmetics['equippedSkill' + si].charAt(0).toUpperCase() + shooterCosmetics['equippedSkill' + si].slice(1).replace('_skill', '')) }}</span>
                  <span v-else class="slot-empty">{{ $t('games.skillSlot') }} {{ si }}</span>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'

const { t } = useI18n()

// ===== Sound Effects System (Web Audio API) =====
const SFX = {
  ctx: null,
  muted: false,
  bgmPlaying: false,
  bgmOsc: null,
  bgmGain: null,
  init() {
    if (this.ctx) return
    try { this.ctx = new (window.AudioContext || window.webkitAudioContext)() } catch { return }
  },
  resume() { if (this.ctx?.state === 'suspended') this.ctx.resume() },
  play(type) {
    if (this.muted || !this.ctx) return
    this.resume()
    const c = this.ctx, now = c.currentTime
    switch (type) {
      case 'shoot': this._beep(880, 0.03, 'square', 0.08); break
      case 'enemyDie': this._noise(0.08, 0.12); break
      case 'bossAppear': this._sweep(120, 60, 0.5, 'sawtooth', 0.15); break
      case 'bossDie': this._noise(0.3, 0.25); this._sweep(200, 80, 0.4, 'square', 0.1); break
      case 'powerup': this._arpeggio([523, 659, 784], 0.08, 'sine', 0.12); break
      case 'playerHit': this._sweep(300, 100, 0.15, 'sawtooth', 0.15); break
      case 'gameOver': this._sweep(400, 100, 0.6, 'sine', 0.15); break
      case 'levelUp': this._arpeggio([523, 659, 784, 1047], 0.1, 'square', 0.1); break
      case 'combo': this._beep(1200, 0.05, 'sine', 0.06); break
      case 'revive': this._arpeggio([262, 330, 392, 523, 659], 0.08, 'sine', 0.15); break
      case 'skill': this._sweep(600, 1200, 0.3, 'sine', 0.1); break
      case 'bomb': this._noise(0.4, 0.3); this._sweep(150, 50, 0.3, 'sawtooth', 0.12); break
    }
  },
  _beep(freq, dur, type, vol) {
    const c = this.ctx, o = c.createOscillator(), g = c.createGain()
    o.type = type; o.frequency.value = freq
    g.gain.setValueAtTime(vol, c.currentTime)
    g.gain.exponentialRampToValueAtTime(0.001, c.currentTime + dur)
    o.connect(g); g.connect(c.destination)
    o.start(); o.stop(c.currentTime + dur)
  },
  _sweep(f1, f2, dur, type, vol) {
    const c = this.ctx, o = c.createOscillator(), g = c.createGain()
    o.type = type
    o.frequency.setValueAtTime(f1, c.currentTime)
    o.frequency.exponentialRampToValueAtTime(Math.max(f2, 20), c.currentTime + dur)
    g.gain.setValueAtTime(vol, c.currentTime)
    g.gain.exponentialRampToValueAtTime(0.001, c.currentTime + dur)
    o.connect(g); g.connect(c.destination)
    o.start(); o.stop(c.currentTime + dur)
  },
  _noise(dur, vol) {
    const c = this.ctx, bufSize = c.sampleRate * dur
    const buf = c.createBuffer(1, bufSize, c.sampleRate)
    const data = buf.getChannelData(0)
    for (let i = 0; i < bufSize; i++) data[i] = (Math.random() * 2 - 1) * 0.5
    const src = c.createBufferSource(), g = c.createGain()
    src.buffer = buf
    g.gain.setValueAtTime(vol, c.currentTime)
    g.gain.exponentialRampToValueAtTime(0.001, c.currentTime + dur)
    src.connect(g); g.connect(c.destination)
    src.start(); src.stop(c.currentTime + dur)
  },
  _arpeggio(freqs, gap, type, vol) {
    freqs.forEach((f, i) => setTimeout(() => this._beep(f, gap * 1.5, type, vol), i * gap * 1000))
  },
  startBGM() {
    if (this.muted || !this.ctx || this.bgmPlaying) return
    this.resume()
    const c = this.ctx
    this.bgmGain = c.createGain()
    this.bgmGain.gain.value = 0.03
    this.bgmGain.connect(c.destination)
    this.bgmPlaying = true
    const melody = [262, 294, 330, 349, 392, 349, 330, 294]
    let idx = 0
    const playNote = () => {
      if (!this.bgmPlaying) return
      const o = c.createOscillator()
      o.type = 'sine'
      o.frequency.value = melody[idx % melody.length]
      o.connect(this.bgmGain)
      o.start()
      o.stop(c.currentTime + 0.3)
      idx++
      this.bgmOsc = setTimeout(playNote, 400)
    }
    playNote()
  },
  stopBGM() {
    this.bgmPlaying = false
    if (this.bgmOsc) { clearTimeout(this.bgmOsc); this.bgmOsc = null }
  },
  toggleMute() {
    this.muted = !this.muted
    if (this.muted) this.stopBGM()
    return this.muted
  }
}

const sfxMuted = ref(false)
const toggleSfx = () => { sfxMuted.value = SFX.toggleMute() }

// ===== Achievement System =====
const ACHIEVEMENTS = [
  { id: 'first_blood', icon: '🎯', condition: (s) => s.totalKills >= 1 },
  { id: 'combo_5', icon: '🔥', condition: (s) => s.maxCombo >= 5 },
  { id: 'combo_10', icon: '💥', condition: (s) => s.maxCombo >= 10 },
  { id: 'combo_20', icon: '☄️', condition: (s) => s.maxCombo >= 20 },
  { id: 'boss_slayer', icon: '⚔️', condition: (s) => s.bossKills >= 1 },
  { id: 'boss_hunter', icon: '🗡️', condition: (s) => s.bossKills >= 5 },
  { id: 'level_5', icon: '⭐', condition: (s) => s.maxLevel >= 5 },
  { id: 'level_10', icon: '🌟', condition: (s) => s.maxLevel >= 10 },
  { id: 'level_20', icon: '💫', condition: (s) => s.maxLevel >= 20 },
  { id: 'max_life', icon: '❤️‍🔥', condition: (s) => s.maxLives >= 7 },
  { id: 'survivor', icon: '🛡️', condition: (s) => s.revives >= 3 },
  { id: 'score_1000', icon: '🏆', condition: (s) => s.highScore >= 1000 },
  { id: 'score_5000', icon: '👑', condition: (s) => s.highScore >= 5000 },
  { id: 'score_10000', icon: '💎', condition: (s) => s.highScore >= 10000 },
  { id: 'powerup_10', icon: '🎁', condition: (s) => s.powerupsCollected >= 10 },
  { id: 'powerup_50', icon: '🎰', condition: (s) => s.powerupsCollected >= 50 },
  { id: 'skill_user', icon: '⚡', condition: (s) => s.skillsUsed >= 10 },
  { id: 'no_hit_3', icon: '🏃', condition: (s) => s.noHitStreak >= 3 },
  { id: 'no_hit_5', icon: '💨', condition: (s) => s.noHitStreak >= 5 },
  { id: 'boss_nohit', icon: '🎯', condition: (s) => s.bossNoHit >= 1 },
  { id: 'double_kill', icon: '✨', condition: (s) => s.doubleScoreKills >= 20 },
  { id: 'coin_spender', icon: '💸', condition: (s) => s.coinsSpent >= 100 },
  { id: 'all_bosses', icon: '🐉', condition: (s) => s.bossKills >= 10 },
  { id: 'score_50000', icon: '🌈', condition: (s) => s.highScore >= 50000 },
]
const unlockedAchievements = ref(JSON.parse(localStorage.getItem('game_achievements') || '[]'))
const achievementToast = ref(null)
let achievementToastTimer = null

function checkAchievements(stats) {
  let newUnlock = false
  for (const a of ACHIEVEMENTS) {
    if (unlockedAchievements.value.includes(a.id)) continue
    if (a.condition(stats)) {
      unlockedAchievements.value.push(a.id)
      newUnlock = true
      showAchievementToast(a)
    }
  }
  if (newUnlock) localStorage.setItem('game_achievements', JSON.stringify(unlockedAchievements.value))
}

function showAchievementToast(achievement) {
  achievementToast.value = achievement
  if (achievementToastTimer) clearTimeout(achievementToastTimer)
  achievementToastTimer = setTimeout(() => { achievementToast.value = null }, 3000)
  SFX.play('levelUp')
}

const achievementStats = ref({ totalKills: 0, maxCombo: 0, bossKills: 0, maxLevel: 0, maxLives: 0, revives: 0, highScore: 0, powerupsCollected: 0, skillsUsed: 0, doubleScoreKills: 0, noHitStreak: 0, coinsSpent: 0, bossNoHit: 0 })
const showAchievements = ref(false)

// ===== Minesweeper state =====
let msBoard = []
let msRows = 9, msCols = 9, msMineCount = 10
const msDifficulty = ref('easy')
let msRevealed = 0, msFlagged = 0
let msGameOver = false, msStartTime = 0, msElapsed = 0
let msFirstClick = true
let msCursorR = 0, msCursorC = 0

function initMinesweeper() {
  const cfg = msDifficulty.value === 'hard' ? [16, 30, 99] : msDifficulty.value === 'medium' ? [16, 16, 40] : [9, 9, 10]
  msRows = cfg[0]; msCols = cfg[1]; msMineCount = cfg[2]
  msBoard = Array.from({ length: msRows }, () =>
    Array.from({ length: msCols }, () => ({ mine: false, revealed: false, flagged: false, count: 0 }))
  )
  msRevealed = 0; msFlagged = 0; msGameOver = false
  msStartTime = 0; msElapsed = 0; msFirstClick = true
  msCursorR = 0; msCursorC = 0
}
function msPlaceMines(safeR, safeC) {
  let placed = 0
  while (placed < msMineCount) {
    const r = Math.floor(Math.random() * msRows)
    const c = Math.floor(Math.random() * msCols)
    if (msBoard[r][c].mine) continue
    if (Math.abs(r - safeR) <= 1 && Math.abs(c - safeC) <= 1) continue
    msBoard[r][c].mine = true; placed++
  }
  msCountNeighbors()
}
function msCountNeighbors() {
  for (let r = 0; r < msRows; r++) {
    for (let c = 0; c < msCols; c++) {
      if (msBoard[r][c].mine) continue
      let cnt = 0
      for (let dr = -1; dr <= 1; dr++) for (let dc = -1; dc <= 1; dc++) {
        const nr = r + dr, nc = c + dc
        if (nr >= 0 && nr < msRows && nc >= 0 && nc < msCols && msBoard[nr][nc].mine) cnt++
      }
      msBoard[r][c].count = cnt
    }
  }
}
function msReveal(r, c) {
  if (r < 0 || r >= msRows || c < 0 || c >= msCols) return
  const cell = msBoard[r][c]
  if (cell.revealed || cell.flagged) return
  cell.revealed = true; msRevealed++
  if (cell.mine) { msGameOver = true; gameState.value = 'over'; saveHighScore(); return }
  if (cell.count === 0) {
    const queue = [[r, c]]
    while (queue.length) {
      const [cr, cc] = queue.shift()
      for (let dr = -1; dr <= 1; dr++) for (let dc = -1; dc <= 1; dc++) {
        const nr = cr + dr, nc = cc + dc
        if (nr < 0 || nr >= msRows || nc < 0 || nc >= msCols) continue
        const n = msBoard[nr][nc]
        if (n.revealed || n.flagged || n.mine) continue
        n.revealed = true; msRevealed++
        if (n.count === 0) queue.push([nr, nc])
      }
    }
  }
  msCheckWin()
}
function msToggleFlag(r, c) {
  const cell = msBoard[r][c]
  if (cell.revealed) return
  cell.flagged = !cell.flagged; msFlagged += cell.flagged ? 1 : -1
}
function msCheckWin() {
  if (msRevealed === msRows * msCols - msMineCount) {
    gameState.value = 'won'
    score.value = Math.max(0, (msRows * msCols - msMineCount) * (msDifficulty.value === 'hard' ? 3 : msDifficulty.value === 'medium' ? 2 : 1) - Math.floor(msElapsed / 1000))
    saveHighScore()
  }
}
function updateMinesweeper(dt) {
  if (msStartTime && !msGameOver && gameState.value === 'playing') msElapsed = Date.now() - msStartTime
}
const MS_NUM_COLORS = ['', '#3b82f6', '#22c55e', '#ef4444', '#1e40af', '#92400e', '#0891b2', '#1e293b', '#6b7280']
function drawMinesweeper(ctx) {
  const W = 400, H = 400
  const cellSize = Math.floor(Math.min((W - 20) / msCols, (H - 50) / msRows))
  const offsetX = Math.floor((W - cellSize * msCols) / 2)
  const offsetY = 40
  ctx.fillStyle = '#1a1f2e'; ctx.fillRect(0, 0, W, H)
  // HUD
  ctx.fillStyle = '#e4e8f0'; ctx.font = 'bold 14px sans-serif'
  ctx.textAlign = 'left'; ctx.fillText('💣 ' + (msMineCount - msFlagged), 10, 25)
  ctx.textAlign = 'right'; ctx.fillText('⏱ ' + Math.floor(msElapsed / 1000) + 's', W - 10, 25)
  if (msDifficulty.value) {
    ctx.textAlign = 'center'; ctx.font = '12px sans-serif'
    ctx.fillText(t('games.ms' + msDifficulty.value.charAt(0).toUpperCase() + msDifficulty.value.slice(1)), W / 2, 25)
  }
  // Draw grid
  for (let r = 0; r < msRows; r++) {
    for (let c = 0; c < msCols; c++) {
      const x = offsetX + c * cellSize, y = offsetY + r * cellSize
      const cell = msBoard[r][c]
      const isCursor = r === msCursorR && c === msCursorC
      if (cell.revealed) {
        ctx.fillStyle = '#2d3548'; ctx.fillRect(x, y, cellSize, cellSize)
        ctx.strokeStyle = '#1a1f2e'; ctx.lineWidth = 0.5; ctx.strokeRect(x, y, cellSize, cellSize)
        if (cell.mine) {
          ctx.fillStyle = '#ef4444'; ctx.beginPath(); ctx.arc(x + cellSize / 2, y + cellSize / 2, cellSize * 0.3, 0, Math.PI * 2); ctx.fill()
          ctx.strokeStyle = '#1e293b'; ctx.lineWidth = 2
          ctx.beginPath(); ctx.moveTo(x + cellSize / 2, y + cellSize * 0.2); ctx.lineTo(x + cellSize / 2, y + cellSize * 0.8); ctx.stroke()
          ctx.beginPath(); ctx.moveTo(x + cellSize * 0.2, y + cellSize / 2); ctx.lineTo(x + cellSize * 0.8, y + cellSize / 2); ctx.stroke()
        } else if (cell.count > 0) {
          ctx.fillStyle = MS_NUM_COLORS[cell.count] || '#e4e8f0'
          ctx.font = `bold ${Math.floor(cellSize * 0.6)}px sans-serif`
          ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
          ctx.fillText(cell.count, x + cellSize / 2, y + cellSize / 2)
        }
      } else {
        // Unrevealed - 3D raised effect
        ctx.fillStyle = '#4a5568'; ctx.fillRect(x, y, cellSize, cellSize)
        ctx.fillStyle = '#6b7280'; ctx.fillRect(x, y, cellSize, 2); ctx.fillRect(x, y, 2, cellSize)
        ctx.fillStyle = '#374151'; ctx.fillRect(x + cellSize - 2, y, 2, cellSize); ctx.fillRect(x, y + cellSize - 2, cellSize, 2)
        if (cell.flagged) {
          ctx.fillStyle = '#ef4444'; ctx.beginPath()
          ctx.moveTo(x + cellSize * 0.3, y + cellSize * 0.7)
          ctx.lineTo(x + cellSize * 0.7, y + cellSize * 0.5)
          ctx.lineTo(x + cellSize * 0.3, y + cellSize * 0.3)
          ctx.closePath(); ctx.fill()
          ctx.strokeStyle = '#92400e'; ctx.lineWidth = 2
          ctx.beginPath(); ctx.moveTo(x + cellSize * 0.3, y + cellSize * 0.3); ctx.lineTo(x + cellSize * 0.3, y + cellSize * 0.8); ctx.stroke()
        }
      }
      // Cursor highlight
      if (isCursor && gameState.value === 'playing') {
        ctx.strokeStyle = '#fbbf24'; ctx.lineWidth = 2; ctx.strokeRect(x + 1, y + 1, cellSize - 2, cellSize - 2)
      }
    }
  }
  // Game over: reveal all mines
  if (msGameOver) {
    ctx.fillStyle = 'rgba(0,0,0,0.5)'; ctx.fillRect(0, 0, W, H)
    ctx.fillStyle = '#ef4444'; ctx.font = 'bold 32px sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
    ctx.fillText('💥 踩雷了！', W / 2, H / 2 - 20)
    ctx.fillStyle = '#a0a8b8'; ctx.font = '16px sans-serif'
    ctx.fillText('点击重新开始', W / 2, H / 2 + 20)
  }
  ctx.textAlign = 'left'; ctx.textBaseline = 'alphabetic'
}

// ===== Fruit Ninja state =====
const FRUIT_TYPES = {
  apple:      { color: '#ef4444', radius: 22, score: 10, label: '🍎' },
  orange:     { color: '#f97316', radius: 20, score: 15, label: '🍊' },
  watermelon: { color: '#22c55e', radius: 30, score: 20, label: '🍉' },
  banana:     { color: '#eab308', radius: 18, score: 12, label: '🍌' },
  strawberry: { color: '#e11d48', radius: 15, score: 50, label: '🍓' }
}
const FRUIT_KEYS = Object.keys(FRUIT_TYPES)
let fnFruits = []
let fnBombs = []
let fnSliceTrail = []
let fnLives = 3
let fnCombo = 0
let fnComboTimer = 0
let fnSpawnTimer = 0
let fnSpawnInterval = 1500
let fnParticles = []
let fnSlicing = false
let fnMouseDown = false
let fnSliceX = 0, fnSliceY = 0
let fnLastSliceX = 0, fnLastSliceY = 0
const FN_GRAVITY = 400
const FN_MAX_LIVES = 3

function initFruitNinja() {
  fnFruits = []; fnBombs = []; fnSliceTrail = []
  fnLives = FN_MAX_LIVES; fnCombo = 0; fnComboTimer = 0
  fnSpawnTimer = 0; fnSpawnInterval = 1500; fnParticles = []
  fnSlicing = false; fnMouseDown = false; fnSliceX = 0; fnSliceY = 0; fnLastSliceX = 0; fnLastSliceY = 0
}
function fnSpawnFruit() {
  const type = FRUIT_KEYS[Math.floor(Math.random() * FRUIT_KEYS.length)]
  const W = 400, H = 400
  const x = 40 + Math.random() * (W - 80)
  const vx = (Math.random() - 0.5) * 80
  const vy = -(350 + Math.random() * 150)
  fnFruits.push({ x, y: H + 20, vx, vy, type, angle: 0, angleV: (Math.random() - 0.5) * 8, sliced: false, sliceTime: 0 })
}
function fnSpawnBomb() {
  const W = 400, H = 400
  const x = 40 + Math.random() * (W - 80)
  const vx = (Math.random() - 0.5) * 80
  const vy = -(300 + Math.random() * 100)
  fnBombs.push({ x, y: H + 20, vx, vy, angle: 0, angleV: (Math.random() - 0.5) * 6 })
}
function fnCheckSlice(x1, y1, x2, y2) {
  let slicedAny = false
  for (const f of fnFruits) {
    if (f.sliced) continue
    const info = FRUIT_TYPES[f.type]
    // Line-circle intersection
    const dx = x2 - x1, dy = y2 - y1
    const fx = x1 - f.x, fy = y1 - f.y
    const a = dx * dx + dy * dy
    const b = 2 * (fx * dx + fy * dy)
    const c = fx * fx + fy * fy - (info.radius + 5) * (info.radius + 5)
    let hit = false
    if (a > 0) {
      const disc = b * b - 4 * a * c
      if (disc >= 0) {
        const t = (-b - Math.sqrt(disc)) / (2 * a)
        if (t >= 0 && t <= 1) hit = true
      }
    }
    if (c <= 0) hit = true
    if (hit) {
      f.sliced = true; f.sliceTime = 0; slicedAny = true
      fnCombo++; fnComboTimer = 1000
      const multiplier = fnCombo >= 5 ? 3 : fnCombo >= 3 ? 2 : 1
      score.value += info.score * multiplier
      // Particles
      for (let i = 0; i < 8; i++) {
        const angle = Math.random() * Math.PI * 2
        const speed = 60 + Math.random() * 120
        fnParticles.push({
          x: f.x, y: f.y,
          vx: Math.cos(angle) * speed, vy: Math.sin(angle) * speed - 50,
          color: info.color, life: 600, maxLife: 600, size: 3 + Math.random() * 4
        })
      }
    }
  }
  // Check bombs
  for (const b of fnBombs) {
    const dx = x2 - x1, dy = y2 - y1
    const fx = x1 - b.x, fy = y1 - b.y
    const a = dx * dx + dy * dy
    const bCoeff = 2 * (fx * dx + fy * dy)
    const cCoeff = fx * fx + fy * fy - 25 * 25
    let hit = false
    if (a > 0) {
      const disc = bCoeff * bCoeff - 4 * a * cCoeff
      if (disc >= 0) {
        const t = (-bCoeff - Math.sqrt(disc)) / (2 * a)
        if (t >= 0 && t <= 1) hit = true
      }
    }
    if (cCoeff <= 0) hit = true
    if (hit) {
      fnLives--
      for (let i = 0; i < 15; i++) {
        const angle = Math.random() * Math.PI * 2
        const speed = 80 + Math.random() * 150
        fnParticles.push({
          x: b.x, y: b.y,
          vx: Math.cos(angle) * speed, vy: Math.sin(angle) * speed - 50,
          color: '#f97316', life: 800, maxLife: 800, size: 4 + Math.random() * 5
        })
      }
      fnBombs = fnBombs.filter(bb => bb !== b)
      if (fnLives <= 0) { gameState.value = 'over'; saveHighScore() }
      break
    }
  }
  if (!slicedAny) fnCombo = 0
}
function updateFruitNinja(dt) {
  const dtSec = dt / 1000
  const W = 400, H = 400
  // Spawn
  fnSpawnTimer += dt
  if (fnSpawnTimer >= fnSpawnInterval) {
    fnSpawnTimer = 0
    fnSpawnFruit()
    if (Math.random() < 0.25) fnSpawnFruit()
    if (Math.random() < 0.15 + score.value / 5000) fnSpawnBomb()
    fnSpawnInterval = Math.max(600, fnSpawnInterval - 5)
  }
  // Update fruits
  for (const f of fnFruits) {
    if (f.sliced) { f.sliceTime += dt; continue }
    f.x += f.vx * dtSec; f.y += f.vy * dtSec
    f.vy += FN_GRAVITY * dtSec
    f.angle += f.angleV * dtSec
  }
  // Update bombs
  for (const b of fnBombs) {
    b.x += b.vx * dtSec; b.y += b.vy * dtSec
    b.vy += FN_GRAVITY * dtSec
    b.angle += b.angleV * dtSec
  }
  // Update particles
  for (const p of fnParticles) {
    p.x += p.vx * dtSec; p.y += p.vy * dtSec
    p.vy += 200 * dtSec; p.life -= dt
  }
  fnParticles = fnParticles.filter(p => p.life > 0)
  // Remove off-screen and sliced fruits
  fnFruits = fnFruits.filter(f => {
    if (f.sliced) return f.sliceTime < 500
    if (f.y > H + 50 && f.vy > 0) { fnLives--; return false }
    return true
  })
  fnBombs = fnBombs.filter(b => b.y < H + 50)
  if (fnLives <= 0 && gameState.value === 'playing') { gameState.value = 'over'; saveHighScore() }
  // Combo decay
  if (fnComboTimer > 0) { fnComboTimer -= dt; if (fnComboTimer <= 0) fnCombo = 0 }
  // Slice trail decay
  fnSliceTrail = fnSliceTrail.filter(t => Date.now() - t.time < 150)
}
function drawFruitNinja(ctx) {
  const W = 400, H = 400
  // Background — wood table
  ctx.fillStyle = '#3b2516'; ctx.fillRect(0, 0, W, H)
  ctx.strokeStyle = '#5a3a20'; ctx.lineWidth = 1
  ctx.globalAlpha = 0.15
  for (let i = 0; i < H; i += 12) {
    const offset = ((i * 7 + 13) % 5) - 2.5 // deterministic pseudo-random
    ctx.beginPath(); ctx.moveTo(0, i); ctx.lineTo(W, i + offset); ctx.stroke()
  }
  ctx.globalAlpha = 1
  // Draw bombs
  for (const b of fnBombs) {
    ctx.save(); ctx.translate(b.x, b.y); ctx.rotate(b.angle)
    ctx.fillStyle = '#1e1e1e'; ctx.beginPath(); ctx.arc(0, 0, 16, 0, Math.PI * 2); ctx.fill()
    ctx.strokeStyle = '#f97316'; ctx.lineWidth = 2
    ctx.beginPath(); ctx.moveTo(0, -16); ctx.lineTo(4, -28); ctx.stroke()
    ctx.fillStyle = '#f97316'; ctx.beginPath(); ctx.arc(4, -30, 4, 0, Math.PI * 2); ctx.fill()
    ctx.restore()
  }
  // Draw fruits
  for (const f of fnFruits) {
    const info = FRUIT_TYPES[f.type]
    ctx.save(); ctx.translate(f.x, f.y); ctx.rotate(f.angle)
    if (f.sliced) {
      const t = Math.min(1, f.sliceTime / 300)
      ctx.globalAlpha = 1 - t
      // Two halves
      ctx.fillStyle = info.color
      ctx.beginPath(); ctx.arc(-5 * t, 0, info.radius, 0.1, Math.PI - 0.1); ctx.fill()
      ctx.beginPath(); ctx.arc(5 * t, 0, info.radius, Math.PI + 0.1, -0.1); ctx.fill()
      ctx.globalAlpha = 1
    } else {
      // Glow
      ctx.shadowColor = info.color; ctx.shadowBlur = 10
      ctx.fillStyle = info.color
      ctx.beginPath(); ctx.arc(0, 0, info.radius, 0, Math.PI * 2); ctx.fill()
      ctx.shadowBlur = 0
      // Highlight
      ctx.fillStyle = 'rgba(255,255,255,0.3)'
      ctx.beginPath(); ctx.arc(-info.radius * 0.3, -info.radius * 0.3, info.radius * 0.35, 0, Math.PI * 2); ctx.fill()
      // Label
      ctx.font = `${info.radius}px sans-serif`
      ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
      ctx.fillText(info.label, 0, 2)
    }
    ctx.restore()
  }
  // Draw slice trail
  if (fnSliceTrail.length > 1) {
    ctx.strokeStyle = 'rgba(255,255,255,0.7)'; ctx.lineWidth = 3; ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.moveTo(fnSliceTrail[0].x, fnSliceTrail[0].y)
    for (let i = 1; i < fnSliceTrail.length; i++) ctx.lineTo(fnSliceTrail[i].x, fnSliceTrail[i].y)
    ctx.stroke()
  }
  // Particles
  for (const p of fnParticles) {
    ctx.globalAlpha = p.life / p.maxLife
    ctx.fillStyle = p.color
    ctx.beginPath(); ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2); ctx.fill()
  }
  ctx.globalAlpha = 1
  // HUD
  ctx.fillStyle = '#f472b6'; ctx.font = 'bold 16px sans-serif'; ctx.textAlign = 'left'
  for (let i = 0; i < FN_MAX_LIVES; i++) ctx.fillText(i < fnLives ? '❤' : '🖤', 10 + i * 22, 25)
  ctx.fillStyle = '#fbbf24'; ctx.font = 'bold 18px sans-serif'; ctx.textAlign = 'right'
  ctx.fillText(score.value, W - 10, 25)
  // Combo
  if (fnCombo >= 2) {
    ctx.fillStyle = fnCombo >= 5 ? '#f43f5e' : fnCombo >= 3 ? '#f97316' : '#fbbf24'
    ctx.font = `bold ${20 + fnCombo * 2}px sans-serif`; ctx.textAlign = 'center'
    ctx.fillText(`COMBO x${fnCombo}`, W / 2, 60)
  }
}

// ===== Chinese Chess state =====
const chessDifficulty = ref(2)
let chessBoard = []
let chessTurn = 'red'
let chessSelected = null
let chessHistory = []
let chessThinking = false
let chessLastMove = null
let chessLegalMoves = []
let chessGameResult = null
const chessPlayerPieces = ref(0)
const chessElapsedMin = ref(0)

function loadAchievementStats() {
  const saved = localStorage.getItem('game_achievement_stats')
  if (saved) achievementStats.value = { ...achievementStats.value, ...JSON.parse(saved) }
}
function saveAchievementStats() {
  localStorage.setItem('game_achievement_stats', JSON.stringify(achievementStats.value))
}

const canvasRef = ref(null)
const nextCanvasRef = ref(null)
const currentGame = ref('snake')
const gameState = ref('idle') // idle | playing | paused | over | won
const score = ref(0)
const highScore = ref(0)
const level = ref(1)
const linesCleared = ref(0)
const bricksLeft = ref(0)
const canvasTouchLocked = ref(false)

// Tab bar scroll
const tabBarWrapRef = ref(null)
const tabCanScrollLeft = ref(false)
const tabCanScrollRight = ref(true)
function onTabScroll() {
  const el = tabBarWrapRef.value
  if (!el) return
  tabCanScrollLeft.value = el.scrollLeft > 5
  tabCanScrollRight.value = el.scrollLeft < el.scrollWidth - el.clientWidth - 5
}
function scrollTabs(delta) {
  tabBarWrapRef.value?.scrollBy({ left: delta, behavior: 'smooth' })
}

// Canvas dimensions
const canvasWidth = 400
const canvasHeight = ref(400)

let animationId = null
let cachedCtx = null
let leaderboardTimer = null
let gridCanvas = null // offscreen canvas for static grids
let gridCanvasDirty = true
let canvasDPR = 1
let flappyGrad = null
let shooterGrad = null
let lastTime = 0

const gameIcon = computed(() => {
  const icons = { snake: '🐍', tetris: '🧱', game2048: '🔢', flappy: '🐤', breakout: '🧩', shooter: '🚀', chineseChess: '♟' }
  return icons[currentGame.value] || '🎮'
})

// ===== Snake =====
const SNAKE_GRID = 20
const SNAKE_CELL = canvasWidth / SNAKE_GRID
let snake = []
let food = null
let direction = 'right'
let nextDirection = 'right'
let snakeSpeed = 150
let snakeTimer = 0

function initSnake() {
  snake = [
    { x: 5, y: 10 },
    { x: 4, y: 10 },
    { x: 3, y: 10 }
  ]
  direction = 'right'
  nextDirection = 'right'
  snakeSpeed = 150
  snakeTimer = 0
  spawnFood()
}

function spawnFood() {
  const occupied = new Set(snake.map(s => `${s.x},${s.y}`))
  if (occupied.size >= SNAKE_GRID * SNAKE_GRID) {
    gameState.value = 'won'
    saveHighScore()
    return
  }
  let pos
  do {
    pos = {
      x: Math.floor(Math.random() * SNAKE_GRID),
      y: Math.floor(Math.random() * SNAKE_GRID)
    }
  } while (occupied.has(`${pos.x},${pos.y}`))
  food = pos
}

function updateSnake(dt) {
  snakeTimer += dt
  if (snakeTimer < snakeSpeed) return
  snakeTimer -= snakeSpeed

  direction = nextDirection
  const head = { ...snake[0] }
  if (direction === 'up') head.y--
  else if (direction === 'down') head.y++
  else if (direction === 'left') head.x--
  else if (direction === 'right') head.x++

  if (head.x < 0 || head.x >= SNAKE_GRID || head.y < 0 || head.y >= SNAKE_GRID) {
    gameState.value = 'over'
    saveHighScore()
    return
  }
  if (snake.some(s => s.x === head.x && s.y === head.y)) {
    gameState.value = 'over'
    saveHighScore()
    return
  }

  snake.unshift(head)
  if (head.x === food.x && head.y === food.y) {
    score.value += 10
    if (snakeSpeed > 60) snakeSpeed -= 2
    spawnFood()
  } else {
    snake.pop()
  }
}

function drawSnake(ctx) {
  ctx.fillStyle = '#f8fafc'
  ctx.fillRect(0, 0, canvasWidth, canvasHeight.value)

  if (gridCanvasDirty || !gridCanvas) buildGridCanvas(SNAKE_CELL, SNAKE_GRID, SNAKE_GRID)
  ctx.drawImage(gridCanvas, 0, 0, canvasWidth, canvasHeight.value)

  snake.forEach((seg, i) => {
    const padding = 1
    const radius = 4
    const x = seg.x * SNAKE_CELL + padding
    const y = seg.y * SNAKE_CELL + padding
    const w = SNAKE_CELL - padding * 2
    const h = SNAKE_CELL - padding * 2

    if (i === 0) {
      ctx.fillStyle = '#1e5eb6'
      ctx.beginPath(); ctx.roundRect(x, y, w, h, radius); ctx.fill()
      ctx.fillStyle = '#fff'
      const eyeSize = 3
      if (direction === 'right' || direction === 'left') {
        const ex = direction === 'right' ? x + w - 6 : x + 3
        ctx.beginPath(); ctx.arc(ex, y + 5, eyeSize, 0, Math.PI * 2); ctx.fill()
        ctx.beginPath(); ctx.arc(ex, y + h - 5, eyeSize, 0, Math.PI * 2); ctx.fill()
      } else {
        const ey = direction === 'down' ? y + h - 6 : y + 3
        ctx.beginPath(); ctx.arc(x + 5, ey, eyeSize, 0, Math.PI * 2); ctx.fill()
        ctx.beginPath(); ctx.arc(x + w - 5, ey, eyeSize, 0, Math.PI * 2); ctx.fill()
      }
    } else {
      const ratio = i / snake.length
      const r = Math.round(30 + ratio * 70)
      const g = Math.round(94 + ratio * 60)
      const b = Math.round(182 - ratio * 40)
      ctx.fillStyle = `rgb(${r},${g},${b})`
      ctx.beginPath(); ctx.roundRect(x, y, w, h, radius); ctx.fill()
    }
  })

  if (food) {
    const cx = food.x * SNAKE_CELL + SNAKE_CELL / 2
    const cy = food.y * SNAKE_CELL + SNAKE_CELL / 2
    ctx.fillStyle = '#ff4d4f'
    ctx.beginPath(); ctx.arc(cx, cy, SNAKE_CELL / 2 - 3, 0, Math.PI * 2); ctx.fill()
    ctx.fillStyle = 'rgba(255,255,255,0.4)'
    ctx.beginPath(); ctx.arc(cx - 3, cy - 3, 3, 0, Math.PI * 2); ctx.fill()
  }
}

function setDirection(dir) {
  const opposites = { up: 'down', down: 'up', left: 'right', right: 'left' }
  if (dir !== opposites[nextDirection]) {
    nextDirection = dir
  }
}

// ===== Tetris =====
const TETRIS_COLS = 10
const TETRIS_ROWS = 20
const TETRIS_CELL = 28

// Standard Tetris pieces with 4 rotation states (SRS)
const TETROMINOS = {
  I: { shapes: [[[0,0,0,0],[1,1,1,1],[0,0,0,0],[0,0,0,0]],[[0,0,1,0],[0,0,1,0],[0,0,1,0],[0,0,1,0]],[[0,0,0,0],[0,0,0,0],[1,1,1,1],[0,0,0,0]],[[0,1,0,0],[0,1,0,0],[0,1,0,0],[0,1,0,0]]], color: '#00d4ff' },
  O: { shapes: [[[1,1],[1,1]],[[1,1],[1,1]],[[1,1],[1,1]],[[1,1],[1,1]]], color: '#ffd700' },
  T: { shapes: [[[0,1,0],[1,1,1],[0,0,0]],[[0,1,0],[0,1,1],[0,1,0]],[[0,0,0],[1,1,1],[0,1,0]],[[0,1,0],[1,1,0],[0,1,0]]], color: '#a855f7' },
  S: { shapes: [[[0,1,1],[1,1,0],[0,0,0]],[[0,1,0],[0,1,1],[0,0,1]],[[0,0,0],[0,1,1],[1,1,0]],[[1,0,0],[1,1,0],[0,1,0]]], color: '#22c55e' },
  Z: { shapes: [[[1,1,0],[0,1,1],[0,0,0]],[[0,0,1],[0,1,1],[0,1,0]],[[0,0,0],[1,1,0],[0,1,1]],[[0,1,0],[1,1,0],[1,0,0]]], color: '#ef4444' },
  L: { shapes: [[[0,0,1],[1,1,1],[0,0,0]],[[0,1,0],[0,1,0],[0,1,1]],[[0,0,0],[1,1,1],[1,0,0]],[[1,1,0],[0,1,0],[0,1,0]]], color: '#f97316' },
  J: { shapes: [[[1,0,0],[1,1,1],[0,0,0]],[[0,1,1],[0,1,0],[0,1,0]],[[0,0,0],[1,1,1],[0,0,1]],[[0,1,0],[0,1,0],[1,1,0]]], color: '#3b82f6' }
}
const PIECE_NAMES = Object.keys(TETROMINOS)

// SRS wall kick data
const WALL_KICKS_JLSTZ = {
  '0>1': [[0,0],[-1,0],[-1,1],[0,-2],[-1,-2]],
  '1>0': [[0,0],[1,0],[1,-1],[0,2],[1,2]],
  '1>2': [[0,0],[1,0],[1,-1],[0,2],[1,2]],
  '2>1': [[0,0],[-1,0],[-1,1],[0,-2],[-1,-2]],
  '2>3': [[0,0],[1,0],[1,1],[0,-2],[1,-2]],
  '3>2': [[0,0],[-1,0],[-1,-1],[0,2],[-1,2]],
  '3>0': [[0,0],[-1,0],[-1,-1],[0,2],[-1,2]],
  '0>3': [[0,0],[1,0],[1,1],[0,-2],[1,-2]]
}
const WALL_KICKS_I = {
  '0>1': [[0,0],[-2,0],[1,0],[-2,-1],[1,2]],
  '1>0': [[0,0],[2,0],[-1,0],[2,1],[-1,-2]],
  '1>2': [[0,0],[-1,0],[2,0],[-1,2],[2,-1]],
  '2>1': [[0,0],[1,0],[-2,0],[1,-2],[-2,1]],
  '2>3': [[0,0],[2,0],[-1,0],[2,1],[-1,-2]],
  '3>2': [[0,0],[-2,0],[1,0],[-2,-1],[1,2]],
  '3>0': [[0,0],[1,0],[-2,0],[1,-2],[-2,1]],
  '0>3': [[0,0],[-1,0],[2,0],[-1,2],[2,-1]]
}

let board = []
let currentPiece = null
let nextPiece = null
let tetrisTimer = 0
let tetrisSpeed = 500
let softDrop = false
let lockDelay = 0
let lockMoves = 0
const LOCK_DELAY = 500
const MAX_LOCK_MOVES = 15
const SOFT_DROP_SPEED = 50

function initTetris() {
  board = Array.from({ length: TETRIS_ROWS }, () => Array(TETRIS_COLS).fill(null))
  currentPiece = null
  nextPiece = randomTetrisPiece()
  tetrisTimer = 0
  tetrisSpeed = 500
  softDrop = false
  lockDelay = 0
  lockMoves = 0
  linesCleared.value = 0
  level.value = 1
  spawnTetrisPiece()
}

function randomTetrisPiece() {
  const name = PIECE_NAMES[Math.floor(Math.random() * PIECE_NAMES.length)]
  const t = TETROMINOS[name]
  const shape = t.shapes[0].map(r => [...r])
  return {
    type: name,
    shape,
    rotation: 0,
    color: t.color,
    x: Math.floor((TETRIS_COLS - shape[0].length) / 2),
    y: 0
  }
}

function spawnTetrisPiece() {
  currentPiece = nextPiece
  nextPiece = randomTetrisPiece()
  lockDelay = 0
  lockMoves = 0
  if (!isValidTetris(currentPiece.shape, currentPiece.x, currentPiece.y)) {
    gameState.value = 'over'
    saveHighScore()
  }
  drawNextPiece()
}

function isValidTetris(shape, px, py) {
  for (let r = 0; r < shape.length; r++) {
    for (let c = 0; c < shape[r].length; c++) {
      if (!shape[r][c]) continue
      const nx = px + c
      const ny = py + r
      if (nx < 0 || nx >= TETRIS_COLS || ny >= TETRIS_ROWS) return false
      if (ny >= 0 && board[ny][nx]) return false
    }
  }
  return true
}

function rotateTetrisPiece() {
  if (!currentPiece) return
  const name = currentPiece.type
  if (name === 'O') return // O doesn't rotate
  const oldRot = currentPiece.rotation
  const newRot = (oldRot + 1) % 4
  const rotated = TETROMINOS[name].shapes[newRot].map(r => [...r])
  const kickKey = `${oldRot}>${newRot}`
  const kicks = name === 'I' ? WALL_KICKS_I[kickKey] : WALL_KICKS_JLSTZ[kickKey]
  for (const [kx, ky] of kicks) {
    if (isValidTetris(rotated, currentPiece.x + kx, currentPiece.y - ky)) {
      currentPiece.shape = rotated
      currentPiece.rotation = newRot
      currentPiece.x += kx
      currentPiece.y -= ky
      if (lockDelay > 0 && lockMoves < MAX_LOCK_MOVES) {
        lockDelay = LOCK_DELAY
        lockMoves++
      }
      return
    }
  }
}

function lockTetrisPiece() {
  const { shape, x, y, color } = currentPiece
  for (let r = 0; r < shape.length; r++) {
    for (let c = 0; c < shape[r].length; c++) {
      if (!shape[r][c]) continue
      const ny = y + r
      if (ny >= 0 && ny < TETRIS_ROWS) {
        board[ny][x + c] = color
      }
    }
  }
  clearTetrisLines()
  spawnTetrisPiece()
}

function clearTetrisLines() {
  let cleared = 0
  for (let r = TETRIS_ROWS - 1; r >= 0; r--) {
    if (board[r].every(cell => cell !== null)) {
      board.splice(r, 1)
      board.unshift(Array(TETRIS_COLS).fill(null))
      cleared++
      r++
    }
  }
  if (cleared > 0) {
    const points = [0, 100, 300, 500, 800]
    score.value += points[cleared] || cleared * 200
    linesCleared.value += cleared
    level.value = Math.floor(linesCleared.value / 10) + 1
    tetrisSpeed = Math.max(80, 500 - (level.value - 1) * 40)
  }
}

function updateTetris(dt) {
  if (!currentPiece) return
  const speed = softDrop ? SOFT_DROP_SPEED : tetrisSpeed
  tetrisTimer += dt
  if (tetrisTimer < speed) return
  tetrisTimer -= speed
  if (isValidTetris(currentPiece.shape, currentPiece.x, currentPiece.y + 1)) {
    currentPiece.y++
    lockDelay = 0
  } else {
    if (lockDelay === 0) {
      lockDelay = LOCK_DELAY
      lockMoves = 0
    } else {
      lockDelay -= speed
      if (lockDelay <= 0) {
        lockTetrisPiece()
      }
    }
  }
}

function tetrisAction(action) {
  if (gameState.value !== 'playing' || !currentPiece) return
  if (action === 'left') {
    if (isValidTetris(currentPiece.shape, currentPiece.x - 1, currentPiece.y)) {
      currentPiece.x--
      if (lockDelay > 0 && lockMoves < MAX_LOCK_MOVES) { lockDelay = LOCK_DELAY; lockMoves++ }
    }
  } else if (action === 'right') {
    if (isValidTetris(currentPiece.shape, currentPiece.x + 1, currentPiece.y)) {
      currentPiece.x++
      if (lockDelay > 0 && lockMoves < MAX_LOCK_MOVES) { lockDelay = LOCK_DELAY; lockMoves++ }
    }
  } else if (action === 'down') {
    softDrop = true
  } else if (action === 'rotate') {
    rotateTetrisPiece()
  } else if (action === 'drop') {
    while (isValidTetris(currentPiece.shape, currentPiece.x, currentPiece.y + 1)) {
      currentPiece.y++
      score.value += 2
    }
    lockTetrisPiece()
  }
}

function tetrisActionRelease(action) {
  if (action === 'down') softDrop = false
}

function drawTetris(ctx) {
  const tetrisOffsetX = Math.floor((canvasWidth - TETRIS_COLS * TETRIS_CELL) / 2)
  ctx.fillStyle = '#f8fafc'
  ctx.fillRect(0, 0, canvasWidth, canvasHeight.value)

  if (gridCanvasDirty || !gridCanvas) buildGridCanvas(TETRIS_CELL, TETRIS_COLS, TETRIS_ROWS)
  ctx.drawImage(gridCanvas, tetrisOffsetX, 0, TETRIS_COLS * TETRIS_CELL, TETRIS_ROWS * TETRIS_CELL)
  for (let r = 0; r < TETRIS_ROWS; r++) {
    for (let c = 0; c < TETRIS_COLS; c++) {
      if (board[r][c]) drawCell(ctx, c, r, board[r][c], tetrisOffsetX)
    }
  }
  if (currentPiece) {
    let ghostY = currentPiece.y
    while (isValidTetris(currentPiece.shape, currentPiece.x, ghostY + 1)) ghostY++
    if (ghostY !== currentPiece.y) {
      ctx.globalAlpha = 0.2
      for (let r = 0; r < currentPiece.shape.length; r++) {
        for (let c = 0; c < currentPiece.shape[r].length; c++) {
          if (currentPiece.shape[r][c]) drawCell(ctx, currentPiece.x + c, ghostY + r, currentPiece.color, tetrisOffsetX)
        }
      }
      ctx.globalAlpha = 1
    }
    for (let r = 0; r < currentPiece.shape.length; r++) {
      for (let c = 0; c < currentPiece.shape[r].length; c++) {
        if (currentPiece.shape[r][c]) drawCell(ctx, currentPiece.x + c, currentPiece.y + r, currentPiece.color, tetrisOffsetX)
      }
    }
  }
}

function drawCell(ctx, x, y, color, offsetX = 0) {
  const padding = 1
  const px = x * TETRIS_CELL + padding + offsetX
  const py = y * TETRIS_CELL + padding
  const size = TETRIS_CELL - padding * 2
  ctx.fillStyle = color
  ctx.beginPath(); ctx.roundRect(px, py, size, size, 3); ctx.fill()
  ctx.fillStyle = 'rgba(255,255,255,0.3)'
  ctx.beginPath(); ctx.roundRect(px, py, size, size / 3, [3, 3, 0, 0]); ctx.fill()
}

function drawNextPiece() {
  if (!nextCanvasRef.value || !nextPiece) return
  const dpr = Math.min(window.devicePixelRatio || 1, 3)
  const nextCanvas = nextCanvasRef.value
  nextCanvas.width = 100 * dpr
  nextCanvas.height = 100 * dpr
  const ctx = nextCanvas.getContext('2d')
  ctx.scale(dpr, dpr)
  ctx.fillStyle = '#f8fafc'
  ctx.fillRect(0, 0, 100, 100)
  const cellSize = 20
  const offsetX = (100 - nextPiece.shape[0].length * cellSize) / 2
  const offsetY = (100 - nextPiece.shape.length * cellSize) / 2
  for (let r = 0; r < nextPiece.shape.length; r++) {
    for (let c = 0; c < nextPiece.shape[r].length; c++) {
      if (nextPiece.shape[r][c]) {
        const px = offsetX + c * cellSize + 1
        const py = offsetY + r * cellSize + 1
        ctx.fillStyle = nextPiece.color
        ctx.beginPath(); ctx.roundRect(px, py, cellSize - 2, cellSize - 2, 3); ctx.fill()
      }
    }
  }
}

// ===== 2048 =====
const GRID_2048 = 4
const CELL_2048 = canvasWidth / GRID_2048
let grid2048 = []
let tileColors2048 = {
  0: '#cdc1b4', 2: '#eee4da', 4: '#ede0c8', 8: '#f2b179',
  16: '#f59563', 32: '#f67c5f', 64: '#f65e3b', 128: '#edcf72',
  256: '#edcc61', 512: '#edc850', 1024: '#edc53f', 2048: '#edc22e'
}

function init2048() {
  grid2048 = Array.from({ length: GRID_2048 }, () => Array(GRID_2048).fill(0))
  addRandomTile2048()
  addRandomTile2048()
  // Draw initial state immediately
  const ctx = canvasRef.value?.getContext('2d')
  if (ctx) draw2048(ctx)
}

function addRandomTile2048() {
  const empty = []
  for (let r = 0; r < GRID_2048; r++) {
    for (let c = 0; c < GRID_2048; c++) {
      if (grid2048[r][c] === 0) empty.push({ r, c })
    }
  }
  if (empty.length === 0) return
  const { r, c } = empty[Math.floor(Math.random() * empty.length)]
  grid2048[r][c] = Math.random() < 0.9 ? 2 : 4
}

function move2048(dir) {
  // Auto-start on first move without resetting the grid
  if (gameState.value !== 'playing') {
    if (gameState.value === 'idle' || gameState.value === 'over' || gameState.value === 'won') {
      if (gameState.value !== 'idle') {
        // Game was over/won, need to re-init
        score.value = 0
        init2048()
      }
      gameState.value = 'playing'
      if (!animationId) animationId = requestAnimationFrame(gameLoop)
    } else {
      return // paused or other states
    }
  }
  let moved = false
  const oldGrid = grid2048.map(r => [...r])

  if (dir === 'left') {
    for (let r = 0; r < GRID_2048; r++) {
      const row = grid2048[r].filter(v => v !== 0)
      for (let i = 0; i < row.length - 1; i++) {
        if (row[i] === row[i + 1]) { row[i] *= 2; score.value += row[i]; row.splice(i + 1, 1) }
      }
      while (row.length < GRID_2048) row.push(0)
      grid2048[r] = row
    }
  } else if (dir === 'right') {
    for (let r = 0; r < GRID_2048; r++) {
      const row = grid2048[r].filter(v => v !== 0)
      for (let i = row.length - 1; i > 0; i--) {
        if (row[i] === row[i - 1]) { row[i] *= 2; score.value += row[i]; row.splice(i - 1, 1) }
      }
      while (row.length < GRID_2048) row.unshift(0)
      grid2048[r] = row
    }
  } else if (dir === 'up') {
    for (let c = 0; c < GRID_2048; c++) {
      const col = []
      for (let r = 0; r < GRID_2048; r++) col.push(grid2048[r][c])
      const filtered = col.filter(v => v !== 0)
      for (let i = 0; i < filtered.length - 1; i++) {
        if (filtered[i] === filtered[i + 1]) { filtered[i] *= 2; score.value += filtered[i]; filtered.splice(i + 1, 1) }
      }
      while (filtered.length < GRID_2048) filtered.push(0)
      for (let r = 0; r < GRID_2048; r++) grid2048[r][c] = filtered[r]
    }
  } else if (dir === 'down') {
    for (let c = 0; c < GRID_2048; c++) {
      const col = []
      for (let r = 0; r < GRID_2048; r++) col.push(grid2048[r][c])
      const filtered = col.filter(v => v !== 0)
      for (let i = filtered.length - 1; i > 0; i--) {
        if (filtered[i] === filtered[i - 1]) { filtered[i] *= 2; score.value += filtered[i]; filtered.splice(i - 1, 1) }
      }
      while (filtered.length < GRID_2048) filtered.unshift(0)
      for (let r = 0; r < GRID_2048; r++) grid2048[r][c] = filtered[r]
    }
  }

  // Check if grid changed
  for (let r = 0; r < GRID_2048; r++) {
    for (let c = 0; c < GRID_2048; c++) {
      if (grid2048[r][c] !== oldGrid[r][c]) { moved = true; break }
    }
    if (moved) break
  }

  if (moved) {
    addRandomTile2048()
    // Check win
    for (let r = 0; r < GRID_2048; r++) {
      for (let c = 0; c < GRID_2048; c++) {
        if (grid2048[r][c] === 2048) {
          gameState.value = 'won'
          saveHighScore()
          return
        }
      }
    }
    // Check game over
    if (check2048GameOver()) {
      gameState.value = 'over'
      saveHighScore()
    }
  }
}

function check2048GameOver() {
  for (let r = 0; r < GRID_2048; r++) {
    for (let c = 0; c < GRID_2048; c++) {
      if (grid2048[r][c] === 0) return false
      if (c < GRID_2048 - 1 && grid2048[r][c] === grid2048[r][c + 1]) return false
      if (r < GRID_2048 - 1 && grid2048[r][c] === grid2048[r + 1][c]) return false
    }
  }
  return true
}

function draw2048(ctx) {
  ctx.fillStyle = '#bbada0'
  ctx.fillRect(0, 0, canvasWidth, canvasHeight.value)

  const gap = 8
  const radius = 6
  for (let r = 0; r < GRID_2048; r++) {
    for (let c = 0; c < GRID_2048; c++) {
      const val = grid2048[r][c]
      const x = c * CELL_2048 + gap / 2
      const y = r * CELL_2048 + gap / 2
      const w = CELL_2048 - gap
      const h = CELL_2048 - gap

      ctx.fillStyle = tileColors2048[val] || '#3c3a32'
      ctx.beginPath(); ctx.roundRect(x, y, w, h, radius); ctx.fill()

      if (val > 0) {
        ctx.fillStyle = val <= 4 ? '#776e65' : '#f9f6f2'
        ctx.font = val >= 100 ? (val >= 1000 ? 'bold 28px sans-serif' : 'bold 32px sans-serif') : 'bold 40px sans-serif'
        ctx.textAlign = 'center'
        ctx.textBaseline = 'middle'
        ctx.fillText(val.toString(), x + w / 2, y + h / 2)
      }
    }
  }
}

// ===== Chinese Chess =====
const CHESS_COLS = 9, CHESS_ROWS = 10
const CHESS_CELL = 42
const CHESS_OX = 30, CHESS_OY = 30

const CHESS_PIECE_NAMES = {
  K: '帅', A: '仕', B: '相', N: '马', R: '车', C: '炮', P: '兵',
  k: '将', a: '士', b: '象', n: '马', r: '车', c: '炮', p: '卒'
}

const PIECE_VALUES = { K: 10000, R: 600, C: 285, N: 270, A: 120, B: 120, P: 30, k: 10000, r: 600, c: 285, n: 270, a: 120, b: 120, p: 30 }
const PIECE_VAL_CAPTURE = { K: 10000, R: 600, C: 285, N: 270, A: 120, B: 120, P: 30, k: 10000, r: 600, c: 285, n: 270, a: 120, b: 120, p: 30 }

// Transposition table for caching evaluated positions
const CHESS_TT_SIZE = 1 << 16
let chessTT = new Map()
function ttClear() { chessTT.clear() }
function ttKey(board, depth, isMax) {
  let h = depth * 31 + (isMax ? 1 : 0)
  for (let r = 0; r < 10; r++)
    for (let c = 0; c < 9; c++) {
      const p = board[r][c]
      h = (h * 31 + (p ? p.charCodeAt(0) : 0)) & 0x7FFFFFFF
    }
  return h % CHESS_TT_SIZE
}

const CHESS_DIFF_MULT = { 1: 1, 2: 2, 3: 3, 4: 5 }
const CHESS_DIFF_DEPTH = { 1: 2, 2: 3, 3: 3, 4: 4 }
const CHESS_DIFF_TIME = { 1: 500, 2: 1000, 3: 2000, 4: 4000 }

// Position value tables for each piece type (red perspective, 9 cols x 10 rows)
const POS_K = [
  [0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0],[0,0,0,1,1,1,0,0,0],[0,0,0,2,2,2,0,0,0],[0,0,0,11,15,11,0,0,0]
]
const POS_A = [
  [0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0],[0,0,0,20,0,20,0,0,0],[0,0,0,0,23,0,0,0,0],[0,0,0,20,0,20,0,0,0]
]
const POS_B = [
  [0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],
  [0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,20,0,0,0,20,0,0],[0,0,0,0,0,0,0,0,0],
  [0,0,0,0,23,0,0,0,0],[0,0,0,0,0,0,0,0,0],[18,0,0,0,0,0,0,0,18]
]
const POS_N = [
  [90,90,90,96,90,96,90,90,90],[90,96,103,97,94,97,103,96,90],[92,98,99,103,99,103,99,98,92],
  [93,108,100,107,100,107,100,108,93],[90,100,99,103,104,103,99,100,90],[90,98,101,102,103,102,101,98,90],
  [92,94,98,95,98,95,98,94,92],[93,92,94,95,92,95,94,92,93],[85,90,92,93,78,93,92,90,85],[88,85,90,88,90,88,90,85,88]
]
const POS_R = [
  [206,208,207,213,214,213,207,208,206],[206,212,209,216,233,216,209,212,206],[206,208,207,214,216,214,207,208,206],
  [206,213,213,216,216,216,213,213,206],[208,211,211,214,215,214,211,211,208],[208,212,212,214,215,214,212,212,208],
  [204,209,204,212,214,212,204,209,204],[198,208,204,212,212,212,204,208,198],[200,208,206,212,200,212,206,208,200],[194,206,204,212,200,212,204,206,194]
]
const POS_C = [
  [100,100,96,91,90,91,96,100,100],[98,98,96,92,89,92,96,98,98],[97,97,96,91,92,91,96,97,97],
  [96,99,99,98,100,98,99,99,96],[96,96,96,96,100,96,96,96,96],[95,96,99,96,100,96,99,96,95],
  [96,96,96,96,96,96,96,96,96],[97,96,100,99,101,99,100,96,97],[96,97,98,98,98,98,98,97,96],[97,96,99,99,98,99,99,96,97]
]
const POS_P_RED = [
  [19,24,34,42,44,42,34,24,19],[19,24,32,37,37,37,32,24,19],[19,23,27,29,30,29,27,23,19],
  [18,22,26,27,27,27,26,22,18],[14,18,20,27,29,27,20,18,14],[10,14,16,20,24,20,16,14,10],
  [9,9,9,11,13,11,9,9,9],[4,8,8,12,18,12,8,8,4],[0,0,2,6,6,6,2,0,0],[0,0,0,0,0,0,0,0,0]
]
const POS_P_BLACK = [
  [19,24,34,42,44,42,34,24,19],[19,24,32,37,37,37,32,24,19],[19,23,27,29,30,29,27,23,19],
  [18,22,26,27,27,27,26,22,18],[14,18,20,27,29,27,20,18,14],[10,14,16,20,24,20,16,14,10],
  [9,9,9,11,13,11,9,9,9],[4,8,8,12,18,12,8,8,4],[0,0,2,6,6,6,2,0,0],[0,0,0,0,0,0,0,0,0]
]

function getPosValue(piece, col, row) {
  const r = row, c = col
  switch (piece) {
    case 'K': return POS_K[r][c] || 0
    case 'A': return POS_A[r][c] || 0
    case 'B': return POS_B[r][c] || 0
    case 'N': return POS_N[r][c] || 0
    case 'R': return POS_R[r][c] || 0
    case 'C': return POS_C[r][c] || 0
    case 'P': return POS_P_RED[r][c] || 0
    case 'k': return POS_K[9 - r][8 - c] || 0
    case 'a': return POS_A[9 - r][8 - c] || 0
    case 'b': return POS_B[9 - r][8 - c] || 0
    case 'n': return POS_N[9 - r][8 - c] || 0
    case 'r': return POS_R[9 - r][8 - c] || 0
    case 'c': return POS_C[9 - r][8 - c] || 0
    case 'p': return POS_P_BLACK[r][c] || 0
    default: return 0
  }
}

function isRed(p) { return p && p >= 'A' && p <= 'Z' }
function isBlack(p) { return p && p >= 'a' && p <= 'z' }
function pieceSide(p) { return isRed(p) ? 'red' : isBlack(p) ? 'black' : null }

function cloneBoard(b) { return b.map(row => [...row]) }

function initChineseChess() {
  chessBoard = [
    ['r','n','b','a','k','a','b','n','r'],
    [null,null,null,null,null,null,null,null,null],
    [null,'c',null,null,null,null,null,'c',null],
    ['p',null,'p',null,'p',null,'p',null,'p'],
    [null,null,null,null,null,null,null,null,null],
    [null,null,null,null,null,null,null,null,null],
    ['P',null,'P',null,'P',null,'P',null,'P'],
    [null,'C',null,null,null,null,null,'C',null],
    [null,null,null,null,null,null,null,null,null],
    ['R','N','B','A','K','A','B','N','R']
  ]
  chessTurn = 'red'
  chessSelected = null
  chessHistory = []
  chessThinking = false
  chessLastMove = null
  chessLegalMoves = []
  chessGameResult = null
  chessPlayerPieces.value = 16
  chessElapsedMin.value = 0
}

function getKingPos(board, side) {
  const k = side === 'red' ? 'K' : 'k'
  for (let r = 0; r < 10; r++)
    for (let c = 0; c < 9; c++)
      if (board[r][c] === k) return { col: c, row: r }
  return null
}

function isKingFacing(board) {
  const rk = getKingPos(board, 'red')
  const bk = getKingPos(board, 'black')
  if (!rk || !bk || rk.col !== bk.col) return false
  const minR = Math.min(rk.row, bk.row)
  const maxR = Math.max(rk.row, bk.row)
  for (let r = minR + 1; r < maxR; r++)
    if (board[r][rk.col]) return false
  return true
}

function inPalace(col, row, side) {
  if (col < 3 || col > 5) return false
  return side === 'red' ? (row >= 7 && row <= 9) : (row >= 0 && row <= 2)
}

function onBoard(col, row) { return col >= 0 && col < 9 && row >= 0 && row < 10 }

function getRawMoves(board, col, row) {
  const piece = board[row][col]
  if (!piece) return []
  const side = pieceSide(piece)
  const moves = []
  const type = piece.toLowerCase()

  if (type === 'k') {
    const dirs = [[0,1],[0,-1],[1,0],[-1,0]]
    for (const [dc, dr] of dirs) {
      const nc = col + dc, nr = row + dr
      if (onBoard(nc, nr) && inPalace(nc, nr, side)) {
        const target = board[nr][nc]
        if (!target || pieceSide(target) !== side) moves.push({ col: nc, row: nr })
      }
    }
  } else if (type === 'a') {
    const dirs = [[1,1],[1,-1],[-1,1],[-1,-1]]
    for (const [dc, dr] of dirs) {
      const nc = col + dc, nr = row + dr
      if (onBoard(nc, nr) && inPalace(nc, nr, side)) {
        const target = board[nr][nc]
        if (!target || pieceSide(target) !== side) moves.push({ col: nc, row: nr })
      }
    }
  } else if (type === 'b') {
    const dirs = [[2,2],[2,-2],[-2,2],[-2,-2]]
    for (const [dc, dr] of dirs) {
      const nc = col + dc, nr = row + dr
      if (!onBoard(nc, nr)) continue
      if (side === 'red' && nr < 5) continue  // Red elephant stays in rows 5-9 (bottom)
      if (side === 'black' && nr > 4) continue  // Black elephant stays in rows 0-4 (top)
      const blockC = col + dc / 2, blockR = row + dr / 2
      if (board[blockR][blockC]) continue
      const target = board[nr][nc]
      if (!target || pieceSide(target) !== side) moves.push({ col: nc, row: nr })
    }
  } else if (type === 'n') {
    const jumps = [[1,2],[1,-2],[-1,2],[-1,-2],[2,1],[2,-1],[-2,1],[-2,-1]]
    const legBlocks = [[0,1],[0,-1],[0,1],[0,-1],[1,0],[1,0],[-1,0],[-1,0]]
    for (let i = 0; i < 8; i++) {
      const nc = col + jumps[i][0], nr = row + jumps[i][1]
      if (!onBoard(nc, nr)) continue
      const lc = col + legBlocks[i][0], lr = row + legBlocks[i][1]
      if (board[lr][lc]) continue
      const target = board[nr][nc]
      if (!target || pieceSide(target) !== side) moves.push({ col: nc, row: nr })
    }
  } else if (type === 'r') {
    const dirs = [[0,1],[0,-1],[1,0],[-1,0]]
    for (const [dc, dr] of dirs) {
      let nc = col + dc, nr = row + dr
      while (onBoard(nc, nr)) {
        const target = board[nr][nc]
        if (target) {
          if (pieceSide(target) !== side) moves.push({ col: nc, row: nr })
          break
        }
        moves.push({ col: nc, row: nr })
        nc += dc; nr += dr
      }
    }
  } else if (type === 'c') {
    const dirs = [[0,1],[0,-1],[1,0],[-1,0]]
    for (const [dc, dr] of dirs) {
      let nc = col + dc, nr = row + dr
      let jumped = false
      while (onBoard(nc, nr)) {
        const target = board[nr][nc]
        if (!jumped) {
          if (target) jumped = true
          else moves.push({ col: nc, row: nr })
        } else {
          if (target) {
            if (pieceSide(target) !== side) moves.push({ col: nc, row: nr })
            break
          }
        }
        nc += dc; nr += dr
      }
    }
  } else if (type === 'p') {
    if (side === 'red') {
      // Red at bottom, moves up (row-1). River at row 4/5. Before crossing: row >= 5
      if (row >= 5) {
        if (onBoard(col, row - 1)) {
          const t = board[row - 1][col]
          if (!t || pieceSide(t) !== side) moves.push({ col, row: row - 1 })
        }
      } else {
        for (const [dc, dr] of [[0,-1],[1,0],[-1,0]]) {
          const nc = col + dc, nr = row + dr
          if (onBoard(nc, nr)) {
            const t = board[nr][nc]
            if (!t || pieceSide(t) !== side) moves.push({ col: nc, row: nr })
          }
        }
      }
    } else {
      // Black at top, moves down (row+1). River at row 4/5. Before crossing: row <= 4
      if (row <= 4) {
        if (onBoard(col, row + 1)) {
          const t = board[row + 1][col]
          if (!t || pieceSide(t) !== side) moves.push({ col, row: row + 1 })
        }
      } else {
        for (const [dc, dr] of [[0,1],[1,0],[-1,0]]) {
          const nc = col + dc, nr = row + dr
          if (onBoard(nc, nr)) {
            const t = board[nr][nc]
            if (!t || pieceSide(t) !== side) moves.push({ col: nc, row: nr })
          }
        }
      }
    }
  }
  return moves
}

function isInCheck(board, side) {
  const kp = getKingPos(board, side)
  if (!kp) return true
  const opp = side === 'red' ? 'black' : 'red'
  for (let r = 0; r < 10; r++)
    for (let c = 0; c < 9; c++)
      if (board[r][c] && pieceSide(board[r][c]) === opp)
        for (const m of getRawMoves(board, c, r))
          if (m.col === kp.col && m.row === kp.row) return true
  return false
}

function makeMoveOnBoard(board, from, to) {
  const newBoard = cloneBoard(board)
  newBoard[to.row][to.col] = newBoard[from.row][from.col]
  newBoard[from.row][from.col] = null
  return newBoard
}

function getLegalMoves(board, col, row) {
  const piece = board[row][col]
  if (!piece) return []
  const side = pieceSide(piece)
  const raw = getRawMoves(board, col, row)
  const legal = []
  for (const m of raw) {
    const nb = makeMoveOnBoard(board, { col, row }, m)
    if (!isInCheck(nb, side) && !isKingFacing(nb)) legal.push(m)
  }
  return legal
}

function hasAnyLegalMove(board, side) {
  for (let r = 0; r < 10; r++)
    for (let c = 0; c < 9; c++)
      if (board[r][c] && pieceSide(board[r][c]) === side)
        if (getLegalMoves(board, c, r).length > 0) return true
  return false
}

function evaluateBoard(board) {
  let score = 0
  for (let r = 0; r < 10; r++) {
    for (let c = 0; c < 9; c++) {
      const p = board[r][c]
      if (!p) continue
      const base = PIECE_VALUES[p] || 0
      const pos = getPosValue(p, c, r)
      const val = base + pos
      score += isRed(p) ? val : -val
    }
  }
  return score
}

// Collect all moves for a side, sorted with captures first (MVV-LVA)
function collectMovesOrdered(board, side) {
  const captures = [], quiets = []
  for (let r = 0; r < 10; r++) {
    for (let c = 0; c < 9; c++) {
      if (!board[r][c] || pieceSide(board[r][c]) !== side) continue
      const moves = getLegalMoves(board, c, r)
      for (const m of moves) {
        const victim = board[m.row][m.col]
        if (victim) {
          captures.push({ from: { col: c, row: r }, to: m, score: (PIECE_VAL_CAPTURE[victim] || 0) - (PIECE_VAL_CAPTURE[board[r][c]] || 0) / 100 })
        } else {
          quiets.push({ from: { col: c, row: r }, to: m, score: 0 })
        }
      }
    }
  }
  captures.sort((a, b) => b.score - a.score)
  return captures.concat(quiets)
}

let chessSearchDeadline = 0
let chessTimeOut = false

function minimax(board, depth, alpha, beta, isMaximizing) {
  if (Date.now() > chessSearchDeadline) { chessTimeOut = true; return evaluateBoard(board) }
  if (depth === 0) return evaluateBoard(board)
  const side = isMaximizing ? 'red' : 'black'

  const ttKeyVal = ttKey(board, depth, isMaximizing)
  const ttEntry = chessTT.get(ttKeyVal)
  if (ttEntry && ttEntry.depth >= depth) {
    if (ttEntry.flag === 0) return ttEntry.score
    if (ttEntry.flag === 1 && ttEntry.score >= beta) return ttEntry.score
    if (ttEntry.flag === -1 && ttEntry.score <= alpha) return ttEntry.score
  }

  const allMoves = collectMovesOrdered(board, side)
  if (allMoves.length === 0) {
    return isInCheck(board, side) ? (isMaximizing ? -99999 : 99999) : 0
  }
  let best = isMaximizing ? -Infinity : Infinity
  for (const mv of allMoves) {
    if (chessTimeOut) return best
    const nb = makeMoveOnBoard(board, mv.from, mv.to)
    const val = minimax(nb, depth - 1, alpha, beta, !isMaximizing)
    if (isMaximizing) {
      if (val > best) best = val
      if (val > alpha) alpha = val
    } else {
      if (val < best) best = val
      if (val < beta) beta = val
    }
    if (beta <= alpha) break
  }

  if (!chessTimeOut) {
    const flag = best >= beta ? 1 : best <= alpha ? -1 : 0
    chessTT.set(ttKeyVal, { score: best, depth, flag })
  }
  return best
}

function aiMove() {
  chessThinking = true
  const maxDepth = CHESS_DIFF_DEPTH[chessDifficulty.value] || 3
  const timeBudget = CHESS_DIFF_TIME[chessDifficulty.value] || 1500
  setTimeout(() => {
    ttClear()
    chessSearchDeadline = Date.now() + timeBudget
    chessTimeOut = false

    let bestMove = null
    let bestScore = Infinity

    // Iterative deepening: search depth 1, 2, ... up to maxDepth or time limit
    for (let d = 1; d <= maxDepth; d++) {
      const allMoves = collectMovesOrdered(chessBoard, 'black')
      if (allMoves.length === 0) break

      let currentBest = null
      let currentScore = Infinity

      for (const mv of allMoves) {
        if (Date.now() > chessSearchDeadline) break
        const nb = makeMoveOnBoard(chessBoard, mv.from, mv.to)
        const val = minimax(nb, d - 1, -Infinity, Infinity, true)
        if (val < currentScore) {
          currentScore = val
          currentBest = mv
        }
      }
      if (!chessTimeOut && currentBest) {
        bestMove = currentBest
        bestScore = currentScore
      }
      if (chessTimeOut) break
    }

    if (bestMove) {
      const captured = chessBoard[bestMove.to.row][bestMove.to.col]
      chessHistory.push({ from: { ...bestMove.from }, to: { ...bestMove.to }, captured, board: cloneBoard(chessBoard) })
      chessBoard[bestMove.to.row][bestMove.to.col] = chessBoard[bestMove.from.row][bestMove.from.col]
      chessBoard[bestMove.from.row][bestMove.from.col] = null
      chessLastMove = { from: bestMove.from, to: bestMove.to }
      if (captured === 'K') {
        chessGameResult = 'lose'
        endChineseChessGame()
      } else {
        chessTurn = 'red'
        if (!hasAnyLegalMove(chessBoard, 'red')) {
          if (isInCheck(chessBoard, 'red')) {
            chessGameResult = 'lose'
          } else {
            chessGameResult = 'draw'
          }
          endChineseChessGame()
        }
      }
    }
    chessThinking = false
  }, 50)
}

function endChineseChessGame() {
  const elapsed = (Date.now() - (chessStartTime || Date.now())) / 60000
  chessElapsedMin.value = Math.round(elapsed)
  let playerPieces = 0
  for (let r = 0; r < 10; r++)
    for (let c = 0; c < 9; c++)
      if (chessBoard[r][c] && isRed(chessBoard[r][c])) playerPieces++
  chessPlayerPieces.value = playerPieces
  score.value = calculateChessScore(chessGameResult, chessDifficulty.value, playerPieces, elapsed)
  gameState.value = 'over'
  saveHighScore()
}

let chessStartTime = 0

function calculateChessScore(result, diff, pieces, elapsedMin) {
  if (result === 'win') {
    const base = 1000
    const mult = CHESS_DIFF_MULT[diff] || 1
    const pieceBonus = 1.0 + (pieces - 1) * 0.05
    const timeBonus = Math.max(0.5, 1.5 - elapsedMin * 0.025)
    return Math.round(base * mult * Math.min(pieceBonus, 1.5) * timeBonus)
  } else if (result === 'draw') {
    return 300
  } else {
    return 100
  }
}

function handleChessClick(col, row) {
  if (chessThinking || chessGameResult) return
  if (chessTurn !== 'red') return
  const clicked = chessBoard[row][col]
  if (chessSelected) {
    if (clicked && isRed(clicked)) {
      chessSelected = { col, row }
      chessLegalMoves = getLegalMoves(chessBoard, col, row)
      return
    }
    const isLegal = chessLegalMoves.some(m => m.col === col && m.row === row)
    if (isLegal) {
      const from = { ...chessSelected }
      const to = { col, row }
      const captured = chessBoard[to.row][to.col]
      chessHistory.push({ from, to, captured, board: cloneBoard(chessBoard) })
      chessBoard[to.row][to.col] = chessBoard[from.row][from.col]
      chessBoard[from.row][from.col] = null
      chessLastMove = { from, to }
      chessSelected = null
      chessLegalMoves = []
      if (captured === 'k') {
        chessGameResult = 'win'
        endChineseChessGame()
        return
      }
      chessTurn = 'black'
      if (!hasAnyLegalMove(chessBoard, 'black')) {
        if (isInCheck(chessBoard, 'black')) {
          chessGameResult = 'win'
        } else {
          chessGameResult = 'draw'
        }
        endChineseChessGame()
        return
      }
      aiMove()
    } else {
      chessSelected = null
      chessLegalMoves = []
    }
  } else {
    if (clicked && isRed(clicked)) {
      chessSelected = { col, row }
      chessLegalMoves = getLegalMoves(chessBoard, col, row)
    }
  }
}

function undoChessMove() {
  if (chessHistory.length < 2 || chessThinking || chessGameResult) return
  const m2 = chessHistory.pop()
  const m1 = chessHistory.pop()
  chessBoard = cloneBoard(m1.board)
  chessLastMove = chessHistory.length > 0 ? { from: chessHistory[chessHistory.length - 1].from, to: chessHistory[chessHistory.length - 1].to } : null
  chessSelected = null
  chessLegalMoves = []
  chessTurn = 'red'
}

function drawChineseChess(ctx) {
  const W = canvasWidth, H = canvasHeight.value
  ctx.clearRect(0, 0, W, H)
  // Background
  ctx.fillStyle = '#f5e6c8'
  ctx.fillRect(0, 0, W, H)

  const ox = CHESS_OX, oy = CHESS_OY, cs = CHESS_CELL
  const bw = cs * 8, bh = cs * 9

  // Board grid lines
  ctx.strokeStyle = '#5c3d2e'
  ctx.lineWidth = 1.5
  for (let r = 0; r < 10; r++) {
    ctx.beginPath(); ctx.moveTo(ox, oy + r * cs); ctx.lineTo(ox + bw, oy + r * cs); ctx.stroke()
  }
  for (let c = 0; c < 9; c++) {
    if (c === 0 || c === 8) {
      ctx.beginPath(); ctx.moveTo(ox + c * cs, oy); ctx.lineTo(ox + c * cs, oy + bh); ctx.stroke()
    } else {
      ctx.beginPath(); ctx.moveTo(ox + c * cs, oy); ctx.lineTo(ox + c * cs, oy + 4 * cs); ctx.stroke()
      ctx.beginPath(); ctx.moveTo(ox + c * cs, oy + 5 * cs); ctx.lineTo(ox + c * cs, oy + bh); ctx.stroke()
    }
  }
  // Palace diagonals
  for (const [c1, r1, c2, r2] of [[3,0,5,2],[5,0,3,2],[3,7,5,9],[5,7,3,9]]) {
    ctx.beginPath(); ctx.moveTo(ox + c1 * cs, oy + r1 * cs); ctx.lineTo(ox + c2 * cs, oy + r2 * cs); ctx.stroke()
  }
  // River text
  ctx.fillStyle = '#5c3d2e'
  ctx.font = 'bold 18px sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('楚 河', ox + bw / 2 - 60, oy + 4.5 * cs)
  ctx.fillText('汉 界', ox + bw / 2 + 60, oy + 4.5 * cs)

  // Last move highlight
  if (chessLastMove) {
    ctx.fillStyle = 'rgba(251,191,36,0.35)'
    const f = chessLastMove.from, t = chessLastMove.to
    ctx.fillRect(ox + f.col * cs - cs / 2, oy + f.row * cs - cs / 2, cs, cs)
    ctx.fillRect(ox + t.col * cs - cs / 2, oy + t.row * cs - cs / 2, cs, cs)
  }

  // Selected piece highlight
  if (chessSelected) {
    ctx.fillStyle = 'rgba(59,130,246,0.3)'
    ctx.fillRect(ox + chessSelected.col * cs - cs / 2, oy + chessSelected.row * cs - cs / 2, cs, cs)
  }

  // Legal move dots
  for (const m of chessLegalMoves) {
    const mx = ox + m.col * cs, my = oy + m.row * cs
    ctx.fillStyle = chessBoard[m.row][m.col] ? 'rgba(239,68,68,0.4)' : 'rgba(34,197,94,0.5)'
    ctx.beginPath(); ctx.arc(mx, my, 8, 0, Math.PI * 2); ctx.fill()
  }

  // Draw pieces
  const pulse = 1 + Math.sin(Date.now() / 300) * 0.08
  for (let r = 0; r < 10; r++) {
    for (let c = 0; c < 9; c++) {
      const p = chessBoard[r][c]
      if (!p) continue
      const x = ox + c * cs, y = oy + r * cs
      const isSelected = chessSelected && chessSelected.col === c && chessSelected.row === r
      const rad = isSelected ? 18 * pulse : 17
      // Piece shadow
      ctx.fillStyle = 'rgba(0,0,0,0.15)'
      ctx.beginPath(); ctx.arc(x + 1, y + 2, rad, 0, Math.PI * 2); ctx.fill()
      // Piece background
      const grad = ctx.createRadialGradient(x - 3, y - 3, 2, x, y, rad)
      if (isRed(p)) {
        grad.addColorStop(0, '#fca5a5')
        grad.addColorStop(1, '#dc2626')
      } else {
        grad.addColorStop(0, '#94a3b8')
        grad.addColorStop(1, '#1e293b')
      }
      ctx.fillStyle = grad
      ctx.beginPath(); ctx.arc(x, y, rad, 0, Math.PI * 2); ctx.fill()
      // Piece border
      ctx.strokeStyle = isRed(p) ? '#991b1b' : '#0f172a'
      ctx.lineWidth = 1.5
      ctx.beginPath(); ctx.arc(x, y, rad, 0, Math.PI * 2); ctx.stroke()
      // Inner ring
      ctx.strokeStyle = isRed(p) ? '#fca5a5' : '#cbd5e1'
      ctx.lineWidth = 1
      ctx.beginPath(); ctx.arc(x, y, rad - 4, 0, Math.PI * 2); ctx.stroke()
      // Piece text
      ctx.fillStyle = isRed(p) ? '#fff' : '#fff'
      ctx.font = 'bold 16px sans-serif'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(CHESS_PIECE_NAMES[p], x, y + 1)
    }
  }

  // Check indicator
  if (isInCheck(chessBoard, 'red') && !chessGameResult) {
    const kp = getKingPos(chessBoard, 'red')
    if (kp) {
      const kx = ox + kp.col * cs, ky = oy + kp.row * cs
      const flash = Math.sin(Date.now() / 150) > 0
      if (flash) {
        ctx.strokeStyle = '#ef4444'
        ctx.lineWidth = 3
        ctx.beginPath(); ctx.arc(kx, ky, 20, 0, Math.PI * 2); ctx.stroke()
      }
    }
    ctx.fillStyle = '#ef4444'
    ctx.font = 'bold 14px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText('将军！', ox + bw / 2, oy - 10)
  }

  // Turn / status indicator at bottom
  const statusY = oy + bh + 28
  ctx.fillStyle = '#374151'
  ctx.font = '13px sans-serif'
  ctx.textAlign = 'center'
  if (chessGameResult) {
    ctx.fillStyle = chessGameResult === 'win' ? '#16a34a' : chessGameResult === 'lose' ? '#dc2626' : '#ca8a04'
    ctx.font = 'bold 15px sans-serif'
    const msg = chessGameResult === 'win' ? '恭喜获胜！' : chessGameResult === 'lose' ? '再接再厉！' : '和棋'
    ctx.fillText(msg, W / 2, statusY)
  } else if (chessThinking) {
    ctx.fillStyle = '#6366f1'
    ctx.fillText('AI 思考中...', W / 2, statusY)
  } else {
    const turnText = chessTurn === 'red' ? '红方回合（你）' : '黑方回合'
    ctx.fillStyle = chessTurn === 'red' ? '#dc2626' : '#1e293b'
    ctx.fillText(turnText, W / 2, statusY)
  }
  // Undo button (bottom-right)
  if (chessHistory.length >= 2 && !chessThinking && !chessGameResult && chessTurn === 'red') {
    const btnX = W - 55, btnY = statusY - 10
    ctx.fillStyle = '#6366f1'
    ctx.beginPath(); ctx.roundRect(btnX, btnY, 50, 22, 4); ctx.fill()
    ctx.fillStyle = '#fff'
    ctx.font = '12px sans-serif'
    ctx.fillText('悔棋', btnX + 25, btnY + 11)
  }
  // Difficulty display
  const diffNames = ['', '简单', '普通', '困难', '大师']
  ctx.fillStyle = '#6b7280'
  ctx.font = '12px sans-serif'
  ctx.textAlign = 'left'
  ctx.fillText('难度: ' + (diffNames[chessDifficulty.value] || ''), 4, statusY)
}

function handleChessCanvasClick(e) {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const scaleX = canvasWidth / rect.width
  const scaleY = canvasHeight.value / rect.height
  const cx = (e.clientX - rect.left) * scaleX
  const cy = (e.clientY - rect.top) * scaleY

  // Check undo button
  if (chessHistory.length >= 2 && !chessThinking && !chessGameResult && chessTurn === 'red') {
    const btnX = canvasWidth - 55, btnY = CHESS_OY + CHESS_CELL * 9 + 18
    if (cx >= btnX && cx <= btnX + 50 && cy >= btnY && cy <= btnY + 22) {
      undoChessMove()
      return
    }
  }

  // Convert to board coordinates
  const col = Math.round((cx - CHESS_OX) / CHESS_CELL)
  const row = Math.round((cy - CHESS_OY) / CHESS_CELL)
  if (col >= 0 && col < 9 && row >= 0 && row < 10) {
    const dx = cx - (CHESS_OX + col * CHESS_CELL)
    const dy = cy - (CHESS_OY + row * CHESS_CELL)
    if (dx * dx + dy * dy <= CHESS_CELL * CHESS_CELL * 0.6) {
      handleChessClick(col, row)
    }
  }
}

// ===== Flappy Bird =====
let bird = { x: 80, y: 200, vy: 0, rotation: 0 }
let pipes = []
let flappyTimer = 0
let pipeSpawnTimer = 0
const GRAVITY = 0.45
const FLAP_FORCE = -7.5
const PIPE_GAP = 130
const PIPE_WIDTH = 52
const PIPE_SPEED = 2.2
const BIRD_SIZE = 20
let flappyStarted = false

function initFlappy() {
  bird = { x: 80, y: 200, vy: 0, rotation: 0 }
  pipes = []
  flappyTimer = 0
  pipeSpawnTimer = 0
  flappyStarted = false
}

function flap() {
  if (gameState.value !== 'playing') return
  if (!flappyStarted) flappyStarted = true
  bird.vy = FLAP_FORCE
}

function updateFlappy(dt) {
  if (!flappyStarted) {
    // Idle bob animation
    bird.y = 200 + Math.sin(Date.now() / 300) * 10
    return
  }

  if (gameState.value !== 'playing') return

  const scale = dt / 16.67
  bird.vy += GRAVITY * scale
  bird.y += bird.vy * scale
  bird.rotation = Math.min(Math.max(bird.vy * 3, -30), 90)

  // Ceiling
  if (bird.y < 0) { bird.y = 0; bird.vy = 0 }
  // Floor
  if (bird.y > canvasHeight.value - BIRD_SIZE) {
    gameState.value = 'over'
    saveHighScore()
    return
  }

  // Spawn pipes
  pipeSpawnTimer += dt
  if (pipeSpawnTimer > 1600) {
    pipeSpawnTimer = 0
    const topH = 40 + Math.random() * (canvasHeight.value - PIPE_GAP - 80)
    pipes.push({ x: canvasWidth, topH, passed: false })
  }

  // Move pipes
  for (const pipe of pipes) {
    pipe.x -= PIPE_SPEED * scale
    // Score when passing
    if (!pipe.passed && pipe.x + PIPE_WIDTH < bird.x) {
      pipe.passed = true
      score.value += 1
    }
  }
  pipes = pipes.filter(p => p.x + PIPE_WIDTH > 0)

  // Collision detection
  const bx = bird.x - BIRD_SIZE / 2
  const by = bird.y - BIRD_SIZE / 2
  const bw = BIRD_SIZE
  const bh = BIRD_SIZE

  for (const pipe of pipes) {
    // Top pipe
    if (rectOverlap(bx, by, bw, bh, pipe.x, 0, PIPE_WIDTH, pipe.topH)) {
      gameState.value = 'over'; saveHighScore(); return
    }
    // Bottom pipe
    const bottomY = pipe.topH + PIPE_GAP
    if (rectOverlap(bx, by, bw, bh, pipe.x, bottomY, PIPE_WIDTH, canvasHeight.value - bottomY)) {
      gameState.value = 'over'; saveHighScore(); return
    }
  }
}

function rectOverlap(x1, y1, w1, h1, x2, y2, w2, h2) {
  return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2
}

function drawFlappy(ctx) {
  // Sky gradient (cached)
  if (!flappyGrad) {
    flappyGrad = ctx.createLinearGradient(0, 0, 0, canvasHeight.value)
    flappyGrad.addColorStop(0, '#4ec0ca')
    flappyGrad.addColorStop(1, '#70e0a0')
  }
  ctx.fillStyle = flappyGrad
  ctx.fillRect(0, 0, canvasWidth, canvasHeight.value)

  // Ground
  ctx.fillStyle = '#ded895'
  ctx.fillRect(0, canvasHeight.value - 20, canvasWidth, 20)
  ctx.fillStyle = '#5d8a3c'
  ctx.fillRect(0, canvasHeight.value - 22, canvasWidth, 4)

  // Pipes
  for (const pipe of pipes) {
    const bottomY = pipe.topH + PIPE_GAP
    // Top pipe
    ctx.fillStyle = '#74bf2e'
    ctx.fillRect(pipe.x, 0, PIPE_WIDTH, pipe.topH)
    ctx.fillStyle = '#5da025'
    ctx.fillRect(pipe.x - 3, pipe.topH - 20, PIPE_WIDTH + 6, 20)
    // Bottom pipe
    ctx.fillStyle = '#74bf2e'
    ctx.fillRect(pipe.x, bottomY, PIPE_WIDTH, canvasHeight.value - bottomY)
    ctx.fillStyle = '#5da025'
    ctx.fillRect(pipe.x - 3, bottomY, PIPE_WIDTH + 6, 20)
  }

  // Bird
  ctx.save()
  ctx.translate(bird.x, bird.y)
  ctx.rotate(bird.rotation * Math.PI / 180)
  // Body
  ctx.fillStyle = '#f5c842'
  ctx.beginPath(); ctx.ellipse(0, 0, BIRD_SIZE / 2, BIRD_SIZE / 2 - 2, 0, 0, Math.PI * 2); ctx.fill()
  // Wing
  ctx.fillStyle = '#e8a525'
  ctx.beginPath(); ctx.ellipse(-3, 2, 8, 5, -0.3, 0, Math.PI * 2); ctx.fill()
  // Eye
  ctx.fillStyle = '#fff'
  ctx.beginPath(); ctx.arc(6, -4, 5, 0, Math.PI * 2); ctx.fill()
  ctx.fillStyle = '#333'
  ctx.beginPath(); ctx.arc(7, -4, 2.5, 0, Math.PI * 2); ctx.fill()
  // Beak
  ctx.fillStyle = '#e85d3a'
  ctx.beginPath(); ctx.moveTo(10, 0); ctx.lineTo(18, 2); ctx.lineTo(10, 5); ctx.closePath(); ctx.fill()
  ctx.restore()
}

// ===== Breakout =====
const BRICK_COLS = 8
const BRICK_ROWS = 5
const BRICK_W = canvasWidth / BRICK_COLS
const BRICK_H = 22
const PADDLE_H = 14
const BALL_R = 7
const BRICK_COLORS = ['#ef4444', '#f97316', '#eab308', '#22c55e', '#3b82f6']
const BRICK_POINTS = [50, 40, 30, 20, 10]

let paddle = { x: 0, w: 70 }
let breakoutBalls = []
let bricks = []
let ballLaunched = false
let breakoutLeft = ref(false)
let breakoutRight = ref(false)
let breakoutLives = 3
let breakoutLevel = 1
let breakoutCombo = 0
let breakoutComboTimer = 0
let breakoutParticles = []
let breakoutPowerups = []
let breakoutLasers = []
let breakoutLaserTimer = 0
let breakoutShakeTimer = 0
let breakoutPendingLevelUp = false
// Active powerup timers
let breakoutPaddleWide = false
let breakoutPaddleWideTimer = 0
let breakoutSlow = false
let breakoutSlowTimer = 0
let breakoutFireball = false
let breakoutFireballTimer = 0
let breakoutLaserActive = false
let breakoutLaserPowerTimer = 0
// Stars for background
let breakoutStars = []
// Level transition
let breakoutLevelAnim = 0

function initBreakout() {
  paddle = { x: (canvasWidth - 70) / 2, w: 70 }
  breakoutBalls = [{ x: canvasWidth / 2, y: canvasHeight.value - 40, vx: 0, vy: 0, trail: [] }]
  ballLaunched = false
  bricks = []
  bricksLeft.value = 0
  breakoutLives = 3
  breakoutLevel = 1
  breakoutCombo = 0
  breakoutComboTimer = 0
  breakoutParticles = []
  breakoutPowerups = []
  breakoutLasers = []
  breakoutLaserTimer = 0
  breakoutShakeTimer = 0
  breakoutPaddleWide = false
  breakoutPaddleWideTimer = 0
  breakoutSlow = false
  breakoutSlowTimer = 0
  breakoutFireball = false
  breakoutFireballTimer = 0
  breakoutLaserActive = false
  breakoutLaserPowerTimer = 0
  breakoutLevelAnim = 0
  breakoutBuildLevel()
  // Init stars
  if (breakoutStars.length === 0) {
    for (let i = 0; i < 40; i++) {
      breakoutStars.push({ x: Math.random() * canvasWidth, y: Math.random() * canvasHeight.value, s: 0.3 + Math.random() * 1.2, a: 0.1 + Math.random() * 0.4 })
    }
  }
}

function breakoutBuildLevel() {
  bricks = []
  bricksLeft.value = 0
  const rows = Math.min(5 + Math.floor((breakoutLevel - 1) / 2), 8)
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < BRICK_COLS; c++) {
      let hp = 1, color = BRICK_COLORS[r % 5], points = BRICK_POINTS[r % 5], type = 'normal'
      if (breakoutLevel >= 3 && r === 0) { hp = 3; color = '#fbbf24'; points = 100; type = 'gold' }
      else if (breakoutLevel >= 2 && r <= 1) { hp = 2; color = '#94a3b8'; points = 60; type = 'silver' }
      bricks.push({
        x: c * BRICK_W, y: r * BRICK_H + 40, w: BRICK_W - 2, h: BRICK_H - 2,
        color, points, alive: true, hp, maxHp: hp, type, flash: 0
      })
      bricksLeft.value++
    }
  }
}

function breakoutNextLevel() {
  breakoutLevel++
  breakoutLevelAnim = 1500
  breakoutPendingLevelUp = false
  // Reset ball to paddle
  breakoutBalls = [{ x: paddle.x + paddle.w / 2, y: canvasHeight.value - 40, vx: 0, vy: 0, trail: [] }]
  ballLaunched = false
  // Clear powerups, lasers, and active powerup states
  breakoutPowerups = []
  breakoutLasers = []
  breakoutPaddleWide = false; breakoutPaddleWideTimer = 0; paddle.w = 70
  breakoutSlow = false; breakoutSlowTimer = 0
  breakoutFireball = false; breakoutFireballTimer = 0
  breakoutLaserActive = false; breakoutLaserPowerTimer = 0
  breakoutBuildLevel()
}

function spawnBreakoutPowerup(x, y) {
  const types = ['expand', 'multiball', 'laser', 'slow', 'life', 'fireball']
  const type = types[Math.floor(Math.random() * types.length)]
  breakoutPowerups.push({ x: x - 10, y, w: 20, h: 20, type, speed: 100 })
}

function breakoutSpawnParticles(x, y, color, count) {
  const maxP = 100
  for (let i = 0; i < count && breakoutParticles.length < maxP; i++) {
    const angle = Math.random() * Math.PI * 2
    const speed = 50 + Math.random() * 150
    breakoutParticles.push({
      x, y, vx: Math.cos(angle) * speed, vy: Math.sin(angle) * speed,
      life: 0.3 + Math.random() * 0.4, maxLife: 0.5, size: 2 + Math.random() * 3, color
    })
  }
}

function launchBall() {
  if (gameState.value !== 'playing' || ballLaunched) return
  ballLaunched = true
  const b = breakoutBalls[0]
  if (!b) return
  const angle = -Math.PI / 4 + Math.random() * Math.PI / 2
  const speed = 4 * (1 + (breakoutLevel - 1) * 0.05)
  b.vx = Math.sin(angle) * speed
  b.vy = -speed
}

function updateBreakout(dt) {
  const scale = dt / 16.67
  const paddleSpeed = 5 * scale
  if (breakoutLeft.value) paddle.x = Math.max(0, paddle.x - paddleSpeed)
  if (breakoutRight.value) paddle.x = Math.min(canvasWidth - paddle.w, paddle.x + paddleSpeed)

  // Level animation countdown
  if (breakoutLevelAnim > 0) breakoutLevelAnim -= dt

  // Combo timer
  if (breakoutComboTimer > 0) { breakoutComboTimer -= dt; if (breakoutComboTimer <= 0) breakoutCombo = 0 }

  // Shake timer
  if (breakoutShakeTimer > 0) breakoutShakeTimer -= dt

  // Powerup timers
  if (breakoutPaddleWideTimer > 0) {
    breakoutPaddleWideTimer -= dt
    if (breakoutPaddleWideTimer <= 0) { breakoutPaddleWide = false; paddle.w = 70 }
  }
  if (breakoutSlowTimer > 0) {
    breakoutSlowTimer -= dt
    if (breakoutSlowTimer <= 0) breakoutSlow = false
  }
  if (breakoutFireballTimer > 0) {
    breakoutFireballTimer -= dt
    if (breakoutFireballTimer <= 0) breakoutFireball = false
  }
  if (breakoutLaserPowerTimer > 0) {
    breakoutLaserPowerTimer -= dt
    if (breakoutLaserPowerTimer <= 0) breakoutLaserActive = false
  }

  // Laser auto-fire
  if (breakoutLaserActive && ballLaunched) {
    breakoutLaserTimer -= dt
    if (breakoutLaserTimer <= 0) {
      breakoutLaserTimer = 200
      const px = paddle.x + paddle.w / 2
      breakoutLasers.push({ x: px - 4, y: canvasHeight.value - 20 - PADDLE_H - 4, w: 3, h: 10 })
      breakoutLasers.push({ x: px + 1, y: canvasHeight.value - 20 - PADDLE_H - 4, w: 3, h: 10 })
    }
  }

  // Laser movement
  for (let i = breakoutLasers.length - 1; i >= 0; i--) {
    breakoutLasers[i].y -= 8 * scale
    if (breakoutLasers[i].y < -10) { breakoutLasers.splice(i, 1); continue }
    // Laser vs bricks
    const l = breakoutLasers[i]
    for (const brick of bricks) {
      if (!brick.alive) continue
      if (l.x + l.w > brick.x && l.x < brick.x + brick.w && l.y < brick.y + brick.h && l.y + l.h > brick.y) {
        breakoutHitBrick(brick, l.x + l.w / 2, l.y)
        breakoutLasers.splice(i, 1)
        break
      }
    }
  }

  // Powerup fall + collection
  const paddleTop = canvasHeight.value - 20 - PADDLE_H
  const paddleBottom = canvasHeight.value - 20
  for (let i = breakoutPowerups.length - 1; i >= 0; i--) {
    const pu = breakoutPowerups[i]
    pu.y += pu.speed * scale
    if (pu.y > canvasHeight.value + 20) { breakoutPowerups.splice(i, 1); continue }
    if (pu.y + pu.h >= paddleTop && pu.y <= paddleBottom && pu.x + pu.w > paddle.x && pu.x < paddle.x + paddle.w) {
      breakoutApplyPowerup(pu.type)
      breakoutPowerups.splice(i, 1)
    }
  }

  // Ball not launched
  if (!ballLaunched) {
    const b = breakoutBalls[0]
    if (b) { b.x = paddle.x + paddle.w / 2; b.y = canvasHeight.value - 40 }
    return
  }

  const speedMul = breakoutSlow ? 0.6 : 1

  // Update balls
  for (let bi = breakoutBalls.length - 1; bi >= 0; bi--) {
    const b = breakoutBalls[bi]
    // Trail
    b.trail.push({ x: b.x, y: b.y })
    if (b.trail.length > 5) b.trail.shift()

    b.x += b.vx * scale * speedMul
    b.y += b.vy * scale * speedMul

    // Wall collision
    if (b.x - BALL_R < 0) { b.x = BALL_R; b.vx = Math.abs(b.vx) }
    if (b.x + BALL_R > canvasWidth) { b.x = canvasWidth - BALL_R; b.vx = -Math.abs(b.vx) }
    if (b.y - BALL_R < 0) { b.y = BALL_R; b.vy = Math.abs(b.vy) }

    // Paddle collision
    if (b.vy > 0 &&
      b.y + BALL_R >= canvasHeight.value - 20 - PADDLE_H &&
      b.y + BALL_R <= canvasHeight.value - 20 &&
      b.x >= paddle.x && b.x <= paddle.x + paddle.w) {
      const hitPos = (b.x - paddle.x) / paddle.w
      const angle = (hitPos - 0.5) * Math.PI * 0.7
      const speed = Math.sqrt(b.vx * b.vx + b.vy * b.vy)
      b.vx = Math.sin(angle) * speed
      b.vy = -Math.abs(Math.cos(angle) * speed)
      b.y = canvasHeight.value - 20 - PADDLE_H - BALL_R
    }

    // Brick collision
    for (const brick of bricks) {
      if (!brick.alive) continue
      if (b.x + BALL_R > brick.x && b.x - BALL_R < brick.x + brick.w &&
        b.y + BALL_R > brick.y && b.y - BALL_R < brick.y + brick.h) {
        breakoutHitBrick(brick, b.x, b.y)
        if (!breakoutFireball) {
          const overlapL = b.x + BALL_R - brick.x
          const overlapR = brick.x + brick.w - (b.x - BALL_R)
          const overlapT = b.y + BALL_R - brick.y
          const overlapB = brick.y + brick.h - (b.y - BALL_R)
          const minO = Math.min(overlapL, overlapR, overlapT, overlapB)
          if (minO === overlapT || minO === overlapB) b.vy = -b.vy
          else b.vx = -b.vx
        }
        break
      }
    }

    // Ball falls out
    if (b.y - BALL_R > canvasHeight.value) {
      breakoutBalls.splice(bi, 1)
    }
  }

  // All balls lost
  if (breakoutBalls.length === 0) {
    breakoutLives--
    breakoutCombo = 0
    if (breakoutLives <= 0) {
      gameState.value = 'over'
      saveHighScore()
      return
    }
    // Reset ball
    breakoutBalls = [{ x: paddle.x + paddle.w / 2, y: canvasHeight.value - 40, vx: 0, vy: 0, trail: [] }]
    ballLaunched = false
  }

  // Particles
  for (let i = breakoutParticles.length - 1; i >= 0; i--) {
    const p = breakoutParticles[i]
    p.x += p.vx * scale; p.y += p.vy * scale
    p.vy += 200 * dt / 1000 // gravity
    p.life -= dt / 1000
    if (p.life <= 0) breakoutParticles.splice(i, 1)
  }

  // Deferred level transition (safe after all loops)
  if (breakoutPendingLevelUp) {
    breakoutPendingLevelUp = false
    breakoutNextLevel()
  }
}

function breakoutHitBrick(brick, hitX, hitY) {
  brick.hp--
  brick.flash = 80
  if (brick.hp <= 0) {
    brick.alive = false
    bricksLeft.value--
    breakoutCombo++
    breakoutComboTimer = 3000
    const mul = breakoutCombo >= 20 ? 5 : breakoutCombo >= 10 ? 3 : breakoutCombo >= 5 ? 2 : 1
    score.value += brick.points * mul
    breakoutSpawnParticles(hitX, hitY, brick.color, 8)
    breakoutShakeTimer = 80
    if (Math.random() < 0.15) spawnBreakoutPowerup(brick.x + brick.w / 2, brick.y + brick.h / 2)
    // Win check — defer to avoid mid-iteration crash with multi-ball
    if (bricksLeft.value === 0) {
      breakoutPendingLevelUp = true
    }
  } else {
    breakoutSpawnParticles(hitX, hitY, brick.color, 3)
  }
}

function breakoutApplyPowerup(type) {
  if (type === 'expand') {
    breakoutPaddleWide = true
    breakoutPaddleWideTimer = 10000
    paddle.w = 110
    paddle.x = Math.min(paddle.x, canvasWidth - paddle.w)
  } else if (type === 'multiball') {
    const src = breakoutBalls[0]
    if (src) {
      const speed = Math.sqrt(src.vx * src.vx + src.vy * src.vy) || 4
      breakoutBalls.push({ x: src.x, y: src.y, vx: speed * 0.7, vy: -speed * 0.7, trail: [] })
      breakoutBalls.push({ x: src.x, y: src.y, vx: -speed * 0.7, vy: -speed * 0.7, trail: [] })
    }
  } else if (type === 'laser') {
    breakoutLaserActive = true
    breakoutLaserPowerTimer = 10000
  } else if (type === 'slow') {
    breakoutSlow = true
    breakoutSlowTimer = 8000
  } else if (type === 'life') {
    breakoutLives = Math.min(breakoutLives + 1, 5)
  } else if (type === 'fireball') {
    breakoutFireball = true
    breakoutFireballTimer = 8000
  }
}

function drawBreakout(ctx) {
  // Shake offset
  let sx = 0, sy = 0
  if (breakoutShakeTimer > 0) {
    sx = (Math.random() - 0.5) * 4
    sy = (Math.random() - 0.5) * 4
  }
  ctx.save()
  ctx.translate(sx, sy)

  // Background gradient
  const grad = ctx.createLinearGradient(0, 0, 0, canvasHeight.value)
  grad.addColorStop(0, '#0f172a')
  grad.addColorStop(1, '#1e1b4b')
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, canvasWidth, canvasHeight.value)

  // Stars
  for (const s of breakoutStars) {
    ctx.globalAlpha = s.a
    ctx.fillStyle = '#e2e8f0'
    ctx.fillRect(s.x, s.y, s.s, s.s)
  }
  ctx.globalAlpha = 1

  // Bricks
  for (const brick of bricks) {
    if (!brick.alive) continue
    let color = brick.color
    if (brick.flash > 0) { brick.flash -= 16; color = '#ffffff' }
    ctx.fillStyle = color
    ctx.beginPath(); ctx.roundRect(brick.x, brick.y, brick.w, brick.h, 3); ctx.fill()
    // Metallic shine for gold/silver
    if (brick.type === 'gold' || brick.type === 'silver') {
      const g = ctx.createLinearGradient(brick.x, brick.y, brick.x, brick.y + brick.h)
      g.addColorStop(0, 'rgba(255,255,255,0.4)')
      g.addColorStop(0.5, 'rgba(255,255,255,0.05)')
      g.addColorStop(1, 'rgba(0,0,0,0.2)')
      ctx.fillStyle = g
      ctx.beginPath(); ctx.roundRect(brick.x, brick.y, brick.w, brick.h, 3); ctx.fill()
    } else {
      ctx.fillStyle = 'rgba(255,255,255,0.15)'
      ctx.beginPath(); ctx.roundRect(brick.x, brick.y, brick.w, brick.h / 3, [3, 3, 0, 0]); ctx.fill()
    }
    // HP indicator for multi-hit bricks
    if (brick.maxHp > 1 && brick.hp > 0) {
      ctx.fillStyle = 'rgba(255,255,255,0.7)'
      ctx.font = 'bold 10px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText(brick.hp, brick.x + brick.w / 2, brick.y + brick.h / 2 + 3)
    }
    // Crack overlay for damaged bricks
    if (brick.hp < brick.maxHp && brick.hp > 0) {
      ctx.strokeStyle = 'rgba(0,0,0,0.4)'
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.moveTo(brick.x + brick.w * 0.3, brick.y)
      ctx.lineTo(brick.x + brick.w * 0.5, brick.y + brick.h * 0.6)
      ctx.lineTo(brick.x + brick.w * 0.7, brick.y + brick.h)
      ctx.stroke()
    }
  }

  // Powerups
  const puColors = { expand: '#22c55e', multiball: '#3b82f6', laser: '#ef4444', slow: '#eab308', life: '#ec4899', fireball: '#f97316' }
  const puLabels = { expand: 'E', multiball: 'M', laser: 'L', slow: 'S', life: '+', fireball: 'F' }
  for (const pu of breakoutPowerups) {
    ctx.fillStyle = puColors[pu.type]
    ctx.beginPath(); ctx.roundRect(pu.x, pu.y, pu.w, pu.h, 4); ctx.fill()
    ctx.fillStyle = '#fff'
    ctx.font = 'bold 12px sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(puLabels[pu.type], pu.x + pu.w / 2, pu.y + pu.h / 2)
  }

  // Lasers
  ctx.fillStyle = '#ef4444'
  for (const l of breakoutLasers) {
    ctx.fillRect(l.x, l.y, l.w, l.h)
    ctx.fillStyle = '#fca5a5'
    ctx.fillRect(l.x, l.y, l.w, l.h / 3)
    ctx.fillStyle = '#ef4444'
  }

  // Ball trails + balls
  for (const b of breakoutBalls) {
    // Trail
    for (let i = 0; i < b.trail.length; i++) {
      const t = b.trail[i]
      const alpha = (i + 1) / b.trail.length * 0.3
      ctx.fillStyle = breakoutFireball ? `rgba(251,146,60,${alpha})` : `rgba(248,250,252,${alpha})`
      ctx.beginPath(); ctx.arc(t.x, t.y, BALL_R * (0.5 + 0.5 * (i + 1) / b.trail.length), 0, Math.PI * 2); ctx.fill()
    }
    // Ball
    if (breakoutFireball) {
      ctx.fillStyle = '#fb923c'
      ctx.shadowColor = '#f97316'
      ctx.shadowBlur = 10
    } else {
      ctx.fillStyle = '#f8fafc'
    }
    ctx.beginPath(); ctx.arc(b.x, b.y, BALL_R, 0, Math.PI * 2); ctx.fill()
    ctx.shadowBlur = 0
    ctx.fillStyle = 'rgba(255,255,255,0.5)'
    ctx.beginPath(); ctx.arc(b.x - 2, b.y - 2, 3, 0, Math.PI * 2); ctx.fill()
  }

  // Particles
  for (const p of breakoutParticles) {
    ctx.globalAlpha = Math.max(0, p.life / p.maxLife)
    ctx.fillStyle = p.color
    ctx.fillRect(p.x - p.size / 2, p.y - p.size / 2, p.size, p.size)
  }
  ctx.globalAlpha = 1

  // Paddle
  const py = canvasHeight.value - 20 - PADDLE_H
  const pg = ctx.createLinearGradient(paddle.x, py, paddle.x, py + PADDLE_H)
  pg.addColorStop(0, '#94a3b8')
  pg.addColorStop(1, '#475569')
  ctx.fillStyle = pg
  ctx.beginPath(); ctx.roundRect(paddle.x, py, paddle.w, PADDLE_H, 7); ctx.fill()
  // Glow edge
  ctx.fillStyle = 'rgba(148,163,184,0.4)'
  ctx.beginPath(); ctx.roundRect(paddle.x, py, paddle.w, 3, [7, 7, 0, 0]); ctx.fill()
  // Laser ports
  if (breakoutLaserActive) {
    ctx.fillStyle = '#ef4444'
    ctx.fillRect(paddle.x + 4, py - 3, 4, 3)
    ctx.fillRect(paddle.x + paddle.w - 8, py - 3, 4, 3)
  }

  // HUD — lives
  ctx.font = '14px sans-serif'
  for (let i = 0; i < breakoutLives; i++) {
    ctx.fillStyle = '#ef4444'
    ctx.fillText('❤', 8 + i * 18, 18)
  }
  // Level
  ctx.fillStyle = '#94a3b8'
  ctx.font = 'bold 12px sans-serif'
  ctx.textAlign = 'right'
  ctx.fillText(t('games.hudLevel') + breakoutLevel, canvasWidth - 8, 18)
  // Combo
  if (breakoutCombo >= 3) {
    const mul = breakoutCombo >= 20 ? 5 : breakoutCombo >= 10 ? 3 : breakoutCombo >= 5 ? 2 : 1
    ctx.fillStyle = breakoutCombo >= 10 ? '#fbbf24' : breakoutCombo >= 5 ? '#a78bfa' : '#60a5fa'
    ctx.font = 'bold 14px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(breakoutCombo + ' ' + t('games.hudCombo') + ' x' + mul, canvasWidth / 2, 18)
  }
  // Active powerup indicators
  ctx.textAlign = 'left'
  ctx.font = '10px sans-serif'
  let puY = 32
  if (breakoutPaddleWide) { ctx.fillStyle = '#22c55e'; ctx.fillText(t('games.breakoutExpand') + ' ' + Math.ceil(breakoutPaddleWideTimer / 1000) + 's', 8, puY); puY += 14 }
  if (breakoutLaserActive) { ctx.fillStyle = '#ef4444'; ctx.fillText(t('games.breakoutLaser') + ' ' + Math.ceil(breakoutLaserPowerTimer / 1000) + 's', 8, puY); puY += 14 }
  if (breakoutSlow) { ctx.fillStyle = '#eab308'; ctx.fillText(t('games.breakoutSlow') + ' ' + Math.ceil(breakoutSlowTimer / 1000) + 's', 8, puY); puY += 14 }
  if (breakoutFireball) { ctx.fillStyle = '#f97316'; ctx.fillText(t('games.breakoutFire') + ' ' + Math.ceil(breakoutFireballTimer / 1000) + 's', 8, puY); puY += 14 }

  // Level transition animation
  if (breakoutLevelAnim > 0) {
    ctx.globalAlpha = Math.min(1, breakoutLevelAnim / 500)
    ctx.fillStyle = '#fbbf24'
    ctx.font = 'bold 28px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(t('games.hudLevel') + breakoutLevel, canvasWidth / 2, canvasHeight.value / 2)
    ctx.globalAlpha = 1
  }

  ctx.restore()
}

// ===== Shooter (雷霆战机) =====
const SHOOTER_PLAYER_W = 40
const SHOOTER_PLAYER_H = 40
const SHOOTER_SPEED = 320
const SHOOTER_FIRE_RATE = 180
const SHOOTER_BULLET_SPEED = 600
const SHOOTER_ENEMY_BULLET_SPEED = 180

let shooterPlayer = { x: 180, y: 350 }
let shooterBullets = []
let shooterEnemies = []
let shooterEnemyBullets = []
let shooterStars = [[], [], []] // 3 layers
let shooterParticles = []
let shooterPowerups = []
let shooterScorePopups = []
let shooterBoss = null
let shooterBossDefeated = null // { name, points, timer }
let shooterTimer = 0
let shooterLevel = 1
let shooterLevelTimer = 0
let shooterFireTimer = 0
let shooterLeft = false
let shooterRight = false
let shooterLives = 3
let shooterMaxLives = 3
let shooterInvincible = false
let shooterInvincibleTimer = 0
let shooterPowerupType = null // 'spread' | 'firerate' | null
let shooterPowerupTimer = 0
let shooterCombo = 0
const shooterMaxCombo = ref(0)
let shooterComboTimer = 0
let shooterFlashTimer = 0
let shooterShield = false
let shooterLaser = false
let shooterLaserTimer = 0
let shooterHoming = false
let shooterHomingTimer = 0
let shooterScreenShake = 0
let shooterRevivalUsed = false // 每关限用一次复活
let shooterDoubleScore = false
let shooterDoubleScoreTimer = 0
let shooterSlowEnemy = false
let shooterSlowEnemyTimer = 0
let shooterMagnet = false
let shooterMagnetTimer = 0
let bossFightHit = false // track if player was hit during boss fight

// Cosmetics system
const shooterShopOpen = ref(false)
const shopTab = ref('player_skin')

function loadCosmetics() {
  const saved = localStorage.getItem('shooter_cosmetics')
  if (saved) {
    try {
      const parsed = JSON.parse(saved)
      shooterCosmetics.value = { ...shooterCosmetics.value, ...parsed }
    } catch {}
  }
  shooterEquippedSkills.value = [
    shooterCosmetics.value.equippedSkill1 || null,
    shooterCosmetics.value.equippedSkill2 || null,
    shooterCosmetics.value.equippedSkill3 || null
  ]
}

function saveCosmetics() {
  localStorage.setItem('shooter_cosmetics', JSON.stringify(shooterCosmetics.value))
}

const shooterCosmetics = ref({ // All cosmetics unlocked by default, persisted to localStorage
  coins: 99999,
  equippedPlayerSkin: 'default',
  equippedEnemySkin: 'default',
  equippedBulletStyle: 'default',
  equippedPowerupSkin: 'default',
  equippedSkill1: '',
  equippedSkill2: '',
  equippedSkill3: ''
})

// Equipped skills from shop
const shooterEquippedSkills = ref([
  shooterCosmetics.value.equippedSkill1 || null,
  shooterCosmetics.value.equippedSkill2 || null,
  shooterCosmetics.value.equippedSkill3 || null
]) // 3 slots
const shooterSkillCooldowns = ref([0, 0, 0]) // cooldown timers in ms
let shooterSkillCooldownTimers = [0, 0, 0]

// Revival dialog
const shooterRevivalDialog = ref(false)
const shooterActiveSkillCooldown = ref(0) // ms remaining
let shooterActiveSkillCooldownTimer = 0
let shooterActiveSkillActive = false
let shooterActiveSkillDuration = 0

// Tier colors for UI
const TIER_COLORS = {
  basic: { bg: '#f3f4f6', border: '#d1d5db', text: '#6b7280', labelKey: 'games.tierBasic' },
  rare: { bg: '#dbeafe', border: '#93c5fd', text: '#2563eb', labelKey: 'games.tierRare' },
  epic: { bg: '#ede9fe', border: '#a78bfa', text: '#7c3aed', labelKey: 'games.tierEpic' },
  legendary: { bg: '#fef3c7', border: '#fbbf24', text: '#d97706', labelKey: 'games.tierLegendary' },
}

// Skin tiers: basic (color swap), rare (color + glow effects), epic (different shape), legendary (unique design + effects)
const PLAYER_SKINS = {
  default:  { body: '#3b82f6', wing: '#2563eb', cockpit: '#93c5fd', engine: '#f59e0b', name: '默认', tier: 'basic', glow: null, shape: 'standard' },
  blaze:    { body: '#ef4444', wing: '#dc2626', cockpit: '#fca5a5', engine: '#f97316', name: '烈焰', tier: 'basic', glow: null, shape: 'standard' },
  emerald:  { body: '#10b981', wing: '#059669', cockpit: '#6ee7b7', engine: '#84cc16', name: '翡翠', tier: 'basic', glow: null, shape: 'standard' },
  frost:    { body: '#67e8f9', wing: '#06b6d4', cockpit: '#cffafe', engine: '#a5f3fc', name: '冰霜', tier: 'basic', glow: '#67e8f9', shape: 'standard' },
  phantom:  { body: '#818cf8', wing: '#6366f1', cockpit: '#c7d2fe', engine: '#a78bfa', name: '幻影', tier: 'rare', glow: '#818cf8', shape: 'swept' },
  royal:    { body: '#fbbf24', wing: '#d97706', cockpit: '#fef3c7', engine: '#f59e0b', name: '皇家', tier: 'rare', glow: '#fbbf24', shape: 'swept' },
  crimson:  { body: '#be123c', wing: '#9f1239', cockpit: '#fecdd3', engine: '#e11d48', name: '深红', tier: 'rare', glow: '#f43f5e', shape: 'swept' },
  viper:    { body: '#15803d', wing: '#166534', cockpit: '#bbf7d0', engine: '#4ade80', name: '毒蛇', tier: 'epic', glow: '#22c55e', shape: 'arrow' },
  striker:  { body: '#7c3aed', wing: '#6d28d9', cockpit: '#ddd6fe', engine: '#a78bfa', name: '突击者', tier: 'epic', glow: '#8b5cf6', shape: 'arrow' },
  dragon:   { body: '#dc2626', wing: '#b91c1c', cockpit: '#fca5a5', engine: '#f97316', name: '龙焰', tier: 'legendary', glow: '#f97316', shape: 'dragon', trail: '#ef4444' },
  galaxy:   { body: '#1e1b4b', wing: '#312e81', cockpit: '#a5b4fc', engine: '#818cf8', name: '星河', tier: 'legendary', glow: '#818cf8', shape: 'galaxy', trail: '#6366f1' },
  titan:    { body: '#78350f', wing: '#92400e', cockpit: '#fde68a', engine: '#f59e0b', name: '泰坦', tier: 'legendary', glow: '#fbbf24', shape: 'titan', trail: '#d97706' },
  devourer: { body: '#0A0A0F', wing: '#FF6B35', cockpit: '#4A1942', engine: '#FFB627', name: '噬星者', tier: 'legendary', glow: '#FF6B35', shape: 'devourer', trail: '#87CEEB' },
  kite:     { body: '#8B4513', wing: '#F5F5DC', cockpit: '#2F4F4F', engine: '#00CED1', name: '纸鸢·机关', tier: 'legendary', glow: '#00FF7F', shape: 'kite', trail: '#00CED1' },
  jellyfish:{ body: '#48D1CC', wing: '#20B2AA', cockpit: '#004D4D', engine: '#ADFF2F', name: '深海水母', tier: 'legendary', glow: '#48D1CC', shape: 'jellyfish', trail: '#ADFF2F' },
  glitch:   { body: '#FF00FF', wing: '#00FFFF', cockpit: '#FFFF00', engine: '#FFFFFF', name: '故障体', tier: 'epic', glow: '#FF00FF', shape: 'glitch' },
  taishui:  { body: '#CD5C5C', wing: '#8B0000', cockpit: '#F08080', engine: '#DC143C', name: '太岁', tier: 'epic', glow: '#DC143C', shape: 'taishui' },
  neonrider:{ body: '#1A1A2E', wing: '#FF0080', cockpit: '#00FFFF', engine: '#8000FF', name: '霓虹骑士', tier: 'legendary', glow: '#FF0080', shape: 'neonrider', trail: '#00FF80' },
}
const ENEMY_SKINS = {
  default:  { normal: '#dc2626', small: '#f87171', fast: '#fbbf24', splitter: '#a78bfa', medium: '#ef4444', tanker: '#64748b', healer: '#22c55e', dasher: '#f97316', name: '默认', tier: 'basic' },
  elite:    { normal: '#7c3aed', small: '#a78bfa', fast: '#fbbf24', splitter: '#c084fc', medium: '#8b5cf6', tanker: '#64748b', healer: '#22c55e', dasher: '#f97316', name: '精英', tier: 'rare' },
  neon:     { normal: '#06b6d4', small: '#22d3ee', fast: '#2dd4bf', splitter: '#34d399', medium: '#0891b2', tanker: '#64748b', healer: '#22c55e', dasher: '#f97316', name: '霓虹', tier: 'rare' },
  inferno:  { normal: '#ea580c', small: '#fb923c', fast: '#fbbf24', splitter: '#f97316', medium: '#c2410c', tanker: '#64748b', healer: '#22c55e', dasher: '#f97316', name: '地狱', tier: 'epic' },
  void:     { normal: '#581c87', small: '#7e22ce', fast: '#a855f7', splitter: '#c084fc', medium: '#6b21a8', tanker: '#64748b', healer: '#22c55e', dasher: '#f97316', name: '虚空', tier: 'epic' },
  dice:     { normal: '#FFFFF0', small: '#FFFFF0', fast: '#FFFFF0', splitter: '#FFFFF0', medium: '#FFFFF0', name: '骰子·厄运', tier: 'rare', shape: 'dice' },
  prion:    { normal: '#E6E6FA', small: '#E6E6FA', fast: '#E6E6FA', splitter: '#E6E6FA', medium: '#E6E6FA', name: '朊病毒', tier: 'rare', shape: 'prion' },
  coin:     { normal: '#8B7355', small: '#8B7355', fast: '#8B7355', splitter: '#8B7355', medium: '#8B7355', name: '古钱·凶泉', tier: 'epic', shape: 'coin' },
  droplet:  { normal: '#C0C0C0', small: '#C0C0C0', fast: '#C0C0C0', splitter: '#C0C0C0', medium: '#C0C0C0', name: '水滴', tier: 'epic', shape: 'droplet' },
  jackbox:  { normal: '#FF4500', small: '#FF4500', fast: '#FF4500', splitter: '#FF4500', medium: '#FF4500', name: '小丑匣', tier: 'legendary', shape: 'jackbox' },
  stamp:    { normal: '#F5F5DC', small: '#F5F5DC', fast: '#F5F5DC', splitter: '#F5F5DC', medium: '#F5F5DC', name: '邮票·远征', tier: 'legendary', shape: 'stamp' },
}
const BULLET_STYLES = {
  default:  { main: '#4ade80', highlight: '#86efac', shape: 'rect', name: '默认', tier: 'basic' },
  plasma:   { main: '#3b82f6', highlight: '#93c5fd', shape: 'rect', name: '等离子', tier: 'basic', glow: '#60a5fa' },
  fire:     { main: '#f97316', highlight: '#fdba74', shape: 'triangle', name: '火焰', tier: 'rare', glow: '#fb923c' },
  ice:      { main: '#e2e8f0', highlight: '#f8fafc', shape: 'diamond', name: '冰霜', tier: 'rare', glow: '#67e8f9' },
  void:     { main: '#a855f7', highlight: '#c084fc', shape: 'rect', name: '虚空', tier: 'epic', glow: '#7e22ce' },
  laser:    { main: '#ef4444', highlight: '#fca5a5', shape: 'laser', name: '激光', tier: 'epic', glow: '#f43f5e' },
  nebula:   { main: '#ec4899', highlight: '#f9a8d4', shape: 'star', name: '星云', tier: 'legendary', glow: '#f472b6' },
  sword:    { main: '#8B4513', highlight: '#A0522D', shape: 'sword', name: '飞剑·青霜', tier: 'legendary', glow: '#00FF7F' },
  note:     { main: '#FF0000', highlight: '#FFD700', shape: 'note', name: '音符·狂想', tier: 'legendary', glow: '#FF7F00' },
  origami:  { main: '#87CEEB', highlight: '#FFB6C1', shape: 'origami', name: '折纸', tier: 'epic', glow: '#F0E68C' },
  dna:      { main: '#FF0000', highlight: '#0000FF', shape: 'dna', name: 'DNA链', tier: 'legendary', glow: '#00FF00' },
  mobius:   { main: '#FFD700', highlight: '#9370DB', shape: 'mobius', name: '莫比乌斯环', tier: 'legendary', glow: '#FFD700' },
  stampbullet: { main: '#8B4513', highlight: '#F5F5DC', shape: 'stampbullet', name: '邮票齿孔', tier: 'epic', glow: '#8B0000' },
}
const POWERUP_SKINS = {
  default:  { icon: '■', colors: { spread: '#a78bfa', firerate: '#fbbf24', bomb: '#f43f5e', shield: '#22d3ee', laser: '#f8fafc', homing: '#c084fc', heal: '#22c55e', life: '#f472b6', score: '#fbbf24', double: '#f472b6' }, name: '默认方块', tier: 'basic' },
  crystal:  { icon: '◆', colors: { spread: '#818cf8', firerate: '#fcd34d', bomb: '#fb7185', shield: '#67e8f9', laser: '#e2e8f0', homing: '#a78bfa', heal: '#4ade80', life: '#f9a8d4', score: '#fcd34d', double: '#f9a8d4' }, name: '水晶棱镜', tier: 'rare', glow: '#818cf8' },
  neon:     { icon: '●', colors: { spread: '#c084fc', firerate: '#fde68a', bomb: '#fda4af', shield: '#a5f3fc', laser: '#f1f5f9', homing: '#d8b4fe', heal: '#86efac', life: '#fbcfe8', score: '#fde68a', double: '#fbcfe8' }, name: '霓虹光球', tier: 'rare', glow: '#c084fc' },
  flame:    { icon: '🔥', colors: { spread: '#c084fc', firerate: '#fbbf24', bomb: '#ef4444', shield: '#fb923c', laser: '#fef3c7', homing: '#a855f7', heal: '#22c55e', life: '#f87171', score: '#f59e0b', double: '#f87171' }, name: '烈焰之心', tier: 'epic', glow: '#ef4444' },
  ice:      { icon: '❄', colors: { spread: '#93c5fd', firerate: '#fef9c3', bomb: '#bae6fd', shield: '#7dd3fc', laser: '#ffffff', homing: '#a5b4fc', heal: '#6ee7b7', life: '#c7d2fe', score: '#fef9c3', double: '#c7d2fe' }, name: '极寒冰晶', tier: 'epic', glow: '#7dd3fc' },
  galaxy:   { icon: '✦', colors: { spread: '#a78bfa', firerate: '#fcd34d', bomb: '#f472b6', shield: '#67e8f9', laser: '#f8fafc', homing: '#c084fc', heal: '#34d399', life: '#fb7185', score: '#fbbf24', double: '#fb7185' }, name: '星云碎片', tier: 'legendary', glow: '#a78bfa' },
}

function getPlayerSkin() {
  const id = shooterCosmetics.value?.equippedPlayerSkin || 'default'
  return PLAYER_SKINS[id] || PLAYER_SKINS.default
}
function getEnemySkin() {
  const id = shooterCosmetics.value?.equippedEnemySkin || 'default'
  return ENEMY_SKINS[id] || ENEMY_SKINS.default
}
function darkenColor(hex, factor) {
  const r = parseInt(hex.slice(1, 3), 16), g = parseInt(hex.slice(3, 5), 16), b = parseInt(hex.slice(5, 7), 16)
  return '#' + [r, g, b].map(c => Math.round(c * factor).toString(16).padStart(2, '0')).join('')
}
function lightenColor(hex, factor) {
  const r = Math.min(255, Math.round(parseInt(hex.slice(1, 3), 16) * factor))
  const g = Math.min(255, Math.round(parseInt(hex.slice(3, 5), 16) * factor))
  const b = Math.min(255, Math.round(parseInt(hex.slice(5, 7), 16) * factor))
  return '#' + [r, g, b].map(c => c.toString(16).padStart(2, '0')).join('')
}
function hexToRgba(hex, alpha) {
  const r = parseInt(hex.slice(1, 3), 16), g = parseInt(hex.slice(3, 5), 16), b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r},${g},${b},${alpha})`
}
function getBulletStyle() {
  const id = shooterCosmetics.value?.equippedBulletStyle || 'default'
  return BULLET_STYLES[id] || BULLET_STYLES.default
}
function getPowerupSkin() {
  const id = shooterCosmetics.value?.equippedPowerupSkin || 'default'
  return POWERUP_SKINS[id] || POWERUP_SKINS.default
}

// Shop preview canvas directive
const vPreviewCanvas = {
  mounted(el, binding) {
    renderPreview(el, binding.value)
  },
  updated(el, binding) {
    renderPreview(el, binding.value)
  }
}

function renderPreview(canvas, { type, id }) {
  const ctx = canvas.getContext('2d')
  const w = canvas.width, h = canvas.height
  ctx.clearRect(0, 0, w, h)
  const cx = w / 2, cy = h / 2

  if (type === 'player') {
    const skin = PLAYER_SKINS[id]
    if (!skin) return
    ctx.save()
    ctx.translate(cx, cy)
    ctx.scale(1.4, 1.4)
    drawPlayerPreview(ctx, skin)
    ctx.restore()
  } else if (type === 'enemy') {
    const skin = ENEMY_SKINS[id]
    if (!skin) return
    ctx.save()
    ctx.translate(cx, cy)
    ctx.scale(1.6, 1.6)
    drawEnemyPreview(ctx, skin)
    ctx.restore()
  } else if (type === 'bullet') {
    const style = BULLET_STYLES[id]
    if (!style) return
    ctx.save()
    ctx.translate(cx, cy)
    ctx.scale(1.8, 1.8)
    drawBulletPreview(ctx, style)
    ctx.restore()
  } else if (type === 'powerup') {
    const skin = POWERUP_SKINS[id]
    if (!skin) return
    drawPowerupPreview(ctx, skin, cx, cy)
  }
}

function drawPlayerPreview(ctx, skin) {
  const shape = skin.shape || 'standard'
  if (shape === 'standard') {
    ctx.fillStyle = skin.body
    ctx.beginPath()
    ctx.moveTo(0, -16)
    ctx.lineTo(-10, 4)
    ctx.lineTo(-6, 16)
    ctx.lineTo(6, 16)
    ctx.lineTo(10, 4)
    ctx.closePath()
    ctx.fill()
    ctx.fillStyle = skin.wing
    ctx.beginPath()
    ctx.moveTo(-4, 2); ctx.lineTo(-14, 16); ctx.lineTo(-2, 16); ctx.closePath(); ctx.fill()
    ctx.beginPath()
    ctx.moveTo(4, 2); ctx.lineTo(14, 16); ctx.lineTo(2, 16); ctx.closePath(); ctx.fill()
  } else if (shape === 'swept') {
    ctx.fillStyle = skin.body
    ctx.beginPath()
    ctx.moveTo(0, -16)
    ctx.lineTo(-14, 6)
    ctx.lineTo(-8, 16)
    ctx.lineTo(8, 16)
    ctx.lineTo(14, 6)
    ctx.closePath()
    ctx.fill()
    ctx.fillStyle = skin.wing
    ctx.beginPath()
    ctx.moveTo(-6, 0); ctx.lineTo(-18, 16); ctx.lineTo(-4, 16); ctx.closePath(); ctx.fill()
    ctx.beginPath()
    ctx.moveTo(6, 0); ctx.lineTo(18, 16); ctx.lineTo(4, 16); ctx.closePath(); ctx.fill()
  } else if (shape === 'arrow') {
    ctx.fillStyle = skin.body
    ctx.beginPath()
    ctx.moveTo(0, -18)
    ctx.lineTo(-6, 6)
    ctx.lineTo(0, 16)
    ctx.lineTo(6, 6)
    ctx.closePath()
    ctx.fill()
    ctx.fillStyle = skin.wing
    ctx.beginPath()
    ctx.moveTo(-4, 4); ctx.lineTo(-16, 16); ctx.lineTo(-2, 16); ctx.closePath(); ctx.fill()
    ctx.beginPath()
    ctx.moveTo(4, 4); ctx.lineTo(16, 16); ctx.lineTo(2, 16); ctx.closePath(); ctx.fill()
  } else if (shape === 'dragon') {
    ctx.fillStyle = skin.body
    ctx.beginPath()
    ctx.moveTo(0, -18)
    ctx.quadraticCurveTo(-6, -4, -8, 8)
    ctx.lineTo(-4, 16)
    ctx.lineTo(4, 16)
    ctx.quadraticCurveTo(6, -4, 8, -4)
    ctx.closePath()
    ctx.fill()
    ctx.fillStyle = skin.wing
    ctx.beginPath()
    ctx.moveTo(-2, -2); ctx.quadraticCurveTo(-18, -4, -12, 8); ctx.lineTo(0, 6); ctx.closePath(); ctx.fill()
    ctx.beginPath()
    ctx.moveTo(2, -2); ctx.quadraticCurveTo(18, -4, 12, 8); ctx.lineTo(0, 6); ctx.closePath(); ctx.fill()
  } else if (shape === 'galaxy') {
    ctx.fillStyle = skin.body
    ctx.beginPath()
    ctx.moveTo(0, -16); ctx.lineTo(-8, -2); ctx.lineTo(-12, 6); ctx.lineTo(-6, 16)
    ctx.lineTo(6, 16); ctx.lineTo(12, 6); ctx.lineTo(8, -2); ctx.closePath()
    ctx.fill()
    ctx.fillStyle = skin.wing
    ctx.globalAlpha = 0.7
    ctx.beginPath()
    ctx.moveTo(2, -4); ctx.lineTo(-18, 2); ctx.lineTo(-6, 14); ctx.closePath(); ctx.fill()
    ctx.beginPath()
    ctx.moveTo(-2, -4); ctx.lineTo(18, 2); ctx.lineTo(6, 14); ctx.closePath(); ctx.fill()
    ctx.globalAlpha = 1
  } else if (shape === 'titan') {
    ctx.fillStyle = skin.body
    ctx.beginPath()
    ctx.moveTo(0, -16); ctx.lineTo(-8, -4); ctx.lineTo(-12, 6); ctx.lineTo(-8, 16)
    ctx.lineTo(8, 16); ctx.lineTo(12, 6); ctx.lineTo(8, -4); ctx.closePath()
    ctx.fill()
    ctx.fillStyle = skin.wing
    ctx.fillRect(-16, -2, 8, 14)
    ctx.fillRect(8, -2, 8, 14)
  } else if (shape === 'devourer') {
    // Accretion disk fragments
    const colors = ['#FF6B35', '#FFB627', '#E0E1DD']
    for (let layer = 0; layer < 3; layer++) {
      const r = 10 + layer * 5
      const fragCount = 6 + layer * 2
      ctx.fillStyle = colors[layer]
      ctx.globalAlpha = 0.7 - layer * 0.15
      for (let i = 0; i < fragCount; i++) {
        const a = (Math.PI * 2 / fragCount) * i + layer * 0.5
        const fx = Math.cos(a) * r, fy = Math.sin(a) * r * 0.5
        const fLen = 4 + Math.sin(i * 2.7) * 2
        ctx.save()
        ctx.translate(fx, fy)
        ctx.rotate(a + Math.PI / 4)
        ctx.fillRect(-fLen / 2, -1.5, fLen, 3)
        ctx.restore()
      }
    }
    ctx.globalAlpha = 1
    // Central void
    const vg = ctx.createRadialGradient(0, 0, 0, 0, 0, 8)
    vg.addColorStop(0, '#0A0A0F'); vg.addColorStop(0.6, '#0A0A0F'); vg.addColorStop(1, 'rgba(74,25,66,0.4)')
    ctx.fillStyle = vg
    ctx.beginPath(); ctx.arc(0, 0, 8, 0, Math.PI * 2); ctx.fill()
    ctx.strokeStyle = 'rgba(74,25,66,0.3)'; ctx.lineWidth = 3; ctx.beginPath(); ctx.arc(0, 0, 11, 0, Math.PI * 2); ctx.stroke()
  } else if (shape === 'kite') {
    // Wings
    ctx.fillStyle = skin.wing
    ctx.globalAlpha = 0.6
    ctx.beginPath(); ctx.moveTo(0, -14); ctx.quadraticCurveTo(-16, -2, -12, 10); ctx.lineTo(0, 6); ctx.closePath(); ctx.fill()
    ctx.beginPath(); ctx.moveTo(0, -14); ctx.quadraticCurveTo(16, -2, 12, 10); ctx.lineTo(0, 6); ctx.closePath(); ctx.fill()
    ctx.globalAlpha = 1
    // Bamboo skeleton
    ctx.strokeStyle = skin.body; ctx.lineWidth = 1.5
    ctx.beginPath(); ctx.moveTo(0, -14); ctx.lineTo(0, 12); ctx.stroke()
    ctx.beginPath(); ctx.moveTo(-10, 0); ctx.lineTo(10, 0); ctx.stroke()
    // Central hub
    ctx.fillStyle = skin.cockpit
    ctx.beginPath(); ctx.arc(0, -2, 3, 0, Math.PI * 2); ctx.fill()
  } else if (shape === 'jellyfish') {
    // Dome (static preview)
    ctx.fillStyle = skin.body
    ctx.globalAlpha = 0.6
    ctx.beginPath()
    ctx.ellipse(0, -4, 14, 10, 0, Math.PI, 0)
    ctx.quadraticCurveTo(14, 4, 10, 8)
    ctx.lineTo(-10, 8)
    ctx.quadraticCurveTo(-14, 4, -14, -4)
    ctx.fill()
    ctx.globalAlpha = 1
    // Inner organs
    ctx.fillStyle = skin.cockpit
    ctx.globalAlpha = 0.5
    ctx.beginPath(); ctx.ellipse(0, -4, 8, 6, 0, 0, Math.PI * 2); ctx.fill()
    ctx.globalAlpha = 1
    // Tentacles
    ctx.strokeStyle = skin.wing; ctx.lineWidth = 1.5; ctx.globalAlpha = 0.6
    for (let t = 0; t < 4; t++) {
      ctx.beginPath()
      ctx.moveTo(-6 + t * 4, 8)
      ctx.quadraticCurveTo(-6 + t * 4 + Math.sin(t * 1.2) * 3, 14, -6 + t * 4, 20)
      ctx.stroke()
    }
    ctx.globalAlpha = 1
  } else if (shape === 'glitch') {
    // Pixel blocks
    ctx.fillStyle = skin.body
    ctx.fillRect(-8, -10, 6, 8)
    ctx.fillRect(-2, -14, 4, 12)
    ctx.fillRect(2, -8, 8, 6)
    ctx.fillRect(-6, -2, 12, 4)
    ctx.fillRect(-4, 4, 8, 8)
    // RGB separation
    ctx.globalAlpha = 0.4
    ctx.fillStyle = '#FF0000'; ctx.fillRect(-10, -12, 5, 4)
    ctx.fillStyle = '#00FF00'; ctx.fillRect(4, 0, 5, 4)
    ctx.fillStyle = '#0000FF'; ctx.fillRect(-6, 6, 5, 4)
    ctx.globalAlpha = 1
    // Scanlines
    ctx.fillStyle = 'rgba(255,255,255,0.15)'
    for (let i = 0; i < 4; i++) ctx.fillRect(-12, -12 + i * 7, 24, 1)
  } else if (shape === 'taishui') {
    // Flesh body
    ctx.fillStyle = skin.body
    ctx.beginPath()
    ctx.ellipse(0, 0, 12, 14, 0, 0, Math.PI * 2)
    ctx.fill()
    // Bone plates
    ctx.fillStyle = '#D2B48C'
    ctx.beginPath(); ctx.ellipse(-6, -8, 4, 3, -0.3, 0, Math.PI * 2); ctx.fill()
    ctx.beginPath(); ctx.ellipse(5, -6, 3, 4, 0.2, 0, Math.PI * 2); ctx.fill()
    // Eye
    ctx.fillStyle = '#FFFF00'
    ctx.beginPath(); ctx.arc(0, -2, 3, 0, Math.PI * 2); ctx.fill()
    ctx.fillStyle = '#000'
    ctx.beginPath(); ctx.arc(0, -2, 1.5, 0, Math.PI * 2); ctx.fill()
    // Tentacles
    ctx.strokeStyle = skin.wing; ctx.lineWidth = 2
    for (let t = 0; t < 5; t++) {
      const ta = (Math.PI / 4) * (t - 2)
      ctx.beginPath()
      ctx.moveTo(Math.cos(ta) * 10, 8 + Math.sin(ta) * 2)
      ctx.quadraticCurveTo(Math.cos(ta) * 16, 16, Math.cos(ta) * 12, 22)
      ctx.stroke()
    }
  } else if (shape === 'neonrider') {
    // Motorcycle body
    ctx.fillStyle = skin.body
    ctx.beginPath()
    ctx.moveTo(-14, 4); ctx.lineTo(-10, -4); ctx.lineTo(10, -4); ctx.lineTo(14, 4)
    ctx.lineTo(10, 8); ctx.lineTo(-10, 8); ctx.closePath()
    ctx.fill()
    // Neon tubes
    ctx.strokeStyle = skin.wing; ctx.lineWidth = 2; ctx.shadowColor = skin.wing; ctx.shadowBlur = 4
    ctx.beginPath(); ctx.moveTo(-12, 2); ctx.lineTo(12, 2); ctx.stroke()
    ctx.beginPath(); ctx.moveTo(-8, -2); ctx.lineTo(8, -2); ctx.stroke()
    ctx.shadowBlur = 0
    // Wheels
    ctx.fillStyle = '#333'
    ctx.beginPath(); ctx.arc(-10, 10, 4, 0, Math.PI * 2); ctx.fill()
    ctx.beginPath(); ctx.arc(10, 10, 4, 0, Math.PI * 2); ctx.fill()
    ctx.strokeStyle = skin.cockpit; ctx.lineWidth = 1
    ctx.beginPath(); ctx.arc(-10, 10, 4, 0, Math.PI * 2); ctx.stroke()
    ctx.beginPath(); ctx.arc(10, 10, 4, 0, Math.PI * 2); ctx.stroke()
    // Rider silhouette
    ctx.fillStyle = skin.cockpit
    ctx.globalAlpha = 0.6
    ctx.beginPath(); ctx.arc(0, -8, 4, 0, Math.PI * 2); ctx.fill()
    ctx.fillRect(-3, -4, 6, 6)
    ctx.globalAlpha = 1
  }
  // Cockpit (common shapes)
  if (['standard', 'swept', 'arrow', 'dragon', 'galaxy', 'titan'].includes(shape)) {
    ctx.fillStyle = skin.cockpit
    ctx.beginPath(); ctx.arc(0, -4, 3, 0, Math.PI * 2); ctx.fill()
  }
}

function drawEnemyPreview(ctx, skin) {
  const shape = skin.shape || 'default'
  if (shape === 'default') {
    ctx.fillStyle = skin.normal
    ctx.beginPath()
    ctx.moveTo(0, -12); ctx.lineTo(-12, -2); ctx.lineTo(-6, 12); ctx.lineTo(6, 12); ctx.lineTo(12, -2)
    ctx.closePath(); ctx.fill()
  } else if (shape === 'dice') {
    const s = 10
    ctx.fillStyle = '#FFFFF0'
    ctx.fillRect(-s, -s, s * 2, s * 2)
    ctx.strokeStyle = '#DAA520'; ctx.lineWidth = 1.5
    ctx.strokeRect(-s, -s, s * 2, s * 2)
    ctx.fillStyle = '#2F2F2F'
    ctx.font = 'bold 10px serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
    ctx.fillText('☠', 0, 0)
  } else if (shape === 'prion') {
    const r = 11
    ctx.fillStyle = 'rgba(230,230,250,0.7)'
    ctx.beginPath(); ctx.arc(0, 0, r, 0, Math.PI * 2); ctx.fill()
    ctx.fillStyle = '#8B0000'
    ctx.beginPath(); ctx.arc(0, 0, r * 0.35, 0, Math.PI * 2); ctx.fill()
    ctx.strokeStyle = '#DC143C'; ctx.lineWidth = 1.2
    ctx.beginPath()
    for (let i = 0; i < 12; i++) {
      const a = (Math.PI * 2 / 12) * i
      ctx.moveTo(Math.cos(a) * r * 0.7, Math.sin(a) * r * 0.7)
      ctx.lineTo(Math.cos(a) * r, Math.sin(a) * r)
    }
    ctx.stroke()
  } else if (shape === 'coin') {
    const r = 11
    ctx.fillStyle = '#8B7355'
    ctx.beginPath(); ctx.arc(0, 0, r, 0, Math.PI * 2); ctx.fill()
    ctx.fillStyle = '#1A1A1A'
    const hs = 4
    ctx.fillRect(-hs / 2, -hs / 2, hs, hs)
    ctx.fillStyle = '#4B0082'; ctx.globalAlpha = 0.6
    ctx.font = '3px serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
    for (let i = 0; i < 8; i++) {
      const a = (Math.PI * 2 / 8) * i
      ctx.fillText('咒', Math.cos(a) * r * 0.72, Math.sin(a) * r * 0.72)
    }
    ctx.globalAlpha = 1
  } else if (shape === 'droplet') {
    const r = 11
    const grad = ctx.createRadialGradient(-3, -3, 0, 0, 0, r)
    grad.addColorStop(0, '#FFFFFF'); grad.addColorStop(0.4, '#C0C0C0'); grad.addColorStop(0.8, '#696969'); grad.addColorStop(1, '#A0A0A0')
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.moveTo(0, -r * 1.2)
    ctx.quadraticCurveTo(r, -r * 0.3, r * 0.7, r * 0.5)
    ctx.quadraticCurveTo(0, r * 1.2, -r * 0.7, r * 0.5)
    ctx.quadraticCurveTo(-r, -r * 0.3, 0, -r * 1.2)
    ctx.fill()
    ctx.fillStyle = 'rgba(255,255,255,0.6)'
    ctx.beginPath(); ctx.ellipse(-2, -3, 1.5, 2.5, -0.5, 0, Math.PI * 2); ctx.fill()
  } else if (shape === 'jackbox') {
    for (let i = 0; i < 4; i++) {
      ctx.fillStyle = i % 2 === 0 ? '#FF4500' : '#FFD700'
      ctx.fillRect(-10, -10 + 5 * i, 20, 5)
    }
    ctx.strokeStyle = '#C0C0C0'; ctx.lineWidth = 2
    ctx.beginPath()
    for (let i = 0; i < 4; i++) {
      ctx.lineTo(-4 + (i % 2 === 0 ? 0 : 8), -14 - i * 3)
    }
    ctx.stroke()
    ctx.fillStyle = '#808080'
    ctx.beginPath(); ctx.arc(2, -20, 3, 0, Math.PI * 2); ctx.fill()
  } else if (shape === 'stamp') {
    ctx.save()
    ctx.fillStyle = '#F5F5DC'
    ctx.fillRect(-10, -10, 20, 20)
    ctx.fillStyle = '#8B4513'
    for (let i = 0; i < 4; i++) {
      ctx.beginPath(); ctx.arc(-7 + i * 5, -10, 1.5, 0, Math.PI * 2); ctx.fill()
      ctx.beginPath(); ctx.arc(-7 + i * 5, 10, 1.5, 0, Math.PI * 2); ctx.fill()
    }
    ctx.fillStyle = '#8B7355'
    ctx.font = '8px serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
    ctx.fillText('✈', 0, 0)
    ctx.strokeStyle = '#8B0000'; ctx.lineWidth = 1
    ctx.beginPath(); ctx.arc(5, 5, 4, 0, Math.PI * 2); ctx.stroke()
    ctx.restore()
  }
}

function drawBulletPreview(ctx, style) {
  const shape = style.shape || 'rect'
  ctx.fillStyle = style.main
  ctx.strokeStyle = style.highlight
  if (shape === 'rect') {
    ctx.fillRect(-2, -8, 4, 16)
    ctx.fillStyle = style.highlight; ctx.fillRect(-1, -8, 2, 6)
  } else if (shape === 'triangle') {
    ctx.beginPath(); ctx.moveTo(0, -8); ctx.lineTo(-4, 8); ctx.lineTo(4, 8); ctx.closePath(); ctx.fill()
  } else if (shape === 'diamond') {
    ctx.beginPath(); ctx.moveTo(0, -8); ctx.lineTo(5, 0); ctx.lineTo(0, 8); ctx.lineTo(-5, 0); ctx.closePath(); ctx.fill()
  } else if (shape === 'laser') {
    ctx.fillRect(-1.5, -12, 3, 24)
    ctx.fillStyle = style.highlight; ctx.globalAlpha = 0.5; ctx.fillRect(-3, -10, 6, 20); ctx.globalAlpha = 1
  } else if (shape === 'star') {
    ctx.beginPath()
    for (let i = 0; i < 5; i++) {
      const a = (Math.PI * 2 / 5) * i - Math.PI / 2
      const r = i % 2 === 0 ? 8 : 4
      ctx[i === 0 ? 'moveTo' : 'lineTo'](Math.cos(a) * r, Math.sin(a) * r)
    }
    ctx.closePath(); ctx.fill()
  } else if (shape === 'sword') {
    ctx.beginPath(); ctx.moveTo(0, -10); ctx.lineTo(-2, 2); ctx.lineTo(2, 2); ctx.closePath(); ctx.fill()
    ctx.fillRect(-2, 2, 4, 2)
    ctx.fillRect(-5, 4, 10, 2)
    ctx.fillRect(-1, 6, 2, 4)
  } else if (shape === 'note') {
    ctx.beginPath(); ctx.arc(0, 4, 4, 0, Math.PI * 2); ctx.fill()
    ctx.fillRect(3, -10, 2, 14)
    ctx.fillStyle = style.highlight
    ctx.beginPath(); ctx.moveTo(5, -10); ctx.quadraticCurveTo(10, -6, 5, -4); ctx.fill()
  } else if (shape === 'origami') {
    ctx.beginPath(); ctx.moveTo(0, -8); ctx.lineTo(-6, 0); ctx.lineTo(0, -2); ctx.lineTo(6, 0); ctx.closePath(); ctx.fill()
    ctx.fillStyle = style.highlight; ctx.globalAlpha = 0.6
    ctx.beginPath(); ctx.moveTo(0, -2); ctx.lineTo(-4, 6); ctx.lineTo(0, 4); ctx.lineTo(4, 6); ctx.closePath(); ctx.fill()
    ctx.globalAlpha = 1
  } else if (shape === 'dna') {
    ctx.lineWidth = 2
    ctx.strokeStyle = style.main; ctx.beginPath()
    for (let i = -6; i <= 6; i += 2) {
      ctx.lineTo(Math.sin(i * 0.8) * 4, i)
    } ctx.stroke()
    ctx.strokeStyle = style.highlight; ctx.beginPath()
    for (let i = -6; i <= 6; i += 2) {
      ctx.lineTo(-Math.sin(i * 0.8) * 4, i)
    } ctx.stroke()
    ctx.strokeStyle = '#fff'; ctx.lineWidth = 0.8
    for (let i = -4; i <= 4; i += 4) {
      ctx.beginPath(); ctx.moveTo(Math.sin(i * 0.8) * 4, i); ctx.lineTo(-Math.sin(i * 0.8) * 4, i); ctx.stroke()
    }
  } else if (shape === 'mobius') {
    ctx.lineWidth = 2.5
    const grad = ctx.createLinearGradient(-6, 0, 6, 0)
    grad.addColorStop(0, style.main); grad.addColorStop(0.5, style.highlight); grad.addColorStop(1, style.main)
    ctx.strokeStyle = grad
    ctx.beginPath()
    for (let t = 0; t <= Math.PI * 2; t += 0.1) {
      const r = 5 + 2 * Math.sin(t * 2)
      ctx.lineTo(Math.cos(t) * r, Math.sin(t) * r * 0.6)
    }
    ctx.closePath(); ctx.stroke()
    ctx.fillStyle = '#fff'; ctx.beginPath(); ctx.arc(3, -1, 1.5, 0, Math.PI * 2); ctx.fill()
    ctx.beginPath(); ctx.arc(-3, 1, 1.5, 0, Math.PI * 2); ctx.fill()
  } else if (shape === 'stampbullet') {
    ctx.fillStyle = '#F5F5DC'
    ctx.fillRect(-6, -8, 12, 16)
    ctx.fillStyle = '#8B4513'
    for (let i = 0; i < 3; i++) {
      ctx.beginPath(); ctx.arc(-4 + i * 4, -8, 1.2, 0, Math.PI * 2); ctx.fill()
      ctx.beginPath(); ctx.arc(-4 + i * 4, 8, 1.2, 0, Math.PI * 2); ctx.fill()
    }
    ctx.fillStyle = style.main
    ctx.font = '6px serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
    ctx.fillText('✉', 0, 0)
  }
}

function drawPowerupPreview(ctx, skin, cx, cy) {
  const sampleColors = Object.values(skin.colors)
  const mainColor = sampleColors[0] || '#a78bfa'
  if (skin.glow) {
    ctx.shadowColor = skin.glow; ctx.shadowBlur = 10
  }
  ctx.fillStyle = mainColor
  if (skin.icon === '◆') {
    ctx.beginPath()
    ctx.moveTo(cx, cy - 20); ctx.lineTo(cx + 16, cy)
    ctx.lineTo(cx, cy + 20); ctx.lineTo(cx - 16, cy)
    ctx.closePath(); ctx.fill()
  } else if (skin.icon === '●') {
    ctx.beginPath(); ctx.arc(cx, cy, 16, 0, Math.PI * 2); ctx.fill()
    ctx.strokeStyle = lightenColor(mainColor, 1.5); ctx.lineWidth = 2
    ctx.beginPath(); ctx.arc(cx, cy, 19, 0, Math.PI * 2); ctx.stroke()
  } else if (skin.icon === '🔥' || skin.icon === '❄' || skin.icon === '✦') {
    ctx.beginPath()
    for (let i = 0; i < 6; i++) {
      const a = Math.PI / 3 * i - Math.PI / 2
      const method = i === 0 ? 'moveTo' : 'lineTo'
      ctx[method](cx + Math.cos(a) * 16, cy + Math.sin(a) * 16)
    }
    ctx.closePath(); ctx.fill()
  } else {
    ctx.beginPath(); ctx.roundRect(cx - 14, cy - 14, 28, 28, 4); ctx.fill()
  }
  ctx.shadowBlur = 0
  ctx.fillStyle = '#fff'; ctx.font = 'bold 14px sans-serif'
  ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
  ctx.fillText(skin.icon, cx, cy)
}

function initShooter() {
  shooterPlayer = { x: canvasWidth / 2 - SHOOTER_PLAYER_W / 2, y: canvasHeight.value - SHOOTER_PLAYER_H - 20 }
  shooterBullets = []
  shooterEnemies = []
  shooterEnemyBullets = []
  shooterParticles = []
  shooterPowerups = []
  shooterScorePopups = []
  shooterBoss = null
  shooterBossDefeated = null
  shooterTimer = 0
  shooterLevel = 1
  shooterLevelTimer = 0
  shooterFireTimer = 0
  shooterLeft = false
  shooterRight = false
  shooterLives = 3
  shooterMaxLives = 3
  shooterInvincible = false
  shooterInvincibleTimer = 0
  shooterPowerupType = null
  shooterPowerupTimer = 0
  shooterCombo = 0
  shooterMaxCombo.value = 0
  shooterComboTimer = 0
  shooterFlashTimer = 0
  shooterShield = false
  shooterLaser = false
  shooterLaserTimer = 0
  shooterHoming = false
  shooterHomingTimer = 0
  shooterScreenShake = 0
  shooterRevivalUsed = false
  shooterRevivalDialog.value = false
  shooterActiveSkillCooldownTimer = 0
  shooterActiveSkillCooldown.value = 0
  shooterActiveSkillActive = false
  shooterActiveSkillDuration = 0
  shooterDoubleScore = false
  shooterDoubleScoreTimer = 0
  shooterSlowEnemy = false
  shooterSlowEnemyTimer = 0
  shooterMagnet = false
  shooterMagnetTimer = 0
  bossFightHit = false
  shooterSkillCooldownTimers = [0, 0, 0]
  shooterSkillCooldowns.value = [0, 0, 0]
  // Init 3-layer stars
  for (let layer = 0; layer < 3; layer++) {
    if (shooterStars[layer].length === 0) {
      const count = [30, 20, 10][layer]
      for (let i = 0; i < count; i++) {
        shooterStars[layer].push({
          x: Math.random() * canvasWidth,
          y: Math.random() * canvasHeight.value,
          speed: [20, 50, 100][layer] + Math.random() * 20,
          size: [0.8, 1.2, 2][layer] + Math.random() * 0.5
        })
      }
    }
  }
}

function shooterFire() {
  if (gameState.value !== 'playing') return
  const cx = shooterPlayer.x + SHOOTER_PLAYER_W / 2
  const ty = shooterPlayer.y - 4
  if (shooterPowerupType === 'spread') {
    shooterBullets.push({ x: cx - 2, y: ty, vx: -60 })
    shooterBullets.push({ x: cx - 2, y: ty, vx: 0 })
    shooterBullets.push({ x: cx - 2, y: ty, vx: 60 })
  } else {
    shooterBullets.push({ x: cx - 2, y: ty, vx: 0 })
  }
  SFX.play('shoot')
}

function spawnShooterEnemy() {
  if (shooterBoss) return
  const r = Math.random()
  let type, w, hp, speed, points
  // Speed scales with level but caps at level 10 to prevent unplayable pace
  const spdMul = Math.min(shooterLevel, 10)
  if (r < Math.max(0.05, 0.40 - shooterLevel * 0.015)) {
    type = 'small'; w = 22; hp = 1; speed = 130 + spdMul * 10; points = 10
  } else if (r < Math.max(0.10, 0.55 - shooterLevel * 0.01)) {
    type = 'fast'; w = 18; hp = 1 + Math.floor(shooterLevel / 8); speed = 260 + spdMul * 12; points = 15
  } else if (r < 0.72) {
    type = 'medium'; w = 34; hp = 3 + Math.floor(shooterLevel / 5); speed = 90 + spdMul * 6; points = 30
  } else if (r < 0.87) {
    type = 'large'; w = 48; hp = 5 + Math.floor(shooterLevel / 4); speed = 60 + spdMul * 5; points = 50
  } else if (r < 0.92) {
    type = 'splitter'; w = 28; hp = 2 + Math.floor(shooterLevel / 5); speed = 80 + spdMul * 6; points = 25
  } else if (r < 0.96) {
    type = 'tanker'; w = 42; hp = 8 + Math.floor(shooterLevel / 3); speed = 45 + spdMul * 4; points = 40
  } else {
    type = 'healer'; w = 26; hp = 2 + Math.floor(shooterLevel / 6); speed = 70 + spdMul * 5; points = 20
  }
  const newEnemy = {
    x: Math.random() * (canvasWidth - w), y: -w,
    w, h: w, hp, maxHp: hp, speed, type, points,
    shootTimer: 1000 + Math.random() * 2000,
    moveDir: (type === 'fast' || type === 'small') ? (Math.random() < 0.3 ? (Math.random() < 0.5 ? 1 : -1) : 0) : (Math.random() < 0.5 ? 1 : -1),
    flash: 0,
    dashTimer: type === 'dasher' ? 2000 + Math.random() * 2000 : 0, dashing: false, dashDir: 0,
    healTimer: type === 'healer' ? 3000 : 0
  }
  shooterEnemies.push(newEnemy)
}

function spawnBoss() {
  const types = ['default', 'zigzag', 'swarm', 'shield', 'teleporter', 'mirror', 'gravity']
  const type = types[Math.floor(Math.random() * types.length)]
  let bossHp, bossSpeed, bossW, bossH
  if (type === 'zigzag') {
    bossHp = 20 + shooterLevel * 3; bossSpeed = 120; bossW = 80; bossH = 40
  } else if (type === 'swarm') {
    bossHp = 16 + shooterLevel * 2.5; bossSpeed = 50; bossW = 60; bossH = 55
  } else if (type === 'shield') {
    bossHp = 25 + shooterLevel * 4; bossSpeed = 40; bossW = 70; bossH = 55
  } else if (type === 'teleporter') {
    bossHp = 22 + shooterLevel * 3.5; bossSpeed = 70; bossW = 65; bossH = 50
  } else if (type === 'mirror') {
    bossHp = 30 + shooterLevel * 4; bossSpeed = 50; bossW = 70; bossH = 55
  } else if (type === 'gravity') {
    bossHp = 28 + shooterLevel * 4; bossSpeed = 40; bossW = 75; bossH = 60
  } else {
    bossHp = 25 + shooterLevel * 5; bossSpeed = 60; bossW = 70; bossH = 50
  }
  shooterBoss = {
    x: canvasWidth / 2 - bossW / 2, y: -bossH - 10,
    w: bossW, h: bossH, hp: bossHp, maxHp: bossHp,
    speed: bossSpeed, moveDir: 1, points: 200,
    shootTimer: 0, phaseTimer: 0, phase: 0,
    flash: 0, type,
    zigzagTimer: 0, zigzagDir: 1,
    swarmTimer: 0,
    shieldActive: type === 'shield',
    teleportTimer: 0, teleporting: false, teleportInvincible: 0,
    mirrorTimer: 0, mirrorClones: null,
    gravityTimer: 0, gravityField: type === 'gravity' ? 120 : 0
  }
  bossFightHit = false
  SFX.play('bossAppear')
}

function spawnPowerup(x, y) {
  const types = ['spread', 'firerate', 'bomb', 'shield', 'laser', 'homing', 'heal', 'life', 'score', 'double']
  const type = types[Math.floor(Math.random() * types.length)]
  shooterPowerups.push({ x: x - 10, y, w: 20, h: 20, type, speed: 100 })
}

function spawnParticles(x, y, color, count) {
  const maxP = 200
  for (let i = 0; i < count && shooterParticles.length < maxP; i++) {
    const angle = Math.random() * Math.PI * 2
    const speed = 40 + Math.random() * 120
    shooterParticles.push({
      x, y, vx: Math.cos(angle) * speed, vy: Math.sin(angle) * speed,
      life: 0.4 + Math.random() * 0.4, maxLife: 0.4 + Math.random() * 0.4,
      size: 2 + Math.random() * 3, color
    })
  }
}

function spawnScorePopup(x, y, text) {
  shooterScorePopups.push({ x, y, text, life: 1.0, maxLife: 1.0 })
}

function shooterHit() {
  if (shooterInvincible) return
  // Shield absorbs one hit
  if (shooterShield) {
    shooterShield = false
    shooterInvincible = true
    shooterInvincibleTimer = 1000
    spawnParticles(shooterPlayer.x + SHOOTER_PLAYER_W / 2, shooterPlayer.y + SHOOTER_PLAYER_H / 2, '#22d3ee', 10)
    return
  }
  shooterLives--
  bossFightHit = true
  achievementStats.value.noHitStreak = 0
  SFX.play('playerHit')
  if (shooterLives <= 0) {
    // Offer revival if not used this level and player has coins
    const coins = shooterCosmetics.value?.coins || 0
    if (!shooterRevivalUsed && coins >= 10) {
      shooterRevivalDialog.value = true
      return
    }
    gameState.value = 'over'
    SFX.stopBGM()
    SFX.play('gameOver')
    saveHighScore()
    return
  }
  shooterInvincible = true
  shooterInvincibleTimer = 2000
  shooterFlashTimer = 200
  spawnParticles(shooterPlayer.x + SHOOTER_PLAYER_W / 2, shooterPlayer.y + SHOOTER_PLAYER_H / 2, '#60a5fa', 15)
  shooterCombo = 0
  shooterComboTimer = 0
}

async function revivePlayer() {
  shooterRevivalDialog.value = false
  shooterRevivalUsed = true
  SFX.play('revive')
  achievementStats.value.revives++
  checkAchievements(achievementStats.value)
  // Deduct coins locally
  if (shooterCosmetics.value) {
    shooterCosmetics.value.coins = Math.max(0, (shooterCosmetics.value.coins || 0) - 10)
    saveCosmetics()
  }
  // Revive with max lives and 3 seconds invincibility
  shooterLives = shooterMaxLives
  shooterInvincible = true
  shooterInvincibleTimer = 3000
  shooterFlashTimer = 3000
  shooterCombo = 0
  shooterComboTimer = 0
  // Re-center player
  shooterPlayer.x = canvasWidth / 2 - SHOOTER_PLAYER_W / 2
  shooterPlayer.y = canvasHeight.value - SHOOTER_PLAYER_H - 20
  spawnParticles(shooterPlayer.x + SHOOTER_PLAYER_W / 2, shooterPlayer.y + SHOOTER_PLAYER_H / 2, '#fbbf24', 20)
}

function declineRevival() {
  shooterRevivalDialog.value = false
  gameState.value = 'over'
  SFX.stopBGM()
  SFX.play('gameOver')
  saveHighScore()
}

function updateShooter(dt) {
  // Freeze game while revival dialog is open
  if (shooterRevivalDialog.value) return
  // Stars
  for (let layer = 0; layer < 3; layer++) {
    for (const s of shooterStars[layer]) {
      s.y += s.speed * dt / 1000
      if (s.y > canvasHeight.value) { s.y = -2; s.x = Math.random() * canvasWidth }
    }
  }

  // Player movement
  if (shooterLeft) shooterPlayer.x -= SHOOTER_SPEED * dt / 1000
  if (shooterRight) shooterPlayer.x += SHOOTER_SPEED * dt / 1000
  shooterPlayer.x = Math.max(0, Math.min(canvasWidth - SHOOTER_PLAYER_W, shooterPlayer.x))

  // Invincibility timer
  if (shooterInvincible) {
    shooterInvincibleTimer -= dt
    if (shooterInvincibleTimer <= 0) { shooterInvincible = false; shooterInvincibleTimer = 0 }
  }
  if (shooterFlashTimer > 0) shooterFlashTimer -= dt

  // Powerup timer
  if (shooterPowerupType) {
    shooterPowerupTimer -= dt
    if (shooterPowerupTimer <= 0) shooterPowerupType = null
  }
  if (shooterHomingTimer > 0) {
    shooterHomingTimer -= dt
    if (shooterHomingTimer <= 0) shooterHoming = false
  }
  if (shooterLaserTimer > 0) {
    shooterLaserTimer -= dt
    if (shooterLaserTimer <= 0) shooterLaser = false
  }
  // Active skill cooldown
  if (shooterActiveSkillCooldownTimer > 0) {
    shooterActiveSkillCooldownTimer -= dt
    shooterActiveSkillCooldown.value = Math.max(0, shooterActiveSkillCooldownTimer)
    if (shooterActiveSkillCooldownTimer <= 0) {
      shooterActiveSkillCooldownTimer = 0
      shooterActiveSkillCooldown.value = 0
    }
  }
  // Active skill duration (invincibility)
  if (shooterActiveSkillActive) {
    shooterActiveSkillDuration -= dt
    if (shooterActiveSkillDuration <= 0) {
      shooterActiveSkillActive = false
      shooterActiveSkillDuration = 0
      // Only clear invincibility if it was from the skill (not from a hit)
      if (shooterInvincibleTimer <= 0) {
        shooterInvincible = false
      }
    }
  }
  // Double score timer
  if (shooterDoubleScore) {
    shooterDoubleScoreTimer -= dt
    if (shooterDoubleScoreTimer <= 0) { shooterDoubleScore = false; shooterDoubleScoreTimer = 0 }
  }
  // Slow enemy timer
  if (shooterSlowEnemy) {
    shooterSlowEnemyTimer -= dt
    if (shooterSlowEnemyTimer <= 0) { shooterSlowEnemy = false; shooterSlowEnemyTimer = 0 }
  }
  // Magnet timer
  if (shooterMagnet) {
    shooterMagnetTimer -= dt
    if (shooterMagnetTimer <= 0) { shooterMagnet = false; shooterMagnetTimer = 0 }
  }
  // Skill cooldown timers
  for (let si = 0; si < 3; si++) {
    if (shooterSkillCooldownTimers[si] > 0) {
      shooterSkillCooldownTimers[si] -= dt
      shooterSkillCooldowns.value[si] = Math.max(0, shooterSkillCooldownTimers[si])
    }
  }
  if (shooterScreenShake > 0) shooterScreenShake -= dt
  if (shooterBossDefeated) { shooterBossDefeated.timer -= dt; if (shooterBossDefeated.timer <= 0) shooterBossDefeated = null }

  // Combo timer
  if (shooterCombo > 0) {
    shooterComboTimer -= dt
    if (shooterComboTimer <= 0) shooterCombo = 0
  }

  // Auto-fire — passive 3ms improvement per level + 1ms per 5000 score, min 80ms
  const scoreBonus = Math.floor(score.value / 5000)
  const baseFireRate = Math.max(80, SHOOTER_FIRE_RATE - shooterLevel * 3 - scoreBonus)
  const fireRate = shooterPowerupType === 'firerate' ? baseFireRate / 2 : baseFireRate
  shooterFireTimer += dt
  if (shooterFireTimer >= fireRate) {
    shooterFireTimer = 0
    shooterFire()
  }

  // Laser beam continuous damage
  if (shooterLaser && gameState.value === 'playing') {
    const lx = shooterPlayer.x + SHOOTER_PLAYER_W / 2
    for (let ei = shooterEnemies.length - 1; ei >= 0; ei--) {
      const e = shooterEnemies[ei]
      if (lx > e.x && lx < e.x + e.w) {
        e.hp -= 2
        e.flash = 60
        if (e.hp <= 0) {
          shooterCombo++; shooterComboTimer = 3000; shooterMaxCombo.value = Math.max(shooterMaxCombo.value, shooterCombo)
          SFX.play('combo')
          const mul = shooterCombo >= 20 ? 5 : shooterCombo >= 10 ? 3 : shooterCombo >= 5 ? 2 : 1
          score.value += e.points * mul * (shooterDoubleScore ? 2 : 1)
          achievementStats.value.totalKills++
          spawnParticles(e.x + e.w / 2, e.y + e.h / 2, '#f87171', 8)
          spawnScorePopup(e.x + e.w / 2, e.y, `+${e.points * mul}`)
          SFX.play('enemyDie')
          if (Math.random() < 0.20) spawnPowerup(e.x + e.w / 2, e.y + e.h)
          // Splitter splits on death
          if (e.type === 'splitter') {
            for (let s = 0; s < 2 + Math.floor(Math.random() * 2); s++) {
              shooterEnemies.push({
                x: e.x + Math.random() * e.w, y: e.y,
                w: 16, h: 16, hp: 1, maxHp: 1, speed: 120 + shooterLevel * 10,
                type: 'small', points: 5, shootTimer: 99999, moveDir: (Math.random() - 0.5) * 2, flash: 0
              })
            }
          }
          shooterEnemies.splice(ei, 1)
        }
      }
    }
    if (shooterBoss) {
      const bo = shooterBoss
      if (bo.teleportInvincible > 0) { /* skip */ } else if (lx > bo.x && lx < bo.x + bo.w) {
        bo.hp -= 2
        bo.flash = 60
        if (bo.hp <= 0) {
          shooterCombo++; shooterComboTimer = 3000; shooterMaxCombo.value = Math.max(shooterMaxCombo.value, shooterCombo)
          SFX.play('bossDie')
          const mul = shooterCombo >= 20 ? 5 : shooterCombo >= 10 ? 3 : shooterCombo >= 5 ? 2 : 1
          score.value += bo.points * mul * (shooterDoubleScore ? 2 : 1)
          achievementStats.value.bossKills++
          checkAchievements(achievementStats.value)
          spawnParticles(bo.x + bo.w / 2, bo.y + bo.h / 2, '#fbbf24', 30)
          spawnScorePopup(bo.x + bo.w / 2, bo.y, `+${bo.points * mul}`)
          spawnPowerup(bo.x + bo.w / 2, bo.y + bo.h)
          shooterScreenShake = 300
          shooterBossDefeated = { name: bo.type, points: bo.points * mul, timer: 2500 }
          shooterBoss = null
        }
      }
    }
  }

  // Update bullets
  for (let i = shooterBullets.length - 1; i >= 0; i--) {
    const b = shooterBullets[i]
    // Homing: track nearest enemy
    if (shooterHoming && !b._noHoming) {
      let nearest = null, minD = Infinity
      const bcx = b.x + 2, bcy = b.y
      for (const e of shooterEnemies) {
        const dx = (e.x + e.w / 2) - bcx, dy = (e.y + e.h / 2) - bcy
        const d = dx * dx + dy * dy
        if (d < minD) { minD = d; nearest = e }
      }
      if (shooterBoss) {
        const bo = shooterBoss
        const dx = (bo.x + bo.w / 2) - bcx, dy = (bo.y + bo.h / 2) - bcy
        const d = dx * dx + dy * dy
        if (d < minD) { minD = d; nearest = bo }
      }
      if (nearest && minD < 200 * 200) {
        const dx = (nearest.x + nearest.w / 2) - bcx
        const dy = (nearest.y + nearest.h / 2) - bcy
        const d = Math.sqrt(dx * dx + dy * dy) || 1
        const turnRate = 400 * dt / 1000
        b.vx = (b.vx || 0) + (dx / d) * turnRate
        b.vx = Math.max(-200, Math.min(200, b.vx))
      }
    }
    // Gravity boss field deflection
    if (shooterBoss && shooterBoss.type === 'gravity' && shooterBoss.gravityField > 0) {
      const bcx = b.x + 2, bcy = b.y
      const gx = shooterBoss.x + shooterBoss.w / 2, gy = shooterBoss.y + shooterBoss.h / 2
      const gdx = gx - bcx, gdy = gy - bcy
      const gDist = Math.sqrt(gdx * gdx + gdy * gdy)
      if (gDist < shooterBoss.gravityField && gDist > 10) {
        // Deflect bullet sideways
        const deflection = (1 - gDist / shooterBoss.gravityField) * 150 * dt / 1000
        b.vx = (b.vx || 0) + (gdy / gDist) * deflection * (bcx < gx ? 1 : -1)
      }
    }
    b.y -= SHOOTER_BULLET_SPEED * dt / 1000
    b.x += (b.vx || 0) * dt / 1000
    if (b.y < -10 || b.x < -10 || b.x > canvasWidth + 10) shooterBullets.splice(i, 1)
  }

  // Level up & boss check
  shooterLevelTimer += dt
  if (shooterLevelTimer >= 25000) {
    shooterLevelTimer = 0
    shooterLevel++
    SFX.play('levelUp')
    achievementStats.value.noHitStreak++
    achievementStats.value.maxLevel = Math.max(achievementStats.value.maxLevel, shooterLevel)
    checkAchievements(achievementStats.value)
    shooterRevivalUsed = false // 每关重置复活次数
    if (shooterLevel % 5 === 0 && !shooterBoss) {
      spawnBoss()
    }
  }

  // Spawn enemies (only when no boss)
  if (!shooterBoss) {
    shooterTimer += dt
    const spawnInterval = Math.max(350, 1400 - shooterLevel * 50)
    if (shooterTimer >= spawnInterval) {
      shooterTimer = 0
      spawnShooterEnemy()
    }
  }

  // Update enemies
  for (let i = shooterEnemies.length - 1; i >= 0; i--) {
    const e = shooterEnemies[i]
    const enemySpeedMul = shooterSlowEnemy ? 0.5 : 1
    e.y += e.speed * dt / 1000 * enemySpeedMul
    if (e.flash > 0) e.flash -= dt
    // Dasher horizontal movement during dash
    if (e.type === 'dasher' && e.dashing && e.dashDir) {
      e.x += e.dashDir * 150 * dt / 1000
    }
    if (e.moveDir) {
      e.x += e.moveDir * 70 * dt / 1000 * enemySpeedMul
      if (e.x <= 0) { e.x = 0; e.moveDir = 1 }
      if (e.x >= canvasWidth - e.w) { e.x = canvasWidth - e.w; e.moveDir = -1 }
    }
    // Healer: heal nearby enemies every 3 seconds
    if (e.type === 'healer') {
      e.healTimer -= dt
      if (e.healTimer <= 0) {
        e.healTimer = 3000
        for (const other of shooterEnemies) {
          if (other === e || other.hp >= other.maxHp) continue
          const dx = (other.x + other.w / 2) - (e.x + e.w / 2)
          const dy = (other.y + other.h / 2) - (e.y + e.h / 2)
          if (dx * dx + dy * dy < 100 * 100) {
            other.hp = Math.min(other.maxHp, other.hp + 1)
          }
        }
        spawnParticles(e.x + e.w / 2, e.y + e.h / 2, '#22c55e', 8)
      }
    }
    // Dasher: periodic dash toward player
    if (e.type === 'dasher') {
      if (e.dashing) {
        e.y += 350 * dt / 1000
        e.dashTimer -= dt
        if (e.dashTimer <= -500) { e.dashing = false; e.dashTimer = 3000 + Math.random() * 2000 }
      } else {
        e.dashTimer -= dt
        if (e.dashTimer <= 0) {
          e.dashing = true
          e.dashTimer = 500
          // Aim toward player
          const dx = shooterPlayer.x + SHOOTER_PLAYER_W / 2 - (e.x + e.w / 2)
          e.dashDir = dx > 0 ? 1 : -1
        }
      }
    }
    if (shooterLevel >= 2 && e.type !== 'small' && e.type !== 'healer') {
      e.shootTimer -= dt
      if (e.shootTimer <= 0) {
        e.shootTimer = Math.max(600, 2500 - shooterLevel * 150)
        shooterEnemyBullets.push({ x: e.x + e.w / 2 - 2, y: e.y + e.h })
      }
    }
    if (e.y > canvasHeight.value + 50) shooterEnemies.splice(i, 1)
  }

  // Update boss
  if (shooterBoss) {
    const b = shooterBoss
    if (b.flash > 0) b.flash -= dt
    // Shield boss: disable shield below 50% HP
    if (b.type === 'shield' && b.shieldActive && b.hp <= b.maxHp * 0.5) {
      b.shieldActive = false
    }
    // Move into position
    if (b.y < 30) {
      b.y += 80 * dt / 1000 * (shooterSlowEnemy ? 0.5 : 1)
    } else {
      // Movement pattern based on type
      const bossSpeedMul = shooterSlowEnemy ? 0.5 : 1
      if (b.type === 'zigzag') {
        // Fast zigzag movement
        b.zigzagTimer += dt
        b.x += b.zigzagDir * (100 + shooterLevel * 8) * dt / 1000 * bossSpeedMul
        if (b.x <= 5) { b.x = 5; b.zigzagDir = 1 }
        if (b.x >= canvasWidth - b.w - 5) { b.x = canvasWidth - b.w - 5; b.zigzagDir = -1 }
      } else if (b.type === 'swarm') {
        // Slow movement
        b.x += b.moveDir * (40 + shooterLevel * 3) * dt / 1000 * bossSpeedMul
        if (b.x <= 5) { b.x = 5; b.moveDir = 1 }
        if (b.x >= canvasWidth - b.w - 5) { b.x = canvasWidth - b.w - 5; b.moveDir = -1 }
        // Summon timer runs every frame
        b.swarmTimer += dt
        if (b.swarmTimer >= 3000) {
          b.swarmTimer = 0
          for (let si = 0; si < 3; si++) {
            shooterEnemies.push({
              x: b.x + b.w / 2 - 10 + si * 20, y: b.y + b.h,
              w: 18, h: 18, hp: 1, maxHp: 1, speed: 120,
              type: 'small', points: 15, shootTimer: 9999, flash: 0
            })
          }
        }
      } else if (b.type === 'teleporter') {
        // Teleporter: standard movement + periodic teleport
        b.x += b.moveDir * (50 + shooterLevel * 4) * dt / 1000 * bossSpeedMul
        if (b.x <= 5) { b.x = 5; b.moveDir = 1 }
        if (b.x >= canvasWidth - b.w - 5) { b.x = canvasWidth - b.w - 5; b.moveDir = -1 }
        b.teleportTimer += dt
        if (b.teleportInvincible > 0) b.teleportInvincible -= dt
        if (b.teleportTimer >= 3000) {
          b.teleportTimer = 0
          b.teleporting = true
          b.teleportInvincible = 1000
          // Teleport to random position
          b.x = 20 + Math.random() * (canvasWidth - b.w - 40)
          b.y = 30 + Math.random() * 80
          spawnParticles(b.x + b.w / 2, b.y + b.h / 2, '#06b6d4', 20)
          // Shoot 3 bullets in spread after teleport
          for (let a = -1; a <= 1; a++) {
            shooterEnemyBullets.push({ x: b.x + b.w / 2 - 2 + a * 20, y: b.y + b.h, vx: a * 60 })
          }
          setTimeout(() => { if (shooterBoss === b) b.teleporting = false }, 300)
        }
      } else if (b.type === 'mirror') {
        // Mirror: standard movement
        b.x += b.moveDir * (40 + shooterLevel * 3) * dt / 1000 * bossSpeedMul
        if (b.x <= 5) { b.x = 5; b.moveDir = 1 }
        if (b.x >= canvasWidth - b.w - 5) { b.x = canvasWidth - b.w - 5; b.moveDir = -1 }
        // Split into clones at 30% HP
        if (!b.mirrorClones && b.hp <= b.maxHp * 0.3) {
          b.mirrorClones = [
            { x: b.x - 60, y: b.y, w: b.w * 0.7, h: b.h * 0.7, hp: Math.floor(b.maxHp * 0.25), maxHp: Math.floor(b.maxHp * 0.25), flash: 0 },
            { x: b.x + 60, y: b.y, w: b.w * 0.7, h: b.h * 0.7, hp: Math.floor(b.maxHp * 0.25), maxHp: Math.floor(b.maxHp * 0.25), flash: 0 }
          ]
          b.hp = Math.floor(b.maxHp * 0.25)
          spawnParticles(b.x + b.w / 2, b.y + b.h / 2, '#ec4899', 25)
        }
      } else if (b.type === 'gravity') {
        // Gravity: slow movement
        b.x += b.moveDir * (30 + shooterLevel * 2) * dt / 1000 * bossSpeedMul
        if (b.x <= 5) { b.x = 5; b.moveDir = 1 }
        if (b.x >= canvasWidth - b.w - 5) { b.x = canvasWidth - b.w - 5; b.moveDir = -1 }
      } else {
        // Default + shield: standard horizontal movement
        b.x += b.moveDir * (50 + shooterLevel * 5) * dt / 1000
        if (b.x <= 5) { b.x = 5; b.moveDir = 1 }
        if (b.x >= canvasWidth - b.w - 5) { b.x = canvasWidth - b.w - 5; b.moveDir = -1 }
      }
      // Boss shooting - alternating patterns
      b.shootTimer -= dt
      b.phaseTimer += dt
      // HP-based phase escalation (every 25% HP lost)
      const hpRatio = b.hp / b.maxHp
      const hpPhase = hpRatio > 0.75 ? 0 : hpRatio > 0.50 ? 1 : hpRatio > 0.25 ? 2 : 3
      if (hpPhase > b.phase) b.phase = hpPhase
      // Timer also cycles phases (but never below HP-dictated phase)
      if (b.phaseTimer > 4000) { b.phaseTimer = 0; b.phase = Math.max(b.phase, (b.phase + 1) % 3) }
      if (b.shootTimer <= 0) {
        if (b.type === 'zigzag') {
          // Triple burst
          b.shootTimer = Math.max(150, 600 - shooterLevel * 25)
          for (let a = -1; a <= 1; a++) {
            shooterEnemyBullets.push({ x: b.x + b.w / 2 - 2 + a * 15, y: b.y + b.h })
          }
        } else if (b.type === 'swarm') {
          // Shoot
          b.shootTimer = Math.max(400, 1500 - shooterLevel * 50)
          shooterEnemyBullets.push({ x: b.x + b.w / 2 - 2, y: b.y + b.h })
        } else if (b.type === 'teleporter') {
          b.shootTimer = Math.max(300, 1000 - shooterLevel * 30)
          shooterEnemyBullets.push({ x: b.x + b.w / 2 - 2, y: b.y + b.h })
        } else if (b.type === 'mirror') {
          b.shootTimer = Math.max(250, 900 - shooterLevel * 30)
          // Mirror bullets: shoot from both sides
          const centerX = canvasWidth / 2
          const offsetFromCenter = b.x + b.w / 2 - centerX
          shooterEnemyBullets.push({ x: b.x + b.w / 2 - 2, y: b.y + b.h })
          shooterEnemyBullets.push({ x: centerX - offsetFromCenter - 2, y: b.y + b.h })
        } else if (b.type === 'gravity') {
          b.gravityTimer += dt
          b.shootTimer = Math.max(300, 1200 - shooterLevel * 40)
          // Ring burst every 5 seconds
          if (b.gravityTimer >= 5000) {
            b.gravityTimer = 0
            for (let a = 0; a < 8; a++) {
              const angle = (Math.PI * 2 / 8) * a
              shooterEnemyBullets.push({ x: b.x + b.w / 2 - 2, y: b.y + b.h / 2, vx: Math.cos(angle) * 120, vy: Math.sin(angle) * 120 })
            }
          } else {
            shooterEnemyBullets.push({ x: b.x + b.w / 2 - 2, y: b.y + b.h })
          }
        } else {
          // Default + shield: standard patterns
          b.shootTimer = Math.max(200, 800 - shooterLevel * 30)
          if (b.phase === 0) {
            shooterEnemyBullets.push({ x: b.x + b.w / 2 - 2, y: b.y + b.h })
          } else if (b.phase === 1) {
            for (let a = -2; a <= 2; a++) {
              shooterEnemyBullets.push({ x: b.x + b.w / 2 - 2 + a * 12, y: b.y + b.h, vx: a * 30 })
            }
          } else {
            shooterEnemyBullets.push({ x: b.x + 10, y: b.y + b.h })
            shooterEnemyBullets.push({ x: b.x + b.w - 14, y: b.y + b.h })
          }
        }
      }
    }
  }

  // Update enemy bullets
  for (let i = shooterEnemyBullets.length - 1; i >= 0; i--) {
    const b = shooterEnemyBullets[i]
    b.y += SHOOTER_ENEMY_BULLET_SPEED * dt / 1000
    if (b.vx) b.x += b.vx * dt / 1000
    if (b.y > canvasHeight.value + 10 || b.x < -10 || b.x > canvasWidth + 10) shooterEnemyBullets.splice(i, 1)
  }

  // Update particles
  for (let i = shooterParticles.length - 1; i >= 0; i--) {
    const p = shooterParticles[i]
    p.x += p.vx * dt / 1000
    p.y += p.vy * dt / 1000
    p.life -= dt / 1000
    if (p.life <= 0) shooterParticles.splice(i, 1)
  }

  // Update score popups
  for (let i = shooterScorePopups.length - 1; i >= 0; i--) {
    const sp = shooterScorePopups[i]
    sp.y -= 40 * dt / 1000
    sp.life -= dt / 1000
    if (sp.life <= 0) shooterScorePopups.splice(i, 1)
  }

  // Update powerups
  for (let i = shooterPowerups.length - 1; i >= 0; i--) {
    const pu = shooterPowerups[i]
    if (shooterMagnet) {
      // Powerup flies toward player
      const targetX = shooterPlayer.x + SHOOTER_PLAYER_W / 2 - pu.w / 2
      const targetY = shooterPlayer.y + SHOOTER_PLAYER_H / 2 - pu.h / 2
      const dx = targetX - pu.x
      const dy = targetY - pu.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist > 1) {
        const magnetSpeed = 300
        pu.x += (dx / dist) * magnetSpeed * dt / 1000
        pu.y += (dy / dist) * magnetSpeed * dt / 1000
      }
    } else {
      pu.y += pu.speed * dt / 1000
    }
    if (pu.y > canvasHeight.value + 20) { shooterPowerups.splice(i, 1); continue }
    // Collision with player
    const px = shooterPlayer.x, py = shooterPlayer.y
    if (pu.x < px + SHOOTER_PLAYER_W && pu.x + pu.w > px && pu.y < py + SHOOTER_PLAYER_H && pu.y + pu.h > py) {
      if (pu.type === 'bomb') {
        SFX.play('bomb')
        for (const e of shooterEnemies) {
          shooterCombo++; shooterComboTimer = 3000; shooterMaxCombo.value = Math.max(shooterMaxCombo.value, shooterCombo)
          const mult = shooterCombo >= 20 ? 5 : shooterCombo >= 10 ? 3 : shooterCombo >= 5 ? 2 : 1
          const pts = e.points * mult * (shooterDoubleScore ? 2 : 1)
          score.value += pts
          if (shooterDoubleScore) { achievementStats.value.doubleScoreKills++; checkAchievements(achievementStats.value) }
          achievementStats.value.totalKills++
          spawnParticles(e.x + e.w / 2, e.y + e.h / 2, '#fbbf24', 6)
          spawnScorePopup(e.x + e.w / 2, e.y, `+${pts}`)
          if (Math.random() < 0.2) spawnPowerup(e.x + e.w / 2, e.y + e.h)
        }
        if (shooterBoss) {
          shooterCombo++; shooterComboTimer = 3000; shooterMaxCombo.value = Math.max(shooterMaxCombo.value, shooterCombo)
          const mult = shooterCombo >= 20 ? 5 : shooterCombo >= 10 ? 3 : shooterCombo >= 5 ? 2 : 1
          const pts = shooterBoss.points * mult * (shooterDoubleScore ? 2 : 1)
          score.value += pts
          spawnParticles(shooterBoss.x + shooterBoss.w / 2, shooterBoss.y + shooterBoss.h / 2, '#fbbf24', 20)
          spawnParticles(shooterBoss.x + shooterBoss.w / 2, shooterBoss.y + shooterBoss.h / 2, '#f43f5e', 15)
          spawnScorePopup(shooterBoss.x + shooterBoss.w / 2, shooterBoss.y, `+${pts}`)
          SFX.play('bossDie')
          achievementStats.value.bossKills++
          checkAchievements(achievementStats.value)
          shooterBossDefeated = { name: shooterBoss.type, points: pts, timer: 2500 }
          spawnPowerup(shooterBoss.x + shooterBoss.w / 2, shooterBoss.y + shooterBoss.h)
          shooterBoss = null
        }
        shooterEnemies = []
        shooterEnemyBullets = []
        checkAchievements(achievementStats.value)
        spawnParticles(px + SHOOTER_PLAYER_W / 2, py, '#fbbf24', 25)
      } else if (pu.type === 'shield') {
        shooterShield = true
      } else if (pu.type === 'laser') {
        shooterLaser = true
        shooterLaserTimer = 8000
      } else if (pu.type === 'homing') {
        shooterHoming = true
        shooterHomingTimer = 10000
      } else if (pu.type === 'heal') {
        if (shooterLives < shooterMaxLives) shooterLives++
        spawnParticles(px + SHOOTER_PLAYER_W / 2, py, '#22c55e', 10)
      } else if (pu.type === 'life') {
        if (shooterMaxLives < 7) { shooterMaxLives++; shooterLives++; achievementStats.value.maxLives = Math.max(achievementStats.value.maxLives, shooterMaxLives); checkAchievements(achievementStats.value) }
        else if (shooterLives < shooterMaxLives) shooterLives++
        spawnParticles(px + SHOOTER_PLAYER_W / 2, py, '#f472b6', 10)
      } else if (pu.type === 'score') {
        score.value += 100 * (shooterDoubleScore ? 2 : 1)
        spawnScorePopup(px + SHOOTER_PLAYER_W / 2, py, '+100')
        spawnParticles(px + SHOOTER_PLAYER_W / 2, py, '#fbbf24', 15)
      } else if (pu.type === 'double') {
        shooterDoubleScore = true
        shooterDoubleScoreTimer = 8000
        spawnParticles(px + SHOOTER_PLAYER_W / 2, py, '#f472b6', 15)
      } else {
        shooterPowerupType = pu.type
        shooterPowerupTimer = 8000
      }
      shooterPowerups.splice(i, 1)
      if (pu.type !== 'bomb') SFX.play('powerup')
    }
  }

  // Collision: player bullets vs enemies
  for (let bi = shooterBullets.length - 1; bi >= 0; bi--) {
    const b = shooterBullets[bi]
    let hit = false
    for (let ei = shooterEnemies.length - 1; ei >= 0; ei--) {
      const e = shooterEnemies[ei]
      if (b.x < e.x + e.w && b.x + 4 > e.x && b.y < e.y + e.h && b.y + 10 > e.y) {
        e.hp--
        e.flash = 80
        hit = true
        // Tanker: 50% chance to resist knockback visual
        if (e.type === 'tanker' && Math.random() < 0.5) { e.flash = 0 } else { e.flash = 80 }
        if (e.hp <= 0) {
          shooterCombo++; shooterMaxCombo.value = Math.max(shooterMaxCombo.value, shooterCombo)
          SFX.play('combo')
          shooterComboTimer = 3000
          const multiplier = shooterCombo >= 20 ? 5 : shooterCombo >= 10 ? 3 : shooterCombo >= 5 ? 2 : 1
          const pts = e.points * multiplier * (shooterDoubleScore ? 2 : 1)
          score.value += pts
          if (shooterDoubleScore) { achievementStats.value.doubleScoreKills++; checkAchievements(achievementStats.value) }
          const pColor = e.type === 'fast' ? '#fbbf24' : e.type === 'splitter' ? '#a78bfa' : e.type === 'small' ? '#f87171' : e.type === 'medium' ? '#ef4444' : e.type === 'tanker' ? '#64748b' : e.type === 'healer' ? '#22c55e' : e.type === 'dasher' ? '#f97316' : '#dc2626'
          spawnParticles(e.x + e.w / 2, e.y + e.h / 2, pColor, 10)
          spawnScorePopup(e.x + e.w / 2, e.y, `+${pts}`)
          if (Math.random() < 0.20) spawnPowerup(e.x + e.w / 2, e.y + e.h)
          // Splitter splits into small enemies
          if (e.type === 'splitter') {
            for (let s = 0; s < 2 + Math.floor(Math.random() * 2); s++) {
              shooterEnemies.push({
                x: e.x + Math.random() * e.w, y: e.y + Math.random() * e.h * 0.5,
                w: 16, h: 16, hp: 1, maxHp: 1, speed: 120 + shooterLevel * 10,
                type: 'small', points: 5, shootTimer: 99999, moveDir: (Math.random() - 0.5) * 2, flash: 0
              })
            }
          }
          shooterEnemies.splice(ei, 1)
          SFX.play('enemyDie')
          achievementStats.value.totalKills++
        }
        break
      }
    }
    // Bullets vs boss
    if (shooterBoss && !hit) {
      const bo = shooterBoss
      // Teleporter invincibility check
      if (bo.teleportInvincible > 0) { /* skip damage */ } else
      if (b.x < bo.x + bo.w && b.x + 4 > bo.x && b.y < bo.y + bo.h && b.y + 10 > bo.y) {
        // Shield boss takes 50% less damage when shield is active
        const dmg = (bo.type === 'shield' && bo.shieldActive) ? 0.5 : 1
        bo.hp -= dmg
        bo.flash = 60
        // Boss HP phase: increase attack density at 75%/50%/25% HP
        if (bo.hp > 0) {
          const pct = bo.hp / bo.maxHp
          if (pct <= 0.25) bo.phase = 2
          else if (pct <= 0.5) bo.phase = Math.max(bo.phase, 1)
        }
        shooterScreenShake = Math.max(shooterScreenShake, 60)
        hit = true
        if (bo.hp <= 0) {
          shooterCombo++; shooterMaxCombo.value = Math.max(shooterMaxCombo.value, shooterCombo)
          SFX.play('combo')
          shooterComboTimer = 3000
          const multiplier = shooterCombo >= 20 ? 5 : shooterCombo >= 10 ? 3 : shooterCombo >= 5 ? 2 : 1
          const pts = bo.points * multiplier * (shooterDoubleScore ? 2 : 1)
          score.value += pts
          spawnParticles(bo.x + bo.w / 2, bo.y + bo.h / 2, '#fbbf24', 30)
          spawnParticles(bo.x + bo.w / 2, bo.y + bo.h / 2, '#f43f5e', 15)
          spawnScorePopup(bo.x + bo.w / 2, bo.y, `+${pts}`)
          SFX.play('bossDie')
          achievementStats.value.bossKills++
          checkAchievements(achievementStats.value)
          // Guaranteed powerup drop
          spawnPowerup(bo.x + bo.w / 2, bo.y + bo.h)
          shooterScreenShake = 300
          shooterBossDefeated = { name: bo.type, points: pts, timer: 2500 }
          shooterBoss = null
        }
      }
    }
    if (hit) shooterBullets.splice(bi, 1)
  }

  // Collision: enemy bullets vs player
  if (!shooterInvincible && !shooterRevivalDialog.value) {
    const px = shooterPlayer.x, py = shooterPlayer.y
    for (let i = shooterEnemyBullets.length - 1; i >= 0; i--) {
      const b = shooterEnemyBullets[i]
      if (b.x < px + SHOOTER_PLAYER_W && b.x + 4 > px && b.y < py + SHOOTER_PLAYER_H && b.y + 8 > py) {
        shooterEnemyBullets.splice(i, 1)
        shooterHit()
        if (gameState.value !== 'playing' || shooterRevivalDialog.value) return
      }
    }
    // Enemies vs player
    for (let i = shooterEnemies.length - 1; i >= 0; i--) {
      const e = shooterEnemies[i]
      if (e.x < px + SHOOTER_PLAYER_W && e.x + e.w > px && e.y < py + SHOOTER_PLAYER_H && e.y + e.h > py) {
        shooterEnemies.splice(i, 1)
        shooterHit()
        if (gameState.value !== 'playing' || shooterRevivalDialog.value) return
      }
    }
  }
}

function drawShooter(ctx) {
  // Screen shake
  let sx = 0, sy = 0
  if (shooterScreenShake > 0) {
    sx = (Math.random() - 0.5) * 6
    sy = (Math.random() - 0.5) * 6
  }
  ctx.save()
  ctx.translate(sx, sy)

  // Background gradient (cached)
  if (!shooterGrad) {
    shooterGrad = ctx.createLinearGradient(0, 0, 0, canvasHeight.value)
    shooterGrad.addColorStop(0, '#020617')
    shooterGrad.addColorStop(0.5, '#0f172a')
    shooterGrad.addColorStop(1, '#1e1b4b')
  }
  ctx.fillStyle = shooterGrad
  ctx.fillRect(0, 0, canvasWidth, canvasHeight.value)

  // Stars - 3 layers
  for (let layer = 0; layer < 3; layer++) {
    const alpha = [0.25, 0.5, 0.9][layer]
    const color = ['#94a3b8', '#cbd5e1', '#f8fafc'][layer]
    ctx.fillStyle = color
    for (const s of shooterStars[layer]) {
      ctx.globalAlpha = alpha
      ctx.fillRect(s.x, s.y, s.size, s.size)
    }
  }
  ctx.globalAlpha = 1

  // Powerups
  const pSkin = getPowerupSkin()
  const puLabels = { spread: 'S', firerate: 'F', bomb: 'B', shield: 'H', laser: 'L', homing: 'T', heal: '+', life: '♥', score: '★', double: '×2' }
  for (const pu of shooterPowerups) {
    const puColor = pSkin.colors[pu.type] || '#fff'
    const pcx = pu.x + pu.w / 2, pcy = pu.y + pu.h / 2
    if (pSkin.glow) {
      ctx.shadowColor = pSkin.glow; ctx.shadowBlur = 8
    }
    if (pSkin.icon === '◆') {
      // Crystal diamond shape
      ctx.fillStyle = puColor
      ctx.beginPath()
      ctx.moveTo(pcx, pu.y); ctx.lineTo(pu.x + pu.w, pcy)
      ctx.lineTo(pcx, pu.y + pu.h); ctx.lineTo(pu.x, pcy)
      ctx.closePath(); ctx.fill()
    } else if (pSkin.icon === '●') {
      // Neon circle
      ctx.fillStyle = puColor
      ctx.beginPath(); ctx.arc(pcx, pcy, pu.w / 2, 0, Math.PI * 2); ctx.fill()
      ctx.strokeStyle = lightenColor(puColor, 1.5); ctx.lineWidth = 1.5
      ctx.beginPath(); ctx.arc(pcx, pcy, pu.w / 2 + 2, 0, Math.PI * 2); ctx.stroke()
    } else if (pSkin.icon === '🔥' || pSkin.icon === '❄' || pSkin.icon === '✦') {
      // Flame/Ice/Galaxy: hexagon
      ctx.fillStyle = puColor
      ctx.beginPath()
      for (let i = 0; i < 6; i++) {
        const a = Math.PI / 3 * i - Math.PI / 2
        const method = i === 0 ? 'moveTo' : 'lineTo'
        ctx[method](pcx + Math.cos(a) * pu.w / 2, pcy + Math.sin(a) * pu.h / 2)
      }
      ctx.closePath(); ctx.fill()
    } else {
      // Default square
      ctx.fillStyle = puColor
      ctx.beginPath(); ctx.roundRect(pu.x, pu.y, pu.w, pu.h, 4); ctx.fill()
    }
    ctx.shadowBlur = 0
    ctx.fillStyle = '#fff'
    ctx.font = 'bold 11px sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(puLabels[pu.type] || '?', pcx, pcy)
  }
  ctx.textAlign = 'left'
  ctx.textBaseline = 'alphabetic'

  // Laser beam
  if (shooterLaser) {
    const lx = shooterPlayer.x + SHOOTER_PLAYER_W / 2
    ctx.strokeStyle = 'rgba(248,250,252,0.8)'
    ctx.lineWidth = 4
    ctx.shadowColor = '#f8fafc'
    ctx.shadowBlur = 15
    ctx.beginPath(); ctx.moveTo(lx, shooterPlayer.y); ctx.lineTo(lx, 0); ctx.stroke()
    ctx.strokeStyle = 'rgba(248,250,252,0.3)'
    ctx.lineWidth = 10
    ctx.beginPath(); ctx.moveTo(lx, shooterPlayer.y); ctx.lineTo(lx, 0); ctx.stroke()
    ctx.shadowBlur = 0
  }

  // Player bullets (cache mobius gradient outside loop)
  let _mobiusGrad = null
  for (const b of shooterBullets) {
    if (shooterHoming) {
      ctx.fillStyle = '#c084fc'
      ctx.fillRect(b.x, b.y, 4, 10)
      ctx.fillStyle = '#e9d5ff'
      ctx.fillRect(b.x + 1, b.y, 2, 4)
    } else {
      const bs = getBulletStyle()
      // Glow effect for rare+ bullets
      if (bs.glow) {
        ctx.shadowColor = bs.glow
        ctx.shadowBlur = 6
      }
      ctx.fillStyle = bs.main
      if (bs.shape === 'triangle') {
        ctx.beginPath()
        ctx.moveTo(b.x + 2, b.y - 2)
        ctx.lineTo(b.x - 1, b.y + 10)
        ctx.lineTo(b.x + 5, b.y + 10)
        ctx.closePath()
        ctx.fill()
      } else if (bs.shape === 'diamond') {
        ctx.beginPath()
        ctx.moveTo(b.x + 2, b.y - 1)
        ctx.lineTo(b.x + 5, b.y + 5)
        ctx.lineTo(b.x + 2, b.y + 11)
        ctx.lineTo(b.x - 1, b.y + 5)
        ctx.closePath()
        ctx.fill()
      } else if (bs.shape === 'laser') {
        // Thin laser beam
        ctx.fillRect(b.x, b.y, 2, 12)
        ctx.fillStyle = bs.highlight
        ctx.globalAlpha = 0.5
        ctx.fillRect(b.x - 1, b.y, 4, 12)
        ctx.globalAlpha = 1
      } else if (bs.shape === 'star') {
        // 4-point star
        const scx = b.x + 2, scy = b.y + 5
        ctx.beginPath()
        for (let i = 0; i < 4; i++) {
          const a = (i * Math.PI / 2) - Math.PI / 2
          const r1 = 6, r2 = 2
          ctx.lineTo(scx + Math.cos(a) * r1, scy + Math.sin(a) * r1)
          const a2 = a + Math.PI / 4
          ctx.lineTo(scx + Math.cos(a2) * r2, scy + Math.sin(a2) * r2)
        }
        ctx.closePath()
        ctx.fill()
      } else if (bs.shape === 'sword') {
        // Flying sword - xianxia bronze sword spinning
        const sAngle = (Date.now() / 200) % (Math.PI * 2)
        ctx.save()
        ctx.translate(b.x + 2, b.y + 5)
        ctx.rotate(sAngle)
        // Blade
        ctx.fillStyle = '#8B4513'
        ctx.beginPath()
        ctx.moveTo(0, -12)
        ctx.lineTo(-2.5, 0)
        ctx.lineTo(0, 12)
        ctx.lineTo(2.5, 0)
        ctx.closePath()
        ctx.fill()
        // Guard
        ctx.fillStyle = '#DAA520'
        ctx.fillRect(-5, -1.5, 10, 3)
        // Rune glow
        ctx.fillStyle = '#00FF7F'
        ctx.globalAlpha = 0.5 + Math.sin(Date.now() / 100) * 0.3
        ctx.fillRect(-1, -6, 2, 4)
        ctx.globalAlpha = 1
        ctx.restore()
      } else if (bs.shape === 'note') {
        // Musical note - oscillating with rainbow
        const nTime = Date.now() / 100
        const nColors = ['#FF0000', '#FF7F00', '#FFFF00', '#00FF00', '#0000FF', '#8B00FF']
        const nColor = nColors[Math.floor(nTime / 2) % nColors.length]
        ctx.fillStyle = nColor
        const nx = b.x + 2 + Math.sin(nTime) * 2
        // Note head (oval)
        ctx.beginPath()
        ctx.ellipse(nx, b.y + 6, 3, 2.5, -0.3, 0, Math.PI * 2)
        ctx.fill()
        // Stem
        ctx.fillRect(nx + 2, b.y - 4, 1.5, 10)
        // Flag
        ctx.beginPath()
        ctx.moveTo(nx + 3.5, b.y - 4)
        ctx.quadraticCurveTo(nx + 8, b.y - 1, nx + 3.5, b.y + 2)
        ctx.fill()
      } else if (bs.shape === 'origami') {
        // Origami - rotating paper airplane/crane/boat
        const oPhase = Math.floor(Date.now() / 400) % 3
        const oTime = Date.now() / 150
        ctx.save()
        ctx.translate(b.x + 2, b.y + 5)
        ctx.rotate(Math.sin(oTime) * 0.15)
        if (oPhase === 0) {
          // Paper airplane
          ctx.fillStyle = '#87CEEB'
          ctx.beginPath()
          ctx.moveTo(0, -10)
          ctx.lineTo(-5, 8)
          ctx.lineTo(0, 5)
          ctx.lineTo(5, 8)
          ctx.closePath()
          ctx.fill()
          ctx.strokeStyle = '#D3D3D3'
          ctx.lineWidth = 0.5
          ctx.beginPath()
          ctx.moveTo(0, -10)
          ctx.lineTo(0, 5)
          ctx.stroke()
        } else if (oPhase === 1) {
          // Paper crane
          ctx.fillStyle = '#FFB6C1'
          ctx.beginPath()
          ctx.moveTo(0, -8)
          ctx.lineTo(-6, 2)
          ctx.lineTo(-3, 8)
          ctx.lineTo(0, 4)
          ctx.lineTo(3, 8)
          ctx.lineTo(6, 2)
          ctx.closePath()
          ctx.fill()
          ctx.strokeStyle = '#D3D3D3'
          ctx.lineWidth = 0.5
          ctx.stroke()
        } else {
          // Paper boat
          ctx.fillStyle = '#F0E68C'
          ctx.beginPath()
          ctx.moveTo(-6, 2)
          ctx.lineTo(6, 2)
          ctx.lineTo(4, 8)
          ctx.lineTo(-4, 8)
          ctx.closePath()
          ctx.fill()
          ctx.beginPath()
          ctx.moveTo(-5, 2)
          ctx.lineTo(0, -6)
          ctx.lineTo(5, 2)
          ctx.closePath()
          ctx.fill()
        }
        ctx.restore()
      } else if (bs.shape === 'dna') {
        // DNA double helix - spinning
        const dTime = Date.now() / 100
        const baseColors = ['#FF0000', '#0000FF', '#00FF00', '#FFFF00']
        for (let i = 0; i < 4; i++) {
          const dy = b.y + i * 3
          const dx1 = b.x + 2 + Math.sin(dTime + i * 1.2) * 3
          const dx2 = b.x + 2 - Math.sin(dTime + i * 1.2) * 3
          // Backbone dots
          ctx.fillStyle = '#FFFFFF'
          ctx.beginPath()
          ctx.arc(dx1, dy, 1.2, 0, Math.PI * 2)
          ctx.fill()
          ctx.beginPath()
          ctx.arc(dx2, dy, 1.2, 0, Math.PI * 2)
          ctx.fill()
          // Base pair
          ctx.strokeStyle = baseColors[i]
          ctx.lineWidth = 1
          ctx.beginPath()
          ctx.moveTo(dx1, dy)
          ctx.lineTo(dx2, dy)
          ctx.stroke()
        }
      } else if (bs.shape === 'mobius') {
        // Mobius strip - ∞ energy ring
        const mTime = Date.now() / 120
        const mcx = b.x + 2, mcy = b.y + 5
        ctx.save()
        ctx.translate(mcx, mcy)
        ctx.rotate(mTime * 0.5)
        // Draw ∞ shape (cached gradient)
        if (!_mobiusGrad) {
          _mobiusGrad = ctx.createLinearGradient(-6, 0, 6, 0)
          _mobiusGrad.addColorStop(0, '#FFD700')
          _mobiusGrad.addColorStop(0.5, '#9370DB')
          _mobiusGrad.addColorStop(1, '#FFD700')
        }
        ctx.strokeStyle = _mobiusGrad
        ctx.lineWidth = 2.5
        ctx.beginPath()
        ctx.moveTo(-5, 0)
        ctx.bezierCurveTo(-5, -5, 0, -5, 0, 0)
        ctx.bezierCurveTo(0, 5, 5, 5, 5, 0)
        ctx.bezierCurveTo(5, -5, 0, -5, 0, 0)
        ctx.bezierCurveTo(0, 5, -5, 5, -5, 0)
        ctx.stroke()
        // Glow center
        ctx.fillStyle = 'rgba(255,215,0,0.4)'
        ctx.beginPath()
        ctx.arc(0, 0, 2, 0, Math.PI * 2)
        ctx.fill()
        ctx.restore()
      } else if (bs.shape === 'stampbullet') {
        // Stamp perforation - dotted line
        const stTime = Date.now() / 80
        for (let i = 0; i < 4; i++) {
          const sy = b.y + i * 3
          const alpha = 0.5 + Math.sin(stTime + i) * 0.3
          ctx.fillStyle = `rgba(139,69,19,${alpha})`
          ctx.beginPath()
          ctx.arc(b.x + 2, sy, 1.5, 0, Math.PI * 2)
          ctx.fill()
        }
      } else {
        ctx.fillRect(b.x, b.y, 4, 10)
        ctx.fillStyle = bs.highlight
        ctx.fillRect(b.x + 1, b.y, 2, 4)
      }
      ctx.shadowBlur = 0
    }
  }

  // Enemy bullets
  for (const b of shooterEnemyBullets) {
    ctx.fillStyle = '#fb923c'
    ctx.fillRect(b.x, b.y, 4, 8)
    ctx.fillStyle = '#fdba74'
    ctx.fillRect(b.x + 1, b.y, 2, 3)
  }

  // Enemies
  const eSkin = getEnemySkin()
  for (const e of shooterEnemies) {
    const isFlashing = e.flash > 0
    // Special shape skins override all enemy type visuals
    if (eSkin.shape && eSkin.shape !== 'default') {
      if (eSkin.shape === 'dice') {
        const dSize = Math.min(e.w, e.h)
        const dFace = Math.floor((Date.now() / 800 + e.x) % 6)
        ctx.fillStyle = isFlashing ? '#fff' : '#FFFFF0'
        ctx.fillRect(e.x + (e.w - dSize) / 2, e.y + (e.h - dSize) / 2, dSize, dSize)
        ctx.strokeStyle = '#DAA520'
        ctx.lineWidth = 1.5
        ctx.strokeRect(e.x + (e.w - dSize) / 2, e.y + (e.h - dSize) / 2, dSize, dSize)
        const dsx = e.x + e.w / 2, dsy = e.y + e.h / 2
        ctx.fillStyle = dFace === 0 ? '#2F2F2F' : dFace === 1 ? '#FFD700' : dFace === 2 ? '#9370DB' : dFace === 3 ? '#DC143C' : dFace === 4 ? '#1E90FF' : '#F5F5DC'
        ctx.save(); ctx.font = `${dSize * 0.5}px serif`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
        const symbols = ['☠', '★', '?', '💣', '✦', '']
        ctx.fillText(symbols[dFace], dsx, dsy); ctx.restore()
      } else if (eSkin.shape === 'prion') {
        const pcx = e.x + e.w / 2, pcy = e.y + e.h / 2
        const pr = Math.min(e.w, e.h) / 2
        ctx.fillStyle = isFlashing ? '#fff' : 'rgba(230,230,250,0.7)'
        ctx.beginPath(); ctx.arc(pcx, pcy, pr, 0, Math.PI * 2); ctx.fill()
        ctx.fillStyle = '#8B0000'
        ctx.beginPath(); ctx.arc(pcx, pcy, pr * 0.35, 0, Math.PI * 2); ctx.fill()
        ctx.strokeStyle = '#DC143C'; ctx.lineWidth = 1.2; ctx.beginPath()
        for (let i = 0; i < 12; i++) {
          const sa = (Math.PI * 2 / 12) * i + Date.now() / 500
          const sLen = pr * (0.7 + Math.sin(Date.now() / 200 + i) * 0.2)
          ctx.moveTo(pcx + Math.cos(sa) * pr * 0.7, pcy + Math.sin(sa) * pr * 0.7)
          ctx.lineTo(pcx + Math.cos(sa) * sLen, pcy + Math.sin(sa) * sLen)
        }
        ctx.stroke()
      } else if (eSkin.shape === 'coin') {
        const ccx = e.x + e.w / 2, ccy = e.y + e.h / 2
        const cr = Math.min(e.w, e.h) / 2
        ctx.save()
        ctx.fillStyle = isFlashing ? '#fff' : '#8B7355'
        ctx.beginPath(); ctx.arc(ccx, ccy, cr, 0, Math.PI * 2); ctx.fill()
        ctx.fillStyle = '#556B2F'; ctx.globalAlpha = 0.4
        ctx.beginPath(); ctx.arc(ccx - cr * 0.3, ccy - cr * 0.2, cr * 0.25, 0, Math.PI * 2); ctx.fill()
        ctx.globalAlpha = 1
        ctx.fillStyle = '#1A1A1A'; const holeSize = cr * 0.4
        ctx.fillRect(ccx - holeSize / 2, ccy - holeSize / 2, holeSize, holeSize)
        ctx.fillStyle = '#4B0082'; ctx.globalAlpha = 0.5 + Math.sin(Date.now() / 300) * 0.3
        ctx.font = `${cr * 0.3}px serif`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
        for (let i = 0; i < 8; i++) {
          const ra = (Math.PI * 2 / 8) * i
          ctx.fillText('咒', ccx + Math.cos(ra) * cr * 0.72, ccy + Math.sin(ra) * cr * 0.72)
        }
        ctx.restore()
      } else if (eSkin.shape === 'droplet') {
        const dcx = e.x + e.w / 2, dcy = e.y + e.h / 2
        const dr = Math.min(e.w, e.h) / 2
        ctx.save(); ctx.translate(dcx, dcy); ctx.scale(dr, dr)
        if (!e._dropletGrad) {
          e._dropletGrad = ctx.createRadialGradient(-0.3, -0.3, 0, 0, 0, 1)
          e._dropletGrad.addColorStop(0, '#FFFFFF')
          e._dropletGrad.addColorStop(0.4, '#C0C0C0')
          e._dropletGrad.addColorStop(0.8, '#696969')
          e._dropletGrad.addColorStop(1, '#A0A0A0')
        }
        ctx.fillStyle = isFlashing ? '#fff' : e._dropletGrad
        ctx.beginPath()
        ctx.moveTo(0, -1.2); ctx.quadraticCurveTo(1, -0.3, 0.7, 0.5)
        ctx.quadraticCurveTo(0, 1.2, -0.7, 0.5); ctx.quadraticCurveTo(-1, -0.3, 0, -1.2)
        ctx.fill(); ctx.restore()
        ctx.fillStyle = 'rgba(255,255,255,0.6)'
        ctx.beginPath(); ctx.ellipse(dcx - dr * 0.2, dcy - dr * 0.3, dr * 0.15, dr * 0.25, -0.5, 0, Math.PI * 2); ctx.fill()
      } else if (eSkin.shape === 'jackbox') {
        const jx = e.x, jy = e.y, jw = e.w, jh = e.h
        const jBounce = Math.sin(Date.now() / 400) * 2
        for (let i = 0; i < 4; i++) {
          ctx.fillStyle = i % 2 === 0 ? '#FF4500' : '#FFD700'
          ctx.fillRect(jx, jy + jBounce + (jh / 4) * i, jw, jh / 4)
        }
        ctx.strokeStyle = '#2F2F2F'; ctx.lineWidth = 1.5
        ctx.beginPath(); ctx.moveTo(jx + jw * 0.3, jy + jBounce); ctx.lineTo(jx + jw * 0.4, jy + jBounce + jh * 0.5); ctx.stroke()
        ctx.strokeStyle = '#C0C0C0'; ctx.lineWidth = 2
        const springH = 8 + Math.sin(Date.now() / 200) * 3
        ctx.beginPath()
        for (let i = 0; i < 4; i++) { const sy = jy + jBounce - springH * (i / 4); ctx.lineTo(jx + jw / 2 + (i % 2 === 0 ? -4 : 4), sy) }
        ctx.stroke()
        ctx.fillStyle = '#808080'
        ctx.beginPath(); ctx.arc(jx + jw / 2, jy + jBounce - springH - 4, 4, 0, Math.PI * 2); ctx.fill()
      } else if (eSkin.shape === 'stamp') {
        const sx = e.x, sy = e.y, sw = e.w, sh = e.h
        ctx.save()
        ctx.fillStyle = isFlashing ? '#fff' : '#F5F5DC'; ctx.fillRect(sx, sy, sw, sh)
        ctx.fillStyle = '#8B4513'
        for (let i = 0; i < Math.floor(sw / 6); i++) {
          ctx.beginPath(); ctx.arc(sx + 3 + i * 6, sy, 2, 0, Math.PI * 2); ctx.fill()
          ctx.beginPath(); ctx.arc(sx + 3 + i * 6, sy + sh, 2, 0, Math.PI * 2); ctx.fill()
        }
        for (let i = 0; i < Math.floor(sh / 6); i++) {
          ctx.beginPath(); ctx.arc(sx, sy + 3 + i * 6, 2, 0, Math.PI * 2); ctx.fill()
          ctx.beginPath(); ctx.arc(sx + sw, sy + 3 + i * 6, 2, 0, Math.PI * 2); ctx.fill()
        }
        ctx.fillStyle = '#8B7355'; ctx.font = `${Math.min(sw, sh) * 0.4}px serif`
        ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
        ctx.fillText('✈', sx + sw / 2, sy + sh / 2)
        ctx.strokeStyle = '#8B0000'; ctx.lineWidth = 1
        ctx.beginPath(); ctx.arc(sx + sw * 0.7, sy + sh * 0.7, 6, 0, Math.PI * 2); ctx.stroke()
        ctx.font = '5px sans-serif'; ctx.fillStyle = '#8B0000'
        ctx.fillText(t('games.diceDead'), sx + sw * 0.7, sy + sh * 0.7)
        ctx.restore()
      }
      // HP bar for shape-skinned enemies
      if (e.maxHp > 1) {
        ctx.fillStyle = '#334155'; ctx.fillRect(e.x, e.y - 6, e.w, 3)
        ctx.fillStyle = e.hp > e.maxHp * 0.5 ? '#4ade80' : e.hp > e.maxHp * 0.25 ? '#fbbf24' : '#ef4444'
        ctx.fillRect(e.x, e.y - 6, e.w * (e.hp / e.maxHp), 3)
      }
      continue
    }
    // Type-based rendering (no shape skin equipped)
    if (e.type === 'small') {
      ctx.fillStyle = isFlashing ? '#fff' : eSkin.small
      ctx.beginPath()
      ctx.moveTo(e.x + e.w / 2, e.y + e.h)
      ctx.lineTo(e.x, e.y)
      ctx.lineTo(e.x + e.w, e.y)
      ctx.closePath()
      ctx.fill()
    } else if (e.type === 'fast') {
      // Diamond shape - narrow and sleek
      ctx.fillStyle = isFlashing ? '#fff' : eSkin.fast
      ctx.beginPath()
      ctx.moveTo(e.x + e.w / 2, e.y)
      ctx.lineTo(e.x + e.w, e.y + e.h / 2)
      ctx.lineTo(e.x + e.w / 2, e.y + e.h)
      ctx.lineTo(e.x, e.y + e.h / 2)
      ctx.closePath()
      ctx.fill()
    } else if (e.type === 'splitter') {
      // Circle shape
      ctx.fillStyle = isFlashing ? '#fff' : eSkin.splitter
      ctx.beginPath()
      ctx.arc(e.x + e.w / 2, e.y + e.h / 2, e.w / 2, 0, Math.PI * 2)
      ctx.fill()
      // Inner ring
      ctx.strokeStyle = 'rgba(255,255,255,0.3)'
      ctx.lineWidth = 1.5
      ctx.beginPath()
      ctx.arc(e.x + e.w / 2, e.y + e.h / 2, e.w / 3, 0, Math.PI * 2)
      ctx.stroke()
    } else if (e.type === 'medium') {
      ctx.fillStyle = isFlashing ? '#fff' : eSkin.medium
      ctx.beginPath()
      const cx = e.x + e.w / 2, cy = e.y + e.h / 2
      for (let i = 0; i < 6; i++) {
        const a = Math.PI / 3 * i - Math.PI / 2
        const method = i === 0 ? 'moveTo' : 'lineTo'
        ctx[method](cx + Math.cos(a) * e.w / 2, cy + Math.sin(a) * e.h / 2)
      }
      ctx.closePath()
      ctx.fill()
    } else if (e.type === 'tanker') {
      // Tanker: thick square with armor texture
      ctx.fillStyle = isFlashing ? '#fff' : (eSkin.tanker || '#64748b')
      ctx.fillRect(e.x, e.y, e.w, e.h)
      // Armor lines
      ctx.strokeStyle = 'rgba(255,255,255,0.2)'
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.moveTo(e.x + 2, e.y + e.h / 3)
      ctx.lineTo(e.x + e.w - 2, e.y + e.h / 3)
      ctx.moveTo(e.x + 2, e.y + e.h * 2 / 3)
      ctx.lineTo(e.x + e.w - 2, e.y + e.h * 2 / 3)
      ctx.stroke()
      // Shield icon
      ctx.fillStyle = 'rgba(255,255,255,0.4)'
      ctx.font = 'bold 12px sans-serif'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText('🛡', e.x + e.w / 2, e.y + e.h / 2)
      ctx.textAlign = 'left'
      ctx.textBaseline = 'alphabetic'
    } else if (e.type === 'healer') {
      // Healer: green circle with cross
      ctx.fillStyle = isFlashing ? '#fff' : (eSkin.healer || '#22c55e')
      ctx.beginPath()
      ctx.arc(e.x + e.w / 2, e.y + e.h / 2, e.w / 2, 0, Math.PI * 2)
      ctx.fill()
      // Cross mark
      ctx.fillStyle = '#fff'
      ctx.fillRect(e.x + e.w / 2 - 2, e.y + e.h / 2 - 6, 4, 12)
      ctx.fillRect(e.x + e.w / 2 - 6, e.y + e.h / 2 - 2, 12, 4)
      // Heal aura (animated)
      const healAlpha = Math.sin(Date.now() / 300) * 0.15 + 0.15
      ctx.strokeStyle = 'rgba(34,197,94,' + healAlpha + ')'
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.arc(e.x + e.w / 2, e.y + e.h / 2, e.w / 2 + 4 + Math.sin(Date.now() / 200) * 2, 0, Math.PI * 2)
      ctx.stroke()
    } else if (e.type === 'dasher') {
      // Dasher: orange arrow shape
      ctx.fillStyle = isFlashing ? '#fff' : (eSkin.dasher || '#f97316')
      ctx.beginPath()
      ctx.moveTo(e.x + e.w / 2, e.y + e.h)
      ctx.lineTo(e.x, e.y + e.h * 0.3)
      ctx.lineTo(e.x + e.w * 0.3, e.y)
      ctx.lineTo(e.x + e.w * 0.7, e.y)
      ctx.lineTo(e.x + e.w, e.y + e.h * 0.3)
      ctx.closePath()
      ctx.fill()
      // Speed lines when dashing
      if (e.dashing) {
        ctx.strokeStyle = 'rgba(249,115,22,0.5)'
        ctx.lineWidth = 1.5
        for (let i = 0; i < 3; i++) {
          const ly = e.y - 5 - i * 4
          ctx.beginPath()
          ctx.moveTo(e.x + e.w * 0.2 + i * 3, ly)
          ctx.lineTo(e.x + e.w * 0.8 - i * 3, ly)
          ctx.stroke()
        }
      }
    } else {
      // Default pentagon for any unhandled type
      ctx.fillStyle = isFlashing ? '#fff' : eSkin.normal
      ctx.beginPath()
      ctx.moveTo(e.x + e.w / 2, e.y)
      ctx.lineTo(e.x, e.y + e.h * 0.4)
      ctx.lineTo(e.x + e.w * 0.15, e.y + e.h)
      ctx.lineTo(e.x + e.w * 0.85, e.y + e.h)
      ctx.lineTo(e.x + e.w, e.y + e.h * 0.4)
      ctx.closePath()
      ctx.fill()
    }
    // HP bar
    if (e.maxHp > 1) {
      ctx.fillStyle = '#334155'
      ctx.fillRect(e.x, e.y - 6, e.w, 3)
      ctx.fillStyle = e.hp > e.maxHp * 0.5 ? '#4ade80' : e.hp > e.maxHp * 0.25 ? '#fbbf24' : '#ef4444'
      ctx.fillRect(e.x, e.y - 6, e.w * (e.hp / e.maxHp), 3)
    }
  }

  // Boss
  if (shooterBoss) {
    const b = shooterBoss
    const isFlashing = b.flash > 0
    const bSkin = getEnemySkin()
    const bossPrimary = bSkin.normal
    const bossDark = darkenColor(bossPrimary, 0.7)
    const bossLight = lightenColor(bossPrimary, 1.4)
    const bossAlpha = hexToRgba(bossPrimary, 0.5)
    if (b.type === 'zigzag') {
      ctx.fillStyle = isFlashing ? '#fff' : bossPrimary
      ctx.beginPath()
      ctx.moveTo(b.x + b.w / 2, b.y)
      ctx.lineTo(b.x, b.y + b.h)
      ctx.lineTo(b.x + b.w, b.y + b.h)
      ctx.closePath()
      ctx.fill()
      ctx.fillStyle = isFlashing ? bossLight : bossDark
      ctx.fillRect(b.x - 8, b.y + b.h - 10, 8, 10)
      ctx.fillRect(b.x + b.w, b.y + b.h - 10, 8, 10)
    } else if (b.type === 'swarm') {
      ctx.fillStyle = isFlashing ? '#fff' : bossPrimary
      ctx.beginPath()
      ctx.arc(b.x + b.w / 2, b.y + b.h / 2, b.w / 2, 0, Math.PI * 2)
      ctx.fill()
      ctx.fillStyle = isFlashing ? bossLight : bossDark
      for (let t = 0; t < 4; t++) {
        const tx = b.x + b.w / 2 + Math.cos(Date.now() / 300 + t * 1.5) * 10
        const ty = b.y + b.h + 5 + t * 4
        ctx.fillRect(tx - 2, ty, 4, 12)
      }
    } else if (b.type === 'shield') {
      ctx.fillStyle = isFlashing ? '#fff' : bossPrimary
      const cx = b.x + b.w / 2, cy = b.y + b.h / 2
      ctx.beginPath()
      for (let i = 0; i < 6; i++) {
        const a = Math.PI / 3 * i - Math.PI / 2
        const method = i === 0 ? 'moveTo' : 'lineTo'
        ctx[method](cx + Math.cos(a) * b.w / 2, cy + Math.sin(a) * b.h / 2)
      }
      ctx.closePath()
      ctx.fill()
      if (b.shieldActive) {
        ctx.strokeStyle = bossAlpha
        ctx.lineWidth = 3
        ctx.beginPath()
        ctx.arc(cx, cy, Math.max(b.w, b.h) / 2 + 6, 0, Math.PI * 2)
        ctx.stroke()
      }
    } else if (b.type === 'teleporter') {
      const tcx = b.x + b.w / 2, tcy = b.y + b.h / 2
      if (b.teleporting) {
        ctx.globalAlpha = 0.3 + Math.sin(Date.now() / 30) * 0.3
      }
      ctx.fillStyle = isFlashing ? '#fff' : bossPrimary
      ctx.beginPath()
      for (let i = 0; i < 6; i++) {
        const a = Math.PI / 3 * i - Math.PI / 2
        const method = i === 0 ? 'moveTo' : 'lineTo'
        ctx[method](tcx + Math.cos(a) * b.w / 2, tcy + Math.sin(a) * b.h / 2)
      }
      ctx.closePath()
      ctx.fill()
      ctx.fillStyle = isFlashing ? bossLight : bossDark
      ctx.font = 'bold 18px sans-serif'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText('⚡', tcx, tcy)
      ctx.textAlign = 'left'
      ctx.textBaseline = 'alphabetic'
      ctx.globalAlpha = 1
      if (b.teleportInvincible > 0) {
        ctx.strokeStyle = hexToRgba(bossPrimary, 0.6)
        ctx.lineWidth = 3
        ctx.beginPath()
        ctx.arc(tcx, tcy, Math.max(b.w, b.h) / 2 + 6, 0, Math.PI * 2)
        ctx.stroke()
      }
    } else if (b.type === 'mirror') {
      const mcx = b.x + b.w / 2, mcy = b.y + b.h / 2
      ctx.fillStyle = isFlashing ? '#fff' : bossPrimary
      ctx.beginPath()
      ctx.moveTo(mcx, b.y)
      ctx.lineTo(b.x + b.w, mcy)
      ctx.lineTo(mcx, b.y + b.h)
      ctx.lineTo(b.x, mcy)
      ctx.closePath()
      ctx.fill()
      ctx.strokeStyle = isFlashing ? bossLight : bossDark
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.moveTo(mcx, b.y + 5)
      ctx.lineTo(mcx, b.y + b.h - 5)
      ctx.stroke()
      ctx.beginPath()
      ctx.moveTo(b.x + 5, mcy)
      ctx.lineTo(b.x + b.w - 5, mcy)
      ctx.stroke()
      if (b.mirrorClones) {
        for (const clone of b.mirrorClones) {
          if (clone.hp <= 0) continue
          const ccx = clone.x + clone.w / 2, ccy = clone.y + clone.h / 2
          ctx.globalAlpha = 0.7
          ctx.fillStyle = clone.flash > 0 ? '#fff' : bossLight
          ctx.beginPath()
          ctx.moveTo(ccx, clone.y)
          ctx.lineTo(clone.x + clone.w, ccy)
          ctx.lineTo(ccx, clone.y + clone.h)
          ctx.lineTo(clone.x, ccy)
          ctx.closePath()
          ctx.fill()
          ctx.globalAlpha = 1
          if (clone.flash > 0) clone.flash -= dt
        }
      }
    } else if (b.type === 'gravity') {
      const gcx = b.x + b.w / 2, gcy = b.y + b.h / 2
      ctx.strokeStyle = hexToRgba(bossPrimary, 0.2)
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.arc(gcx, gcy, b.gravityField, 0, Math.PI * 2)
      ctx.stroke()
      for (let i = 0; i < 8; i++) {
        const angle = (Date.now() / 1000 + i * Math.PI / 4) % (Math.PI * 2)
        const innerR = b.w / 2 + 5
        const outerR = b.gravityField - 5
        ctx.strokeStyle = hexToRgba(bossPrimary, 0.15)
        ctx.beginPath()
        ctx.moveTo(gcx + Math.cos(angle) * innerR, gcy + Math.sin(angle) * innerR)
        ctx.lineTo(gcx + Math.cos(angle) * outerR, gcy + Math.sin(angle) * outerR)
        ctx.stroke()
      }
      ctx.fillStyle = isFlashing ? '#fff' : bossPrimary
      ctx.beginPath()
      ctx.arc(gcx, gcy, b.w / 2, 0, Math.PI * 2)
      ctx.fill()
      ctx.fillStyle = isFlashing ? bossLight : bossDark
      ctx.beginPath()
      ctx.arc(gcx, gcy, b.w / 4, 0, Math.PI * 2)
      ctx.fill()
    } else {
      ctx.fillStyle = isFlashing ? '#fff' : bossPrimary
      ctx.beginPath()
      ctx.moveTo(b.x + b.w / 2, b.y)
      ctx.lineTo(b.x, b.y + b.h * 0.6)
      ctx.lineTo(b.x + b.w * 0.2, b.y + b.h)
      ctx.lineTo(b.x + b.w * 0.8, b.y + b.h)
      ctx.lineTo(b.x + b.w, b.y + b.h * 0.6)
      ctx.closePath()
      ctx.fill()
      ctx.fillStyle = isFlashing ? bossLight : bossDark
      ctx.fillRect(b.x - 10, b.y + 15, 10, 25)
      ctx.fillRect(b.x + b.w, b.y + 15, 10, 25)
    }
    // Boss HP bar
    ctx.fillStyle = '#1e293b'
    ctx.fillRect(20, 8, canvasWidth - 40, 8)
    ctx.fillStyle = b.hp > b.maxHp * 0.5 ? '#a78bfa' : b.hp > b.maxHp * 0.25 ? '#fbbf24' : '#ef4444'
    ctx.fillRect(20, 8, (canvasWidth - 40) * Math.max(0, b.hp / b.maxHp), 8)
    ctx.fillStyle = '#e2e8f0'
    ctx.font = 'bold 10px sans-serif'
    ctx.textAlign = 'center'
    const bossNames = { zigzag: t('games.bossZigzag'), swarm: t('games.bossSwarm'), shield: t('games.bossShield'), teleporter: t('games.bossTeleporter'), mirror: t('games.bossMirror'), gravity: t('games.bossGravity') }
    ctx.fillText(bossNames[b.type] || t('games.bossDefault'), canvasWidth / 2, 15)
    ctx.textAlign = 'left'
  }

  // Particles
  for (const p of shooterParticles) {
    ctx.globalAlpha = Math.max(0, p.life / p.maxLife)
    ctx.fillStyle = p.color
    ctx.fillRect(p.x - p.size / 2, p.y - p.size / 2, p.size, p.size)
  }
  ctx.globalAlpha = 1

  // Player ship (skip frames during invincibility for blink effect)
  const shouldDrawPlayer = !shooterInvincible || Math.floor(Date.now() / 80) % 2 === 0
  if (shouldDrawPlayer) {
    const px = shooterPlayer.x, py = shooterPlayer.y
    const skin = getPlayerSkin()
    const cx = px + SHOOTER_PLAYER_W / 2, cy = py + SHOOTER_PLAYER_H / 2
    const w = SHOOTER_PLAYER_W, h = SHOOTER_PLAYER_H

    // Glow effect for rare+ skins
    if (skin.glow) {
      ctx.shadowColor = skin.glow
      ctx.shadowBlur = 12 + Math.sin(Date.now() / 150) * 4
    }

    // Draw ship based on shape
    if (skin.shape === 'swept') {
      // Swept-wing design - wider, more aggressive
      ctx.fillStyle = skin.body
      ctx.beginPath()
      ctx.moveTo(cx, py)
      ctx.lineTo(px - 6, py + h * 0.7)
      ctx.lineTo(px - 2, py + h)
      ctx.lineTo(px + w + 2, py + h)
      ctx.lineTo(px + w + 6, py + h * 0.7)
      ctx.closePath()
      ctx.fill()
      // Forward-swept wings
      ctx.fillStyle = skin.wing
      ctx.beginPath()
      ctx.moveTo(px - 2, py + h * 0.4)
      ctx.lineTo(px - 10, py + h * 0.8)
      ctx.lineTo(px + 2, py + h)
      ctx.closePath()
      ctx.fill()
      ctx.beginPath()
      ctx.moveTo(px + w + 2, py + h * 0.4)
      ctx.lineTo(px + w + 10, py + h * 0.8)
      ctx.lineTo(px + w - 2, py + h)
      ctx.closePath()
      ctx.fill()
    } else if (skin.shape === 'arrow') {
      // Arrow/dart shape - sleek and pointed
      ctx.fillStyle = skin.body
      ctx.beginPath()
      ctx.moveTo(cx, py - 4)
      ctx.lineTo(px - 8, py + h * 0.65)
      ctx.lineTo(px, py + h)
      ctx.lineTo(px + w, py + h)
      ctx.lineTo(px + w + 8, py + h * 0.65)
      ctx.closePath()
      ctx.fill()
      // Delta wings
      ctx.fillStyle = skin.wing
      ctx.beginPath()
      ctx.moveTo(px + 4, py + h * 0.5)
      ctx.lineTo(px - 12, py + h)
      ctx.lineTo(px + 6, py + h)
      ctx.closePath()
      ctx.fill()
      ctx.beginPath()
      ctx.moveTo(px + w - 4, py + h * 0.5)
      ctx.lineTo(px + w + 12, py + h)
      ctx.lineTo(px + w - 6, py + h)
      ctx.closePath()
      ctx.fill()
    } else if (skin.shape === 'dragon') {
      // Dragon shape - organic curves, horn-like wings
      ctx.fillStyle = skin.body
      ctx.beginPath()
      ctx.moveTo(cx, py - 6)
      ctx.quadraticCurveTo(px - 4, py + h * 0.3, px, py + h * 0.7)
      ctx.lineTo(px + 4, py + h)
      ctx.lineTo(px + w - 4, py + h)
      ctx.quadraticCurveTo(px + w, py + h * 0.7, px + w + 4, py + h * 0.3)
      ctx.closePath()
      ctx.fill()
      // Horn wings
      ctx.fillStyle = skin.wing
      ctx.beginPath()
      ctx.moveTo(px + 2, py + h * 0.3)
      ctx.quadraticCurveTo(px - 14, py + h * 0.2, px - 8, py + h * 0.7)
      ctx.lineTo(px + 4, py + h * 0.8)
      ctx.closePath()
      ctx.fill()
      ctx.beginPath()
      ctx.moveTo(px + w - 2, py + h * 0.3)
      ctx.quadraticCurveTo(px + w + 14, py + h * 0.2, px + w + 8, py + h * 0.7)
      ctx.lineTo(px + w - 4, py + h * 0.8)
      ctx.closePath()
      ctx.fill()
    } else if (skin.shape === 'galaxy') {
      // Galaxy shape - ethereal, star-like
      ctx.fillStyle = skin.body
      ctx.beginPath()
      ctx.moveTo(cx, py - 8)
      ctx.lineTo(px + 6, py + h * 0.35)
      ctx.lineTo(px - 4, py + h * 0.6)
      ctx.lineTo(px + 8, py + h)
      ctx.lineTo(px + w - 8, py + h)
      ctx.lineTo(px + w + 4, py + h * 0.6)
      ctx.lineTo(px + w - 6, py + h * 0.35)
      ctx.closePath()
      ctx.fill()
      // Energy wings
      ctx.fillStyle = skin.wing
      ctx.globalAlpha = 0.7
      ctx.beginPath()
      ctx.moveTo(px + 4, py + h * 0.4)
      ctx.lineTo(px - 16, py + h * 0.5)
      ctx.lineTo(px, py + h * 0.9)
      ctx.closePath()
      ctx.fill()
      ctx.beginPath()
      ctx.moveTo(px + w - 4, py + h * 0.4)
      ctx.lineTo(px + w + 16, py + h * 0.5)
      ctx.lineTo(px + w, py + h * 0.9)
      ctx.closePath()
      ctx.fill()
      ctx.globalAlpha = 1
    } else if (skin.shape === 'titan') {
      // Titan shape - heavy, armored, angular
      ctx.fillStyle = skin.body
      ctx.beginPath()
      ctx.moveTo(cx, py)
      ctx.lineTo(px - 2, py + h * 0.3)
      ctx.lineTo(px - 6, py + h * 0.6)
      ctx.lineTo(px - 2, py + h)
      ctx.lineTo(px + w + 2, py + h)
      ctx.lineTo(px + w + 6, py + h * 0.6)
      ctx.lineTo(px + w + 2, py + h * 0.3)
      ctx.closePath()
      ctx.fill()
      // Armor plates
      ctx.fillStyle = skin.wing
      ctx.fillRect(px - 10, py + h * 0.3, 14, 20)
      ctx.fillRect(px + w - 4, py + h * 0.3, 14, 20)
      // Central stripe
      ctx.fillStyle = skin.cockpit
      ctx.fillRect(cx - 3, py + 8, 6, h - 16)
    } else if (skin.shape === 'devourer') {
      // Devourer - black hole with accretion disk
      const dTime = Date.now() / 1000
      // Accretion disk - 3 layers of irregular fragments (batched with setTransform)
      const layerColors = ['#FF6B35', '#FFB627', '#E0E1DD']
      for (let layer = 0; layer < 3; layer++) {
        const r = 18 + layer * 10
        const fragCount = 8 + layer * 2
        ctx.fillStyle = layerColors[layer]
        ctx.globalAlpha = 0.7 - layer * 0.15
        for (let i = 0; i < fragCount; i++) {
          const fa = (Math.PI * 2 / fragCount) * i + dTime * (1.5 - layer * 0.3)
          const fLen = 6 + Math.sin(i * 2.7 + dTime * 3) * 4
          const fx = cx + Math.cos(fa) * r
          const fy = cy + Math.sin(fa) * r * 0.5
          const cos = Math.cos(fa + Math.PI / 4) * canvasDPR
          const sin = Math.sin(fa + Math.PI / 4) * canvasDPR
          ctx.setTransform(cos, sin, -sin, cos, fx * canvasDPR, fy * canvasDPR)
          ctx.fillRect(-fLen / 2, -2, fLen, 4)
        }
      }
      ctx.setTransform(canvasDPR, 0, 0, canvasDPR, 0, 0)
      ctx.globalAlpha = 1
      // Central void
      const voidGrad = ctx.createRadialGradient(cx, cy, 0, cx, cy, 14)
      voidGrad.addColorStop(0, '#0A0A0F')
      voidGrad.addColorStop(0.6, '#0A0A0F')
      voidGrad.addColorStop(1, 'rgba(74,25,66,0.4)')
      ctx.fillStyle = voidGrad
      ctx.beginPath()
      ctx.arc(cx, cy, 14, 0, Math.PI * 2)
      ctx.fill()
      // Dark halo
      ctx.strokeStyle = 'rgba(74,25,66,0.3)'
      ctx.lineWidth = 6
      ctx.beginPath()
      ctx.arc(cx, cy, 20, 0, Math.PI * 2)
      ctx.stroke()
      // Stardust particles
      for (let i = 0; i < 6; i++) {
        const pa = dTime * 2 + i * 1.05
        const pr = 25 + Math.sin(dTime * 3 + i) * 5
        const px2 = cx + Math.cos(pa) * pr
        const py2 = cy + Math.sin(pa) * pr * 0.5
        ctx.fillStyle = i % 2 === 0 ? '#FFFFFF' : '#87CEEB'
        ctx.globalAlpha = 0.4 + Math.sin(dTime * 4 + i) * 0.3
        ctx.beginPath()
        ctx.arc(px2, py2, 1.5, 0, Math.PI * 2)
        ctx.fill()
      }
      ctx.globalAlpha = 1
    } else if (skin.shape === 'kite') {
      // Paper kite - Chinese swallow kite with bamboo skeleton
      const kTime = Date.now() / 1000
      // Wings (semi-transparent silk with ink wash)
      ctx.fillStyle = '#F5F5DC'
      ctx.globalAlpha = 0.75
      ctx.beginPath()
      ctx.moveTo(cx, py + 5)
      ctx.quadraticCurveTo(px - 16, py + h * 0.3, px - 4, py + h * 0.85)
      ctx.lineTo(cx - 2, py + h * 0.7)
      ctx.closePath()
      ctx.fill()
      ctx.beginPath()
      ctx.moveTo(cx, py + 5)
      ctx.quadraticCurveTo(px + w + 16, py + h * 0.3, px + w + 4, py + h * 0.85)
      ctx.lineTo(cx + 2, py + h * 0.7)
      ctx.closePath()
      ctx.fill()
      ctx.globalAlpha = 1
      // Ink wash texture
      ctx.fillStyle = '#2F4F4F'
      ctx.globalAlpha = 0.15
      ctx.beginPath()
      ctx.ellipse(cx - 8, cy - 2, 8, 5, -0.3, 0, Math.PI * 2)
      ctx.fill()
      ctx.beginPath()
      ctx.ellipse(cx + 10, cy + 4, 6, 4, 0.2, 0, Math.PI * 2)
      ctx.fill()
      ctx.globalAlpha = 1
      // Bamboo skeleton
      ctx.strokeStyle = '#8B4513'
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.moveTo(cx, py)
      ctx.lineTo(cx, py + h)
      ctx.stroke()
      ctx.beginPath()
      ctx.moveTo(px - 6, py + h * 0.4)
      ctx.lineTo(px + w + 6, py + h * 0.4)
      ctx.stroke()
      // Diagonal struts
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.moveTo(cx, py + 5)
      ctx.lineTo(px - 4, py + h * 0.85)
      ctx.stroke()
      ctx.beginPath()
      ctx.moveTo(cx, py + 5)
      ctx.lineTo(px + w + 4, py + h * 0.85)
      ctx.stroke()
      // Data ribbons (3 flowing tails)
      const ribbonColors = ['#00CED1', '#FF1493', '#00FF7F']
      for (let i = 0; i < 3; i++) {
        ctx.strokeStyle = ribbonColors[i]
        ctx.lineWidth = 2
        ctx.globalAlpha = 0.6
        ctx.beginPath()
        const rx = cx + (i - 1) * 4
        ctx.moveTo(rx, py + h)
        for (let j = 1; j <= 4; j++) {
          const ry = py + h + j * 8
          const rOff = Math.sin(kTime * 3 + i * 2 + j * 0.8) * (4 + j)
          ctx.lineTo(rx + rOff, ry)
        }
        ctx.stroke()
      }
      ctx.globalAlpha = 1
    } else if (skin.shape === 'jellyfish') {
      // Deep sea jellyfish - translucent dome with tentacle cannons
      const jTime = Date.now() / 1000
      const breathe = 0.9 + Math.sin(jTime * Math.PI) * 0.1
      // Umbrella dome
      ctx.save()
      ctx.translate(cx, cy)
      ctx.scale(1, breathe)
      const domeGrad = ctx.createRadialGradient(0, -5, 0, 0, 0, 22)
      domeGrad.addColorStop(0, 'rgba(0,77,77,0.8)')
      domeGrad.addColorStop(0.5, 'rgba(72,209,204,0.5)')
      domeGrad.addColorStop(1, 'rgba(72,209,204,0.2)')
      ctx.fillStyle = domeGrad
      ctx.beginPath()
      ctx.arc(0, -4, 22, Math.PI, 0)
      ctx.quadraticCurveTo(22, 10, 16, 14)
      ctx.lineTo(-16, 14)
      ctx.quadraticCurveTo(-22, 10, -22, -4)
      ctx.fill()
      // Radial veins
      ctx.strokeStyle = 'rgba(0,255,127,0.3)'
      ctx.lineWidth = 1
      for (let i = 0; i < 6; i++) {
        const va = Math.PI + (Math.PI / 7) * (i + 1)
        ctx.beginPath()
        ctx.moveTo(0, -4)
        ctx.lineTo(Math.cos(va) * 20, Math.sin(va) * 16)
        ctx.stroke()
      }
      // Wavy edge notches
      ctx.fillStyle = 'rgba(72,209,204,0.4)'
      for (let i = 0; i < 8; i++) {
        const na = Math.PI + (Math.PI / 9) * (i + 0.5)
        ctx.beginPath()
        ctx.arc(Math.cos(na) * 22, Math.sin(na) * 14 + 2, 3, 0, Math.PI * 2)
        ctx.fill()
      }
      ctx.restore()
      // Tentacle cannons
      const tentLen = [30, 38, 45, 38, 30]
      const tentColors = ['#20B2AA', '#48D1CC', '#20B2AA', '#48D1CC', '#20B2AA']
      for (let i = 0; i < 5; i++) {
        const tx = px + 6 + (i * (w - 12) / 4)
        ctx.strokeStyle = tentColors[i]
        ctx.lineWidth = 2.5
        ctx.beginPath()
        ctx.moveTo(tx, py + h * 0.7)
        for (let j = 1; j <= 3; j++) {
          const ty = py + h * 0.7 + j * (tentLen[i] / 3)
          const tOff = Math.sin(jTime * 2.5 + i * 1.3 + j * 0.7) * 4
          ctx.lineTo(tx + tOff, ty)
        }
        ctx.stroke()
        // Bio-cannon tips
        const tipX = tx + Math.sin(jTime * 2.5 + i * 1.3 + 2.1) * 4
        const tipY = py + h * 0.7 + tentLen[i]
        ctx.fillStyle = '#008B8B'
        ctx.beginPath()
        ctx.arc(tipX, tipY, 3, 0, Math.PI * 2)
        ctx.fill()
        // Fluorescent glow
        ctx.fillStyle = 'rgba(173,255,47,0.5)'
        ctx.beginPath()
        ctx.arc(tipX, tipY, 2, 0, Math.PI * 2)
        ctx.fill()
      }
      // Core glow
      ctx.fillStyle = 'rgba(173,255,47,0.6)'
      ctx.beginPath()
      ctx.arc(cx, cy - 4, 4, 0, Math.PI * 2)
      ctx.fill()
    } else if (skin.shape === 'glitch') {
      // Glitch entity - pixel blocks with RGB separation (seeded random, frozen per 100ms)
      const gTime = Date.now()
      const gSeed = Math.floor(gTime / 100)
      const gRand = (n) => { let s = (gSeed * 9301 + n * 49297) % 233280; return s / 233280 }
      const blocks = 16
      const rOff = Math.sin(gTime / 100) * 3
      const bOff = -rOff
      const GLITCH_COLORS = ['#FF00FF', '#00FFFF', '#FFFF00', '#FF0000', '#00FF00', '#0000FF']
      for (let i = 0; i < blocks; i++) {
        const bx = px + gRand(i * 6) * w
        const by = py + gRand(i * 6 + 1) * h
        const bw = 8 + gRand(i * 6 + 2) * 16
        const bh = 4 + gRand(i * 6 + 3) * 10
        if (gRand(i * 6 + 4) < 0.3) {
          ctx.fillStyle = '#DC143C'
        } else {
          ctx.fillStyle = GLITCH_COLORS[Math.floor(gRand(i * 6 + 5) * 6)]
        }
        ctx.globalAlpha = 0.6 + gRand(i * 6 + 6) * 0.4
        ctx.fillRect(bx, by, bw, bh)
      }
      ctx.globalAlpha = 1
      ctx.globalCompositeOperation = 'lighter'
      ctx.fillStyle = 'rgba(255,0,0,0.1)'
      ctx.fillRect(px + rOff, py, w, h)
      ctx.fillStyle = 'rgba(0,0,255,0.1)'
      ctx.fillRect(px + bOff, py, w, h)
      ctx.globalCompositeOperation = 'source-over'
      ctx.fillStyle = 'rgba(128,128,128,0.08)'
      for (let sy = py; sy < py + h; sy += 4) {
        ctx.fillRect(px, sy, w, 1)
      }
      if (Math.sin(gTime / 500) > 0.95) {
        ctx.fillStyle = 'rgba(255,255,255,0.3)'
        ctx.fillRect(px - 5, py + gRand(99) * h, w + 10, 3)
      }
    } else if (skin.shape === 'taishui') {
      // Tai Sui - Cthulhu flesh mass with eyes and bone plates
      const tTime = Date.now() / 1000
      const pulse = 0.95 + Math.sin(tTime * Math.PI * 1.2) * 0.05
      ctx.save()
      ctx.translate(cx, cy)
      ctx.scale(pulse, pulse)
      // Flesh body
      ctx.fillStyle = '#CD5C5C'
      ctx.beginPath()
      ctx.moveTo(0, -20)
      ctx.quadraticCurveTo(-22, -10, -18, 8)
      ctx.quadraticCurveTo(-10, 22, 0, 20)
      ctx.quadraticCurveTo(10, 22, 18, 8)
      ctx.quadraticCurveTo(22, -10, 0, -20)
      ctx.fill()
      // Flesh highlights
      ctx.fillStyle = '#F08080'
      ctx.globalAlpha = 0.4
      ctx.beginPath()
      ctx.ellipse(-5, -5, 6, 8, 0.3, 0, Math.PI * 2)
      ctx.fill()
      ctx.globalAlpha = 1
      // Bone plates
      ctx.fillStyle = '#F5F5F5'
      ctx.strokeStyle = '#8B7355'
      ctx.lineWidth = 1
      ctx.beginPath()
      ctx.moveTo(-12, -14)
      ctx.lineTo(-6, -18)
      ctx.lineTo(2, -15)
      ctx.lineTo(-4, -10)
      ctx.closePath()
      ctx.fill()
      ctx.stroke()
      ctx.beginPath()
      ctx.moveTo(6, -12)
      ctx.lineTo(14, -8)
      ctx.lineTo(12, 0)
      ctx.lineTo(4, -4)
      ctx.closePath()
      ctx.fill()
      ctx.stroke()
      // Tentacles
      ctx.strokeStyle = '#8B0000'
      ctx.lineWidth = 2.5
      for (let i = 0; i < 3; i++) {
        const tSide = i === 0 ? -1 : i === 1 ? 1 : 0
        const tBase = i < 2 ? 10 : 18
        ctx.beginPath()
        ctx.moveTo(tSide * 14, tBase)
        for (let j = 1; j <= 3; j++) {
          const ty2 = tBase + j * 8
          const tx2 = tSide * 14 + Math.sin(tTime * 2 + i * 2 + j) * 5
          ctx.lineTo(tx2, ty2)
        }
        ctx.stroke()
      }
      // Eye
      ctx.fillStyle = '#FFFFFF'
      ctx.beginPath()
      ctx.ellipse(0, -2, 7, 8, 0, 0, Math.PI * 2)
      ctx.fill()
      ctx.fillStyle = '#000000'
      const eyeDir = Math.sin(tTime * 0.5) * 2
      ctx.beginPath()
      ctx.arc(eyeDir, -2, 4, 0, Math.PI * 2)
      ctx.fill()
      // Mouth cannon
      ctx.fillStyle = '#1A0000'
      ctx.beginPath()
      ctx.moveTo(-8, 14)
      ctx.quadraticCurveTo(0, 20 + Math.sin(tTime * 3) * 2, 8, 14)
      ctx.quadraticCurveTo(4, 16, 0, 15)
      ctx.quadraticCurveTo(-4, 16, -8, 14)
      ctx.fill()
      // Teeth
      ctx.fillStyle = '#F5F5F5'
      for (let i = 0; i < 5; i++) {
        const tx3 = -6 + i * 3
        ctx.beginPath()
        ctx.moveTo(tx3, 14)
        ctx.lineTo(tx3 + 1, 17)
        ctx.lineTo(tx3 + 2, 14)
        ctx.fill()
      }
      ctx.restore()
      // Blood drips
      ctx.fillStyle = '#DC143C'
      ctx.globalAlpha = 0.6
      for (let i = 0; i < 2; i++) {
        const dx = cx + Math.sin(tTime + i * 3) * 10
        const dy = py + h + (tTime * 20 + i * 15) % 20
        ctx.beginPath()
        ctx.arc(dx, dy, 2, 0, Math.PI * 2)
        ctx.fill()
      }
      ctx.globalAlpha = 1
    } else if (skin.shape === 'neonrider') {
      // Neon Knight - cyberpunk hover motorcycle
      const nTime = Date.now() / 1000
      // Motorcycle body
      ctx.fillStyle = '#1A1A2E'
      ctx.beginPath()
      ctx.moveTo(cx - 22, cy + 2)
      ctx.quadraticCurveTo(cx - 28, cy - 6, cx - 18, cy - 10)
      ctx.lineTo(cx + 18, cy - 10)
      ctx.quadraticCurveTo(cx + 28, cy - 6, cx + 22, cy + 2)
      ctx.lineTo(cx + 20, cy + 6)
      ctx.lineTo(cx - 20, cy + 6)
      ctx.closePath()
      ctx.fill()
      // Neon tubes
      const neonColors = ['#FF0080', '#00FF80', '#8000FF']
      const neonAlpha = 0.6 + Math.sin(nTime * Math.PI) * 0.3
      ctx.globalAlpha = neonAlpha
      // Front wheel neon
      ctx.strokeStyle = '#FF0080'
      ctx.lineWidth = 2.5
      ctx.beginPath()
      ctx.arc(cx - 16, cy + 4, 6, 0, Math.PI * 2)
      ctx.stroke()
      // Rear wheel neon
      ctx.strokeStyle = '#00FF80'
      ctx.beginPath()
      ctx.arc(cx + 16, cy + 4, 8, 0, Math.PI * 2)
      ctx.stroke()
      // Body outline neon
      ctx.strokeStyle = '#8000FF'
      ctx.lineWidth = 1.5
      ctx.beginPath()
      ctx.moveTo(cx - 22, cy + 2)
      ctx.quadraticCurveTo(cx - 28, cy - 6, cx - 18, cy - 10)
      ctx.lineTo(cx + 18, cy - 10)
      ctx.quadraticCurveTo(cx + 28, cy - 6, cx + 22, cy + 2)
      ctx.stroke()
      ctx.globalAlpha = 1
      // Holographic rider silhouette
      ctx.fillStyle = '#00FFFF'
      ctx.globalAlpha = 0.5 + Math.sin(nTime * 4) * 0.15
      ctx.beginPath()
      // Head
      ctx.arc(cx - 2, cy - 20, 5, 0, Math.PI * 2)
      ctx.fill()
      // Body
      ctx.beginPath()
      ctx.moveTo(cx - 6, cy - 14)
      ctx.lineTo(cx + 4, cy - 14)
      ctx.lineTo(cx + 2, cy - 6)
      ctx.lineTo(cx - 4, cy - 6)
      ctx.closePath()
      ctx.fill()
      ctx.globalAlpha = 1
      // Headlight
      ctx.fillStyle = '#FFFFFF'
      ctx.globalAlpha = 0.8
      ctx.beginPath()
      ctx.ellipse(cx - 24, cy - 4, 3, 2, 0, 0, Math.PI * 2)
      ctx.fill()
      ctx.globalAlpha = 1
      // Turbo exhaust particles (deterministic)
      for (let i = 0; i < 4; i++) {
        const ex = cx + 24 + ((nTime * 7 + i * 3.7) % 1) * 8
        const ey = cy + 2 + (Math.sin(nTime * 5 + i * 2.3) * 3)
        ctx.fillStyle = i % 2 === 0 ? '#00FFFF' : '#FF00FF'
        ctx.globalAlpha = 0.3 + Math.abs(Math.sin(nTime * 3 + i * 1.7)) * 0.3
        ctx.beginPath()
        ctx.arc(ex, ey, 1.5, 0, Math.PI * 2)
        ctx.fill()
      }
      ctx.globalAlpha = 1
    } else {
      // Standard shape (default + basic color swaps)
      ctx.fillStyle = skin.body
      ctx.beginPath()
      ctx.moveTo(cx, py)
      ctx.lineTo(px + 4, py + h * 0.6)
      ctx.lineTo(px, py + h)
      ctx.lineTo(px + w, py + h)
      ctx.lineTo(px + w - 4, py + h * 0.6)
      ctx.closePath()
      ctx.fill()
      // Wings
      ctx.fillStyle = skin.wing
      ctx.fillRect(px - 4, py + h * 0.5, 8, 14)
      ctx.fillRect(px + w - 4, py + h * 0.5, 8, 14)
    }

    // Skip common cockpit/engine for self-contained shapes
    const skipCommon = ['devourer', 'jellyfish', 'glitch', 'taishui', 'neonrider', 'kite'].includes(skin.shape)
    if (!skipCommon) {
      // Cockpit (common)
      ctx.fillStyle = skin.cockpit
      ctx.beginPath()
      ctx.ellipse(cx, py + 16, 4, 6, 0, 0, Math.PI * 2)
      ctx.fill()

      // Engine glow (common, animated)
      const glowH = 6 + Math.sin(Date.now() / 50) * 3
      ctx.fillStyle = skin.engine
      ctx.beginPath()
      ctx.moveTo(px + 10, py + h)
      ctx.lineTo(cx, py + h + glowH)
      ctx.lineTo(px + w - 10, py + h)
      ctx.closePath()
      ctx.fill()
      ctx.fillStyle = skin.engine
      ctx.globalAlpha = 0.6
      ctx.beginPath()
      ctx.moveTo(px + 14, py + h)
      ctx.lineTo(cx, py + h + glowH * 0.6)
      ctx.lineTo(px + w - 14, py + h)
      ctx.closePath()
      ctx.fill()
      ctx.globalAlpha = 1
    }

    // Reset shadow
    ctx.shadowBlur = 0

    // Trail effect for legendary skins
    if (skin.trail) {
      ctx.globalAlpha = 0.3
      ctx.fillStyle = skin.trail
      for (let i = 1; i <= 3; i++) {
        const trailH = 4 + i * 3
        ctx.globalAlpha = 0.15 / i
        ctx.beginPath()
        ctx.moveTo(cx - 6 + i, py + h + i * 6)
        ctx.lineTo(cx, py + h + trailH + i * 8)
        ctx.lineTo(cx + 6 - i, py + h + i * 6)
        ctx.closePath()
        ctx.fill()
      }
      ctx.globalAlpha = 1
    }

    // Invincibility shield ring
    if (shooterInvincible) {
      ctx.strokeStyle = 'rgba(96,165,250,0.5)'
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.arc(cx, cy, w / 2 + 4, 0, Math.PI * 2)
      ctx.stroke()
    }
    // Shield powerup - rotating energy ring
    if (shooterShield) {
      const t = Date.now() / 300
      ctx.strokeStyle = '#22d3ee'
      ctx.lineWidth = 2
      ctx.shadowColor = '#22d3ee'
      ctx.shadowBlur = 8
      for (let i = 0; i < 3; i++) {
        const a = t + (i * Math.PI * 2 / 3)
        ctx.beginPath()
        ctx.arc(cx, cy, w / 2 + 8, a, a + 1.2)
        ctx.stroke()
      }
      ctx.shadowBlur = 0
    }
  }

  // Score popups
  for (const sp of shooterScorePopups) {
    ctx.globalAlpha = Math.max(0, sp.life / sp.maxLife)
    ctx.fillStyle = '#fbbf24'
    ctx.font = 'bold 14px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(sp.text, sp.x, sp.y)
  }
  ctx.globalAlpha = 1
  ctx.textAlign = 'left'

  // Boss defeated celebration overlay
  if (shooterBossDefeated) {
    const bd = shooterBossDefeated
    const progress = 1 - bd.timer / 2500
    const alpha = progress < 0.1 ? progress / 0.1 : progress > 0.7 ? (1 - progress) / 0.3 : 1
    ctx.globalAlpha = alpha * 0.85
    ctx.fillStyle = '#0f172a'
    ctx.fillRect(0, canvasHeight.value / 2 - 50, canvasWidth, 100)
    ctx.globalAlpha = alpha
    ctx.fillStyle = '#fbbf24'
    ctx.font = 'bold 22px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText('⚔️ ' + t('games.bossDefeated') + ' ⚔️', canvasWidth / 2, canvasHeight.value / 2 - 15)
    const bossNames = { zigzag: t('games.bossZigzag'), swarm: t('games.bossSwarm'), shield: t('games.bossShield') }
    ctx.fillStyle = '#e2e8f0'
    ctx.font = '16px sans-serif'
    ctx.fillText((bossNames[bd.name] || t('games.bossDefault')) + '  +' + bd.points + ' ' + t('games.score'), canvasWidth / 2, canvasHeight.value / 2 + 15)
    ctx.globalAlpha = 1
    ctx.textAlign = 'left'
  }

  // HUD - top left
  // Lives
  ctx.fillStyle = '#f8fafc'
  ctx.font = '12px sans-serif'
  ctx.fillText('❤️'.repeat(shooterLives) + '🖤'.repeat(Math.max(0, shooterMaxLives - shooterLives)), 8, 20)
  // Shield indicator
  if (shooterShield) {
    ctx.fillStyle = '#22d3ee'
    ctx.font = 'bold 11px sans-serif'
    ctx.fillText(t('games.hudShield'), 8, 36)
  }
  // Powerup indicator
  if (shooterPowerupType) {
    const puColor = shooterPowerupType === 'spread' ? '#a78bfa' : '#fbbf24'
    const puName = shooterPowerupType === 'spread' ? t('games.hudSpread') : t('games.hudRapid')
    ctx.fillStyle = puColor
    ctx.font = 'bold 11px sans-serif'
    ctx.fillText(puName, 8, shooterShield ? 50 : 36)
    const barW = 50, barY = shooterShield ? 54 : 40
    ctx.fillStyle = '#334155'
    ctx.fillRect(8, barY, barW, 3)
    ctx.fillStyle = puColor
    ctx.fillRect(8, barY, barW * (shooterPowerupTimer / 8000), 3)
  }
  if (shooterLaser) {
    ctx.fillStyle = '#f8fafc'
    ctx.font = 'bold 11px sans-serif'
    ctx.fillText(t('games.hudLaser') + ' ' + Math.ceil(shooterLaserTimer / 1000) + 's', 8, 64)
  }
  if (shooterHoming) {
    ctx.fillStyle = '#c084fc'
    ctx.font = 'bold 11px sans-serif'
    ctx.fillText(t('games.hudHoming') + ' ' + Math.ceil(shooterHomingTimer / 1000) + 's', 8, 78)
  }
  if (shooterDoubleScore) {
    ctx.fillStyle = '#f472b6'
    ctx.font = 'bold 12px sans-serif'
    ctx.fillText(t('games.hudDouble') + ' ' + Math.ceil(shooterDoubleScoreTimer / 1000) + 's', 8, 92)
  }
  if (shooterSlowEnemy) {
    ctx.fillStyle = '#60a5fa'
    ctx.font = 'bold 11px sans-serif'
    ctx.fillText('⏳ ' + Math.ceil(shooterSlowEnemyTimer / 1000) + 's', 8, shooterDoubleScore ? 106 : 92)
  }
  if (shooterMagnet) {
    ctx.fillStyle = '#fbbf24'
    ctx.font = 'bold 11px sans-serif'
    const magnetY = (shooterDoubleScore ? 106 : 92) + (shooterSlowEnemy ? 14 : 0)
    ctx.fillText('🧲 ' + Math.ceil(shooterMagnetTimer / 1000) + 's', 8, magnetY)
  }
  // Level
  ctx.fillStyle = '#94a3b8'
  ctx.font = '12px sans-serif'
  ctx.textAlign = 'right'
  ctx.fillText(`${t('games.hudLevel')}${shooterLevel}`, canvasWidth - 8, 20)
  // Combo
  if (shooterCombo >= 3) {
    const comboColor = shooterCombo >= 20 ? '#f43f5e' : shooterCombo >= 10 ? '#f59e0b' : shooterCombo >= 5 ? '#a78bfa' : '#60a5fa'
    ctx.fillStyle = comboColor
    ctx.font = shooterCombo >= 10 ? 'bold 18px sans-serif' : 'bold 16px sans-serif'
    const multiplier = shooterCombo >= 20 ? 'x5' : shooterCombo >= 10 ? 'x3' : 'x2'
    ctx.fillText(`${shooterCombo} ${t('games.hudCombo')} ${multiplier}`, canvasWidth - 8, 40)
  }
  ctx.textAlign = 'left'

  // Active skill buttons (bottom right) - 3 slots
  const btnR = 20
  for (let si = 0; si < 3; si++) {
    const btnX = canvasWidth - btnR - 12 - si * (btnR * 2 + 8)
    const btnY = canvasHeight.value - btnR - 12
    const skillId = shooterEquippedSkills.value[si]
    const cdTimer = shooterSkillCooldownTimers[si]
    const skillReady = cdTimer <= 0
    const cooldownMax = skillId ? (SHOP_SKILLS[skillId]?.cooldown || 60000) : 60000
    // Button background
    ctx.globalAlpha = skillReady ? 0.85 : 0.4
    ctx.fillStyle = skillReady ? "#3b82f6" : "#475569"
    ctx.beginPath()
    ctx.arc(btnX, btnY, btnR, 0, Math.PI * 2)
    ctx.fill()
    // Cooldown ring
    if (!skillReady) {
      const cdRatio = cdTimer / cooldownMax
      ctx.strokeStyle = "#94a3b8"
      ctx.lineWidth = 3
      ctx.beginPath()
      ctx.arc(btnX, btnY, btnR, -Math.PI / 2, -Math.PI / 2 + Math.PI * 2 * (1 - cdRatio))
      ctx.stroke()
    }
    // Icon
    ctx.globalAlpha = skillReady ? 1 : 0.5
    ctx.fillStyle = "#fff"
    ctx.font = "bold 14px sans-serif"
    ctx.textAlign = "center"
    ctx.textBaseline = "middle"
    const icon = skillId ? (SHOP_SKILLS[skillId]?.icon || "🛡") : "🛡"
    ctx.fillText(icon, btnX, btnY)
    // Key hint
    ctx.font = "9px sans-serif"
    ctx.fillStyle = "#94a3b8"
    ctx.fillText(si === 0 ? "Q" : si === 1 ? "E" : "R", btnX, btnY + btnR + 8)
    ctx.textAlign = "left"
    ctx.textBaseline = "alphabetic"
    ctx.globalAlpha = 1
    // Ready pulse
    if (skillReady) {
      const pulse = Math.sin(Date.now() / 200) * 0.15 + 0.25
      ctx.strokeStyle = "rgba(59,130,246," + pulse + ")"
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.arc(btnX, btnY, btnR + 4, 0, Math.PI * 2)
      ctx.stroke()
    }
  }
  // Active skill indicator
  if (shooterActiveSkillActive) {
    ctx.fillStyle = "#60a5fa"
    ctx.font = "bold 11px sans-serif"
    ctx.textAlign = "right"
    ctx.fillText(t("games.hudInvincible") + " " + Math.ceil(shooterActiveSkillDuration / 1000) + "s", canvasWidth - 8, canvasHeight.value - btnR * 2 - 20)
    ctx.textAlign = "left"
  }

  // Coins display (top right, below level)
  if (shooterCosmetics.value) {
    ctx.fillStyle = '#fbbf24'
    ctx.font = '11px sans-serif'
    ctx.textAlign = 'right'
    ctx.fillText('💰 ' + shooterCosmetics.value.coins, canvasWidth - 8, 34)
    ctx.textAlign = 'left'
  }

  ctx.restore()
}

function onCanvasClick(e) {
  if (currentGame.value === 'flappy' && gameState.value === 'playing') {
    flap()
  } else if (currentGame.value === 'shooter' && gameState.value === 'playing') {
    // Check if click is on active skill button
    const clickedSlot = getClickedSkillSlot(e)
    if (clickedSlot >= 0) {
      activateShooterSkill(clickedSlot)
    } else {
      shooterFire()
    }
  } else if (currentGame.value === 'chineseChess' && gameState.value === 'playing') {
    handleChessCanvasClick(e)
  } else if (currentGame.value === 'minesweeper') {
    if (gameState.value === 'over' || gameState.value === 'won') { startGame(); return }
    if (gameState.value !== 'playing') return
    const canvas = canvasRef.value; if (!canvas) return
    const rect = canvas.getBoundingClientRect()
    const scaleX = 400 / rect.width, scaleY = 400 / rect.height
    const mx = (e.clientX - rect.left) * scaleX, my = (e.clientY - rect.top) * scaleY
    const cellSize = Math.floor(Math.min((400 - 20) / msCols, (400 - 50) / msRows))
    const offsetX = Math.floor((400 - cellSize * msCols) / 2), offsetY = 40
    const c = Math.floor((mx - offsetX) / cellSize), r = Math.floor((my - offsetY) / cellSize)
    if (r < 0 || r >= msRows || c < 0 || c >= msCols) return
    msCursorR = r; msCursorC = c
    if (e.button === 2) { msToggleFlag(r, c); return }
    if (msFirstClick) { msPlaceMines(r, c); msStartTime = Date.now(); msFirstClick = false }
    msReveal(r, c)
  } else if (currentGame.value === 'fruitninja') {
    if (gameState.value === 'over' || gameState.value === 'won') { startGame(); return }
  }
}

function onCanvasRightClick(e) {
  if (currentGame.value !== 'minesweeper' || gameState.value !== 'playing') return
  const canvas = canvasRef.value; if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const scaleX = 400 / rect.width, scaleY = 400 / rect.height
  const mx = (e.clientX - rect.left) * scaleX, my = (e.clientY - rect.top) * scaleY
  const cellSize = Math.floor(Math.min((400 - 20) / msCols, (400 - 50) / msRows))
  const offsetX = Math.floor((400 - cellSize * msCols) / 2), offsetY = 40
  const c = Math.floor((mx - offsetX) / cellSize), r = Math.floor((my - offsetY) / cellSize)
  if (r >= 0 && r < msRows && c >= 0 && c < msCols) msToggleFlag(r, c)
}

function getClickedSkillSlot(e) {
  const canvas = canvasRef.value
  if (!canvas) return -1
  const rect = canvas.getBoundingClientRect()
  const scaleX = canvasWidth / rect.width
  const scaleY = canvasHeight.value / rect.height
  const cx = (e.clientX - rect.left) * scaleX
  const cy = (e.clientY - rect.top) * scaleY
  const btnR = 20
  for (let si = 0; si < 3; si++) {
    const btnX = canvasWidth - btnR - 12 - si * (btnR * 2 + 8)
    const btnY = canvasHeight.value - btnR - 12
    const dx = cx - btnX, dy = cy - btnY
    if (dx * dx + dy * dy <= (btnR + 10) * (btnR + 10)) return si
  }
  return -1
}

function activateShooterSkill(slotIndex) {
  // slotIndex: 0 or 1. If not provided, activate default (slot 0) or legacy invincible
  if (slotIndex === undefined) slotIndex = 0
  if (shooterSkillCooldownTimers[slotIndex] > 0) return
  const skillId = shooterEquippedSkills.value[slotIndex]
  if (!skillId) {
    // No skill equipped - use default invincible (legacy behavior)
    if (shooterActiveSkillCooldownTimer > 0) return
    shooterActiveSkillActive = true
    shooterActiveSkillDuration = 5000
    SFX.play('skill')
    shooterInvincible = true
    shooterInvincibleTimer = 5000
    shooterActiveSkillCooldownTimer = 60000
    shooterActiveSkillCooldown.value = 60000
    spawnParticles(shooterPlayer.x + SHOOTER_PLAYER_W / 2, shooterPlayer.y + SHOOTER_PLAYER_H / 2, '#60a5fa', 20)
    return
  }
  // Activate the equipped skill
  const px = shooterPlayer.x + SHOOTER_PLAYER_W / 2
  const py = shooterPlayer.y + SHOOTER_PLAYER_H / 2
  const cooldown = SHOP_SKILLS[skillId]?.cooldown || 60000
  shooterSkillCooldownTimers[slotIndex] = cooldown
  shooterSkillCooldowns.value[slotIndex] = cooldown
  SFX.play('skill')
  achievementStats.value.skillsUsed++
  checkAchievements(achievementStats.value)
  if (skillId === 'shield_skill') {
    shooterInvincible = true
    shooterInvincibleTimer = 5000
    shooterShield = true
    spawnParticles(px, py, '#22d3ee', 20)
  } else if (skillId === 'bomb_skill') {
    SFX.play('bomb')
    for (const e of shooterEnemies) {
      shooterCombo++; shooterComboTimer = 3000; shooterMaxCombo.value = Math.max(shooterMaxCombo.value, shooterCombo)
      const mult = shooterCombo >= 20 ? 5 : shooterCombo >= 10 ? 3 : shooterCombo >= 5 ? 2 : 1
      const pts = e.points * mult * (shooterDoubleScore ? 2 : 1)
      score.value += pts
      if (shooterDoubleScore) { achievementStats.value.doubleScoreKills++; checkAchievements(achievementStats.value) }
      achievementStats.value.totalKills++
      spawnParticles(e.x + e.w / 2, e.y + e.h / 2, '#fbbf24', 6)
      spawnScorePopup(e.x + e.w / 2, e.y, `+${pts}`)
      if (Math.random() < 0.2) spawnPowerup(e.x + e.w / 2, e.y + e.h)
    }
    if (shooterBoss) {
      shooterCombo++; shooterComboTimer = 3000; shooterMaxCombo.value = Math.max(shooterMaxCombo.value, shooterCombo)
      const mult = shooterCombo >= 20 ? 5 : shooterCombo >= 10 ? 3 : shooterCombo >= 5 ? 2 : 1
      const pts = shooterBoss.points * mult * (shooterDoubleScore ? 2 : 1)
      score.value += pts
      spawnParticles(shooterBoss.x + shooterBoss.w / 2, shooterBoss.y + shooterBoss.h / 2, '#fbbf24', 20)
      spawnParticles(shooterBoss.x + shooterBoss.w / 2, shooterBoss.y + shooterBoss.h / 2, '#f43f5e', 15)
      spawnScorePopup(shooterBoss.x + shooterBoss.w / 2, shooterBoss.y, `+${pts}`)
      SFX.play('bossDie')
      achievementStats.value.bossKills++
      checkAchievements(achievementStats.value)
      shooterBossDefeated = { name: shooterBoss.type, points: pts, timer: 2500 }
      spawnPowerup(shooterBoss.x + shooterBoss.w / 2, shooterBoss.y + shooterBoss.h)
      shooterBoss = null
    }
    shooterEnemies = []
    shooterEnemyBullets = []
    checkAchievements(achievementStats.value)
    spawnParticles(px, py, '#f43f5e', 25)
  } else if (skillId === 'slow_skill') {
    shooterSlowEnemy = true
    shooterSlowEnemyTimer = 5000
    spawnParticles(px, py, '#60a5fa', 15)
  } else if (skillId === 'magnet_skill') {
    shooterMagnet = true
    shooterMagnetTimer = 8000
    spawnParticles(px, py, '#fbbf24', 15)
  } else if (skillId === 'rage_skill') {
    shooterPowerupType = 'firerate'
    shooterPowerupTimer = 8000
    spawnParticles(px, py, '#f43f5e', 15)
  } else if (skillId === 'heal_skill') {
    shooterLives = Math.min(shooterMaxLives, shooterLives + 2)
    spawnParticles(px, py, '#22c55e', 15)
  }
}

// Canvas touch handlers for snake swipe + shooter/breakout drag
let shooterDragStartX = 0
let shooterDragPlayerStartX = 0
let breakoutDragStartX = 0
let breakoutDragPaddleStartX = 0
let canvasTouchStartX = 0
let canvasTouchStartY = 0
let msTouchTimer = null
let msTouchFired = false

function handleMinesweeperTouch(cx, cy, isFlag) {
  const canvas = canvasRef.value; if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const scaleX = 400 / rect.width, scaleY = 400 / rect.height
  const mx = (cx - rect.left) * scaleX, my = (cy - rect.top) * scaleY
  const cellSize = Math.floor(Math.min((400 - 20) / msCols, (400 - 50) / msRows))
  const offsetX = Math.floor((400 - cellSize * msCols) / 2), offsetY = 40
  const c = Math.floor((mx - offsetX) / cellSize), r = Math.floor((my - offsetY) / cellSize)
  if (r < 0 || r >= msRows || c < 0 || c >= msCols) return
  msCursorR = r; msCursorC = c
  if (isFlag) { msToggleFlag(r, c); return }
  if (msFirstClick) { msPlaceMines(r, c); msStartTime = Date.now(); msFirstClick = false }
  msReveal(r, c)
}

function msMove(dir) {
  if (gameState.value !== 'playing') return
  if (dir === 'up') msCursorR = Math.max(0, msCursorR - 1)
  else if (dir === 'down') msCursorR = Math.min(msRows - 1, msCursorR + 1)
  else if (dir === 'left') msCursorC = Math.max(0, msCursorC - 1)
  else if (dir === 'right') msCursorC = Math.min(msCols - 1, msCursorC + 1)
}
function msActionReveal() {
  if (gameState.value !== 'playing') return
  if (msFirstClick) { msPlaceMines(msCursorR, msCursorC); msStartTime = Date.now(); msFirstClick = false }
  msReveal(msCursorR, msCursorC)
}
function msActionFlag() {
  if (gameState.value !== 'playing') return
  msToggleFlag(msCursorR, msCursorC)
}

function getCanvasScale() {
  const canvas = canvasRef.value
  if (!canvas) return 1
  return canvasWidth / canvas.getBoundingClientRect().width
}

let canvasTouchActive = false

function isClickOnSkillButton(e) {
  return getClickedSkillSlot(e) >= 0
}

function onCanvasTouchStart(e) {
  if (e.touches.length !== 1) return
  const touch = e.touches[0]
  canvasTouchStartX = touch.clientX
  canvasTouchStartY = touch.clientY
  canvasTouchActive = true

  if (currentGame.value === 'flappy' && gameState.value === 'playing') {
    e.preventDefault() // suppress synthesized click to prevent double-flap
    flap()
  } else if (currentGame.value === 'shooter' && gameState.value === 'playing') {
    e.preventDefault()
    // Check if touch is on active skill button
    const fakeEvent = { clientX: touch.clientX, clientY: touch.clientY }
    if (isClickOnSkillButton(fakeEvent)) {
      activateShooterSkill(getClickedSkillSlot(fakeEvent))
      return
    }
    shooterDragStartX = touch.clientX
    shooterDragPlayerStartX = shooterPlayer.x
    shooterFire()
  } else if (currentGame.value === 'breakout' && gameState.value === 'playing') {
    e.preventDefault()
    breakoutDragStartX = touch.clientX
    breakoutDragPaddleStartX = paddle.x
  } else if (currentGame.value === 'chineseChess' && gameState.value === 'playing') {
    e.preventDefault()
    handleChessCanvasClick({ clientX: touch.clientX, clientY: touch.clientY })
  } else if (currentGame.value === 'minesweeper' && gameState.value === 'playing') {
    e.preventDefault()
    msTouchTimer = setTimeout(() => {
      msTouchFired = true
      handleMinesweeperTouch(touch.clientX, touch.clientY, true)
    }, 400)
    msTouchFired = false
  } else if (currentGame.value === 'fruitninja' && gameState.value === 'playing') {
    e.preventDefault()
    fnSlicing = true
    const canvas = canvasRef.value; if (!canvas) return
    const rect = canvas.getBoundingClientRect()
    fnSliceX = (touch.clientX - rect.left) * (400 / rect.width)
    fnSliceY = (touch.clientY - rect.top) * (400 / rect.height)
    fnLastSliceX = fnSliceX; fnLastSliceY = fnSliceY
    fnSliceTrail.push({ x: fnSliceX, y: fnSliceY, time: Date.now() })
  }
}

function onCanvasTouchMove(e) {
  if (e.touches.length !== 1) return
  const touch = e.touches[0]
  const dx = touch.clientX - canvasTouchStartX
  const dy = touch.clientY - canvasTouchStartY

  if (currentGame.value === 'shooter' && gameState.value === 'playing') {
    const scale = getCanvasScale()
    const moveDx = (touch.clientX - shooterDragStartX) * scale
    shooterPlayer.x = Math.max(0, Math.min(canvasWidth - SHOOTER_PLAYER_W, shooterDragPlayerStartX + moveDx))
  } else if (currentGame.value === 'breakout' && gameState.value === 'playing') {
    const scale = getCanvasScale()
    const moveDx = (touch.clientX - breakoutDragStartX) * scale
    paddle.x = Math.max(0, Math.min(canvasWidth - paddle.w, breakoutDragPaddleStartX + moveDx))
  } else if (currentGame.value === 'snake' && gameState.value === 'playing') {
    const absDx = Math.abs(dx)
    const absDy = Math.abs(dy)
    if (Math.max(absDx, absDy) > 20) {
      if (absDx > absDy) setDirection(dx > 0 ? 'right' : 'left')
      else setDirection(dy > 0 ? 'down' : 'up')
      canvasTouchStartX = touch.clientX
      canvasTouchStartY = touch.clientY
    }
  } else if (currentGame.value === 'fruitninja' && fnSlicing && gameState.value === 'playing') {
    e.preventDefault()
    const canvas = canvasRef.value; if (!canvas) return
    const rect = canvas.getBoundingClientRect()
    const nx = (touch.clientX - rect.left) * (400 / rect.width)
    const ny = (touch.clientY - rect.top) * (400 / rect.height)
    fnCheckSlice(fnLastSliceX, fnLastSliceY, nx, ny)
    fnSliceTrail.push({ x: nx, y: ny, time: Date.now() })
    fnLastSliceX = nx; fnLastSliceY = ny
  }
}

function onCanvasTouchEnd(e) {
  canvasTouchActive = false
  if (currentGame.value === 'minesweeper' && msTouchTimer) {
    clearTimeout(msTouchTimer); msTouchTimer = null
    if (!msTouchFired && gameState.value === 'playing') {
      const touch = e.changedTouches[0]
      if (touch) handleMinesweeperTouch(touch.clientX, touch.clientY, false)
    }
  }
  if (currentGame.value === 'fruitninja') fnSlicing = false
}

function buildGridCanvas(cellSize, cols, rows) {
  const w = cols * cellSize
  const h = rows * cellSize
  gridCanvas = document.createElement('canvas')
  gridCanvas.width = w * canvasDPR
  gridCanvas.height = h * canvasDPR
  const gctx = gridCanvas.getContext('2d')
  gctx.scale(canvasDPR, canvasDPR)
  gctx.strokeStyle = '#e2e8f0'
  gctx.lineWidth = 0.5
  for (let i = 0; i <= cols; i++) {
    gctx.beginPath(); gctx.moveTo(i * cellSize, 0); gctx.lineTo(i * cellSize, h); gctx.stroke()
  }
  for (let i = 0; i <= rows; i++) {
    gctx.beginPath(); gctx.moveTo(0, i * cellSize); gctx.lineTo(w, i * cellSize); gctx.stroke()
  }
  gridCanvasDirty = false
}

// ===== Game Loop =====
function applyCanvasDPR() {
  const canvas = canvasRef.value
  if (!canvas) return
  canvasDPR = Math.min(window.devicePixelRatio || 1, 3)
  canvas.width = canvasWidth * canvasDPR
  canvas.height = canvasHeight.value * canvasDPR
  cachedCtx = canvas.getContext('2d')
  cachedCtx.scale(canvasDPR, canvasDPR)
  gridCanvasDirty = true
  flappyGrad = null
  shooterGrad = null
}

function ensureCtx() {
  if (!cachedCtx) cachedCtx = canvasRef.value?.getContext('2d')
  return cachedCtx
}

function gameLoop(timestamp) {
  if (gameState.value !== 'playing') {
    // Flappy idle bob needs continuous rendering
    if (currentGame.value === 'flappy' && gameState.value === 'idle') {
      const ctx = ensureCtx()
      if (ctx) { updateFlappy(16); drawFlappy(ctx) }
      animationId = requestAnimationFrame(gameLoop)
      return
    }
    // Paused/over/won: draw final state once, then stop loop
    if (gameState.value === 'paused' || gameState.value === 'over' || gameState.value === 'won') {
      canvasTouchLocked.value = false
      const ctx = ensureCtx()
      if (ctx) {
        if (currentGame.value === 'snake') drawSnake(ctx)
        else if (currentGame.value === 'tetris') drawTetris(ctx)
        else if (currentGame.value === 'game2048') draw2048(ctx)
        else if (currentGame.value === 'breakout') drawBreakout(ctx)
        else if (currentGame.value === 'shooter') drawShooter(ctx)
        else if (currentGame.value === 'chineseChess') drawChineseChess(ctx)
        else if (currentGame.value === 'minesweeper') drawMinesweeper(ctx)
        else if (currentGame.value === 'fruitninja') drawFruitNinja(ctx)
      }
    }
    // Stop the loop — startGame() will restart it
    animationId = null
    return
  }

  const dt = Math.min(lastTime ? timestamp - lastTime : 16, 100)
  lastTime = timestamp

  const ctx = ensureCtx()
  if (!ctx) { animationId = null; return }

  if (currentGame.value === 'snake') {
    updateSnake(dt)
    drawSnake(ctx)
  } else if (currentGame.value === 'tetris') {
    updateTetris(dt)
    drawTetris(ctx)
  } else if (currentGame.value === 'game2048') {
    draw2048(ctx)
  } else if (currentGame.value === 'flappy') {
    updateFlappy(dt)
    drawFlappy(ctx)
  } else if (currentGame.value === 'breakout') {
    updateBreakout(dt)
    drawBreakout(ctx)
  } else if (currentGame.value === 'shooter') {
    updateShooter(dt)
    drawShooter(ctx)
  } else if (currentGame.value === 'chineseChess') {
    drawChineseChess(ctx)
  } else if (currentGame.value === 'minesweeper') {
    updateMinesweeper(dt)
    drawMinesweeper(ctx)
  } else if (currentGame.value === 'fruitninja') {
    updateFruitNinja(dt)
    drawFruitNinja(ctx)
  }

  animationId = requestAnimationFrame(gameLoop)
}

function startGame() {
  SFX.init()
  if (gameState.value === 'playing') return
  if (gameState.value === 'idle' || gameState.value === 'over' || gameState.value === 'won') {
    score.value = 0
    if (currentGame.value === 'snake') initSnake()
    else if (currentGame.value === 'tetris') initTetris()
    else if (currentGame.value === 'game2048') init2048()
    else if (currentGame.value === 'flappy') initFlappy()
    else if (currentGame.value === 'breakout') initBreakout()
    else if (currentGame.value === 'shooter') initShooter()
    else if (currentGame.value === 'chineseChess') { initChineseChess(); chessStartTime = Date.now() }
    else if (currentGame.value === 'minesweeper') initMinesweeper()
    else if (currentGame.value === 'fruitninja') initFruitNinja()
  }
  gameState.value = 'playing'
  if (currentGame.value === 'shooter') SFX.startBGM()
  lastTime = 0
  // Lock screen for snake (swipe control) and shooter (drag control)
  canvasTouchLocked.value = true
  if (!animationId) animationId = requestAnimationFrame(gameLoop)
}

function pauseGame() {
  if (gameState.value === 'playing') {
    gameState.value = 'paused'
    canvasTouchLocked.value = false
    SFX.stopBGM()
  }
}

function resetGame() {
  gameState.value = 'idle'
  canvasTouchLocked.value = false
  SFX.stopBGM()
  score.value = 0
  level.value = 1
  linesCleared.value = 0
  bricksLeft.value = 0
  softDrop = false
  breakoutLeft.value = false
  breakoutRight.value = false
  breakoutLives = 3
  breakoutLevel = 1
  breakoutCombo = 0
  breakoutParticles = []
  breakoutPowerups = []
  breakoutLasers = []
  breakoutPaddleWide = false
  breakoutSlow = false
  breakoutFireball = false
  breakoutLaserActive = false
  shooterLeft = false
  shooterRight = false
  const ctx = canvasRef.value?.getContext('2d')
  if (ctx) {
    ctx.save()
    ctx.setTransform(1, 0, 0, 1, 0, 0)
    ctx.fillStyle = '#f8fafc'
    ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height)
    ctx.restore()
  }
}

function switchGame(game) {
  if (currentGame.value === game) return
  currentGame.value = game
  canvasHeight.value = game === 'tetris' ? TETRIS_ROWS * TETRIS_CELL : game === 'chineseChess' ? 480 : 400
  gridCanvasDirty = true
  gridCanvas = null
  flappyGrad = null
  shooterGrad = null
  resetGame()
  loadHighScore()
  // Auto-scroll tab bar to show the active game
  nextTick(() => {
    const wrap = tabBarWrapRef.value
    if (!wrap) return
    const activeBtn = wrap.querySelector('.tab-btn.active')
    if (activeBtn) {
      const btnLeft = activeBtn.offsetLeft
      const btnRight = btnLeft + activeBtn.offsetWidth
      const scrollLeft = wrap.scrollLeft
      const visibleWidth = wrap.clientWidth
      if (btnLeft < scrollLeft) wrap.scrollTo({ left: btnLeft - 10, behavior: 'smooth' })
      else if (btnRight > scrollLeft + visibleWidth) wrap.scrollTo({ left: btnRight - visibleWidth + 10, behavior: 'smooth' })
    }
    setTimeout(() => onTabScroll(), 300)
  })
}

function saveHighScore() {
  const key = `game_high_${currentGame.value}`
  const saved = localStorage.getItem(key)
  const best = saved ? parseInt(saved) : 0
  if (score.value > best) {
    localStorage.setItem(key, score.value.toString())
    highScore.value = score.value
  }
  // Update achievement stats
  if (currentGame.value === 'shooter') {
    achievementStats.value.highScore = Math.max(achievementStats.value.highScore, score.value)
    achievementStats.value.maxCombo = Math.max(achievementStats.value.maxCombo, shooterMaxCombo.value)
    checkAchievements(achievementStats.value)
    saveAchievementStats()
  }
}

// Shop skills data (used by game logic)
const SHOP_SKILLS = {
  shield_skill: { icon: '🛡️', cooldown: 45000 },
  bomb_skill: { icon: '💥', cooldown: 60000 },
  slow_skill: { icon: '⏳', cooldown: 40000 },
  magnet_skill: { icon: '🧲', cooldown: 35000 },
  rage_skill: { icon: '🔥', cooldown: 50000 },
  heal_skill: { icon: '💚', cooldown: 50000 },
}

// Shop functions — all items unlocked, persisted to localStorage
function doEquip(slot, itemId) {
  if (slot === 'player_skin') shooterCosmetics.value.equippedPlayerSkin = itemId
  else if (slot === 'enemy_skin') shooterCosmetics.value.equippedEnemySkin = itemId
  else if (slot === 'bullet_style') shooterCosmetics.value.equippedBulletStyle = itemId
  else if (slot === 'powerup_skin') shooterCosmetics.value.equippedPowerupSkin = itemId
  else if (slot === 'skill_1') shooterCosmetics.value.equippedSkill1 = itemId
  else if (slot === 'skill_2') shooterCosmetics.value.equippedSkill2 = itemId
  else if (slot === 'skill_3') shooterCosmetics.value.equippedSkill3 = itemId
  shooterEquippedSkills.value = [
    shooterCosmetics.value.equippedSkill1 || null,
    shooterCosmetics.value.equippedSkill2 || null,
    shooterCosmetics.value.equippedSkill3 || null
  ]
  saveCosmetics()
  ElMessage.success(itemId ? t('games.equipSuccess') : t('games.unequipSuccess'))
}

function toggleEquipSkill(skillId) {
  const eq1 = shooterCosmetics.value.equippedSkill1
  const eq2 = shooterCosmetics.value.equippedSkill2
  const eq3 = shooterCosmetics.value.equippedSkill3
  if (eq1 === skillId) { doEquip('skill_1', ''); return }
  if (eq2 === skillId) { doEquip('skill_2', ''); return }
  if (eq3 === skillId) { doEquip('skill_3', ''); return }
  if (!eq1) { doEquip('skill_1', skillId) }
  else if (!eq2) { doEquip('skill_2', skillId) }
  else if (!eq3) { doEquip('skill_3', skillId) }
  else { doEquip('skill_1', skillId) }
}

function clearSkillSlot(slotIndex) {
  const slot = slotIndex === 0 ? 'skill_1' : slotIndex === 1 ? 'skill_2' : 'skill_3'
  if (shooterCosmetics.value[slot]) doEquip(slot, '')
}

function loadHighScore() {
  const key = `game_high_${currentGame.value}`
  const saved = localStorage.getItem(key)
  highScore.value = saved ? parseInt(saved) : 0
}

// Keyboard controls
function handleKeydown(e) {
  if (currentGame.value === 'snake') {
    if (gameState.value !== 'playing') return
    const keyMap = {
      ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right',
      w: 'up', s: 'down', a: 'left', d: 'right',
      W: 'up', S: 'down', A: 'left', D: 'right'
    }
    const dir = keyMap[e.key]
    if (dir) { e.preventDefault(); setDirection(dir) }
  } else if (currentGame.value === 'tetris') {
    if (gameState.value !== 'playing') return
    if (e.key === 'ArrowLeft' || e.key === 'a') { e.preventDefault(); tetrisAction('left') }
    else if (e.key === 'ArrowRight' || e.key === 'd') { e.preventDefault(); tetrisAction('right') }
    else if (e.key === 'ArrowDown' || e.key === 's') { e.preventDefault(); tetrisAction('down') }
    else if (e.key === 'ArrowUp' || e.key === 'w') { e.preventDefault(); tetrisAction('rotate') }
    else if (e.key === ' ') { e.preventDefault(); tetrisAction('drop') }
  } else if (currentGame.value === 'game2048') {
    if (gameState.value !== 'playing') return
    if (e.key === 'ArrowLeft' || e.key === 'a') { e.preventDefault(); move2048('left') }
    else if (e.key === 'ArrowRight' || e.key === 'd') { e.preventDefault(); move2048('right') }
    else if (e.key === 'ArrowUp' || e.key === 'w') { e.preventDefault(); move2048('up') }
    else if (e.key === 'ArrowDown' || e.key === 's') { e.preventDefault(); move2048('down') }
  } else if (currentGame.value === 'flappy') {
    if (gameState.value !== 'playing') return
    if (e.key === ' ' || e.key === 'ArrowUp' || e.key === 'w') { e.preventDefault(); flap() }
  } else if (currentGame.value === 'breakout') {
    if (e.key === 'ArrowLeft' || e.key === 'a') breakoutLeft.value = true
    else if (e.key === 'ArrowRight' || e.key === 'd') breakoutRight.value = true
    else if (e.key === ' ') { e.preventDefault(); launchBall() }
  } else if (currentGame.value === 'shooter') {
    if (gameState.value !== 'playing') return
    if (e.key === 'ArrowLeft' || e.key === 'a') shooterLeft = true
    else if (e.key === 'ArrowRight' || e.key === 'd') shooterRight = true
    else if (e.key === ' ') { e.preventDefault(); shooterFire() }
    else if (e.key === 'q' || e.key === 'Q') activateShooterSkill(0)
    else if (e.key === 'e' || e.key === 'E') activateShooterSkill(1)
    else if (e.key === 'r' || e.key === 'R') activateShooterSkill(2)
  } else if (currentGame.value === 'minesweeper') {
    if (gameState.value !== 'playing') return
    if (e.key === 'ArrowUp') { e.preventDefault(); msCursorR = Math.max(0, msCursorR - 1) }
    else if (e.key === 'ArrowDown') { e.preventDefault(); msCursorR = Math.min(msRows - 1, msCursorR + 1) }
    else if (e.key === 'ArrowLeft') { e.preventDefault(); msCursorC = Math.max(0, msCursorC - 1) }
    else if (e.key === 'ArrowRight') { e.preventDefault(); msCursorC = Math.min(msCols - 1, msCursorC + 1) }
    else if (e.key === ' ') {
      e.preventDefault()
      if (msFirstClick) { msPlaceMines(msCursorR, msCursorC); msStartTime = Date.now(); msFirstClick = false }
      msReveal(msCursorR, msCursorC)
    }
    else if (e.key === 'f' || e.key === 'F') msToggleFlag(msCursorR, msCursorC)
  }
}

function handleKeyup(e) {
  if (currentGame.value === 'tetris') {
    if (e.key === 'ArrowDown' || e.key === 's') tetrisActionRelease('down')
  } else if (currentGame.value === 'breakout') {
    if (e.key === 'ArrowLeft' || e.key === 'a') breakoutLeft.value = false
    else if (e.key === 'ArrowRight' || e.key === 'd') breakoutRight.value = false
  } else if (currentGame.value === 'shooter') {
    if (e.key === 'ArrowLeft' || e.key === 'a') shooterLeft = false
    else if (e.key === 'ArrowRight' || e.key === 'd') shooterRight = false
  }
}

// Touch swipe for snake and 2048
let touchStartX = 0
let touchStartY = 0

function handleTouchStart(e) {
  const touch = e.touches[0]
  touchStartX = touch.clientX
  touchStartY = touch.clientY
  // Prevent page scroll only when touching the canvas area (not D-pad buttons etc.)
  if (canvasTouchLocked.value && gameState.value === 'playing') {
    const target = e.target
    if (target.closest && target.closest('.game-canvas-wrap')) {
      e.preventDefault()
    }
  }
}

function handleTouchEnd(e) {
  const touch = e.changedTouches[0]
  const dx = touch.clientX - touchStartX
  const dy = touch.clientY - touchStartY
  const absDx = Math.abs(dx)
  const absDy = Math.abs(dy)
  if (Math.max(absDx, absDy) < 30) return

  if (currentGame.value === 'snake' && gameState.value === 'playing' && !canvasTouchActive) {
    if (absDx > absDy) setDirection(dx > 0 ? 'right' : 'left')
    else setDirection(dy > 0 ? 'down' : 'up')
  } else if (currentGame.value === 'game2048' && gameState.value === 'playing') {
    if (absDx > absDy) move2048(dx > 0 ? 'right' : 'left')
    else move2048(dy > 0 ? 'down' : 'up')
  }
}

function autoPause() {
  if (gameState.value === 'playing') {
    gameState.value = 'paused'
    saveGameState()
  }
}

function saveGameState() {
  const state = {
    game: currentGame.value,
    state: gameState.value,
    score: score.value,
    highScore: highScore.value
  }
  if (currentGame.value === 'snake') {
    state.data = { snake, food, direction, nextDirection }
  } else if (currentGame.value === 'tetris') {
    state.data = { board, currentPiece, nextPiece, level: level.value, linesCleared: linesCleared.value }
  } else if (currentGame.value === 'game2048') {
    state.data = { grid: grid2048 }
  } else if (currentGame.value === 'flappy') {
    state.data = { bird, pipes, pipeSpawnTimer, flappyStarted }
  } else if (currentGame.value === 'breakout') {
    state.data = {
      paddle, balls: breakoutBalls, bricks, ballLaunched, bricksLeft: bricksLeft.value,
      lives: breakoutLives, level: breakoutLevel, combo: breakoutCombo, comboTimer: breakoutComboTimer,
      powerups: breakoutPowerups, lasers: breakoutLasers, particles: breakoutParticles,
      paddleWide: breakoutPaddleWide, paddleWideTimer: breakoutPaddleWideTimer,
      slow: breakoutSlow, slowTimer: breakoutSlowTimer,
      fireball: breakoutFireball, fireballTimer: breakoutFireballTimer,
      laserActive: breakoutLaserActive, laserPowerTimer: breakoutLaserPowerTimer
    }
  } else if (currentGame.value === 'shooter') {
    state.data = {
      player: shooterPlayer, bullets: shooterBullets, enemies: shooterEnemies, enemyBullets: shooterEnemyBullets,
      level: shooterLevel, levelTimer: shooterLevelTimer, timer: shooterTimer, fireTimer: shooterFireTimer,
      particles: shooterParticles, powerups: shooterPowerups, scorePopups: shooterScorePopups, boss: shooterBoss,
      lives: shooterLives, maxLives: shooterMaxLives, maxCombo: shooterMaxCombo.value, invincible: shooterInvincible, invincibleTimer: shooterInvincibleTimer,
      powerupType: shooterPowerupType, powerupTimer: shooterPowerupTimer,
      combo: shooterCombo, comboTimer: shooterComboTimer, flashTimer: shooterFlashTimer,
      stars: shooterStars,
      shield: shooterShield, laser: shooterLaser, laserTimer: shooterLaserTimer,
      homing: shooterHoming, homingTimer: shooterHomingTimer,
      activeSkillCooldownTimer: shooterActiveSkillCooldownTimer,
      activeSkillActive: shooterActiveSkillActive,
      activeSkillDuration: shooterActiveSkillDuration,
      doubleScore: shooterDoubleScore, doubleScoreTimer: shooterDoubleScoreTimer,
      slowEnemy: shooterSlowEnemy, slowEnemyTimer: shooterSlowEnemyTimer,
      magnet: shooterMagnet, magnetTimer: shooterMagnetTimer,
      skillCooldownTimers: [...shooterSkillCooldownTimers]
    }
  } else if (currentGame.value === 'chineseChess') {
    state.data = {
      board: chessBoard, turn: chessTurn, difficulty: chessDifficulty.value,
      selected: chessSelected, history: chessHistory, lastMove: chessLastMove,
      legalMoves: chessLegalMoves, gameResult: chessGameResult, startTime: chessStartTime
    }
  } else if (currentGame.value === 'minesweeper') {
    state.data = {
      board: msBoard, rows: msRows, cols: msCols, mineCount: msMineCount,
      difficulty: msDifficulty.value, revealed: msRevealed, flagged: msFlagged,
      gameOver: msGameOver, startTime: msStartTime, elapsed: msElapsed,
      firstClick: msFirstClick, cursorR: msCursorR, cursorC: msCursorC
    }
  } else if (currentGame.value === 'fruitninja') {
    state.data = {
      fruits: fnFruits, bombs: fnBombs, lives: fnLives, combo: fnCombo,
      comboTimer: fnComboTimer, spawnTimer: fnSpawnTimer, spawnInterval: fnSpawnInterval,
      particles: fnParticles
    }
  }
  localStorage.setItem('game_saved_state', JSON.stringify(state))
}

function restoreGameState() {
  const raw = localStorage.getItem('game_saved_state')
  if (!raw) return false
  try {
    const saved = JSON.parse(raw)
    if (!saved.game || saved.state !== 'paused') {
      localStorage.removeItem('game_saved_state')
      return false
    }
    currentGame.value = saved.game
    score.value = saved.score || 0
    highScore.value = saved.highScore || 0
    canvasHeight.value = saved.game === 'tetris' ? TETRIS_ROWS * TETRIS_CELL : saved.game === 'chineseChess' ? 480 : 400

    if (saved.game === 'snake' && saved.data) {
      snake = saved.data.snake || []
      food = saved.data.food
      direction = saved.data.direction || 'right'
      nextDirection = saved.data.nextDirection || 'right'
    } else if (saved.game === 'tetris' && saved.data) {
      board = saved.data.board || []
      currentPiece = saved.data.currentPiece
      nextPiece = saved.data.nextPiece
      level.value = saved.data.level || 1
      linesCleared.value = saved.data.linesCleared || 0
      tetrisSpeed = Math.max(80, 500 - (level.value - 1) * 40)
    } else if (saved.game === 'game2048' && saved.data) {
      grid2048 = saved.data.grid || []
    } else if (saved.game === 'flappy' && saved.data) {
      bird = saved.data.bird || { x: 80, y: 200, vy: 0, rotation: 0 }
      pipes = saved.data.pipes || []
      pipeSpawnTimer = saved.data.pipeSpawnTimer || 0
      flappyStarted = saved.data.flappyStarted || false
    } else if (saved.game === 'breakout' && saved.data) {
      paddle = saved.data.paddle || { x: (canvasWidth - 70) / 2, w: 70 }
      breakoutBalls = saved.data.balls || [{ x: canvasWidth / 2, y: canvasHeight.value - 40, vx: 0, vy: 0, trail: [] }]
      bricks = saved.data.bricks || []
      ballLaunched = saved.data.ballLaunched || false
      bricksLeft.value = saved.data.bricksLeft || 0
      breakoutLives = saved.data.lives ?? 3
      breakoutLevel = saved.data.level || 1
      breakoutCombo = saved.data.combo || 0
      breakoutComboTimer = saved.data.comboTimer || 0
      breakoutPowerups = saved.data.powerups || []
      breakoutLasers = saved.data.lasers || []
      breakoutParticles = saved.data.particles || []
      breakoutPaddleWide = saved.data.paddleWide || false
      breakoutPaddleWideTimer = saved.data.paddleWideTimer || 0
      breakoutSlow = saved.data.slow || false
      breakoutSlowTimer = saved.data.slowTimer || 0
      breakoutFireball = saved.data.fireball || false
      breakoutFireballTimer = saved.data.fireballTimer || 0
      breakoutLaserActive = saved.data.laserActive || false
      breakoutLaserPowerTimer = saved.data.laserPowerTimer || 0
    } else if (saved.game === 'shooter' && saved.data) {
      shooterPlayer = saved.data.player || { x: canvasWidth / 2 - SHOOTER_PLAYER_W / 2, y: canvasHeight.value - SHOOTER_PLAYER_H - 20 }
      shooterBullets = saved.data.bullets || []
      shooterEnemies = saved.data.enemies || []
      shooterEnemyBullets = saved.data.enemyBullets || []
      shooterLevel = saved.data.level || 1
      shooterLevelTimer = saved.data.levelTimer || 0
      shooterTimer = saved.data.timer || 0
      shooterFireTimer = saved.data.fireTimer || 0
      shooterParticles = saved.data.particles || []
      shooterPowerups = saved.data.powerups || []
      shooterScorePopups = saved.data.scorePopups || []
      shooterBoss = saved.data.boss || null
      shooterLives = saved.data.lives ?? 3
      shooterMaxLives = saved.data.maxLives ?? 3
      shooterMaxCombo.value = saved.data.maxCombo ?? 0
      shooterInvincible = saved.data.invincible || false
      shooterInvincibleTimer = saved.data.invincibleTimer || 0
      shooterPowerupType = saved.data.powerupType || null
      shooterPowerupTimer = saved.data.powerupTimer || 0
      shooterCombo = saved.data.combo || 0
      shooterComboTimer = saved.data.comboTimer || 0
      shooterFlashTimer = saved.data.flashTimer || 0
      shooterShield = saved.data.shield || false
      shooterLaser = saved.data.laser || false
      shooterLaserTimer = saved.data.laserTimer || 0
      shooterHoming = saved.data.homing || false
      shooterHomingTimer = saved.data.homingTimer || 0
      shooterActiveSkillCooldownTimer = saved.data.activeSkillCooldownTimer || 0
      shooterActiveSkillCooldown.value = shooterActiveSkillCooldownTimer
      shooterDoubleScore = saved.data.doubleScore || false
      shooterDoubleScoreTimer = saved.data.doubleScoreTimer || 0
      shooterSlowEnemy = saved.data.slowEnemy || false
      shooterSlowEnemyTimer = saved.data.slowEnemyTimer || 0
      shooterMagnet = saved.data.magnet || false
      shooterMagnetTimer = saved.data.magnetTimer || 0
      shooterSkillCooldownTimers = saved.data.skillCooldownTimers || [0, 0, 0]
      shooterSkillCooldowns.value = [...shooterSkillCooldownTimers]
      shooterActiveSkillActive = saved.data.activeSkillActive || false
      shooterActiveSkillDuration = saved.data.activeSkillDuration || 0
      shooterScreenShake = 0
      shooterLeft = false
      shooterRight = false
      // Restore 3-layer stars
      if (saved.data.stars && saved.data.stars.length === 3) {
        shooterStars = saved.data.stars
      } else if (shooterStars[0].length === 0) {
        for (let layer = 0; layer < 3; layer++) {
          const count = [30, 20, 10][layer]
          for (let i = 0; i < count; i++) {
            shooterStars[layer].push({
              x: Math.random() * canvasWidth,
              y: Math.random() * canvasHeight.value,
              speed: [20, 50, 100][layer] + Math.random() * 20,
              size: [0.8, 1.2, 2][layer] + Math.random() * 0.5
            })
          }
        }
      }
    } else if (saved.game === 'chineseChess' && saved.data) {
      chessBoard = saved.data.board || []
      chessTurn = saved.data.turn || 'red'
      chessDifficulty.value = saved.data.difficulty || 2
      chessSelected = saved.data.selected || null
      chessHistory = saved.data.history || []
      chessLastMove = saved.data.lastMove || null
      chessLegalMoves = saved.data.legalMoves || []
      chessGameResult = saved.data.gameResult || null
      chessStartTime = saved.data.startTime || Date.now()
      chessThinking = false
    } else if (saved.game === 'minesweeper' && saved.data) {
      msBoard = saved.data.board || []; msRows = saved.data.rows || 9; msCols = saved.data.cols || 9
      msMineCount = saved.data.mineCount || 10; msDifficulty.value = saved.data.difficulty || 'easy'
      msRevealed = saved.data.revealed || 0; msFlagged = saved.data.flagged || 0
      msGameOver = saved.data.gameOver || false; msStartTime = saved.data.startTime || 0
      msElapsed = saved.data.elapsed || 0; msFirstClick = saved.data.firstClick !== undefined ? saved.data.firstClick : true
      msCursorR = saved.data.cursorR || 0; msCursorC = saved.data.cursorC || 0
    } else if (saved.game === 'fruitninja' && saved.data) {
      fnFruits = saved.data.fruits || []; fnBombs = saved.data.bombs || []
      fnLives = saved.data.lives ?? 3; fnCombo = saved.data.combo || 0
      fnComboTimer = saved.data.comboTimer || 0; fnSpawnTimer = saved.data.spawnTimer || 0
      fnSpawnInterval = saved.data.spawnInterval || 1500; fnParticles = saved.data.particles || []
      fnSlicing = false; fnMouseDown = false; fnSliceTrail = []
    }

    gameState.value = 'paused'
    localStorage.removeItem('game_saved_state')
    return true
  } catch (e) {
    localStorage.removeItem('game_saved_state')
    return false
  }
}

function handleVisibilityChange() {
  if (document.hidden) autoPause()
}

function handleBeforeUnload() {
  // No-op: scores are stored locally only
}

function onCanvasMouseDown(e) {
  if (currentGame.value !== 'fruitninja' || gameState.value !== 'playing') return
  const canvas = canvasRef.value; if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  fnMouseDown = true; fnSlicing = true
  fnSliceX = (e.clientX - rect.left) * (400 / rect.width)
  fnSliceY = (e.clientY - rect.top) * (400 / rect.height)
  fnLastSliceX = fnSliceX; fnLastSliceY = fnSliceY
  fnSliceTrail = [{ x: fnSliceX, y: fnSliceY, time: Date.now() }]
}
function onCanvasMouseMove(e) {
  if (currentGame.value !== 'fruitninja' || !fnSlicing || gameState.value !== 'playing') return
  const canvas = canvasRef.value; if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const nx = (e.clientX - rect.left) * (400 / rect.width)
  const ny = (e.clientY - rect.top) * (400 / rect.height)
  fnCheckSlice(fnLastSliceX, fnLastSliceY, nx, ny)
  fnSliceTrail.push({ x: nx, y: ny, time: Date.now() })
  fnLastSliceX = nx; fnLastSliceY = ny
}
function onCanvasMouseEnter(e) {
  if (currentGame.value !== 'fruitninja' || gameState.value !== 'playing') return
  if (fnMouseDown && !fnSlicing) {
    fnSlicing = true
    const canvas = canvasRef.value; if (!canvas) return
    const rect = canvas.getBoundingClientRect()
    fnLastSliceX = (e.clientX - rect.left) * (400 / rect.width)
    fnLastSliceY = (e.clientY - rect.top) * (400 / rect.height)
  }
}
function onCanvasMouseLeave() {
  fnSlicing = false
}
function onDocumentMouseUp() {
  fnMouseDown = false; fnSlicing = false
}

onMounted(() => {
  applyCanvasDPR()
  loadCosmetics()
  loadAchievementStats()
  const restored = restoreGameState()
  if (restored) {
    loadHighScore()
  } else {
    loadHighScore()
    if (cachedCtx) {
      cachedCtx.fillStyle = '#f8fafc'
      cachedCtx.fillRect(0, 0, canvasWidth, canvasHeight.value)
    }
  }
  // Start loop for flappy idle bob or restored playing state
  if (currentGame.value === 'flappy' || gameState.value === 'playing' || gameState.value === 'paused') {
    animationId = requestAnimationFrame(gameLoop)
  }
  window.addEventListener('keydown', handleKeydown)
  window.addEventListener('keyup', handleKeyup)
  window.addEventListener('touchstart', handleTouchStart, { passive: false })
  window.addEventListener('touchend', handleTouchEnd, { passive: true })
  window.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('beforeunload', handleBeforeUnload)
  // Fruit Ninja mouse events
  const cv = canvasRef.value
  if (cv) {
    cv.addEventListener('mousedown', onCanvasMouseDown)
    cv.addEventListener('mousemove', onCanvasMouseMove)
    cv.addEventListener('mouseenter', onCanvasMouseEnter)
    cv.addEventListener('mouseleave', onCanvasMouseLeave)
  }
  document.addEventListener('mouseup', onDocumentMouseUp)
  // Initialize tab scroll state (delayed to ensure layout is complete)
  nextTick(() => {
    onTabScroll()
    setTimeout(() => onTabScroll(), 200)
  })
  window.addEventListener('resize', onTabScroll)
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  clearTimeout(leaderboardTimer)
  cachedCtx = null
  gridCanvas = null
  flappyGrad = null
  shooterGrad = null
  const cv = canvasRef.value
  if (cv) {
    cv.removeEventListener('mousedown', onCanvasMouseDown)
    cv.removeEventListener('mousemove', onCanvasMouseMove)
    cv.removeEventListener('mouseenter', onCanvasMouseEnter)
    cv.removeEventListener('mouseleave', onCanvasMouseLeave)
  }
  document.removeEventListener('mouseup', onDocumentMouseUp)
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('keyup', handleKeyup)
  window.removeEventListener('touchstart', handleTouchStart)
  window.removeEventListener('touchend', handleTouchEnd)
  window.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('beforeunload', handleBeforeUnload)
  window.removeEventListener('resize', onTabScroll)
  SFX.stopBGM()
  SFX.ctx?.close()
})

// Reapply DPR when canvas height changes (e.g. Tetris switch)
watch(canvasHeight, () => { nextTick(applyCanvasDPR) })

// Hide floating player when shop is open
watch(shooterShopOpen, (open) => {
  document.body.classList.toggle('shop-open', open)
})

onBeforeRouteLeave(() => {
  autoPause()
})
</script>

<style scoped>
.games-page {
  max-width: 600px;
  margin: 0 auto;
}

.game-card {
  border-radius: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.header-sub {
  font-size: 13px;
  color: #94a3b8;
}

.tab-bar-outer {
  display: flex;
  align-items: center;
  gap: 4px;
  position: relative;
  min-width: 0;
  flex: 1;
}

.tab-bar-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  flex: 1;
  min-width: 0;
  touch-action: pan-x;
}

.tab-bar-wrap::-webkit-scrollbar {
  display: none;
}

.tab-arrow {
  flex-shrink: 0;
  width: 24px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: #e2e8f0;
  color: #475569;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  padding: 0;
  line-height: 1;
}

.tab-arrow:hover {
  background: #cbd5e1;
}

.tab-bar {
  display: flex;
  gap: 4px;
  background: #f1f5f9;
  border-radius: 10px;
  padding: 3px;
  white-space: nowrap;
}

.tab-btn {
  padding: 8px 14px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  background: transparent;
  color: #64748b;
  transition: all 0.2s;
  white-space: nowrap;
}

.tab-btn.active {
  background: #fff;
  color: #1e5eb6;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.tab-btn:hover:not(.active) {
  color: #1e5eb6;
}

.game-area {
  display: flex;
  gap: 16px;
  justify-content: center;
  align-items: flex-start;
}

.game-canvas-wrap {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  border: 2px solid #e2e8f0;
  flex-shrink: 0;
}

.game-canvas-wrap.touch-locked {
  touch-action: none;
  -webkit-touch-callout: none;
}

.game-canvas {
  display: block;
}

.game-overlay {
  position: absolute;
  inset: 0;
  background: rgba(248,250,252,0.85);
  display: flex;
  align-items: center;
  justify-content: center;
}

.game-over-overlay {
  background: rgba(255,255,255,0.9);
}

.revival-overlay {
  background: rgba(0,0,0,0.8);
  z-index: 20;
}
.revival-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 28px 36px;
  background: #1e1e2e;
  border-radius: 16px;
  border: 2px solid #fbbf24;
  box-shadow: 0 0 30px rgba(251,191,36,0.3);
}
.revival-icon {
  font-size: 48px;
}
.revival-title {
  font-size: 20px;
  font-weight: 700;
  color: #fbbf24;
}
.revival-desc {
  font-size: 14px;
  color: #94a3b8;
  text-align: center;
  line-height: 1.5;
}
.revival-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}
.revival-btn {
  padding: 10px 24px;
  border-radius: 8px;
  border: none;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.revival-btn-accept {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #1e1e2e;
}
.revival-btn-accept:hover {
  transform: scale(1.05);
  box-shadow: 0 0 12px rgba(251,191,36,0.5);
}
.revival-btn-decline {
  background: #334155;
  color: #94a3b8;
}
.revival-btn-decline:hover {
  background: #475569;
  color: #e2e8f0;
}

/* Pause menu */
.pause-overlay {
  background: rgba(0,0,0,0.75);
  z-index: 15;
}
.pause-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px 40px;
  background: #1e1e2e;
  border-radius: 16px;
  border: 2px solid #60a5fa;
  box-shadow: 0 0 30px rgba(96,165,250,0.3);
}
.pause-icon {
  font-size: 48px;
}
.pause-title {
  font-size: 22px;
  font-weight: 700;
  color: #e2e8f0;
}
.pause-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}
.pause-btn {
  padding: 12px 28px;
  border-radius: 8px;
  border: none;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}
.pause-btn-resume {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #fff;
}
.pause-btn-resume:hover {
  transform: scale(1.05);
  box-shadow: 0 0 12px rgba(34,197,94,0.5);
}
.pause-btn-restart {
  background: #334155;
  color: #e2e8f0;
}
.pause-btn-restart:hover {
  background: #475569;
}
.pause-btn-quit {
  background: #334155;
  color: #94a3b8;
}
.pause-btn-quit:hover {
  background: #475569;
  color: #e2e8f0;
}

/* Achievement system */
.achievement-btn {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(30,30,46,0.85);
  border: 1px solid #fbbf24;
  color: #fbbf24;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  z-index: 5;
  transition: all 0.2s;
}
.achievement-btn:hover {
  background: rgba(251,191,36,0.2);
  transform: scale(1.05);
}
.achievement-toast {
  position: absolute;
  top: 50px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #1e1e2e, #2d2b55);
  border: 2px solid #fbbf24;
  border-radius: 12px;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 25;
  box-shadow: 0 0 20px rgba(251,191,36,0.4);
}
.achievement-toast-icon {
  font-size: 28px;
}
.achievement-toast-text {
  display: flex;
  flex-direction: column;
}
.achievement-toast-title {
  font-size: 11px;
  color: #fbbf24;
  text-transform: uppercase;
  letter-spacing: 1px;
}
.achievement-toast-name {
  font-size: 15px;
  font-weight: 700;
  color: #e2e8f0;
}
.achievement-fade-enter-active { transition: all 0.4s ease; }
.achievement-fade-leave-active { transition: all 0.6s ease; }
.achievement-fade-enter-from { opacity: 0; transform: translateX(-50%) translateY(-20px); }
.achievement-fade-leave-to { opacity: 0; transform: translateX(-50%) translateY(-20px); }
.achievement-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
}
.achievement-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #1e293b;
  border-radius: 8px;
  border: 1px solid #334155;
}
.achievement-locked {
  opacity: 0.4;
  filter: grayscale(0.8);
}
.achievement-icon {
  font-size: 20px;
}
.achievement-name {
  flex: 1;
  font-size: 13px;
  color: #e2e8f0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.achievement-check {
  color: #22c55e;
  font-weight: 700;
}
.achievement-footer {
  margin-top: 12px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
}

.game-won-overlay {
  background: rgba(255,255,255,0.9);
}

.overlay-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.overlay-icon {
  font-size: 48px;
}

.overlay-text {
  font-size: 16px;
  color: #64748b;
  font-weight: 500;
}

.overlay-title {
  font-size: 22px;
  font-weight: 700;
  color: #ef4444;
}

.game-won-overlay .overlay-title {
  color: #22c55e;
}

.overlay-score {
  font-size: 16px;
  color: #64748b;
}

.overlay-combo {
  font-size: 14px;
  color: #a78bfa;
  display: flex;
  align-items: center;
  gap: 6px;
}
.combo-multiplier {
  font-weight: 700;
  color: #f59e0b;
  font-size: 16px;
}

.overlay-saved {
  font-size: 13px;
  color: #22c55e;
  margin-top: 4px;
}

.overlay-saved-warn {
  color: #f59e0b;
}

.next-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.preview-label {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 500;
}

.next-canvas {
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
}

.game-stats {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin: 20px 0;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}

.stat-value.best {
  color: #1e5eb6;
}

.game-controls {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
  touch-action: manipulation;
}

.mobile-controls {
  display: none;
  justify-content: center;
  margin: 16px 0;
}

.dpad {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.dpad-row {
  display: flex;
  gap: 4px;
}

.dpad-btn {
  width: 52px;
  height: 52px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  font-size: 20px;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  user-select: none;
  -webkit-user-select: none;
  touch-action: manipulation;
}

.dpad-btn:active {
  background: #e8f0fe;
  border-color: #1e5eb6;
  color: #1e5eb6;
  transform: scale(0.92);
}

.dpad-btn.center {
  background: #f1f5f9;
  border-color: #f1f5f9;
  cursor: default;
}

.tetris-pad {
  display: flex;
  gap: 8px;
}

.hard-drop {
  background: #fff0f0;
  border-color: #fecaca;
  color: #ef4444;
}

.game-tips {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  padding-top: 8px;
}

/* ===== Leaderboard ===== */
.leaderboard-section {
  margin-top: 16px;
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px 20px;
}

.leaderboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  gap: 12px;
  flex-wrap: wrap;
}

.leaderboard-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.leaderboard-tabs {
  display: flex;
  gap: 4px;
  background: #e2e8f0;
  border-radius: 8px;
  padding: 3px;
}

.lb-tab {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  background: transparent;
  color: #64748b;
  transition: all 0.2s;
  white-space: nowrap;
}

.lb-tab.active {
  background: #fff;
  color: #1e5eb6;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.leaderboard-content {
  min-height: 60px;
}

.lb-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.lb-table th {
  text-align: left;
  padding: 8px 12px;
  color: #94a3b8;
  font-weight: 500;
  font-size: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.lb-table td {
  padding: 8px 12px;
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
}

.lb-table tr.lb-me td {
  background: #eff6ff;
  color: #1e5eb6;
  font-weight: 600;
}

.lb-table tr:last-child td {
  border-bottom: none;
}

.lb-rank {
  width: 50px;
  text-align: center !important;
}

.lb-score {
  text-align: right !important;
  font-weight: 600;
}

.lb-score.highlight {
  color: #1e5eb6;
  font-size: 15px;
}

.lb-time {
  text-align: right !important;
  font-size: 12px;
  color: #94a3b8;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}

.rank-1 { background: linear-gradient(135deg, #fbbf24, #f59e0b); }
.rank-2 { background: linear-gradient(135deg, #94a3b8, #64748b); }
.rank-3 { background: linear-gradient(135deg, #f97316, #ea580c); }

.lb-empty {
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  padding: 24px 0;
}

.lb-loading {
  text-align: center;
  color: #94a3b8;
  padding: 24px 0;
}

.lb-login-hint {
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  padding: 16px 0;
}

/* ===== Mobile ===== */
@media (max-width: 768px) {
  .games-page { padding: 0 4px; }
  .card-header { flex-direction: column; align-items: flex-start; gap: 8px; }
  .tab-bar-outer { width: 100%; }
  .tab-bar-wrap { width: 100%; position: relative; }
  .tab-bar-wrap:not(.scrolled-end)::after {
    content: '';
    position: absolute;
    right: 0; top: 0; bottom: 0;
    width: 32px;
    background: linear-gradient(to right, transparent, var(--el-bg-color, #fff));
    pointer-events: none;
    border-radius: 0 10px 10px 0;
  }
  .tab-btn { padding: 6px 8px; font-size: 12px; }
  .tab-btn .tab-label { display: none; }
  .tab-arrow { width: 28px; height: 32px; font-size: 18px; }
  .game-canvas-wrap { width: 100%; border-radius: 8px; }
  .game-canvas { width: 100%; height: auto; }
  .mobile-controls { display: flex; }
  .game-stats { gap: 12px; padding: 12px 8px; flex-wrap: wrap; justify-content: space-around; }
  .stat-value { font-size: 18px; }
  .game-controls { gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
  .game-controls .el-button { flex: 1; min-width: 80px; }
  .game-tips { font-size: 12px; padding: 6px 4px; }
  .overlay-icon { font-size: 36px; }
  .overlay-title { font-size: 18px; }
  .overlay-score { font-size: 14px; }
  .leaderboard-section { padding: 12px 8px; margin-top: 12px; }
  .leaderboard-header { flex-direction: column; align-items: flex-start; gap: 8px; }
  .leaderboard-tabs { width: 100%; overflow-x: auto; }
  .lb-table { font-size: 12px; }
  .lb-table th, .lb-table td { padding: 6px 8px; }
  .dpad-btn { width: 56px; height: 56px; font-size: 22px; }
  .next-preview { display: none; }
  :deep(.el-dialog) { width: 95% !important; margin: 0 auto; }
}

/* ===== Dark Mode ===== */
/* Shop styles */
.shop-btn {
  margin-top: 12px;
  padding: 8px 20px;
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #fff;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}
.shop-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(245,158,11,0.4);
}

.chess-difficulty-panel {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.chess-diff-btn {
  padding: 6px 16px;
  border: 2px solid #6366f1;
  border-radius: 16px;
  background: transparent;
  color: #6366f1;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.chess-diff-btn.active,
.chess-diff-btn:hover {
  background: #6366f1;
  color: #fff;
}

.shop-content {
  padding: 0 4px;
}
.shop-balance {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border-radius: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.coin-icon { font-size: 24px; }
.coin-amount { font-size: 22px; font-weight: 700; color: #92400e; }
.coin-label { font-size: 13px; color: #b45309; }
.tier-legend {
  display: flex;
  gap: 6px;
  margin-left: auto;
}
.tier-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}
.tier-basic { background: #f3f4f6; color: #6b7280; }
.tier-rare { background: #dbeafe; color: #2563eb; }
.tier-epic { background: #ede9fe; color: #7c3aed; }
.tier-legendary { background: #fef3c7; color: #d97706; }

.shop-top-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}
.shop-close-btn {
  padding: 10px 32px;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}
.shop-close-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(239,68,68,0.4);
}
.shop-dialog .el-dialog__body {
  max-height: 65vh;
  overflow-y: auto;
}

.shop-tabs {
  margin-top: 8px;
}
.shop-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  padding: 8px 0;
}
.shop-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px 10px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  transition: border-color 0.2s, box-shadow 0.2s;
  position: relative;
}
.shop-item:hover {
  border-color: #93c5fd;
  box-shadow: 0 2px 8px rgba(59,130,246,0.15);
}
.shop-item.equipped {
  border-color: #3b82f6;
  background: #eff6ff;
}
/* Tier borders */
.tier-border-basic { border-color: #d1d5db; }
.tier-border-rare { border-color: #93c5fd; }
.tier-border-epic { border-color: #a78bfa; }
.tier-border-legendary { border-color: #fbbf24; }
.tier-border-legendary:hover { box-shadow: 0 0 16px rgba(251,191,36,0.3); }

.tier-badge {
  position: absolute;
  top: -8px;
  right: -4px;
  font-size: 9px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 700;
  z-index: 1;
}

.item-preview {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;
  background: #1e1e2e;
}

.item-name {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
.item-desc {
  font-size: 10px;
  color: #9ca3af;
  text-align: center;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.item-action {
  margin-top: 4px;
}
.btn-equipped, .btn-equip {
  padding: 4px 14px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-equipped {
  background: #dbeafe;
  color: #2563eb;
  cursor: default;
}
.btn-equip {
  background: #3b82f6;
  color: #fff;
}
.btn-equip:hover { background: #2563eb; }
.skill-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  background: linear-gradient(135deg, #1e1e2e, #2d2d44);
}
.skill-slots {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}
.skill-slot-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}
.skill-slot-row {
  display: flex;
  gap: 12px;
}
.skill-slot {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
  border: 2px dashed #d1d5db;
  border-radius: 10px;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  min-height: 44px;
}
.skill-slot:hover {
  border-color: #93c5fd;
  background: #f0f7ff;
}
.slot-empty {
  color: #9ca3af;
  font-size: 12px;
}

/* v2 */
</style>
