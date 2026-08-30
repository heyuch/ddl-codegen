package hyc.codegen.core.gen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hyc.codegen.core.config.ArtifactConfig;
import hyc.codegen.core.io.PathResolver;
import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;
import hyc.codegen.core.model.Table;
import hyc.codegen.core.naming.NamingService;
import hyc.codegen.core.types.TypeMapper;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 单表 × 单 artifact 的生成上下文：表、artifact 配置、命名与类型映射的便捷入口。
 * <p>
 * 由 {@link GenerationContext#tableContext} 创建，生成器不直接持有底层服务。
 */
// EI 抑制：表上下文持有 Table 模型与 artifact 配置：生成器高频读取，Table 深拷贝不现实且拷贝后与 config 查找结果引用不等
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public final class TableContext {

    private final Table table;

    private final GenerationContext gctx;

    private final String artifactName;

    private final ArtifactConfig artifactConfig;

    private final NamingService naming;

    private final TypeMapper types;

    private final @Nullable String enumPackage;

    private final String nullableAnnotation;

    TableContext(Table table, ArtifactConfig artifactConfig, GenerationContext gctx) {
        this.table = table;
        this.gctx = gctx;
        this.artifactName = artifactConfig.getName();
        this.artifactConfig = artifactConfig;
        this.naming = gctx.getNaming();
        this.types = gctx.getTypeMapper();
        this.enumPackage = gctx.enumPackageFor(artifactConfig.getName());
        this.nullableAnnotation = gctx.getConfig().getNullableAnnotation();
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

    /** 包名（artifact 配置；Java 类产物必须配置，缺失即配置错误）。 */
    public String packageName() {
        String pkg = artifactConfig.getPkg();
        if (pkg == null) {
            throw new IllegalStateException(
                    "产物 '" + artifactConfig.getName() + "' 缺少 package 配置（Java 类产物必须配置 package）");
        }
        return pkg;
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

    /** 列 → 成员类型（查询契约：路由到本产物生成器的 fieldType，type/enums 特性生效）。 */
    public String typeOf(Column column) {
        return gctx.generatorFor(artifactName).fieldType(column, this);
    }

    public NamingService getNaming() {
        return naming;
    }

    public TypeMapper getTypeMapper() {
        return types;
    }

    /** enum 产物包（enums 特性开启时已校验存在）。 */
    public @Nullable String getEnumPackage() {
        return enumPackage;
    }

    /** 按字段名反查列（字段名 = 命名策略转换后的列名）；未匹配返回 null。 */
    public @Nullable Column findColumn(String fieldName) {
        for (Column column : table.getColumns()) {
            if (fieldName(column).equals(fieldName)) {
                return column;
            }
        }
        return null;
    }

    /** enums 特性开关（enum 列 → 枚举类视图）。 */
    public boolean usesEnums() {
        return Boolean.parseBoolean(artifactConfig.getOption("enums"));
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

    public @Nullable String tableComment() {
        return table.getComment();
    }

}
