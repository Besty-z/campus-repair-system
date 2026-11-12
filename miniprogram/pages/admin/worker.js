// 维修工管理（仅 ADMIN）：搜索 + 分页列表 + 新增/编辑 + 开通/停用
const { get, post, put } = require('../../utils/request');

const PAGE_SIZE = 10;

Page({
  data: {
    keyword: '',
    list: [],
    pageNum: 1,
    hasMore: true,
    loading: false,
    // 新增/编辑弹窗
    showModal: false,
    editingId: null,       // null=新增，否则为编辑的维修工ID
    formPhone: '',
    formName: '',
    formIntro: '',
    formStatus: 1,         // 1在职 0停用
    categories: [],        // 分类选项（含勾选状态）
    saving: false
  },

  onLoad() {
    this.loadCategories();
    this.reload();
  },

  /** 加载启用分类（用于擅长分类多选） */
  loadCategories() {
    get('/api/categories')
      .then((list) => {
        this.setData({ categories: this.buildCategories(list || [], []) });
      })
      .catch(() => {});
  },

  /**
   * 生成带勾选状态的分类选项
   * @param {Array} list 分类列表
   * @param {Array} selectedIds 已选分类ID
   * @returns {Array}
   */
  buildCategories(list, selectedIds) {
    return (list || []).map((item) =>
      Object.assign({}, item, { checked: selectedIds.indexOf(item.id) >= 0 })
    );
  },

  /** 重置并加载第一页 */
  reload() {
    this.setData({ pageNum: 1, hasMore: true }, () => {
      this.loadList();
    });
  },

  /** 加载维修工分页 */
  loadList() {
    if (this.data.loading || !this.data.hasMore) {
      return;
    }
    this.setData({ loading: true });
    const params = { pageNum: this.data.pageNum, pageSize: PAGE_SIZE };
    if (this.data.keyword.trim()) {
      params.keyword = this.data.keyword.trim();
    }
    get('/api/workers/admin/page', params)
      .then((page) => {
        const records = (page.records || []).map(this.decorateWorker);
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
   * 给维修工附加展示字段
   * @param {object} worker 维修工原始数据
   * @returns {object}
   */
  decorateWorker(worker) {
    return Object.assign({}, worker, {
      avatarText: (worker.name || '工').slice(0, 1),
      statusText: worker.status === 1 ? '在职' : '已停用',
      avgRatingText: worker.avgRating == null ? '暂无' : Number(worker.avgRating).toFixed(1),
      catNames: worker.categoryNames || []
    });
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  /** 关键词搜索确认 */
  onSearch() {
    this.reload();
  },

  /** 打开新增弹窗 */
  showAdd() {
    this.setData({
      showModal: true,
      editingId: null,
      formPhone: '',
      formName: '',
      formIntro: '',
      formStatus: 1,
      categories: this.buildCategories(this.data.categories, [])
    });
  },

  /** 打开编辑弹窗，回填原值 */
  showEdit(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      showModal: true,
      editingId: item.id,
      formPhone: item.phone || '',
      formName: item.name || '',
      formIntro: item.intro || '',
      formStatus: item.status == null ? 1 : item.status,
      categories: this.buildCategories(this.data.categories, item.categoryIds || [])
    });
  },

  hideModal() {
    this.setData({ showModal: false });
  },

  /** 弹窗内容点击不冒泡到遮罩 */
  noop() {},

  onFormPhoneInput(e) {
    this.setData({ formPhone: e.detail.value });
  },

  onFormNameInput(e) {
    this.setData({ formName: e.detail.value });
  },

  onFormIntroInput(e) {
    this.setData({ formIntro: e.detail.value });
  },

  /** 编辑时切换在职状态 */
  onFormStatusChange(e) {
    this.setData({ formStatus: e.detail.value ? 1 : 0 });
  },

  /** 切换擅长分类勾选 */
  toggleFormCategory(e) {
    const id = e.currentTarget.dataset.id;
    const categories = this.data.categories.map((item) =>
      item.id === id ? Object.assign({}, item, { checked: !item.checked }) : item
    );
    this.setData({ categories });
  },

  /** 保存维修工（新增或编辑） */
  saveWorker() {
    const { editingId, formPhone, formName, formIntro, formStatus, saving } = this.data;
    if (saving) {
      return;
    }
    if (!editingId && !/^1\d{10}$/.test(formPhone.trim())) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }
    if (!formName.trim()) {
      wx.showToast({ title: '请输入维修工姓名', icon: 'none' });
      return;
    }
    const categoryIds = this.data.categories
      .filter((item) => item.checked)
      .map((item) => item.id);
    const body = {
      name: formName.trim(),
      intro: formIntro.trim(),
      categoryIds
    };
    const promise = editingId
      ? put('/api/workers/admin/' + editingId, Object.assign({}, body, { status: formStatus }))
      : post('/api/workers/admin', Object.assign({}, body, { phone: formPhone.trim() }));
    this.setData({ saving: true });
    promise
      .then(() => {
        wx.showToast({ title: '保存成功', icon: 'success' });
        this.setData({ showModal: false });
        this.reload();
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ saving: false });
      });
  },

  /** 开通 / 停用维修工 */
  toggleStatus(e) {
    const item = e.currentTarget.dataset.item;
    const targetStatus = item.status === 1 ? 0 : 1;
    const actionText = targetStatus === 1 ? '开通' : '停用';
    wx.showModal({
      title: actionText + '确认',
      content: '确定' + actionText + '维修工「' + item.name + '」吗？',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        put('/api/workers/admin/' + item.id, {
          name: item.name,
          intro: item.intro || '',
          categoryIds: item.categoryIds || [],
          status: targetStatus
        })
          .then(() => {
            wx.showToast({ title: '操作成功', icon: 'success' });
            this.reload();
          })
          .catch(() => {});
      }
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
