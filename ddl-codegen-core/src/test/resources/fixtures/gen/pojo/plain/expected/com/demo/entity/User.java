package com.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;

public class User {

    /**
     * 主键
     */
    @Generated("ddl-codegen")
    private Long id;

    /**
     * 用户名
     */
    @Generated("ddl-codegen")
    private String name;

    /**
     * 昵称
     */
    @Generated("ddl-codegen")
    private String nick;

    /**
     * 金额
     */
    @Generated("ddl-codegen")
    private BigDecimal amount;

    /**
     * 是否有效
     */
    @Generated("ddl-codegen")
    private Boolean score;

    /**
     * 创建时间
     */
    @Generated("ddl-codegen")
    private LocalDateTime createdAt;

}
