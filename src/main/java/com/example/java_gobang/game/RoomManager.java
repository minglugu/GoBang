package com.example.java_gobang.game;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

// 房间管理器类
// 此类也是唯一实例. 有一个房间管理器就可以管所有房间了
// 视频 #40 - #
@Component
public class RoomManager {
    // Integer：用户id，String：房间id, 根据玩家的id，找到玩家所属的房间，即对应的房间id。
    private ConcurrentHashMap<Integer, String> userIdToRoomId = new ConcurrentHashMap<>();
    // 房间Id对应的Room object
    private ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();


    // 相房间管理器里面添加元素
    // key通过room可以拿到，添加房间room的时候，把userId 也同时加进去。
    public void add(Room room, int userId1, int userId2) {
        // 映射关系：userId1, userId2 -> roomId 加到一个hashmap userIdToRoomId里
        userIdToRoomId.put(userId1, room.getRoomId());
        userIdToRoomId.put(userId2, room.getRoomId());
        // 映射关系：roomId -> room 加到第二个hashmap rooms里
        rooms.put(room.getRoomId(), room);
    }

    // 所以删除房间的时候，要同时移除房间id和其中两个用户的id
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
