<script setup>
defineProps({
  character: Object,
  raceName: String,
  className: String,
});

defineEmits(["click", "delete"]);
</script>

<template>
  <div class="character-card dossier surface-parchment border-ornate corner-flourish" @click="$emit('click', character)">
    <!-- Ornate portrait frame -->
    <div class="dossier-portrait" aria-hidden="true">
      <div class="portrait-frame">
        <span class="portrait-sigil">{{ (className || character.name || '?').charAt(0) }}</span>
        <span class="portrait-ring"></span>
      </div>
    </div>

    <div class="ch-card-header dossier-header">
      <h3 class="dossier-name text-glow-gold">{{ character.name }}</h3>
      <span class="ch-level dossier-level">Lv.{{ character.level }}</span>
    </div>
    <p class="ch-card-meta dossier-meta">{{ raceName || '?' }} · {{ className || '?' }}</p>
    <p class="ch-card-player dossier-player">勇者 · {{ character.playerName || '未知' }}</p>
    <p v-if="character.subtitle" class="ch-card-summary dossier-summary">{{ character.subtitle }}</p>

    <button class="ch-delete-btn dossier-delete" @click.stop="$emit('delete', character)" title="抹除此卷宗">✕</button>
  </div>
</template>

<style scoped>
.dossier {
  position: relative;
  padding: 20px 20px 18px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-card), 0 0 18px rgba(201, 162, 39, .08);
  overflow: hidden;
}
.dossier::after {
  content: "";
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 100%;
  background: linear-gradient(180deg, rgba(201, 162, 39, .05) 0%, transparent 35%);
  pointer-events: none;
}
.dossier:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-deep), 0 0 30px var(--gold-glow);
}

/* Ornate portrait frame */
.dossier-portrait {
  display: flex;
  justify-content: center;
  margin-bottom: 14px;
}
.portrait-frame {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--gold-dim);
  background: radial-gradient(circle at 40% 30%, var(--gold-soft), var(--bg-card) 60%, var(--bg-stone));
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, .5), 0 0 14px var(--gold-glow);
  transition: all var(--transition-base);
}
.portrait-frame::before {
  content: "";
  position: absolute;
  inset: 4px;
  border: 1px solid var(--line-gold);
  border-radius: 50%;
  pointer-events: none;
}
.portrait-sigil {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 900;
  color: var(--gold-bright);
  text-shadow: 0 0 12px var(--gold-glow);
}
.portrait-ring {
  position: absolute;
  inset: -6px;
  border: 1px dashed var(--line-gold);
  border-radius: 50%;
  opacity: .4;
  transition: opacity var(--transition-base);
}
.dossier:hover .portrait-frame {
  border-color: var(--gold);
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, .4), 0 0 24px var(--gold-glow);
}
.dossier:hover .portrait-ring {
  opacity: 1;
  animation: dossierRing 12s linear infinite;
}
@keyframes dossierRing {
  to { transform: rotate(360deg); }
}

/* Header */
.dossier-header {
  align-items: center;
  margin-bottom: 4px;
}
.dossier-name {
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 800;
  color: var(--ink-bright);
  letter-spacing: .04em;
  margin: 0;
}
.dossier-level {
  font-family: var(--font-display);
  font-size: 12px;
  background: linear-gradient(180deg, var(--gold-bright), var(--gold));
  color: var(--bg-void);
  padding: 2px 12px;
  border-radius: 2px;
  font-weight: 800;
  letter-spacing: .06em;
  box-shadow: 0 2px 6px var(--gold-glow);
}
.dossier-meta {
  font-family: var(--font-heading);
  font-size: 13px;
  color: var(--gold);
  margin: 0 0 2px;
  letter-spacing: .04em;
}
.dossier-player {
  font-size: 12px;
  color: var(--muted);
  font-style: italic;
  margin: 0;
}
.dossier-summary {
  font-size: 12px;
  color: var(--ink-soft);
  margin-top: 8px;
  line-height: 1.5;
  font-style: italic;
}

/* Delete */
.dossier-delete {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: var(--bg-stone);
  color: var(--muted);
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all var(--transition-base);
}
.dossier:hover .dossier-delete { opacity: 1; }
.dossier-delete:hover {
  background: var(--crimson);
  color: #fff;
  border-color: var(--crimson);
  box-shadow: 0 0 12px rgba(107, 29, 29, .5);
}
</style>
