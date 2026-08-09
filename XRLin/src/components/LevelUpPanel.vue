<script setup>
import { ref, computed, watch } from "vue";

const props = defineProps({
  visible: { type: Boolean, default: false },
  sessionId: { type: String, default: "" },
  levelUpData: { type: Object, default: null },
  characterName: { type: String, default: "" },
});

const emit = defineEmits(["close", "confirmed"]);

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080/api";

const loading = ref(false);
const errorMsg = ref("");

// 玩家选择状态
const asiChoice = ref(null);      // { type: "stat", stat: "str" } | { type: "feat", featId: "xxx" }
const selectedSpell = ref(null);
const selectedFeat = ref(null);
const selectedCombatStyle = ref(null);

// 重置选择
watch(() => props.visible, (v) => {
  if (v) {
    asiChoice.value = null;
    selectedSpell.value = null;
    selectedFeat.value = null;
    selectedCombatStyle.value = null;
    errorMsg.value = "";
  }
});

// 从 rewards 中分类
const passiveRewards = computed(() => {
  if (!props.levelUpData?.rewards) return [];
  return props.levelUpData.rewards.filter(r => !r.requiresChoice);
});

const choiceRewards = computed(() => {
  if (!props.levelUpData?.rewards) return [];
  return props.levelUpData.rewards.filter(r => r.requiresChoice);
});

const hasAsi = computed(() =>
  choiceRewards.value.some(r => r.rewardType === "ASI"));
const hasNewSpell = computed(() =>
  choiceRewards.value.some(r => r.rewardType === "NEW_SPELL"));
const hasCombatStyle = computed(() =>
  choiceRewards.value.some(r => r.rewardType === "COMBAT_STYLE"));
const hasFeatChoice = computed(() =>
  choiceRewards.value.some(r => r.rewardType === "FEAT_CHOICE"));

// 可选项
const availableSpells = computed(() => props.levelUpData?.availableSpells || []);
const availableFeats = computed(() => props.levelUpData?.availableFeats || []);
const asiOptions = computed(() => props.levelUpData?.asiOptions || []);

// 战斗风格选项 (从 reward data 中提取)
const combatStyleOptions = computed(() => {
  const csReward = choiceRewards.value.find(r => r.rewardType === "COMBAT_STYLE");
  return csReward?.data?.options || [];
});

// 是否所有必选项已选择
const allChoicesMade = computed(() => {
  if (hasAsi.value && !asiChoice.value) return false;
  if (hasNewSpell.value && !selectedSpell.value) return false;
  if (hasCombatStyle.value && !selectedCombatStyle.value) return false;
  if (hasFeatChoice.value && !selectedFeat.value) return false;
  return true;
});

// 选择属性
function selectStat(stat) {
  asiChoice.value = { type: "stat", stat, amount: 2 };
}
function selectAsiFeat(featId) {
  asiChoice.value = { type: "feat", featId };
}

// 确认升级
async function confirmLevelUp() {
  if (!allChoicesMade.value || loading.value) return;
  loading.value = true;
  errorMsg.value = "";

  try {
    const choices = {};
    if (asiChoice.value) choices.asi = asiChoice.value;
    if (selectedSpell.value) choices.spellId = selectedSpell.value;
    if (selectedCombatStyle.value) choices.combatStyle = selectedCombatStyle.value;
    if (selectedFeat.value) choices.featId = selectedFeat.value;

    const token = localStorage.getItem("auth_token");
    const res = await fetch(`${API_BASE}/adventure/${props.sessionId}/levelup`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ choices }),
    });
    const json = await res.json();
    const data = json.data || json;
    if (json.code && json.code !== 200) {
      errorMsg.value = json.msg || "升级失败";
      return;
    }
    emit("confirmed", data);
  } catch (e) {
    errorMsg.value = "升级失败: " + e.message;
  } finally {
    loading.value = false;
  }
}

const statNames = {
  str: "力量", dex: "敏捷", con: "体质",
  int: "智力", wis: "感知", cha: "魅力",
};
</script>

<template>
  <transition name="lp-fade">
    <div v-if="visible && levelUpData" class="lp-overlay">
      <!-- Golden aura background -->
      <div class="lp-aura"></div>
      <!-- Rising golden particles -->
      <div class="lp-particles">
        <span v-for="n in 16" :key="n" class="lp-particle" :style="{ '--n': n }"></span>
      </div>

      <div class="lp-modal">
        <!-- Ornate corner frames -->
        <div class="lp-corner lp-corner-tl"></div>
        <div class="lp-corner lp-corner-tr"></div>
        <div class="lp-corner lp-corner-bl"></div>
        <div class="lp-corner lp-corner-br"></div>

        <!-- ═══ Header ═══ -->
        <div class="lp-header">
          <div class="lp-header-glow"></div>
          <div class="lp-rune-circle">
            <span class="lp-rune lp-rune-1">✦</span>
            <span class="lp-rune lp-rune-2">✧</span>
            <span class="lp-rune lp-rune-3">✦</span>
          </div>
          <div class="lp-star-wrap">
            <span class="lp-star">★</span>
            <div class="lp-star-aura"></div>
          </div>
          <h2 class="lp-awaken-title text-glow-gold">新的力量觉醒</h2>
          <div class="lp-level-row">
            <span class="lp-level-current">Lv.{{ levelUpData.newLevel - 1 }}</span>
            <span class="lp-arrow">⟶</span>
            <span class="lp-level-new">Lv.{{ levelUpData.newLevel }}</span>
          </div>
          <div class="lp-class-badge">{{ levelUpData.className }}</div>
          <p class="lp-character-name">{{ characterName }}</p>
        </div>

        <div class="lp-divider">
          <span class="lp-divider-deco">✦</span>
        </div>

        <!-- ═══ 自动获得的奖励 ═══ -->
        <div v-if="passiveRewards.length" class="lp-section">
          <h3 class="lp-section-title">
            <span class="lp-section-icon">⚡</span>
            <span>自动获得</span>
          </h3>
          <div class="lp-passive-list">
            <div v-for="r in passiveRewards" :key="r.id" class="lp-passive-card">
              <div class="lp-passive-icon">✦</div>
              <div class="lp-passive-info">
                <span class="lp-passive-name">{{ r.rewardName }}</span>
                <span class="lp-passive-desc">{{ r.description }}</span>
              </div>
              <div class="lp-passive-glow"></div>
            </div>
          </div>
        </div>

        <!-- ═══ 属性提升选择 ═══ -->
        <div v-if="hasAsi" class="lp-section">
          <h3 class="lp-section-title">
            <span class="lp-section-icon">📊</span>
            <span>属性提升</span>
          </h3>
          <p class="lp-hint">选择一项属性 +2，或选择一个专长</p>
          <div class="lp-asi-grid">
            <div
              v-for="opt in asiOptions.filter(o => o !== 'feat')"
              :key="opt"
              class="lp-asi-card"
              :class="{ selected: asiChoice?.type === 'stat' && asiChoice?.stat === opt.split('+')[0] }"
              @click="selectStat(opt.split('+')[0])"
            >
              <span class="lp-asi-stat">{{ statNames[opt.split('+')[0]] }}</span>
              <span class="lp-asi-bonus">+2</span>
              <div class="lp-asi-glow"></div>
            </div>
          </div>
          <div class="lp-asi-feat-section">
            <div class="lp-sub-label">或选择专长</div>
            <div class="lp-choice-grid">
              <div
                v-for="f in availableFeats"
                :key="f.id"
                class="lp-choice-card"
                :class="{ selected: asiChoice?.type === 'feat' && asiChoice?.featId === f.id }"
                @click="selectAsiFeat(f.id)"
              >
                <span class="lp-choice-name">{{ f.name }}</span>
                <span class="lp-choice-summary">{{ f.summary }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- ═══ 战斗风格选择 ═══ -->
        <div v-if="hasCombatStyle" class="lp-section">
          <h3 class="lp-section-title">
            <span class="lp-section-icon">⚔️</span>
            <span>战斗风格</span>
          </h3>
          <p class="lp-hint">选择一种战斗风格</p>
          <div class="lp-choice-grid">
            <div
              v-for="cs in combatStyleOptions"
              :key="cs.id"
              class="lp-choice-card"
              :class="{ selected: selectedCombatStyle === cs.id }"
              @click="selectedCombatStyle = cs.id"
            >
              <span class="lp-choice-name">{{ cs.name }}</span>
              <span class="lp-choice-summary">{{ cs.description }}</span>
            </div>
          </div>
        </div>

        <!-- ═══ 法术选择 ═══ -->
        <div v-if="hasNewSpell" class="lp-section">
          <h3 class="lp-section-title">
            <span class="lp-section-icon">📖</span>
            <span>学习新法术</span>
          </h3>
          <div v-if="availableSpells.length === 0" class="lp-empty">暂无可学法术</div>
          <div v-else class="lp-choice-grid">
            <div
              v-for="s in availableSpells"
              :key="s.id"
              class="lp-choice-card lp-spell-card"
              :class="{ selected: selectedSpell === s.id }"
              @click="selectedSpell = s.id"
            >
              <span class="lp-choice-name">{{ s.name }}</span>
              <span class="lp-choice-summary">{{ s.summary }}</span>
            </div>
          </div>
        </div>

        <!-- ═══ 专长选择 (FEAT_CHOICE) ═══ -->
        <div v-if="hasFeatChoice" class="lp-section">
          <h3 class="lp-section-title">
            <span class="lp-section-icon">🏆</span>
            <span>选择专长</span>
          </h3>
          <div class="lp-choice-grid">
            <div
              v-for="f in availableFeats"
              :key="f.id"
              class="lp-choice-card"
              :class="{ selected: selectedFeat === f.id }"
              @click="selectedFeat = f.id"
            >
              <span class="lp-choice-name">{{ f.name }}</span>
              <span class="lp-choice-summary">{{ f.summary }}</span>
            </div>
          </div>
        </div>

        <!-- ═══ 错误信息 ═══ -->
        <div v-if="errorMsg" class="lp-error">⚠️ {{ errorMsg }}</div>

        <!-- ═══ 按钮 ═══ -->
        <div class="lp-buttons">
          <button class="lp-cancel-btn" @click="emit('close')" :disabled="loading">
            <span>取消</span>
          </button>
          <button
            class="lp-confirm-btn"
            :disabled="!allChoicesMade || loading"
            @click="confirmLevelUp"
          >
            <span class="lp-confirm-glow"></span>
            <span v-if="loading" class="lp-btn-loading">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </span>
            <span v-else class="lp-confirm-text">确认升级</span>
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
/* ═══ Overlay & Background Aura ═══ */
.lp-fade-enter-active { animation: lpFade 0.5s ease; }
.lp-fade-leave-active { animation: lpFade 0.3s ease reverse; }
@keyframes lpFade { from { opacity: 0; } to { opacity: 1; } }

.lp-overlay {
  position: fixed;
  inset: 0;
  background: radial-gradient(ellipse at center, rgba(50, 35, 5, 0.8), rgba(0, 0, 0, 0.92));
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  overflow-y: auto;
  padding: 20px;
  backdrop-filter: blur(10px);
}

/* Pulsing golden aura behind modal */
.lp-aura {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 800px;
  height: 800px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(201, 162, 39, 0.08), rgba(201, 162, 39, 0.02) 50%, transparent 70%);
  animation: lpAuraPulse 4s ease-in-out infinite;
  pointer-events: none;
}
@keyframes lpAuraPulse {
  0%, 100% { opacity: 0.6; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 1; transform: translate(-50%, -50%) scale(1.1); }
}

/* Rising golden particles */
.lp-particles {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 1;
}
.lp-particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: var(--gold-bright, #e8c44a);
  border-radius: 50%;
  opacity: 0;
  box-shadow: 0 0 8px rgba(232, 196, 74, 0.7);
  animation: lpParticleRise 8s ease-in infinite;
  animation-delay: calc(var(--n) * 0.5s);
  left: calc(5% + var(--n) * 6%);
  bottom: 0;
}
@keyframes lpParticleRise {
  0% { opacity: 0; transform: translateY(0) scale(0.5); }
  10% { opacity: 0.8; }
  90% { opacity: 0.3; }
  100% { opacity: 0; transform: translateY(-100vh) scale(1.2); }
}

/* ═══ Modal ═══ */
.lp-modal {
  position: relative;
  background: linear-gradient(135deg, #1a1520, #0d0b14);
  border: 1px solid var(--gold, #c9a227);
  border-radius: 14px;
  padding: 32px 28px 28px;
  max-width: 680px;
  width: 100%;
  max-height: 88vh;
  overflow-y: auto;
  box-shadow:
    0 0 60px rgba(232, 196, 74, 0.15),
    0 0 120px rgba(201, 162, 39, 0.08),
    0 8px 40px rgba(0, 0, 0, 0.6),
    inset 0 0 60px rgba(201, 162, 39, 0.02);
  animation: lpRise 0.6s cubic-bezier(.34, 1.56, .64, 1);
  z-index: 2;
}
@keyframes lpRise {
  from { opacity: 0; transform: translateY(40px) scale(0.92); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* Ornate corner frames */
.lp-corner {
  position: absolute;
  width: 20px;
  height: 20px;
  pointer-events: none;
  z-index: 3;
}
.lp-corner-tl { top: 6px; left: 6px; border-top: 2px solid var(--gold, #c9a227); border-left: 2px solid var(--gold, #c9a227); border-top-left-radius: 6px; }
.lp-corner-tr { top: 6px; right: 6px; border-top: 2px solid var(--gold, #c9a227); border-right: 2px solid var(--gold, #c9a227); border-top-right-radius: 6px; }
.lp-corner-bl { bottom: 6px; left: 6px; border-bottom: 2px solid var(--gold, #c9a227); border-left: 2px solid var(--gold, #c9a227); border-bottom-left-radius: 6px; }
.lp-corner-br { bottom: 6px; right: 6px; border-bottom: 2px solid var(--gold, #c9a227); border-right: 2px solid var(--gold, #c9a227); border-bottom-right-radius: 6px; }

/* Custom scrollbar */
.lp-modal::-webkit-scrollbar { width: 8px; }
.lp-modal::-webkit-scrollbar-track { background: rgba(0, 0, 0, 0.3); border-radius: 4px; }
.lp-modal::-webkit-scrollbar-thumb {
  background: linear-gradient(var(--gold-dim, #8a7020), var(--gold, #c9a227));
  border-radius: 4px;
}

/* ═══ Header ═══ */
.lp-header {
  text-align: center;
  margin-bottom: 8px;
  position: relative;
}
.lp-header-glow {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(232, 196, 74, 0.12), transparent 70%);
  pointer-events: none;
  animation: lpHeaderGlow 3s ease-in-out infinite;
}
@keyframes lpHeaderGlow {
  0%, 100% { opacity: 0.5; transform: translateX(-50%) scale(1); }
  50% { opacity: 1; transform: translateX(-50%) scale(1.15); }
}

/* Rune circle decoration */
.lp-rune-circle {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 8px;
  opacity: 0.5;
}
.lp-rune {
  font-size: 14px;
  color: var(--gold, #c9a227);
  animation: lpRuneFloat 3s ease-in-out infinite;
}
.lp-rune-2 { animation-delay: 1s; }
.lp-rune-3 { animation-delay: 2s; }
@keyframes lpRuneFloat {
  0%, 100% { transform: translateY(0); opacity: 0.3; }
  50% { transform: translateY(-4px); opacity: 0.8; }
}

/* Star with aura */
.lp-star-wrap {
  position: relative;
  display: inline-block;
  margin-bottom: 10px;
}
.lp-star {
  font-size: 42px;
  color: var(--gold-bright, #e8c44a);
  filter: drop-shadow(0 0 12px rgba(232, 196, 74, 0.6));
  animation: lpStarPulse 1.8s ease-in-out infinite;
  display: block;
}
@keyframes lpStarPulse {
  0%, 100% { transform: scale(1) rotate(0deg); filter: drop-shadow(0 0 12px rgba(232, 196, 74, 0.6)); }
  50% { transform: scale(1.15) rotate(15deg); filter: drop-shadow(0 0 24px rgba(232, 196, 74, 0.9)); }
}
.lp-star-aura {
  position: absolute;
  inset: -20px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(232, 196, 74, 0.15), transparent 70%);
  animation: lpStarAura 3s ease-in-out infinite;
  pointer-events: none;
}
@keyframes lpStarAura {
  0%, 100% { opacity: 0.4; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.3); }
}

/* Main title */
.lp-awaken-title {
  font-family: var(--font-display, "Cinzel Decorative", serif);
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  letter-spacing: 0.12em;
  background: linear-gradient(180deg, var(--gold-bright, #e8c44a), var(--gold, #c9a227), var(--gold-dim, #8a7020));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 0 30px rgba(232, 196, 74, 0.3);
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.5));
}

.lp-level-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 8px;
}
.lp-level-current {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 15px;
  color: var(--muted, #6b5d4a);
  font-weight: 600;
}
.lp-arrow {
  color: var(--gold, #c9a227);
  font-size: 20px;
  animation: lpArrowPulse 2s ease-in-out infinite;
}
@keyframes lpArrowPulse {
  0%, 100% { opacity: 0.6; transform: translateX(0); }
  50% { opacity: 1; transform: translateX(4px); }
}
.lp-level-new {
  font-family: var(--font-display, "Cinzel Decorative", serif);
  font-size: 22px;
  color: var(--gold-bright, #e8c44a);
  font-weight: 800;
  text-shadow: 0 0 16px rgba(232, 196, 74, 0.4);
}

.lp-class-badge {
  display: inline-block;
  background: linear-gradient(135deg, rgba(201, 162, 39, 0.15), rgba(201, 162, 39, 0.05));
  border: 1px solid var(--gold, #c9a227);
  color: var(--gold-bright, #e8c44a);
  padding: 4px 16px;
  border-radius: 20px;
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  box-shadow: 0 0 12px rgba(201, 162, 39, 0.1);
}

.lp-character-name {
  font-family: var(--font-body, serif);
  font-size: 14px;
  color: var(--ink-soft, #a89880);
  margin: 10px 0 0;
  font-style: italic;
}

/* ═══ Divider ═══ */
.lp-divider {
  display: flex;
  align-items: center;
  margin: 20px 0;
}
.lp-divider::before,
.lp-divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-gold, rgba(201, 162, 39, 0.3)), transparent);
}
.lp-divider-deco {
  color: var(--gold, #c9a227);
  font-size: 14px;
  padding: 0 16px;
}

/* ═══ Sections ═══ */
.lp-section { margin-bottom: 22px; }
.lp-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 15px;
  font-weight: 700;
  color: var(--gold, #c9a227);
  margin: 0 0 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(201, 162, 39, 0.2);
  letter-spacing: 0.05em;
}
.lp-section-icon {
  font-size: 16px;
  filter: drop-shadow(0 0 6px rgba(201, 162, 39, 0.3));
}
.lp-hint {
  font-size: 12px;
  color: var(--muted, #6b5d4a);
  margin-bottom: 12px;
  font-style: italic;
}

/* ═══ Passive Rewards ═══ */
.lp-passive-list { display: flex; flex-direction: column; gap: 8px; }
.lp-passive-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(201, 162, 39, 0.06), rgba(201, 162, 39, 0.02));
  border: 1px solid rgba(201, 162, 39, 0.15);
  border-radius: 8px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}
.lp-passive-card:hover {
  border-color: rgba(201, 162, 39, 0.35);
  box-shadow: 0 0 16px rgba(201, 162, 39, 0.08);
}
.lp-passive-icon {
  color: var(--gold, #c9a227);
  font-size: 16px;
  margin-top: 1px;
  filter: drop-shadow(0 0 4px rgba(201, 162, 39, 0.3));
}
.lp-passive-info { display: flex; flex-direction: column; gap: 3px; flex: 1; }
.lp-passive-name {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 14px;
  font-weight: 700;
  color: var(--ink-bright, #f0e6d4);
}
.lp-passive-desc { font-size: 12px; color: var(--muted, #6b5d4a); line-height: 1.5; }
.lp-passive-glow {
  position: absolute;
  right: -20px;
  top: -20px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(201, 162, 39, 0.06), transparent 70%);
  pointer-events: none;
}

/* ═══ ASI Grid ═══ */
.lp-asi-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 14px;
}
.lp-asi-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px 8px;
  background: linear-gradient(135deg, rgba(30, 22, 18, 0.8), rgba(17, 13, 10, 0.6));
  border: 2px solid rgba(61, 47, 32, 0.6);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}
.lp-asi-card:hover {
  border-color: var(--gold, #c9a227);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(232, 196, 74, 0.12);
}
.lp-asi-card.selected {
  border-color: var(--gold-bright, #e8c44a);
  background: linear-gradient(135deg, rgba(201, 162, 39, 0.15), rgba(201, 162, 39, 0.05));
  box-shadow:
    0 0 0 1px var(--gold, #c9a227),
    0 0 20px rgba(232, 196, 74, 0.2),
    inset 0 0 20px rgba(201, 162, 39, 0.05);
}
.lp-asi-glow {
  position: absolute;
  inset: 0;
  border-radius: 6px;
  background: radial-gradient(circle at center, rgba(232, 196, 74, 0.08), transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}
.lp-asi-card.selected .lp-asi-glow { opacity: 1; animation: lpGlowShimmer 2s ease-in-out infinite; }
@keyframes lpGlowShimmer {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}
.lp-asi-stat {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 14px;
  font-weight: 700;
  color: var(--ink, #d4c8b8);
  z-index: 1;
}
.lp-asi-card.selected .lp-asi-stat { color: var(--gold-bright, #e8c44a); }
.lp-asi-bonus {
  font-family: var(--font-display, "Cinzel Decorative", serif);
  font-size: 20px;
  font-weight: 800;
  color: var(--gold, #c9a227);
  z-index: 1;
}
.lp-asi-card.selected .lp-asi-bonus { color: var(--gold-bright, #e8c44a); text-shadow: 0 0 12px rgba(232, 196, 74, 0.5); }

.lp-asi-feat-section { margin-top: 8px; }
.lp-sub-label {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 12px;
  color: var(--muted, #6b5d4a);
  margin-bottom: 8px;
  letter-spacing: 0.03em;
}

/* ═══ Choice Grid ═══ */
.lp-choice-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.lp-choice-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  background: linear-gradient(135deg, rgba(30, 22, 18, 0.8), rgba(17, 13, 10, 0.6));
  border: 2px solid rgba(61, 47, 32, 0.5);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}
.lp-choice-card::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent, rgba(201, 162, 39, 0.03));
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}
.lp-choice-card:hover {
  border-color: var(--gold, #c9a227);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(232, 196, 74, 0.1);
}
.lp-choice-card:hover::before { opacity: 1; }
.lp-choice-card.selected {
  border-color: var(--gold-bright, #e8c44a);
  background: linear-gradient(135deg, rgba(201, 162, 39, 0.12), rgba(201, 162, 39, 0.03));
  box-shadow:
    0 0 0 1px var(--gold, #c9a227),
    0 0 20px rgba(232, 196, 74, 0.15),
    inset 0 0 30px rgba(201, 162, 39, 0.04);
}
.lp-choice-card.selected::after {
  content: "\2713";
  position: absolute;
  top: 8px;
  right: 8px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--gold-bright, #e8c44a), var(--gold, #c9a227));
  color: #1a1010;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 0 10px rgba(232, 196, 74, 0.4);
  animation: lpCheckPop 0.3s cubic-bezier(.34, 1.56, .64, 1);
}
@keyframes lpCheckPop {
  from { transform: scale(0); }
  to { transform: scale(1); }
}
.lp-choice-name {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 14px;
  font-weight: 700;
  color: var(--ink-bright, #f0e6d4);
  z-index: 1;
}
.lp-choice-card.selected .lp-choice-name { color: var(--gold-bright, #e8c44a); }
.lp-choice-summary { font-size: 11px; color: var(--muted, #6b5d4a); line-height: 1.5; z-index: 1; }

/* Spell cards — arcane theme */
.lp-spell-card { border-color: rgba(125, 90, 170, 0.25); }
.lp-spell-card:hover { border-color: var(--arcane-glow, #7d5aaa); box-shadow: 0 4px 16px rgba(125, 90, 170, 0.12); }
.lp-spell-card.selected {
  border-color: var(--arcane-glow, #7d5aaa);
  background: linear-gradient(135deg, rgba(93, 58, 138, 0.12), rgba(93, 58, 138, 0.03));
  box-shadow: 0 0 0 1px var(--arcane-glow, #7d5aaa), 0 0 20px rgba(125, 90, 170, 0.15);
}
.lp-spell-card.selected::after { background: linear-gradient(135deg, var(--arcane-glow, #7d5aaa), var(--arcane, #5d3a8a)); }

.lp-empty {
  text-align: center;
  color: var(--muted, #6b5d4a);
  padding: 20px;
  font-size: 13px;
  font-style: italic;
}

.lp-error {
  color: var(--crimson-bright, #9b2d2d);
  font-size: 13px;
  margin-bottom: 12px;
  text-align: center;
  padding: 10px;
  background: rgba(107, 29, 29, 0.15);
  border: 1px solid rgba(155, 45, 45, 0.3);
  border-radius: 6px;
}

/* ═══ Buttons ═══ */
.lp-buttons {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}
.lp-cancel-btn {
  flex: 1;
  background: rgba(30, 22, 18, 0.6);
  border: 1px solid rgba(61, 47, 32, 0.6);
  color: var(--ink-soft, #a89880);
  padding: 14px;
  border-radius: 8px;
  cursor: pointer;
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  transition: all 0.25s ease;
}
.lp-cancel-btn:hover:not(:disabled) {
  background: rgba(61, 47, 32, 0.4);
  border-color: var(--line-light, #5a4630);
  color: var(--ink, #d4c8b8);
}
.lp-cancel-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.lp-confirm-btn {
  flex: 2;
  position: relative;
  background: linear-gradient(135deg, var(--gold, #c9a227), var(--gold-dim, #8a7020));
  border: 1px solid var(--gold-bright, #e8c44a);
  color: #1a1010;
  padding: 14px;
  border-radius: 8px;
  cursor: pointer;
  font-family: var(--font-display, "Cinzel Decorative", serif);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.08em;
  transition: all 0.25s ease;
  box-shadow:
    0 0 20px rgba(232, 196, 74, 0.2),
    0 4px 16px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
  overflow: hidden;
}
.lp-confirm-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transform: translateX(-100%);
  transition: transform 0.6s ease;
  pointer-events: none;
}
.lp-confirm-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow:
    0 0 30px rgba(232, 196, 74, 0.35),
    0 8px 24px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}
.lp-confirm-btn:hover:not(:disabled) .lp-confirm-glow { transform: translateX(100%); }
.lp-confirm-btn:active:not(:disabled) { transform: translateY(0); }
.lp-confirm-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.lp-confirm-text, .lp-btn-loading { position: relative; z-index: 1; }
.lp-btn-loading { display: flex; gap: 5px; justify-content: center; }
.dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #1a1010;
  animation: lpDotBounce 1s infinite alternate;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes lpDotBounce { to { opacity: 0.3; transform: translateY(-5px); } }

/* ═══ Responsive ═══ */
@media (max-width: 520px) {
  .lp-modal { padding: 24px 18px; }
  .lp-awaken-title { font-size: 22px; }
  .lp-asi-grid { grid-template-columns: repeat(2, 1fr); }
  .lp-choice-grid { grid-template-columns: 1fr; }
  .lp-buttons { flex-direction: column; }
}
</style>
