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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// 自定义 handler 匿名实现 21 行（阈值 20 微超，测试内聚的 handler 直白可读）
@SuppressWarnings("AnonInnerLength")
class TypeMapperTest {

    private final DdlConfig config = new DdlConfig();

    private final TypeMapper mapper = new TypeMapper();

    private Column column(String sqlType) {
        return Column.builder().name("c").sqlType(sqlType).build();
    }

    @Test
    void customHandlerResolveTypeHook() {
        DdlAnnotationHandler handler = new DdlAnnotationHandler() {

            @Override
            public String name() {
                return "custom";
            }

            @Override
            public void parse(Meta meta, @Nullable String value) {}

            @Override
            public String resolveType(Column column, String defaultType) {
                return "com.example.Money";
            }

            @Override
            public Set<MetaTarget> targets() {
                return EnumSet.of(MetaTarget.COLUMN);
            }

        };
        TypeMapper customMapper = new TypeMapper(Collections.singletonList(handler));
        assertEquals("com.example.Money", customMapper.resolveType("user", column("amount")));
    }

    @Test
    void datetimeMapsToLocalDateTime() {
        assertEquals("java.time.LocalDateTime", mapper.sqlToJava(column("datetime")));
    }

    @Test
    void decimalMapsToBigDecimal() {
        assertEquals("java.math.BigDecimal", mapper.sqlToJava(column("decimal")));
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
    void jdbcTypeMapping() {
        assertEquals("VARCHAR", TypeMapper.sqlToJdbcType("varchar"));
        assertEquals("BIGINT", TypeMapper.sqlToJdbcType("bigint"));
        assertEquals("TINYINT", TypeMapper.sqlToJdbcType("tinyint"));
        assertEquals("DECIMAL", TypeMapper.sqlToJdbcType("decimal"));
        assertEquals("TIMESTAMP", TypeMapper.sqlToJdbcType("datetime"));
        assertEquals("INTEGER", TypeMapper.sqlToJdbcType("int"));
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
    void tinyintOneMapsToBoolean() {
        Column c = Column.builder().name("valid").sqlType("tinyint").length(1).build();
        assertEquals("java.lang.Boolean", mapper.sqlToJava(c));
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
    void unknownTypeDefaultsToString() {
        assertEquals("java.lang.String", mapper.sqlToJava(column("geometry")));
    }

    @Test
    void unsignedIntMapsToLong() {
        Column c = Column.builder().name("cnt").sqlType("int").unsigned(true).build();
        assertEquals("java.lang.Long", mapper.sqlToJava(c));
    }

}
