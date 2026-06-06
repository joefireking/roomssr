<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <template #header>房间状态分布</template>
          <div ref="pieRef" style="height:400px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>入住率</template>
          <div ref="gaugeRef" style="height:400px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px">
      <template #header>房间状态明细</template>
      <el-table :data="statusData" stripe>
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">{{ statusNames[row.status] || '未知' }}</template>
        </el-table-column>
        <el-table-column prop="count" label="数量" />
        <el-table-column label="占比">
          <template #default="{ row }">{{ totalRooms ? ((row.count / totalRooms) * 100).toFixed(1) + '%' : '0%' }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const pieRef = ref()
const gaugeRef = ref()
const statusData = ref<any[]>([])
let pieChart: echarts.ECharts | null = null
let gaugeChart: echarts.ECharts | null = null

const statusNames: Record<number, string> = { 0: '空置', 1: '已租', 2: '维修', 3: '预定' }
const statusColors: Record<number, string> = { 0: '#67c23a', 1: '#409eff', 2: '#e6a23c', 3: '#909399' }
const totalRooms = computed(() => statusData.value.reduce((sum, s) => sum + s.count, 0))

async function loadData() {
  try {
    const res: any = await request.get('/rooms/status-count')
    statusData.value = res.data || []
  } catch {
    statusData.value = []
  }

  if (pieRef.value) {
    pieChart?.dispose()
    pieChart = echarts.init(pieRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: statusData.value.map((s: any) => ({
          name: statusNames[s.status] || '未知',
          value: s.count,
          itemStyle: { color: statusColors[s.status] || '#ccc' }
        })),
        label: { formatter: '{b}: {c} ({d}%)' }
      }]
    })
  }

  if (gaugeRef.value) {
    gaugeChart?.dispose()
    gaugeChart = echarts.init(gaugeRef.value)
    const rented = statusData.value.find((s: any) => s.status === 1)?.count || 0
    const rate = totalRooms.value ? (rented / totalRooms.value) * 100 : 0
    gaugeChart.setOption({
      series: [{
        type: 'gauge', progress: { show: true, width: 18 },
        axisLine: { lineStyle: { width: 18 } },
        axisTick: { show: false },
        splitLine: { length: 10, lineStyle: { width: 2, color: '#999' } },
        pointer: { itemStyle: { color: 'auto' } },
        axisLabel: { distance: 25, fontSize: 12 },
        detail: { valueAnimation: true, formatter: '{value}%', fontSize: 24, offsetCenter: [0, '70%'] },
        data: [{ value: Math.round(rate * 10) / 10 }]
      }]
    })
  }
}

function handleResize() {
  pieChart?.resize()
  gaugeChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  pieChart?.dispose()
  gaugeChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>
