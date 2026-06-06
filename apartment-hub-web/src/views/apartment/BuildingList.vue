<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="公寓">
          <el-select v-model="query.apartmentId" placeholder="全部" clearable @change="loadData">
            <el-option v-for="a in apartments" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query.current = 1; loadData()">搜索</el-button>
          <el-button type="success" @click="showDialog()">新增</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="apartmentName" label="公寓">
          <template #default="{ row }">{{ getApartmentName(row.apartmentId) }}</template>
        </el-table-column>
        <el-table-column prop="floors" label="楼层数" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑' : '新增'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="公寓" prop="apartmentId">
          <el-select v-model="form.apartmentId" placeholder="请选择">
            <el-option v-for="a in apartments" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="楼层数"><el-input-number v-model="form.floors" :min="1" /></el-form-item>
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
const apartments = ref<any[]>([])
const total = ref(0)
const formRef = ref()
const query = reactive({ apartmentId: null as any, current: 1, size: 10 })
const form = reactive<any>({ id: null, apartmentId: null, name: '', floors: 1, description: '', status: 1 })
const rules = { apartmentId: [{ required: true, message: '必填', trigger: 'change' }], name: [{ required: true, message: '必填', trigger: 'blur' }] }

function getApartmentName(id: number) {
  return apartments.value.find(a => a.id === id)?.name || ''
}

async function loadApartments() {
  try {
    const res: any = await request.get('/apartments/list', { params: { size: 100 } })
    apartments.value = res.data.records
  } catch { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/buildings/list', { params: query })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch { /* ignore */ } finally { loading.value = false }
}

function showDialog(row?: any) {
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, apartmentId: null, name: '', floors: 1, description: '', status: 1 })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) await request.put(`/buildings/${form.id}`, form)
    else await request.post('/buildings', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch { /* ignore */ } finally { saving.value = false }
}

async function handleDelete(id: number) {
  try {
    await request.delete(`/buildings/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* ignore */ }
}

onMounted(() => { loadApartments(); loadData() })
</script>
