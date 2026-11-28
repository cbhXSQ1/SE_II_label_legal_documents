# 后端契约模块拆分设计

## 背景

后端目前只在 `feature/data-round-submission` 分支，`main` 只有前端和接口文档。功能层如果要开始写，必须先把被引用的实体、Repository 接口、枚举和规则接口变成可编译的模块，否则 `mvn compile` 找不到类型。数据层、规则层、功能层需要并行推进，不能靠互相等待。

## 目标

- 把后端拆成 7 个 Maven 模块，契约、数据层、规则层、功能层、启动装配各自独立编译。
- 依赖单向，不允许循环；功能层编译时不依赖 MySQL 代码、规则具体类和启动模块。
- 包名保持不变，只移动目录，减少现有 import 改动。
- 功能层可以先用契约和假对象写服务、控制器和测试，数据层与规则层后补。
- 不改页面接口、数据层 SQL、表结构和业务规则。

## 模块与职责

```text
backend/
  pom.xml                    父 POM，packaging=pom
  legal-common/              AppResult、PageResult、enums、业务异常
  legal-data-api/            data.entity、data.repository、DataConflictException
  legal-data-mysql/          data.mysql 的 Jdbc 类、SQL、迁移脚本
  legal-rule-api/            rule.identity、task、annotation、adjudication、
                             export、teaching、notification 的接口和 DTO
  legal-rule-impl/           DefaultIdentityRule、DefaultTaskRule 等规则具体类
  legal-function/            function.controller、service、dto、support、event
  legal-app/                 启动类、application.yml、集成装配
```

职责边界：

| 模块 | 放什么 | 不放什么 |
| --- | --- | --- |
| `legal-common` | `AppResult`、`PageResult`、`DataScope`、`IdentityType`、`LoginUser`、`TaskStatus`、`RoundType`、`SubmissionStatus`、`ApplicationStatus` 等共用类型和业务异常 | Repository、实体、规则、Spring 业务服务 |
| `legal-data-api` | `Account`、`AnnotationTask`、`Submission`、`TaskRound` 等实体，13 个 Repository 接口，`DataConflictException` | Jdbc、SQL、MySQL 依赖、业务服务 |
| `legal-data-mysql` | `JdbcTaskRepository`、`JdbcAnnotationRepository` 等，`db/mysql` 脚本 | 功能层服务、规则接口 |
| `legal-rule-api` | `IdentityRule`、`TaskRule`、`SegmentationRule`、`AdjudicationRule`、`TeachingRule`、`NotificationService` 等接口和签名用到的 DTO | `Default*Rule`、Spring 服务、Jdbc |
| `legal-rule-impl` | `com.seii.legal.rule.impl.*` 下的规则具体类 | 功能层服务、控制器、Jdbc |
| `legal-function` | `function.controller`、`service`、`dto`、`support`、`event` | 启动类、Jdbc 具体类、`Default*Rule`、`application.yml` |
| `legal-app` | `com.seii.legal.app.LegalApplication`、`application.yml`、运行期装配 | 业务编排、规则具体类、Jdbc 具体类 |

## 依赖规则

```text
legal-common
    ^
    |
legal-data-api <--- legal-data-mysql
    ^
    |
legal-rule-api <--- legal-rule-impl
    ^
    |
legal-function
    ^
    |
legal-app -. runtime .-> legal-data-mysql
          -. runtime .-> legal-rule-impl
```

- `legal-common` 不依赖其他业务模块。
- `legal-data-api` 只依赖 `legal-common`。
- `legal-data-mysql` 依赖 `legal-data-api` 和 `legal-common`，不依赖规则层和功能层。
- `legal-rule-api` 依赖 `legal-common`，签名需要实体时依赖 `legal-data-api`；不依赖 MySQL 和功能层。
- `legal-rule-impl` 依赖 `legal-rule-api`、`legal-data-api`、`legal-common`，不依赖功能层。
- `legal-function` 编译依赖 `legal-common`、`legal-data-api`、`legal-rule-api`；不依赖 `legal-data-mysql`、`legal-rule-impl`、`legal-app`。
- `legal-app` 依赖 `legal-function`，并以 `runtime` 范围依赖 `legal-data-mysql`、`legal-rule-impl`；其他模块都不依赖 `legal-app`。
- 任何模块都不允许反向依赖 `legal-function` 或 `legal-app`；出现反向依赖就说明边界错了。

## 包结构

```text
com.seii.legal.common.result
com.seii.legal.common.page
com.seii.legal.common.enums
com.seii.legal.common.exception

com.seii.legal.data.entity
com.seii.legal.data.repository
com.seii.legal.data.mysql

com.seii.legal.rule.identity
com.seii.legal.rule.task
com.seii.legal.rule.annotation
com.seii.legal.rule.adjudication
com.seii.legal.rule.export
com.seii.legal.rule.teaching
com.seii.legal.rule.notification
com.seii.legal.rule.common
com.seii.legal.rule.impl

com.seii.legal.function.controller
com.seii.legal.function.service
com.seii.legal.function.dto
com.seii.legal.function.support
com.seii.legal.function.event

com.seii.legal.app
```

规则接口留在 `com.seii.legal.rule.*`，规则具体类统一放 `com.seii.legal.rule.impl.*`。不把一个包拆到两个模块里，例如 `DefaultTaskRule` 不放 `com.seii.legal.rule.task`，避免模块边界和包边界打架。

## 并行开发边界

- 数据层改 `legal-data-api` 的契约时先提接口变更，再改 `legal-data-mysql`；数据层测试只依赖 `legal-data-api`，不编译规则层和功能层。
- 规则层只改 `legal-rule-api` 和 `legal-rule-impl`；规则单测直接构造实体和 DTO，不启动 Spring，不依赖 MySQL。
- 功能层只改 `legal-function`；服务通过构造器注入 Repository 和规则接口，测试用假 Repository、假规则，不把 `legal-data-mysql`、`legal-rule-impl`、`legal-app` 放进编译类路径。
- 启动装配只改 `legal-app`；它只负责 Spring Boot 启动、数据源配置和运行期依赖，不写业务。
- 跨模块契约变更由提出方先改 `legal-data-api` 或 `legal-rule-api`，再通知使用方；CI 用 `mvn -pl ... -am compile` 确认下游还能编译。
- 前端不受影响，页面接口字段和返回结构不变。

## 启动与装配

- `LegalApplication` 放 `com.seii.legal.app`，标注 `@SpringBootApplication(scanBasePackages = "com.seii.legal")`，这样 `com.seii.legal.rule.impl` 和 `com.seii.legal.data.mysql` 在运行期进入组件容器。
- `legal-app/pom.xml` 对 `legal-data-mysql`、`legal-rule-impl` 使用 `runtime` 依赖；编译期看不到具体类，运行期由 Spring 注入接口。
- 数据源配置和 `application.yml` 放 `legal-app/src/main/resources`；建表脚本放 `legal-data-mysql/src/main/resources/db/mysql`。
- 规则具体类用 `@Component` 或 `@Service`，Repository 具体类用 `@Repository`，功能层服务只依赖接口。

## 构建与验证

- 父 POM 统一 `groupId=com.seii.legal`，`version=0.1.0-SNAPSHOT`，Java 17，Spring Boot 3.5.7。
- 功能层独立编译：

```powershell
mvn -q -pl legal-function -am compile
```

该命令只拉起 `legal-common`、`legal-data-api`、`legal-rule-api`、`legal-function`，不编译 `legal-data-mysql`、`legal-rule-impl`、`legal-app`。

- 数据层独立测试：

```powershell
mvn -q -pl legal-data-mysql -am test
```

- 规则层独立测试：

```powershell
mvn -q -pl legal-rule-impl -am test
```

- 全量构建：

```powershell
mvn -q test
```

- 启动装配构建：

```powershell
mvn -q -pl legal-app -am package
```

- 依赖检查：`mvn -q -pl legal-function dependency:tree` 里不能出现 `legal-data-mysql`、`legal-rule-impl`、`legal-app`；`mvn -q -pl legal-data-api dependency:tree` 里不能出现规则层和功能层。
- 数据层集成测试继续按 `LEGAL_TEST_JDBC_URL`、`LEGAL_TEST_DB_USER`、`LEGAL_TEST_DB_PASSWORD` 决定是否运行；未配置时跳过，不阻塞功能层和规则层。

## 迁移步骤

1. 从 `origin/feature/data-round-submission` 建 `feature/backend-modules`，把 `main` 的接口文档合入，保证文档和代码在同一分支。
2. 新建父 POM，把原来的 `legal-data` 拆成 `legal-data-api` 和 `legal-data-mysql`。
3. 把 `common/enums` 移到 `legal-common`；`data/entity`、`data/repository`、`DataConflictException` 移到 `legal-data-api`；`data/mysql` 和 `db/mysql` 移到 `legal-data-mysql`。
4. 新建 `legal-rule-api`、`legal-rule-impl`、`legal-function`、`legal-app` 四个模块和 POM；规则接口进 `legal-rule-api`，具体类进 `legal-rule-impl`，功能层代码进 `legal-function`，启动类进 `legal-app`。
5. 加 `LegalApplication` 和 `application.yml`，把 `legal-data-mysql`、`legal-rule-impl` 设为 `runtime` 依赖。
6. 跑功能层独立编译、数据层测试、规则层测试、启动装配构建和全量构建；全绿后合入 `main`。

## 风险与应对

- 包移动导致 import 断裂：包名保持不变，只改模块目录；用 `git mv` 保留历史。
- Spring 扫描不到规则和数据层：启动类显式 `scanBasePackages = "com.seii.legal"`，并检查 `@Component` 注解。
- 契约变更连锁编译：契约变更先合 `legal-data-api` 或 `legal-rule-api`，下游用 `-am` 验证；必要时在 PR 里列出受影响模块。
- 规则接口依赖数据实体形成耦合：只允许 `rule-api -> data-api` 单向；如果后续规则签名继续膨胀，再在 `rule-api` 里引入规则自己的 DTO，不回头改数据层。
- 测试数据库不可用：数据层集成测试跳过，功能层和规则层用假对象覆盖主要流程。

## 非目标

- 不改页面接口的请求字段、返回结构和错误码。
- 不改数据层表结构、SQL 语义和现有 Repository 方法。
- 不引入微服务、消息队列、独立部署、Maven 私服发布。

## 验收标准

- `mvn -q -pl legal-function -am compile` 通过，且编译类路径没有 `legal-data-mysql`、`legal-rule-impl`、`legal-app`。
- `mvn -q -pl legal-data-mysql -am test`、`mvn -q -pl legal-rule-impl -am test` 分别通过。
- `mvn -q -pl legal-app -am package` 通过，`mvn -q test` 全量通过；没有循环依赖。
- 启动 `LegalApplication` 能注入 Repository 和规则接口，页面控制器可以开始接服务。
- 数据层、规则层、功能层可以各自开分支并行改，不需要等另外两层完成。
