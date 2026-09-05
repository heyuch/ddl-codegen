package com.demo.mapper;

import java.util.List;
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

    @Nullable
    @Generated
    UserPo findByName(@Param("name") String name);

    @Generated
    List<UserPo> findByStatus(@Param("status") Integer status);

    @Generated
    List<UserPo> findByRegion(@Param("region") String region);

    @Generated
    List<UserPo> findByRegionAndLevel(@Param("region") String region, @Param("level") Integer level);

}
