// 维修工「我的资料」：身份信息 + 数据统计 + 简介与擅长分类编辑
const { get, put } = require('../../utils/request');

Page({
  data: {
    worker: null,          // 维修工档案（GET /workers/me）
    avatarText: '工',
    statusText: '',        // 在职状态文案
    completedCount: 0,     // 完成工单数
    avgRatingText: '暂无', // 平均评分
    ratingCount: 0,        // 评价数
    categories: [],        // 分类选项（含勾选状态）
    intro: '',             // 简介编辑值
    loading: true,
    saving: false
  },

  onLoad() {
    this.loadCategories();
    this.loadProfile();
  },

  /** 加载维修工档案 */
  loadProfile() {
    get('/api/workers/me')
      .then((worker) => {
        const selectedIds = worker.categoryIds || [];
        this.setData({
          worker,
          avatarText: (worker.name || '工').slice(0, 1),
          statusText: worker.status === 1 ? '在职' : '已停用',
          completedCount: worker.completedCount || 0,
          avgRatingText: worker.avgRating == null ? '暂无' : Number(worker.avgRating).toFixed(1),
          ratingCount: worker.ratingCount || 0,
          intro: worker.intro || '',
          categories: this.buildCategories(this.data.categories, selectedIds),
          loading: false
        });
      })
      .catch(() => {
        this.setData({ worker: null, loading: false });
      });
  },

  /** 加载启用分类（用于擅长分类多选） */
  loadCategories() {
    get('/api/categories')
      .then((list) => {
        const selectedIds = this.data.worker ? (this.data.worker.categoryIds || []) : [];
        this.setData({
          categories: this.buildCategories(list || [], selectedIds)
        });
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

  /** 切换擅长分类勾选 */
  toggleCategory(e) {
    const id = e.currentTarget.dataset.id;
    const categories = this.data.categories.map((item) =>
      item.id === id ? Object.assign({}, item, { checked: !item.checked }) : item
    );
    this.setData({ categories });
  },

  /** 简介输入 */
  onIntroInput(e) {
    this.setData({ intro: e.detail.value });
  },

  /** 保存资料：PUT /workers/me */
  saveProfile() {
    if (this.data.saving || !this.data.worker) {
      return;
    }
    const categoryIds = this.data.categories
      .filter((item) => item.checked)
      .map((item) => item.id);
    this.setData({ saving: true });
    put('/api/workers/me', {
      intro: this.data.intro.trim(),
      categoryIds
    })
      .then(() => {
        wx.showToast({ title: '保存成功', icon: 'success' });
        this.loadProfile();
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ saving: false });
      });
  }
});
