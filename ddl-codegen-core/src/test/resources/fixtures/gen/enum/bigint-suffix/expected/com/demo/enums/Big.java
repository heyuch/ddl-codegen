package com.demo.enums;

import javax.annotation.processing.Generated;

import org.checkerframework.checker.nullness.qual.Nullable;

public enum Big {

    @Generated("ddl-codegen")
    X(1L, "一"),

    @Generated("ddl-codegen")
    Y(3000000000L, "十亿"),

    ;

    @Generated("ddl-codegen")
    private final Long code;

    @Generated("ddl-codegen")
    private final String desc;

    @Generated("ddl-codegen")
    private Big(Long code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Generated("ddl-codegen")
    public Long getCode() {
        return code;
    }

    @Generated("ddl-codegen")
    public String getDesc() {
        return desc;
    }

    @Nullable
    @Generated("ddl-codegen")
    public static Big fromCodeNullable(@Nullable Long code) {
        if (code == null) {
            return null;
        }
        for (Big e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        return null;
    }

    @Generated("ddl-codegen")
    public static Big fromCode(Long code) {
        if (code == null) {
            throw new IllegalArgumentException("code 不能为 null");
        }
        for (Big e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("未知枚举值: " + code);
    }

}
