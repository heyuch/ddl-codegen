package com.demo.mapper;

import javax.annotation.processing.Generated;

import com.demo.pojo.UserPo;

public interface UserMapper {

    @Generated("ddl-codegen")
    int insert(UserPo userPo);

    @Generated("ddl-codegen")
    int update(UserPo userPo);

}
