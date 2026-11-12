// 工单管理页（仅 ADMIN）：筛选 + 分页列表 + 接单 / 改状态 / 查看详情
const { get, put } = require('../../utils/request');
const util = require('../../utils/util');

const PAGE_SIZE = 10;

Page({
  data: {
    statusTabs: [
      { label: '全部', value: '' },
      { label: '待接单', value: 'PENDING' },
      { label: '维修中', value: 'IN_PROGRESS' },
      { label: '已完成', value: 'FINISHED' },
      { label: '驳回', value: 'REJECTED' }
    ],
    statusIndex: 0,
    locationTypes: [
      { label: '全部地点', value: '' },
      { label: '宿舍楼内', value: 'DORM' },
      { label: '公共区域', value: 'PUBLIC' }
    ],
    locationTypeIndex: 0,
    categories: [{ id: '', name: '全部分类' }], // 分类筛选（含「全部」项）
    categoryIndex: 0,
    keyword: '',
    list: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    // 指派维修工弹窗
    showAssignModal: false,
    assignOrderId: null,
    assignWorkers: [],
    assignWorkerId: null
  },

  onLoad() {
    this.loadCategories();
    this.reload();
  },

  /** 加载分类（筛选用，全部启用分类） */
  loadCategories() {
    get('/api/categories').then((list) => {
      this.setData({ categories: [{ id: '', name: '全部分类' }].concat(list || []) });
    }).catch(() => {});
  },

  /** 重置并加载第一页 */
  reload() {
    this.setData({ pageNum: 1, hasMore: true }, () => this.loadList());
  },

  /** 加载分页列表 */
  loadList() {
    if (this.data.loading || !this.data.hasMore) {
      return;
    }
    this.setData({ loading: true });
    const params = { pageNum: this.data.pageNum, pageSize: PAGE_SIZE };
    const status = this.data.statusTabs[this.data.statusIndex].value;
    const locationType = this.data.locationTypes[this.data.locationTypeIndex].value;
    const category = this.data.categories[this.data.categoryIndex];
    if (status) params.status = status;
    if (locationType) params.locationType = locationType;
    if (category && category.id) params.categoryId = category.id;
    if (this.data.keyword.trim()) params.keyword = this.data.keyword.trim();
    get('/api/orders/admin/page', params)
      .then((page) => {
        const records = (page.records || []).map(this.decorateOrder);
        const list = this.data.pageNum === 1 ? records : this.data.list.concat(records);
        this.setData({
          list,
          hasMore: this.data.pageNum < (page.pages || 1),
          loading: false
        });
      })
      .catch(() => this.setData({ loading: false }));
  },

  /**
   * 给工单附加展示字段
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
      descSummary: util.summary(order.description, 40)
    });
  },

  onStatusTabChange(e) {
    this.setData({ statusIndex: Number(e.currentTarget.dataset.index) }, () => this.reload());
  },

  onLocationTypeChange(e) {
    this.setData({ locationTypeIndex: Number(e.detail.value) }, () => this.reload());
  },

  onCategoryChange(e) {
    this.setData({ categoryIndex: Number(e.detail.value) }, () => this.reload());
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  /** 关键词搜索确认 */
  onSearch() {
    this.reload();
  },

  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) {
      return;
    }
    this.setData({ pageNum: this.data.pageNum + 1 }, () => this.loadList());
  },

  onPullDownRefresh() {
    this.setData({ pageNum: 1, hasMore: true });
    this.loadList();
    // 简单延迟停止刷新动画（loadList 异步完成前动画保留）
    setTimeout(() => wx.stopPullDownRefresh(), 800);
  },

  /** 接单：PENDING → IN_PROGRESS */
  acceptOrder(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '接单确认',
      content: '确定接取该工单吗？',
      success: (res) => {
        if (!res.confirm) return;
        put('/api/orders/admin/' + id + '/accept')
          .then(() => {
            wx.showToast({ title: '接单成功', icon: 'success' });
            this.reload();
          })
          .catch(() => {});
      }
    });
  },

  /** 修改状态：先选目标状态，再填维修备注 */
  changeStatus(e) {
    const id = e.currentTarget.dataset.id;
    const currentStatus = e.currentTarget.dataset.status;
    // 根据当前状态给出允许流转的目标状态
    let options = [];
    if (currentStatus === 'PENDING') {
      options = ['IN_PROGRESS', 'REJECTED'];
    } else if (currentStatus === 'IN_PROGRESS') {
      options = ['FINISHED', 'REJECTED'];
    } else {
      wx.showToast({ title: '终态工单不可修改', icon: 'none' });
      return;
    }
    const optionTexts = options.map((s) => util.statusText(s));
    wx.showActionSheet({
      itemList: optionTexts,
      success: (res) => {
        const targetStatus = options[res.tapIndex];
        this.promptRemark(id, targetStatus);
      }
    });
  },

  /**
   * 弹输入框填写维修备注并提交状态修改
   * @param {number|string} id 工单ID
   * @param {string} status 目标状态
   */
  promptRemark(id, status) {
    wx.showModal({
      title: '修改为「' + util.statusText(status) + '」',
      editable: true,
      placeholderText: '请填写维修备注（可选）',
      success: (res) => {
        if (!res.confirm) return;
        put('/api/orders/admin/' + id + '/status', {
          status,
          adminRemark: (res.content || '').trim()
        })
          .then(() => {
            wx.showToast({ title: '修改成功', icon: 'success' });
            this.reload();
          })
          .catch(() => {});
      }
    });
  },

  /** 查看详情（复用详情页，后端允许 ADMIN 看任意工单） */
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/order/detail?id=' + id });
  },

  /** 打开指派弹窗，加载在职维修工 */
  openAssign(e) {
    const id = e.currentTarget.dataset.id;
    this.setData({ assignOrderId: id, assignWorkerId: null });
    get('/api/workers/admin/enabled')
      .then((list) => {
        const workers = list || [];
        this.setData({ assignWorkers: workers, showAssignModal: true });
        if (workers.length === 0) {
          wx.showToast({ title: '暂无在职维修工', icon: 'none' });
        }
      })
      .catch(() => {});
  },

  /** 关闭指派弹窗 */
  hideAssign() {
    this.setData({ showAssignModal: false });
  },

  /** 弹窗内容点击不冒泡到遮罩 */
  noop() {},

  /** 选择维修工 */
  selectWorker(e) {
    this.setData({ assignWorkerId: e.currentTarget.dataset.id });
  },

  /** 确认指派 */
  confirmAssign() {
    const { assignOrderId, assignWorkerId } = this.data;
    if (!assignWorkerId) {
      wx.showToast({ title: '请选择维修工', icon: 'none' });
      return;
    }
    put('/api/orders/admin/' + assignOrderId + '/assign', { workerId: assignWorkerId })
      .then(() => {
        wx.showToast({ title: '指派成功', icon: 'success' });
        this.setData({ showAssignModal: false });
        this.reload();
      })
      .catch(() => {});
  }
});
