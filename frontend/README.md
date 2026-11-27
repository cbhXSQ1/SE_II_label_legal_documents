# 法律论证标注与教学支持系统 · 前端

Vue 3 前端，与后端（Spring Boot）通过统一 `AppResult` 契约对接；内置 Mock 数据层，后端未就绪也能独立跑通整条流程。

## 技术栈

- Vue 3（组合式 API `<script setup>`）+ Vite
- Vue Router 4（路由 + 守卫）、Pinia 2（状态）、Axios（请求）
- Element Plus（UI 组件）

## 目录结构与 MVC 分层

```
src/
├─ api/          M（数据访问层）：request.js 统一解包 AppResult，index.js 按模块组织接口
├─ store/        M（状态层）：auth.js 当前账号/身份会话
├─ mock/         Mock 层：adapter.js 拦截 axios、db.js 种子数据、handlers.js 路由表
├─ router/       C（控制层）：路由定义 + 登录/身份守卫
├─ layouts/      V（布局）
├─ views/        V（页面，按模块分目录：login/select/home/annotate/admin/teacher/student/…）
├─ components/   V（复用组件，如 ArgumentDiagram）
└─ utils/        dict.js 领域字典、nav.js 导航、format.js 格式化、session.js 会话
```

对应关系：**M** = `api/` + `store/`，**V** = `views/` + `components/` + `layouts/`，**C** = `router/` + store 里的 action。

## 运行

```bash
cd frontend
npm install          # 国内建议：npm install --registry=https://registry.npmmirror.com
npm run dev          # http://localhost:5173
```

## 演示账号（密码均为 123456）

| 账号 | 姓名 | 身份 |
| --- | --- | --- |
| `admin` | 李管理 | 管理员 + 教学系统管理员（多身份，登录后进选择页） |
| `qian` | 钱任务 | 任务管理者 |
| `liu` | 刘标注 | 标注员（有 T1/D1 完整标注样例） |
| `chen` | 陈标注 | 标注员 |
| `zhangsan` | 张三 | 学生 + 标注员（多身份） |
| `wang` | 王仲裁 | 仲裁员 |
| `sun` | 孙专家 | 领域专家 |
| `zhao` | 赵老师 | 教师 |

## Mock 开关

`.env` 里 `VITE_USE_MOCK=true` 使用内置 Mock；后端就绪后改为 `false`，请求走 `/app` 代理到 `http://localhost:8080`（见 `vite.config.js`）。

## 与后端契约

- 统一响应 `{ success, message, data, allowedActions }`，`api/request.js` 已解包，`success=false` 时自动 `ElMessage.error` 并 reject。
- 请求头带 `Authorization`（登录凭证）。
- 分页 `{ records, total, page, size }`；百分比保留一位小数。

## 当前进度（本仓库首版）

- ✅ 工程脚手架 + 路由（全部模块/页面已声明）+ 登录/选择身份 + 布局
- ✅ 标注工作台（三栏：原文+要素列表 / 切分·要素·关系·图示 / 论证图示 SVG 拖动缩放）
- ✅ 任务列表（首页按身份展示）
- ⏳ 其余模块（管理后台、裁定、教师、学生、教学管理员等）路由已建，页面待实现（`PagePlaceholder`）

## 后续实现顺序建议

1. 任务管理（文书上传、创建/发布任务、任务进度、导出）
2. 查看与裁定（比对、裁定冲突、图示审查、最终版本）
3. 教师 / 学生 / 教学系统管理员（教学侧）
4. 管理后台（账号审批、角色、指南导入、总体情况）
