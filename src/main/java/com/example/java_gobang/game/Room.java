package com.example.java_gobang.game;

import com.example.java_gobang.model.User;

import java.util.UUID;

// 这个类，是一个游戏房间
public class Room {
    // 使用字符串类型来表示，用于生成唯一值。
    private String roomId;

    private User user1;
    private User user2;

    // constructor
    public Room() {
        // 构造 Room 的时候，生成一个唯一的字符串，表示房间 id
        // 使用 UUID 来作为房间 id
        // UUID 表示“世界上唯一的身份标识”，16进制表示的数字，
        // 任意一次调用，每次结果都不同
        roomId = UUID.randomUUID().toString();
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public User getUser1() {
        return user1;
    }

    public void setUser1(User user1) {
        this.user1 = user1;
    }

    public User getUser2() {
        return user2;
    }

    public void setUser2(User user2) {
        this.user2 = user2;
    }
}
