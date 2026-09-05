package com.demo.repository;

import java.util.List;
import javax.annotation.processing.Generated;

import com.demo.entity.User;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface UserRepository {

    @Nullable
    @Generated
    User findById(Long id);

    @Nullable
    @Generated
    User findByName(String name);

    @Generated
    List<User> findByStatus(Integer status);

    @Generated
    List<User> findByRegion(String region);

    @Generated
    List<User> findByRegionAndLevel(String region, Integer level);

}
