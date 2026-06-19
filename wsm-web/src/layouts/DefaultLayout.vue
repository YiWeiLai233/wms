<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside
      :width="appStore.sidebarCollapsed ? '64px' : '220px'"
      class="layout-aside"
    >
      <div class="logo" @click="router.push('/dashboard')">
        <div class="logo-icon-wrap">
          <svg viewBox="0 0 32 32" class="logo-icon">
            <defs>
              <linearGradient id="logoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#60a5fa" />
                <stop offset="100%" style="stop-color:#3b82f6" />
              </linearGradient>
            </defs>
            <rect width="32" height="32" rx="8" fill="url(#logoGrad)" />
            <text x="16" y="22" text-anchor="middle" fill="white" font-size="16" font-weight="bold" font-family="Arial">W</text>
          </svg>
        </div>
        <transition name="fade">
          <span v-show="!appStore.sidebarCollapsed" class="logo-text">WSM 仓库管理</span>
        </transition>
      </div>

      <el-scrollbar class="menu-scrollbar">
        <el-menu
          :default-active="currentPath"
          :collapse="appStore.sidebarCollapsed"
          :collapse-transition="false"
          background-color="transparent"
          text-color="#94a3b8"
          active-text-color="#ffffff"
          router
        >
          <template v-for="item in filteredMenuList" :key="item.title">
            <!-- 有子菜单 -->
            <el-sub-menu v-if="item.children" :index="item.title" popper-class="sidebar-popper">
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

      <!-- 底部折叠按钮 -->
      <div class="sidebar-collapse-btn" @click="appStore.toggleSidebar()">
        <el-icon :size="18">
          <component :is="appStore.sidebarCollapsed ? 'Expand' : 'Fold'" />
        </el-icon>
      </div>

      <!-- 侧边栏边缘拖拽按钮 -->
      <div
        class="sidebar-drag-handle"
        :class="{ collapsed: appStore.sidebarCollapsed }"
        @click="appStore.toggleSidebar()"
      >
        <div class="drag-indicator">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
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
import type { MenuItem } from '@/utils/constants'
import AiFloatingChat from '@/components/AiFloatingChat.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const currentPath = computed(() => route.path)
const currentTitle = computed(() => (route.meta?.title as string) || '')

// 根据权限过滤菜单
const filteredMenuList = computed(() => {
  return MENU_LIST.map(item => {
    if (item.children) {
      const filteredChildren = item.children.filter(child => {
        if (!child.permission) return true
        return userStore.hasPermission(child.permission)
      })
      if (filteredChildren.length === 0) return null
      return { ...item, children: filteredChildren }
    }
    if (item.permission && !userStore.hasPermission(item.permission)) return null
    return item
  }).filter(Boolean) as MenuItem[]
})

onMounted(async () => {
  try {
    await userStore.fetchProfile()
  } catch {
    // 获取用户信息失败，可能 token 过期
  }
})

async function handleCommand(command: string) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录吗？', '提示', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      })
      // 调用后端登出接口，使当前 token 失效
      try {
        const { logout } = await import('@/api/auth')
        await logout()
      } catch {}
      userStore.logout()
      router.push('/login')
    } catch {}
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.layout-aside {
  background: linear-gradient(180deg, #1a2332 0%, #1e293b 100%);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: visible;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  position: relative;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.15);
}

// Logo 区域
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  cursor: pointer;
  flex-shrink: 0;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);

  .logo-icon-wrap {
    width: 36px;
    height: 36px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    background: rgba(59, 130, 246, 0.15);
    padding: 2px;
  }

  .logo-icon {
    width: 32px;
    height: 32px;
  }

  .logo-text {
    color: #fff;
    font-size: 16px;
    font-weight: 700;
    white-space: nowrap;
    overflow: hidden;
    letter-spacing: 0.02em;
  }
}

// 菜单滚动区
.menu-scrollbar {
  flex: 1;
  overflow: hidden;
  padding: 8px 0;
}

// 菜单样式
.el-menu {
  border-right: none;
  padding: 0 8px;

  // 子菜单标题
  :deep(.el-sub-menu__title) {
    height: 44px;
    line-height: 44px;
    margin: 2px 0;
    border-radius: 8px;
    transition: all 0.2s ease;
    font-size: 13px;
    color: #94a3b8 !important;

    &:hover {
      background-color: rgba(255, 255, 255, 0.06) !important;
      color: #cbd5e1 !important;
    }

    .el-icon {
      font-size: 17px;
      margin-right: 10px;
    }
  }

  // 子菜单展开时标题高亮
  :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
    color: #e2e8f0 !important;
    background-color: rgba(255, 255, 255, 0.04) !important;
  }

  // 菜单项
  :deep(.el-menu-item) {
    height: 40px;
    line-height: 40px;
    margin: 1px 0;
    border-radius: 8px;
    transition: all 0.2s ease;
    font-size: 13px;
    padding-left: 52px !important;

    &:hover {
      background-color: rgba(255, 255, 255, 0.06) !important;
      color: #cbd5e1 !important;
    }

    // 活跃状态 — 左边蓝色指示条 + 蓝色背景
    &.is-active {
      background: linear-gradient(90deg, rgba(59, 130, 246, 0.2) 0%, rgba(59, 130, 246, 0.08) 100%) !important;
      color: #fff !important;
      position: relative;
      font-weight: 500;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 8px;
        bottom: 8px;
        width: 3px;
        background: var(--color-primary);
        border-radius: 0 2px 2px 0;
      }
    }
  }

  // 一级菜单项（无子菜单）
  :deep(.el-menu-item:first-child) {
    padding-left: 20px !important;
  }
}

// 折叠按钮
.sidebar-collapse-btn {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #64748b;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  transition: all 0.2s;
  flex-shrink: 0;

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    color: #94a3b8;
  }
}

// 拖拽手柄
.sidebar-drag-handle {
  position: absolute;
  right: -16px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 48px;
  background: var(--color-primary);
  border-radius: 0 6px 6px 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 100;
  box-shadow: 2px 0 8px rgba(59, 130, 246, 0.3);

  &:hover {
    opacity: 1;
    right: -20px;
    width: 20px;
    background: var(--color-primary-dark);
  }

  .drag-indicator {
    display: flex;
    flex-direction: column;
    gap: 3px;
    align-items: center;

    span {
      width: 3px;
      height: 3px;
      background: rgba(255, 255, 255, 0.8);
      border-radius: 50%;
    }
  }
}

.layout-aside:hover .sidebar-drag-handle {
  opacity: 0.6;
}

// 主内容区
.layout-main {
  flex-direction: column;
  overflow: hidden;
  background: var(--color-bg-page);
  min-width: 0;
}

.layout-header {
  height: 56px !important;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: var(--shadow-header);
  z-index: 10;
  flex-shrink: 0;
  box-sizing: border-box;
  border-bottom: 1px solid var(--color-border-light);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: color var(--transition-fast);

  &:hover {
    color: var(--color-primary);
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
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);

  &:hover {
    background: var(--color-bg-page);
  }
}

.user-avatar {
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
}

.user-name {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.layout-content {
  overflow-y: auto;
  padding: 20px;
  flex: 1;
  min-height: 0;
}
</style>

<style lang="scss">
// 全局：侧边栏弹出子菜单样式
.sidebar-popper {
  .el-menu {
    padding: 4px;
    background: #1e293b !important;
    border: 1px solid rgba(255, 255, 255, 0.08) !important;
    border-radius: 8px !important;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3) !important;
  }

  .el-menu-item {
    height: 38px !important;
    line-height: 38px !important;
    border-radius: 6px !important;
    font-size: 13px !important;
    color: #94a3b8 !important;

    &:hover {
      background: rgba(255, 255, 255, 0.08) !important;
      color: #e2e8f0 !important;
    }

    &.is-active {
      background: rgba(59, 130, 246, 0.2) !important;
      color: #fff !important;
    }
  }
}
</style>
