// 我的工单列表（tabBar 页）：状态筛选 + 下拉刷新 + 上拉分页
const { get } = require('../../utils/request');
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
      { label: '已完成', value: 'FINISHED' },
      { label: '驳回', value: 'REJECTED' }
    ],
    activeTab: 0,      // 当前状态 tab
    list: [],          // 工单列表（已附加中文/颜色字段）
    pageNum: 1,
    hasMore: true,     // 是否还有下一页
    loading: false
  },

  onShow() {
    // 每次显示刷新第一页（提交成功后回来能看到新工单）
    const app = getApp();
    if (!app.checkLogin()) {
      this.setData({ list: [], hasMore: true, pageNum: 1 });
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

  /** 切换状态 tab */
  onTabChange(e) {
    const idx = Number(e.currentTarget.dataset.index);
    if (idx === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: idx }, () => {
      this.reload();
    });
  },

  /** 加载列表（分页累加） */
  loadList() {
    if (this.data.loading || !this.data.hasMore) {
      return;
    }
    this.setData({ loading: true });
    const status = this.data.tabs[this.data.activeTab].value;
    const params = { pageNum: this.data.pageNum, pageSize: PAGE_SIZE };
    if (status) {
      params.status = status;
    }
    get('/api/orders/my', params)
      .then((page) => {
        const records = (page.records || []).map(this.decorateOrder);
        const list = this.data.pageNum === 1 ? records : this.data.list.concat(records);
        this.setData({
          list,
          hasMore: this.data.pageNum < (page.pages || 1),
          loading: false
        });
      })
      .catch(() => {
        this.setData({ loading: false });
      });
  },

  /**
   * 给工单附加展示字段（状态中文、颜色、地点文本、描述摘要）
   * @param {object} order 工单原始数据
   * @returns {object}
   */
  decorateOrder(order) {
    const locationText = order.locationType === 'DORM'
      ? (order.buildingNo || '') + ' ' + (order.roomNo || '')
      : (order.publicLocation || '');
    return Object.assign({}, order, {
      statusText: util.statusText(order.status),
      statusColor: util.statusColor(order.status),
      locationText: locationText.trim(),
      descSummary: util.summary(order.description, 40),
      // 预估费用：null 表示面议
      estimatedText: order.estimatedPrice == null ? '面议' : '¥' + util.formatMoney(order.estimatedPrice)
    });
  },

  /** 下拉刷新 */
  onPullDownRefresh() {
    this.setData({ pageNum: 1, hasMore: true });
    const status = this.data.tabs[this.data.activeTab].value;
    const params = { pageNum: 1, pageSize: PAGE_SIZE };
    if (status) {
      params.status = status;
    }
    get('/api/orders/my', params)
      .then((page) => {
        this.setData({
          list: (page.records || []).map(this.decorateOrder),
          hasMore: 1 < (page.pages || 1),
          pageNum: 1
        });
      })
      .catch(() => {})
      .finally(() => {
        wx.stopPullDownRefresh();
      });
  },

  /** 上拉加载更多 */
  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) {
      return;
    }
    this.setData({ pageNum: this.data.pageNum + 1 }, () => {
      this.loadList();
    });
  },

  /** 点击卡片进详情 */
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/order/detail?id=' + id });
  },

  /** 空状态：去提交报修 */
  goSubmit() {
    wx.switchTab({ url: '/pages/repair/submit' });
  }
});
