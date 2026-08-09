<script setup>
import { ref, onMounted } from "vue";

const visible = ref(false);
onMounted(() => {
  requestAnimationFrame(() => {
    visible.value = true;
  });
});
</script>

<template>
  <section class="hero banner-sigil" :class="{ visible }">
    <!-- Atmospheric backdrop layers -->
    <div class="banner-bg-glow" aria-hidden="true"></div>
    <div class="banner-rune-circle" aria-hidden="true"></div>
    <div class="banner-rune-ring" aria-hidden="true"></div>

    <!-- Ornate top frame -->
    <div class="banner-frame-top" aria-hidden="true">
      <span class="bt-left">❖</span>
      <span class="bt-line"></span>
      <span class="bt-center">⚜</span>
      <span class="bt-line"></span>
      <span class="bt-right">❖</span>
    </div>

    <div class="hero-content banner-content">
      <p class="hero-tagline banner-tagline">
        <span class="tag-rune" aria-hidden="true">✦</span>
        DND世界资料台
        <span class="tag-rune" aria-hidden="true">✦</span>
      </p>
      <h2 class="banner-title text-glow-gold">把职业、血脉、法术与遭遇放进同一个冒险控制台。</h2>
      <div class="banner-ornaments" aria-hidden="true">
        <span class="bo-dot crimson"></span>
        <span class="bo-dot gold"></span>
        <span class="bo-dot arcane"></span>
        <span class="bo-dot frost"></span>
      </div>
    </div>

    <!-- Ornate bottom frame -->
    <div class="banner-frame-bottom" aria-hidden="true">
      <span class="bb-left">❖</span>
      <span class="bb-line"></span>
      <span class="bb-center">✦</span>
      <span class="bb-line"></span>
      <span class="bb-right">❖</span>
    </div>

    <div class="hero-rings banner-rings" aria-hidden="true">
      <span></span><span></span><span></span><span></span>
    </div>
  </section>
</template>

<style scoped>
.hero.banner-sigil {
  position: relative;
  text-align: center;
  padding: 80px 24px 72px;
  overflow: hidden;
  opacity: 0;
  transform: translateY(24px);
  transition: opacity .9s ease, transform .9s ease;
}
.hero.banner-sigil.visible {
  opacity: 1;
  transform: translateY(0);
}

/* Background glow */
.banner-bg-glow {
  position: absolute;
  top: -140px;
  left: 50%;
  translate: -50% 0;
  width: 760px;
  height: 760px;
  background: radial-gradient(
    circle,
    rgba(201, 162, 39, .12),
    rgba(93, 58, 138, .06) 40%,
    transparent 70%
  );
  pointer-events: none;
  animation: bannerPulseGlow 6s ease-in-out infinite;
}
@keyframes bannerPulseGlow {
  0%, 100% { opacity: .5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.06); }
}

/* Rune circles */
.banner-rune-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 540px;
  height: 540px;
  border: 1px solid var(--line-gold);
  border-radius: 50%;
  opacity: .12;
  pointer-events: none;
  animation: bannerRotateRunes 80s linear infinite;
}
.banner-rune-circle::before {
  content: "";
  position: absolute;
  inset: 40px;
  border: 1px dashed var(--line-gold);
  border-radius: 50%;
}
.banner-rune-circle::after {
  content: "";
  position: absolute;
  inset: 90px;
  border: 1px solid var(--line-gold);
  border-radius: 50%;
}
.banner-rune-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 320px;
  height: 320px;
  border: 1px solid rgba(93, 58, 138, .15);
  border-radius: 50%;
  pointer-events: none;
  animation: bannerRotateRunes 50s linear infinite reverse;
}
@keyframes bannerRotateRunes {
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

/* Ornate top frame */
.banner-frame-top,
.banner-frame-bottom {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  max-width: 520px;
  margin: 0 auto;
  position: relative;
  z-index: 2;
}
.banner-frame-top { margin-bottom: 28px; }
.banner-frame-bottom { margin-top: 32px; }
.bt-left, .bt-right, .bb-left, .bb-right {
  font-size: 14px;
  color: var(--gold-dim);
  flex-shrink: 0;
}
.bt-center, .bb-center {
  font-size: 18px;
  color: var(--gold);
  text-shadow: 0 0 10px var(--gold-glow);
  flex-shrink: 0;
}
.bt-line, .bb-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-gold), transparent);
}

/* Hero content */
.hero-content.banner-content {
  position: relative;
  z-index: 2;
  max-width: 680px;
  margin: 0 auto;
}

.hero-tagline.banner-tagline {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  font-family: var(--font-heading);
  font-size: 13px;
  color: var(--gold-dim);
  letter-spacing: .4em;
  text-transform: uppercase;
  margin: 0 auto 24px;
}
.tag-rune {
  font-size: 10px;
  color: var(--gold);
  text-shadow: 0 0 6px var(--gold-glow);
  animation: tagRunePulse 3s ease-in-out infinite;
}
@keyframes tagRunePulse {
  0%, 100% { opacity: .5; }
  50% { opacity: 1; }
}

.banner-title {
  font-family: var(--font-display);
  font-size: 38px;
  font-weight: 800;
  letter-spacing: .04em;
  line-height: 1.35;
  margin: 0 auto;
  background: linear-gradient(180deg, var(--gold-bright) 0%, var(--gold) 50%, var(--gold-dim) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 4px 20px rgba(0, 0, 0, .8);
  filter: drop-shadow(0 0 30px rgba(201, 162, 39, .12));
}

/* Ornament dots */
.banner-ornaments {
  display: flex;
  justify-content: center;
  gap: 28px;
  margin-top: 36px;
}
.bo-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  animation: bannerOrnamentPulse 2.5s ease-in-out infinite;
}
.bo-dot.crimson { background: var(--crimson-bright); animation-delay: 0s; }
.bo-dot.gold { background: var(--gold); animation-delay: .5s; }
.bo-dot.arcane { background: var(--arcane-glow); animation-delay: 1s; }
.bo-dot.frost { background: var(--frost); animation-delay: 1.5s; }
@keyframes bannerOrnamentPulse {
  0%, 100% { transform: scale(1); opacity: .4; box-shadow: 0 0 4px currentColor; }
  50% { transform: scale(2); opacity: 1; box-shadow: 0 0 14px currentColor; }
}

/* Legacy rings */
.hero-rings.banner-rings {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
  opacity: .08;
}
.hero-rings.banner-rings span {
  position: absolute;
  top: 50%;
  left: 50%;
  border: 1px solid var(--gold-dim);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}
.hero-rings.banner-rings span:nth-child(1) { width: 600px; height: 600px; }
.hero-rings.banner-rings span:nth-child(2) { width: 460px; height: 460px; border-style: dashed; }
.hero-rings.banner-rings span:nth-child(3) { width: 340px; height: 340px; }
.hero-rings.banner-rings span:nth-child(4) { width: 220px; height: 220px; border-style: dashed; }

@media (max-width: 768px) {
  .banner-title { font-size: 26px; }
  .banner-rune-circle { width: 340px; height: 340px; }
  .banner-rune-ring { width: 220px; height: 220px; }
  .banner-ornaments { gap: 18px; }
}
</style>
