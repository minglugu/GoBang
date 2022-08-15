package com.example.java_gobang.game;

import com.example.java_gobang.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Queue;

@Component
// 这个类表示"匹配器"，通过这个类，负责完成整个匹配
public class Matcher {
    // 创建3个匹配队列d, 可以更加细分化
    private Queue<User> normalQueue = new LinkedList<>();
    private Queue<User> highQueue = new LinkedList<>();
    private Queue<User> veryHighQueue = new LinkedList<>();

    @Autowired
    private OnlineUserManager onlineUserManager;

    // 操作匹配队列的方法
    // add a user to the corresponding queue
    public void add(User user) {
        if (user.getScore() < 2000) {
            normalQueue.offer(user);
            System.out.println("把玩家 " + user.getUsername() + " 加入到了normalQueue中！");
        } else if (user.getScore() >= 2000 && user.getScore() < 3000) {
            highQueue.offer(user);
            System.out.println("把玩家 " + user.getUsername() + " 加入到了highQueue中！");
        } else {
            veryHighQueue.offer(user);
            System.out.println("把玩家 " + user.getUsername() + " 加入到了veryHighQueue中！");
        }
    }

    // 当玩家点击停止匹配的时候，需要把玩家从匹配队列中删除
    public void remove(User user) {
        if (user.getScore() < 2000) {
            normalQueue.remove(user);
            System.out.println("把玩家 " + user.getUsername() + " 从normalQueue中删除！");
        } else if (user.getScore() >= 2000 && user.getScore() < 3000) {
            highQueue.offer(user);
            System.out.println("把玩家 " + user.getUsername() + " 从highQueue删除！");
        } else {
            veryHighQueue.offer(user);
            System.out.println("把玩家 " + user.getUsername() + " 从veryHighQueue删除！");
        }
    }

    
}

