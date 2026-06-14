<template>
  <div class="page-container ai-page">
    <PageHeader title="AI 助手">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="startNewConversation">新会话</el-button>
      </template>
    </PageHeader>

    <div class="assistant-shell">
      <aside class="conversation-panel">
        <div class="panel-title">会话记录</div>
        <el-scrollbar class="conversation-scroll">
          <div
            v-for="conversation in conversations"
            :key="conversation.id"
            class="conversation-item"
            :class="{ active: conversation.id === currentConversationId }"
            @click="selectConversation(conversation.id)"
          >
            <div class="conversation-title">{{ conversation.title || '新会话' }}</div>
            <el-button
              link
              type="danger"
              icon="Delete"
              class="delete-button"
              @click.stop="removeConversation(conversation.id)"
            />
          </div>
          <el-empty v-if="!conversationLoading && conversations.length === 0" description="暂无会话" :image-size="80" />
        </el-scrollbar>
      </aside>

      <section class="chat-panel">
        <el-scrollbar ref="messageScrollbar" class="message-scroll">
          <div v-if="messages.length === 0" class="empty-chat">
            <el-icon :size="46"><ChatDotRound /></el-icon>
            <div>开始提问知识库内容</div>
          </div>

          <div
            v-for="message in messages"
            :key="message.localId || message.id"
            class="message-row"
            :class="message.role"
          >
            <div class="message-bubble">
              <div class="message-content">{{ message.content }}</div>

              <div v-if="message.role === 'assistant' && parsedMetadata(message).needConfirm" class="confirm-block">
                当前请求需要确认，确认前不会执行写操作。
              </div>

              <AiActionConfirmCard
                v-if="message.role === 'assistant' && parsedMetadata(message).pendingAction?.status === 'PENDING'"
                :action="parsedMetadata(message).pendingAction!"
                :loading="actionProcessing"
                @confirm="confirmPendingAction"
                @cancel="cancelPendingAction"
              />

              <div v-if="message.role === 'assistant' && parsedMetadata(message).toolCalls.length" class="meta-block">
                <div class="meta-title">工具调用</div>
                <el-tag
                  v-for="tool in parsedMetadata(message).toolCalls"
                  :key="tool.toolName"
                  size="small"
                  :type="tool.status === 'FAILED' ? 'danger' : 'success'"
                >
                  {{ tool.toolName }} · {{ tool.status }}
                </el-tag>
              </div>

              <div v-if="message.role === 'assistant' && parsedMetadata(message).sources.length" class="meta-block">
                <div class="meta-title">来源</div>
                <div v-for="source in parsedMetadata(message).sources" :key="source.documentId + source.title" class="source-item">
                  <span class="source-title">{{ source.title || '知识库文档' }}</span>
                  <span v-if="source.score" class="source-score">{{ source.score.toFixed(3) }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-scrollbar>

        <div class="composer">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="输入问题"
            @keydown.ctrl.enter.prevent="sendMessage"
          />
          <el-button type="primary" icon="Promotion" :loading="sending" :disabled="!inputText.trim()" @click="sendMessage">
            发送
          </el-button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ScrollbarInstance } from 'element-plus'
import {
  cancelAiAction,
  confirmAiAction,
  deleteAiConversation,
  getAiConversations,
  getAiMessages,
  sendAiMessage,
} from '@/api/ai'
import type { AiActionExecuteResult, AiConversation, AiMessage, AiPendingAction, AiSource, AiToolCall } from '@/api/ai'
import AiActionConfirmCard from '@/components/ai/AiActionConfirmCard.vue'
import PageHeader from '@/components/PageHeader.vue'

type LocalMessage = AiMessage & {
  localId?: string
}

interface MessageMetadata {
  sources: AiSource[]
  toolCalls: AiToolCall[]
  needConfirm: boolean
  pendingAction?: AiPendingAction
}

const conversations = ref<AiConversation[]>([])
const messages = ref<LocalMessage[]>([])
const currentConversationId = ref<number>()
const inputText = ref('')
const sending = ref(false)
const conversationLoading = ref(false)
const actionProcessing = ref(false)
const messageScrollbar = ref<ScrollbarInstance>()

function parsedMetadata(message: LocalMessage): MessageMetadata {
  if (!message.metadata) return { sources: [], toolCalls: [], needConfirm: false }
  try {
    const data = JSON.parse(message.metadata)
    return {
      sources: Array.isArray(data.sources) ? data.sources : [],
      toolCalls: Array.isArray(data.toolCalls) ? data.toolCalls : [],
      needConfirm: data.needConfirm === true,
      pendingAction: data.pendingAction,
    }
  } catch {
    return { sources: [], toolCalls: [], needConfirm: false }
  }
}

async function loadConversations() {
  conversationLoading.value = true
  try {
    const res = await getAiConversations()
    conversations.value = res.data || []
  } finally {
    conversationLoading.value = false
  }
}

async function selectConversation(id: number) {
  currentConversationId.value = id
  const res = await getAiMessages(id)
  messages.value = res.data || []
  scrollToBottom()
}

function startNewConversation() {
  currentConversationId.value = undefined
  messages.value = []
  inputText.value = ''
}

async function removeConversation(id: number) {
  await ElMessageBox.confirm('确定删除该会话吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
  await deleteAiConversation(id)
  ElMessage.success('会话已删除')
  if (currentConversationId.value === id) {
    startNewConversation()
  }
  await loadConversations()
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  const pendingConversationId = currentConversationId.value
  inputText.value = ''
  messages.value.push({
    localId: `user-${Date.now()}`,
    id: Date.now(),
    conversationId: pendingConversationId || 0,
    role: 'user',
    content: text,
    createdAt: new Date().toISOString(),
  })
  scrollToBottom()

  sending.value = true
  try {
    const res = await sendAiMessage({
      conversationId: pendingConversationId,
      message: text,
      mode: 'knowledge',
    })
    currentConversationId.value = res.data.conversationId
    messages.value.push({
      localId: `assistant-${Date.now()}`,
      id: Date.now(),
      conversationId: res.data.conversationId,
      role: 'assistant',
      content: res.data.answer,
      metadata: JSON.stringify({
        sources: res.data.sources || [],
        toolCalls: res.data.toolCalls || [],
        needConfirm: res.data.needConfirm,
        pendingAction: res.data.pendingAction,
      }),
      createdAt: new Date().toISOString(),
    })
    await loadConversations()
    scrollToBottom()
  } catch {
    inputText.value = text
  } finally {
    sending.value = false
  }
}

async function confirmPendingAction(action: AiPendingAction) {
  if (actionProcessing.value) return
  if (action.riskLevel === 'HIGH') {
    try {
      await ElMessageBox.confirm('该操作为高风险操作，确认后会修改业务数据。确定执行吗？', '高风险确认', {
        type: 'error',
        confirmButtonText: '确认执行高风险操作',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }
  }

  actionProcessing.value = true
  try {
    const res = await confirmAiAction(action.actionId)
    updatePendingAction(action.actionId, 'EXECUTED', res.data)
    appendAssistantNotice(formatActionResult(res.data))
    ElMessage.success('操作已执行')
  } finally {
    actionProcessing.value = false
  }
}

async function cancelPendingAction(action: AiPendingAction) {
  if (actionProcessing.value) return
  actionProcessing.value = true
  try {
    await cancelAiAction(action.actionId)
    updatePendingAction(action.actionId, 'CANCELLED')
    appendAssistantNotice(`已取消待确认操作：${action.actionName}`)
    ElMessage.success('操作已取消')
  } finally {
    actionProcessing.value = false
  }
}

function updatePendingAction(actionId: number, status: AiPendingAction['status'], result?: AiActionExecuteResult) {
  messages.value = messages.value.map((message) => {
    if (!message.metadata) return message
    try {
      const data = JSON.parse(message.metadata)
      if (data.pendingAction?.actionId !== actionId) return message
      data.pendingAction = {
        ...data.pendingAction,
        status,
        resultData: result?.resultData,
        errorMessage: result?.errorMessage,
      }
      data.needConfirm = status === 'PENDING'
      return { ...message, metadata: JSON.stringify(data) }
    } catch {
      return message
    }
  })
}

function appendAssistantNotice(content: string) {
  messages.value.push({
    localId: `assistant-action-${Date.now()}`,
    id: Date.now(),
    conversationId: currentConversationId.value || 0,
    role: 'assistant',
    content,
    metadata: JSON.stringify({ sources: [], toolCalls: [], needConfirm: false }),
    createdAt: new Date().toISOString(),
  })
  scrollToBottom()
}

function formatActionResult(result: AiActionExecuteResult) {
  if (result.status === 'EXECUTED') {
    return `操作已执行：${result.actionType}\n结果：${JSON.stringify(result.resultData || {}, null, 2)}`
  }
  return `操作执行失败：${result.errorMessage || '未知错误'}`
}

function scrollToBottom() {
  nextTick(() => {
    const wrap = messageScrollbar.value?.wrapRef
    if (wrap) {
      wrap.scrollTop = wrap.scrollHeight
    }
  })
}

onMounted(async () => {
  await loadConversations()
  if (conversations.value[0]) {
    await selectConversation(conversations.value[0].id)
  }
})
</script>

<style scoped lang="scss">
.ai-page {
  height: 100%;
  min-height: 0;
}

.assistant-shell {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  height: calc(100vh - 128px);
  min-height: 560px;
}

.conversation-panel,
.chat-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  min-height: 0;
}

.conversation-panel {
  display: flex;
  flex-direction: column;
}

.panel-title {
  height: 48px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  border-bottom: 1px solid #e5e7eb;
}

.conversation-scroll {
  flex: 1;
  min-height: 0;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 46px;
  padding: 8px 10px 8px 14px;
  cursor: pointer;
  border-bottom: 1px solid #f1f5f9;

  &:hover,
  &.active {
    background: #eff6ff;
  }
}

.conversation-title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-button {
  opacity: 0;
}

.conversation-item:hover .delete-button {
  opacity: 1;
}

.chat-panel {
  display: flex;
  flex-direction: column;
}

.message-scroll {
  flex: 1;
  min-height: 0;
  padding: 18px;
}

.empty-chat {
  height: 100%;
  min-height: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #94a3b8;
  font-size: 14px;
}

.message-row {
  display: flex;
  margin-bottom: 16px;

  &.user {
    justify-content: flex-end;

    .message-bubble {
      background: #3b82f6;
      color: #fff;
      border-color: #3b82f6;
    }
  }

  &.assistant {
    justify-content: flex-start;
  }
}

.message-bubble {
  max-width: min(720px, 78%);
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.7;
  font-size: 14px;
}

.meta-block {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.confirm-block {
  margin-top: 10px;
  padding: 8px 10px;
  border: 1px solid #fed7aa;
  border-radius: 6px;
  background: #fff7ed;
  color: #9a3412;
  font-size: 13px;
}

.meta-title {
  width: 100%;
  font-size: 12px;
  color: #64748b;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border: 1px solid #dbeafe;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
}

.source-score {
  color: #64748b;
}

.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px;
  gap: 12px;
  padding: 16px;
  border-top: 1px solid #e5e7eb;
}

@media (max-width: 900px) {
  .assistant-shell {
    grid-template-columns: 1fr;
    height: auto;
  }

  .conversation-panel {
    height: 220px;
  }

  .chat-panel {
    height: 620px;
  }
}
</style>
