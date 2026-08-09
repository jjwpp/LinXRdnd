import { ref, computed } from "vue";

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080/api";

// ── 全局响应式状态 ──
const token = ref(localStorage.getItem("auth_token") || "");
const user = ref(JSON.parse(localStorage.getItem("auth_user") || "null"));

const isLoggedIn = computed(() => !!token.value);
const nickname = computed(() => user.value?.nickname || "");

export function useAuth() {
  /** 获取验证码 */
  async function fetchCaptcha() {
    const res = await fetch(`${API_BASE}/auth/captcha`, { method: "POST" });
    const json = await res.json();
    if (json.code !== 200) throw new Error(json.msg || "获取验证码失败");
    return json.data; // { captchaId, captcha }
  }

  /** 注册 */
  async function register(data) {
    const res = await fetch(`${API_BASE}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    const json = await res.json();
    if (json.code !== 200) throw new Error(json.msg || "注册失败");
    setSession(json.data);
    return json.data;
  }

  /** 登录 */
  async function login(data) {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });
    const json = await res.json();
    if (json.code !== 200) throw new Error(json.msg || "登录失败");
    setSession(json.data);
    return json.data;
  }

  /** 登出 */
  async function logout() {
    try {
      await fetch(`${API_BASE}/auth/logout`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token.value}` },
      });
    } catch { /* ignore */ }
    clearSession();
  }

  /** 从后端恢复会话 */
  async function restoreSession() {
    if (!token.value) return false;
    try {
      const res = await fetch(`${API_BASE}/auth/me`, {
        headers: { Authorization: `Bearer ${token.value}` },
      });
      const json = await res.json();
      if (json.code === 200 && json.data) {
        setSession(json.data);
        return true;
      }
    } catch { /* ignore */ }
    clearSession();
    return false;
  }

  function setSession(data) {
    token.value = data.token;
    user.value = data.user;
    localStorage.setItem("auth_token", data.token);
    localStorage.setItem("auth_user", JSON.stringify(data.user));
  }

  function clearSession() {
    token.value = "";
    user.value = null;
    localStorage.removeItem("auth_token");
    localStorage.removeItem("auth_user");
  }

  /** 获取带 Authorization header 的配置对象（供其他 API 调用使用） */
  function authHeaders() {
    const h = {};
    if (token.value) h["Authorization"] = `Bearer ${token.value}`;
    return h;
  }

  return {
    token,
    user,
    isLoggedIn,
    nickname,
    fetchCaptcha,
    register,
    login,
    logout,
    restoreSession,
    authHeaders,
  };
}
