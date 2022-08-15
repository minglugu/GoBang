package com.example.java_gobang.game;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

// 这个类表示用户的在线状态
@Component
public class OnlineUserManager {
    // 这个哈希表，就用来表示当前用户在游戏大厅在线状态。
    // 当前使用HashMap 来存储用户的在线状态。如果是多线程访问同一个HashMap，就容易出现线程安全问题
    private ConcurrentHashMap<Integer, WebSocketSession> gameHall = new ConcurrentHashMap<>();

    // 进入游戏大厅
    public void enterGameHall(int userId, WebSocketSession webSocketSession) {
        gameHall.put(userId, webSocketSession);
    }

    // 推出游戏大厅
    public void exitGameHall(int userId) {
        gameHall.remove(userId);
    }

    // 查找用户
    public WebSocketSession getFromGameHall(int userId) {
        return gameHall.get(userId);
    }
}
