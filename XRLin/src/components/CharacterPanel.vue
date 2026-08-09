<script setup>
import { ref, watch } from "vue";

const props = defineProps({
  visible: { type: Boolean, default: false },
  sessionId: { type: String, default: "" },
});

const emit = defineEmits(["close"]);

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080/api";

const panelData = ref(null);
const loading = ref(false);
const errorMsg = ref("");
const activeTab = ref("abilities"); // abilities | combat | spells

async function loadPanel() {
  if (!props.sessionId || !props.visible) return;
  loading.value = true;
  errorMsg.value = "";
  try {
    const token = localStorage.getItem("auth_token");
    const res = await fetch(`${API_BASE}/adventure/${props.sessionId}/character-panel`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    const json = await res.json();
    const data = json.data || json;
    if (data && !data.error) {
      panelData.value = data;
    } else {
      errorMsg.value = data?.error || "加载角色面板失败";
    }
  } catch (e) {
    errorMsg.value = "加载失败: " + e.message;
  } finally {
    loading.value = false;
  }
}

watch(() => props.visible, (v) => {
  if (v) loadPanel();
});

const abilityList = [
  { key: "str", label: "力量", modKey: "strMod" },
  { key: "dex", label: "敏捷", modKey: "dexMod" },
  { key: "con", label: "体质", modKey: "conMod" },
  { key: "int", label: "智力", modKey: "intMod" },
  { key: "wis", label: "感知", modKey: "wisMod" },
  { key: "cha", label: "魅力", modKey: "chaMod" },
];

function formatMod(val) {
  const v = val ?? 0;
  return v >= 0 ? `+${v}` : `${v}`;
}
</script>

<template>
  <transition name="panel-fade">
    <div v-if="visible" class="cp-overlay" @click.self="emit('close')">
      <div class="cp-modal">
        <!-- Ornate corner frames -->
        <div class="cp-corner cp-corner-tl"></div>
        <div class="cp-corner cp-corner-tr"></div>
        <div class="cp-corner cp-corner-bl"></div>
        <div class="cp-corner cp-corner-br"></div>

        <!-- ═══ Header ═══ -->
        <div class="cp-header">
          <div class="cp-header-glow"></div>
          <!-- Portrait frame -->
          <div class="cp-portrait-frame">
            <div class="cp-portrait-inner">
              <span class="cp-portrait-icon">{{ panelData?.class ? '🧙' : '👤' }}</span>
            </div>
            <div class="cp-portrait-ring"></div>
          </div>
          <!-- Name & info -->
          <div class="cp-title-area">
            <h2 class="cp-title text-glow-gold">{{ panelData?.name || '角色' }}</h2>
            <div class="cp-subtitle" v-if="panelData">
              <span class="cp-sub-race">{{ panelData.race }}</span>
              <span class="cp-sub-sep">·</span>
              <span class="cp-sub-class">{{ panelData.class }}</span>
              <span class="cp-sub-sep">·</span>
              <span class="cp-sub-level">Lv.{{ panelData.level }}</span>
            </div>
          </div>
          <button class="cp-close" @click="emit('close')">
            <span>×</span>
          </button>
        </div>

        <!-- ═══ Loading ═══ -->
        <div v-if="loading" class="cp-loading">
          <span class="cp-loading-orb"></span>
          <span>加载角色信息...</span>
        </div>

        <!-- ═══ Error ═══ -->
        <div v-else-if="errorMsg" class="cp-error">⚠️ {{ errorMsg }}</div>

        <!-- ═══ Content ═══ -->
        <div v-else-if="panelData" class="cp-body">
          <!-- Tab navigation -->
          <div class="cp-tabs">
            <button class="cp-tab" :class="{ active: activeTab === 'abilities' }" @click="activeTab = 'abilities'">
              <span class="cp-tab-icon">⚔</span>
              <span>属性</span>
            </button>
            <button class="cp-tab" :class="{ active: activeTab === 'combat' }" @click="activeTab = 'combat'">
              <span class="cp-tab-icon">🛡</span>
              <span>战斗</span>
            </button>
            <button class="cp-tab" :class="{ active: activeTab === 'spells' }" @click="activeTab = 'spells'">
              <span class="cp-tab-icon">✦</span>
              <span>法术</span>
            </button>
          </div>

          <!-- ═══ Abilities Tab ═══ -->
          <div v-if="activeTab === 'abilities'" class="cp-tab-content">
            <div class="ability-grid">
              <div v-for="a in abilityList" :key="a.key" class="ability-card">
                <div class="ability-card-glow"></div>
                <div class="ability-label">{{ a.label }}</div>
                <div class="ability-score-circle">
                  <div class="ability-score-ring"></div>
                  <span class="ability-score">{{ panelData.abilities?.[a.key] ?? 10 }}</span>
                </div>
                <div class="ability-mod">{{ formatMod(panelData.abilities?.[a.modKey]) }}</div>
              </div>
            </div>

            <!-- Equipment -->
            <div class="equip-section">
              <h3 class="equip-title">
                <span class="equip-title-deco">✦</span>
                <span>装备</span>
                <span class="equip-title-deco">✦</span>
              </h3>
              <div class="equip-row" v-if="panelData.weapon">
                <span class="equip-icon">⚔️</span>
                <span class="equip-label">武器</span>
                <span class="equip-val">{{ panelData.weapon.name }}</span>
                <span class="equip-sub">{{ panelData.weapon.damage }} {{ panelData.weapon.damageType }}</span>
              </div>
              <div class="equip-row" v-else>
                <span class="equip-icon">⚔️</span>
                <span class="equip-label">武器</span>
                <span class="equip-val muted">无</span>
              </div>
              <div class="equip-row" v-if="panelData.armor">
                <span class="equip-icon">🛡️</span>
                <span class="equip-label">护甲</span>
                <span class="equip-val">{{ panelData.armor.name }}</span>
                <span class="equip-sub">AC +{{ panelData.armor.acBonus }}</span>
              </div>
              <div class="equip-row" v-else>
                <span class="equip-icon">🛡️</span>
                <span class="equip-label">护甲</span>
                <span class="equip-val muted">无</span>
              </div>
            </div>
          </div>

          <!-- ═══ Combat Tab ═══ -->
          <div v-if="activeTab === 'combat'" class="cp-tab-content">
            <div class="combat-stat-grid">
              <div class="cs-card hp-card">
                <div class="cs-card-glow"></div>
                <div class="cs-icon">❤️</div>
                <div class="cs-label">生命值</div>
                <div class="cs-val">{{ panelData.combatStats?.hp }}<span class="cs-val-sep">/</span>{{ panelData.combatStats?.maxHp }}</div>
              </div>
              <div class="cs-card ac-card">
                <div class="cs-card-glow"></div>
                <div class="cs-icon">🛡️</div>
                <div class="cs-label">护甲等级</div>
                <div class="cs-val">{{ panelData.combatStats?.ac }}</div>
              </div>
              <div class="cs-card xp-card">
                <div class="cs-card-glow"></div>
                <div class="cs-icon">✦</div>
                <div class="cs-label">经验值</div>
                <div class="cs-val">{{ panelData.combatStats?.xp }}<span class="cs-val-sep">/</span>{{ panelData.combatStats?.xpToNext }}</div>
              </div>
              <div class="cs-card speed-card">
                <div class="cs-card-glow"></div>
                <div class="cs-icon">🏃</div>
                <div class="cs-label">速度</div>
                <div class="cs-val">{{ panelData.combatStats?.speed || 30 }}<span class="cs-unit">ft</span></div>
              </div>
              <div class="cs-card dice-card">
                <div class="cs-card-glow"></div>
                <div class="cs-icon">🎲</div>
                <div class="cs-label">生命骰</div>
                <div class="cs-val">d{{ panelData.combatStats?.hitDie }}<span class="cs-unit">×{{ panelData.combatStats?.hitDice }}/{{ panelData.combatStats?.maxHitDice }}</span></div>
              </div>
            </div>

            <!-- Combat status -->
            <div class="combat-status" v-if="panelData.combatInfo">
              <h3 class="equip-title">
                <span class="equip-title-deco">✦</span>
                <span>战斗状态</span>
                <span class="equip-title-deco">✦</span>
              </h3>
              <div class="status-row">
                <span class="status-label">当前回合</span>
                <span class="status-val">Round {{ panelData.combatInfo.round }}</span>
              </div>
              <div class="status-row">
                <span class="status-label">行动点</span>
                <span class="status-val">{{ panelData.combatInfo.actionPoints }}/{{ panelData.combatInfo.maxActionPoints }}</span>
              </div>
            </div>
          </div>

          <!-- ═══ Spells Tab ═══ -->
          <div v-if="activeTab === 'spells'" class="cp-tab-content">
            <div v-if="panelData.spells && panelData.spells.length" class="spell-list">
              <div v-for="(spell, i) in panelData.spells" :key="i" class="spell-item">
                <span class="spell-bullet">✦</span>
                <span class="spell-text">{{ spell }}</span>
                <div class="spell-glow"></div>
              </div>
            </div>
            <div v-else class="empty-text">
              <span class="empty-icon">📜</span>
              <span>暂无法术</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
/* ═══ Overlay ═══ */
.cp-overlay {
  position: fixed;
  inset: 0;
  z-index: 280;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: radial-gradient(ellipse at center, rgba(20, 15, 10, 0.75), rgba(0, 0, 0, 0.88));
  backdrop-filter: blur(6px);
}
.panel-fade-enter-active, .panel-fade-leave-active { transition: opacity 0.3s ease; }
.panel-fade-enter-from, .panel-fade-leave-to { opacity: 0; }

/* ═══ Modal ═══ */
.cp-modal {
  position: relative;
  background: linear-gradient(135deg, #1a1520, #0d0b14);
  border: 1px solid var(--gold, #c9a227);
  border-radius: 14px;
  padding: 0;
  max-width: 520px;
  width: 100%;
  max-height: 82vh;
  overflow-y: auto;
  box-shadow:
    0 0 40px rgba(201, 162, 39, 0.12),
    0 0 80px rgba(201, 162, 39, 0.05),
    0 16px 48px rgba(0, 0, 0, 0.6),
    inset 0 0 60px rgba(201, 162, 39, 0.02);
  animation: cpRise 0.4s cubic-bezier(.34, 1.56, .64, 1);
}
@keyframes cpRise {
  from { opacity: 0; transform: translateY(30px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* Ornate corner frames */
.cp-corner {
  position: absolute;
  width: 18px;
  height: 18px;
  z-index: 3;
  pointer-events: none;
}
.cp-corner-tl { top: 6px; left: 6px; border-top: 2px solid var(--gold, #c9a227); border-left: 2px solid var(--gold, #c9a227); border-top-left-radius: 6px; }
.cp-corner-tr { top: 6px; right: 6px; border-top: 2px solid var(--gold, #c9a227); border-right: 2px solid var(--gold, #c9a227); border-top-right-radius: 6px; }
.cp-corner-bl { bottom: 6px; left: 6px; border-bottom: 2px solid var(--gold, #c9a227); border-left: 2px solid var(--gold, #c9a227); border-bottom-left-radius: 6px; }
.cp-corner-br { bottom: 6px; right: 6px; border-bottom: 2px solid var(--gold, #c9a227); border-right: 2px solid var(--gold, #c9a227); border-bottom-right-radius: 6px; }

/* Custom scrollbar */
.cp-modal::-webkit-scrollbar { width: 8px; }
.cp-modal::-webkit-scrollbar-track { background: rgba(0, 0, 0, 0.3); }
.cp-modal::-webkit-scrollbar-thumb {
  background: linear-gradient(var(--gold-dim, #8a7020), var(--gold, #c9a227));
  border-radius: 4px;
}

/* ═══ Header ═══ */
.cp-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 24px 18px;
  border-bottom: 1px solid rgba(201, 162, 39, 0.2);
  background: linear-gradient(180deg, rgba(201, 162, 39, 0.06), transparent);
  position: relative;
  overflow: hidden;
}
.cp-header-glow {
  position: absolute;
  top: -40px;
  left: 30%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(201, 162, 39, 0.06), transparent 70%);
  pointer-events: none;
}

/* Portrait frame */
.cp-portrait-frame {
  position: relative;
  width: 64px;
  height: 64px;
  flex-shrink: 0;
}
.cp-portrait-inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: radial-gradient(circle, #2a1f2e, #1a1018);
  border: 2px solid var(--gold, #c9a227);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 0 16px rgba(201, 162, 39, 0.15),
    inset 0 2px 8px rgba(0, 0, 0, 0.5);
}
.cp-portrait-icon {
  font-size: 32px;
  filter: drop-shadow(0 0 8px rgba(201, 162, 39, 0.2));
}
.cp-portrait-ring {
  position: absolute;
  inset: -5px;
  border: 1px dashed rgba(201, 162, 39, 0.2);
  border-radius: 50%;
  animation: cpRingRotate 20s linear infinite;
  pointer-events: none;
}
@keyframes cpRingRotate { to { transform: rotate(360deg); } }

.cp-title-area { flex: 1; position: relative; z-index: 1; }
.cp-title {
  font-family: var(--font-display, "Cinzel Decorative", serif);
  font-size: 22px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(180deg, var(--gold-bright, #e8c44a), var(--gold, #c9a227));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.05em;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.5));
}
.cp-subtitle {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 13px;
}
.cp-sub-race { color: var(--ink, #d4c8b8); }
.cp-sub-class { color: var(--gold, #c9a227); font-weight: 600; }
.cp-sub-level { color: var(--gold-bright, #e8c44a); font-weight: 700; }
.cp-sub-sep { color: var(--muted, #6b5d4a); }

.cp-close {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(201, 162, 39, 0.2);
  color: var(--ink-soft, #a89880);
  width: 32px;
  height: 32px;
  border-radius: 6px;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}
.cp-close:hover {
  border-color: var(--crimson-bright, #9b2d2d);
  color: var(--crimson-bright, #9b2d2d);
  background: rgba(107, 29, 29, 0.15);
  box-shadow: 0 0 10px rgba(155, 45, 45, 0.15);
}

/* ═══ Loading / Error ═══ */
.cp-loading, .cp-error {
  padding: 50px 20px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
}
.cp-loading { color: var(--muted, #6b5d4a); font-size: 14px; }
.cp-loading-orb {
  width: 40px;
  height: 40px;
  border: 2px solid rgba(201, 162, 39, 0.2);
  border-top-color: var(--gold, #c9a227);
  border-radius: 50%;
  animation: cpSpin 1s linear infinite;
}
@keyframes cpSpin { to { transform: rotate(360deg); } }
.cp-error { color: var(--crimson-bright, #9b2d2d); }

/* ═══ Body ═══ */
.cp-body { padding: 18px 24px 24px; }

/* ═══ Tabs ═══ */
.cp-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 18px;
  background: rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(61, 47, 32, 0.5);
  border-radius: 8px;
  padding: 4px;
}
.cp-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 8px;
  border: none;
  background: none;
  color: var(--muted, #6b5d4a);
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.25s ease;
  letter-spacing: 0.03em;
}
.cp-tab-icon { font-size: 14px; }
.cp-tab.active {
  background: linear-gradient(135deg, rgba(201, 162, 39, 0.2), rgba(201, 162, 39, 0.08));
  color: var(--gold-bright, #e8c44a);
  box-shadow: 0 0 12px rgba(201, 162, 39, 0.1), inset 0 1px 0 rgba(201, 162, 39, 0.1);
  border: 1px solid rgba(201, 162, 39, 0.25);
}
.cp-tab:hover:not(.active) { color: var(--gold, #c9a227); }

/* ═══ Ability Grid ═══ */
.ability-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 22px;
}
.ability-card {
  text-align: center;
  padding: 16px 8px 12px;
  background: linear-gradient(135deg, rgba(30, 22, 18, 0.8), rgba(17, 13, 10, 0.6));
  border: 1px solid rgba(201, 162, 39, 0.12);
  border-radius: 8px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.ability-card-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, rgba(201, 162, 39, 0.04), transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}
.ability-card:hover {
  border-color: rgba(201, 162, 39, 0.35);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}
.ability-card:hover .ability-card-glow { opacity: 1; }
.ability-label {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 11px;
  color: var(--muted, #6b5d4a);
  font-weight: 600;
  margin-bottom: 8px;
  letter-spacing: 0.05em;
  position: relative;
  z-index: 1;
}
.ability-score-circle {
  position: relative;
  width: 48px;
  height: 48px;
  margin: 0 auto 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ability-score-ring {
  position: absolute;
  inset: 0;
  border: 2px solid rgba(201, 162, 39, 0.2);
  border-radius: 50%;
  background: radial-gradient(circle, rgba(201, 162, 39, 0.05), transparent);
  transition: border-color 0.3s ease;
}
.ability-card:hover .ability-score-ring { border-color: rgba(201, 162, 39, 0.5); }
.ability-score {
  font-family: var(--font-display, "Cinzel Decorative", serif);
  font-size: 22px;
  font-weight: 800;
  color: var(--gold, #c9a227);
  line-height: 1;
  position: relative;
  z-index: 1;
  text-shadow: 0 0 12px rgba(201, 162, 39, 0.2);
}
.ability-mod {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 13px;
  color: var(--ink, #d4c8b8);
  font-weight: 600;
  position: relative;
  z-index: 1;
}

/* ═══ Equipment ═══ */
.equip-section { margin-top: 8px; }
.equip-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 14px;
  font-weight: 700;
  color: var(--gold, #c9a227);
  margin: 0 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(201, 162, 39, 0.15);
  letter-spacing: 0.05em;
}
.equip-title-deco { color: var(--gold-dim, #8a7020); font-size: 11px; }
.equip-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 4px;
  border-bottom: 1px solid rgba(61, 47, 32, 0.3);
  transition: all 0.2s ease;
}
.equip-row:hover { background: rgba(201, 162, 39, 0.03); }
.equip-icon { font-size: 16px; width: 20px; text-align: center; }
.equip-label {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 12px;
  color: var(--muted, #6b5d4a);
  width: 36px;
  flex-shrink: 0;
  font-weight: 600;
}
.equip-val {
  font-family: var(--font-body, serif);
  font-size: 14px;
  color: var(--ink-bright, #f0e6d4);
  font-weight: 600;
  flex: 1;
}
.equip-sub {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 11px;
  color: var(--gold-dim, #8a7020);
  margin-left: auto;
  font-weight: 600;
}
.muted { color: var(--muted, #6b5d4a) !important; }

/* ═══ Combat Stats Grid ═══ */
.combat-stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}
.cs-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 16px 12px;
  background: linear-gradient(135deg, rgba(30, 22, 18, 0.8), rgba(17, 13, 10, 0.6));
  border: 1px solid rgba(61, 47, 32, 0.5);
  border-radius: 8px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.cs-card-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center top, rgba(201, 162, 39, 0.04), transparent 70%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}
.cs-card:hover { border-color: rgba(201, 162, 39, 0.3); transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3); }
.cs-card:hover .cs-card-glow { opacity: 1; }
.hp-card { border-color: rgba(155, 45, 45, 0.25); }
.hp-card:hover { border-color: rgba(155, 45, 45, 0.5); }
.ac-card { border-color: rgba(74, 122, 154, 0.2); }
.ac-card:hover { border-color: rgba(74, 122, 154, 0.45); }
.xp-card { border-color: rgba(125, 90, 170, 0.2); }
.xp-card:hover { border-color: rgba(125, 90, 170, 0.45); }
.cs-icon { font-size: 22px; position: relative; z-index: 1; }
.cs-label {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 11px;
  color: var(--muted, #6b5d4a);
  font-weight: 600;
  letter-spacing: 0.03em;
  position: relative;
  z-index: 1;
}
.cs-val {
  font-family: var(--font-display, "Cinzel Decorative", serif);
  font-size: 18px;
  font-weight: 800;
  color: var(--ink-bright, #f0e6d4);
  position: relative;
  z-index: 1;
}
.hp-card .cs-val { color: #e07070; }
.ac-card .cs-val { color: #6ab0e0; }
.xp-card .cs-val { color: #b090e0; }
.cs-val-sep { color: var(--muted, #6b5d4a); margin: 0 2px; font-weight: 400; }
.cs-unit { font-size: 12px; color: var(--muted, #6b5d4a); font-weight: 600; margin-left: 4px; }

/* ═══ Combat Status ═══ */
.combat-status { margin-top: 8px; }
.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 4px;
  border-bottom: 1px solid rgba(61, 47, 32, 0.3);
}
.status-label {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 13px;
  color: var(--muted, #6b5d4a);
  font-weight: 600;
}
.status-val {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 14px;
  color: var(--gold, #c9a227);
  font-weight: 700;
}

/* ═══ Spell List ═══ */
.spell-list { display: flex; flex-direction: column; gap: 8px; }
.spell-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(93, 58, 138, 0.08), rgba(30, 22, 18, 0.6));
  border-left: 3px solid var(--arcane, #5d3a8a);
  border-radius: 0 8px 8px 0;
  position: relative;
  overflow: hidden;
  transition: all 0.25s ease;
}
.spell-item:hover {
  border-left-color: var(--arcane-glow, #7d5aaa);
  background: linear-gradient(135deg, rgba(93, 58, 138, 0.12), rgba(30, 22, 18, 0.7));
  box-shadow: 0 0 12px rgba(125, 90, 170, 0.08);
}
.spell-glow {
  position: absolute;
  right: -20px;
  top: -20px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(125, 90, 170, 0.06), transparent 70%);
  pointer-events: none;
}
.spell-bullet {
  color: var(--arcane-glow, #7d5aaa);
  font-size: 14px;
  filter: drop-shadow(0 0 4px rgba(125, 90, 170, 0.3));
  z-index: 1;
}
.spell-text {
  font-family: var(--font-body, serif);
  font-size: 13px;
  color: var(--ink, #d4c8b8);
  line-height: 1.6;
  z-index: 1;
}

/* ═══ Empty State ═══ */
.empty-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
  color: var(--muted, #6b5d4a);
  padding: 40px 0;
  font-size: 14px;
  font-style: italic;
}
.empty-icon { font-size: 36px; opacity: 0.3; filter: grayscale(50%); }

/* ═══ Responsive ═══ */
@media (max-width: 480px) {
  .ability-grid { grid-template-columns: repeat(2, 1fr); }
  .combat-stat-grid { grid-template-columns: 1fr; }
  .cp-header { padding: 18px 16px 14px; gap: 12px; }
  .cp-portrait-frame { width: 52px; height: 52px; }
  .cp-portrait-icon { font-size: 26px; }
  .cp-title { font-size: 18px; }
  .cp-body { padding: 14px 16px 20px; }
}
</style>
