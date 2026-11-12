// 工单详情页：USER/VIP 看本人工单，ADMIN 可看任意工单（后端控制权限）
const { get } = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    id: null,
    order: null,       // 工单详情（已附加中文/颜色字段）
    steps: [],         // 维修进度时间线
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
        // 拼装展示字段
        const decorated = Object.assign({}, order, {
          statusText: util.statusText(order.status),
          statusColor: util.statusColor(order.status),
          statusDesc: util.statusDesc(order.status),
          locationTypeText: util.locationTypeText(order.locationType),
          categoryName: order.categoryName || order.customCategory || '其他故障',
          locationText: order.locationType === 'DORM'
            ? (order.buildingNo || '') + ' ' + (order.roomNo || '')
            : (order.publicLocation || '')
        });
        this.setData({
          order: decorated,
          steps: this.buildSteps(order),
          imageUrls: (order.images || []).map((p) => util.imageUrl(p))
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
  }
});
