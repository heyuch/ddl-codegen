package hyc.codegen.core.ddl;

import java.util.Arrays;
import java.util.List;

import hyc.codegen.core.model.Schema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 语句应用：按顺序累积到 Schema，rename 产出记录、drop 移除表。
 */
class StatementApplierTest {

    private final DruidDdlParser parser = new DruidDdlParser();
    private final StatementApplier applier = new StatementApplier();

    @Test
    void createThenAlterInOneBatch() {
        String ddl = "CREATE TABLE t_user (\n"
                + "  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',\n"
                + "  name VARCHAR(50) COMMENT '用户名',\n"
                + "  PRIMARY KEY (id)\n"
                + ") COMMENT '用户表';\n"
                + "ALTER TABLE t_user ADD COLUMN email VARCHAR(100) COMMENT '邮箱';\n"
                + "ALTER TABLE t_user DROP COLUMN name;";

        Schema schema = new Schema();
        ApplyResult result = applier.apply(schema, parser.parse(ddl));

        assertTrue(schema.contains("t_user"));
        assertEquals(Arrays.asList("id", "email"), tableColumns(schema, "t_user"));
        // 同一批次内后续语句可见前面结果：name 被删、email 新增
        assertEquals(Arrays.asList("t_user"), result.getAffectedTables());
    }

    @Test
    void dropRemovesTableAndRecords() {
        Schema schema = new Schema();
        schema.addTable(new hyc.codegen.core.model.Table("t_user", null));

        ApplyResult result = applier.apply(schema, parser.parse("DROP TABLE t_user"));

        assertFalse(schema.contains("t_user"));
        assertEquals(Arrays.asList("t_user"), result.getDroppedTables());
    }

    @Test
    void renameTableProducesRenameRecord() {
        Schema schema = new Schema();
        schema.addTable(new hyc.codegen.core.model.Table("t_user", null));

        ApplyResult result = applier.apply(schema, parser.parse("ALTER TABLE t_user RENAME TO t_account"));

        assertFalse(schema.contains("t_user"));
        assertTrue(schema.contains("t_account"));
        hyc.codegen.core.model.Table renamed = schema.getTable("t_account");
        assertNotNull(renamed);
        assertEquals("t_account", renamed.getName());

        assertEquals(1, result.getTableRenames().size());
        ApplyResult.TableRename rename = result.getTableRenames().get(0);
        assertEquals("t_user", rename.getFrom());
        assertEquals("t_account", rename.getTo());
    }

    @Test
    void columnAndIndexRenamesRecorded() {
        Schema schema = new Schema();
        hyc.codegen.core.model.Table table = new hyc.codegen.core.model.Table("t_user", null);
        table.addColumn(hyc.codegen.core.model.Column.builder().name("create_time").sqlType("datetime").build());
        table.addIndex(hyc.codegen.core.model.Index.builder().name("idx_a").columns(Arrays.asList("a")).build());
        schema.addTable(table);

        String ddl = "ALTER TABLE t_user RENAME COLUMN create_time TO created_at, "
                + "RENAME INDEX idx_a TO idx_b";
        ApplyResult result = applier.apply(schema, parser.parse(ddl));

        hyc.codegen.core.model.Table tUser = schema.getTable("t_user");
        assertNotNull(tUser);
        assertTrue(tUser.hasColumn("created_at"));
        assertNull(tUser.getColumn("create_time"));
        assertTrue(tUser.hasIndex("idx_b"));

        assertEquals(1, result.getColumnRenames().size());
        assertEquals("create_time", result.getColumnRenames().get(0).getFrom());
        assertEquals(1, result.getIndexRenames().size());
        assertEquals("idx_a", result.getIndexRenames().get(0).getFrom());
    }

    @Test
    void alterOnMissingTableIsNoOp() {
        Schema schema = new Schema();

        ApplyResult result = applier.apply(schema, parser.parse("ALTER TABLE ghost ADD COLUMN x INT"));

        assertFalse(schema.contains("ghost"));
        assertTrue(result.getAffectedTables().isEmpty());
    }

    private List<String> tableColumns(Schema schema, String tableName) {
        List<String> result = new java.util.ArrayList<>();
        hyc.codegen.core.model.Table table = schema.getTable(tableName);
        if (table == null) {
            throw new AssertionError("表 " + tableName + " 应存在");
        }
        for (hyc.codegen.core.model.Column column : table.getColumns()) {
            result.add(column.getName());
        }
        return result;
    }

}
