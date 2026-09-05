package com.demo.mapper;

import javax.annotation.processing.Generated;

import com.demo.pojo.UserPo;

/**
 * 无主键表
 */
public interface UserMapper {

    /**
     * 插入记录
     */
    @Generated("ddl-codegen")
    int insert(UserPo userPo);

    /**
     * 更新记录
     */
    @Generated("ddl-codegen")
    int update(UserPo userPo);

}
