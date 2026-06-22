<script setup lang="ts">
import { reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import AuthLayout from "@/components/AuthLayout.vue";
import BaseButton from "@/components/BaseButton.vue";
import FormField from "@/components/FormField.vue";
import { ApiClientError } from "@/api/http";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const form = reactive({
  email: "",
  password: ""
});
const submitting = ref(false);
const error = ref<string | null>(null);

async function submit() {
  error.value = null;
  submitting.value = true;
  try {
    await auth.login({
      email: form.email.trim(),
      password: form.password
    });
    const redirect = typeof route.query.redirect === "string"
      ? route.query.redirect
      : "/projects";
    await router.push(redirect);
  } catch (caught) {
    error.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to sign in";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <AuthLayout title="Sign in" subtitle="Use your account to continue.">
    <form class="form-stack auth-form" @submit.prevent="submit">
      <FormField label="Email">
        <input
          v-model="form.email"
          name="email"
          type="email"
          autocomplete="email"
          required
          autofocus
        />
      </FormField>
      <FormField label="Password">
        <input
          v-model="form.password"
          name="password"
          type="password"
          autocomplete="current-password"
          required
        />
      </FormField>
      <p v-if="error" class="form-error" role="alert">{{ error }}</p>
      <BaseButton class="full-width" type="submit" variant="primary" :disabled="submitting">
        {{ submitting ? "Signing in..." : "Sign in" }}
      </BaseButton>
    </form>
    <p class="auth-switch">
      New to TaskFlow?
      <RouterLink :to="{ name: 'register' }">Create an account</RouterLink>
    </p>
  </AuthLayout>
</template>
