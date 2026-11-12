// 维修工「我的任务」：状态筛选 + 下拉刷新 + 触底分页 + 接单 / 完成维修
const { get, put } = require('../../utils/request');
const util = require('../../utils/util');
const images = require('../../utils/images');

const PAGE_SIZE = 10;

Page({
  data: {
    emptyImage: images.EMPTY_IMAGE, // 空状态插图
    tabs: [
      { label: '全部', value: '' },
      { label: '待接单', value: 'PENDING' },
      { label: '维修中', value: 'IN_PROGRESS' },
      { label: '已完成', value: 'FINISHED' }
    ],
    activeTab: 0,        // 当前状态 tab
    list: [],            // 任务列表（已附加展示字段）
    pageNum: 1,
    hasMore: true,       // 是否还有下一页
    loading: false,
    // 完成维修弹窗
    showFinishModal: false,
    finishOrderId: null,
    finishRemark: ''
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

  /** 切换状态筛选 */
  onTabChange(e) {
    const idx = Number(e.currentTarget.dataset.index);
    if (idx === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: idx }, () => {
      this.reload();
    });
  },

  /** 当前筛选状态值 */
  currentStatus() {
    return this.data.tabs[this.data.activeTab].value;
  },

  /** 加载任务分页 */
  loadList() {
    if (this.data.loading || !this.data.hasMore) {
      return;
    }
    this.setData({ loading: true });
    const params = { pageNum: this.data.pageNum, pageSize: PAGE_SIZE };
    const status = this.currentStatus();
    if (status) {
      params.status = status;
    }
    get('/api/orders/worker/page', params)
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
      descSummary: util.summary(order.description, 36)
    });
  },

  /** 接单：PENDING → IN_PROGRESS */
  acceptOrder(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '接单确认',
      content: '确定接取该工单吗？',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        put('/api/orders/worker/' + id + '/accept')
          .then(() => {
            wx.showToast({ title: '接单成功', icon: 'success' });
            this.reload();
          })
          .catch(() => {});
      }
    });
  },

  /** 打开完成维修弹窗 */
  openFinish(e) {
    this.setData({
      showFinishModal: true,
      finishOrderId: e.currentTarget.dataset.id,
      finishRemark: ''
    });
  },

  /** 关闭完成维修弹窗 */
  hideFinish() {
    this.setData({ showFinishModal: false });
  },

  /** 弹窗内容点击不冒泡到遮罩 */
  noop() {},

  /** 维修备注输入 */
  onRemarkInput(e) {
    this.setData({ finishRemark: e.detail.value });
  },

  /** 提交完成维修 */
  submitFinish() {
    const { finishOrderId, finishRemark } = this.data;
    if (!finishOrderId) {
      return;
    }
    put('/api/orders/worker/' + finishOrderId + '/status', {
      status: 'FINISHED',
      adminRemark: finishRemark.trim()
    })
      .then(() => {
        wx.showToast({ title: '维修已完成', icon: 'success' });
        this.setData({ showFinishModal: false });
        this.reload();
      })
      .catch(() => {});
  },

  /** 查看工单详情 */
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/order/detail?id=' + id });
  },

  /** 去抢单大厅 */
  goPool() {
    wx.navigateTo({ url: '/pages/worker/pool' });
  },

  /** 下拉刷新 */
  onPullDownRefresh() {
    this.setData({ pageNum: 1, hasMore: true });
    const params = { pageNum: 1, pageSize: PAGE_SIZE };
    const status = this.currentStatus();
    if (status) {
      params.status = status;
    }
    get('/api/orders/worker/page', params)
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
