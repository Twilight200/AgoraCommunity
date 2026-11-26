<template>
  <div class="page page-wide">
    <header class="page-header">
      <h1 class="page-title">板块</h1>
      <RouterLink class="button button-primary" to="/compose"><AppIcon name="plus" :size="14" />发布主题</RouterLink>
    </header>

    <div class="toolbar">
      <label class="search">
        <AppIcon name="search" :size="14" />
        <input v-model="query" class="input" type="search" aria-label="搜索板块" placeholder="搜索" />
      </label>
      <div class="segmented" role="group" aria-label="排序">
        <button type="button" :aria-pressed="sort === 'active'" @click="sort = 'active'">最近活跃</button>
        <button type="button" :aria-pressed="sort === 'name'" @click="sort = 'name'">名称</button>
      </div>
    </div>

    <ul v-if="visibleCategories.length">
      <li v-for="category in visibleCategories" :key="category.id">
        <RouterLink class="category-row" :class="category.color" :to="`/topics/${category.topicId}`">
          <span class="symbol" aria-hidden="true">{{ category.symbol }}</span>
          <span>
            <span class="category-name">{{ category.name }}</span>
            <span class="category-desc">{{ category.description }}</span>
          </span>
          <span class="category-count tabular">{{ category.topicCount }}<span class="sr-only"> 个主题</span></span>
          <span class="category-latest"><span>{{ category.latest }}</span>{{ category.time }}</span>
        </RouterLink>
      </li>
    </ul>
    <p v-else class="empty">没有匹配“{{ query.trim() }}”的板块</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'

const query = ref('')
const sort = ref<'active' | 'name'>('active')
const categories = [
  { id: 1, topicId: 101, name: '产品设计', description: '产品体验、交互与设计方法', symbol: '产', color: 'tone-1', topicCount: 128, latest: '如何设计一个不打扰用户的通知系统？', time: '32 分钟前' },
  { id: 2, topicId: 102, name: '开发交流', description: '工程实践、工具与技术讨论', symbol: '开', color: 'tone-2', topicCount: 96, latest: '从零开始搭建本地开发环境', time: '1 小时前' },
  { id: 3, topicId: 103, name: '读书与思考', description: '记录阅读，交换值得继续想的问题', symbol: '读', color: 'tone-3', topicCount: 64, latest: '最近读到的一段话', time: '2 小时前' },
  { id: 4, topicId: 104, name: '团队协作', description: '流程、沟通与共同完成的工作', symbol: '协', color: 'tone-4', topicCount: 42, latest: '把复杂问题写成一页纸之后', time: '3 小时前' },
]
const visibleCategories = computed(() => {
  const result = categories.filter(item => `${item.name}${item.description}`.includes(query.value.trim()))
  return sort.value === 'name' ? [...result].sort((a, b) => a.name.localeCompare(b.name, 'zh-CN')) : result
})
</script>
