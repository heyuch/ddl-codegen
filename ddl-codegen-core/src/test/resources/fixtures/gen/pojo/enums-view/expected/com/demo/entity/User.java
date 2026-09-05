package com.demo.entity;

import javax.annotation.processing.Generated;

import com.demo.enums.Status;
import com.demo.enums.Type;

public class User {

    /**
     * 主键
     */
    @Generated("ddl-codegen")
    private Long id;

    /**
     * 状态
     */
    @Generated("ddl-codegen")
    private Status status;

    /**
     * 类型
     */
    @Generated("ddl-codegen")
    private Type type;

}
