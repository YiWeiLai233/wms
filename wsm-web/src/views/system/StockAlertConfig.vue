<template>
  <div class="page-container">
    <PageHeader title="库存预警设置" subtitle="配置SKU库存预警阈值" />

    <el-tabs v-model="activeTab" class="mb-4">
      <el-tab-pane label="预警配置" name="config" />
      <el-tab-pane label="低库存商品" name="lowStock" />
      <el-tab-pane label="缺货商品" name="outOfStock" />
    </el-tabs>

    <!-- 预警配置 Tab -->
    <template v-if="activeTab === 'config'">
      <div class="card mb-4">
        <el-form :model="searchParams" inline>
          <el-form-item label="SKU编码">
            <el-input v-model="searchParams.skuCode" placeholder="SKU编码" clearable style="width: 140px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="SKU名称">
            <el-input v-model="searchParams.skuName" placeholder="SKU名称" clearable style="width: 140px" @keyup.enter="handleSearch" />
          </el-form-item>
          <el-form-item label="仓库">
            <el-select v-model="searchParams.warehouseId" placeholder="全部" clearable style="width: 130px">
              <el-option label="通用（所有仓库）" :value="0" />
              <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchParams.enabled" placeholder="全部" clearable style="width: 100px">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
            <el-button icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="card">
        <div class="mb-4">
          <el-button type="primary" icon="Plus" @click="openDialog()">新增预警配置</el-button>
        </div>

        <el-table :data="tableData" v-loading="loading" stripe border>
          <el-table-column prop="skuCode" label="SKU编码" width="130" />
          <el-table-column prop="skuName" label="SKU名称" min-width="120" show-overflow-tooltip />
          <el-table-column prop="warehouseName" label="仓库" width="120">
            <template #default="{ row }">
              {{ row.warehouseName || '通用（所有仓库）' }}
            </template>
          </el-table-column>
          <el-table-column prop="lowStockThreshold" label="低库存阈值" width="110" align="center" />
          <el-table-column prop="outOfStockThreshold" label="缺货阈值" width="100" align="center" />
          <el-table-column prop="enabled" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.enabled === 1 ? 'success' : 'danger'" size="small">
                {{ row.enabled === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
          <el-table-column prop="updatedAt" label="更新时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link icon="Edit" @click="openDialog(row)">编辑</el-button>
              <el-button :type="row.enabled === 1 ? 'warning' : 'success'" link @click="handleToggleEnabled(row)">
                {{ row.enabled === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-popconfirm title="确认删除该预警配置？" @confirm="handleDelete(row.id)">
                <template #reference>
                  <el-button type="danger" link icon="Delete">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <div class="mt-4 flex justify-end">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </template>

    <!-- 低库存商品 Tab -->
    <template v-if="activeTab === 'lowStock'">
      <div class="card">
        <el-table :data="lowStockList" v-loading="lowStockLoading" stripe border>
          <el-table-column prop="skuCode" label="SKU编码" width="130" />
          <el-table-column prop="skuName" label="SKU名称" min-width="120" show-overflow-tooltip />
          <el-table-column prop="warehouseName" label="仓库" width="120" />
          <el-table-column prop="quantity" label="当前库存" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="warning" size="small">{{ row.quantity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lowStockThreshold" label="低库存阈值" width="110" align="center" />
          <el-table-column prop="outOfStockThreshold" label="缺货阈值" width="100" align="center" />
          <el-table-column prop="alertStatusName" label="预警状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="warning" size="small">{{ row.alertStatusName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!lowStockLoading && lowStockList.length === 0" description="暂无低库存商品" />
      </div>
    </template>

    <!-- 缺货商品 Tab -->
    <template v-if="activeTab === 'outOfStock'">
      <div class="card">
        <el-table :data="outOfStockList" v-loading="outOfStockLoading" stripe border>
          <el-table-column prop="skuCode" label="SKU编码" width="130" />
          <el-table-column prop="skuName" label="SKU名称" min-width="120" show-overflow-tooltip />
          <el-table-column prop="warehouseName" label="仓库" width="120" />
          <el-table-column prop="quantity" label="当前库存" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="danger" size="small">{{ row.quantity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="outOfStockThreshold" label="缺货阈值" width="100" align="center" />
          <el-table-column prop="alertStatusName" label="预警状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="danger" size="small">{{ row.alertStatusName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!outOfStockLoading && outOfStockList.length === 0" description="暂无缺货商品" />
      </div>
    </template>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑预警配置' : '新增预警配置'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="SKU" prop="skuId">
          <el-select
            v-model="formData.skuId"
            placeholder="搜索SKU"
            filterable
            remote
            :remote-method="searchSku"
            :loading="skuLoading"
            style="width: 100%"
            :disabled="isEdit"
          >
            <el-option v-for="sku in skuOptions" :key="sku.id" :label="`${sku.skuCode} - ${sku.name}`" :value="sku.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="formData.warehouseId" placeholder="通用（所有仓库）" clearable style="width: 100%" :disabled="isEdit">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
          <div class="form-tip">不选择仓库表示该规则适用于所有仓库</div>
        </el-form-item>
        <el-form-item label="低库存阈值" prop="lowStockThreshold">
          <el-input-number v-model="formData.lowStockThreshold" :min="0" :max="999999" style="width: 180px" />
        </el-form-item>
        <el-form-item label="缺货阈值" prop="outOfStockThreshold">
          <el-input-number v-model="formData.outOfStockThreshold" :min="0" :max="999999" style="width: 180px" />
          <div class="form-tip">缺货阈值不能大于低库存阈值</div>
        </el-form-item>
        <el-form-item label="启用状态" prop="enabled">
          <el-switch v-model="formData.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="备注说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getStockAlertConfigs,
  createStockAlertConfig,
  updateStockAlertConfig,
  deleteStockAlertConfig,
  updateStockAlertEnabled,
  getLowStockList,
  getOutOfStockList,
} from '@/api/stockAlert'
import type { StockAlertConfig, StockAlertStatus } from '@/api/stockAlert'
import { getWarehouseList } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { getAllSkuList } from '@/api/product'
import type { Sku } from '@/api/product'
import { useTable } from '@/composables/useTable'
import { formatDateTime } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

const activeTab = ref('config')

const { tableData, loading, pagination, searchParams, handleSearch, handleReset, handlePageChange, handleSizeChange, fetchData } = useTable<StockAlertConfig>(getStockAlertConfigs)

const warehouses = ref<Warehouse[]>([])
const skuOptions = ref<Sku[]>([])
const skuLoading = ref(false)

// 低库存/缺货列表
const lowStockList = ref<StockAlertStatus[]>([])
const lowStockLoading = ref(false)
const outOfStockList = ref<StockAlertStatus[]>([])
const outOfStockLoading = ref(false)

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({
  skuId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  lowStockThreshold: 10,
  outOfStockThreshold: 0,
  enabled: 1,
  remark: '',
})

const validateThreshold = (_rule: any, _value: any, callback: any) => {
  if (formData.outOfStockThreshold > formData.lowStockThreshold) {
    callback(new Error('缺货阈值不能大于低库存阈值'))
  } else {
    callback()
  }
}

const formRules: FormRules = {
  skuId: [{ required: true, message: '请选择SKU', trigger: 'change' }],
  lowStockThreshold: [{ required: true, message: '请输入低库存阈值', trigger: 'change' }],
  outOfStockThreshold: [
    { required: true, message: '请输入缺货阈值', trigger: 'change' },
    { validator: validateThreshold, trigger: 'change' },
  ],
}

onMounted(async () => {
  await loadWarehouses()
})

watch(activeTab, (tab) => {
  if (tab === 'lowStock') loadLowStock()
  if (tab === 'outOfStock') loadOutOfStock()
})

async function loadWarehouses() {
  try {
    const res = await getWarehouseList({ page: 1, size: 100 })
    warehouses.value = res.data.list || []
  } catch {
    warehouses.value = []
  }
}

async function searchSku(keyword: string) {
  skuLoading.value = true
  try {
    const res = await getAllSkuList({ keyword: keyword || undefined, page: 1, size: 50 })
    skuOptions.value = res.data?.list || []
  } catch {
    skuOptions.value = []
  } finally {
    skuLoading.value = false
  }
}

async function loadLowStock() {
  lowStockLoading.value = true
  try {
    const res = await getLowStockList()
    lowStockList.value = res.data || []
  } catch {
    lowStockList.value = []
  } finally {
    lowStockLoading.value = false
  }
}

async function loadOutOfStock() {
  outOfStockLoading.value = true
  try {
    const res = await getOutOfStockList()
    outOfStockList.value = res.data || []
  } catch {
    outOfStockList.value = []
  } finally {
    outOfStockLoading.value = false
  }
}

function resetForm() {
  Object.assign(formData, {
    skuId: undefined,
    warehouseId: undefined,
    lowStockThreshold: 10,
    outOfStockThreshold: 0,
    enabled: 1,
    remark: '',
  })
  skuOptions.value = []
}

function openDialog(row?: StockAlertConfig) {
  resetForm()
  if (row) {
    isEdit.value = true
    editId.value = row.id
    Object.assign(formData, {
      skuId: row.skuId,
      warehouseId: row.warehouseId || undefined,
      lowStockThreshold: row.lowStockThreshold,
      outOfStockThreshold: row.outOfStockThreshold,
      enabled: row.enabled,
      remark: row.remark,
    })
    // 加载SKU选项
    skuOptions.value = [{ id: row.skuId, skuCode: row.skuCode, name: row.skuName } as any]
  } else {
    isEdit.value = false
    editId.value = null
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !formData.skuId) return

  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      await updateStockAlertConfig(editId.value, {
        lowStockThreshold: formData.lowStockThreshold,
        outOfStockThreshold: formData.outOfStockThreshold,
        enabled: formData.enabled,
        remark: formData.remark,
      })
      ElMessage.success('修改成功')
    } else {
      await createStockAlertConfig({
        skuId: formData.skuId,
        warehouseId: formData.warehouseId || null,
        lowStockThreshold: formData.lowStockThreshold,
        outOfStockThreshold: formData.outOfStockThreshold,
        enabled: formData.enabled,
        remark: formData.remark,
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}

async function handleToggleEnabled(row: StockAlertConfig) {
  try {
    await updateStockAlertEnabled(row.id, row.enabled === 1 ? 0 : 1)
    ElMessage.success(row.enabled === 1 ? '已禁用' : '已启用')
    fetchData()
  } catch {}
}

async function handleDelete(id: number) {
  try {
    await deleteStockAlertConfig(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
