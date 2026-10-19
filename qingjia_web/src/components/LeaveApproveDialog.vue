<template>
  <el-dialog :model-value="modelValue" title="审批" width="520px" append-to-body :close-on-click-modal="false"
    @update:model-value="handleVisibleChange">
    <div v-loading="loading">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="申请人">
          {{ application?.studentName || '-' }}
          <span v-if="application?.className" class="muted">（{{ application.className }}）</span>
        </el-descriptions-item>
        <el-descriptions-item label="请假类型">{{ application?.typeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请假时间">
          {{ application?.begintime || '-' }} ~ {{ application?.endtime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="请假途径">{{ application?.path || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请假原因">{{ application?.reason || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="approve-form">
        <el-form-item label="审批结果" prop="result">
          <el-radio-group v-model="form.result" @change="rebuildValidate">
            <el-radio :value="1">同意</el-radio>
            <el-radio :value="2">不同意</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" prop="opinion">
          <el-input v-model="form.opinion" type="textarea" :rows="3" maxlength="200" show-word-limit
            placeholder="同意可留空；不同意请写明原因" />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="handleVisibleChange(false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
/**
 * 审批弹窗（班主任 / 辅导员 / 院领导三个页面共用）。
 *
 * 哑组件：表单校验和收集在这里，真正调哪个审批接口由父组件在 @submit 里决定
 * （三个页面的 URL 不同，但提交体都是 {result, opinion}）。
 */
import { nextTick, reactive, ref, watch } from 'vue'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'
import type { LeaveApplicationVo } from '@/type/leavePojo'

const props = defineProps<{
  modelValue: boolean
  application: LeaveApplicationVo | null
  /** 提交中，用来禁用按钮 */
  submitting?: boolean
  /** 拉取详情中 */
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: { result: number; opinion: string }): void
}>()

const formRef = ref<FormInstance>()
const form = reactive<{ result: number | null; opinion: string }>({ result: null, opinion: '' })

/**
 * 不同意时必须写意见：只写一句「不同意」的话，学生看到的只有「审批未通过」，
 * 不知道要改什么。同意则允许留空 —— 后端 opinion 是 @NotNull，所以这里补一个
 * 「同意」兜底，免得提交空串被 Bean Validation 拦下来。
 */
const opinionRule: FormItemRule = {
  validator: (_rule, value, callback) => {
    if (form.result === 2 && String(value ?? '').trim() === '') {
      return callback(new Error('不同意时请填写审批意见'))
    }
    return callback()
  },
  trigger: 'blur',
}

const rules: FormRules = {
  result: [{ required: true, message: '请选择审批结果', trigger: 'change' }],
  opinion: [opinionRule],
}

/** 重选结果后清掉上一次的校验红字（同意 -> 不同意 会多出一条必填规则） */
const rebuildValidate = () => {
  void nextTick(() => formRef.value?.clearValidate('opinion'))
}

const resetForm = () => {
  form.result = null
  form.opinion = ''
  void nextTick(() => formRef.value?.clearValidate())
}

const handleVisibleChange = (value: boolean) => {
  if (!value) resetForm()
  emit('update:modelValue', value)
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || form.result == null) return

  const opinion = form.opinion.trim() || (form.result === 1 ? '同意' : '')
  emit('submit', { result: form.result, opinion })
}

// 弹窗每次打开都重置，避免残留上一次的审批结果
watch(
  () => props.modelValue,
  (visible) => {
    if (visible) resetForm()
  },
)
</script>

<style scoped>
.approve-form {
  margin-top: 16px;
}

.muted {
  color: #909399;
}
</style>
