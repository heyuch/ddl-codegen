package com.demo.repository.impl;

import java.util.List;
import javax.annotation.processing.Generated;

import com.demo.converter.UserConverter;
import com.demo.entity.User;
import com.demo.mapper.UserMapper;
import com.demo.pojo.UserPo;
import com.demo.repository.UserRepository;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * 用户表
 */
public class UserRepositoryImpl implements UserRepository {

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
     * 插入记录
     */
    @Override
    @Generated("ddl-codegen")
    public int insert(User user) {
        return userMapper.insert(userConverter.toUserPo(user));
    }

    /**
     * 更新记录
     */
    @Override
    @Generated("ddl-codegen")
    public int update(User user) {
        return userMapper.update(userConverter.toUserPo(user));
    }

    /**
     * 按主键删除记录
     */
    @Override
    @Generated("ddl-codegen")
    public int deleteById(Long id) {
        return userMapper.deleteById(id);
    }

    /**
     * 按 主键 查询
     */
    @Override
    @Nullable
    @Generated("ddl-codegen")
    public User findById(Long id) {
        UserPo po = userMapper.findById(id);
        return po == null ? null : userConverter.toUser(po);
    }

    /**
     * 按 用户名 查询
     */
    @Override
    @Nullable
    @Generated("ddl-codegen")
    public User findByName(String name) {
        UserPo po = userMapper.findByName(name);
        return po == null ? null : userConverter.toUser(po);
    }

}
