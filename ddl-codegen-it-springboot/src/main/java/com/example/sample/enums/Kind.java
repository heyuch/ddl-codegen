package com.example.sample.enums;

import javax.annotation.processing.Generated;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 用户表
 */
@Getter
@RequiredArgsConstructor
public enum Kind {

    /**
     * 普通
     */
    @Generated("ddl-codegen")
    NORMAL("NORMAL", "普通"),

    /**
     * 试用
     */
    @Generated("ddl-codegen")
    TRIAL("TRIAL", "试用"),

    ;

    /**
     * code - 数据库存储值
     */
    @Generated("ddl-codegen")
    private final String code;

    /**
     * desc - 枚举值描述
     */
    @Generated("ddl-codegen")
    private final String desc;

    /**
     * 按 code 宽松反查：无匹配或入参为 null 时返回 null
     */
    @Nullable
    @Generated("ddl-codegen")
    public static Kind fromCodeNullable(@Nullable String code) {
        if (code == null) {
            return null;
        }
        for (Kind e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 按 code 严格反查：入参为 null 或无匹配时抛异常
     */
    @Generated("ddl-codegen")
    public static Kind fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("code 不能为 null");
        }
        for (Kind e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("未知枚举值: " + code);
    }

}
