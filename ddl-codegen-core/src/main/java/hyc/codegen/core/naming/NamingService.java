package hyc.codegen.core.naming;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hyc.codegen.core.config.DdlConfig;
import hyc.codegen.core.model.Index;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 命名服务：表名→类名、列名→字段名、索引→查询方法名的全部转换。
 * <p>
 * 表名变换链：剥前缀（{@code t_} → 空）→ 剥分表后缀（{@code user_0} → {@code user}）→ snake→Pascal。
 * 配置了 {@link TableNameStrategy} 时整体替换该链（逃生口）。
 * 方法名 = 配置前缀 + 索引列按序 camelCase 以 And 连接（{@code name,gender} → {@code findByNameAndGender}）。
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "服务持有 DdlConfig（命名规则只读）")
public final class NamingService {

    /** 保留字全集：Java 关键字 + 常见 SQL 保留字（列名如 {@code order} 命中时按配置追加后缀）。 */
    private static final Set<String> RESERVED_WORDS = new HashSet<>(Arrays.asList(
            // Java 关键字
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while",
            // SQL 保留字（MySQL）
            "order", "group", "desc", "asc", "key", "index", "table", "select", "from", "where",
            "join", "left", "right", "inner", "outer", "and", "or", "not", "null", "in", "on", "as",
            "by", "having", "limit", "union", "all", "between", "case", "when", "then", "else", "end",
            "exists", "distinct", "into", "grant", "primary", "unique", "foreign", "default", "check",
            "constraint", "user", "values", "interval", "natural", "partition", "over", "rank", "range",
            "regexp", "rlike"));

    private final DdlConfig config;

    private final @Nullable TableNameStrategy strategy;

    public NamingService(DdlConfig config) {
        this(config, null);
    }

    public NamingService(DdlConfig config, @Nullable TableNameStrategy strategy) {
        this.config = config;
        this.strategy = strategy;
    }

    /** 表名 → 基类名（不含 artifact 后缀）；配置了策略则整体委托。 */
    public String tableClassName(String tableName) {
        if (strategy != null) {
            return strategy.toBaseClassName(tableName);
        }

        String name = stripPrefixes(tableName);
        if (config.isTableStripShardSuffix()) {
            name = stripShardSuffix(name);
        }
        return toPascalCase(name);
    }

    /** 表名 → artifact 类名 = 基类名 + 该 artifact 配置的后缀。 */
    public String artifactClassName(String tableName, String artifactKind) {
        String suffix = config.artifact(artifactKind)
                .map(a -> a.getSuffix())
                .orElse("");
        return tableClassName(tableName) + suffix;
    }

    /** 列名 → 字段名（camelCase + 保留字后缀）。 */
    public String columnFieldName(String columnName) {
        String name = config.isColumnCamelCase() ? toCamelCase(columnName) : columnName;
        if (RESERVED_WORDS.contains(name)) {
            return name + config.getColumnKeywordSuffix();
        }
        return name;
    }

    /** 索引 → 查询方法名：前缀 + By + 列 camelCase 以 And 连接（name,gender → findByNameAndGender）。 */
    public String indexMethodName(Index index) {
        return indexMethodName(index.getColumns());
    }

    /** 列序列 → 查询方法名（最左前缀拆分时使用）。 */
    public String indexMethodName(List<String> columns) {
        StringBuilder sb = new StringBuilder(config.getMethodPrefix()).append("By");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append("And");
            }
            sb.append(upperFirst(toCamelCase(columns.get(i))));
        }
        return sb.toString();
    }

    /** enum 类名：column 风格 = 列名 PascalCase；tableColumn 风格 = 表基类名 + 列名 PascalCase。 */
    public String enumClassName(String tableName, String columnName) {
        String column = toPascalCase(columnName);
        if ("tableColumn".equals(config.getEnumStyle())) {
            return tableClassName(tableName) + column;
        }
        return column;
    }

    /** 按前缀列表剥表名前缀（大小写不敏感）。 */
    private String stripPrefixes(String tableName) {
        String name = tableName;
        for (String prefix : config.getTableStripPrefixes()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                name = name.substring(prefix.length());
            }
        }
        return name;
    }

    /** 按配置正则剥分表后缀（默认 {@code _\d+$}，如 {@code user_0} → {@code user}）。 */
    private String stripShardSuffix(String name) {
        Pattern pattern = Pattern.compile(config.getTableShardPattern());
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            return name.substring(0, matcher.start());
        }
        return name;
    }

    /** snake_case → PascalCase（user_profile → UserProfile；USER → User）。 */
    private String toPascalCase(String name) {
        StringBuilder sb = new StringBuilder();
        for (String word : name.split("_")) {
            if (!word.isEmpty()) {
                sb.append(capitalize(word));
            }
        }
        return sb.toString();
    }

    /** snake_case → camelCase（user_id → userId；首段小写，其余段首字母大写）。 */
    private String toCamelCase(String name) {
        String[] words = name.split("_");
        StringBuilder sb = new StringBuilder(words[0].toLowerCase(Locale.ROOT));
        for (int i = 1; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                sb.append(capitalize(words[i]));
            }
        }
        return sb.toString();
    }

    /** 首字母大写、其余小写（name → Name；用于 snake 片段）。 */
    private static String capitalize(String word) {
        if (word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT);
    }

    /** 仅首字母大写、保留其余（userId → UserId；用于已是 camelCase 的列名）。 */
    private static String upperFirst(String word) {
        if (word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }

}
