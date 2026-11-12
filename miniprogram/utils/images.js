// 样板图片常量：图片随项目放在后端 uploads/samples 下，由后端静态映射提供，
// 避免使用外链（外链存在防盗链与域名白名单限制）
const SAMPLE_BASE = 'http://localhost:8080/api/uploads/samples';

const LOGIN_BG = SAMPLE_BASE + '/common/login-bg_1.jpg';
const EMPTY_IMAGE = SAMPLE_BASE + '/common/empty_1.jpg';
const WORKER_AVATAR = SAMPLE_BASE + '/common/worker_1.jpg';
const BANNER_IMAGES = [
  SAMPLE_BASE + '/common/banner-repair_1.jpg',
  SAMPLE_BASE + '/common/banner-vip_1.jpg',
  SAMPLE_BASE + '/common/banner-track_1.jpg'
];
const MINE_HEADER = SAMPLE_BASE + '/common/mine-header_1.jpg';

module.exports = {
  LOGIN_BG,
  EMPTY_IMAGE,
  WORKER_AVATAR,
  BANNER_IMAGES,
  MINE_HEADER
};
