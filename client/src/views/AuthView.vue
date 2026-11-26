<template>
  <div class="auth">
    <div class="auth-panel">
      <RouterLink class="brand" to="/" aria-label="Agora 首页"><span class="brand-mark" aria-hidden="true">A</span></RouterLink>
      <h1>{{ isRegister ? '创建账号' : '登录 Agora' }}</h1>
      <p v-if="route.query.registered" class="field-hint" role="status">账号已创建，请登录。</p>
      <form class="auth-form" @submit.prevent="submit">
        <div class="field">
          <label class="field-label" for="username">用户名</label>
          <input id="username" v-model="username" class="input" autocomplete="username" required :minlength="isRegister ? 6 : undefined" :maxlength="isRegister ? 18 : 64" :aria-describedby="isRegister ? 'username-hint' : undefined" />
          <span v-if="isRegister" id="username-hint" class="field-hint">6–18 位字母、数字或下划线</span>
        </div>
        <div class="field">
          <label class="field-label" for="password">密码</label>
          <input id="password" v-model="password" class="input" type="password" :autocomplete="isRegister ? 'new-password' : 'current-password'" required :minlength="isRegister ? 12 : 1" :aria-describedby="isRegister ? 'password-hint' : undefined" />
          <span v-if="isRegister" id="password-hint" class="field-hint">至少 12 位</span>
        </div>
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <button class="button button-primary button-block" type="submit">{{ isRegister ? '创建账号' : '登录' }}</button>
      </form>
      <p class="auth-switch">{{ isRegister ? '已有账号？' : '没有账号？' }} <RouterLink :to="isRegister ? '/login' : '/register'">{{ isRegister ? '登录' : '注册' }}</RouterLink></p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { formatApiError, login, register } from '../api/client'

const route = useRoute()
const isRegister = computed(() => route.path === '/register')
const username = ref('')
const password = ref('')
const error = ref('')
const router = useRouter()

async function submit() {
  error.value = ''
  try {
    if (isRegister.value) {
      if (!/^[A-Za-z0-9_]{6,18}$/.test(username.value)) { error.value = '用户名须为 6–18 位字母、数字或下划线。'; return }
      await register(username.value, password.value)
      await router.push('/login?registered=1')
      return
    }
    await login(username.value, password.value)
    await router.push('/')
  } catch (cause) {
    error.value = formatApiError(cause)
  }
}
</script>
