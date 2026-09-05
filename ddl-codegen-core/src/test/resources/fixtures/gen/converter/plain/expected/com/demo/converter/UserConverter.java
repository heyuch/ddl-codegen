package com.demo.converter;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

import com.demo.entity.User;
import com.demo.pojo.UserPo;

public final class UserConverter {

    /**
     * 把 UserPo 转成 User
     */
    @Generated("ddl-codegen")
    public User toUser(UserPo source) {
        User user = new User();
        user.setId(source.getId());
        user.setName(source.getName());
        user.setNote(source.getNote());
        return user;
    }

    /**
     * 把 User 转成 UserPo
     */
    @Generated("ddl-codegen")
    public UserPo toUserPo(User target) {
        UserPo userPo = new UserPo();
        userPo.setId(target.getId());
        userPo.setName(target.getName());
        userPo.setNote(target.getNote());
        return userPo;
    }

    /**
     * 把 UserPo 列表转成 User 列表
     */
    @Generated("ddl-codegen")
    public List<User> toUserList(List<UserPo> sourceList) {
        java.util.List<User> list = new java.util.ArrayList<>();
        if (sourceList != null) {
            for (UserPo source : sourceList) {
                list.add(toUser(source));
            }
        }
        return list;
    }

    /**
     * 把 User 列表转成 UserPo 列表
     */
    @Generated("ddl-codegen")
    public List<UserPo> toUserPoList(List<User> targetList) {
        java.util.List<UserPo> list = new java.util.ArrayList<>();
        if (targetList != null) {
            for (User target : targetList) {
                list.add(toUserPo(target));
            }
        }
        return list;
    }

}
