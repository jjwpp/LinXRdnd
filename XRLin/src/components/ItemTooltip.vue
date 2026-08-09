<script setup>
import { computed } from "vue";

const props = defineProps({
  item: { type: Object, default: null },
});

// 稀有度颜色映射
const rarityColors = {
  COMMON: { color: "#a89880", glow: "rgba(168,152,128,.2)", border: "#5a4a38" },
  UNCOMMON: { color: "#4caf50", glow: "rgba(76,175,80,.25)", border: "#2e7d32" },
  RARE: { color: "#42a5f5", glow: "rgba(66,165,245,.25)", border: "#1565c0" },
  EPIC: { color: "#ab47bc", glow: "rgba(171,71,188,.3)", border: "#6a1b9a" },
  LEGENDARY: { color: "#ffd700", glow: "rgba(255,215,0,.3)", border: "#b8860b" },
};

const effectTypeLabels = {
  HEAL: "回复生命",
  MANA: "回复法力",
  BUFF: "属性增强",
  REMOVE_DEBUFF: "解除异常",
  DAMAGE: "造成伤害",
  SPECIAL: "特殊效果",
};

const itemTypeLabels = {
  POTION: "药水",
  WEAPON: "武器",
  ARMOR: "护甲",
  HELMET: "头盔",
  RING: "戒指",
  AMULET: "项链",
  SCROLL: "卷轴",
  WAND: "魔杖",
  MAGIC_ITEM: "魔法物品",
  consumable: "消耗品",
  weapon: "武器",
  armor: "护甲",
};

const itemIcon = computed(() => {
  if (!props.item) return "📦";
  const type = (props.item.itemType || "").toUpperCase();
  const icons = {
    POTION: "🧪", WEAPON: "⚔️", ARMOR: "🛡️", HELMET: "⛑️",
    RING: "💍", AMULET: "📿", SCROLL: "📜", WAND: "🪄",
    MAGIC_ITEM: "💎", CONSUMABLE: "🧪",
  };
  return icons[type] || "📦";
});

const rarityStyle = computed(() => {
  if (!props.item) return rarityColors.COMMON;
  const rarity = (props.item.rarity || "COMMON").toUpperCase();
  return rarityColors[rarity] || rarityColors.COMMON;
});

const effectLabel = computed(() => {
  if (!props.item?.effectType) return null;
  return effectTypeLabels[props.item.effectType] || props.item.effectType;
});

const typeLabel = computed(() => {
  if (!props.item?.itemType) return "物品";
  return itemTypeLabels[props.item.itemType] || props.item.itemType;
});

// 解析效果详情
const effectDetails = computed(() => {
  if (!props.item?.details) return null;
  try {
    const d = typeof props.item.details === "string"
      ? JSON.parse(props.item.details)
      : props.item.details;
    const parts = [];
    if (d.effectType === "HEAL") {
      parts.push(`回复 ${d.value} HP`);
    } else if (d.effectType === "BUFF") {
      parts.push(`属性 +${d.value}（${d.attribute || "?"}）`);
      if (d.duration) parts.push(`持续 ${d.duration} 回合`);
    } else if (d.effectType === "DAMAGE") {
      if (d.damageDice) parts.push(`伤害 ${d.damageDice}（${d.damageType || "未知"}）`);
      else if (d.value) parts.push(`伤害 ${d.value}（${d.damageType || "未知"}）`);
    } else if (d.effectType === "MANA") {
      parts.push(`恢复 ${d.value || 1} 个法术位`);
    }
    if (d.actionCost) parts.push(`消耗 ${d.actionCost} 行动点`);
    return parts;
  } catch {
    return null;
  }
});
</script>

<template>
  <div
    v-if="item"
    class="item-tooltip arcane-scroll"
    :style="{ '--rarity-color': rarityStyle.color, '--rarity-glow': rarityStyle.glow, '--rarity-border': rarityStyle.border }"
  >
    <span class="scroll-corner tl" aria-hidden="true">✦</span>
    <span class="scroll-corner tr" aria-hidden="true">✦</span>
    <span class="scroll-corner bl" aria-hidden="true">✦</span>
    <span class="scroll-corner br" aria-hidden="true">✦</span>

    <div class="tt-header">
      <span class="tt-icon">{{ itemIcon }}</span>
      <div class="tt-title-area">
        <h4 class="tt-name" :style="{ color: rarityStyle.color }">{{ item.itemName }}</h4>
        <span class="tt-subtitle" v-if="item.subtitle">{{ item.subtitle }}</span>
      </div>
    </div>

    <div class="tt-meta">
      <span class="tt-tag">{{ typeLabel }}</span>
      <span class="tt-tag rarity-tag" :style="{ color: rarityStyle.color, borderColor: rarityStyle.border }">
        {{ (item.rarity || "COMMON").toUpperCase() }}
      </span>
    </div>

    <p class="tt-summary" v-if="item.summary">{{ item.summary }}</p>

    <div class="tt-divider" aria-hidden="true">
      <span class="td-line"></span>
      <span class="td-glyph" :style="{ color: rarityStyle.color }">✦</span>
      <span class="td-line"></span>
    </div>

    <div class="tt-effect" v-if="effectLabel">
      <div class="tt-effect-header">
        <span class="tt-effect-icon">✦</span>
        <span class="tt-effect-label">{{ effectLabel }}</span>
      </div>
      <ul class="tt-effect-list" v-if="effectDetails && effectDetails.length">
        <li v-for="(detail, i) in effectDetails" :key="i">{{ detail }}</li>
      </ul>
    </div>

    <div class="tt-footer" v-if="item.actionCost">
      <span class="tt-action-cost">⚡ 消耗 {{ item.actionCost }} 行动点</span>
    </div>

    <div class="tt-equipped" v-if="item.equipped || item.isEquipped">
      <span class="tt-equipped-tag">✓ 已装备{{ item.slot ? ` · ${item.slot}` : "" }}</span>
    </div>
  </div>
</template>

<style scoped>
.item-tooltip.arcane-scroll {
  width: 280px;
  background:
    linear-gradient(180deg, rgba(42, 31, 23, .97), rgba(20, 14, 10, .98)),
    var(--texture-parchment);
  border: 1px solid var(--rarity-border, #3d2f20);
  border-radius: 6px;
  padding: 16px 14px 14px;
  box-shadow: 0 10px 36px rgba(0, 0, 0, .85), 0 0 24px var(--rarity-glow, transparent);
  font-family: var(--font-body);
  position: relative;
  z-index: 1000;
  animation: scrollReveal .25s ease-out;
}
@keyframes scrollReveal {
  from { opacity: 0; transform: translateY(6px) scale(.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.item-tooltip.arcane-scroll::before {
  content: "";
  position: absolute;
  inset: 3px;
  border: 1px solid var(--rarity-border, var(--line));
  border-radius: 4px;
  opacity: .4;
  pointer-events: none;
}
.item-tooltip.arcane-scroll::after {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: 6px;
  background: var(--noise);
  opacity: .04;
  pointer-events: none;
}

/* Ornate corners */
.scroll-corner {
  position: absolute;
  font-size: 9px;
  color: var(--rarity-color, var(--gold-dim));
  opacity: .7;
  pointer-events: none;
  z-index: 2;
  text-shadow: 0 0 6px var(--rarity-glow, transparent);
}
.scroll-corner.tl { top: 5px; left: 5px; }
.scroll-corner.tr { top: 5px; right: 5px; }
.scroll-corner.bl { bottom: 5px; left: 5px; }
.scroll-corner.br { bottom: 5px; right: 5px; }

.tt-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 10px;
  position: relative;
  z-index: 1;
}
.tt-icon {
  font-size: 30px;
  filter: drop-shadow(0 0 10px var(--rarity-glow, transparent));
  flex-shrink: 0;
}
.tt-title-area {
  flex: 1;
  min-width: 0;
}
.tt-name {
  margin: 0;
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: .04em;
  text-shadow: 0 0 14px var(--rarity-glow, transparent);
}
.tt-subtitle {
  font-size: 11px;
  color: var(--muted);
  font-style: italic;
}

.tt-meta {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
  position: relative;
  z-index: 1;
}
.tt-tag {
  font-family: var(--font-heading);
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 3px;
  background: var(--bg-stone);
  border: 1px solid var(--line);
  color: var(--ink-soft);
  letter-spacing: .06em;
  text-transform: uppercase;
}
.rarity-tag {
  border-width: 1px;
  text-shadow: 0 0 8px var(--rarity-glow, transparent);
}

.tt-summary {
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink);
  margin: 0 0 8px;
  font-style: italic;
  position: relative;
  z-index: 1;
}

/* Ornate divider */
.tt-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0 10px;
  position: relative;
  z-index: 1;
}
.td-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--rarity-border, var(--line-gold)), transparent);
}
.td-glyph {
  font-size: 10px;
}

.tt-effect {
  background: linear-gradient(180deg, var(--bg-void), rgba(11, 9, 8, .6));
  border: 1px solid var(--rarity-border, var(--line));
  border-radius: 4px;
  padding: 8px 10px;
  margin-bottom: 8px;
  position: relative;
  z-index: 1;
}
.tt-effect-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.tt-effect-icon {
  color: var(--rarity-color, var(--gold));
  font-size: 12px;
  text-shadow: 0 0 6px var(--rarity-glow, transparent);
}
.tt-effect-label {
  font-family: var(--font-heading);
  font-size: 11px;
  font-weight: 700;
  color: var(--rarity-color, var(--gold));
  letter-spacing: .06em;
}
.tt-effect-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.tt-effect-list li {
  font-size: 11px;
  color: var(--ink-soft);
  padding: 1px 0;
  padding-left: 12px;
  position: relative;
}
.tt-effect-list li::before {
  content: "◆";
  position: absolute;
  left: 0;
  font-size: 7px;
  top: 5px;
  color: var(--rarity-color, var(--gold));
}

.tt-footer {
  margin-top: 4px;
  position: relative;
  z-index: 1;
}
.tt-action-cost {
  font-size: 11px;
  color: var(--ember);
  font-weight: 600;
  font-family: var(--font-heading);
  letter-spacing: .04em;
}

.tt-equipped {
  margin-top: 6px;
  position: relative;
  z-index: 1;
}
.tt-equipped-tag {
  font-size: 11px;
  color: var(--nature);
  font-weight: 700;
  font-family: var(--font-heading);
  letter-spacing: .04em;
  text-shadow: 0 0 6px rgba(74, 122, 58, .4);
}
</style>
