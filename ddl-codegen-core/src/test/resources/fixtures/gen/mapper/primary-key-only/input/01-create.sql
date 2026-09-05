create table t_user (
    id bigint not null auto_increment comment '主键',
    name varchar(50) not null comment '用户名',
    note varchar(100) not null comment '备注',
    primary key (id)
) comment '仅主键表'
