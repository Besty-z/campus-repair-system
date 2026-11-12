// 我的（tabBar 页）：用户信息展示 + 按角色显示功能入口
const { get } = require('../../utils/request');
const util = require('../../utils/util');
const role = require('../../utils/role');

Page({
  data: {
    userInfo: null,      // 当前用户信息
    avatarText: '用',    // 头像文字（昵称首字）
    roleText: '',        // 角色中文
    roleColor: '',       // 角色颜色
    isVip: false,
    isAdmin: false,
    isWorker: false
  },

  onShow() {
    const app = getApp();
    const userInfo = app.globalData.userInfo;
    if (!userInfo) {
      this.setData({
        userInfo: null,
        avatarText: '用',
        roleText: '',
        isVip: false,
        isAdmin: false,
        isWorker: false
      });
      return;
    }
    this.renderUser(userInfo);
    // 刷新一次最新用户信息（角色可能被管理员修改过）
    get('/api/auth/profile')
      .then((fresh) => {
        app.globalData.userInfo = fresh;
        wx.setStorageSync('userInfo', fresh);
        this.renderUser(fresh);
      })
      .catch(() => {});
  },

  /**
   * 渲染用户信息相关展示字段
   * @param {object} userInfo 用户信息
   */
  renderUser(userInfo) {
    const nickname = userInfo.nickname || '校园用户';
    this.setData({
      userInfo,
      avatarText: nickname.slice(0, 1) || '用',
      roleText: util.roleText(userInfo.role),
      roleColor: util.roleColor(userInfo.role),
      isVip: userInfo.role === 'VIP',
      isAdmin: userInfo.role === 'ADMIN',
      isWorker: userInfo.role === 'WORKER'
    });
  },

  /** 课程演示：点击角色徽章切换身份 */
  onSwitchRole() {
    role.switchRole();
  },

  /** 去登录 */
  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  /** 跳转提交报修 */
  goSubmit() {
    wx.switchTab({ url: '/pages/repair/submit' });
  },

  /** 跳转我的工单 */
  goOrders() {
    wx.switchTab({ url: '/pages/order/list' });
  },

  /** 跳报修统计（VIP） */
  goStats() {
    wx.navigateTo({ url: '/pages/stats/stats' });
  },

  /** 跳管理页面（ADMIN） */
  goAdminOrder() {
    wx.navigateTo({ url: '/pages/admin/order' });
  },

  goAdminCategory() {
    wx.navigateTo({ url: '/pages/admin/category' });
  },

  goAdminUser() {
    wx.navigateTo({ url: '/pages/admin/user' });
  },

  goAdminWorker() {
    wx.navigateTo({ url: '/pages/admin/worker' });
  },

  /** 维修工：我的任务 */
  goWorkerTasks() {
    wx.navigateTo({ url: '/pages/worker/tasks' });
  },

  /** 维修工：抢单大厅 */
  goWorkerPool() {
    wx.navigateTo({ url: '/pages/worker/pool' });
  },

  /** 维修工：我的资料 */
  goWorkerProfile() {
    wx.navigateTo({ url: '/pages/worker/profile' });
  },

  /** 退出登录 */
  logout() {
    wx.showModal({
      title: '提示',
      content: '确定退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          getApp().clearLogin();
          this.setData({
            userInfo: null,
            avatarText: '用',
            roleText: '',
            isVip: false,
            isAdmin: false,
            isWorker: false
          });
          wx.showToast({ title: '已退出', icon: 'success' });
        }
      }
    });
  }
});
