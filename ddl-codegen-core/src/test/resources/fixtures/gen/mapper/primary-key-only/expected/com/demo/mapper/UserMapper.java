package com.demo.mapper;

import javax.annotation.processing.Generated;

import com.demo.pojo.UserPo;
import org.apache.ibatis.annotations.Param;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface UserMapper {

    @Generated
    int insert(UserPo userPo);

    @Generated
    int update(UserPo userPo);

    @Generated
    int deleteById(@Param("id") Long id);

    @Nullable
    @Generated
    UserPo findById(@Param("id") Long id);

}
