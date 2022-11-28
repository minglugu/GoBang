package com.example.java_gobang.model;

import org.apache.ibatis.annotations.Mapper;

// 创建UserMapper接口，表示该如何操作数据库。
// 用 MyBatis 的相关 xml 配置文件，来自动的实现数据库的操作。
// 跟application.yml里面的Mybatis配置相匹配。
// mybatis:
//  mapper-locations: classpath:mapper/**Mapper.xml

@Mapper
public interface UserMapper {
    // 往数据库里插入一个用户。用于注册功能。参数用 User 对象来表示。
    void insert(User user);

    // 根据用户名，来查询用户的详细信息,用于登录功能
    User selectByName(String username);

    // 获胜方进行的处理：总场数 +1，获胜场数 +1，天梯分数 + 30
    void userWin(int userId);

    // 失败方进行的处理：总场数 +1，获胜场数 不变，天梯分数 -30
    void userLose(int userId);
}
