create table t_user (
    id bigint not null comment '逻辑 id',
    name varchar(50) not null comment '用户名',
    amount decimal(10,2) comment '金额'
) comment '无主键表'
