// 会员报修统计页（仅 VIP 入口可见，后端也会校验权限）
const { get } = require('../../utils/request');

Page({
  data: {
    stats: null,        // 统计总览 { total, pending, inProgress, finished, rejected }
    byCategory: [],     // 分类分布（附加百分比）
    byMonth: []         // 按月统计
  },

  onLoad() {
    this.loadStats();
  },

  /** 加载统计数据 */
  loadStats() {
    get('/api/orders/my/stats')
      .then((data) => {
        // 计算分类占比的最大值，用于进度条宽度换算
        const list = data.byCategory || [];
        const max = list.reduce((m, item) => Math.max(m, item.count), 0) || 1;
        const byCategory = list.map((item) => ({
          categoryName: item.categoryName,
          count: item.count,
          percent: Math.round((item.count / max) * 100)
        }));
        this.setData({
          stats: data,
          byCategory,
          byMonth: data.byMonth || []
        });
      })
      .catch(() => {});
  }
});
