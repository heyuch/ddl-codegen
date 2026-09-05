package com.demo.enums;

import javax.annotation.processing.Generated;

import org.checkerframework.checker.nullness.qual.Nullable;

public enum Gender {

    @Generated("ddl-codegen")
    MALE("male", "男"),

    @Generated("ddl-codegen")
    FEMALE("female", "女"),

    ;

    @Generated("ddl-codegen")
    private final String code;

    @Generated("ddl-codegen")
    private final String desc;

    @Generated("ddl-codegen")
    private Gender(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Generated("ddl-codegen")
    public String getCode() {
        return code;
    }

    @Generated("ddl-codegen")
    public String getDesc() {
        return desc;
    }

    @Nullable
    @Generated("ddl-codegen")
    public static Gender fromCodeNullable(@Nullable String code) {
        if (code == null) {
            return null;
        }
        for (Gender e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        return null;
    }

    @Generated("ddl-codegen")
    public static Gender fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("code 不能为 null");
        }
        for (Gender e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("未知枚举值: " + code);
    }

}
