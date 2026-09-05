package com.example.sample.repository;

import javax.annotation.processing.Generated;

import com.example.sample.entity.User;
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

}
