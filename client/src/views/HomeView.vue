<template>
  <div class="page page-wide">
    <header class="page-header">
      <h1 class="page-title">首页</h1>
      <RouterLink class="button button-primary" to="/compose"><AppIcon name="plus" :size="14" />发布主题</RouterLink>
    </header>

    <div class="home-layout">
      <section aria-labelledby="feed-title">
        <h2 id="feed-title" class="sr-only">主题流</h2>
        <div class="tabs" role="tablist" aria-label="主题流筛选">
          <button class="tab" type="button" role="tab" aria-selected="true">推荐</button>
          <button class="tab" type="button" role="tab" aria-selected="false">最新</button>
          <button class="tab" type="button" role="tab" aria-selected="false">关注</button>
        </div>

        <ul>
          <li v-for="topic in topics" :key="topic.title">
            <article class="feed-item">
              <div class="feed-main">
                <RouterLink v-if="topic.to" class="feed-title" :to="topic.to">{{ topic.title }}</RouterLink>
                <a v-else class="feed-title" :href="topic.href">{{ topic.title }}</a>
                <p class="meta">
                  <span class="feed-category" :class="topic.tone"><span class="dot" aria-hidden="true"></span>{{ topic.category }}</span>
                  <span class="meta-sep" aria-hidden="true">·</span><span>{{ topic.author }}</span>
                  <span class="meta-sep" aria-hidden="true">·</span><span>{{ topic.time }}</span>
                </p>
              </div>
              <div class="feed-stats tabular">
                <span class="stat" :aria-label="`点赞数 ${topic.likes}`"><AppIcon name="heart" :size="14" />{{ topic.likes }}</span>
                <span class="stat" :aria-label="`回复数 ${topic.replies}`"><AppIcon name="message" :size="14" />{{ topic.replies }}</span>
              </div>
            </article>
          </li>
        </ul>
      </section>

      <aside aria-labelledby="boards-title">
        <div class="side-title"><h2 id="boards-title">热门板块</h2><RouterLink to="/categories">全部</RouterLink></div>
        <ul class="board-list">
          <li v-for="board in boards" :key="board.name">
            <a class="board-link" href="#topics" :class="board.tone">
              <span class="symbol" aria-hidden="true">{{ board.symbol }}</span>
              <span>{{ board.name }}</span>
              <span class="count tabular" :aria-label="`今日 ${board.today} 个主题`">{{ board.today }}</span>
            </a>
          </li>
        </ul>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'

const topics = [
  { title: '如何设计一个不打扰用户的通知系统？', to: '/topics/101', category: '产品设计', tone: 'tone-1', author: '林默', time: '32 分钟前', likes: 24, replies: 18 },
  { title: '从零开始搭建本地开发环境，有哪些值得保留的习惯？', to: '/topics/102', category: '开发', tone: 'tone-2', author: '许舟', time: '1 小时前', likes: 16, replies: 9 },
  { title: '你会怎样整理一个长期维护的 Java 项目？', href: '#topic-3', category: '工程实践', tone: 'tone-3', author: '周然', time: '2 小时前', likes: 11, replies: 27 },
  { title: '把复杂问题写成一页纸之后，团队真的会更快吗？', href: '#topic-4', category: '团队协作', tone: 'tone-4', author: '顾言', time: '3 小时前', likes: 8, replies: 12 },
]

const boards = [
  { name: '产品设计', symbol: '产', tone: 'tone-1', today: 24 },
  { name: '开发交流', symbol: '开', tone: 'tone-2', today: 18 },
  { name: '读书与思考', symbol: '书', tone: 'tone-3', today: 12 },
]
</script>
