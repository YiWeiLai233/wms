import { ref, reactive, onMounted } from 'vue'
import type { Ref } from 'vue'

/**
 * 表格分页通用逻辑
 * @param fetchApi 获取数据的 API 函数
 * @param defaultParams 默认搜索参数
 * @param immediate 是否立即加载
 */
export function useTable<T = any>(
  fetchApi: (params: any) => Promise<any>,
  defaultParams: Record<string, any> = {},
  immediate = true
) {
  const tableData: Ref<T[]> = ref([]) as Ref<T[]>
  const loading = ref(false)
  const pagination = reactive({
    page: 1,
    size: 10,
    total: 0,
  })
  const searchParams = reactive<Record<string, any>>({ ...defaultParams })

  async function fetchData() {
    loading.value = true
    try {
      const res = await fetchApi({
        page: pagination.page,
        size: pagination.size,
        ...searchParams,
      })
      tableData.value = res.data.list || []
      pagination.total = res.data.total || 0
    } catch {
      tableData.value = []
      pagination.total = 0
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {
    pagination.page = 1
    fetchData()
  }

  function handleReset() {
    Object.keys(searchParams).forEach((key) => {
      searchParams[key] = defaultParams[key] ?? undefined
    })
    pagination.page = 1
    fetchData()
  }

  function handlePageChange(page: number) {
    pagination.page = page
    fetchData()
  }

  function handleSizeChange(size: number) {
    pagination.size = size
    pagination.page = 1
    fetchData()
  }

  if (immediate) {
    onMounted(() => fetchData())
  }

  return {
    tableData,
    loading,
    pagination,
    searchParams,
    fetchData,
    handleSearch,
    handleReset,
    handlePageChange,
    handleSizeChange,
  }
}
