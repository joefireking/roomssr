<template>
  <div>
    <el-card>
      <el-steps :active="step" finish-status="success" style="margin-bottom:30px">
        <el-step title="选择房间" />
        <el-step title="租户与合同" />
        <el-step title="确认" />
      </el-steps>

      <!-- 第一步：选择房间 -->
      <div v-if="step === 0">
        <el-form inline style="margin-bottom:16px">
          <el-form-item label="楼栋">
            <el-select v-model="roomQuery.buildingId" placeholder="全部" clearable @change="loadRooms">
              <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="loadRooms">搜索</el-button></el-form-item>
        </el-form>
        <el-row :gutter="16">
          <el-col :span="6" v-for="room in rooms" :key="room.id" style="margin-bottom:16px">
            <el-card shadow="hover" :class="{ 'selected-room': selectedRoom?.id === room.id }" @click="selectRoom(room)" style="cursor:pointer">
              <div style="font-weight:bold;font-size:16px">{{ room.roomNumber }}</div>
              <div style="color:#909399">{{ room.typeName }} | {{ room.floor }}楼</div>
              <div style="color:#f56c6c;font-size:18px;margin-top:8px">¥{{ room.rentPrice }}/月</div>
              <el-tag type="success" size="small" style="margin-top:4px">空置</el-tag>
            </el-card>
          </el-col>
        </el-row>
        <el-button type="primary" :disabled="!selectedRoom" @click="step = 1">下一步</el-button>
      </div>

      <!-- 第二步：租户与合同 -->
      <div v-if="step === 1">
        <el-form ref="contractFormRef" :model="contractForm" :rules="contractRules" label-width="120px" style="max-width:600px">
          <el-divider>租户信息</el-divider>
          <el-form-item label="租户" prop="tenantId">
            <el-select v-model="contractForm.tenantId" filterable placeholder="请选择租户" style="width:100%">
              <el-option v-for="t in tenants" :key="t.id" :label="`${t.name} (${t.phone})`" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-divider>合同条款</el-divider>
          <el-form-item label="开始日期" prop="startDate">
            <el-date-picker v-model="contractForm.startDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="结束日期" prop="endDate">
            <el-date-picker v-model="contractForm.endDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="租金（元）" prop="rentAmount">
            <el-input-number v-model="contractForm.rentAmount" :min="0" :precision="2" />
          </el-form-item>
          <el-form-item label="押金（元）" prop="depositAmount">
            <el-input-number v-model="contractForm.depositAmount" :min="0" :precision="2" />
          </el-form-item>
          <el-form-item label="付款周期">
            <el-select v-model="contractForm.paymentCycle">
              <el-option label="月付" :value="1" /><el-option label="季付" :value="3" /><el-option label="年付" :value="12" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注"><el-input v-model="contractForm.remark" type="textarea" /></el-form-item>
        </el-form>
        <div style="margin-top:16px">
          <el-button @click="step = 0">上一步</el-button>
          <el-button type="primary" @click="validateStep2">下一步</el-button>
        </div>
      </div>

      <!-- 第三步：确认 -->
      <div v-if="step === 2">
        <el-descriptions title="合同摘要" :column="2" border>
          <el-descriptions-item label="房间">{{ selectedRoom?.roomNumber }}</el-descriptions-item>
          <el-descriptions-item label="楼栋">{{ selectedRoom?.buildingName }}</el-descriptions-item>
          <el-descriptions-item label="租户">{{ tenants.find(t => t.id === contractForm.tenantId)?.name }}</el-descriptions-item>
          <el-descriptions-item label="租期">{{ contractForm.startDate }} ~ {{ contractForm.endDate }}</el-descriptions-item>
          <el-descriptions-item label="租金">¥{{ contractForm.rentAmount }}/月</el-descriptions-item>
          <el-descriptions-item label="押金">¥{{ contractForm.depositAmount }}</el-descriptions-item>
          <el-descriptions-item label="付款周期">{{ { 1: '月付', 3: '季付', 12: '年付' }[contractForm.paymentCycle as number] }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:16px">
          <el-button @click="step = 1">上一步</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const step = ref(0)
const submitting = ref(false)
const contractFormRef = ref()
const selectedRoom = ref<any>(null)
const buildings = ref<any[]>([])
const rooms = ref<any[]>([])
const tenants = ref<any[]>([])
const roomQuery = reactive({ buildingId: null as any })

const contractForm = reactive({
  tenantId: null as any, roomId: null as any, startDate: '', endDate: '',
  rentAmount: 0, depositAmount: 0, paymentCycle: 1, remark: ''
})
const contractRules = {
  tenantId: [{ required: true, message: '必填', trigger: 'change' }],
  startDate: [{ required: true, message: '必填', trigger: 'change' }],
  endDate: [{ required: true, message: '必填', trigger: 'change' }],
  rentAmount: [{ required: true, message: '必填', trigger: 'change' }],
  depositAmount: [{ required: true, message: '必填', trigger: 'change' }]
}

function selectRoom(room: any) {
  selectedRoom.value = room
  contractForm.roomId = room.id
  contractForm.rentAmount = room.rentPrice
  contractForm.depositAmount = room.rentPrice
}

async function loadRooms() {
  try {
    const params: any = { status: 0, size: 100 }
    if (roomQuery.buildingId) params.buildingId = roomQuery.buildingId
    const res: any = await request.get('/rooms/list', { params })
    rooms.value = res.data.records
  } catch {
    rooms.value = []
    ElMessage.error('加载房间列表失败')
  }
}

async function loadOptions() {
  try {
    const [bRes, tRes]: any = await Promise.all([
      request.get('/buildings/list', { params: { size: 100 } }),
      request.get('/tenants/list', { params: { size: 100 } })
    ])
    buildings.value = bRes.data.records
    tenants.value = tRes.data.records
  } catch {
    buildings.value = []
    tenants.value = []
    ElMessage.error('加载选项数据失败')
  }
}

async function validateStep2() {
  await contractFormRef.value?.validate()
  step.value = 2
}

async function handleSubmit() {
  submitting.value = true
  try {
    await request.post('/contracts', contractForm)
    ElMessage.success('合同创建成功')
    router.push('/contract/list')
  } finally { submitting.value = false }
}

onMounted(() => { loadOptions(); loadRooms() })
</script>

<style scoped>
.selected-room {
  border-color: #409eff;
  background: #ecf5ff;
}
</style>
