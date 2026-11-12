// 身份切换（课程演示）：同一账号在普通用户 / 会员 / 维修工 / 管理员之间切换
// 依赖后端 PUT /api/auth/switch-role，返回与登录一致的 { token, user }
const { put } = require('./request');

/** 可选身份，顺序即 ActionSheet 展示顺序 */
const ROLE_OPTIONS = [
  { role: 'USER', text: '普通用户' },
  { role: 'VIP', text: '会员' },
  { role: 'WORKER', text: '维修工' },
  { role: 'ADMIN', text: '管理员' }
];

/** 切换中标记：模块级共享，防止首页与「我的」重复触发 */
let switching = false;

/**
 * 发起身份切换：选择身份 -> 二次确认 -> 调用后端并刷新登录态
 * 用户取消选择时不产生副作用，切换失败会 toast 提示
 * @returns {Promise<void>}
 */
function switchRole() {
  const app = getApp();
  const userInfo = app.globalData.userInfo;
  // 未登录时引导去登录页
  if (!app.globalData.token || !userInfo) {
    wx.showToast({ title: '请先登录', icon: 'none' });
    setTimeout(() => {
      wx.navigateTo({ url: '/pages/login/login' });
    }, 600);
    return Promise.resolve();
  }
  if (switching) {
    return Promise.resolve();
  }

  const currentRole = userInfo.role;
  const itemList = ROLE_OPTIONS.map((item) => (
    item.role === currentRole ? item.text + '（当前）' : item.text
  ));

  return new Promise((resolve) => {
    wx.showActionSheet({
      itemList,
      success(res) {
        const target = ROLE_OPTIONS[res.tapIndex];
        // 用户直接选中了当前身份，无需切换
        if (!target || target.role === currentRole) {
          resolve();
          return;
        }
        confirmAndSwitch(target).then(resolve);
      },
      fail() {
        // 用户取消选择
        resolve();
      }
    });
  });
}

/**
 * 二次确认后执行切换
 * @param {{ role: string, text: string }} target 目标身份
 * @returns {Promise<void>}
 */
function confirmAndSwitch(target) {
  return new Promise((resolve) => {
    wx.showModal({
      title: '切换身份',
      content: '课程演示功能：切换为「' + target.text + '」会立即改变菜单与数据权限，是否继续？',
      confirmText: '确认切换',
      confirmColor: '#2c6bf2',
      success(res) {
        if (!res.confirm) {
          resolve();
          return;
        }
        doSwitch(target).then(resolve);
      },
      fail() {
        resolve();
      }
    });
  });
}

/**
 * 调用切换接口并更新全局登录态
 * @param {{ role: string, text: string }} target 目标身份
 * @returns {Promise<void>}
 */
function doSwitch(target) {
  if (switching) {
    return Promise.resolve();
  }
  switching = true;
  const app = getApp();
  wx.showLoading({ title: '切换中', mask: true });
  return put('/api/auth/switch-role', { role: target.role })
    .then((data) => {
      // 与登录保持一致的存储方式：内存（globalData）+ 本地缓存
      app.setLogin(data.token, data.user);
      wx.hideLoading();
      wx.showToast({ title: '已切换为' + target.text, icon: 'success' });
      // 回到首页，使首页与「我的」按新身份重新渲染
      setTimeout(() => {
        wx.reLaunch({ url: '/pages/home/home' });
      }, 700);
    })
    .catch((err) => {
      // 收起 loading 后再补一次失败提示，避免 loading 覆盖 request 的统一 toast
      wx.hideLoading();
      wx.showToast({
        title: (err && err.message) || '切换失败，请稍后重试',
        icon: 'none'
      });
    })
    .finally(() => {
      switching = false;
    });
}

module.exports = { switchRole, ROLE_OPTIONS };
