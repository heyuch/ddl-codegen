create table t_user (
    id bigint not null auto_increment comment '主键',
    big bigint not null comment '大数 1=一(X) 3000000000=十亿(Y) @enum:Big',
    primary key (id));
