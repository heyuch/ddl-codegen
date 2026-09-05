package com.example.sample.entity;

import javax.annotation.processing.Generated;

import com.example.sample.enums.Gender;
import com.example.sample.enums.Kind;
import com.example.sample.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
     * 性别
     */
    @Generated("ddl-codegen")
    private Gender gender;

    /**
     * 状态
     */
    @Generated("ddl-codegen")
    private Status status;

    /**
     * 类型
     */
    @Generated("ddl-codegen")
    private Kind kind;

    /**
     * 备注
     */
    @Generated("ddl-codegen")
    private String note;

}
