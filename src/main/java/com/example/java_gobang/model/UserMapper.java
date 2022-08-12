package com.example.java_gobang.model;

public interface UserMapper {
    // 往数据库里插入一个用户。用于注册功能。
    void insert(User user);

    // 根据用户名，来查询用户的详细信息,用于登录功能
    User selectByName(String username);
}
