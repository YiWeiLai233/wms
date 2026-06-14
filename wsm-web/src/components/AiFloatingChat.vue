<template>
  <div class="ai-float-container">
    <!-- 浮动按钮 -->
    <div class="ai-float-btn" @click="toggleChat">
      <el-icon :size="24"><ChatDotRound /></el-icon>
    </div>

    <!-- 聊天窗口 -->
    <transition name="slide-up">
      <div v-if="visible" class="ai-chat-window">
        <div class="chat-header">
          <span class="chat-title">AI 助手</span>
          <div class="chat-actions">
            <el-icon class="action-btn" @click="goFullPage"><FullScreen /></el-icon>
            <el-icon class="action-btn" @click="visible = false"><Close /></el-icon>
          </div>
        </div>

        <div ref="messageContainer" class="chat-messages">
          <div v-if="messages.length === 0" class="empty-msg">
            <el-icon :size="32"><ChatDotRound /></el-icon>
            <div>有什么可以帮您？</div>
          </div>
          <div
            v-for="msg in messages"
            :key="msg.localId || msg.id"
            class="msg-row"
            :class="msg.role"
          >
            <div class="msg-bubble">{{ msg.content }}</div>
          </div>
          <div v-if="sending" class="msg-row assistant">
            <div class="msg-bubble loading">思考中...</div>
          </div>
        </div>

        <div class="chat-input">
          <el-input
            v-model="inputText"
            placeholder="输入问题，回车发送"
            :disabled="sending"
            @keydown.enter.exact="handleSend"
          >
            <template #append>
              <el-button :icon="Promotion" :loading="sending" :disabled="!inputText.trim()" @click="handleSend" />
            </template>
          </el-input>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotRound, FullScreen, Close, Promotion } from '@element-plus/icons-vue'
import { sendAiMessage, getAiMessages } from '@/api/ai'
import type { AiMessage } from '@/api/ai'

const router = useRouter()
const visible = ref(false)
const sending = ref(false)
const inputText = ref('')
const messages = ref<AiMessage[]>([])
const conversationId = ref<number | null>(null)
const messageContainer = ref<HTMLElement>()

function toggleChat() {
  visible.value = !visible.value
  if (visible.value && conversationId.value) {
    loadMessages()
  }
}

function goFullPage() {
  visible.value = false
  router.push('/ai/assistant')
}

async function handleSend(event?: KeyboardEvent) {
  if (event?.isComposing) return
  event?.preventDefault()

  const text = inputText.value.trim()
  if (!text || sending.value) return

  messages.value.push({
    id: Date.now(),
    localId: Date.now(),
    role: 'user',
    content: text,
    createdAt: new Date().toISOString(),
  } as AiMessage)

  inputText.value = ''
  await nextTick()
  scrollToBottom()

  sending.value = true
  try {
    const res = await sendAiMessage({
      message: text,
      conversationId: conversationId.value || undefined,
    })

    const data = res.data
    conversationId.value = data.conversationId

    messages.value.push({
      id: data.messageId || Date.now() + 1,
      localId: Date.now() + 1,
      role: 'assistant',
      content: data.answer,
      createdAt: new Date().toISOString(),
    } as AiMessage)

    await nextTick()
    scrollToBottom()
  } catch {
    messages.value.push({
      id: Date.now() + 2,
      localId: Date.now() + 2,
      role: 'assistant',
      content: '抱歉，暂时无法回答，请稍后重试。',
      createdAt: new Date().toISOString(),
    } as AiMessage)
  } finally {
    sending.value = false
  }
}

async function loadMessages() {
  if (!conversationId.value) return
  try {
    const res = await getAiMessages(conversationId.value)
    messages.value = res.data || []
    await nextTick()
    scrollToBottom()
  } catch {}
}

function scrollToBottom() {
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
}
</script>

<style scoped lang="scss">
.ai-float-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
}

.ai-float-btn {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #3b82f6;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
  transition: all 0.2s;

  &:hover {
    background: #2563eb;
    transform: scale(1.05);
  }
}

.ai-chat-window {
  position: absolute;
  bottom: 64px;
  right: 0;
  width: 380px;
  height: 500px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  height: 48px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #3b82f6;
  color: #fff;
  flex-shrink: 0;
}

.chat-title {
  font-size: 15px;
  font-weight: 600;
}

.chat-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  cursor: pointer;
  opacity: 0.8;
  transition: opacity 0.2s;

  &:hover {
    opacity: 1;
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f8fafc;
}

.empty-msg {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #94a3b8;
  gap: 8px;
  font-size: 14px;
}

.msg-row {
  margin-bottom: 12px;
  display: flex;

  &.user {
    justify-content: flex-end;

    .msg-bubble {
      background: #3b82f6;
      color: #fff;
      border-radius: 12px 12px 4px 12px;
    }
  }

  &.assistant {
    justify-content: flex-start;

    .msg-bubble {
      background: #fff;
      color: #1e293b;
      border-radius: 12px 12px 12px 4px;
      border: 1px solid #e2e8f0;
    }
  }
}

.msg-bubble {
  max-width: 80%;
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;

  &.loading {
    color: #94a3b8;
    font-style: italic;
  }
}

.chat-input {
  padding: 12px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
  flex-shrink: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: all 0.25s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
