<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { useApi } from "../composables/useApi";
import { useAuth } from "../composables/useAuth";
import CharacterCard from "../components/CharacterCard.vue";
import LoadingSkeleton from "../components/LoadingSkeleton.vue";

const router = useRouter();
const { loading, error, checkHealth, fetchCharacters, fetchEntry, deleteCharacter } = useApi();
const { isLoggedIn } = useAuth();

const characters = ref([]);
const selectedChar = ref(null);
const resolvedRace = ref(null);
const resolvedClass = ref(null);

// Cache for resolved entity names
const nameCache = ref({});

const categoryLabels = {
  class: "职业", race: "种族", spell: "法术",
  monster: "怪物", "magic-item": "魔法物品", feat: "专长",
};

async function getEntityName(category, id) {
  if (!id) return "?";
  const key = `${category}:${id}`;
  if (nameCache.value[key]) return nameCache.value[key];
  try {
    const entry = await fetchEntry(category, id);
    nameCache.value[key] = entry.name;
    return entry.name;
  } catch {
    return id;
  }
}

function getRaceName(char) {
  return nameCache.value[`race:${char.raceId}`] || char.raceId || "?";
}

function getClassName(char) {
  return nameCache.value[`class:${char.classId}`] || char.classId || "?";
}

onMounted(async () => {
  if (!isLoggedIn.value) {
    router.replace("/login");
    return;
  }
  await checkHealth();
  await loadCharacters();
});

async function loadCharacters() {
  try {
    characters.value = await fetchCharacters();
    // Pre-resolve all race/class names
    const names = {};
    for (const ch of characters.value) {
      if (ch.raceId && !names[`race:${ch.raceId}`]) {
        try { names[`race:${ch.raceId}`] = await getEntityName("race", ch.raceId); } catch {}
      }
      if (ch.classId && !names[`class:${ch.classId}`]) {
        try { names[`class:${ch.classId}`] = await getEntityName("class", ch.classId); } catch {}
      }
    }
    nameCache.value = { ...nameCache.value, ...names };
  } catch {
    // handled by error ref
  }
}

async function selectCharacter(ch) {
  selectedChar.value = ch;
  if (ch.raceId) resolvedRace.value = await getEntityName("race", ch.raceId);
  if (ch.classId) resolvedClass.value = await getEntityName("class", ch.classId);
}

async function handleDelete(ch) {
  if (!confirm(`确定删除角色 "${ch.name}" 吗？`)) return;
  try {
    await deleteCharacter(ch.id);
    characters.value = characters.value.filter((c) => c.id !== ch.id);
    if (selectedChar.value?.id === ch.id) {
      selectedChar.value = null;
    }
  } catch (e) {
    alert("删除失败: " + e.message);
  }
}

// Parse JSON strings from backend
function parseJson(val) {
  if (!val) return [];
  if (Array.isArray(val)) return val;
  if (typeof val === "string") {
    try { return JSON.parse(val); } catch { return []; }
  }
  return [];
}
</script>

<template>
  <div class="characters-page archives">
    <!-- Decorative rune circle backdrop -->
    <div class="archives-rune-circle" aria-hidden="true"></div>

    <!-- Hero Header -->
    <header class="archives-hero">
      <div class="archives-hero-ornament" aria-hidden="true">
        <span class="orn-left">❖</span>
        <span class="orn-bar"></span>
        <span class="orn-mid">⚜</span>
        <span class="orn-bar"></span>
        <span class="orn-right">❖</span>
      </div>
      <p class="archives-eyebrow">ADVENTURER ARCHIVES</p>
      <h1 class="archives-title text-glow-gold">冒险者档案</h1>
      <p class="archives-sub">在此查阅你麾下英雄的卷宗 · CHRONICLES OF YOUR COMPANY</p>
      <button class="rune-btn archives-create pulse-glow" @click="router.push('/characters/new')">
        <span class="rune-glyph">✦</span> 缔造新英雄
      </button>
      <div class="archives-hero-ornament bottom" aria-hidden="true">
        <span class="orn-left">❖</span>
        <span class="orn-bar"></span>
        <span class="orn-mid">✦</span>
        <span class="orn-bar"></span>
        <span class="orn-right">❖</span>
      </div>
    </header>

    <!-- Error -->
    <div v-if="error && !characters.length" class="empty-state archives-empty surface-stone border-ornate">
      <span class="empty-icon">⚠</span>
      <p>{{ error }}</p>
      <button class="retry-btn" @click="loadCharacters">重新卜筮</button>
    </div>

    <!-- Loading -->
    <LoadingSkeleton v-else-if="loading && !characters.length" :count="3" />

    <!-- Empty State -->
    <div v-else-if="!characters.length" class="empty-state archives-empty surface-parchment border-ornate">
      <span class="empty-icon">🜲</span>
      <p class="archives-empty-title">卷宗空寂</p>
      <p class="archives-empty-desc">尚未有任何英雄的传说被铭刻于此</p>
      <p style="font-size:13px;color:var(--muted)">缔造你的第一位 D&D 冒险者，开启传奇</p>
      <button class="rune-btn archives-create" style="margin-top:18px" @click="router.push('/characters/new')">
        <span class="rune-glyph">✦</span> 缔造首位英雄
      </button>
    </div>

    <!-- Character Grid + Detail -->
    <div v-else class="characters-layout archives-layout">
      <div class="characters-grid archives-grid">
        <CharacterCard
          v-for="ch in characters"
          :key="ch.id"
          :character="ch"
          :race-name="getRaceName(ch)"
          :class-name="getClassName(ch)"
          @click="selectCharacter"
          @delete="handleDelete"
        />
      </div>

      <!-- Detail Panel — open character sheet -->
      <aside class="character-detail archives-sheet surface-parchment border-ornate corner-flourish" v-if="selectedChar">
        <div class="sheet-seal" aria-hidden="true">⚜</div>
        <p class="sheet-eyebrow">CHARACTER SHEET</p>
        <h2 class="sheet-name text-glow-gold">{{ selectedChar.name }}</h2>
        <p class="detail-meta">
          {{ getRaceName(selectedChar) }} · {{ getClassName(selectedChar) }}
        </p>
        <p class="sheet-level">
          <span class="lvl-badge">Lv.{{ selectedChar.level }}</span>
        </p>
        <p class="detail-player">玩家 · {{ selectedChar.playerName }}</p>

        <div class="sheet-divider" aria-hidden="true">
          <span>✦</span>
        </div>

        <p v-if="selectedChar.summary" class="detail-summary">{{ selectedChar.summary }}</p>

        <div v-if="parseJson(selectedChar.featIds).length" class="detail-section sheet-section">
          <h4 class="sheet-section-title">📜 专长 · FEATS ({{ parseJson(selectedChar.featIds).length }})</h4>
          <div class="tag-list">
            <span v-for="id in parseJson(selectedChar.featIds)" :key="id" class="tag tag-feat">
              {{ id }}
            </span>
          </div>
        </div>

        <div v-if="parseJson(selectedChar.spellIds).length" class="detail-section sheet-section">
          <h4 class="sheet-section-title">✨ 法术 · SPELLS ({{ parseJson(selectedChar.spellIds).length }})</h4>
          <div class="tag-list">
            <span v-for="id in parseJson(selectedChar.spellIds)" :key="id" class="tag tag-spell">
              {{ id }}
            </span>
          </div>
        </div>

        <button class="btn-adventure sheet-adventure" @click="router.push(`/adventure?characterId=${selectedChar.id}`)">
          ⚔ 踏上征途
        </button>
      </aside>

      <!-- Empty detail hint -->
      <aside class="character-detail archives-sheet-hint surface-stone" v-else>
        <span class="hint-glyph">🜲</span>
        <p class="hint-text">选择左侧任一卷宗</p>
        <p class="hint-sub">展开英雄的角色卡</p>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.archives {
  position: relative;
  padding: 56px 24px 80px;
}

/* Decorative rune circle */
.archives-rune-circle {
  position: absolute;
  top: 40px;
  left: 50%;
  transform: translateX(-50%);
  width: 520px;
  height: 520px;
  border: 1px solid var(--line-gold);
  border-radius: 50%;
  opacity: .07;
  pointer-events: none;
  animation: rotateRunes 90s linear infinite;
}
.archives-rune-circle::before {
  content: "";
  position: absolute;
  inset: 50px;
  border: 1px dashed var(--line-gold);
  border-radius: 50%;
}
.archives-rune-circle::after {
  content: "";
  position: absolute;
  inset: 110px;
  border: 1px solid var(--line-gold);
  border-radius: 50%;
}
@keyframes rotateRunes {
  to { transform: translateX(-50%) rotate(360deg); }
}

/* Hero */
.archives-hero {
  text-align: center;
  margin-bottom: 44px;
  position: relative;
  z-index: 2;
  animation: fadeScaleIn .6s ease-out;
}
.archives-eyebrow {
  font-family: var(--font-heading);
  font-size: 12px;
  color: var(--gold-dim);
  letter-spacing: .45em;
  text-transform: uppercase;
  margin: 0 0 8px;
}
.archives-title {
  font-family: var(--font-display);
  font-size: 46px;
  font-weight: 900;
  margin: 0 0 10px;
  letter-spacing: .08em;
  background: linear-gradient(180deg, var(--gold-bright) 0%, var(--gold) 50%, var(--gold-dim) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 30px rgba(201, 162, 39, .15));
}
.archives-sub {
  font-family: var(--font-body);
  font-size: 15px;
  font-style: italic;
  color: var(--ink-soft);
  margin: 0 0 26px;
  letter-spacing: .04em;
}

.archives-create {
  font-size: 15px;
  padding: 14px 38px;
}
.archives-create .rune-glyph {
  margin-right: 8px;
  color: var(--gold-bright);
  text-shadow: 0 0 10px var(--gold-glow);
}

/* Hero ornaments */
.archives-hero-ornament {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin: 18px auto 0;
  max-width: 420px;
  color: var(--gold-dim);
}
.archives-hero-ornament.bottom { margin: 22px auto 0; }
.orn-mid { font-size: 18px; color: var(--gold); filter: drop-shadow(0 0 6px var(--gold-glow)); }
.orn-left, .orn-right { font-size: 12px; color: var(--gold-dim); }
.orn-bar {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-gold), transparent);
}

/* Empty states */
.archives-empty {
  max-width: 520px;
  margin: 0 auto;
  padding: 48px 28px;
  border-radius: var(--radius-lg);
  text-align: center;
}
.archives-empty .empty-icon {
  font-size: 52px;
  filter: drop-shadow(0 0 14px var(--gold-glow));
}
.archives-empty-title {
  font-family: var(--font-display);
  font-size: 24px;
  color: var(--ink-bright);
  margin: 8px 0 4px;
  letter-spacing: .05em;
}
.archives-empty-desc {
  color: var(--gold);
  font-style: italic;
  margin: 0 0 6px;
}

/* Layout grid */
.archives-layout {
  max-width: 1080px;
  margin: 0 auto;
}
.archives-grid {
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
}

/* Character sheet detail */
.archives-sheet {
  position: sticky;
  top: 90px;
  padding: 28px 24px 24px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-deep), 0 0 30px var(--gold-glow);
  animation: fadeScaleIn .35s ease-out;
}
.sheet-seal {
  position: absolute;
  top: -14px;
  left: 50%;
  transform: translateX(-50%);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--gold);
  color: var(--gold-bright);
  font-size: 18px;
  box-shadow: 0 0 16px var(--gold-glow);
}
.sheet-eyebrow {
  font-family: var(--font-heading);
  font-size: 11px;
  color: var(--gold-dim);
  letter-spacing: .3em;
  text-transform: uppercase;
  text-align: center;
  margin: 0 0 6px;
}
.sheet-name {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 900;
  color: var(--ink-bright);
  text-align: center;
  margin: 0 0 6px;
  letter-spacing: .04em;
}
.sheet-name.text-glow-gold {
  background: linear-gradient(180deg, var(--gold-bright), var(--gold));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.archives-sheet .detail-meta {
  text-align: center;
  color: var(--gold);
  font-family: var(--font-heading);
  font-size: 13px;
  letter-spacing: .08em;
  margin: 0 0 8px;
}
.sheet-level { text-align: center; margin: 0 0 6px; }
.lvl-badge {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 800;
  color: var(--bg-void);
  background: linear-gradient(180deg, var(--gold-bright), var(--gold));
  padding: 3px 16px;
  border-radius: 2px;
  letter-spacing: .08em;
  box-shadow: 0 2px 8px var(--gold-glow);
}
.archives-sheet .detail-player {
  text-align: center;
  color: var(--muted);
  font-size: 12px;
  font-style: italic;
  margin: 0 0 6px;
}

.sheet-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
}
.sheet-divider::before,
.sheet-divider::after {
  content: "";
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-gold), transparent);
}
.sheet-divider span { color: var(--gold); font-size: 12px; }

.archives-sheet .detail-summary {
  color: var(--ink);
  font-size: 14px;
  line-height: 1.8;
  font-style: italic;
  margin: 0 0 14px;
}
.sheet-section { margin-top: 12px; }
.sheet-section-title {
  font-family: var(--font-heading);
  font-size: 12px;
  font-weight: 700;
  color: var(--gold);
  letter-spacing: .1em;
  margin-bottom: 8px;
}
.sheet-adventure {
  margin-top: 18px;
  font-size: 14px;
}

/* Empty hint sheet */
.archives-sheet-hint {
  position: sticky;
  top: 90px;
  padding: 56px 24px;
  border-radius: var(--radius-lg);
  text-align: center;
  border: 1px dashed var(--line);
}
.hint-glyph {
  font-size: 44px;
  color: var(--gold-dim);
  filter: drop-shadow(0 0 10px var(--gold-glow));
  opacity: .6;
}
.hint-text {
  font-family: var(--font-heading);
  font-size: 15px;
  color: var(--ink-soft);
  letter-spacing: .1em;
  margin: 12px 0 4px;
}
.hint-sub {
  font-size: 12px;
  color: var(--muted);
  font-style: italic;
}

@media (max-width: 920px) {
  .archives-title { font-size: 36px; }
  .archives-rune-circle { width: 360px; height: 360px; }
}
@media (max-width: 560px) {
  .archives-title { font-size: 30px; }
  .archives { padding: 36px 16px 60px; }
  .archives-create { width: 100%; }
}
</style>
