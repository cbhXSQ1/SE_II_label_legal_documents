# 后端功能层模块（`backend/`）

本目录为「中文裁判文书法律论证结构标注与教学支持系统」后端 Maven 工程，严格按照仓库 `接口文档/后端/功能层/总纲.md` 约定的包结构组织，包含功能层完整实现及对接数据层、规则层的公共契约接口。

## 目录与包结构（对齐 `总纲.md`）

```text
backend/
├─ pom.xml
├─ README.md
├─ 功能层内部设计与实现规范.md
└─ src/
   ├─ main/
   │  ├─ java/com/seii/legal/
   │  │  ├─ LegalAnnotationApplication.java
   │  │  ├─ function/
   │  │  │  ├─ controller/    # 11 个 REST Controller + GlobalExceptionHandler
   │  │  │  ├─ service/       # 11 个 AppService + 功能层内部支撑（模板/适配器/策略/事件）
   │  │  │  └─ dto/           # 11 个业务域 Request / Response DTO
   │  │  ├─ common/
   │  │  │  ├─ result/        # AppResult、业务/鉴权异常、TimeFormats
   │  │  │  ├─ page/          # PageResult
   │  │  │  └─ enums/         # LoginUser 及全部公共枚举
   │  │  ├─ data/
   │  │  │  ├─ entity/        # 46 个数据实体 record 契约
   │  │  │  └─ repository/    # 13 个数据层 Repository 接口契约（由数据层小组实现）
   │  │  └─ rule/
   │  │     ├─ identity/      # 规则层接口契约（由规则层小组实现）
   │  │     ├─ guide/
   │  │     ├─ task/
   │  │     ├─ annotation/
   │  │     ├─ adjudication/
   │  │     ├─ teaching/
   │  │     ├─ export/
   │  │     ├─ common/
   │  │     └─ notification/
   │  └─ resources/application.yml
   └─ test/java/com/seii/legal/function/service/   # 功能层单元测试
```

## 构建与测试

环境要求：JDK 21+、Maven 3.9+。

```bash
# 编译并运行单元测试
mvn -f backend/pom.xml test
```

## 详细设计说明

功能层内部设计模式、用例编排契约、异常处理与跨组联调注意事项见 [功能层内部设计与实现规范.md](功能层内部设计与实现规范.md)。
