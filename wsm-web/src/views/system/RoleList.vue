<template>
  <div class="page-container">
    <PageHeader title="角色管理" subtitle="管理角色和权限配置">
      <template #actions>
        <el-button type="primary" icon="Plus" @click="openDialog()">新增角色</el-button>
      </template>
    </PageHeader>

    <div class="card">
      <el-table :data="roleList" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link icon="Edit" @click="openDialog(row)">编辑</el-button>
            <el-button type="primary" link icon="Setting" @click="openPermissionDialog(row)">配置权限</el-button>
            <el-popconfirm title="确定删除该角色吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 角色编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="如 SUPER_ADMIN" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如 超级管理员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置弹窗 -->
    <el-dialog v-model="permissionDialogVisible" title="配置权限" width="600px" destroy-on-close>
      <div class="mb-3 text-sm text-gray-500">
        为角色 <strong>{{ currentRole?.roleName }}</strong> 配置可访问的功能模块
      </div>
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        :props="{ label: 'permissionName', children: 'children' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedPermissionIds"
        default-expand-all
      />
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPermissions" @click="handleSavePermissions">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { ElTree } from 'element-plus'
import {
  getRoleList, createRole, updateRole, deleteRole,
  getPermissionTree, getRolePermissionIds
} from '@/api/permission'
import type { Permission, Role, RoleForm } from '@/api/permission'
import PageHeader from '@/components/PageHeader.vue'

const loading = ref(false)
const roleList = ref<Role[]>([])

// 角色编辑相关
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<RoleForm>({
  roleCode: '',
  roleName: '',
  description: '',
  permissionIds: []
})
const rules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

// 权限配置相关
const permissionDialogVisible = ref(false)
const savingPermissions = ref(false)
const permissionTree = ref<Permission[]>([])
const checkedPermissionIds = ref<number[]>([])
const currentRole = ref<Role | null>(null)
const treeRef = ref<InstanceType<typeof ElTree>>()

async function fetchRoleList() {
  loading.value = true
  try {
    const res = await getRoleList()
    roleList.value = res.data || []
  } catch {
    roleList.value = []
  } finally {
    loading.value = false
  }
}

async function fetchPermissionTree() {
  try {
    const res = await getPermissionTree()
    permissionTree.value = res.data || []
  } catch {
    permissionTree.value = []
  }
}

function openDialog(row?: Role) {
  if (row) {
    form.id = row.id
    form.roleCode = row.roleCode
    form.roleName = row.roleName
    form.description = row.description || ''
  } else {
    form.id = undefined
    form.roleCode = ''
    form.roleName = ''
    form.description = ''
  }
  form.permissionIds = []
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (form.id) {
      await updateRole(form)
      ElMessage.success('更新成功')
    } else {
      await createRole(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await fetchRoleList()
  } catch {} finally {
    saving.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteRole(id)
    ElMessage.success('删除成功')
    await fetchRoleList()
  } catch {}
}

async function openPermissionDialog(role: Role) {
  currentRole.value = role
  checkedPermissionIds.value = []

  try {
    const res = await getRolePermissionIds(role.id)
    checkedPermissionIds.value = res.data || []
  } catch {
    checkedPermissionIds.value = []
  }

  permissionDialogVisible.value = true

  // 等待树渲染后设置选中状态
  await nextTick()
  if (treeRef.value) {
    treeRef.value.setCheckedKeys(checkedPermissionIds.value)
  }
}

async function handleSavePermissions() {
  if (!currentRole.value || !treeRef.value) return

  savingPermissions.value = true
  try {
    // 获取选中的叶子节点ID
    const checkedKeys = treeRef.value.getCheckedKeys(true) as number[]
    const halfCheckedKeys = treeRef.value.getHalfCheckedKeys() as number[]
    const allCheckedKeys = [...new Set([...checkedKeys, ...halfCheckedKeys])]

    await updateRole({
      id: currentRole.value.id,
      roleCode: currentRole.value.roleCode,
      roleName: currentRole.value.roleName,
      description: currentRole.value.description || '',
      permissionIds: allCheckedKeys
    })
    ElMessage.success('权限配置成功')
    permissionDialogVisible.value = false
  } catch {} finally {
    savingPermissions.value = false
  }
}

onMounted(() => {
  fetchRoleList()
  fetchPermissionTree()
})
</script>
