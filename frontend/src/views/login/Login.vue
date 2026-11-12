<template>
  <div class="login-page">
    <!-- 左侧品牌区：渐变背景 + 装饰圆 + 产品卖点 -->
    <section class="brand-side">
      <div class="brand-deco brand-deco-a" />
      <div class="brand-deco brand-deco-b" />
      <div class="brand-deco brand-deco-c" />

      <div class="brand-top">
        <div class="brand-logo">修</div>
        <div class="brand-name">校园报修预约系统</div>
      </div>

      <div class="brand-copy">
        <h1>让每一次报修<br />都被看见</h1>
        <p>宿舍楼内 · 校园公共区域 · 手机号一键登录<br />小程序与网页共用一套账号与权限</p>
      </div>

      <ul class="brand-features">
        <li>
          <span class="feature-dot" />
          <div>
            <strong>拍照报修</strong>
            <em>最多 5 张故障照片，定位更准确</em>
          </div>
        </li>
        <li>
          <span class="feature-dot" />
          <div>
            <strong>进度可查</strong>
            <em>待接单 / 维修中 / 已完成 / 驳回</em>
          </div>
        </li>
        <li>
          <span class="feature-dot" />
          <div>
            <strong>会员优先</strong>
            <em>预约上门时间，紧急工单优先处理</em>
          </div>
        </li>
      </ul>
    </section>

    <!-- 右侧表单区 -->
    <section class="form-side">
      <div class="form-card">
        <div class="form-head">
          <h2>{{ activeTab === 'login' ? '欢迎回来' : '创建账号' }}</h2>
          <p>{{ activeTab === 'login' ? '使用手机号验证码登录' : `手机号注册，注册后即为${ROLE_MAP.USER.label}` }}</p>
        </div>

        <!-- 课程演示提示：验证码不发真实短信，查看后端控制台日志 -->
        <div class="demo-tip">
          <span class="demo-dot" />
          课程演示环境：验证码请查看后端控制台日志
        </div>

        <el-tabs v-model="activeTab" stretch>
          <!-- ================= 登录 ================= -->
          <el-tab-pane label="验证码登录" name="login">
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0">
              <el-form-item prop="phone">
                <el-input
                  v-model="loginForm.phone"
                  placeholder="请输入手机号"
                  maxlength="11"
                  size="large"
                  clearable
                >
                  <template #prefix><span class="prefix-text">+86</span></template>
                </el-input>
              </el-form-item>
              <el-form-item prop="code">
                <div class="code-row">
                  <el-input
                    v-model="loginForm.code"
                    placeholder="请输入验证码"
                    maxlength="6"
                    size="large"
                  />
                  <el-button
                    class="code-btn"
                    :disabled="countdown > 0"
                    :loading="sending"
                    @click="handleSendCode"
                  >
                    {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
              <el-button class="submit-btn" type="primary" size="large" :loading="loading" @click="handleLogin">
                登 录
              </el-button>
            </el-form>
          </el-tab-pane>

          <!-- ================= 注册 ================= -->
          <el-tab-pane label="注册新账号" name="register">
            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="0">
              <el-form-item prop="nickname">
                <el-input
                  v-model="registerForm.nickname"
                  placeholder="请输入昵称（如：张三）"
                  maxlength="20"
                  size="large"
                  clearable
                />
              </el-form-item>
              <el-form-item prop="phone">
                <el-input
                  v-model="registerForm.phone"
                  placeholder="请输入手机号"
                  maxlength="11"
                  size="large"
                  clearable
                />
              </el-form-item>
              <el-form-item prop="code">
                <div class="code-row">
                  <el-input
                    v-model="registerForm.code"
                    placeholder="请输入验证码"
                    maxlength="6"
                    size="large"
                  />
                  <el-button
                    class="code-btn"
                    :disabled="countdown > 0"
                    :loading="sending"
                    @click="handleSendCode"
                  >
                    {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
              <el-button class="submit-btn" type="primary" size="large" :loading="loading" @click="handleRegister">
                注册并登录
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>

        <div class="form-foot">
          <span>预置管理员手机号</span>
          <b>13800000000</b>
        </div>
      </div>

      <p class="copyright">校园报修预约系统 · 课程设计作品</p>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendCode, login, register } from '../../api/auth'
import { useUserStore } from '../../store/user'
import { ROLE_MAP } from '../../utils/dict'

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
/* 整页：左右分屏 */
.login-page {
  min-height: 100vh;
  display: flex;
  background: #f4f7fc;
}

/* ===== 左侧品牌区 ===== */
.brand-side {
  position: relative;
  width: 52%;
  max-width: 760px;
  padding: 54px 64px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(150deg, #173a7a 0%, #2c6bf2 55%, #17bfd4 120%);
}

/* 背景装饰圆，制造柔和层次 */
.brand-deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.brand-deco-a {
  width: 420px;
  height: 420px;
  right: -140px;
  top: -160px;
}

.brand-deco-b {
  width: 220px;
  height: 220px;
  left: -90px;
  bottom: 70px;
  background: rgba(255, 255, 255, 0.07);
}

.brand-deco-c {
  width: 120px;
  height: 120px;
  right: 120px;
  bottom: -40px;
  background: rgba(255, 255, 255, 0.12);
}

.brand-top {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-logo {
  width: 46px;
  height: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  font-size: 22px;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(6px);
}

.brand-name {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.brand-copy {
  position: relative;
  margin: 40px 0;
}

.brand-copy h1 {
  margin: 0;
  font-size: 44px;
  line-height: 1.28;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.brand-copy p {
  margin: 20px 0 0;
  font-size: 14px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.82);
}

.brand-features {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.brand-features li {
  flex: 1 1 180px;
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
}

.feature-dot {
  width: 8px;
  height: 8px;
  margin-top: 7px;
  border-radius: 50%;
  background: #8ff0e4;
  flex-shrink: 0;
}

.brand-features strong {
  display: block;
  font-size: 14px;
  font-weight: 600;
}

.brand-features em {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  font-style: normal;
  color: rgba(255, 255, 255, 0.72);
  line-height: 1.6;
}

/* ===== 右侧表单区 ===== */
.form-side {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
}

.form-card {
  width: 100%;
  max-width: 420px;
  padding: 34px 34px 26px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(18, 41, 74, 0.1);
}

.form-head h2 {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  color: #12294a;
  letter-spacing: -0.3px;
}

.form-head p {
  margin: 8px 0 18px;
  font-size: 13px;
  color: #7a879b;
}

.demo-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #a06a12;
  background: #fff8ea;
  border-radius: 12px;
}

.demo-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f5a524;
  flex-shrink: 0;
}

.prefix-text {
  color: #7a879b;
  font-size: 13px;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}

.code-btn {
  width: 116px;
  height: 40px;
  border-radius: 10px;
  border: 1px solid #d6e2fd;
  color: #2c6bf2;
  background: #f2f6ff;
  font-weight: 600;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 12px;
}

.form-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #e9eff8;
  font-size: 12px;
  color: #a9b4c6;
}

.form-foot b {
  color: #2c6bf2;
  font-weight: 600;
}

.copyright {
  margin: 22px 0 0;
  font-size: 12px;
  color: #a9b4c6;
}

/* 移动端：隐藏品牌区，表单占满 */
@media (max-width: 900px) {
  .brand-side {
    display: none;
  }
}
</style>
