import type { Pinia } from "pinia";
import {
  createRouter,
  createWebHistory,
  type RouterHistory
} from "vue-router";
import LoginPage from "@/pages/LoginPage.vue";
import ProjectDetailPage from "@/pages/ProjectDetailPage.vue";
import ProjectsPage from "@/pages/ProjectsPage.vue";
import RegisterPage from "@/pages/RegisterPage.vue";
import { useAuthStore } from "@/stores/auth";

export function createAppRouter(
  pinia: Pinia,
  history: RouterHistory = createWebHistory()
) {
  const router = createRouter({
    history,
    routes: [
      {
        path: "/",
        redirect: "/projects"
      },
      {
        path: "/login",
        name: "login",
        component: LoginPage,
        meta: { guestOnly: true }
      },
      {
        path: "/register",
        name: "register",
        component: RegisterPage,
        meta: { guestOnly: true }
      },
      {
        path: "/projects",
        name: "projects",
        component: ProjectsPage,
        meta: { requiresAuth: true }
      },
      {
        path: "/projects/:projectId",
        name: "project-detail",
        component: ProjectDetailPage,
        meta: { requiresAuth: true }
      },
      {
        path: "/:pathMatch(.*)*",
        redirect: "/projects"
      }
    ]
  });

  router.beforeEach(to => {
    const auth = useAuthStore(pinia);
    if (to.meta.requiresAuth && !auth.isAuthenticated) {
      return {
        name: "login",
        query: { redirect: to.fullPath }
      };
    }
    if (to.meta.guestOnly && auth.isAuthenticated) {
      return { name: "projects" };
    }
    return true;
  });

  return router;
}
