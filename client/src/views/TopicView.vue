<template>
  <div class="page">
    <nav class="crumbs" aria-label="面包屑">
      <RouterLink to="/categories">板块</RouterLink>
      <AppIcon name="chevron-right" :size="12" />
      <span class="tone-2"><span class="dot" aria-hidden="true"></span>{{ localTopic?.categoryName || '主题讨论' }}</span>
    </nav>
    <h1 class="topic-title">{{ topicTitle }}</h1>
    <p class="meta"><span class="tabular">已加载 {{ floors.length }} 条楼层</span></p>

    <div class="topic-bar">
      <button class="button button-secondary" type="button" :aria-pressed="showOnlyAuthor" @click="toggleAuthor">{{ showOnlyAuthor ? '查看全部' : '只看楼主' }}</button>
      <div class="jump">
        <label for="floor-jump">跳至</label>
        <input id="floor-jump" v-model.number="jumpFloor" class="input tabular" type="number" min="1" @keydown.enter="jumpToFloor" />
        <span>楼</span>
        <button class="text-button" type="button" @click="jumpToFloor">前往</button>
      </div>
    </div>

    <p v-if="loading" class="empty" role="status">正在加载楼层…</p>
    <p v-else-if="loadError" class="form-error" role="alert">{{ loadError }}</p>
    <p v-else-if="!visibleFloors.length" class="empty">暂时没有可显示的楼层</p>
    <article v-for="floor in visibleFloors" :id="`floor-${floor.floorNo}`" :key="floor.id" class="floor" :class="{ 'is-author': showOnlyAuthor }">
      <span class="avatar" aria-hidden="true">{{ String(floor.userId).slice(-1) }}</span>
      <div>
        <div class="byline">
          <strong>用户 {{ floor.userId }}</strong>
          <span v-if="showOnlyAuthor" class="badge">楼主</span>
          <a class="floor-number tabular" :href="`#floor-${floor.floorNo}`">#{{ floor.floorNo }}</a>
        </div>
        <p class="floor-content">{{ floor.content }}</p>
        <button class="text-button" type="button" @click="quote(floor)">引用回复</button>
      </div>
    </article>

    <nav class="pagination" aria-label="楼层分页">
      <button class="icon-button" type="button" aria-label="上一页" :disabled="page === 1" @click="page--"><AppIcon name="chevron-left" /></button>
      <button v-for="pageNumber in loadedPages" :key="pageNumber" class="page-number tabular" type="button" :aria-current="page === pageNumber ? 'page' : undefined" @click="page = pageNumber">{{ pageNumber }}</button>
      <button v-if="hasNext" class="icon-button" type="button" aria-label="加载下一页" :disabled="loading" @click="nextPage"><AppIcon name="chevron-right" /></button>
    </nav>

    <section class="reply-box" aria-label="回复">
      <div v-if="quoted" class="quote-chip">
        <span>回复用户 {{ quoted.userId }} · #{{ quoted.floorNo }}</span>
        <button class="icon-button" type="button" aria-label="取消引用" @click="quoted = null"><AppIcon name="x" :size="12" /></button>
      </div>
      <textarea v-model="replyText" aria-label="回复内容" placeholder="写下你的回复…" rows="4"></textarea>
      <p v-if="replyError" class="form-error" role="alert">{{ replyError }}</p>
      <div class="reply-actions"><button class="button button-primary" type="button" :disabled="replying" @click="submitReply">{{ replying ? '发送中…' : '回复' }}</button></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useRoute } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'
import { formatApiError, getToken, request, type ReplyPage, type ReplyView } from '../api/client'

const route = useRoute()
const topicId = computed(() => String(route.params.topicId))
const localTopic = computed(() => {
  try { return JSON.parse(localStorage.getItem(`agora-topic-${topicId.value}`) || 'null') as { title?: string; categoryName?: string } | null }
  catch { return null }
})
const topicTitle = computed(() => localTopic.value?.title || `主题 ${topicId.value}`)
const page = ref(1)
const jumpFloor = ref(1)
const showOnlyAuthor = ref(false)
const quoted = ref<ReplyView | null>(null)
const replyText = ref('')
const replyError = ref('')
const floors = ref<ReplyView[]>([])
const loading = ref(false)
const replying = ref(false)
const loadError = ref('')
const hasNext = ref(false)
const nextCursor = ref<number | null>(null)
const pages = ref<ReplyView[][]>([])
const loadedPages = computed(() => pages.value.map((_, index) => index + 1))
const visibleFloors = computed(() => pages.value[page.value - 1] || [])

async function fetchPage(afterFloor?: number) {
  if (!/^\\d+$/.test(topicId.value)) { loadError.value = '主题 ID 格式无效。'; return }
  loading.value = true; loadError.value = ''
  try {
    const params = new URLSearchParams()
    if (afterFloor !== undefined) params.set('afterFloor', String(afterFloor))
    if (showOnlyAuthor.value) params.set('onlyAuthor', 'true')
    const result = await request<ReplyPage>(`/api/forum/topics/${topicId.value}/replies?${params}`)
    floors.value = [...floors.value, ...result.replies]
    pages.value[page.value - 1] = result.replies
    pages.value = [...pages.value]
    hasNext.value = result.hasNext
    nextCursor.value = result.nextCursor
  } catch (cause) {
    loadError.value = formatApiError(cause)
  } finally { loading.value = false }
}

onMounted(() => fetchPage())
watch(topicId, () => { floors.value = []; pages.value = []; page.value = 1; showOnlyAuthor.value = false; fetchPage() })
async function toggleAuthor() { showOnlyAuthor.value = !showOnlyAuthor.value; page.value = 1; floors.value = []; pages.value = []; await fetchPage() }
async function nextPage() {
  if (!hasNext.value || nextCursor.value === null || loading.value) return
  page.value += 1
  await fetchPage(nextCursor.value)
}
async function jumpToFloor() {
  if (!Number.isInteger(jumpFloor.value) || jumpFloor.value < 1) { replyError.value = '请输入有效楼层号。'; return }
  const cachedIndex = floors.value.findIndex(floor => floor.floorNo >= jumpFloor.value)
  if (cachedIndex >= 0) { page.value = pages.value.findIndex(items => items.some(floor => floor.floorNo >= jumpFloor.value)) + 1; return }
  page.value = 1
  floors.value = []; pages.value = []
  await fetchPage(jumpFloor.value - 1)
  if (!visibleFloors.value.length) { page.value = Math.max(1, pages.value.length); replyError.value = '没有找到该楼层。' }
}
function quote(floor: ReplyView) { quoted.value = floor; replyText.value = `> #${floor.floorNo} ${floor.content}\n\n`; document.querySelector('.reply-box textarea')?.scrollIntoView({ behavior: 'smooth', block: 'center' }) }
async function submitReply() {
  const content = replyText.value.trim()
  if (!content) { replyError.value = '请先填写回复内容。'; return }
  if (!getToken()) { replyError.value = '请先登录后再回复。'; return }
  replying.value = true; replyError.value = ''
  try {
    await request<{ replyId: number; floorNo: number }>(`/api/forum/topics/${topicId.value}/replies`, { method: 'POST', auth: true, body: { content } })
    showOnlyAuthor.value = false; page.value = 1; floors.value = []; pages.value = []; replyText.value = ''; quoted.value = null
    await fetchPage()
  } catch (cause) { replyError.value = formatApiError(cause) }
  finally { replying.value = false }
}
</script>
