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
    categories: [],            // 当前地点类型下的分类列表（末尾含「其他（自行填写）」）
    categoryIndex: -1,         // 选中的分类下标
    isOtherCategory: false,    // 是否选择了「其他（自行填写）」
    customCategory: '',        // 自定义故障类型（选择「其他」时填写）
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

  onShow() {
    // tabBar 页面：每次显示时检查登录态
    const app = getApp();
    if (!app.checkLogin()) {
      return;
    }
    const userInfo = app.globalData.userInfo || {};
    this.setData({ isVip: userInfo.role === 'VIP', imgBaseUrl: app.globalData.imageBaseUrl });
    this.initAppointmentRange();
    this.loadCategories();
  },

  /** 加载故障分类（按地点类型联动），末尾追加「其他（自行填写）」 */
  loadCategories() {
    const locationType = this.data.locationTypeIndex === 0 ? 'DORM' : 'PUBLIC';
    get('/api/categories', { locationType }).then((list) => {
      const categories = (list || []).concat([OTHER_CATEGORY]);
      this.setData({
        categories,
        categoryIndex: -1,
        isOtherCategory: false,
        customCategory: ''
      });
    }).catch(() => {});
  },

  /** 切换地点类型：清空对应字段、已选分类与自定义输入，并重新加载分类 */
  onLocationTypeChange(e) {
    const idx = Number(e.currentTarget.dataset.value);
    this.setData({
      locationTypeIndex: idx,
      buildingNo: '',
      roomNo: '',
      publicLocation: '',
      categoryIndex: -1,
      isOtherCategory: false,
      customCategory: ''
    });
    this.loadCategories();
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

  /** 分类选择：选中最后一项（「其他（自行填写）」）时展示自定义输入框 */
  onCategoryChange(e) {
    const idx = Number(e.detail.value);
    const selected = this.data.categories[idx];
    const isOtherCategory = !!(selected && selected.id === null);
    const updates = { categoryIndex: idx, isOtherCategory };
    if (!isOtherCategory) {
      updates.customCategory = '';
    }
    this.setData(updates);
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
    if (d.categoryIndex < 0) {
      wx.showToast({ title: '请选择故障分类', icon: 'none' });
      return;
    }
    if (d.isOtherCategory && !d.customCategory.trim()) {
      wx.showToast({ title: '请填写故障类型', icon: 'none' });
      return;
    }
    if (!d.description.trim()) {
      wx.showToast({ title: '请填写故障描述', icon: 'none' });
      return;
    }
    // 选择「其他（自行填写）」时 categoryId 传 null，改传 customCategory
    const body = {
      locationType: d.locationTypeIndex === 0 ? 'DORM' : 'PUBLIC',
      buildingNo: d.locationTypeIndex === 0 ? d.buildingNo.trim() : null,
      roomNo: d.locationTypeIndex === 0 ? d.roomNo.trim() : null,
      publicLocation: d.locationTypeIndex === 1 ? d.publicLocation.trim() : null,
      categoryId: d.isOtherCategory ? null : d.categories[d.categoryIndex].id,
      customCategory: d.isOtherCategory ? d.customCategory.trim() : null,
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
      categoryIndex: -1,
      isOtherCategory: false,
      customCategory: '',
      description: '',
      images: [],
      urgent: false,
      appointmentText: '',
      appointmentIndex: [0, 0]
    });
  }
});
