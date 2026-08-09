const { createApp, ref, computed, onMounted, watch } = Vue;

createApp({
  setup() {
    const categories = ref([
      { id: 'class', name: '职业', icon: '⚔️', api: '/api/classes' },
      { id: 'race', name: '种族', icon: '🧝', api: '/api/races' },
      { id: 'spell', name: '法术', icon: '✨', api: '/api/spells' },
      { id: 'monster', name: '怪物', icon: '👹', api: '/api/monsters' },
      { id: 'magic-item', name: '魔法物品', icon: '💍', api: '/api/items' },
      { id: 'feat', name: '专长', icon: '🎯', api: '/api/feats' }
    ]);
    const entries = ref([]);
    const randomItems = ref([]);
    const selectedCategory = ref('');
    const keyword = ref('');
    const loading = ref(false);
    const selectedEntry = ref(null);
    let searchTimer = null;

    const totalCount = computed(() =>
      categories.value.length > 0 ? categories.value.length * 15 : 114
    );

    function apiFor(catId) {
      const cat = categories.value.find(c => c.id === catId);
      return cat ? cat.api : '';
    }

    async function apiGet(path) {
      const res = await fetch(path);
      if (!res.ok) throw new Error('API error: ' + res.status);
      const json = await res.json();
      return json.data;
    }

    async function fetchEntries() {
      loading.value = true;
      try {
        if (selectedCategory.value) {
          const base = apiFor(selectedCategory.value);
          const params = new URLSearchParams();
          if (keyword.value.trim()) params.set('q', keyword.value.trim());
          const qs = params.toString();
          entries.value = await apiGet(base + (qs ? '?' + qs : ''));
        } else {
          // Search all categories in parallel
          const all = await Promise.all(
            categories.value.map(c =>
              apiGet(c.api).catch(() => [])
            )
          );
          let merged = all.flat();
          if (keyword.value.trim()) {
            const kw = keyword.value.trim().toLowerCase();
            merged = merged.filter(e =>
              (e.name && e.name.toLowerCase().includes(kw)) ||
              (e.subtitle && e.subtitle.toLowerCase().includes(kw)) ||
              (e.summary && e.summary.toLowerCase().includes(kw)) ||
              (e.tags && e.tags.some(t => t && t.toLowerCase().includes(kw)))
            );
          }
          entries.value = merged;
        }
      } catch (e) {
        console.error('Failed to fetch entries:', e);
        entries.value = [];
      } finally {
        loading.value = false;
      }
    }

    async function fetchRandom() {
      try {
        const results = [];
        for (const cat of categories.value) {
          try {
            const item = await apiGet(cat.api + '/random');
            if (item) results.push(item);
          } catch (e) { /* skip */ }
        }
        randomItems.value = results;
      } catch (e) {
        randomItems.value = [];
      }
    }

    async function handleRandomExplore() {
      loading.value = true;
      try {
        const cat = categories.value[Math.floor(Math.random() * categories.value.length)];
        const item = await apiGet(cat.api + '/random');
        if (item) selectedEntry.value = item;
      } catch (e) {
        console.error('Random failed:', e);
      } finally {
        loading.value = false;
      }
    }

    function selectCategory(id) {
      selectedCategory.value = id;
      fetchEntries();
    }

    function onSearch() {
      clearTimeout(searchTimer);
      searchTimer = setTimeout(() => fetchEntries(), 300);
    }

    function openDetail(entry) {
      selectedEntry.value = entry;
      document.body.style.overflow = 'hidden';
    }

    function closeDetail() {
      selectedEntry.value = null;
      document.body.style.overflow = '';
    }

    function onKeydown(e) {
      if (e.key === 'Escape') closeDetail();
    }

    onMounted(() => {
      fetchEntries();
      fetchRandom();
      document.addEventListener('keydown', onKeydown);
    });

    return {
      categories, entries, randomItems, selectedCategory, keyword,
      loading, selectedEntry, totalCount,
      selectCategory, onSearch, openDetail, closeDetail, handleRandomExplore
    };
  }
}).mount('#app');
