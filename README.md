# 校园报修预约系统 — 项目说明书

## 一、项目简介

本系统是一套面向校园场景的**报修预约管理平台**，包含三个端：

| 端 | 技术栈 | 目录 |
|---|---|---|
| 后端服务 | SpringBoot 3 + MyBatis-Plus + MySQL 8 + JWT | `backend/` |
| 前端网页端 | Vue 3 + Vite + Element Plus + ECharts | `frontend/` |
| 微信小程序 | 原生小程序（WXML/WXSS/JS） | `miniprogram/` |

三端共用**同一套后端接口和同一个 MySQL 数据库**，同一账号在小程序和网页登录，身份权限完全一致。

### 角色与权限设计

系统支持三种角色，通过用户表 `role` 字段区分：

| 角色 | 权限说明 |
|---|---|
| 普通用户（USER） | 免费注册。单次报修最多上传 **1 张**故障图片；不能自选预约时间（系统默认派单）；不能标记紧急；仅可查看本人工单 |
| 会员用户（VIP） | 由管理员开通。单次最多上传 **5 张**图片；可自选预约上门时间；可添加**紧急标记**（工单优先排序）；可查看个人报修统计 |
| 维修管理员（ADMIN） | 全部工单管理（紧急工单置顶优先展示）、接单、修改工单状态、填写维修备注；故障分类动态维护（增删改）；用户管理（开通/取消会员） |

报修地点分两大类：**宿舍楼内**（需填楼号+房间号）与**校园公共区域**（需填具体位置）。故障分类由管理员在后台动态维护，系统预置宿舍楼 6 类、公共区域 6 类初始分类。

## 二、目录结构

```
campus-repair-system/
├── sql/
│   └── schema.sql          # 建库建表脚本（含初始分类与预置管理员数据）
├── docs/
│   └── api.md              # 前后端接口契约文档
├── backend/                # SpringBoot 后端
│   ├── src/main/java/com/campus/repair/
│   │   ├── controller/     # 接口层
│   │   ├── service/        # 业务层（含权限差异校验、状态机流转）
│   │   ├── mapper/         # MyBatis-Plus 数据访问层
│   │   ├── entity/ dto/ vo/# 实体、请求对象、返回对象
│   │   ├── config/         # 拦截器、跨域、静态资源配置
│   │   └── common/         # 统一返回体、业务异常
│   ├── src/test/           # JUnit5 + Mockito 单元测试
│   └── uploads/            # 故障图片本地存储目录（运行时自动创建）
├── frontend/               # Vue3 网页后台（用户端+管理端一体）
└── miniprogram/            # 微信小程序
```

## 三、本地运行步骤

### 1. 准备数据库

安装并启动 MySQL 8.0+，执行建表脚本。

**PowerShell（Windows 推荐，支持会提示输入密码）：**

```powershell
mysql --default-character-set=utf8mb4 -u root -p -e "source sql/schema.sql"
```

**CMD：**

```cmd
mysql --default-character-set=utf8mb4 -u root -p < sql/schema.sql
```

> 说明：PowerShell 不支持 `<` 重定向符号，直接输 `mysql -u root -p < sql/schema.sql` 会报「< 运算符是为将来使用而保留的」，请使用上面的 `source` 写法，或先切到 `cmd` 再执行。
>
> 必须带上 `--default-character-set=utf8mb4`，否则 Windows 客户端可能用 GBK/latin1 解析脚本中的中文，出现 `ERROR 1406 Data too long for column 'nickname'` 之类的报错。
>
> 若提示 `Access denied for user 'root'`，说明本机 MySQL 密码不是 `123456`：执行时按提示输入你自己的 root 密码即可，同时把 `backend/src/main/resources/application.yml` 中的 `password` 改成同一个值。

脚本会自动创建数据库 `campus_repair`、全部数据表、12 个预置故障分类，并预置一个管理员账号（手机号 `13800000000`）。

### 2. 启动后端（端口 8080）

数据库账号密码通过**环境变量**注入（配置项在 `backend/src/main/resources/application.yml`）：

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_HOST` | localhost | 数据库地址 |
| `MYSQL_PORT` | 3306 | 数据库端口 |
| `MYSQL_DB` | campus_repair | 数据库名 |
| `MYSQL_USERNAME` | root | 用户名 |
| `MYSQL_PASSWORD` | 123456 | 密码 |

本机密码不是 `123456` 时，启动前设置环境变量即可，**不用改代码**：

```powershell
# PowerShell（当前窗口有效）
$env:MYSQL_PASSWORD = "你的root密码"
cd backend
mvn spring-boot:run
```

```cmd
:: CMD（当前窗口有效）
set MYSQL_PASSWORD=你的root密码
cd backend
mvn spring-boot:run
```

> IDEA 中运行：`Run → Edit Configurations → Environment variables` 填 `MYSQL_PASSWORD=你的root密码`；永久生效可用 `setx MYSQL_PASSWORD "你的密码"`（新开窗口后生效）。

> **课程演示说明**：本系统采用**模拟短信验证码**，不接入真实短信服务。点击「获取验证码」后，验证码会打印在后端控制台日志中（`模拟发送短信验证码: 手机号=xxx, 验证码=xxx`），直接查看控制台即可登录。

### 3. 启动 Vue 网页后台（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 http://localhost:5173 ，手机号 + 验证码登录。用预置管理员手机号 `13800000000` 登录即进入管理后台视图。

### 4. 运行微信小程序

用「微信开发者工具」导入 `miniprogram/` 目录（测试号即可），**勾选「详情 → 本地设置 → 不校验合法域名」**后即可在模拟器中运行。

> 真机预览时，把 `miniprogram/app.js` 中的 `baseUrl` 改为电脑的局域网 IP（如 `http://192.168.x.x:8080`），手机与电脑需在同一 WiFi 下。

## 四、核心功能清单

### 微信小程序端
- 首页：品牌渐变头部、轮播提示、快捷入口、报修流程、最近工单与会员权益引导
- 手机号注册、验证码登录
- 普通用户：提交报修（限 1 张图、无预约时间）、查看本人工单（待接单/维修中/已完成/驳回）、查看故障图片
- 会员用户：最多 5 张图、自选预约上门时间、紧急标记、个人报修统计
- 管理员：工单管理（紧急优先、按状态/地点/故障类型筛选、看图）、接单/改状态/填备注、故障分类管理、用户会员管理

### Vue 网页后台
- 同一登录入口，按角色渲染不同菜单
- 工作台首页：欢迎横幅、工单状态统计磁贴、最近工单与快捷入口
- 普通用户：提交报修（遵循免费版限制）、查看本人工单与故障照片
- 会员：会员全部权限 + ECharts 报修统计图表
- 管理员：用户管理（开通/取消会员）、工单管理（分页查询、接单、状态流转、维修备注、图片预览、紧急优先）、故障分类增删改

## 五、关键设计说明

1. **权限互通**：小程序与网页共用 `/api/auth/login` 接口，登录签发 JWT（有效期 7 天），后端拦截器统一鉴权，角色判定全部在服务端完成，前端仅做菜单展示控制，保证越权操作被后端拦截。
2. **会员差异控制**：提交工单时后端按角色强制校验——普通用户传多张图、传预约时间、传紧急标记均被拒绝或强制重置，防止绕过前端。
3. **工单状态机**：`待接单 → 维修中 → 已完成`，中间可流转为`驳回`，终态不可逆，非法流转返回业务异常。
4. **紧急工单排序**：管理员工单列表按 `urgent DESC, create_time DESC` 排序，紧急工单始终优先展示。
5. **故障分类动态化**：分类存于数据库，管理员可随时新增/编辑/删除；已被工单引用的分类执行逻辑删除（停用），保证历史工单数据完整性。
6. **图片存储**：采用课程作业简易方案——图片上传到后端本地 `uploads/` 目录，通过静态资源映射访问，数据库只存相对路径。
7. **安全细节**：接口参数用 Bean Validation 校验；SQL 全部走 MyBatis-Plus 参数绑定，无注入风险；统一异常处理返回友好中文提示。
8. **界面设计**：网页端与小程序统一采用一套设计语言——品牌渐变（`#2c6bf2 → #17bfd4`）、深色侧边栏、大圆角卡片、胶囊状态标签与柔和投影；状态色固定为待接单琥珀 `#f5a524`、维修中品牌蓝 `#2c6bf2`、已完成薄荷绿 `#0fb583`、驳回红 `#ef4b4b`。网页端全局样式集中在 `frontend/src/styles/theme.css`，小程序全局样式集中在 `miniprogram/app.wxss`。

## 六、测试

后端核心业务（会员/普通用户权限差异、工单状态机、分类逻辑删除）配有 JUnit 5 + Mockito 单元测试：

```bash
cd backend
mvn test
```

---

**课程作业信息**：本系统为《软件工程/移动应用开发》课程设计作品，代码含完整中文注释，可直接本地运行演示。
