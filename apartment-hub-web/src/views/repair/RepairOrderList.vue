<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable @change="resetAndLoad">
            <el-option label="待处理" :value="0" /><el-option label="已分配" :value="1" />
            <el-option label="处理中" :value="2" /><el-option label="已完成" :value="3" />
            <el-option label="已验证" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable @change="resetAndLoad">
            <el-option label="水管" :value="0" /><el-option label="家具" :value="1" />
            <el-option label="电器" :value="2" /><el-option label="网络" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query.current = 1; loadData()">搜索</el-button>
          <el-button type="success" @click="showCreateDialog">创建工单</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="orderNo" label="工单号" width="180" />
        <el-table-column label="房间">
          <template #default="{ row }">{{ roomMap[row.roomId] || row.roomId }}</template>
        </el-table-column>
        <el-table-column prop="type" label="类型">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabels[row.type] || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)" size="small">{{ priorityLabels[row.priority] || '普通' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabels[row.status] || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="primary" @click="handleAssign(row)">分配</el-button>
            <el-button v-if="row.status === 1" size="small" type="warning" @click="handleStart(row)">开始</el-button>
            <el-button v-if="row.status === 2" size="small" type="success" @click="handleComplete(row)">完成</el-button>
            <el-button v-if="row.status === 3" size="small" @click="handleVerify(row)">验证</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />

      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.current"
        v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next"
        @current-change="loadData" @size-change="loadData" />
    </el-card>

    <el-dialog v-model="createVisible" title="创建报修工单" width="500px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="房间" prop="roomId">
          <el-select v-model="createForm.roomId" filterable placeholder="请选择房间">
            <el-option v-for="r in allRooms" :key="r.id" :label="r.roomNumber" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="createForm.type">
            <el-option label="水管" :value="0" /><el-option label="家具" :value="1" />
            <el-option label="电器" :value="2" /><el-option label="网络" :value="3" /><el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="createForm.priority">
            <el-option label="紧急" :value="0" /><el-option label="普通" :value="1" /><el-option label="低" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="createForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useTable } from '@/composables/useTable'

const { loading, tableData, total, query, loadData, resetAndLoad } = useTable<{ status: any; type: any }>({
  url: '/repair-orders/list',
  filters: { status: null, type: null }
})
const saving = ref(false)
const createVisible = ref(false)
const allRooms = ref<any[]>([])
const createFormRef = ref()
const createForm = reactive({ roomId: null as any, type: 0, priority: 1, description: '' })
const createRules = { roomId: [{ required: true, message: '必填', trigger: 'change' }], type: [{ required: true, message: '必填', trigger: 'change' }] }
const roomMap = ref<Record<number, string>>({})

async function loadRoomMap() {
  try {
    const res: any = await request.get('/rooms/list', { params: { size: 1000 } })
    res.data.records.forEach((r: any) => { roomMap.value[r.id] = r.roomNumber })
  } catch { /* ignore */ }
}

const typeLabels: Record<number, string> = { 0: '水管', 1: '家具', 2: '电器', 3: '网络', 4: '其他' }
const statusLabels: Record<number, string> = { 0: '待处理', 1: '已分配', 2: '处理中', 3: '已完成', 4: '已验证' }
const priorityLabels: Record<number, string> = { 0: '紧急', 1: '普通', 2: '低' }
function statusType(s: number) { return ['', 'primary', 'warning', 'success', ''][s] || 'info' }
function priorityType(s: number) { return ['danger', '', 'info'][s] || '' }

async function showCreateDialog() {
  try {
    const res: any = await request.get('/rooms/list', { params: { size: 100 } })
    allRooms.value = res.data.records
  } catch {
    allRooms.value = []
    ElMessage.error('加载房间列表失败')
    return
  }
  Object.assign(createForm, { roomId: null, type: 0, priority: 1, description: '' })
  createVisible.value = true
}

async function handleCreate() {
  await createFormRef.value?.validate()
  saving.value = true
  try {
    await request.post('/repair-orders', createForm)
    ElMessage.success('工单创建成功')
    createVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function handleAssign(row: any) {
  try {
    const { value } = await ElMessageBox.prompt('指派用户ID', '分配工单')
    await request.put(`/repair-orders/${row.id}/assign?assigneeId=${value}`)
    ElMessage.success('分配成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('分配失败')
  }
}

async function handleStart(row: any) {
  try {
    await request.put(`/repair-orders/${row.id}/start`)
    ElMessage.success('已开始处理')
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleComplete(row: any) {
  try {
    const { value } = await ElMessageBox.prompt('处理备注', '完成工单', { inputType: 'textarea' })
    await request.put(`/repair-orders/${row.id}/complete?remark=${encodeURIComponent(value || '')}`)
    ElMessage.success('工单已完成')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

async function handleVerify(row: any) {
  try {
    await request.put(`/repair-orders/${row.id}/verify`)
    ElMessage.success('已验证')
    loadData()
  } catch {
    ElMessage.error('验证失败')
  }
}

onMounted(() => { loadRoomMap(); loadData() })
</script>
