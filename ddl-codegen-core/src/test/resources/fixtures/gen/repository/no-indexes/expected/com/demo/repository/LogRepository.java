package com.demo.repository;

import javax.annotation.processing.Generated;

import com.demo.entity.Log;

/**
 * 无索引表
 */
public interface LogRepository {

    /**
     * 插入记录
     */
    @Generated("ddl-codegen")
    int insert(Log log);

    /**
     * 更新记录
     */
    @Generated("ddl-codegen")
    int update(Log log);

}
