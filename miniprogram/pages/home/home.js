// 首页：广告位、服务保障、服务分类、快捷入口、最近工单与会员引导
const { get } = require('../../utils/request');
const util = require('../../utils/util');
const role = require('../../utils/role');
const images = require('../../utils/images');

Page({
  data: {
    userInfo: null,
    emptyImage: images.EMPTY_IMAGE, // 最近工单空态插图
    roleText: '',
    isVip: false,
    isAdmin: false,
    isWorker: false,
    greeting: '你好',
    ads: [],                   // 广告位（接口 /api/ads，失败时隐藏）
    services: [],              // 服务分类（含价格展示字段）
    // 服务保障条（静态四项）
    guarantees: [
      { value: '300+', label: '校园点位' },
      { value: '认证', label: '专业师傅' },
      { value: '98%', label: '好评率' },
      { value: '1 小时', label: '快速响应' }
    ],
    // 3 条 banner 各对应一张样板背景图
    banners: [
      { tag: '校园报修', title: '报修一键提交', desc: '宿舍楼与校园公共区域全覆盖', img: images.BANNER_IMAGES[0] },
      { tag: '会员优先', title: '预约上门时间', desc: '紧急工单优先安排，处理更及时', img: images.BANNER_IMAGES[1] },
      { tag: '进度透明', title: '全程状态可查', desc: '待接单、维修中、已完成一目了然', img: images.BANNER_IMAGES[2] }
    ],
    recentOrders: [],
    loading: false
  },

  onShow() {
    this.setGreeting();
    const app = getApp();
    const userInfo = app.globalData.userInfo || null;
    const isWorker = userInfo ? userInfo.role === 'WORKER' : false;
    this.setData({
      userInfo,
      roleText: userInfo ? util.roleText(userInfo.role) : '',
      isVip: userInfo ? userInfo.role === 'VIP' : false,
      isAdmin: userInfo ? userInfo.role === 'ADMIN' : false,
      isWorker
    });
    this.loadAds();
    this.loadServices();
    if (userInfo) {
      this.loadRecentOrders(isWorker);
    } else {
      this.setData({ recentOrders: [] });
    }
  },

  /**
   * 加载广告位：接口需登录，未登录或失败时静默隐藏
   */
  loadAds() {
    const app = getApp();
    if (!app.globalData.token) {
      this.setData({ ads: [] });
      return;
    }
    get('/api/ads')
      .then((list) => {
        const ads = (list || []).map((item) => Object.assign({}, item, {
          imageUrl: util.imageUrl(item.imageUrl)
        }));
        this.setData({ ads });
      })
      .catch(() => {
        this.setData({ ads: [] });
      });
  },

  /**
   * 加载服务分类：附带单价、会员价（8 折）与角标展示字段，失败时隐藏服务区
   */
  loadServices() {
    get('/api/categories')
      .then((list) => {
        const services = (list || []).map((item) => {
          const price = Number(item.price);
          const hasPrice = isFinite(price) && price > 0;
          return {
            id: item.id,
            name: item.name,
            imageUrl: util.imageUrl(item.imageUrl),
            initial: (item.name || '修').slice(0, 1),
            hasPrice,
            priceText: hasPrice ? util.formatMoney(price) : '',
            memberPriceText: hasPrice ? util.formatMoney(price * 0.8) : ''
          };
        });
        this.setData({ services });
      })
      .catch(() => {
        this.setData({ services: [] });
      });
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

  /**
   * 加载最近 3 条工单
   * @param {boolean} isWorker 是否维修工：维修工读取任务分页接口
   */
  loadRecentOrders(isWorker) {
    this.setData({ loading: true });
    const url = isWorker ? '/api/orders/worker/page' : '/api/orders/my';
    get(url, { pageNum: 1, pageSize: 3 })
      .then((page) => {
        const records = (page.records || []).map((order) => {
          const locationText = order.locationType === 'DORM'
            ? ((order.buildingNo || '') + ' ' + (order.roomNo || '')).trim()
            : (order.publicLocation || '');
          const catTitle = order.categoryNames && order.categoryNames.length > 0
            ? order.categoryNames.join(' / ')
            : (order.categoryName || '其他故障');
          return Object.assign({}, order, {
            statusText: util.statusText(order.status),
            statusColor: util.statusColor(order.status),
            locationText,
            catTitle
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

  /** 课程演示：点击顶部角色徽章切换身份 */
  onSwitchRole() {
    role.switchRole();
  },

  goSubmit() {
    if (this.requireLogin()) {
      wx.switchTab({ url: '/pages/repair/submit' });
    }
  },

  /**
   * 点击服务分类：带上分类 id 跳转报修页（tabBar 页无法带参，暂存缓存由报修页读取）
   * @param {object} e 事件对象，dataset.id 为分类 id
   */
  goService(e) {
    if (!this.requireLogin()) {
      return;
    }
    const id = e.currentTarget.dataset.id;
    if (id !== null && id !== undefined && id !== '') {
      wx.setStorageSync('pendingCategoryId', String(id));
    }
    wx.switchTab({ url: '/pages/repair/submit' });
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

  /** 维修工：我的任务 */
  goWorkerTasks() {
    if (this.requireLogin()) {
      wx.navigateTo({ url: '/pages/worker/tasks' });
    }
  },

  /** 维修工：抢单大厅 */
  goWorkerPool() {
    if (this.requireLogin()) {
      wx.navigateTo({ url: '/pages/worker/pool' });
    }
  },

  /** 维修工：我的资料 */
  goWorkerProfile() {
    if (this.requireLogin()) {
      wx.navigateTo({ url: '/pages/worker/profile' });
    }
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/order/detail?id=' + id });
  },

  onPullDownRefresh() {
    if (getApp().globalData.userInfo) {
      this.loadRecentOrders(this.data.isWorker);
    }
    wx.stopPullDownRefresh();
  }
});
