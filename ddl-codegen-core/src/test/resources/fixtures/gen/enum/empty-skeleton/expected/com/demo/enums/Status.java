package com.demo.enums;

import javax.annotation.processing.Generated;

import org.checkerframework.checker.nullness.qual.Nullable;

public enum Status {

    ;

    @Generated("ddl-codegen")
    private final Integer code;

    @Generated("ddl-codegen")
    private final String desc;

    @Generated("ddl-codegen")
    private Status(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Generated("ddl-codegen")
    public Integer getCode() {
        return code;
    }

    @Generated("ddl-codegen")
    public String getDesc() {
        return desc;
    }

    @Nullable
    @Generated("ddl-codegen")
    public static Status fromCodeNullable(@Nullable Integer code) {
        if (code == null) {
            return null;
        }
        for (Status e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        return null;
    }

    @Generated("ddl-codegen")
    public static Status fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("code 不能为 null");
        }
        for (Status e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("未知枚举值: " + code);
    }

}
