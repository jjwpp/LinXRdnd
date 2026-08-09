<script setup>
import { ref } from "vue";

const model = defineModel({ type: String, default: "" });
const inputRef = ref(null);

function focus() {
  inputRef.value?.focus();
}

defineExpose({ focus });
</script>

<template>
  <div class="search-panel inscription-panel">
    <label for="searchInput" class="inscription-label">
      <span class="label-sigil">⟁</span>
      <span class="label-text">奥术检索</span>
    </label>
    <div class="search-input-wrap inscription-wrap">
      <span class="inscription-glyph-left" aria-hidden="true">✦</span>
      <input
        id="searchInput"
        ref="inputRef"
        v-model="model"
        type="search"
        class="inscription-input"
        placeholder="铭刻想要寻觅的奥秘…"
        autocomplete="off"
      >
      <button
        v-if="model"
        class="search-clear inscription-clear"
        title="清除铭文"
        @click="model = ''"
      >
        ✕
      </button>
    </div>
  </div>
</template>

<style scoped>
/* ===== Magical inscription search area ===== */
.inscription-panel {
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

.inscription-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 13px;
  letter-spacing: .18em;
  color: var(--gold);
  text-transform: uppercase;
  white-space: nowrap;
  text-shadow: 0 0 10px var(--gold-glow);
}
.inscription-label .label-sigil {
  font-size: 16px;
  color: var(--gold-bright);
  filter: drop-shadow(0 0 6px var(--gold-glow));
  animation: sigilPulse 3s ease-in-out infinite;
}
@keyframes sigilPulse {
  0%, 100% { opacity: .7; transform: rotate(0); }
  50% { opacity: 1; transform: rotate(8deg); }
}

/* The inscription frame — gold-trimmed carved stone */
.inscription-wrap {
  position: relative;
  border: 1px solid var(--gold-dim);
  border-radius: var(--radius-sm);
  background:
    linear-gradient(180deg, var(--bg-stone), var(--bg-dark));
  box-shadow:
    inset 0 1px 0 rgba(201, 162, 39, .12),
    inset 0 -1px 0 rgba(0, 0, 0, .6),
    0 2px 10px rgba(0, 0, 0, .4);
  transition: box-shadow var(--transition-base), border-color var(--transition-base);
}
/* inner ornate border */
.inscription-wrap::before {
  content: "";
  position: absolute;
  inset: 3px;
  border: 1px solid var(--line);
  border-radius: 2px;
  pointer-events: none;
  transition: border-color var(--transition-base);
}
/* glowing rune along the top edge when focused */
.inscription-wrap::after {
  content: "";
  position: absolute;
  top: -1px; left: 10%; right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--gold), transparent);
  opacity: 0;
  transition: opacity var(--transition-base);
}

.inscription-glyph-left {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  color: var(--gold-dim);
  filter: drop-shadow(0 0 4px var(--gold-soft));
  z-index: 2;
  transition: color var(--transition-base);
}

.inscription-input {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: var(--radius-sm);
  padding: 0 44px 0 40px;
  background: transparent;
  color: var(--ink-bright);
  font-family: var(--font-body);
  font-size: 16px;
  letter-spacing: .02em;
  position: relative;
  z-index: 1;
  transition: color var(--transition-base);
}
.inscription-input:focus {
  outline: none;
}
.inscription-wrap:focus-within {
  border-color: var(--gold);
  box-shadow:
    inset 0 1px 0 rgba(201, 162, 39, .2),
    0 0 0 3px var(--gold-soft),
    0 0 22px var(--gold-glow);
}
.inscription-wrap:focus-within::before { border-color: var(--line-gold); }
.inscription-wrap:focus-within::after { opacity: 1; }
.inscription-wrap:focus-within .inscription-glyph-left {
  color: var(--gold-bright);
  filter: drop-shadow(0 0 8px var(--gold-glow));
}
.inscription-input::placeholder {
  color: var(--muted);
  font-family: var(--font-uncial);
  font-style: italic;
  letter-spacing: .04em;
}

.inscription-clear {
  right: 8px;
  border-color: var(--gold-dim);
  background: var(--bg-card);
  color: var(--gold);
  z-index: 2;
}
.inscription-clear:hover {
  background: var(--crimson);
  color: #fff;
  border-color: var(--crimson-bright);
  box-shadow: 0 0 10px var(--crimson-soft);
}

@media (max-width: 560px) {
  .inscription-panel { grid-template-columns: 1fr; gap: 8px; }
  .inscription-label { justify-content: center; }
}
</style>
