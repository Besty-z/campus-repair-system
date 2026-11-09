// 用户管理页（仅 ADMIN）：搜索 + 分页列表 + 设置/取消会员
const { get, put } = require('../../utils/request');
const util = require('../../utils/util');

const PAGE_SIZE = 10;

Page({
  data: {
    keyword: '',
    list: [],
    pageNum: 1,
    hasMore: true,
    loading: false
  },

  onLoad() {
    this.reload();
  },

  /** 重置并加载第一页 */
  reload() {
    this.setData({ pageNum: 1, hasMore: true }, () => this.loadList());
  },

  /** 加载用户分页 */
  loadList() {
    if (this.data.loading || !this.data.hasMore) {
      return;
    }
    this.setData({ loading: true });
    const params = { pageNum: this.data.pageNum, pageSize: PAGE_SIZE };
    if (this.data.keyword.trim()) {
      params.keyword = this.data.keyword.trim();
    }
    get('/api/admin/users/page', params)
      .then((page) => {
        const records = (page.records || []).map((u) => Object.assign({}, u, {
          roleText: util.roleText(u.role),
          roleColor: util.roleColor(u.role)
        }));
        const list = this.data.pageNum === 1 ? records : this.data.list.concat(records);
        this.setData({
          list,
          hasMore: this.data.pageNum < (page.pages || 1),
          loading: false
        });
      })
      .catch(() => this.setData({ loading: false }));
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onSearch() {
    this.reload();
  },

  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) {
      return;
    }
    this.setData({ pageNum: this.data.pageNum + 1 }, () => this.loadList());
  },

  /**
   * 设置/取消会员（ADMIN 角色不显示按钮，由 wxml 控制）
   * @param {object} e 事件对象，dataset 带用户 id 与目标角色
   */
  setRole(e) {
    const { id, role } = e.currentTarget.dataset;
    const actionText = role === 'VIP' ? '设为会员' : '取消会员';
    wx.showModal({
      title: '操作确认',
      content: '确定' + actionText + '该用户吗？',
      success: (res) => {
        if (!res.confirm) return;
        put('/api/admin/users/' + id + '/role', { role })
          .then(() => {
            wx.showToast({ title: '操作成功', icon: 'success' });
            this.reload();
          })
          .catch(() => {});
      }
    });
  }
});
