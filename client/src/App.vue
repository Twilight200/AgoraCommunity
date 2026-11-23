<template>
  <n-config-provider>
    <n-message-provider>
      <AppShell />
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import AppShell from './layouts/AppShell.vue'
</script>
