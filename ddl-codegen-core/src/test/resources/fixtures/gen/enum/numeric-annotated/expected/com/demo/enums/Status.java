package com.demo.enums;

import javax.annotation.processing.Generated;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 状态
 */
public enum Status {

    /**
     * 初始
     */
    @Generated("ddl-codegen")
    INIT(1, "初始"),

    /**
     * 活跃
     */
    @Generated("ddl-codegen")
    STATUS_2(2, "活跃"),

    /**
     * 停用
     */
    @Generated("ddl-codegen")
    SUSPEND(3, "停用"),

    ;

    /**
     * code - 数据库存储值
     */
    @Generated("ddl-codegen")
    private final Integer code;

    /**
     * desc - 枚举值描述
     */
    @Generated("ddl-codegen")
    private final String desc;

    /**
     * 私有构造：code + desc
     */
    @Generated("ddl-codegen")
    private Status(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取数据库存储值 code
     */
    @Generated("ddl-codegen")
    public Integer getCode() {
        return code;
    }

    /**
     * 获取枚举值描述 desc
     */
    @Generated("ddl-codegen")
    public String getDesc() {
        return desc;
    }

    /**
     * 按 code 宽松反查：无匹配或入参为 null 时返回 null
     */
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

    /**
     * 按 code 严格反查：入参为 null 或无匹配时抛异常
     */
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
