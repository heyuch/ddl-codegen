package hyc.codegen.core.gen;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.model.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code @enum} DDL 注解特性端到端：声明列按枚举处理 → enums=true 字段视图与 converter 桥接、
 * ALTER 增量同步 + 用户手写保留、手写常量与生成常量去重守卫。
 * <p>
 * 职责边界：EnumGenerator 输出契约（模板/lombok 形态/骨架/常量文本/校验错误）由 golden 契约套件
 * {@link EnumGeneratorTest} 承担，本类不再重复模板文本断言（20260906-02 职责收敛）。
 */
// 门面式集成测试：引用类型数 ≈ 被测管线涉及类数（§6 元素驱动）
@SuppressWarnings({"ClassDataAbstractionCoupling", "ClassFanOutComplexity", "MethodLength", "ExecutableStatementCount",
    "VariableDeclarationUsageDistance"})
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

    @Test
    void alterIncrementalSyncKeepsUserCodeAndOrder() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = featureConfig();
        Schema schema = new Schema();
        String create = "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 1=初始(INIT) 2=活跃(ACTIVE) 3=停用(SUSPEND) @enum:Status',\n"
                + "    primary key (id))";
        support.generate(config, schema, create);

        // 用户手写方法
        Path file = tempDir().resolve("com/demo/enums/Status.java");
        String userMethod = "\n    /** 用户手写方法 */\n    public String hello() {\n        return \"hi\";\n    }\n";
        String existing = support.readGenerated("com/demo/enums/Status.java");
        int lastBrace = existing.lastIndexOf('}');
        Files.write(file, (existing.substring(0, lastBrace) + userMethod + existing.substring(lastBrace))
                .getBytes(StandardCharsets.UTF_8));

        // ALTER 改 comment 枚举项：INIT desc 改、删 SUSPEND、增 AUDIT
        String alter = "alter table t_user modify column status tinyint not null "
                + "comment '状态 1=待处理(INIT) 2=活跃(ACTIVE) 4=审核(AUDIT) @enum:Status'";
        support.generate(config, schema, alter);
        String after = support.readGenerated("com/demo/enums/Status.java");
        assertTrue(after.contains("INIT(1, \"待处理\")"), after);
        assertTrue(after.contains("AUDIT(4, \"审核\")"), after);
        assertTrue(after.contains("public String hello()"), "用户手写方法应保留");
        assertTrue(after.indexOf("INIT(1") < after.indexOf("ACTIVE(2"), after);

        // 幂等：同输入重跑无变化
        support.generate(config, schema, alter);
        assertEquals(after, support.readGenerated("com/demo/enums/Status.java"), "同输入重跑应无变化");
    }

    @Test
    void enumFeatureDrivesViewsAndConverterBridge() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = featureConfig();
        support.generate(config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint unsigned not null comment '状态 1=初始(INIT) 2=活跃(ACTIVE) @enum:Status',\n"
                + "    primary key (id))");

        // enums=true 视图：entity 用枚举类、po 保持自然类型 Integer
        assertTrue(support.readGenerated("com/demo/entity/User.java").contains("private Status status"));
        assertTrue(support.readGenerated("com/demo/pojo/UserPo.java").contains("private Integer status"));

        // converter 桥接：标量→enum fromCode、enum→标量 getCode，均空安全（@enum 特性跨生成器契约）
        String converter = support.readGenerated("com/demo/converter/UserConverter.java");
        assertTrue(converter.contains(
                "user.setStatus(source.getStatus() == null ? null : Status.fromCode(source.getStatus()));"),
                converter);
        assertTrue(converter.contains(
                "userPo.setStatus(target.getStatus() == null ? null : target.getStatus().getCode());"),
                converter);
    }

    /** 特性链路配置：entity(enums=true) + enum + po + entityConverter(po→entity)。 */
    private DdlConfig featureConfig() {
        DdlConfig config = support().tableConfig();
        ArtifactConfig entity = new ArtifactConfig("entity");
        entity.setGenerator("pojo");
        entity.setModule("");
        entity.setPkg("com.demo.entity");
        entity.putOption("enums", "true");
        config.addArtifact(entity);
        support().addArtifact(config, "enum", "enum", "com.demo.enums", "");
        support().addArtifact(config, "po", "pojo", "com.demo.pojo", "Po");
        ArtifactConfig converter = new ArtifactConfig("entityConverter");
        converter.setGenerator("converter");
        converter.setModule("");
        converter.setPkg("com.demo.converter");
        converter.setSuffix("Converter");
        converter.setSource("po");
        converter.setTarget("entity");
        config.addArtifact(converter);
        return config;
    }

    @Test
    void skeletonUserConstantBlocksGeneratorDuplicate() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = featureConfig();
        Schema schema = new Schema();
        support.generate(config, schema, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint not null comment '状态 @enum:Status',\n"
                + "    primary key (id))");

        // 用户自补常量（无 @Generated，模拟空骨架后手写）
        Path file = tempDir().resolve("com/demo/enums/Status.java");
        String userConstant = "\n    PENDING(9, \"待定\"),\n";
        String existing = support.readGenerated("com/demo/enums/Status.java");
        int semi = existing.indexOf(';');
        Files.write(file, (existing.substring(0, semi) + userConstant + existing.substring(semi))
                .getBytes(StandardCharsets.UTF_8));

        // DDL 再补同名列表项 → 不生成重复常量（保留用户那份）
        String alter = "alter table t_user modify column status tinyint not null "
                + "comment '状态 9=待定(PENDING) @enum:Status'";
        support.generate(config, schema, alter);
        String after = support.readGenerated("com/demo/enums/Status.java");
        assertEquals(1, countOccurrences(after, "PENDING(9, \"待定\")"), "不应生成重复常量");
    }

    private GeneratorTestSupport support() {
        return new GeneratorTestSupport(temp,
                new PojoGenerator(), new EnumGenerator(), new ConverterGenerator());
    }

    private Path tempDir() {
        Path dir = temp;
        if (dir == null) {
            throw new AssertionError("JUnit 未注入 @TempDir");
        }
        return dir;
    }

}
