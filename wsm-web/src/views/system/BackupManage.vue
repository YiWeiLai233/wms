<template>
  <div class="page-container">
    <PageHeader title="数据库备份" subtitle="全量备份与增量备份">
      <template #actions>
        <el-button type="primary" icon="Download" @click="openBackupDialog('INCREMENTAL')">增量备份</el-button>
        <el-button type="success" icon="FolderOpened" @click="openBackupDialog('FULL')">全量备份</el-button>
      </template>
    </PageHeader>

    <div class="card mb-4">
      <el-alert type="info" :closable="false" show-icon class="mb-4">
        <template #title>
          <strong>全量备份</strong>：导出整个数据库为 SQL 文件 &nbsp;|&nbsp;
          <strong>增量备份</strong>：仅导出自上次备份以来变更的数据
        </template>
      </el-alert>
    </div>

    <div class="card">
      <h3 class="text-sm font-semibold text-gray-700 mb-3">备份历史</h3>
      <el-table :data="backupList" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="备份类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.backupType === 'FULL' ? 'success' : 'primary'" size="small">
              {{ row.backupType === 'FULL' ? '全量备份' : '增量备份' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column label="文件大小" width="110" align="right">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="备份时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'SUCCESS' && row.backupType === 'FULL'" type="success" link icon="RefreshRight" @click="openIncrDialog(row)">增量备份</el-button>
            <el-button v-if="row.status === 'SUCCESS'" type="primary" link icon="Download" @click="handleDownload(row)">下载</el-button>
            <el-popconfirm title="确定删除该备份记录吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 备份对话框 -->
    <el-dialog v-model="backupDialogVisible" :title="backupType === 'FULL' ? '全量备份' : '增量备份'" width="520px" destroy-on-close>
      <el-form ref="backupFormRef" :model="backupForm" label-width="100px">
        <el-form-item v-if="backupType === 'INCREMENTAL'" label="基准备份">
          <el-select v-model="backupForm.baseBackupId" placeholder="选择基准全量备份（不选则自动取最近备份）" clearable style="width: 100%">
            <el-option
              v-for="item in fullBackupList"
              :key="item.id"
              :label="`#${item.id} ${item.fileName} (${formatDateTime(item.createdAt)})`"
              :value="item.id"
            />
          </el-select>
          <div class="text-xs text-gray-400 mt-1">增量备份将导出自该备份之后变更的数据</div>
        </el-form-item>
        <el-form-item label="保存方式">
          <el-radio-group v-model="backupForm.saveMode">
            <el-radio value="download">下载到本地</el-radio>
            <el-radio value="server">保存到服务器</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="backupForm.saveMode === 'server'" label="服务器路径">
          <el-input v-model="backupForm.backupPath" placeholder="如 /data/backup" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="backupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="backing" @click="handleBackup">
          {{ backupForm.saveMode === 'server' ? '开始备份' : '开始备份并下载' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { fullBackup, incrementalBackup, getBackupList, downloadBackup, deleteBackup } from '@/api/backup'
import type { BackupRecord } from '@/api/backup'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const backupList = ref<BackupRecord[]>([])

const backupDialogVisible = ref(false)
const backupType = ref<'FULL' | 'INCREMENTAL'>('FULL')
const backing = ref(false)
const backupFormRef = ref<FormInstance>()

// 筛选出成功的全量备份
const fullBackupList = computed(() =>
  backupList.value.filter(b => b.backupType === 'FULL' && b.status === 'SUCCESS')
)

const backupForm = reactive({
  saveMode: 'download' as 'download' | 'server',
  backupPath: '',
  baseBackupId: undefined as number | undefined,
})

async function fetchBackupList() {
  loading.value = true
  try {
    const res = await getBackupList()
    backupList.value = res.data || []
  } catch {
    backupList.value = []
  } finally {
    loading.value = false
  }
}

function openBackupDialog(type: 'FULL' | 'INCREMENTAL') {
  backupType.value = type
  backupForm.saveMode = 'download'
  backupForm.backupPath = ''
  backupForm.baseBackupId = undefined
  backupDialogVisible.value = true
}

function openIncrDialog(row: BackupRecord) {
  backupType.value = 'INCREMENTAL'
  backupForm.saveMode = 'download'
  backupForm.backupPath = ''
  backupForm.baseBackupId = row.id
  backupDialogVisible.value = true
}

async function handleBackup() {
  backing.value = true
  try {
    const backupPath = backupForm.saveMode === 'server' ? backupForm.backupPath : undefined

    let result
    if (backupType.value === 'FULL') {
      result = await fullBackup(backupPath)
    } else {
      result = await incrementalBackup(backupForm.baseBackupId, backupPath)
    }

    const record = result.data
    ElMessage.success('备份成功')

    // 如果是下载模式，触发浏览器下载
    if (backupForm.saveMode === 'download' && record?.fileName) {
      await triggerDownload(record.fileName)
    }

    backupDialogVisible.value = false
    await fetchBackupList()
  } catch {} finally {
    backing.value = false
  }
}

async function handleDownload(row: BackupRecord) {
  await triggerDownload(row.fileName)
}

async function triggerDownload(fileName: string) {
  try {
    const blob = await downloadBackup(fileName)
    const url = window.URL.createObjectURL(blob as unknown as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败')
  }
}

async function handleDelete(id: number) {
  try {
    await deleteBackup(id)
    ElMessage.success('删除成功')
    await fetchBackupList()
  } catch {}
}

function formatSize(bytes?: number): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return `${size.toFixed(i === 0 ? 0 : 1)} ${units[i]}`
}

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 19)
}

onMounted(() => {
  fetchBackupList()
})
</script>
