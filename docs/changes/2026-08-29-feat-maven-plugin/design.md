# maven-plugin：Maven 插件分发与运行

## 背景与问题

工具目前只能通过 CLI（`java -jar ddl-codegen-cli.jar`）运行。需要提供 Maven 插件形式：
让使用者能通过 `mvn ddl-codegen:generate` 在项目里直接运行，支持指定项目根目录、
config 文件、内联 DDL 字符串、DDL 文件（含文件内行范围，如 `create-user.sql:66-120`）。
同时需要完整的单元测试与集成测试。

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| **1. DDL 输入参数形态**：A) `ddlFile`（单个，解析 `path:start-end` 后缀）+ `ddl` 字符串；B) `ddlFiles` 列表 + 独立 `range` 参数 | A 直接贴合需求、API 最小；B 灵活但参数面复杂、范围语义分散 | A 对"多个文件"场景需后续扩展 | **推荐 A**（需求未提多文件；扩展时加列表参数即可） |
| **2. 生命周期绑定**：A) 不绑 defaultPhase，显式 `mvn ddl-codegen:generate`；B) 绑 generate-sources | A 可控：DDL 驱动的生成是"刻意"动作，不应每次构建隐式执行；B 全自动 | A 使用者需显式调用；B 可能意外改动源码/拖慢构建 | **推荐 A** + `skip` 参数 |
| **3. 复用 CLI 管线**：A) 从 `Main` 提取 `Codegen` 门面到 core，CLI 与插件共用；B) 插件重拼 Druid/StatementApplier/CodeGenerator | A 单一管线防漂移（修一处两边生效）；B 重复实现 | A 需重构 Main（小） | **推荐 A** |
| **4. config 默认名**：统一为 `ddl-codegen.properties`（用户新 spec），CLI 同步加默认值；B) 维持现状 `ddlgen.properties` 文档约定 | A 一致性好、CLI/插件行为对齐；B 不动现状 | A 需改 README 与 CLI 少量代码 | **推荐 A**（对齐用户命名） |
| **5. projectRoot 语义**：`projectRoot` 参数（默认 `${project.basedir}`）覆盖 config 推导的根；`configFile` 默认跟随 projectRoot | 参数显式时以参数为准（契约：以 config/参数为准）；config 默认 = `projectRoot/ddl-codegen.properties` | — | **推荐** |
| **6. 测试策略**：单元（Mojo 参数默认值、范围解析、门面）+ 集成用 maven-invoker 起真实 `mvn` 在临时项目执行并断言生成文件；B) 仅单元测试 + MojoTestCase | A 是真集成（真实 Maven 生命周期/类加载）；B 快但不覆盖运行环境 | A 需先 install 插件到本地仓库 | **推荐 A**（invoker 是 maven 插件 IT 的标准做法） |

## 方案

### 新模块 `ddl-codegen-maven-plugin`

- 依赖 `ddl-codegen-core`（compile）+ `maven-plugin-api`/`maven-plugin-annotations`（provided）
- 父 pom 加模块；`maven-plugin-plugin` 生成插件描述符（goalPrefix 自动为 `ddl-codegen`）

### Mojo `generate`（注解 @Mojo，无 defaultPhase）

| 参数 | 类型 | 默认 | 语义 |
|---|---|---|---|
| `projectRoot` | File | `${project.basedir}`（普通 mvn 即 cwd） | 项目根，覆盖 config 推导 |
| `configFile` | File | `${projectRoot}/ddl-codegen.properties` | 配置文件；不存在→报错 |
| `ddl` | String | null | 内联 DDL 字符串（与 ddlFile 互斥，同时设置→报错） |
| `ddlFile` | File | null | DDL 文件，支持 `path:start-end` 后缀（如 `create-user.sql:66-120`）；行范围越界→钳制到文件边界 + warning |
| `dryRun` | boolean | false | 只报告不写盘 |
| `skip` | boolean | false | 跳过执行 |

执行流程：解析参数 → 加载 config → 读 DDL（内联或文件+范围切片）→ 调 `Codegen` 门面 → 变更报告打到 Maven log（getLog）。

### core 新增门面 `hyc.codegen.core.Codegen`

```java
public final class Codegen {
    /** 解析 config → 构建默认生成器/拦截器 → 执行生成，返回变更报告 */
    public static ChangeReport run(Path configFile, Path projectRoot,
            String ddlText, boolean dryRun) throws Exception;
}
```
- 内部复用 Main 现有的 instantiateHandlers/defaultGenerators/defaultInterceptors 逻辑（从 Main 迁移到门面）
- `Main`（CLI）重构为调门面：行为不变，另加 `--config` 缺省 = `cwd/ddl-codegen.properties`（对齐插件默认）
- README/AGENTS 文档同步（config 文件名、新模块）

### 测试

- **单元**（ddl-codegen-maven-plugin/src/test）：
  - `DdlFileRangeTest`：`file:66-120` / 无后缀 / 越界钳制 / start>end 报错 / 非数字报错
  - `GenerateMojoTest`：参数默认值、ddl 与 ddlFile 互斥校验、skip、projectRoot 覆盖（直接实例化 Mojo 注入参数，不跑真实 mvn）
  - core 侧 `CodegenTest`：门面 run 的端到端（临时目录 + 真实 DDL）
- **集成**（maven-invoker-plugin，src/it）：
  - `it-simple`：临时项目 + ddl-codegen.properties + create.sql，`mvn ddl-codegen:generate` → 断言生成文件存在且内容正确、重跑幂等
  - `it-range`：带行范围的 ddlFile → 断言只生成范围内表
  - `it-inline`：`-Dddl=...` 内联执行
  - 运行方式：先 `mvn install -pl ddl-codegen-maven-plugin -DskipTests`（安装插件及依赖到本地仓库），再 `mvn verify -pl ddl-codegen-maven-plugin`（invoker 在临时目录跑真实 mvn）

## 改动文件与影响面

- 新增：`ddl-codegen-maven-plugin/pom.xml`、`Mojo`、范围解析工具、单元测试、`src/it/*` 集成工程
- 修改：`ddl-codegen-core`（新增 `Codegen` 门面）、`ddl-codegen-cli/Main`（重构为调门面 + config 默认值）、父 `pom.xml`（加模块）、`README.md`/`AGENTS.md` 文档地图（config 默认名 `ddl-codegen.properties`）
- 影响：CLI 行为对齐（config 默认值）；`Codegen` 门面成为 CLI/插件共用的唯一管线入口（后续扩展统一走它）

## 验证

- 单元测试：范围解析边界（约 8 例）、Mojo 参数/互斥/skip（约 6 例）、`Codegen` 门面（3 例）
- 集成测试：3 个 invoker 工程，断言生成文件、幂等、范围生效
- 全量：`JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test` 全绿 + `mvn validate` 静态检查全绿
- PIT：核心新增逻辑（范围解析）补充变异测试关注
