<template>
  <div class="page-header">
    <div class="page-header__left">
      <div class="page-header__indicator"></div>
      <div>
        <h2 class="page-header__title">{{ title }}</h2>
        <span v-if="subtitle" class="page-header__subtitle">{{ subtitle }}</span>
      </div>
    </div>
    <div class="page-header__right">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border-light);

  &__left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__indicator {
    width: 4px;
    height: 24px;
    background: var(--color-primary);
    border-radius: 2px;
    flex-shrink: 0;
  }

  &__title {
    font-size: 18px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin: 0;
    letter-spacing: -0.01em;
  }

  &__subtitle {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-top: 2px;
    display: block;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}
</style>
