// 首页：用户问候、快捷入口、最近工单与会员引导
const { get } = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    userInfo: null,
    roleText: '',
    isVip: false,
    isAdmin: false,
    greeting: '你好',
    banners: [
      { tag: '校园报修', title: '报修一键提交', desc: '宿舍楼与校园公共区域全覆盖' },
      { tag: '会员优先', title: '预约上门时间', desc: '紧急工单优先安排，处理更及时' },
      { tag: '进度透明', title: '全程状态可查', desc: '待接单、维修中、已完成一目了然' }
    ],
    recentOrders: [],
    loading: false
  },

  onShow() {
    this.setGreeting();
    const app = getApp();
    const userInfo = app.globalData.userInfo || null;
    this.setData({
      userInfo,
      roleText: userInfo ? util.roleText(userInfo.role) : '',
      isVip: userInfo ? userInfo.role === 'VIP' : false,
      isAdmin: userInfo ? userInfo.role === 'ADMIN' : false
    });
    if (userInfo) {
      this.loadRecentOrders();
    } else {
      this.setData({ recentOrders: [] });
    }
  },

  /** 根据当前时间生成问候语 */
  setGreeting() {
    const hour = new Date().getHours();
    let greeting = '晚上好';
    if (hour < 6) {
      greeting = '夜深了';
    } else if (hour < 9) {
      greeting = '早上好';
    } else if (hour < 12) {
      greeting = '上午好';
    } else if (hour < 14) {
      greeting = '中午好';
    } else if (hour < 18) {
      greeting = '下午好';
    }
    this.setData({ greeting });
  },

  /** 加载最近 3 条工单 */
  loadRecentOrders() {
    this.setData({ loading: true });
    get('/api/orders/my', { pageNum: 1, pageSize: 3 })
      .then((page) => {
        const records = (page.records || []).map((order) => {
          const locationText = order.locationType === 'DORM'
            ? ((order.buildingNo || '') + ' ' + (order.roomNo || '')).trim()
            : (order.publicLocation || '');
          return Object.assign({}, order, {
            statusText: util.statusText(order.status),
            statusColor: util.statusColor(order.status),
            locationText
          });
        });
        this.setData({ recentOrders: records, loading: false });
      })
      .catch(() => {
        this.setData({ loading: false });
      });
  },

  /** 未登录时跳转登录页，已登录返回 true */
  requireLogin() {
    const app = getApp();
    if (!app.globalData.token) {
      wx.navigateTo({ url: '/pages/login/login' });
      return false;
    }
    return true;
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' });
  },

  goSubmit() {
    if (this.requireLogin()) {
      wx.switchTab({ url: '/pages/repair/submit' });
    }
  },

  goOrders() {
    if (this.requireLogin()) {
      wx.switchTab({ url: '/pages/order/list' });
    }
  },

  goStats() {
    wx.navigateTo({ url: '/pages/stats/stats' });
  },

  goAdmin() {
    wx.navigateTo({ url: '/pages/admin/order' });
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/order/detail?id=' + id });
  },

  onPullDownRefresh() {
    if (getApp().globalData.userInfo) {
      this.loadRecentOrders();
    }
    wx.stopPullDownRefresh();
  }
});
