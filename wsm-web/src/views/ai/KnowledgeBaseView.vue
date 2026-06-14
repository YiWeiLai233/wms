<template>
  <div class="page-container">
    <PageHeader title="知识库管理">
      <template #actions>
        <el-button icon="Refresh" @click="fetchList">刷新</el-button>
      </template>
    </PageHeader>

    <div class="card upload-card">
      <el-upload
        drag
        :show-file-list="false"
        :http-request="handleUpload"
        :before-upload="beforeUpload"
        accept=".pdf,.doc,.docx,.xlsx,.xls,.txt,.csv,.md"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文档拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 PDF、Word、Excel、TXT、CSV、Markdown，单个文件不超过 10MB</div>
        </template>
      </el-upload>
    </div>

    <!-- 处理日志 -->
    <div v-if="processingDocs.length > 0" class="card log-card">
      <div class="flex items-center mb-2">
        <el-icon class="mr-1 text-blue-500"><Loading /></el-icon>
        <span class="font-semibold text-sm">处理日志</span>
      </div>
      <div class="log-container">
        <div v-for="doc in processingDocs" :key="doc.id" class="log-item">
          <span class="log-time">{{ formatDateTime(doc.createdAt) }}</span>
          <el-tag :type="statusType(doc.status)" size="small" class="mx-2">{{ statusLabel(doc.status) }}</el-tag>
          <span class="log-title">{{ doc.title }}</span>
          <span v-if="doc.status === 'PROCESSING'" class="log-spinner">
            <el-icon class="is-loading"><Loading /></el-icon>
          </span>
          <span v-if="doc.status === 'SUCCESS'" class="log-success">
            ✓ 已完成，共 {{ doc.chunkCount }} 个分块
          </span>
          <span v-if="doc.status === 'FAILED'" class="log-error">
            ✗ {{ doc.errorMessage || '处理失败' }}
          </span>
        </div>
      </div>
    </div>

    <div class="card">
      <el-table :data="documents" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="分块数" width="90" align="right" />
        <el-table-column prop="createdAt" label="上传时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.errorMessage || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="Refresh" @click="handleRebuild(row.id)">重建</el-button>
            <el-popconfirm title="确定删除该知识库文档吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && documents.length === 0" description="暂无知识库文档" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { Loading, UploadFilled } from '@element-plus/icons-vue'
import {
  deleteKnowledge,
  getKnowledgeList,
  rebuildKnowledge,
  uploadKnowledge,
} from '@/api/ai'
import type { AiKnowledgeDocument } from '@/api/ai'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const documents = ref<AiKnowledgeDocument[]>([])
const loading = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

const processingDocs = computed(() =>
  documents.value.filter((d) => d.status === 'PROCESSING' || d.status === 'PENDING')
)

async function fetchList() {
  loading.value = true
  try {
    const res = await getKnowledgeList()
    documents.value = res.data || []
  } finally {
    loading.value = false
  }
}

function beforeUpload(file: File) {
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

async function handleUpload(options: UploadRequestOptions) {
  try {
    await uploadKnowledge(options.file as File)
    ElMessage.success('文档已上传，正在处理')
    options.onSuccess?.({})
    await fetchList()
    startPolling()
  } catch (error) {
    options.onError?.(error as Error)
  }
}

async function handleRebuild(id: number) {
  await rebuildKnowledge(id)
  ElMessage.success('已提交重建')
  await fetchList()
  startPolling()
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    await fetchList()
    if (processingDocs.value.length === 0) {
      stopPolling()
      ElMessage.success('知识库处理完成')
    }
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function handleDelete(id: number) {
  await deleteKnowledge(id)
  ElMessage.success('删除成功')
  fetchList()
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待处理',
    PROCESSING: '处理中',
    SUCCESS: '成功',
    FAILED: '失败',
  }
  return map[status] || status
}

function statusType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'info',
    PROCESSING: 'warning',
    SUCCESS: 'success',
    FAILED: 'danger',
  }
  return map[status] || 'info'
}

onMounted(async () => {
  await fetchList()
  if (processingDocs.value.length > 0) {
    startPolling()
  }
})

onUnmounted(stopPolling)
</script>

<style scoped lang="scss">
.upload-card {
  margin-bottom: 16px;
}
.log-card {
  margin-bottom: 16px;
  background: #fafafa;
}
.log-container {
  max-height: 200px;
  overflow-y: auto;
  font-size: 13px;
}
.log-item {
  display: flex;
  align-items: center;
  padding: 4px 0;
  border-bottom: 1px solid #f0f0f0;
  &:last-child {
    border-bottom: none;
  }
}
.log-time {
  color: #999;
  font-size: 12px;
  min-width: 150px;
}
.log-title {
  color: #333;
  flex: 1;
}
.log-spinner {
  color: #e6a23c;
  margin-left: 8px;
}
.log-success {
  color: #67c23a;
  margin-left: 8px;
  font-size: 12px;
}
.log-error {
  color: #f56c6c;
  margin-left: 8px;
  font-size: 12px;
}
</style>
