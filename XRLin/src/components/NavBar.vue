<script setup>
import { useRoute, useRouter } from "vue-router";
import { useTheme } from "../composables/useTheme";
import { useAuth } from "../composables/useAuth";

const route = useRoute();
const router = useRouter();
const { theme, toggle } = useTheme();
const { isLoggedIn, nickname, logout } = useAuth();

const navItems = [
  { path: "/", label: "典籍首页", icon: "✦", en: "Codex" },
  { path: "/browse", label: "资料浏览", icon: "📜", en: "Archive" },
  { path: "/random", label: "命运骰", icon: "🎲", en: "Fate" },
  { path: "/characters", label: "冒险者", icon: "⚔", en: "Heroes" },
];

async function handleLogout() {
  await logout();
  router.push("/login");
}
</script>

<template>
  <nav class="navbar">
    <div class="nav-brand">
      <router-link to="/" class="nav-logo">
        <span class="logo-icon">⚜</span>
        <span class="logo-stack">
          <span class="logo-text">彩虹金刚</span>
          <span class="logo-sub">Rainbow Vajra</span>
        </span>
      </router-link>
    </div>

    <div class="nav-links">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-link"
        :class="{ active: route.path === item.path }"
      >
        <span class="nav-icon">{{ item.icon }}</span>
        <span class="nav-label">{{ item.label }}</span>
      </router-link>

      <button class="nav-theme-btn" :title="theme === 'light' ? '暗色模式' : '亮色模式'" @click="toggle">
        {{ theme === 'light' ? '🌙' : '☀️' }}
      </button>

      <!-- 用户区域 -->
      <div class="nav-user-area">
        <template v-if="isLoggedIn">
          <span class="nav-user-badge">⚜</span>
          <span class="nav-user-name">{{ nickname }}</span>
          <button class="nav-logout-btn" @click="handleLogout">离去</button>
        </template>
        <router-link v-else to="/login" class="nav-login-btn rune-btn">
          <span class="login-rune">⚔</span>
          登录
        </router-link>
      </div>
    </div>
  </nav>
</template>

<style scoped>
/* ===== Ornate metal/stone navbar ===== */
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 32px;
  background:
    linear-gradient(180deg, rgba(26, 20, 14, .97), rgba(17, 13, 10, .92)),
    radial-gradient(ellipse at 30% 0%, rgba(201, 162, 39, .06), transparent 60%);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(16px);
  position: sticky;
  top: 0;
  z-index: 50;
  box-shadow: 0 4px 24px rgba(0, 0, 0, .55), inset 0 -1px 0 rgba(0, 0, 0, .5);
}
/* Double gold filigree line along the bottom */
.navbar::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background:
    linear-gradient(90deg, transparent, var(--gold-dim) 12%, var(--gold) 50%, var(--gold-dim) 88%, transparent),
    linear-gradient(90deg, transparent, var(--line-gold) 20%, var(--line-gold) 80%, transparent);
  background-size: 100% 1px, 100% 2px;
  background-position: bottom, top;
  background-repeat: no-repeat;
  box-shadow: 0 0 12px var(--gold-glow);
}

/* ===== Brand / logo ===== */
.nav-logo {
  display: flex;
  align-items: center;
  gap: 14px;
  text-decoration: none;
  color: var(--ink-bright);
  position: relative;
}
.logo-icon {
  font-size: 28px;
  color: var(--gold-bright);
  filter: drop-shadow(0 0 8px var(--gold-glow)) drop-shadow(0 0 16px rgba(201, 162, 39, .2));
  animation: logoBreath 4s ease-in-out infinite;
}
@keyframes logoBreath {
  0%, 100% { filter: drop-shadow(0 0 8px var(--gold-glow)) drop-shadow(0 0 14px rgba(201, 162, 39, .15)); }
  50% { filter: drop-shadow(0 0 12px var(--gold-glow)) drop-shadow(0 0 26px rgba(201, 162, 39, .35)); }
}
.logo-stack { display: flex; flex-direction: column; line-height: 1; }
.logo-text {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 900;
  letter-spacing: .08em;
  background: linear-gradient(180deg, var(--gold-bright), var(--gold), var(--gold-dim));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: 0 2px 4px rgba(0, 0, 0, .5);
}

/* ===== Nav links ===== */
.nav-links { display: flex; align-items: center; gap: 2px; }
.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  color: var(--ink-soft);
  text-decoration: none;
  font-family: var(--font-heading);
  font-weight: 500;
  font-size: 13px;
  letter-spacing: .1em;
  text-transform: uppercase;
  transition: all var(--transition-base);
  position: relative;
  border-radius: var(--radius-sm);
}
/* Glowing rune underline on hover */
.nav-link::after {
  content: "";
  position: absolute;
  bottom: 3px;
  left: 50%;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--gold-bright), transparent);
  box-shadow: 0 0 8px var(--gold-glow);
  transition: width var(--transition-base);
  transform: translateX(-50%);
}
.nav-link:hover {
  color: var(--ink-bright);
  background: rgba(201, 162, 39, .06);
}
.nav-link:hover::after { width: 70%; }
.nav-link.active {
  color: var(--gold-bright);
  background: rgba(201, 162, 39, .08);
}
.nav-link.active::after { width: 70%; }
.nav-icon {
  font-size: 14px;
  filter: drop-shadow(0 0 3px var(--gold-glow));
  transition: filter var(--transition-base), transform var(--transition-base);
}
.nav-link:hover .nav-icon {
  filter: drop-shadow(0 0 8px var(--gold-bright));
  transform: translateY(-1px) scale(1.1);
}
.nav-link.active .nav-icon {
  color: var(--gold-bright);
  filter: drop-shadow(0 0 8px var(--gold-bright));
}

/* ===== Theme toggle (rune circle) ===== */
.nav-theme-btn {
  width: 40px;
  height: 40px;
  border: 1px solid var(--line-light);
  border-radius: 50%;
  background: radial-gradient(circle, var(--bg-card), var(--bg-stone));
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  margin-left: 10px;
  transition: all var(--transition-base);
  color: var(--gold);
  box-shadow: inset 0 1px 0 rgba(201, 162, 39, .12), inset 0 -2px 4px rgba(0, 0, 0, .5);
  position: relative;
}
.nav-theme-btn::before {
  content: "";
  position: absolute;
  inset: -3px;
  border: 1px dashed var(--line-gold);
  border-radius: 50%;
  opacity: 0;
  transition: opacity var(--transition-base);
}
.nav-theme-btn:hover {
  border-color: var(--gold);
  box-shadow: 0 0 16px var(--gold-glow), inset 0 1px 0 rgba(201, 162, 39, .2);
  transform: rotate(15deg);
}
.nav-theme-btn:hover::before { opacity: 1; }

/* ===== User area ===== */
.nav-user-area {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 10px;
  padding-left: 16px;
  border-left: 1px solid var(--line);
}
.nav-user-badge {
  color: var(--gold);
  font-size: 14px;
  filter: drop-shadow(0 0 5px var(--gold-glow));
}
.nav-user-name {
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
  color: var(--gold);
  letter-spacing: .05em;
}
.nav-logout-btn {
  padding: 6px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--line);
  background: var(--bg-card);
  color: var(--ink-soft);
  cursor: pointer;
  font-family: var(--font-heading);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: .06em;
  transition: all var(--transition-base);
}
.nav-logout-btn:hover {
  border-color: var(--crimson-bright);
  color: var(--crimson-bright);
  background: var(--crimson-soft);
  box-shadow: 0 0 12px rgba(155, 45, 45, .25);
}

/* ===== Login as glowing rune button ===== */
.nav-login-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border-radius: var(--radius-sm);
  text-decoration: none;
  font-size: 13px;
  letter-spacing: .1em;
  position: relative;
  overflow: hidden;
}
.nav-login-btn:hover {
  transform: translateY(-1px);
}
.login-rune {
  font-size: 14px;
  filter: drop-shadow(0 0 4px var(--gold-glow));
  transition: filter var(--transition-base);
}
.nav-login-btn:hover .login-rune {
  filter: drop-shadow(0 0 8px var(--gold-bright));
}

.logo-sub {
  font-family: var(--font-heading);
  font-size: 9px;
  color: var(--gold-dim);
  letter-spacing: .28em;
  text-transform: uppercase;
  margin-top: 3px;
}

@media (min-width: 768px) {
  .logo-sub { display: inline; }
}
@media (max-width: 767px) {
  .logo-sub { display: none; }
  .logo-stack { display: none; }
}
@media (max-width: 560px) {
  .navbar { padding: 10px 14px; }
  .nav-link { padding: 7px 10px; font-size: 12px; }
  .nav-label { display: none; }
  .nav-icon { font-size: 16px; margin: 0; }
}
</style>
