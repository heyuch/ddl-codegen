package com.demo.entity;

import javax.annotation.processing.Generated;

import com.demo.enums.Status;
import com.demo.enums.Type;

public class User {

    @Generated("ddl-codegen")
    private Long id;

    @Generated("ddl-codegen")
    private Status status;

    @Generated("ddl-codegen")
    private Type type;

}
