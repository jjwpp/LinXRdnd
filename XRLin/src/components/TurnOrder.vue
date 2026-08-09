<script setup>
import { computed } from "vue";

const props = defineProps({
  round: { type: Number, default: 1 },
  combatPhase: { type: String, default: "PLAYER_TURN" },
  enemies: { type: Array, default: () => [] },
  playerName: { type: String, default: "玩家" },
});

const turnOrder = computed(() => {
  const order = [];
  order.push({
    name: props.playerName,
    type: "player",
    alive: true,
    active: props.combatPhase === "PLAYER_TURN",
    icon: "🧙",
  });
  for (const e of props.enemies || []) {
    order.push({
      name: e.name,
      type: "enemy",
      alive: e.alive,
      active: props.combatPhase === "ENEMY_TURN" && e.alive,
      icon: e.alive ? "👹" : "💀",
    });
  }
  return order;
});
</script>

<template>
  <div class="turn-order-bar">
    <!-- Round indicator with ornate styling -->
    <div class="round-info">
      <span class="round-deco">✦</span>
      <span class="round-icon">⚔</span>
      <span class="round-text">Round {{ round }}</span>
      <span class="round-deco">✦</span>
    </div>
    <!-- Turn order list -->
    <div class="turn-list">
      <div
        v-for="(unit, i) in turnOrder"
        :key="i"
        class="turn-unit"
        :class="[
          unit.type,
          { active: unit.active, dead: !unit.alive }
        ]"
      >
        <span class="tu-icon-wrap">
          <span class="tu-icon">{{ unit.icon }}</span>
        </span>
        <span class="tu-name">{{ unit.name }}</span>
        <span v-if="unit.active" class="tu-indicator"></span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.turn-order-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 20px;
  background: linear-gradient(180deg, rgba(30, 22, 18, 0.85), rgba(17, 13, 10, 0.5));
  border-bottom: 1px solid var(--line-gold, rgba(201, 162, 39, 0.3));
  z-index: 3;
  position: relative;
}
.turn-order-bar::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(201, 162, 39, 0.2), transparent);
}

/* Round indicator */
.round-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 16px;
  border-radius: var(--radius-sm, 4px);
  background: linear-gradient(180deg, var(--bg-card, #251c15), var(--bg-stone, #17100c));
  border: 1px solid var(--crimson-bright, #9b2d2d);
  flex-shrink: 0;
  box-shadow: inset 0 1px 0 rgba(155, 45, 45, 0.1), 0 0 10px rgba(155, 45, 45, 0.08);
}
.round-deco {
  color: var(--gold-dim, #8a7020);
  font-size: 9px;
}
.round-icon {
  font-size: 16px;
  filter: drop-shadow(0 0 4px rgba(155, 45, 45, 0.3));
}
.round-text {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 14px;
  font-weight: 700;
  color: var(--crimson-bright, #9b2d2d);
  letter-spacing: 0.08em;
  text-shadow: 0 0 8px rgba(155, 45, 45, 0.2);
}

/* Turn list */
.turn-list {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  flex: 1;
  scrollbar-width: thin;
}
.turn-unit {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border-radius: var(--radius-sm, 4px);
  border: 1px solid;
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 11px;
  white-space: nowrap;
  position: relative;
  transition: all 0.25s ease;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
}
.tu-icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}
.tu-icon { font-size: 14px; }
.tu-name { font-weight: 600; letter-spacing: 0.03em; }

/* Player turn unit */
.turn-unit.player {
  border-color: rgba(201, 162, 39, 0.4);
  background: linear-gradient(135deg, rgba(201, 162, 39, 0.1), rgba(30, 22, 18, 0.6));
}
.turn-unit.player .tu-name { color: var(--gold, #c9a227); }

/* Enemy turn unit */
.turn-unit.enemy {
  border-color: rgba(155, 45, 45, 0.3);
  background: linear-gradient(135deg, rgba(155, 45, 45, 0.06), rgba(20, 12, 12, 0.6));
}
.turn-unit.enemy .tu-name { color: #d0a0a0; }

/* Dead units */
.turn-unit.dead {
  opacity: 0.25;
  text-decoration: line-through;
  filter: grayscale(1);
}

/* Active turn — glowing pulse */
.turn-unit.active {
  border-width: 2px;
  animation: activePulse 1.5s ease-in-out infinite;
}
.turn-unit.player.active {
  border-color: var(--gold, #c9a227);
  color: var(--gold, #c9a227);
  box-shadow: 0 0 16px rgba(201, 162, 39, 0.3), inset 0 1px 0 rgba(201, 162, 39, 0.15);
}
.turn-unit.enemy.active {
  border-color: var(--crimson-bright, #9b2d2d);
  color: var(--crimson-bright, #9b2d2d);
  box-shadow: 0 0 16px rgba(155, 45, 45, 0.3), inset 0 1px 0 rgba(155, 45, 45, 0.15);
}
@keyframes activePulse {
  0%, 100% { box-shadow: 0 0 10px currentColor; }
  50% { box-shadow: 0 0 20px currentColor; }
}

/* Active indicator dot */
.tu-indicator {
  position: absolute;
  bottom: -3px;
  left: 50%;
  transform: translateX(-50%);
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 6px currentColor;
  animation: indicatorBlink 1s ease-in-out infinite;
}
@keyframes indicatorBlink {
  0%, 100% { opacity: 1; transform: translateX(-50%) scale(1); }
  50% { opacity: 0.5; transform: translateX(-50%) scale(0.7); }
}
</style>
