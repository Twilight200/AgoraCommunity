<template>
  <div class="not-found">
    <strong aria-hidden="true">404</strong>
    <h1>页面不存在</h1>
    <RouterLink class="button button-secondary" to="/">返回首页</RouterLink>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>
