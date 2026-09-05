package com.demo.enums;

import javax.annotation.processing.Generated;

import org.checkerframework.checker.nullness.qual.Nullable;

public enum UserStatus {

    @Generated
    STATUS_1(1, "初始"),

    @Generated
    STATUS_2(2, "活跃"),

    ;

    @Generated
    private final Integer code;

    @Generated
    private final String desc;

    @Generated
    private UserStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Generated
    public Integer getCode() {
        return code;
    }

    @Generated
    public String getDesc() {
        return desc;
    }

    @Nullable
    @Generated
    public static UserStatus fromCodeNullable(@Nullable Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatus e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        return null;
    }

    @Generated
    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("code 不能为 null");
        }
        for (UserStatus e : values()) {
            if (java.util.Objects.equals(e.code, code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("未知枚举值: " + code);
    }

}
