// AI 智能报修助手：会话管理与对话
const { get, post, del } = require('../../utils/request');

// API Key 本地存储键
const AI_KEY_STORAGE_KEY = 'ai_deepseek_key';

Page({
  data: {
    sessions: [],            // 会话列表
    currentSessionId: null,  // 当前会话ID
    messages: [],            // 消息列表
    inputText: '',           // 输入内容
    thinking: false,         // AI 思考中
    showSessionPanel: false, // 会话选择面板
    scrollToView: '',        // 滚动定位
    // API Key 配置
    deepseekKey: '',         // 用户配置的 API Key
    hasApiKey: false,        // 是否已配置 Key
    showKeySetting: false,   // Key 设置弹窗
    keyInput: ''             // 弹窗输入框内容
  },

  onLoad() {
    const key = wx.getStorageSync(AI_KEY_STORAGE_KEY) || '';
    this.setData({ deepseekKey: key, hasApiKey: !!key });
    this.loadSessions();
  },

  onShow() {
    // 每次显示刷新会话列表（可能从其他页面回来）
    this.loadSessions();
  },

  /** 加载会话列表 */
  loadSessions() {
    get('/api/ai/chat/sessions')
      .then((list) => {
        const sessions = list || [];
        this.setData({ sessions });
        // 若无当前会话且存在会话，默认选中第一个
        if (!this.data.currentSessionId && sessions.length > 0) {
          this.selectSession(sessions[0].id);
        }
      })
      .catch(() => {
        this.setData({ sessions: [] });
      });
  },

  /** 选择会话并加载消息 */
  selectSession(id) {
    if (this.data.currentSessionId === id) {
      this.setData({ showSessionPanel: false });
      return;
    }
    this.setData({ currentSessionId: id, messages: [], showSessionPanel: false });
    this.loadMessages(id);
  },

  /** 加载某会话消息 */
  loadMessages(sessionId) {
    get('/api/ai/chat/sessions/' + sessionId + '/messages')
      .then((list) => {
        this.setData({ messages: list || [] });
        this.scrollToBottom();
      })
      .catch(() => {
        this.setData({ messages: [] });
      });
  },

  /** 新建会话 */
  handleNewSession() {
    post('/api/ai/chat/sessions')
      .then((session) => {
        const sessions = [session].concat(this.data.sessions);
        this.setData({
          sessions,
          currentSessionId: session.id,
          messages: [],
          showSessionPanel: false
        });
      })
      .catch(() => {
        wx.showToast({ title: '创建失败', icon: 'none' });
      });
  },

  /** 删除会话 */
  handleDeleteSession(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '提示',
      content: '确定删除该会话吗？',
      success: (res) => {
        if (!res.confirm) return;
        del('/api/ai/chat/sessions/' + id)
          .then(() => {
            const sessions = this.data.sessions.filter((s) => s.id !== id);
            const currentId = this.data.currentSessionId === id ? null : this.data.currentSessionId;
            this.setData({
              sessions,
              currentSessionId: currentId,
              messages: currentId ? this.data.messages : []
            });
            if (!currentId && sessions.length > 0) {
              this.selectSession(sessions[0].id);
            }
            wx.showToast({ title: '已删除', icon: 'success' });
          })
          .catch(() => {
            wx.showToast({ title: '删除失败', icon: 'none' });
          });
      }
    });
  },

  /** 快捷提问 */
  handleQuickPrompt(e) {
    const prompt = e.currentTarget.dataset.prompt;
    this.setData({ inputText: prompt });
    this.handleSend();
  },

  /** 输入框变化 */
  onInput(e) {
    this.setData({ inputText: e.detail.value });
  },

  /** 发送消息 */
  handleSend() {
    const prompt = this.data.inputText.trim();
    if (!prompt || this.data.thinking) return;

    let sessionId = this.data.currentSessionId;
    const doSend = (sid) => {
      // 插入用户消息
      const userMsg = {
        id: 'u-' + Date.now(),
        role: 'user',
        content: prompt
      };
      const messages = this.data.messages.concat([userMsg]);
      this.setData({
        messages,
        inputText: '',
        thinking: true,
        currentSessionId: sid
      });
      this.scrollToBottom();

      const reqBody = { sessionId: sid, prompt };
      if (this.data.deepseekKey) {
        reqBody.apiKey = this.data.deepseekKey;
      }
      post('/api/ai/chat/messages', reqBody)
        .then((aiMsg) => {
          const list = this.data.messages.concat([aiMsg]);
          // 更新会话标题
          const sessions = this.data.sessions.map((s) => {
            if (s.id === sid && s.title === '新会话') {
              return Object.assign({}, s, {
                title: prompt.length > 20 ? prompt.slice(0, 20) + '…' : prompt
              });
            }
            return s;
          });
          this.setData({ messages: list, sessions, thinking: false });
          this.scrollToBottom();
        })
        .catch(() => {
          const errMsg = {
            id: 'e-' + Date.now(),
            role: 'assistant',
            content: '抱歉，发送失败，请稍后重试。'
          };
          this.setData({
            messages: this.data.messages.concat([errMsg]),
            thinking: false
          });
          this.scrollToBottom();
        });
    };

    // 若无当前会话，先创建
    if (!sessionId) {
      post('/api/ai/chat/sessions', {})
        .then((session) => {
          const sessions = [session].concat(this.data.sessions);
          this.setData({ sessions });
          doSend(session.id);
        })
        .catch(() => {
          wx.showToast({ title: '创建会话失败', icon: 'none' });
        });
    } else {
      doSend(sessionId);
    }
  },

  /** 滚动到底部 */
  scrollToBottom() {
    const messages = this.data.messages;
    if (messages.length === 0) return;
    const last = messages[messages.length - 1];
    this.setData({ scrollToView: 'msg-' + last.id });
  },

  /** 切换会话面板显示 */
  toggleSessionPanel() {
    this.setData({ showSessionPanel: !this.data.showSessionPanel });
  },

  /** 打开 API Key 设置弹窗 */
  openKeySetting() {
    this.setData({ showKeySetting: true, keyInput: this.data.deepseekKey });
  },

  /** 关闭 API Key 设置弹窗 */
  closeKeySetting() {
    this.setData({ showKeySetting: false });
  },

  /** Key 输入框变化 */
  onKeyInput(e) {
    this.setData({ keyInput: e.detail.value });
  },

  /** 保存 API Key 到本地存储 */
  saveKey() {
    const key = (this.data.keyInput || '').trim();
    this.setData({
      deepseekKey: key,
      hasApiKey: !!key,
      showKeySetting: false
    });
    if (key) {
      wx.setStorageSync(AI_KEY_STORAGE_KEY, key);
      wx.showToast({ title: 'Key 已保存', icon: 'success' });
    } else {
      wx.removeStorageSync(AI_KEY_STORAGE_KEY);
      wx.showToast({ title: '已清除 Key，使用模拟建议', icon: 'none' });
    }
  },

  /** 清除 API Key */
  clearKey() {
    this.setData({
      deepseekKey: '',
      keyInput: '',
      hasApiKey: false
    });
    wx.removeStorageSync(AI_KEY_STORAGE_KEY);
    wx.showToast({ title: '已清除 Key', icon: 'none' });
  }
});
