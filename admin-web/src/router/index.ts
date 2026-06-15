import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/home',
    children: [
      {
        path: '/home',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '运营数据', icon: 'HomeFilled' }
      },
      {
        path: '/admission-review',
        name: 'AdmissionReview',
        component: () => import('@/views/admission-review/index.vue'),
        meta: { title: '入驻审核', icon: 'UserFilled', parent: 'review-management' }
      },
      {
        path: '/skill-review',
        name: 'SkillReview',
        component: () => import('@/views/skill-review/index.vue'),
        meta: { title: '技能审核', icon: 'Medal', parent: 'review-management' }
      },
      {
        path: '/tag-management',
        name: 'TagManagement',
        component: () => import('@/views/tag-management/index.vue'),
        meta: { title: '评价标签', icon: 'PriceTag', parent: 'evaluation-management' }
      },
      {
        path: '/user-review',
        name: 'UserReview',
        component: () => import('@/views/user-review/index.vue'),
        meta: { title: '用户评价', icon: 'ChatDotRound', parent: 'evaluation-management' }
      },
      {
        path: '/nurse-management',
        name: 'NurseManagement',
        component: () => import('@/views/nurse-management/index.vue'),
        meta: { title: '护工管理', icon: 'User' }
      },
      {
        path: '/order-management',
        name: 'OrderManagement',
        component: () => import('@/views/order-management/index.vue'),
        meta: { title: '订单管理', icon: 'Document' }
      },
      {
        path: '/skill-management',
        name: 'SkillManagement',
        component: () => import('@/views/skill-management/index.vue'),
        meta: { title: '技能管理', icon: 'DataAnalysis' }
      },
      {
        path: '/service-package',
        name: 'ServicePackage',
        component: () => import('@/views/service-package/index.vue'),
        meta: { title: '服务包管理', icon: 'Box' }
      },
      {
        path: '/knowledge-base',
        name: 'KnowledgeBase',
        component: () => import('@/views/knowledge-base/index.vue'),
        meta: { title: '知识库管理', icon: 'Reading' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.path === '/login') {
    next()
  } else {
    if (token) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
