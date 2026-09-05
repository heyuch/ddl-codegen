package hyc.codegen.core.gen;

import java.nio.file.Path;

import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.model.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EnumGenerator golden 契约测试：SQL-enum 列（无列表/列表补 desc-name）、@enum 数字/字符列、
 * 裸 @enum 命名（column/tableColumn）、空骨架、lombok 两形态、bigint L 后缀，及校验错误组。
 * 期望产物见 {@code fixtures/gen/enum/**}。
 */
class EnumGeneratorTest {

    @TempDir
    @Nullable
    Path temp;

    private void assertEnumError(GeneratorTestSupport support, DdlConfig config, String ddl,
            String columnName, String messagePart) {
        Schema schema = new Schema();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> support.generate(config, schema, ddl));
        String message = e.getMessage();
        assertTrue(message != null && message.contains(columnName), message == null ? "无异常消息" : message);
        assertTrue(message != null && message.contains(messagePart), message == null ? "无异常消息" : message);
    }

    @Test
    void bareEnumTableColumnStyleNaming() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = enumConfig(support);
        config.setEnumStyle("tableColumn");
        generate(support, config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始 2=活跃 @enum',\n"
                + "    primary key (id))");
        support.assertGolden("enum/table-column-style");
    }

    @Test
    void bigintCodeLiteralGetsSuffix() throws Exception {
        GeneratorTestSupport support = support();
        generate(support, enumConfig(support), "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    big bigint not null comment '大数 1=一(X) 3000000000=十亿(Y) @enum:Big',\n"
                + "    primary key (id))");
        support.assertGolden("enum/bigint-suffix");
    }

    @Test
    void emptyItemListSkeletonEnum() throws Exception {
        GeneratorTestSupport support = support();
        generate(support, enumConfig(support), "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 @enum:Status',\n"
                + "    primary key (id))");
        support.assertGolden("enum/empty-skeleton");
    }

    private DdlConfig enumConfig(GeneratorTestSupport support) {
        DdlConfig config = support.tableConfig();
        support.addArtifact(config, "enum", "enum", "com.demo.enums", "");
        return config;
    }

    private void generate(GeneratorTestSupport support, DdlConfig config, String ddl) {
        support.generate(config, ddl);
    }

    @Test
    void lombokModeAnnotations() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = enumConfig(support);
        support.artifact(config, "enum").putOption("lombok", "true");
        generate(support, config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始(INIT) 2=活跃(ACTIVE) @enum:Status',\n"
                + "    primary key (id))");
        support.assertGolden("enum/lombok-mode");
    }

    @Test
    void numericAnnotatedEnumExplicitAndDefaultNames() throws Exception {
        GeneratorTestSupport support = support();
        generate(support, enumConfig(support), "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint unsigned not null comment '状态 1=初始(INIT) 2=活跃 3=停用(SUSPEND) @enum:Status',\n"
                + "    primary key (id))");
        support.assertGolden("enum/numeric-annotated");
    }

    @Test
    void sqlEnumColumnWithListFillsDescAndName() throws Exception {
        GeneratorTestSupport support = support();
        generate(support, enumConfig(support), "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    gender enum('male','female') comment '性别 male=男(MALE) female=女(FEMALE)',\n"
                + "    primary key (id))");
        support.assertGolden("enum/sql-enum-with-list");
    }

    @Test
    void sqlEnumColumnWithoutListDescEmpty() throws Exception {
        GeneratorTestSupport support = support();
        generate(support, enumConfig(support), "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    gender enum('male','female') comment '性别',\n"
                + "    primary key (id))");
        support.assertGolden("enum/sql-enum-no-list");
    }

    @Test
    void stringBareEnumCodeAsName() throws Exception {
        GeneratorTestSupport support = support();
        generate(support, enumConfig(support), "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    type varchar(20) not null comment '类型 NORMAL=普通 TRIAL=试用 @enum',\n"
                + "    primary key (id))");
        support.assertGolden("enum/string-bare");
    }

    private GeneratorTestSupport support() {
        return new GeneratorTestSupport(temp, new EnumGenerator());
    }

    @Test
    void validationErrorsFailFast() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = enumConfig(support);
        assertEnumError(support, config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    amount decimal(10,2) not null comment '金额 1=一 @enum:Amount',\n"
                + "    primary key (id))", "amount", "@enum 不受支持");
        assertEnumError(support, config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 abc=坏 @enum:Status',\n"
                + "    primary key (id))", "status", "不是整数");
        assertEnumError(support, config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始 01=重复 @enum:Status',\n"
                + "    primary key (id))", "status", "code 重复");
        assertEnumError(support, config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    kind varchar(20) not null comment '类型 a-b=一 a_b=二 @enum:Kind',\n"
                + "    primary key (id))", "kind", "常量名重复");
        assertEnumError(support, config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始 @enum:a.b',\n"
                + "    primary key (id))", "status", "不是合法简单类名");
        assertEnumError(support, config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始 @enum:Status @type:java.lang.String',\n"
                + "    primary key (id))", "status", "@enum 与列级 @type");
        assertEnumError(support, config, "create table t_user ("
                + " id bigint not null auto_increment comment '主键',"
                + " a tinyint not null comment 'A 1=一 @enum:Both',"
                + " b varchar(20) not null comment 'B x=二 @enum:Both',"
                + " primary key (id))", "a", "相同枚举类名");
    }

}
