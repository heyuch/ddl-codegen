-- 真实集成样例 DDL（20260906-03）：DDL/config 是生成代码的唯一事实源；
-- 同时作为 Testcontainers MySQL 的初始化脚本。注：create 内联普通索引不被解析进生成模型，
-- 集成验证主链以主键/唯一键派生 findById/findByName 为主（多列 List findBy 由 20260906-02 golden 覆盖）。

create table t_user (
    id bigint not null auto_increment comment '主键',
    name varchar(50) not null comment '用户名',
    gender enum('male','female') comment '性别',
    status tinyint unsigned not null comment '状态 1=初始(INIT) 2=活跃(ACTIVE) 3=停用(SUSPEND) @enum:Status',
    kind varchar(20) not null comment '类型 NORMAL=普通 TRIAL=试用 @enum:Kind',
    note varchar(200) comment '备注',
    primary key (id),
    unique key uk_name (name)
) comment '用户表';
