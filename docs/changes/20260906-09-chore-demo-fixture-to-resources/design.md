# demo-fixture-to-resources

> 变更号：20260906-09　变更类型：chore　目录：`docs/changes/20260906-09-chore-demo-fixture-to-resources/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

`ddl-codegen-tree/src/test/java/hyc/codegen/tree/Demo.java` 是 round-trip 字节全等夹具（JavaParserTest.parse + RoundTripSmokeTest.demoRoundTripIsByteExact 只做文件文本 parse→print 比对，无任何代码引用 `hyc.codegen.tree.Demo` 类——grep 全仓实证）。它躺在 test 源码目录被编译，被迫背了三处「不得改动夹具」的特殊处理：pom checker `-AskipDefs=hyc\.codegen\.tree\.Demo`（防检查器要求加注解）、pom spotless `<exclude>`（防格式化）、checkstyle-suppressions.xml 整文件 suppression。移入 `src/test/resources` 后不再是编译输入，三处特殊处理全部可删，夹具维护自由度恢复（改完跑字节断言即可）。

## 方案

- `git mv` `src/test/java/hyc/codegen/tree/Demo.java` → `src/test/resources/fixtures/roundtrip/Demo.java`（tree 模块首个 resources；路径与 core `fixtures/gen/<case>` 惯例同构，`roundtrip` 表用途）。
- 消费方改路径（沿用现有 File/CWD 读取风格，与 core harness `Paths.get("src/test/resources/...")` 同款）：JavaParserTest.parse、RoundTripSmokeTest.demoRoundTripIsByteExact → `src/test/resources/fixtures/roundtrip/Demo.java`。
- 删除：pom `-AskipDefs` 参数（含注释）、pom spotless `<exclude>`（含注释）、checkstyle-suppressions.xml 首条整文件 suppression（保留第二条测试级 suppression）。
- spotless includes 仅 `src/{main,test}/java`，resources 天然不格式化——无需新增排除。
- 文档：static-rules-review §3 M0.1 spotless 行（「Demo 排除格式化→合理」已陈旧）追加消除注记，指向本变更。

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| A：移入 test resources（fixtures 子目录） | 三处特殊处理全删；与 core fixtures 惯例同构；夹具改自由 | File 路径仍依赖 CWD（现状已如此） | 推荐 |
| B：留在 test java 保留排除 | 无改动 | 三处配置继续背着、夹具仍被全套门禁约束 | 否决 |

## 改动文件与影响面

- `git mv`：`ddl-codegen-tree/src/test/java/hyc/codegen/tree/Demo.java` → `ddl-codegen-tree/src/test/resources/fixtures/roundtrip/Demo.java`
- `ddl-codegen-tree/src/test/java/hyc/codegen/tree/JavaParserTest.java`（L31 路径）
- `ddl-codegen-tree/src/test/java/hyc/codegen/tree/RoundTripSmokeTest.java`（demoRoundTripIsByteExact L300 路径）
- `pom.xml`（删 `-AskipDefs` arg + spotless exclude）
- `checkstyle-suppressions.xml`（删 Demo 整文件 suppression）
- `docs/static-rules-review.md`（M0.1 spotless 行注记）
- 影响：Demo 不再被 test-compile 编译——语法有效性由 JavaParserTest/RoundTripSmokeTest parse 兜底；字节全等断言针对文件内容、与位置无关。

## 验收标准

1. Demo.java 位于 `src/test/resources/fixtures/roundtrip/`，git 历史保留（git mv）。
2. pom/checkstyle-suppressions.xml 中无任何 Demo 相关特殊处理（grep 实证）。
3. JavaParserTest.parse 与 demoRoundTripIsByteExact 仍字节全等绿。
4. `mvn clean test` 全量绿 + 编译期 0 告警。

## 验证

- `JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test`；`grep -rn 'Demo' pom.xml checkstyle-suppressions.xml` 为空
- `new-change.sh check` / `check-docs`（static-rules-review 改动）
