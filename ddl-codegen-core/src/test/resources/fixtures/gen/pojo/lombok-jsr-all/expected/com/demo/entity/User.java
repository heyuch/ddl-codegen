package com.demo.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.Nullable;
import javax.annotation.processing.Generated;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    @Generated("ddl-codegen")
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @NotNull
    @Generated("ddl-codegen")
    private Long id;

    /**
     * 用户名
     */
    @NotNull
    @Size(max = 50)
    @Generated("ddl-codegen")
    private String name;

    /**
     * 昵称
     */
    @Size(max = 50)
    @Nullable
    @Generated("ddl-codegen")
    private String nick;

    /**
     * 金额
     */
    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Generated("ddl-codegen")
    private BigDecimal amount;

    /**
     * 是否有效
     */
    @NotNull
    @Generated("ddl-codegen")
    private Boolean score;

    /**
     * 创建时间
     */
    @Nullable
    @Generated("ddl-codegen")
    private LocalDateTime createdAt;

}
