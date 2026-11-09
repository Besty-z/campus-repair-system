# 校园报修预约系统 — 前后端接口契约

- 后端基础地址：`http://localhost:8080/api`
- 图片等静态资源：`http://localhost:8080` + 接口返回的相对路径（如 `/uploads/2026/11/xxx.jpg`）
- 认证方式：请求头 `Authorization: Bearer <token>`（JWT，登录接口签发，有效期 7 天）
- 统一响应体：`{ "code": 200, "message": "success", "data": ... }`
  - `code=200` 成功；`401` 未登录/登录过期；`403` 无权限；`400` 参数/业务校验失败；`500` 服务器异常
  - 失败时 `message` 为可直接提示给用户的中文文案

## 角色与权限规则

| 角色 role | 说明 | 关键限制 |
|---|---|---|
| USER | 普通免费用户 | 单次最多上传 1 张图片；不能自选预约时间；不能标记紧急；只能看自己的工单 |
| VIP | 会员用户 | 最多 5 张图片；可自选预约上门时间；可标记紧急（urgent=1，列表优先排序）；可查看个人报修统计 |
| ADMIN | 维修管理员 | 全部工单管理、接单/改状态/填备注；故障分类增删改；用户管理（设置/取消会员） |

- 注册即 USER；会员身份由 ADMIN 在用户管理中开通。
- 同一账号小程序端和网页端通用，权限一致。

## 通用枚举

- 地点类型 locationType：`DORM`（宿舍楼内）/ `PUBLIC`（校园公共区域）
- 工单状态 status：`PENDING`（待接单）/ `IN_PROGRESS`（维修中）/ `FINISHED`（已完成）/ `REJECTED`（驳回）

## 1. 认证接口 `/auth`（无需 token）

### POST `/auth/send-code` 发送登录验证码
请求：`{ "phone": "13800000000" }`
响应 data：`{ "expireSeconds": 300 }`
> 课程作业：不发真实短信，验证码打印在后端控制台日志中（`模拟发送短信验证码`），有效期 5 分钟。

### POST `/auth/register` 手机号注册
请求：`{ "phone": "13912345678", "code": "123456", "nickname": "张三" }`
逻辑：校验验证码 → 手机号已存在则报错 → 创建 role=USER 账号 → 直接返回登录态
响应 data：同登录接口

### POST `/auth/login` 验证码登录
请求：`{ "phone": "13800000000", "code": "123456" }`
响应 data：
```json
{
  "token": "eyJhbGciOi...",
  "user": { "id": 1, "phone": "13800000000", "nickname": "维修管理员", "role": "ADMIN" }
}
```
> 手机号未注册时自动按普通用户创建账号并登录（小程序一键体验）；已禁用账号返回 403。

### GET `/auth/profile` 获取当前登录用户信息（需 token）
响应 data：`{ "id": 1, "phone": "...", "nickname": "...", "role": "VIP" }`

## 2. 文件上传（需 token）

### POST `/file/upload`
Content-Type: `multipart/form-data`，字段名 `file`
限制：jpg/jpeg/png/gif，≤ 5MB；保存到后端本地 `uploads/` 目录
响应 data：`"/uploads/2026/11/08/uuid.jpg"`（相对路径，前端拼接 `http://localhost:8080` 使用）

## 3. 故障分类 `/categories`

### GET `/categories` 查询分类列表（需 token，所有角色可用）
参数：`locationType`（可选，DORM/PUBLIC）
响应 data：`[ { "id": 1, "name": "水电故障", "locationType": "DORM", "sort": 1 } ]`（仅返回启用状态，按 sort 升序）

### GET `/categories/admin/all` 全量分类含停用（仅 ADMIN）
响应 data：`[ { "id": 1, "name": "...", "locationType": "DORM", "sort": 1, "status": 1 } ]`

### POST `/categories` 新增分类（仅 ADMIN）
请求：`{ "name": "网络故障", "locationType": "DORM", "sort": 7 }`

### PUT `/categories/{id}` 修改分类（仅 ADMIN）
请求：`{ "name": "网络与宽带", "locationType": "DORM", "sort": 7, "status": 1 }`

### DELETE `/categories/{id}` 删除分类（仅 ADMIN）
> 若分类下已存在工单，执行逻辑删除（status=0）；否则物理删除。响应 message 说明实际行为。

## 4. 报修工单 `/orders`（需 token）

### POST `/orders` 提交报修工单（USER / VIP）
请求：
```json
{
  "locationType": "DORM",
  "buildingNo": "5号楼",
  "roomNo": "302",
  "publicLocation": null,
  "categoryId": 1,
  "description": "宿舍水龙头漏水",
  "images": ["/uploads/2026/11/08/a.jpg"],
  "urgent": 0,
  "appointmentTime": null
}
```
规则：
- `locationType=DORM` 时 `buildingNo`、`roomNo` 必填；`PUBLIC` 时 `publicLocation` 必填
- USER：`images` 最多 1 张；`urgent` 强制 0；`appointmentTime` 强制 null
- VIP：`images` 最多 5 张；`appointmentTime` 格式 `yyyy-MM-dd HH:mm:ss`，必须是将来的时间；`urgent` 可为 1
- 响应 data：创建成功的工单对象（同 GET 详情结构）

### GET `/orders/my` 我的工单列表（USER / VIP）
参数：`status`（可选）、`pageNum`（默认1）、`pageSize`（默认10）
响应 data：分页对象（见下文分页结构），按创建时间倒序

### GET `/orders/my/stats` 我的报修统计（仅 VIP）
响应 data：
```json
{
  "total": 12, "pending": 2, "inProgress": 1, "finished": 8, "rejected": 1,
  "byCategory": [ { "categoryName": "水电故障", "count": 4 } ],
  "byMonth": [ { "month": "2026-10", "count": 5 } ]
}
```

### GET `/orders/admin/page` 工单分页查询（仅 ADMIN）
参数：`status`、`locationType`、`categoryId`、`keyword`（模糊匹配位置/描述/工单号）、`pageNum`、`pageSize`
排序：**紧急工单优先（urgent 倒序），再按创建时间倒序**

### PUT `/orders/admin/{id}/accept` 接单（仅 ADMIN）
PENDING → IN_PROGRESS，记录接单人与接单时间

### PUT `/orders/admin/{id}/status` 修改工单状态（仅 ADMIN）
请求：`{ "status": "FINISHED", "adminRemark": "已更换水龙头，测试无渗漏" }`
允许流转：PENDING→IN_PROGRESS / REJECTED；IN_PROGRESS→FINISHED / REJECTED；终态不可再改

### GET `/orders/{id}` 工单详情（USER/VIP 仅本人，ADMIN 任意）
响应 data：
```json
{
  "id": 1, "orderNo": "R202611080001",
  "userId": 2, "userPhone": "13912345678", "userNickname": "张三",
  "locationType": "DORM", "buildingNo": "5号楼", "roomNo": "302", "publicLocation": null,
  "categoryId": 1, "categoryName": "水电故障",
  "description": "宿舍水龙头漏水",
  "images": ["/uploads/2026/11/08/a.jpg"],
  "urgent": 0, "appointmentTime": null,
  "status": "PENDING", "adminRemark": null,
  "createTime": "2026-11-08 10:00:00", "acceptTime": null, "finishTime": null
}
```

## 5. 用户管理 `/admin/users`（仅 ADMIN）

### GET `/admin/users/page` 用户分页
参数：`keyword`（手机号/昵称模糊）、`role`（可选）、`pageNum`、`pageSize`
响应 data：分页对象，记录含 `id, phone, nickname, role, status, createTime`

### PUT `/admin/users/{id}/role` 设置用户身份
请求：`{ "role": "VIP" }`（可选值 USER / VIP；管理员角色不允许通过此接口变更）

## 分页对象结构（MyBatis-Plus 风格）

```json
{ "records": [ ... ], "total": 100, "current": 1, "size": 10, "pages": 10 }
```

## 日期格式

统一字符串 `yyyy-MM-dd HH:mm:ss`（后端 Jackson 全局配置）。
