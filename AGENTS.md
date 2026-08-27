# AGENTS.md

This document provides guidelines for AI agents working on this codebase.

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
