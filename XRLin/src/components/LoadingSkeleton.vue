<script setup>
defineProps({
  count: { type: Number, default: 6 },
});
</script>

<template>
  <div class="cards codex-skeleton-grid">
    <div v-for="n in count" :key="n" class="skeleton-card codex-skeleton-card shimmer-magical">
      <span class="sk-crest" aria-hidden="true">✦</span>
      <div class="skeleton-line skeleton-title codex-sk-title"></div>
      <div class="skeleton-line skeleton-meta codex-sk-meta"></div>
      <div class="skeleton-line skeleton-text codex-sk-text"></div>
      <div class="skeleton-line skeleton-text short codex-sk-text"></div>
      <div class="skeleton-tags codex-sk-tags">
        <span class="skeleton-tag codex-sk-tag"></span>
        <span class="skeleton-tag codex-sk-tag"></span>
        <span class="skeleton-tag short-tag codex-sk-tag"></span>
      </div>
      <span class="sk-foot" aria-hidden="true">❧</span>
    </div>
  </div>
</template>

<style scoped>
/* ===== Magical shimmer loading scrolls ===== */
.codex-skeleton-grid { gap: 16px; }

.codex-skeleton-card {
  position: relative;
  min-height: 190px;
  padding: 22px 18px 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background:
    linear-gradient(180deg, rgba(201, 162, 39, .04) 0%, transparent 30%),
    linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  box-shadow: var(--shadow-card), inset 0 1px 0 rgba(255, 255, 255, .02);
  overflow: hidden;
}
/* inner ornate border */
.codex-skeleton-card::before {
  content: "";
  position: absolute;
  inset: 5px;
  border: 1px solid transparent;
  border-radius: 2px;
  pointer-events: none;
}

.sk-crest,
.sk-foot {
  position: absolute;
  z-index: 2;
  font-size: 11px;
  color: var(--gold-dim);
  opacity: .6;
  pointer-events: none;
  left: 50%;
  transform: translateX(-50%);
}
.sk-crest { top: 7px; }
.sk-foot { bottom: 6px; }

/* gold arcane shimmer instead of plain grey */
.codex-sk-title,
.codex-sk-meta,
.codex-sk-text,
.codex-sk-tag {
  background: linear-gradient(
    90deg,
    rgba(61, 47, 32, .6) 0%,
    rgba(201, 162, 39, .25) 40%,
    rgba(232, 196, 74, .35) 50%,
    rgba(201, 162, 39, .25) 60%,
    rgba(61, 47, 32, .6) 70%
  );
  background-size: 200% 100%;
  animation: arcaneShimmer 2s ease-in-out infinite;
  border-radius: 2px;
}
@keyframes arcaneShimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.codex-sk-title { height: 20px; width: 60%; margin-bottom: 14px; }
.codex-sk-meta { height: 13px; width: 45%; margin-bottom: 14px; }
.codex-sk-text { height: 13px; width: 90%; margin-bottom: 12px; }
.codex-sk-text.short { width: 70%; }
.codex-sk-tags { display: flex; gap: 8px; margin-top: 16px; }
.codex-sk-tag { width: 50px; height: 22px; display: inline-block; }
.codex-sk-tag.short-tag { width: 35px; }
</style>
