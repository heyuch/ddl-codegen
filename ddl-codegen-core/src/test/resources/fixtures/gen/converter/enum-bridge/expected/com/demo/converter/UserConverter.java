package com.demo.converter;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

import com.demo.entity.User;
import com.demo.enums.Status;
import com.demo.pojo.UserPo;

public class UserConverter {

    @Generated
    public User toUser(UserPo source) {
        User user = new User();
        user.setId(source.getId());
        user.setStatus(source.getStatus() == null ? null : Status.fromCode(source.getStatus()));
        user.setName(source.getName());
        return user;
    }

    @Generated
    public UserPo toUserPo(User target) {
        UserPo userPo = new UserPo();
        userPo.setId(target.getId());
        userPo.setStatus(target.getStatus() == null ? null : target.getStatus().getCode());
        userPo.setName(target.getName());
        return userPo;
    }

    @Generated
    public List<User> toUserList(List<UserPo> sourceList) {
        java.util.List<User> list = new java.util.ArrayList<>();
        if (sourceList != null) {
            for (UserPo source : sourceList) {
                list.add(toUser(source));
            }
        }
        return list;
    }

    @Generated
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
