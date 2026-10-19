<template>
    <el-dialog :model-value="modelValue" title="修改密码" width="420px" :close-on-click-modal="false" append-to-body
        @update:model-value="handleVisibleChange" @closed="handleClosed">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" @submit.prevent>
            <el-form-item label="原密码" prop="oldPassword">
                <el-input v-model="form.oldPassword" type="password" placeholder="请输入原密码" show-password clearable />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="form.newPassword" type="password" placeholder="6-20 个字符" show-password clearable />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" show-password
                    @keyup.enter="handleSubmit" />
            </el-form-item>
        </el-form>

        <template #footer>
            <el-button @click="handleVisibleChange(false)">取消</el-button>
            <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useLoginStore } from '@/stores/loginStores'

defineProps<{ modelValue: boolean }>()

const emit = defineEmits<{
    (e: 'update:modelValue', value: boolean): void
    (e: 'success'): void
}>()

const loginStore = useLoginStore()
const formRef = ref<FormInstance | null>(null)
const loading = ref(false)

const form = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
})

// 单字段规则交给 el-form；跨字段的两条（新旧不能相同、两次输入要一致）
// 放到 handleSubmit 里命令式判断，比自定义 validator 好维护，也绕开了
// async-validator 的 validator 参数类型在 strict 模式下对不齐的问题。
const rules = reactive<FormRules>({
    oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
    ],
    confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }],
})

const handleVisibleChange = (value: boolean) => {
    emit('update:modelValue', value)
}

// 关闭后清空表单，避免下次打开还留着上一次输入的密码
const handleClosed = () => {
    formRef.value?.resetFields()
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
}

const handleSubmit = async () => {
    if (!formRef.value || loading.value) return

    try {
        await formRef.value.validate()
    } catch {
        return // 校验失败，el-form 已经把错误标在对应字段上了
    }

    if (form.newPassword === form.oldPassword) {
        ElMessage.warning('新密码不能与原密码相同')
        return
    }
    if (form.newPassword !== form.confirmPassword) {
        ElMessage.warning('两次输入的新密码不一致')
        return
    }

    loading.value = true
    try {
        await loginStore.changePassword({
            oldPassword: form.oldPassword,
            newPassword: form.newPassword,
        })
        ElMessage.success('密码修改成功，请使用新密码重新登录')
        emit('update:modelValue', false)
        emit('success')
    } catch (error) {
        // request.ts 的响应拦截器已经弹过提示的（handled 标记），这里不再重复弹
        const handled =
            typeof error === 'object' && error !== null && (error as { handled?: boolean }).handled === true
        if (!handled) {
            const message = error instanceof Error ? error.message : '修改失败，请稍后重试'
            ElMessage.error(message)
        }
    } finally {
        loading.value = false
    }
}
</script>
