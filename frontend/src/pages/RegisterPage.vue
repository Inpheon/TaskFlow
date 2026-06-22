<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import AuthLayout from "@/components/AuthLayout.vue";
import BaseButton from "@/components/BaseButton.vue";
import FormField from "@/components/FormField.vue";
import { ApiClientError } from "@/api/http";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();
const form = reactive({
  displayName: "",
  email: "",
  password: ""
});
const touched = ref(false);
const submitting = ref(false);
const error = ref<string | null>(null);

const validationError = computed(() => {
  if (!touched.value) {
    return null;
  }
  if (!form.displayName.trim()) {
    return "Display name is required.";
  }
  if (form.displayName.length > 120) {
    return "Display name must be 120 characters or fewer.";
  }
  if (form.password.length < 8 || form.password.length > 100) {
    return "Password must contain between 8 and 100 characters.";
  }
  return null;
});

async function submit() {
  touched.value = true;
  error.value = null;
  if (validationError.value) {
    return;
  }
  submitting.value = true;
  try {
    await auth.register({
      displayName: form.displayName.trim(),
      email: form.email.trim(),
      password: form.password
    });
    await router.push({ name: "projects" });
  } catch (caught) {
    error.value = caught instanceof ApiClientError
      ? caught.message
      : "Unable to create the account";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <AuthLayout title="Create account" subtitle="Enter your details to create a TaskFlow account.">
    <form class="form-stack auth-form" @submit.prevent="submit">
      <FormField label="Display name">
        <input
          v-model="form.displayName"
          name="displayName"
          autocomplete="name"
          maxlength="121"
          required
          autofocus
        />
      </FormField>
      <FormField label="Email">
        <input
          v-model="form.email"
          name="email"
          type="email"
          autocomplete="email"
          required
        />
      </FormField>
      <FormField label="Password" hint="8 to 100 characters">
        <input
          v-model="form.password"
          name="password"
          type="password"
          autocomplete="new-password"
          minlength="8"
          maxlength="100"
          required
        />
      </FormField>
      <p v-if="validationError" class="form-error" role="alert">{{ validationError }}</p>
      <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>
      <BaseButton class="full-width" type="submit" variant="primary" :disabled="submitting">
        {{ submitting ? "Creating account..." : "Create account" }}
      </BaseButton>
    </form>
    <p class="auth-switch">
      Already have an account?
      <RouterLink :to="{ name: 'login' }">Sign in</RouterLink>
    </p>
  </AuthLayout>
</template>
