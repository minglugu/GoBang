package com.example.java_gobang.game;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

// 房间管理器类
// 此类也是唯一实例
@Component
public class RoomManager {
    private ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    // 相房间管理器里面添加元素
    // key通过room可以拿到
    public void add(Room room) {
        rooms.put(room.getRoomId(), room);
    }

    // 删除房间
    public void remove(String roomId) {
        rooms.remove(roomId);
    }

    // 查找room
    public Room getRoomByRoomId(String roomId) {
        return rooms.get(roomId);
    }

}
