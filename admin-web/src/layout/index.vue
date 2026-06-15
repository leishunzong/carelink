<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '200px'" class="layout-aside">
      <div class="logo-container">
        <span v-if="!isCollapse" class="logo-title">护联管理系统</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :router="false"
        background-color="#001529"
        text-color="#fff"
        active-text-color="#1890ff"
        @select="handleMenuSelect"
      >
        <template v-for="item in menuList" :key="item.path">
          <!-- 有子菜单的情况 -->
          <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
            <template #title>
              <el-icon><component :is="item.meta.icon" /></el-icon>
              <span>{{ item.meta.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
            >
              <el-icon><component :is="child.meta.icon" /></el-icon>
              <template #title>{{ child.meta.title }}</template>
            </el-menu-item>
          </el-sub-menu>
          
          <!-- 无子菜单的情况 -->
          <el-menu-item
            v-else
            :index="item.path"
          >
            <el-icon><component :is="item.meta.icon" /></el-icon>
            <template #title>{{ item.meta.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主体区域 -->
    <el-container>
      <!-- 顶栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-icon" @click="toggleCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><Avatar /></el-icon>
              <span class="username">管理员</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区域 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component, route }">
          <transition name="fade" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

const isCollapse = ref(false)

// 定义菜单结构（用于显示，与路由配置独立）
const menuConfig = [
  {
    path: '/home',
    meta: { title: '首页', icon: 'HomeFilled' }
  },
  {
    path: 'review-management',
    meta: { title: '审核管理', icon: 'Operation' },
    children: [
      { path: '/admission-review', meta: { title: '入驻审核', icon: 'UserFilled' } },
      { path: '/skill-review', meta: { title: '技能审核', icon: 'Medal' } }
    ]
  },
  {
    path: 'evaluation-management',
    meta: { title: '评价管理', icon: 'Star' },
    children: [
      { path: '/tag-management', meta: { title: '评价标签', icon: 'PriceTag' } },
      { path: '/user-review', meta: { title: '用户评价', icon: 'ChatDotRound' } }
    ]
  },
  {
    path: '/nurse-management',
    meta: { title: '护工管理', icon: 'User' }
  },
  {
    path: '/order-management',
    meta: { title: '订单管理', icon: 'Document' }
  },
  {
    path: '/skill-management',
    meta: { title: '技能管理', icon: 'DataAnalysis' }
  },
  {
    path: '/service-package',
    meta: { title: '服务包管理', icon: 'Box' }
  },
  {
    path: '/knowledge-base',
    meta: { title: '知识库管理', icon: 'Reading' }
  }
]

// 菜单列表
const menuList = computed(() => menuConfig)

// 当前激活的菜单
const activeMenu = computed(() => {
  return route.path
})

// 当前页面标题
const currentTitle = computed(() => {
  // 查找当前路由对应的菜单项
  const findMenuItem = (items: any[]): any => {
    for (const item of items) {
      if (item.path === route.path) {
        return item
      }
      if (item.children) {
        const found = findMenuItem(item.children)
        if (found) return found
      }
    }
    return null
  }
  
  const current = findMenuItem(menuList.value)
  return current?.meta?.title || '首页'
})

// 切换侧边栏折叠状态
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 菜单选择事件
const handleMenuSelect = (index: string) => {
  router.push(index)
}

// 下拉菜单命令处理
const handleCommand = (command: string) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      localStorage.removeItem('token')
      router.push('/login')
      ElMessage.success('退出成功')
    }).catch(() => {})
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  width: 100%;
  height: 100vh;
}

.layout-aside {
  background-color: #001529;
  transition: width 0.3s;
  overflow-x: hidden;

  .logo-container {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;
    background-color: #002140;

    .logo-title {
      color: #fff;
      font-size: 18px;
      font-weight: 600;
      white-space: nowrap;
    }
  }

  .el-menu {
    border-right: none;
  }
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-icon {
      font-size: 20px;
      cursor: pointer;
      transition: color 0.3s;

      &:hover {
        color: #1890ff;
      }
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;

      .username {
        font-size: 14px;
      }
    }
  }
}

.layout-main {
  background-color: #f0f2f5;
  padding: 24px;
  overflow-y: auto;
}

// 页面切换动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
