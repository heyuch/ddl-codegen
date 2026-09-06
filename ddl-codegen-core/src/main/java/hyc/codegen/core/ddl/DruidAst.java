package hyc.codegen.core.ddl;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLDataType;
import com.alibaba.druid.sql.ast.SQLDataTypeImpl;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLName;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Druid AST 的取值辅助：从名称/注释/类型参数/排序列中提取模型需要的字符串与整数。
 * <p>
 * 包内可见，仅供 {@link DruidDdlParser} 使用。所有方法对空值宽容（返回 {@code null}/空列表），
 * 取值失败时回退到 toString 并剥离反引号/引号。
 */
final class DruidAst {

    private DruidAst() {}

    /** 提取建列名（剥反引号；列定义语法必有名称，getColumnName() 与原始行为一致非 null 才可用）。 */
    static String columnName(SQLColumnDefinition definition) {
        return stripBackticks(definition.getColumnName());
    }

    /** 提取排序列的列名列表（{@code (a, b)} 形式）。 */
    static List<String> columnNames(List<SQLSelectOrderByItem> items) {
        List<String> names = new ArrayList<>();
        for (SQLSelectOrderByItem item : items) {
            SQLExpr expr = item.getExpr();
            if (expr instanceof SQLIdentifierExpr) {
                // getSimpleName() 保留反引号（实证），须剥除使索引列名与模型列名一致
                names.add(stripBackticks(((SQLIdentifierExpr)expr).getSimpleName()));
            } else if (expr != null) {
                names.add(stripBackticks(expr.toString()));
            }
        }
        return names;
    }

    /** 提取注释文本（SQLCharExpr 取原生值，其余回退 toString 去引号）。 */
    static @Nullable String commentOf(@Nullable SQLExpr comment) {
        return textOf(comment);
    }

    /** 提取类型参数中指定下标的整数（如 varchar(50) 的 50、decimal(10,2) 的 10）。越界返回 0。 */
    static int intArgument(SQLDataType dataType, int index) {
        List<SQLExpr> arguments = dataType.getArguments();
        if (arguments == null || index >= arguments.size()) {
            return 0;
        }
        SQLExpr argument = arguments.get(index);
        if (argument instanceof SQLIntegerExpr) {
            return ((SQLIntegerExpr)argument).getNumber().intValue();
        }
        try {
            return Integer.parseInt(argument.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 提取名称（去掉反引号与库名前缀，如 {@code db.t_user} → {@code t_user}）。
     * 反引号在 parse 层统一剥除：模型只存真名，MySQL 引号语义不流入 codegen（20260906-11）。
     */
    static @Nullable String nameOf(@Nullable SQLName name) {
        if (name == null) {
            return null;
        }
        if (name instanceof SQLIdentifierExpr) {
            // getSimpleName() 保留反引号（实证），须剥除
            return stripBackticks(((SQLIdentifierExpr)name).getSimpleName());
        }
        String text = name.toString();
        int dot = text.lastIndexOf('.');
        return stripBackticks(dot >= 0 ? text.substring(dot + 1) : text);
    }

    /** 提取类型参数中指定下标的字符串（如 enum('male') 的 male）。越界或非字符时回退 toString。 */
    static @Nullable String stringArgument(SQLDataType dataType, int index) {
        List<SQLExpr> arguments = dataType.getArguments();
        if (arguments == null || index >= arguments.size()) {
            return null;
        }
        SQLExpr argument = arguments.get(index);
        if (argument instanceof SQLCharExpr) {
            return String.valueOf(((SQLCharExpr)argument).getValue());
        }
        return stripBackticks(argument.toString());
    }

    private static String stripBackticks(String text) {
        return text.replace("`", "");
    }

    /** 提取字面量文本（SQLCharExpr 取原生值，其余回退 toString）。 */
    static @Nullable String textOf(@Nullable SQLExpr expr) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof SQLCharExpr) {
            return String.valueOf(((SQLCharExpr)expr).getValue());
        }
        return expr.toString();
    }

    /** 是否 UNSIGNED（Druid 的 unsigned 标记在数据类型实现类上）。 */
    static boolean unsigned(SQLColumnDefinition definition) {
        SQLDataType dataType = definition.getDataType();
        return dataType instanceof SQLDataTypeImpl && ((SQLDataTypeImpl)dataType).isUnsigned();
    }

}
