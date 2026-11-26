import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { title: '首页' },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/AuthView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/AuthView.vue'),
      meta: { title: '注册' },
    },
    {
      path: '/categories',
      name: 'categories',
      component: () => import('../views/CategoryListView.vue'),
      meta: { title: '板块' },
    },
    {
      path: '/topics/:topicId',
      name: 'topic',
      component: () => import('../views/TopicView.vue'),
      meta: { title: '主题' },
    },
    {
      path: '/compose',
      name: 'compose',
      component: () => import('../views/ComposeView.vue'),
      meta: { title: '发布主题' },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '社区')} · Agora`
})

export default router
