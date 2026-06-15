<template>
  <div class="date-range-picker">
    <div class="quick-buttons">
      <el-button
        v-for="item in quickOptions"
        :key="item.value"
        :type="activeQuick === item.value ? 'primary' : ''"
        size="small"
        @click="handleQuickSelect(item.value)"
      >
        {{ item.label }}
      </el-button>
    </div>
    <el-date-picker
      v-model="dateRange"
      type="datetimerange"
      range-separator="至"
      start-placeholder="开始时间"
      end-placeholder="结束时间"
      value-format="YYYY-MM-DD HH:mm:ss"
      :style="{ width: width }"
      @change="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface QuickOption {
  label: string
  value: string
}

const props = withDefaults(defineProps<{
  modelValue?: string[]
  width?: string
}>(), {
  modelValue: () => [],
  width: '360px'
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void
  (e: 'change', value: string[]): void
}>()

const quickOptions: QuickOption[] = [
  { label: '今天', value: 'today' },
  { label: '昨天', value: 'yesterday' },
  { label: '近7天', value: 'last7days' },
  { label: '近30天', value: 'last30days' },
  { label: '本月', value: 'thisMonth' },
  { label: '上月', value: 'lastMonth' },
  { label: '本季度', value: 'thisQuarter' },
  { label: '上季度', value: 'lastQuarter' },
  { label: '本年', value: 'thisYear' },
  { label: '去年', value: 'lastYear' },
]

const activeQuick = ref<string>('')
const dateRange = ref<string[]>(props.modelValue)

watch(() => props.modelValue, (val) => {
  dateRange.value = val
})

function getDateRange(type: string): string[] {
  const now = new Date()
  const formatDate = (date: Date) => {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d} 00:00:00`
  }
  const formatEndTime = (date: Date) => {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d} 23:59:59`
  }

  let start: Date
  let end: Date

  switch (type) {
    case 'today':
      start = new Date(now)
      end = new Date(now)
      break
    case 'yesterday':
      start = new Date(now)
      start.setDate(start.getDate() - 1)
      end = new Date(start)
      break
    case 'last7days':
      start = new Date(now)
      start.setDate(start.getDate() - 6)
      end = new Date(now)
      break
    case 'last30days':
      start = new Date(now)
      start.setDate(start.getDate() - 29)
      end = new Date(now)
      break
    case 'thisMonth':
      start = new Date(now.getFullYear(), now.getMonth(), 1)
      end = new Date(now)
      break
    case 'lastMonth':
      start = new Date(now.getFullYear(), now.getMonth() - 1, 1)
      end = new Date(now.getFullYear(), now.getMonth(), 0)
      break
    case 'thisQuarter':
      const thisQuarter = Math.floor(now.getMonth() / 3)
      start = new Date(now.getFullYear(), thisQuarter * 3, 1)
      end = new Date(now)
      break
    case 'lastQuarter':
      const lastQuarter = Math.floor(now.getMonth() / 3) - 1
      if (lastQuarter < 0) {
        start = new Date(now.getFullYear() - 1, 9, 1)
        end = new Date(now.getFullYear() - 1, 11, 31)
      } else {
        start = new Date(now.getFullYear(), lastQuarter * 3, 1)
        end = new Date(now.getFullYear(), lastQuarter * 3 + 3, 0)
      }
      break
    case 'thisYear':
      start = new Date(now.getFullYear(), 0, 1)
      end = new Date(now)
      break
    case 'lastYear':
      start = new Date(now.getFullYear() - 1, 0, 1)
      end = new Date(now.getFullYear() - 1, 11, 31)
      break
    default:
      return []
  }

  return [formatDate(start), formatEndTime(end)]
}

function handleQuickSelect(type: string) {
  activeQuick.value = type
  const range = getDateRange(type)
  dateRange.value = range
  emit('update:modelValue', range)
  emit('change', range)
}

function handleChange(value: string[]) {
  activeQuick.value = ''
  emit('update:modelValue', value || [])
  emit('change', value || [])
}
</script>

<style scoped lang="scss">
.date-range-picker {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quick-buttons {
  display: flex;
  gap: 4px;

  .el-button {
    padding: 5px 8px;
    font-size: 12px;
  }
}
</style>
