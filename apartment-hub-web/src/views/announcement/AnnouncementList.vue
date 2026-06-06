<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>Announcement Management</span>
          <el-button type="success" @click="openDialog()">New Announcement</el-button>
        </div>
      </template>

      <el-form inline style="margin-bottom:16px">
        <el-form-item label="Status">
          <el-select v-model="query.status" placeholder="All" clearable @change="loadData">
            <el-option label="Published" :value="1" />
            <el-option label="Draft" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="query.current = 1; loadData()">Search</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="title" label="Title" min-width="200" show-overflow-tooltip />
        <el-table-column prop="summary" label="Summary" min-width="250" show-overflow-tooltip />
        <el-table-column prop="status" label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? 'Published' : 'Draft' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="topFlag" label="Pinned" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.topFlag === 1" type="warning" size="small">Top</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="Created" width="170" />
        <el-table-column label="Actions" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">Edit</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="No announcements" />

      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.current"
        v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next"
        @current-change="loadData" @size-change="loadData" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? 'Edit Announcement' : 'New Announcement'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="Title" required>
          <el-input v-model="form.title" placeholder="Enter title" />
        </el-form-item>
        <el-form-item label="Summary">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="Brief summary" />
        </el-form-item>
        <el-form-item label="Content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="Full content" />
        </el-form-item>
        <el-form-item label="Status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">Published</el-radio>
            <el-radio :value="0">Draft</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Pinned">
          <el-switch v-model="form.topFlag" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :disabled="!form.title" @click="handleSave">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ status: null as any, current: 1, size: 10 })
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const form = reactive({ title: '', summary: '', content: '', status: 1, topFlag: 0 })

async function loadData() {
  loading.value = true
  try {
    const res: any = await request.get('/announcements/list', { params: query })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

function openDialog(row?: any) {
  if (row) {
    editId.value = row.id
    form.title = row.title
    form.summary = row.summary || ''
    form.content = row.content || ''
    form.status = row.status
    form.topFlag = row.topFlag || 0
  } else {
    editId.value = null
    form.title = ''
    form.summary = ''
    form.content = ''
    form.status = 1
    form.topFlag = 0
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.title.trim()) { ElMessage.warning('Title is required'); return }
  try {
    if (editId.value) {
      await request.put(`/announcements/${editId.value}`, form)
    } else {
      await request.post('/announcements', form)
    }
    ElMessage.success('Saved')
    dialogVisible.value = false
    loadData()
  } catch { /* handled by interceptor */ }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('Delete this announcement?', 'Confirm')
    await request.delete(`/announcements/${row.id}`)
    ElMessage.success('Deleted')
    loadData()
  } catch { /* cancelled */ }
}

onMounted(loadData)
</script>
