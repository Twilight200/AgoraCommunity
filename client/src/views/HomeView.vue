<template>
  <div class="page-content">
    <section class="page-heading">
      <div><p class="eyebrow">社区首页</p><h1>今天，社区在讨论什么</h1></div>
      <button class="compose-button" type="button">＋ 发布主题</button>
    </section>

    <div class="feed-layout">
      <section class="feed-column" aria-labelledby="feed-title">
        <div class="feed-toolbar"><div class="feed-tabs" role="tablist" aria-label="主题流筛选"><button class="feed-tab active" type="button" role="tab" aria-selected="true">推荐</button><button class="feed-tab" type="button" role="tab" aria-selected="false">最新</button><button class="feed-tab" type="button" role="tab" aria-selected="false">关注</button></div><span class="demo-note">演示数据</span></div>
        <h2 id="feed-title" class="sr-only">主题流</h2>
        <div class="topic-list">
          <article class="topic-row"><div class="topic-vote" aria-label="点赞数 24"><strong>24</strong><span>赞</span></div><div class="topic-main"><a href="#topic-1" class="topic-title">如何设计一个不打扰用户的通知系统？</a><p class="topic-meta"><span class="topic-tag">产品设计</span><span>林默</span><span>·</span><span>32 分钟前</span></p></div><div class="topic-replies">18<br><span>回复</span></div></article>
          <article class="topic-row"><div class="topic-vote" aria-label="点赞数 16"><strong>16</strong><span>赞</span></div><div class="topic-main"><a href="#topic-2" class="topic-title">从零开始搭建本地开发环境，有哪些值得保留的习惯？</a><p class="topic-meta"><span class="topic-tag topic-tag-blue">开发</span><span>许舟</span><span>·</span><span>1 小时前</span></p></div><div class="topic-replies">9<br><span>回复</span></div></article>
          <article class="topic-row"><div class="topic-vote" aria-label="点赞数 11"><strong>11</strong><span>赞</span></div><div class="topic-main"><a href="#topic-3" class="topic-title">你会怎样整理一个长期维护的 Java 项目？</a><p class="topic-meta"><span class="topic-tag topic-tag-amber">工程实践</span><span>周然</span><span>·</span><span>2 小时前</span></p></div><div class="topic-replies">27<br><span>回复</span></div></article>
          <article class="topic-row"><div class="topic-vote" aria-label="点赞数 8"><strong>8</strong><span>赞</span></div><div class="topic-main"><a href="#topic-4" class="topic-title">把复杂问题写成一页纸之后，团队真的会更快吗？</a><p class="topic-meta"><span class="topic-tag topic-tag-green">团队协作</span><span>顾言</span><span>·</span><span>3 小时前</span></p></div><div class="topic-replies">12<br><span>回复</span></div></article>
        </div>
      </section>

      <aside class="side-column" aria-label="社区信息">
        <section class="side-section"><div class="side-heading"><h2>热门节点</h2><a href="#topics">查看全部</a></div><ul class="node-list"><li><span class="node-icon node-icon-red">产</span><div><strong>产品设计</strong><small>今日 24 个主题</small></div></li><li><span class="node-icon node-icon-blue">开</span><div><strong>开发交流</strong><small>今日 18 个主题</small></div></li><li><span class="node-icon node-icon-amber">书</span><div><strong>读书与思考</strong><small>今日 12 个主题</small></div></li></ul></section>
        <section class="side-section side-note"><h2>社区状态</h2><p><span class="status-dot"></span>服务运行正常</p><span>接口接入后，这里会显示实时主题与未读通知。</span></section>
      </aside>
    </div>
  </div>
</template>
