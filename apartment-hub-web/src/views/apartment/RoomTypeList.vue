<template>
  <div>
    <el-card>
      <el-button type="success" style="margin-bottom:16px" @click="showDialog()">新增</el-button>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="typeName" label="房型名称" />
        <el-table-column prop="area" label="面积（m²）" />
        <el-table-column prop="orientation" label="朝向" />
        <el-table-column prop="basePrice" label="基础价格（元）">
          <template #default="{ row }">¥{{ row.basePrice }}</template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
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
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑' : '新增'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="房型名称" prop="typeName"><el-input v-model="form.typeName" /></el-form-item>
        <el-form-item label="面积（m²）"><el-input-number v-model="form.area" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="朝向"><el-input v-model="form.orientation" /></el-form-item>
        <el-form-item label="基础价格"><el-input-number v-model="form.basePrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
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
const formRef = ref()
const form = reactive<any>({ id: null, typeName: '', area: 0, orientation: '', basePrice: 0, description: '' })
const rules = { typeName: [{ required: true, message: '必填', trigger: 'blur' }] }

async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/room-types/all')
    tableData.value = res.data
  } finally { loading.value = false }
}

function showDialog(row?: any) {
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, typeName: '', area: 0, orientation: '', basePrice: 0, description: '' })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) await request.put(`/room-types/${form.id}`, form)
    else await request.post('/room-types', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  try {
    await request.delete(`/room-types/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(loadData)
</script>
