<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="时间段">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM" start-placeholder="开始" end-placeholder="结束" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      </el-form>

      <div ref="chartRef" style="height:400px;margin:20px 0"></div>

      <el-table :data="tableData" stripe>
        <el-table-column prop="month" label="月份" />
        <el-table-column prop="total" label="收入（元）">
          <template #default="{ row }">¥{{ row.total || 0 }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const chartRef = ref()
const dateRange = ref<string[]>([])
const tableData = ref<any[]>([])
let chart: echarts.ECharts | null = null

async function loadData() {
  if (!dateRange.value || dateRange.value.length < 2) return
  try {
    const res: any = await request.get('/bills/revenue', {
      params: { startMonth: dateRange.value[0], endMonth: dateRange.value[1] }
    })
    tableData.value = res.data || []

    if (chartRef.value) {
      if (!chart) chart = echarts.init(chartRef.value)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: res.data.map((d: any) => d.month) },
        yAxis: { type: 'value', name: '收入（元）' },
        series: [{
          type: 'bar',
          data: res.data.map((d: any) => d.total || 0),
          itemStyle: { color: '#409eff' },
          barWidth: '40%'
        }]
      })
    }
  } catch {
    tableData.value = []
    ElMessage.error('加载收入数据失败')
  }
}

function handleResize() { chart?.resize() }

onMounted(() => {
  const now = new Date()
  const end = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  const start = `${now.getFullYear() - 1}-${String(now.getMonth() + 1).padStart(2, '0')}`
  dateRange.value = [start, end]
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  chart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>
