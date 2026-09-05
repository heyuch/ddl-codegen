# springboot-integration-sample

> 变更号：20260906-03　变更类型：chore（测试/示例工程；不改 core/cli/plugin 生产代码与既有契约）　目录：`docs/changes/20260906-03-chore-springboot-integration-sample/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

20260906-02 用 golden 契约测试验证「生成产物 = 预期文本」，但**产物能编译、能在真实运行环境被调用**这一层无自动化证据。用户建议（2026-09-06）：建真实 Spring Boot 工程（MySQL + MyBatis），拟定 DDL，用本工具生成代码，验证**能编译、能调用**。用户进一步决策（2026-09-06）：**模块级门禁对本模块同样有用——生成代码 = 一等代码，应过我们成熟的门禁规则；违规不是"生成代码就这样"，而是信号：生成器质量不足，需改生成器产出高质量代码**（本模块门禁 = 生成器质量验收器）。

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| A：在既有 e2e 里 javac 编译生成物 | 无新模块 | 只验编译不验运行；Spring/MyBatis 组装复杂 | 否决 |
| B：**新增模块 `ddl-codegen-it-springboot`**（Boot 2.7 + MyBatis + MySQL[Testcontainers]，生成代码提交，真库调用 + **门禁全开验收生成代码质量**） | 编译/装配/运行/质量四层一次验证；门禁暴露生成器债（如 checker 对 @Nullable 单值桥接的报错 = 真实缺陷信号） | 新模块依赖/BOM；Docker 缺失跳过；门禁可能暴露需 core 修复的问题（见「门禁分级策略」） | **选定** |
| C：Testcontainers vs 本地 MySQL vs H2 | — | 本地库漂移；H2 对 MySQL 方言兼容风险高 | Testcontainers（mysql:8）；H2 否决 |
| D：IT 默认随 reactor vs profile 门控 | 默认跑=CI 每次真库验证 | CI 时长/网络 | 默认随 reactor + `disabledWithoutDocker` 自动 skip；成本不可接受再 profile 门控 |
| E：生成代码提交 vs 构建时再生成 | 提交=评审可见 | 漂移需守卫 | 提交（**经 spotless 归一化后**）+ 再生成 recipe 含 spotless + `git diff --exit-code` |
| F：模块门禁**全部跳过**（初稿） | 构建简单 | 放弃"生成代码质量验收器"价值，与用户决策相悖 | 否决 |
| G：**模块门禁分级开放**（质量类开、覆盖类/格式化另策） | 质量门禁直接验收生成物；违规分类处置（修生成器 or 记录） | 实现期需 spike 分类；可能牵出 core 修复 | **选定** |

## 门禁分级策略（用户决策核心；替代初稿"模块级全部 skip"）

生成代码 = 一等代码。模块级门禁按语义分级，**质量类全开**：

| 门禁 | 对生成代码的语义 | 处置 |
|---|---|---|
| **checkstyle** | 风格/结构规则（import 序、声明序、可见性、行长等）——生成的类是否满足我们写 Java 的同一标准 | **开**；违规 → 分类：生成器排版缺陷（改 JavaCodegen/生成器）vs 规则对生成物不适用（如要求手写 javadoc 的规则 → 模块级豁免带理由 + progress 记录，不静默） |
| **spotbugs（effort=Max）** | 字节码级缺陷（空指针路径、死代码等） | **开**；违规即生成器债 → 修复生成器（或确认误报 → 带理由豁免 + 记录） |
| **checkerframework Nullness（error）** | 可空性契约——**已预判会抓到真实缺陷**：mapper 单值 findBy 声明返回 `@Nullable` po，repositoryImpl 直传 converter 非空形参（生成实现未命中时 NPE，语义违约） | **开**；该缺陷 → 修复 `MybatisRepositoryImplGenerator`（空安全守卫 `po == null ? null : toX(po)`）——修复会改变生成产物字节 → **按产物变化另立 fix 变更**（紧随本变更，模块门禁绿依赖其落地；见「协调」） |
| **error-prone** | 编译期易错模式 | 开（编译期随 compilerArgs；同 checker 处置） |
| **spotless** | 格式化 | **不直接对生成器原始字节校验**（生成物布局由 JavaCodegen 决定，字节契约由 20260906-02 golden 守护）；模块提交物 = **spotless 归一化后**的生成代码（消费者工程本就会格式化）——格式化器视角的质量问题（如行宽）由 02 golden/JavaCodegen 承担，不在本模块重复 |
| **jacoco ≥75%** | 覆盖率度量的是**测试执行面**，不是代码质量信号；生成类在 IT 场景不可能全载入 | **模块级跳过**（理由记录：质量验收选 checkstyle/spotbugs/checker/error-prone；覆盖门槛仍由 core 主模块守护） |

**协调**：本模块门禁绿 依赖 ① 生成器修复（@Nullable 单值桥接等）落地 ② 模块提交物经 spotless 归一。实施顺序：先 spike——用当前生成器生成、跑门禁，产出**违规清单**（每类：现象/门禁/根因=生成器债 or 规则不适用）；对生成器债 → 立对应 fix 变更（改生成器 + 同步更新 20260906-02 golden fixture），本模块随之绿；对规则不适用项 → 模块级豁免（带理由注释，非静默）。**违规清单与处置表记录 progress（本模块即生成器质量验收器）**。

### 新模块 `ddl-codegen-it-springboot/`（加入根 reactor）

**依赖/版本（评审核实）**：Java 11 + Boot **2.7.18**（Boot BOM import 放模块自身 dependencyManagement）；`mybatis-spring-boot-starter 2.3.2`；`com.mysql:mysql-connector-j`(runtime)；lombok；checker-qual；Testcontainers **1.21.x**（junit-jupiter+mysql，禁 2.0.x）；jakarta.annotation-api（`javax.annotation.Resource` 显式声明）。

**内容**：`src/main/resources/ddl/schema.sql`（保守 MySQL 子集：SQL-enum gender、`@enum` tinyint `status tinyint unsigned`(非 tinyint(1))、`@enum` varchar `kind`(非 char)、pk `id bigint auto_increment`、unique `name`、普通索引 `idx_status(status)` 与多列 `idx_a_b(a,b)`——**各索引首列互异**，QueryMethods 前缀不去重）；`ddl-codegen.properties` 全链路；生成代码经 **spotless 归一后提交**（含 `src/main/resources/mapper/*.xml`——XML 若 spotless 不管则保持生成字节，记录）；手写样例：`@SpringBootApplication` + `@MapperScan` + `mybatis.mapper-locations` + 组件扫描覆盖生成包 + 一个 service 方法演示 repository 查询断言枚举。

**集成测试**（`@Testcontainers(disabledWithoutDocker=true)` + `@Container MySQLContainer` + `@DynamicPropertySource`（Boot 2.7 无 @ServiceConnection）注入 datasource，URL 带 `useSSL=false&allowPublicKeyRetrieval=true`；schema.sql 作 init）：
1. `UserMapper`(po) `insert(status=2)`+`findById` → po.status==2 且 DB 存 2（code 存储语义）
2. `UserRepository.findById`（converter `toUser`）→ entity.status==`Status.ACTIVE`/`getCode()==2`/`getDesc()=="活跃"`；`findByStatus(2)`（**repository 参数标量 Integer**，签名断言进测试）List + `toUserList`
3. `@enum` char 列 kind：写 `"TRIAL"` 读回 `Type.TRIAL`
4. update/deleteById 走 po；`findByAAndB` 多列索引调用一次
5. SQL-enum gender：po(String) 存 `"male"` → converter → entity.gender==`Gender.MALE`
- **单值 findBy 未命中**：核心生成器修复（见门禁分级）落地后即**可测**（守卫返 null）——若修复在更后落地，本场景先测命中路径并在 progress 标注。
- 隔离：insert 回填 id 行级清理/唯一数据；不断言 DB 中文。

**再生成 recipe**：cli 再生成 → spotless apply → `git diff --exit-code`；生成目录禁手改（除标注的样例手写区）。

## 类职责与交互

- 新增：模块 pom、应用主类、schema.sql、ddl-codegen.properties、提交生成代码、测试类、再生成脚本、spike 违规清单（progress）
- 可能牵出：core 生成器修复（fix 变更，改 `MybatisRepositoryImplGenerator` 等；同步 02 golden fixture）
- 根 pom：modules 增模块（Boot BOM 放模块层）
- 文档影响：README/AGENTS/architecture 模块表 → 收尾记忆自检 + check-docs

## 对账与冲突清单（独立评审 + 用户决策 → 处置）

- 初稿"模块级全部 skip" → **用户决策否决**：门禁 = 生成器质量验收器；改为分级开放（G 方案）。
- 评审必改（保留）：@ServiceConnection→@DynamicPropertySource；版本锁 2.7.18/2.3.2/Testcontainers 1.21.x；DDL 硬约束（索引首列互异/非 tinyint(1)/非 char/pk=id）；repository 参数标量签名；装配（@MapperScan 等）；再生成 recipe 含 spotless；jacoco 跳过理由记录。
- checker Nullness 预判缺陷（@Nullable 单值桥接）→ 不再"记已知不修"，改为**生成器 fix 变更**驱动；@Nullable 语义违约亦随之修复。

## 改动文件与影响面

- 新增目录 `ddl-codegen-it-springboot/`；根 pom（modules）；文档三处
- 影响：reactor 新增成员；**core/cli/plugin 生产代码本变更零改动**；门禁暴露的生成器缺陷在**跟随的 fix 变更**修复（产物变化 → 变更类型与破坏性在彼记录）；本模块门禁按分级策略生效
- 文档影响：**是**（README/AGENTS/architecture）→ 记忆自检 + check-docs

## 验收标准（完成 = 下列行为全部成立）

1. 模块门禁按分级策略生效并绿（checkstyle/spotbugs/checker/error-prone 对提交生成物通过；豁免项带理由记录；jacoco 模块级跳过注明）。
2. `mvn clean test` 全 reactor 绿（无 Docker 本模块 IT 自动 skip）；有 Docker 时 5 类调用场景全绿（编译+装配+真库运行）。
3. spike 违规清单逐条有处置（生成器债 → 对应 fix 变更已立/已合；规则不适用 → 带理由豁免），progress 记录完整。
4. 手写样例与生成物共存；再生成 recipe 跑完 `git diff --exit-code` 为空；示例 DDL/config 与 README 一致可直接照抄。

## 验证

- 测试：模块集成测试 + 门禁；core 门禁回归（生成器若在 fix 变更改动）
- 验证命令：`JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test`；`new-change.sh check` + `check-docs`；Docker 环境重跑 IT
