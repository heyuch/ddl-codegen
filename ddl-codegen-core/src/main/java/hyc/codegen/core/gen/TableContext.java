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

    private final String artifactKind;

    private final ArtifactConfig artifactConfig;

    private final NamingService naming;

    private final TypeMapper types;

    TableContext(Table table, String artifactKind, ArtifactConfig artifactConfig,
            NamingService naming, TypeMapper types) {
        this.table = table;
        this.artifactKind = artifactKind;
        this.artifactConfig = artifactConfig;
        this.naming = naming;
        this.types = types;
    }

    public Table getTable() {
        return table;
    }

    public String getArtifactKind() {
        return artifactKind;
    }

    public ArtifactConfig getArtifactConfig() {
        return artifactConfig;
    }

    /** 类名（基类名 + 该 artifact 配置的后缀）。 */
    public String className() {
        return naming.artifactClassName(table.getName(), artifactKind);
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

    /** 列 → Java 类型（按 artifact 解析，含 @type 覆盖）。 */
    public String typeOf(Column column) {
        return types.resolveType(table.getName(), column, artifactKind);
    }

    /** 列 → MyBatis jdbcType。 */
    public String jdbcType(Column column) {
        return TypeMapper.sqlToJdbcType(column.getSqlType());
    }

    /** 索引 → 查询方法名（前缀 + By + 列 And 连接）。 */
    public String methodName(Index index) {
        return naming.indexMethodName(index);
    }

    /** enum 列 → 枚举类名（按命名策略）。 */
    public String enumClassName(Column column) {
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
