create table `order` (
    id bigint not null auto_increment comment '主键',
    `group` varchar(20) not null comment '分组',
    `order` int not null comment '排序',
    name varchar(50) not null comment '名称',
    primary key (id),
    KEY idx_group_order (`group`, `order`)
) comment '订单表'
