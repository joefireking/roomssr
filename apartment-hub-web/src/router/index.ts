import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'

const Layout = () => import('@/layout/index.vue')

function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp * 1000 < Date.now()
  } catch {
    return true
  }
}

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/login/NotFound.vue'),
    meta: { title: '404', hidden: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      }
    ]
  },
  {
    path: '/apartment',
    component: Layout,
    redirect: '/apartment/list',
    meta: { title: '房产管理', icon: 'OfficeBuilding' },
    children: [
      {
        path: 'list',
        name: 'ApartmentList',
        component: () => import('@/views/apartment/ApartmentList.vue'),
        meta: { title: '公寓列表' }
      },
      {
        path: 'building',
        name: 'BuildingList',
        component: () => import('@/views/apartment/BuildingList.vue'),
        meta: { title: '楼栋管理' }
      },
      {
        path: 'room',
        name: 'RoomList',
        component: () => import('@/views/apartment/RoomList.vue'),
        meta: { title: '房间管理' }
      },
      {
        path: 'room-type',
        name: 'RoomTypeList',
        component: () => import('@/views/apartment/RoomTypeList.vue'),
        meta: { title: '房型管理' }
      },
      {
        path: 'discover',
        name: 'RoomDiscover',
        component: () => import('@/views/apartment/RoomDiscover.vue'),
        meta: { title: 'Room Discovery' }
      }
    ]
  },
  {
    path: '/tenant',
    component: Layout,
    children: [
      {
        path: 'list',
        name: 'TenantList',
        component: () => import('@/views/tenant/TenantList.vue'),
        meta: { title: '租户管理', icon: 'User' }
      }
    ]
  },
  {
    path: '/contract',
    component: Layout,
    redirect: '/contract/list',
    meta: { title: '合同管理', icon: 'Document' },
    children: [
      {
        path: 'list',
        name: 'ContractList',
        component: () => import('@/views/contract/ContractList.vue'),
        meta: { title: '合同列表' }
      },
      {
        path: 'create',
        name: 'ContractCreate',
        component: () => import('@/views/contract/ContractCreate.vue'),
        meta: { title: '新建合同' }
      }
    ]
  },
  {
    path: '/bill',
    component: Layout,
    children: [
      {
        path: 'list',
        name: 'BillList',
        component: () => import('@/views/bill/BillList.vue'),
        meta: { title: '账单管理', icon: 'Money' }
      }
    ]
  },
  {
    path: '/checkout',
    component: Layout,
    children: [
      {
        path: 'list',
        name: 'CheckoutList',
        component: () => import('@/views/checkout/CheckoutList.vue'),
        meta: { title: '退租管理', icon: 'SwitchButton' }
      }
    ]
  },
  {
    path: '/repair',
    component: Layout,
    children: [
      {
        path: 'list',
        name: 'RepairOrderList',
        component: () => import('@/views/repair/RepairOrderList.vue'),
        meta: { title: '报修管理', icon: 'SetUp' }
      }
    ]
  },
  {
    path: '/report',
    component: Layout,
    redirect: '/report/revenue',
    meta: { title: '数据报表', icon: 'DataAnalysis' },
    children: [
      {
        path: 'revenue',
        name: 'RevenueReport',
        component: () => import('@/views/report/RevenueReport.vue'),
        meta: { title: '收入报表' }
      },
      {
        path: 'occupancy',
        name: 'OccupancyReport',
        component: () => import('@/views/report/OccupancyReport.vue'),
        meta: { title: '入住率报表' }
      }
    ]
  },
  {
    path: '/announcement',
    component: Layout,
    children: [
      {
        path: 'list',
        name: 'AnnouncementList',
        component: () => import('@/views/announcement/AnnouncementList.vue'),
        meta: { title: 'Announcement Management', icon: 'Bell' }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    redirect: '/system/user',
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('@/views/system/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'role',
        name: 'RoleManage',
        component: () => import('@/views/system/RoleManage.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'dict',
        name: 'DictManage',
        component: () => import('@/views/system/DictManage.vue'),
        meta: { title: '字典管理' }
      },
      {
        path: 'log',
        name: 'OperationLog',
        component: () => import('@/views/system/OperationLog.vue'),
        meta: { title: '操作日志' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/404', meta: { hidden: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path === '/login') {
    if (token && !isTokenExpired(token)) {
      next('/')
      return
    }
    next()
  } else if (!token || isTokenExpired(token)) {
    localStorage.removeItem('token')
    ElMessage.warning('登录已过期，请重新登录')
    next('/login')
  } else {
    document.title = ((to.meta.title as string) || '公寓管理系统') + ' - 公寓管理系统'
    next()
  }
})

export default router
