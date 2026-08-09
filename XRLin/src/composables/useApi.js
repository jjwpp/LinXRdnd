import { ref } from "vue";
import { useAuth } from "./useAuth";

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080/api";

/** 获取当前 token 对应的 Authorization 头 */
function getAuthHeader() {
  const token = localStorage.getItem("auth_token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

// Category → backend endpoint mapping
const CATEGORIES = {
  class:        { id: "class",        name: "职业",     description: "战士、法师、游荡者等冒险职业",   endpoint: "/class" },
  race:         { id: "race",         name: "种族",     description: "人类、精灵、矮人等可玩种族",     endpoint: "/race" },
  spell:        { id: "spell",        name: "法术",     description: "从1环到9环的奥术与神术",        endpoint: "/spell" },
  monster:      { id: "monster",      name: "怪物",     description: "从地精到远古龙的怪物图鉴",      endpoint: "/monster" },
  "magic-item": { id: "magic-item",   name: "魔法物品", description: "武器、护甲、药水与奇物",        endpoint: "/magic-item" },
  feat:         { id: "feat",         name: "专长",     description: "强化角色的特殊能力",            endpoint: "/feat" },
};

export function useApi() {
  const loading = ref(false);
  const error = ref(null);
  const apiOnline = ref(false);

  async function checkHealth() {
    try {
      const res = await fetch(`${API_BASE}/health`);
      apiOnline.value = res.ok;
      return res.ok;
    } catch {
      apiOnline.value = false;
      return false;
    }
  }

  // Core fetch: unwraps R<T> { code, msg, data } → returns data
  async function fetchJson(path) {
    loading.value = true;
    error.value = null;
    try {
      const res = await fetch(`${API_BASE}${path}`, {
        headers: { ...getAuthHeader() },
      });
      if (!res.ok) {
        const errBody = await res.json().catch(() => ({}));
        throw new Error(errBody.msg || errBody.error || `请求失败: ${res.status}`);
      }
      const result = await res.json();
      // Unwrap R<T> response wrapper from backend
      if (result && typeof result.code !== "undefined") {
        if (result.code !== 200) {
          throw new Error(result.msg || "请求失败");
        }
        return result.data;
      }
      return result;
    } catch (e) {
      error.value = e.message;
      throw e;
    } finally {
      loading.value = false;
    }
  }

  // Convert raw entity (from any table) → unified CodexEntryDTO format
  function toEntry(raw, category) {
    if (!raw) return null;
    let tags = raw.tags || [];
    let details = raw.details || [];
    if (typeof tags === "string") {
      try { tags = JSON.parse(tags); } catch { tags = []; }
    }
    if (typeof details === "string") {
      try { details = JSON.parse(details); } catch { details = []; }
    }
    let classIds = raw.classIds || [];
    if (typeof classIds === "string") {
      try { classIds = JSON.parse(classIds); } catch { classIds = []; }
    }
    return {
      id: raw.id,
      name: raw.name,
      subtitle: raw.subtitle || "",
      summary: raw.summary || "",
      category,
      tags,
      details,
      level: raw.level,
      classIds,
      // 图片 URL（怪物立绘来自 MinIO，职业/角色可能有男女立绘）
      imageUrl: raw.imageUrl || null,
      maleImageUrl: raw.maleImageUrl || null,
      femaleImageUrl: raw.femaleImageUrl || null,
    };
  }

  // ── Aggregate API ──────────────────────────────────────────

  /** Fetch all category stats (id, name, description, count) */
  async function fetchCategories() {
    const results = await Promise.all(
      Object.values(CATEGORIES).map(async (cat) => {
        try {
          const count = await fetchJson(`${cat.endpoint}/count`);
          return { id: cat.id, name: cat.name, description: cat.description, count: count || 0 };
        } catch {
          return { id: cat.id, name: cat.name, description: cat.description, count: 0 };
        }
      })
    );
    return results;
  }

  /**
   * Fetch entries, optionally filtered by category and/or keyword.
   * @param {string} [category] - category id (e.g. "spell")
   * @param {string} [keyword]  - search keyword
   */
  async function fetchEntries(category, keyword) {
    const q = keyword?.trim();

    if (category && CATEGORIES[category]) {
      const cat = CATEGORIES[category];
      const params = q ? `?q=${encodeURIComponent(q)}` : "";
      const list = await fetchJson(`${cat.endpoint}/list${params}`);
      return (list || []).map((e) => toEntry(e, category));
    }

    // No category → aggregate from all
    if (q) {
      // With keyword: query each category endpoint
      const results = await Promise.all(
        Object.entries(CATEGORIES).map(async ([catKey, cat]) => {
          try {
            const list = await fetchJson(`${cat.endpoint}/list?q=${encodeURIComponent(q)}`);
            return (list || []).map((e) => toEntry(e, catKey));
          } catch {
            return [];
          }
        })
      );
      return results.flat();
    }

    // No keyword, no category → fetch all from all endpoints
    const results = await Promise.all(
      Object.entries(CATEGORIES).map(async ([catKey, cat]) => {
        try {
          const list = await fetchJson(`${cat.endpoint}/list`);
          return (list || []).map((e) => toEntry(e, catKey));
        } catch {
          return [];
        }
      })
    );
    return results.flat();
  }

  /**
   * Fetch a single entry by category + id.
   * If category is unknown, tries all categories.
   */
  async function fetchEntry(category, id) {
    if (category && CATEGORIES[category]) {
      const raw = await fetchJson(`${CATEGORIES[category].endpoint}/${id}`);
      return toEntry(raw, category);
    }
    // Fallback: try each category until found
    for (const [catKey, cat] of Object.entries(CATEGORIES)) {
      try {
        const raw = await fetchJson(`${cat.endpoint}/${id}`);
        if (raw) return toEntry(raw, catKey);
      } catch { /* continue */ }
    }
    throw new Error(`条目未找到: ${id}`);
  }

  /** Fetch a single random entry, optionally within a category */
  async function fetchRandom(category) {
    if (category && CATEGORIES[category]) {
      const raw = await fetchJson(`${CATEGORIES[category].endpoint}/random`);
      return toEntry(raw, category);
    }
    // Pick a random category, try until one succeeds
    const shuffled = Object.keys(CATEGORIES).sort(() => Math.random() - 0.5);
    for (const catKey of shuffled) {
      try {
        const raw = await fetchJson(`${CATEGORIES[catKey].endpoint}/random`);
        if (raw) return toEntry(raw, catKey);
      } catch { /* try next */ }
    }
    throw new Error("没有可用的条目");
  }

  /**
   * Fetch multiple random entries.
   * @param {string} [category] - limit to one category
   * @param {number} [count=3]  - number of entries
   */
  async function fetchRandomSome(category, count = 3) {
    if (category && CATEGORIES[category]) {
      const list = await fetchJson(
        `${CATEGORIES[category].endpoint}/random/some?count=${count}`
      );
      return (list || []).map((e) => toEntry(e, category));
    }
    // Distribute across categories
    const catKeys = Object.keys(CATEGORIES);
    const perCat = Math.max(1, Math.ceil(count / catKeys.length));
    const results = await Promise.all(
      catKeys.map(async (catKey) => {
        try {
          const list = await fetchJson(
            `${CATEGORIES[catKey].endpoint}/random/some?count=${perCat}`
          );
          return (list || []).map((e) => toEntry(e, catKey));
        } catch {
          return [];
        }
      })
    );
    return results
      .flat()
      .sort(() => Math.random() - 0.5)
      .slice(0, count);
  }

  // ── Character API ──────────────────────────────────────────

  async function postJson(path, body) {
    loading.value = true;
    error.value = null;
    try {
      const res = await fetch(`${API_BASE}${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...getAuthHeader() },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const errBody = await res.json().catch(() => ({}));
        throw new Error(errBody.msg || errBody.error || `请求失败: ${res.status}`);
      }
      const result = await res.json();
      if (result && typeof result.code !== "undefined") {
        if (result.code !== 200) throw new Error(result.msg || "请求失败");
        return result.data;
      }
      return result;
    } catch (e) {
      error.value = e.message;
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function delJson(path) {
    loading.value = true;
    error.value = null;
    try {
      const res = await fetch(`${API_BASE}${path}`, {
        method: "DELETE",
        headers: { ...getAuthHeader() },
      });
      if (!res.ok) {
        const errBody = await res.json().catch(() => ({}));
        throw new Error(errBody.msg || errBody.error || `请求失败: ${res.status}`);
      }
      const result = await res.json();
      if (result && typeof result.code !== "undefined") {
        if (result.code !== 200) throw new Error(result.msg || "请求失败");
        return result.data;
      }
      return result;
    } catch (e) {
      error.value = e.message;
      throw e;
    } finally {
      loading.value = false;
    }
  }

  /** Fetch all saved characters */
  async function fetchCharacters() {
    return await fetchJson("/player-character/list");
  }

  /** Fetch a single character by ID */
  async function fetchCharacter(id) {
    return await fetchJson(`/player-character/${id}`);
  }

  /** Create a new character */
  async function createCharacter(data) {
    return await postJson("/player-character", data);
  }

  /** Delete a character */
  async function deleteCharacter(id) {
    return await delJson(`/player-character/${id}`);
  }

  return {
    loading,
    error,
    apiOnline,
    checkHealth,
    fetchJson,
    fetchCategories,
    fetchEntries,
    fetchEntry,
    fetchRandom,
    fetchRandomSome,
    fetchCharacters,
    fetchCharacter,
    createCharacter,
    deleteCharacter,
    CATEGORIES,
  };
}
