package com.example.java_gobang.model;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    // 往数据库里插入一个用户。用于注册功能。
    void insert(User user);

    // 根据用户名，来查询用户的详细信息,用于登录功能
    User selectByName(String username);

    // 获胜方进行的处理：总场数 +1，获胜场数 +1，天梯分数 + 30
    void userWin(int userId);

    // 失败方进行的处理：总场数 +1，获胜场数 不变，天梯分数 -30
    void userLose(int userId);
}
