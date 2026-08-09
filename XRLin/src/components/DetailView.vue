<script setup>
import classImages from "../composables/useClassImages";
import monsterImages from "../composables/useMonsterImages";

const props = defineProps({
  entry: Object,
  categoryLabel: String,
});

const categoryEmojis = {
  class: "⚔️",
  race: "🧝",
  spell: "✨",
  monster: "👹",
  "magic-item": "💍",
  feat: "📜",
};

function getEntryImage(entry) {
  if (entry.category === "class") return classImages[entry.id];
  if (entry.category === "monster") {
    if (entry.imageUrl) return entry.imageUrl;
    return monsterImages[entry.id];
  }
  return null;
}
</script>

<template>
  <article v-if="entry" class="detail-view codex-folio">
    <span class="folio-corner tl" aria-hidden="true">❦</span>
    <span class="folio-corner tr" aria-hidden="true">❦</span>
    <span class="folio-corner bl" aria-hidden="true">❦</span>
    <span class="folio-corner br" aria-hidden="true">❦</span>

    <!-- 立绘图（职业/怪物） -->
    <div v-if="getEntryImage(entry)" class="dv-portrait">
      <img :src="getEntryImage(entry)" :alt="entry.name" />
      <div class="dv-portrait-overlay"></div>
      <span class="dv-frame-rune tl">✦</span>
      <span class="dv-frame-rune br">✦</span>
    </div>

    <div class="divider-ornament codex-folio-divider"><span>✦ ❖ ✦</span></div>

    <header class="dv-header codex-folio-header">
      <span class="dv-emoji codex-folio-emoji">{{ categoryEmojis[entry.category] || "📦" }}</span>
      <div>
        <p class="dv-meta">{{ categoryLabel }} · {{ entry.subtitle }}</p>
        <h1 class="dv-title codex-folio-title">{{ entry.name }}</h1>
      </div>
    </header>

    <p class="dv-summary codex-folio-summary">{{ entry.summary }}</p>

    <div class="tags codex-folio-tags">
      <span
        v-for="tag in entry.tags"
        :key="tag"
        class="tag"
        :class="`tag-${entry.category}`"
      >{{ tag }}</span>
    </div>

    <div v-if="entry.details?.length" class="dv-details codex-folio-details">
      <div class="divider-ornament codex-folio-divider"><span>§ § §</span></div>
      <h3 class="codex-folio-section-title">
        <span class="section-rune">❧</span>
        <span>古籍详录</span>
        <span class="section-rune">❧</span>
      </h3>
      <ul class="codex-folio-lore">
        <li v-for="(detail, idx) in entry.details" :key="idx">
          {{ detail }}
        </li>
      </ul>
    </div>

    <div class="divider-ornament codex-folio-divider"><span>finis</span></div>
  </article>
</template>

<style scoped>
/* ===== Full codex folio page ===== */
.codex-folio {
  position: relative;
  padding: 34px 32px 40px;
  border: 1px solid var(--line-gold);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(180deg, rgba(42, 31, 23, .8), rgba(23, 16, 12, .9)),
    var(--texture-parchment) center / cover;
  box-shadow:
    var(--shadow-deep),
    inset 0 0 80px rgba(0, 0, 0, .4),
    inset 0 0 0 1px rgba(201, 162, 39, .06);
}
/* inner ornate border */
.codex-folio::before {
  content: "";
  position: absolute;
  inset: 10px;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  pointer-events: none;
}
/* glowing top crest */
.codex-folio::after {
  content: "";
  position: absolute;
  top: 0; left: 18%; right: 18%;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--gold), transparent);
  box-shadow: 0 0 12px var(--gold-glow);
  pointer-events: none;
}

/* corner flourishes */
.folio-corner {
  position: absolute;
  z-index: 3;
  font-size: 15px;
  color: var(--gold-dim);
  filter: drop-shadow(0 0 5px var(--gold-soft));
  pointer-events: none;
}
.folio-corner.tl { top: 16px; left: 16px; }
.folio-corner.tr { top: 16px; right: 16px; }
.folio-corner.bl { bottom: 16px; left: 16px; }
.folio-corner.br { bottom: 16px; right: 16px; }

.codex-folio-divider {
  margin: 18px 0;
  position: relative;
  z-index: 1;
}
.codex-folio-divider span {
  font-family: var(--font-rune);
  letter-spacing: .35em;
  color: var(--gold);
  font-size: 13px;
}

/* portrait */
.dv-portrait {
  position: relative;
  width: 100%;
  max-width: 420px;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  border-radius: var(--radius-md);
  border: 1px solid var(--line-gold);
  margin-bottom: 20px;
  background: var(--bg-stone);
  box-shadow:
    inset 0 0 28px rgba(0, 0, 0, .5),
    0 0 20px rgba(201, 162, 39, .1);
}
.dv-portrait img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center top;
  filter: brightness(.9) contrast(1.1);
}
.dv-portrait-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 60%, rgba(42, 31, 23, .95) 100%);
  pointer-events: none;
}
.dv-frame-rune {
  position: absolute;
  z-index: 2;
  font-size: 13px;
  color: var(--gold);
  filter: drop-shadow(0 0 6px var(--gold-glow));
}
.dv-frame-rune.tl { top: 10px; left: 10px; }
.dv-frame-rune.br { bottom: 10px; right: 10px; }

/* header */
.codex-folio-header {
  position: relative;
  z-index: 1;
  border-bottom: 1px solid var(--line-gold);
  padding-bottom: 22px;
}
.codex-folio-emoji {
  font-size: 60px;
  filter: drop-shadow(0 0 16px var(--gold-glow));
}
.codex-folio-title {
  font-family: var(--font-display);
  font-size: 44px;
  font-weight: 900;
  letter-spacing: .05em;
  color: var(--ink-bright);
  margin: 0;
  text-shadow: 0 2px 12px rgba(0, 0, 0, .7), 0 0 28px rgba(201, 162, 39, .12);
}

/* summary — drop cap like an illuminated manuscript */
.codex-folio-summary {
  font-family: var(--font-serif);
  font-size: 18px;
  line-height: 1.9;
  color: var(--ink);
  text-align: justify;
  position: relative;
  z-index: 1;
}
.codex-folio-summary::first-letter {
  font-family: var(--font-display);
  font-size: 52px;
  font-weight: 900;
  color: var(--gold);
  float: left;
  line-height: .9;
  padding: 4px 12px 0 0;
  text-shadow: 0 0 16px var(--gold-glow);
}

.codex-folio-tags { position: relative; z-index: 1; }

/* details section */
.codex-folio-details {
  margin-top: 12px;
  padding-top: 8px;
  border-top: none;
  position: relative;
  z-index: 1;
}
.codex-folio-section-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: .14em;
  color: var(--gold);
  text-transform: uppercase;
  margin-bottom: 18px;
  text-shadow: 0 0 14px var(--gold-glow);
}
.codex-folio-section-title .section-rune {
  font-size: 16px;
  color: var(--gold-dim);
  filter: drop-shadow(0 0 5px var(--gold-soft));
}
.codex-folio-lore {
  padding-left: 26px;
  max-width: 680px;
}
.codex-folio-lore li {
  font-family: var(--font-serif);
  color: var(--ink);
  font-size: 16px;
  line-height: 1.9;
  margin-bottom: 10px;
}
.codex-folio-lore li::marker {
  content: "❧ ";
  color: var(--gold);
}

@media (max-width: 560px) {
  .codex-folio { padding: 24px 18px 28px; }
  .codex-folio-title { font-size: 30px; }
  .folio-corner { font-size: 12px; }
}
</style>
