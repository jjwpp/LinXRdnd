<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useApi } from "../composables/useApi";
import { useAuth } from "../composables/useAuth";
import LoadingSkeleton from "../components/LoadingSkeleton.vue";

// 职业立绘图片
import { getClassImage } from "../composables/useClassImages";

const router = useRouter();
const { loading, fetchEntries, createCharacter, CATEGORIES } = useApi();
const { isLoggedIn, nickname, user } = useAuth();

// 未登录则跳转登录页
onMounted(() => {
  if (!isLoggedIn.value) {
    router.replace("/login");
  }
});

const currentStep = ref(0);
const saving = ref(false);
const saveError = ref("");

// Data from API
const races = ref([]);
const classes = ref([]);
const feats = ref([]);
const spells = ref([]);

// Spellcaster class IDs (classes with spellcasting)
const SPELLCASTER_IDS = [
  "bard", "cleric", "druid", "sorcerer",
  "warlock", "wizard", "paladin", "ranger"
];

// Builder state - level always 1
const character = reactive({
  name: "",
  level: 1,
  race: null,
  class: null,
  gender: "male", // 'male' | 'female'
  feats: [],
  spells: [],
  // DND 5e 六维属性（标准阵列）
  abilities: {
    strength: 15,
    dexterity: 14,
    constitution: 13,
    intelligence: 12,
    wisdom: 10,
    charisma: 8,
  },
});

// 标准阵列
const STANDARD_ARRAY = [15, 14, 13, 12, 10, 8];
const ABILITY_KEYS = ["strength", "dexterity", "constitution", "intelligence", "wisdom", "charisma"];
const ABILITY_LABELS = {
  strength: "力量 (STR)",
  dexterity: "敏捷 (DEX)",
  constitution: "体质 (CON)",
  intelligence: "智力 (INT)",
  wisdom: "感知 (WIS)",
  charisma: "魅力 (CHA)",
};
const ABILITY_ICONS = {
  strength: "💪",
  dexterity: "🏃",
  constitution: "❤️",
  intelligence: "🧠",
  wisdom: "👁️",
  charisma: "🎤",
};

// 职业推荐属性排序
const CLASS_ABILITY_PRIORITY = {
  fighter: ["strength", "constitution", "dexterity"],
  barbarian: ["strength", "constitution", "dexterity"],
  paladin: ["strength", "charisma", "constitution"],
  ranger: ["dexterity", "constitution", "wisdom"],
  rogue: ["dexterity", "constitution", "intelligence"],
  monk: ["dexterity", "wisdom", "constitution"],
  wizard: ["intelligence", "constitution", "dexterity"],
  sorcerer: ["charisma", "constitution", "dexterity"],
  warlock: ["charisma", "constitution", "dexterity"],
  bard: ["charisma", "dexterity", "constitution"],
  cleric: ["wisdom", "constitution", "strength"],
  druid: ["wisdom", "constitution", "dexterity"],
};

// 当前已分配的值
const assignedValues = computed(() => Object.values(character.abilities));

// 检查某个值是否还可以分配（避免重复使用标准阵列中的值超过其出现次数）
function canAssignValue(value, currentKey) {
  // 标准阵列中每个值只出现一次
  const usedByOthers = ABILITY_KEYS.filter(k => k !== currentKey && character.abilities[k] === value).length;
  return usedByOthers === 0;
}

// 计算属性调整值
function getMod(score) {
  return Math.floor((score - 10) / 2);
}

function formatMod(score) {
  const mod = getMod(score);
  return mod >= 0 ? `+${mod}` : `${mod}`;
}

// 自动按职业推荐分配
function autoAssignByClass() {
  if (!character.class) return;
  const priority = CLASS_ABILITY_PRIORITY[character.class.id] || ABILITY_KEYS;
  const sortedArray = [...STANDARD_ARRAY].sort((a, b) => b - a); // [15, 14, 13, 12, 10, 8]
  // 按优先级分配最高值
  const newAbilities = {};
  let arrIdx = 0;
  for (const key of priority) {
    newAbilities[key] = sortedArray[arrIdx++];
  }
  // 剩余属性分配剩余值
  for (const key of ABILITY_KEYS) {
    if (!newAbilities[key]) {
      newAbilities[key] = sortedArray[arrIdx++];
    }
  }
  character.abilities = newAbilities;
}

const isSpellcaster = computed(() =>
  character.class && SPELLCASTER_IDS.includes(character.class.id)
);

// Steps definition
const steps = computed(() => {
  const base = [
    { title: "基本信息", icon: "📝" },
    { title: "选择种族", icon: "🧝" },
    { title: "选择职业", icon: "⚔️" },
    { title: "属性分配", icon: "🎲" },
    { title: "选择专长", icon: "📜" },
  ];
  if (isSpellcaster.value) {
    base.push({ title: "选择法术", icon: "✨" });
  }
  base.push({ title: "确认创建", icon: "✅" });
  return base;
});

const featStepIndex = computed(() => 4);
const spellStepIndex = computed(() => 5);
const reviewStepIndex = computed(() => isSpellcaster.value ? 6 : 5);

const canGoNext = computed(() => {
  switch (currentStep.value) {
    case 0: return character.name.trim();
    case 1: return character.race !== null;
    case 2: return character.class !== null;
    case 3: return true; // abilities always have defaults
    case 4: return true; // feats optional
    case 5: return true; // spells optional
    default: return true;
  }
});

onMounted(async () => {
  // Pre-fetch all data in parallel
  const [raceData, classData, featData, spellData] = await Promise.all([
    fetchEntries("race").catch(() => []),
    fetchEntries("class").catch(() => []),
    fetchEntries("feat").catch(() => []),
    fetchEntries("spell").catch(() => []),
  ]);
  races.value = raceData;
  classes.value = classData;
  feats.value = featData;
  spells.value = spellData;
});

function next() {
  if (!canGoNext.value) return;
  if (currentStep.value < steps.value.length - 1) {
    currentStep.value++;
  }
}

function prev() {
  if (currentStep.value > 0) currentStep.value--;
}

function goToStep(idx) {
  if (idx <= reviewStepIndex.value) currentStep.value = idx;
}

function toggleFeat(feat) {
  const idx = character.feats.findIndex((f) => f.id === feat.id);
  if (idx >= 0) {
    character.feats.splice(idx, 1);
  } else if (character.feats.length < 3) {
    character.feats.push(feat);
  }
}

function toggleSpell(spell) {
  const idx = character.spells.findIndex((s) => s.id === spell.id);
  if (idx >= 0) {
    character.spells.splice(idx, 1);
  } else {
    character.spells.push(spell);
  }
}

function isFeatSelected(feat) {
  return character.feats.some((f) => f.id === feat.id);
}

function isSpellSelected(spell) {
  return character.spells.some((s) => s.id === spell.id);
}

// When class changes, clear selected spells that don't match
function selectClass(cls) {
  character.class = cls;
  // Clear spells if class changed
  if (character.spells.length > 0) {
    character.spells = character.spells.filter((s) =>
      s.classIds && s.classIds.includes(cls.id)
    );
  }
}

// 获取当前性别下的职业图片
function classImg(classId) {
  return getClassImage(classId, character.gender);
}

async function saveCharacter() {
  saving.value = true;
  saveError.value = "";
  try {
    const payload = {
      name: character.name,
      level: 1, // Always level 1
      raceId: character.race?.id,
      classId: character.class?.id,
      gender: character.gender,
      featIds: character.feats.map((f) => f.id),
      spellIds: character.spells.map((s) => s.id),
      strength: character.abilities.strength,
      dexterity: character.abilities.dexterity,
      constitution: character.abilities.constitution,
      intelligence: character.abilities.intelligence,
      wisdom: character.abilities.wisdom,
      charisma: character.abilities.charisma,
      subtitle: `${character.race?.name} ${character.class?.name}`,
      summary: `${nickname.value} 的 1 级 ${character.race?.name} ${character.class?.name}`,
    };
    await createCharacter(payload);
    router.push("/characters");
  } catch (e) {
    saveError.value = e.message || "保存失败";
  } finally {
    saving.value = false;
  }
}

// Filtered spells: match selected class + level 0 or 1 only
const availableSpells = computed(() => {
  if (!character.class) return [];
  const classId = character.class.id;
  return spells.value.filter((s) => {
    // Must match the selected class
    const matchesClass = s.classIds && s.classIds.includes(classId);
    if (!matchesClass) return false;
    // Only level 0 (cantrip) and level 1 for initial selection
    const sl = s.level != null ? s.level : 1;
    return sl === 0 || sl === 1;
  });
});

// Spells grouped by level
const spellsByLevel = computed(() => {
  const groups = {};
  for (const s of availableSpells.value) {
    const key = s.level === 0 ? "戏法" : `第${s.level}环`;
    if (!groups[key]) groups[key] = [];
    groups[key].push(s);
  }
  return groups;
});

const spellLevels = computed(() => {
  const keys = Object.keys(spellsByLevel.value);
  // Sort: 戏法 first, then by number
  return keys.sort((a, b) => {
    if (a === "戏法") return -1;
    if (b === "戏法") return 1;
    const na = parseInt(a.match(/\d+/)?.[0] || "99");
    const nb = parseInt(b.match(/\d+/)?.[0] || "99");
    return na - nb;
  });
});

// Category labels for display
const categoryLabels = {
  class: "职业", race: "种族", spell: "法术",
  monster: "怪物", "magic-item": "魔法物品", feat: "专长",
};
</script>

<template>
  <div class="builder-page ceremony">
    <!-- Ceremony Header -->
    <header class="ceremony-header">
      <div class="ceremony-ornament" aria-hidden="true">
        <span class="co-line"></span>
        <span class="co-glyph">⚜</span>
        <span class="co-line"></span>
      </div>
      <p class="ceremony-eyebrow">HERO'S CREATION · SACRED RITE</p>
      <h1 class="ceremony-title text-glow-gold">英雄诞生</h1>
      <p class="ceremony-sub">于圣火与符文之间，铸就你的传奇</p>
    </header>

    <!-- Quest Progression Path -->
    <div class="builder-steps quest-path">
      <template v-for="(step, idx) in steps" :key="idx">
        <button
          class="step-dot quest-node"
          :class="{ active: idx === currentStep, done: idx < currentStep, clickable: idx <= reviewStepIndex }"
          :disabled="idx > reviewStepIndex"
          @click="goToStep(idx)"
        >
          <span class="quest-rune" aria-hidden="true">
            <span class="quest-rune-inner">{{ step.icon }}</span>
          </span>
          <span class="step-label">{{ step.title }}</span>
        </button>
        <span
          v-if="idx < steps.length - 1"
          class="quest-link"
          :class="{ filled: idx < currentStep }"
          aria-hidden="true"
        ></span>
      </template>
    </div>

    <!-- Step Content -->
    <div class="builder-body ceremony-body">
      <LoadingSkeleton v-if="loading && !races.length" :count="6" />

      <!-- Step 0: Basic Info -->
      <div v-show="currentStep === 0" class="step-panel rite-panel surface-parchment border-ornate">
        <div class="rite-seal" aria-hidden="true">✦</div>
        <p class="rite-eyebrow">RITE I · IDENTITY</p>
        <h2 class="step-title">铭刻真名</h2>
        <p class="step-desc">为你的冒险者取一个名字，开启传奇之旅</p>
        <div class="form-group">
          <label>角色名 · TRUE NAME</label>
          <input v-model="character.name" placeholder="例如: 阿拉里克" class="input-field" maxlength="50" autofocus />
        </div>
        <div class="form-group">
          <label>玩家名 · SOUL BEARER</label>
          <div class="player-name-display">
            <span class="player-name-badge">{{ nickname || '未登录' }}</span>
            <span class="player-name-hint">自动绑定当前登录勇者</span>
          </div>
        </div>
        <div class="form-group">
          <label>等级 · TIER</label>
          <div class="level-locked">
            <span class="level-badge">Lv.1</span>
            <span class="level-hint">新晋英雄自 1 级起步，于冒险中精进</span>
          </div>
        </div>
      </div>

      <!-- Step 1: Select Race -->
      <div v-show="currentStep === 1" class="step-panel rite-panel surface-parchment border-ornate">
        <div class="rite-seal" aria-hidden="true">🜲</div>
        <p class="rite-eyebrow">RITE II · BLOODLINE</p>
        <h2 class="step-title">抉择血脉</h2>
        <p class="step-desc">种族决定角色的基础属性与文化传承</p>
        <div class="select-grid codex-grid">
          <button
            v-for="race in races"
            :key="race.id"
            class="select-card codex-entry"
            :class="{ selected: character.race?.id === race.id }"
            @click="character.race = race"
          >
            <span class="codex-rune" aria-hidden="true">❖</span>
            <h3>{{ race.name }}</h3>
            <p class="card-sub">{{ race.subtitle }}</p>
            <p class="card-summary">{{ race.summary }}</p>
          </button>
        </div>
      </div>

      <!-- Step 2: Select Class -->
      <div v-show="currentStep === 2" class="step-panel rite-panel surface-parchment border-ornate">
        <div class="rite-seal" aria-hidden="true">⚔</div>
        <p class="rite-eyebrow">RITE III · VOCATION</p>
        <h2 class="step-title">选定道途</h2>
        <p class="step-desc">职业决定角色的战斗风格与核心能力</p>
        <div class="gender-toggle">
          <button
            class="gender-btn"
            :class="{ active: character.gender === 'male' }"
            @click="character.gender = 'male'"
          >♂ 男性</button>
          <button
            class="gender-btn"
            :class="{ active: character.gender === 'female' }"
            @click="character.gender = 'female'"
          >♀ 女性</button>
        </div>
        <div class="select-grid codex-grid">
          <button
            v-for="cls in classes"
            :key="cls.id"
            class="select-card class-select-card codex-entry"
            :class="{ selected: character.class?.id === cls.id }"
            @click="selectClass(cls)"
          >
            <div class="class-portrait" v-if="classImg(cls.id)">
              <img :src="classImg(cls.id)" :alt="cls.name" loading="lazy" />
              <div class="class-portrait-overlay"></div>
            </div>
            <h3>{{ cls.name }}</h3>
            <p class="card-sub">{{ cls.subtitle }}</p>
            <p class="card-summary">{{ cls.summary }}</p>
            <div class="card-tags">
              <span v-for="tag in cls.tags" :key="tag" class="tag tag-class">{{ tag }}</span>
            </div>
          </button>
        </div>
      </div>

      <!-- Step 3: Ability Scores -->
      <div v-show="currentStep === 3" class="step-panel rite-panel surface-parchment border-ornate">
        <div class="rite-seal" aria-hidden="true">⚄</div>
        <p class="rite-eyebrow">RITE IV · ATTRIBUTES</p>
        <h2 class="step-title">掷定命石</h2>
        <p class="step-desc">
          以标准阵列 [15, 14, 13, 12, 10, 8] 分配六维命石
          <button v-if="character.class" class="auto-assign-btn" @click="autoAssignByClass">
            按 {{ character.class.name }} 推荐
          </button>
        </p>
        <div class="ability-alloc-grid rune-stones-grid">
          <div v-for="key in ABILITY_KEYS" :key="key" class="ability-alloc-card rune-stone">
            <div class="ability-header">
              <span class="ability-icon">{{ ABILITY_ICONS[key] }}</span>
              <span class="ability-name">{{ ABILITY_LABELS[key] }}</span>
            </div>
            <div class="ability-value">
              <select v-model="character.abilities[key]" class="ability-select">
                <option
                  v-for="val in STANDARD_ARRAY"
                  :key="val"
                  :value="val"
                  :disabled="!canAssignValue(val, key) && character.abilities[key] !== val"
                >
                  {{ val }} ({{ formatMod(val) }})
                </option>
              </select>
              <span class="ability-mod">{{ formatMod(character.abilities[key]) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 4: Select Feats -->
      <div v-show="currentStep === featStepIndex" class="step-panel rite-panel surface-parchment border-ornate">
        <div class="rite-seal" aria-hidden="true">📜</div>
        <p class="rite-eyebrow">RITE V · FEATS</p>
        <h2 class="step-title">铭刻专长</h2>
        <p class="step-desc">
          可选最多 3 项专长以强化角色
          <span class="selected-badge" v-if="character.feats.length">{{ character.feats.length }}/3</span>
        </p>
        <div class="select-grid codex-grid">
          <button
            v-for="feat in feats"
            :key="feat.id"
            class="select-card multi codex-entry"
            :class="{ selected: isFeatSelected(feat) }"
            @click="toggleFeat(feat)"
          >
            <span class="codex-rune" aria-hidden="true">❖</span>
            <h3>{{ feat.name }}</h3>
            <p class="card-sub">{{ feat.subtitle }}</p>
            <p class="card-summary">{{ feat.summary }}</p>
          </button>
        </div>
      </div>

      <!-- Step 5: Select Spells (spellcaster only) -->
      <div v-show="currentStep === spellStepIndex && isSpellcaster" class="step-panel rite-panel surface-parchment border-ornate">
        <div class="rite-seal" aria-hidden="true">✦</div>
        <p class="rite-eyebrow">RITE VI · ARCANA</p>
        <h2 class="step-title">抄录法术</h2>
        <p class="step-desc">
          为你的 {{ character.class?.name }} 挑选法术（仅可学 1 环及戏法）
          <span class="selected-badge" v-if="character.spells.length">{{ character.spells.length }} 个</span>
        </p>
        <div v-if="availableSpells.length === 0" class="empty-spells">
          该道途暂无可抄录之法术
        </div>
        <div v-for="lvl in spellLevels" :key="lvl" class="spell-level-group">
          <h3 class="spell-lvl-title">{{ lvl }}</h3>
          <div class="select-grid codex-grid">
            <button
              v-for="spell in spellsByLevel[lvl]"
              :key="spell.id"
              class="select-card multi spell-card codex-entry"
              :class="{ selected: isSpellSelected(spell) }"
              @click="toggleSpell(spell)"
            >
              <span class="codex-rune arcane" aria-hidden="true">✦</span>
              <h4>{{ spell.name }}</h4>
              <p class="card-sub">{{ spell.subtitle }}</p>
              <p class="card-summary">{{ spell.summary }}</p>
            </button>
          </div>
        </div>
      </div>

      <!-- Step Review & Save -->
      <div v-show="currentStep === reviewStepIndex" class="step-panel rite-panel">
        <div class="ceremony-final-ornament" aria-hidden="true">
          <span class="co-line"></span>
          <span class="co-glyph">⚜</span>
          <span class="co-line"></span>
        </div>
        <p class="rite-eyebrow center">FINAL RITE · CONFIRMATION</p>
        <h2 class="step-title center">英雄已成</h2>
        <p class="step-desc center">审视你的造物，随后封印此卷</p>

        <div class="review-card surface-parchment border-ornate corner-flourish">
          <div class="review-hero">
            <h3 class="text-glow-gold">{{ character.name || '(未命名)' }}</h3>
            <p>{{ nickname }} · Lv.1</p>
          </div>

          <div class="review-grid">
            <div class="review-item" @click="goToStep(1)">
              <span class="review-label">🜲 血脉</span>
              <span class="review-val">{{ character.race?.name || '未选择' }}</span>
            </div>
            <div class="review-item" @click="goToStep(2)">
              <span class="review-label">⚔ 道途</span>
              <span class="review-val">{{ character.class?.name || '未选择' }} · {{ character.gender === 'female' ? '女' : '男' }}</span>
            </div>
            <div class="review-item" @click="goToStep(3)">
              <span class="review-label">⚄ 命石</span>
              <span class="review-val">STR {{ character.abilities.strength }} · DEX {{ character.abilities.dexterity }} · CON {{ character.abilities.constitution }} · INT {{ character.abilities.intelligence }} · WIS {{ character.abilities.wisdom }} · CHA {{ character.abilities.charisma }}</span>
            </div>
            <div class="review-item" @click="goToStep(featStepIndex)">
              <span class="review-label">📜 专长</span>
              <span class="review-val">{{ character.feats.length ? character.feats.map(f => f.name).join('、') : '未选择' }}</span>
            </div>
            <div v-if="isSpellcaster" class="review-item" @click="goToStep(spellStepIndex)">
              <span class="review-label">✦ 法术</span>
              <span class="review-val">{{ character.spells.length ? character.spells.map(s => s.name).join('、') : '未选择' }}</span>
            </div>
          </div>
        </div>

        <div v-if="saveError" class="save-error">⚠ {{ saveError }}</div>
        <button class="btn-primary save-btn ceremony-seal-btn shimmer-magical" :disabled="saving" @click="saveCharacter">
          {{ saving ? '封印中...' : '⚜ 缔造英雄' }}
        </button>
      </div>
    </div>

    <!-- Navigation -->
    <div class="builder-nav">
      <button class="btn-back rune-nav-btn" :disabled="currentStep === 0" @click="prev">
        <span aria-hidden="true">⟵</span> 退回
      </button>
      <span class="nav-hint">第 {{ currentStep + 1 }} / {{ steps.length }} 阶</span>
      <button
        v-if="currentStep < reviewStepIndex"
        class="btn-primary rune-nav-btn"
        :disabled="!canGoNext"
        @click="next"
      >
        前行 <span aria-hidden="true">⟶</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.ceremony {
  position: relative;
  padding: 48px 24px 110px;
}

/* Ceremony header */
.ceremony-header {
  text-align: center;
  margin-bottom: 36px;
  animation: fadeScaleIn .6s ease-out;
}
.ceremony-eyebrow {
  font-family: var(--font-heading);
  font-size: 11px;
  color: var(--gold-dim);
  letter-spacing: .4em;
  text-transform: uppercase;
  margin: 0 0 8px;
}
.ceremony-title {
  font-family: var(--font-display);
  font-size: 44px;
  font-weight: 900;
  margin: 0 0 8px;
  letter-spacing: .1em;
  background: linear-gradient(180deg, var(--gold-bright), var(--gold), var(--gold-dim));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  filter: drop-shadow(0 0 28px rgba(201, 162, 39, .18));
}
.ceremony-sub {
  font-family: var(--font-body);
  font-style: italic;
  color: var(--ink-soft);
  font-size: 15px;
  margin: 0;
}
.ceremony-ornament,
.ceremony-final-ornament {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin: 0 auto 10px;
  max-width: 360px;
}
.ceremony-final-ornament { margin: 0 auto 10px; }
.co-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-gold), transparent);
}
.co-glyph {
  font-size: 20px;
  color: var(--gold);
  filter: drop-shadow(0 0 8px var(--gold-glow));
}

/* Quest progression path */
.quest-path {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 0;
  margin-bottom: 36px;
}
.quest-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 6px;
  border: none;
  background: transparent;
  color: var(--muted);
  font-family: var(--font-heading);
  font-size: 11px;
  letter-spacing: .06em;
  cursor: default;
  transition: all var(--transition-base);
}
.quest-node.clickable { cursor: pointer; }
.quest-rune {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--line);
  background: radial-gradient(circle, var(--bg-card), var(--bg-stone));
  box-shadow: inset 0 0 10px rgba(0, 0, 0, .5);
  transition: all var(--transition-base);
  position: relative;
}
.quest-rune::before {
  content: "";
  position: absolute;
  inset: -4px;
  border: 1px dashed var(--line-gold);
  border-radius: 50%;
  opacity: 0;
  transition: opacity var(--transition-base);
}
.quest-rune-inner { font-size: 18px; filter: grayscale(.4); transition: all var(--transition-base); }
.quest-node.clickable:hover .quest-rune { border-color: var(--gold); transform: translateY(-2px); }
.quest-node.clickable:hover .quest-rune::before { opacity: .5; animation: rotateRunes 8s linear infinite; }
.quest-node.active .quest-rune {
  border-color: var(--gold);
  background: radial-gradient(circle, var(--gold-soft), var(--bg-stone));
  box-shadow: 0 0 18px var(--gold-glow), inset 0 0 10px rgba(201, 162, 39, .12);
}
.quest-node.active .quest-rune-inner { filter: none; transform: scale(1.1); }
.quest-node.active { color: var(--gold-bright); font-weight: 700; }
.quest-node.active .quest-rune::before { opacity: .8; animation: rotateRunes 12s linear infinite; }
.quest-node.done .quest-rune {
  border-color: var(--nature);
  background: radial-gradient(circle, var(--nature-soft), var(--bg-stone));
}
.quest-node.done .quest-rune-inner { filter: none; }
.quest-node.done { color: var(--nature); }
.quest-link {
  width: 28px;
  height: 1px;
  background: var(--line);
  margin: 0 2px;
  position: relative;
  top: -10px;
  transition: background var(--transition-base);
}
.quest-link.filled {
  background: linear-gradient(90deg, var(--nature), var(--gold));
}

/* Rite panels (parchment) */
.rite-panel {
  position: relative;
  padding: 32px 28px 28px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-deep), 0 0 24px rgba(201, 162, 39, .08);
  animation: fadeScaleIn .35s ease-out;
}
.rite-seal {
  position: absolute;
  top: -16px;
  left: 50%;
  transform: translateX(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle, var(--bg-card), var(--bg-stone));
  border: 1px solid var(--gold);
  color: var(--gold-bright);
  font-size: 20px;
  box-shadow: 0 0 18px var(--gold-glow);
}
.rite-eyebrow {
  font-family: var(--font-heading);
  font-size: 11px;
  color: var(--gold-dim);
  letter-spacing: .3em;
  text-transform: uppercase;
  margin: 0 0 6px;
}
.rite-eyebrow.center { text-align: center; }
.rite-panel .step-title { font-size: 30px; }
.rite-panel .step-title.center { text-align: center; }
.rite-panel .step-desc.center { text-align: center; }
.rite-panel .step-desc { color: var(--ink-soft); }

/* Codex entries */
.codex-entry { position: relative; }
.codex-rune {
  position: absolute;
  top: 10px;
  right: 12px;
  font-size: 16px;
  color: var(--gold-dim);
  opacity: .5;
  transition: all var(--transition-base);
}
.codex-rune.arcane { color: var(--arcane-glow); }
.codex-entry:hover .codex-rune { opacity: 1; color: var(--gold); transform: rotate(45deg); }
.codex-entry.selected .codex-rune { color: var(--gold-bright); opacity: 1; text-shadow: 0 0 10px var(--gold-glow); }

/* Rune stones (ability scores) */
.rune-stones-grid { gap: 14px; }
.rune-stone {
  position: relative;
  background:
    radial-gradient(circle at 50% 30%, var(--gold-soft), transparent 60%),
    var(--bg-card);
  border: 2px solid var(--line);
  border-radius: var(--radius-md);
}
.rune-stone::before {
  content: "";
  position: absolute;
  inset: 3px;
  border: 1px solid var(--line);
  border-radius: calc(var(--radius-md) - 3px);
  pointer-events: none;
}
.rune-stone:hover { border-color: var(--gold); }
.rune-stone .ability-select {
  background: var(--bg-stone);
  border: 2px solid var(--gold-dim);
  box-shadow: inset 0 0 8px rgba(0, 0, 0, .5);
}

/* Final ceremony button */
.ceremony-seal-btn {
  width: 100%;
  padding: 18px;
  font-size: 18px;
  letter-spacing: .15em;
  background: linear-gradient(180deg, var(--gold-soft), var(--bg-card));
  border: 2px solid var(--gold);
  color: var(--gold-bright);
  box-shadow: 0 0 28px var(--gold-glow), inset 0 1px 0 rgba(232, 196, 74, .3);
  text-shadow: 0 0 12px var(--gold-glow);
}
.ceremony-seal-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 36px var(--gold-glow), inset 0 1px 0 rgba(232, 196, 74, .4);
}

/* Nav buttons as rune buttons */
.rune-nav-btn {
  letter-spacing: .1em;
}

/* Responsive */
@media (max-width: 920px) {
  .ceremony-title { font-size: 34px; }
  .quest-path { gap: 0; }
  .quest-link { width: 16px; }
  .rite-panel { padding: 26px 18px 22px; }
  .rite-panel .step-title { font-size: 24px; }
}
@media (max-width: 560px) {
  .ceremony { padding: 32px 14px 100px; }
  .ceremony-title { font-size: 28px; }
  .quest-node { font-size: 10px; }
  .quest-rune { width: 38px; height: 38px; }
  .quest-rune-inner { font-size: 15px; }
}
</style>
