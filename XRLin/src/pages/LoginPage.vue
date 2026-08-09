<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useAuth } from "../composables/useAuth";

const router = useRouter();
const { login, register, fetchCaptcha, isLoggedIn } = useAuth();

const mode = ref("login"); // login | register
const loading = ref(false);
const errorMsg = ref("");

// 表单数据
const form = ref({
  nickname: "",
  username: "",
  password: "",
  captcha: "",
});

// 验证码
const captchaId = ref("");
const captchaText = ref("");
const captchaLoading = ref(false);
const captchaCountdown = ref(0);

onMounted(() => {
  if (isLoggedIn.value) {
    router.replace("/");
    return;
  }
  refreshCaptcha();
});

async function refreshCaptcha() {
  captchaLoading.value = true;
  errorMsg.value = "";
  try {
    const data = await fetchCaptcha();
    captchaId.value = data.captchaId;
    captchaText.value = data.captcha;
    form.value.captcha = "";
    startCountdown();
  } catch (e) {
    errorMsg.value = e.message;
  } finally {
    captchaLoading.value = false;
  }
}

function startCountdown() {
  captchaCountdown.value = 60;
  const timer = setInterval(() => {
    captchaCountdown.value--;
    if (captchaCountdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);
}

async function handleSubmit() {
  errorMsg.value = "";
  if (!form.value.username.trim()) {
    errorMsg.value = "请输入用户名";
    return;
  }
  if (!form.value.password.trim()) {
    errorMsg.value = "请输入密码";
    return;
  }
  if (mode.value === "register" && !form.value.nickname.trim()) {
    errorMsg.value = "请输入昵称";
    return;
  }
  if (!form.value.captcha.trim()) {
    errorMsg.value = "请输入验证码";
    return;
  }

  loading.value = true;
  try {
    const payload = {
      username: form.value.username.trim(),
      password: form.value.password,
      captchaId: captchaId.value,
      captcha: form.value.captcha.trim(),
    };
    if (mode.value === "register") {
      payload.nickname = form.value.nickname.trim();
      await register(payload);
    } else {
      await login(payload);
    }
    router.replace("/");
  } catch (e) {
    errorMsg.value = e.message;
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
}

function switchMode() {
  mode.value = mode.value === "login" ? "register" : "login";
  errorMsg.value = "";
  form.value.captcha = "";
  refreshCaptcha();
}
</script>

<template>
  <div class="auth-page portal">
    <!-- Full-screen dark fantasy backdrop -->
    <div class="portal-bg" aria-hidden="true"></div>
    <div class="portal-bg-overlay" aria-hidden="true"></div>

    <!-- Magical particle field -->
    <div class="portal-particles" aria-hidden="true">
      <span class="portal-spark" v-for="n in 18" :key="n"></span>
    </div>

    <!-- Twin rune circles -->
    <div class="portal-runes" aria-hidden="true"></div>
    <div class="portal-rune-ring" aria-hidden="true"></div>

    <!-- Floating magical scroll / portal -->
    <div class="auth-card portal-scroll surface-parchment border-ornate corner-flourish">
      <div class="portal-scroll-top" aria-hidden="true">
        <span class="roller-left">⚜</span>
        <span class="roller-right">⚜</span>
      </div>

      <!-- Logo / Portal sigil -->
      <div class="auth-header">
        <div class="portal-sigil" aria-hidden="true">
          <span class="sigil-core">⚜</span>
        </div>
        <p class="auth-eyebrow">ADVENTURER'S GATE</p>
        <h1 class="auth-title text-glow-gold">冒险者登录</h1>
        <p class="auth-subtitle">穿越符文之门 · 步入传奇</p>
      </div>

      <!-- 模式切换 -->
      <div class="auth-tabs">
        <button
          class="auth-tab"
          :class="{ active: mode === 'login' }"
          @click="mode = 'login'; errorMsg = ''"
        >登·入</button>
        <button
          class="auth-tab"
          :class="{ active: mode === 'register' }"
          @click="mode = 'register'; errorMsg = ''"
        >立·誓</button>
      </div>

      <!-- 表单 -->
      <form class="auth-form" @submit.prevent="handleSubmit">
        <div class="form-field" v-if="mode === 'register'">
          <label class="field-label">❖ 昵称 · APPellation</label>
          <input
            v-model="form.nickname"
            type="text"
            class="field-input"
            placeholder="输入你的称号"
            maxlength="50"
          />
        </div>

        <div class="form-field">
          <label class="field-label">⚔ 用户名 · SIGIL NAME</label>
          <input
            v-model="form.username"
            type="text"
            class="field-input"
            placeholder="输入真名"
            maxlength="50"
            @keydown.enter="handleSubmit"
          />
        </div>

        <div class="form-field">
          <label class="field-label">🕮 密文 · ARCANE WORD</label>
          <input
            v-model="form.password"
            type="password"
            class="field-input"
            placeholder="输入密文（至少6位）"
            maxlength="100"
            @keydown.enter="handleSubmit"
          />
        </div>

        <div class="form-field">
          <label class="field-label">✦ 契印 · SIGIL OF PROOF</label>
          <div class="captcha-row">
            <input
              v-model="form.captcha"
              type="text"
              class="field-input captcha-input"
              placeholder="辨识符文"
              maxlength="4"
              @keydown.enter="handleSubmit"
            />
            <button
              type="button"
              class="captcha-display"
              :disabled="captchaLoading"
              @click="refreshCaptcha"
              :title="captchaCountdown > 0 ? `${captchaCountdown}s 后可重铸` : '点击重铸契印'"
            >
              <span v-if="captchaLoading" class="captcha-spinner"></span>
              <span v-else class="captcha-text">{{ captchaText }}</span>
            </button>
          </div>
          <p class="captcha-hint" v-if="captchaCountdown > 0">
            契印效力尚余 {{ captchaCountdown }}s
          </p>
        </div>

        <!-- 错误提示 -->
        <div v-if="errorMsg" class="auth-error">
          <span>⚠ {{ errorMsg }}</span>
        </div>

        <!-- 提交 -->
        <button type="submit" class="auth-submit rune-portal-btn shimmer-magical" :disabled="loading">
          <span v-if="loading" class="submit-spinner"></span>
          <span v-else>{{ mode === "login" ? "⚜ 步入传送门" : "✦ 缔结誓约" }}</span>
        </button>
      </form>

      <!-- 切换 -->
      <p class="auth-switch">
        <span v-if="mode === 'login'">
          尚未立誓？<a @click="switchMode">缔结新约</a>
        </span>
        <span v-else>
          已有誓约？<a @click="switchMode">重返传送门</a>
        </span>
      </p>

      <div class="portal-scroll-bottom" aria-hidden="true">
        <span class="roller-left">⚜</span>
        <span class="roller-right">⚜</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.portal {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
}

/* Full-screen dark fantasy backdrop */
.portal-bg {
  position: absolute;
  inset: 0;
  background: var(--hero-bg) center / cover no-repeat;
  filter: brightness(.32) contrast(1.25) saturate(.7);
  z-index: 0;
  animation: portalBgDrift 30s ease-in-out infinite alternate;
}
@keyframes portalBgDrift {
  from { transform: scale(1) translateY(0); }
  to { transform: scale(1.08) translateY(-12px); }
}
.portal-bg-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at center, rgba(93, 58, 138, .18) 0%, transparent 45%),
    radial-gradient(ellipse at center, transparent 30%, rgba(11, 9, 8, .65) 75%, var(--bg-void) 100%),
    linear-gradient(180deg, rgba(11, 9, 8, .4) 0%, rgba(11, 9, 8, .7) 70%, var(--bg-void) 100%);
  z-index: 1;
}

/* Particle sparks */
.portal-particles {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}
.portal-spark {
  position: absolute;
  bottom: 0;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--gold-bright);
  box-shadow: 0 0 8px var(--gold-bright);
  opacity: 0;
  animation: sparkFloat 9s ease-in-out infinite;
}
/* Distribute sparks across the portal with staggered positions & delays */
.portal-spark:nth-child(1)  { left: 5%;  animation-delay: 0s;   animation-duration: 8s; }
.portal-spark:nth-child(2)  { left: 12%; animation-delay: 1.2s; animation-duration: 10s; }
.portal-spark:nth-child(3)  { left: 20%; animation-delay: 2.4s; animation-duration: 9s; }
.portal-spark:nth-child(4)  { left: 28%; animation-delay: .6s;  animation-duration: 11s; }
.portal-spark:nth-child(5)  { left: 36%; animation-delay: 3s;   animation-duration: 8.5s; }
.portal-spark:nth-child(6)  { left: 44%; animation-delay: 1.8s; animation-duration: 9.5s; }
.portal-spark:nth-child(7)  { left: 52%; animation-delay: 4.2s; animation-duration: 10.5s; }
.portal-spark:nth-child(8)  { left: 58%; animation-delay: .3s;  animation-duration: 8s; }
.portal-spark:nth-child(9)  { left: 64%; animation-delay: 2.1s; animation-duration: 9s; }
.portal-spark:nth-child(10) { left: 70%; animation-delay: 3.6s; animation-duration: 11.5s; }
.portal-spark:nth-child(11) { left: 76%; animation-delay: 1.5s; animation-duration: 8.5s; }
.portal-spark:nth-child(12) { left: 82%; animation-delay: 4.8s; animation-duration: 10s; }
.portal-spark:nth-child(13) { left: 88%; animation-delay: 2.7s; animation-duration: 9.5s; }
.portal-spark:nth-child(14) { left: 94%; animation-delay: .9s;  animation-duration: 8s; }
.portal-spark:nth-child(15) { left: 16%; animation-delay: 5.1s; animation-duration: 10.5s; }
.portal-spark:nth-child(16) { left: 40%; animation-delay: 3.3s; animation-duration: 9s; }
.portal-spark:nth-child(17) { left: 66%; animation-delay: 5.7s; animation-duration: 11s; }
.portal-spark:nth-child(18) { left: 90%; animation-delay: 6.3s; animation-duration: 8.5s; }
@keyframes sparkFloat {
  0% { transform: translateY(0) scale(0); opacity: 0; }
  15% { opacity: .9; }
  85% { opacity: .4; }
  100% { transform: translateY(-340px) scale(1.4); opacity: 0; }
}

/* Twin rune circles */
.portal-runes {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 680px;
  height: 680px;
  border: 1px solid var(--line-gold);
  border-radius: 50%;
  opacity: .12;
  pointer-events: none;
  z-index: 2;
  animation: rotateRunes 100s linear infinite;
}
.portal-runes::before {
  content: "";
  position: absolute;
  inset: 70px;
  border: 1px dashed var(--line-gold);
  border-radius: 50%;
}
.portal-rune-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 460px;
  height: 460px;
  border: 1px solid var(--arcane-glow);
  border-radius: 50%;
  opacity: .15;
  pointer-events: none;
  z-index: 2;
  animation: rotateRunes 60s linear infinite reverse;
}
@keyframes rotateRunes {
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

/* Floating scroll / portal card */
.portal-scroll {
  width: 100%;
  max-width: 440px;
  padding: 44px 38px 36px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-deep), 0 0 50px rgba(201, 162, 39, .18), 0 0 80px rgba(93, 58, 138, .12);
  position: relative;
  z-index: 3;
  animation: portalFadeIn .7s cubic-bezier(.2, .8, .2, 1);
}
@keyframes portalFadeIn {
  from { opacity: 0; transform: translateY(24px) scale(.96); filter: blur(4px); }
  to { opacity: 1; transform: translateY(0) scale(1); filter: blur(0); }
}
.portal-scroll::before {
  content: "";
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, var(--gold), var(--arcane-glow), var(--gold), transparent);
}

/* Scroll rollers */
.portal-scroll-top,
.portal-scroll-bottom {
  position: absolute;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  pointer-events: none;
}
.portal-scroll-top { top: -10px; }
.portal-scroll-bottom { bottom: -10px; }
.roller-left, .roller-right {
  font-size: 18px;
  color: var(--gold);
  filter: drop-shadow(0 0 8px var(--gold-glow));
  opacity: .8;
}

/* Header / sigil */
.auth-header {
  text-align: center;
  margin-bottom: 26px;
}
.portal-sigil {
  width: 64px;
  height: 64px;
  margin: 0 auto 10px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--gold);
  background: radial-gradient(circle, var(--gold-soft), transparent 70%);
  box-shadow: 0 0 24px var(--gold-glow), inset 0 0 14px rgba(201, 162, 39, .12);
  animation: sigilPulse 3s ease-in-out infinite;
}
@keyframes sigilPulse {
  0%, 100% { box-shadow: 0 0 24px var(--gold-glow), inset 0 0 14px rgba(201, 162, 39, .12); }
  50% { box-shadow: 0 0 40px var(--gold-glow), 0 0 60px rgba(93, 58, 138, .2), inset 0 0 18px rgba(201, 162, 39, .2); }
}
.sigil-core {
  font-size: 30px;
  color: var(--gold-bright);
  text-shadow: 0 0 14px var(--gold-glow);
}
.auth-eyebrow {
  font-family: var(--font-heading);
  font-size: 11px;
  color: var(--gold-dim);
  letter-spacing: .4em;
  text-transform: uppercase;
  margin: 0 0 6px;
}
.auth-title {
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 900;
  margin: 0;
  letter-spacing: .08em;
  background: linear-gradient(180deg, var(--gold-bright), var(--gold), var(--gold-dim));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.auth-subtitle {
  font-family: var(--font-body);
  font-size: 13px;
  color: var(--ink-soft);
  font-style: italic;
  margin-top: 6px;
  letter-spacing: .04em;
}

/* Tabs */
.auth-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  background: var(--bg-dark);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 4px;
}
.auth-tab {
  flex: 1;
  padding: 10px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--ink-soft);
  font-family: var(--font-heading);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: .12em;
  cursor: pointer;
  transition: all var(--transition-base);
}
.auth-tab.active {
  background: linear-gradient(180deg, var(--bg-card), var(--bg-stone));
  color: var(--gold-bright);
  box-shadow: inset 0 1px 0 var(--line-gold), 0 2px 8px rgba(0, 0, 0, .3);
  border: 1px solid var(--gold-dim);
  text-shadow: 0 0 10px var(--gold-glow);
}

/* Form */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field-label {
  font-family: var(--font-heading);
  font-size: 12px;
  font-weight: 700;
  color: var(--gold);
  letter-spacing: .14em;
  text-transform: uppercase;
}
.field-input {
  width: 100%;
  height: 46px;
  padding: 0 16px;
  border: 1px solid var(--line-light);
  border-radius: var(--radius-sm);
  background: linear-gradient(180deg, var(--bg-stone), var(--bg-dark));
  color: var(--ink-bright);
  font-family: var(--font-body);
  font-size: 15px;
  outline: none;
  transition: all var(--transition-base);
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, .4);
}
.field-input:focus {
  border-color: var(--gold);
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, .4), 0 0 0 3px var(--gold-soft), 0 0 16px var(--gold-glow);
}
.field-input::placeholder {
  color: var(--muted);
  font-style: italic;
}

/* Captcha */
.captcha-row {
  display: flex;
  gap: 10px;
  align-items: stretch;
}
.captcha-input {
  flex: 1;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}
.captcha-display {
  flex-shrink: 0;
  width: 124px;
  height: 46px;
  border: 1px solid var(--gold-dim);
  border-radius: var(--radius-sm);
  background:
    linear-gradient(135deg, var(--bg-dark), var(--bg-card)),
    var(--texture-parchment);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-base);
  position: relative;
  overflow: hidden;
  box-shadow: inset 0 0 12px rgba(0, 0, 0, .5);
}
.captcha-display:hover {
  border-color: var(--gold);
  box-shadow: inset 0 0 12px rgba(0, 0, 0, .5), 0 0 16px var(--gold-glow);
}
.captcha-text {
  font-family: var(--font-rune), "Courier New", monospace;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.2em;
  color: var(--gold);
  text-shadow: 0 0 10px var(--gold-glow);
  user-select: none;
  transform: skewX(-6deg);
  filter: drop-shadow(1px 1px 0 var(--arcane)) drop-shadow(-1px -1px 0 var(--frost));
}
.captcha-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--line-light);
  border-top-color: var(--gold);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.captcha-hint {
  font-size: 11px;
  color: var(--muted);
  margin: 0;
  font-style: italic;
  letter-spacing: .04em;
}

/* Error */
.auth-error {
  background: var(--crimson-soft);
  border: 1px solid var(--crimson);
  color: var(--crimson-bright);
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-family: var(--font-body);
  animation: shake 0.3s ease;
  text-shadow: 0 0 8px rgba(155, 45, 45, .4);
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

/* Submit — glowing rune portal button */
.auth-submit.rune-portal-btn {
  width: 100%;
  height: 52px;
  border: 2px solid var(--gold);
  border-radius: var(--radius-sm);
  background: linear-gradient(180deg, var(--gold-soft), var(--bg-card));
  color: var(--gold-bright);
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: .18em;
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: 0 0 24px var(--gold-glow), inset 0 1px 0 rgba(232, 196, 74, .3);
  text-shadow: 0 0 12px var(--gold-glow);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 4px;
}
.auth-submit.rune-portal-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 0 40px var(--gold-glow), 0 0 60px rgba(93, 58, 138, .25), inset 0 1px 0 rgba(232, 196, 74, .4);
  border-color: var(--gold-bright);
}
.auth-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.submit-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(201, 162, 39, .3);
  border-top-color: var(--gold-bright);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/* Switch link */
.auth-switch {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--muted);
  font-style: italic;
  font-family: var(--font-body);
}
.auth-switch a {
  color: var(--gold);
  cursor: pointer;
  font-weight: 600;
  font-style: normal;
  font-family: var(--font-heading);
  letter-spacing: .05em;
  text-decoration: none;
  transition: all var(--transition-base);
}
.auth-switch a:hover {
  color: var(--gold-bright);
  text-shadow: 0 0 10px var(--gold-glow);
}

/* Responsive */
@media (max-width: 600px) {
  .portal-runes { width: 460px; height: 460px; }
  .portal-rune-ring { width: 320px; height: 320px; }
  .portal-scroll { padding: 36px 22px 28px; }
  .auth-title { font-size: 26px; }
}
@media (max-width: 480px) {
  .portal-scroll { padding: 32px 18px 24px; }
  .auth-title { font-size: 23px; }
  .captcha-display { width: 104px; }
  .captcha-text { font-size: 19px; }
  .portal-runes { width: 340px; height: 340px; }
  .portal-rune-ring { width: 240px; height: 240px; }
}
</style>
