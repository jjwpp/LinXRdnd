<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useApi } from "../composables/useApi";
import { useRecentlyViewed } from "../composables/useRecentlyViewed";
import CategoryCard from "../components/CategoryCard.vue";
import StatBar from "../components/StatBar.vue";
import FeaturedEntries from "../components/FeaturedEntries.vue";

const router = useRouter();
const { checkHealth, fetchCategories } = useApi();
const { recentlyViewed } = useRecentlyViewed();

const categories = ref([]);
const visible = ref(false);

const categoryLabels = {
  class: "职业",
  race: "种族",
  spell: "法术",
  monster: "怪物",
  "magic-item": "魔法物品",
  feat: "专长",
};

const totalEntries = computed(() =>
  categories.value.reduce((s, c) => s + c.count, 0)
);

onMounted(async () => {
  try {
    await checkHealth();
    categories.value = await fetchCategories();
  } catch {
    // silent
  }
  requestAnimationFrame(() => {
    visible.value = true;
  });
});
</script>

<template>
  <div class="home-page" :class="{ visible }">
    <!-- Full-screen Cinematic Hero -->
    <section class="home-hero">
      <div class="hero-bg-layer" aria-hidden="true"></div>
      <div class="hero-bg-overlay" aria-hidden="true"></div>
      <div class="hero-glow" aria-hidden="true"></div>
      <div class="hero-runes" aria-hidden="true"></div>

      <!-- Floating magical particles & rising embers -->
      <div class="hero-particles" aria-hidden="true">
        <span
          v-for="n in 14"
          :key="'spark' + n"
          class="hero-spark"
          :class="'spark-' + ((n % 3) + 1)"
          :style="{
            left: ((n * 37) % 100) + '%',
            animationDelay: (n * 0.55) + 's',
            animationDuration: (7 + (n % 5)) + 's',
          }"
        ></span>
        <span
          v-for="n in 9"
          :key="'ember' + n"
          class="hero-ember"
          :style="{
            left: ((n * 29 + 6) % 100) + '%',
            animationDelay: (n * 0.8) + 's',
            animationDuration: (4 + (n % 4)) + 's',
            '--drift': (((n * 13) % 50) - 25) + 'px',
          }"
        ></span>
      </div>

      <div class="hero-content">
        <span class="hero-sigil" aria-hidden="true">⚜</span>
        <h1 class="hero-title">
          <span class="title-line">彩虹金刚</span>
          <span class="title-sub">RAINBOW VAJRA</span>
        </h1>
        <p class="hero-desc">
          翻开这本远古魔法典籍，踏入龙与地下城的史诗世界
        </p>
        <p class="hero-tagline">DND Fantasy Adventure System</p>

        <div class="hero-actions">
          <button class="rune-btn hero-btn" @click="router.push('/browse')">
            ⚔ 开始探索
          </button>
          <button
            class="rune-btn hero-btn hero-btn-arcane"
            @click="router.push('/random')"
          >
            🎲 命运之骰
          </button>
        </div>

        <div class="hero-ornaments" aria-hidden="true">
          <span></span><span></span><span></span><span></span>
        </div>
      </div>

      <div class="hero-scroll-hint" aria-hidden="true">
        <span class="scroll-line"></span>
        <span class="scroll-arrow">▼</span>
      </div>
    </section>

    <!-- Stats Scroll -->
    <StatBar :total-entries="totalEntries" :total-categories="categories.length" />

    <!-- Six Stone Tablets -->
    <CategoryCard :categories="categories" />

    <!-- Featured -->
    <FeaturedEntries />

    <!-- Recently Viewed -->
    <section v-if="recentlyViewed.length" class="recent-section">
      <div class="divider-ornament" aria-hidden="true">
        <span>✦ RECENT ✦</span>
      </div>
      <h2 class="section-title">最近浏览</h2>
      <div class="recent-list">
        <router-link
          v-for="item in recentlyViewed"
          :key="item.id"
          :to="`/detail/${item.category}/${item.id}`"
          class="recent-item"
        >
          <span>{{ item.name }}</span>
          <span class="recent-cat">{{ categoryLabels[item.category] }}</span>
        </router-link>
      </div>
    </section>

    <!-- Footer CTA -->
    <section class="home-cta">
      <div class="divider-ornament" aria-hidden="true">
        <span>✦ BEGIN ✦</span>
      </div>
      <h2>准备好开始冒险了吗？</h2>
      <p>超过 {{ totalEntries }} 条 DND 资料等你探索</p>
      <button class="rune-btn" @click="router.push('/browse')">
        进入资料库 →
      </button>
    </section>
  </div>
</template>

<style scoped>
/* ===== Full-screen cinematic hero ===== */
.home-hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 120px 24px 110px;
  overflow: hidden;
  isolation: isolate;
}

.hero-bg-layer {
  position: absolute;
  inset: 0;
  background-image: var(--hero-bg);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  filter: brightness(.36) contrast(1.25) saturate(.85);
  z-index: 0;
  transform: scale(1.05);
}

.hero-bg-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 50% 40%, transparent 0%, rgba(11, 9, 8, .5) 55%, var(--bg-void) 100%),
    linear-gradient(180deg, rgba(11, 9, 8, .5) 0%, rgba(11, 9, 8, .55) 60%, var(--bg-void) 100%);
  z-index: 1;
}

.hero-glow {
  position: absolute;
  top: 6%;
  left: 50%;
  translate: -50% 0;
  width: 780px;
  height: 780px;
  max-width: 92vw;
  background: radial-gradient(circle, rgba(201, 162, 39, .16), rgba(93, 58, 138, .07) 42%, transparent 70%);
  pointer-events: none;
  z-index: 1;
  animation: heroPulse 6s ease-in-out infinite;
}
@keyframes heroPulse {
  0%, 100% { opacity: .55; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.06); }
}

/* Keep the rotating rune circle from global, just nudge behind content */
.hero-runes {
  z-index: 1;
  opacity: .12;
}

/* ===== Hero content ===== */
.hero-content {
  position: relative;
  z-index: 3;
  text-align: center;
  max-width: 820px;
  animation: heroRise 1.1s cubic-bezier(.2, .8, .2, 1) both;
}
@keyframes heroRise {
  from { opacity: 0; transform: translateY(34px); }
  to { opacity: 1; transform: translateY(0); }
}

.hero-sigil {
  display: block;
  font-size: 30px;
  color: var(--gold);
  margin-bottom: 14px;
  filter: drop-shadow(0 0 10px var(--gold-glow));
  animation: sigilBreath 4s ease-in-out infinite;
}
@keyframes sigilBreath {
  0%, 100% { opacity: .55; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.08); }
}

.hero-title { margin-bottom: 22px; }

.title-line {
  display: block;
  font-family: var(--font-display);
  font-size: clamp(44px, 9vw, 92px);
  font-weight: 900;
  letter-spacing: .1em;
  line-height: 1.1;
  background: linear-gradient(180deg, #fbe07a 0%, var(--gold-bright) 35%, var(--gold) 60%, var(--gold-dim) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 4px 28px rgba(201, 162, 39, .35)) drop-shadow(0 0 60px rgba(201, 162, 39, .12));
  animation: titleGlow 5s ease-in-out infinite;
}
@keyframes titleGlow {
  0%, 100% { filter: drop-shadow(0 4px 28px rgba(201, 162, 39, .28)) drop-shadow(0 0 50px rgba(201, 162, 39, .1)); }
  50% { filter: drop-shadow(0 4px 34px rgba(201, 162, 39, .5)) drop-shadow(0 0 80px rgba(201, 162, 39, .2)); }
}

.title-sub {
  display: block;
  font-family: var(--font-heading);
  font-size: clamp(12px, 2.4vw, 16px);
  color: var(--gold-dim);
  letter-spacing: .55em;
  text-transform: uppercase;
  margin-top: 16px;
  text-indent: .55em;
}

.hero-desc {
  font-family: var(--font-body);
  font-size: clamp(15px, 2.4vw, 19px);
  font-style: italic;
  color: var(--ink-soft);
  max-width: 620px;
  margin: 0 auto 8px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, .8);
}

.hero-tagline {
  font-family: var(--font-heading);
  font-size: 12px;
  color: var(--gold-dim);
  letter-spacing: .42em;
  text-transform: uppercase;
  margin: 0 auto 38px;
  text-indent: .42em;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.hero-btn {
  padding: 15px 38px;
  font-size: 15px;
  letter-spacing: .14em;
}
.hero-btn-arcane {
  border-color: var(--arcane);
  color: var(--arcane-glow);
  box-shadow: inset 0 1px 0 rgba(125, 90, 170, .2), 0 2px 10px rgba(0, 0, 0, .4);
}
.hero-btn-arcane::before {
  background: radial-gradient(circle at center, rgba(125, 90, 170, .2), transparent 70%);
}
.hero-btn-arcane:hover {
  border-color: var(--arcane-glow);
  color: #d9c7f0;
  box-shadow: inset 0 1px 0 rgba(125, 90, 170, .3), 0 4px 22px rgba(93, 58, 138, .35);
}

/* ===== Hero floating particles & embers ===== */
.hero-particles {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  overflow: hidden;
}
.hero-spark {
  position: absolute;
  bottom: -12px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  animation: sparkRise linear infinite;
}
.hero-spark.spark-1 {
  background: var(--gold-bright);
  box-shadow: 0 0 6px var(--gold-bright), 0 0 14px var(--gold-glow);
}
.hero-spark.spark-2 {
  background: var(--arcane-glow);
  box-shadow: 0 0 6px var(--arcane-glow), 0 0 14px var(--arcane-soft);
}
.hero-spark.spark-3 {
  background: #ffe9a8;
  box-shadow: 0 0 8px #ffe9a8, 0 0 16px var(--gold-glow);
}
@keyframes sparkRise {
  0% { transform: translateY(0) translateX(0) scale(.6); opacity: 0; }
  12% { opacity: .9; }
  50% { transform: translateY(-46vh) translateX(18px) scale(1.2); }
  88% { opacity: .5; }
  100% { transform: translateY(-92vh) translateX(-12px) scale(.4); opacity: 0; }
}

.hero-ember {
  position: absolute;
  bottom: -8px;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--ember);
  box-shadow: 0 0 6px var(--ember), 0 0 12px rgba(196, 90, 42, .5);
  animation: emberDrift ease-out infinite;
}
@keyframes emberDrift {
  0% { transform: translateY(0) translateX(0); opacity: 0; }
  12% { opacity: .85; }
  100% { transform: translateY(-70vh) translateX(var(--drift, 20px)); opacity: 0; }
}

/* Scroll hint */
.hero-scroll-hint {
  position: absolute;
  bottom: 26px;
  left: 50%;
  translate: -50% 0;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--gold-dim);
  animation: hintBob 2.4s ease-in-out infinite;
}
.scroll-line { width: 1px; height: 26px; background: linear-gradient(var(--gold), transparent); }
.scroll-arrow { font-size: 11px; letter-spacing: 0; }
@keyframes hintBob {
  0%, 100% { transform: translateY(0); opacity: .5; }
  50% { transform: translateY(7px); opacity: 1; }
}

/* ===== Page entrance stagger ===== */
.home-page { opacity: 0; }
.home-page.visible { opacity: 1; }

@media (max-width: 920px) {
  .home-hero { min-height: 88vh; padding: 110px 20px 90px; }
}
@media (max-width: 560px) {
  .title-sub { letter-spacing: .38em; }
  .hero-tagline { letter-spacing: .3em; }
  .hero-actions { gap: 12px; }
  .hero-btn { padding: 13px 26px; font-size: 13px; }
}
</style>
