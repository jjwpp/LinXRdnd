import { ref, watchEffect } from "vue";

const THEME_KEY = "xrlin-theme";
const theme = ref(localStorage.getItem(THEME_KEY) || "light");

export function useTheme() {
  function toggle() {
    theme.value = theme.value === "light" ? "dark" : "light";
  }

  watchEffect(() => {
    document.documentElement.setAttribute("data-theme", theme.value);
    localStorage.setItem(THEME_KEY, theme.value);
  });

  return { theme, toggle };
}
