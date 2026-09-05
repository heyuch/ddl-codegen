create table t_user (
    id bigint not null auto_increment comment '主键',
    amount varchar(30) not null comment '金额 @type:java.math.BigDecimal',
    primary key (id)
)
