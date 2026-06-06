<template>
  <div class="dashboard page-shell">
    <section class="hero">
      <div class="hero-copy">
        <div class="eyebrow">Apartment operation center</div>
        <h1>租住、账单、维修，一屏看清</h1>
        <p>用真实运营指标追踪房源状态、月度收入和待处理事项，空库或接口异常时也会展示演示数据。</p>
        <div class="hero-actions">
          <el-button type="primary" :icon="Refresh" @click="refreshAll">刷新数据</el-button>
          <el-button :icon="OfficeBuilding" @click="$router.push('/apartment/room')">查看房源</el-button>
        </div>
      </div>
      <div class="hero-panel">
        <img src="/images/apartment-hero.svg" alt="Modern apartment building" />
      </div>
    </section>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="12" :lg="6" v-for="item in statCards" :key="item.label">
        <el-card shadow="hover" class="stat-card-wrap">
          <div class="stat-card">
            <div>
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
            <div class="stat-icon" :style="{ background: item.bg, color: item.color }">
              <el-icon :size="26"><component :is="item.icon" /></el-icon>
            </div>
          </div>
          <div class="stat-note">{{ item.note }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card class="chart-card">
          <template #header>
            <div>
              <h2 class="section-title">收入趋势</h2>
              <div class="section-subtitle">最近 12 个月已收金额</div>
            </div>
          </template>
          <div ref="revenueChartRef" class="chart chart-large"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card class="chart-card">
          <template #header>
            <div>
              <h2 class="section-title">房间状态</h2>
              <div class="section-subtitle">空置、已租、维修和预定分布</div>
            </div>
          </template>
          <div ref="roomChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="16">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <h2 class="section-title">Latest Announcements</h2>
              <el-button text type="primary" @click="$router.push('/announcement/list')">View All</el-button>
            </div>
          </template>
          <div v-if="latestAnnouncements.length === 0" style="color:#999;text-align:center;padding:20px">No announcements</div>
          <div v-else class="announcement-list">
            <div v-for="item in latestAnnouncements" :key="item.id" class="announcement-item">
              <div class="announcement-title">
                <el-tag v-if="item.topFlag === 1" type="warning" size="small" style="margin-right:6px">Top</el-tag>
                {{ item.title }}
              </div>
              <div class="announcement-meta">{{ item.summary || item.content?.substring(0, 80) }}</div>
              <div class="announcement-time">{{ item.createTime }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <h2 class="section-title">入住率</h2>
          </template>
          <div class="room-showcase">
            <article v-for="room in showcaseRooms" :key="room.title" class="room-card">
              <img :src="room.image" :alt="room.title" />
              <div>
                <strong>{{ room.title }}</strong>
                <span>{{ room.meta }}</span>
              </div>
            </article>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <h2 class="section-title">入住率</h2>
          </template>
          <div ref="gaugeChartRef" class="chart chart-gauge"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <h2 class="section-title">逾期账单</h2>
          </template>
          <el-table :data="overdueBills" stripe size="small" height="286">
            <el-table-column prop="billNo" label="账单号" min-width="120" />
            <el-table-column prop="amount" label="金额" width="90">
              <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="dueDate" label="到期日" width="110" />
            <el-table-column label="状态" width="86">
              <template #default><el-tag type="danger" size="small">逾期</el-tag></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive, nextTick } from 'vue'
import { Refresh, OfficeBuilding } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const roomChartRef = ref<HTMLElement>()
const revenueChartRef = ref<HTMLElement>()
const gaugeChartRef = ref<HTMLElement>()
let roomChart: echarts.ECharts | null = null
let revenueChart: echarts.ECharts | null = null
let gaugeChart: echarts.ECharts | null = null

const fallbackDashboard = {
  totalRooms: 42,
  rentedRooms: 31,
  totalPaid: 186800,
  totalPending: 39200,
  totalOverdue: 8400,
  roomStatus: [
    { status: 0, count: 8 },
    { status: 1, count: 31 },
    { status: 2, count: 2 },
    { status: 3, count: 1 }
  ]
}

const fallbackRevenue = [
  12400, 16800, 22100, 24500, 28600, 31800, 37400, 43800, 49200, 56600, 62800, 70400
].map((total, index) => ({ month: monthOffset(index - 11), total }))

const fallbackOverdue = [
  { billNo: 'BILL-202606-011', amount: 3800, dueDate: '2026-06-01' },
  { billNo: 'BILL-202606-014', amount: 2500, dueDate: '2026-06-03' },
  { billNo: 'BILL-202605-022', amount: 2100, dueDate: '2026-05-28' }
]

const statCards = reactive([
  { label: '总房间数', value: 0, note: '覆盖全部楼栋房源', icon: 'OfficeBuilding', color: '#2563eb', bg: '#dbeafe' },
  { label: '已出租', value: 0, note: '当前有效租约房间', icon: 'User', color: '#0f9f6e', bg: '#d1fae5' },
  { label: '入住率', value: '0%', note: '运营健康度指标', icon: 'TrendCharts', color: '#d97706', bg: '#fef3c7' },
  { label: '累计收入', value: '¥0', note: '已支付账单金额', icon: 'Money', color: '#dc2626', bg: '#fee2e2' }
])

const overdueBills = ref<Array<{ billNo: string; amount: number; dueDate: string }>>([])
const latestAnnouncements = ref<Array<{ id: number; title: string; summary?: string; content?: string; topFlag?: number; createTime?: string }>>([])
const showcaseRooms = [
  { title: '阳光单间', meta: '25m2 / 南向 / ¥1500 起', image: '/images/room-standard.svg' },
  { title: '家庭套房', meta: '60m2 / 带阳台 / ¥3800 起', image: '/images/room-suite.svg' }
]

function monthOffset(offset: number) {
  const now = new Date()
  const date = new Date(now.getFullYear(), now.getMonth() + offset, 1)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function formatAmount(value: number | string) {
  return Number(value || 0).toLocaleString('zh-CN')
}

interface DashboardData {
  totalRooms?: number
  rentedRooms?: number
  totalPaid?: number
  roomStatus?: Array<{ status: number | string; count: number }>
}

function applyDashboard(d: DashboardData | null) {
  const data = d && d.totalRooms !== undefined ? d : fallbackDashboard
  const rateNum = data.totalRooms ? (data.rentedRooms! / data.totalRooms) * 100 : 0
  statCards[0].value = data.totalRooms || 0
  statCards[1].value = data.rentedRooms || 0
  statCards[2].value = `${rateNum.toFixed(1)}%`
  statCards[3].value = `¥${formatAmount(data.totalPaid || 0)}`
  renderRoomChart(data.roomStatus?.length ? data.roomStatus : fallbackDashboard.roomStatus)
  renderGauge(rateNum)
}

async function loadDashboard() {
  try {
    const res = await request.get<DashboardData>('/reports/dashboard')
    applyDashboard(res.data)
  } catch {
    if (userStore.token) applyDashboard(fallbackDashboard)
  }
}

async function loadRevenue() {
  try {
    const startMonth = monthOffset(-11)
    const endMonth = monthOffset(0)
    const res = await request.get<Array<{ month: string; total: number }>>('/bills/revenue', { params: { startMonth, endMonth } })
    renderRevenueChart(res.data?.length ? res.data : fallbackRevenue)
  } catch {
    if (userStore.token) renderRevenueChart(fallbackRevenue)
  }
}

async function loadOverdue() {
  try {
    const res = await request.get<{ records: Array<{ billNo: string; amount: number; dueDate: string }> }>('/bills/overdue', { params: { current: 1, size: 5 } })
    overdueBills.value = res.data?.records?.length ? res.data.records : fallbackOverdue
  } catch {
    if (userStore.token) overdueBills.value = fallbackOverdue
  }
}

async function loadAnnouncements() {
  try {
    const res: any = await request.get('/announcements/latest', { params: { limit: 5 } })
    latestAnnouncements.value = res.data || []
  } catch { latestAnnouncements.value = [] }
}

function renderRoomChart(roomStatus: Array<{ status: number | string; count: number }>) {
  if (!roomChartRef.value) return
  roomChart?.dispose()
  roomChart = echarts.init(roomChartRef.value)
  const statusNames: Record<string, string> = { 0: '空置', 1: '已租', 2: '维修', 3: '预定', Vacant: '空置', Rented: '已租', Maintenance: '维修', Reserved: '预定' }
  const statusColors: Record<string, string> = { 0: '#0f9f6e', 1: '#2563eb', 2: '#d97706', 3: '#64748b', Vacant: '#0f9f6e', Rented: '#2563eb', Maintenance: '#d97706', Reserved: '#64748b' }
  roomChart.setOption({
    color: ['#0f9f6e', '#2563eb', '#d97706', '#64748b'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, icon: 'circle' },
    series: [{
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      data: roomStatus.map((s) => ({
        name: statusNames[String(s.status)] || '未知',
        value: Number(s.count || 0),
        itemStyle: { color: statusColors[String(s.status)] || '#94a3b8' }
      })),
      label: { formatter: '{b} {d}%', color: '#42526b' }
    }]
  })
}

function renderRevenueChart(rows: Array<{ month: string; total: number }>) {
  if (!revenueChartRef.value) return
  revenueChart?.dispose()
  revenueChart = echarts.init(revenueChartRef.value)
  revenueChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (value: number | string) => `¥${formatAmount(value)}` },
    grid: { left: 44, right: 20, top: 28, bottom: 36 },
    xAxis: { type: 'category', data: rows.map((d) => d.month), axisLine: { lineStyle: { color: '#d8e0ea' } } },
    yAxis: { type: 'value', axisLabel: { formatter: (v: number) => `${v / 1000}k` }, splitLine: { lineStyle: { color: '#edf1f6' } } },
    series: [{
      type: 'line',
      smooth: true,
      symbolSize: 8,
      areaStyle: { color: 'rgba(37, 99, 235, 0.12)' },
      lineStyle: { width: 4, color: '#2563eb' },
      itemStyle: { color: '#2563eb' },
      data: rows.map((d) => Number(d.total || 0))
    }]
  })
}

function renderGauge(rateNum: number) {
  if (!gaugeChartRef.value) return
  gaugeChart?.dispose()
  gaugeChart = echarts.init(gaugeChartRef.value)
  gaugeChart.setOption({
    series: [{
      type: 'gauge',
      startAngle: 210,
      endAngle: -30,
      progress: { show: true, width: 16, itemStyle: { color: '#0f9f6e' } },
      axisLine: { lineStyle: { width: 16, color: [[0.5, '#fee2e2'], [0.8, '#fef3c7'], [1, '#d1fae5']] } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { color: '#687386', distance: 20 },
      pointer: { width: 5, itemStyle: { color: '#172033' } },
      detail: { valueAnimation: true, formatter: '{value}%', fontSize: 28, fontWeight: 800, color: '#172033', offsetCenter: [0, '62%'] },
      data: [{ value: Math.round(rateNum * 10) / 10 }]
    }]
  })
}

async function refreshAll() {
  await Promise.all([loadDashboard(), loadRevenue(), loadOverdue(), loadAnnouncements()])
  nextTick(handleResize)
}

function handleResize() {
  roomChart?.resize()
  revenueChart?.resize()
  gaugeChart?.resize()
}

onMounted(() => {
  refreshAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  roomChart?.dispose()
  revenueChart?.dispose()
  gaugeChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.hero {
  display: grid;
  min-height: 260px;
  grid-template-columns: minmax(360px, 0.95fr) minmax(420px, 1.05fr);
  overflow: hidden;
  background: #ffffff;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  box-shadow: 0 16px 38px rgba(30, 41, 59, 0.08);
}

.hero-copy {
  padding: 34px 38px;
}

.eyebrow {
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
  text-transform: uppercase;
}

.hero h1 {
  max-width: 520px;
  margin: 12px 0;
  color: #172033;
  font-size: 34px;
  line-height: 1.2;
  letter-spacing: 0;
}

.hero p {
  max-width: 560px;
  margin: 0;
  color: #687386;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
}

.hero-panel {
  min-height: 260px;
  background: #e8f1f3;
}

.hero-panel img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.stat-row {
  row-gap: 16px;
}

.stat-card-wrap {
  height: 100%;
}

.stat-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.stat-value {
  color: #172033;
  font-size: 28px;
  font-weight: 900;
  line-height: 1;
}

.stat-label {
  color: #687386;
  margin-top: 8px;
}

.stat-note {
  margin-top: 16px;
  color: #8792a2;
  font-size: 12px;
}

.stat-icon {
  display: inline-flex;
  width: 54px;
  height: 54px;
  flex: 0 0 54px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

.chart-card {
  height: 100%;
}

.chart {
  height: 320px;
}

.chart-large {
  height: 338px;
}

.chart-gauge {
  height: 286px;
}

.room-showcase {
  display: grid;
  gap: 14px;
}

.room-card {
  display: grid;
  grid-template-columns: 116px 1fr;
  gap: 14px;
  align-items: center;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #edf1f6;
  border-radius: 8px;
}

.room-card img {
  width: 116px;
  height: 76px;
  object-fit: cover;
  border-radius: 6px;
}

.room-card strong {
  display: block;
  color: #172033;
  font-size: 15px;
}

.room-card span {
  display: block;
  margin-top: 8px;
  color: #687386;
  font-size: 13px;
}

.announcement-list {
  max-height: 286px;
  overflow-y: auto;
}
.announcement-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.announcement-item:last-child {
  border-bottom: none;
}
.announcement-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}
.announcement-meta {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.announcement-time {
  font-size: 12px;
  color: #9ca3af;
}
</style>
