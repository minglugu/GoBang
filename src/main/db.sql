-- 如何设置并连接云服务器的CentOS里面数据库，URL：https://zhuanlan.zhihu.com/p/49046496

create database if not exists java_gobang;

use java_gobang;

drop table if exists user;
create table user(
    userId int primary key auto_increment,
    username varchar(50) unique,    -- 字段的长度，是有产品经理确定在需求文档里面
    password varchar(50),           -- 字段的长度，是有产品经理确定在需求文档里面
    score int,              -- 天梯积分
    totalCount int,         -- 比赛总场数
    winCount int            -- 获胜场数
);

-- 设置成null，那么就是自增组件
insert into user values(null, 'zhangsan', '123', 1000, 0, 0);
insert into user values(null, 'lisi', '123', 1000, 0, 0);
insert into user values(null, 'wangwu', '651', 1000, 0, 0);