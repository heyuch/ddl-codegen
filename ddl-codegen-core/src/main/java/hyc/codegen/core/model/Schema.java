package hyc.codegen.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 内存中的数据库模式：按表名注册，保持表创建顺序。
 * <p>
 * 同一批次 DDL 语句按顺序应用到这里，后续语句可见先前语句的结果。
 */
public final class Schema {

    private final Map<String, Table> tables = new LinkedHashMap<>();

    /** 新增/替换表（同名替换保持原位置）。 */
    public void addTable(Table table) {
        tables.put(table.getName(), table);
    }

    /** 按名取表；不存在时返回 {@code null}。 */
    public @Nullable Table getTable(String name) {
        return tables.get(name);
    }

    /** 是否包含指定表。 */
    public boolean contains(String name) {
        return tables.containsKey(name);
    }

    /** 删除表；不存在时静默。 */
    public void removeTable(String name) {
        tables.remove(name);
    }

    /** 表改名（移除后追加到末尾）。 */
    public void renameTable(String from, String to) {
        Table table = tables.remove(from);
        if (table != null) {
            table.rename(to);
            tables.put(to, table);
        }
    }

    /** 全部表名（创建顺序）。 */
    public List<String> tableNames() {
        return new ArrayList<>(tables.keySet());
    }

    /** 全部表（创建顺序）。 */
    public List<Table> tables() {
        return new ArrayList<>(tables.values());
    }

}
