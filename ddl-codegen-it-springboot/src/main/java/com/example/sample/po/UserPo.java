package com.example.sample.po;

import javax.annotation.processing.Generated;

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
public class UserPo {

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
    private String gender;

    /**
     * 状态
     */
    @Generated("ddl-codegen")
    private Integer status;

    /**
     * 类型
     */
    @Generated("ddl-codegen")
    private String kind;

    /**
     * 备注
     */
    @Generated("ddl-codegen")
    private String note;

}
