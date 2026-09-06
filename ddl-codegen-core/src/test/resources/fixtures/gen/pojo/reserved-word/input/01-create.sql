create table t_user (
    id bigint not null auto_increment comment '主键',
    `order` int not null comment '排序',
    `class` varchar(30) not null comment '类别',
    name varchar(50) not null comment '名称',
    primary key (id)
)
