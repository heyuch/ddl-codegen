package com.demo.mapper;

import javax.annotation.processing.Generated;

import com.demo.pojo.UserPo;
import org.apache.ibatis.annotations.Param;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface UserMapper {

    @Generated("ddl-codegen")
    int insert(UserPo userPo);

    @Generated("ddl-codegen")
    int update(UserPo userPo);

    @Generated("ddl-codegen")
    int deleteById(@Param("id") Long id);

    @Nullable
    @Generated("ddl-codegen")
    UserPo findById(@Param("id") Long id);

}
