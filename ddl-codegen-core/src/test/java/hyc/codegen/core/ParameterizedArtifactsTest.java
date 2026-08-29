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
import hyc.codegen.core.gen.FieldArtifactGenerator;
import hyc.codegen.core.gen.MapperGenerator;
import hyc.codegen.core.gen.MapperXmlGenerator;
import hyc.codegen.core.gen.MybatisRepositoryImplGenerator;
import hyc.codegen.core.gen.RepositoryGenerator;
import hyc.codegen.core.io.ChangeReport;
import hyc.codegen.core.model.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 产物与生成器解耦场景（design: feat-parameterized-artifacts）：
 * 无 po（mapper.target=entity）、自定义产物（dto）、多 converter、一致性校验。
 */
class ParameterizedArtifactsTest {

    @TempDir
    Path temp;

    private DdlConfig config;

    private CodeGenerator generator;

    private static final String DDL = "create table user (\n"
            + "    id bigint primary key,\n"
            + "    name varchar(50) comment '用户名',\n"
            + "    gender enum('male','female') comment '性别'\n"
            + ")";

    private void setUp(ArtifactConfig... artifacts) {
        config = new DdlConfig();
        config.setRoot(temp);
        for (ArtifactConfig artifact : artifacts) {
            config.addArtifact(artifact);
        }
        List<ArtifactGenerator> generators = Arrays.asList(
                new FieldArtifactGenerator(),
                new EnumGenerator(),
                new MapperGenerator(),
                new MapperXmlGenerator(),
                new RepositoryGenerator(),
                new MybatisRepositoryImplGenerator(),
                new ConverterGenerator());
        generator = new CodeGenerator(generators, Collections.emptyList());
    }

    private void generate() {
        DdlParser parser = new DruidDdlParser();
        Schema schema = new Schema();
        ApplyResult result = new StatementApplier().apply(schema, parser.parse(DDL));
        ChangeReport report = generator.generate(config, schema, result, Collections.emptyList());
        assertTrue(report.hasChanges(), "应产生变更: " + report.summary());
    }

    private String read(String relative) throws Exception {
        return new String(Files.readAllBytes(temp.resolve(relative)), StandardCharsets.UTF_8);
    }

    private ArtifactConfig enumArtifact() {
        ArtifactConfig a = new ArtifactConfig("enum");
        a.setGenerator("enum");
        a.setModule("");
        a.setPkg("com.demo.enums");
        return a;
    }

    private static ArtifactConfig artifact(String name, String generator) {
        ArtifactConfig a = new ArtifactConfig(name);
        a.setGenerator(generator);
        a.setModule("");
        return a;
    }

    @Test
    void mapperTargetCanBeEntityWithoutPo() throws Exception {
        ArtifactConfig entity = artifact("entity", "pojo");
        entity.setPkg("com.demo.entity");
        entity.setUse(Arrays.asList("enums"));
        ArtifactConfig mapper = artifact("mapper", "mybatisMapper");
        mapper.setPkg("com.demo.mapper");
        mapper.setSuffix("Mapper");
        mapper.setTarget("entity");

        setUp(entity, enumArtifact(), mapper);
        generate();

        String mapperCode = read("com/demo/mapper/UserMapper.java");
        assertTrue(mapperCode.contains("int insert(User user)"), mapperCode);
        assertTrue(mapperCode.contains("User findById"), mapperCode);
    }

    @Test
    void customDtoArtifactAndEnumViewByUse() throws Exception {
        ArtifactConfig entity = artifact("entity", "pojo");
        entity.setPkg("com.demo.entity");
        entity.setUse(Arrays.asList("enums"));
        ArtifactConfig dto = artifact("dto", "pojo");
        dto.setPkg("com.demo.dto");
        dto.setSuffix("Dto");

        setUp(entity, enumArtifact(), dto);
        generate();

        String entityCode = read("com/demo/entity/User.java");
        assertTrue(entityCode.contains("private Gender gender"), entityCode);
        String dtoCode = read("com/demo/dto/UserDto.java");
        assertTrue(dtoCode.contains("private String gender"), dtoCode);
    }

    @Test
    void multipleConvertersWithSourceTarget() throws Exception {
        ArtifactConfig entity = artifact("entity", "pojo");
        entity.setPkg("com.demo.entity");
        entity.setUse(Arrays.asList("enums"));
        ArtifactConfig po = artifact("po", "pojo");
        po.setPkg("com.demo.pojo");
        po.setSuffix("Po");
        ArtifactConfig dto = artifact("dto", "pojo");
        dto.setPkg("com.demo.dto");
        dto.setSuffix("Dto");
        dto.setUse(Arrays.asList("enums"));

        ArtifactConfig entityConverter = artifact("entityConverter", "converter");
        entityConverter.setPkg("com.demo.converter");
        entityConverter.setSuffix("Converter");
        entityConverter.setSource("po");
        entityConverter.setTarget("entity");
        ArtifactConfig dtoConverter = artifact("dtoConverter", "converter");
        dtoConverter.setPkg("com.demo.converter");
        dtoConverter.setSuffix("DtoConverter");
        dtoConverter.setSource("entity");
        dtoConverter.setTarget("dto");

        setUp(entity, enumArtifact(), po, dto, entityConverter, dtoConverter);
        generate();

        String converter = read("com/demo/converter/UserConverter.java");
        assertTrue(converter.contains("public User toUser(UserPo source)"), converter);
        assertTrue(converter.contains("public UserPo toUserPo(User target)"), converter);
        String dtoConverterCode = read("com/demo/converter/UserDtoConverter.java");
        assertTrue(dtoConverterCode.contains("public UserDto toUserDto(User source)"), dtoConverterCode);
    }

    @Test
    void repositoryImplConverterConsistencyValidated() throws Exception {
        ArtifactConfig entity = artifact("entity", "pojo");
        entity.setPkg("com.demo.entity");
        entity.setUse(Arrays.asList("enums"));
        ArtifactConfig po = artifact("po", "pojo");
        po.setPkg("com.demo.pojo");
        po.setSuffix("Po");
        ArtifactConfig mapper = artifact("mapper", "mybatisMapper");
        mapper.setPkg("com.demo.mapper");
        mapper.setSuffix("Mapper");
        mapper.setTarget("po");
        // converter source=entity 与 mapper.target=po 不一致
        ArtifactConfig converter = artifact("entityConverter", "converter");
        converter.setPkg("com.demo.converter");
        converter.setSuffix("Converter");
        converter.setSource("entity");
        converter.setTarget("entity");
        ArtifactConfig repository = artifact("repository", "repository");
        repository.setPkg("com.demo.repository");
        repository.setSuffix("Repository");
        repository.setTarget("entity");
        ArtifactConfig impl = artifact("repositoryImpl", "mybatisRepositoryImpl");
        impl.setPkg("com.demo.repository.impl");
        impl.setSuffix("RepositoryImpl");
        impl.setTarget("entity");
        impl.putOption("mapper", "mapper");
        impl.putOption("converter", "entityConverter");

        setUp(entity, enumArtifact(), po, mapper, converter, repository, impl);

        IllegalStateException e = assertThrows(IllegalStateException.class, this::generate);
        assertTrue(e.getMessage().contains("source"), e.getMessage());
        assertTrue(e.getMessage().contains("mapper"), e.getMessage());
    }

    @Test
    void converterDirectWithoutPoUsesEntity() throws Exception {
        ArtifactConfig entity = artifact("entity", "pojo");
        entity.setPkg("com.demo.entity");
        entity.setUse(Arrays.asList("enums"));
        ArtifactConfig mapper = artifact("mapper", "mybatisMapper");
        mapper.setPkg("com.demo.mapper");
        mapper.setSuffix("Mapper");
        mapper.setTarget("entity");
        ArtifactConfig repository = artifact("repository", "repository");
        repository.setPkg("com.demo.repository");
        repository.setSuffix("Repository");
        repository.setTarget("entity");
        ArtifactConfig impl = artifact("repositoryImpl", "mybatisRepositoryImpl");
        impl.setPkg("com.demo.repository.impl");
        impl.setSuffix("RepositoryImpl");
        impl.setTarget("entity");
        impl.putOption("mapper", "mapper");

        setUp(entity, enumArtifact(), mapper, repository, impl);
        generate();

        // mapper.target == impl.target → 直连，无 converter
        String implCode = read("com/demo/repository/impl/UserRepositoryImpl.java");
        assertFalse(implCode.contains("Converter"), implCode);
        assertTrue(implCode.contains("return userMapper.findById(id);"), implCode);
    }

}
