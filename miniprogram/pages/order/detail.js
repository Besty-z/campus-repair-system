// 工单详情页：USER/VIP 看本人工单，ADMIN 可看任意工单（后端控制权限）
const { get, post } = require('../../utils/request');
const util = require('../../utils/util');
const images = require('../../utils/images');

Page({
  data: {
    emptyImage: images.EMPTY_IMAGE, // 无故障照片时的占位图
    id: null,
    order: null,       // 工单详情（已附加中文/颜色字段）
    steps: [],         // 维修进度时间线
    imageUrls: [],     // 拼接完整域名后的图片地址，用于九宫格与预览
    canRate: false,    // 当前用户是否可评价（报修人 + 已完成 + 未评价）
    stars: [1, 2, 3, 4, 5],
    showRateModal: false,
    formScore: 5,
    formComment: '',
    submitting: false
  },

  onLoad(options) {
    this.setData({ id: options.id });
    this.loadDetail();
  },

  /** 加载工单详情 */
  loadDetail() {
    get('/api/orders/' + this.data.id)
      .then((order) => {
        // 拼装展示字段
        const decorated = Object.assign({}, order, {
          statusText: util.statusText(order.status),
          statusColor: util.statusColor(order.status),
          statusDesc: util.statusDesc(order.status),
          locationTypeText: util.locationTypeText(order.locationType),
          categoryName: order.categoryName || order.customCategory || '其他故障',
          // 预估费用：null 表示面议
          estimatedText: order.estimatedPrice == null ? '面议' : '¥' + util.formatMoney(order.estimatedPrice),
          locationText: order.locationType === 'DORM'
            ? (order.buildingNo || '') + ' ' + (order.roomNo || '')
            : (order.publicLocation || '')
        });
        // 当前用户是报修人、工单已完成且尚未评价时，展示「评价维修」入口
        const userInfo = getApp().globalData.userInfo;
        const isRequester = !!(userInfo && order.userId != null
          && String(userInfo.id) === String(order.userId));
        this.setData({
          order: decorated,
          steps: this.buildSteps(order),
          imageUrls: (order.images || []).map((p) => util.imageUrl(p)),
          canRate: isRequester && order.status === 'FINISHED' && !order.ratingScore
        });
      })
      .catch(() => {});
  },

  /** 根据工单时间构建进度节点 */
  buildSteps(order) {
    return [
      { name: '提交报修', time: order.createTime, done: true },
      { name: '维修接单', time: order.acceptTime, done: !!order.acceptTime },
      { name: '维修完成', time: order.finishTime, done: !!order.finishTime }
    ];
  },

  /** 点击图片放大预览 */
  previewImage(e) {
    const idx = e.currentTarget.dataset.index;
    wx.previewImage({ current: this.data.imageUrls[idx], urls: this.data.imageUrls });
  },

  /** 打开评价弹窗 */
  openRate() {
    this.setData({ showRateModal: true, formScore: 5, formComment: '' });
  },

  /** 关闭评价弹窗 */
  hideRate() {
    this.setData({ showRateModal: false });
  },

  /** 弹窗内容点击不冒泡到遮罩 */
  noop() {},

  /** 选择星级 */
  onScoreTap(e) {
    this.setData({ formScore: Number(e.currentTarget.dataset.score) });
  },

  /** 评价内容输入 */
  onCommentInput(e) {
    this.setData({ formComment: e.detail.value });
  },

  /** 提交评价 */
  submitRating() {
    const score = Number(this.data.formScore);
    if (!score || score < 1 || score > 5) {
      wx.showToast({ title: '请选择 1-5 星评分', icon: 'none' });
      return;
    }
    if (this.data.submitting) {
      return;
    }
    this.setData({ submitting: true });
    post('/api/orders/' + this.data.id + '/rating', {
      score,
      comment: this.data.formComment.trim()
    })
      .then(() => {
        wx.showToast({ title: '评价成功', icon: 'success' });
        this.setData({ showRateModal: false });
        this.loadDetail();
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ submitting: false });
      });
  }
});
