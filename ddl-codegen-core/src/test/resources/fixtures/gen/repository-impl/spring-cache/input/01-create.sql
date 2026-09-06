create table t_user (
    id bigint not null auto_increment comment '主键',
    name varchar(50) not null comment '用户名',
    region varchar(20) not null comment '区域',
    level tinyint not null comment '级别',
    primary key (id),
    unique key uk_name (name),
    key idx_region_level (region, level)
) comment '用户表'
