// 校园报修预约系统 - 小程序入口
// 注意：真机预览时请把 baseUrl 改为电脑局域网 IP（如 http://192.168.x.x:8080），
// 并在微信开发者工具「详情 -> 本地设置」勾选「不校验合法域名」
const BASE_URL = 'http://localhost:8080';

App({
  globalData: {
    baseUrl: BASE_URL,   // 后端基础地址（接口前缀为 baseUrl + '/api'）
    token: '',           // 登录令牌（JWT）
    userInfo: null       // 当前登录用户信息 { id, phone, nickname, role }
  },

  onLaunch() {
    // 启动时从本地缓存恢复登录态
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    if (token) {
      this.globalData.token = token;
    }
    if (userInfo) {
      this.globalData.userInfo = userInfo;
    }
  },

  /**
   * 保存登录态（内存 + 本地缓存）
   * @param {string} token JWT 令牌
   * @param {object} userInfo 用户信息
   */
  setLogin(token, userInfo) {
    this.globalData.token = token;
    this.globalData.userInfo = userInfo;
    wx.setStorageSync('token', token);
    wx.setStorageSync('userInfo', userInfo);
  },

  /**
   * 清除登录态
   */
  clearLogin() {
    this.globalData.token = '';
    this.globalData.userInfo = null;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
  },

  /**
   * 检查是否已登录，未登录则跳转登录页
   * @returns {boolean} 是否已登录
   */
  checkLogin() {
    if (this.globalData.token) {
      return true;
    }
    wx.navigateTo({ url: '/pages/login/login' });
    return false;
  }
});
