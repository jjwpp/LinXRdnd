<script setup>
import { ref, computed, watch, onMounted } from "vue";
import ItemCard from "./ItemCard.vue";
import ItemTooltip from "./ItemTooltip.vue";

const props = defineProps({
  visible: { type: Boolean, default: false },
  characterId: { type: String, default: "" },
  characterName: { type: String, default: "冒险者" },
  sessionId: { type: String, default: "" },
  // 战斗模式：显示行动点，限制使用
  inCombat: { type: Boolean, default: false },
  actionPoints: { type: Number, default: 1 },
  // 从 SSE 同步的物品列表（可选，优先于 API 拉取）
  syncItems: { type: Array, default: null },
});

const emit = defineEmits(["close", "use-item", "equip-item", "item-used"]);

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080/api";

// ── 状态 ──
const items = ref([]);
const loading = ref(false);
const errorMsg = ref("");
const filterType = ref("ALL"); // ALL / POTION / WEAPON / ARMOR / SCROLL / MAGIC_ITEM
const sortBy = ref("rarity"); // rarity / type / name

// ── 稀有度排序权重 ──
const rarityWeight = {
  LEGENDARY: 5, EPIC: 4, RARE: 3, UNCOMMON: 2, COMMON: 1,
};

// ── 物品加载 ──
async function loadInventory() {
  // 如果有同步物品，优先使用
  if (props.syncItems && props.syncItems.length >= 0) {
    items.value = (props.syncItems || []).map(normalizeItem);
    return;
  }
  if (!props.characterId) return;

  loading.value = true;
  errorMsg.value = "";
  try {
    const token = localStorage.getItem("auth_token");
    const res = await fetch(`${API_BASE}/inventory/${props.characterId}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    const json = await res.json();
    const data = json.data || json;
    if (data && data.items) {
      items.value = data.items.map(normalizeItem);
    } else {
      items.value = [];
    }
  } catch (e) {
    errorMsg.value = "加载背包失败: " + e.message;
    items.value = [];
  } finally {
    loading.value = false;
  }
}

// ── 物品字段规范化 ──
function normalizeItem(raw) {
  const item = { ...raw };
  // 统一字段名
  if (item.isEquipped !== undefined) item.equipped = item.isEquipped;
  if (!item.itemName && item.name) item.itemName = raw.name;
  // 解析 details JSON 提取效果信息
  if (item.details && typeof item.details === "string") {
    try {
      const d = JSON.parse(item.details);
      item.effectType = d.effectType || null;
      item.actionCost = d.actionCost || 1;
      item.parsedDetails = d;
    } catch {}
  } else if (item.details && typeof item.details === "object") {
    item.effectType = item.details.effectType || null;
    item.actionCost = item.details.actionCost || 1;
    item.parsedDetails = item.details;
  }
  return item;
}

// ── 过滤和排序 ──
const filteredItems = computed(() => {
  let list = [...items.value];
  if (filterType.value !== "ALL") {
    list = list.filter(i => (i.itemType || "").toUpperCase() === filterType.value);
  }
  // 排序
  list.sort((a, b) => {
    if (sortBy.value === "rarity") {
      const ra = rarityWeight[(a.rarity || "COMMON").toUpperCase()] || 0;
      const rb = rarityWeight[(b.rarity || "COMMON").toUpperCase()] || 0;
      return rb - ra;
    }
    if (sortBy.value === "type") {
      return (a.itemType || "").localeCompare(b.itemType || "");
    }
    if (sortBy.value === "name") {
      return (a.itemName || "").localeCompare(b.itemName || "");
    }
    return 0;
  });
  // 已装备的排前面
  list.sort((a, b) => {
    const ea = a.equipped || a.isEquipped ? 1 : 0;
    const eb = b.equipped || b.isEquipped ? 1 : 0;
    return eb - ea;
  });
  return list;
});

// ── 统计信息 ──
const totalItems = computed(() => {
  return items.value.reduce((sum, i) => sum + (i.quantity || 1), 0);
});

const equippedCount = computed(() => {
  return items.value.filter(i => i.equipped || i.isEquipped).length;
});

const maxSlots = 30;

const weightPercent = computed(() => {
  return Math.min(100, (totalItems.value / maxSlots) * 100);
});

// ── 过滤器配置 ──
const filterTabs = [
  { key: "ALL", label: "全部", icon: "📦" },
  { key: "POTION", label: "药水", icon: "🧪" },
  { key: "WEAPON", label: "武器", icon: "⚔️" },
  { key: "ARMOR", label: "护甲", icon: "🛡️" },
  { key: "SCROLL", label: "卷轴", icon: "📜" },
  { key: "MAGIC_ITEM", label: "奇物", icon: "💎" },
];

// ── 物品操作 ──
async function handleUseItem(item) {
  if (loading.value) return;
  loading.value = true;
  errorMsg.value = "";

  try {
    const token = localStorage.getItem("auth_token");
    const res = await fetch(`${API_BASE}/inventory/use`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({
        characterId: props.characterId,
        itemId: item.itemId || item.id,
        battleId: props.sessionId || null,
      }),
    });
    const json = await res.json();
    const data = json.data || json;

    if (json.code && json.code !== 200) {
      errorMsg.value = json.msg || "使用失败";
      return;
    }

    // 更新本地数量
    const idx = items.value.findIndex(i => (i.itemId || i.id) === (item.itemId || item.id));
    if (idx >= 0) {
      if (data.remainingQuantity !== undefined) {
        items.value[idx].quantity = data.remainingQuantity;
      } else {
        items.value[idx].quantity = Math.max(0, (items.value[idx].quantity || 1) - 1);
      }
      // 如果数量为0，移除
      if (items.value[idx].quantity <= 0) {
        items.value.splice(idx, 1);
      }
    }

    // 通知父组件
    emit("item-used", {
      item,
      result: data,
      remainingQuantity: data.remainingQuantity,
    });
  } catch (e) {
    errorMsg.value = "使用物品失败: " + e.message;
  } finally {
    loading.value = false;
  }
}

async function handleEquipItem(item) {
  if (loading.value) return;
  loading.value = true;
  errorMsg.value = "";

  try {
    const token = localStorage.getItem("auth_token");
    const isEquipped = item.equipped || item.isEquipped;
    const slot = item.slot || inferSlotFromType(item.itemType);

    const res = await fetch(`${API_BASE}/inventory/equip`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({
        characterId: props.characterId,
        itemId: item.itemId || item.id,
        slot,
        equip: !isEquipped,
      }),
    });
    const json = await res.json();
    const data = json.data || json;

    if (json.code && json.code !== 200) {
      errorMsg.value = json.msg || "装备失败";
      return;
    }

    // 更新本地装备状态
    // 先卸下同槽位的其他物品
    if (!isEquipped) {
      items.value.forEach(i => {
        if ((i.itemId || i.id) !== (item.itemId || item.id) && (i.equipped || i.isEquipped) && i.slot === slot) {
          i.equipped = false;
          i.isEquipped = false;
          i.slot = null;
        }
      });
    }
    // 切换目标物品
    const idx = items.value.findIndex(i => (i.itemId || i.id) === (item.itemId || item.id));
    if (idx >= 0) {
      items.value[idx].equipped = !isEquipped;
      items.value[idx].isEquipped = !isEquipped;
      items.value[idx].slot = !isEquipped ? slot : null;
    }

    emit("equip-item", { item, equipped: !isEquipped, slot });
  } catch (e) {
    errorMsg.value = "装备失败: " + e.message;
  } finally {
    loading.value = false;
  }
}

function inferSlotFromType(type) {
  const t = (type || "").toUpperCase();
  if (t === "WEAPON") return "WEAPON";
  if (t === "ARMOR") return "ARMOR";
  if (t === "HELMET") return "HELMET";
  if (t === "RING") return "RING";
  if (t === "AMULET") return "AMULET";
  return "WEAPON";
}

// ── 监听 ──
watch(() => props.visible, (val) => {
  if (val) loadInventory();
});

watch(() => props.syncItems, () => {
  if (props.syncItems) loadInventory();
}, { deep: true });

// ── 初始化 ──
onMounted(() => {
  if (props.visible) loadInventory();
});

// ── 关闭面板 ──
function handleClose() {
  emit("close");
}
</script>

<template>
  <Teleport to="body">
    <Transition name="inv-panel">
      <div v-if="visible" class="inv-overlay" @click.self="handleClose">
        <div class="inv-window">
          <!-- ═══ 装饰边框 ═══ -->
          <div class="inv-frame-tl"></div>
          <div class="inv-frame-tr"></div>
          <div class="inv-frame-bl"></div>
          <div class="inv-frame-br"></div>

          <!-- ═══ 标题栏 ═══ -->
          <div class="inv-title-bar">
            <div class="inv-title-left">
              <span class="inv-title-icon">🎒</span>
              <div class="inv-title-text">
                <h2 class="inv-title">冒险者背包</h2>
                <span class="inv-subtitle">Adventurer's Pack</span>
              </div>
            </div>
            <button class="inv-close-btn" @click="handleClose">
              <span>×</span>
            </button>
          </div>

          <!-- ═══ 角色信息栏 ═══ -->
          <div class="inv-char-bar">
            <div class="inv-char-info">
              <span class="inv-char-name">{{ characterName }}</span>
              <span class="inv-char-divider">·</span>
              <span class="inv-char-load">
                负重 <strong>{{ totalItems }}</strong> / {{ maxSlots }}
              </span>
            </div>
            <!-- 负重条 -->
            <div class="inv-load-bar">
              <div class="inv-load-fill" :style="{ width: weightPercent + '%' }"></div>
            </div>
            <!-- 战斗模式行动点 -->
            <div v-if="inCombat" class="inv-ap-display">
              <span class="ap-label">行动点</span>
              <span class="ap-value" :class="{ depleted: actionPoints <= 0 }">
                ⚡ {{ actionPoints }}
              </span>
            </div>
            <!-- 装备计数 -->
            <div class="inv-equipped-count" v-if="equippedCount > 0">
              <span>✓ 已装备 {{ equippedCount }}</span>
            </div>
          </div>

          <!-- ═══ 错误提示 ═══ -->
          <Transition name="fade">
            <div v-if="errorMsg" class="inv-error">
              ⚠️ {{ errorMsg }}
              <button @click="errorMsg = ''" class="inv-error-dismiss">×</button>
            </div>
          </Transition>

          <!-- ═══ 过滤器 ─══ -->
          <div class="inv-filter-bar">
            <button
              v-for="tab in filterTabs"
              :key="tab.key"
              class="inv-filter-tab"
              :class="{ active: filterType === tab.key }"
              @click="filterType = tab.key"
            >
              <span class="tab-icon">{{ tab.icon }}</span>
              <span class="tab-label">{{ tab.label }}</span>
            </button>
            <div class="inv-sort">
              <select v-model="sortBy" class="inv-sort-select">
                <option value="rarity">按稀有度</option>
                <option value="type">按类型</option>
                <option value="name">按名称</option>
              </select>
            </div>
          </div>

          <!-- ═══ 物品网格 ═══ -->
          <div class="inv-grid-area">
            <div v-if="loading" class="inv-loading">
              <span class="inv-loading-orb"></span>
              <span>正在整理背包...</span>
            </div>

            <div v-else-if="filteredItems.length === 0" class="inv-empty">
              <div class="inv-empty-icon">📜</div>
              <p class="inv-empty-text">
                {{ items.length === 0 ? "背包空空如也" : "此分类下没有物品" }}
              </p>
              <p class="inv-empty-hint" v-if="items.length === 0">
                击败怪物或在冒险中发现宝藏来填满你的背包
              </p>
            </div>

            <div v-else class="inv-grid">
              <ItemCard
                v-for="item in filteredItems"
                :key="item.id || item.itemId"
                :item="item"
                :show-use-button="true"
                :show-equip-button="true"
                :disabled="loading"
                @use="handleUseItem"
                @equip="handleEquipItem"
              />
            </div>
          </div>

          <!-- ═══ 底部提示 ═══ -->
          <div class="inv-footer">
            <span class="inv-footer-hint">悬停查看详情 · 点击使用或装备</span>
            <span class="inv-footer-count">{{ filteredItems.length }} 件物品</span>
          </div>

          <!-- ═══ 魔法粒子装饰 ═══ -->
          <div class="inv-particles">
            <span v-for="n in 5" :key="n" class="inv-particle" :style="{ '--n': n }"></span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ═══ 遮罩层 ═══ */
.inv-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(2px);
  z-index: 9000;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ═══ 背包主窗口 ═══ */
.inv-window {
  position: relative;
  width: 600px;
  max-width: 95vw;
  height: 500px;
  max-height: 85vh;
  background: linear-gradient(160deg, #1a1310 0%, #0f0a08 100%);
  border: 2px solid #3d2f20;
  border-radius: 8px;
  box-shadow:
    0 0 0 1px #1a1310,
    0 0 0 3px #c9a227,
    0 0 0 4px #1a1310,
    0 20px 60px rgba(0, 0, 0, 0.8),
    0 0 40px rgba(201, 162, 39, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: var(--font-body, "Alegreya Sans", sans-serif);
}

/* 羊皮卷纹理 */
.inv-window::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 30%, rgba(101, 67, 33, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 70%, rgba(93, 58, 138, 0.06) 0%, transparent 50%),
    repeating-linear-gradient(
      45deg,
      transparent,
      transparent 2px,
      rgba(201, 162, 39, 0.01) 2px,
      rgba(201, 162, 39, 0.01) 4px
    );
  pointer-events: none;
  z-index: 0;
}

/* ═══ 装饰角框 ═══ */
.inv-frame-tl, .inv-frame-tr, .inv-frame-bl, .inv-frame-br {
  position: absolute;
  width: 24px;
  height: 24px;
  z-index: 2;
  pointer-events: none;
}
.inv-frame-tl { top: 4px; left: 4px; border-top: 2px solid #c9a227; border-left: 2px solid #c9a227; border-top-left-radius: 6px; }
.inv-frame-tr { top: 4px; right: 4px; border-top: 2px solid #c9a227; border-right: 2px solid #c9a227; border-top-right-radius: 6px; }
.inv-frame-bl { bottom: 4px; left: 4px; border-bottom: 2px solid #c9a227; border-left: 2px solid #c9a227; border-bottom-left-radius: 6px; }
.inv-frame-br { bottom: 4px; right: 4px; border-bottom: 2px solid #c9a227; border-right: 2px solid #c9a227; border-bottom-right-radius: 6px; }

/* ═══ 标题栏 ═══ */
.inv-title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px 12px;
  border-bottom: 1px solid rgba(201, 162, 39, 0.2);
  background: linear-gradient(180deg, rgba(201, 162, 39, 0.05) 0%, transparent 100%);
  position: relative;
  z-index: 1;
  flex-shrink: 0;
}
.inv-title-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.inv-title-icon {
  font-size: 28px;
  filter: drop-shadow(0 0 10px rgba(201, 162, 39, 0.4));
}
.inv-title {
  margin: 0;
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 18px;
  font-weight: 700;
  color: #c9a227;
  letter-spacing: 0.05em;
  text-shadow: 0 0 12px rgba(201, 162, 39, 0.3);
}
.inv-subtitle {
  font-size: 10px;
  color: #6b5a44;
  letter-spacing: 0.15em;
  text-transform: uppercase;
}
.inv-close-btn {
  width: 28px;
  height: 28px;
  border: 1px solid rgba(201, 162, 39, 0.3);
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.4);
  color: #8a7560;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  line-height: 1;
  padding: 0;
}
.inv-close-btn:hover {
  border-color: #c9a227;
  color: #c9a227;
  background: rgba(201, 162, 39, 0.1);
  box-shadow: 0 0 10px rgba(201, 162, 39, 0.2);
}

/* ═══ 角色信息栏 ═══ */
.inv-char-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 20px;
  border-bottom: 1px solid rgba(61, 47, 32, 0.5);
  background: rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 1;
  flex-shrink: 0;
}
.inv-char-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.inv-char-name {
  font-family: var(--font-heading, "Cinzel", serif);
  font-weight: 600;
  color: #d4c4a0;
  letter-spacing: 0.03em;
}
.inv-char-divider { color: #4a3d2e; }
.inv-char-load {
  color: #8a7560;
  font-size: 12px;
}
.inv-char-load strong { color: #c9a227; }

.inv-load-bar {
  flex: 1;
  height: 4px;
  background: rgba(0, 0, 0, 0.4);
  border-radius: 2px;
  overflow: hidden;
  max-width: 120px;
}
.inv-load-fill {
  height: 100%;
  background: linear-gradient(90deg, #c9a227, #ffd700);
  border-radius: 2px;
  transition: width 0.3s ease;
  box-shadow: 0 0 6px rgba(201, 162, 39, 0.3);
}

.inv-ap-display {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}
.ap-label { color: #6b5a44; }
.ap-value {
  color: #ff9800;
  font-weight: 700;
  font-size: 14px;
}
.ap-value.depleted { color: #5a4a38; }

.inv-equipped-count {
  font-size: 11px;
  color: #4caf50;
  letter-spacing: 0.03em;
}

/* ═══ 错误提示 ═══ */
.inv-error {
  margin: 8px 20px 0;
  padding: 8px 12px;
  background: rgba(107, 29, 29, 0.3);
  border: 1px solid #6b1d1d;
  border-radius: 4px;
  color: #ef5350;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
  flex-shrink: 0;
}
.inv-error-dismiss {
  background: none;
  border: none;
  color: #ef5350;
  cursor: pointer;
  font-size: 16px;
  padding: 0 4px;
}

/* ═══ 过滤栏 ═══ */
.inv-filter-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 20px;
  border-bottom: 1px solid rgba(61, 47, 32, 0.5);
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  overflow-x: auto;
}
.inv-filter-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 3px;
  color: #6b5a44;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  font-family: inherit;
}
.inv-filter-tab:hover {
  color: #c9a227;
  border-color: rgba(201, 162, 39, 0.2);
}
.inv-filter-tab.active {
  color: #c9a227;
  border-color: rgba(201, 162, 39, 0.5);
  background: rgba(201, 162, 39, 0.08);
  box-shadow: 0 0 8px rgba(201, 162, 39, 0.1);
}
.tab-icon { font-size: 13px; }
.tab-label { font-weight: 600; letter-spacing: 0.03em; }

.inv-sort {
  margin-left: auto;
}
.inv-sort-select {
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(201, 162, 39, 0.2);
  border-radius: 3px;
  color: #8a7560;
  font-size: 11px;
  padding: 3px 8px;
  cursor: pointer;
  font-family: inherit;
  outline: none;
}
.inv-sort-select:focus {
  border-color: rgba(201, 162, 39, 0.5);
}
.inv-sort-select option {
  background: #1a1310;
  color: #d4c4a0;
}

/* ═══ 物品网格区 ═══ */
.inv-grid-area {
  flex: 1;
  overflow-y: auto;
  padding: 12px 20px;
  position: relative;
  z-index: 1;
}

/* 自定义滚动条 */
.inv-grid-area::-webkit-scrollbar {
  width: 6px;
}
.inv-grid-area::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.3);
}
.inv-grid-area::-webkit-scrollbar-thumb {
  background: rgba(201, 162, 39, 0.3);
  border-radius: 3px;
}
.inv-grid-area::-webkit-scrollbar-thumb:hover {
  background: rgba(201, 162, 39, 0.5);
}

.inv-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 8px;
}

/* ═══ 加载状态 ═══ */
.inv-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: #6b5a44;
  font-size: 13px;
}
.inv-loading-orb {
  width: 40px;
  height: 40px;
  border: 2px solid rgba(201, 162, 39, 0.2);
  border-top-color: #c9a227;
  border-radius: 50%;
  animation: invSpin 1s linear infinite;
}
@keyframes invSpin {
  to { transform: rotate(360deg); }
}

/* ═══ 空状态 ═══ */
.inv-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
  text-align: center;
}
.inv-empty-icon {
  font-size: 48px;
  opacity: 0.3;
  filter: grayscale(50%);
}
.inv-empty-text {
  font-family: var(--font-heading, "Cinzel", serif);
  font-size: 14px;
  color: #6b5a44;
  margin: 0;
  letter-spacing: 0.05em;
}
.inv-empty-hint {
  font-size: 11px;
  color: #4a3d2e;
  margin: 0;
  font-style: italic;
  max-width: 280px;
}

/* ═══ 底部 ═══ */
.inv-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 20px;
  border-top: 1px solid rgba(61, 47, 32, 0.5);
  background: rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 1;
  flex-shrink: 0;
}
.inv-footer-hint {
  font-size: 10px;
  color: #4a3d2e;
  letter-spacing: 0.03em;
}
.inv-footer-count {
  font-size: 10px;
  color: #6b5a44;
  font-weight: 600;
}

/* ═══ 魔法粒子 ═══ */
.inv-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}
.inv-particle {
  position: absolute;
  width: 3px;
  height: 3px;
  background: #c9a227;
  border-radius: 50%;
  opacity: 0;
  box-shadow: 0 0 6px rgba(201, 162, 39, 0.6);
  animation: invFloat 6s ease-in-out infinite;
  animation-delay: calc(var(--n) * 1.2s);
  left: calc(10% + var(--n) * 20%);
  bottom: 0;
}
@keyframes invFloat {
  0%, 100% { opacity: 0; transform: translateY(0); }
  50% { opacity: 0.4; transform: translateY(-400px); }
}

/* ═══ 面板动画 ═══ */
.inv-panel-enter-active {
  transition: opacity 0.25s ease;
}
.inv-panel-enter-active .inv-window {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), opacity 0.3s ease;
}
.inv-panel-enter-from {
  opacity: 0;
}
.inv-panel-enter-from .inv-window {
  transform: scale(0.85) translateY(20px);
  opacity: 0;
}
.inv-panel-leave-active {
  transition: opacity 0.2s ease;
}
.inv-panel-leave-active .inv-window {
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.inv-panel-leave-to {
  opacity: 0;
}
.inv-panel-leave-to .inv-window {
  transform: scale(0.9);
  opacity: 0;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* ═══ 响应式 ═══ */
@media (max-width: 640px) {
  .inv-window {
    width: 95vw;
    height: 80vh;
  }
  .inv-grid {
    grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
    gap: 6px;
  }
  .inv-filter-bar {
    padding: 8px 12px;
  }
  .inv-grid-area {
    padding: 8px 12px;
  }
  .inv-title-bar {
    padding: 10px 14px;
  }
  .inv-char-bar {
    padding: 6px 14px;
  }
  .inv-footer {
    padding: 6px 14px;
  }
}
</style>
