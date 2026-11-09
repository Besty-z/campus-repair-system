// 网络请求封装：Promise 版 wx.request，自动带 token，统一错误处理
// 注意：getApp() 必须在函数内调用，模块加载时 App 实例可能尚未创建

/**
 * 统一请求方法
 * @param {object} options 请求配置 { url, method, data, header }
 *   url 以 /api 开头，自动拼接全局 baseUrl
 * @returns {Promise<any>} 成功时 resolve 后端返回的 data 字段
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const app = getApp();
    const baseUrl = (app && app.globalData.baseUrl) || 'http://localhost:8080';
    const token = (app && app.globalData.token) || wx.getStorageSync('token') || '';

    wx.request({
      url: baseUrl + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: Object.assign(
        { 'Content-Type': 'application/json' },
        token ? { Authorization: 'Bearer ' + token } : {},
        options.header || {}
      ),
      success(res) {
        const body = res.data || {};
        // 后端统一响应体 { code, message, data }
        if (res.statusCode === 200 && body.code === 200) {
          // raw 模式返回完整响应体（用于需要读取 message 的场景，如分类删除）
          resolve(options.raw ? body : body.data);
          return;
        }
        // 401：登录失效，清除登录态并跳登录页
        if (res.statusCode === 401 || body.code === 401) {
          wx.showToast({ title: body.message || '请重新登录', icon: 'none' });
          const app = getApp();
          if (app) {
            app.clearLogin();
          }
          setTimeout(() => {
            wx.navigateTo({ url: '/pages/login/login' });
          }, 800);
          reject(body);
          return;
        }
        // 其它业务错误：toast 提示后端 message
        wx.showToast({ title: body.message || '请求失败', icon: 'none' });
        reject(body);
      },
      fail(err) {
        wx.showToast({ title: '网络异常，请检查后端是否启动', icon: 'none' });
        reject(err);
      }
    });
  });
}

/**
 * GET 请求
 * @param {string} url 接口路径（/api 开头）
 * @param {object} data 查询参数
 * @returns {Promise<any>}
 */
function get(url, data) {
  return request({ url, method: 'GET', data });
}

/**
 * POST 请求
 * @param {string} url 接口路径
 * @param {object} data 请求体
 * @returns {Promise<any>}
 */
function post(url, data) {
  return request({ url, method: 'POST', data });
}

/**
 * PUT 请求
 * @param {string} url 接口路径
 * @param {object} data 请求体
 * @returns {Promise<any>}
 */
function put(url, data) {
  return request({ url, method: 'PUT', data });
}

/**
 * DELETE 请求
 * @param {string} url 接口路径
 * @param {object} options 可选配置，{ raw: true } 时 resolve 完整响应体 { code, message, data }
 * @returns {Promise<any>}
 */
function del(url, options) {
  return request(Object.assign({ url, method: 'DELETE' }, options || {}));
}

/**
 * 上传文件（multipart/form-data）
 * @param {string} url 接口路径（/api/file/upload）
 * @param {string} filePath 本地文件路径
 * @returns {Promise<string>} 成功时 resolve 后端返回的相对路径
 */
function uploadFile(url, filePath) {
  return new Promise((resolve, reject) => {
    const app = getApp();
    const baseUrl = (app && app.globalData.baseUrl) || 'http://localhost:8080';
    const token = (app && app.globalData.token) || wx.getStorageSync('token') || '';
    wx.uploadFile({
      url: baseUrl + url,
      filePath: filePath,
      name: 'file',
      header: token ? { Authorization: 'Bearer ' + token } : {},
      success(res) {
        let body = {};
        try {
          body = JSON.parse(res.data);
        } catch (e) {
          wx.showToast({ title: '上传响应解析失败', icon: 'none' });
          reject(e);
          return;
        }
        if (body.code === 200) {
          resolve(body.data);
        } else {
          wx.showToast({ title: body.message || '上传失败', icon: 'none' });
          reject(body);
        }
      },
      fail(err) {
        wx.showToast({ title: '上传失败，请检查网络', icon: 'none' });
        reject(err);
      }
    });
  });
}

module.exports = { request, get, post, put, del, uploadFile };
