<template>
  <div>
    <el-card>
      <el-button type="success" style="margin-bottom:16px" @click="showDialog()">Add Dict</el-button>
      <el-form inline style="margin-bottom:16px">
        <el-form-item label="Type">
          <el-select v-model="filterType" placeholder="All" clearable @change="loadData">
            <el-option v-for="t in dictTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="dictType" label="Type" />
        <el-table-column prop="dictCode" label="Code" />
        <el-table-column prop="dictLabel" label="Label" />
        <el-table-column prop="sortOrder" label="Sort" />
        <el-table-column prop="status" label="Status">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? 'Active' : 'Disabled' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="showDialog(row)">Edit</el-button>
            <el-popconfirm title="Delete?" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger">Delete</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? 'Edit' : 'Add'" width="400px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="Type" prop="dictType"><el-input v-model="form.dictType" /></el-form-item>
        <el-form-item label="Code" prop="dictCode"><el-input v-model="form.dictCode" /></el-form-item>
        <el-form-item label="Label" prop="dictLabel"><el-input v-model="form.dictLabel" /></el-form-item>
        <el-form-item label="Sort"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="handleSave">Save</el-button>
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
  dictType: [{ required: true, message: 'Required', trigger: 'blur' }],
  dictCode: [{ required: true, message: 'Required', trigger: 'blur' }],
  dictLabel: [{ required: true, message: 'Required', trigger: 'blur' }]
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
    ElMessage.success('Saved')
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('Save failed')
  }
}

async function handleDelete(id: number) {
  try {
    await request.delete(`/sys/dicts/${id}`)
    ElMessage.success('Deleted')
    loadData()
  } catch {
    ElMessage.error('Delete failed')
  }
}

onMounted(loadData)
</script>
