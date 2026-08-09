<script setup>
import { useFavorites } from "../composables/useFavorites";

const props = defineProps({
  entryId: String,
});

const { isFavorite, toggle } = useFavorites();
</script>

<template>
  <button
    class="fav-btn sigil-btn"
    :class="{ favorited: isFavorite(entryId) }"
    :title="isFavorite(entryId) ? '解除封印' : '封存此卷'"
    @click.stop="toggle(entryId)"
  >
    <span class="sigil-glyph">{{ isFavorite(entryId) ? '✦' : '✧' }}</span>
    <span class="sigil-ring" aria-hidden="true"></span>
  </button>
</template>

<style scoped>
/* ===== Magical bookmark sigil ===== */
.sigil-btn {
  position: relative;
  width: 38px;
  height: 38px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: radial-gradient(circle, var(--bg-card), var(--bg-stone));
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: all var(--transition-base);
  box-shadow:
    inset 0 1px 0 rgba(201, 162, 39, .1),
    inset 0 -1px 0 rgba(0, 0, 0, .5),
    0 2px 6px rgba(0, 0, 0, .4);
}

.sigil-glyph {
  font-family: var(--font-rune);
  font-size: 18px;
  color: var(--gold-dim);
  line-height: 1;
  transition: color var(--transition-base), transform var(--transition-base), text-shadow var(--transition-base);
  z-index: 1;
}

/* rotating dashed ring around the sigil */
.sigil-ring {
  position: absolute;
  inset: -4px;
  border: 1px dashed var(--line-gold);
  border-radius: 50%;
  opacity: 0;
  transition: opacity var(--transition-base);
  pointer-events: none;
}

.sigil-btn:hover {
  transform: scale(1.12);
  border-color: var(--gold);
  box-shadow:
    inset 0 1px 0 rgba(201, 162, 39, .2),
    0 0 16px var(--gold-glow);
}
.sigil-btn:hover .sigil-glyph {
  color: var(--gold-bright);
  transform: rotate(15deg);
  text-shadow: 0 0 10px var(--gold-glow);
}
.sigil-btn:hover .sigil-ring { opacity: .7; animation: spinRing 6s linear infinite; }
@keyframes spinRing { to { transform: rotate(360deg); } }

/* Favorited — glowing sealed sigil */
.sigil-btn.favorited {
  border-color: var(--gold);
  background: radial-gradient(circle, rgba(201, 162, 39, .18), var(--bg-stone));
  box-shadow:
    inset 0 1px 0 rgba(201, 162, 39, .25),
    0 0 18px var(--gold-glow);
}
.sigil-btn.favorited .sigil-glyph {
  color: var(--gold-bright);
  text-shadow: 0 0 12px var(--gold-glow);
  animation: sigilPulse 2.4s ease-in-out infinite;
}
.sigil-btn.favorited .sigil-ring {
  opacity: 1;
  border-color: var(--gold);
  animation: spinRing 8s linear infinite;
}
@keyframes sigilPulse {
  0%, 100% { opacity: .9; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.12); }
}
</style>
