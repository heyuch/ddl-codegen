package hyc.codegen.core.ddl;

import java.util.Arrays;
import java.util.List;

import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;
import hyc.codegen.core.model.Table;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Druid 解析适配：create/alter/drop/rename 语句 → 规范化模型操作。
 */
class DruidDdlParserTest {

    private final DruidDdlParser parser = new DruidDdlParser();

    @Test
    void createTableWithFullFeatures() {
        String ddl = "CREATE TABLE t_user (\n"
                + "  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',\n"
                + "  name VARCHAR(50) NOT NULL DEFAULT '' COMMENT '用户名',\n"
                + "  gender ENUM('male','female') NOT NULL COMMENT '性别',\n"
                + "  credits DECIMAL(10,2) DEFAULT 0.00 COMMENT '积分',\n"
                + "  ext_info VARCHAR(255) COMMENT '@type:UserExtInfo',\n"
                + "  secret INT COMMENT '@ignore',\n"
                + "  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n"
                + "  PRIMARY KEY (id),\n"
                + "  UNIQUE KEY uk_name (name),\n"
                + "  KEY idx_credits (credits)\n"
                + ") COMMENT '用户表'";

        List<DdlOperation> ops = parser.parse(ddl);

        assertEquals(1, ops.size());
        CreateTableOp op = (CreateTableOp)ops.get(0);
        Table table = op.getTable();

        assertEquals("t_user", table.getName());
        assertEquals("用户表", table.getComment());

        // 列定义
        assertEquals(Arrays.asList("id", "name", "gender", "credits", "ext_info", "secret", "create_time"),
                names(table.getColumns()));
        Column id = table.getColumn("id");
        assertEquals("bigint", id.getSqlType());
        assertFalse(id.isNullable());
        assertTrue(id.isUnsigned());
        assertTrue(id.isAutoIncrement());

        Column name = table.getColumn("name");
        assertEquals("varchar", name.getSqlType());
        assertEquals(50, name.getLength());
        assertEquals("", name.getDefaultValue());

        Column gender = table.getColumn("gender");
        assertTrue(gender.isEnum());
        assertEquals(Arrays.asList("male", "female"), gender.getEnumValues());

        Column credits = table.getColumn("credits");
        assertEquals(10, credits.getPrecision());
        assertEquals(2, credits.getScale());

        // 注解元数据
        assertEquals("UserExtInfo", table.getColumn("ext_info").getMeta().getString("type"));
        assertTrue(table.getColumn("secret").getMeta().isTrue("ignore"));

        // 索引
        assertEquals(3, table.getIndexes().size());
        assertTrue(table.getIndex("PRIMARY").isUnique());
        assertEquals(Arrays.asList("id"), table.getIndex("PRIMARY").getColumns());
        assertTrue(table.getIndex("uk_name").isUnique());
        assertFalse(table.getIndex("idx_credits").isUnique());
    }

    @Test
    void tableLevelAsAnnotation() {
        String ddl = "CREATE TABLE t_account (id BIGINT NOT NULL, PRIMARY KEY (id)) COMMENT '账户表 @as:Account'";

        Table table = ((CreateTableOp)parser.parse(ddl).get(0)).getTable();

        assertEquals("Account", table.getMeta().getString("as"));
    }

    @Test
    void unknownAnnotationDoesNotBreakParse() {
        String ddl = "CREATE TABLE t_x (id BIGINT NOT NULL COMMENT '@boolean', PRIMARY KEY (id))";

        Table table = ((CreateTableOp)parser.parse(ddl).get(0)).getTable();

        assertNull(table.getColumn("id").getMeta().getString("type"));
        assertEquals("t_x", table.getName());
    }

    @Test
    void addIndexWithIgnoreAnnotation() {
        // PIT 抓到的缺口：ALTER ADD INDEX 的注解处理路径未被测试
        String ddl = "ALTER TABLE t_user ADD INDEX idx_name (name) COMMENT '@ignore'";
        List<DdlOperation> ops = parser.parse(ddl);

        assertEquals(1, ops.size());
        assertTrue(ops.get(0) instanceof AddIndexOp);
        Index index = ((AddIndexOp)ops.get(0)).getIndex();
        assertNotNull(index.getMeta().get("ignore"), "索引注释 @ignore 应被解析");
    }

    @Test
    void unnamedIndexGetsSynthesizedName() {
        String ddl = "CREATE TABLE t_y (a INT, b INT, KEY (a), UNIQUE (b))";

        Table table = ((CreateTableOp)parser.parse(ddl).get(0)).getTable();

        assertTrue(table.hasIndex("idx_a"));
        assertTrue(table.hasIndex("uk_b"));
    }

    @Test
    void alterTableAllForms() {
        String ddl = "ALTER TABLE t_user\n"
                + "  ADD COLUMN email VARCHAR(100) COMMENT '邮箱',\n"
                + "  DROP COLUMN secret,\n"
                + "  MODIFY COLUMN name VARCHAR(100) NOT NULL,\n"
                + "  CHANGE COLUMN credits points DECIMAL(12,2),\n"
                + "  ADD INDEX idx_email (email),\n"
                + "  DROP INDEX idx_credits,\n"
                + "  RENAME COLUMN create_time TO created_at";

        List<DdlOperation> ops = parser.parse(ddl);

        assertEquals(7, ops.size());
        assertEquals("t_user", ops.get(0).tableName());

        assertTrue(ops.get(0) instanceof AddColumnOp);
        assertEquals("email", ((AddColumnOp)ops.get(0)).getColumn().getName());

        assertTrue(ops.get(1) instanceof DropColumnOp);
        assertEquals("secret", ((DropColumnOp)ops.get(1)).getColumnName());

        assertTrue(ops.get(2) instanceof ChangeColumnOp);
        ChangeColumnOp modify = (ChangeColumnOp)ops.get(2);
        assertEquals("name", modify.getOldName());
        assertEquals("name", modify.getNewColumn().getName());
        assertEquals(100, modify.getNewColumn().getLength());

        assertTrue(ops.get(3) instanceof ChangeColumnOp);
        ChangeColumnOp change = (ChangeColumnOp)ops.get(3);
        assertEquals("credits", change.getOldName());
        assertEquals("points", change.getNewColumn().getName());
        assertEquals(12, change.getNewColumn().getPrecision());

        assertTrue(ops.get(4) instanceof AddIndexOp);
        assertEquals("idx_email", ((AddIndexOp)ops.get(4)).getIndex().getName());

        assertTrue(ops.get(5) instanceof DropIndexOp);
        assertEquals("idx_credits", ((DropIndexOp)ops.get(5)).getIndexName());

        assertTrue(ops.get(6) instanceof RenameColumnOp);
        RenameColumnOp renameColumn = (RenameColumnOp)ops.get(6);
        assertEquals("create_time", renameColumn.getFrom());
        assertEquals("created_at", renameColumn.getTo());
    }

    @Test
    void renameTableStatement() {
        String ddl = "ALTER TABLE t_user RENAME TO t_account";

        List<DdlOperation> ops = parser.parse(ddl);

        assertEquals(1, ops.size());
        assertTrue(ops.get(0) instanceof RenameTableOp);
        RenameTableOp rename = (RenameTableOp)ops.get(0);
        assertEquals("t_user", rename.getFrom());
        assertEquals("t_account", rename.getTo());
    }

    @Test
    void dropTableStatement() {
        String ddl = "DROP TABLE IF EXISTS t_user";

        List<DdlOperation> ops = parser.parse(ddl);

        assertEquals(1, ops.size());
        DropTableOp drop = (DropTableOp)ops.get(0);
        assertEquals("t_user", drop.tableName());
    }

    private List<String> names(List<Column> columns) {
        List<String> result = new java.util.ArrayList<>();
        for (Column column : columns) {
            result.add(column.getName());
        }
        return result;
    }

}
