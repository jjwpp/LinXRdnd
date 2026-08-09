<script setup>
import { ref, onMounted } from "vue";

defineProps({
  categories: Array,
});

const visible = ref(false);
onMounted(() => requestAnimationFrame(() => (visible.value = true)));

const emojis = {
  class: "⚔️",
  race: "🧝",
  spell: "✨",
  monster: "🐉",
  "magic-item": "💍",
  feat: "📜",
};

const enLabels = {
  class: "CLASS ARCHIVE",
  race: "RACE RECORDS",
  spell: "SPELL GRIMOIRE",
  monster: "BESTIARY",
  "magic-item": "ARMORY",
  feat: "FEAT SCROLLS",
};

const accentColors = {
  class: "var(--crimson-bright)",
  race: "var(--gold)",
  spell: "var(--arcane-glow)",
  monster: "var(--nature)",
  "magic-item": "var(--ember)",
  feat: "var(--frost)",
};
</script>

<template>
  <section class="category-grid" :class="{ visible }">
    <div class="divider-ornament" aria-hidden="true">
      <span>✦ ARCHIVES ✦</span>
    </div>
    <h2 class="section-title tablet-title">六大典籍</h2>
    <p class="tablet-intro">六块远古石板镌刻着世界的知识，触碰其一以开启卷宗</p>

    <div class="category-cards">
      <router-link
        v-for="cat in categories"
        :key="cat.id"
        :to="`/browse?category=${cat.id}`"
        class="cat-card stone-tablet"
        :style="{ '--accent': accentColors[cat.id] || 'var(--gold)' }"
      >
        <!-- Corner flourishes -->
        <span class="tablet-corner tl" aria-hidden="true">✦</span>
        <span class="tablet-corner tr" aria-hidden="true">✦</span>
        <span class="tablet-corner bl" aria-hidden="true">✦</span>
        <span class="tablet-corner br" aria-hidden="true">✦</span>

        <div class="tablet-icon-ring" aria-hidden="true">
          <span class="tablet-icon">{{ emojis[cat.id] || "📦" }}</span>
        </div>
        <span class="cat-en">{{ enLabels[cat.id] || "ARCHIVE" }}</span>
        <h3>{{ cat.name }}</h3>
        <p class="cat-desc">{{ cat.description }}</p>
        <strong class="cat-count">{{ cat.count }}<span class="cat-unit"> 条</span></strong>
        <span class="tablet-enter" aria-hidden="true">⟡ 进入卷宗 ⟡</span>
      </router-link>
    </div>
  </section>
</template>

<style scoped>
/* ===== Section heading ===== */
.tablet-title {
  text-align: center;
  font-size: 26px;
}
.tablet-title::after {
  left: 50%;
  transform: translateX(-50%);
}
.tablet-intro {
  text-align: center;
  font-family: var(--font-body);
  font-style: italic;
  color: var(--muted);
  font-size: 14px;
  margin: -10px 0 30px;
  letter-spacing: .04em;
}

/* ===== Stone tablet card ===== */
.category-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 22px;
}

.stone-tablet {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 34px 22px 26px;
  position: relative;
  overflow: hidden;
  text-decoration: none;
  color: var(--ink);
  border-radius: 10px;
  /* Carved stone slab */
  background:
    linear-gradient(180deg, var(--bg-card) 0%, var(--bg-stone) 55%, #120c08 100%);
  border: 1px solid var(--line);
  box-shadow:
    var(--shadow-card),
    inset 0 1px 0 rgba(255, 255, 255, .04),
    inset 0 -3px 8px rgba(0, 0, 0, .5),
    inset 0 0 30px rgba(0, 0, 0, .35);
  transition: all var(--transition-base);
  isolation: isolate;
}

/* Engraved top band */
.stone-tablet::before {
  content: "";
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 4px;
  background: linear-gradient(90deg, transparent, var(--accent, var(--gold)) 30%, var(--accent, var(--gold)) 70%, transparent);
  box-shadow: 0 0 12px var(--accent, var(--gold));
  transition: height var(--transition-base), opacity var(--transition-base);
}

/* Inner ornate border */
.stone-tablet::after {
  content: "";
  position: absolute;
  inset: 10px;
  border: 1px solid transparent;
  border-radius: 6px;
  transition: border-color var(--transition-base);
  pointer-events: none;
  z-index: 1;
}

/* Corner flourishes */
.tablet-corner {
  position: absolute;
  font-size: 11px;
  color: var(--accent, var(--gold-dim));
  opacity: .35;
  transition: opacity var(--transition-base), text-shadow var(--transition-base);
  z-index: 2;
  pointer-events: none;
}
.tablet-corner.tl { top: 14px; left: 14px; }
.tablet-corner.tr { top: 14px; right: 14px; }
.tablet-corner.bl { bottom: 14px; left: 14px; }
.tablet-corner.br { bottom: 14px; right: 14px; }

.stone-tablet:hover {
  transform: translateY(-7px);
  border-color: var(--accent, var(--gold));
  box-shadow:
    var(--shadow-deep),
    0 0 36px var(--accent, var(--gold-glow)),
    inset 0 1px 0 rgba(255, 255, 255, .06),
    inset 0 0 30px rgba(0, 0, 0, .3);
}
.stone-tablet:hover::before { height: 6px; }
.stone-tablet:hover::after { border-color: var(--line-gold); }
.stone-tablet:hover .tablet-corner {
  opacity: 1;
  text-shadow: 0 0 8px var(--accent, var(--gold));
}

/* Engraved icon inside a rune ring */
.tablet-icon-ring {
  position: relative;
  width: 78px;
  height: 78px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  border-radius: 50%;
  border: 1px solid var(--line-gold);
  background: radial-gradient(circle, rgba(201, 162, 39, .08), transparent 70%);
  z-index: 2;
}
.tablet-icon-ring::before {
  content: "";
  position: absolute;
  inset: -5px;
  border: 1px dashed var(--line);
  border-radius: 50%;
  opacity: .5;
  transition: opacity var(--transition-base), transform var(--transition-slow);
}
.stone-tablet:hover .tablet-icon-ring::before {
  opacity: 1;
  transform: rotate(45deg);
}
.tablet-icon {
  font-size: 38px;
  filter: drop-shadow(0 0 10px var(--accent, var(--gold-glow)));
  transition: transform var(--transition-base), filter var(--transition-base);
}
.stone-tablet:hover .tablet-icon {
  transform: scale(1.18);
  filter: drop-shadow(0 0 16px var(--accent, var(--gold)));
}

.cat-en {
  font-family: var(--font-heading);
  font-size: 10px;
  color: var(--accent, var(--gold-dim));
  letter-spacing: .24em;
  text-transform: uppercase;
  margin-bottom: 8px;
  opacity: .65;
  transition: opacity var(--transition-base);
  z-index: 2;
}
.stone-tablet:hover .cat-en { opacity: 1; }

.stone-tablet h3 {
  font-family: var(--font-heading);
  font-size: 19px;
  margin-bottom: 8px;
  color: var(--ink-bright);
  letter-spacing: .08em;
  text-shadow: 0 -1px 0 rgba(0, 0, 0, .8), 0 1px 0 rgba(201, 162, 39, .12);
  z-index: 2;
}

.cat-desc {
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 16px;
  line-height: 1.6;
  font-style: italic;
  z-index: 2;
}

.cat-count {
  font-family: var(--font-display);
  font-size: 26px;
  color: var(--accent, var(--gold));
  font-weight: 700;
  text-shadow: 0 0 18px var(--accent, var(--gold-glow));
  z-index: 2;
}
.cat-unit {
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 400;
  color: var(--muted);
  margin-left: 2px;
  text-shadow: none;
}

.tablet-enter {
  display: block;
  margin-top: 14px;
  font-family: var(--font-heading);
  font-size: 11px;
  letter-spacing: .18em;
  color: var(--gold-dim);
  opacity: 0;
  transform: translateY(6px);
  transition: opacity var(--transition-base), transform var(--transition-base), color var(--transition-base);
  z-index: 2;
}
.stone-tablet:hover .tablet-enter {
  opacity: 1;
  transform: translateY(0);
  color: var(--accent, var(--gold));
}

@media (max-width: 560px) {
  .category-cards { grid-template-columns: repeat(2, 1fr); gap: 14px; }
  .stone-tablet { padding: 26px 14px 22px; }
  .tablet-icon-ring { width: 64px; height: 64px; }
  .tablet-icon { font-size: 30px; }
  .stone-tablet h3 { font-size: 16px; }
  .tablet-enter { display: none; }
}
</style>
