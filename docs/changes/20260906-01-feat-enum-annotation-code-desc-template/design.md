# enum-annotation-code-desc-template

> 变更号：20260906-01　变更类型：feat（**带破坏性**：SQL enum 列生成产物模板整体更换）　目录：`docs/changes/20260906-01-feat-enum-annotation-code-desc-template/`
> 本文件只记设计与取舍；评审结论与实现偏差不写这里（落变更目录 `progress.md`）。

## 背景与问题

现状 `EnumGenerator`（`ddl-codegen-core/.../gen/EnumGenerator.java`）只为 **SQL `enum(...)` 类型列**生成枚举类（常量 = DDL 字面量 + `value()`/`fromValue(String)` 单 String 模板）。用户实际 DDL 大量用 tinyint/varchar 状态列，枚举语义写在列 comment 里：

```sql
status  tinyint unsigned NOT NULL comment '状态 1=初始(INIT) 2=活跃(ACTIVE) 3=停用(SUSPEND) @enum:Status',
type    varchar(20)     NOT NULL comment '类型 NORMAL=普通 VIP=高级 TRIAL=试用 @enum:Type',
```

需求（用户评审决议）：
1. 非 SQL-enum 列通过 comment 注解 `@enum` 标注声明「按枚举处理」并生成枚举类；枚举项格式 `{code}={desc}({name})`，name 可选。
2. 枚举类模板改为 code/desc 双字段；**SQL enum 列也统一迁移此模板**（breaking，用户已确认）。
3. `@enum` 列接入 `enums=true` 跨产物视图（entity 字段用枚举类、converter 桥接）——用户已确认。
4. comment 无枚举项 → 生成**空枚举类骨架**（无常量，留用户后续自补）；常量名统一走 `constantName()` 大写清洗——用户已确认。
5. `EnumGenerator` 支持产物 `lombok` 选项（同 pojo 键语义）：`true` → 类级 `@RequiredArgsConstructor` + `@Getter`；`false`（默认）→ 生成手写私有构造器与 getter 方法——用户调整意见 ①。
6. `@enum` 标注下枚举类名：可显式给出（`@enum:Status`），省略时（裸 `@enum`）命名**与 enum column 同一方式**（既有 `naming.enum.style` 配置：column/tableColumn）——用户调整意见 ②（已按「简化」修正，v5）。
7. 枚举类生成**一对反查方法**：`fromCodeNullable`（入参/返回均 `@Nullable`，null 入参与匹配不上都返回 null、不抛异常）+ `fromCode`（严格版，null 入参抛异常、匹配不上抛异常、返回永不 null）——用户调整意见 ③。
8. 生成代码里的 `@Nullable` 注解**由用户配置指定**（复用全局键 `annotations.nullable`，缺省 checkerframework qual）——用户调整意见 ④。
9. **ALTER DDL（如修改 comment 枚举项）后的增量更新语义**：成员级 reconcile，不整类重写；enum 常量的增/删/**同名 code/desc 值改**都要同步（后者现机制是洞，见 v7 补强）——用户提问澄清。

现状关键约束（代码实证，独立评审已逐条核验）：
- 所有 enum 语义消费点都挂在「`column.getEnumValues()` 非空」单开关上：`EnumGenerator` L134/L150、`PojoGenerator.fieldType` L114、`ConverterGenerator` L84/L136、`TypeMapper.resolveType` L134；而 `getEnumValues()` 只由 DruidDdlParser 从 `enum(...)` 填充 → 新 `@enum` 列需引入统一的「枚举列」判定。
- `@enum` 注解目前是未知注解（内置仅 type/as/ignore），被 `AnnotationProcessor` warning 忽略；`DruidDdlParser()` 默认构造即用 `AnnotationRegistry.builtin()`，EnumHandler 入 builtin 即全链路生效，无其他装配点。
- `ConverterGenerator.toMethod`（L136-147）的 enum 视图判定是 `fieldType != "java.lang.String"` 的启发式——对数字 `@enum` 列非枚举端是 `Integer`，会误判；且桥接硬编码 `.value()`/`.fromValue()`，与新模板不符。
- `JavaCodegen.printEnumConstants`（L299-317）：枚举无常量时**直接 return 不打 `;`**；而空枚举类带字段/方法时 Java 语法必须有 `;` → tree 需小修。
- reconcile 只对**字段/方法成员**生效：类级注解（`@Getter`/`@RequiredArgsConstructor`）与 enum 常量 init 的变更**不会**同步到已存在文件（既有架构边界，本次不做类级 reconcile）；字段签名 `signature(Variable)` = **仅类型**（AbstractJavaGenerator L286-288），常量 init 不比 → **同名 code/desc 值改不触发替换**（需本变更补强）；方法签名 = 返回 + 参数类型序列 + 方法体（空白归一化，体变也替换，L266-283）。
- `Class`（tree）：`fields` 为有序列表，`addField` 追加、`removeField` 按 equals 删，**无原位替换 API**（L78-83/235-236）；enum 常量构建侧经 `builder.enumConstant` 打 `VariableKind.ENUM_CONSTANT`（L319-325）。
- **解析侧 `JavaTreeConverter.visitClass` 把所有 Variable 成员统一标 `VariableKind.FIELD`**（L66-82），含 enum 常量 → 解析/构建两侧 kind 不对称；`JavaCodegen.isEnumConstant`（L166-190）靠「PUBLIC+STATIC+FINAL 且 type == 类名」启发式兜底识别解析侧常量。
- `Meta.put(key, null)` 是**删键**而非存 null（Meta.java L50-56）→ 裸注解的占位写法必须显式（见下 EnumHandler 设计）。
- `PojoGenerator.fieldType` 中 `@type` 优先于 enums 视图（L107-118）→ `@enum`×`@type` 同列需要显式冲突处理。
- pojo 的 lombok 特性为**显式开启**（`entity.lombok=true`，Boolean.parseBoolean，默认 false）→ enum 的 `lombok` 选项沿用同键同默认。
- `NamingService.enumClassName(table, column)`：column 风格 = 列 Pascal（默认）；tableColumn 风格 = `tableClassName(table) + 列 Pascal`（NamingService L90-96，style 读 config `naming.enum.style`）；`tableClassName` 走剥前缀/分表链并尊重 `TableNameStrategy`（L137-147）。**表级 `@as` 不进命名服务**（meta 不可见，SQL-enum 现有 tableColumn 风格同样不含表级 `@as`）。本设计**不加新命名方法**——裸 `@enum` 与 enum column 复用同一 `enumClassName`。
- `@Nullable` 可配置通道**已存在**：全局键 `annotations.nullable`（`annotations.*` 保留命名空间，`PropertiesConfigLoader` 解析、`DdlConfig.getNullableAnnotation()` 消费，缺省 `org.checkerframework.checker.nullness.qual.Nullable`）；`TableContext.getNullableAnnotation()` 已暴露给生成器，pojo 的 jsr305 特性同源消费。生成器只**消费**该配置，不硬编码注解 FQN——用户可配任意注解（如 `javax.annotation.Nullable`）。
- tree 的 Method/参数级注解：方法注解（`@Generated` 既已打）打印已验证；**参数级注解打印需实现时实证**（tree 若缺 → 小修 + 用例，验收断言含参数 `@Nullable`）。

## 可选方案与取舍

| 方案 | 优点 | 缺点 | 结论 |
|---|---|---|---|
| A：双模板共存（SQL-enum 保旧 `value()` 模板，仅 `@enum` 列用 code/desc） | 改动最小、旧产物零破坏 | 生成器两套模板；SQL-enum 列语义与全库风格割裂；用户已否决 | 否决（用户拍板统一模板） |
| B：**统一 code/desc 模板，列 kind 决定项源与 code 类型**（本设计） | 单一模板、语义统一；SQL-enum 存储字面量天然是 code；desc/name 可由 comment 列表按 code 匹配补充 | 破坏既有 SQL-enum 产物（方法/字段全变） | **选定** |
| C：`@enum` 仅出独立枚举文件、不接入 `enums=true`/converter | 面最小 | entity 字段仍是 Integer/String，枚举类悬空无消费点 | 否决（用户已确认接入） |
| D：反查用 switch（String/Integer 走 case） | 形似旧 fromValue | `long` 不能 switch（bigint 列 code 为 Long 时编译不过），需两套 body | 否决 |
| E：反查统一 `Objects.equals` 遍历常量 | 一套 body 覆盖 String/Integer/Long；空常量自然走 null/抛错 | 无 switch 直查，O(n)（枚举常量数量级可忽略） | **选定** |
| F：构造器/getter 固定手写或固定 lombok 单形态 | 产物路径单一 | 无法适配有无 lombok 依赖的项目 | 否决（用户要求选项双形态） |
| G：**`lombok` 选项双形态**（true 注解 / false 手写） | 与 pojo lombok 特性同语义；两种形态方法面一致（均 `getCode()`/`getDesc()` + 私有构造器） | 两套生成路径、多一组测试 | **选定** |
| H：裸 `@enum` 类名与 **enum column 同一命名**（既有 `naming.enum.style`：column/tableColumn） | 一条命名规则覆盖两类列，无新命名方法、零配置语义变化 | 默认 style=column 时裸 `@enum` 得列名 `Status`，跨表同名列同包需显式 `@enum:名`/`@as` 避撞 | **选定**（用户简化后拍板） |
| I：裸 `@enum` 类名 = 固定 {TableName}{ColumnName}（新增不受 style 配置影响的命名方法） | 默认名带表名前缀，跨表撞名概率低 | 引入与 `naming.enum.style` 平行的第二套命名规则，破坏既有配置心智 | 否决（v3 曾采用，用户要求简化撤回） |
| J：反查仅单一 `fromCode`（null → null / 未知 → 抛） | 方法少 | null 入参与未知 code 语义混杂，调用方无法区分宽松/严格 | 否决（用户要求方法对） |
| K：**反查方法对 `fromCodeNullable`（全宽松）+ `fromCode`（全严格）** | 宽松/严格语义显式分离、`@Nullable` 类型表达；converter 可保持旧「未知 code 抛错」语义 | 每枚举类多一个方法 | **选定** |
| L：`@Nullable` 硬编码 checkerframework qual | 无配置面 | 用户若用 javax/jetbrains 等其他注解体系无法替换 | 否决 |
| M：**`@Nullable` FQN 消费全局 `annotations.nullable`（用户可配任意注解）** | 复用既有全局配置通道、与 pojo jsr305 同源一致；无新增配置键 | 生成枚举类恒带该注解 → 消费项目需具备所配注解的依赖 | **选定** |
| N：枚举数据变化（增/删/改）→ **整类重写** | 实现最简、顺序天然与 DDL 一致 | 抹掉用户手写常量/方法（骨架自补流），违背「只动 @Generated 成员」契约 | 否决 |
| O：枚举常量变更走既有 remove+append 替换 | 不动 tree API | 被改常量移到末尾 → ordinal/values() 顺序漂移；重排场景仍过期 | 否决 |
| P：**enum 常量签名纳入 init 文本 + 原位替换**（签名变即原索引替换） | 增/删/**同名 code/desc 值改**三者全部增量同步；保持 DDL 声明顺序；@Generated 与用户常量边界不变 | 需 tree 小改（Class.replaceField、常量 kind 标注、init 规范文本 helper） | **选定**（v7 补强） |

## 方案

### 语义模型（新增「枚举列」概念）

一个列**生成枚举类**当且仅当 `isEnumColumn()`：`sqlType == "enum"`（原生 SQL-enum 列）**或**列 comment 含 `@enum`（`meta` 键 `"enum"`）。`@enum` 标注 = 声明该列按枚举处理。枚举项来源与 code 类型按列 kind 分：

| 列 kind | 枚举项来源 | code 字段类型 | 无项时行为 |
|---|---|---|---|
| SQL `enum(...)` 列（可同时带 `@enum`，仅用于类名） | `enum(...)` 字面量**为主**；comment 列表项中 code == 某字面量的项补充其 desc/name（code 不匹配任何字面量的列表项 → warning 跳过） | `String`（存储字面量） | 恒有项（字面量至少 1 个）；无列表项匹配 → desc = `""` |
| 非 SQL-enum 列 + `@enum` 标注 | comment 列表 | 列自然映射（`TypeMapper.sqlToJava`）：`Integer`/`Long`/`String` | 空列表 → **空枚举类骨架**（仍生成字段/构造/反查方法） |
| 其他列 | — | — | 不生成 |

**枚举类名规则（按优先级）**：
1. 列级 `@as`（通用改名覆盖，既有语义保留）；
2. `@enum` 显式值（简单 Java 标识符**且非 Java 关键字**，否则报错）；
3. 缺省（裸 `@enum` **与 SQL-enum 列同一方式**）→ 既有 `NamingService.enumClassName(table, column)`，按 `naming.enum.style`：默认 column → 列 Pascal（`status` → `Status`）；tableColumn → `{表基类名}{列 Pascal}`（`t_user.status` → `UserStatus`）。表级 `@as` 不进命名服务（与既有行为一致，见现状约束）。

`@enum` 仅允许在**列**注释（targets=COLUMN，其余位置 warning 忽略）。

**`@enum` × `@type` 同列 → fail-fast 报错**：`@type` 在 PojoGenerator 优先于 enums 视图（L107-118），二者叠加会使 converter 精确比对（见下）静默失去桥接且无 warning；声明矛盾直接报错最清晰。

### 增量更新语义（ALTER DDL 后的枚举类同步；v7 核心补强）

成员级 reconcile（**不整类重写**，方案 N 否决：会抹掉用户手写成员）。枚举项各类变化的同步方式：

| 变化 | 同步机制 | 说明 |
|---|---|---|
| 增项（新 name） | @Generated 常量按名**追加**（现有） | 追加在末尾 |
| 删项 | @Generated 常量按名**删除**（现有） | 文件里不再有该项 |
| 改名 | 旧名删除 + 新名追加（现有） | 位置移尾（接受） |
| **同名 code/desc 值改** | **signature 纳入 init 规范化文本 → 原位替换**（v7 补强，方案 P） | 旧机制是洞：签名只看类型，改值不触发替换、静默过期 |
| 构造器/getter/fromCode 等 | 方法签名 = 返回 + 参数 + body（现有，体变即替换） | fromCode body 遍历 values()，天然不含项数据 |
| 类级注解 / lombok 形态 / 项顺序重排 | **不 reconcile**（既有边界） | 删文件重生成；见「删除/升级语义」 |

**用户手写代码保留契约（reconcile 前提）**：reconcile 只增/删/改**带 `@Generated` 的成员**——用户手写成员（无 `@Generated`，如自增方法、自补常量、自定义字段、类级注解/javadoc）**一律原样保留**，包括 enum 常量值被同步时也只替换 `@Generated` 那份；用户手写方法与期望生成方法同名时由「跳过新增 + warning」守卫防重复（见消费点开关迁移表）。这正是「不整类重写」的根本原因（方案 N）。反例边界：用户手改 `@Generated` 成员内容会在下次生成时被还原为 DDL 值（工具拥有该成员）；类级注解/lombok 形态/顺序三类结构性变更仍须删文件重生成（见「删除/升级语义」）——这些均已在验收 11/12 覆盖。

实现配套（tree 小改）：
- `JavaTreeConverter.visitClass`：enum 类的常量成员标 `VariableKind.ENUM_CONSTANT`（消除解析/构建两侧 kind 不对称；`JavaCodegen.isEnumConstant` 的 kind 分支即可覆盖解析侧，启发式保留作裸 AST 兜底）。
- `JavaCodegen`：抽公共静态 helper 产出**常量 init 规范化文本**（打印器与 reconcile 签名同源，避免两处语义漂移）——文本变换 = 现 visitEnumConstants L546-555 规则（`new X(...)` → `(...)`；裸值 → `(value)`；已 `(` 开头原样）+ 空白归一化。
- `Class`：新增**原位替换** API（`replaceField(old, new)`：按索引替换，保持声明顺序）——方案 O 的 remove+append 会让被改常量 ordinal/values() 顺序漂移，否决。
- `AbstractJavaGenerator.signature(Variable)`：enum 常量（`VariableKind.ENUM_CONSTANT`）→ 类型 + init 规范化文本；普通字段维持类型-only（serialVersionUID 等不受影响）。`reconcileFields` 对常量替换走原位 `replaceField`（普通字段沿用现有 remove+append 语义，行为不变）。

### EnumHandler（新内置注解）

`AnnotationRegistry.builtin()` 追加注册。`parse(Meta, @Nullable value)`：值非空 → `meta.put("enum", value)`；值为 null（裸 `@enum`）→ **`meta.put("enum", Boolean.TRUE)` 占位**（`Meta.put(k,null)` 是删键，裸注解必须显式占位，否则整列静默退化为普通列）。判读方统一：`isEnumColumn()` 用 `meta.containsKey("enum")`；类名提取仅当值为 `String` 时使用（`TableContext.enumClassName`），非 String（裸标注）→ 走命名策略（规则 3）。

### 枚举项 grammar 与校验

- 解析（新 `EnumItem` + `EnumCommentParser`）：comment 按空白切 token；匹配 `{code}={desc}` 或 `{code}={desc}({name})`——code/desc/name 均不含空白；desc 不含 ASCII `()`；`(name)` 必须整 token 后缀。不含 `=` 的 token（正文前缀「状态」等）自然跳过；含 `=` 但结构不符 → warning 跳过该项。
- 常量名（候选 → 统一 `constantName()` 大写清洗，保证合法标识符）：显式 name → name 原文；无 name 且 code 为 String → code 原文；无 name 且 code 为数字 → `列名_rawCode`（示例 `STATUS_1`）。**唯一性判定与重复检测一律以清洗后最终常量名为准**（如 String code `1` → `_1` 属合法怪异名、`a-b` 与 `a_b` 清洗后同为 `A_B` → 判重报错）。
- code 字面量：数字列 rawCode 按列类型解析（Integer/Long，**long 字面量打印补 `L`** 防 int 溢出），解析失败/溢出 → 报错。**前导零**（`01=初始`）：数值判定/去重以解析后数值为准，常量名沿用 raw 文本（`STATUS_01`）。
- **fail-fast 报错**（`IllegalStateException`，消息含表.列）：
  - `@enum` 列自然类型不在 {String,Integer,Long}（decimal/BigDecimal、tinyint(1)/Boolean、date 等）→ 不支持 `@enum`；
  - 数字列上非数字 code、code 解析溢出；
  - 同枚举类内 code 重复（数值列按解析后值判）或常量名重复（按清洗后名判）；
  - `@enum` 值非法标识符或为 Java 关键字；`@enum` 与列级 `@type` 同列；
  - **同表内两个枚举列解析出相同类名** → 报错（含两列名）。跨表同 enum 包撞名 → 既有 `@as` 同族限制，写入已知限制，本期不解决（生成实例无状态，跨表去重需全局状态）。
  - 结构不符文本除外（warning 容忍——正文里可能恰含 `=`）。

### 生成模板（统一，`buildEnum` 重写；成员形态按产物 `lombok` 选项切换）

产物选项 `lombok`（`ctx.getArtifactConfig().getOption("lombok")`，`Boolean.parseBoolean`，**默认 false**，与 pojo `lombok=true` 同为显式开启）。

**lombok=true**（类级 `@Getter` + `@RequiredArgsConstructor`；构造器/getter 由 lombok 生成，无手写成员；枚举构造器按 JLS 为私有，lombok 按此生成；成员照常打 `@Generated`）：

```java
package com.demo.enums;

import javax.annotation.processing.Generated;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
@RequiredArgsConstructor
public enum Status {
    @Generated
    INIT(1, "初始"),
    @Generated
    ACTIVE(2, "活跃"),
    @Generated
    SUSPEND(3, "停用"),
    ;
    @Generated
    private final Integer code;

    @Generated
    private final String desc;

    @Nullable
    @Generated
    public static Status fromCodeNullable(@Nullable Integer code) {
        if (code == null) {
            return null;
        }
        for (Status e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        return null;
    }

    @Generated
    public static Status fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("code 不能为 null");
        }
        for (Status e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("未知枚举值: " + code);
    }
}
```

**lombok=false（默认，手写私有构造器 + `public getCode()/getDesc()` 成员，均打 `@Generated`）**：

```java
package com.demo.enums;

import javax.annotation.processing.Generated;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum Status {
    @Generated
    INIT(1, "初始"),
    @Generated
    ACTIVE(2, "活跃"),
    @Generated
    SUSPEND(3, "停用"),
    ;
    @Generated
    private final Integer code;

    @Generated
    private final String desc;

    @Generated
    private Status(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Generated
    public Integer getCode() {
        return code;
    }

    @Generated
    public String getDesc() {
        return desc;
    }

    @Nullable
    @Generated
    public static Status fromCodeNullable(@Nullable Integer code) {
        if (code == null) {
            return null;
        }
        for (Status e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        return null;
    }

    @Generated
    public static Status fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("code 不能为 null");
        }
        for (Status e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("未知枚举值: " + code);
    }
}
```

（模板 = 真实产物形态：enum 常量/字段/构造器/getter/fromCode 每个成员前各有 `@Generated` 行；import 随产物包与 `annotations.nullable` 配置变化——`@Nullable` 为缺省 checkerframework qual 的示意，包声明/类名随实际列与命名策略。）

公共约定：
- 两形态**方法面一致**：均私有构造器（参数序 = 字段序 code, desc）+ `getCode()`/`getDesc()` + 反查方法对 → converter 恒用 `.getCode()` 与 `.fromCode(...)`（见「跨产物桥接」）。
- **反查方法对**（与 lombok 形态无关，恒生成）：
  - `fromCodeNullable(@Nullable T code)`（返回标 `@Nullable`）：null 入参 → null；遍历 `Objects.equals(e.code, code)` 命中返回常量；不命中 → **返回 null**（不抛异常）。空常量类 → 恒返回 null。
  - `fromCode(T code)`（无 `@Nullable`，返回非 null 契约）：null 入参 → 抛 `IllegalArgumentException("code 不能为 null")`；不命中 → 抛 `IllegalArgumentException("未知枚举值: " + code)`。空常量类 → 恒抛未知枚举值。
  - **`@Nullable` FQN = `ctx.getNullableAnnotation()`**（全局键 `annotations.nullable`，用户可配任意注解 FQN，如 `javax.annotation.Nullable`；缺省 checkerframework qual，方案 L/M 取舍），标在方法返回与参数上；生成文件带该注解 import。
  - body 统一 `Objects.equals` 遍历（方案 D/E 取舍；一套 body 覆盖 String/Integer/Long）。
- 打印格式与 `JavaCodegen` 实际输出一致：常量逐行 `,` 结尾 + 独立 `;` 行；常量多参 init 走 `SourceExpr("(...)")`（tree 已验证支持，`JavaCodegenTest.enums` 实证）。
- 空枚举类：无常量但字段/成员仍在 → tree 需保证 `enum { ; ... }` 输出（`printEnumConstants` 空常量且类体有成员时补 `;`，JavaCodegen 小修 + JavaCodegenTest 用例）。
- 常量命名/类名沿用 `markGenerated`，字段/方法成员照常打 `@Generated`。**注意**：`markGenerated` 遍历 fields（**含 enum 常量**，`Class.enumConstant` 即入 fields 列表）与 methods 全量打 `@javax.annotation.processing.Generated`——真实产物中每个常量/字段/方法前各有一行 `@Generated`（上方模板即真实形态）；该注解是 reconcile「工具拥有 vs 用户手写」的 ownership 标记：带它才被增/删/改（含常量 init 原位替换），不带则永不触碰（见「用户手写代码保留契约」）。lombok=true 形态下构造器/getter 由 lombok 生成、不在 reconcile 范围。

### 跨产物视图与桥接（`enums=true` 接入）

- `Column.isEnumColumn()` 作为唯一判定源；`PojoGenerator.fieldType` L114 判定 `enums && getEnumValues()非空` → `enums && isEnumColumn()`。
- `ConverterGenerator`：extraImports 与 toMethod 的列判定改 `isEnumColumn()`；enum 视图判定改为「fieldType == `enumPackage + "." + ctx.enumClassName(column)`」精确比对（弃 `!= String` 启发式，修数字列误判）；`enumPackage == null` 时跳过该列桥接（无 enum 产物则无枚举视图可言）。
- 桥接表达式（`Expr.nullSafe` 语义沿用）：
  - enum 视图 → 标量视图：`getter == null ? null : getter.getCode()`（`.value()` 时代的 nullSafe + value() 平移为 getCode()）；
  - 标量视图 → enum 视图：`getter == null ? null : Xxx.fromCode(getter)`（**保持旧 fromValue 语义：null 短路、未知 code 抛错**；不直接裸调 fromCodeNullable，避免转换静默吞掉脏数据。宽松反查留给用户层自行调用 fromCodeNullable）。
- `TypeMapper.resolveType` **不变**：原生 SQL-enum 列标量视图恒 String（L134 保留）；`@enum` 列标量视图 = 自然映射（tinyint→Integer 等，po 等无 enums 产物得到数字类型而非 String）。差异入文档。
- 影响链确认（评审核验）：Mapper/XML/Repository/RepositoryImpl 的参数与返回类型全走 `ctx.typeOf` → 各生成器 base `fieldType` → `TypeMapper.resolveType`，resolveType 本次不变 → 这些产物视图对 `@enum` 数字列自动为自然映射（Integer），无需改动、不构成遗漏消费点。

### 消费点开关迁移

| 消费点 | 现开关 | 新开关 |
|---|---|---|
| `EnumGenerator.generate/shouldGenerate` | `getEnumValues()` 非空 | `isEnumColumn()`（`@enum` 列空项也要生成骨架 → shouldGenerate 需含之） |
| `PojoGenerator.fieldType` | `enums && getEnumValues()` 非空 | `enums && isEnumColumn()` |
| `ConverterGenerator` L84/L136 | `getEnumValues()` 非空 | `isEnumColumn()` + enum 包存在 |
| `TypeMapper.resolveType` L134 | 不变（仅原生 SQL-enum 恒 String） | 不变 |
| `TableContext.enumClassName` | `@as` > `naming.enumClassName` | `@as` > `@enum` 值 > `naming.enumClassName`（裸 `@enum` 与 SQL-enum 列同走命名策略，无第二条轨道） |
| `AbstractJavaGenerator.reconcileFields` | 签名 = 类型-only；同名成员无 @Generated 匹配 → addField | **enum 常量签名 = 类型 + init 规范化文本**（变则原位 `replaceField`）；普通字段语义不变；期望成员名与既有**非 @Generated** 成员重复 → 跳过新增 + warning（防重复常量/字段） |
| `JavaTreeConverter.visitClass` | 所有 Variable 成员标 FIELD | enum 类常量成员标 `VariableKind.ENUM_CONSTANT`（两侧 kind 对称） |
| `JavaCodegen` | 空常量直接 return 不打 `;`；常量 init 变换内嵌 visitEnumConstants | 空常量且类体有成员补 `;`；抽 `enumConstantInitText` 公共 helper（打印与 reconcile 签名同源） |
| `Class`（tree） | addField 追加 / removeField 按 equals 删 | 新增 `replaceField(old, new)` 原位替换（保持声明顺序） |

### 删除/升级语义（沿用现状 + 记录限制）

- 整表无枚举列 → `shouldGenerate=false` → 删当前文件（同现状）；单列 `@enum` 移除/列删除后旧文件不自动清理 → **已知限制扩展**（现状对 SQL-enum 列同样如此，architecture L162）。
- **需删文件重生成的情形收窄为三类**（其余枚举项变化均增量同步）：SQL-enum 旧模板升级（模板/成员整体换代，类级注解不 reconcile）；`lombok` 选项翻转（`true→false` 残留 `@Getter`/`@RequiredArgsConstructor` 类注解 + 手写 getter 重名编译失败；`false→true` getter 被删但注解不补）；comment 中枚举项**顺序重排**（按名 reconcile 不追序，需删文件重生成以对齐 values() 序；显式保持 name 顺序稳定则不受影响）。
- 常量 init 值变更（同名 code/desc 改）→ v7 起**增量原位替换**（见「增量更新语义」），不再属删文件场景。

### 评审焦点（默认值已定，实现前请确认）

1. SQL-enum 列项源以 `enum(...)` 字面量为主、comment 列表仅按 code 匹配补 desc/name；无列表时 desc=`""`，**不因无列表生成空类**。
2. 产物 `lombok` 选项默认 `false`（与 pojo 显式开启同语义）；`true` → `@Getter` + `@RequiredArgsConstructor`；`false` → 手写私有构造器 + `getCode()/getDesc()`。选项翻转需删文件重生成。
3. 枚举类名：`@as` > `@enum` 值 > `naming.enumClassName`（裸 `@enum` 与 enum column **同一命名函数**，受 `naming.enum.style` 配置驱动；无新增命名方法、无第二套规则）。
4. 反查方法对恒生成（与 lombok 形态无关）；**`@Nullable` FQN 由用户经全局 `annotations.nullable` 配置**（缺省 checkerframework qual）——生成枚举类将**恒带**该注解（区别于 pojo 的 jsr305 可选）：消费项目需具备**所配注解**的依赖（缺省 = checker-qual，或按需配 `javax.annotation.Nullable` 等并加对应依赖）；README 写明配置方式与依赖提示。
5. 语义错误 fail-fast 抛错；结构不符文本 warning 跳过。
6. **ALTER DDL 增量更新**：增/删/改名/同名 code/desc 值改均成员级同步（v7：常量签名纳入 init + 原位替换）；类级注解、lombok 形态、项顺序重排三类仍须删文件重生成。
7. `@enum`×`@type` 同列 fail-fast；同表枚举类名撞名 fail-fast；跨表同包撞名记已知限制（显式 `@enum:名`/`@as` 可避撞）。

## 类职责与交互（中/大改动必填）

- `io.github.heyuch.codegen.core.annotation.EnumHandler`（新）— 内置注解 `@enum`（仅 COLUMN）：值写入列 `meta["enum"]`，无值写 `Boolean.TRUE` 占位。实现时 javadoc 一句话职责。依赖 model.Meta；被 AnnotationRegistry.builtin 注册。
- `io.github.heyuch.codegen.core.model.Column`（改）— 加 `isEnumColumn()`（=`isEnum() || meta 含 "enum"`）与 javadoc 说明 meta `"enum"` 键语义；`isEnum()` 保留。被 gen 各消费点依赖。
- `io.github.heyuch.codegen.core.model.EnumItem`（新）— 枚举项值对象：rawCode/desc/name(@Nullable)，纯字段不可变。仅被 parser 与 EnumGenerator 消费。
- `io.github.heyuch.codegen.core.model.EnumCommentParser`（新）— comment → `List<EnumItem>`（grammar 见方案）；结构不符含 `=` token 记 warning 跳过。仅被 EnumGenerator 消费（未来 mapper/XML 视图可复用）。
- `io.github.heyuch.codegen.core.gen.EnumGenerator`（改）— 列 kind 分派项源/类型/常量名，code/desc 模板生成（按产物 `lombok` 选项两形态 + 反查方法对，`@Nullable` 读 `ctx.getNullableAnnotation()`）；类名/命名/删除语义不变；同表类名撞名 fail-fast。核心改动。
- `io.github.heyuch.codegen.core.gen.PojoGenerator`（改）— `fieldType` 的 enums 判定开关换 `isEnumColumn()`（一行）。
- `io.github.heyuch.codegen.core.gen.ConverterGenerator`（改）— 枚举列判定、enum 视图精确比对、桥接 `fromCode`（nullSafe）/`getCode`（nullSafe）。
- `io.github.heyuch.codegen.core.gen.TableContext`（改）— `enumClassName` 增 `@enum` 值优先级（String 才取；`@as` 仍最高）。
- `io.github.heyuch.codegen.core.gen.AbstractJavaGenerator`（改）— `signature(Variable)` 对 enum 常量纳入 init 规范化文本；`reconcileFields` 常量替换走原位 `replaceField`；期望成员名与既有非 @Generated 成员重复时跳过新增 + warning。
- `io.github.heyuch.codegen.tree.JavaTreeConverter`（改）— enum 类常量成员标 `VariableKind.ENUM_CONSTANT`。
- `io.github.heyuch.codegen.tree.JavaCodegen`（改）— `printEnumConstants` 空常量且类体有成员补 `;`；抽 `enumConstantInitText` 公共 helper（打印与签名同源）；参数级注解打印若有缺失一并补。
- `io.github.heyuch.codegen.tree.Class`（改）— 新增 `replaceField(old, new)` 原位替换。

交互链：DDL comment → `AnnotationProcessor`（EnumHandler 写 meta "enum"）→ `Column.isEnumColumn()` 供 EnumGenerator（出文件）/PojoGenerator（fieldType 视图）/ConverterGenerator（import + 桥接）三处消费；枚举项只在 EnumGenerator 内经 `EnumCommentParser` 展开为常量；枚举类名由 `TableContext.enumClassName`（@as / @enum 值 / naming 策略）单点产出；`@Nullable` FQN 由 `TableContext.getNullableAnnotation()` 单点产出（全局 `annotations.nullable`）；枚举类再生成走 `AbstractJavaGenerator` reconcile——常量签名含 init（原位替换），方法按签名含 body 替换，类级注解/形态不动。

## 对账与冲突清单（独立评审输出 + 用户调整 → 处置）

- 裸 `@enum` 无值若照 AsHandler 写法会因 `Meta.put(k,null)` 删键而静默失效 → **已调整**：EnumHandler 显式 `Boolean.TRUE` 占位 + `isEnumColumn`/类名提取判读规则 + 验收补裸 `@enum` 用例。
- String code 为纯数字/含 `-` 的缺 name 场景命名未定义 → **已调整**：写明清洗后判重、`_1` 等怪异名合法、加表驱动单测。
- 设计模板示例与实际打印器格式不符（缺 `;`）→ **已调整**：模板与 README 示例统一为打印器实际输出（`,` 行 + 独立 `;` 行）。
- `@enum`×`@type` 同列在精确比对下静默丢桥接 → **已调整**：fail-fast 报错 + 验收错误用例。
- 同表/跨表枚举类名撞名无防护 → **已调整（部分）**：同表 fail-fast；跨表同包记已知限制（生成器无状态约束，与旧 `@as` 同族，本期不做全局去重）。
- 空骨架自补常量与后续 reconcile 增常量重名 → **已调整**：`AbstractJavaGenerator.reconcileFields` 加「非 @Generated 同名成员已存在则跳过新增 + warning」（顺带修正 pojo 手写字段同族隐患）；验收补 E2E。
- `lombok` 选项/翻转与类级注解不 reconcile 矛盾 → **已调整**：选项默认 false、双形态按选项生成；翻转失效写入已知限制 + 迁移指引（与焦点⑥合并）。
- 验收「常量按名增删」对 init 静默过期缺说明 → **已调整（v7）**：不再过期——同名 code/desc 值改纳入签名原位替换；需删文件场景收窄为模板升级/lombok 翻转/顺序重排三类。
- mapper 直连 entity 时数字 code 与 `Enum.name()` 的 MyBatis 运行时风险 → **已调整**：README 补一句「enum 语义走 po→converter→entity 链路；mapper 直连需自配 typeHandler」（与 SQL-enum 现状同族）。
- parser 小边界（前导零、Java 关键字、全角括号）→ **已纳入**：规则与单测补全。
- 用户提问（ALTER 枚举项如何更新）→ **已纳入（v7，方案 N/O/P）**：成员级 reconcile + 常量签名含 init 原位替换；类级/形态/顺序仍删文件重生成。
- 用户调整 ①（lombok 选项双形态）→ **已纳入**（方案 G）；③（反查方法对）→ **已纳入**（方案 K）；④（`@Nullable` 可配置）→ **已纳入**（方案 M：复用全局 `annotations.nullable`，非硬编码）。
- 用户调整 ②（裸 `@enum` 命名）：v3 曾按「固定 {TableName}{ColumnName} + 新增命名方法」处理 → **v5 按用户简化撤回**：裸 `@enum` 与 enum column **同一命名函数**（`naming.enum.style` 驱动），无新增命名方法（方案 H 选定 / I 否决）。
- 其余核验（消费点行号、tree 能力、reconcile 边界、无独立 EnumGeneratorTest、cli/plugin 无 enum 夹具）→ 无冲突，记录备查。

## 改动文件与影响面

- 改动（core main）：`annotation/EnumHandler.java`(新)、`annotation/AnnotationRegistry.java`、`model/Column.java`、`model/EnumItem.java`(新)、`model/EnumCommentParser.java`(新)、`gen/EnumGenerator.java`、`gen/PojoGenerator.java`、`gen/ConverterGenerator.java`、`gen/TableContext.java`、`gen/AbstractJavaGenerator.java`（**不加** naming/config 改动——命名复用既有 `NamingService.enumClassName`，`@Nullable` 复用既有 `annotations.nullable`）
- 改动（tree main）：`tree/JavaTreeConverter.java`、`tree/JavaCodegen.java`、`tree/Class.java`
- 测试：tree `JavaCodegenTest`（空常量 `;`、`replaceField`、round-trip 常量 kind/init、参数级注解若缺失）、`JavaTreeConverterTest`（如有）；core `EndToEndTest`（enum 文本/converter/字段断言重写 + `@enum` 场景）、`ParameterizedArtifactsTest`（字段类型断言核对）、`DruidDdlParserTest`/`AnnotationProcessorTest`（@enum 解析入 meta）、新增 `EnumCommentParser` 单测与 `@enum` 端到端用例（core 现无独立 EnumGeneratorTest，视需要建）
- 影响：所有生成/消费 enum 语义的既有 SQL-enum 产物（**breaking**：`value()`/`fromValue`/单 String 字段 → `code`/`desc` + getter + `fromCodeNullable`/`fromCode`，形态随 `lombok` 选项）；Converter 生成代码调用点变化（`.value()`→`.getCode()`、`.fromValue()`→`.fromCode()`，均 nullSafe 包裹）；**生成枚举类恒引用 `annotations.nullable` 配置的注解（缺省 checkerframework qual）**——消费项目需具备**所配注解**的依赖（可配置替换为 `javax.annotation.Nullable` 等并加对应依赖）；reconcile 语义扩展（enum 常量 init 变更现在会替换——既有产物若曾被手改过 init 会被还原为 DDL 值，属期望行为）；`naming.enum.style` 对裸 `@enum` 与 SQL-enum 列同等生效（行为不变）。grep 可验证：`fromValue`、`.value()`、`getEnumValues()`、`isEnum()`、`constantName(`、`fromValueBody`、`enumClassName`、`getNullableAnnotation`、`signature(Variable`、`reconcileFields`、EndToEndTest/ParameterizedArtifactsTest 断言文本。
- 文档影响：**是**（改记忆文档）——`docs/architecture.md`（生成器表 enum 行、注解表加 enum 行、config 特性表加 enum.lombok、reconcile/增量更新语义、已知限制/契约）、`docs/changes/README.md`（索引行，收尾追加）、`docs/glossary.md`（@enum/枚举列条目）；`README.md`（用户手册 @enum DDL 示例 + 模板 + `enum.lombok` 选项 + `annotations.nullable` 配置方式与依赖提示 + 升级破坏提示 + typeHandler 桥接说明 + ALTER 增量更新说明）。收尾需跑「记忆文档自检用例集」。

## 验收标准（完成 = 下列行为全部成立）

> 1. 给定 `status tinyint unsigned comment '状态 1=初始(INIT) 2=活跃(ACTIVE) 3=停用(SUSPEND) @enum:Status'` + enum 产物配置 → 默认（lombok=false）生成 `public enum Status`：常量 `INIT(1, "初始")`/`ACTIVE(2, "活跃")`/`SUSPEND(3, "停用")`、`private final Integer code` + `private final String desc`、手写私有构造器、`public Integer getCode()`/`public String getDesc()`；输出无「未知 DDL 注解 @enum」warning。
> 2. 给定 `type varchar(20) comment '类型 NORMAL=普通 VIP=高级 TRIAL=试用 @enum:Type'` → `Type` 枚举：常量 `NORMAL("NORMAL", "普通")` 等（code String、name 缺省 = code）。
> 3. 数字列 `@enum` 某项缺 name（`1=初始`）→ 常量名 `STATUS_1`；字符列缺 name → code 即常量名；显式 `(INIT)` → `INIT`（全部大写清洗）；String code `a-b` 与 `a_b` 同清洗名 → 报错。
> 4. 给定 `@enum:Status` 且无列表项 → 空枚举类骨架（无常量；有 code/desc 字段、构造、getter、反查方法对），输出可编译（含独立 `;` 行）。
> 5. 裸 `@enum` 类名与 enum column 同规则：表 `t_user` 列 `status` 裸 `@enum` → style=column（默认）类名 `Status`；`naming.enum.style=tableColumn` → `UserStatus`；带列级 `@as:UStatus` → `UStatus`（`@as` 优先）；无列表项 → 同 4 的骨架形态。
> 6. 给定 decimal/tinyint(1) 列带 `@enum`、数字列上 code 为 `abc`、code 重复（数值列含前导零按解析值判）、常量名重复、`@enum` 值为 `a.b` 或 `class`、`@enum`×`@type` 同列、同表两枚举列同名 → 均抛 `IllegalStateException` 且消息含表名与列名。
> 7. 生成的反查方法对（lombok 两形态一致）：`fromCodeNullable` 的方法与参数均带 `@Nullable`，`null` 入参与未知 code → 返回 null 不抛异常，命中 → 返回常量；`fromCode` 无 `@Nullable`，`null` 入参或未知 code → 抛 `IllegalArgumentException`，命中 → 返回常量；空常量类 → fromCodeNullable 恒 null、fromCode 恒抛。
> 8. `@Nullable` 注解可配置：缺省（不配 `annotations.nullable`）→ 生成的 `@Nullable` 为 `org.checkerframework.checker.nullness.qual.Nullable`（含 import）；配置 `annotations.nullable=javax.annotation.Nullable` → 生成文件里 `@Nullable` 与 import 均改为该 FQN；pojo jsr305 特性不受影响（同源同键）。
> 9. entity 开 `enums=true` 且含 `@enum` 数字列 → entity 字段类型为枚举类；同表 po 未开 enums → 字段类型 `Integer`；converter 生成 `... == null ? null : Xxx.fromCode(...)` 与 `... == null ? null : ....getCode()` 空安全表达式（保持未知 code 抛错语义）。
> 10. 给定 `gender enum('male','female') comment '性别 male=男(MALE) female=女(FEMALE)'` → `Gender` 常量 `MALE("male", "男")`；comment 无列表 → 常量 `MALE("male", "")`；两者均无 `value()`/`fromValue`。
> 11. **ALTER 增量更新**：同一表先以 `1=初始(INIT) 2=活跃(ACTIVE)` 生成，改 DDL 为 `1=待处理(INIT) 2=活跃(ACTIVE) 4=审核(AUDIT)`（改 INIT 的 desc、删掉原 SUSPEND、增 AUDIT——含 SUSPEND 的场景一并验证）→ 再生成：INIT 的 init 原位更新为新值且**声明顺序不变**、删项消失、增项追加；连续第三次执行无变化（幂等）；文件里用户手写常量/方法不动。comment 列表**顺序重排**（项全保留仅换序）→ 枚举常量顺序不追（已知限制，文档注明删文件重生成）。
> 12. 同一 DDL 连续执行两次 → 第二次所有文件无变化（幂等）；既有含 `@Generated` 枚举文件的列枚举项增删 → 常量按名增删，用户手写成员不动；空骨架文件用户手写常量后 DDL 再补同名列表项 → 不再生成重复常量（保留用户那份 + warning）。
> 13. 表注释/索引注释出现 `@enum` → warning 忽略，不中断（目标位置不符）。
> 14. enum 产物配 `lombok=true` → 类级 `@Getter` + `@RequiredArgsConstructor`，无常量外手写构造器/getter 成员，产物含反查方法对；converter 仍生成 `.getCode()`（两形态方法面一致）。`lombok` 选项翻转不保证（已知限制，需删文件重生成）。

## 验证

- 测试（由验收逐条派生）：`JavaCodegenTest`（空常量 `;`、`Class.replaceField` 原位替换、常量 round-trip kind/init 规范化、参数级注解打印若实现期发现缺失则补用例）；`EnumCommentParserTest`（grammar 正反例：缺 name/空项/结构不符/全角括号可过半角丢弃/前导零）；`EnumHandler`/`AnnotationProcessorTest`（meta 占位与位置 warning）；`EndToEndTest` 重写 + 新增 `@enum` 端到端用例（1-5/7-11/14，含 lombok 两形态、裸 @enum 类名、反查方法对、`annotations.nullable` 缺省与自定义两组、**ALTER 增量更新流程**）；语义报错用例（6）；幂等/手写保留/重名跳过沿用与扩展 reconcile 测试（12）。PIT 关注：反查方法对 null/not-found/hit/default 全分支、parser grammar 分支、常量名缺省规则分支、错误路径、reconcile 常量签名含 init 的分支。
- 验证命令：`JAVA_HOME=/opt/homebrew/opt/openjdk@11 mvn clean test` 全绿（spotless/checkstyle/error-prone/checkerframework/spotbugs/jacoco 全进）；`new-change.sh check` +（改记忆文档后）`check-docs`。
