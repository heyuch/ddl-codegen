package com.demo.enums;

import javax.annotation.processing.Generated;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
@RequiredArgsConstructor
public enum Status {

    @Generated
    INIT(1, "初始"),

    @Generated
    ACTIVE(2, "活跃"),

    ;

    @Generated
    private final Integer code;

    @Generated
    private final String desc;

    @Nullable
    @Generated
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

    @Generated
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
