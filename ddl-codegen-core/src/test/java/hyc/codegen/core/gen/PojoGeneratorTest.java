package hyc.codegen.core.gen;

import java.nio.file.Path;

import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.config.DdlConfig;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PojoGenerator golden 契约测试：特性选项单开/组合（lombok/serializable/jsr303/jsr305/type）、
 * `@Nullable` 定制值、保留字列名、enums 视图。期望产物见 {@code fixtures/gen/pojo/**}。
 */
class PojoGeneratorTest {

    @TempDir
    @Nullable
    Path temp;

    private ArtifactConfig entity(DdlConfig config, GeneratorTestSupport support) {
        return support.addArtifact(config, "entity", "pojo", "com.demo.entity", "");
    }

    @Test
    void enumsViewUsesEnumClassField() throws Exception {
        // enums=true 需要 enum 产物（恰 1 个）→ 注册 EnumGenerator 一并产出；golden 子集断言 entity
        GeneratorTestSupport support = new GeneratorTestSupport(temp, new PojoGenerator(), new EnumGenerator());
        DdlConfig config = support.tableConfig();
        ArtifactConfig entity = support.addArtifact(config, "entity", "pojo", "com.demo.entity", "");
        entity.putOption("enums", "true");
        support.addArtifact(config, "enum", "enum", "com.demo.enums", "");
        support.generate(config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    status tinyint unsigned not null comment '状态 1=初始(INIT) @enum:Status',\n"
                + "    type varchar(20) not null comment '类型 NORMAL=普通 @enum:Type',\n"
                + "    primary key (id))");
        support.assertGoldenSubset("pojo/enums-view", "com/demo/entity/User.java");

        String entityText = support.readGenerated("com/demo/entity/User.java");
        assertTrue(entityText.contains("private Status status;"), entityText);
        assertTrue(entityText.contains("private Type type;"), entityText);
    }

    @Test
    void lombokSerializableJsr303Jsr305WithCustomNullable() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = plainConfig(support);
        config.setNullableAnnotation("javax.annotation.Nullable");
        ArtifactConfig entity = support.artifact(config, "entity");
        entity.putOption("lombok", "true");
        entity.putOption("serializable", "true");
        entity.putOption("jsr303", "true");
        entity.putOption("jsr305", "true");
        support.generate(config, sampleDdl());
        support.assertGolden("pojo/lombok-jsr-all");

        String entityText = support.readGenerated("com/demo/entity/User.java");
        assertTrue(entityText.contains("@Data"), entityText);
        assertTrue(entityText.contains("implements Serializable"), entityText);
        assertTrue(entityText.contains("serialVersionUID"), entityText);
        assertTrue(entityText.contains("import javax.annotation.Nullable;"), entityText);
        assertFalse(entityText.contains("org.checkerframework"), entityText);
    }

    private DdlConfig plainConfig(GeneratorTestSupport support) {
        DdlConfig config = support.tableConfig();
        entity(config, support);
        return config;
    }

    @Test
    void plainNoOptions() throws Exception {
        GeneratorTestSupport support = support();
        support.generate(plainConfig(support), sampleDdl());
        support.assertGolden("pojo/plain");
    }

    private String sampleDdl() {
        return "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    name varchar(50) not null comment '用户名',\n"
                + "    nick varchar(50) comment '昵称',\n"
                + "    amount decimal(10,2) not null comment '金额',\n"
                + "    score tinyint(1) not null comment '是否有效',\n"
                + "    created_at datetime comment '创建时间',\n"
                + "    primary key (id))";
    }

    private GeneratorTestSupport support() {
        return new GeneratorTestSupport(temp, new PojoGenerator());
    }

    @Test
    void typeAnnotationOverridesColumnType() throws Exception {
        GeneratorTestSupport support = support();
        DdlConfig config = plainConfig(support);
        support.artifact(config, "entity").putOption("type", "true");
        support.generate(config, "create table t_user (\n"
                + "    id bigint not null auto_increment comment '主键',\n"
                + "    amount varchar(30) not null comment '金额 @type:java.math.BigDecimal',\n"
                + "    primary key (id))");
        support.assertGolden("pojo/type-override");
    }

}
