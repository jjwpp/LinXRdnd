<template>
  <footer class="app-footer">
    <div class="footer-gild-line" aria-hidden="true"></div>

    <div class="footer-crest" aria-hidden="true">
      <span class="crest-rune">✦</span>
      <span class="crest-sigil">⚜</span>
      <span class="crest-rune">✦</span>
    </div>

    <p class="footer-title">彩虹金刚 · Rainbow Vajra Codex</p>
    <p class="footer-hint">
      内容为 SRD 风格摘要，完整规则请参考官方规则书
    </p>
    <p class="footer-copy">DND Ultimate Codex — Forged in Shadow &amp; Gold</p>

    <div class="footer-corner fl" aria-hidden="true">⟡</div>
    <div class="footer-corner fr" aria-hidden="true">⟡</div>
  </footer>
</template>

<style scoped>
.app-footer {
  padding: 38px 24px 30px;
  border-top: 1px solid var(--line);
  text-align: center;
  color: var(--muted);
  font-size: 13px;
  background:
    linear-gradient(180deg, var(--bg-dark), var(--bg-void)),
    radial-gradient(ellipse at 50% 0%, rgba(201, 162, 39, .05), transparent 60%);
  margin-top: auto;
  position: relative;
}

/* Double gilded top border */
.footer-gild-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background:
    linear-gradient(90deg, transparent, var(--gold-dim) 12%, var(--gold) 50%, var(--gold-dim) 88%, transparent),
    linear-gradient(90deg, transparent, var(--line-gold) 20%, var(--line-gold) 80%, transparent);
  background-size: 100% 1px, 100% 2px;
  background-position: bottom, top;
  background-repeat: no-repeat;
  box-shadow: 0 0 14px var(--gold-glow);
}

.footer-crest {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  margin-bottom: 12px;
}
.crest-rune {
  font-size: 12px;
  color: var(--gold-dim);
  letter-spacing: .3em;
  filter: drop-shadow(0 0 4px var(--gold-glow));
}
.crest-sigil {
  font-size: 22px;
  color: var(--gold);
  filter: drop-shadow(0 0 10px var(--gold-glow));
  animation: crestBreath 4s ease-in-out infinite;
}
@keyframes crestBreath {
  0%, 100% { opacity: .7; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.08); }
}

.app-footer p {
  margin-bottom: 4px;
  font-family: var(--font-heading);
  letter-spacing: .08em;
}
.footer-title {
  font-family: var(--font-display) !important;
  font-size: 15px;
  color: var(--gold);
  letter-spacing: .1em;
  margin-bottom: 4px;
  text-shadow: 0 0 12px var(--gold-glow);
}
.footer-hint {
  font-size: 12px;
  opacity: .6;
  font-style: italic;
  font-family: var(--font-body) !important;
}
.footer-copy {
  font-family: var(--font-heading) !important;
  font-size: 11px;
  color: var(--muted);
  letter-spacing: .15em;
  text-transform: uppercase;
  margin-top: 8px;
  opacity: .5;
}

.footer-corner {
  position: absolute;
  top: 14px;
  font-size: 12px;
  color: var(--gold-dim);
  opacity: .5;
}
.footer-corner.fl { left: 24px; }
.footer-corner.fr { right: 24px; }
</style>
