<script setup>
import { computed } from "vue";
import monsterImages from "../composables/useMonsterImages";

const props = defineProps({
  visible: { type: Boolean, default: false },
  encounterInfo: { type: Object, default: null },
  narrative: { type: String, default: "" },
  loading: { type: Boolean, default: false },
});

const emit = defineEmits(["confirm", "dismiss"]);

const dangerConfig = {
  TRIVIAL: { label: "轻松", color: "#4caf50", icon: "🟢" },
  EASY: { label: "简单", color: "#8bc34a", icon: "🟢" },
  MEDIUM: { label: "中等", color: "#ff9800", icon: "🟡" },
  HARD: { label: "困难", color: "#f44336", icon: "🔴" },
  DEADLY: { label: "致命", color: "#b71c1c", icon: "💀" },
};

const dangerDisplay = computed(() => {
  const level = props.encounterInfo?.dangerLevel || "MEDIUM";
  return dangerConfig[level] || dangerConfig.MEDIUM;
});

const enemyGroups = computed(() => {
  if (!props.encounterInfo?.enemies) return [];
  const groups = {};
  for (const e of props.encounterInfo.enemies) {
    if (!groups[e.name]) {
      groups[e.name] = { ...e, count: 0 };
    }
    groups[e.name].count++;
  }
  return Object.values(groups);
});

// 获取敌人预览图片（优先 MinIO URL，回退本地静态图）
function getPreviewImage(group) {
  if (group.imageUrl) return group.imageUrl;
  if (group.monsterId && monsterImages[group.monsterId]) return monsterImages[group.monsterId];
  return null;
}
</script>

<template>
  <transition name="encounter-fade">
    <div v-if="visible && encounterInfo" class="encounter-overlay">
      <div class="encounter-backdrop"></div>
      <div class="encounter-scroll-wrap">
        <!-- Scroll top roller -->
        <div class="scroll-roller-top">
          <div class="roller-cap-left"></div>
          <div class="roller-cap-right"></div>
        </div>

        <div class="encounter-modal scroll-unroll-content">
          <!-- Ornate corner frames -->
          <div class="enc-corner enc-corner-tl"></div>
          <div class="enc-corner enc-corner-tr"></div>
          <div class="enc-corner enc-corner-bl"></div>
          <div class="enc-corner enc-corner-br"></div>

          <!-- Title area -->
          <div class="encounter-header">
            <div class="encounter-icon-wrap">
              <span class="encounter-swords">⚔️</span>
              <div class="encounter-glow"></div>
            </div>
            <h2 class="encounter-title text-glow-gold">遭 遇 战</h2>
            <div class="encounter-subtitle">发现敌人！</div>
          </div>

          <!-- Narrative text -->
          <div v-if="narrative" class="encounter-narrative">
            {{ narrative }}
          </div>

          <!-- Danger level badge -->
          <div class="danger-badge" :style="{ borderColor: dangerDisplay.color, color: dangerDisplay.color }">
            <span class="danger-icon">{{ dangerDisplay.icon }}</span>
            <span class="danger-label">危险等级: {{ dangerDisplay.label }}</span>
          </div>

          <!-- Enemy list -->
          <div class="enemy-preview-list">
            <div class="preview-header">
              <span class="preview-title">敌人信息</span>
              <span class="preview-count">共 {{ encounterInfo.enemyCount }} 个敌人</span>
            </div>
            <div v-for="(group, i) in enemyGroups" :key="i" class="enemy-preview-card">
              <div class="epc-left">
                <div class="epc-avatar" :class="{ 'has-image': getPreviewImage(group) }">
                  <img v-if="getPreviewImage(group)" :src="getPreviewImage(group)" :alt="group.name" class="epc-avatar-img" />
                  <span v-else>👹</span>
                </div>
                <div class="epc-count" v-if="group.count > 1">×{{ group.count }}</div>
              </div>
              <div class="epc-right">
                <div class="epc-name">{{ group.name }}</div>
                <div class="epc-stats">
                  <span class="epc-stat">
                    <span class="epc-stat-label">HP</span>
                    <span class="epc-stat-val">{{ group.hp }}</span>
                  </span>
                  <span class="epc-stat">
                    <span class="epc-stat-label">AC</span>
                    <span class="epc-stat-val">{{ group.ac }}</span>
                  </span>
                  <span class="epc-stat">
                    <span class="epc-stat-label">CR</span>
                    <span class="epc-stat-val">{{ group.cr }}</span>
                  </span>
                  <span class="epc-stat" v-if="group.damageType">
                    <span class="epc-stat-label">伤害</span>
                    <span class="epc-stat-val">{{ group.damageType }}</span>
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Confirm button -->
          <div class="encounter-actions">
            <button class="encounter-confirm-btn" :disabled="loading" @click="emit('confirm')">
              <span v-if="loading" class="btn-loading">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
              </span>
              <span v-else>⚔ 进入战斗</span>
            </button>
          </div>
        </div>

        <!-- Scroll bottom roller -->
        <div class="scroll-roller-bottom">
          <div class="roller-cap-left"></div>
          <div class="roller-cap-right"></div>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.encounter-overlay {
  position: fixed;
  inset: 0;
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.encounter-backdrop {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, rgba(40, 5, 5, 0.75), rgba(0, 0, 0, 0.92));
  backdrop-filter: blur(6px);
}

/* Scroll wrapper for unrolling effect */
.encounter-scroll-wrap {
  position: relative;
  max-width: 480px;
  width: 100%;
  max-height: 85vh;
  overflow-y: auto;
  animation: scrollUnrollWrap 0.6s cubic-bezier(.34, 1.56, .64, 1);
}
@keyframes scrollUnrollWrap {
  0% { max-height: 0; opacity: 0; transform: scaleY(0); }
  30% { opacity: 1; }
  100% { max-height: 85vh; opacity: 1; transform: scaleY(1); }
}

/* Scroll rollers */
.scroll-roller-top,
.scroll-roller-bottom {
  height: 14px;
  background: linear-gradient(180deg, #3d2f20, #2a1f17, #3d2f20);
  border: 1px solid var(--gold-dim, #8a7020);
  border-radius: 4px;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
}
.scroll-roller-top { border-bottom: none; border-radius: 4px 4px 0 0; }
.scroll-roller-bottom { border-top: none; border-radius: 0 0 4px 4px; }
.roller-cap-left,
.roller-cap-right {
  position: absolute;
  top: -3px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: radial-gradient(circle, #5a4630, #3d2f20);
  border: 1px solid var(--gold-dim, #8a7020);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.5);
}
.roller-cap-left { left: -8px; }
.roller-cap-right { right: -8px; }

.encounter-modal {
  position: relative;
  background: linear-gradient(135deg, rgba(42, 31, 23, 0.95), rgba(30, 22, 18, 0.95));
  border-left: 2px solid var(--gold-dim, #8a7020);
  border-right: 2px solid var(--gold-dim, #8a7020);
  padding: 36px 32px 28px;
  width: 100%;
  box-shadow: 0 0 60px rgba(201, 162, 39, 0.15), inset 0 0 60px rgba(0, 0, 0, 0.3);
}
.scroll-unroll-content {
  animation: contentFade 0.4s ease 0.3s backwards;
}
@keyframes contentFade {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* Ornate corners */
.enc-corner {
  position: absolute;
  width: 18px;
  height: 18px;
  pointer-events: none;
}
.enc-corner-tl { top: 4px; left: 4px; border-top: 2px solid var(--gold, #c9a227); border-left: 2px solid var(--gold, #c9a227); }
.enc-corner-tr { top: 4px; right: 4px; border-top: 2px solid var(--gold, #c9a227); border-right: 2px solid var(--gold, #c9a227); }
.enc-corner-bl { bottom: 4px; left: 4px; border-bottom: 2px solid var(--gold, #c9a227); border-left: 2px solid var(--gold, #c9a227); }
.enc-corner-br { bottom: 4px; right: 4px; border-bottom: 2px solid var(--gold, #c9a227); border-right: 2px solid var(--gold, #c9a227); }

.encounter-fade-enter-active { animation: encounterFade 0.3s ease; }
.encounter-fade-leave-active { animation: encounterFade 0.3s ease reverse; }
@keyframes encounterFade { from { opacity: 0; } to { opacity: 1; } }

/* Title */
.encounter-header { text-align: center; margin-bottom: 24px; }
.encounter-icon-wrap {
  position: relative;
  display: inline-block;
  margin-bottom: 12px;
}
.encounter-swords {
  font-size: 52px;
  filter: drop-shadow(0 0 20px rgba(155, 45, 45, 0.5));
  animation: swordGlow 2s ease-in-out infinite;
}
@keyframes swordGlow {
  0%, 100% { transform: scale(1) rotate(0); filter: drop-shadow(0 0 20px rgba(155, 45, 45, 0.5)); }
  50% { transform: scale(1.1) rotate(-3deg); filter: drop-shadow(0 0 30px rgba(155, 45, 45, 0.7)); }
}
.encounter-glow {
  position: absolute;
  inset: -20px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(155, 45, 45, 0.15), transparent 70%);
  animation: glowPulse 3s ease-in-out infinite;
}
@keyframes glowPulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.2); }
}
.encounter-title {
  font-family: var(--font-display, "Cinzel Decorative", serif);
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 4px;
  background: linear-gradient(135deg, #9b2d2d, #c9a227, #9b2d2d);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.15em;
}
.encounter-subtitle {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 14px;
  color: var(--ink-soft, #a89880);
  letter-spacing: 0.05em;
}

/* Narrative */
.encounter-narrative {
  font-family: var(--font-body, serif);
  font-size: 14px;
  line-height: 1.8;
  color: var(--ink, #d4c8b8);
  margin-bottom: 16px;
  padding: 14px 18px;
  background: rgba(20, 15, 12, 0.5);
  border-left: 3px solid var(--gold, #c9a227);
  border-radius: 0 var(--radius-sm, 4px) var(--radius-sm, 4px) 0;
  white-space: pre-wrap;
}

/* Danger badge */
.danger-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border: 1px solid;
  border-radius: 20px;
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 18px;
  width: fit-content;
  margin-left: auto;
  margin-right: auto;
  background: rgba(0, 0, 0, 0.3);
  letter-spacing: 0.03em;
}
.danger-icon { font-size: 16px; }

/* Enemy list */
.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(201, 162, 39, 0.2);
}
.preview-title {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 14px;
  font-weight: 700;
  color: var(--gold, #c9a227);
  letter-spacing: 0.05em;
}
.preview-count {
  font-size: 12px;
  color: var(--muted, #6b5d4a);
}
.enemy-preview-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  margin-bottom: 8px;
  background: linear-gradient(135deg, rgba(30, 15, 15, 0.6), rgba(20, 12, 12, 0.5));
  border: 1px solid rgba(155, 45, 45, 0.2);
  border-radius: var(--radius-sm, 4px);
  transition: all 0.25s ease;
  animation: cardSlideIn 0.4s ease backwards;
  box-shadow: inset 0 1px 0 rgba(155, 45, 45, 0.05);
}
.enemy-preview-card:nth-child(2) { animation-delay: 0.1s; }
.enemy-preview-card:nth-child(3) { animation-delay: 0.2s; }
.enemy-preview-card:nth-child(4) { animation-delay: 0.3s; }
@keyframes cardSlideIn {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}
.enemy-preview-card:hover {
  border-color: rgba(155, 45, 45, 0.5);
  background: linear-gradient(135deg, rgba(40, 20, 20, 0.7), rgba(30, 15, 15, 0.6));
  box-shadow: 0 0 12px rgba(155, 45, 45, 0.1);
}
.epc-left {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.epc-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: radial-gradient(circle, #2a1a1a, #1a0a0a);
  border: 2px solid var(--crimson-bright, #9b2d2d);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  box-shadow: 0 0 12px rgba(155, 45, 45, 0.2), inset 0 2px 4px rgba(0, 0, 0, 0.5);
  overflow: hidden;
  position: relative;
}
.epc-avatar.has-image {
  border-color: var(--gold, #c9a227);
  box-shadow: 0 0 12px var(--gold-glow, rgba(201, 162, 39, 0.25));
}
.epc-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
}
.epc-count {
  position: absolute;
  top: -4px;
  right: -8px;
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 12px;
  font-weight: 800;
  color: #fff;
  background: var(--gold, #c9a227);
  padding: 1px 7px;
  border-radius: 10px;
  min-width: 20px;
  text-align: center;
  box-shadow: 0 0 6px rgba(201, 162, 39, 0.3);
}
.epc-right { flex: 1; }
.epc-name {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 15px;
  font-weight: 700;
  color: var(--ink-bright, #f0e6d4);
  margin-bottom: 4px;
}
.epc-stats { display: flex; gap: 12px; flex-wrap: wrap; }
.epc-stat { display: flex; align-items: center; gap: 3px; }
.epc-stat-label {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 10px;
  color: var(--muted, #6b5d4a);
  font-weight: 600;
  text-transform: uppercase;
}
.epc-stat-val {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 13px;
  color: var(--ink, #d4c8b8);
  font-weight: 700;
}

/* Confirm button */
.encounter-actions { margin-top: 24px; }
.encounter-confirm-btn {
  width: 100%;
  background: linear-gradient(135deg, #9b2d2d, #6b1d1d, #4a0a0a);
  border: 1px solid var(--crimson-bright, #9b2d2d);
  color: #fff;
  padding: 16px;
  border-radius: var(--radius-sm, 4px);
  cursor: pointer;
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 17px;
  font-weight: 700;
  transition: all 0.25s ease;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1), 0 4px 24px rgba(155, 45, 45, 0.3);
  letter-spacing: 0.1em;
}
.encounter-confirm-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.15), 0 8px 32px rgba(155, 45, 45, 0.5);
  filter: brightness(1.15);
}
.encounter-confirm-btn:active:not(:disabled) {
  transform: translateY(0);
}
.encounter-confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-loading { display: flex; gap: 5px; justify-content: center; }
.dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #fff;
  animation: dotBounce 1s infinite alternate;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes dotBounce { to { opacity: 0.3; transform: translateY(-5px); } }

@media (max-width: 480px) {
  .encounter-modal { padding: 24px 18px; }
  .encounter-title { font-size: 26px; }
  .epc-stats { gap: 8px; }
}
</style>
