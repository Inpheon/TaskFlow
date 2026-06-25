import { createApp } from "vue";
import { createPinia } from "pinia";
import PrimeVue from "primevue/config";
import Aura from "@primevue/themes/aura";
import App from "./App.vue";
import { configureHttpAuth } from "./api/http";
import { createAppRouter } from "./router";
import { useAuthStore } from "./stores/auth";
import "./style.css";

async function bootstrap() {
  const app = createApp(App);
  const pinia = createPinia();
  const router = createAppRouter(pinia);
  const auth = useAuthStore(pinia);

  app.use(pinia);
  app.use(PrimeVue, {
    theme: {
      preset: Aura,
      options: { darkModeSelector: false },
    },
  });

  configureHttpAuth(
    () => auth.accessToken,
    () => {
      auth.clearSession();
      void router.push({ name: "login" });
    }
  );

  await auth.initialize();
  app.use(router);
  await router.isReady();
  app.mount("#app");
}

void bootstrap();
