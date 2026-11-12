import request from './request'

// ===================== 广告位接口 =====================

// 获取启用中的广告位（需登录，后端按 sort 升序返回）
export function getAds() {
  return request.get('/ads')
}
