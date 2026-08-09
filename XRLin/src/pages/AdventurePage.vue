<script setup>
import { ref, nextTick, onMounted, computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useApi } from "../composables/useApi";
import { getClassImage } from "../composables/useClassImages";
import monsterImages from "../composables/useMonsterImages";
import { getMapByContext } from "../composables/useMapImages";
import InventoryPanel from "../components/InventoryPanel.vue";
import BattleInventoryButton from "../components/BattleInventoryButton.vue";
import EncounterDialog from "../components/EncounterDialog.vue";
import CharacterPanel from "../components/CharacterPanel.vue";
import ActionBar from "../components/ActionBar.vue";
import TurnOrder from "../components/TurnOrder.vue";
import LevelUpPanel from "../components/LevelUpPanel.vue";
import NarrationPanel from "../components/NarrationPanel.vue";

const route = useRoute();
const router = useRouter();
const { fetchCharacter, fetchEntry } = useApi();

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080/api";

// ── 状态 ──
const characterId = ref(route.query.characterId || "");
const character = ref(null);
const raceName = ref("");
const className = ref("");
const sessionId = ref("");

const storyLog = ref([]);
const streamingText = ref("");
const choices = ref([]);
const playerInput = ref("");
const loading = ref(false);
const errorMsg = ref("");
const storyArea = ref(null);
const narrationPanelRef = ref(null);

// 战斗/状态
const phase = ref("EXPLORE");
const stats = ref({ hp: 0, maxHp: 0, ac: 0, level: 1, xp: 0, xpToNext: 120, spellSlots: {}, maxSpellSlots: {}, abilities: null, hitDie: 0, hitDice: 0, maxHitDice: 0 });
const combat = ref(null);
const levelUpData = ref(null);
const inventory = ref([]);
const showInventory = ref(false);
const showCombatInventory = ref(false);
const showCharacterPanel = ref(false);

// 遭遇信息
const encounterInfo = ref(null);
const encounterLoading = ref(false);

// 法术选择弹窗
const spellList = ref([]);
const showSpellList = ref(false);

// 当前选中的敌人目标（点击敌人图框进行选择）
const selectedTarget = ref(null);

// 战斗日志（独立于故事日志）
const combatLog = ref([]);

// 战斗胜利结算
const victoryData = ref(null);
const pendingState = ref(null);

// ── 初始化 ──
onMounted(async () => {
  if (!characterId.value) {
    router.replace("/characters");
    return;
  }
  try {
    character.value = await fetchCharacter(characterId.value);
    if (character.value?.raceId) {
      const r = await fetchEntry("race", character.value.raceId);
      raceName.value = r?.name || "?";
    }
    if (character.value?.classId) {
      const c = await fetchEntry("class", character.value.classId);
      className.value = c?.name || "?";
    }
    startAdventure();
  } catch (e) {
    errorMsg.value = "加载角色失败: " + e.message;
  }
});

// ── SSE 流式请求 ──
async function streamSSE(url, body) {
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(`请求失败: ${response.status}`);
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  function processBuffer() {
    const events = buffer.split("\n\n");
    buffer = events.pop();
    for (const eventStr of events) {
      parseSSEEvent(eventStr);
    }
  }

  function parseSSEEvent(eventStr) {
    const lines = eventStr.split("\n");
    let eventType = "";
    let dataStr = "";
    for (const line of lines) {
      if (line.startsWith("event:")) eventType = line.slice(6).trim();
      else if (line.startsWith("data:")) {
        let val = line.slice(5);
        if (val.startsWith(" ")) val = val.slice(1);
        dataStr += (dataStr ? "\n" : "") + val;
      }
    }
    if (eventType) handleSSEEvent(eventType, dataStr);
  }

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    processBuffer();
  }
  // 刷新解码器并处理 buffer 中可能残留的最后一个事件
  buffer += decoder.decode();
  if (buffer.trim()) {
    parseSSEEvent(buffer);
  }
}

function handleSSEEvent(type, data) {
  if (type === "session") {
    sessionId.value = data.trim();
  } else if (type === "token") {
    streamingText.value += data;
    scrollToBottom();
  } else if (type === "done") {
    try {
      const result = JSON.parse(data);
      const cleanNarrative = (result.narrative || streamingText.value).trim();
      if (cleanNarrative) {
        storyLog.value.push(cleanNarrative);
        // 战斗模式下也推入战斗日志
        if (phase.value === "COMBAT" || result.phase === "COMBAT") {
          combatLog.value.push(cleanNarrative);
        }
      }
      streamingText.value = "";
      choices.value = result.choices || [];

      // 更新状态
      const wasCombat = phase.value === "COMBAT";
      const wasEncounter = phase.value === "ENCOUNTER";

      // 如果有战斗结算结果，延迟应用状态（等玩家点确定后再切回探索）
      if (result.combatResult) {
        victoryData.value = result.combatResult;
        // 暂存状态，等玩家确认后再应用
        pendingState.value = {
          phase: result.phase,
          stats: result.stats,
          combat: result.combat,
          choices: result.choices,
          levelUp: result.levelUp,
          inventory: result.inventory,
          encounterInfo: result.encounterInfo,
        };
      } else {
        if (result.phase) phase.value = result.phase;
        if (result.stats) stats.value = result.stats;
        if (result.combat !== undefined) combat.value = result.combat;
        if (result.levelUp) levelUpData.value = result.levelUp;
        if (result.inventory) inventory.value = result.inventory;
        if (result.spells) spellList.value = result.spells;
        if (result.encounterInfo !== undefined) encounterInfo.value = result.encounterInfo;

        // 从遭遇切到战斗时，清除遭遇信息
        if (wasEncounter && result.phase === "COMBAT") {
          encounterInfo.value = null;
        }

        // 从战斗切到非战斗时，清空战斗日志
        if (wasCombat && result.phase !== "COMBAT") {
          combatLog.value = [];
        }
      }
    } catch {
      storyLog.value.push(streamingText.value.trim());
      streamingText.value = "";
      choices.value = [];
    }
    loading.value = false;
    scrollToBottom();
  } else if (type === "error") {
    errorMsg.value = data;
    loading.value = false;
  }
}

// ── 冒险操作 ──
async function startAdventure() {
  loading.value = true;
  errorMsg.value = "";
  streamingText.value = "";
  try {
    await streamSSE(`${API_BASE}/adventure/start`, { characterId: characterId.value });
  } catch (e) {
    errorMsg.value = "冒险启动失败: " + e.message;
    loading.value = false;
  }
}

async function performAction(action) {
  console.log('[Adventure] performAction called with:', action);
  if (!sessionId.value) {
    console.warn('[Adventure] sessionId is empty!');
    errorMsg.value = "会话未建立，请刷新页面重试";
    return;
  }
  if (loading.value) {
    console.warn('[Adventure] loading is true, skipping');
    return;
  }
  if (!action.trim()) {
    console.warn('[Adventure] action is empty');
    return;
  }
  if (phase.value === "DEAD" || phase.value === "LEVELUP") {
    console.warn('[Adventure] phase is', phase.value, 'skipping');
    return;
  }

  // ENCOUNTER 阶段：通过 confirmEncounter API 确认进入战斗
  if (phase.value === "ENCOUNTER") {
    await confirmEncounter();
    return;
  }

  // 战斗模式下记录到战斗日志
  if (phase.value === "COMBAT") {
    combatLog.value.push(`【你】${action}`);
  } else {
    storyLog.value.push(`【你】${action}`);
  }
  choices.value = [];
  playerInput.value = "";
  loading.value = true;
  errorMsg.value = "";

  try {
    await streamSSE(`${API_BASE}/adventure/${sessionId.value}/act`, { action });
  } catch (e) {
    errorMsg.value = "行动失败: " + e.message;
    loading.value = false;
  }
}

function selectChoice(choice) {
  console.log('[Adventure] selectChoice clicked:', choice);
  console.log('[Adventure] sessionId:', sessionId.value, 'loading:', loading.value, 'phase:', phase.value);
  const action = choice.replace(/^\d+\.\s*/, "").trim();
  performAction(action);
}

function submitInput() {
  if (playerInput.value.trim()) performAction(playerInput.value.trim());
}

// ── 长休 ──
async function longRest() {
  if (!sessionId.value || loading.value) return;
  loading.value = true;
  try {
    const res = await fetch(`${API_BASE}/adventure/${sessionId.value}/rest`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
    });
    const json = await res.json();
    const data = json.data || json;
    if (data.stats) stats.value = data.stats;
    if (data.phase) phase.value = data.phase;
    if (data.narrative) storyLog.value.push(`【系统】${data.narrative}`);
    else storyLog.value.push("【系统】长休完毕，体力和法术位已完全恢复。");
  } catch (e) {
    errorMsg.value = "长休失败: " + e.message;
  }
  loading.value = false;
}

// ── 短休 ──
async function shortRest() {
  if (!sessionId.value || loading.value) return;
  loading.value = true;
  try {
    const res = await fetch(`${API_BASE}/adventure/${sessionId.value}/short-rest`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
    });
    const json = await res.json();
    const data = json.data || json;
    if (data.stats) stats.value = data.stats;
    if (data.phase) phase.value = data.phase;
    if (data.narrative) storyLog.value.push(`【系统】${data.narrative}`);
    else storyLog.value.push("【系统】短休完毕，消耗生命骰恢复了部分HP。");
  } catch (e) {
    errorMsg.value = "短休失败: " + e.message;
  }
  loading.value = false;
}

// ── 使用背包物品 ──
async function useItem(itemId) {
  if (!sessionId.value || loading.value) return;
  loading.value = true;
  try {
    const res = await fetch(`${API_BASE}/adventure/${sessionId.value}/use-item`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ itemId }),
    });
    const json = await res.json();
    const data = json.data || json;
    if (data.stats) stats.value = data.stats;
    if (data.inventory) inventory.value = data.inventory;
    if (data.narrative) {
      if (phase.value === "COMBAT") combatLog.value.push(`【系统】${data.narrative}`);
      else storyLog.value.push(`【系统】${data.narrative}`);
    }
  } catch (e) {
    errorMsg.value = "使用物品失败: " + e.message;
  }
  loading.value = false;
}

// ── 升级 ──
function onLevelUpConfirmed(data) {
  if (data.narrative) storyLog.value.push(data.narrative);
  if (data.choices) choices.value = data.choices;
  if (data.stats) stats.value = data.stats;
  if (data.phase) phase.value = data.phase;
  if (data.spells) spellList.value = data.spells;
  if (data.inventory) inventory.value = data.inventory;
  levelUpData.value = null;
}

// ── 工具 ──
function scrollToBottom() {
  nextTick(() => {
    if (narrationPanelRef.value) narrationPanelRef.value.scrollToBottom();
    const combatLogEl = document.querySelector(".combat-log-area");
    if (combatLogEl) combatLogEl.scrollTop = combatLogEl.scrollHeight;
  });
}

const charDisplay = computed(() => {
  if (!character.value) return "";
  return `${raceName.value} ${className.value}`;
});

// 角色立绘图片（优先使用后端返回的 MinIO URL，回退到本地静态图片）
const charImage = computed(() => {
  if (!character.value) return null;
  const gender = character.value.gender || "male";
  // 优先使用后端返回的 MinIO URL
  if (gender === "female" && character.value.femaleImageUrl) {
    return character.value.femaleImageUrl;
  }
  if (gender === "male" && character.value.maleImageUrl) {
    return character.value.maleImageUrl;
  }
  // 回退到本地静态图片
  if (!character.value.classId) return null;
  return getClassImage(character.value.classId, gender);
});

// 敌人立绘图片（优先使用后端返回的 MinIO URL，回退到本地静态图片）
function getEnemyImage(enemy) {
  // 优先使用后端返回的 imageUrl（来自 MinIO）
  if (enemy?.imageUrl) return enemy.imageUrl;
  // 回退到本地静态图片映射（通过 monsterId 匹配）
  if (enemy?.monsterId) return monsterImages[enemy.monsterId] || null;
  return null;
}

// 当前地图（根据故事文本关键词匹配）
const currentMap = computed(() => {
  const latestStory = storyLog.value.length > 0
    ? storyLog.value[storyLog.value.length - 1]
    : (streamingText.value || "");
  return getMapByContext(latestStory);
});

// 战斗地图（根据战斗日志或敌人类型匹配）
const combatMap = computed(() => {
  const logText = combatLog.value.join(" ") || "";
  return getMapByContext(logText);
});

const hpPercent = computed(() => {
  return stats.value.maxHp > 0 ? (stats.value.hp / stats.value.maxHp) * 100 : 0;
});

const xpPercent = computed(() => {
  return stats.value.xpToNext > 0 ? (stats.value.xp / stats.value.xpToNext) * 100 : 0;
});

const hpColor = computed(() => {
  if (hpPercent.value > 60) return "linear-gradient(90deg, #2e7d32, #4caf50, #66bb6a)";
  if (hpPercent.value > 30) return "linear-gradient(90deg, #e65100, #ff9800, #ffa726)";
  return "linear-gradient(90deg, #b71c1c, #f44336, #ef5350)";
});

const spellSlotList = computed(() => {
  const max = stats.value.maxSpellSlots || {};
  const cur = stats.value.spellSlots || {};
  const list = [];
  for (let lv = 1; lv <= 9; lv++) {
    if (max[lv] && max[lv] > 0) {
      list.push({ level: lv, current: cur[lv] || 0, max: max[lv] });
    }
  }
  return list;
});

const isDead = computed(() => phase.value === "DEAD");
const isLevelUp = computed(() => phase.value === "LEVELUP" && levelUpData.value);
const isEncounter = computed(() => phase.value === "ENCOUNTER" && encounterInfo.value);
const isCombat = computed(() => phase.value === "COMBAT" && combat.value);
const showVictory = computed(() => !!victoryData.value);
const aliveEnemies = computed(() => combat.value?.enemies?.filter(e => e.alive) || []);
const hasSpells = computed(() => (spellList.value.length > 0) || (stats.value.spellSlots && Object.keys(stats.value.spellSlots).length > 0) || (spellSlotList.value.length > 0));

// 六维属性展示
const abilityList = computed(() => {
  const a = stats.value.abilities;
  if (!a) return [];
  return [
    { key: "STR", label: "力量", val: a.str, mod: a.strMod },
    { key: "DEX", label: "敏捷", val: a.dex, mod: a.dexMod },
    { key: "CON", label: "体质", val: a.con, mod: a.conMod },
    { key: "INT", label: "智力", val: a.int, mod: a.intMod },
    { key: "WIS", label: "感知", val: a.wis, mod: a.wisMod },
    { key: "CHA", label: "魅力", val: a.cha, mod: a.chaMod },
  ];
});

// 可使用的物品（消耗品）
const consumables = computed(() => {
  return (inventory.value || []).filter(i => i.itemType === "consumable" && i.quantity > 0);
});

// 战斗中行动点
const combatActionPoints = computed(() => {
  return combat.value?.actionPoints ?? 1;
});

// 背包物品总数（供战斗按钮显示）
const inventoryCount = computed(() => {
  return (inventory.value || []).reduce((sum, i) => sum + (i.quantity || 1), 0);
});

// ── 背包物品使用回调（来自 InventoryPanel）──
function onItemUsedFromPanel(payload) {
  const { item, result } = payload;
  // 更新角色状态
  if (result && result.stats) {
    stats.value = result.stats;
  }
  if (result && result.combat) {
    combat.value = result.combat;
  } else if (result && result.combat === null) {
    combat.value = null;
  }
  // 更新背包列表
  if (result && result.inventory) {
    inventory.value = result.inventory;
  }
  // 添加战斗/故事日志
  if (result && result.narrative) {
    if (phase.value === "COMBAT") {
      combatLog.value.push(`【系统】${result.narrative}`);
    } else {
      storyLog.value.push(`【系统】${result.narrative}`);
    }
  }
  // 更新阶段（可能从战斗变为探索等）
  if (result && result.phase) {
    phase.value = result.phase;
  }
  scrollToBottom();
}

// ── 战斗中打开背包 ──
function openCombatInventory() {
  showCombatInventory.value = true;
}

function closeCombatInventory() {
  showCombatInventory.value = false;
}

// ── 遭遇确认：进入战斗 ──
async function confirmEncounter() {
  if (!sessionId.value || encounterLoading.value) return;
  encounterLoading.value = true;
  try {
    const token = localStorage.getItem("auth_token");
    const res = await fetch(`${API_BASE}/adventure/${sessionId.value}/encounter/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    });
    const json = await res.json();
    const data = json.data || json;
    if (data.phase) phase.value = data.phase;
    if (data.stats) stats.value = data.stats;
    if (data.combat !== undefined) combat.value = data.combat;
    if (data.encounterInfo !== undefined) encounterInfo.value = data.encounterInfo;
    encounterInfo.value = null; // 清除遭遇弹窗
    combatLog.value = [];
    if (data.narrative) combatLog.value.push(data.narrative);
  } catch (e) {
    errorMsg.value = "进入战斗失败: " + e.message;
  } finally {
    encounterLoading.value = false;
  }
}

// ── 打开角色面板 ──
function openCharacterPanel() {
  showCharacterPanel.value = true;
}

// ── 结束回合 ──
async function endTurn() {
  if (!sessionId.value || loading.value) return;
  if (combat.value?.combatPhase !== "PLAYER_TURN") return;
  // 清空选中的目标
  selectedTarget.value = null;
  loading.value = true;
  try {
    const token = localStorage.getItem("auth_token");
    const res = await fetch(`${API_BASE}/adventure/${sessionId.value}/end-turn`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    });
    const json = await res.json();
    const data = json.data || json;
    if (data.phase) phase.value = data.phase;
    if (data.stats) stats.value = data.stats;
    if (data.combat !== undefined) combat.value = data.combat;
    // 逐条显示敌人攻击结果
    if (Array.isArray(data.enemyAttackEntries) && data.enemyAttackEntries.length > 0) {
      for (const entry of data.enemyAttackEntries) {
        combatLog.value.push(`【敌方回合】${entry}`);
      }
    } else if (data.narrative) {
      combatLog.value.push(`【敌方回合】${data.narrative}`);
    }
    // 如果有战斗结算结果（敌人在攻击中可能触发玩家死亡等），显示胜利/失败页面
    if (data.combatResult) {
      victoryData.value = data.combatResult;
      pendingState.value = {
        phase: data.phase,
        stats: data.stats,
        combat: data.combat,
        choices: data.choices,
        levelUp: data.levelUp,
        inventory: data.inventory,
        encounterInfo: data.encounterInfo,
      };
    }
    // 如果角色死亡
    if (data.phase === "DEAD") {
      combatLog.value = [];
    }
  } catch (e) {
    errorMsg.value = "结束回合失败: " + e.message;
  } finally {
    loading.value = false;
  }
}

// ── 战斗行动快捷按钮 ──
function quickAttack() {
  const target = resolveTarget();
  if (target) {
    performAction(`近战攻击 ${target}`);
  } else {
    errorMsg.value = "请先点击右侧敌人选择目标";
  }
}

function quickRangedAttack() {
  const target = resolveTarget();
  if (target) {
    performAction(`远程射击 ${target}`);
  } else {
    errorMsg.value = "请先点击右侧敌人选择目标";
  }
}

function quickMove() {
  // 移动功能已移除
}

function quickCastSpell() {
  if (spellList.value.length === 0) {
    performAction("施放法术");
    return;
  }
  showSpellList.value = true;
}

function selectSpell(spellEntry) {
  showSpellList.value = false;
  // spellEntry 格式: "法术名: 简述"
  const spellName = spellEntry.split(":")[0].trim().replace("【专长】", "");
  const target = resolveTarget();
  if (target) {
    performAction(`施放法术 ${spellName} 攻击 ${target}`);
  } else {
    errorMsg.value = "请先点击右侧敌人选择目标";
  }
}

// ── 目标选择 ──
function resolveTarget() {
  // 优先使用玩家选中的目标
  if (selectedTarget.value) {
    const alive = aliveEnemies.value.find(e => e.name === selectedTarget.value);
    if (alive) return alive.name;
  }
  // 否则选择第一个存活敌人
  if (aliveEnemies.value.length === 1) return aliveEnemies.value[0].name;
  // 多个存活敌人且未选择：不自动选择，提示玩家
  return null;
}

function selectEnemy(enemy) {
  if (!enemy.alive) return;
  selectedTarget.value = enemy.name;
}

// 战斗状态变化时清空选中目标
watch(() => combat.value?.round, () => {
  selectedTarget.value = null;
});

// ── 战斗胜利确认 ──
function dismissVictory() {
  if (pendingState.value) {
    if (pendingState.value.phase) phase.value = pendingState.value.phase;
    if (pendingState.value.stats) stats.value = pendingState.value.stats;
    if (pendingState.value.combat !== undefined) combat.value = pendingState.value.combat;
    if (pendingState.value.choices) choices.value = pendingState.value.choices;
    if (pendingState.value.levelUp) levelUpData.value = pendingState.value.levelUp;
    if (pendingState.value.inventory) inventory.value = pendingState.value.inventory;
    if (pendingState.value.encounterInfo !== undefined) encounterInfo.value = pendingState.value.encounterInfo;
  }
  victoryData.value = null;
  pendingState.value = null;
  combatLog.value = [];
  scrollToBottom();
}
</script>


<template>
  <div class="adventure-page" :class="{ 'in-combat': isCombat }">
    <!-- ════════════════════════════════════════════
         EXPLORE MODE — Fullscreen map with overlay panels
         ════════════════════════════════════════════ -->
    <template v-if="!isCombat && !isEncounter && !showVictory && !isDead && !isLevelUp">
      <!-- Fullscreen map background -->
      <div class="explore-map-fullscreen" v-if="stats.maxHp > 0">
        <img :src="currentMap" class="explore-map-img" alt="冒险场景" />
        <div class="explore-map-overlay"></div>
        <div class="explore-map-vignette"></div>
        <!-- Ember particles -->
        <div class="map-embers">
          <span v-for="n in 12" :key="n" class="map-ember" :style="{ '--n': n }"></span>
        </div>
        <!-- Character portrait (large, right side) -->
        <div class="explore-char-portrait" v-if="charImage">
          <img :src="charImage" :alt="character?.name" />
        </div>
        <!-- Scene label -->
        <div class="explore-scene-label">
          <span class="map-label-deco">✦</span>
          <span class="map-label-text">{{ charDisplay }} · Lv.{{ stats.level }}</span>
          <span class="map-label-deco">✦</span>
        </div>
      </div>

      <!-- Overlay layer: all UI panels on top of the map -->
      <div class="explore-overlay">
        <!-- Top HUD bar (semi-transparent) -->
        <div class="adv-hud-bar">
          <div class="hud-ornament-left"></div>
          <div class="hud-ornament-right"></div>
          <button class="back-btn" @click="router.push('/characters')">
            <span class="back-arrow">←</span>
            <span class="back-text">返回</span>
          </button>
          <div class="char-info" v-if="character">
            <span class="char-name text-engraved-gold">{{ character.name }}</span>
            <span class="char-meta">{{ charDisplay }} · Lv.{{ stats.level }} · AC {{ stats.ac }}</span>
          </div>
          <div class="header-right">
            <button class="inv-toggle-btn" @click="showInventory = !showInventory">
              <span class="inv-icon">🎒</span>
              <span class="inv-label">背包</span>
              <span class="inv-badge" v-if="inventoryCount > 0">{{ inventoryCount }}</span>
            </button>
            <div class="level-badge">Lv.{{ stats.level }}</div>
          </div>
        </div>

        <!-- Left side: Stats panel (floating, compact) -->
        <div class="explore-stats-float" v-if="stats.maxHp > 0">
          <div class="stats-panel-inner">
            <div class="stat-row">
              <span class="stat-label">HP</span>
              <div class="bar-wrap hp-bar-wrap">
                <div class="bar-fill hp-bar" :style="{ width: hpPercent + '%', background: hpColor }"></div>
                <span class="bar-text">{{ stats.hp }} / {{ stats.maxHp }}</span>
              </div>
            </div>
            <div class="stat-row">
              <span class="stat-label">XP</span>
              <div class="bar-wrap xp-bar-wrap">
                <div class="bar-fill xp-bar" :style="{ width: xpPercent + '%' }"></div>
                <span class="bar-text">{{ stats.xp }} / {{ stats.xpToNext }}</span>
              </div>
            </div>
            <!-- Six ability scores -->
            <div class="ability-bar" v-if="abilityList.length">
              <span v-for="a in abilityList" :key="a.key" class="ability-chip">
                <span class="ability-key">{{ a.key }}</span>
                <span class="ability-val">{{ a.val }}</span>
                <span class="ability-mod">{{ a.mod >= 0 ? '+' : '' }}{{ a.mod }}</span>
              </span>
            </div>
            <!-- Hit dice -->
            <div class="stat-row" v-if="stats.maxHitDice > 0">
              <span class="stat-label">生命骰</span>
              <span class="dice-text">d{{ stats.hitDie }} × {{ stats.hitDice }}/{{ stats.maxHitDice }}</span>
            </div>
            <div class="spell-slots" v-if="spellSlotList.length">
              <span class="stat-label">法术位</span>
              <div class="slot-list">
                <span v-for="s in spellSlotList" :key="s.level" class="slot-chip" :class="{ depleted: s.current === 0 }">
                  {{ s.level }}环 {{ s.current }}/{{ s.max }}
                </span>
              </div>
            </div>
            <!-- Rest buttons -->
            <div class="explore-rest-btns" v-if="phase === 'EXPLORE' && !loading">
              <button class="rest-rune-btn short-rest-btn" @click="shortRest">
                <span class="rest-icon">⏱</span>
                <span class="rest-label">短休</span>
              </button>
              <button class="rest-rune-btn long-rest-btn" @click="longRest">
                <span class="rest-icon">🌙</span>
                <span class="rest-label">长休</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Right side: Narration panel (independent, scrollable) -->
        <div class="explore-narration-float">
          <NarrationPanel
            ref="narrationPanelRef"
            :story-log="storyLog"
            :streaming-text="streamingText"
            :loading="loading"
          />
        </div>

        <!-- Error message -->
        <div v-if="errorMsg" class="adv-error">⚠️ {{ errorMsg }}<button @click="errorMsg = ''" class="dismiss">×</button></div>

        <!-- Bottom: Action area (choices + input, always visible) -->
        <div class="explore-action-bar">
          <div class="action-area">
            <div v-if="choices.length && !loading" class="choices">
              <button v-for="(choice, i) in choices" :key="i" class="choice-btn" @click="selectChoice(choice)">
                <span class="choice-num">{{ i + 1 }}</span>
                <span class="choice-text">{{ choice.replace(/^\d+\.\s*/, "") }}</span>
              </button>
            </div>
            <div class="input-row" v-if="!loading">
              <input v-model="playerInput" type="text" class="action-input" placeholder="或者输入你想做的事..." @keydown.enter="submitInput" />
              <button class="send-btn" @click="submitInput" :disabled="!playerInput.trim()">
                <span class="send-rune">ᚱ</span>
                <span class="send-text">行动</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ═══ Encounter Dialog ═══ -->
    <EncounterDialog
      :visible="isEncounter"
      :encounter-info="encounterInfo"
      :narrative="streamingText || (storyLog.length ? storyLog[storyLog.length - 1] : '')"
      :loading="encounterLoading || loading"
      @confirm="confirmEncounter"
    />

    <!-- ════════════════════════════════════════════
         BATTLE MODE — BG3-style combat arena
         ════════════════════════════════════════════ -->
    <transition name="combat-transition">
      <div v-if="isCombat || showVictory" class="combat-arena" :class="{ 'victory-pending': showVictory }">
        <!-- Combat map background -->
        <div class="combat-map-bg">
          <img :src="combatMap" alt="战场" />
          <div class="combat-map-overlay"></div>
          <div class="combat-vignette"></div>
        </div>
        <!-- Combat background particles (embers & sparks) -->
        <div class="combat-bg-effects">
          <div class="combat-spark" v-for="n in 16" :key="'spark-' + n" :style="{ '--n': n }"></div>
          <div class="combat-ember-particle" v-for="n in 10" :key="'ember-' + n" :style="{ '--n': n }"></div>
        </div>

        <!-- Top: Turn info + turn order -->
        <TurnOrder
          :round="combat?.round || 1"
          :combat-phase="combat?.combatPhase || 'PLAYER_TURN'"
          :enemies="combat?.enemies || []"
          :player-name="character?.name || '冒险者'"
        />

        <!-- Main: Enemy display -->
        <div class="combat-main">
          <div class="enemy-zone">
            <div
              v-for="(e, i) in combat.enemies"
              :key="i"
              class="enemy-figure"
              :class="{
                dead: !e.alive,
                targeting: aliveEnemies.length === 1 && e.alive,
                selected: selectedTarget === e.name && e.alive,
                clickable: e.alive
              }"
              @click="selectEnemy(e)"
              :title="e.alive ? `点击选择 ${e.name} 作为目标` : ''"
            >
              <div class="enemy-avatar-frame">
                <div class="enemy-avatar" :class="{ 'has-image': getEnemyImage(e) }">
                  <img v-if="getEnemyImage(e)" :src="getEnemyImage(e)" :alt="e.name" class="enemy-avatar-img" />
                  <span v-else class="enemy-emoji">{{ e.alive ? '👹' : '💀' }}</span>
                  <div class="enemy-pulse" v-if="e.alive"></div>
                  <div class="enemy-target-reticle" v-if="selectedTarget === e.name && e.alive">
                    <span class="reticle-ring"></span>
                    <span class="reticle-cross reticle-cross-h"></span>
                    <span class="reticle-cross reticle-cross-v"></span>
                  </div>
                  <div class="enemy-skull-overlay" v-if="!e.alive">💀</div>
                </div>
              </div>
              <div class="enemy-info">
                <span class="enemy-name-tag">{{ e.name }}</span>
                <div class="enemy-hp-track" v-if="e.alive">
                  <div class="enemy-hp-bar-inner" :style="{ width: (e.hp / e.maxHp * 100) + '%' }"></div>
                  <span class="enemy-hp-label">{{ e.hp }}/{{ e.maxHp }}</span>
                </div>
                <span v-else class="enemy-slain">已击败</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Combat log — fantasy journal style -->
        <div class="combat-log-area">
          <div class="combat-log-border-deco"></div>
          <div v-for="(log, i) in combatLog" :key="i" class="combat-log-entry" :class="{ 'player-log': log.startsWith('【你】') }">
            {{ log }}
          </div>
          <div v-if="streamingText" class="combat-log-entry streaming">
            {{ streamingText.split("---")[0].trim() }}<span class="cursor">▎</span>
          </div>
          <div v-if="loading && !streamingText" class="combat-loading">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
        </div>

        <!-- Bottom: Player info + action bar + choices/input -->
        <div class="combat-action-zone">
          <!-- Player character bottom bar -->
          <div class="player-bottom-bar">
            <div class="pbb-portrait-frame">
              <div class="pbb-portrait" v-if="charImage">
                <img :src="charImage" :alt="character?.name" />
              </div>
              <div class="pbb-portrait pbb-portrait-fallback" v-else>
                <span>🧙</span>
              </div>
            </div>
            <div class="pbb-info">
              <div class="pbb-header">
                <span class="pbb-name">{{ character?.name || '冒险者' }}</span>
                <span class="pbb-class">{{ className }} · Lv.{{ stats.level }}</span>
                <button class="pbb-char-btn" @click="openCharacterPanel" :disabled="loading">
                  <span>👤</span>
                </button>
              </div>
              <div class="pbb-hp-row">
                <span class="pbb-hp-label">HP</span>
                <div class="pbb-hp-bar">
                  <div class="pbb-hp-fill" :style="{ width: hpPercent + '%', background: hpColor }"></div>
                  <span class="pbb-hp-text">{{ stats.hp }} / {{ stats.maxHp }}</span>
                </div>
                <div class="pbb-ac-badge">
                  <span class="pbb-ac-shield">🛡</span>
                  <span class="pbb-ac-val">{{ stats.ac }}</span>
                </div>
                <div class="pbb-slots" v-if="spellSlotList.length">
                  <span class="pbb-slot" v-for="s in spellSlotList.slice(0, 3)" :key="s.level" :class="{ depleted: s.current === 0 }">
                    {{ s.level }}环 {{ s.current }}/{{ s.max }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="errorMsg" class="combat-error">⚠️ {{ errorMsg }}<button @click="errorMsg = ''" class="dismiss">×</button></div>

          <!-- Action bar -->
          <ActionBar
            :action-points="combat?.actionPoints ?? 1"
            :max-action-points="combat?.maxActionPoints ?? 1"
            :combat-phase="combat?.combatPhase || 'PLAYER_TURN'"
            :loading="loading"
            :has-spells="hasSpells"
            @attack="quickAttack"
            @ranged-attack="quickRangedAttack"
            @cast-spell="quickCastSpell"
            @use-item="openCombatInventory"
            @end-turn="endTurn"
          />

          <!-- Combat choices as stone tablets -->
          <div v-if="choices.length && !loading" class="combat-choices">
            <button v-for="(choice, i) in choices" :key="i" class="combat-btn" :class="`combat-btn-${i % 3}`" @click="selectChoice(choice)">
              <span class="combat-btn-num">{{ i + 1 }}</span>
              <span class="combat-btn-text">{{ choice.replace(/^\d+\.\s*/, "") }}</span>
            </button>
          </div>
          <div class="combat-input-row" v-if="!loading">
            <input v-model="playerInput" type="text" class="combat-input" placeholder="或输入自定义战斗行动..." @keydown.enter="submitInput" />
            <button class="combat-send" @click="submitInput" :disabled="!playerInput.trim()">⚡</button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ════════════════════════════════════════════
         VICTORY SCREEN
         ════════════════════════════════════════════ -->
    <transition name="victory-transition">
      <div v-if="showVictory" class="victory-overlay">
        <div class="victory-modal">
          <div class="victory-modal-frame-tl"></div>
          <div class="victory-modal-frame-tr"></div>
          <div class="victory-modal-frame-bl"></div>
          <div class="victory-modal-frame-br"></div>
          <!-- Victory title -->
          <div class="victory-header">
            <div class="victory-icon">🏆</div>
            <h2 class="victory-title text-glow-gold">战斗胜利</h2>
            <div class="victory-subtitle" v-if="victoryData.leveledUp">
              <span class="levelup-tag">⭐ 升级到 Lv.{{ victoryData.newLevel }}!</span>
            </div>
          </div>

          <!-- XP gained with shimmer -->
          <div class="victory-section xp-section shimmer-magical">
            <div class="section-icon">✦</div>
            <div class="section-content">
              <span class="section-label">经验值</span>
              <span class="xp-gained">+{{ victoryData.xpGained }} XP</span>
            </div>
          </div>

          <!-- Loot with rarity-colored borders -->
          <div class="victory-section loot-section" v-if="victoryData.loot && victoryData.loot.length">
            <div class="loot-header">
              <span class="section-icon">🎒</span>
              <span class="section-label">战利品</span>
            </div>
            <div class="loot-grid">
              <div
                v-for="(item, i) in victoryData.loot"
                :key="i"
                class="loot-item"
                :class="`rarity-${item.rarity}`"
                :style="{ '--delay': i * 0.15 + 's' }"
              >
                <span class="loot-icon">{{ item.icon }}</span>
                <div class="loot-info">
                  <span class="loot-name">{{ item.name }}</span>
                  <span class="loot-qty" v-if="item.quantity > 1">×{{ item.quantity }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="victory-section loot-empty" v-else>
            <span class="loot-empty-text">未发现战利品</span>
          </div>

          <!-- Confirm button -->
          <button class="victory-confirm-btn" @click="dismissVictory">
            确认并继续探索
          </button>
        </div>
      </div>
    </transition>

    <!-- ════════════════════════════════════════════
         DEATH SCREEN
         ════════════════════════════════════════════ -->
    <transition name="death-transition">
      <div v-if="isDead" class="death-screen">
        <div class="death-bg"></div>
        <div class="death-content">
          <div class="death-icon">💀</div>
          <h2 class="death-title">冒险终结</h2>
          <p>你的角色已倒下，冒险存档已被清除。</p>
          <div class="death-stats" v-if="character">
            <span>{{ character.name }}</span>
            <span>{{ charDisplay }}</span>
            <span>Lv.{{ stats.level }}</span>
          </div>
          <button class="death-btn" @click="router.push('/characters')">返回角色列表</button>
        </div>
      </div>
    </transition>

    <!-- ═══ Level Up Panel ═══ -->
    <LevelUpPanel
      :visible="isLevelUp"
      :session-id="sessionId"
      :level-up-data="levelUpData"
      :character-name="character?.name || '冒险者'"
      @close="levelUpData = null"
      @confirmed="onLevelUpConfirmed"
    />

    <!-- ═══ Inventory Panel (explore mode) ═══ -->
    <InventoryPanel
      :visible="showInventory"
      :character-id="characterId"
      :character-name="character?.name || '冒险者'"
      :session-id="sessionId"
      :in-combat="false"
      :sync-items="inventory"
      @close="showInventory = false"
      @item-used="onItemUsedFromPanel"
    />

    <!-- ═══ Inventory Panel (combat mode) ═══ -->
    <InventoryPanel
      :visible="showCombatInventory"
      :character-id="characterId"
      :character-name="character?.name || '冒险者'"
      :session-id="sessionId"
      :in-combat="true"
      :action-points="combatActionPoints"
      :sync-items="inventory"
      @close="closeCombatInventory"
      @item-used="onItemUsedFromPanel"
    />

    <!-- ═══ Character Panel ═══ -->
    <CharacterPanel
      :visible="showCharacterPanel"
      :session-id="sessionId"
      @close="showCharacterPanel = false"
    />

    <!-- ═══ Spell Selection Modal ═══ -->
    <transition name="fade">
      <div v-if="showSpellList" class="spell-list-overlay" @click.self="showSpellList = false">
        <div class="spell-list-modal">
          <div class="spell-list-header">
            <span class="spell-list-icon">✨</span>
            <h3>选择法术</h3>
            <button class="spell-list-close" @click="showSpellList = false">×</button>
          </div>
          <div class="spell-list-body">
            <div v-if="spellList.length === 0" class="spell-list-empty">
              暂无可用法术
            </div>
            <div
              v-for="(spell, i) in spellList"
              :key="i"
              class="spell-card"
              @click="selectSpell(spell)"
            >
              <div class="spell-card-name">{{ spell.split(':')[0].trim() }}</div>
              <div class="spell-card-desc">{{ spell.split(':').slice(1).join(':').trim() || '神秘法术' }}</div>
            </div>
          </div>
          <div class="spell-list-footer">
            <button class="spell-cancel-btn" @click="showSpellList = false">取消</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>


<style scoped>
.adventure-page {
  width: 100%;
  height: 100vh;
  height: 100dvh;
  margin: 0;
  padding: 0;
  position: relative;
  overflow: hidden;
}

/* ════════════════════════════════════════════
   EXPLORE MODE — Fullscreen Map + Overlay
   ════════════════════════════════════════════ */

/* ── Fullscreen Map Background ── */
.explore-map-fullscreen {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  background: var(--bg-void);
}
.explore-map-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  filter: brightness(0.55) contrast(1.2) saturate(0.85);
  transition: filter 0.6s ease, opacity 0.8s ease;
}
.explore-map-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg,
    rgba(11, 9, 8, 0.4) 0%,
    transparent 25%,
    transparent 50%,
    rgba(10, 8, 20, 0.75) 85%,
    rgba(8, 6, 14, 0.95) 100%);
  pointer-events: none;
}
.explore-map-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, transparent 30%, rgba(0, 0, 0, 0.7) 100%);
  pointer-events: none;
}

/* Map ember particles (fullscreen) */
.map-embers {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.map-ember {
  position: absolute;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--ember, #c97a3a);
  box-shadow: 0 0 6px var(--ember, #c97a3a);
  opacity: 0;
  left: calc(5% + var(--n) * 7.5%);
  bottom: -5px;
  animation: mapEmberRiseFS calc(5s + var(--n) * 0.6s) ease-out infinite;
  animation-delay: calc(var(--n) * -0.5s);
}
@keyframes mapEmberRiseFS {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  10% { opacity: 0.6; }
  80% { opacity: 0.2; }
  100% { transform: translateY(-100vh) translateX(calc(var(--n) * 10px - 48px)); opacity: 0; }
}

/* ── Large Character Portrait (right side of map) ── */
.explore-char-portrait {
  position: absolute;
  bottom: 0;
  right: 2%;
  height: 75vh;
  max-height: 600px;
  z-index: 1;
  filter: drop-shadow(-8px -4px 24px rgba(0, 0, 0, 0.7));
  pointer-events: none;
  animation: portraitFadeIn 0.8s ease;
}
.explore-char-portrait img {
  height: 100%;
  width: auto;
  object-fit: contain;
  object-position: bottom center;
  filter: brightness(0.92) contrast(1.12);
}
@keyframes portraitFadeIn {
  from { opacity: 0; transform: translateX(40px); }
  to { opacity: 1; transform: translateX(0); }
}

/* ── Scene Label (top-left of map) ── */
.explore-scene-label {
  position: absolute;
  top: 80px;
  left: 24px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 18px;
  background: linear-gradient(135deg, rgba(30, 22, 18, 0.82), rgba(20, 15, 12, 0.7));
  backdrop-filter: blur(6px);
  border: 1px solid var(--line-gold);
  border-radius: var(--radius-sm);
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 700;
  color: var(--gold-bright);
  letter-spacing: 0.1em;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.8);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.5);
  animation: labelSlideIn 0.5s ease;
}
.map-label-deco {
  font-size: 10px;
  color: var(--gold-dim);
  filter: drop-shadow(0 0 4px var(--gold-soft));
}
@keyframes labelSlideIn {
  from { opacity: 0; transform: translateX(-30px); }
  to { opacity: 1; transform: translateX(0); }
}

/* ── Overlay Layer (all UI on top of map) ── */
.explore-overlay {
  position: fixed;
  inset: 0;
  z-index: 10;
  display: flex;
  flex-direction: column;
  pointer-events: none;
}
.explore-overlay > * {
  pointer-events: auto;
}

/* ── Ornate HUD Top Bar ── */
.adv-hud-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  border-bottom: 1px solid var(--line-gold);
  flex-shrink: 0;
  position: relative;
  background: linear-gradient(180deg, rgba(20, 15, 12, 0.88), rgba(15, 11, 8, 0.75));
  backdrop-filter: blur(10px);
  border: 1px solid var(--line-gold);
  border-top: none;
  border-left: none;
  border-right: none;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4), inset 0 -1px 0 rgba(201, 162, 39, 0.08);
}
.adv-hud-bar::after {
  content: "";
  position: absolute;
  bottom: -1px;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--gold), transparent);
}
.hud-ornament-left,
.hud-ornament-right {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: linear-gradient(180deg, transparent, var(--gold-dim), transparent);
  pointer-events: none;
}
.hud-ornament-left { left: 8px; }
.hud-ornament-right { right: 8px; }

.back-btn {
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--line-light);
  color: var(--ink-soft);
  padding: 8px 18px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 13px;
  transition: var(--transition-base);
  font-weight: 600;
  font-family: var(--font-heading);
  display: flex;
  align-items: center;
  gap: 6px;
  letter-spacing: 0.05em;
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.08), 0 2px 8px rgba(0, 0, 0, 0.3);
}
.back-arrow { transition: transform 0.25s ease; font-size: 15px; }
.back-btn:hover {
  border-color: var(--gold);
  color: var(--gold-bright);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.2), 0 0 16px var(--gold-glow);
}
.back-btn:hover .back-arrow { transform: translateX(-4px); }

.char-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}
.char-name {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.06em;
}
.char-meta {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 0.03em;
}

.level-badge {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 800;
  color: var(--gold-bright);
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--gold-dim);
  padding: 4px 16px;
  border-radius: 20px;
  letter-spacing: 0.08em;
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.15), 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* ── Floating Stats Panel (left side, over map) ── */
.explore-stats-float {
  position: absolute;
  top: 80px;
  left: 24px;
  width: 280px;
  max-width: calc(100vw - 48px);
  z-index: 5;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--line-gold);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.6), inset 0 0 30px rgba(0, 0, 0, 0.3);
  animation: statsSlideIn 0.4s ease;
}
@keyframes statsSlideIn {
  from { opacity: 0; transform: translateX(-30px); }
  to { opacity: 1; transform: translateX(0); }
}
.stats-panel-inner {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: linear-gradient(135deg, rgba(25, 18, 14, 0.92), rgba(18, 13, 10, 0.94));
  backdrop-filter: blur(8px);
  position: relative;
}
.stats-panel-inner::before {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 30% 20%, rgba(201, 162, 39, 0.04), transparent 60%);
  pointer-events: none;
}
.stat-row { display: flex; align-items: center; gap: 10px; position: relative; z-index: 1; }
.stat-label {
  font-family: var(--font-heading);
  font-size: 11px;
  font-weight: 700;
  color: var(--gold-dim);
  width: 42px;
  text-align: right;
  flex-shrink: 0;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}
.bar-wrap {
  flex: 1;
  height: 24px;
  background: var(--bg-void);
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  border: 1px solid var(--line);
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.6);
}
.hp-bar-wrap { box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.6), 0 0 12px rgba(76, 175, 80, 0.08); }
.xp-bar-wrap { box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.6), 0 0 12px rgba(93, 58, 138, 0.08); }
.bar-fill {
  height: 100%;
  border-radius: 12px;
  transition: width 0.6s cubic-bezier(.4, 0, .2, 1);
  position: relative;
  overflow: hidden;
}
.bar-fill::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255,255,255,0.22) 0%, transparent 50%, rgba(0,0,0,0.12) 100%);
}
.bar-fill::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  animation: shimmer 3s infinite;
}
@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}
.xp-bar {
  background: linear-gradient(90deg, #5d3a8a, #7d5aaa, #a878d4);
  box-shadow: 0 0 10px rgba(93, 58, 138, 0.3);
}
.bar-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0,0,0,0.8);
  letter-spacing: 0.03em;
  z-index: 1;
  pointer-events: none;
}

/* Ability chips */
.ability-bar {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  position: relative;
  z-index: 1;
}
.ability-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--line);
  font-size: 11px;
  font-family: var(--font-heading);
  font-weight: 600;
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.05);
  transition: var(--transition-base);
}
.ability-chip:hover {
  border-color: var(--gold-dim);
  box-shadow: 0 0 10px var(--gold-glow);
}
.ability-key { color: var(--gold); font-weight: 700; letter-spacing: 0.05em; }
.ability-val { color: var(--ink-bright); }
.ability-mod { color: var(--ink-soft); font-size: 10px; }

.dice-text {
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 700;
  color: var(--gold);
  letter-spacing: 0.05em;
}

.spell-slots { display: flex; align-items: center; gap: 8px; position: relative; z-index: 1; }
.slot-list { display: flex; gap: 6px; flex-wrap: wrap; flex: 1; }
.slot-chip {
  font-family: var(--font-heading);
  font-size: 10px;
  padding: 3px 12px;
  border-radius: 12px;
  background: var(--arcane-soft);
  color: var(--arcane-glow);
  border: 1px solid var(--arcane);
  font-weight: 600;
  transition: var(--transition-base);
  letter-spacing: 0.03em;
  box-shadow: 0 0 8px rgba(93, 58, 138, 0.1);
}
.slot-chip.depleted {
  opacity: 0.25;
  text-decoration: line-through;
  background: var(--bg-card);
  box-shadow: none;
}

/* ── Rest Buttons (inside floating stats) ── */
.explore-rest-btns { display: flex; gap: 8px; margin-top: 4px; }
.rest-rune-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid var(--line-light);
  border-radius: var(--radius-sm);
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  color: var(--ink-soft);
  cursor: pointer;
  font-size: 13px;
  font-family: var(--font-heading);
  font-weight: 600;
  letter-spacing: 0.08em;
  transition: var(--transition-base);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.08), 0 2px 8px rgba(0, 0, 0, 0.3);
  position: relative;
  overflow: hidden;
}
.rest-rune-btn::before {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at center, rgba(201, 162, 39, 0.12), transparent 70%);
  opacity: 0;
  transition: opacity var(--transition-base);
}
.rest-rune-btn:hover {
  border-color: var(--gold);
  color: var(--gold-bright);
  transform: translateY(-1px);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.2), 0 0 20px var(--gold-glow);
}
.rest-rune-btn:hover::before { opacity: 1; }
.rest-rune-btn:active { transform: translateY(0); }
.short-rest-btn { border-color: var(--nature); }
.short-rest-btn:hover { border-color: var(--nature); color: #6acd4a; box-shadow: 0 0 16px rgba(74, 122, 58, 0.2); }
.long-rest-btn { border-color: var(--arcane); }
.long-rest-btn:hover { border-color: var(--arcane-glow); color: var(--arcane-glow); box-shadow: 0 0 16px rgba(93, 58, 138, 0.2); }
.rest-icon { font-size: 18px; }
.rest-label { letter-spacing: 0.1em; }

/* ── Floating Narration Panel (right side, over map) ── */
.explore-narration-float {
  position: absolute;
  top: 80px;
  right: 24px;
  width: 380px;
  max-width: calc(100vw - 48px);
  max-height: calc(100vh - 280px);
  z-index: 5;
  display: flex;
  flex-direction: column;
  animation: narrationSlideIn 0.4s ease;
}
@keyframes narrationSlideIn {
  from { opacity: 0; transform: translateX(30px); }
  to { opacity: 1; transform: translateX(0); }
}
/* NarrationPanel internal scroll area adapts to floating container */
.explore-narration-float :deep(.narration-scroll) {
  max-height: calc(100vh - 280px);
}
.explore-narration-float :deep(.narration-content) {
  max-height: calc(100vh - 340px);
}

/* ── Bottom Action Bar (choices + input, always visible) ── */
.explore-action-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 8;
  padding: 16px 24px 20px;
  background: linear-gradient(0deg,
    rgba(10, 7, 6, 0.95) 0%,
    rgba(12, 9, 8, 0.85) 60%,
    transparent 100%);
  backdrop-filter: blur(8px);
  border-top: 1px solid var(--line-gold);
}
.explore-action-bar::before {
  content: "";
  position: absolute;
  top: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--gold-dim), transparent);
}

/* ── Action Area — stone tablet choices ── */
.action-area {
  flex-shrink: 0;
  padding: 0;
  max-width: 680px;
  margin: 0 auto;
}

/* ── Shared loading dot & cursor (combat mode) ── */
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--arcane-glow);
  box-shadow: 0 0 6px var(--arcane-glow);
  animation: bounce 1s infinite alternate;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { to { opacity: 0.3; transform: translateY(-5px); } }
.cursor {
  display: inline-block;
  color: var(--arcane-glow);
  animation: blink 0.8s steps(2) infinite;
  font-weight: 100;
}
@keyframes blink { to { opacity: 0; } }
.choices { display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.choice-btn {
  display: flex;
  align-items: center;
  gap: 14px;
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--line);
  color: var(--ink);
  padding: 14px 18px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  text-align: left;
  font-size: 14px;
  font-family: var(--font-body);
  transition: var(--transition-base);
  position: relative;
  overflow: hidden;
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.05), 0 2px 8px rgba(0, 0, 0, 0.3);
}
.choice-btn::before {
  content: "";
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 3px;
  background: var(--gold);
  transform: scaleY(0);
  transition: transform 0.25s ease;
}
.choice-btn:hover {
  border-color: var(--gold-dim);
  background: linear-gradient(180deg, var(--bg-hover), var(--bg-card));
  transform: translateX(6px);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.15), 0 4px 16px rgba(0, 0, 0, 0.4), 0 0 12px var(--gold-glow);
}
.choice-btn:hover::before { transform: scaleY(1); }
.choice-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(180deg, var(--bg-stone), var(--bg-dark));
  color: var(--gold-bright);
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
  border: 1px solid var(--gold-dim);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.1), 0 2px 4px rgba(0, 0, 0, 0.4);
}
.choice-text { flex: 1; line-height: 1.5; }
.input-row { display: flex; gap: 8px; }
.action-input {
  flex: 1;
  background: var(--bg-card);
  border: 1px solid var(--line);
  color: var(--ink);
  padding: 13px 18px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-family: var(--font-body);
  outline: none;
  transition: var(--transition-base);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.3);
}
.action-input:focus {
  border-color: var(--gold);
  box-shadow: 0 0 0 3px var(--gold-soft), inset 0 2px 4px rgba(0, 0, 0, 0.3);
}
.action-input::placeholder { color: var(--muted); font-style: italic; }
.send-btn {
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--gold-dim);
  color: var(--gold-bright);
  padding: 0 24px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.08em;
  transition: var(--transition-base);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.12), 0 2px 8px rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  gap: 6px;
}
.send-rune {
  font-family: var(--font-rune);
  font-size: 18px;
  color: var(--gold);
}
.send-btn:hover:not(:disabled) {
  border-color: var(--gold);
  color: var(--ink-bright);
  transform: translateY(-1px);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.25), 0 0 20px var(--gold-glow);
}
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* ── Error ── */
.adv-error {
  background: var(--crimson-soft);
  border: 1px solid var(--crimson-bright);
  color: var(--crimson-bright);
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  animation: shake 0.3s ease;
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}
.dismiss { background: none; border: none; color: var(--crimson-bright); font-size: 20px; cursor: pointer; padding: 0 4px; }

/* ════════════════════════════════════════════
   BATTLE MODE — BG3-style combat arena
   ════════════════════════════════════════════ */

.combat-transition-enter-active {
  animation: combatEnter 0.5s cubic-bezier(.34, 1.56, .64, 1);
}
.combat-transition-leave-active {
  animation: combatEnter 0.3s ease reverse;
}
@keyframes combatEnter {
  from { opacity: 0; transform: scale(1.1); filter: blur(8px); }
  to { opacity: 1; transform: scale(1); filter: blur(0); }
}

.combat-arena {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  background: radial-gradient(ellipse at 50% 30%, #1a1520 0%, #0d0b14 50%, #08060e 100%);
  overflow: hidden;
}

/* Combat map background */
.combat-map-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}
.combat-map-bg img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  filter: brightness(0.3) contrast(1.25) saturate(0.7);
}
.combat-map-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, rgba(10, 5, 15, 0.4) 0%, rgba(5, 3, 10, 0.9) 100%);
  pointer-events: none;
}
.combat-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, transparent 30%, rgba(0, 0, 0, 0.7) 90%);
  pointer-events: none;
  z-index: 1;
}

/* Combat background particles — embers & sparks */
.combat-bg-effects {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 1;
}
.combat-spark {
  position: absolute;
  width: 2px;
  height: 2px;
  border-radius: 50%;
  background: rgba(232, 196, 74, 0.6);
  box-shadow: 0 0 4px rgba(232, 196, 74, 0.5);
  left: calc(50% + (var(--n) * 41 - 328) * 1px);
  bottom: -10px;
  animation: sparkRise calc(2.5s + var(--n) * 0.3s) linear infinite;
  animation-delay: calc(var(--n) * -0.4s);
}
@keyframes sparkRise {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  10% { opacity: 0.8; }
  90% { opacity: 0.3; }
  100% { transform: translateY(-100vh) translateX(calc(var(--n) * 12 - 96px)); opacity: 0; }
}
.combat-ember-particle {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--ember);
  box-shadow: 0 0 8px var(--ember);
  left: calc(10% + var(--n) * 9%);
  bottom: -10px;
  animation: emberRiseCombat calc(4s + var(--n) * 0.5s) ease-out infinite;
  animation-delay: calc(var(--n) * -0.8s);
}
@keyframes emberRiseCombat {
  0% { transform: translateY(0) translateX(0) scale(1); opacity: 0; }
  10% { opacity: 0.7; }
  80% { opacity: 0.3; }
  100% { transform: translateY(-100vh) translateX(calc(var(--n) * 15 - 75px)) scale(0.3); opacity: 0; }
}

/* ── Enemy Zone ── */
.enemy-zone {
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  gap: 28px;
  padding: 20px 20px 16px;
  flex-wrap: wrap;
  z-index: 2;
}
.combat-main {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 12px 16px;
  overflow: hidden;
  z-index: 2;
  min-height: 0;
}
.combat-main .enemy-zone {
  flex: 1;
  padding: 0;
  align-items: flex-start;
  align-content: flex-start;
  gap: 28px;
}
.enemy-figure {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-width: 140px;
  animation: enemyAppear 0.5s cubic-bezier(.34, 1.56, .64, 1) backwards;
}
.enemy-figure:nth-child(1) { animation-delay: 0.1s; }
.enemy-figure:nth-child(2) { animation-delay: 0.2s; }
.enemy-figure:nth-child(3) { animation-delay: 0.3s; }
.enemy-figure:nth-child(4) { animation-delay: 0.4s; }
@keyframes enemyAppear {
  from { opacity: 0; transform: translateY(-20px) scale(0.8); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.enemy-figure.dead {
  opacity: 0.35;
  filter: grayscale(1);
  animation: enemyDeath 0.6s ease;
}
@keyframes enemyDeath {
  0% { opacity: 1; transform: scale(1) rotate(0); }
  50% { transform: scale(1.1) rotate(5deg); }
  100% { opacity: 0.35; transform: scale(0.9) rotate(-5deg); }
}

/* Ornate enemy avatar frame */
.enemy-avatar-frame {
  position: relative;
  padding: 4px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--gold-dim), var(--bg-dark), var(--gold-dim));
  box-shadow: 0 0 16px rgba(201, 162, 39, 0.1), inset 0 0 8px rgba(0, 0, 0, 0.5);
}
.enemy-figure.dead .enemy-avatar-frame {
  background: linear-gradient(135deg, #3a2a2a, var(--bg-dark), #3a2a2a);
}
.enemy-avatar {
  position: relative;
  width: 100px;
  height: 120px;
  border-radius: 8px;
  background: radial-gradient(circle, #2a1a2a 0%, #1a0a1a 100%);
  border: 1px solid var(--crimson-bright);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 0 20px rgba(155, 45, 45, 0.2), inset 0 2px 8px rgba(0, 0, 0, 0.6);
}
.enemy-avatar.has-image { border-radius: 8px; }
.enemy-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  filter: brightness(0.85) contrast(1.2);
}
.enemy-emoji {
  font-size: 42px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.5));
}
.enemy-pulse {
  position: absolute;
  inset: -4px;
  border-radius: 10px;
  border: 2px solid var(--crimson-bright);
  opacity: 0;
  animation: enemyPulse 2s ease-in-out infinite;
}
@keyframes enemyPulse {
  0% { opacity: 0.4; transform: scale(1); }
  100% { opacity: 0; transform: scale(1.3); }
}

/* Targeting reticle */
.enemy-target-reticle {
  position: absolute;
  inset: -12px;
  pointer-events: none;
}
.reticle-ring {
  position: absolute;
  inset: 0;
  border: 2px solid var(--crimson-bright);
  border-radius: 12px;
  animation: reticleRotate 3s linear infinite;
  box-shadow: 0 0 16px rgba(155, 45, 45, 0.5);
}
.reticle-ring::before,
.reticle-ring::after {
  content: "";
  position: absolute;
  width: 8px;
  height: 8px;
  border: 2px solid var(--gold-bright);
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
}
.reticle-ring::before { top: -5px; left: -5px; }
.reticle-ring::after { bottom: -5px; right: -5px; }
.reticle-cross {
  position: absolute;
  background: var(--gold-bright);
  box-shadow: 0 0 8px var(--gold-bright);
}
.reticle-cross-h {
  top: 50%;
  left: -8px;
  right: -8px;
  height: 1px;
  transform: translateY(-50%);
}
.reticle-cross-v {
  left: 50%;
  top: -8px;
  bottom: -8px;
  width: 1px;
  transform: translateX(-50%);
}
@keyframes reticleRotate {
  from { transform: rotate(0); }
  to { transform: rotate(360deg); }
}

/* Skull overlay for dead enemies */
.enemy-skull-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  background: rgba(0, 0, 0, 0.5);
  filter: grayscale(1);
  z-index: 5;
  animation: skullFadeIn 0.6s ease;
}
@keyframes skullFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.enemy-figure.targeting .enemy-avatar-frame {
  animation: targetPulse 1.5s ease-in-out infinite;
}
@keyframes targetPulse {
  0%, 100% { box-shadow: 0 0 16px rgba(201, 162, 39, 0.1), inset 0 0 8px rgba(0, 0, 0, 0.5), 0 0 0 0 rgba(155, 45, 45, 0); }
  50% { box-shadow: 0 0 16px rgba(201, 162, 39, 0.1), inset 0 0 8px rgba(0, 0, 0, 0.5), 0 0 0 8px rgba(155, 45, 45, 0.2); }
}
.enemy-figure.clickable { cursor: pointer; transition: transform 0.15s ease; }
.enemy-figure.clickable:hover { transform: translateY(-4px); }
.enemy-figure.selected .enemy-avatar-frame {
  filter: drop-shadow(0 0 12px rgba(232, 196, 74, 0.6));
  animation: targetSelectedPulse 1s ease-in-out infinite;
}
@keyframes targetSelectedPulse {
  0%, 100% { box-shadow: 0 0 0 2px rgba(232, 196, 74, 0.5), 0 0 16px rgba(232, 196, 74, 0.3); }
  50% { box-shadow: 0 0 0 4px rgba(232, 196, 74, 0.8), 0 0 28px rgba(232, 196, 74, 0.6); }
}
.enemy-figure.selected .enemy-name-tag {
  color: var(--gold-bright);
  font-weight: 800;
  text-shadow: 0 0 8px rgba(232, 196, 74, 0.4);
}

.enemy-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  width: 100%;
}
.enemy-name-tag {
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 600;
  color: #f0e0e0;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.9);
  letter-spacing: 0.03em;
}
.enemy-hp-track {
  position: relative;
  width: 100%;
  height: 18px;
  background: rgba(0, 0, 0, 0.7);
  border-radius: 9px;
  overflow: hidden;
  border: 1px solid rgba(155, 45, 45, 0.4);
  box-shadow: inset 0 1px 4px rgba(0, 0, 0, 0.6);
}
.enemy-hp-bar-inner {
  height: 100%;
  background: linear-gradient(90deg, #4a0a0a, #8b1a1a, #c62828);
  transition: width 0.5s cubic-bezier(.4, 0, .2, 1);
  position: relative;
  box-shadow: 0 0 8px rgba(155, 45, 45, 0.3);
}
.enemy-hp-bar-inner::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255,255,255,0.18) 0%, transparent 50%, rgba(0,0,0,0.15) 100%);
}
.enemy-hp-label {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  color: #fff;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.9);
  font-family: var(--font-heading);
}
.enemy-slain {
  font-family: var(--font-heading);
  font-size: 11px;
  color: var(--muted);
  font-style: italic;
  letter-spacing: 0.05em;
}

/* ── Combat Log — fantasy journal ── */
.combat-log-area {
  flex-shrink: 0;
  max-height: 150px;
  overflow-y: auto;
  padding: 12px 22px;
  z-index: 2;
  scroll-behavior: smooth;
  background: linear-gradient(135deg, rgba(30, 22, 18, 0.7), rgba(17, 13, 10, 0.6));
  border-top: 1px solid var(--line-gold);
  border-bottom: 1px solid var(--line-gold);
  position: relative;
  box-shadow: inset 0 4px 12px rgba(0, 0, 0, 0.4);
}
.combat-log-border-deco {
  position: absolute;
  top: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--gold-dim), transparent);
}
.combat-log-entry {
  font-family: var(--font-body);
  font-size: 14px;
  line-height: 1.8;
  color: #d4c8b8;
  margin-bottom: 12px;
  padding: 10px 16px;
  background: rgba(30, 20, 15, 0.5);
  border-left: 3px solid var(--arcane);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  white-space: pre-wrap;
  animation: fadeInUp 0.3s ease;
}
.combat-log-entry.player-log {
  color: var(--gold-bright);
  font-style: italic;
  font-family: var(--font-heading);
  border-left-color: var(--gold);
  background: linear-gradient(90deg, var(--gold-soft), transparent);
}
.combat-log-entry.streaming {
  color: var(--ink-soft);
  border-left-color: var(--crimson-bright);
}
.combat-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 0;
}
.combat-loading .dot {
  width: 8px;
  height: 8px;
  background: var(--crimson-bright);
  box-shadow: 0 0 6px var(--crimson-bright);
}

/* ── Player Bottom Bar — BG3 HUD ── */
.player-bottom-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  background: linear-gradient(135deg, rgba(30, 22, 18, 0.95), rgba(20, 15, 12, 0.92));
  border: 1px solid var(--line-gold);
  border-radius: var(--radius-md);
  margin-bottom: 8px;
  backdrop-filter: blur(8px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(201, 162, 39, 0.08);
}
.pbb-portrait-frame {
  padding: 3px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--gold), var(--gold-dim), var(--gold));
  flex-shrink: 0;
  box-shadow: 0 0 12px rgba(201, 162, 39, 0.15);
}
.pbb-portrait {
  width: 56px;
  height: 72px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--bg-dark);
  background: radial-gradient(circle, #2a2040, #1a1025);
}
.pbb-portrait img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  filter: brightness(0.95) contrast(1.1);
}
.pbb-portrait-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}
.pbb-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.pbb-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pbb-name {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  color: var(--gold-bright);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: 0.04em;
  text-shadow: 0 0 8px rgba(201, 162, 39, 0.15);
}
.pbb-class {
  font-family: var(--font-heading);
  font-size: 11px;
  color: var(--muted);
  white-space: nowrap;
  letter-spacing: 0.03em;
}
.pbb-char-btn {
  margin-left: auto;
  background: var(--gold-soft);
  border: 1px solid var(--gold-dim);
  color: var(--gold);
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.08);
}
.pbb-char-btn:hover:not(:disabled) {
  border-color: var(--gold);
  background: rgba(201, 162, 39, 0.15);
  box-shadow: 0 0 10px var(--gold-glow);
}
.pbb-char-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.pbb-hp-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pbb-hp-label {
  font-family: var(--font-heading);
  font-size: 11px;
  font-weight: 700;
  color: var(--muted);
  flex-shrink: 0;
  letter-spacing: 0.08em;
}
.pbb-hp-bar {
  position: relative;
  flex: 1;
  height: 22px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 11px;
  overflow: hidden;
  border: 1px solid var(--line);
  min-width: 120px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.5);
}
.pbb-hp-fill {
  height: 100%;
  border-radius: 11px;
  transition: width 0.5s ease, background 0.3s ease;
  position: relative;
}
.pbb-hp-fill::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255,255,255,0.18) 0%, transparent 50%);
}
.pbb-hp-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.9);
  font-family: var(--font-heading);
}
.pbb-ac-badge {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--frost);
  flex-shrink: 0;
  box-shadow: inset 0 1px 0 rgba(74, 122, 154, 0.15), 0 0 8px rgba(74, 122, 154, 0.1);
}
.pbb-ac-shield { font-size: 12px; }
.pbb-ac-val {
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 800;
  color: var(--frost);
}
.pbb-slots {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
.pbb-slot {
  font-family: var(--font-heading);
  font-size: 10px;
  padding: 3px 8px;
  border-radius: var(--radius-sm);
  background: var(--arcane-soft);
  border: 1px solid var(--arcane);
  color: var(--arcane-glow);
  font-weight: 600;
  white-space: nowrap;
  box-shadow: 0 0 6px rgba(93, 58, 138, 0.1);
}
.pbb-slot.depleted {
  opacity: 0.3;
  text-decoration: line-through;
  box-shadow: none;
}

/* ── Combat Action Zone ── */
.combat-action-zone {
  flex-shrink: 0;
  padding: 10px 16px 16px;
  background: linear-gradient(0deg, rgba(11, 9, 8, 0.97), rgba(20, 15, 12, 0.6));
  border-top: 1px solid var(--line-gold);
  z-index: 2;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.5);
}
.combat-error {
  background: var(--crimson-soft);
  border: 1px solid var(--crimson-bright);
  color: var(--crimson-bright);
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.combat-choices {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}
.combat-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  text-align: left;
  font-size: 13px;
  font-weight: 500;
  font-family: var(--font-body);
  transition: var(--transition-base);
  position: relative;
  overflow: hidden;
  border: 1px solid;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03), 0 2px 6px rgba(0, 0, 0, 0.3);
}
.combat-btn-0 {
  background: linear-gradient(135deg, rgba(107, 29, 29, 0.15), rgba(30, 15, 20, 0.8));
  border-color: rgba(155, 45, 45, 0.4);
  color: #f0d0d0;
}
.combat-btn-0:hover {
  border-color: var(--crimson-bright);
  background: linear-gradient(135deg, rgba(107, 29, 29, 0.25), rgba(40, 15, 20, 0.9));
  transform: translateX(6px);
  box-shadow: 0 4px 16px rgba(155, 45, 45, 0.15);
}
.combat-btn-1 {
  background: linear-gradient(135deg, rgba(93, 58, 138, 0.15), rgba(25, 20, 35, 0.8));
  border-color: rgba(125, 90, 170, 0.4);
  color: #e0d0f0;
}
.combat-btn-1:hover {
  border-color: var(--arcane-glow);
  background: linear-gradient(135deg, rgba(93, 58, 138, 0.25), rgba(35, 20, 45, 0.9));
  transform: translateX(6px);
  box-shadow: 0 4px 16px rgba(125, 90, 170, 0.15);
}
.combat-btn-2 {
  background: linear-gradient(135deg, rgba(74, 122, 154, 0.15), rgba(20, 25, 35, 0.8));
  border-color: rgba(74, 122, 154, 0.4);
  color: #d0e0f0;
}
.combat-btn-2:hover {
  border-color: var(--frost);
  background: linear-gradient(135deg, rgba(74, 122, 154, 0.25), rgba(20, 30, 45, 0.9));
  transform: translateX(6px);
  box-shadow: 0 4px 16px rgba(74, 122, 154, 0.15);
}
.combat-btn::before {
  content: "";
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 3px;
  transform: scaleY(0);
  transition: transform 0.25s ease;
}
.combat-btn-0::before { background: var(--crimson-bright); }
.combat-btn-1::before { background: var(--arcane-glow); }
.combat-btn-2::before { background: var(--frost); }
.combat-btn:hover::before { transform: scaleY(1); }
.combat-btn-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  font-family: var(--font-heading);
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  background: linear-gradient(180deg, var(--bg-stone), var(--bg-dark));
  border: 1px solid var(--line-light);
  color: var(--gold-bright);
}
.combat-btn-text { flex: 1; line-height: 1.5; }

.combat-input-row { display: flex; gap: 8px; }
.combat-input {
  flex: 1;
  background: rgba(20, 15, 12, 0.85);
  border: 1px solid var(--line);
  color: var(--ink-bright);
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-family: var(--font-body);
  outline: none;
  transition: var(--transition-base);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.4);
}
.combat-input:focus {
  border-color: var(--gold);
  box-shadow: 0 0 0 3px var(--gold-soft), inset 0 2px 4px rgba(0, 0, 0, 0.4);
}
.combat-input::placeholder { color: var(--muted); font-style: italic; }
.combat-send {
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--gold-dim);
  color: var(--gold-bright);
  width: 48px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 18px;
  transition: var(--transition-base);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.12), 0 2px 8px rgba(0, 0, 0, 0.4);
}
.combat-send:hover:not(:disabled) {
  border-color: var(--gold);
  transform: translateY(-1px);
  box-shadow: 0 0 16px var(--gold-glow);
}
.combat-send:disabled { opacity: 0.4; cursor: not-allowed; }

/* ════════════════════════════════════════════
   DEATH SCREEN
   ════════════════════════════════════════════ */
.death-transition-enter-active { animation: deathFade 1s ease; }
@keyframes deathFade {
  from { opacity: 0; backdrop-filter: blur(0); }
  to { opacity: 1; backdrop-filter: blur(8px); }
}
.death-screen {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}
.death-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at center, rgba(60, 0, 0, 0.85), rgba(0, 0, 0, 0.97));
}
.death-content {
  position: relative;
  text-align: center;
  color: #ccc;
  animation: deathRise 1.2s ease;
}
@keyframes deathRise {
  from { opacity: 0; transform: translateY(40px) scale(0.85); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.death-icon {
  font-size: 90px;
  margin-bottom: 20px;
  filter: drop-shadow(0 0 30px rgba(155, 45, 45, 0.5));
  animation: deathFloat 3s ease-in-out infinite;
}
@keyframes deathFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
.death-title {
  font-family: var(--font-display);
  font-size: 38px;
  color: #9b2d2d;
  margin-bottom: 14px;
  text-shadow: 0 0 30px rgba(155, 45, 45, 0.5);
  letter-spacing: 0.1em;
  font-weight: 700;
}
.death-content p { font-size: 14px; color: var(--muted); margin-bottom: 16px; font-family: var(--font-body); }
.death-stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 28px;
  font-size: 13px;
  color: var(--ink-soft);
  font-family: var(--font-heading);
  letter-spacing: 0.03em;
}
.death-stats span { padding: 2px 0; }
.death-btn {
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--crimson-bright);
  color: var(--crimson-bright);
  padding: 14px 40px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.08em;
  transition: var(--transition-base);
  box-shadow: inset 0 1px 0 rgba(155, 45, 45, 0.1), 0 4px 16px rgba(0, 0, 0, 0.4);
}
.death-btn:hover {
  transform: translateY(-2px);
  border-color: #d32f2f;
  color: #ff6b6b;
  box-shadow: 0 0 24px rgba(155, 45, 45, 0.3);
}

/* ════════════════════════════════════════════
   VICTORY SCREEN
   ════════════════════════════════════════════ */
.combat-arena.victory-pending {
  filter: brightness(0.5) blur(2px);
  pointer-events: none;
}
.victory-transition-enter-active {
  animation: victoryEnter 0.5s cubic-bezier(.34, 1.56, .64, 1);
}
.victory-transition-leave-active {
  animation: victoryEnter 0.3s ease reverse;
}
@keyframes victoryEnter {
  from { opacity: 0; backdrop-filter: blur(0); }
  to { opacity: 1; backdrop-filter: blur(6px); }
}
.victory-overlay {
  position: fixed;
  inset: 0;
  z-index: 150;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(ellipse at center, rgba(30, 22, 8, 0.6), rgba(0, 0, 0, 0.85));
  padding: 20px;
}
.victory-modal {
  background: linear-gradient(135deg, var(--bg-panel), var(--bg-card));
  border: 1px solid var(--gold);
  border-radius: var(--radius-lg);
  padding: 36px 30px 26px;
  max-width: 440px;
  width: 100%;
  box-shadow: 0 0 60px rgba(232, 196, 74, 0.25), 0 20px 60px rgba(0, 0, 0, 0.5);
  animation: victoryRise 0.6s cubic-bezier(.34, 1.56, .64, 1);
  position: relative;
  overflow: hidden;
}
.victory-modal-frame-tl, .victory-modal-frame-tr, .victory-modal-frame-bl, .victory-modal-frame-br {
  position: absolute;
  width: 20px;
  height: 20px;
  z-index: 3;
  pointer-events: none;
}
.victory-modal-frame-tl { top: 6px; left: 6px; border-top: 2px solid var(--gold); border-left: 2px solid var(--gold); border-top-left-radius: 6px; }
.victory-modal-frame-tr { top: 6px; right: 6px; border-top: 2px solid var(--gold); border-right: 2px solid var(--gold); border-top-right-radius: 6px; }
.victory-modal-frame-bl { bottom: 6px; left: 6px; border-bottom: 2px solid var(--gold); border-left: 2px solid var(--gold); border-bottom-left-radius: 6px; }
.victory-modal-frame-br { bottom: 6px; right: 6px; border-bottom: 2px solid var(--gold); border-right: 2px solid var(--gold); border-bottom-right-radius: 6px; }
@keyframes victoryRise {
  from { opacity: 0; transform: translateY(40px) scale(0.9); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.victory-modal::before {
  content: "";
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: conic-gradient(from 0deg, transparent, rgba(232, 196, 74, 0.06), transparent, rgba(232, 196, 74, 0.06), transparent);
  animation: victoryGlow 8s linear infinite;
  pointer-events: none;
}
@keyframes victoryGlow { to { transform: rotate(360deg); } }
.victory-modal > * { position: relative; z-index: 1; }

/* Victory title */
.victory-header { text-align: center; margin-bottom: 24px; }
.victory-icon {
  font-size: 56px;
  filter: drop-shadow(0 0 20px rgba(232, 196, 74, 0.4));
  animation: trophyBounce 1.5s ease-in-out infinite;
}
@keyframes trophyBounce {
  0%, 100% { transform: translateY(0) rotate(0); }
  50% { transform: translateY(-6px) rotate(3deg); }
}
.victory-title {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 700;
  margin: 8px 0 4px;
  background: linear-gradient(135deg, var(--gold), #ffd700, var(--gold-dim));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.08em;
}
.victory-subtitle { margin-top: 6px; }
.levelup-tag {
  display: inline-block;
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 700;
  color: var(--arcane-glow);
  background: var(--arcane-soft);
  border: 1px solid var(--arcane);
  padding: 4px 16px;
  border-radius: 12px;
  animation: levelupPulse 1.5s ease-in-out infinite;
  letter-spacing: 0.05em;
}
@keyframes levelupPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(125, 90, 170, 0.2); }
  50% { box-shadow: 0 0 0 6px rgba(125, 90, 170, 0); }
}

/* XP section */
.victory-section { margin-bottom: 18px; }
.xp-section {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background: linear-gradient(135deg, var(--arcane-soft), transparent);
  border: 1px solid var(--arcane);
  border-radius: var(--radius-md);
  animation: sectionSlideIn 0.5s ease 0.2s backwards;
}
.section-icon {
  font-size: 22px;
  color: var(--arcane-glow);
  filter: drop-shadow(0 0 6px rgba(125, 90, 170, 0.3));
}
.section-content { display: flex; flex-direction: column; gap: 2px; }
.section-label {
  font-family: var(--font-heading);
  font-size: 12px;
  color: var(--muted);
  font-weight: 600;
  letter-spacing: 0.05em;
}
.xp-gained {
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 800;
  color: var(--arcane-glow);
  text-shadow: 0 0 12px rgba(125, 90, 170, 0.3);
}

/* Loot section */
.loot-section { animation: sectionSlideIn 0.5s ease 0.4s backwards; }
.loot-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.loot-header .section-icon { font-size: 18px; color: var(--gold); filter: drop-shadow(0 0 6px var(--gold-glow)); }
.loot-header .section-label { font-size: 13px; color: var(--ink-soft); }
.loot-grid { display: flex; flex-direction: column; gap: 8px; }
.loot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--line);
  background: var(--bg-card);
  animation: lootSlideIn 0.5s cubic-bezier(.34, 1.56, .64, 1) backwards;
  animation-delay: var(--delay, 0s);
  transition: var(--transition-base);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.03);
}
.loot-item:hover { transform: translateX(4px); box-shadow: 0 0 12px var(--gold-glow); }
@keyframes lootSlideIn {
  from { opacity: 0; transform: translateX(-20px); }
  to { opacity: 1; transform: translateX(0); }
}
@keyframes sectionSlideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.loot-icon { font-size: 28px; filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2)); }
.loot-info { display: flex; align-items: center; gap: 8px; flex: 1; }
.loot-name { font-family: var(--font-heading); font-size: 14px; font-weight: 600; color: var(--ink); }
.loot-qty { font-family: var(--font-heading); font-size: 13px; font-weight: 700; color: var(--gold); }

/* Rarity borders */
.rarity-common { border-left: 3px solid var(--muted); }
.rarity-uncommon { border-left: 3px solid var(--nature); }
.rarity-uncommon .loot-name { color: var(--nature); }
.rarity-rare {
  border-left: 3px solid var(--arcane);
  background: linear-gradient(90deg, var(--arcane-soft), var(--bg-card));
}
.rarity-rare .loot-name { color: var(--arcane-glow); }
.rarity-rare .loot-icon { animation: rareGlow 2s ease-in-out infinite; }
@keyframes rareGlow {
  0%, 100% { filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2)); }
  50% { filter: drop-shadow(0 0 10px rgba(125, 90, 170, 0.5)); }
}
.loot-empty { text-align: center; padding: 16px; }
.loot-empty-text { font-family: var(--font-heading); font-size: 13px; color: var(--muted); font-style: italic; }

/* Confirm button */
.victory-confirm-btn {
  width: 100%;
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--gold);
  color: var(--gold-bright);
  padding: 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.08em;
  transition: var(--transition-base);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.15), 0 4px 20px rgba(232, 196, 74, 0.15);
  margin-top: 6px;
  animation: sectionSlideIn 0.5s ease 0.6s backwards;
}
.victory-confirm-btn:hover {
  transform: translateY(-2px);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, 0.25), 0 0 30px rgba(232, 196, 74, 0.3);
  color: var(--ink-bright);
}
.victory-confirm-btn:active { transform: translateY(0); }

/* ── Responsive ── */
@media (max-width: 900px) {
  .explore-stats-float {
    width: 240px;
    top: 72px;
    left: 12px;
  }
  .explore-narration-float {
    width: 300px;
    right: 12px;
    top: 72px;
  }
  .explore-char-portrait {
    height: 55vh;
    right: 1%;
  }
}
@media (max-width: 560px) {
  /* Fullscreen map responsive */
  .explore-stats-float {
    width: calc(100vw - 24px);
    left: 12px;
    top: 64px;
    max-height: 200px;
    overflow-y: auto;
  }
  .explore-narration-float {
    width: calc(100vw - 24px);
    right: 12px;
    top: auto;
    bottom: 120px;
    max-height: 180px;
  }
  .explore-char-portrait {
    height: 40vh;
    opacity: 0.5;
  }
  .explore-scene-label {
    top: 64px;
    left: 12px;
    font-size: 11px;
    padding: 6px 12px;
  }
  .explore-action-bar {
    padding: 12px 12px 14px;
  }
  /* Combat mode */
  .char-name { font-size: 17px; }
  .choice-btn { padding: 10px 14px; font-size: 13px; }
  .enemy-zone { gap: 14px; }
  .enemy-avatar { width: 80px; height: 96px; }
  .enemy-emoji { font-size: 32px; }
  .enemy-figure { min-width: 100px; }
  .combat-log-entry { font-size: 13px; padding: 8px 12px; }
  .death-title { font-size: 30px; }
  .death-icon { font-size: 70px; }
  .combat-main { flex-direction: column; padding: 8px; }
  .combat-main .enemy-zone { padding: 0; }
  .pbb-portrait { width: 44px; height: 56px; }
  .pbb-slots { display: none; }
  .rest-rune-btn { padding: 8px 12px; font-size: 12px; }
  .victory-title { font-size: 24px; }
}

/* ── Inventory toggle button ── */
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}
.inv-toggle-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--line-light);
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  color: var(--gold);
  cursor: pointer;
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.05em;
  transition: all .25s ease;
  box-shadow: inset 0 1px 0 rgba(201,162,39,.08), 0 2px 8px rgba(0,0,0,.3);
}
.inv-toggle-btn:hover {
  border-color: var(--gold);
  color: var(--gold-bright);
  box-shadow: 0 0 16px var(--gold-glow);
  transform: translateY(-1px);
}
.inv-icon { font-size: 16px; filter: drop-shadow(0 0 4px var(--gold-glow)); }
.inv-badge {
  font-family: var(--font-heading);
  font-size: 11px;
  font-weight: 800;
  color: #fff;
  background: var(--crimson);
  padding: 1px 7px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
  box-shadow: 0 0 6px rgba(107, 29, 29, 0.4);
}

/* ── Spell list modal ── */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.spell-list-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  z-index: 270;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}
.spell-list-modal {
  width: 440px;
  max-width: 92vw;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, rgba(30, 22, 18, 0.98), rgba(17, 13, 10, 0.98));
  border: 1px solid var(--arcane);
  border-radius: var(--radius-lg);
  box-shadow: 0 0 40px rgba(93, 58, 138, 0.2), 0 8px 40px rgba(0, 0, 0, 0.5);
  overflow: hidden;
}
.spell-list-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--arcane);
  background: var(--arcane-soft);
}
.spell-list-icon { font-size: 20px; filter: drop-shadow(0 0 6px var(--arcane-glow)); }
.spell-list-header h3 {
  flex: 1;
  margin: 0;
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 700;
  color: var(--arcane-glow);
  letter-spacing: 0.05em;
}
.spell-list-close {
  width: 28px;
  height: 28px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--bg-card);
  color: var(--ink-soft);
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.spell-list-close:hover {
  border-color: var(--crimson-bright);
  color: var(--crimson-bright);
  background: var(--crimson-soft);
}
.spell-list-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.spell-list-empty {
  text-align: center;
  color: var(--muted);
  padding: 40px 0;
  font-family: var(--font-heading);
  font-size: 14px;
}
.spell-card {
  padding: 12px 16px;
  border: 1px solid var(--arcane);
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, var(--arcane-soft), var(--bg-card));
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: inset 0 1px 0 rgba(93, 58, 138, 0.08);
}
.spell-card:hover {
  border-color: var(--arcane-glow);
  background: linear-gradient(135deg, rgba(93, 58, 138, 0.2), var(--bg-hover));
  transform: translateX(4px);
  box-shadow: 0 0 16px rgba(93, 58, 138, 0.2);
}
.spell-card-name {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 700;
  color: var(--arcane-glow);
  margin-bottom: 4px;
}
.spell-card-desc {
  font-family: var(--font-body);
  font-size: 12px;
  color: var(--ink-soft);
  line-height: 1.5;
}
.spell-list-footer {
  padding: 10px 20px;
  border-top: 1px solid var(--arcane);
  display: flex;
  justify-content: flex-end;
}
.spell-cancel-btn {
  padding: 6px 20px;
  border: 1px solid var(--line-light);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--ink-soft);
  font-family: var(--font-heading);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  letter-spacing: 0.05em;
}
.spell-cancel-btn:hover {
  border-color: var(--gold-dim);
  color: var(--gold);
}
</style>
