<script setup>
import { onMounted, computed } from "vue";
import { useRoute } from "vue-router";
import { useTheme } from "./composables/useTheme";
import { useAuth } from "./composables/useAuth";
import NavBar from "./components/NavBar.vue";
import ParticleBg from "./components/ParticleBg.vue";
import AppFooter from "./components/AppFooter.vue";

const route = useRoute();

// Initialize theme
useTheme();

// 尝试恢复登录会话
const { restoreSession } = useAuth();
onMounted(() => {
  restoreSession();
});

// 登录页隐藏导航栏和页脚
const isAuthPage = computed(() => route.path === "/login");
</script>

<template>
  <ParticleBg />
  <NavBar v-if="!isAuthPage" />
  <main class="app-main" :class="{ 'auth-main': isAuthPage }">
    <router-view v-slot="{ Component }">
      <Transition name="page">
        <component :is="Component" :key="route.path" />
      </Transition>
    </router-view>
  </main>
  <AppFooter v-if="!isAuthPage" />
</template>

<style>
.app-main {
  flex: 1;
  position: relative;
  z-index: 1;
}

/* 登录页全屏布局 */
.auth-main {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 页面切换淡入淡出（交叉过渡，无空白间隙） */
.page-enter-active,
.page-leave-active {
  transition: opacity .25s ease;
}
.page-enter-from {
  opacity: 0;
}
.page-leave-active {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  opacity: 0;
}
</style>
