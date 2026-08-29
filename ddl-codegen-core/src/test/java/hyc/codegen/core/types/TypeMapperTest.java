package hyc.codegen.core.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import hyc.codegen.core.annotation.DdlAnnotationHandler;
import hyc.codegen.core.annotation.MetaTarget;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Meta;
import hyc.codegen.core.naming.NamingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeMapperTest {

    private final DdlConfig config = new DdlConfig();

    private final NamingService naming = new NamingService(config);

    private final TypeMapper mapper = new TypeMapper(naming);

    private Column column(String sqlType) {
        return Column.builder().name("c").sqlType(sqlType).build();
    }

    @Test
    void pojoEnumColumnMapsToString() {
        Column c = Column.builder()
                .name("gender")
                .sqlType("enum")
                .enumValues(Arrays.asList("male", "female"))
                .build();
        assertEquals("java.lang.String", mapper.resolveType("user", c));
    }

    @Test
    void entityEnumColumnAlsoStringInTypeMapper() {
        // enum 类解析已移到 TableContext（含 @as 与 enum 包名）；TypeMapper 对 enum 列一律 String
        Column c = Column.builder()
                .name("gender")
                .sqlType("enum")
                .enumValues(Arrays.asList("male", "female"))
                .build();
        assertEquals("java.lang.String", mapper.resolveType("user", c));
    }

    @Test
    void typeAnnotationHandledByTableContextNotTypeMapper() {
        // @type 与 enum 视图已收进 TableContext（按 use:enums），TypeMapper 只做 SQL 映射
        Column c = Column.builder()
                .name("ext_info")
                .sqlType("varchar")
                .enumValues(Arrays.asList("a"))
                .build();
        c.getMeta().put("type", "com.msxf.ValidEnum");
        assertEquals("java.lang.String", mapper.resolveType("user", c));
    }

    @Test
    void decimalMapsToBigDecimal() {
        assertEquals("java.math.BigDecimal", mapper.sqlToJava(column("decimal")));
    }

    @Test
    void tinyintOneMapsToBoolean() {
        Column c = Column.builder().name("valid").sqlType("tinyint").length(1).build();
        assertEquals("java.lang.Boolean", mapper.sqlToJava(c));
    }

    @Test
    void unsignedIntMapsToLong() {
        Column c = Column.builder().name("cnt").sqlType("int").unsigned(true).build();
        assertEquals("java.lang.Long", mapper.sqlToJava(c));
    }

    @Test
    void datetimeMapsToLocalDateTime() {
        assertEquals("java.time.LocalDateTime", mapper.sqlToJava(column("datetime")));
    }

    @Test
    void unknownTypeDefaultsToString() {
        assertEquals("java.lang.String", mapper.sqlToJava(column("geometry")));
    }

    @Test
    void jdbcTypeMapping() {
        assertEquals("VARCHAR", TypeMapper.sqlToJdbcType("varchar"));
        assertEquals("BIGINT", TypeMapper.sqlToJdbcType("bigint"));
        assertEquals("TINYINT", TypeMapper.sqlToJdbcType("tinyint"));
        assertEquals("DECIMAL", TypeMapper.sqlToJdbcType("decimal"));
        assertEquals("TIMESTAMP", TypeMapper.sqlToJdbcType("datetime"));
        assertEquals("INTEGER", TypeMapper.sqlToJdbcType("int"));
    }

    @Test
    void customHandlerResolveTypeHook() {
        DdlAnnotationHandler handler = new DdlAnnotationHandler() {

            @Override
            public String name() {
                return "custom";
            }

            @Override
            public Set<MetaTarget> targets() {
                return EnumSet.of(MetaTarget.COLUMN);
            }

            @Override
            public void parse(Meta meta, String value) {}

            @Override
            public String resolveType(Column column, String defaultType) {
                return "com.example.Money";
            }

        };
        TypeMapper customMapper = new TypeMapper(naming, Collections.singletonList(handler));
        assertEquals("com.example.Money", customMapper.resolveType("user", column("amount")));
    }

}
