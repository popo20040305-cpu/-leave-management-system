<script setup lang="ts">
/**
 * 菜单管理（菜单「系统管理 -> 权限管理」，url = /menu/list，文档第 13 章）。
 *
 * 左树右表结构：左侧 el-tree 展示完整菜单树（目录 -> 菜单 -> 按钮），
 * 每个节点上直接放「＋ 添加子项 / ↑ 上移 / ↓ 下移 / ✕ 删除」；
 * 右侧是当前选中节点的编辑表单（新增时是待建草稿，修改时是回显的详情）。
 *
 * 权限串必须和后端 ApiMenuController 上的 @RequiresPermissions 一致：
 *   system:menu:query（进入本页即可看）  system:menu:add
 *   system:menu:update（改和移动共用）   system:menu:delete
 * 上移下移没有单独的权限项，复用 update。
 */
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useMenuStore } from '@/stores/menuStore'
import { usePermission } from '@/utils/usePermissionUtil'
import type { MenuPojo, MenuVo } from '@/type/menu'

const store = useMenuStore()
const { hasPermission } = usePermission()

const PERM_ADD = 'system:menu:add'
const PERM_UPDATE = 'system:menu:update'
const PERM_DELETE = 'system:menu:delete'

const canAdd = computed(() => hasPermission(PERM_ADD))
const canUpdate = computed(() => hasPermission(PERM_UPDATE))
const canDelete = computed(() => hasPermission(PERM_DELETE))

/* ---------------- 树 ---------------- */

const treeRef = ref()

const defaultProps = {
  children: 'subMenus',
  label: 'name',
}

/** 树节点类型图标：1 目录 / 2 菜单 / 3 按钮 */
const typeIcon = (type: number) => (type === 1 ? '📁' : type === 2 ? '📄' : '🔘')

const handleNodeClick = (data: MenuVo) => {
  if (data.id) {
    store.getDetail(data.id)
  }
}

/* ---------------- 右侧表单 ---------------- */

const formRef = ref<FormInstance>()

/**
 * 表单用 id=0 表示「新建」（树节点点击要能整份赋值进来，不能是 undefined），
 * 提交时 store.save() 会把 0 抹成 null，后端按「id 为空」当新增处理。
 */
const formData = reactive<MenuPojo>({
  id: 0,
  name: '',
  permission: '',
  pid: 0,
  pids: '',
  url: '',
  type: 1,
  icon: '',
  order_num: 0,
})

watch(
  () => store.currentMenu,
  (newVal) => {
    if (newVal) {
      Object.assign(formData, newVal)
      // 弹回同一节点时校验红字要清掉，否则上一次的错误提示会一直挂着
      void nextTick(() => formRef.value?.clearValidate())
    }
  },
  { immediate: true, deep: true },
)

const formRules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  permission: [{ required: true, message: '请输入权限标识', trigger: 'blur' }],
}

/** 父节点是目录 -> 子项是菜单；父节点是菜单 -> 子项是按钮 */
const handleAddChild = (data: MenuVo) => {
  if (data.type === 1) {
    store.createNew(data.id, 2)
  } else if (data.type === 2) {
    store.createNew(data.id, 3)
  }
}

const handleAddRoot = () => {
  store.createNew(0, 1)
}

const handleMove = async (data: MenuVo, direction: 'up' | 'down') => {
  try {
    await store.move(data.id, direction)
  } catch {
    // 已经是第一个 / 最后一个时后端会拒绝，提示由拦截器弹出
  }
}

const handleDelete = async (data: MenuVo) => {
  try {
    await ElMessageBox.confirm(
      `确定删除「${data.name}」及其所有子菜单吗？删除后无法恢复。`,
      '警告',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return // 用户点了取消
  }
  try {
    await store.remove(data.id)
    ElMessage.success('删除成功')
  } catch {
    // 错误提示拦截器已弹过
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    // id=0 是新建占位，交给 store 抹成 null；type 已经由「添加子项」时确定
    await store.save({ ...formData })
    ElMessage.success(store.editMode === 'edit' ? '保存成功' : '新增成功')
  } catch {
    // 业务错误（同级重名、无权限…）拦截器已经弹过
  }
}

/* ---------------- 生命周期 ---------------- */

onMounted(async () => {
  try {
    await store.fetchTree()
  } catch {
    // fetchTree 内部已把树置空，这里不用再处理
  }
})
</script>

<template>
  <div class="menu-manage-container">
    <!-- 左侧树 -->
    <div class="left-tree">
      <div class="tree-header">
        <el-button v-if="canAdd" type="primary" size="small" @click="handleAddRoot">
          ➕ 新建目录
        </el-button>
      </div>

      <el-tree
        ref="treeRef"
        class="tree-with-line"
        :data="store.menuTree"
        v-loading="store.treeLoading"
        :props="defaultProps"
        node-key="id"
        default-expand-all
        highlight-current
        empty-text="暂无菜单，请点击上方「新建目录」"
        @node-click="handleNodeClick"
      >
        <template #default="{ data }">
          <span class="custom-tree-node">
            <span class="node-label">
              <span>{{ typeIcon(data.type) }}</span>
              <span>{{ data.name }}</span>
            </span>
            <span class="node-actions">
              <!-- 目录能加菜单、菜单能加按钮，按钮不能再加子项 -->
              <el-button
                v-if="data.type !== 3 && canAdd"
                size="small"
                link
                @click.stop="handleAddChild(data)"
              >
                ＋
              </el-button>
              <el-button v-if="canUpdate" size="small" link @click.stop="handleMove(data, 'up')">
                ↑
              </el-button>
              <el-button v-if="canUpdate" size="small" link @click.stop="handleMove(data, 'down')">
                ↓
              </el-button>
              <el-button
                v-if="canDelete"
                size="small"
                link
                type="danger"
                @click.stop="handleDelete(data)"
              >
                ✕
              </el-button>
            </span>
          </span>
        </template>
      </el-tree>
    </div>

    <!-- 右侧编辑面板 -->
    <div class="right-panel">
      <div v-if="store.currentMenu" class="form-container">
        <h3>{{ store.editMode === 'add' ? '新增菜单' : '编辑菜单' }}</h3>
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
          <el-form-item label="名称" prop="name">
            <el-input v-model="formData.name" placeholder="请输入名称" maxlength="50" />
          </el-form-item>
          <el-form-item label="权限标识" prop="permission">
            <el-input v-model="formData.permission" placeholder="如：system:menu:query" maxlength="100" />
          </el-form-item>
          <el-form-item label="类型">
            <!-- 类型由父节点决定（目录下只能建菜单、菜单下只能建按钮），这里只读展示 -->
            <el-tag :type="formData.type === 1 ? 'primary' : formData.type === 2 ? 'success' : 'info'">
              {{ formData.type === 1 ? '目录' : formData.type === 2 ? '菜单' : '按钮' }}
            </el-tag>
          </el-form-item>
          <el-form-item v-if="formData.type === 2" label="URL">
            <el-input v-model="formData.url" placeholder="如 /menu/list，按钮可留空" maxlength="200" />
          </el-form-item>
          <el-form-item v-if="formData.type === 1" label="图标">
            <el-input v-model="formData.icon" placeholder="如 images/leftico01.png，可留空" maxlength="200" />
          </el-form-item>
          <el-form-item label="排序号">
            <el-input-number v-model="formData.order_num" :min="0" :max="999" />
            <div class="field-tip">同级内从小到大排列，也可用树上的 ↑ ↓ 直接调整</div>
          </el-form-item>
          <el-form-item>
            <el-button
              v-if="store.editMode === 'add' ? canAdd : canUpdate"
              type="primary"
              @click="handleSubmit"
            >
              保存
            </el-button>
            <el-button @click="store.resetCurrent">取消</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div v-else class="empty-tip">
        <p>请从左侧选择菜单，或点击「➕ 新建目录」</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.menu-manage-container {
  display: flex;
  min-height: 100%;
  background: #f0f9fd;
  border-radius: 4px;
}

.left-tree {
  width: 380px;
  flex-shrink: 0;
  background: #f0f9fd;
  border-right: 1px solid #ddd;
  padding: 16px 12px;
  overflow-y: auto;
  max-height: calc(100vh - 120px);
}

.tree-header {
  margin-bottom: 16px;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  padding-right: 8px;
}

.node-label {
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.node-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.right-panel {
  flex: 1;
  background: #f8fafc;
  padding: 32px 40px;
  overflow-y: auto;
  max-height: calc(100vh - 120px);
}

.form-container {
  background: #fff;
  border-radius: 8px;
  padding: 32px 36px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
  max-width: 720px;
  margin: 0 auto;
}

.form-container h3 {
  font-size: 20px;
  font-weight: 600;
  color: #1e2a3a;
  margin: 0 0 28px;
  border-left: 4px solid #18a689;
  padding-left: 16px;
}

.field-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.empty-tip {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 240px;
  color: #999;
  font-size: 16px;
}
</style>
