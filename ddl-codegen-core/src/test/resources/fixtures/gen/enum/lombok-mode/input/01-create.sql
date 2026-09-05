create table t_user (
    id bigint not null auto_increment comment '主键',
    status tinyint not null comment '状态 1=初始(INIT) 2=活跃(ACTIVE) @enum:Status',
    primary key (id));
