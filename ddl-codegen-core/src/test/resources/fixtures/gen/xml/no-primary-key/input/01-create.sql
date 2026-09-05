create table t_user (
    id bigint not null comment '逻辑 id',
    name varchar(50) not null comment '用户名',
    note varchar(100) comment '备注'
) comment '无主键表'
