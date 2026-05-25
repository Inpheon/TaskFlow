import { defineStore } from "pinia";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    accessToken: null as string | null,
    displayName: null as string | null
  }),
  actions: {
    setSession(accessToken: string, displayName: string) {
      this.accessToken = accessToken;
      this.displayName = displayName;
    },
    clearSession() {
      this.accessToken = null;
      this.displayName = null;
    }
  }
});

