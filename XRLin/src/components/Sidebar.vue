<script setup>
defineProps({
  categories: Array,
  activeCategory: String,
  totalCount: Number,
  showFavorites: Boolean,
});

defineEmits(["select", "toggleFavorites"]);

const emojis = {
  class: "⚔️",
  race: "🧝",
  spell: "✨",
  monster: "👹",
  "magic-item": "💍",
  feat: "📜",
};
</script>

<template>
  <aside class="sidebar codex-chapters" aria-label="资料分类">
    <div class="chapter-title-block">
      <div class="divider-ornament"><span>❖</span></div>
      <p class="chapter-eyebrow">Index</p>
      <h2 class="chapter-heading text-engraved-gold">卷宗目录</h2>
      <div class="divider-ornament"><span>❖</span></div>
    </div>

    <button
      class="category chapter-tab"
      :class="{ active: activeCategory === '' && !showFavorites }"
      @click="$emit('select', '')"
    >
      <span class="tab-rune">❦</span>
      <span class="tab-label">全部典籍</span>
      <strong class="tab-count">{{ totalCount }}</strong>
    </button>

    <button
      class="fav-filter chapter-bookmark"
      :class="{ active: showFavorites }"
      @click="$emit('toggleFavorites')"
    >
      <span class="bookmark-sigil">✦</span>
      <span class="bookmark-label">珍藏卷轴</span>
    </button>

    <div class="chapter-divider"><span>§</span></div>

    <button
      v-for="cat in categories"
      :key="cat.id"
      class="category chapter-tab"
      :class="[`cat-${cat.id}`, { active: activeCategory === cat.id }]"
      :title="cat.description"
      @click="$emit('select', cat.id)"
    >
      <span class="tab-rune">{{ emojis[cat.id] || '❧' }}</span>
      <span class="tab-label">{{ cat.name }}</span>
      <strong class="tab-count">{{ cat.count }}</strong>
    </button>

    <p class="chapter-footer">— 目录终 —</p>
  </aside>
</template>

<style scoped>
/* ===== Codex chapter sidebar ===== */
.codex-chapters {
  padding: 26px 18px;
  background:
    linear-gradient(180deg, rgba(23, 16, 12, .96), rgba(11, 9, 8, .98)),
    var(--texture-parchment) center / cover;
  position: relative;
}
.codex-chapters::after {
  background: linear-gradient(180deg, transparent, var(--gold-glow) 18%, var(--gold-glow) 82%, transparent);
}

/* Heading block */
.chapter-title-block {
  text-align: center;
  margin-bottom: 18px;
  position: relative;
}
.chapter-title-block .divider-ornament { margin: 2px auto; max-width: 160px; }
.chapter-title-block .divider-ornament span { font-size: 11px; }
.chapter-eyebrow {
  font-family: var(--font-heading);
  font-size: 10px;
  letter-spacing: .4em;
  text-transform: uppercase;
  color: var(--gold-dim);
  margin: 0;
}
.chapter-heading {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 900;
  letter-spacing: .1em;
  margin: 2px 0;
}

/* ===== Chapter bookmark tabs ===== */
.chapter-tab {
  position: relative;
  display: grid;
  grid-template-columns: 22px 1fr auto;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  padding: 12px 14px 12px 12px;
  border: 1px solid var(--line);
  border-left: 3px solid var(--line-light);
  border-radius: 2px 6px 6px 2px;
  background:
    linear-gradient(90deg, rgba(201, 162, 39, .06), transparent 60%),
    linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  color: var(--ink);
  cursor: pointer;
  font-family: var(--font-heading);
  font-size: 13px;
  letter-spacing: .06em;
  text-align: left;
  transition: all var(--transition-base);
  overflow: hidden;
}
/* tassel / bookmark notch on the left edge */
.chapter-tab::before {
  content: "";
  position: absolute;
  left: -3px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  background: var(--gold);
  box-shadow: 0 0 8px var(--gold-glow);
  transition: height var(--transition-base);
}
/* gold trim along the top */
.chapter-tab::after {
  content: "";
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-gold), transparent);
  opacity: .5;
  transition: opacity var(--transition-base);
}
.chapter-tab .tab-rune {
  font-size: 16px;
  color: var(--gold-dim);
  filter: drop-shadow(0 0 4px var(--gold-soft));
  transition: color var(--transition-base), transform var(--transition-base);
}
.chapter-tab .tab-label { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.chapter-tab .tab-count {
  font-family: var(--font-display);
  font-size: 14px;
  color: var(--gold-dim);
  font-weight: 700;
}

.chapter-tab:hover {
  border-color: var(--gold-dim);
  transform: translateX(5px);
  background:
    linear-gradient(90deg, rgba(201, 162, 39, .14), transparent 60%),
    linear-gradient(180deg, var(--bg-card), var(--bg-stone));
}
.chapter-tab:hover::before { height: 60%; }
.chapter-tab:hover::after { opacity: 1; }
.chapter-tab:hover .tab-rune { color: var(--gold); transform: scale(1.1); }

/* Active chapter — glowing bookmark */
.chapter-tab.active {
  border-color: var(--gold);
  border-left-color: var(--gold-bright);
  background:
    linear-gradient(90deg, rgba(201, 162, 39, .2), rgba(201, 162, 39, .04) 70%),
    linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  color: var(--gold-bright);
  font-weight: 700;
  box-shadow:
    inset 0 0 18px rgba(201, 162, 39, .12),
    0 0 18px var(--gold-glow);
}
.chapter-tab.active::before { height: 80%; }
.chapter-tab.active::after { opacity: 1; }
.chapter-tab.active .tab-rune {
  color: var(--gold-bright);
  filter: drop-shadow(0 0 8px var(--gold-glow));
  animation: runePulse 2.4s ease-in-out infinite;
}
.chapter-tab.active .tab-count { color: var(--gold-bright); }
@keyframes runePulse {
  0%, 100% { opacity: .85; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.18); }
}

/* Category-specific accent tassels */
.chapter-tab.cat-class      { border-left-color: var(--crimson-bright); }
.chapter-tab.cat-class.active      { border-left-color: var(--crimson-bright); }
.chapter-tab.cat-class.active::before { background: var(--crimson-bright); box-shadow: 0 0 8px var(--crimson-soft); }

.chapter-tab.cat-race       { border-left-color: var(--gold); }
.chapter-tab.cat-race.active::before { background: var(--gold-bright); }

.chapter-tab.cat-spell      { border-left-color: var(--arcane-glow); }
.chapter-tab.cat-spell.active { box-shadow: inset 0 0 18px rgba(93,58,138,.18), 0 0 18px rgba(93,58,138,.3); }
.chapter-tab.cat-spell.active::before { background: var(--arcane-glow); box-shadow: 0 0 8px var(--arcane-soft); }

.chapter-tab.cat-monster    { border-left-color: var(--nature); }
.chapter-tab.cat-monster.active::before { background: var(--nature); box-shadow: 0 0 8px var(--nature-soft); }

.chapter-tab.cat-magic-item { border-left-color: var(--ember); }
.chapter-tab.cat-magic-item.active::before { background: var(--ember); box-shadow: 0 0 8px var(--ember-soft); }

.chapter-tab.cat-feat       { border-left-color: var(--frost); }
.chapter-tab.cat-feat.active::before { background: var(--frost); box-shadow: 0 0 8px var(--frost-soft); }

/* ===== Favorites bookmark ===== */
.chapter-bookmark {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 11px 14px;
  border: 1px dashed var(--gold-dim);
  border-radius: 2px 6px 6px 2px;
  background: var(--gold-soft);
  color: var(--gold);
  cursor: pointer;
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: 13px;
  letter-spacing: .08em;
  transition: all var(--transition-base);
  position: relative;
}
.chapter-bookmark .bookmark-sigil {
  font-size: 14px;
  filter: drop-shadow(0 0 4px var(--gold-glow));
}
.chapter-bookmark:hover {
  border-style: solid;
  background: rgba(201, 162, 39, .18);
  transform: translateX(4px);
}
.chapter-bookmark.active {
  border-style: solid;
  border-color: var(--gold);
  background: rgba(201, 162, 39, .22);
  color: var(--gold-bright);
  box-shadow: 0 0 16px var(--gold-glow);
}
.chapter-bookmark.active .bookmark-sigil { animation: runePulse 2.4s ease-in-out infinite; }

/* ornamental divider between sections */
.chapter-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 16px 0 14px;
  color: var(--gold-dim);
}
.chapter-divider::before,
.chapter-divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-gold), transparent);
}
.chapter-divider span {
  font-family: var(--font-rune);
  font-size: 12px;
  letter-spacing: .2em;
}

.chapter-footer {
  margin-top: 22px;
  text-align: center;
  font-family: var(--font-uncial);
  font-size: 12px;
  letter-spacing: .25em;
  color: var(--muted);
  font-style: italic;
}

@media (max-width: 920px) {
  .codex-chapters {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    padding: 14px;
    overflow-x: auto;
  }
  .chapter-title-block,
  .chapter-divider,
  .chapter-footer { display: none; }
  .chapter-tab {
    flex: 0 0 auto;
    grid-template-columns: auto auto;
    width: auto;
    margin-bottom: 0;
    white-space: nowrap;
    padding: 8px 12px;
  }
  .chapter-tab .tab-count { display: inline; }
  .chapter-tab:hover { transform: translateY(-2px); }
}
</style>
