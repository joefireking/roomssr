<template>
  <el-container class="layout-container">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="aside">
      <div class="logo">
        <div class="logo-mark"><OfficeBuilding /></div>
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">公寓管理系统</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="appStore.sidebarCollapsed"
        router
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#1890ff"
        :collapse-transition="false"
      >
        <template v-for="item in menuRoutes" :key="item.path">
          <el-sub-menu v-if="item.children && item.children.length > 1" :index="item.path">
            <template #title>
              <el-icon v-if="item.meta?.icon"><component :is="item.meta.icon" /></el-icon>
              <span>{{ item.meta?.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="item.path + '/' + child.path"
            >
              {{ child.meta?.title }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="getMenuPath(item)">
            <el-icon v-if="getMenuMeta(item)?.icon"><component :is="getMenuMeta(item)?.icon" /></el-icon>
            <template #title>{{ getMenuMeta(item)?.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item">{{ item }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <span class="avatar">{{ userInitial }}</span>
              <span class="user-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { constantRoutes } from '@/router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import type { RouteRecordRaw } from 'vue-router'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const menuRoutes = computed(() =>
  constantRoutes.filter(r => !r.meta?.hidden && r.path !== '/login' && r.path !== '/404')
)

function getMenuPath(item: RouteRecordRaw) {
  if (item.children && item.children.length === 1) {
    return item.path + '/' + item.children[0].path
  }
  return item.redirect as string || item.path
}

function getMenuMeta(item: RouteRecordRaw) {
  if (item.children && item.children.length === 1) {
    return item.children[0].meta
  }
  return item.meta
}

const breadcrumbs = computed(() => {
  return route.matched.filter(r => r.meta?.title).map(r => r.meta.title as string)
})

const userInitial = computed(() => {
  const name = userStore.userInfo?.realName || userStore.userInfo?.username || 'U'
  return name.slice(0, 1).toUpperCase()
})

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.getUserInfo()
    } catch {
      // handled by interceptor
    }
  }
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background: linear-gradient(180deg, #162033 0%, #1f2937 52%, #26331f 100%);
  transition: width 0.3s;
  overflow: hidden;
  box-shadow: 10px 0 30px rgba(15, 23, 42, 0.16);
}
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}
.logo-text {
  white-space: nowrap;
  letter-spacing: 0;
}
.logo-mark {
  display: inline-flex;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  background: linear-gradient(135deg, #2f80ed, #27ae60);
  border-radius: 8px;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.35);
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(10px);
  box-shadow: 0 1px 0 rgba(148, 163, 184, 0.22);
  padding: 0 22px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #475569;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #1f2937;
  font-weight: 600;
}
.avatar {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: #2563eb;
  border-radius: 50%;
  font-size: 13px;
}
.user-name {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.main {
  min-height: calc(100vh - 64px);
  background: transparent;
  padding: 18px 20px 24px;
  overflow-y: auto;
}
:deep(.el-menu) {
  border-right: none;
  background: transparent;
}
:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  margin: 4px 10px;
  border-radius: 8px;
}
:deep(.el-menu-item.is-active) {
  background: rgba(37, 99, 235, 0.18);
  color: #fff;
}
</style>
