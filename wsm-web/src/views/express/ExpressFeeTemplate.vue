<template>
  <div class="page-container">
    <PageHeader title="费用模板管理">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增模板</el-button>
      </template>
    </PageHeader>

    <!-- 退货模板提醒 -->
    <el-alert type="info" :closable="false" class="mb-4">
      <template #title>
        <div class="text-sm">
          <span class="font-semibold">退货快递费提示：</span>
          <span>如需创建退货单时自动计算快递费，请先在</span>
          <el-button type="primary" link @click="$router.push('/express/companies')">快递公司管理</el-button>
          <span>中创建名称包含「退货」的快递公司，然后在此处为其创建费用模板。</span>
        </div>
      </template>
    </el-alert>

    <!-- 搜索 -->
    <div class="card mb-4">
      <el-form :model="searchParams" inline>
        <el-form-item label="快递公司">
          <el-select v-model="searchParams.companyId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="c in companyList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板名称">
          <el-input v-model="searchParams.name" placeholder="模板名称" clearable style="width: 150px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <el-table :data="tableData" v-loading="loading" stripe border>
        <el-table-column prop="companyName" label="快递公司" width="120" />
        <el-table-column prop="name" label="模板名称" min-width="100" />
        <el-table-column prop="templateType" label="模板类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.templateType === 'FIRST_CONTINUE' ? 'warning' : 'primary'" size="small">
              {{ row.templateType === 'FIRST_CONTINUE' ? '首重续重' : '阶梯计费' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isDefault" label="默认" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success" size="small">默认</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="View" @click="viewDetail(row)">详情</el-button>
            <el-button type="primary" link icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除该模板吗？" @confirm="handleDelete(row.id)">
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="模板详情" width="600px">
      <el-descriptions :column="2" border class="mb-4">
        <el-descriptions-item label="快递公司">{{ detail.companyName }}</el-descriptions-item>
        <el-descriptions-item label="模板名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="模板类型">
          <el-tag :type="detail.templateType === 'LADDER' ? 'primary' : 'warning'" size="small">
            {{ detail.templateType === 'LADDER' ? '阶梯计费' : '首重续重' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="是否默认">
          <el-tag v-if="detail.isDefault === 1" type="success" size="small">是</el-tag>
          <span v-else>否</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 首重续重详情 -->
      <template v-if="detail.templateType === 'FIRST_CONTINUE'">
        <h4 class="mb-2 text-sm font-semibold text-gray-700">首重续重配置</h4>
        <el-descriptions :column="2" border class="mb-4">
          <el-descriptions-item label="首重">{{ detail.firstWeight }} kg</el-descriptions-item>
          <el-descriptions-item label="首重费用">
            <span class="text-red-500 font-bold">¥{{ detail.firstFee?.toFixed(2) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="续重">{{ detail.additionalWeight }} kg</el-descriptions-item>
          <el-descriptions-item label="续重费用">
            <span class="text-red-500 font-bold">¥{{ detail.additionalFee?.toFixed(2) }}/kg</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <!-- 阶梯详情 -->
      <template v-if="detail.templateType === 'LADDER' || !detail.templateType">
        <h4 class="mb-2 text-sm font-semibold text-gray-700">费用阶梯</h4>
        <el-table :data="detail.steps || []" border size="small">
          <el-table-column prop="minWeight" label="最小重量(kg)" width="120" align="center">
            <template #default="{ row }">{{ row.minWeight }} kg</template>
          </el-table-column>
          <el-table-column prop="maxWeight" label="最大重量(kg)" width="120" align="center">
            <template #default="{ row }">{{ row.maxWeight }} kg</template>
          </el-table-column>
          <el-table-column prop="fee" label="费用(元)" min-width="100" align="center">
            <template #default="{ row }">
              <span class="text-red-500 font-bold">¥{{ row.fee.toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="750px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="快递公司" prop="companyId">
              <el-select v-model="form.companyId" placeholder="请选择公司" style="width: 100%">
                <el-option v-for="c in companyList" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板名称" prop="name">
              <el-input v-model="form.name" placeholder="如：标准费率" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="10">
            <el-form-item label="模板类型" prop="templateType">
              <el-select v-model="form.templateType" style="width: 100%">
                <el-option label="阶梯计费" value="LADDER" />
                <el-option label="首重续重" value="FIRST_CONTINUE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="设为默认" label-width="80px">
              <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="状态" label-width="60px">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>

        <!-- 阶梯计费 -->
        <template v-if="form.templateType === 'LADDER'">
          <el-divider content-position="left">费用阶梯</el-divider>
          <div v-for="(step, index) in form.steps" :key="index" class="flex items-center gap-2 mb-2">
            <el-input-number v-model="step.minWeight" :min="0" :precision="2" placeholder="最小重量" style="width: 130px" />
            <span class="text-gray-500">~</span>
            <el-input-number v-model="step.maxWeight" :min="0" :precision="2" placeholder="最大重量" style="width: 130px" />
            <span class="text-gray-500">kg，费用</span>
            <el-input-number v-model="step.fee" :min="0" :precision="2" placeholder="费用" style="width: 150px" />
            <span class="text-gray-500">元</span>
            <el-button type="danger" icon="Delete" circle @click="removeStep(index)" />
          </div>
          <el-button type="primary" icon="Plus" @click="addStep">添加阶梯</el-button>
        </template>

        <!-- 首重续重 -->
        <template v-if="form.templateType === 'FIRST_CONTINUE'">
          <el-divider content-position="left">首重续重配置</el-divider>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="首重(kg)">
                <el-input-number v-model="form.firstWeight" :min="0.1" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="首重费用(元)">
                <el-input-number v-model="form.firstFee" :min="0" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="续重(kg)">
                <el-input-number v-model="form.additionalWeight" :min="0.1" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="续重费用(元/kg)">
                <el-input-number v-model="form.additionalFee" :min="0" :precision="2" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getCompanyList, getTemplatePage, getTemplateDetail, createTemplate, updateTemplate, deleteTemplate } from '@/api/express'
import type { ExpressCompany, ExpressFeeTemplate, ExpressFeeStep } from '@/api/express'
import PageHeader from '@/components/PageHeader.vue'

const companyList = ref<ExpressCompany[]>([])
const tableData = ref<ExpressFeeTemplate[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })
const searchParams = reactive({ companyId: undefined as number | undefined, name: '' })

const detailVisible = ref(false)
const detail = ref<Partial<ExpressFeeTemplate>>({})

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  companyId: undefined as number | undefined,
  name: '',
  templateType: 'LADDER',
  firstWeight: 1,
  firstFee: 0,
  additionalWeight: 1,
  additionalFee: 0,
  isDefault: 0,
  status: 1,
  remark: '',
  steps: [] as ExpressFeeStep[],
})

const rules: FormRules = {
  companyId: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
}

onMounted(async () => {
  try {
    const res = await getCompanyList()
    companyList.value = res.data || []
  } catch {}
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getTemplatePage({
      page: pagination.page,
      size: pagination.size,
      companyId: searchParams.companyId || undefined,
      name: searchParams.name || undefined,
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
  searchParams.companyId = undefined
  searchParams.name = ''
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

async function viewDetail(row: ExpressFeeTemplate) {
  try {
    const res = await getTemplateDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch {}
}

function openDialog(row?: ExpressFeeTemplate) {
  isEdit.value = !!row
  if (row) {
    // 编辑时需要加载详情
    getTemplateDetail(row.id).then(res => {
      const data = res.data
      Object.assign(form, {
        id: data.id,
        companyId: data.companyId,
        name: data.name,
        templateType: data.templateType || 'LADDER',
        firstWeight: data.firstWeight || 1,
        firstFee: data.firstFee || 0,
        additionalWeight: data.additionalWeight || 1,
        additionalFee: data.additionalFee || 0,
        isDefault: data.isDefault,
        status: data.status,
        remark: data.remark,
        steps: data.steps || [],
      })
    })
  } else {
    Object.assign(form, {
      id: undefined,
      companyId: undefined,
      name: '',
      templateType: 'LADDER',
      firstWeight: 1,
      firstFee: 0,
      additionalWeight: 1,
      additionalFee: 0,
      isDefault: 0,
      status: 1,
      remark: '',
      steps: [{ minWeight: 1, maxWeight: 2, fee: 3, sortOrder: 0 }],
    })
  }
  dialogVisible.value = true
}

function addStep() {
  const lastStep = form.steps[form.steps.length - 1]
  const newMinWeight = lastStep ? lastStep.maxWeight : 1
  form.steps.push({
    minWeight: newMinWeight,
    maxWeight: newMinWeight + 1,
    fee: 0,
    sortOrder: form.steps.length,
  })
}

function removeStep(index: number) {
  form.steps.splice(index, 1)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // 阶梯计费类型需要检查 steps
  if (form.templateType === 'LADDER' && form.steps.length === 0) {
    ElMessage.warning('请至少添加一个费用阶梯')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateTemplate(form)
      ElMessage.success('修改成功')
    } else {
      await createTemplate(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteTemplate(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}
</script>
