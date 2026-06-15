<template>
  <div class="image-preview" v-if="src">
    <div class="thumbnail" @click="handlePreview">
      <img :src="src" alt="缩略图" />
    </div>

    <!-- 图片预览 -->
    <el-dialog v-model="visible" title="图片预览" width="600px" destroy-on-close>
      <div class="preview-container">
        <img :src="src" alt="预览" class="preview-image" />
      </div>
    </el-dialog>
  </div>
  <span v-else class="text-gray-400">-</span>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  src?: string
}>()

const visible = ref(false)

function handlePreview() {
  visible.value = true
}
</script>

<style scoped lang="scss">
.image-preview {
  display: inline-block;
}

.thumbnail {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.preview-container {
  display: flex;
  justify-content: center;

  .preview-image {
    max-width: 100%;
    max-height: 500px;
    object-fit: contain;
  }
}
</style>
