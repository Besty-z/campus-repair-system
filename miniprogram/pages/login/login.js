// 登录/注册一体页：手机号 + 验证码
// 预置管理员账号：手机号 13800000000（数据库已初始化 role=ADMIN），验证码登录即可
// 课程演示：后端不发真实短信，验证码打印在后端控制台日志（关键字「模拟发送短信验证码」）
const { post } = require('../../utils/request');
const images = require('../../utils/images');

Page({
  data: {
    loginBg: images.LOGIN_BG, // 品牌区背景图
    phone: '',        // 手机号
    code: '',         // 验证码
    counting: 0,      // 倒计时剩余秒数，0 表示可重新发送
    sending: false,   // 是否正在发送验证码
    logging: false    // 是否正在登录
  },

  onUnload() {
    // 页面销毁时清理定时器
    if (this.timer) {
      clearInterval(this.timer);
    }
  },

  /** 手机号输入 */
  onPhoneInput(e) {
    this.setData({ phone: e.detail.value.trim() });
  },

  /** 验证码输入 */
  onCodeInput(e) {
    this.setData({ code: e.detail.value.trim() });
  },

  /** 发送验证码并启动 60 秒倒计时 */
  sendCode() {
    const { phone, counting, sending } = this.data;
    if (counting > 0 || sending) {
      return;
    }
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }
    this.setData({ sending: true });
    post('/api/auth/send-code', { phone })
      .then(() => {
        wx.showToast({ title: '验证码已发送', icon: 'success' });
        this.startCountdown();
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ sending: false });
      });
  },

  /** 启动 60 秒倒计时 */
  startCountdown() {
    this.setData({ counting: 60 });
    this.timer = setInterval(() => {
      const left = this.data.counting - 1;
      if (left <= 0) {
        clearInterval(this.timer);
        this.timer = null;
      }
      this.setData({ counting: Math.max(left, 0) });
    }, 1000);
  },

  /** 登录/注册：后端在手机号未注册时会自动注册 */
  login() {
    const { phone, code, logging } = this.data;
    if (logging) {
      return;
    }
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }
    if (!code) {
      wx.showToast({ title: '请输入验证码', icon: 'none' });
      return;
    }
    this.setData({ logging: true });
    post('/api/auth/login', { phone, code })
      .then((data) => {
        // data: { token, user }
        const app = getApp();
        app.setLogin(data.token, data.user);
        wx.showToast({ title: '登录成功', icon: 'success' });
        setTimeout(() => {
          // 有上一页则返回，否则切到首页
          const pages = getCurrentPages();
          if (pages.length > 1) {
            wx.navigateBack();
          } else {
            wx.switchTab({ url: '/pages/home/home' });
          }
        }, 600);
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ logging: false });
      });
  }
});
