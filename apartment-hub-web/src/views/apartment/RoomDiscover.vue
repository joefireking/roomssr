<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>Room Discovery</span>
          <el-button @click="loadData" :loading="loading">Refresh</el-button>
        </div>
      </template>

      <el-empty v-if="!loading && groups.length === 0" description="No vacant rooms available" />

      <div v-for="group in groups" :key="group.buildingId" class="building-group">
        <div class="building-header">
          <el-icon><OfficeBuilding /></el-icon>
          <span class="building-name">{{ group.buildingName }}</span>
          <el-tag type="success" size="small">{{ group.count }} vacant rooms</el-tag>
        </div>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="room in group.rooms" :key="room.id">
            <el-card shadow="hover" class="room-card" @click="showSimilar(room)">
              <div class="room-info">
                <div class="room-number">{{ room.roomNumber }}</div>
                <div class="room-detail">
                  <span>Floor {{ room.floor }}</span>
                  <span v-if="room.typeName">{{ room.typeName }}</span>
                </div>
                <div class="room-price">&yen;{{ room.rentPrice }}/month</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <el-dialog v-model="similarDialogVisible" title="Similar Rooms" width="700px" destroy-on-close>
      <p v-if="similarRooms.length === 0" style="color:#999">No similar vacant rooms found in this building.</p>
      <el-table v-else :data="similarRooms" stripe>
        <el-table-column prop="roomNumber" label="Room" />
        <el-table-column prop="floor" label="Floor" width="80" />
        <el-table-column prop="rentPrice" label="Rent">
          <template #default="{ row }">&yen;{{ row.rentPrice }}</template>
        </el-table-column>
        <el-table-column prop="status" label="Status">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { OfficeBuilding } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface RoomItem {
  id: number
  roomNumber: string
  floor: number
  rentPrice: number
  status: string
  typeName?: string
  image?: string
}

interface BuildingGroup {
  buildingId: number
  buildingName: string
  rooms: RoomItem[]
  count: number
}

const loading = ref(false)
const groups = ref<BuildingGroup[]>([])
const similarDialogVisible = ref(false)
const similarRooms = ref<RoomItem[]>([])

const statusMap: Record<string, { label: string; type: string }> = {
  VACANT: { label: 'Vacant', type: 'success' },
  RENTED: { label: 'Rented', type: 'danger' },
  MAINTENANCE: { label: 'Maintenance', type: 'warning' },
  RESERVED: { label: 'Reserved', type: 'info' }
}
function statusLabel(s: string) { return statusMap[s]?.label || s }
function statusType(s: string) { return statusMap[s]?.type || 'info' }

async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/rooms/discover')
    groups.value = res.data
  } finally { loading.value = false }
}

async function showSimilar(room: RoomItem) {
  try {
    const res: any = await request.get(`/rooms/${room.id}/similar`)
    similarRooms.value = res.data
    similarDialogVisible.value = true
  } catch { /* handled */ }
}

onMounted(loadData)
</script>

<style scoped>
.building-group {
  margin-bottom: 24px;
}
.building-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}
.building-name {
  flex: 1;
}
.room-card {
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}
.room-card:hover {
  transform: translateY(-2px);
}
.room-info {
  text-align: center;
}
.room-number {
  font-size: 18px;
  font-weight: 700;
  color: #2563eb;
  margin-bottom: 6px;
}
.room-detail {
  display: flex;
  justify-content: center;
  gap: 12px;
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 8px;
}
.room-price {
  font-size: 16px;
  font-weight: 600;
  color: #059669;
}
</style>
