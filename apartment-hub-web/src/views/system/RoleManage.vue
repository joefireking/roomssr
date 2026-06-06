<template>
  <div>
    <el-card>
      <el-button type="success" style="margin-bottom:16px" @click="showDialog()">新增角色</el-button>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button size="small" @click="showDialog(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="showPermissionDialog(row)">权限</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="roleName"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="编码" prop="roleCode"><el-input v-model="form.roleCode" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permDialogVisible" title="分配权限" width="400px">
      <el-tree ref="treeRef" :data="permTree" show-checkbox node-key="id" :default-checked-keys="checkedKeys"
        :props="{ children: 'children', label: 'name' }" />
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermissions">保存</el-button>
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
const permDialogVisible = ref(false)
const tableData = ref<any[]>([])
const permTree = ref<any[]>([])
const checkedKeys = ref<number[]>([])
const currentRoleId = ref<number>(0)
const formRef = ref()
const treeRef = ref()
const form = reactive<any>({ id: null, roleName: '', roleCode: '', description: '', status: 1 })
const rules = {
  roleName: [{ required: true, message: '必填', trigger: 'blur' }],
  roleCode: [{ required: true, message: '必填', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/sys/roles/all')
    tableData.value = res.data
  } finally { loading.value = false }
}

async function loadPermTree() {
  try {
    const res: any = await request.get('/sys/permissions/tree')
    permTree.value = res.data
  } catch {
    permTree.value = []
    ElMessage.error('加载权限树失败')
  }
}

function showDialog(row?: any) {
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, roleName: '', roleCode: '', description: '', status: 1 })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) await request.put(`/sys/roles/${form.id}`, form)
    else await request.post('/sys/roles', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function showPermissionDialog(row: any) {
  currentRoleId.value = row.id
  permDialogVisible.value = true
  try {
    const res: any = await request.get(`/sys/roles/${row.id}/permissions`)
    checkedKeys.value = res.data || []
  } catch { checkedKeys.value = [] }
}

async function handleSavePermissions() {
  try {
    const keys = treeRef.value?.getCheckedKeys() || []
    await request.put(`/sys/roles/${currentRoleId.value}`, { ...form, permissionIds: keys })
    ElMessage.success('权限更新成功')
    permDialogVisible.value = false
  } catch {
    ElMessage.error('权限更新失败')
  }
}

async function handleDelete(id: number) {
  try {
    await request.delete(`/sys/roles/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(() => { loadData(); loadPermTree() })
</script>
