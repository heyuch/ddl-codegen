package com.demo.repository.impl;

import java.util.List;
import javax.annotation.Resource;
import javax.annotation.processing.Generated;

import com.demo.converter.UserConverter;
import com.demo.entity.User;
import com.demo.mapper.UserMapper;
import com.demo.repository.UserRepository;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class UserRepositoryImpl implements UserRepository {

    @Resource
    @Generated("ddl-codegen")
    private UserMapper userMapper;

    @Resource
    @Generated("ddl-codegen")
    private UserConverter userConverter;

    @Override
    @Nullable
    @Generated("ddl-codegen")
    public User findById(Long id) {
        return userConverter.toUser(userMapper.findById(id));
    }

    @Override
    @Nullable
    @Generated("ddl-codegen")
    public User findByName(String name) {
        return userConverter.toUser(userMapper.findByName(name));
    }

}
