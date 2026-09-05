create table t_user (
    id bigint not null auto_increment comment '主键',
    name varchar(50) not null comment '用户名',
    status tinyint unsigned not null comment '状态 1=初始(INIT) 2=活跃(ACTIVE) @enum:Status',
    primary key (id),
    unique key uk_name (name)
) comment '用户表'
