package com.demo.mapper;

import java.util.List;
import javax.annotation.processing.Generated;

import com.demo.pojo.UserPo;
import org.apache.ibatis.annotations.Param;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 用户表
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
     * 按 状态 查询
     */
    @Generated("ddl-codegen")
    List<UserPo> findByStatus(@Param("status") Integer status);

    /**
     * 按 区域 查询
     */
    @Generated("ddl-codegen")
    List<UserPo> findByRegion(@Param("region") String region);

    /**
     * 按 区域、级别 查询
     */
    @Generated("ddl-codegen")
    List<UserPo> findByRegionAndLevel(@Param("region") String region, @Param("level") Integer level);

}
