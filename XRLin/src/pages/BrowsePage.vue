<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useApi } from "../composables/useApi";
import { useRecentlyViewed } from "../composables/useRecentlyViewed";
import { useFavorites } from "../composables/useFavorites";
import Sidebar from "../components/Sidebar.vue";
import SearchBar from "../components/SearchBar.vue";
import EntryList from "../components/EntryList.vue";
import DetailPanel from "../components/DetailPanel.vue";
import ComparePanel from "../components/ComparePanel.vue";

const route = useRoute();
const router = useRouter();
const { loading, error, checkHealth, fetchCategories, fetchEntries } = useApi();
const { addToRecent } = useRecentlyViewed();
const { favorites } = useFavorites();

const categoryLabels = {
  class: "职业",
  race: "种族",
  spell: "法术",
  monster: "怪物",
  "magic-item": "魔法物品",
  feat: "专长",
};

const categories = ref([]);
const entries = ref([]);
const selectedEntry = ref(null);
const selectedId = ref(null);
const activeCategory = ref(route.query.category || "");
const keyword = ref(route.query.q || "");
const searchInputRef = ref(null);
const compareList = ref([]);
const showFavoritesOnly = ref(false);

const totalCount = computed(() =>
  categories.value.reduce((sum, item) => sum + item.count, 0)
);

const displayEntries = computed(() => {
  if (showFavoritesOnly.value) {
    return entries.value.filter((e) => favorites.value.includes(e.id));
  }
  return entries.value;
});

onMounted(async () => {
  try {
    await checkHealth();
    await loadCategories();
    await loadEntries();
  } catch (e) {
    console.error("初始化失败:", e);
  }
  window.addEventListener("keydown", onKeyDown);
});

onUnmounted(() => {
  window.removeEventListener("keydown", onKeyDown);
});

watch([activeCategory, keyword], () => {
  router.replace({ query: { category: activeCategory.value || undefined, q: keyword.value || undefined } });
  loadEntries();
});

watch(selectedEntry, (entry) => {
  if (entry) addToRecent(entry);
});

async function loadCategories() {
  categories.value = await fetchCategories();
}

async function loadEntries() {
  try {
    entries.value = await fetchEntries(
      activeCategory.value || null,
      keyword.value || null
    );

    if (!entries.value.length) {
      selectedEntry.value = null;
      selectedId.value = null;
      return;
    }

    const stillVisible = entries.value.find((item) => item.id === selectedId.value);
    if (!stillVisible) {
      selectedEntry.value = entries.value[0];
      selectedId.value = entries.value[0]?.id || null;
    }
  } catch {
    entries.value = [];
  }
}

function setCategory(category) {
  activeCategory.value = category;
}

function selectEntry(entry) {
  selectedEntry.value = entry;
  selectedId.value = entry.id;
}

function goToDetail(entry) {
  addToRecent(entry);
  router.push(`/detail/${entry.category}/${entry.id}`);
}

function toggleCompare(entry) {
  const idx = compareList.value.findIndex((e) => e.id === entry.id);
  if (idx >= 0) {
    compareList.value.splice(idx, 1);
  } else if (compareList.value.length < 2) {
    compareList.value.push(entry);
  }
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: "smooth" });
}

// Keyboard
const selectedIndex = computed(() =>
  entries.value.findIndex((e) => e.id === selectedId.value)
);

function onKeyDown(e) {
  if (e.target.tagName === "INPUT" || e.target.tagName === "TEXTAREA" || e.target.isContentEditable) {
    if (e.key === "Escape") searchInputRef.value?.focus();
    return;
  }
  switch (e.key) {
    case "ArrowDown":
    case "j":
      e.preventDefault();
      if (entries.value.length) {
        const next = (selectedIndex.value + 1) % entries.value.length;
        selectEntry(entries.value[next]);
      }
      break;
    case "ArrowUp":
    case "k":
      e.preventDefault();
      if (entries.value.length) {
        const prev = (selectedIndex.value - 1 + entries.value.length) % entries.value.length;
        selectEntry(entries.value[prev]);
      }
      break;
    case "Enter":
      if (selectedEntry.value) goToDetail(selectedEntry.value);
      break;
    case "Escape":
      if (keyword.value) {
        keyword.value = "";
      } else if (activeCategory.value) {
        setCategory("");
      }
      break;
    case "/":
      e.preventDefault();
      searchInputRef.value?.focus();
      break;
  }
}
</script>

<template>
  <main class="browse-layout codex">
    <Sidebar
      :categories="categories"
      :active-category="activeCategory"
      :total-count="totalCount"
      :show-favorites="showFavoritesOnly"
      @select="setCategory"
      @toggle-favorites="showFavoritesOnly = !showFavoritesOnly"
    />

    <section class="browse-workspace codex-page">
      <span class="codex-spine" aria-hidden="true"></span>

      <header class="codex-header">
        <div class="divider-ornament"><span>✦</span></div>
        <h1 class="codex-title text-engraved-gold">古卷典籍</h1>
        <p class="codex-subtitle">翻阅尘封之卷 · Codex of the Forgotten Realms</p>
        <div class="divider-ornament"><span>✦</span></div>
      </header>

      <SearchBar ref="searchInputRef" v-model="keyword" />

      <!-- Error -->
      <div v-if="error && !entries.length" class="empty-state codex-empty">
        <span class="empty-icon">⚠️</span>
        <p>{{ error }}</p>
        <button class="retry-btn" @click="loadEntries">重试</button>
      </div>

      <div v-else class="content-grid">
        <EntryList
          :entries="displayEntries"
          :loading="loading"
          :selected-id="selectedId"
          :category-labels="categoryLabels"
          @select="selectEntry"
          @compare="toggleCompare"
        />

        <DetailPanel
          :entry="selectedEntry"
          :category-label="categoryLabels[selectedEntry?.category]"
          @view-detail="goToDetail"
        />
      </div>
    </section>
  </main>

  <ComparePanel
    :entries="compareList"
    @remove="(id) => compareList = compareList.filter(e => e.id !== id)"
  />

  <Transition name="detail-fade">
    <button v-if="entries.length" class="back-to-top codex-back-top" @click="scrollToTop">↑</button>
  </Transition>
</template>

<style scoped>
/* ===== Codex framing — feels like an opened spellbook ===== */
.codex {
  position: relative;
}

/* The open parchment page */
.codex-page {
  position: relative;
  padding: 30px 32px 40px;
  background:
    linear-gradient(180deg, rgba(42, 31, 23, .55), rgba(17, 13, 10, .85)),
    var(--texture-parchment) center / cover;
  border-left: 1px solid var(--line-gold);
  box-shadow: inset 0 0 80px rgba(0, 0, 0, .55);
}

/* Leather book spine between sidebar and page */
.codex-spine {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 14px;
  background:
    linear-gradient(90deg, rgba(0, 0, 0, .6), rgba(60, 40, 24, .35) 40%, rgba(0, 0, 0, .55));
  border-right: 1px solid rgba(201, 162, 39, .18);
  box-shadow: inset -2px 0 6px rgba(0, 0, 0, .6);
  pointer-events: none;
  z-index: 1;
}

/* ===== Codex header ===== */
.codex-header {
  text-align: center;
  margin-bottom: 26px;
  position: relative;
  z-index: 2;
}
.codex-header .divider-ornament {
  margin: 4px auto;
  max-width: 420px;
}
.codex-title {
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 900;
  letter-spacing: .12em;
  margin: 4px 0;
  line-height: 1.1;
}
.codex-subtitle {
  font-family: var(--font-heading);
  font-size: 12px;
  letter-spacing: .35em;
  text-transform: uppercase;
  color: var(--gold-dim);
  margin: 6px 0 0;
}

.codex-empty {
  border-color: var(--crimson);
  background: rgba(107, 29, 29, .08);
}

/* Back-to-top as a glowing rune seal */
.codex-back-top {
  font-family: var(--font-rune);
  border-color: var(--gold-dim);
  background: radial-gradient(circle, var(--bg-card), var(--bg-stone));
}
.codex-back-top:hover {
  border-color: var(--gold);
  color: var(--gold-bright);
}

@media (max-width: 920px) {
  .codex-page { padding: 22px 18px 30px; }
  .codex-spine { display: none; }
  .codex-title { font-size: 26px; }
}
</style>
