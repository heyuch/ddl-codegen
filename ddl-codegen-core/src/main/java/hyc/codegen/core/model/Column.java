package hyc.codegen.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 表的列模型。
 * <p>
 * 由 DDL 解析产生：{@code sqlType} 为 MySQL 原始类型名（小写，如 varchar/decimal/enum），
 * {@code enumValues} 仅对 enum 列非空，{@code meta} 存放 DDL 注解解析结果。
 */
// EI 抑制：Meta 是开放读写容器（javadoc 契约）：注解处理器 getMeta().put() 写入、生成器读取，拷贝则写入丢失
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public final class Column {

    private final String name;
    private final String sqlType;
    private final int length;
    private final int precision;
    private final int scale;
    private final boolean nullable;
    private final boolean unsigned;
    private final boolean autoIncrement;
    private final @Nullable String defaultValue;
    private final @Nullable String comment;
    private final List<String> enumValues;
    private final Meta meta = new Meta();

    private Column(Builder b) {
        if (b.name == null || b.sqlType == null) {
            throw new IllegalStateException("Column 构建缺失必填字段（name/sqlType）");
        }
        this.name = b.name;
        this.sqlType = b.sqlType;
        this.length = b.length;
        this.precision = b.precision;
        this.scale = b.scale;
        this.nullable = b.nullable;
        this.unsigned = b.unsigned;
        this.autoIncrement = b.autoIncrement;
        this.defaultValue = b.defaultValue;
        this.comment = b.comment;
        this.enumValues = Collections.unmodifiableList(new ArrayList<>(b.enumValues));
    }

    public String getName() {
        return name;
    }

    public String getSqlType() {
        return sqlType;
    }

    /** 长度（varchar(n)/char(n) 等）；无长度概念时返回 0。 */
    public int getLength() {
        return length;
    }

    /** 精度（decimal(p,s) 的 p 等）；无精度概念时返回 0。 */
    public int getPrecision() {
        return precision;
    }

    /** 刻度（decimal(p,s) 的 s 等）；无刻度概念时返回 0。 */
    public int getScale() {
        return scale;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isUnsigned() {
        return unsigned;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public @Nullable String getDefaultValue() {
        return defaultValue;
    }

    public @Nullable String getComment() {
        return comment;
    }

    /** enum 列的取值列表（来自 DDL {@code enum(...)}）；非 enum 列为空列表。 */
    public List<String> getEnumValues() {
        return enumValues;
    }

    /** 是否为 MySQL enum 类型列。 */
    public boolean isEnum() {
        return "enum".equals(sqlType);
    }

    /** 列元数据（DDL 注解结果写入这里，开放读写）。 */
    public Meta getMeta() {
        return meta;
    }

    /** 返回同名定义的副本列（保留全部字段与元数据），用于列改名。 */
    public Column renamedTo(String newName) {
        Column copy = builder()
                .name(newName)
                .sqlType(sqlType)
                .length(length)
                .precision(precision)
                .scale(scale)
                .nullable(nullable)
                .unsigned(unsigned)
                .autoIncrement(autoIncrement)
                .defaultValue(defaultValue)
                .comment(comment)
                .enumValues(enumValues)
                .build();
        copy.meta.putAll(meta);
        return copy;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@link Column} 构建器。
     * 可修改构建对象：字段由 builder 方法在 build() 前设置，初始化时序检查不适用。
     */
    public static final class Builder {

        @MonotonicNonNull
        private String name;
        @MonotonicNonNull
        private String sqlType;
        private int length;
        private int precision;
        private int scale;
        private boolean nullable = true;
        private boolean unsigned;
        private boolean autoIncrement;
        private @Nullable String defaultValue;
        private @Nullable String comment;
        private final List<String> enumValues = new ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder sqlType(String sqlType) {
            this.sqlType = sqlType;
            return this;
        }

        public Builder length(int length) {
            this.length = length;
            return this;
        }

        public Builder precision(int precision) {
            this.precision = precision;
            return this;
        }

        public Builder scale(int scale) {
            this.scale = scale;
            return this;
        }

        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Builder unsigned(boolean unsigned) {
            this.unsigned = unsigned;
            return this;
        }

        public Builder autoIncrement(boolean autoIncrement) {
            this.autoIncrement = autoIncrement;
            return this;
        }

        public Builder defaultValue(@Nullable String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder comment(@Nullable String comment) {
            this.comment = comment;
            return this;
        }

        public Builder enumValues(List<String> enumValues) {
            this.enumValues.addAll(enumValues);
            return this;
        }

        public Column build() {
            return new Column(this);
        }

    }

}
