<template>
    <div class="login-container">
        <!-- 左侧：背景 + 标题 -->
        <div class="left-panel">
            <div class="title">请假管理系统</div>
        </div>

        <!-- 右侧：登录表单 -->
        <div class="right-panel">
            <div class="login-box">
                <h2>用户登录</h2>
                <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-width="0" size="large">
                    <el-form-item prop="username">
                        <el-input v-model="loginForm.username" placeholder="请输入用户名（学号/工号）" prefix-icon="User"
                            clearable />
                    </el-form-item>

                    <el-form-item prop="password">
                        <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock"
                            show-password @keyup.enter="handleLogin" />
                    </el-form-item>

                    <el-form-item>
                        <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">
                            登录
                        </el-button>
                    </el-form-item>
                </el-form>

                <!-- 提示信息 -->
                <div class="extra-info">
                    <span>默认账号：教师工号或学生学号</span>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { useLoginStore } from '@/stores/loginStores';

const router = useRouter();
const route = useRoute();
const loginStore = useLoginStore();

const loginForm = reactive({ username: '', password: '' });
const rules = reactive<FormRules>({
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
    ],
});

const loginFormRef = ref<FormInstance | null>(null);
const loading = ref(false);

const isApiHandledError = (error: unknown): boolean =>
    !!error && typeof error === 'object' && (error as { handled?: boolean }).handled === true;

const handleLogin = async () => {
    if (!loginFormRef.value) {
        ElMessage.warning('表单未就绪');
        return;
    }

    loading.value = true;
    try {
        await loginFormRef.value.validate();
        await loginStore.login({
            username: loginForm.username.trim(),
            password: loginForm.password,
        });
        ElMessage.success('登录成功');
        const redirect = (route.query.redirect as string) || '/';
        router.push(redirect);
    } catch (error: unknown) {
        // Element Plus 表单校验失败时 reject 的是一个对象而非 Error，这里静默返回
        if (error && typeof error === 'object' && !(error instanceof Error)) {
            return;
        }
        if (!isApiHandledError(error)) {
            const message = error instanceof Error ? error.message : String(error);
            ElMessage.error(message || '登录失败，请重试');
        }
    } finally {
        loading.value = false;
    }
};
</script>

<style scoped>
.login-container {
    display: flex;
    height: 100vh;
    width: 100%;
    overflow: hidden;
    background: #f0f2f5;
    /* 整体背景色（右侧会覆盖） */
}

/* 左侧面板：占 2/3 */
.left-panel {
    flex: 2;
    background: url('https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=1200&h=800&fit=crop&crop=center') center / cover no-repeat;
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
}

/* 左侧标题文字，添加半透明遮罩提高可读性 */
.left-panel::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.4);
}

.left-panel .title {
    position: relative;
    z-index: 1;
    color: #fff;
    font-size: 48px;
    font-weight: 700;
    letter-spacing: 4px;
    text-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
    padding: 20px;
    text-align: center;
}

/* 右侧面板：占 1/3 */
.right-panel {
    flex: 1;
    background: #ebecec;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px;
}

.login-box {
    width: 100%;
    max-width: 360px;
}

.login-box h2 {
    text-align: center;
    margin-bottom: 30px;
    color: #333;
    font-weight: 500;
}

.extra-info {
    margin-top: 16px;
    text-align: center;
    color: #999;
    font-size: 13px;
}

/* 响应式处理：小屏幕下左侧隐藏 */
@media (max-width: 768px) {
    .left-panel {
        display: none;
    }

    .right-panel {
        flex: 1;
    }
}

/* 让登录框内的输入框有浅灰色背景，与白色面板区分 */
.login-box :deep(.el-input__wrapper) {
    background-color: #f5f7fa;
    box-shadow: 0 0 0 1px #dcdfe6 inset;
    transition: box-shadow 0.2s;
}

/* 输入框聚焦时高亮 */
.login-box :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px #409eff inset;
}
</style>
