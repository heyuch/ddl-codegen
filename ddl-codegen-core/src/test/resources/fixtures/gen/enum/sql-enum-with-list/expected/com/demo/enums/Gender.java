package com.demo.enums;

import javax.annotation.processing.Generated;

import org.checkerframework.checker.nullness.qual.Nullable;

public enum Gender {

    @Generated
    MALE("male", "男"),

    @Generated
    FEMALE("female", "女"),

    ;

    @Generated
    private final String code;

    @Generated
    private final String desc;

    @Generated
    private Gender(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Generated
    public String getCode() {
        return code;
    }

    @Generated
    public String getDesc() {
        return desc;
    }

    @Nullable
    @Generated
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

    @Generated
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
