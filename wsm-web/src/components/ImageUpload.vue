<template>
  <div class="image-upload">
    <div v-if="modelValue" class="image-preview" @click="handlePreview">
      <img :src="modelValue" alt="图片" />
      <div class="image-actions">
        <el-icon class="action-icon" @click.stop="handlePreview"><ZoomIn /></el-icon>
        <el-icon class="action-icon delete" @click.stop="handleDelete"><Delete /></el-icon>
      </div>
    </div>
    <el-upload
      v-else
      :action="uploadUrl"
      :headers="uploadHeaders"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      accept="image/*"
    >
      <div class="upload-trigger">
        <el-icon :size="24"><Plus /></el-icon>
        <span class="upload-text">{{ placeholder }}</span>
      </div>
    </el-upload>

    <!-- 图片预览 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="600px" destroy-on-close>
      <div class="preview-container">
        <img :src="modelValue" alt="预览" class="preview-image" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { ZoomIn, Delete, Plus } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  maxSize?: number
}>(), {
  modelValue: '',
  placeholder: '点击上传图片',
  maxSize: 5
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const previewVisible = ref(false)

const uploadUrl = 'http://localhost:8080/api/images/upload'
// 使用computed确保token刷新后headers也能更新
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token') || ''}`
}))

function beforeUpload(file: File) {
  const isImage = file.type.startsWith('image/')
  const isLtMax = file.size / 1024 / 1024 < props.maxSize

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLtMax) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB!`)
    return false
  }
  return true
}

function handleSuccess(response: any) {
  if (response.code === 200) {
    emit('update:modelValue', response.data)
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleError() {
  ElMessage.error('上传失败')
}

function handlePreview() {
  previewVisible.value = true
}

function handleDelete() {
  emit('update:modelValue', '')
}
</script>

<style scoped lang="scss">
.image-upload {
  display: inline-block;
}

.image-preview {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .image-actions {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    opacity: 0;
    transition: opacity 0.3s;

    &:hover {
      opacity: 1;
    }

    .action-icon {
      color: #fff;
      font-size: 18px;
      cursor: pointer;

      &:hover {
        color: #409eff;
      }

      &.delete:hover {
        color: #f56c6c;
      }
    }
  }
}

.upload-trigger {
  width: 100px;
  height: 100px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.3s;

  &:hover {
    border-color: #409eff;
  }

  .upload-text {
    margin-top: 8px;
    font-size: 12px;
    color: #999;
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
