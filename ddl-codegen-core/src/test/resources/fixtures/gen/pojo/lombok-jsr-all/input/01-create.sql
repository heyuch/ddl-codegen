create table t_user (
    id bigint not null auto_increment comment '主键',
    name varchar(50) not null comment '用户名',
    nick varchar(50) comment '昵称',
    amount decimal(10,2) not null comment '金额',
    score tinyint(1) not null comment '是否有效',
    created_at datetime comment '创建时间',
    primary key (id)
)
