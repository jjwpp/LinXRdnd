<script setup>
import { computed } from "vue";

const props = defineProps({
  actionPoints: { type: Number, default: 1 },
  maxActionPoints: { type: Number, default: 1 },
  combatPhase: { type: String, default: "PLAYER_TURN" },
  loading: { type: Boolean, default: false },
  hasSpells: { type: Boolean, default: false },
});

const emit = defineEmits([
  "attack",
  "ranged-attack",
  "cast-spell",
  "use-item",
  "end-turn",
]);

const isPlayerTurn = computed(() => props.combatPhase === "PLAYER_TURN");
const canAct = computed(() => isPlayerTurn.value && !props.loading && props.actionPoints > 0);
</script>

<template>
  <div class="action-bar">
    <!-- Action Points display as glowing pips -->
    <div class="ap-display">
      <div class="ap-icon-wrap">
        <span class="ap-icon">⚡</span>
      </div>
      <div class="ap-dots">
        <span
          v-for="n in maxActionPoints"
          :key="n"
          class="ap-dot"
          :class="{ filled: n <= actionPoints }"
        ></span>
      </div>
      <span class="ap-text">{{ actionPoints }}/{{ maxActionPoints }}</span>
    </div>

    <!-- Action buttons as circular rune buttons -->
    <div class="action-buttons">
      <button
        class="rune-action-btn attack-btn"
        :disabled="!canAct"
        @click="emit('attack')"
        title="近战攻击"
      >
        <span class="btn-rune-ring"></span>
        <span class="btn-icon">⚔️</span>
        <span class="btn-label">攻击</span>
      </button>

      <button
        class="rune-action-btn ranged-btn"
        :disabled="!canAct"
        @click="emit('ranged-attack')"
        title="远程攻击"
      >
        <span class="btn-rune-ring"></span>
        <span class="btn-icon">🏹</span>
        <span class="btn-label">射击</span>
      </button>

      <button
        class="rune-action-btn spell-btn"
        :disabled="!canAct || !hasSpells"
        @click="emit('cast-spell')"
        title="施放法术"
      >
        <span class="btn-rune-ring"></span>
        <span class="btn-icon">✨</span>
        <span class="btn-label">法术</span>
      </button>

      <button
        class="rune-action-btn item-btn"
        :disabled="!isPlayerTurn || loading"
        @click="emit('use-item')"
        title="打开背包"
      >
        <span class="btn-rune-ring"></span>
        <span class="btn-icon">🎒</span>
        <span class="btn-label">物品</span>
      </button>

      <button
        class="rune-action-btn end-turn-btn"
        :disabled="!isPlayerTurn || loading"
        @click="emit('end-turn')"
        title="结束回合"
      >
        <span class="btn-rune-ring"></span>
        <span class="btn-icon">⏭</span>
        <span class="btn-label">结束</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.action-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 18px;
  background: linear-gradient(0deg, rgba(11, 9, 8, 0.97), rgba(20, 15, 12, 0.6));
  border-top: 1px solid var(--line-gold, rgba(201, 162, 39, 0.3));
  border-radius: var(--radius-md, 8px);
}

/* Action Points — glowing pips */
.ap-display {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: var(--radius-sm, 4px);
  background: linear-gradient(180deg, var(--bg-card, #251c15), var(--bg-stone, #17100c));
  border: 1px solid var(--gold-dim, #8a7020);
  flex-shrink: 0;
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.08), 0 2px 8px rgba(0, 0, 0, 0.3);
}
.ap-icon-wrap {
  display: flex;
  align-items: center;
}
.ap-icon {
  font-size: 16px;
  filter: drop-shadow(0 0 4px rgba(232, 196, 74, 0.4));
}
.ap-dots { display: flex; gap: 4px; }
.ap-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(201, 162, 39, 0.1);
  border: 1px solid rgba(201, 162, 39, 0.25);
  transition: all 0.3s ease;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.4);
}
.ap-dot.filled {
  background: radial-gradient(circle, #ffd700, #c9a227);
  border-color: #e8c44a;
  box-shadow: 0 0 10px rgba(232, 196, 74, 0.6), inset 0 1px 2px rgba(255, 255, 255, 0.3);
  animation: apPulse 2s ease-in-out infinite;
}
@keyframes apPulse {
  0%, 100% { box-shadow: 0 0 8px rgba(232, 196, 74, 0.5), inset 0 1px 2px rgba(255, 255, 255, 0.3); }
  50% { box-shadow: 0 0 14px rgba(232, 196, 74, 0.8), inset 0 1px 2px rgba(255, 255, 255, 0.3); }
}
.ap-text {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 13px;
  font-weight: 700;
  color: #e8c44a;
  letter-spacing: 0.05em;
}

/* Action button group */
.action-buttons {
  display: flex;
  gap: 8px;
  flex: 1;
  justify-content: center;
  flex-wrap: wrap;
}

/* Circular rune action buttons */
.rune-action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: 2px solid;
  cursor: pointer;
  transition: all 0.25s ease;
  background: radial-gradient(circle, var(--bg-card, #251c15), var(--bg-stone, #17100c));
  position: relative;
  overflow: visible;
  box-shadow:
    inset 0 2px 6px rgba(0, 0, 0, 0.6),
    0 2px 8px rgba(0, 0, 0, 0.4);
}
.rune-action-btn:hover:not(:disabled) {
  transform: translateY(-3px) scale(1.05);
  box-shadow:
    inset 0 2px 4px rgba(0, 0, 0, 0.4),
    0 6px 20px rgba(0, 0, 0, 0.5);
}
.rune-action-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.95);
}
.rune-action-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
  filter: grayscale(0.5);
}

/* Magical rune ring on hover */
.btn-rune-ring {
  position: absolute;
  inset: -5px;
  border: 1px dashed transparent;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}
.rune-action-btn:hover:not(:disabled) .btn-rune-ring {
  opacity: 1;
  animation: rotateRunes 8s linear infinite;
}
@keyframes rotateRunes {
  to { transform: rotate(360deg); }
}

.btn-icon {
  font-size: 22px;
  filter: drop-shadow(0 2px 3px rgba(0, 0, 0, 0.5));
  z-index: 1;
}
.btn-label {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.05em;
  z-index: 1;
}

/* Per-button color themes */
.attack-btn { border-color: rgba(155, 45, 45, 0.5); }
.attack-btn:hover:not(:disabled) {
  border-color: #9b2d2d;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.4), 0 0 20px rgba(155, 45, 45, 0.3);
}
.attack-btn .btn-rune-ring { border-color: rgba(155, 45, 45, 0.4); }
.attack-btn .btn-label { color: #d08080; }
.attack-btn:hover:not(:disabled) .btn-label { color: #ff8080; }

.ranged-btn { border-color: rgba(74, 122, 58, 0.5); }
.ranged-btn:hover:not(:disabled) {
  border-color: #4a7a3a;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.4), 0 0 20px rgba(74, 122, 58, 0.3);
}
.ranged-btn .btn-rune-ring { border-color: rgba(74, 122, 58, 0.4); }
.ranged-btn .btn-label { color: #80b070; }
.ranged-btn:hover:not(:disabled) .btn-label { color: #8acd4a; }

.spell-btn { border-color: rgba(125, 90, 170, 0.5); }
.spell-btn:hover:not(:disabled) {
  border-color: #7d5aaa;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.4), 0 0 20px rgba(125, 90, 170, 0.3);
}
.spell-btn .btn-rune-ring { border-color: rgba(125, 90, 170, 0.4); }
.spell-btn .btn-label { color: #b0a0d0; }
.spell-btn:hover:not(:disabled) .btn-label { color: #c8a8ff; }

.item-btn { border-color: rgba(201, 162, 39, 0.5); }
.item-btn:hover:not(:disabled) {
  border-color: #c9a227;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.4), 0 0 20px rgba(201, 162, 39, 0.3);
}
.item-btn .btn-rune-ring { border-color: rgba(201, 162, 39, 0.4); }
.item-btn .btn-label { color: #c9a227; }
.item-btn:hover:not(:disabled) .btn-label { color: #e8c44a; }

/* End Turn — prominent iron/gold button */
.end-turn-btn {
  border-color: rgba(201, 162, 39, 0.6) !important;
  background: radial-gradient(circle, #2a2017, #1a140e) !important;
  box-shadow:
    inset 0 2px 6px rgba(0, 0, 0, 0.5),
    inset 0 -1px 0 rgba(201, 162, 39, 0.15),
    0 2px 10px rgba(0, 0, 0, 0.4);
}
.end-turn-btn:hover:not(:disabled) {
  border-color: #e8c44a !important;
  box-shadow:
    inset 0 2px 4px rgba(0, 0, 0, 0.3),
    inset 0 -1px 0 rgba(232, 196, 74, 0.3),
    0 0 24px rgba(232, 196, 74, 0.4);
}
.end-turn-btn .btn-rune-ring { border-color: rgba(232, 196, 74, 0.5); }
.end-turn-btn .btn-label { color: #c9a227; font-weight: 700; }
.end-turn-btn:hover:not(:disabled) .btn-label { color: #ffd700; }

@media (max-width: 560px) {
  .action-bar { flex-direction: column; gap: 10px; padding: 10px 12px; }
  .action-buttons { width: 100%; }
  .rune-action-btn { width: 52px; height: 52px; }
  .btn-icon { font-size: 20px; }
  .btn-label { font-size: 8px; }
}
</style>
