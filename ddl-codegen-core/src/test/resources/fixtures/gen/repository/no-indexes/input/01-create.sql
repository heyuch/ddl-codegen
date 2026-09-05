create table t_log (
    id bigint not null comment '逻辑 id',
    msg varchar(200) not null comment '消息'
) comment '无索引表'
