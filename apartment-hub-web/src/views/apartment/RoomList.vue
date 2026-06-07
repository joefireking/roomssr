<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="楼栋">
          <el-select v-model="query.buildingId" placeholder="全部" clearable @change="resetAndLoad">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable @change="resetAndLoad">
            <el-option label="空置" :value="0" /><el-option label="已租" :value="1" />
            <el-option label="维修" :value="2" /><el-option label="预定" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query.current = 1; loadData()">搜索</el-button>
          <el-button type="success" @click="showDialog()">新增</el-button>
          <el-button-group style="margin-left:8px">
            <el-button :type="viewMode === 'table' ? 'primary' : ''" @click="viewMode = 'table'">列表</el-button>
            <el-button :type="viewMode === 'card' ? 'primary' : ''" @click="viewMode = 'card'">卡片</el-button>
          </el-button-group>
        </el-form-item>
      </el-form>

      <!-- Card View -->
      <div v-if="viewMode === 'card'" v-loading="loading" class="room-card-grid">
        <el-card v-for="room in tableData" :key="room.id" shadow="hover" class="room-card-item" @click="showDialog(room)">
          <el-image v-if="room.image" :src="room.image" fit="cover" class="room-card-img" />
          <div v-else class="room-card-img room-card-placeholder">无图片</div>
          <div class="room-card-info">
            <div class="room-card-header">
              <span class="room-card-number">{{ room.roomNumber }}</span>
              <el-tag :type="statusType(room.status)" size="small">{{ statusLabel(room.status) }}</el-tag>
            </div>
            <div class="room-card-detail">{{ room.buildingName || '-' }} | {{ room.typeName || '-' }} | {{ room.floor }}楼</div>
            <div class="room-card-price">¥{{ room.rentPrice }}/月</div>
          </div>
        </el-card>
        <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />
      </div>

      <!-- Table View -->
      <el-table v-if="viewMode === 'table'" :data="tableData" stripe v-loading="loading">
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <el-image v-if="row.image" :src="row.image" fit="cover" style="width:60px;height:60px;border-radius:6px" preview-teleported :preview-src-list="[row.image]" />
            <span v-else class="no-image">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="roomNumber" label="房号" />
        <el-table-column prop="buildingName" label="楼栋" />
        <el-table-column prop="typeName" label="房型" />
        <el-table-column prop="floor" label="楼层" />
        <el-table-column prop="rentPrice" label="租金（元）">
          <template #default="{ row }">¥{{ row.rentPrice }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
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
        <el-form-item label="楼栋" prop="buildingId">
          <el-select v-model="form.buildingId" placeholder="请选择">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房型">
          <el-select v-model="form.roomTypeId" placeholder="请选择" clearable>
            <el-option v-for="t in roomTypes" :key="t.id" :label="t.typeName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房号" prop="roomNumber"><el-input v-model="form.roomNumber" /></el-form-item>
        <el-form-item label="楼层"><el-input-number v-model="form.floor" :min="1" /></el-form-item>
        <el-form-item label="租金（元）"><el-input-number v-model="form.rentPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="房间图片">
          <div style="display:flex;align-items:center;gap:12px">
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="onUploadSuccess"
              :on-error="onUploadError"
              accept="image/*"
            >
              <el-button type="primary" size="small">上传图片</el-button>
            </el-upload>
            <el-image v-if="form.image" :src="form.image" fit="cover" style="width:80px;height:60px;border-radius:6px" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useTable } from '@/composables/useTable'

const uploadUrl = '/api/upload'
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${localStorage.getItem('token') || ''}` }))

function onUploadSuccess(res: any) {
  form.image = res.data.url
  ElMessage.success('上传成功')
}

function onUploadError() {
  ElMessage.error('上传失败')
}

const { loading, tableData, total, query, loadData, resetAndLoad } = useTable<{ buildingId: any; status: any }>({
  url: '/rooms/list',
  filters: { buildingId: null, status: null }
})
const saving = ref(false)
const dialogVisible = ref(false)
const viewMode = ref<'table' | 'card'>('table')
const buildings = ref<any[]>([])
const roomTypes = ref<any[]>([])
const formRef = ref()
const form = reactive<any>({ id: null, buildingId: null, roomTypeId: null, roomNumber: '', floor: 1, rentPrice: 0, image: '', status: 0 })
const rules = { buildingId: [{ required: true, message: '必填', trigger: 'change' }], roomNumber: [{ required: true, message: '必填', trigger: 'blur' }] }

const statusLabels: Record<number, string> = { 0: '空置', 1: '已租', 2: '维修', 3: '预定' }
const statusTypes: Record<number, string> = { 0: 'success', 1: 'primary', 2: 'warning', 3: 'info' }
function statusLabel(s: number) { return statusLabels[s] || '未知' }
function statusType(s: number) { return statusTypes[s] || 'info' }

async function loadOptions() {
  try {
    const [bRes, tRes]: any = await Promise.all([
      request.get('/buildings/list', { params: { size: 100 } }),
      request.get('/room-types/all')
    ])
    buildings.value = bRes.data.records
    roomTypes.value = tRes.data
  } catch {
    buildings.value = []
    roomTypes.value = []
    ElMessage.error('加载选项数据失败')
  }
}

const defaultForm = { id: null, buildingId: null, roomTypeId: null, roomNumber: '', floor: 1, rentPrice: 0, image: '', status: 0 }

function showDialog(row?: any) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, row ? { ...row } : { ...defaultForm })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (form.id) await request.put(`/rooms/${form.id}`, form)
    else await request.post('/rooms', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  try {
    await request.delete(`/rooms/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(() => { loadOptions(); loadData() })
</script>

<style scoped>
.room-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 8px;
}
.room-card-item {
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.room-card-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}
.room-card-img {
  width: 100%;
  height: 120px;
  border-radius: 4px;
  object-fit: cover;
}
.room-card-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 13px;
}
.room-card-info {
  padding: 8px 0 0;
}
.room-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.room-card-number {
  font-weight: 700;
  font-size: 15px;
}
.room-card-detail {
  color: #909399;
  font-size: 12px;
  margin: 6px 0;
}
.room-card-price {
  color: #f56c6c;
  font-weight: 700;
  font-size: 16px;
}
</style>
