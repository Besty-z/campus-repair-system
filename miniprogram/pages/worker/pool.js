// 维修工「抢单大厅」：待接单且未指派工单，紧急优先
const { get, put } = require('../../utils/request');
const util = require('../../utils/util');
const images = require('../../utils/images');

const PAGE_SIZE = 10;

Page({
  data: {
    emptyImage: images.EMPTY_IMAGE, // 空状态插图
    list: [],          // 抢单池列表（已附加展示字段）
    pageNum: 1,
    hasMore: true,
    loading: false
  },

  onShow() {
    const app = getApp();
    if (!app.checkLogin()) {
      this.setData({ list: [], pageNum: 1, hasMore: true });
      return;
    }
    this.reload();
  },

  /** 重新加载第一页 */
  reload() {
    this.setData({ pageNum: 1, hasMore: true }, () => {
      this.loadList();
    });
  },

  /** 加载抢单池分页 */
  loadList() {
    if (this.data.loading || !this.data.hasMore) {
      return;
    }
    this.setData({ loading: true });
    get('/api/orders/worker/pool', {
      pageNum: this.data.pageNum,
      pageSize: PAGE_SIZE
    })
      .then((page) => {
        const records = (page.records || []).map(this.decorateOrder);
        const list = this.data.pageNum === 1 ? records : this.data.list.concat(records);
        this.setData({
          list,
          hasMore: this.computeHasMore(page, list.length),
          loading: false
        });
      })
      .catch(() => {
        this.setData({ loading: false });
      });
  },

  /**
   * 计算是否还有下一页：优先使用 total，兼容 pages 字段
   * @param {object} page 后端分页数据
   * @param {number} loadedCount 已加载条数
   * @returns {boolean}
   */
  computeHasMore(page, loadedCount) {
    if (typeof page.total === 'number') {
      return loadedCount < page.total;
    }
    if (typeof page.pages === 'number') {
      return this.data.pageNum < page.pages;
    }
    return (page.records || []).length >= PAGE_SIZE;
  },

  /**
   * 给工单附加展示字段（状态中文、颜色、地点文本、分类标签）
   * @param {object} order 工单原始数据
   * @returns {object}
   */
  decorateOrder(order) {
    const locationText = order.locationType === 'DORM'
      ? ((order.buildingNo || '') + ' ' + (order.roomNo || '')).trim()
      : (order.publicLocation || '');
    const catNames = order.categoryNames && order.categoryNames.length > 0
      ? order.categoryNames
      : [order.categoryName || '其他故障'];
    return Object.assign({}, order, {
      statusText: util.statusText(order.status),
      statusColor: util.statusColor(order.status),
      locationText,
      catNames,
      descSummary: util.summary(order.description, 40)
    });
  },

  /** 抢单：接取后刷新列表 */
  grabOrder(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '抢单确认',
      content: '确定接取该工单吗？接单后请尽快联系报修人。',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        put('/api/orders/worker/' + id + '/accept')
          .then(() => {
            wx.showToast({ title: '抢单成功', icon: 'success' });
            this.reload();
          })
          .catch(() => {});
      }
    });
  },

  /** 查看工单详情 */
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/order/detail?id=' + id });
  },

  /** 下拉刷新 */
  onPullDownRefresh() {
    this.setData({ pageNum: 1, hasMore: true });
    get('/api/orders/worker/pool', { pageNum: 1, pageSize: PAGE_SIZE })
      .then((page) => {
        const list = (page.records || []).map(this.decorateOrder);
        this.setData({
          list,
          hasMore: this.computeHasMore(page, list.length),
          pageNum: 1
        });
      })
      .catch(() => {})
      .finally(() => {
        wx.stopPullDownRefresh();
      });
  },

  /** 触底加载更多 */
  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) {
      return;
    }
    this.setData({ pageNum: this.data.pageNum + 1 }, () => {
      this.loadList();
    });
  }
});
