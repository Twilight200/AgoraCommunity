<template>
  <div class="app-shell">
    <aside class="sidebar" aria-label="主导航">
      <RouterLink class="brand" to="/" aria-label="Agora 首页">
        <span class="brand-mark">A</span>
        <span>agora</span>
      </RouterLink>

      <div class="workspace-label">浏览</div>
      <nav class="nav-list">
        <RouterLink class="nav-item" to="/" :class="{ active: route.name === 'home' }">
          <span class="nav-icon" aria-hidden="true">⌂</span>
          <span>首页</span>
        </RouterLink>
        <a class="nav-item nav-item-muted" href="#topics"><span class="nav-icon" aria-hidden="true">#</span><span>节点</span></a>
        <a class="nav-item nav-item-muted" href="#saved"><span class="nav-icon" aria-hidden="true">☆</span><span>已收藏</span></a>
      </nav>

      <div class="workspace-label workspace-label-spaced">你的空间</div>
      <nav class="nav-list">
        <a class="nav-item nav-item-muted" href="#notifications"><span class="nav-icon" aria-hidden="true">◌</span><span>通知</span><span class="nav-count">0</span></a>
      </nav>

      <div class="sidebar-bottom">
        <div class="connection-status"><span class="status-dot"></span>服务连接正常</div>
        <button class="profile-button" type="button" aria-label="当前用户">
          <span class="avatar">访</span>
          <span class="profile-copy"><strong>访客</strong><small>未登录</small></span>
          <span class="profile-menu" aria-hidden="true">···</span>
        </button>
      </div>
    </aside>

    <main class="main-area">
      <header class="topbar">
        <div class="breadcrumbs"><span>Agora</span><span class="crumb-divider">/</span><strong>{{ route.meta.title }}</strong></div>
        <div class="topbar-actions"><span class="environment-tag">开发环境</span><button class="icon-button" type="button" aria-label="通知" title="通知">♧</button></div>
      </header>
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRoute, RouterLink, RouterView } from 'vue-router'

const route = useRoute()
</script>
