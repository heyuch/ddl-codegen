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
import hyc.codegen.core.gen.ArtifactGenerator;
import hyc.codegen.core.gen.CodeGenerator;
import hyc.codegen.core.gen.ConverterGenerator;
import hyc.codegen.core.gen.EnumGenerator;
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

    private @Nullable DdlConfig config;

    private @Nullable CodeGenerator generator;

    /** @BeforeEach 初始化字段：语法层不保证非 null（标注 @Nullable），使用点经此显式校验。 */
    private DdlConfig config() {
        if (config == null) {
            throw new AssertionError("@BeforeEach 未初始化 config");
        }
        return config;
    }

    /** @BeforeEach 初始化字段：语法层不保证非 null（标注 @Nullable），使用点经此显式校验。 */
    private CodeGenerator generator() {
        if (generator == null) {
            throw new AssertionError("@BeforeEach 未初始化 generator");
        }
        return generator;
    }

    @BeforeEach
    void setUp() {
        config = new DdlConfig();
        config().setRoot(tempDir());
        config().addTableStripPrefix("t_");
        config().setTableStripShardSuffix(true);

        add("entity", "pojo", "com.demo.entity", "", "");
        config().artifact("entity").get().putOption("lombok", "true");
        config().artifact("entity").get().putOption("jsr303", "true");
        config().artifact("entity").get().putOption("enums", "true");
        add("enum", "enum", "com.demo.enums", "", "");
        add("po", "pojo", "com.demo.pojo", "Po", "");
        add("mapper", "mybatisMapper", "com.demo.mapper", "Mapper", "");
        config().artifact("mapper").get().setTarget("po");
        add("xml", "mybatisXml", null, null, "");
        config().artifact("xml").get().setPath("src/main/resources/mapper");
        config().artifact("xml").get().setTarget("po");
        add("repository", "repository", "com.demo.repository", "Repository", "");
        config().artifact("repository").get().setTarget("entity");
        add("repositoryImpl", "mybatisRepositoryImpl", "com.demo.repository.impl", "RepositoryImpl", "");
        config().artifact("repositoryImpl").get().setTarget("entity");
        config().artifact("repositoryImpl").get().putOption("mapper", "mapper");
        config().artifact("repositoryImpl").get().putOption("converter", "entityConverter");
        add("entityConverter", "converter", "com.demo.converter", "Converter", "");
        config().artifact("entityConverter").get().setSource("po");
        config().artifact("entityConverter").get().setTarget("entity");

        List<ArtifactGenerator> generators = Arrays.asList(
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
        DdlParser parser = new DruidDdlParser();
        Schema schema = new Schema();
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

}
