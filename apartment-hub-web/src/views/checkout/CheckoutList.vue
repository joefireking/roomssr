<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable @change="loadData">
            <el-option label="生效" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="query.current = 1; loadData()">搜索</el-button></el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="contractNo" label="合同编号" />
        <el-table-column prop="tenantId" label="租户" />
        <el-table-column prop="roomId" label="房间" />
        <el-table-column prop="startDate" label="开始日期" />
        <el-table-column prop="endDate" label="结束日期" />
        <el-table-column prop="depositAmount" label="押金">
          <template #default="{ row }">¥{{ row.depositAmount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" type="warning" @click="showCheckout(row)">退租结算</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />

      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.current"
        v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next"
        @current-change="loadData" @size-change="loadData" />
    </el-card>

    <el-dialog v-model="checkoutVisible" title="退租结算" width="500px">
      <el-form :model="checkoutForm" label-width="120px">
        <el-form-item label="押金">¥{{ checkoutContract?.depositAmount }}</el-form-item>
        <el-form-item label="损坏费用"><el-input-number v-model="checkoutForm.damageCost" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="违约金"><el-input-number v-model="checkoutForm.penaltyAmount" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="退租原因"><el-input v-model="checkoutForm.terminateReason" type="textarea" /></el-form-item>
        <el-divider />
        <el-form-item label="退款金额">
          <span style="font-size:20px;font-weight:bold;color:#67c23a">¥{{ calculateRefund() }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkoutVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCheckout">确认退租</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const submitting = ref(false)
const checkoutVisible = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const checkoutContract = ref<any>(null)
const query = reactive({ status: 1, current: 1, size: 10 })
const checkoutForm = reactive({ contractId: null as any, damageCost: 0, penaltyAmount: 0, terminateReason: '' })

function calculateRefund() {
  const deposit = checkoutContract.value?.depositAmount || 0
  const refund = deposit - checkoutForm.damageCost - checkoutForm.penaltyAmount
  return Math.max(0, refund).toFixed(2)
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/contracts/list', { params: query })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function showCheckout(row: any) {
  checkoutContract.value = row
  checkoutForm.contractId = row.id
  checkoutForm.damageCost = 0
  checkoutForm.penaltyAmount = 0
  checkoutForm.terminateReason = ''
  checkoutVisible.value = true
}

async function handleCheckout() {
  try {
    await ElMessageBox.confirm(
      `退款金额：¥${calculateRefund()}，确认退租？`,
      '退租确认',
      { confirmButtonText: '确认退租', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  submitting.value = true
  try {
    const res: any = await request.post('/contracts/checkout', checkoutForm)
    ElMessage.success(`退租完成，退款：¥${res.data}`)
    checkoutVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

onMounted(loadData)
</script>
