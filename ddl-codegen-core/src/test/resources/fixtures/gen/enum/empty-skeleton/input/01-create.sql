create table t_user (
    id bigint not null auto_increment comment '主键',
    status tinyint not null comment '状态 @enum:Status',
    primary key (id));
