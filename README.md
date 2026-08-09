# 彩虹金刚

彩虹金刚是一个 DND 资料库项目，按目录拆分为:

- `XRLin`: Vue3 + Vite 前端，负责搜索、筛选、列表和详情展示。
- `LinXR`: Spring Boot 后端，负责提供职业、种族、1-9 环法术、怪物等资料 API。

## 启动后端

```powershell
cd D:\Web-code\LXR\LinXR
mvn spring-boot:run
```

后端默认地址:

```text
http://localhost:8080
```

## 启动前端

首次运行先安装依赖:

```powershell
cd D:\Web-code\LXR\XRLin
npm install
```

启动 Vue3 开发服务器:

```powershell
npm run dev
```

前端默认地址:

```text
http://localhost:5173
```

前端默认请求:

```text
http://localhost:8080/api/codex
```

## API

- `GET /api/codex/categories`: 分类统计。
- `GET /api/codex/entries`: 查询全部条目。
- `GET /api/codex/entries?category=spell&q=火球`: 按分类和关键词查询。
- `GET /api/codex/entries/{id}`: 查询单个条目。

## 内容维护

内置资料在:

```text
LinXR\src\main\java\com\jjwpp\linxr\codex\CodexSeed.java
```

现在的内容是 SRD 风格的中文摘要种子库，结构已经覆盖职业、种族、1-9 环法术、怪物。后续可以继续往 `CodexSeed` 添加完整条目，或迁移到数据库与后台管理页面。
