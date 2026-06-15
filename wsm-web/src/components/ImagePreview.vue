<template>
  <div class="image-preview" v-if="src">
    <div class="thumbnail" @click="handlePreview">
      <img :src="src" alt="缩略图" @error="handleError" @load="handleLoad" />
    </div>

    <!-- 图片预览 -->
    <el-dialog v-model="visible" title="图片预览" width="600px" destroy-on-close append-to-body>
      <div class="preview-container">
        <img :src="src" alt="预览" class="preview-image" @error="handleError" />
      </div>
    </el-dialog>
  </div>
  <span v-else class="text-gray-400">-</span>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  src?: string
}>()

const visible = ref(false)

function handlePreview() {
  visible.value = true
}

function handleError(e: Event) {
  console.error('图片加载失败:', props.src, e)
}

function handleLoad() {
  console.log('图片加载成功:', props.src)
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
