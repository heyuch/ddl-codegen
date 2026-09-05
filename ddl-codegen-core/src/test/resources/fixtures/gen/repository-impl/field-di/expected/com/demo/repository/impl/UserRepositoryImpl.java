package com.demo.repository.impl;

import java.util.List;
import javax.annotation.Resource;
import javax.annotation.processing.Generated;

import com.demo.converter.UserConverter;
import com.demo.entity.User;
import com.demo.mapper.UserMapper;
import com.demo.repository.UserRepository;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 用户表
 */
public final class UserRepositoryImpl implements UserRepository {

    /**
     * 数据访问 Mapper
     */
    @Resource
    @Generated("ddl-codegen")
    private UserMapper userMapper;

    /**
     * 实体转换器
     */
    @Resource
    @Generated("ddl-codegen")
    private UserConverter userConverter;

    /**
     * 按 主键 查询
     */
    @Override
    @Nullable
    @Generated("ddl-codegen")
    public User findById(Long id) {
        return userMapper.findById(id) == null ? null : userConverter.toUser(userMapper.findById(id));
    }

    /**
     * 按 用户名 查询
     */
    @Override
    @Nullable
    @Generated("ddl-codegen")
    public User findByName(String name) {
        return userMapper.findByName(name) == null ? null : userConverter.toUser(userMapper.findByName(name));
    }

}
