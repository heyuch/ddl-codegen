package hyc.codegen.core.gen;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import javax.lang.model.element.Modifier;

import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.ddl.ApplyResult;
import hyc.codegen.core.ddl.DdlParser;
import hyc.codegen.core.ddl.DruidDdlParser;
import hyc.codegen.core.ddl.StatementApplier;
import hyc.codegen.core.io.ChangeReport;
import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Schema;
import hyc.codegen.tree.Class;
import hyc.codegen.tree.TypeReference;
import hyc.codegen.tree.Variable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 成员级 reconcile 生命周期测试：create → alter（增/删/改类型）→ 用户代码保留 → drop。
 * <p>
 * 覆盖字段与方法两级 reconcile（PIT 变异测试曾抓出方法级 reconcile 未被测试）。
 */
class ReconcileLifecycleTest {

    // JUnit @TempDir 注入，语法层不保证非 null：标 @Nullable，使用点经 tempDir() 显式校验。
    @TempDir
    @Nullable
    Path temp;

    /** @TempDir 注入目录：JUnit 保证注入，但语法层不保证非 null（标注 @Nullable），使用点经此显式校验。 */
    private Path tempDir() {
        Path dir = temp;
        if (dir == null) {
            throw new AssertionError("JUnit 未注入 @TempDir");
        }
        return dir;
    }

    /** 测试生成器：每列一个 private 字段 + 一个由表名驱动的 describe() 方法（覆盖方法级 reconcile）。 */
    static final class TestGenerator extends AbstractJavaArtifactGenerator {

        @Override
        public String kind() {
            return "test";
        }

        @Override
        protected void buildClass(Class.Builder builder, TableContext ctx, GenerationContext gctx) {
            for (Column column : ctx.columns()) {
                builder.field(Variable.builder()
                        .modifiers(Modifier.PRIVATE)
                        .type(new TypeReference(ctx.typeOf(column)))
                        .name(ctx.fieldName(column))
                        .build());
            }
            builder.method(hyc.codegen.tree.Method.builder()
                    .modifiers(Modifier.PUBLIC)
                    .returnType(new TypeReference("java.lang.String"))
                    .name("describe")
                    .body("return \"" + ctx.getTable().getName() + "\";")
                    .build());
        }

    }

    private DdlConfig config() {
        DdlConfig config = new DdlConfig();
        config.setRoot(tempDir());
        ArtifactConfig artifact = new ArtifactConfig("test");
        artifact.setGenerator("test");
        artifact.setModule("");
        artifact.setPkg("com.test");
        config.addArtifact(artifact);
        return config;
    }

    private ChangeReport generate(DdlConfig config, Schema schema, String ddl) {
        DdlParser parser = new DruidDdlParser();
        ApplyResult result = new StatementApplier().apply(schema, parser.parse(ddl));
        CodeGenerator generator = new CodeGenerator(Collections.singletonList(new TestGenerator()));
        return generator.generate(config, schema, result, Collections.emptyList());
    }

    private String content() throws Exception {
        return new String(Files.readAllBytes(tempDir().resolve("com/test/User.java")), StandardCharsets.UTF_8);
    }

    private boolean fileExists() {
        return Files.isRegularFile(tempDir().resolve("com/test/User.java"));
    }

    @Test
    void createThenAlterThenDrop() throws Exception {
        DdlConfig config = config();

        // create
        Schema schema = new Schema();
        String create = "create table user (id bigint primary key, name varchar(50) not null comment '用户名')";
        generate(config, schema, create);
        assertTrue(fileExists());
        String created = content();
        assertTrue(created.contains("package com.test;"), created);
        assertTrue(created.contains("private Long id"));
        assertTrue(created.contains("private String name"));
        assertTrue(created.contains("@Generated"));
        assertTrue(created.contains("public String describe()"), created);
        assertTrue(created.contains("class User"));

        // alter add column：字段/方法级 reconcile + 包保留（PIT 抓到的缺口）
        String alter = "alter table user add column email varchar(100) comment '邮箱'";
        generate(config, schema, alter);
        String afterAdd = content();
        assertTrue(afterAdd.contains("package com.test;"), afterAdd);
        assertTrue(afterAdd.contains("private String email"));
        assertTrue(afterAdd.contains("private String name"));
        assertTrue(afterAdd.contains("public String describe()"), afterAdd);

        // 幂等：同样输入重跑 → 无变化
        generate(config, schema, alter);
        assertEquals(afterAdd, content(), "同输入重跑应无变化");

        // alter drop column
        String dropColumn = "alter table user drop column name";
        generate(config, schema, dropColumn);
        String afterDrop = content();
        assertFalse(afterDrop.contains("private String name"));
        assertTrue(afterDrop.contains("private String email"));
        assertTrue(afterDrop.contains("public String describe()"), afterDrop);

        // 类型变化：email varchar → bigint
        String changeType = "alter table user modify column email bigint";
        generate(config, schema, changeType);
        String afterType = content();
        assertTrue(afterType.contains("private Long email"));
        assertFalse(afterType.contains("private String email"));

        // drop table
        String dropTable = "drop table user";
        generate(config, schema, dropTable);
        assertFalse(fileExists());
    }

    @Test
    void userWrittenMembersArePreserved() throws Exception {
        DdlConfig config = config();
        Schema schema = new Schema();
        generate(config, schema, "create table user (id bigint primary key)");

        // 用户手写一个方法（模拟用户编辑：插入到类结尾前）
        Path file = tempDir().resolve("com/test/User.java");
        String userMethod = "\n    /** 用户手写方法 */\n    public String hello() {\n        return \"hi\";\n    }\n";
        String existing = content();
        int lastBrace = existing.lastIndexOf('}');
        String edited = existing.substring(0, lastBrace) + userMethod + existing.substring(lastBrace);
        Files.write(file, edited.getBytes(StandardCharsets.UTF_8));

        // alter 增加列 → 用户方法必须保留
        generate(config, schema, "alter table user add column name varchar(50)");
        String after = content();
        assertTrue(after.contains("public String hello()"));
        assertTrue(after.contains("return \"hi\";"));
        assertTrue(after.contains("private String name"));
    }

    @Test
    void renameTableMovesFiles() throws Exception {
        DdlConfig config = config();
        Schema schema = new Schema();
        generate(config, schema, "create table user (id bigint primary key)");
        assertTrue(fileExists());

        generate(config, schema, "alter table user rename to account");
        assertFalse(fileExists());
        assertTrue(Files.isRegularFile(tempDir().resolve("com/test/Account.java")));
    }

}
