<template>
  <div class="page-container">
    <PageHeader title="数据库备份" subtitle="全量备份与增量备份">
      <template #actions>
        <el-button icon="Setting" @click="settingsDialogVisible = true">备份设置</el-button>
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
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
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

    <!-- 备份设置对话框 -->
    <el-dialog v-model="settingsDialogVisible" title="备份设置" width="580px" destroy-on-close>
      <el-form :model="settingsForm" label-width="110px">
        <el-divider content-position="left">自动备份</el-divider>
        <el-form-item label="启用自动备份">
          <el-switch v-model="settingsForm.autoBackupEnabled" />
        </el-form-item>
        <template v-if="settingsForm.autoBackupEnabled">
          <el-form-item label="增量备份周期">
            <el-select v-model="settingsForm.incrementalCron" style="width: 100%">
              <el-option label="每天凌晨 3:00" value="0 0 3 * * ?" />
              <el-option label="每天凌晨 2:00" value="0 0 2 * * ?" />
              <el-option label="每天凌晨 4:00" value="0 0 4 * * ?" />
              <el-option label="每12小时" value="0 0 */12 * * ?" />
              <el-option label="每6小时" value="0 0 */6 * * ?" />
            </el-select>
          </el-form-item>
          <el-form-item label="全量备份周期">
            <el-select v-model="settingsForm.fullCron" style="width: 100%">
              <el-option label="每周日凌晨 4:00" value="0 0 4 ? * SUN" />
              <el-option label="每周一凌晨 4:00" value="0 0 4 ? * MON" />
              <el-option label="每月1号凌晨 4:00" value="0 0 4 1 * ?" />
              <el-option label="不自动全量" value="" />
            </el-select>
          </el-form-item>
          <el-form-item label="保留天数">
            <el-input-number v-model="settingsForm.retentionDays" :min="7" :max="365" />
            <span class="ml-2 text-sm text-gray-400">超过天数的备份自动清理</span>
          </el-form-item>
        </template>

        <el-divider content-position="left">远程备份</el-divider>
        <el-form-item label="启用远程备份">
          <el-switch v-model="settingsForm.remoteBackupEnabled" />
        </el-form-item>
        <template v-if="settingsForm.remoteBackupEnabled">
          <el-form-item label="远程类型">
            <el-select v-model="settingsForm.remoteType" style="width: 100%">
              <el-option label="SSH (scp/rsync)" value="ssh" />
              <el-option label="SFTP" value="sftp" />
              <el-option label="FTP" value="ftp" />
              <el-option label="OSS (阿里云)" value="oss" />
              <el-option label="COS (腾讯云)" value="cos" />
              <el-option label="S3 (AWS)" value="s3" />
            </el-select>
          </el-form-item>
          <el-form-item label="服务器地址">
            <el-input v-model="settingsForm.remoteHost" placeholder="如 192.168.1.100 或 backup.example.com" />
          </el-form-item>
          <el-form-item label="端口">
            <el-input v-model="settingsForm.remotePort" :placeholder="settingsForm.remoteType === 'ssh' || settingsForm.remoteType === 'sftp' ? '22' : '21'" style="width: 120px" />
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="settingsForm.remoteUsername" placeholder="登录用户名" />
          </el-form-item>
          <el-form-item :label="settingsForm.remoteType === 'ssh' ? '密码 / 密钥' : '密码'">
            <el-input v-model="settingsForm.remotePassword" type="password" show-password :placeholder="settingsForm.remoteType === 'ssh' ? '密码或私钥路径(~/.ssh/id_rsa)' : '登录密码'" />
          </el-form-item>
          <el-form-item label="远程路径">
            <el-input v-model="settingsForm.remotePath" placeholder="如 /data/backup/wms" />
          </el-form-item>
          <el-form-item v-if="settingsForm.remoteType === 'ssh'" label="传输方式">
            <el-radio-group v-model="settingsForm.sshMethod">
              <el-radio value="scp">scp（简单可靠）</el-radio>
              <el-radio value="rsync">rsync（增量传输）</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" plain :loading="testingConnection" @click="handleTestConnection">
              <el-icon><Link /></el-icon>
              测试连接
            </el-button>
            <span v-if="connectionResult !== null" class="ml-3 text-sm" :class="connectionResult ? 'text-green-500' : 'text-red-500'">
              {{ connectionResult ? '✅ 连接成功' : '❌ 连接失败' }}
            </span>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="settingsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSettings">保存设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { fullBackup, incrementalBackup, getBackupList, downloadBackupUrl, deleteBackup, testBackupConnection } from '@/api/backup'
import type { BackupRecord } from '@/api/backup'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const backupList = ref<BackupRecord[]>([])

const backupDialogVisible = ref(false)
const backupType = ref<'FULL' | 'INCREMENTAL'>('FULL')
const backing = ref(false)
const backupFormRef = ref<FormInstance>()

const backupForm = reactive({
  saveMode: 'download' as 'download' | 'server',
  backupPath: '',
})

// 备份设置
const settingsDialogVisible = ref(false)
const settingsForm = reactive({
  autoBackupEnabled: true,
  incrementalCron: '0 0 3 * * ?',
  fullCron: '0 0 4 ? * SUN',
  retentionDays: 30,
  remoteBackupEnabled: false,
  remoteType: 'ssh',
  sshMethod: 'scp',
  remoteHost: '',
  remotePort: '22',
  remoteUsername: '',
  remotePassword: '',
  remotePath: '',
})

// 测试连接
const testingConnection = ref(false)
const connectionResult = ref<boolean | null>(null)

async function handleTestConnection() {
  if (!settingsForm.remoteHost || !settingsForm.remoteUsername) {
    ElMessage.warning('请填写服务器地址和用户名')
    return
  }

  testingConnection.value = true
  connectionResult.value = null

  try {
    const res = await testBackupConnection({
      type: settingsForm.remoteType,
      host: settingsForm.remoteHost,
      port: settingsForm.remotePort || (settingsForm.remoteType === 'ssh' || settingsForm.remoteType === 'sftp' ? '22' : '21'),
      username: settingsForm.remoteUsername,
      password: settingsForm.remotePassword,
      path: settingsForm.remotePath,
      sshMethod: settingsForm.sshMethod,
    })
    connectionResult.value = res.data === true
    if (connectionResult.value) {
      ElMessage.success('连接成功')
    } else {
      ElMessage.error('连接失败，请检查配置')
    }
  } catch {
    connectionResult.value = false
    ElMessage.error('连接失败，请检查配置')
  } finally {
    testingConnection.value = false
  }
}

function handleSaveSettings() {
  localStorage.setItem('backupSettings', JSON.stringify(settingsForm))
  ElMessage.success('备份设置已保存')
  settingsDialogVisible.value = false
}

function loadSettings() {
  const saved = localStorage.getItem('backupSettings')
  if (saved) {
    try {
      Object.assign(settingsForm, JSON.parse(saved))
    } catch {}
  }
}

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
      result = await incrementalBackup(backupPath)
    }

    const record = result.data
    ElMessage.success('备份成功')

    // 如果是下载模式，触发浏览器下载
    if (backupForm.saveMode === 'download' && record?.fileName) {
      const url = downloadBackupUrl(record.fileName)
      const a = document.createElement('a')
      a.href = url
      a.download = record.fileName
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
    }

    backupDialogVisible.value = false
    await fetchBackupList()
  } catch {} finally {
    backing.value = false
  }
}

function handleDownload(row: BackupRecord) {
  const url = downloadBackupUrl(row.fileName)
  const a = document.createElement('a')
  a.href = url
  a.download = row.fileName
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
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
  loadSettings()
})
</script>
