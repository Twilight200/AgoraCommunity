<template>
  <div class="page">
    <form class="compose" @submit.prevent="submit">
      <div class="compose-head">
        <label for="category">发布到</label>
        <select id="category" v-model="category" class="select">
        <option :value="2">开发交流</option><option :value="1">产品设计</option><option :value="3">读书与思考</option><option :value="4">团队协作</option>
        </select>
      </div>

      <h1 class="sr-only">发布主题</h1>
      <input id="title" v-model="title" class="title-input" maxlength="100" required aria-label="标题" aria-describedby="title-count" placeholder="标题" />
      <span id="title-count" class="field-hint tabular" aria-live="polite">{{ title.length }} / 100</span>

      <div class="editor">
        <div class="editor-bar">
          <span id="body-hint" class="field-hint">支持 Markdown，最多 10,000 字</span>
          <div class="segmented" role="group" aria-label="编辑模式">
            <button type="button" :aria-pressed="!preview" @click="preview = false">编辑</button>
            <button type="button" :aria-pressed="preview" @click="preview = true">预览</button>
          </div>
        </div>
        <textarea v-if="!preview" id="body" v-model="body" rows="12" maxlength="10000" aria-label="正文" aria-describedby="body-hint" placeholder="写点什么…"></textarea>
        <div v-else class="preview" aria-label="正文预览" v-html="previewHtml"></div>
      </div>

      <div class="compose-foot">
        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <RouterLink class="button button-ghost" to="/categories">取消</RouterLink>
        <button class="button button-primary" type="submit">发布</button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { formatApiError, getToken, request } from '../api/client'

const category = ref(2)
const title = ref('')
const body = ref('')
const preview = ref(false)
const error = ref('')
const router = useRouter()
const categoryNames: Record<number, string> = { 1: '产品设计', 2: '开发交流', 3: '读书与思考', 4: '团队协作' }
const previewHtml = computed(() => body.value.trim().split('\n').map(line => line.startsWith('## ') ? `<h2>${escapeHtml(line.slice(3))}</h2>` : `<p>${escapeHtml(line) || '&nbsp;'}</p>`).join(''))
function escapeHtml(text: string) { return text.replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char] || char) }
async function submit() {
  if (!title.value.trim() || !body.value.trim()) { error.value = '请填写主题标题和正文。'; return }
  try {
    if (!getToken()) { error.value = '请先登录后再发布主题。'; return }
    const result = await request<{ topicId: number }>('/api/forum/topics', {
      method: 'POST', auth: true,
      body: { categoryId: category.value, title: title.value.trim(), content: body.value.trim() },
    })
    try { localStorage.setItem(`agora-topic-${result.topicId}`, JSON.stringify({ title: title.value.trim(), categoryName: categoryNames[category.value] })) } catch { /* The created topic remains available from the API. */ }
    await router.push(`/topics/${result.topicId}`)
  } catch (cause) {
    error.value = formatApiError(cause)
  }
}
</script>
