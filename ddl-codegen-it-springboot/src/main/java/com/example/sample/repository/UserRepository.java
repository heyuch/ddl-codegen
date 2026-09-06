package com.example.sample.repository;

import javax.annotation.processing.Generated;

import com.example.sample.entity.User;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 用户表
 */
public interface UserRepository {

    /**
     * 按主键删除记录
     */
    @Generated("ddl-codegen")
    int deleteById(Long id);

    /**
     * 清理该实体相关的缓存键
     */
    @Generated("ddl-codegen")
    void evictCaches(User user);

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
     * 插入记录
     */
    @Generated("ddl-codegen")
    int insert(User user);

    /**
     * 更新记录
     */
    @Generated("ddl-codegen")
    int update(User user);

}
