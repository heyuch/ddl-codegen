package com.demo.repository.impl;

import java.util.List;
import javax.annotation.processing.Generated;

import com.demo.converter.UserConverter;
import com.demo.entity.User;
import com.demo.mapper.UserMapper;
import com.demo.repository.UserRepository;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class UserRepositoryImpl implements UserRepository {

    @Generated("ddl-codegen")
    public UserRepositoryImpl(UserMapper userMapper, UserConverter userConverter) {
        this.userMapper = userMapper;
        this.userConverter = userConverter;
    }

    @Override
    @Nullable
    @Generated("ddl-codegen")
    public User findById(Long id) {
        return userMapper.findById(id) == null ? null : userConverter.toUser(userMapper.findById(id));
    }

    @Override
    @Nullable
    @Generated("ddl-codegen")
    public User findByName(String name) {
        return userMapper.findByName(name) == null ? null : userConverter.toUser(userMapper.findByName(name));
    }

}
