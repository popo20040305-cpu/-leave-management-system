<script setup lang="ts">
/**
 * 角色管理（菜单「系统管理 -> 角色管理」，url = /role/index，文档第 14 章）。
 *
 * 结构和学院管理一致：搜索（角色名模糊匹配）+ 表格 + 新增/修改共用弹窗 + 删除二次确认，
 * 外加本模块特有的「指定角色权限」—— 弹窗里一棵带勾选联动的 el-tree，
 * 叶子是按钮/菜单，父节点由联动自动推导。
 *
 * 权限串（与后端 ApiRoleController 一致）：
 *   system:role:query / add / update / delete，指定权限 system:role:zdjsqx
 */
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { TreeInstance } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import Pagination from '@/components/Pagination.vue'
import { usePageTable } from '@/composables/usePageTable'
import { usePermission } from '@/utils/usePermissionUtil'
import { useBaseInfoStore } from '@/stores/baseInfoStore'
import { deleteRole, getPermTree, getRoleInfo, getRoleList, savePerm, saveRole } from '@/api/roleApi'
import { emptyRoleForm, type RolePojo } from '@/type/rolePojo'
import type { MenuVo } from '@/type/menu'

const baseInfoStore = useBaseInfoStore()
const { hasPermission } = usePermission()

const PERM_ADD = 'system:role:add'
const PERM_UPDATE = 'system:role:update'
const PERM_DELETE = 'system:role:delete'
const PERM_SAVE_PERM = 'system:role:zdjsqx'

const canAdd = computed(() => hasPermission(PERM_ADD))
const canUpdate = computed(() => hasPermission(PERM_UPDATE))
const canDelete = computed(() => hasPermission(PERM_DELETE))
const canSavePerm = computed(() => hasPermission(PERM_SAVE_PERM))

/* ---------------- 列表 ---------------- */

const page = usePageTable<RolePojo, { name: string }>((params) => getRoleList(params), {
  query: { name: '' },
})

/** 角色类型字典：{ id -> name }，表格列直接查表渲染 */
const roleTypeName = (roleType: number | undefined) => {
  if (roleType == null) return '-'
  return baseInfoStore.roleTypes.find((item) => item.id === roleType)?.name ?? String(roleType)
}

/* ---------------- 新增 / 修改弹窗 ---------------- */

const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<RolePojo>(emptyRoleForm())

const rules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleType: [{ required: true, message: '请选择角色类型', trigger: 'change' }],
}

const openAdd = async () => {
  Object.assign(form, emptyRoleForm())
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const openEdit = async (row: RolePojo) => {
  const id = row.id
  if (id == null) return

  // 先用列表行数据把弹窗填上（立刻可见），再拉一次详情覆盖
  Object.assign(form, { id, name: row.name, remark: row.remark ?? '', roleType: row.roleType })
  dialogTitle.value = '修改角色'
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()

  try {
    const latest = await getRoleInfo(id)
    Object.assign(form, { name: latest.name, remark: latest.remark ?? '', roleType: latest.roleType })
  } catch {
    // 拉不到就沿用列表里的值，错误提示拦截器已弹过
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: RolePojo = {
      id: form.id,
      name: form.name.trim(),
      remark: (form.remark ?? '').trim(),
      roleType: form.roleType,
    }
    const isAdd = payload.id == null
    await saveRole(payload)
    ElMessage.success(isAdd ? '新增成功' : '修改成功')
    dialogVisible.value = false
    await page.load()
  } catch {
    // 重名（50801）、无权限（403）等业务错误，拦截器已弹过
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row: RolePojo) => {
  const id = row.id
  if (id == null) return

  try {
    await ElMessageBox.confirm(
      `确定删除角色「${row.name}」吗？该角色的权限和用户关联会一并清除。`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }

  try {
    await deleteRole(id)
    ElMessage.success('删除成功')
    await page.reloadAfterDelete()
  } catch {
    // 错误提示拦截器已弹过
  }
}

/* ---------------- 指定角色权限（权限树弹窗） ---------------- */

const permVisible = ref(false)
const permLoading = ref(false)
const permSaving = ref(false)
const permRoleId = ref<number | null>(null)
const permRoleName = ref('')
const permTreeRef = ref<TreeInstance>()
const permTreeData = ref<MenuVo[]>([])
/** 后端回传的叶子权限 id，作为 el-tree 的默认勾选 */
const checkedMenuIds = ref<number[]>([])

const treeProps = { children: 'subMenus', label: 'name' }

const openPerm = async (row: RolePojo) => {
  const id = row.id
  if (id == null) return

  permRoleId.value = id
  permRoleName.value = row.name
  permVisible.value = true
  permLoading.value = true
  permTreeData.value = []
  checkedMenuIds.value = []

  try {
    const res = await getPermTree(id)
    permTreeData.value = res.tree ?? []
    checkedMenuIds.value = res.checkedIds ?? []
    // 树渲染出来后把已勾选的节点滚进视野
    await nextTick()
    checkedMenuIds.value.forEach((menuId) => permTreeRef.value?.setCurrentKey(menuId))
  } catch {
    // 拉不到就让弹窗空着，错误提示拦截器已弹过
  } finally {
    permLoading.value = false
  }
}

/**
 * 取当前勾选的叶子节点 id。
 *
 * getCheckedKeys() 在勾选联动开启时会把「父节点 + 叶子」一起返回，
 * 这里只留叶子：父节点存进 role_menu 没有意义（后端会按 pids 自动补父），
 * 而且混着存会让下次回显的剔除逻辑白做功。
 * 判断依据是节点有没有 subMenus —— 没有子节点的就是叶子。
 */
const collectLeafKeys = (nodes: MenuVo[], allKeys: (string | number)[]): number[] => {
  const leaves: number[] = []
  const walk = (list: MenuVo[]) => {
    for (const node of list) {
      if (node.subMenus && node.subMenus.length) {
        walk(node.subMenus)
      } else if (node.id != null && allKeys.includes(node.id)) {
        leaves.push(node.id)
      }
    }
  }
  walk(nodes)
  return leaves
}

const handlePermSubmit = async () => {
  if (permRoleId.value == null) return

  // 树是勾选联动的，用 getCheckedKeys 拿「父 + 叶」，再过滤成叶子
  const checkedKeys = permTreeRef.value?.getCheckedKeys() ?? []
  const halfCheckedKeys = permTreeRef.value?.getHalfCheckedKeys() ?? []
  const leafIds = collectLeafKeys(permTreeData.value, [...checkedKeys, ...halfCheckedKeys])

  permSaving.value = true
  try {
    await savePerm(permRoleId.value, leafIds)
    ElMessage.success('权限保存成功')
    permVisible.value = false
  } catch {
    // 错误提示拦截器已弹过
  } finally {
    permSaving.value = false
  }
}

/* ---------------- 生命周期 ---------------- */

onMounted(() => {
  // 进页面前确保字典已加载（角色类型下拉要用）。失败不影响主流程
  if (!baseInfoStore.loaded) {
    void baseInfoStore.load().catch((error) => console.warn('基础数据加载失败:', error))
  }
})
</script>

<template>
  <div class="role-page">
    <el-card shadow="never">
      <template #header>
        <span class="card-title">角色管理</span>
      </template>

      <!-- 查询栏 -->
      <el-form :inline="true" class="search-bar" @submit.prevent>
        <el-form-item label="角色名称">
          <el-input
            v-model="page.form.name"
            placeholder="支持模糊查询"
            clearable
            style="width: 180px"
            @keyup.enter="page.search"
            @clear="page.search"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="page.search">查询</el-button>
          <el-button :icon="Refresh" @click="page.reset">重置</el-button>
          <el-button v-if="canAdd" type="success" :icon="Plus" @click="openAdd">新增角色</el-button>
        </el-form-item>
      </el-form>

      <!-- 列表 -->
      <el-table :data="page.records" v-loading="page.loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="角色名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="角色类型" width="140" align="center">
          <template #default="{ row }">{{ roleTypeName(row.roleType) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canSavePerm" link type="warning" @click="openPerm(row)">权限</el-button>
            <el-button v-if="canUpdate" link type="primary" @click="openEdit(row)">修改</el-button>
            <el-button v-if="canDelete" link type="danger" @click="handleDelete(row)">删除</el-button>
            <span v-if="!canUpdate && !canDelete && !canSavePerm" class="no-perm">无操作权限</span>
          </template>
        </el-table-column>
        <template #empty>暂无数据</template>
      </el-table>

      <Pagination
        v-model:current="page.current"
        v-model:size="page.size"
        :total="page.total"
        @change="page.onPageChange"
      />
    </el-card>

    <!-- 新增 / 修改弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色类型" prop="roleType">
          <el-select v-model="form.roleType" placeholder="请选择角色类型" style="width: 100%">
            <el-option
              v-for="item in baseInfoStore.roleTypes"
              :key="item.id"
              :label="item.name"
              :value="item.id!"
            />
          </el-select>
          <div class="field-tip">角色类型决定数据可见范围：个人 / 班级 / 学院 / 全部</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 指定角色权限弹窗 -->
    <el-dialog
      v-model="permVisible"
      :title="`指定角色权限 - ${permRoleName}`"
      width="560px"
      :close-on-click-modal="false"
    >
      <div v-loading="permLoading" class="perm-tree-box">
        <el-tree
          ref="permTreeRef"
          :data="permTreeData"
          :props="treeProps"
          node-key="id"
          show-checkbox
          default-expand-all
          :default-checked-keys="checkedMenuIds"
          empty-text="暂无权限数据"
        />
        <div class="field-tip">
          勾选子权限时父级会自动勾上（父级决定菜单是否显示，保存时会一并存入）
        </div>
      </div>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSaving" @click="handlePermSubmit">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.role-page {
  min-height: 100%;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.search-bar {
  margin-bottom: 4px;
}

.search-bar :deep(.el-form-item) {
  margin-bottom: 12px;
}

.no-perm {
  color: #909399;
  font-size: 12px;
}

.field-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.perm-tree-box {
  min-height: 260px;
  max-height: 420px;
  overflow-y: auto;
  padding: 4px 8px;
}
</style>
