<template>
  <div class="page-container">
    <PageHeader title="文件管理" subtitle="上传与管理文件" />

    <div class="card">
      <!-- 上传区域 -->
      <div class="mb-6">
        <el-upload
          ref="uploadRef"
          :action="uploadUrl"
          :headers="uploadHeaders"
          :data="uploadData"
          :before-upload="beforeUpload"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :show-file-list="false"
          drag
          multiple
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持任意格式文件，单个文件不超过 10MB</div>
          </template>
        </el-upload>
      </div>

      <!-- 已上传文件列表 -->
      <h3 class="text-sm font-semibold text-gray-700 mb-3">已上传文件</h3>
      <el-table :data="fileList" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="fileName" label="文件名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="fileType" label="文件类型" width="130" />
        <el-table-column prop="fileSize" label="文件大小" width="100" align="right">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="预览" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="isImage(row.fileType)"
              :src="row.url"
              :preview-src-list="[row.url]"
              preview-teleported
              style="width: 40px; height: 40px; border-radius: 4px"
              fit="cover"
            />
            <el-icon v-else :size="20" class="text-gray-400"><Document /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="Download" @click="handleDownload(row)">下载</el-button>
            <el-popconfirm title="确定删除该文件吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && fileList.length === 0" class="mt-8">
        <el-empty description="暂无已上传文件" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadInstance, UploadProps } from 'element-plus'
import { uploadFile, deleteFile } from '@/api/file'
import type { FileInfo } from '@/api/file'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const uploadRef = ref<UploadInstance>()
const fileList = ref<FileInfo[]>([])
const loading = ref(false)

// 上传配置
const uploadUrl = '/api/files/upload'
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})
const uploadData = computed(() => ({}))

function beforeUpload(file: File) {
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

function handleUploadSuccess(response: any) {
  if (response.code === 200) {
    ElMessage.success('上传成功')
    fileList.value.unshift(response.data)
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleUploadError() {
  ElMessage.error('上传失败，请检查网络或服务器')
}

function handleDownload(file: FileInfo) {
  const link = document.createElement('a')
  link.href = file.url
  link.download = file.fileName
  link.click()
}

async function handleDelete(id: number) {
  try {
    await deleteFile(id)
    ElMessage.success('删除成功')
    fileList.value = fileList.value.filter((f) => f.id !== id)
  } catch {}
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function isImage(type: string): boolean {
  return type?.startsWith('image/')
}

onMounted(() => {
  // 文件列表暂不支持分页查询，上传后自动添加到列表
})
</script>
