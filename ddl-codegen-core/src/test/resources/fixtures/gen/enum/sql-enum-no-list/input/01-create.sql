create table t_user (
    id bigint not null auto_increment comment '主键',
    gender enum('male','female') comment '性别',
    primary key (id));
