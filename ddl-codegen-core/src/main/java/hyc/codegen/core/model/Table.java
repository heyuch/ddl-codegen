package hyc.codegen.core.model;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 数据表模型。
 * <p>
 * 由 DDL 的 create table 产生，被 alter/drop 语句就地修改：
 * 列与索引保持 DDL 定义顺序（代码生成依赖列序），同名替换、删除均保持其余位置不变。
 */
// EI 抑制：Meta 是开放读写容器（javadoc 契约）：注解处理器 getMeta().put() 写入、生成器读取，拷贝则写入丢失
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public final class Table {

    private String name;
    private final @Nullable String comment;
    private final List<Column> columns = new ArrayList<>();
    private final List<Index> indexes = new ArrayList<>();
    private final Meta meta = new Meta();

    public Table(String name, @Nullable String comment) {
        this.name = name;
        this.comment = comment;
    }

    public String getName() {
        return name;
    }

    public @Nullable String getComment() {
        return comment;
    }

    /** 列列表（DDL 定义顺序，不可变视图）。 */
    public List<Column> getColumns() {
        return new ArrayList<>(columns);
    }

    /** 索引列表（DDL 定义顺序，不可变视图）。 */
    public List<Index> getIndexes() {
        return new ArrayList<>(indexes);
    }

    /** 按名查列；不存在时返回 {@code null}。 */
    public @Nullable Column getColumn(String columnName) {
        for (Column column : columns) {
            if (column.getName().equals(columnName)) {
                return column;
            }
        }
        return null;
    }

    /** 是否包含指定列。 */
    public boolean hasColumn(String columnName) {
        return getColumn(columnName) != null;
    }

    /** 新增列；同名已存在时先移除（保持新列追加到末尾）。 */
    public void addColumn(Column column) {
        removeColumn(column.getName());
        columns.add(column);
    }

    /** 删除列；不存在时静默。 */
    public void removeColumn(String columnName) {
        columns.removeIf(column -> column.getName().equals(columnName));
    }

    /** 列改名：原地替换为新名副本，保持列位置与元数据。 */
    public void renameColumn(String from, String to) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equals(from)) {
                columns.set(i, columns.get(i).renamedTo(to));
                return;
            }
        }
    }

    /** 按名替换列（改类型/可空性等）；目标列不存在时静默。新列插入旧列位置。 */
    public void replaceColumn(String oldName, Column newColumn) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getName().equals(oldName)) {
                columns.set(i, newColumn);
                return;
            }
        }
    }

    /** 按名查索引；不存在时返回 {@code null}。 */
    public @Nullable Index getIndex(String indexName) {
        for (Index index : indexes) {
            if (index.getName().equals(indexName)) {
                return index;
            }
        }
        return null;
    }

    /** 是否包含指定索引。 */
    public boolean hasIndex(String indexName) {
        return getIndex(indexName) != null;
    }

    /** 新增索引；同名已存在时先移除（保持新索引追加到末尾）。 */
    public void addIndex(Index index) {
        removeIndex(index.getName());
        indexes.add(index);
    }

    /** 删除索引；不存在时静默。 */
    public void removeIndex(String indexName) {
        indexes.removeIf(index -> index.getName().equals(indexName));
    }

    /** 索引改名：原地替换为新名副本，保持位置与元数据。 */
    public void renameIndex(String from, String to) {
        for (int i = 0; i < indexes.size(); i++) {
            if (indexes.get(i).getName().equals(from)) {
                indexes.set(i, indexes.get(i).renamedTo(to));
                return;
            }
        }
    }

    /** 表元数据（表级 DDL 注解结果写入这里，开放读写）。 */
    public Meta getMeta() {
        return meta;
    }

    /** 就地改名（同包 {@link Schema} 专用，保持名称与注册表一致）。 */
    void rename(String newName) {
        this.name = newName;
    }

}
