<script setup>
import { ref, onMounted } from "vue";

const props = defineProps({
  totalEntries: Number,
  totalCategories: Number,
});

const animated = {
  entries: ref(0),
  categories: ref(0),
};

function animateCount(key, target) {
  const duration = 1500;
  const start = performance.now();
  function tick(now) {
    const elapsed = now - start;
    const progress = Math.min(elapsed / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    animated[key].value = Math.round(eased * target);
    if (progress < 1) requestAnimationFrame(tick);
  }
  requestAnimationFrame(tick);
}

onMounted(() => {
  animateCount("entries", props.totalEntries);
  animateCount("categories", props.totalCategories);
});
</script>

<template>
  <section class="stat-bar scroll-stat">
    <span class="scroll-cap cap-left" aria-hidden="true">⚜</span>

    <div class="stat">
      <span class="stat-value">{{ animated.entries.value }}</span>
      <span class="stat-label">资料条目</span>
    </div>
    <span class="stat-seal" aria-hidden="true">✦</span>
    <div class="stat">
      <span class="stat-value">{{ animated.categories.value }}</span>
      <span class="stat-label">分类</span>
    </div>
    <span class="stat-seal" aria-hidden="true">✦</span>
    <div class="stat">
      <span class="stat-value stat-rune">✧</span>
      <span class="stat-label">每日探索</span>
    </div>

    <span class="scroll-cap cap-right" aria-hidden="true">⚜</span>
  </section>
</template>

<style scoped>
/* ===== Ancient parchment scroll bar ===== */
.scroll-stat {
  position: relative;
  z-index: 3;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0;
  max-width: 660px;
  margin: -56px auto 56px;
  padding: 30px 70px;
  border-radius: 8px;
  /* Parchment texture */
  background:
    linear-gradient(180deg, rgba(58, 43, 30, .93), rgba(38, 28, 20, .96)),
    var(--texture-parchment);
  background-size: cover, cover;
  border: 1px solid var(--gold-dim);
  box-shadow:
    var(--shadow-deep),
    inset 0 0 60px rgba(0, 0, 0, .4),
    inset 0 1px 0 rgba(201, 162, 39, .18),
    0 0 0 1px rgba(0, 0, 0, .4);
  animation: scrollRise .9s cubic-bezier(.2, .8, .2, 1) both;
}
@keyframes scrollRise {
  from { opacity: 0; transform: translateY(28px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Ornate inner border */
.scroll-stat::before {
  content: "";
  position: absolute;
  inset: 8px;
  border: 1px solid var(--line-gold);
  border-radius: 4px;
  pointer-events: none;
}

/* Scroll end caps (rolled edges) */
.scroll-cap {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 34px;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: var(--gold-bright);
  background:
    linear-gradient(180deg, #2c2014, #1a130c 50%, #2c2014);
  border: 1px solid var(--gold-dim);
  border-radius: 6px;
  box-shadow:
    inset 0 2px 4px rgba(0, 0, 0, .6),
    inset 0 -2px 4px rgba(0, 0, 0, .6),
    0 4px 14px rgba(0, 0, 0, .5);
  filter: drop-shadow(0 0 8px var(--gold-glow));
  z-index: 2;
}
.cap-left { left: -16px; }
.cap-right { right: -16px; }

.stat { flex: 1; text-align: center; z-index: 2; }
.stat-value {
  display: block;
  font-family: var(--font-display);
  font-size: 42px;
  font-weight: 900;
  color: var(--gold);
  text-shadow: 0 0 22px var(--gold-glow), 0 2px 4px rgba(0, 0, 0, .7);
  line-height: 1;
}
.stat-rune {
  font-size: 34px;
  color: var(--gold-bright);
  filter: drop-shadow(0 0 12px var(--gold-glow));
  animation: runeTwinkle 3s ease-in-out infinite;
}
@keyframes runeTwinkle {
  0%, 100% { opacity: .7; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.12); }
}
.stat-label {
  display: block;
  font-family: var(--font-heading);
  font-size: 12px;
  color: var(--ink-soft);
  letter-spacing: .22em;
  text-transform: uppercase;
  margin-top: 8px;
}

.stat-seal {
  font-size: 13px;
  color: var(--gold-dim);
  padding: 0 14px;
  z-index: 2;
  filter: drop-shadow(0 0 4px var(--gold-glow));
}

@media (max-width: 920px) {
  .scroll-stat {
    flex-direction: column;
    gap: 14px;
    margin: 16px 20px 40px;
    padding: 28px 28px;
  }
  .scroll-cap { display: none; }
  .stat-seal { padding: 0; font-size: 16px; }
}
</style>
