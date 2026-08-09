<script setup>
import { useTheme } from "../composables/useTheme";

const props = defineProps({
  apiOnline: Boolean,
});

const { theme, toggle } = useTheme();
</script>

<template>
  <header class="topbar ornate-bar">
    <!-- Ornate bottom border line -->
    <div class="bar-glow-line" aria-hidden="true"></div>

    <div class="topbar-brand bar-crest">
      <span class="crest-sigil" aria-hidden="true">⚜</span>
      <div class="crest-text">
        <p class="eyebrow crest-eyebrow">Rainbow Vajra Codex</p>
        <h1 class="crest-title text-glow-gold">彩虹金刚</h1>
      </div>
      <span class="crest-sigil right" aria-hidden="true">⚜</span>
    </div>

    <div class="topbar-actions bar-actions">
      <div class="api-state bar-oracle" :class="{ online: props.apiOnline }">
        <span class="status-dot oracle-dot"></span>
        <span class="oracle-text">{{ props.apiOnline ? "资料库已连接" : "后端未连接" }}</span>
      </div>
      <button
        class="theme-toggle bar-toggle"
        :title="theme === 'light' ? '切换到暗色模式' : '切换到亮色模式'"
        @click="toggle"
      >
        <span v-if="theme === 'light'" class="theme-icon">🌙</span>
        <span v-else class="theme-icon">☀️</span>
      </button>
    </div>
  </header>
</template>

<style scoped>
.topbar.ornate-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 32px;
  background: linear-gradient(180deg, rgba(11, 9, 8, .96), rgba(17, 13, 10, .90));
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(16px);
  position: sticky;
  top: 0;
  z-index: 50;
  transition: background .4s ease;
}

/* Ornate glowing bottom line */
.bar-glow-line {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    var(--line-gold) 15%,
    var(--gold-glow) 50%,
    var(--line-gold) 85%,
    transparent
  );
  pointer-events: none;
}
.bar-glow-line::after {
  content: "✦";
  position: absolute;
  bottom: -7px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: var(--gold-dim);
  text-shadow: 0 0 8px var(--gold-glow);
  background: var(--bg-dark);
  padding: 0 8px;
}

/* Brand crest */
.topbar-brand.bar-crest {
  display: flex;
  align-items: center;
  gap: 14px;
}
.crest-sigil {
  font-size: 24px;
  color: var(--gold);
  filter: drop-shadow(0 0 8px var(--gold-glow));
  animation: crestSigilPulse 4s ease-in-out infinite;
}
.crest-sigil.right {
  animation-delay: 2s;
}
@keyframes crestSigilPulse {
  0%, 100% { opacity: .7; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.08); }
}
.crest-text {
  display: flex;
  flex-direction: column;
}
.crest-eyebrow {
  font-family: var(--font-heading);
  font-size: 10px;
  color: var(--gold-dim);
  letter-spacing: .3em;
  text-transform: uppercase;
  margin: 0 0 2px;
}
.crest-title {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 900;
  letter-spacing: .08em;
  margin: 0;
  background: linear-gradient(180deg, var(--gold-bright), var(--gold), var(--gold-dim));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 2px 4px rgba(0, 0, 0, .5);
}

/* Actions */
.topbar-actions.bar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* API state — oracle crystal */
.api-state.bar-oracle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--bg-stone);
  font-family: var(--font-heading);
  font-size: 12px;
  letter-spacing: .06em;
  color: var(--muted);
  transition: all var(--transition-base);
}
.api-state.bar-oracle.online {
  border-color: var(--nature);
  color: var(--nature);
  background: var(--nature-soft);
  box-shadow: 0 0 10px rgba(74, 122, 58, .15);
}
.oracle-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--crimson);
  box-shadow: 0 0 6px rgba(107, 29, 29, .6);
  animation: oraclePulse 2s ease-in-out infinite;
}
.api-state.bar-oracle.online .oracle-dot {
  background: var(--nature);
  box-shadow: 0 0 8px rgba(74, 122, 58, .6);
}
@keyframes oraclePulse {
  0%, 100% { opacity: .6; }
  50% { opacity: 1; }
}
.oracle-text {
  white-space: nowrap;
}

/* Theme toggle — rune button */
.theme-toggle.bar-toggle {
  width: 40px;
  height: 40px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: radial-gradient(circle, var(--bg-card), var(--bg-stone));
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all var(--transition-base);
  color: var(--gold);
  position: relative;
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, .1), 0 2px 8px rgba(0, 0, 0, .4);
}
.theme-toggle.bar-toggle::before {
  content: "";
  position: absolute;
  inset: -3px;
  border: 1px dashed var(--line-gold);
  border-radius: 50%;
  opacity: 0;
  transition: opacity var(--transition-base);
}
.theme-toggle.bar-toggle:hover {
  border-color: var(--gold);
  color: var(--gold-bright);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, .2), 0 0 16px var(--gold-glow);
  transform: rotate(15deg);
}
.theme-toggle.bar-toggle:hover::before {
  opacity: 1;
  animation: barToggleRotate 8s linear infinite;
}
@keyframes barToggleRotate {
  to { transform: rotate(360deg); }
}
.theme-icon {
  filter: drop-shadow(0 0 4px var(--gold-glow));
}

@media (max-width: 560px) {
  .topbar.ornate-bar { padding: 12px 16px; }
  .crest-title { font-size: 18px; }
  .crest-eyebrow { display: none; }
  .crest-sigil { font-size: 20px; }
  .oracle-text { display: none; }
  .api-state.bar-oracle { padding: 6px 10px; }
}
</style>
