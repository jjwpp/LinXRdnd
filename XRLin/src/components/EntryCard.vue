<script setup>
import classImages from "../composables/useClassImages";
import monsterImages from "../composables/useMonsterImages";

const props = defineProps({
  entry: Object,
  isActive: Boolean,
  categoryLabel: String,
});

defineEmits(["click", "dblclick"]);

function getEntryImage(entry) {
  if (entry.category === "class") return classImages[entry.id];
  if (entry.category === "monster") {
    // 优先使用后端返回的 MinIO URL
    if (entry.imageUrl) return entry.imageUrl;
    return monsterImages[entry.id];
  }
  return null;
}
</script>

<template>
  <button
    class="card codex-card corner-flourish"
    :class="{ active: isActive, 'card-with-portrait': getEntryImage(entry) }"
    :data-category="entry.category"
    @click="$emit('click', entry)"
    @dblclick="$emit('dblclick', entry)"
  >
    <!-- ornate top edge -->
    <span class="card-crest" aria-hidden="true">✦</span>

    <!-- 立绘图（职业/怪物） -->
    <div v-if="getEntryImage(entry)" class="entry-portrait">
      <img :src="getEntryImage(entry)" :alt="entry.name" loading="lazy" />
      <div class="entry-portrait-overlay"></div>
      <span class="portrait-frame-rune tl">❦</span>
      <span class="portrait-frame-rune br">❦</span>
    </div>

    <div class="codex-card-body">
      <h3 class="codex-card-name">{{ entry.name }}</h3>
      <p class="card-meta">
        <strong>{{ categoryLabel }}</strong> · {{ entry.subtitle }}
      </p>
      <p class="card-summary">{{ entry.summary }}</p>
      <div class="tags">
        <span v-for="tag in entry.tags" :key="tag" class="tag" :class="`tag-${entry.category}`">{{ tag }}</span>
      </div>
    </div>

    <!-- ornate bottom flourish -->
    <span class="card-foot-ornament" aria-hidden="true">❧</span>
  </button>
</template>

<style scoped>
/* ===== Parchment scroll entry card ===== */
.codex-card {
  padding: 20px 18px 18px;
  background:
    linear-gradient(180deg, rgba(201, 162, 39, .05) 0%, transparent 30%),
    linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  position: relative;
  overflow: hidden;
  transition: all var(--transition-base);
}
/* double ornate inner border */
.codex-card::before {
  content: "";
  position: absolute;
  inset: 5px;
  border: 1px solid transparent;
  border-radius: 2px;
  pointer-events: none;
  transition: border-color var(--transition-base);
}
/* warm parchment wash */
.codex-card::after {
  background:
    linear-gradient(180deg, rgba(201, 162, 39, .06) 0%, transparent 35%),
    radial-gradient(circle at 50% 0%, rgba(201, 162, 39, .04), transparent 60%);
  pointer-events: none;
}

/* corner flourishes (override global positioning for our corners) */
.codex-card.corner-flourish::before { /* keep transparent inner; flourishes below */ }
.codex-card .card-crest,
.codex-card .card-foot-ornament {
  position: absolute;
  z-index: 2;
  font-size: 11px;
  color: var(--gold-dim);
  pointer-events: none;
  transition: color var(--transition-base), text-shadow var(--transition-base);
}
.codex-card .card-crest {
  top: 7px;
  left: 50%;
  transform: translateX(-50%);
}
.codex-card .card-foot-ornament {
  bottom: 6px;
  left: 50%;
  transform: translateX(-50%);
}

.codex-card-body { position: relative; z-index: 1; }

.codex-card-name {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 6px;
  color: var(--ink-bright);
  letter-spacing: .03em;
  text-shadow: 0 1px 3px rgba(0, 0, 0, .5);
}

/* portrait frame flourishes */
.entry-portrait {
  position: relative;
  width: 100%;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  margin-bottom: 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--bg-stone);
  box-shadow: inset 0 0 18px rgba(0, 0, 0, .5);
}
.entry-portrait img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center top;
  transition: transform .4s ease, filter .4s ease;
  filter: brightness(.9) contrast(1.1);
}
.codex-card:hover .entry-portrait img {
  transform: scale(1.08);
  filter: brightness(1) contrast(1.15);
}
.entry-portrait-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 40%, var(--bg-card) 100%);
  pointer-events: none;
}
.portrait-frame-rune {
  position: absolute;
  z-index: 2;
  font-size: 11px;
  color: var(--gold);
  filter: drop-shadow(0 0 4px var(--gold-glow));
  opacity: .7;
  transition: opacity var(--transition-base);
}
.portrait-frame-rune.tl { top: 6px; left: 6px; }
.portrait-frame-rune.br { bottom: 6px; right: 6px; }
.codex-card:hover .portrait-frame-rune { opacity: 1; }

/* ===== Hover & active — glowing parchment ===== */
.codex-card:hover {
  transform: translateY(-5px);
  border-color: var(--gold);
  box-shadow:
    var(--shadow-deep),
    0 0 26px var(--gold-glow),
    inset 0 0 18px rgba(201, 162, 39, .05);
}
.codex-card:hover::before { border-color: var(--line-gold); }
.codex-card:hover .card-crest,
.codex-card:hover .card-foot-ornament {
  color: var(--gold-bright);
  text-shadow: 0 0 8px var(--gold-glow);
}

.codex-card.active {
  border-color: var(--gold);
  box-shadow:
    0 0 0 2px var(--line-gold),
    0 8px 30px var(--gold-glow),
    inset 0 0 22px rgba(201, 162, 39, .08);
}
.codex-card.active::before { border-color: var(--line-gold); }
.codex-card.active .card-crest,
.codex-card.active .card-foot-ornament {
  color: var(--gold-bright);
  text-shadow: 0 0 10px var(--gold-glow);
}
.codex-card.active .codex-card-name { color: var(--gold-bright); }

/* category accent glows on hover */
.codex-card[data-category="class"]:hover      { box-shadow: var(--shadow-deep), 0 0 26px rgba(155, 45, 45, .35); }
.codex-card[data-category="race"]:hover       { box-shadow: var(--shadow-deep), 0 0 26px var(--gold-glow); }
.codex-card[data-category="spell"]:hover      { box-shadow: var(--shadow-deep), 0 0 26px rgba(93, 58, 138, .4); }
.codex-card[data-category="monster"]:hover    { box-shadow: var(--shadow-deep), 0 0 26px rgba(74, 122, 58, .35); }
.codex-card[data-category="magic-item"]:hover { box-shadow: var(--shadow-deep), 0 0 26px rgba(196, 90, 42, .4); }
.codex-card[data-category="feat"]:hover       { box-shadow: var(--shadow-deep), 0 0 26px rgba(74, 122, 154, .4); }

.card-with-portrait { padding-top: 20px; }
</style>
