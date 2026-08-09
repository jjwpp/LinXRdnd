<script setup>
import { ref, onMounted } from "vue";
import { useApi } from "../composables/useApi";
import EntryCard from "./EntryCard.vue";

const { fetchRandomSome } = useApi();
const entries = ref([]);

const categoryLabels = {
  class: "职业",
  race: "种族",
  spell: "法术",
  monster: "怪物",
  "magic-item": "魔法物品",
  feat: "专长",
};

onMounted(async () => {
  try {
    entries.value = await fetchRandomSome(null, 3);
  } catch {
    // silent
  }
});
</script>

<template>
  <section v-if="entries.length" class="featured-section">
    <div class="divider-ornament" aria-hidden="true">
      <span>✦ ORACLES ✦</span>
    </div>
    <h2 class="section-title featured-title">
      <span class="title-rune" aria-hidden="true">🎯</span>
      命运之选
    </h2>
    <p class="featured-intro">卷轴自行翻开，浮现三段命运的启示</p>
    <div class="cards">
      <EntryCard
        v-for="entry in entries"
        :key="entry.id"
        :entry="entry"
        :category-label="categoryLabels[entry.category]"
      />
    </div>
  </section>
</template>

<style scoped>
.featured-section {
  position: relative;
}
.featured-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
}
.title-rune {
  font-size: 22px;
  filter: drop-shadow(0 0 8px var(--gold-glow));
  animation: oraclePulse 3s ease-in-out infinite;
}
@keyframes oraclePulse {
  0%, 100% { transform: scale(1) rotate(0); opacity: .85; }
  50% { transform: scale(1.12) rotate(-6deg); opacity: 1; }
}
.featured-intro {
  font-family: var(--font-body);
  font-style: italic;
  color: var(--muted);
  font-size: 14px;
  margin: -12px 0 24px;
  letter-spacing: .04em;
}
</style>
