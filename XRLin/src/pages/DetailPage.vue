<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useApi } from "../composables/useApi";
import { useRecentlyViewed } from "../composables/useRecentlyViewed";
import FavoriteBtn from "../components/FavoriteBtn.vue";
import DetailView from "../components/DetailView.vue";
import LoadingSkeleton from "../components/LoadingSkeleton.vue";

const route = useRoute();
const router = useRouter();
const { loading, error, fetchEntry, fetchRandomSome } = useApi();
const { addToRecent } = useRecentlyViewed();

const entry = ref(null);
const related = ref([]);

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
    entry.value = await fetchEntry(route.params.category, route.params.id);
    addToRecent(entry.value);
    // Load related entries from the same category
    if (entry.value) {
      related.value = await fetchRandomSome(entry.value.category, 3);
    }
  } catch {
    // handled by error ref
  }
});
</script>

<template>
  <div class="detail-page codex">
    <!-- Breadcrumb as a path of runes -->
    <nav class="breadcrumb codex-trail">
      <router-link to="/" class="trail-rune">⌂ 首页</router-link>
      <span class="trail-link" aria-hidden="true">❖</span>
      <router-link to="/browse" class="trail-rune">浏览</router-link>
      <span class="trail-link" aria-hidden="true">❖</span>
      <span class="current trail-rune active">{{ entry?.name || '详情' }}</span>
    </nav>

    <!-- Error -->
    <div v-if="error" class="empty-state codex-empty surface-stone border-ornate">
      <span class="empty-icon">⚠</span>
      <p>{{ error }}</p>
      <button class="retry-btn" @click="router.back()">退回</button>
    </div>

    <!-- Loading -->
    <LoadingSkeleton v-else-if="loading" :count="1" />

    <!-- Content -->
    <template v-else-if="entry">
      <div class="detail-actions codex-actions">
        <button class="btn-back" @click="router.back()">⟵ 返回卷宗</button>
        <FavoriteBtn :entry-id="entry.id" />
      </div>

      <div class="codex-manuscript surface-parchment border-ornate corner-flourish">
        <div class="manuscript-seal" aria-hidden="true">⚜</div>
        <DetailView :entry="entry" :category-label="categoryLabels[entry.category]" />
      </div>

      <!-- Ornate divider -->
      <div class="codex-divider" aria-hidden="true">
        <span class="cd-line"></span>
        <span class="cd-glyph">✦</span>
        <span class="cd-glyph small">❖</span>
        <span class="cd-glyph">✦</span>
        <span class="cd-line"></span>
      </div>

      <!-- Related -->
      <section v-if="related.length" class="related-section codex-related">
        <h2 class="section-title">🔗 关联卷轴</h2>
        <div class="related-cards codex-scrolls">
          <router-link
            v-for="item in related"
            :key="item.id"
            :to="`/detail/${item.category}/${item.id}`"
            class="related-card codex-scroll surface-stone border-ornate"
          >
            <span class="scroll-rune" aria-hidden="true">❖</span>
            <strong>{{ item.name }}</strong>
            <span>{{ item.subtitle }}</span>
          </router-link>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.codex {
  max-width: 920px;
  margin: 0 auto;
  padding: 36px 24px;
}

/* Breadcrumb rune path */
.codex-trail {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 30px;
  font-family: var(--font-heading);
  font-size: 13px;
  color: var(--muted);
  letter-spacing: .06em;
}
.trail-rune {
  padding: 6px 14px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: var(--bg-stone);
  color: var(--gold);
  text-decoration: none;
  transition: all var(--transition-base);
  font-weight: 600;
}
.trail-rune:hover {
  border-color: var(--gold);
  color: var(--gold-bright);
  box-shadow: 0 0 12px var(--gold-glow);
}
.trail-rune.active {
  border-color: var(--gold);
  background: var(--gold-soft);
  color: var(--gold-bright);
  box-shadow: inset 0 0 10px rgba(201, 162, 39, .1);
}
.trail-link {
  color: var(--gold-dim);
  font-size: 11px;
}

/* Empty state */
.codex-empty {
  padding: 48px 28px;
  border-radius: var(--radius-lg);
  text-align: center;
}

/* Actions */
.codex-actions { margin-bottom: 20px; }

/* Manuscript */
.codex-manuscript {
  position: relative;
  padding: 38px 32px 30px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-deep), 0 0 30px var(--gold-glow);
  animation: fadeScaleIn .4s ease-out;
}
.manuscript-seal {
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

/* Ornate divider */
.codex-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin: 48px 0;
  max-width: 920px;
}
.cd-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-gold), transparent);
}
.cd-glyph {
  font-size: 14px;
  color: var(--gold);
  filter: drop-shadow(0 0 6px var(--gold-glow));
}
.cd-glyph.small { font-size: 11px; color: var(--gold-dim); }

/* Related scrolls */
.codex-related { max-width: 920px; margin: 0 auto; }
.codex-scrolls { gap: 16px; }
.codex-scroll {
  position: relative;
  flex: 1;
  min-width: 200px;
  padding: 22px 20px 18px;
  border-radius: var(--radius-md);
  text-decoration: none;
  color: var(--ink);
  transition: all var(--transition-base);
}
.codex-scroll:hover {
  border-color: var(--gold);
  transform: translateY(-4px);
  box-shadow: 0 6px 24px var(--gold-glow);
}
.scroll-rune {
  position: absolute;
  top: 10px;
  right: 14px;
  font-size: 14px;
  color: var(--gold-dim);
  opacity: .5;
  transition: all var(--transition-base);
}
.codex-scroll:hover .scroll-rune {
  opacity: 1;
  color: var(--gold-bright);
  transform: rotate(45deg);
}
.codex-scroll strong {
  display: block;
  margin-bottom: 4px;
  font-family: var(--font-heading);
  font-size: 16px;
  color: var(--ink-bright);
  letter-spacing: .04em;
}
.codex-scroll span { font-size: 13px; color: var(--muted); font-style: italic; }

@media (max-width: 560px) {
  .codex-manuscript { padding: 32px 18px 24px; }
  .codex-scroll { min-width: 100%; }
}
</style>
