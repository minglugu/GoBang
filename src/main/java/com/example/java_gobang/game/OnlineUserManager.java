package com.example.java_gobang.game;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

// 这个类表示用户的在线状态，可以同时管理游戏大厅和游戏房间的状态，同时放在一个“在线用户管理器”中
@Component
public class OnlineUserManager {
    // 这个哈希表，就用来表示当前用户在游戏大厅在线状态。
    // 当前使用HashMap 来存储用户的在线状态。如果是多线程访问同一个HashMap，就容易出现线程安全问题
    private ConcurrentHashMap<Integer, WebSocketSession> gameHall = new ConcurrentHashMap<>();

    // 这个HashMap用来表示当前用户，在游戏房间的在线状态
    private ConcurrentHashMap<Integer, WebSocketSession> gameRoom = new ConcurrentHashMap<>();

    // 游戏大厅的方法
    // 进入游戏大厅
    // 拿到用户的id和session，存到hashmap里。
    public void enterGameHall(int userId, WebSocketSession webSocketSession) {
        gameHall.put(userId, webSocketSession);
    }

    // 退出游戏大厅
    public void exitGameHall(int userId) {
        gameHall.remove(userId);
    }

    // 查找用户
    public WebSocketSession getFromGameHall(int userId) {
        return gameHall.get(userId);
    }

    // 游戏房间的方法
    // 进入游戏房间
    public void enterGameRoom(int userId, WebSocketSession webSocketSession) {
        gameRoom.put(userId, webSocketSession);
    }

    // 退出游戏房间
    public void exitGameRoom(int userId) {
        gameRoom.remove(userId);
    }

    // 在游戏房间里查找用户
    public WebSocketSession getFromGameRoom(int userId) {
        return gameRoom.get(userId);
    }
}
