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
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
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
    fetchList()
  } catch (error) {
    options.onError?.(error as Error)
  }
}

async function handleRebuild(id: number) {
  await rebuildKnowledge(id)
  ElMessage.success('已提交重建')
  fetchList()
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

onMounted(fetchList)
</script>

<style scoped lang="scss">
.upload-card {
  margin-bottom: 16px;
}
</style>
