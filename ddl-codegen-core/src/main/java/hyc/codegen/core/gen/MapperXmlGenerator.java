package hyc.codegen.core.gen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import hyc.codegen.core.io.ChangeStatus;
import hyc.codegen.core.io.FileWriter;
import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;
import hyc.codegen.core.naming.NamingService;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * MyBatis Mapper XML 生成器（kind {@code mybatisXml}，整文件重生成，字符串模板）。
 * <p>
 * 结构：BaseResultMap（property=字段名 column=列名 jdbcType）、BaseColumnList（t. 别名）、
 * insert（自增主键不入列 + useGeneratedKeys 回填）、deleteById、update（不含主键列）、
 * findById 与索引派生的 findBy*（等值 AND 连接）。XML 方法 id 与 Mapper 接口一致。
 */
public final class MapperXmlGenerator implements Generator {

    /** 生成器注册名。 */
    public static final String NAME = "mybatisXml";

    /**
     * MySQL 8.0 RESERVED 关键字（官方 Keywords and Reserved Words 表，2023-07 版）。
     * 本生成器是唯一与 MySQL 打交道的一方：SQL 文本中的表名/列名为保留字时须反引号包裹
     * （模型在 parse 层已剥引号存真名，见 20260906-11）。Java 侧命名不关心此表。
     */
    private static final Set<String> MYSQL_RESERVED = new HashSet<>(Arrays.asList(
            ("ACCESSIBLE ADD ALL ALTER ANALYZE AND AS ASC ASENSITIVE BEFORE BETWEEN BIGINT BINARY BLOB BOTH "
                    + "BY CALL CASCADE CASE CHANGE CHAR CHARACTER CHECK COLLATE COLUMN CONDITION CONSTRAINT "
                    + "CONTINUE CONVERT CREATE CROSS CUBE CUME_DIST CURRENT_DATE CURRENT_TIME CURRENT_TIMESTAMP "
                    + "CURRENT_USER CURSOR DATABASE DATABASES DAY_HOUR DAY_MICROSECOND DAY_MINUTE DAY_SECOND DEC "
                    + "DECIMAL DECLARE DEFAULT DELAYED DELETE DENSE_RANK DESC DESCRIBE DETERMINISTIC DISTINCT "
                    + "DISTINCTROW DIV DOUBLE DROP DUAL EACH ELSE ELSEIF EMPTY ENCLOSED ESCAPED EXISTS EXPLAIN "
                    + "FALSE FETCH FIRST FLOAT FLOAT4 FLOAT8 FOR FORCE FOREIGN FROM FULLTEXT FUNCTION GENERATED "
                    + "GET GRANT GROUP GROUPING GROUPS HAVING HIGH_PRIORITY HOUR_MICROSECOND HOUR_MINUTE "
                    + "HOUR_SECOND IF IGNORE IN INDEX INFILE INNER INOUT INSENSITIVE INSERT INT INT1 INT2 INT3 "
                    + "INT4 INT8 INTEGER INTERVAL INTO IO_AFTER_GTIDS IO_BEFORE_GTIDS IS ITERATE JOIN JSON_TABLE "
                    + "KEY KEYS KILL LAG LAST_VALUE LEAD LEADING LEAVE LEFT LIKE LIMIT LINEAR LINES LOAD "
                    + "LOCALTIME LOCALTIMESTAMP LOCK LONG LONGBLOB LONGTEXT LOOP LOW_PRIORITY MASTER_BIND "
                    + "MASTER_SSL_VERIFY_SERVER_CERT MATCH MAXVALUE MEDIUMBLOB MEDIUMINT MEDIUMTEXT MIDDLEINT "
                    + "MINUTE_MICROSECOND MINUTE_SECOND MOD MODIFIES NATURAL NOT NO_WRITE_TO_BINLOG NTH_VALUE "
                    + "NTILE NULL NUMERIC OF ON OPTIMIZE OPTIMIZER_COSTS OPTION OPTIONALLY OR ORDER OUT OUTFILE "
                    + "OVER PARTITION PERCENT_RANK PRECEDING PRIMARY PROCEDURE PURGE RANGE RANK READ READS "
                    + "READ_WRITE REAL RECURSIVE REFERENCES REGEXP RELEASE RENAME REPEAT REPLACE REQUIRE "
                    + "RESIGNAL RESTRICT RETURN REVOKE RIGHT RLIKE ROW ROWS ROW_NUMBER SCHEMA SCHEMAS "
                    + "SECOND_MICROSECOND SELECT SENSITIVE SEPARATOR SET SHOW SIGNAL SMALLINT SPATIAL SPECIFIC "
                    + "SQL SQLEXCEPTION SQLSTATE SQLWARNING SQL_BIG_RESULT SQL_CALC_FOUND_ROWS SQL_SMALL_RESULT "
                    + "SSL STARTING STORED STRAIGHT_JOIN SYSTEM TABLE TERMINATED THEN TINYBLOB TINYINT TINYTEXT "
                    + "TO TRAILING TRIGGER TRUE UNDO UNION UNIQUE UNLOCK UNSIGNED UPDATE USAGE USE USING "
                    + "UTC_DATE UTC_TIME UTC_TIMESTAMP VALUES VARBINARY VARCHAR VARCHARACTER VARYING VIRTUAL "
                    + "WHEN WHERE WHILE WINDOW WITH WRITE XOR YEAR_MONTH ZEROFILL").split(" ")));

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }

    /** MySQL 保留字 → 反引号包裹（大小写不敏感）；非保留字原样输出。 */
    private static String sqlName(String name) {
        return MYSQL_RESERVED.contains(name.toUpperCase(Locale.ROOT)) ? "`" + name + "`" : name;
    }

    private void baseColumnList(List<Column> columns, StringBuilder sb) {
        sb.append("    <sql id=\"BaseColumnList\">\n");
        for (int i = 0; i < columns.size(); i++) {
            sb.append("        t.").append(sqlName(columns.get(i).getName()));
            sb.append(i < columns.size() - 1 ? "," : "");
            sb.append("\n");
        }
        sb.append("    </sql>\n\n");
    }

    private String build(TableContext ctx, NamingService naming, String poType, String namespace) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        sb.append("<!DOCTYPE mapper\n");
        sb.append("        PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\"\n");
        sb.append("        \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\" >\n");
        sb.append("<mapper namespace=\"").append(namespace).append("\">\n\n");

        String tableName = ctx.getTable().getName();
        String tableSql = sqlName(tableName);
        Column id = idColumn(ctx);
        List<Column> columns = visibleColumns(ctx);

        resultMap(ctx, columns, id, poType, sb);
        baseColumnList(columns, sb);

        sb.append(insertXml(ctx, tableSql, columns, id, poType));
        sb.append("\n");
        if (id != null) {
            sb.append(deleteXml(tableSql, id, ctx));
            sb.append("\n");
            sb.append(updateXml(ctx, tableSql, columns, id, poType));
            sb.append("\n");
        }

        for (Index index : ctx.indexes()) {
            for (QueryMethods.Spec spec : QueryMethods.of(index, naming)) {
                // PRIMARY 索引 spec 天然产出 findBy<IdPascal>（pk 列名 id → findById）
                sb.append(selectXml(ctx, tableSql, spec.getMethodName(),
                        spec.getColumns().toArray(new String[0])));
                sb.append("\n");
            }
        }

        sb.append("</mapper>\n");
        return sb.toString();
    }

    /** XML 无 Java 类/字段，查询契约不适用。 */
    @Override
    public String className(TableContext ctx) {
        throw new UnsupportedOperationException("XML 产物无类名");
    }

    private String deleteXml(String tableSql, Column id, TableContext ctx) {
        return "    <delete id=\"deleteById\">\n"
                + "        DELETE FROM\n"
                + "        " + tableSql + "\n"
                + "        WHERE\n"
                + "        " + sqlName(id.getName()) + " = #{" + ctx.fieldName(id) + ",jdbcType=" + ctx.jdbcType(id)
                + "}\n"
                + "    </delete>\n";
    }

    @Override
    public String fieldName(Column column, TableContext ctx) {
        throw new UnsupportedOperationException("XML 产物无字段");
    }

    @Override
    public String fieldType(Column column, TableContext ctx) {
        throw new UnsupportedOperationException("XML 产物无成员类型");
    }

    @Override
    public void generate(TableContext ctx, GenerationContext gctx) {
        hyc.codegen.core.config.ArtifactConfig mapper = gctx.resolveReference(
                ctx.getArtifactName(), "mapper", MapperGenerator.NAME);
        String mapperFqn = gctx.refFqn(ctx.getTable().getName(), mapper);
        String mapperName = simpleName(mapperFqn);
        String namespace = mapperFqn;
        hyc.codegen.core.config.ArtifactConfig target = gctx.resolveReference(
                ctx.getArtifactName(), "target", PojoGenerator.NAME);
        String poType = gctx.refFqn(ctx.getTable().getName(), target);

        String xml = build(ctx, gctx.getNaming(), poType, namespace);
        String path = ctx.getArtifactConfig().getPath();
        if (path == null) {
            throw new IllegalStateException(
                    "mybatisXml 产物 '" + ctx.getArtifactName() + "' 缺少 path 配置（XML 产物必须配置 path）");
        }
        Path file = ctx.xmlFile(gctx.getProjectRoot(), path, mapperName + ".xml");
        try {
            ChangeStatus status = FileWriter.writeIfChanged(file, xml);
            gctx.getReport().add(file, status, "mybatisXml " + mapperName);
        } catch (IOException e) {
            throw new IllegalStateException("写 XML 失败: " + file, e);
        }
    }

    /** 主键列（PRIMARY 索引首列；无则 null）。 */
    private @Nullable Column idColumn(TableContext ctx) {
        for (Index index : ctx.indexes()) {
            if (index.isUnique() && "PRIMARY".equalsIgnoreCase(index.getName())) {
                return ctx.getTable().getColumn(index.getColumns().get(0));
            }
        }
        return null;
    }

    private String insertXml(TableContext ctx, String tableSql, List<Column> columns, @Nullable Column id,
            String poType) {
        boolean generated = id != null && id.isAutoIncrement();
        StringBuilder sb = new StringBuilder();
        sb.append("    <insert id=\"insert\"");
        if (generated && id != null) {
            // keyColumn 是 JDBC 回填元数据（驱动按名匹配），非 SQL 语句文本，不加引
            sb.append("\n            keyColumn=\"")
                    .append(id.getName())
                    .append("\"\n            keyProperty=\"")
                    .append(ctx.fieldName(id))
                    .append("\"\n            parameterType=\"")
                    .append(poType)
                    .append("\"");
        } else {
            sb.append("\n            parameterType=\"").append(poType).append("\"");
        }
        sb.append("\n            useGeneratedKeys=\"").append(generated).append("\">\n\n");

        sb.append("        INSERT INTO ").append(tableSql).append("\n");
        sb.append("        (\n");
        for (Column column : columns) {
            if (column.isAutoIncrement()) {
                continue;
            }
            sb.append("        ").append(sqlName(column.getName())).append(",\n");
        }
        stripTrailingComma(sb);
        sb.append("        )\n");
        sb.append("        VALUES\n");
        sb.append("        (\n");
        for (Column column : columns) {
            if (column.isAutoIncrement()) {
                continue;
            }
            sb.append("        #{")
                    .append(ctx.fieldName(column))
                    .append(",jdbcType=")
                    .append(ctx.jdbcType(column))
                    .append("},\n");
        }
        stripTrailingComma(sb);
        sb.append("        )\n");
        sb.append("    </insert>\n");
        return sb.toString();
    }

    @Override
    public String kind() {
        return NAME;
    }

    private void resultMap(TableContext ctx, List<Column> columns, @Nullable Column id, String poType,
            StringBuilder sb) {
        sb.append("    <resultMap id=\"BaseResultMap\" type=\"").append(poType).append("\">\n");
        for (Column column : columns) {
            boolean isId = column == id;
            sb.append("        <")
                    .append(isId ? "id" : "result")
                    .append(" property=\"")
                    .append(ctx.fieldName(column))
                    .append("\" column=\"")
                    .append(column.getName())
                    .append("\" jdbcType=\"")
                    .append(ctx.jdbcType(column))
                    .append("\"/>\n");
        }
        sb.append("    </resultMap>\n\n");
    }

    private String selectXml(TableContext ctx, String tableSql, String methodId, String... whereColumns) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <select id=\"").append(methodId).append("\" resultMap=\"BaseResultMap\">\n");
        sb.append("        SELECT\n");
        sb.append("        <include refid=\"BaseColumnList\"/>\n");
        sb.append("        FROM\n");
        sb.append("        ").append(tableSql).append(" t\n");
        sb.append("        WHERE\n");
        for (int i = 0; i < whereColumns.length; i++) {
            Column column = ctx.getTable().getColumn(whereColumns[i]);
            if (column == null) {
                throw new IllegalStateException("索引列 '" + whereColumns[i] + "' 在表 '" + ctx.getTable().getName()
                        + "' 中不存在（DDL 索引引用了未定义的列）");
            }
            sb.append("        t.")
                    .append(sqlName(column.getName()))
                    .append(" = #{")
                    .append(ctx.fieldName(column))
                    .append(",jdbcType=")
                    .append(ctx.jdbcType(column))
                    .append("}");
            sb.append(i < whereColumns.length - 1 ? "\n        AND\n" : "\n");
        }
        sb.append("    </select>\n");
        return sb.toString();
    }

    private void stripTrailingComma(StringBuilder sb) {
        int len = sb.length();
        if (len >= 2 && sb.charAt(len - 2) == ',' && sb.charAt(len - 1) == '\n') {
            sb.setLength(len - 2);
            sb.append('\n');
        }
    }

    private String updateXml(TableContext ctx, String tableSql, List<Column> columns, Column id, String poType) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <update id=\"update\" parameterType=\"").append(poType).append("\">\n");
        sb.append("        UPDATE\n");
        sb.append("        ").append(tableSql).append("\n");
        sb.append("        SET\n");
        for (Column column : columns) {
            if (column == id) {
                continue;
            }
            sb.append("        ")
                    .append(sqlName(column.getName()))
                    .append(" = #{")
                    .append(ctx.fieldName(column))
                    .append(",jdbcType=")
                    .append(ctx.jdbcType(column))
                    .append("},\n");
        }
        stripTrailingComma(sb);
        sb.append("        WHERE\n");
        sb.append("        ")
                .append(sqlName(id.getName()))
                .append(" = #{")
                .append(ctx.fieldName(id))
                .append(",jdbcType=")
                .append(ctx.jdbcType(id))
                .append("}\n");
        sb.append("    </update>\n");
        return sb.toString();
    }

    private List<Column> visibleColumns(TableContext ctx) {
        List<Column> columns = new ArrayList<>();
        for (Column column : ctx.columns()) {
            columns.add(column);
        }
        return columns;
    }

}
