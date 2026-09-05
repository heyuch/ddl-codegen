package com.example.sample.mapper;

import javax.annotation.processing.Generated;

import com.example.sample.po.UserPo;
import org.apache.ibatis.annotations.Param;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 用户表
 */
public interface UserMapper {

    /**
     * 按主键删除记录
     */
    @Generated("ddl-codegen")
    int deleteById(@Param("id") Long id);

    /**
     * 按 主键 查询
     */
    @Nullable
    @Generated("ddl-codegen")
    UserPo findById(@Param("id") Long id);

    /**
     * 按 用户名 查询
     */
    @Nullable
    @Generated("ddl-codegen")
    UserPo findByName(@Param("name") String name);

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
