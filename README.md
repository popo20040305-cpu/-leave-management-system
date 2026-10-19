# 学生请假管理系统（Spring Boot + Vue 3）

一套前后端分离的学生请假审批系统，覆盖「登录 → 请假申请 → 三级审批 → 销假归档」完整业务闭环，
后端提供 48 个 RESTful 接口，前端基于 Vue 3 + TypeScript + Vite 构建。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot、MyBatis-Plus、Apache Shiro、Druid、MySQL 8、Thymeleaf（部分后台页面） |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Axios、Pinia |
| 数据库 | MySQL 8（`qingjia/DB/qingjia.sql` 可直接导入） |

## 功能模块

- **认证与权限**：Shiro + Bearer Token 登录鉴权，RBAC 动态菜单与按钮级权限控制
- **请假流程**：学生提交 → 班主任审批 → 教师审批 → 院系审批 → 销假归档，支持附件（病假证明）上传
- **角色管理**：学生 / 班主任 / 教师 / 管理员四类角色，菜单按角色动态渲染
- **基础数据**：班级、院系、用户、请假类型维护
- **数据看板**：请假统计与审批进度概览

## 目录结构

```
.
├── qingjia/                 # 后端 Spring Boot 工程
│   ├── src/main/java/       # 控制层 / 服务层 / 数据访问层
│   ├── src/main/resources/
│   │   ├── mapper/          # MyBatis XML
│   │   └── application.yml.example   # 配置模板（复制为 application.yml 后填写密码）
│   └── DB/qingjia.sql       # 建库脚本与初始数据
└── qingjia_web/             # 前端 Vue 3 + TS 工程
    ├── src/views/           # 页面（含 leave 请假模块）
    └── src/api/             # 接口封装
```

## 本地启动

### 1. 准备数据库

```bash
mysql -uroot -p < qingjia/DB/qingjia.sql
```

### 2. 启动后端

```bash
cd qingjia
cp src/main/resources/application.yml.example src/main/resources/application.yml
# 编辑 application.yml，把 spring.datasource.password 改成本机 MySQL 密码
mvn spring-boot:run
```

后端地址：`http://localhost:8089/qingjia`

### 3. 启动前端

```bash
cd qingjia_web
npm install
npm run dev
```

## 说明

- 仓库不包含 `application.yml`（含本机数据库密码），请从 `application.yml.example` 复制后自行填写。
- 上传的图片、运行日志不纳入版本管理。
