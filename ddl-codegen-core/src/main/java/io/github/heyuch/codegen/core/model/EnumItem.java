package io.github.heyuch.codegen.core.model;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 枚举项原始数据（来自列 comment 的枚举列表，格式 {@code {code}={desc}({name})}，name 可选）。
 * <p>
 * {@code rawCode} 保留 comment 原文（如数字列的前导零 {@code 01}）；code 的 Java 类型判定、
 * 常量名缺省规则与唯一性校验由消费方（{@code EnumGenerator}）按列类型完成。
 */
public final class EnumItem {

    private final String rawCode;

    private final String desc;

    private final @Nullable String name;

    public EnumItem(String rawCode, String desc, @Nullable String name) {
        this.rawCode = rawCode;
        this.desc = desc;
        this.name = name;
    }

    /** 枚举值描述（comment 中 {@code =} 与可选 {@code (name)} 之间的文本）。 */
    public String getDesc() {
        return desc;
    }

    /** 枚举项常量名（可选；缺省时由消费方按列类型派生）。 */
    public @Nullable String getName() {
        return name;
    }

    /** 枚举项 code 原文（数据库存储值，类型由列决定）。 */
    public String getRawCode() {
        return rawCode;
    }

    @Override
    public String toString() {
        return name == null ? rawCode + "=" + desc : rawCode + "=" + desc + "(" + name + ")";
    }

}
