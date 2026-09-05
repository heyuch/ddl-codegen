package hyc.codegen.core.gen;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.ddl.ApplyResult;
import hyc.codegen.core.ddl.DdlParser;
import hyc.codegen.core.ddl.DruidDdlParser;
import hyc.codegen.core.ddl.StatementApplier;
import hyc.codegen.core.io.ChangeReport;
import hyc.codegen.core.model.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code @enum} 注解枚举列端到端：code/desc 模板两形态、裸 @enum 命名、空骨架、
 * ALTER 增量更新与用户代码保留、语义 fail-fast、可配置 @Nullable。
 */
// 门面式集成测试：引用类型数 ≈ 被测管线涉及类数（§6 元素驱动）
@SuppressWarnings({"ClassDataAbstractionCoupling", "ClassFanOutComplexity", "MethodLength", "ExecutableStatementCount"})
class EnumAnnotationTest {

    @TempDir
    @Nullable
    Path temp;

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private void add(DdlConfig config, String name, String generator, String pkg, String suffix) {
        ArtifactConfig artifact = new ArtifactConfig(name);
        artifact.setGenerator(generator);
        artifact.setModule("");
        artifact.setPkg(pkg);
        artifact.setSuffix(suffix);
        config.addArtifact(artifact);
    }

    @Test
    void alterIncrementalSyncKeepsUserCodeAndOrder() throws Exception {
        DdlConfig config = config(false);
        Schema schema = new Schema();
        String create = "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始(INIT) 2=活跃(ACTIVE) 3=停用(SUSPEND) @enum:Status',\n"
                + "    primary key (id))";
        generate(config, schema, create);

        // 用户手写方法
        Path file = tempDir().resolve("com/demo/enums/Status.java");
        String userMethod = "\n    /** 用户手写方法 */\n    public String hello() {\n        return \"hi\";\n    }\n";
        String existing = read("com/demo/enums/Status.java");
        int lastBrace = existing.lastIndexOf('}');
        Files.write(file, (existing.substring(0, lastBrace) + userMethod + existing.substring(lastBrace))
                .getBytes(StandardCharsets.UTF_8));

        // ALTER 改 comment 枚举项：INIT desc 改、删 SUSPEND、增 AUDIT
        String alter = "alter table t_user modify column status tinyint not null "
                + "comment '状态 1=待处理(INIT) 2=活跃(ACTIVE) 4=审核(AUDIT) @enum:Status'";
        generate(config, schema, alter);
        String after = read("com/demo/enums/Status.java");
        assertTrue(after.contains("INIT(1, \"待处理\")"), after);
        assertTrue(after.contains("ACTIVE(2, \"活跃\")"), after);
        assertTrue(after.contains("AUDIT(4, \"审核\")"), after);
        assertFalse(after.contains("SUSPEND"), after);
        assertTrue(after.contains("public String hello()"), "用户手写方法应保留");
        assertTrue(after.contains("return \"hi\";"), after);
        // 顺序保持 DDL 声明序：INIT 原位更新不漂移
        assertTrue(after.indexOf("INIT(1") < after.indexOf("ACTIVE(2"), after);
        assertTrue(after.indexOf("ACTIVE(2") < after.indexOf("AUDIT(4"), after);

        // 幂等：同输入重跑无变化
        generate(config, schema, alter);
        assertEquals(after, read("com/demo/enums/Status.java"), "同输入重跑应无变化");
    }

    /** 按产物名取配置（测试约定产物已配置）。 */
    private ArtifactConfig artifact(DdlConfig config, String name) {
        ArtifactConfig artifactConfig = config.artifact(name);
        if (artifactConfig == null) {
            throw new AssertionError("产物未配置: " + name);
        }
        return artifactConfig;
    }

    private void assertEnumError(String ddl, String columnName, String messagePart) throws Exception {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> generateOnlyEnum(config(false), ddl));
        String message = e.getMessage();
        assertTrue(message != null && message.contains(columnName), message == null ? "无异常消息" : message);
        assertTrue(message != null && message.contains(messagePart), message == null ? "无异常消息" : message);
    }

    @Test
    void bareEnumNamingFollowsEnumColumnStyle() throws Exception {
        // tableColumn 风格：t_user.status → UserStatus（与 SQL-enum 列同一命名函数）
        DdlConfig config = config(false);
        config.setEnumStyle("tableColumn");
        generate(config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始 2=活跃 @enum',\n"
                + "    primary key (id))");

        // 数字列缺 name → 常量名 {列名}_{rawCode}
        String status = read("com/demo/enums/UserStatus.java");
        assertTrue(status.contains("public enum UserStatus"), status);
        assertTrue(status.contains("STATUS_1(1, \"初始\")"), status);
        assertTrue(status.contains("STATUS_2(2, \"活跃\")"), status);
        assertFalse(Files.exists(tempDir().resolve("com/demo/enums/Status.java")), "不应生成列名风格类");
    }

    private DdlConfig config(boolean withConverter) {
        DdlConfig config = new DdlConfig();
        config.setRoot(tempDir());
        config.addTableStripPrefix("t_");
        config.setTableStripShardSuffix(true);

        add(config, "entity", "pojo", "com.demo.entity", "");
        artifact(config, "entity").putOption("enums", "true");
        add(config, "enum", "enum", "com.demo.enums", "");
        add(config, "po", "pojo", "com.demo.pojo", "Po");
        if (withConverter) {
            add(config, "entityConverter", "converter", "com.demo.converter", "Converter");
            artifact(config, "entityConverter").setSource("po");
            artifact(config, "entityConverter").setTarget("entity");
        }
        return config;
    }

    @Test
    void emptyItemListGeneratesSkeletonEnum() throws Exception {
        DdlConfig config = config(false);
        generate(config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 @enum:Status',\n"
                + "    primary key (id))");

        String status = read("com/demo/enums/Status.java");
        assertTrue(status.contains("public enum Status"), status);
        assertFalse(status.contains("INIT"), status);
        assertTrue(status.contains("    ;"), status);
        assertTrue(status.contains("private final Integer code;"), status);
        assertTrue(status.contains("private final String desc;"), status);
        assertTrue(status.contains("public static Status fromCodeNullable"), status);
    }

    private ChangeReport generate(DdlConfig config, Schema schema, String ddl) {
        ApplyResult result = new StatementApplier().apply(schema, new DruidDdlParser().parse(ddl));
        CodeGenerator generator = new CodeGenerator(Arrays.asList(
                new PojoGenerator(), new EnumGenerator(), new ConverterGenerator()));
        return generator.generate(config, schema, result, Collections.emptyList());
    }

    private ChangeReport generate(DdlConfig config, String ddl) {
        Schema schema = new Schema();
        return generate(config, schema, ddl);
    }

    private ChangeReport generateOnlyEnum(DdlConfig config, String ddl) {
        DdlParser parser = new DruidDdlParser();
        Schema schema = new Schema();
        ApplyResult result = new StatementApplier().apply(schema, parser.parse(ddl));
        CodeGenerator generator = new CodeGenerator(Collections.singletonList(new EnumGenerator()));
        return generator.generate(config, schema, result, Collections.emptyList());
    }

    @Test
    void lombokModeUsesAnnotationsAndSameMethodSurface() throws Exception {
        DdlConfig config = config(true);
        artifact(config, "enum").putOption("lombok", "true");
        generate(config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始(INIT) 2=活跃(ACTIVE) @enum:Status',\n"
                + "    primary key (id))");

        String status = read("com/demo/enums/Status.java");
        assertTrue(status.contains("@Getter"), status);
        assertTrue(status.contains("@RequiredArgsConstructor"), status);
        assertTrue(status.contains("INIT(1, \"初始\")"), status);
        assertTrue(status.contains("public static Status fromCode(Integer code)"), status);
        assertFalse(status.contains("private Status(Integer code, String desc)"), status);
        assertFalse(status.contains("public Integer getCode()"), status);

        // converter 仍用 .getCode()（lombok 生成的 getter 同名，方法面一致）
        String converter = read("com/demo/converter/UserConverter.java");
        assertTrue(converter.contains("target.getStatus().getCode()"), converter);
    }

    @Test
    void longCodeLiteralGetsSuffix() throws Exception {
        DdlConfig config = config(false);
        generate(config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    big bigint not null comment '大数 1=一(X) 3000000000=十亿(Y) @enum:Big',\n"
                + "    primary key (id))");

        String big = read("com/demo/enums/Big.java");
        assertTrue(big.contains("X(1L, \"一\")"), big);
        assertTrue(big.contains("Y(3000000000L, \"十亿\")"), big);
        assertTrue(big.contains("private final Long code;"), big);
    }

    @Test
    void nullableAnnotationIsConfigurable() throws Exception {
        DdlConfig config = config(false);
        config.setNullableAnnotation("javax.annotation.Nullable");
        generate(config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始(INIT) @enum:Status',\n"
                + "    primary key (id))");

        String status = read("com/demo/enums/Status.java");
        assertTrue(status.contains("import javax.annotation.Nullable;"), status);
        assertTrue(status.contains("fromCodeNullable(@Nullable Integer code)"), status);
        assertFalse(status.contains("org.checkerframework"), status);
    }

    @Test
    void numericEnumWithViewsAndConverterBridge() throws Exception {
        DdlConfig config = config(true);
        generate(config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint unsigned not null comment '状态 1=初始(INIT) 2=活跃(ACTIVE) "
                + "3=停用(SUSPEND) @enum:Status',\n"
                + "    primary key (id))");

        String status = read("com/demo/enums/Status.java");
        assertTrue(status.contains("public enum Status"), status);
        assertTrue(status.contains("INIT(1, \"初始\")"), status);
        assertTrue(status.contains("ACTIVE(2, \"活跃\")"), status);
        assertTrue(status.contains("SUSPEND(3, \"停用\")"), status);
        assertTrue(status.contains("private final Integer code;"), status);
        assertTrue(status.contains("private final String desc;"), status);
        assertTrue(status.contains("private Status(Integer code, String desc)"), status);
        assertTrue(status.contains("public Integer getCode()"), status);
        assertTrue(status.contains("public String getDesc()"), status);
        assertTrue(status.contains("public static Status fromCodeNullable(@Nullable Integer code)"), status);
        assertTrue(status.contains("public static Status fromCode(Integer code)"), status);
        assertTrue(status.contains("org.checkerframework.checker.nullness.qual.Nullable"), status);
        assertTrue(status.contains("code 不能为 null"), status);
        assertTrue(status.contains("未知枚举值"), status);
        assertTrue(status.contains("@Generated"), status);
        // lombok=false 默认：手写构造/getter，无 lombok 注解；新模板无 value()/fromValue
        assertFalse(status.contains("@Getter"), status);
        assertFalse(status.contains("fromValue"), status);

        // enums=true 视图：entity 用枚举类；po 保持自然类型 Integer
        assertTrue(read("com/demo/entity/User.java").contains("private Status status"));
        assertTrue(read("com/demo/pojo/UserPo.java").contains("private Integer status"));

        // converter 桥接：标量→enum 用 fromCode、enum→标量用 getCode，均空安全
        String converter = read("com/demo/converter/UserConverter.java");
        assertTrue(converter.contains(
                "user.setStatus(source.getStatus() == null ? null : Status.fromCode(source.getStatus()));"), converter);
        assertTrue(converter.contains(
                "userPo.setStatus(target.getStatus() == null ? null : target.getStatus().getCode());"), converter);
    }

    private String read(String relative) throws Exception {
        return new String(Files.readAllBytes(tempDir().resolve(relative)), StandardCharsets.UTF_8);
    }

    @Test
    void semanticErrorsFailFastWithTableAndColumn() throws Exception {
        assertEnumError("create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    amount decimal(10,2) not null comment '金额 1=一 @enum:Amount',\n"
                + "    primary key (id))", "amount", "@enum 不受支持");
        assertEnumError("create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 abc=坏 @enum:Status',\n"
                + "    primary key (id))", "status", "不是整数");
        assertEnumError("create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始 01=重复 @enum:Status',\n"
                + "    primary key (id))", "status", "code 重复");
        assertEnumError("create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    kind varchar(20) not null comment '类型 a-b=一 a_b=二 @enum:Kind',\n"
                + "    primary key (id))", "kind", "常量名重复");
        assertEnumError("create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始 @enum:a.b',\n"
                + "    primary key (id))", "status", "不是合法简单类名");
        assertEnumError("create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始 @enum:class',\n"
                + "    primary key (id))", "status", "不是合法简单类名");
        assertEnumError("create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始 @enum:Status @type:java.lang.String',\n"
                + "    primary key (id))", "status", "@enum 与列级 @type");
        assertEnumError("create table t_user ("
                + " id bigint not null auto_increment comment '主键',"
                + " a tinyint not null comment 'A 1=一 @enum:Both',"
                + " b varchar(20) not null comment 'B x=二 @enum:Both',"
                + " primary key (id))", "a", "相同枚举类名");
    }

    @Test
    void skeletonUserConstantBlocksGeneratorDuplicate() throws Exception {
        DdlConfig config = config(false);
        Schema schema = new Schema();
        generate(config, schema, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 @enum:Status',\n"
                + "    primary key (id))");

        // 用户自补常量（无 @Generated，模拟空骨架后手写）
        Path file = tempDir().resolve("com/demo/enums/Status.java");
        String userConstant = "\n    PENDING(9, \"待定\"),\n";
        String existing = read("com/demo/enums/Status.java");
        int semi = existing.indexOf(';');
        Files.write(file, (existing.substring(0, semi) + userConstant + existing.substring(semi))
                .getBytes(StandardCharsets.UTF_8));

        // DDL 再补同名列表项 → 不生成重复常量（保留用户那份）
        String alter = "alter table t_user modify column status tinyint not null "
                + "comment '状态 9=待定(PENDING) @enum:Status'";
        generate(config, schema, alter);
        String after = read("com/demo/enums/Status.java");
        assertEquals(1, countOccurrences(after, "PENDING(9, \"待定\")"), "不应生成重复常量");
    }

    @Test
    void stringBareEnumUsesCodeAsConstantName() throws Exception {
        DdlConfig config = config(false);
        generate(config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    type varchar(20) not null comment '类型 NORMAL=普通 VIP=高级 TRIAL=试用 @enum',\n"
                + "    primary key (id))");

        String type = read("com/demo/enums/Type.java");
        assertTrue(type.contains("public enum Type"), type);
        assertTrue(type.contains("NORMAL(\"NORMAL\", \"普通\")"), type);
        assertTrue(type.contains("VIP(\"VIP\", \"高级\")"), type);
        assertTrue(type.contains("TRIAL(\"TRIAL\", \"试用\")"), type);
        assertTrue(type.contains("private final String code;"), type);
        assertTrue(read("com/demo/entity/User.java").contains("private Type type"));
    }

    private Path tempDir() {
        Path dir = temp;
        if (dir == null) {
            throw new AssertionError("JUnit 未注入 @TempDir");
        }
        return dir;
    }

}
