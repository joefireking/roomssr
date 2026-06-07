<template>
  <div>
    <el-card>
      <el-form inline>
        <el-form-item label="Module"><el-input v-model="query.module" placeholder="Module" clearable /></el-form-item>
        <el-form-item label="User"><el-input v-model="query.username" placeholder="Username" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="query.current = 1; loadData()">Search</el-button></el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="username" label="User" />
        <el-table-column prop="module" label="Module" />
        <el-table-column prop="operation" label="Operation" />
        <el-table-column prop="method" label="Method" width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" />
        <el-table-column prop="duration" label="Duration">
          <template #default="{ row }">{{ row.duration }}ms</template>
        </el-table-column>
        <el-table-column prop="status" label="Status">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? 'OK' : 'Fail' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="Time" />
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />

      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.current"
        v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next"
        @current-change="loadData" @size-change="loadData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useTable } from '@/composables/useTable'

const { loading, tableData, total, query, loadData } = useTable<{ module: string; username: string }>({
  url: '/sys/logs/list',
  filters: { module: '', username: '' }
})

onMounted(loadData)
</script>
