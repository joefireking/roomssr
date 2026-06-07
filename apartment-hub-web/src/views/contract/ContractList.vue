<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable @change="resetAndLoad">
            <el-option label="草稿" :value="0" /><el-option label="生效" :value="1" />
            <el-option label="到期" :value="2" /><el-option label="终止" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query.current = 1; loadData()">搜索</el-button>
          <el-button type="success" @click="$router.push('/contract/create')">新建合同</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="contractNo" label="合同编号" width="180" />
        <el-table-column label="租户">
          <template #default="{ row }">{{ tenantMap[row.tenantId] || row.tenantId }}</template>
        </el-table-column>
        <el-table-column label="房间">
          <template #default="{ row }">{{ roomMap[row.roomId] || row.roomId }}</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" />
        <el-table-column prop="endDate" label="结束日期" />
        <el-table-column prop="rentAmount" label="租金">
          <template #default="{ row }">¥{{ row.rentAmount }}</template>
        </el-table-column>
        <el-table-column prop="depositAmount" label="押金">
          <template #default="{ row }">¥{{ row.depositAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="contractStatusType(row.status)">{{ contractStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="viewContract(row)">查看合同</el-button>
            <el-button v-if="row.status === 1" size="small" type="warning" @click="handleTerminate(row)">终止</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />

      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.current"
        v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next"
        @current-change="loadData" @size-change="loadData" />
    </el-card>

    <ContractViewer v-model:visible="viewerVisible" :contract-id="viewerContractId" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import ContractViewer from './ContractViewer.vue'
import { useTable } from '@/composables/useTable'

const { loading, tableData, total, query, loadData, resetAndLoad } = useTable<{ status: any }>({
  url: '/contracts/list',
  filters: { status: null }
})
const viewerVisible = ref(false)
const viewerContractId = ref<number | null>(null)
const tenantMap = ref<Record<number, string>>({})
const roomMap = ref<Record<number, string>>({})

async function loadLookups() {
  try {
    const [tRes, rRes]: any = await Promise.all([
      request.get('/tenants/list', { params: { size: 1000 } }),
      request.get('/rooms/list', { params: { size: 1000 } })
    ])
    tRes.data.records.forEach((t: any) => { tenantMap.value[t.id] = t.name })
    rRes.data.records.forEach((r: any) => { roomMap.value[r.id] = r.roomNumber })
  } catch { /* ignore */ }
}

const statusLabels: Record<number, string> = { 0: '草稿', 1: '生效', 2: '到期', 3: '终止' }
const statusTypes: Record<number, string> = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }
function contractStatusLabel(s: number) { return statusLabels[s] || '未知' }
function contractStatusType(s: number) { return statusTypes[s] || 'info' }

function viewContract(row: any) {
  viewerContractId.value = row.id
  viewerVisible.value = true
}

async function handleTerminate(row: any) {
  try {
    const { value } = await ElMessageBox.prompt('终止原因', '终止合同', { inputType: 'textarea' })
    await request.put(`/contracts/${row.id}/terminate?reason=${encodeURIComponent(value || '')}`)
    ElMessage.success('合同已终止')
    loadData()
  } catch { /* cancelled */ }
}

onMounted(() => { loadLookups(); loadData() })
</script>
