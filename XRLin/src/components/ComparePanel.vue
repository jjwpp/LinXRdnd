<script setup>
import { computed } from "vue";

const props = defineProps({
  entries: Array,
});

defineEmits(["remove"]);

const hasData = computed(() => props.entries?.length > 0);
</script>

<template>
  <div v-if="hasData" class="compare-panel codex-compare">
    <span class="compare-spell-rune" aria-hidden="true">⚔</span>
    <div
      v-for="entry in entries"
      :key="entry.id"
      class="compare-item codex-compare-item"
    >
      <span class="compare-item-rune">❦</span>
      <div class="compare-item-text">
        <strong>{{ entry.name }}</strong>
        <span class="compare-cat">{{ entry.subtitle }}</span>
      </div>
      <button class="compare-remove codex-compare-remove" @click="$emit('remove', entry.id)">✕</button>
    </div>
    <div v-if="entries.length === 2" class="compare-ready codex-compare-ready">
      <span class="ready-rune">⟡</span>
      <span>双卷已列，可决高下</span>
      <span class="ready-rune">⟡</span>
    </div>
  </div>
</template>

<style scoped>
/* ===== Floating comparison scroll ===== */
.codex-compare {
  position: fixed;
  bottom: 22px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 22px;
  border: 1px solid var(--gold-dim);
  border-radius: var(--radius-md);
  background:
    linear-gradient(180deg, rgba(42, 31, 23, .96), rgba(23, 16, 12, .98)),
    var(--texture-parchment) center / cover;
  box-shadow:
    var(--shadow-deep),
    0 0 26px var(--gold-glow),
    inset 0 0 30px rgba(0, 0, 0, .35);
  z-index: 30;
  backdrop-filter: blur(16px);
  animation: floatPulse 4s ease-in-out infinite;
}
@keyframes floatPulse {
  0%, 100% { box-shadow: var(--shadow-deep), 0 0 22px var(--gold-glow), inset 0 0 30px rgba(0, 0, 0, .35); }
  50% { box-shadow: var(--shadow-deep), 0 0 36px var(--gold-glow), inset 0 0 30px rgba(0, 0, 0, .35); }
}
/* inner ornate border */
.codex-compare::before {
  content: "";
  position: absolute;
  inset: 4px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  pointer-events: none;
}
/* top crest */
.codex-compare::after {
  content: "";
  position: absolute;
  top: 0; left: 20%; right: 20%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--gold), transparent);
  pointer-events: none;
}

.compare-spell-rune {
  font-size: 18px;
  color: var(--gold-bright);
  filter: drop-shadow(0 0 8px var(--gold-glow));
  animation: runePulse 2.4s ease-in-out infinite;
}
@keyframes runePulse {
  0%, 100% { opacity: .8; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.15); }
}

.codex-compare-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  background:
    linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--line-gold);
  border-radius: var(--radius-sm);
  font-size: 14px;
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, .1);
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
}
.codex-compare-item:hover {
  border-color: var(--gold);
  box-shadow: 0 0 14px var(--gold-glow);
}
.compare-item-rune {
  font-size: 12px;
  color: var(--gold-dim);
  filter: drop-shadow(0 0 4px var(--gold-soft));
}
.compare-item-text { display: flex; flex-direction: column; line-height: 1.3; }
.compare-item-text strong {
  font-family: var(--font-heading);
  color: var(--ink-bright);
  font-size: 14px;
  letter-spacing: .03em;
}
.compare-cat {
  color: var(--gold-dim);
  font-size: 11px;
  font-style: italic;
  letter-spacing: .04em;
}

.codex-compare-remove {
  background: none;
  border: 1px solid transparent;
  cursor: pointer;
  color: var(--muted);
  font-size: 13px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-base);
}
.codex-compare-remove:hover {
  color: #fff;
  background: var(--crimson);
  border-color: var(--crimson-bright);
  box-shadow: 0 0 10px var(--crimson-soft);
}

.codex-compare-ready {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--gold-bright);
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: .08em;
  padding: 6px 14px;
  border: 1px solid var(--gold);
  border-radius: var(--radius-sm);
  background: var(--gold-soft);
  text-shadow: 0 0 8px var(--gold-glow);
}
.codex-compare-ready .ready-rune {
  font-size: 12px;
  color: var(--gold-bright);
  filter: drop-shadow(0 0 6px var(--gold-glow));
  animation: runePulse 2.4s ease-in-out infinite;
}
</style>
