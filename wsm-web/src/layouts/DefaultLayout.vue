<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo" @click="router.push('/dashboard')">
        <svg viewBox="0 0 32 32" class="logo-icon">
          <rect width="32" height="32" rx="6" fill="#3b82f6" />
          <text x="16" y="22" text-anchor="middle" fill="white" font-size="16" font-weight="bold" font-family="Arial">W</text>
        </svg>
        <transition name="fade">
          <span v-show="!appStore.sidebarCollapsed" class="logo-text">WSM 仓库管理</span>
        </transition>
      </div>

      <el-scrollbar class="menu-scrollbar">
        <el-menu
          :default-active="currentPath"
          :collapse="appStore.sidebarCollapsed"
          :collapse-transition="false"
          background-color="#1e293b"
          text-color="#94a3b8"
          active-text-color="#ffffff"
          router
        >
          <template v-for="item in MENU_LIST" :key="item.title">
            <!-- 有子菜单 -->
            <el-sub-menu v-if="item.children" :index="item.title">
              <template #title>
                <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item
                v-for="child in item.children"
                :key="child.path"
                :index="child.path"
              >
                {{ child.title }}
              </el-menu-item>
            </el-sub-menu>
            <!-- 无子菜单 -->
            <el-menu-item v-else :index="item.path">
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <!-- 右侧内容区 -->
    <el-container class="layout-main">
      <!-- 顶栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon
            class="collapse-btn"
            :size="20"
            @click="appStore.toggleSidebar()"
          >
            <component :is="appStore.sidebarCollapsed ? 'Expand' : 'Fold'" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ userStore.userInfo?.realName?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>

  <!-- 全局 AI 浮动聊天窗口 -->
  <AiFloatingChat />
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { MENU_LIST } from '@/utils/constants'
import AiFloatingChat from '@/components/AiFloatingChat.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const currentPath = computed(() => route.path)
const currentTitle = computed(() => (route.meta?.title as string) || '')

onMounted(async () => {
  try {
    await userStore.fetchProfile()
  } catch {
    // 获取用户信息失败，可能 token 过期
  }
})

function handleCommand(command: string) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    }).then(() => {
      userStore.logout()
      router.push('/login')
    })
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.layout-aside {
  background: #1e293b;
  transition: width 0.3s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  cursor: pointer;
  flex-shrink: 0;
  gap: 10px;

  .logo-icon {
    width: 32px;
    height: 32px;
    flex-shrink: 0;
  }

  .logo-text {
    color: #fff;
    font-size: 15px;
    font-weight: 600;
    white-space: nowrap;
    overflow: hidden;
  }
}

.menu-scrollbar {
  flex: 1;
  overflow: hidden;
}

.el-menu {
  border-right: none;

  :deep(.el-sub-menu__title),
  :deep(.el-menu-item) {
    height: 48px;
    line-height: 48px;

    &:hover {
      background-color: #334155 !important;
    }
  }

  :deep(.el-menu-item.is-active) {
    background-color: #3b82f6 !important;
    color: #fff !important;
    border-radius: 0;
  }
}

.layout-main {
  flex-direction: column;
  overflow: hidden;
  background: #f1f5f9;
  min-width: 0;
}

.layout-header {
  height: 56px !important;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  z-index: 10;
  flex-shrink: 0;
  box-sizing: border-box;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #64748b;
  transition: color 0.2s;

  &:hover {
    color: #3b82f6;
  }
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;

  &:hover {
    background: #f1f5f9;
  }
}

.user-avatar {
  background: #3b82f6;
  color: #fff;
  font-size: 14px;
}

.user-name {
  font-size: 14px;
  color: #334155;
}

.layout-content {
  overflow-y: auto;
  padding: 24px;
  flex: 1;
  min-height: 0;
}
</style>
