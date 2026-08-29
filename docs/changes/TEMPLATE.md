# design.md（变更设计说明模板）

> 本文件是 docs/changes/{YYYY-MM-DD}-{feat/opt/fix/chore}-{标题}/design.md 的模板：
> 中/大改动按「开发工作流」（AGENTS.md）先写设计再实现；小改动（1-3 文件）可省略，把分析+设计写进 commit message。
> 小改动（1-3 文件、逻辑清晰）可省略本文件，把分析+设计写进 commit message。
> 变更完成后，目录可归档到 docs/changes/archive/ 或随变更保留。

## 背景与问题

（要解决的问题、触发场景、现状不足——一两段即可）

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| A：... | | | 推荐 |
| B：... | | | 否决 |

## 方案

（选定的方案：核心思路、关键代码位置、与现有架构的衔接）

## 改动文件与影响面

- 改动：`path/to/file.java`（改什么）
- 影响：依赖此逻辑的模块/生成器/契约（如 @Generated 所有权、config 键、SPI 签名）

## 验证

- 测试：新增/修改哪些测试（含 PIT 关注点，如适用）
- 验证命令：`JAVA_HOME=... mvn clean test`
