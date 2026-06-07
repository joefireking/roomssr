import { ref, reactive } from 'vue'
import request from '@/utils/request'

interface UseTableOptions<T extends Record<string, any> = {}> {
  /** API path, e.g. '/rooms/list' */
  url: string
  /** Extra query params beyond current/size */
  filters?: T
  /** Page size, default 10 */
  pageSize?: number
  /** If true, response.data is a plain array (no pagination) */
  flat?: boolean
}

type QueryType<T> = { current: number; size: number } & T

export function useTable<T extends Record<string, any> = {}>(options: UseTableOptions<T>) {
  const { url, filters = {} as T, pageSize = 10, flat = false } = options

  const loading = ref(false)
  const tableData = ref<any[]>([])
  const total = ref(0)

  const query = reactive({
    current: 1,
    size: pageSize,
    ...filters
  }) as QueryType<T>

  async function loadData() {
    loading.value = true
    try {
      const params: Record<string, any> = { ...query }
      Object.keys(params).forEach(k => {
        if (params[k] === null || params[k] === undefined || params[k] === '') {
          delete params[k]
        }
      })
      const res: any = await request.get(url, { params })
      if (flat) {
        tableData.value = res.data || []
        total.value = (res.data || []).length
      } else {
        tableData.value = res.data?.records || []
        total.value = res.data?.total || 0
      }
    } finally {
      loading.value = false
    }
  }

  function resetAndLoad() {
    query.current = 1
    loadData()
  }

  return { loading, tableData, total, query, loadData, resetAndLoad }
}
