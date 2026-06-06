<template>
  <el-dialog v-model="visible" title="Electronic Contract" width="800px" destroy-on-close>
    <div v-loading="loading" class="contract-content" v-html="contractHtml" />
    <template #footer>
      <el-button @click="visible = false">Close</el-button>
      <el-button type="primary" @click="handleDownload">Download Contract</el-button>
      <el-button @click="handlePrint">Print</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const props = defineProps<{ contractId: number | null }>()
const visible = defineModel<boolean>('visible', { default: false })
const loading = ref(false)
const contractHtml = ref('')

watch(() => props.contractId, async (id) => {
  if (id && visible.value) {
    loading.value = true
    try {
      const res: any = await request.get(`/contracts/${id}/agreement`)
      contractHtml.value = res.data
    } catch { contractHtml.value = '<p>Failed to load contract</p>' }
    finally { loading.value = false }
  }
})

watch(visible, async (v) => {
  if (v && props.contractId) {
    loading.value = true
    try {
      const res: any = await request.get(`/contracts/${props.contractId}/agreement`)
      contractHtml.value = res.data
    } catch { contractHtml.value = '<p>Failed to load contract</p>' }
    finally { loading.value = false }
  }
})

function handleDownload() {
  if (!props.contractId) return
  const token = localStorage.getItem('token')
  const link = document.createElement('a')
  link.href = `/api/contracts/${props.contractId}/agreement/download`
  link.setAttribute('download', '')
  if (token) {
    fetch(`/api/contracts/${props.contractId}/agreement/download`, {
      headers: { Authorization: `Bearer ${token}` }
    }).then(r => r.blob()).then(blob => {
      const url = URL.createObjectURL(blob)
      link.href = url
      link.click()
      URL.revokeObjectURL(url)
    }).catch(() => ElMessage.error('Download failed'))
  } else {
    link.click()
  }
}

function handlePrint() {
  const printWindow = window.open('', '_blank')
  if (printWindow) {
    printWindow.document.write(contractHtml.value)
    printWindow.document.close()
    printWindow.print()
  }
}
</script>

<style scoped>
.contract-content {
  max-height: 60vh;
  overflow-y: auto;
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 4px;
  background: #fff;
}
</style>
