<template>
  <div class="stat-card" :class="{ 'stat-card--clickable': to }" :style="{ borderTop: `3px solid ${color}` }" @click="handleClick">
    <div class="stat-card__content">
      <div class="stat-card__value">{{ value }}</div>
      <div class="stat-card__label">{{ label }}</div>
    </div>
    <div class="stat-card__icon" :style="{ backgroundColor: color + '15', color }">
      <el-icon :size="28"><component :is="icon" /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const props = defineProps<{
  label: string
  value: string | number
  icon: string
  color?: string
  to?: string
}>()

const router = useRouter()

function handleClick() {
  if (props.to) {
    router.push(props.to)
  }
}
</script>

<style scoped lang="scss">
.stat-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: var(--shadow-card);
  transition: transform 0.2s, box-shadow 0.2s;

  &--clickable {
    cursor: pointer;

    &:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-card-hover);
    }

    &:active {
      transform: translateY(0);
    }
  }

  &__value {
    font-size: 28px;
    font-weight: 700;
    color: var(--color-text-primary);
    line-height: 1.2;
  }

  &__label {
    font-size: 14px;
    color: var(--color-text-secondary);
    margin-top: 4px;
  }

  &__icon {
    width: 56px;
    height: 56px;
    border-radius: var(--radius-lg);
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
