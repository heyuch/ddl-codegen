package com.demo.repository.impl;

import java.util.List;
import javax.annotation.processing.Generated;

import com.demo.converter.UserConverter;
import com.demo.entity.User;
import com.demo.mapper.UserMapper;
import com.demo.pojo.UserPo;
import com.demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;

/**
 * 用户表
 */
@Slf4j
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

    /**
     * 自引用代理（内部调用经此触发缓存清理）
     */
    @Autowired
    @Lazy
    @Generated("ddl-codegen")
    private UserRepository self;

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
        UserPo userPo = userConverter.toUserPo(user);
        int rows = userMapper.insert(userPo);
        if (rows > 0) {
            self.evictCaches(userConverter.toUser(userPo));
        }
        return rows;
    }

    /**
     * 更新记录
     */
    @Override
    @Generated("ddl-codegen")
    public int update(User user) {
        UserPo old = userMapper.findById(user.getId());
        UserPo userPo = userConverter.toUserPo(user);
        int rows = userMapper.update(userPo);
        if (rows > 0) {
            if (old != null) {
                self.evictCaches(userConverter.toUser(old));
            }
            self.evictCaches(user);
        }
        return rows;
    }

    /**
     * 按主键删除记录
     */
    @Override
    @Generated("ddl-codegen")
    public int deleteById(Long id) {
        UserPo old = userMapper.findById(id);
        int rows = userMapper.deleteById(id);
        if (rows > 0 && old != null) {
            self.evictCaches(userConverter.toUser(old));
        }
        return rows;
    }

    /**
     * 清理该实体相关的缓存键
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = "User", key = "'findById:' + #user.id"),
            @CacheEvict(cacheNames = "User", key = "'findByName:' + #user.name"),
            @CacheEvict(cacheNames = "User", key = "'findByRegion:' + #user.region"),
            @CacheEvict(cacheNames = "User", key = "'findByRegionAndLevel:' + #user.region + ',' + #user.level")
    })
    @Override
    @Generated("ddl-codegen")
    public void evictCaches(User user) {
        log.info("evictCaches: {}:{}", "User", "findById:" + user.getId());
        log.info("evictCaches: {}:{}", "User", "findByName:" + user.getName());
        log.info("evictCaches: {}:{}", "User", "findByRegion:" + user.getRegion());
        log.info("evictCaches: {}:{}", "User", "findByRegionAndLevel:" + user.getRegion() + "," + user.getLevel());
    }

    /**
     * 按 主键 查询
     */
    @Cacheable(cacheNames="User", key="'findById:' + #id")
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
    @Cacheable(cacheNames="User", key="'findByName:' + #name")
    @Override
    @Nullable
    @Generated("ddl-codegen")
    public User findByName(String name) {
        UserPo po = userMapper.findByName(name);
        return po == null ? null : userConverter.toUser(po);
    }

    /**
     * 按 区域 查询
     */
    @Cacheable(cacheNames="User", key="'findByRegion:' + #region")
    @Override
    @Generated("ddl-codegen")
    public List<User> findByRegion(String region) {
        return userConverter.toUserList(userMapper.findByRegion(region));
    }

    /**
     * 按 区域、级别 查询
     */
    @Cacheable(cacheNames="User", key="'findByRegionAndLevel:' + #region + ',' + #level")
    @Override
    @Generated("ddl-codegen")
    public List<User> findByRegionAndLevel(String region, Integer level) {
        return userConverter.toUserList(userMapper.findByRegionAndLevel(region, level));
    }

}
