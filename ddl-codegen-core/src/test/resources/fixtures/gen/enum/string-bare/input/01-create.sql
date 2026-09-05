create table t_user (
    id bigint not null auto_increment comment '主键',
    type varchar(20) not null comment '类型 NORMAL=普通 TRIAL=试用 @enum',
    primary key (id));
