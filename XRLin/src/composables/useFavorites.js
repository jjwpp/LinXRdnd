import { ref, watch } from "vue";

const STORAGE_KEY = "xrlin-favorites";

const favorites = ref(load());

function load() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) || [];
  } catch {
    return [];
  }
}

function save() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(favorites.value));
}

export function useFavorites() {
  function toggle(id) {
    const idx = favorites.value.indexOf(id);
    if (idx >= 0) {
      favorites.value.splice(idx, 1);
    } else {
      favorites.value.push(id);
    }
    save();
  }

  function isFavorite(id) {
    return favorites.value.includes(id);
  }

  function remove(id) {
    favorites.value = favorites.value.filter((fid) => fid !== id);
    save();
  }

  return { favorites, toggle, isFavorite, remove };
}
