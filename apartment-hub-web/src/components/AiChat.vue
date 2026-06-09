<template>
  <div class="ai-chat-container">
    <!-- Floating Button -->
    <div v-if="!open" class="ai-chat-fab" @click="openChat">
      <span class="ai-chat-fab-icon">🤖</span>
    </div>

    <!-- Chat Window -->
    <Transition name="slide-up">
      <div v-if="open" class="ai-chat-window">
        <div class="ai-chat-header">
          <div class="ai-chat-header-left">
            <span class="ai-chat-avatar">🤖</span>
            <div>
              <div class="ai-chat-title">AI 公寓管家</div>
              <div class="ai-chat-subtitle">{{ connected ? '在线' : '连接中...' }}</div>
            </div>
          </div>
          <div class="ai-chat-header-right">
            <el-button text circle size="small" @click="clearChat">
              <el-icon><Delete /></el-icon>
            </el-button>
            <el-button text circle size="small" @click="open = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="ai-chat-body" ref="bodyRef">
          <div v-if="messages.length === 0" class="ai-chat-empty">
            <div class="ai-chat-empty-icon">🏠</div>
            <div class="ai-chat-empty-text">问我任何公寓管理相关的问题</div>
            <div class="ai-chat-suggestions">
              <div
                v-for="q in suggestions"
                :key="q"
                class="ai-chat-suggestion"
                @click="sendMessage(q)"
              >{{ q }}</div>
            </div>
          </div>

          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="ai-chat-message"
            :class="msg.role"
          >
            <div class="ai-chat-message-avatar">
              {{ msg.role === 'user' ? '👤' : '🤖' }}
            </div>
            <div class="ai-chat-message-content">
              <div class="ai-chat-message-text" v-text="msg.content"></div>
              <span v-if="msg.streaming" class="ai-chat-cursor">|</span>
            </div>
          </div>
        </div>

        <div class="ai-chat-footer">
          <el-input
            v-model="input"
            placeholder="输入您的问题..."
            :disabled="loading"
            @keyup.enter="sendMessage()"
          >
            <template #append>
              <el-button
                :icon="loading ? undefined : Promotion"
                :loading="loading"
                :disabled="!input.trim()"
                @click="sendMessage()"
              />
            </template>
          </el-input>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { Promotion, Delete, Close } from '@element-plus/icons-vue'

interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
  streaming?: boolean
}

const open = ref(false)
const input = ref('')
const loading = ref(false)
const connected = ref(false)
const messages = ref<ChatMessage[]>([])
const bodyRef = ref<HTMLElement>()

let ws: WebSocket | null = null
let reconnectTimer: number | null = null

const suggestions = [
  'A-101房间有人住吗',
  '有哪些逾期未缴的账单',
  '本月租金收入多少',
  '未来60天内到期的合同有哪些',
  '帮我查一下张三的租客信息',
]

function getToken(): string {
  return localStorage.getItem('token') || ''
}

function getWsUrl(): string {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = location.host
  return `${protocol}//${host}/ws/ai/chat?token=${getToken()}`
}

function connect() {
  if (ws && ws.readyState === WebSocket.OPEN) return

  try {
    ws = new WebSocket(getWsUrl())

    ws.onopen = () => {
      connected.value = true
      loading.value = false
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        handleMessage(data)
      } catch {
        // ignore parse errors
      }
    }

    ws.onclose = () => {
      connected.value = false
      // Auto reconnect after 3 seconds
      if (open.value) {
        reconnectTimer = window.setTimeout(connect, 3000)
      }
    }

    ws.onerror = () => {
      connected.value = false
    }
  } catch {
    connected.value = false
  }
}

function handleMessage(data: any) {
  switch (data.type) {
    case 'system':
      messages.value.push({ role: 'system', content: data.content })
      scrollToBottom()
      break

    case 'start':
      loading.value = true
      messages.value.push({ role: 'assistant', content: '', streaming: true })
      scrollToBottom()
      break

    case 'token':
      if (messages.value.length > 0) {
        const last = messages.value[messages.value.length - 1]
        if (last.streaming) {
          last.content += data.content
        }
      }
      scrollToBottom()
      break

    case 'done':
      if (messages.value.length > 0) {
        messages.value[messages.value.length - 1].streaming = false
      }
      loading.value = false
      scrollToBottom()
      break

    case 'error':
      if (messages.value.length > 0) {
        messages.value[messages.value.length - 1].streaming = false
      }
      messages.value.push({ role: 'system', content: data.content || '出错了' })
      loading.value = false
      scrollToBottom()
      break
  }
}

function sendMessage(text?: string) {
  const msg = text || input.value.trim()
  if (!msg || loading.value) return

  if (!connected.value) {
    messages.value.push({ role: 'system', content: '正在连接 AI 服务，请稍后再试...' })
    connect()
    return
  }

  messages.value.push({ role: 'user', content: msg })
  input.value = ''
  loading.value = true
  scrollToBottom()

  ws?.send(JSON.stringify({ message: msg }))
}

function clearChat() {
  messages.value = []
}

function scrollToBottom() {
  nextTick(() => {
    if (bodyRef.value) {
      bodyRef.value.scrollTop = bodyRef.value.scrollHeight
    }
  })
}

function openChat() {
  open.value = true
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    connect()
  }
}

// Cleanup on unmount when chat is closed
watch(open, (val) => {
  if (!val) {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }
})
</script>

<style scoped>
.ai-chat-container {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
}

/* Floating Action Button */
.ai-chat-fab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.ai-chat-fab:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 24px rgba(37, 99, 235, 0.55);
}
.ai-chat-fab-icon {
  font-size: 24px;
  line-height: 1;
}

/* Chat Window */
.ai-chat-window {
  width: 400px;
  height: 560px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Slide-up transition */
.slide-up-enter-active {
  transition: all 0.3s ease-out;
}
.slide-up-leave-active {
  transition: all 0.2s ease-in;
}
.slide-up-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.98);
}

/* Header */
.ai-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
}
.ai-chat-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ai-chat-avatar {
  font-size: 28px;
  line-height: 1;
}
.ai-chat-title {
  font-size: 15px;
  font-weight: 600;
}
.ai-chat-subtitle {
  font-size: 12px;
  opacity: 0.8;
}
.ai-chat-header-right {
  display: flex;
  gap: 4px;
}
.ai-chat-header-right .el-button {
  color: #fff;
}

/* Body */
.ai-chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f8fafc;
}
.ai-chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 60px;
}
.ai-chat-empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.ai-chat-empty-text {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 20px;
}
.ai-chat-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
.ai-chat-suggestion {
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s;
}
.ai-chat-suggestion:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

/* Messages */
.ai-chat-message {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.ai-chat-message.user {
  flex-direction: row-reverse;
}
.ai-chat-message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.ai-chat-message-content {
  max-width: 75%;
}
.ai-chat-message-text {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.ai-chat-message.user .ai-chat-message-text {
  background: #2563eb;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.ai-chat-message.assistant .ai-chat-message-text {
  background: #fff;
  color: #1e293b;
  border: 1px solid #e2e8f0;
  border-bottom-left-radius: 4px;
}
.ai-chat-message.system .ai-chat-message-text {
  background: #fef3c7;
  color: #92400e;
  font-size: 13px;
  text-align: center;
  max-width: 100%;
  border-radius: 10px;
}
.ai-chat-message.system {
  justify-content: center;
}
.ai-chat-message.system .ai-chat-message-avatar {
  display: none;
}
.ai-chat-message.system .ai-chat-message-content {
  max-width: 90%;
}

.ai-chat-cursor {
  display: inline;
  animation: blink 1s infinite;
  color: #2563eb;
  font-weight: bold;
}
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* Footer */
.ai-chat-footer {
  padding: 12px 16px;
  border-top: 1px solid #e2e8f0;
}
.ai-chat-footer :deep(.el-input-group__append) {
  background: #2563eb;
  border-color: #2563eb;
  padding: 0 10px;
}
.ai-chat-footer :deep(.el-input-group__append .el-button) {
  color: #fff;
}
</style>
