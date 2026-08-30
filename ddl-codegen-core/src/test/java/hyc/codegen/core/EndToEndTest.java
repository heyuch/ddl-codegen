package hyc.codegen.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.ddl.ApplyResult;
import hyc.codegen.core.ddl.DdlParser;
import hyc.codegen.core.ddl.DruidDdlParser;
import hyc.codegen.core.ddl.StatementApplier;
import hyc.codegen.core.gen.CodeGenerator;
import hyc.codegen.core.gen.ConverterGenerator;
import hyc.codegen.core.gen.EnumGenerator;
import hyc.codegen.core.gen.Generator;
import hyc.codegen.core.gen.MapperGenerator;
import hyc.codegen.core.gen.MapperXmlGenerator;
import hyc.codegen.core.gen.MybatisRepositoryImplGenerator;
import hyc.codegen.core.gen.PojoGenerator;
import hyc.codegen.core.gen.RepositoryGenerator;
import hyc.codegen.core.io.ChangeReport;
import hyc.codegen.core.model.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端验收：完整 DDL（enum/@type/@as/@ignore/索引）→ 全链路 artifact 文件。
 */
class EndToEndTest {

    @TempDir
    @Nullable
    Path temp;

    private @Nullable DdlConfig config;

    private @Nullable CodeGenerator generator;

    /**
     * @TempDir 注入目录：JUnit 保证注入，但语法层不保证非 null（标注 @Nullable），使用点经此显式校验。
     */
    private Path tempDir() {
        Path dir = temp;
        if (dir == null) {
            throw new AssertionError("JUnit 未注入 @TempDir");
        }
        return dir;
    }

    private DdlConfig config() {
        if (config == null) {
            throw new AssertionError("@BeforeEach 未初始化 config");
        }
        return config;
    }

    /**
     * @BeforeEach 初始化字段：语法层不保证非 null（标注 @Nullable），使用点经此显式校验。
     */
    private CodeGenerator generator() {
        if (generator == null) {
            throw new AssertionError("@BeforeEach 未初始化 generator");
        }
        return generator;
    }

    /**
     * 按产物名取配置（测试约定产物已配置）。
     */
    private ArtifactConfig artifact(String name) {
        ArtifactConfig artifactConfig = config().artifact(name);
        if (artifactConfig == null) {
            throw new AssertionError("产物未配置: " + name);
        }
        return artifactConfig;
    }

    @BeforeEach
    void setUp() {
        config = new DdlConfig();
        config().setRoot(tempDir());
        config().addTableStripPrefix("t_");
        config().setTableStripShardSuffix(true);

        add("entity", "pojo", "com.demo.entity", "", "");
        artifact("entity").putOption("lombok", "true");
        artifact("entity").putOption("jsr303", "true");
        artifact("entity").putOption("enums", "true");
        add("enum", "enum", "com.demo.enums", "", "");
        add("po", "pojo", "com.demo.pojo", "Po", "");
        add("mapper", "mybatisMapper", "com.demo.mapper", "Mapper", "");
        artifact("mapper").setTarget("po");
        add("xml", "mybatisXml", null, null, "");
        artifact("xml").setPath("src/main/resources/mapper");
        artifact("xml").setTarget("po");
        add("repository", "repository", "com.demo.repository", "Repository", "");
        artifact("repository").setTarget("entity");
        add("repositoryImpl", "mybatisRepositoryImpl", "com.demo.repository.impl", "RepositoryImpl", "");
        artifact("repositoryImpl").setTarget("entity");
        artifact("repositoryImpl").putOption("mapper", "mapper");
        artifact("repositoryImpl").putOption("converter", "entityConverter");
        add("entityConverter", "converter", "com.demo.converter", "Converter", "");
        artifact("entityConverter").setSource("po");
        artifact("entityConverter").setTarget("entity");

        List<Generator> generators = Arrays.asList(
                new PojoGenerator(),
                new EnumGenerator(),
                new MapperGenerator(),
                new MapperXmlGenerator(),
                new RepositoryGenerator(),
                new MybatisRepositoryImplGenerator(),
                new ConverterGenerator());
        generator = new CodeGenerator(generators);
    }

    private void add(String name, String generator, @Nullable String pkg, @Nullable String suffix, String use) {
        ArtifactConfig artifact = new ArtifactConfig(name);
        artifact.setGenerator(generator);
        artifact.setModule("");
        artifact.setPkg(pkg);
        artifact.setSuffix(suffix);
        config().addArtifact(artifact);
    }

    private void generate(String ddl) {
        generate(new DruidDdlParser(), new Schema(), ddl);
    }

    /**
     * 在同一 schema 上连续应用 DDL（alter 流程测试用：create 后 alter 需复用模型状态）。
     */
    private void generate(DdlParser parser, Schema schema, String ddl) {
        ApplyResult result = new StatementApplier().apply(schema, parser.parse(ddl));
        ChangeReport report = generator().generate(config(), schema, result, Collections.emptyList());
        assertTrue(report.hasChanges(), "应产生变更: " + report.summary());
    }

    private String read(String relative) throws Exception {
        return new String(Files.readAllBytes(tempDir().resolve(relative)), StandardCharsets.UTF_8);
    }

    private static final String DDL = "create table t_user (\n"
            + "    id bigint not null auto_increment comment '主键',\n"
            + "    name varchar(50) not null comment '用户名',\n"
            + "    gender enum('male','female') comment '性别',\n"
            + "    status tinyint not null default 0 comment '状态',\n"
            + "    credits decimal(10,2) comment '积分',\n"
            + "    ext_info varchar(500) comment '扩展信息 @ignore',\n"
            + "    create_time datetime comment '创建时间',\n"
            + "    primary key (id),\n"
            + "    unique key uk_name (name),\n"
            + "    index idx_status (status) comment '@ignore'\n"
            + ") comment '用户表'";

    @Test
    void fullChainGeneratesAllArtifacts() throws Exception {
        generate(DDL);

        // Entity：字段、@Generated、lombok、jsr303、@ignore 排除、@as 枚举
        String entity = read("com/demo/entity/User.java");
        assertTrue(entity.contains("private Long id"), entity);
        assertTrue(entity.contains("private String name"), entity);
        assertTrue(entity.contains("private Gender gender"), entity);
        assertTrue(entity.contains("private BigDecimal credits"), entity);
        assertTrue(entity.contains("private LocalDateTime createTime"), entity);
        assertFalse(entity.contains("extInfo"), entity);
        assertTrue(entity.contains("@Data"), entity);
        assertTrue(entity.contains("@NotNull"), entity);
        assertTrue(entity.contains("@Size(max = 50)"), entity);
        assertTrue(entity.contains("@Generated"), entity);
        assertTrue(entity.contains("public class User"), entity);

        // Enum：@as 命名 + 常量 + value/fromValue
        String gender = read("com/demo/enums/Gender.java");
        assertTrue(gender.contains("MALE(\"male\")"), gender);
        assertTrue(gender.contains("FEMALE(\"female\")"), gender);
        assertTrue(gender.contains("fromValue"), gender);
        assertTrue(gender.contains("public String value()"), gender);

        // Pojo：enum → String，@type 不影响 pojo
        String pojo = read("com/demo/pojo/UserPo.java");
        assertTrue(pojo.contains("private String gender"), pojo);
        assertTrue(pojo.contains("private BigDecimal credits"), pojo);

        // Mapper：CRUD + 唯一键 @Nullable + @ignore 索引跳过
        String mapper = read("com/demo/mapper/UserMapper.java");
        assertTrue(mapper.contains("int insert(UserPo userPo)"), mapper);
        assertTrue(mapper.contains("int update(UserPo userPo)"), mapper);
        assertTrue(mapper.contains("int deleteById(@Param(\"id\") Long id)"), mapper);
        assertTrue(mapper.contains("@Nullable"), mapper);
        assertTrue(mapper.contains("UserPo findById(@Param(\"id\") Long id)"), mapper);
        assertTrue(mapper.contains("UserPo findByName(@Param(\"name\") String name)"), mapper);
        assertFalse(mapper.contains("findByStatus"), mapper);

        // XML：resultMap/BaseColumnList/CRUD/select、id 与接口一致、t. 别名、insert 不含自增主键
        String xml = read("src/main/resources/mapper/UserMapper.xml");
        assertTrue(xml.contains("<mapper namespace=\"com.demo.mapper.UserMapper\">"), xml);
        assertTrue(xml.contains("<id property=\"id\" column=\"id\" jdbcType=\"BIGINT\"/>"), xml);
        assertTrue(xml.contains("t.create_time"), xml);
        assertFalse(xml.contains("ext_info"), xml);
        assertTrue(xml.contains("<select id=\"findById\""), xml);
        assertTrue(xml.contains("<select id=\"findByName\""), xml);
        assertFalse(xml.contains("findByStatus"), xml);
        assertTrue(xml.contains("useGeneratedKeys=\"true\""), xml);
        assertFalse(xml.contains("t.id,\n        id"), xml);

        // Repository：entity 视图（enum 参数用枚举类）
        String repository = read("com/demo/repository/UserRepository.java");
        assertTrue(repository.contains("@Nullable"), repository);
        assertTrue(repository.contains("User findById(Long id)"), repository);
        assertTrue(repository.contains("User findByName(String name)"), repository);
        assertFalse(repository.contains("findByStatus"), repository);

        // RepositoryImpl：桥接 + enum 转换
        String impl = read("com/demo/repository/impl/UserRepositoryImpl.java");
        assertTrue(impl.contains("private UserMapper userMapper;"), impl);
        assertTrue(impl.contains("private UserConverter userConverter;"), impl);
        assertTrue(impl.contains("return userConverter.toUser(userMapper.findById(id));"), impl);
        assertTrue(impl.contains("return userConverter.toUser(userMapper.findByName(name));"), impl);

        // Converter：逐字段 + enum 双向转换
        String converter = read("com/demo/converter/UserConverter.java");
        assertTrue(converter.contains("User user = new User();"), converter);
        assertTrue(
                converter.contains(
                        "user.setGender(source.getGender() == null ? null : Gender.fromValue(source.getGender()));"),
                converter);
        assertTrue(
                converter.contains("userPo.setGender(target.getGender() == null ? null : target.getGender().value());"),
                converter);
    }

    @Test
    void alterFlowUpdatesArtifacts() throws Exception {
        DdlParser parser = new DruidDdlParser();
        Schema schema = new Schema();
        generate(parser, schema, DDL);

        // rename column：create_time → created_at（非索引列，避免索引引用牵连）
        generate(parser, schema, "alter table t_user rename column create_time to created_at");
        String entity = read("com/demo/entity/User.java");
        assertTrue(entity.contains("private LocalDateTime createdAt"), entity);
        assertFalse(entity.contains("createTime"), "entity 不应残留旧字段名");
        String xml = read("src/main/resources/mapper/UserMapper.xml");
        assertTrue(xml.contains("created_at"), xml);
        assertFalse(xml.contains("create_time"), "XML 不应残留旧列引用");

        // add index（无 @ignore）→ mapper/XML 出现 findByStatus
        generate(parser, schema, "alter table t_user add index idx_status (status)");
        String mapper = read("com/demo/mapper/UserMapper.java");
        assertTrue(mapper.contains("findByStatus"), mapper);
        assertTrue(read("src/main/resources/mapper/UserMapper.xml").contains("<select id=\"findByStatus\""));

        // drop index → findByStatus 消失（@Generated 方法随模型移除）
        generate(parser, schema, "alter table t_user drop index idx_status");
        assertFalse(read("com/demo/mapper/UserMapper.java").contains("findByStatus"));
        assertFalse(read("src/main/resources/mapper/UserMapper.xml").contains("<select id=\"findByStatus\""));
    }

}
