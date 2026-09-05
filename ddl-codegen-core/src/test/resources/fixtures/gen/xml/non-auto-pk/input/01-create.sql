create table t_user (
    id bigint not null comment '业务主键',
    name varchar(50) not null comment '用户名',
    primary key (id)
) comment '非自增主键表'
