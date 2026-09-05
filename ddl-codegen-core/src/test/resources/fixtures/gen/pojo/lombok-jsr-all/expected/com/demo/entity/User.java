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

    @NotNull
    @Generated("ddl-codegen")
    private Long id;

    @NotNull
    @Size(max = 50)
    @Generated("ddl-codegen")
    private String name;

    @Size(max = 50)
    @Nullable
    @Generated("ddl-codegen")
    private String nick;

    @NotNull
    @Digits(integer = 8, fraction = 2)
    @Generated("ddl-codegen")
    private BigDecimal amount;

    @NotNull
    @Generated("ddl-codegen")
    private Boolean score;

    @Nullable
    @Generated("ddl-codegen")
    private LocalDateTime createdAt;

}
