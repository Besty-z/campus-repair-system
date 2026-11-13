<template>
  <div class="ai-chat-page">
    <!-- 左侧会话栏 -->
    <aside class="session-panel">
      <button class="new-session-btn" @click="handleNewSession">
        <el-icon><Plus /></el-icon>
        <span>新建会话</span>
      </button>

      <div class="session-list">
        <div
          v-for="s in sessions"
          :key="s.id"
          class="session-item"
          :class="{ active: currentSessionId === s.id }"
          @click="handleSelectSession(s.id)"
        >
          <el-icon class="session-icon"><ChatDotRound /></el-icon>
          <span class="session-title">{{ s.title }}</span>
          <el-icon
            class="session-delete"
            @click.stop="handleDeleteSession(s.id)"
          >
            <Delete />
          </el-icon>
        </div>
        <div v-if="!loadingSessions && sessions.length === 0" class="session-empty">
          暂无会话，点击上方新建
        </div>
      </div>
    </aside>

    <!-- 右侧对话区 -->
    <section class="chat-panel">
      <!-- 顶部栏 -->
      <div class="chat-header">
        <div class="chat-title">
          <el-icon class="chat-title-icon"><MagicStick /></el-icon>
          <span>校园报修智能助手</span>
        </div>
        <div class="chat-header-actions">
          <span class="key-badge" :class="hasApiKey ? 'key-on' : 'key-off'">
            {{ hasApiKey ? '已配置 Key' : '未配置 Key' }}
          </span>
          <el-button text @click="openKeyDialog">
            <el-icon><Setting /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="message-list" ref="messageListRef">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-row"
          :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'"
        >
          <div class="avatar" :class="msg.role === 'user' ? 'avatar-user' : 'avatar-ai'">
            {{ msg.role === 'user' ? '我' : 'AI' }}
          </div>
          <div class="bubble">
            <pre class="bubble-content">{{ msg.content }}</pre>
          </div>
        </div>

        <!-- 思考中占位 -->
        <div v-if="thinking" class="message-row msg-ai">
          <div class="avatar avatar-ai">AI</div>
          <div class="bubble bubble-thinking">
            <span class="dot" />
            <span class="dot" />
            <span class="dot" />
          </div>
        </div>

        <div v-if="messages.length === 0 && !thinking" class="chat-empty">
          <el-icon class="empty-icon"><MagicStick /></el-icon>
          <p>你好，我是校园报修智能助手</p>
          <p class="empty-sub">告诉我你的故障，我来帮你判断分类、给出排查建议</p>
          <div class="quick-prompts">
            <span
              v-for="p in quickPrompts"
              :key="p"
              class="quick-prompt"
              @click="handleQuickPrompt(p)"
            >{{ p }}</span>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="描述你遇到的故障，例如：宿舍水龙头漏水"
          resize="none"
          @keydown.enter.exact.prevent="handleSend"
        />
        <button
          class="send-btn"
          :disabled="!inputText.trim() || thinking"
          @click="handleSend"
        >
          <el-icon><Promotion /></el-icon>
          发送
        </button>
      </div>
    </section>

    <!-- API Key 配置弹窗 -->
    <el-dialog v-model="keyDialogVisible" title="配置 DeepSeek API Key" width="460px">
      <p class="key-tip">
        输入你自己的 DeepSeek API Key，仅保存在你当前浏览器本地（localStorage），
        发送消息时随请求透传给后端调用 DeepSeek，不会上传到服务器数据库。
        若不配置，将使用内置的模拟报修建议。
      </p>
      <el-input
        v-model="keyInput"
        type="password"
        show-password
        placeholder="sk-..."
        clearable
        @keyup.enter="saveKey"
      />
      <template #footer>
        <el-button @click="clearKey" :disabled="!hasApiKey">清除 Key</el-button>
        <el-button @click="keyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveKey">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  Delete,
  Plus,
  Promotion,
  MagicStick,
  Setting
} from '@element-plus/icons-vue'
import {
  createSession,
  deleteSession,
  listMessages,
  listSessions,
  sendMessage
} from '../../api/ai'

// API Key 本地存储键
const AI_KEY_STORAGE_KEY = 'ai_deepseek_key'

// 会话与消息
const sessions = ref([])
const currentSessionId = ref(null)
const messages = ref([])
const loadingSessions = ref(false)
const thinking = ref(false)
const inputText = ref('')
const messageListRef = ref(null)

// API Key 配置
const deepseekKey = ref('')
const keyInput = ref('')
const keyDialogVisible = ref(false)
const hasApiKey = computed(() => !!deepseekKey.value.trim())

// 快捷提问
const quickPrompts = [
  '宿舍水龙头漏水怎么办',
  '空调不制冷了',
  '宿舍突然断电跳闸',
  'WiFi 连不上网'
]

/** 加载会话列表 */
async function loadSessions() {
  loadingSessions.value = true
  try {
    const res = await listSessions()
    sessions.value = res.data || []
  } catch {
    sessions.value = []
  } finally {
    loadingSessions.value = false
  }
}

/** 新建会话 */
async function handleNewSession() {
  try {
    const res = await createSession('新会话')
    const session = res.data
    sessions.value.unshift(session)
    currentSessionId.value = session.id
    messages.value = []
  } catch {
    ElMessage.error('创建会话失败')
  }
}

/** 选择会话并加载消息 */
async function handleSelectSession(id) {
  if (currentSessionId.value === id) return
  currentSessionId.value = id
  messages.value = []
  try {
    const res = await listMessages(id)
    messages.value = res.data || []
    scrollToBottom()
  } catch {
    messages.value = []
  }
}

/** 删除会话 */
async function handleDeleteSession(id) {
  try {
    await ElMessageBox.confirm('确定删除该会话吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteSession(id)
    sessions.value = sessions.value.filter((s) => s.id !== id)
    if (currentSessionId.value === id) {
      currentSessionId.value = null
      messages.value = []
    }
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

/** 快捷提问 */
function handleQuickPrompt(prompt) {
  inputText.value = prompt
  handleSend()
}

/** 打开 API Key 配置弹窗 */
function openKeyDialog() {
  keyInput.value = deepseekKey.value
  keyDialogVisible.value = true
}

/** 保存 API Key 到本地存储 */
function saveKey() {
  const key = keyInput.value.trim()
  deepseekKey.value = key
  if (key) {
    localStorage.setItem(AI_KEY_STORAGE_KEY, key)
    ElMessage.success('API Key 已保存到本地')
  } else {
    localStorage.removeItem(AI_KEY_STORAGE_KEY)
    ElMessage.info('已清除 API Key，将使用模拟建议')
  }
  keyDialogVisible.value = false
}

/** 清除 API Key */
function clearKey() {
  deepseekKey.value = ''
  keyInput.value = ''
  localStorage.removeItem(AI_KEY_STORAGE_KEY)
  ElMessage.info('已清除 API Key，将使用模拟建议')
  keyDialogVisible.value = false
}

/** 发送消息 */
async function handleSend() {
  const prompt = inputText.value.trim()
  if (!prompt || thinking.value) return

  // 若无当前会话，先创建
  if (!currentSessionId.value) {
    try {
      const res = await createSession(prompt.length > 20 ? prompt.slice(0, 20) + '…' : prompt)
      const session = res.data
      sessions.value.unshift(session)
      currentSessionId.value = session.id
    } catch {
      ElMessage.error('创建会话失败')
      return
    }
  }

  // 先把用户消息插到列表
  const userMsg = {
    id: 'u-' + Date.now(),
    role: 'user',
    content: prompt
  }
  messages.value.push(userMsg)
  inputText.value = ''
  thinking.value = true
  await scrollToBottom()

  try {
    const payload = { sessionId: currentSessionId.value, prompt }
    if (deepseekKey.value.trim()) {
      payload.apiKey = deepseekKey.value.trim()
    }
    const res = await sendMessage(payload)
    messages.value.push(res.data)
    // 更新会话标题（若为默认「新会话」）
    const cur = sessions.value.find((s) => s.id === currentSessionId.value)
    if (cur && cur.title === '新会话') {
      cur.title = prompt.length > 20 ? prompt.slice(0, 20) + '…' : prompt
    }
  } catch {
    messages.value.push({
      id: 'e-' + Date.now(),
      role: 'assistant',
      content: '抱歉，发送失败，请稍后重试。'
    })
  } finally {
    thinking.value = false
    await scrollToBottom()
  }
}

/** 滚动到底部 */
async function scrollToBottom() {
  await nextTick()
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

onMounted(() => {
  deepseekKey.value = localStorage.getItem(AI_KEY_STORAGE_KEY) || ''
  loadSessions()
})
</script>

<style scoped>
.ai-chat-page {
  display: flex;
  height: calc(100vh - 160px);
  min-height: 520px;
  gap: 16px;
}

/* ============ 左侧会话栏 ============ */
.session-panel {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(18, 41, 74, 0.06);
  overflow: hidden;
}

.new-session-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 14px;
  padding: 10px 0;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #2c6bf2, #17bfd4);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.new-session-btn:hover {
  opacity: 0.9;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px 14px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 9px;
  cursor: pointer;
  transition: background 0.18s;
  margin-bottom: 4px;
}
.session-item:hover {
  background: #f0f5ff;
}
.session-item.active {
  background: #e6efff;
}

.session-icon {
  color: #2c6bf2;
  font-size: 16px;
  flex-shrink: 0;
}

.session-title {
  flex: 1;
  font-size: 13px;
  color: #2a3a55;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-delete {
  color: #b4bdce;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.18s, color 0.18s;
}
.session-item:hover .session-delete {
  opacity: 1;
}
.session-delete:hover {
  color: #ef4b4b;
}

.session-empty {
  padding: 30px 10px;
  text-align: center;
  font-size: 12px;
  color: #9aa7bd;
}

/* ============ 右侧对话区 ============ */
.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(18, 41, 74, 0.06);
  overflow: hidden;
}

/* 顶部栏 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #eef1f7;
  background: #fafbfe;
}
.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #1b2a45;
}
.chat-title-icon {
  color: #2c6bf2;
  font-size: 18px;
}
.chat-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.key-badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 999px;
}
.key-badge.key-on {
  background: #e6f7ee;
  color: #16a34a;
}
.key-badge.key-off {
  background: #fef0e6;
  color: #ea580c;
}
.key-tip {
  font-size: 12px;
  color: #7a879b;
  line-height: 1.7;
  margin: 0 0 14px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 22px 28px;
}

.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
  align-items: flex-start;
}
.message-row.msg-user {
  flex-direction: row-reverse;
}

.avatar {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}
.avatar-user {
  background: linear-gradient(135deg, #8ff0e4, #a9c8ff);
  color: #0a1728;
}
.avatar-ai {
  background: linear-gradient(135deg, #2c6bf2, #17bfd4);
}

.bubble {
  max-width: 70%;
  padding: 11px 15px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}
.msg-user .bubble {
  background: #2c6bf2;
  color: #fff;
  border-top-right-radius: 4px;
}
.msg-ai .bubble {
  background: #f4f7fc;
  color: #2a3a55;
  border-top-left-radius: 4px;
}

.bubble-content {
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
}

.bubble-thinking {
  display: flex;
  gap: 5px;
  padding: 14px 16px;
}
.bubble-thinking .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2c6bf2;
  animation: blink 1.2s infinite ease-in-out;
}
.bubble-thinking .dot:nth-child(2) {
  animation-delay: 0.2s;
}
.bubble-thinking .dot:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

/* 空态 */
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #7a879b;
  text-align: center;
}
.empty-icon {
  font-size: 48px;
  color: #2c6bf2;
  margin-bottom: 12px;
}
.chat-empty p {
  margin: 4px 0;
  font-size: 14px;
}
.empty-sub {
  font-size: 12px !important;
  color: #9aa7bd;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 18px;
  max-width: 520px;
}
.quick-prompt {
  padding: 6px 14px;
  background: #f0f5ff;
  color: #2c6bf2;
  border-radius: 999px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.18s;
}
.quick-prompt:hover {
  background: #dce8ff;
}

/* 输入区 */
.input-area {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #eef1f7;
  background: #fafbfe;
}
.input-area :deep(.el-textarea__inner) {
  border-radius: 10px;
  font-size: 14px;
}

.send-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 40px;
  padding: 0 18px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #2c6bf2, #17bfd4);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.send-btn:not(:disabled):hover {
  opacity: 0.9;
}
</style>
