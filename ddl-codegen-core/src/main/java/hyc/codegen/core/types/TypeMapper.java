package hyc.codegen.core.types;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import hyc.codegen.core.annotation.DdlAnnotationHandler;
import hyc.codegen.core.model.Column;
import hyc.codegen.core.naming.NamingService;

/**
 * 类型映射：列 → Java 类型（按 artifact 解析）、SQL 类型 → MyBatis jdbcType。
 * <p>
 * 解析顺序（DESIGN §8）：{@code @type} 注解 > enum 列（entity 用枚举类、其余用 String）> SQL→Java 基础类型 > 自定义处理器钩子。
 * POJO 的 SQL→Java 映射为固定内置表（不进 config）。
 */
public final class TypeMapper {

    /** SQL 类型 → Java 类型基础映射（忽略长度/无符号等列属性差异）。 */
    private static final Map<String, String> JAVA_TYPES = Map.ofEntries(
            Map.entry("smallint", "java.lang.Integer"),
            Map.entry("mediumint", "java.lang.Integer"),
            Map.entry("int", "java.lang.Integer"),
            Map.entry("integer", "java.lang.Integer"),
            Map.entry("year", "java.lang.Integer"),
            Map.entry("bigint", "java.lang.Long"),
            Map.entry("decimal", "java.math.BigDecimal"),
            Map.entry("numeric", "java.math.BigDecimal"),
            Map.entry("float", "java.lang.Float"),
            Map.entry("double", "java.lang.Double"),
            Map.entry("boolean", "java.lang.Boolean"),
            Map.entry("bool", "java.lang.Boolean"),
            Map.entry("char", "java.lang.String"),
            Map.entry("varchar", "java.lang.String"),
            Map.entry("tinytext", "java.lang.String"),
            Map.entry("text", "java.lang.String"),
            Map.entry("mediumtext", "java.lang.String"),
            Map.entry("longtext", "java.lang.String"),
            Map.entry("json", "java.lang.String"),
            Map.entry("binary", "byte[]"),
            Map.entry("varbinary", "byte[]"),
            Map.entry("tinyblob", "byte[]"),
            Map.entry("blob", "byte[]"),
            Map.entry("mediumblob", "byte[]"),
            Map.entry("longblob", "byte[]"),
            Map.entry("date", "java.time.LocalDate"),
            Map.entry("datetime", "java.time.LocalDateTime"),
            Map.entry("timestamp", "java.time.LocalDateTime"),
            Map.entry("time", "java.time.LocalTime"));

    /** 无符号且可能超出有符号上限的整数类型（映射 Long）。 */
    private static final Set<String> INTEGER_TYPES = Set.of(
            "smallint", "mediumint", "int", "integer", "year");

    /** SQL 类型 → java.sql.Types 名映射（MyBatis jdbcType）。 */
    private static final Map<String, String> JDBC_TYPES = Map.ofEntries(
            Map.entry("tinyint", "TINYINT"),
            Map.entry("smallint", "SMALLINT"),
            Map.entry("mediumint", "INTEGER"),
            Map.entry("int", "INTEGER"),
            Map.entry("integer", "INTEGER"),
            Map.entry("year", "INTEGER"),
            Map.entry("bigint", "BIGINT"),
            Map.entry("decimal", "DECIMAL"),
            Map.entry("numeric", "DECIMAL"),
            Map.entry("float", "FLOAT"),
            Map.entry("double", "DOUBLE"),
            Map.entry("boolean", "BOOLEAN"),
            Map.entry("bool", "BOOLEAN"),
            Map.entry("char", "CHAR"),
            Map.entry("varchar", "VARCHAR"),
            Map.entry("json", "VARCHAR"),
            Map.entry("tinytext", "LONGVARCHAR"),
            Map.entry("text", "LONGVARCHAR"),
            Map.entry("mediumtext", "LONGVARCHAR"),
            Map.entry("longtext", "LONGVARCHAR"),
            Map.entry("binary", "VARBINARY"),
            Map.entry("varbinary", "VARBINARY"),
            Map.entry("tinyblob", "BLOB"),
            Map.entry("blob", "BLOB"),
            Map.entry("mediumblob", "BLOB"),
            Map.entry("longblob", "BLOB"),
            Map.entry("date", "DATE"),
            Map.entry("datetime", "TIMESTAMP"),
            Map.entry("timestamp", "TIMESTAMP"),
            Map.entry("time", "TIME"));

    private final NamingService naming;

    private final List<DdlAnnotationHandler> customHandlers;

    public TypeMapper(NamingService naming) {
        this(naming, Collections.emptyList());
    }

    public TypeMapper(NamingService naming, List<DdlAnnotationHandler> customHandlers) {
        this.naming = naming;
        this.customHandlers = customHandlers;
    }

    /**
     * 列 → Java 类型（全限定名或原样返回的简单名）。
     * <p>
     * 解析顺序：enum 列 → String（实体视图的枚举类与 {@code @type} 覆盖由 TableContext#typeOf
     * 按 use:enums 处理，这里只管 SQL 映射）> SQL→Java 基础类型 > 自定义处理器钩子。
     *
     * @param tableName 所属表名
     * @param column    目标列
     */
    public String resolveType(String tableName, Column column) {
        // 1. enum 列：非实体视图固定 String
        if (!column.getEnumValues().isEmpty()) {
            return "java.lang.String";
        }

        // 2. SQL → Java 基础类型
        String javaType = sqlToJava(column);

        // 3. 自定义注解处理器类型解析钩子
        for (DdlAnnotationHandler handler : customHandlers) {
            javaType = handler.resolveType(column, javaType);
        }
        return javaType;
    }

    /**
     * SQL 类型 + 列属性 → Java 类型（全限定名；未知类型保守映射 String）。
     * <p>
     * 注意：无符号整数可能超出有符号上限，int/mediumint/smallint 无符号时映射 Long。
     */
    public static String sqlToJava(Column column) {
        String type = column.getSqlType().toLowerCase(Locale.ROOT);
        if ("tinyint".equals(type)) {
            return column.getLength() == 1 ? "java.lang.Boolean" : "java.lang.Integer";
        }
        if (column.isUnsigned() && INTEGER_TYPES.contains(type)) {
            return "java.lang.Long";
        }
        String javaType = JAVA_TYPES.get(type);
        return javaType != null ? javaType : "java.lang.String";
    }

    /** SQL 类型 → java.sql.Types 名（MyBatis jdbcType；未知类型保守映射 VARCHAR）。 */
    public static String sqlToJdbcType(String sqlType) {
        String jdbcType = JDBC_TYPES.get(sqlType.toLowerCase(Locale.ROOT));
        return jdbcType != null ? jdbcType : "VARCHAR";
    }

}
