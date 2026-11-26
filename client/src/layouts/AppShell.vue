<template>
  <div class="app-shell">
    <aside class="sidebar" aria-label="主导航">
      <RouterLink class="brand" to="/" aria-label="Agora 首页">
        <span class="brand-mark" aria-hidden="true">A</span>
        <span>Agora</span>
      </RouterLink>

      <nav class="nav-list">
        <RouterLink class="nav-item" to="/" :class="{ active: route.name === 'home' }" aria-label="首页">
          <AppIcon name="home" /><span>首页</span>
        </RouterLink>
        <RouterLink class="nav-item" to="/categories" :class="{ active: route.name === 'categories' || route.name === 'topic' || route.name === 'compose' }" aria-label="板块">
          <AppIcon name="hash" /><span>板块</span>
        </RouterLink>
        <a class="nav-item" href="#saved" aria-label="已收藏"><AppIcon name="star" /><span>已收藏</span></a>
        <a class="nav-item" href="#notifications" aria-label="通知"><AppIcon name="bell" /><span>通知</span></a>
      </nav>

      <div class="sidebar-bottom">
        <RouterLink v-if="!sessionUsername" class="profile-button" to="/login" aria-label="登录或注册">
          <span class="avatar" aria-hidden="true">访</span><span>登录</span>
        </RouterLink>
        <button v-else class="profile-button" type="button" aria-label="退出登录" @click="signOut">
          <span class="avatar" aria-hidden="true">{{ sessionUsername.slice(0, 1) }}</span><span>{{ sessionUsername }}</span><span class="signout-label">退出</span>
        </button>
      </div>
    </aside>

    <main class="main-area">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRoute, RouterLink, RouterView } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import { logout, sessionUsername } from '../api/client'

const route = useRoute()
async function signOut() { try { await logout() } catch { /* Local session is cleared even when the API is unavailable. */ } }
</script>
