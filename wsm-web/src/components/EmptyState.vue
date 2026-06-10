<template>
  <div class="empty-state">
    <el-empty :description="description" :image-size="120">
      <slot />
    </el-empty>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ description?: string }>(), {
  description: '暂无数据',
})
</script>

<style scoped lang="scss">
.empty-state {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}
</style>
