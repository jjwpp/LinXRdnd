<script setup>
import { computed, ref } from "vue";
import ItemTooltip from "./ItemTooltip.vue";

const props = defineProps({
  item: { type: Object, required: true },
  showUseButton: { type: Boolean, default: false },
  showEquipButton: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
});

const emit = defineEmits(["use", "equip"]);

const showTooltip = ref(false);

// 稀有度样式
const rarityColors = {
  COMMON: { color: "#a89880", glow: "rgba(168,152,128,.15)", border: "#3d2f20" },
  UNCOMMON: { color: "#4caf50", glow: "rgba(76,175,80,.2)", border: "#1b5e20" },
  RARE: { color: "#42a5f5", glow: "rgba(66,165,245,.2)", border: "#0d47a1" },
  EPIC: { color: "#ab47bc", glow: "rgba(171,71,188,.25)", border: "#4a148c" },
  LEGENDARY: { color: "#ffd700", glow: "rgba(255,215,0,.25)", border: "#827717" },
};

const rarityStyle = computed(() => {
  const r = (props.item.rarity || "COMMON").toUpperCase();
  return rarityColors[r] || rarityColors.COMMON;
});

const itemIcon = computed(() => {
  const type = (props.item.itemType || "").toUpperCase();
  const icons = {
    POTION: "🧪", WEAPON: "⚔️", ARMOR: "🛡️", HELMET: "⛑️",
    RING: "💍", AMULET: "📿", SCROLL: "📜", WAND: "🪄",
    MAGIC_ITEM: "💎", CONSUMABLE: "🧪",
  };
  return icons[type] || "📦";
});

const isUsable = computed(() => {
  if (props.disabled) return false;
  const type = (props.item.itemType || "").toUpperCase();
  return type === "POTION" || type === "CONSUMABLE" || type === "SCROLL" || !!props.item.effectType;
});

const isEquippable = computed(() => {
  const type = (props.item.itemType || "").toUpperCase();
  return ["WEAPON", "ARMOR", "HELMET", "RING", "AMULET"].includes(type);
});
</script>

<template>
  <div
    class="item-card relic"
    :class="[`rarity-${(item.rarity || 'COMMON').toLowerCase()}`, { equipped: item.equipped || item.isEquipped, disabled }]"
    :style="{ '--rc': rarityStyle.color, '--rg': rarityStyle.glow, '--rb': rarityStyle.border }"
    @mouseenter="showTooltip = true"
    @mouseleave="showTooltip = false"
  >
    <!-- 稀有度光效边框 -->
    <div class="card-glow"></div>
    <!-- Ornate corner flourishes -->
    <span class="relic-corner tl" aria-hidden="true">✦</span>
    <span class="relic-corner br" aria-hidden="true">✦</span>

    <!-- 物品图标 -->
    <div class="card-icon">{{ itemIcon }}</div>

    <!-- 数量 -->
    <span class="card-qty" v-if="item.quantity > 1">×{{ item.quantity }}</span>

    <!-- 装备标记 -->
    <span class="card-equipped-mark" v-if="item.equipped || item.isEquipped">✓</span>

    <!-- 物品名称 -->
    <div class="card-name" :style="{ color: rarityStyle.color }">{{ item.itemName }}</div>

    <!-- 类型标签 -->
    <div class="card-type">{{ (item.itemType || "ITEM").toUpperCase() }}</div>

    <!-- 操作按钮 -->
    <div class="card-actions" v-if="!disabled">
      <button
        v-if="showUseButton && isUsable"
        class="card-btn use-btn"
        @click.stop="emit('use', item)"
      >
        施用
      </button>
      <button
        v-if="showEquipButton && isEquippable"
        class="card-btn equip-btn"
        @click.stop="emit('equip', item)"
      >
        {{ (item.equipped || item.isEquipped) ? '卸下' : '装备' }}
      </button>
    </div>

    <!-- 悬浮提示 -->
    <Transition name="tooltip-fade">
      <div class="tooltip-wrapper" v-if="showTooltip">
        <ItemTooltip :item="item" />
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.item-card.relic {
  position: relative;
  background:
    radial-gradient(circle at 50% 25%, var(--rg, transparent), transparent 65%),
    linear-gradient(160deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--rb, var(--line));
  border-radius: 6px;
  padding: 10px 8px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: all .25s ease;
  overflow: visible;
  min-height: 100px;
}
.item-card.relic::before {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: 6px;
  background: var(--noise);
  opacity: .04;
  pointer-events: none;
}
.item-card.relic:hover {
  border-color: var(--rc, var(--gold));
  transform: translateY(-3px);
  box-shadow: 0 6px 24px rgba(0, 0, 0, .55), 0 0 22px var(--rg, transparent);
}
.item-card.relic.disabled {
  opacity: .5;
  cursor: not-allowed;
}
.item-card.relic.equipped {
  border-color: var(--nature);
  box-shadow: inset 0 0 14px rgba(74, 122, 58, .18), 0 0 14px rgba(74, 122, 58, .2);
}

/* Ornate corners */
.relic-corner {
  position: absolute;
  font-size: 8px;
  color: var(--rc, var(--gold-dim));
  opacity: .5;
  pointer-events: none;
  transition: opacity .25s ease;
  z-index: 2;
}
.relic-corner.tl { top: 3px; left: 4px; }
.relic-corner.br { bottom: 3px; right: 4px; }
.item-card.relic:hover .relic-corner { opacity: 1; text-shadow: 0 0 6px var(--rg, transparent); }

.card-glow {
  position: absolute;
  inset: -1px;
  border-radius: 6px;
  background: linear-gradient(135deg, var(--rg, transparent), transparent 50%);
  opacity: 0;
  transition: opacity .3s ease;
  pointer-events: none;
  z-index: 0;
}
.item-card.relic:hover .card-glow {
  opacity: 1;
}

.card-icon {
  font-size: 32px;
  filter: drop-shadow(0 2px 8px var(--rg, transparent));
  z-index: 1;
  transition: transform .3s ease;
}
.item-card.relic:hover .card-icon {
  transform: scale(1.18) translateY(-2px);
}

.card-qty {
  position: absolute;
  top: 4px;
  right: 6px;
  font-size: 12px;
  font-weight: 800;
  color: var(--ink-bright);
  background: rgba(0, 0, 0, .65);
  padding: 1px 6px;
  border-radius: 3px;
  z-index: 2;
  border: 1px solid var(--rb, var(--line));
}

.card-equipped-mark {
  position: absolute;
  top: 4px;
  left: 6px;
  font-size: 12px;
  color: var(--nature);
  font-weight: 800;
  z-index: 2;
  text-shadow: 0 0 6px rgba(74, 122, 58, .6);
}

.card-name {
  font-family: var(--font-heading);
  font-size: 11px;
  font-weight: 700;
  text-align: center;
  line-height: 1.3;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  z-index: 1;
  text-shadow: 0 0 8px var(--rg, transparent);
}

.card-type {
  font-size: 8px;
  color: var(--muted);
  letter-spacing: .12em;
  text-transform: uppercase;
  z-index: 1;
  font-family: var(--font-heading);
}

.card-actions {
  display: flex;
  gap: 4px;
  margin-top: 4px;
  z-index: 1;
}
.card-btn {
  font-family: var(--font-heading);
  font-size: 10px;
  padding: 3px 10px;
  border: 1px solid var(--line-light);
  border-radius: 3px;
  background: var(--bg-void);
  color: var(--ink-soft);
  cursor: pointer;
  font-weight: 600;
  letter-spacing: .06em;
  transition: all .2s ease;
}
.use-btn:hover {
  border-color: var(--ember);
  color: var(--ember);
  background: var(--ember-soft);
  box-shadow: 0 0 10px rgba(196, 90, 42, .3);
}
.equip-btn:hover {
  border-color: var(--gold);
  color: var(--gold);
  background: var(--gold-soft);
  box-shadow: 0 0 10px var(--gold-glow);
}

/* Tooltip */
.tooltip-wrapper {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  pointer-events: none;
}
.tooltip-fade-enter-active,
.tooltip-fade-leave-active {
  transition: opacity .2s ease, transform .2s ease;
}
.tooltip-fade-enter-from,
.tooltip-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(4px);
}
</style>
