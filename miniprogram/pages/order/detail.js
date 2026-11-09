// 工单详情页：USER/VIP 看本人工单，ADMIN 可看任意工单（后端控制权限）
const { get } = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    id: null,
    order: null,       // 工单详情（已附加中文/颜色字段）
    imageUrls: []      // 拼接完整域名后的图片地址，用于九宫格与预览
  },

  onLoad(options) {
    this.setData({ id: options.id });
    this.loadDetail();
  },

  /** 加载工单详情 */
  loadDetail() {
    get('/api/orders/' + this.data.id)
      .then((order) => {
        const app = getApp();
        // 拼装展示字段
        const decorated = Object.assign({}, order, {
          statusText: util.statusText(order.status),
          statusColor: util.statusColor(order.status),
          locationTypeText: util.locationTypeText(order.locationType),
          locationText: order.locationType === 'DORM'
            ? (order.buildingNo || '') + ' ' + (order.roomNo || '')
            : (order.publicLocation || '')
        });
        this.setData({
          order: decorated,
          imageUrls: (order.images || []).map((p) => app.globalData.baseUrl + p)
        });
      })
      .catch(() => {});
  },

  /** 点击图片放大预览 */
  previewImage(e) {
    const idx = e.currentTarget.dataset.index;
    wx.previewImage({ current: this.data.imageUrls[idx], urls: this.data.imageUrls });
  }
});
