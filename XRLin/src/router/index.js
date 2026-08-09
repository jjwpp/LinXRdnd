import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/login",
    name: "Login",
    component: () => import("../pages/LoginPage.vue"),
  },
  {
    path: "/",
    name: "Home",
    component: () => import("../pages/HomePage.vue"),
  },
  {
    path: "/browse",
    name: "Browse",
    component: () => import("../pages/BrowsePage.vue"),
  },
  {
    path: "/detail/:category/:id",
    name: "Detail",
    component: () => import("../pages/DetailPage.vue"),
    props: true,
  },
  {
    path: "/random",
    name: "Random",
    component: () => import("../pages/RandomPage.vue"),
  },
  {
    path: "/characters",
    name: "Characters",
    component: () => import("../pages/CharacterList.vue"),
  },
  {
    path: "/characters/new",
    name: "CharacterBuilder",
    component: () => import("../pages/CharacterBuilder.vue"),
  },
  {
    path: "/adventure",
    name: "Adventure",
    component: () => import("../pages/AdventurePage.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
