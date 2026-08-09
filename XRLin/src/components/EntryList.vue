<script setup>
import { useRouter } from "vue-router";
import EntryCard from "./EntryCard.vue";
import LoadingSkeleton from "./LoadingSkeleton.vue";

defineProps({
  entries: Array,
  loading: Boolean,
  selectedId: String,
  categoryLabels: Object,
});

const emit = defineEmits(["select", "compare"]);
const router = useRouter();

function onCardClick(entry) {
  emit("select", entry);
}

function onCardDblClick(entry) {
  router.push(`/detail/${entry.category}/${entry.id}`);
}
</script>

<template>
  <section class="results codex-results" aria-label="资料列表">
    <div class="section-head codex-section-head">
      <h2 class="codex-results-title">
        <span class="title-rune">❧</span>
        <span>典籍条目</span>
      </h2>
      <span class="results-count">
        {{ loading ? "卷轴展开中…" : `共 ${entries.length} 卷` }}
      </span>
    </div>

    <div class="divider-ornament codex-results-divider"><span>✦ ✦ ✦</span></div>

    <LoadingSkeleton v-if="loading && !entries.length" />

    <div v-else-if="!entries.length" class="empty-state codex-empty-state">
      <span class="empty-icon">📭</span>
      <p>此页空空如也</p>
      <p class="empty-hint">未寻得匹配之卷，或可调整检索铭文再试</p>
    </div>

    <TransitionGroup v-else name="cards" tag="div" class="cards codex-cards">
      <EntryCard
        v-for="entry in entries"
        :key="entry.id"
        :entry="entry"
        :is-active="selectedId === entry.id"
        :category-label="categoryLabels[entry.category]"
        @click="onCardClick(entry)"
        @dblclick="onCardDblClick(entry)"
      />
    </TransitionGroup>
  </section>
</template>

<style scoped>
/* ===== Codex results section ===== */
.codex-results { position: relative; }

.codex-section-head {
  align-items: baseline;
  margin-bottom: 6px;
}
.codex-results-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 800;
  letter-spacing: .12em;
  color: var(--ink-bright);
  margin: 0;
  text-shadow: 0 2px 6px rgba(0, 0, 0, .6);
}
.codex-results-title .title-rune {
  font-size: 20px;
  color: var(--gold);
  filter: drop-shadow(0 0 8px var(--gold-glow));
}
.results-count {
  font-family: var(--font-heading);
  font-size: 12px;
  letter-spacing: .14em;
  color: var(--gold-dim);
  font-style: normal;
  text-transform: uppercase;
}

.codex-results-divider {
  margin: 10px 0 22px;
}
.codex-results-divider span { letter-spacing: .3em; font-size: 12px; }

.codex-empty-state {
  border-style: dashed;
  border-color: var(--gold-dim);
  background: rgba(201, 162, 39, .04);
}
.codex-empty-state .empty-icon {
  filter: grayscale(.3) opacity(.8);
}
.codex-empty-state p {
  font-family: var(--font-uncial);
  letter-spacing: .08em;
  color: var(--ink-soft);
}
.codex-empty-state .empty-hint {
  font-family: var(--font-body);
  font-style: italic;
  font-size: 13px;
  color: var(--muted);
  letter-spacing: 0;
  margin-top: 4px;
}

.codex-cards { gap: 16px; }

/* Stagger entry animation like unrolling scrolls */
.codex-cards :deep(.card) {
  animation: scrollUnroll .4s ease-out backwards;
}
</style>
