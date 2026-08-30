package hyc.codegen.core.model;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SchemaModel 基础行为：列/索引增删改查、改名、顺序保持、元数据容器。
 */
class SchemaModelTest {

    private Column column(String name) {
        return Column.builder().name(name).sqlType("varchar").length(50).build();
    }

    @Test
    void schemaAddGetRemoveTable() {
        Schema schema = new Schema();
        schema.addTable(new Table("user", "用户表"));
        schema.addTable(new Table("account", null));

        assertTrue(schema.contains("user"));
        Table user = schema.getTable("user");
        assertNotNull(user);
        assertEquals("用户表", user.getComment());
        assertNull(schema.getTable("missing"));
        assertEquals(Arrays.asList("user", "account"), schema.tableNames());

        schema.removeTable("user");
        assertFalse(schema.contains("user"));
    }

    @Test
    void renameTableMovesToEnd() {
        Schema schema = new Schema();
        schema.addTable(new Table("a", null));
        schema.addTable(new Table("b", null));
        schema.renameTable("a", "c");

        assertFalse(schema.contains("a"));
        Table c = schema.getTable("c");
        assertNotNull(c);
        assertEquals("c", c.getName());
        // LinkedHashMap 改名采用移除后追加语义
        assertEquals(Arrays.asList("b", "c"), schema.tableNames());
    }

    @Test
    void addAndRemoveColumnPreservesOrder() {
        Table table = new Table("user", null);
        table.addColumn(column("id"));
        table.addColumn(column("name"));
        table.addColumn(column("email"));

        assertEquals(Arrays.asList("id", "name", "email"), columnNames(table));
        assertTrue(table.hasColumn("name"));

        table.removeColumn("name");
        assertEquals(Arrays.asList("id", "email"), columnNames(table));

        // 同名重新添加 → 追加到末尾
        table.addColumn(column("id"));
        assertEquals(Arrays.asList("email", "id"), columnNames(table));
    }

    @Test
    void renameColumnKeepsPositionAndMeta() {
        Table table = new Table("user", null);
        Column name = column("name");
        name.getMeta().put("type", "java.lang.String");
        table.addColumn(name);

        table.renameColumn("name", "userName");

        Column renamed = table.getColumn("userName");
        assertNotNull(renamed);
        assertEquals("userName", renamed.getName());
        assertEquals(50, renamed.getLength());
        assertEquals("java.lang.String", renamed.getMeta().getString("type"));
        assertNull(table.getColumn("name"));
    }

    @Test
    void replaceColumnKeepsPosition() {
        Table table = new Table("user", null);
        table.addColumn(column("id"));
        table.addColumn(column("name"));
        table.addColumn(column("email"));

        table.replaceColumn("name", Column.builder().name("name").sqlType("varchar").length(100).build());

        List<Column> columns = table.getColumns();
        assertEquals(3, columns.size());
        assertEquals(100, columns.get(1).getLength());
    }

    @Test
    void indexAddRemoveRename() {
        Table table = new Table("user", null);
        table.addIndex(Index.builder().name("uk_name").unique(true).columns(Arrays.asList("name")).build());
        table.addIndex(Index.builder().name("idx_status").columns(Arrays.asList("status")).build());

        Index ukName = table.getIndex("uk_name");
        assertNotNull(ukName);
        assertTrue(ukName.isUnique());
        Index idxStatus = table.getIndex("idx_status");
        assertNotNull(idxStatus);
        assertFalse(idxStatus.isUnique());

        table.renameIndex("idx_status", "idx_state");
        assertNull(table.getIndex("idx_status"));
        Index idxState = table.getIndex("idx_state");
        assertNotNull(idxState);
        assertEquals(Arrays.asList("status"), idxState.getColumns());

        table.removeIndex("uk_name");
        assertFalse(table.hasIndex("uk_name"));
    }

    @Test
    void metaIsOpenContainer() {
        Meta meta = new Meta();
        meta.put("type", "UserExtInfo");
        meta.put("ignore", Boolean.TRUE);

        assertEquals("UserExtInfo", meta.getString("type"));
        assertTrue(meta.isTrue("ignore"));
        assertFalse(meta.isTrue("missing"));
        assertEquals(2, meta.asMap().size());
    }

    private List<String> columnNames(Table table) {
        List<String> result = new java.util.ArrayList<>();
        for (Column column : table.getColumns()) {
            result.add(column.getName());
        }
        return result;
    }

}
