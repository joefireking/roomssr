<template>
  <div>
    <el-card>
      <el-button type="success" style="margin-bottom:16px" @click="showDialog()">新增字典</el-button>
      <el-form inline style="margin-bottom:16px">
        <el-form-item label="类型">
          <el-select v-model="filterType" placeholder="全部" clearable @change="loadData">
            <el-option v-for="t in dictTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="dictType" label="类型" />
        <el-table-column prop="dictCode" label="编码" />
        <el-table-column prop="dictLabel" label="标签" />
        <el-table-column prop="sortOrder" label="排序" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑字典' : '新增字典'" width="400px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="类型" prop="dictType"><el-input v-model="form.dictType" /></el-form-item>
        <el-form-item label="编码" prop="dictCode"><el-input v-model="form.dictCode" /></el-form-item>
        <el-form-item label="标签" prop="dictLabel"><el-input v-model="form.dictLabel" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const dialogVisible = ref(false)
const tableData = ref<any[]>([])
const filterType = ref('')
const dictTypes = ref<string[]>([])
const formRef = ref()
const form = reactive<any>({ id: null, dictType: '', dictCode: '', dictLabel: '', sortOrder: 0, status: 1 })
const rules = {
  dictType: [{ required: true, message: '必填', trigger: 'blur' }],
  dictCode: [{ required: true, message: '必填', trigger: 'blur' }],
  dictLabel: [{ required: true, message: '必填', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/sys/dicts/list', { params: filterType.value ? { dictType: filterType.value } : {} })
    tableData.value = res.data
    const types = new Set(res.data.map((d: any) => d.dictType))
    dictTypes.value = Array.from(types) as string[]
  } finally { loading.value = false }
}

function showDialog(row?: any) {
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, dictType: '', dictCode: '', dictLabel: '', sortOrder: 0, status: 1 })
  dialogVisible.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
    if (form.id) await request.put(`/sys/dicts/${form.id}`, form)
    else await request.post('/sys/dicts', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('保存失败')
  }
}

async function handleDelete(id: number) {
  try {
    await request.delete(`/sys/dicts/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('Delete failed')
  }
}

onMounted(loadData)
</script>
