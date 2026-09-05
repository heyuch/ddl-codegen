create table t_user (
    id bigint not null auto_increment comment '主键',
    name varchar(50) not null comment '用户名',
    status int not null comment '状态',
    region varchar(20) not null comment '区域',
    level tinyint not null comment '级别',
    gender enum('male','female') comment '性别',
    primary key (id),
    unique key uk_name (name)
) comment '用户表'
