package com.example.sample.repository.impl;

import javax.annotation.processing.Generated;

import com.example.sample.converter.UserConverter;
import com.example.sample.entity.User;
import com.example.sample.mapper.UserMapper;
import com.example.sample.repository.UserRepository;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 用户表
 */
public final class UserRepositoryImpl implements UserRepository {

    /**
     * 数据访问 Mapper
     */
    @Generated("ddl-codegen")
    private final UserMapper userMapper;

    /**
     * 实体转换器
     */
    @Generated("ddl-codegen")
    private final UserConverter userConverter;

    @Generated("ddl-codegen")
    public UserRepositoryImpl(UserMapper userMapper, UserConverter userConverter) {
        this.userMapper = userMapper;
        this.userConverter = userConverter;
    }

    /**
     * 按 主键 查询
     */
    @Override
    @Nullable
    @Generated("ddl-codegen")
    public User findById(Long id) {
        com.example.sample.po.UserPo po = userMapper.findById(id);
        return po == null ? null : userConverter.toUser(po);
    }

    /**
     * 按 用户名 查询
     */
    @Override
    @Nullable
    @Generated("ddl-codegen")
    public User findByName(String name) {
        com.example.sample.po.UserPo po = userMapper.findByName(name);
        return po == null ? null : userConverter.toUser(po);
    }

}
