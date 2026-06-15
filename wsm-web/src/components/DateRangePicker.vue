<template>
  <el-date-picker
    v-model="dateRange"
    type="datetimerange"
    range-separator="至"
    start-placeholder="开始时间"
    end-placeholder="结束时间"
    value-format="YYYY-MM-DD HH:mm:ss"
    :shortcuts="shortcuts"
    :style="{ width: width }"
    @change="handleChange"
  />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

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

const shortcuts = [
  {
    text: '今天',
    value: () => {
      const now = new Date()
      return [now, now]
    },
  },
  {
    text: '昨天',
    value: () => {
      const now = new Date()
      const start = new Date(now)
      start.setDate(start.getDate() - 1)
      return [start, start]
    },
  },
  {
    text: '近7天',
    value: () => {
      const now = new Date()
      const start = new Date(now)
      start.setDate(start.getDate() - 6)
      return [start, now]
    },
  },
  {
    text: '近30天',
    value: () => {
      const now = new Date()
      const start = new Date(now)
      start.setDate(start.getDate() - 29)
      return [start, now]
    },
  },
  {
    text: '本月',
    value: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth(), 1)
      return [start, now]
    },
  },
  {
    text: '上月',
    value: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth() - 1, 1)
      const end = new Date(now.getFullYear(), now.getMonth(), 0)
      return [start, end]
    },
  },
  {
    text: '本季度',
    value: () => {
      const now = new Date()
      const quarter = Math.floor(now.getMonth() / 3)
      const start = new Date(now.getFullYear(), quarter * 3, 1)
      return [start, now]
    },
  },
  {
    text: '上季度',
    value: () => {
      const now = new Date()
      let quarter = Math.floor(now.getMonth() / 3) - 1
      let year = now.getFullYear()
      if (quarter < 0) {
        quarter = 3
        year -= 1
      }
      const start = new Date(year, quarter * 3, 1)
      const end = new Date(year, quarter * 3 + 3, 0)
      return [start, end]
    },
  },
  {
    text: '本年',
    value: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), 0, 1)
      return [start, now]
    },
  },
  {
    text: '去年',
    value: () => {
      const now = new Date()
      const start = new Date(now.getFullYear() - 1, 0, 1)
      const end = new Date(now.getFullYear() - 1, 11, 31)
      return [start, end]
    },
  },
]

const dateRange = ref<string[]>(props.modelValue)

watch(() => props.modelValue, (val) => {
  dateRange.value = val
})

function handleChange(value: string[]) {
  emit('update:modelValue', value || [])
  emit('change', value || [])
}
</script>
