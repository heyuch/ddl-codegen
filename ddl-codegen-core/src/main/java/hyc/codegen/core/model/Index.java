package hyc.codegen.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 表的索引模型（含 PRIMARY KEY / UNIQUE KEY / 普通 INDEX）。
 * <p>
 * 名称约定：主键名为 {@code PRIMARY}（对应 MySQL information_schema 惯例）；
 * {@code unique} 为真表示主键或唯一键；{@code columns} 为索引列名（按索引定义顺序）。
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "Meta 开放读写容器（javadoc 契约），拷贝则写入丢失")
public final class Index {

    /** 主键索引名（MySQL 惯例）。 */
    public static final String PRIMARY = "PRIMARY";

    private final String name;
    private final boolean unique;
    private final List<String> columns;
    private final @Nullable String comment;
    private final Meta meta = new Meta();

    private Index(Builder b) {
        if (b.name == null) {
            throw new IllegalStateException("Index 构建缺失必填字段 name");
        }
        this.name = b.name;
        this.unique = b.unique;
        this.columns = Collections.unmodifiableList(new ArrayList<>(b.columns));
        this.comment = b.comment;
    }

    public String getName() {
        return name;
    }

    /** 是否为唯一约束（PRIMARY KEY / UNIQUE KEY）。 */
    public boolean isUnique() {
        return unique;
    }

    /** 索引列名（按定义顺序，不可变）。 */
    public List<String> getColumns() {
        return columns;
    }

    public @Nullable String getComment() {
        return comment;
    }

    /** 索引元数据（DDL 注解结果写入这里，开放读写）。 */
    public Meta getMeta() {
        return meta;
    }

    /** 返回同名定义的副本索引（保留全部字段与元数据），用于索引改名。 */
    public Index renamedTo(String newName) {
        Index copy = builder()
                .name(newName)
                .unique(unique)
                .columns(columns)
                .comment(comment)
                .build();
        copy.meta.putAll(meta);
        return copy;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@link Index} 构建器。
     * 可修改构建对象：字段由 builder 方法在 build() 前设置，初始化时序检查不适用。
     */
    public static final class Builder {

        @MonotonicNonNull
        private String name;
        private boolean unique;
        private final List<String> columns = new ArrayList<>();
        private @Nullable String comment;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        public Builder columns(List<String> columns) {
            this.columns.addAll(columns);
            return this;
        }

        public Builder comment(@Nullable String comment) {
            this.comment = comment;
            return this;
        }

        public Index build() {
            return new Index(this);
        }

    }

}
