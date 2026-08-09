<script setup>
import { useRouter } from "vue-router";
import classImages from "../composables/useClassImages";
import monsterImages from "../composables/useMonsterImages";

const router = useRouter();

const props = defineProps({
  entry: Object,
  categoryLabel: String,
});

function viewDetail(entry) {
  router.push(`/detail/${entry.category}/${entry.id}`);
}

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
  <aside class="detail codex-detail" aria-label="条目详情">
    <span class="codex-detail-corner tl" aria-hidden="true">❦</span>
    <span class="codex-detail-corner tr" aria-hidden="true">❦</span>
    <span class="codex-detail-corner bl" aria-hidden="true">❦</span>
    <span class="codex-detail-corner br" aria-hidden="true">❦</span>

    <div v-if="!entry" class="detail-empty codex-detail-empty">
      <span class="empty-icon">📖</span>
      <p>翻阅一卷以观其详</p>
      <p class="empty-hint">从左侧典籍中择一卷轴</p>
    </div>

    <Transition name="detail-fade" mode="out-in">
      <article v-if="entry" :key="entry.id" class="detail-content codex-scroll">
        <!-- 立绘图（职业/怪物） -->
        <div v-if="getEntryImage(entry)" class="dp-portrait">
          <img :src="getEntryImage(entry)" :alt="entry.name" />
          <div class="dp-portrait-overlay"></div>
          <span class="dp-frame-rune tl">✦</span>
          <span class="dp-frame-rune br">✦</span>
        </div>

        <header class="detail-header codex-detail-header">
          <div class="divider-ornament codex-mini-divider"><span>✦</span></div>
          <p class="subtitle">{{ categoryLabel }} · {{ entry.subtitle }}</p>
          <h2 class="codex-detail-title">{{ entry.name }}</h2>
        </header>

        <p class="detail-summary">{{ entry.summary }}</p>

        <div class="tags">
          <span
            v-for="tag in entry.tags"
            :key="tag"
            class="tag"
            :class="`tag-${entry.category}`"
          >{{ tag }}</span>
        </div>

        <div v-if="entry.details?.length" class="detail-section codex-detail-section">
          <h3 class="codex-section-heading">
            <span class="heading-rune">❧</span>
            <span>古籍记载</span>
            <span class="heading-rune">❧</span>
          </h3>
          <ul class="codex-lore-list">
            <li v-for="(detail, idx) in entry.details" :key="idx">
              {{ detail }}
            </li>
          </ul>
        </div>

        <button class="view-full-btn codex-open-btn" @click="viewDetail(entry)">
          <span class="open-rune">⟡</span>
          <span>展开完整卷轴</span>
          <span class="open-rune">⟡</span>
        </button>
      </article>
    </Transition>
  </aside>
</template>

<style scoped>
/* ===== Open magical scroll / codex page ===== */
.codex-detail {
  position: relative;
  padding: 28px 26px;
  border: 1px solid var(--line-gold);
  border-radius: var(--radius-md);
  background:
    linear-gradient(180deg, rgba(42, 31, 23, .85), rgba(23, 16, 12, .92)),
    var(--texture-parchment) center / cover;
  box-shadow:
    var(--shadow-deep),
    inset 0 0 60px rgba(0, 0, 0, .35),
    inset 0 0 0 1px rgba(201, 162, 39, .06);
}
/* inner ornate border */
.codex-detail::before {
  content: "";
  position: absolute;
  inset: 8px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  pointer-events: none;
}
/* top gold crest line */
.codex-detail::after {
  content: "";
  position: absolute;
  top: 0; left: 16%; right: 16%;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--gold), transparent);
  box-shadow: 0 0 10px var(--gold-glow);
  pointer-events: none;
}

/* corner flourishes */
.codex-detail-corner {
  position: absolute;
  z-index: 3;
  font-size: 13px;
  color: var(--gold-dim);
  filter: drop-shadow(0 0 4px var(--gold-soft));
  pointer-events: none;
}
.codex-detail-corner.tl { top: 12px; left: 12px; }
.codex-detail-corner.tr { top: 12px; right: 12px; }
.codex-detail-corner.bl { bottom: 12px; left: 12px; }
.codex-detail-corner.br { bottom: 12px; right: 12px; }

.codex-detail-empty {
  border: 1px dashed var(--line-gold);
  background: rgba(201, 162, 39, .04);
  position: relative;
  z-index: 1;
}
.codex-detail-empty .empty-icon {
  filter: grayscale(.2) opacity(.85);
  font-size: 48px;
}
.codex-detail-empty p {
  font-family: var(--font-uncial);
  letter-spacing: .08em;
  color: var(--ink-soft);
  margin: 0;
}
.codex-detail-empty .empty-hint {
  font-family: var(--font-body);
  font-style: italic;
  font-size: 13px;
  color: var(--muted);
  letter-spacing: 0;
  margin-top: 6px;
}

.codex-scroll { position: relative; z-index: 1; }

/* portrait with ornate frame */
.dp-portrait {
  position: relative;
  width: 100%;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  border-radius: var(--radius-sm);
  border: 1px solid var(--line-gold);
  margin-bottom: 16px;
  background: var(--bg-stone);
  box-shadow:
    inset 0 0 24px rgba(0, 0, 0, .5),
    0 0 16px rgba(201, 162, 39, .08);
}
.dp-portrait img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center top;
  filter: brightness(.9) contrast(1.1);
}
.dp-portrait-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 40%, rgba(42, 31, 23, .9) 100%);
  pointer-events: none;
}
.dp-frame-rune {
  position: absolute;
  z-index: 2;
  font-size: 12px;
  color: var(--gold);
  filter: drop-shadow(0 0 6px var(--gold-glow));
}
.dp-frame-rune.tl { top: 8px; left: 8px; }
.dp-frame-rune.br { bottom: 8px; right: 8px; }

/* header */
.codex-detail-header { text-align: center; margin-bottom: 8px; }
.codex-mini-divider { margin: 0 auto 10px; max-width: 200px; }
.codex-mini-divider span { font-size: 11px; }
.codex-detail-header .subtitle {
  letter-spacing: .14em;
  color: var(--gold);
}
.codex-detail-title {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 900;
  letter-spacing: .05em;
  color: var(--ink-bright);
  margin: 4px 0 0;
  text-shadow: 0 2px 10px rgba(0, 0, 0, .6), 0 0 20px rgba(201, 162, 39, .1);
}

/* summary reads like ancient text */
.codex-scroll .detail-summary {
  font-family: var(--font-serif);
  font-size: 15px;
  line-height: 1.85;
  color: var(--ink);
  text-align: justify;
  position: relative;
  padding: 0 4px;
}
.codex-scroll .detail-summary::first-letter {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 900;
  color: var(--gold);
  float: left;
  line-height: 1;
  padding: 2px 8px 0 0;
  text-shadow: 0 0 12px var(--gold-glow);
}

/* section heading — ornate */
.codex-detail-section {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--line-gold);
}
.codex-section-heading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: .14em;
  color: var(--gold);
  text-transform: uppercase;
  margin-bottom: 14px;
}
.codex-section-heading .heading-rune {
  font-size: 13px;
  color: var(--gold-dim);
  filter: drop-shadow(0 0 4px var(--gold-soft));
}
.codex-lore-list {
  padding-left: 22px;
}
.codex-lore-list li {
  font-family: var(--font-serif);
  color: var(--ink);
  line-height: 1.85;
  margin-bottom: 8px;
}
.codex-lore-list li::marker {
  content: "❧ ";
  color: var(--gold);
}

/* open full scroll button */
.codex-open-btn {
  margin-top: 24px;
  padding: 14px 20px;
  border: 1px solid var(--arcane);
  border-radius: var(--radius-sm);
  background:
    linear-gradient(180deg, var(--arcane-soft), transparent);
  color: var(--arcane-glow);
  cursor: pointer;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 14px;
  letter-spacing: .1em;
  text-transform: uppercase;
  transition: all var(--transition-base);
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  position: relative;
  overflow: hidden;
}
.codex-open-btn .open-rune {
  font-size: 13px;
  filter: drop-shadow(0 0 4px var(--arcane-soft));
  transition: transform var(--transition-base);
}
.codex-open-btn:hover {
  background: linear-gradient(180deg, var(--arcane-soft), var(--arcane-soft));
  color: var(--ink-bright);
  border-color: var(--arcane-glow);
  box-shadow: 0 0 22px rgba(93, 58, 138, .35), inset 0 0 14px rgba(93, 58, 138, .1);
  transform: translateY(-2px);
}
.codex-open-btn:hover .open-rune {
  transform: rotate(90deg);
  color: var(--arcane-glow);
}
</style>
