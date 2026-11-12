// 提交报修页（tabBar 首页）：地点类型 / 分类联动 / 描述 / 图片上传 / VIP 预约与紧急
const { get, post, uploadFile } = require('../../utils/request');
const util = require('../../utils/util');

// 「其他（自行填写）」分类哨兵：id 为 null 表示用户自定义故障类型
const OTHER_CATEGORY = { id: null, name: '其他（自行填写）' };

Page({
  data: {
    imgBaseUrl: '',            // 图片完整地址前缀（onShow 时注入，图片实际地址在 /api 下）
    locationTypes: ['宿舍楼内', '校园公共区域'],
    locationTypeIndex: 0,      // 0=DORM 1=PUBLIC
    buildingNo: '',            // 宿舍楼号（DORM）
    roomNo: '',                // 房间号（DORM）
    publicLocation: '',        // 公共区域具体位置（PUBLIC）
    categories: [],            // 故障分类列表（末尾含「其他（自行填写）」），每项含 selected/price
    otherSelected: false,      // 是否选择了「其他（自行填写）」，与普通分类互斥
    customCategory: '',        // 自定义故障类型（选择「其他」时填写）
    // 费用预估（由所选分类单价汇总，VIP 自动 8 折）
    priceItems: [],            // 已选分类价格明细 [{ id, name, priceText }]
    totalPriceText: '',        // 原价合计
    discountText: '',          // 会员优惠金额
    estimateText: '',          // 预估费用（VIP 为折后价）
    hasEstimate: false,        // 是否已选可计价分类
    description: '',
    images: [],                // 已上传成功的服务端相对路径
    isVip: false,              // 是否会员（控制图片数量、预约时间、紧急开关）
    urgent: false,             // 是否紧急（仅 VIP）
    // 预约上门时间：两列 picker（日期 + 时间段）
    appointmentRange: [[], []], // picker 的 range
    appointmentIndex: [0, 0],   // 当前选中下标
    appointmentText: '',        // 已选时间展示文本
    submitting: false
  },

  /**
   * 支持从首页服务卡片带分类 id 进入（tabBar 页通过缓存传递，也兼容 query 参数）
   * @param {object} options 页面参数，categoryId 为预选分类 id
   */
  onLoad(options) {
    if (options && options.categoryId) {
      this.pendingCategoryId = String(options.categoryId);
    }
  },

  onShow() {
    // tabBar 页面：每次显示时检查登录态
    const app = getApp();
    if (!app.checkLogin()) {
      return;
    }
    // 读取首页服务卡片暂存的分类 id（switchTab 无法携带 query 参数）
    const pendingId = wx.getStorageSync('pendingCategoryId');
    if (pendingId) {
      this.pendingCategoryId = String(pendingId);
      wx.removeStorageSync('pendingCategoryId');
    }
    const userInfo = app.globalData.userInfo || {};
    this.setData({ isVip: userInfo.role === 'VIP', imgBaseUrl: app.globalData.imageBaseUrl });
    this.initAppointmentRange();
    this.loadCategories();
  },

  /**
   * 加载故障分类（按维修工种划分，不再区分地点类型），末尾追加「其他（自行填写）」；
   * 分类数据到达后再执行预选与费用计算
   */
  loadCategories() {
    get('/api/categories').then((list) => {
      const categories = (list || []).map((c) => {
        const price = Number(c.price);
        const hasPrice = isFinite(price) && price > 0;
        return {
          id: c.id,
          name: c.name,
          price: hasPrice ? price : null,
          priceText: hasPrice ? util.formatMoney(price) : '',
          selected: false
        };
      }).concat([Object.assign({}, OTHER_CATEGORY, { price: null, priceText: '', selected: false })]);
      this.setData({
        categories,
        otherSelected: false,
        customCategory: ''
      }, () => {
        if (this.pendingCategoryId) {
          this.applyPendingCategory();
        } else {
          this.refreshEstimate();
        }
      });
    }).catch(() => {});
  },

  /** 应用首页传来的分类 id（等分类异步加载完成后预选） */
  applyPendingCategory() {
    if (!this.pendingCategoryId) {
      return;
    }
    const pendingId = String(this.pendingCategoryId);
    this.pendingCategoryId = null;
    const idx = this.data.categories.findIndex((c) => c.id !== null && String(c.id) === pendingId);
    if (idx < 0) {
      this.refreshEstimate();
      return;
    }
    const updates = {};
    this.data.categories.forEach((c, i) => {
      updates['categories[' + i + '].selected'] = i === idx;
    });
    updates.otherSelected = false;
    updates.customCategory = '';
    this.setData(updates, () => {
      this.refreshEstimate();
    });
  },

  /**
   * 计算费用预估：已选分类单价求和；VIP 自动 8 折；「其他」为面议
   */
  refreshEstimate() {
    const d = this.data;
    if (d.otherSelected) {
      this.setData({
        priceItems: [],
        totalPriceText: '',
        discountText: '',
        estimateText: '面议',
        hasEstimate: false
      });
      return;
    }
    const selected = d.categories.filter((c) => c.selected && c.id !== null && c.price > 0);
    const total = selected.reduce((sum, c) => sum + c.price, 0);
    // 前端展示与后端一致：8 折后保留两位
    const discount = d.isVip ? Math.round(total * 0.2 * 100) / 100 : 0;
    const estimate = Math.round((total - discount) * 100) / 100;
    this.setData({
      priceItems: selected.map((c) => ({ id: c.id, name: c.name, priceText: util.formatMoney(c.price) })),
      totalPriceText: util.formatMoney(total),
      discountText: util.formatMoney(discount),
      estimateText: util.formatMoney(estimate),
      hasEstimate: selected.length > 0
    });
  },

  /** 切换地点类型：清空对应地点字段即可（分类不再随地点类型变化） */
  onLocationTypeChange(e) {
    const idx = Number(e.currentTarget.dataset.value);
    this.setData({
      locationTypeIndex: idx,
      buildingNo: '',
      roomNo: '',
      publicLocation: ''
    });
  },

  onBuildingInput(e) {
    this.setData({ buildingNo: e.detail.value });
  },

  onRoomInput(e) {
    this.setData({ roomNo: e.detail.value });
  },

  onPublicLocationInput(e) {
    this.setData({ publicLocation: e.detail.value });
  },

  /**
   * 分类多选：普通分类可同时选中多个；
   * 「其他（自行填写）」与普通分类互斥，选中它清空普通分类，选中普通分类则取消它。
   */
  onCategoryTap(e) {
    const idx = Number(e.currentTarget.dataset.index);
    const target = this.data.categories[idx];
    if (!target) {
      return;
    }
    const otherIdx = this.data.categories.findIndex((c) => c.id === null);
    const updates = {};
    if (target.id === null) {
      // 点击「其他」：切换自身；选中时清空全部普通分类
      const selected = !target.selected;
      this.data.categories.forEach((c, i) => {
        updates['categories[' + i + '].selected'] = selected ? c.id === null : false;
      });
      updates.otherSelected = selected;
      updates.customCategory = selected ? this.data.customCategory : '';
      this.setData(updates, () => {
        this.refreshEstimate();
      });
      return;
    }
    // 点击普通分类：切换自身，同时取消「其他」
    updates['categories[' + idx + '].selected'] = !target.selected;
    if (otherIdx >= 0) {
      updates['categories[' + otherIdx + '].selected'] = false;
    }
    updates.otherSelected = false;
    updates.customCategory = '';
    this.setData(updates, () => {
      this.refreshEstimate();
    });
  },

  onCustomCategoryInput(e) {
    this.setData({ customCategory: e.detail.value });
  },

  onDescInput(e) {
    this.setData({ description: e.detail.value });
  },

  onUrgentChange(e) {
    this.setData({ urgent: e.detail.value });
  },

  /**
   * 生成预约时间两列数据：未来 7 天日期 + 固定时间段
   */
  initAppointmentRange() {
    const days = [];
    const now = new Date();
    for (let i = 0; i < 7; i++) {
      const d = new Date(now.getTime() + i * 24 * 3600 * 1000);
      const label = d.getFullYear() + '-' + util.formatTime(d).slice(5, 10); // yyyy-MM-dd
      days.push(label);
    }
    const slots = ['08:00-10:00', '10:00-12:00', '14:00-16:00', '16:00-18:00', '19:00-21:00'];
    this.setData({ 'appointmentRange[0]': days, 'appointmentRange[1]': slots });
  },

  /** 预约时间列变化（保持两列联动即可，无需特殊处理） */
  onAppointmentColumnChange(e) {
    const idx = this.data.appointmentIndex.slice();
    idx[e.detail.column] = e.detail.value;
    this.setData({ appointmentIndex: idx });
  },

  /** 预约时间确认：拼成 yyyy-MM-dd HH:mm:ss */
  onAppointmentChange(e) {
    const idx = e.detail.value;
    const day = this.data.appointmentRange[0][idx[0]];
    const slot = this.data.appointmentRange[1][idx[1]];
    const startHour = slot.slice(0, 5); // 取时间段开始时刻
    this.setData({
      appointmentIndex: idx,
      appointmentText: day + ' ' + startHour + ':00'
    });
  },

  /** 选择图片：USER 限 1 张，VIP 限 5 张 */
  chooseImage() {
    const maxCount = this.data.isVip ? 5 : 1;
    const remain = maxCount - this.data.images.length;
    if (remain <= 0) {
      wx.showToast({ title: this.data.isVip ? '最多上传5张图片' : '普通用户限传1张，升级会员可传5张', icon: 'none' });
      return;
    }
    wx.chooseMedia({
      count: remain,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        // 逐张上传到后端
        res.tempFiles.forEach((file) => {
          this.uploadOne(file.tempFilePath);
        });
      }
    });
  },

  /**
   * 上传单张图片到 /api/file/upload
   * @param {string} filePath 本地临时文件路径
   */
  uploadOne(filePath) {
    wx.showLoading({ title: '上传中…', mask: true });
    uploadFile('/api/file/upload', filePath)
      .then((relativePath) => {
        const images = this.data.images.concat(relativePath);
        this.setData({ images });
      })
      .catch(() => {})
      .finally(() => {
        wx.hideLoading();
      });
  },

  /** 删除已选图片 */
  removeImage(e) {
    const idx = e.currentTarget.dataset.index;
    const images = this.data.images.slice();
    images.splice(idx, 1);
    this.setData({ images });
  },

  /** 预览已选图片 */
  previewImage(e) {
    const idx = e.currentTarget.dataset.index;
    const urls = this.data.images.map((p) => util.imageUrl(p));
    wx.previewImage({ current: urls[idx], urls });
  },

  /** 提交工单 */
  submit() {
    const d = this.data;
    if (d.submitting) {
      return;
    }
    // 基础校验
    if (d.locationTypeIndex === 0) {
      if (!d.buildingNo.trim() || !d.roomNo.trim()) {
        wx.showToast({ title: '请填写楼号和房间号', icon: 'none' });
        return;
      }
    } else if (!d.publicLocation.trim()) {
      wx.showToast({ title: '请填写具体位置', icon: 'none' });
      return;
    }
    // 分类校验：普通分类可多选；「其他」与普通分类互斥且自定义内容必填
    const selectedIds = d.categories
      .filter((c) => c.selected && c.id !== null)
      .map((c) => c.id);
    if (!d.otherSelected && selectedIds.length === 0) {
      wx.showToast({ title: '请选择故障分类（可多选）', icon: 'none' });
      return;
    }
    if (d.otherSelected && !d.customCategory.trim()) {
      wx.showToast({ title: '请填写故障类型', icon: 'none' });
      return;
    }
    if (!d.description.trim()) {
      wx.showToast({ title: '请填写故障描述', icon: 'none' });
      return;
    }
    // 多选分类传 categoryIds；选择「其他」时传空数组并改传 customCategory
    const body = {
      locationType: d.locationTypeIndex === 0 ? 'DORM' : 'PUBLIC',
      buildingNo: d.locationTypeIndex === 0 ? d.buildingNo.trim() : null,
      roomNo: d.locationTypeIndex === 0 ? d.roomNo.trim() : null,
      publicLocation: d.locationTypeIndex === 1 ? d.publicLocation.trim() : null,
      categoryIds: d.otherSelected ? [] : selectedIds,
      customCategory: d.otherSelected ? d.customCategory.trim() : null,
      description: d.description.trim(),
      images: d.images,
      urgent: d.isVip && d.urgent ? 1 : 0,
      appointmentTime: d.isVip && d.appointmentText ? d.appointmentText : null
    };
    this.setData({ submitting: true });
    post('/api/orders', body)
      .then(() => {
        wx.showToast({ title: '提交成功', icon: 'success' });
        // 重置表单并跳「我的工单」
        setTimeout(() => {
          this.resetForm();
          wx.switchTab({ url: '/pages/order/list' });
        }, 800);
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ submitting: false });
      });
  },

  /** 提交成功后重置表单 */
  resetForm() {
    this.setData({
      buildingNo: '',
      roomNo: '',
      publicLocation: '',
      categories: this.data.categories.map((c) => Object.assign({}, c, { selected: false })),
      otherSelected: false,
      customCategory: '',
      description: '',
      images: [],
      urgent: false,
      appointmentText: '',
      appointmentIndex: [0, 0]
    }, () => {
      this.refreshEstimate();
    });
  }
});
