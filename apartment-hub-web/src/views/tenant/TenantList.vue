<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="姓名"><el-input v-model="query.name" placeholder="搜索" clearable /></el-form-item>
        <el-form-item label="电话"><el-input v-model="query.phone" placeholder="电话" clearable /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query.current = 1; loadData()">搜索</el-button>
          <el-button type="success" @click="showDialog()">新增租户</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="gender" label="性别">
          <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="idCard" label="身份证号">
          <template #default="{ row }">{{ row.idCard?.replace(/^(.{6})(.*)(.{4})$/, '$1****$3') }}</template>
        </el-table-column>
        <el-table-column prop="tag" label="标签">
          <template #default="{ row }">
            <el-tag v-if="row.tag" :type="row.tag === 'student' ? 'primary' : row.tag === 'family' ? 'success' : 'warning'" size="small">{{ { student: '学生', 'white-collar': '白领', family: '家庭' }[row.tag] || row.tag }}</el-tag>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑租户' : '新增租户'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio><el-radio :value="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="电话" prop="phone"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="身份证号" prop="idCard"><el-input v-model="form.idCard" /></el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tag" placeholder="请选择" clearable>
            <el-option label="学生" value="student" /><el-option label="白领" value="white-collar" />
            <el-option label="家庭" value="family" />
          </el-select>
        </el-form-item>
        <el-form-item label="紧急联系人"><el-input v-model="form.emergencyContact" /></el-form-item>
        <el-form-item label="紧急联系电话"><el-input v-model="form.emergencyPhone" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
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
const total = ref(0)
const formRef = ref()
const query = reactive({ name: '', phone: '', current: 1, size: 10 })
const form = reactive<any>({ id: null, name: '', gender: 1, phone: '', idCard: '', tag: '', emergencyContact: '', emergencyPhone: '', remark: '' })
const rules = {
  name: [{ required: true, message: '必填', trigger: 'blur' }],
  phone: [{ required: true, message: '必填', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  idCard: [{ required: true, message: '必填', trigger: 'blur' }, { pattern: /^\d{17}[\dXx]$/, message: '身份证号格式不正确', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/tenants/list', { params: query })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function showDialog(row?: any) {
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, name: '', gender: 1, phone: '', idCard: '', tag: '', emergencyContact: '', emergencyPhone: '', remark: '' })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) await request.put(`/tenants/${form.id}`, form)
    else await request.post('/tenants', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  try {
    await request.delete(`/tenants/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(loadData)
</script>
