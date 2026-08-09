<script setup>
defineProps({
  itemCount: { type: Number, default: 0 },
  disabled: { type: Boolean, default: false },
});

const emit = defineEmits(["click"]);
</script>

<template>
  <button
    class="battle-inv-btn"
    :class="{ disabled, 'has-items': itemCount > 0 }"
    :disabled="disabled"
    @click="emit('click')"
  >
    <span class="btn-icon">🎒</span>
    <span class="btn-label">背包</span>
    <span class="btn-count" v-if="itemCount > 0">{{ itemCount }}</span>
    <span class="btn-pulse" v-if="!disabled && itemCount > 0"></span>
  </button>
</template>

<style scoped>
.battle-inv-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--line-light);
  border-radius: var(--radius-sm);
  color: var(--gold);
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: .08em;
  cursor: pointer;
  transition: all .25s ease;
  box-shadow: inset 0 1px 0 rgba(201,162,39,.1), 0 2px 8px rgba(0,0,0,.4);
}
.battle-inv-btn:hover:not(.disabled) {
  border-color: var(--gold);
  color: var(--gold-bright);
  box-shadow: inset 0 1px 0 rgba(201,162,39,.3), 0 0 16px var(--gold-glow);
  transform: translateY(-1px);
}
.battle-inv-btn:active:not(.disabled) {
  transform: translateY(0);
}
.battle-inv-btn.disabled {
  opacity: .4;
  cursor: not-allowed;
}

.btn-icon {
  font-size: 18px;
  filter: drop-shadow(0 0 6px var(--gold-glow));
}

.btn-label {
  text-transform: uppercase;
}

.btn-count {
  font-size: 11px;
  font-weight: 800;
  color: var(--ink-bright);
  background: var(--crimson);
  padding: 1px 7px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.btn-pulse {
  position: absolute;
  inset: -2px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--gold);
  opacity: 0;
  animation: btnPulse 2s ease-in-out infinite;
  pointer-events: none;
}
@keyframes btnPulse {
  0%, 100% { opacity: 0; transform: scale(1); }
  50% { opacity: .4; transform: scale(1.03); }
}
</style>
