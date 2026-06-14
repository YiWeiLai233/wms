<template>
  <div class="action-confirm-card" :class="{ high: action.riskLevel === 'HIGH' }">
    <div class="action-header">
      <div>
        <div class="action-name">{{ action.actionName }}</div>
        <div class="action-type">{{ action.actionType }}</div>
      </div>
      <el-tag :type="riskTagType" effect="dark">{{ action.riskLevel }}</el-tag>
    </div>

    <div v-if="action.summary" class="summary">{{ action.summary }}</div>

    <div class="detail-grid">
      <div class="detail-label">状态</div>
      <div>{{ action.status }}</div>
      <div class="detail-label">过期时间</div>
      <div>{{ action.expireAt || '-' }}</div>
      <div class="detail-label">参数</div>
      <pre>{{ formattedParams }}</pre>
    </div>

    <div class="actions">
      <el-button :loading="loading" @click="$emit('cancel', action)">取消</el-button>
      <el-button :type="action.riskLevel === 'HIGH' ? 'danger' : 'primary'" :loading="loading" @click="$emit('confirm', action)">
        {{ action.riskLevel === 'HIGH' ? '确认执行高风险操作' : '确认执行' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AiPendingAction } from '@/api/ai'

const props = defineProps<{
  action: AiPendingAction
  loading?: boolean
}>()

defineEmits<{
  confirm: [action: AiPendingAction]
  cancel: [action: AiPendingAction]
}>()

const riskTagType = computed(() => {
  if (props.action.riskLevel === 'HIGH') return 'danger'
  if (props.action.riskLevel === 'MEDIUM') return 'warning'
  return 'success'
})

const formattedParams = computed(() => JSON.stringify(props.action.requestParams || {}, null, 2))
</script>

<style scoped lang="scss">
.action-confirm-card {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
  color: #1f2937;

  &.high {
    border-color: #fecaca;
    background: #fef2f2;
  }
}

.action-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.action-name {
  font-size: 14px;
  font-weight: 600;
}

.action-type {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.summary {
  margin-top: 10px;
  line-height: 1.6;
  font-size: 13px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 8px 10px;
  margin-top: 10px;
  font-size: 12px;
}

.detail-label {
  color: #64748b;
}

pre {
  max-height: 180px;
  margin: 0;
  padding: 8px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  white-space: pre-wrap;
  word-break: break-word;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
