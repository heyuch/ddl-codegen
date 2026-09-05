package com.demo.repository;

import java.util.List;
import javax.annotation.processing.Generated;

import com.demo.entity.User;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 用户表
 */
public interface UserRepository {

    /**
     * 按 主键 查询
     */
    @Nullable
    @Generated("ddl-codegen")
    User findById(Long id);

    /**
     * 按 用户名 查询
     */
    @Nullable
    @Generated("ddl-codegen")
    User findByName(String name);

    /**
     * 按 状态 查询
     */
    @Generated("ddl-codegen")
    List<User> findByStatus(Integer status);

    /**
     * 按 区域 查询
     */
    @Generated("ddl-codegen")
    List<User> findByRegion(String region);

    /**
     * 按 区域、级别 查询
     */
    @Generated("ddl-codegen")
    List<User> findByRegionAndLevel(String region, Integer level);

}
