package com.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;

public class User {

    @Generated("ddl-codegen")
    private Long id;

    @Generated("ddl-codegen")
    private String name;

    @Generated("ddl-codegen")
    private String nick;

    @Generated("ddl-codegen")
    private BigDecimal amount;

    @Generated("ddl-codegen")
    private Boolean score;

    @Generated("ddl-codegen")
    private LocalDateTime createdAt;

}
