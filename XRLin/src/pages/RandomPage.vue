<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useApi } from "../composables/useApi";
import DetailView from "../components/DetailView.vue";
import FavoriteBtn from "../components/FavoriteBtn.vue";

const router = useRouter();
const { loading, fetchRandom } = useApi();
const entry = ref(null);
const history = ref([]);
const rolling = ref(false);

const categoryLabels = {
  class: "职业",
  race: "种族",
  spell: "法术",
  monster: "怪物",
  "magic-item": "魔法物品",
  feat: "专长",
};

async function rollEntry() {
  rolling.value = true;
  entry.value = null;
  // Short delay for animation effect
  await new Promise((r) => setTimeout(r, 400));
  try {
    const result = await fetchRandom();
    entry.value = result;
    history.value.unshift(result);
    if (history.value.length > 10) history.value.pop();
  } catch {
    // silent
  }
  rolling.value = false;
}

// Initial roll
rollEntry();
</script>

<template>
  <div class="random-page fate">
    <!-- Hero area -->
    <div class="random-hero fate-hero">
      <div class="fate-rune-circle" aria-hidden="true"></div>
      <div class="hero-glow" style="position:absolute;top:-80px;left:50%;translate:-50% 0;width:400px;height:400px;"></div>

      <!-- Large dice sigil -->
      <div class="fate-dice-sigil" :class="{ rolling }" aria-hidden="true">
        <span class="dice-face">⚄</span>
      </div>

      <p class="fate-eyebrow">DICE OF FATE</p>
      <h1 class="fate-title text-glow-gold">命运之骰</h1>
      <p class="random-sub">FATE'S WHISPER</p>
      <p class="random-desc">命运之指将引你窥见未知的知识</p>

      <button class="btn-primary roll-btn fate-roll-btn pulse-glow shimmer-magical" :class="{ rolling }" :disabled="rolling" @click="rollEntry">
        <span v-if="rolling" class="dice-rolling">⚄</span>
        <span v-else>⚄ 再启命运之骰</span>
      </button>
    </div>

    <!-- Revelation scroll -->
    <div class="random-result fate-result">
      <Transition name="detail-fade" mode="out-in">
        <div v-if="entry" :key="entry.id" class="random-entry-wrap fate-revelation surface-parchment border-ornate corner-flourish">
          <div class="revelation-seal" aria-hidden="true">✦</div>
          <p class="revelation-eyebrow">REVELATION · 命运所揭示</p>
          <div class="random-actions fate-actions">
            <button class="btn-back" @click="router.push(`/detail/${entry.category}/${entry.id}`)">
              展开完整卷宗 ⟶
            </button>
            <FavoriteBtn :entry-id="entry.id" />
          </div>
          <DetailView :entry="entry" :category-label="categoryLabels[entry.category]" />
        </div>
        <div v-else class="empty-state fate-rolling-state surface-stone border-ornate">
          <span class="empty-icon dice-rolling">⚄</span>
          <p>命运之骰滚动中...</p>
        </div>
      </Transition>
    </div>

    <!-- History as rune tokens -->
    <section v-if="history.length > 1" class="random-history fate-chronicle">
      <div class="divider-ornament" aria-hidden="true">
        <span>✦ CHRONICLE ✦</span>
      </div>
      <h2 class="section-title">本轮卜筮纪闻</h2>
      <div class="history-list fate-tokens">
        <button
          v-for="(item, idx) in history"
          :key="item.id + '-' + idx"
          class="history-item fate-token"
          @click="router.push(`/detail/${item.category}/${item.id}`)"
        >
          <span class="fate-token-rune" aria-hidden="true">❖</span>
          <span class="hi-name">{{ item.name }}</span>
          <span class="hi-cat">{{ categoryLabels[item.category] }}</span>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.fate {
  position: relative;
  padding: 40px 24px 72px;
}

/* Hero */
.fate-hero {
  text-align: center;
  padding: 32px 24px 40px;
  position: relative;
}
.fate-rune-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 440px;
  height: 440px;
  border: 1px solid var(--line-gold);
  border-radius: 50%;
  opacity: .1;
  pointer-events: none;
  animation: fateRotate 70s linear infinite;
}
.fate-rune-circle::before {
  content: "";
  position: absolute;
  inset: 40px;
  border: 1px dashed var(--line-gold);
  border-radius: 50%;
}
.fate-rune-circle::after {
  content: "";
  position: absolute;
  inset: 90px;
  border: 1px solid var(--line-gold);
  border-radius: 50%;
}
@keyframes fateRotate {
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

/* Large dice sigil */
.fate-dice-sigil {
  position: relative;
  width: 110px;
  height: 110px;
  margin: 0 auto 18px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--gold);
  background: radial-gradient(circle at 35% 30%, var(--gold-soft), var(--bg-card) 60%, var(--bg-stone));
  box-shadow: 0 0 32px var(--gold-glow), inset 0 2px 8px rgba(232, 196, 74, .2), inset 0 -4px 12px rgba(0, 0, 0, .5);
  transition: all var(--transition-base);
}
.fate-dice-sigil::before {
  content: "";
  position: absolute;
  inset: 6px;
  border: 1px solid var(--line-gold);
  border-radius: 12px;
  pointer-events: none;
}
.dice-face {
  font-size: 56px;
  color: var(--gold-bright);
  text-shadow: 0 0 18px var(--gold-glow);
}
.fate-dice-sigil.rolling {
  animation: fateDiceTumble .5s ease-in-out infinite;
  box-shadow: 0 0 48px var(--gold-glow), 0 0 80px rgba(93, 58, 138, .3), inset 0 2px 8px rgba(232, 196, 74, .3);
}
@keyframes fateDiceTumble {
  0%   { transform: rotate(0) scale(1); }
  25%  { transform: rotate(-12deg) scale(1.05); }
  50%  { transform: rotate(8deg) scale(.97); }
  75%  { transform: rotate(-6deg) scale(1.03); }
  100% { transform: rotate(0) scale(1); }
}

.fate-eyebrow {
  font-family: var(--font-heading);
  font-size: 12px;
  color: var(--gold-dim);
  letter-spacing: .4em;
  text-transform: uppercase;
  margin: 0 0 8px;
  position: relative;
  z-index: 2;
}
.fate-title {
  font-family: var(--font-display);
  font-size: 46px;
  font-weight: 900;
  margin: 0 0 6px;
  letter-spacing: .08em;
  background: linear-gradient(180deg, var(--gold-bright), var(--gold), var(--gold-dim));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 28px rgba(201, 162, 39, .18));
  position: relative;
  z-index: 2;
}
.random-sub {
  font-family: var(--font-heading);
  font-size: 12px;
  color: var(--gold-dim);
  letter-spacing: .3em;
  text-transform: uppercase;
  margin: 4px 0 12px;
  position: relative;
  z-index: 2;
}
.random-desc {
  color: var(--ink-soft);
  font-size: 17px;
  margin: 0 0 30px;
  font-style: italic;
  position: relative;
  z-index: 2;
}

/* Roll button */
.fate-roll-btn {
  padding: 18px 52px;
  font-size: 17px;
  letter-spacing: .14em;
  border: 2px solid var(--gold);
  background: linear-gradient(180deg, var(--gold-soft), var(--bg-card));
  color: var(--gold-bright);
  text-shadow: 0 0 12px var(--gold-glow);
  position: relative;
  z-index: 2;
}
.fate-roll-btn:hover:not(:disabled) {
  box-shadow: 0 0 36px var(--gold-glow), 0 0 60px rgba(93, 58, 138, .2);
  border-color: var(--gold-bright);
}
.fate-roll-btn .dice-rolling {
  font-size: 22px;
}

/* Revelation scroll */
.fate-result { max-width: 820px; margin: 0 auto; }
.fate-revelation {
  position: relative;
  padding: 36px 28px 28px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-deep), 0 0 36px var(--gold-glow);
}
.revelation-seal {
  position: absolute;
  top: -16px;
  left: 50%;
  transform: translateX(-50%);
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--gold);
  color: var(--gold-bright);
  font-size: 18px;
  box-shadow: 0 0 18px var(--gold-glow);
}
.revelation-eyebrow {
  font-family: var(--font-heading);
  font-size: 11px;
  color: var(--gold-dim);
  letter-spacing: .3em;
  text-transform: uppercase;
  text-align: center;
  margin: 0 0 16px;
}
.fate-actions {
  justify-content: space-between;
  margin-bottom: 18px;
}
.fate-rolling-state {
  max-width: 820px;
  margin: 0 auto;
  padding: 56px 24px;
  text-align: center;
  border-radius: var(--radius-lg);
}
.fate-rolling-state .empty-icon {
  font-size: 52px;
  color: var(--gold);
}

/* History rune tokens */
.fate-chronicle { margin-top: 56px; max-width: 820px; margin-inline: auto; }
.fate-tokens { gap: 10px; }
.fate-token {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px 10px 14px;
  border: 1px solid var(--line);
  border-radius: 40px;
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  cursor: pointer;
  font-family: var(--font-body);
  font-size: 14px;
  color: var(--ink);
  transition: all var(--transition-base);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .02);
}
.fate-token:hover {
  border-color: var(--gold);
  transform: translateY(-2px);
  box-shadow: 0 4px 18px var(--gold-glow);
  background: linear-gradient(180deg, var(--gold-soft), var(--bg-stone));
}
.fate-token-rune {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle, var(--gold-soft), transparent);
  border: 1px solid var(--line-gold);
  color: var(--gold);
  font-size: 11px;
  flex-shrink: 0;
  transition: all var(--transition-base);
}
.fate-token:hover .fate-token-rune {
  color: var(--gold-bright);
  border-color: var(--gold);
  box-shadow: 0 0 10px var(--gold-glow);
  transform: rotate(45deg);
}
.fate-token .hi-cat { font-size: 12px; color: var(--muted); font-style: italic; }

@media (max-width: 560px) {
  .fate-title { font-size: 34px; }
  .fate-dice-sigil { width: 92px; height: 92px; }
  .dice-face { font-size: 46px; }
  .fate-roll-btn { padding: 16px 36px; font-size: 15px; }
  .fate-revelation { padding: 30px 18px 22px; }
}
</style>
