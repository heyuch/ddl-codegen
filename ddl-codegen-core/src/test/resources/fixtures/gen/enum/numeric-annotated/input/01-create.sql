create table t_user (
    id bigint not null auto_increment comment '主键',
    status tinyint unsigned not null comment '状态 1=初始(INIT) 2=活跃 3=停用(SUSPEND) @enum:Status',
    primary key (id));
