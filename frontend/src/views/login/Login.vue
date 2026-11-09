<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="title">校园报修预约系统</h2>
      <p class="subtitle">手机号验证码登录 / 注册</p>

      <!-- 课程演示提示：验证码不发真实短信，查看后端控制台日志 -->
      <el-alert
        class="demo-tip"
        type="warning"
        :closable="false"
        title="课程演示：验证码请查看后端控制台日志"
      />

      <el-tabs v-model="activeTab" stretch>
        <!-- ================= 登录 ================= -->
        <el-tab-pane label="验证码登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0">
            <el-form-item prop="phone">
              <el-input v-model="loginForm.phone" placeholder="请输入手机号" maxlength="11" clearable>
                <template #prefix>+86</template>
              </el-input>
            </el-form-item>
            <el-form-item prop="code">
              <div class="code-row">
                <el-input v-model="loginForm.code" placeholder="请输入验证码" maxlength="6" />
                <el-button
                  type="primary"
                  plain
                  :disabled="countdown > 0"
                  :loading="sending"
                  @click="handleSendCode"
                >
                  {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-button class="submit-btn" type="primary" :loading="loading" @click="handleLogin">
              登 录
            </el-button>
            <p class="admin-tip">预置管理员手机号：13800000000</p>
          </el-form>
        </el-tab-pane>

        <!-- ================= 注册 ================= -->
        <el-tab-pane label="注册新账号" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="0">
            <el-form-item prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="请输入昵称" maxlength="20" clearable />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="registerForm.phone" placeholder="请输入手机号" maxlength="11" clearable />
            </el-form-item>
            <el-form-item prop="code">
              <div class="code-row">
                <el-input v-model="registerForm.code" placeholder="请输入验证码" maxlength="6" />
                <el-button
                  type="primary"
                  plain
                  :disabled="countdown > 0"
                  :loading="sending"
                  @click="handleSendCode"
                >
                  {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-button class="submit-btn" type="success" :loading="loading" @click="handleRegister">
              注册并登录
            </el-button>
            <p class="admin-tip">注册即为普通用户，会员由管理员开通</p>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendCode, login, register } from '../../api/auth'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()

// 当前激活的页签：login 登录 / register 注册
const activeTab = ref('login')

// ---------- 表单数据 ----------
const loginFormRef = ref()
const registerFormRef = ref()
const loginForm = reactive({ phone: '', code: '' })
const registerForm = reactive({ nickname: '', phone: '', code: '' })

// ---------- 状态 ----------
const loading = ref(false) // 登录/注册按钮 loading
const sending = ref(false) // 发送验证码按钮 loading
const countdown = ref(0) // 验证码重发倒计时（秒）
let timer = null

// 手机号校验规则（11 位数字，1 开头）
const phoneRule = {
  pattern: /^1\d{10}$/,
  message: '请输入正确的 11 位手机号',
  trigger: 'blur'
}

const loginRules = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, phoneRule],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const registerRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, phoneRule],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

/**
 * 发送验证码：根据当前页签取对应表单的手机号
 * 课程演示不发真实短信，验证码打印在后端控制台日志
 */
async function handleSendCode() {
  const phone = activeTab.value === 'login' ? loginForm.phone : registerForm.phone
  if (!/^1\d{10}$/.test(phone)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  sending.value = true
  try {
    await sendCode(phone)
    ElMessage.success('验证码已发送，请查看后端控制台日志')
    startCountdown()
  } finally {
    sending.value = false
  }
}

// 启动 60 秒重发倒计时
function startCountdown() {
  countdown.value = 60
  clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
}

// 登录成功后统一处理：保存登录态并跳转首页
function afterLogin(res) {
  userStore.setLogin(res.data.token, res.data.user)
  ElMessage.success('登录成功')
  router.push('/')
}

// 提交登录表单
async function handleLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    const res = await login({ phone: loginForm.phone, code: loginForm.code })
    afterLogin(res)
  } finally {
    loading.value = false
  }
}

// 提交注册表单（注册成功后端直接返回登录态）
async function handleRegister() {
  await registerFormRef.value.validate()
  loading.value = true
  try {
    const res = await register({
      phone: registerForm.phone,
      code: registerForm.code,
      nickname: registerForm.nickname
    })
    afterLogin(res)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.title {
  margin: 0;
  text-align: center;
  color: #303133;
}

.subtitle {
  margin: 8px 0 16px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.demo-tip {
  margin-bottom: 16px;
}

.code-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}

.submit-btn {
  width: 100%;
}

.admin-tip {
  margin: 12px 0 0;
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
}
</style>
