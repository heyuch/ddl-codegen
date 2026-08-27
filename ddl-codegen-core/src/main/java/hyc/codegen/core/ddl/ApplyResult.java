package hyc.codegen.core.ddl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * DDL 语句应用结果：供生成阶段路由使用。
 * <p>
 * {@code affectedTables} 为本次批次中发生过任何变更的表（按首次变更顺序）；
 * rename/drop 单独记录——重命名需要生成阶段做文件移动（而非删旧建新），
 * drop 需要删除对应文件。
 */
public final class ApplyResult {

    private final LinkedHashSet<String> affectedTables = new LinkedHashSet<>();
    private final List<TableRename> tableRenames = new ArrayList<>();
    private final List<ColumnRename> columnRenames = new ArrayList<>();
    private final List<IndexRename> indexRenames = new ArrayList<>();
    private final List<String> droppedTables = new ArrayList<>();

    /** 受影响表（按首次变更顺序，去重）。 */
    public List<String> getAffectedTables() {
        return new ArrayList<>(affectedTables);
    }

    /** 表改名记录。 */
    public List<TableRename> getTableRenames() {
        return new ArrayList<>(tableRenames);
    }

    /** 列改名记录（表内）。 */
    public List<ColumnRename> getColumnRenames() {
        return new ArrayList<>(columnRenames);
    }

    /** 索引改名记录（表内）。 */
    public List<IndexRename> getIndexRenames() {
        return new ArrayList<>(indexRenames);
    }

    /** 被 drop 的表（按顺序）。 */
    public List<String> getDroppedTables() {
        return new ArrayList<>(droppedTables);
    }

    void affect(String tableName) {
        affectedTables.add(tableName);
    }

    void tableRenamed(String from, String to) {
        tableRenames.add(new TableRename(from, to));
    }

    void columnRenamed(String tableName, String from, String to) {
        columnRenames.add(new ColumnRename(tableName, from, to));
    }

    void indexRenamed(String tableName, String from, String to) {
        indexRenames.add(new IndexRename(tableName, from, to));
    }

    void dropped(String tableName) {
        droppedTables.add(tableName);
    }

    @Override
    public String toString() {
        return "ApplyResult{affected=" + affectedTables + ", dropped=" + droppedTables
                + ", renames=" + tableRenames + "}";
    }

    /** 表改名记录（{@code from → to}）。 */
    public static final class TableRename {

        private final String from;
        private final String to;

        TableRename(String from, String to) {
            this.from = from;
            this.to = to;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        @Override
        public String toString() {
            return from + " -> " + to;
        }

    }

    /** 列改名记录（表内，{@code from → to}）。 */
    public static final class ColumnRename {

        private final String tableName;
        private final String from;
        private final String to;

        ColumnRename(String tableName, String from, String to) {
            this.tableName = tableName;
            this.from = from;
            this.to = to;
        }

        public String getTableName() {
            return tableName;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        @Override
        public String toString() {
            return tableName + "." + from + " -> " + to;
        }

    }

    /** 索引改名记录（表内，{@code from → to}）。 */
    public static final class IndexRename {

        private final String tableName;
        private final String from;
        private final String to;

        IndexRename(String tableName, String from, String to) {
            this.tableName = tableName;
            this.from = from;
            this.to = to;
        }

        public String getTableName() {
            return tableName;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        @Override
        public String toString() {
            return tableName + "." + from + " -> " + to;
        }

    }

}
