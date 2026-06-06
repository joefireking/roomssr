<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="用户名"><el-input v-model="query.username" placeholder="搜索" clearable /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query.current = 1; loadData()">搜索</el-button>
          <el-button type="success" @click="showDialog()">新增用户</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="真实姓名" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="showDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />

      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.current"
        v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next"
        @current-change="loadData" @size-change="loadData" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item v-if="!form.id" label="密码"><el-input v-model="form.password" placeholder="默认：123456" /></el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio><el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const tableData = ref<any[]>([])
const roles = ref<any[]>([])
const total = ref(0)
const formRef = ref()
const query = reactive({ username: '', current: 1, size: 10 })
const form = reactive<any>({ id: null, username: '', password: '', realName: '', phone: '', email: '', status: 1, roleIds: [] })
const rules = { username: [{ required: true, message: '必填', trigger: 'blur' }] }

async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/sys/users/list', { params: query })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

async function loadRoles() {
  try {
    const res: any = await request.get('/sys/roles/all')
    roles.value = res.data
  } catch {
    roles.value = []
    ElMessage.error('加载角色列表失败')
  }
}

async function showDialog(row?: any) {
  if (row) {
    Object.assign(form, { ...row, password: '', roleIds: [] })
    try {
      const res: any = await request.get(`/sys/users/${row.id}/role-ids`)
      form.roleIds = res.data || []
    } catch { form.roleIds = [] }
  } else {
    Object.assign(form, { id: null, username: '', password: '', realName: '', phone: '', email: '', status: 1, roleIds: [] })
  }
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) await request.put(`/sys/users/${form.id}`, form)
    else await request.post('/sys/users', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  try {
    await request.delete(`/sys/users/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(() => { loadData(); loadRoles() })
</script>
