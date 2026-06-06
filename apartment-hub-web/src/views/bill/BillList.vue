<template>
  <div>
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover">
          <div style="font-size:14px;color:#909399">{{ card.label }}</div>
          <div style="font-size:24px;font-weight:bold;margin-top:8px" :style="{color: card.color}">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable @change="loadData">
            <el-option label="待付" :value="0" /><el-option label="已付" :value="1" />
            <el-option label="逾期" :value="2" /><el-option label="取消" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.billType" placeholder="全部" clearable @change="loadData">
            <el-option label="租金" :value="0" /><el-option label="押金" :value="1" />
            <el-option label="水电" :value="2" /><el-option label="物业" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query.current = 1; loadData()">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="billNo" label="账单号" width="180" />
        <el-table-column prop="tenantId" label="租户" />
        <el-table-column prop="billType" label="类型">
          <template #default="{ row }">
            <el-tag :type="billTypeTag(row.billType)">{{ billTypeLabel(row.billType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column prop="billingMonth" label="账单月份" />
        <el-table-column prop="dueDate" label="到期日" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="billStatusType(row.status)">{{ billStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button v-if="row.status === 0 || row.status === 2" size="small" type="success" @click="showPayDialog(row)">缴费</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />

      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.current"
        v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next"
        @current-change="loadData" @size-change="loadData" />
    </el-card>

    <el-dialog v-model="payDialogVisible" title="缴费" width="400px">
      <el-form label-width="100px">
        <el-form-item label="账单号">{{ payBill?.billNo }}</el-form-item>
        <el-form-item label="金额">¥{{ payBill?.amount }}</el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="payForm.paymentMethod" style="width:100%">
            <el-option label="现金" :value="0" /><el-option label="银行转账" :value="1" />
            <el-option label="支付宝" :value="2" /><el-option label="微信" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="handlePay">确认缴费</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const paying = ref(false)
const payDialogVisible = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const payBill = ref<any>(null)
const query = reactive({ status: null as any, billType: null as any, current: 1, size: 10 })
const payForm = reactive({ paymentMethod: 0, amount: 0, remark: '' })

const statCards = ref([
  { label: '已付', value: '¥0', color: '#67c23a' },
  { label: '待付', value: '¥0', color: '#e6a23c' },
  { label: '逾期', value: '¥0', color: '#f56c6c' }
])

const typeLabels: Record<number, string> = { 0: '租金', 1: '押金', 2: '水电', 3: '物业' }
const typeTags: Record<number, string> = { 0: 'primary', 1: 'warning', 2: 'info', 3: '' }
const statusLabels: Record<number, string> = { 0: '待付', 1: '已付', 2: '逾期', 3: '取消' }
const statusTypes: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
function billTypeLabel(s: number) { return typeLabels[s] || '未知' }
function billTypeTag(s: number) { return typeTags[s] || '' }
function billStatusLabel(s: number) { return statusLabels[s] || '未知' }
function billStatusType(s: number) { return statusTypes[s] || 'info' }

async function loadStats() {
  try {
    const res: any = await request.get('/bills/stats')
    statCards.value[0].value = '¥' + (res.data.totalPaid || 0)
    statCards.value[1].value = '¥' + (res.data.totalPending || 0)
    statCards.value[2].value = '¥' + (res.data.totalOverdue || 0)
  } catch { /* handled */ }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/bills/list', { params: query })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function showPayDialog(bill: any) {
  payBill.value = bill
  payForm.amount = bill.amount
  payForm.paymentMethod = 0
  payForm.remark = ''
  payDialogVisible.value = true
}

async function handlePay() {
  paying.value = true
  try {
    await request.post(`/bills/${payBill.value.id}/pay`, payForm)
    ElMessage.success('缴费成功')
    payDialogVisible.value = false
    loadData()
    loadStats()
  } finally { paying.value = false }
}

onMounted(() => { loadData(); loadStats() })
</script>
