package com.demo.repository;

import java.util.List;
import javax.annotation.processing.Generated;

import com.demo.entity.User;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface UserRepository {

    @Nullable
    @Generated("ddl-codegen")
    User findById(Long id);

    @Nullable
    @Generated("ddl-codegen")
    User findByName(String name);

    @Generated("ddl-codegen")
    List<User> findByStatus(Integer status);

    @Generated("ddl-codegen")
    List<User> findByRegion(String region);

    @Generated("ddl-codegen")
    List<User> findByRegionAndLevel(String region, Integer level);

}
