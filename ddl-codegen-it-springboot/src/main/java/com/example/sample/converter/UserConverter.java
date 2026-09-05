package com.example.sample.converter;

import java.util.List;
import javax.annotation.processing.Generated;

import com.example.sample.entity.User;
import com.example.sample.enums.Gender;
import com.example.sample.enums.Kind;
import com.example.sample.enums.Status;
import com.example.sample.po.UserPo;

/**
 * 用户表
 */
public final class UserConverter {

    /**
     * 把 UserPo 转成 User
     */
    @Generated("ddl-codegen")
    public User toUser(UserPo source) {
        User user = new User();
        user.setId(source.getId());
        user.setName(source.getName());
        user.setGender(source.getGender() == null ? null : Gender.fromCode(source.getGender()));
        user.setStatus(source.getStatus() == null ? null : Status.fromCode(source.getStatus()));
        user.setKind(source.getKind() == null ? null : Kind.fromCode(source.getKind()));
        user.setNote(source.getNote());
        return user;
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
     * 把 User 转成 UserPo
     */
    @Generated("ddl-codegen")
    public UserPo toUserPo(User target) {
        UserPo userPo = new UserPo();
        userPo.setId(target.getId());
        userPo.setName(target.getName());
        userPo.setGender(target.getGender() == null ? null : target.getGender().getCode());
        userPo.setStatus(target.getStatus() == null ? null : target.getStatus().getCode());
        userPo.setKind(target.getKind() == null ? null : target.getKind().getCode());
        userPo.setNote(target.getNote());
        return userPo;
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
