import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'
import type { SysUser, LoginForm, Result } from '@/types'

interface LoginResult {
  token: string
  userId: number
  username: string
}

interface UserInfoResult {
  user: SysUser
  roles: string[]
  permissions: string[]
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<SysUser | null>(null)
  const roles = ref<string[]>(JSON.parse(localStorage.getItem('roles') || '[]'))
  const permissions = ref<string[]>(JSON.parse(localStorage.getItem('permissions') || '[]'))

  async function login(form: LoginForm) {
    const res = await request.post('/auth/login', form) as unknown as Result<LoginResult>
    token.value = res.data.token
    localStorage.setItem('token', res.data.token)
    await getUserInfo()
    return res.data
  }

  async function getUserInfo() {
    const res = await request.get('/auth/info') as unknown as Result<UserInfoResult>
    userInfo.value = res.data.user
    roles.value = res.data.roles
    permissions.value = res.data.permissions
    localStorage.setItem('roles', JSON.stringify(res.data.roles))
    localStorage.setItem('permissions', JSON.stringify(res.data.permissions))
    return res.data
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('roles')
    localStorage.removeItem('permissions')
  }

  function hasPermission(code: string) {
    return permissions.value.includes(code)
  }

  return { token, userInfo, roles, permissions, login, getUserInfo, logout, hasPermission }
})
