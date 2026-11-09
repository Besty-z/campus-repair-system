// 分类管理页（仅 ADMIN）：按地点类型分组展示 + 新增/编辑弹窗 + 删除确认
const { get, post, put, del } = require('../../utils/request');

Page({
  data: {
    dormCategories: [],      // 宿舍楼内分类
    publicCategories: [],    // 公共区域分类
    // 新增/编辑弹窗
    showModal: false,
    editingId: null,         // null=新增，否则为编辑的分类ID
    formName: '',
    formLocationTypeIndex: 0, // 0=DORM 1=PUBLIC
    formSort: '',
    formStatus: 1,           // 编辑时保留原状态（1启用 0停用）
    locationTypeLabels: ['宿舍楼内', '校园公共区域']
  },

  onLoad() {
    this.loadCategories();
  },

  /** 加载全量分类（含停用，仅 ADMIN 接口） */
  loadCategories() {
    get('/api/categories/admin/all')
      .then((list) => {
        const all = list || [];
        this.setData({
          dormCategories: all.filter((c) => c.locationType === 'DORM'),
          publicCategories: all.filter((c) => c.locationType === 'PUBLIC')
        });
      })
      .catch(() => {});
  },

  /** 打开新增弹窗 */
  showAdd() {
    this.setData({
      showModal: true,
      editingId: null,
      formName: '',
      formLocationTypeIndex: 0,
      formSort: ''
    });
  },

  /** 打开编辑弹窗，回填原值 */
  showEdit(e) {
    const item = e.currentTarget.dataset.item;
    this.setData({
      showModal: true,
      editingId: item.id,
      formName: item.name,
      formLocationTypeIndex: item.locationType === 'DORM' ? 0 : 1,
      formSort: String(item.sort),
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

  onFormLocationTypeChange(e) {
    this.setData({ formLocationTypeIndex: Number(e.detail.value) });
  },

  onFormSortInput(e) {
    this.setData({ formSort: e.detail.value });
  },

  /** 保存（新增或编辑） */
  saveCategory() {
    const { editingId, formName, formLocationTypeIndex, formSort, formStatus } = this.data;
    if (!formName.trim()) {
      wx.showToast({ title: '请输入分类名称', icon: 'none' });
      return;
    }
    const body = {
      name: formName.trim(),
      locationType: formLocationTypeIndex === 0 ? 'DORM' : 'PUBLIC',
      sort: parseInt(formSort, 10) || 0
    };
    // 编辑时保留原状态字段
    const promise = editingId
      ? put('/api/categories/' + editingId, Object.assign({}, body, { status: formStatus == null ? 1 : formStatus }))
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
