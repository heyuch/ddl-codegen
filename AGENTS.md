# AGENTS.md

This document provides guidelines for AI agents working on this codebase.

## 开发工作流（先分析设计，后实现）

任何非平凡改动（新功能 / 优化 / 问题修复）必须按顺序执行，不得跳过分析直接写代码：

1. **分析**：问题是什么、为什么做、有哪些可选方案、各自的取舍（每项一句话）
2. **设计**：采用哪个方案、改动哪些文件、影响面（依赖此逻辑的其他模块/生成器）、测试策略
3. **实现**：按设计写代码
4. **验证**：`JAVA_HOME=... mvn clean test` 全绿 + 静态检查全绿
5. **文档同步**：涉及架构/数据流/约定变化时，更新本文件「项目架构」节或 `DESIGN.md`；决策追加到 `PROGRESS.md`「关键决策记录」

轻量落地（不引入工具）：
- **小改动**（1-3 个文件、逻辑清晰）：分析+设计写进 commit message（首行概括，正文给取舍）
- **中/大改动**：先在 `PROGRESS.md` 追加决策记录（问题/方案/取舍/影响面）；复杂设计写 `docs/design-notes/<name>.md`（用 `TEMPLATE.md` 模板）
- **禁止直接开写**：先写出「要改什么、为什么、怎么改、影响谁」再动代码

## 项目架构（顶层）

DDL 驱动的 Java 代码生成框架：手写 MySQL DDL（create/alter/drop）→ 增量生成 MyBatis 链路代码（Mapper/XML/Pojo/RepositoryImpl/Converter/Entity/枚举）。

### 模块与依赖方向

```
ddl-codegen-cli  →  ddl-codegen-core  →  ddl-codegen-tree
（命令行入口）      （框架：模型/解析/命名/类型/生成器）  （自研可修改 Java AST，零依赖）
```

运行时外部依赖仅 druid（DDL 解析）。

### 数据流（核心管线）

```
DDL 文本
  → DruidDdlParser（→ DdlOperation[]）
  → StatementApplier（应用到 Schema，产出 ApplyResult：受影响表/改名/删除记录）
  → CodeGenerator（按 config artifacts.* 启用顺序，逐表 × 逐 artifact 调用生成器）
  → AbstractJavaArtifactGenerator（定位文件 → 解析现有源码 → 只 reconcile @Generated 成员 → 拦截器 → 打印）
  → FileWriter（字节比对，无变化不写盘）
  → 文件 + ChangeReport
```

### 关键机制

- **@Generated 成员级增量同步（无 manifest）**：文件位置 = config 推导（根 + module + package + 类名）；工具只增删改带 `@Generated` 的成员，用户手写代码永不触碰；解析失败不覆盖
- **reconcile 即 diff**：模型有而文件无 → 增；有而模型无 → 删；签名/类型变 → 替换；一致 → 跳过
- **DDL 注解**（注释中 `@name[:value]`）：`@type`（复用已有类型）/ `@as`（生成类命名）/ `@ignore`（跳过）；未知注解 warning 忽略不中断

### 扩展点（三层 SPI）

| SPI | 位置 | 用途 |
|---|---|---|
| `ArtifactGenerator` | `hyc.codegen.core.gen` | 自定义 artifact；继承 `AbstractJavaArtifactGenerator` 只写成员构建 |
| `ArtifactInterceptor` | `hyc.codegen.core.interceptor` | AST 装饰（内置 lombok/jsr303），config `use` 引用 |
| `DdlAnnotationHandler` | `hyc.codegen.core.annotation` | DDL 注解解析 + 类型钩子，`annotations.custom` 注册 |

另有可替换 SPI：`DdlParser`（DDL 解析）/ `ConfigLoader`（配置加载）/ `TableNameStrategy`（命名）。

### 文档地图

| 文档 | 内容 |
|---|---|
| `README.md` | 快速开始 + config/注解参考 |
| `DESIGN.md` | 完整技术方案（边界契约/模块/管线/SPI/风险取舍） |
| `PROGRESS.md` | 开发进度 + 关键决策记录 + 已知限制 |
| `STATIC-RULES-REVIEW.md` | 静态检查规则考察（阈值基线/实证/抑制准则） |
| `TASKS.md` | 任务列表 |
| `docs/design-notes/` | 变更设计说明（先设计后实现的工作流产物） |

### 修改约束

- 架构/数据流变化必须同步本节约 `DESIGN.md`
- 新生成器必须遵守「config 存在即启用」与「@Generated 成员所有权」契约（见 `DESIGN.md` §1）

## Build, Lint, and Test Commands

This is a Maven multi-module project using Java 11.

### Build Commands
```bash
# Build all modules
mvn clean compile

# Build with tests
mvn clean test

# Build a specific module
mvn -pl ddl-codegen-core clean compile
```

### Running Tests
```bash
# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=PoTest

# Run a single test method
mvn test -Dtest=PoTest#generate

# Run tests in a specific module
mvn -pl ddl-codegen-core test

# Run tests with verbose output
mvn test -X
```

### Code Quality
```bash
# Check for dependency updates
mvn versions:display-dependency-updates

# Check for plugin updates
mvn versions:display-plugin-updates
```

## Code Style Guidelines

### General Principles
- Write clean, readable code with minimal complexity
- Avoid unnecessary abstractions; prefer simplicity
- Use meaningful and concise names for variables, methods, and classes

### Java Version
- Target Java 11 compatibility
- Prefer simple if else statements rather than streams, optional, only use lambdas with simple logics

### Imports
- Use explicit imports (no wildcard imports like `java.util.*`)
- Group imports in this order:
  1. `java.*` imports
  2. `javax.*` imports
  3. Third-party libraries
  4. Project imports (`hyc.codegen.*`)
- Sort imports alphabetically within each group

### Naming Conventions
- **Classes**: UpperCamelCase (e.g., `JavaGenerator`, `TableResolver`)
- **Methods**: lowerCamelCase (e.g., `generateCode`, `collectUserDefinedFields`)
- **Variables**: lowerCamelCase (e.g., `module`, `pkg`, `useLombok`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT`)
- **Packages**: lowercase (e.g., `hyc.codegen.core`)

### Types and Generics
- Use `List`, `Map`, `Set` interfaces over concrete implementations in method signatures
- Use `ArrayList` when concrete implementation is needed
- Use `LinkedHashMap` when insertion order matters
- Specify generic type parameters explicitly (no raw types)

### Null Handling
- Use `@Nullable` annotation from `javax.annotation.Nullable` for nullable parameters and return values
- Use `@Nullable` on method parameters that can be null
- Consider using early returns to avoid deep nesting with null checks
- Example:
  ```java
  @Nullable
  public String getName() { ... }

  public void process(@Nullable String input) {
      if (input == null || input.isEmpty()) {
          return;
      }
      // proceed with input
  }
  ```

### Error Handling
- Use exceptions for exceptional conditions, not control flow
- Propagate exceptions with meaningful context
- Use try-with-resources for any `AutoCloseable` resources
- Example:
  ```java
  try (BufferedReader r = new BufferedReader(new FileReader(file))) {
      // read file
  } catch (IOException e) {
      throw new RuntimeException("Failed to read file: " + file.getName(), e);
  }
  ```

### Comments
- Use Chinese comments for business logic explanations (consistent with existing codebase)
- Use English for technical documentation
- Javadoc for public APIs
- Inline comments for non-obvious logic
- Avoid redundant comments (e.g., `i++ // increment i`)

### Lombok Usage
- Use `@Data`, `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor` appropriately
- Mark Lombok dependencies as `provided` scope (not included in runtime)
- Use `@Slf4j` for logging in classes that need logging

### Testing
- Use JUnit 5
- Use `Assert.assertEquals`, `Assert.assertNotNull`, etc. for assertions
- Place test files in `src/test/java` with same package structure
- Test file naming: `<ClassName>Test.java`
- Test method naming: `test<Operation>()` or `<operation>Should<ExpectedResult>()`

### Code Structure
- Package-private fields are acceptable for internal classes
- Keep classes focused: single responsibility principle
- Limit method length; extract helper methods when needed

## Quality Bar（开源项目标准）

- 代码按开源项目标准编写：命名传达意图（不用 `U`/`Tmp` 这类无名工具类名）、类小而聚焦、包按职责组织、公共 API 最小化（不暴露无需暴露的）
- 复用旧代码：质量不达标直接优化，不机械照搬；迁移即改进
- 本项目同时是学习材料：结构清晰、命名优雅、注释解释 WHY 而非 WHAT
- 静态检查是硬门槛：spotless/checkstyle/error-prone/checkerframework 报错按提示修复（规则考察见 `STATIC-RULES-REVIEW.md`）
