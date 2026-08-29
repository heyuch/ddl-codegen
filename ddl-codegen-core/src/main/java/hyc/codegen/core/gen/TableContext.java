package hyc.codegen.core.gen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.io.PathResolver;
import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;
import hyc.codegen.core.model.Table;
import hyc.codegen.core.naming.NamingService;
import hyc.codegen.core.types.TypeMapper;

/**
 * 单表 × 单 artifact 的生成上下文：表、artifact 配置、命名与类型映射的便捷入口。
 * <p>
 * 由 {@link GenerationContext#tableContext} 创建，生成器不直接持有底层服务。
 */
public final class TableContext {

    private final Table table;

    private final String artifactName;

    private final ArtifactConfig artifactConfig;

    private final NamingService naming;

    private final TypeMapper types;

    @Nullable
    private final String enumPackage;

    private final String nullableAnnotation;

    TableContext(Table table, ArtifactConfig artifactConfig,
            NamingService naming, TypeMapper types, @Nullable String enumPackage, String nullableAnnotation) {
        this.table = table;
        this.artifactName = artifactConfig.getName();
        this.artifactConfig = artifactConfig;
        this.naming = naming;
        this.types = types;
        this.enumPackage = enumPackage;
        this.nullableAnnotation = nullableAnnotation;
    }

    public Table getTable() {
        return table;
    }

    /** 产物名。 */
    public String getArtifactName() {
        return artifactName;
    }

    /** {@code @Nullable} 注解全限定名（config {@code annotations.nullable}）。 */
    public String getNullableAnnotation() {
        return nullableAnnotation;
    }

    public ArtifactConfig getArtifactConfig() {
        return artifactConfig;
    }

    /** 类名（基类名 + 该 artifact 配置的后缀；表注释 {@code @as} 可整体覆盖基类名）。 */
    public String className() {
        Object as = table.getMeta().get("as");
        if (as != null) {
            return as + artifactConfig.getSuffix();
        }
        return naming.artifactClassName(table.getName(), artifactName);
    }

    /** 包名（artifact 配置）。 */
    public String packageName() {
        return artifactConfig.getPkg();
    }

    /** Java 类文件路径：根 + module + package 路径 + 类名。 */
    public Path javaFile(Path projectRoot) {
        return PathResolver.javaFile(projectRoot, artifactConfig.getModule(), packageName(), className());
    }

    /** 资源文件路径：根 + module + 相对资源路径 + 文件名。 */
    public Path xmlFile(Path projectRoot, String resourcePath, String fileName) {
        return PathResolver.xmlFile(projectRoot, artifactConfig.getModule(), resourcePath, fileName);
    }

    /** 列名 → 字段名（命名策略）。 */
    public String fieldName(Column column) {
        return naming.columnFieldName(column.getName());
    }

    /**
     * 列 → Java 类型（按产物解析，视图完全由配置决定，无产物名硬编码）：
     * use 含 {@code enums}（实体视图）时——{@code @type} 覆盖优先，enum 列返回枚举类全限定名（含 {@code @as}）；
     * 否则走 {@link TypeMapper}（enum→String 等 SQL 映射）。
     */
    public String typeOf(Column column) {
        if (usesEnums()) {
            Object type = column.getMeta().get("type");
            if (type != null) {
                return type.toString();
            }
            if (!column.getEnumValues().isEmpty()) {
                return enumPackage + "." + enumClassName(column);
            }
        }
        return types.resolveType(table.getName(), column);
    }

    /** 按字段名反查列（字段名 = 命名策略转换后的列名）；未匹配返回 null。 */
    @Nullable
    public Column findColumn(String fieldName) {
        for (Column column : table.getColumns()) {
            if (fieldName(column).equals(fieldName)) {
                return column;
            }
        }
        return null;
    }

    /** use 链是否含 enums（enum 列 → 枚举类视图）。 */
    public boolean usesEnums() {
        return artifactConfig.getUse().contains("enums");
    }

    /** 列 → MyBatis jdbcType。 */
    public String jdbcType(Column column) {
        return TypeMapper.sqlToJdbcType(column.getSqlType());
    }

    /** 索引 → 查询方法名（前缀 + By + 列 And 连接）。 */
    public String methodName(Index index) {
        return naming.indexMethodName(index);
    }

    /** enum 列 → 枚举类名（列注释 {@code @as} 优先，否则按命名策略）。 */
    public String enumClassName(Column column) {
        Object as = column.getMeta().get("as");
        if (as != null) {
            return as.toString();
        }
        return naming.enumClassName(table.getName(), column.getName());
    }

    /** 该表的字段列表（列序）。 */
    public List<Column> columns() {
        return new ArrayList<>(table.getColumns());
    }

    /** 该表的索引列表。 */
    public List<Index> indexes() {
        return new ArrayList<>(table.getIndexes());
    }

    @Nullable
    public String tableComment() {
        return table.getComment();
    }

}
