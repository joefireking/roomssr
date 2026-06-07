<template>
  <div>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>公告管理</span>
          <el-button type="success" @click="openDialog()">新建公告</el-button>
        </div>
      </template>

      <el-form inline style="margin-bottom:16px">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable @change="resetAndLoad">
            <el-option label="已发布" :value="1" />
            <el-option label="草稿" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="resetAndLoad">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="summary" label="摘要" min-width="250" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="topFlag" label="置顶" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.topFlag === 1" type="warning" size="small">置顶</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无公告" />

      <el-pagination style="margin-top:16px;justify-content:flex-end" v-model:current-page="query.current"
        v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next"
        @current-change="loadData" @size-change="loadData" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑公告' : '新建公告'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="简要摘要" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="完整内容" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">已发布</el-radio>
            <el-radio :value="0">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.topFlag" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!form.title" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useTable } from '@/composables/useTable'

const { loading, tableData, total, query, loadData, resetAndLoad } = useTable<{ status: any }>({
  url: '/announcements/list',
  filters: { status: null }
})
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const form = reactive({ title: '', summary: '', content: '', status: 1, topFlag: 0 })

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
  if (!form.title.trim()) { ElMessage.warning('请输入标题'); return }
  try {
    if (editId.value) {
      await request.put(`/announcements/${editId.value}`, form)
    } else {
      await request.post('/announcements', form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } catch { /* handled by interceptor */ }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确定删除该公告？', '确认')
    await request.delete(`/announcements/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

onMounted(loadData)
</script>
