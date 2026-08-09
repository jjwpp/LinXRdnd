<script setup>
import { ref, watch, nextTick, computed } from "vue";

const props = defineProps({
  storyLog: { type: Array, default: () => [] },
  streamingText: { type: String, default: "" },
  loading: { type: Boolean, default: false },
  // 是否默认展开
  defaultExpanded: { type: Boolean, default: true },
});

const emit = defineEmits(["scroll-to-bottom"]);

const panelRef = ref(null);
const expanded = ref(props.defaultExpanded);
const autoScroll = ref(true);

// 是否有新内容未读
const hasUnread = ref(false);
const lastReadCount = ref(0);

const totalEntries = computed(() => {
  let count = props.storyLog.length;
  if (props.streamingText) count++;
  return count;
});

// 监听故事日志变化，自动滚动到底部
watch(
  () => [props.storyLog.length, props.streamingText],
  () => {
    if (expanded.value && autoScroll.value) {
      nextTick(() => scrollToBottom());
    } else if (!expanded.value) {
      hasUnread.value = true;
    }
  },
  { flush: "post" }
);

function scrollToBottom() {
  if (panelRef.value) {
    panelRef.value.scrollTop = panelRef.value.scrollHeight;
  }
}

function togglePanel() {
  expanded.value = !expanded.value;
  if (expanded.value) {
    hasUnread.value = false;
    lastReadCount.value = totalEntries.value;
    nextTick(() => scrollToBottom());
  }
}

function handleScroll() {
  if (!panelRef.value) return;
  const el = panelRef.value;
  const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 40;
  autoScroll.value = atBottom;
  if (atBottom) {
    hasUnread.value = false;
    lastReadCount.value = totalEntries.value;
  }
}

defineExpose({ scrollToBottom, expanded });
</script>

<template>
  <div class="narration-panel-wrap" :class="{ collapsed: !expanded }">
    <!-- 折叠时的浮动按钮 -->
    <transition name="narration-fab">
      <button v-if="!expanded" class="narration-fab" @click="togglePanel">
        <span class="fab-icon">📜</span>
        <span class="fab-label">旁白</span>
        <span v-if="hasUnread" class="fab-unread-dot"></span>
      </button>
    </transition>

    <!-- 展开时的卷轴面板 -->
    <transition name="narration-expand">
      <div v-if="expanded" class="narration-scroll">
        <!-- 顶部装饰条 -->
        <div class="narration-header">
          <div class="narration-header-deco-left"></div>
          <div class="narration-title-wrap">
            <span class="narration-rune">❧</span>
            <span class="narration-title">冒险纪事</span>
            <span class="narration-rune">❧</span>
          </div>
          <button class="narration-collapse-btn" @click="togglePanel" title="收起旁白">
            <span class="collapse-icon">▼</span>
          </button>
          <div class="narration-header-deco-right"></div>
        </div>

        <!-- 可滚动的故事内容 -->
        <div
          ref="panelRef"
          class="narration-content"
          @scroll="handleScroll"
        >
          <div
            v-for="(para, i) in storyLog"
            :key="i"
            class="narration-para"
            :class="{
              'player-action': para.startsWith('【你】'),
              'sys-msg': para.startsWith('【系统】'),
            }"
          >{{ para }}</div>

          <!-- 流式输出文本 -->
          <div v-if="streamingText" class="narration-para streaming">
            {{ streamingText.split("---")[0].trim() }}<span class="cursor">▎</span>
          </div>

          <!-- 加载提示 -->
          <div v-if="loading && !streamingText" class="narration-loading">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            <span class="loading-text">地下城主正在编织故事...</span>
          </div>
        </div>

        <!-- 底部装饰 -->
        <div class="narration-footer">
          <span class="narration-footer-rune">✦</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.narration-panel-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
}

/* ── 折叠时的浮动按钮 ── */
.narration-fab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 1px solid var(--gold-dim);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(30, 22, 18, 0.92), rgba(17, 13, 10, 0.88));
  backdrop-filter: blur(10px);
  color: var(--gold-bright);
  cursor: pointer;
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
  transition: var(--transition-base);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(201, 162, 39, 0.1);
  position: relative;
}
.narration-fab:hover {
  border-color: var(--gold);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.5), 0 0 16px var(--gold-glow);
  transform: translateY(-2px);
}
.fab-icon { font-size: 18px; }
.fab-unread-dot {
  position: absolute;
  top: 4px;
  right: 6px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--crimson-bright, #9b2d2d);
  box-shadow: 0 0 8px rgba(155, 45, 45, 0.6);
  animation: pulseDot 1.5s ease-in-out infinite;
}
@keyframes pulseDot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.3); }
}

/* ── 展开时的卷轴面板 ── */
.narration-scroll {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--line-gold);
  border-radius: var(--radius-md);
  background:
    linear-gradient(135deg, rgba(30, 22, 18, 0.88), rgba(23, 16, 12, 0.92));
  backdrop-filter: blur(8px);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.5),
    inset 0 0 40px rgba(0, 0, 0, 0.3),
    inset 0 0 0 1px rgba(201, 162, 39, 0.04);
  overflow: hidden;
  position: relative;
}

/* 顶部装饰条 */
.narration-header {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid var(--line-gold);
  background: linear-gradient(180deg, rgba(42, 31, 23, 0.6), transparent);
  position: relative;
  flex-shrink: 0;
}
.narration-header-deco-left,
.narration-header-deco-right {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-gold), transparent);
}
.narration-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
}
.narration-rune {
  font-size: 11px;
  color: var(--gold-dim);
  filter: drop-shadow(0 0 4px var(--gold-soft));
}
.narration-title {
  font-family: var(--font-heading);
  font-size: 13px;
  font-weight: 700;
  color: var(--gold);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  text-shadow: 0 0 8px var(--gold-glow);
}
.narration-collapse-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--gold-dim);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: var(--transition-base);
  font-size: 12px;
}
.narration-collapse-btn:hover {
  color: var(--gold-bright);
  background: var(--gold-soft);
}
.collapse-icon {
  display: inline-block;
  transition: transform 0.3s ease;
}

/* 可滚动内容区 */
.narration-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 18px;
  scroll-behavior: smooth;
  position: relative;
  max-height: 320px;
  min-height: 120px;
}
.narration-content::-webkit-scrollbar {
  width: 6px;
}
.narration-content::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}
.narration-content::-webkit-scrollbar-thumb {
  background: var(--gold-dim);
  border-radius: 3px;
}
.narration-content::-webkit-scrollbar-thumb:hover {
  background: var(--gold);
}

/* 段落样式 */
.narration-para {
  font-family: var(--font-body);
  font-size: 14px;
  line-height: 1.85;
  color: var(--ink);
  margin-bottom: 14px;
  white-space: pre-wrap;
  animation: narrationFadeIn 0.4s ease;
  padding-left: 12px;
  border-left: 2px solid transparent;
  transition: border-color 0.3s ease;
}
@keyframes narrationFadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
.narration-para.player-action {
  color: var(--gold-bright);
  font-weight: 500;
  font-style: italic;
  font-family: var(--font-heading);
  padding: 8px 14px;
  background: linear-gradient(90deg, var(--gold-soft), transparent);
  border-left: 3px solid var(--gold);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  text-shadow: 0 0 10px rgba(201, 162, 39, 0.1);
}
.narration-para.sys-msg {
  color: var(--arcane-glow);
  font-size: 13px;
  font-style: italic;
  padding: 6px 14px;
  background: var(--arcane-soft);
  border-left: 3px solid var(--arcane);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}
.narration-para.streaming {
  color: var(--ink-soft);
  border-left: 3px solid var(--arcane-glow);
}
.cursor {
  display: inline-block;
  color: var(--arcane-glow);
  animation: blink 0.8s steps(2) infinite;
  font-weight: 100;
}
@keyframes blink { to { opacity: 0; } }

/* 加载提示 */
.narration-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 13px;
  padding: 8px 0;
  font-style: italic;
  font-family: var(--font-heading);
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--arcane-glow);
  box-shadow: 0 0 6px var(--arcane-glow);
  animation: dotBounce 1s infinite alternate;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes dotBounce { to { opacity: 0.3; transform: translateY(-4px); } }

/* 底部装饰 */
.narration-footer {
  display: flex;
  justify-content: center;
  padding: 6px;
  border-top: 1px solid var(--line);
  background: linear-gradient(0deg, rgba(42, 31, 23, 0.4), transparent);
  flex-shrink: 0;
}
.narration-footer-rune {
  font-size: 10px;
  color: var(--gold-dim);
  filter: drop-shadow(0 0 4px var(--gold-soft));
}

/* ── 过渡动画 ── */
.narration-fab-enter-active,
.narration-fab-leave-active {
  transition: all 0.3s ease;
}
.narration-fab-enter-from,
.narration-fab-leave-to {
  opacity: 0;
  transform: scale(0.8) translateY(10px);
}
.narration-expand-enter-active,
.narration-expand-leave-active {
  transition: all 0.35s cubic-bezier(.4, 0, .2, 1);
}
.narration-expand-enter-from,
.narration-expand-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* ── 响应式 ── */
@media (max-width: 600px) {
  .narration-content {
    max-height: 240px;
    padding: 12px 14px;
  }
  .narration-para {
    font-size: 13px;
    line-height: 1.75;
  }
}
</style>
