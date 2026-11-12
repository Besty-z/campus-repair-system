// 分类管理页（仅 ADMIN）：分类列表（按维修工种划分）+ 新增/编辑弹窗 + 删除确认
const { get, post, put, del } = require('../../utils/request');
const util = require('../../utils/util');

Page({
  data: {
    categories: [],          // 全量分类（含停用）
    // 新增/编辑弹窗
    showModal: false,
    editingId: null,         // null=新增，否则为编辑的分类ID
    formName: '',
    formSort: '',
    formPrice: '',           // 服务价格（元，必填）
    formImageUrl: '',        // 编辑时保留原图片地址
    formStatus: 1            // 编辑时保留原状态（1启用 0停用）
  },

  onLoad() {
    this.loadCategories();
  },

  /** 加载全量分类（含停用，仅 ADMIN 接口），附带价格展示文本 */
  loadCategories() {
    get('/api/categories/admin/all')
      .then((list) => {
        const categories = (list || []).map((c) => Object.assign({}, c, {
          priceText: c.price == null ? '面议' : '¥' + util.formatMoney(c.price)
        }));
        this.setData({ categories });
      })
      .catch(() => {});
  },

  /** 打开新增弹窗 */
  showAdd() {
    this.setData({
      showModal: true,
      editingId: null,
      formName: '',
      formSort: '',
      formPrice: '',
      formImageUrl: '',
      formStatus: 1
    });
  },

  /** 打开编辑弹窗，回填原值（价格、图片地址按原值保留） */
  showEdit(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      showModal: true,
      editingId: item.id,
      formName: item.name,
      formSort: String(item.sort),
      formPrice: item.price == null ? '' : String(item.price),
      formImageUrl: item.imageUrl || '',
      formStatus: item.status
    });
  },

  hideModal() {
    this.setData({ showModal: false });
  },

  /** 空方法：阻止弹窗内容点击冒泡到遮罩关闭弹窗 */
  noop() {},

  onFormNameInput(e) {
    this.setData({ formName: e.detail.value });
  },

  onFormSortInput(e) {
    this.setData({ formSort: e.detail.value });
  },

  onFormPriceInput(e) {
    this.setData({ formPrice: e.detail.value });
  },

  /** 保存（新增或编辑） */
  saveCategory() {
    const { editingId, formName, formSort, formPrice, formImageUrl, formStatus } = this.data;
    if (!formName.trim()) {
      wx.showToast({ title: '请输入分类名称', icon: 'none' });
      return;
    }
    // 价格必填，最多两位小数
    const priceText = String(formPrice).trim();
    if (!priceText) {
      wx.showToast({ title: '请输入服务价格', icon: 'none' });
      return;
    }
    if (!/^\d+(\.\d{1,2})?$/.test(priceText)) {
      wx.showToast({ title: '价格格式不正确（最多两位小数）', icon: 'none' });
      return;
    }
    const body = {
      name: formName.trim(),
      sort: parseInt(formSort, 10) || 0,
      price: Number(priceText)
    };
    // 编辑时保留原状态与图片地址
    const promise = editingId
      ? put('/api/categories/' + editingId, Object.assign({}, body, {
        status: formStatus == null ? 1 : formStatus,
        imageUrl: formImageUrl
      }))
      : post('/api/categories', body);
    promise
      .then(() => {
        wx.showToast({ title: '保存成功', icon: 'success' });
        this.setData({ showModal: false });
        this.loadCategories();
      })
      .catch(() => {});
  },

  /** 删除分类：确认后调用接口，toast 展示后端返回的实际删除行为 */
  deleteCategory(e) {
    const item = e.currentTarget.dataset.item;
    wx.showModal({
      title: '删除确认',
      content: '确定删除分类「' + item.name + '」吗？',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return;
        // raw 模式取完整响应体，后端 message 说明是逻辑删除还是物理删除
        del('/api/categories/' + item.id, { raw: true })
          .then((body) => {
            wx.showToast({ title: body.message || '删除成功', icon: 'none' });
            this.loadCategories();
          })
          .catch(() => {});
      }
    });
  }
});
