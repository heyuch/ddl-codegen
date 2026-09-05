create table t_user (
    id bigint not null auto_increment comment '主键',
    status tinyint unsigned not null comment '状态 1=初始(INIT) @enum:Status',
    type varchar(20) not null comment '类型 NORMAL=普通 @enum:Type',
    primary key (id)
)
