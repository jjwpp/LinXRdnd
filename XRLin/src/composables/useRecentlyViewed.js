import { ref } from "vue";

const STORAGE_KEY = "xrlin-recently-viewed";
const MAX_ITEMS = 10;

const viewed = ref(load());

function load() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) || [];
  } catch {
    return [];
  }
}

function save() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(viewed.value));
}

export function useRecentlyViewed() {
  function add(entry) {
    if (!entry?.id) return;
    // Remove if already exists
    viewed.value = viewed.value.filter((item) => item.id !== entry.id);
    // Add to front
    viewed.value.unshift({
      id: entry.id,
      name: entry.name,
      category: entry.category,
      timestamp: new Date().toISOString(),
    });
    // Trim
    if (viewed.value.length > MAX_ITEMS) {
      viewed.value = viewed.value.slice(0, MAX_ITEMS);
    }
    save();
  }

  return { recentlyViewed: viewed, addToRecent: add };
}
