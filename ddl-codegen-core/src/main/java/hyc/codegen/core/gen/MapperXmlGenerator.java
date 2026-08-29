package hyc.codegen.core.gen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import hyc.codegen.core.io.ChangeStatus;
import hyc.codegen.core.io.FileWriter;
import hyc.codegen.core.model.Column;
import hyc.codegen.core.model.Index;
import hyc.codegen.core.naming.NamingService;

/**
 * MyBatis Mapper XML 生成器（kind {@code mybatisXml}，整文件重生成，字符串模板）。
 * <p>
 * 结构：BaseResultMap（property=字段名 column=列名 jdbcType）、BaseColumnList（t. 别名）、
 * insert（自增主键不入列 + useGeneratedKeys 回填）、deleteById、update（不含主键列）、
 * findById 与索引派生的 findBy*（等值 AND 连接）。XML 方法 id 与 Mapper 接口一致。
 */
public final class MapperXmlGenerator implements ArtifactGenerator {

    /** 生成器注册名。 */
    public static final String NAME = "mybatisXml";

    @Override
    public String kind() {
        return NAME;
    }

    @Override
    public void generate(TableContext ctx, GenerationContext gctx) {
        hyc.codegen.core.config.ArtifactConfig mapper = gctx.resolveReference(
                ctx.getArtifactName(), "mapper", MapperGenerator.NAME);
        String mapperFqn = gctx.refFqn(ctx.getTable().getName(), mapper);
        String mapperName = simpleName(mapperFqn);
        String namespace = mapperFqn;
        hyc.codegen.core.config.ArtifactConfig target = gctx.resolveReference(
                ctx.getArtifactName(), "target", FieldArtifactGenerator.NAME);
        String poType = gctx.refFqn(ctx.getTable().getName(), target);

        String xml = build(ctx, gctx.getNaming(), poType, namespace);
        Path file = ctx.xmlFile(gctx.getProjectRoot(), ctx.getArtifactConfig().getPath(), mapperName + ".xml");
        try {
            ChangeStatus status = FileWriter.writeIfChanged(file, xml);
            gctx.getReport().add(file, status, "mybatisXml " + mapperName);
        } catch (IOException e) {
            throw new IllegalStateException("写 XML 失败: " + file, e);
        }
    }

    private String build(TableContext ctx, NamingService naming, String poType, String namespace) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        sb.append("<!DOCTYPE mapper\n");
        sb.append("        PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\"\n");
        sb.append("        \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\" >\n");
        sb.append("<mapper namespace=\"").append(namespace).append("\">\n\n");

        String tableName = ctx.getTable().getName();
        Column id = idColumn(ctx);
        List<Column> columns = visibleColumns(ctx);

        resultMap(ctx, columns, id, poType, sb);
        baseColumnList(ctx, columns, sb);

        sb.append(insertXml(ctx, tableName, columns, id, poType));
        sb.append("\n");
        if (id != null) {
            sb.append(deleteXml(tableName, id, ctx));
            sb.append("\n");
            sb.append(updateXml(ctx, tableName, columns, id, poType));
            sb.append("\n");
            sb.append(selectXml(ctx, tableName, "findById", id.getName()));
            sb.append("\n");
        }

        for (Index index : ctx.indexes()) {
            if (MetaSupport.isIgnored(index)) {
                continue;
            }
            for (QueryMethods.Spec spec : QueryMethods.of(index, naming)) {
                if ("findById".equals(spec.getMethodName())) {
                    continue;
                }
                sb.append(selectXml(ctx, tableName, spec.getMethodName(),
                        spec.getColumns().toArray(new String[0])));
                sb.append("\n");
            }
        }

        sb.append("</mapper>\n");
        return sb.toString();
    }

    private void resultMap(TableContext ctx, List<Column> columns, Column id, String poType, StringBuilder sb) {
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

    private void baseColumnList(TableContext ctx, List<Column> columns, StringBuilder sb) {
        sb.append("    <sql id=\"BaseColumnList\">\n");
        for (int i = 0; i < columns.size(); i++) {
            sb.append("        t.").append(columns.get(i).getName());
            sb.append(i < columns.size() - 1 ? "," : "");
            sb.append("\n");
        }
        sb.append("    </sql>\n\n");
    }

    private String insertXml(TableContext ctx, String tableName, List<Column> columns, Column id, String poType) {
        boolean generated = id != null && id.isAutoIncrement();
        StringBuilder sb = new StringBuilder();
        sb.append("    <insert id=\"insert\"");
        if (generated) {
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

        sb.append("        INSERT INTO ").append(tableName).append("\n");
        sb.append("        (\n");
        for (Column column : columns) {
            if (column.isAutoIncrement()) {
                continue;
            }
            sb.append("        ").append(column.getName()).append(",\n");
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

    private String deleteXml(String tableName, Column id, TableContext ctx) {
        return "    <delete id=\"deleteById\">\n"
                + "        DELETE FROM\n"
                + "        " + tableName + "\n"
                + "        WHERE\n"
                + "        " + id.getName() + " = #{" + ctx.fieldName(id) + ",jdbcType=" + ctx.jdbcType(id) + "}\n"
                + "    </delete>\n";
    }

    private String updateXml(TableContext ctx, String tableName, List<Column> columns, Column id, String poType) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <update id=\"update\" parameterType=\"").append(poType).append("\">\n");
        sb.append("        UPDATE\n");
        sb.append("        ").append(tableName).append("\n");
        sb.append("        SET\n");
        for (Column column : columns) {
            if (column == id) {
                continue;
            }
            sb.append("        ")
                    .append(column.getName())
                    .append(" = #{")
                    .append(ctx.fieldName(column))
                    .append(",jdbcType=")
                    .append(ctx.jdbcType(column))
                    .append("},\n");
        }
        stripTrailingComma(sb);
        sb.append("        WHERE\n");
        sb.append("        ")
                .append(id.getName())
                .append(" = #{")
                .append(ctx.fieldName(id))
                .append(",jdbcType=")
                .append(ctx.jdbcType(id))
                .append("}\n");
        sb.append("    </update>\n");
        return sb.toString();
    }

    private String selectXml(TableContext ctx, String tableName, String methodId, String... whereColumns) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <select id=\"").append(methodId).append("\" resultMap=\"BaseResultMap\">\n");
        sb.append("        SELECT\n");
        sb.append("        <include refid=\"BaseColumnList\"/>\n");
        sb.append("        FROM\n");
        sb.append("        ").append(tableName).append(" t\n");
        sb.append("        WHERE\n");
        for (int i = 0; i < whereColumns.length; i++) {
            Column column = ctx.getTable().getColumn(whereColumns[i]);
            sb.append("        t.")
                    .append(column.getName())
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

    /** 主键列（PRIMARY 索引首列；无则 null）。 */
    private Column idColumn(TableContext ctx) {
        for (Index index : ctx.indexes()) {
            if (index.isUnique() && "PRIMARY".equalsIgnoreCase(index.getName())) {
                return ctx.getTable().getColumn(index.getColumns().get(0));
            }
        }
        return null;
    }

    private List<Column> visibleColumns(TableContext ctx) {
        List<Column> columns = new ArrayList<>();
        for (Column column : ctx.columns()) {
            if (!MetaSupport.isIgnored(column)) {
                columns.add(column);
            }
        }
        return columns;
    }

    private void stripTrailingComma(StringBuilder sb) {
        int len = sb.length();
        if (len >= 2 && sb.charAt(len - 2) == ',' && sb.charAt(len - 1) == '\n') {
            sb.setLength(len - 2);
            sb.append('\n');
        }
    }

    private static String simpleName(String fqn) {
        int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }

}
