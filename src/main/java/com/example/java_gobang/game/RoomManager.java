package com.example.java_gobang.game;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

// 房间管理器类
// 此类也是唯一实例
@Component
public class RoomManager {
    // Interger：用户id，String：房间id
    private ConcurrentHashMap<Integer, String> userIdToRoomId = new ConcurrentHashMap<>();
    // 房间Id对应的Room object
    private ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();


    // 相房间管理器里面添加元素
    // key通过room可以拿到
    public void add(Room room, int userId1, int userId2) {
        // 映射关系：userId1, userId2 -> roomId
        userIdToRoomId.put(userId1, room.getRoomId());
        userIdToRoomId.put(userId2, room.getRoomId());
        // 映射关系：roomId -> room
        rooms.put(room.getRoomId(), room);
    }

    // 删除房间
    public void remove(String roomId, int userId1, int userId2) {
        rooms.remove(roomId);
        userIdToRoomId.remove(userId1);
        userIdToRoomId.remove(userId2);
    }

    // 查找room
    public Room getRoomByRoomId(String roomId) {
        return rooms.get(roomId);
    }

    // 根据用户Id去第一个HashMap里面查，查到房间Id后，再去房间里面查找
    public Room getRoomByUserId(int userId) {
        // 根据userId查房间号
        String roomId = userIdToRoomId.get(userId);
        if (roomId == null) {
            // userId -> roomId 映射关系不存在，直接返回 null
            return null;
        }
        Room room = rooms.get(roomId);
        return room;
    }

}
