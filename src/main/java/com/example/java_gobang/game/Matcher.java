package com.example.java_gobang.game;

import com.example.java_gobang.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

@Component
// 这个类表示"匹配器"，通过这个类，负责完成整个匹配。是全局为一的对象。
// 在其它代码中，需要调用到这个匹配器
public class Matcher {
    // 创建3个匹配队列,。可以更加细分化成多个队列, 用User信息来表示
    private Queue<User> normalQueue = new LinkedList<>();
    private Queue<User> highQueue = new LinkedList<>();
    private Queue<User> veryHighQueue = new LinkedList<>();

    @Autowired
    private OnlineUserManager onlineUserManager;

    // 引入房间管理器，将两个用户添加到游戏房间里。
    @Autowired
    private RoomManager roomManager;

    // 不实例化，会造成空指针异常。所以要new一个实例对象。
    private ObjectMapper objectMapper = new ObjectMapper();

    // 操作匹配队列的方法
    // add a user to the corresponding queue
    // 三个队列是分开处理各自的线程的，彼此之间是互不干扰的,
    // 所以是对三个队列，分开加锁的。
    public void add(User user) {
        if (user.getScore() < 2000) {
            synchronized (normalQueue) {
                normalQueue.offer(user);
                // 玩家进入队列，那么进行通知
                normalQueue.notify(); // 和matchQueue.wait();对应
            }
            System.out.println("把玩家 " + user.getUsername() + " 加入到了normalQueue中！");
        } else if (user.getScore() >= 2000 && user.getScore() < 3000) {
            synchronized (highQueue){
                highQueue.offer(user);
                // 玩家进入队列，那么进行通知
                highQueue.notify(); // 和matchQueue.wait();对应
            }
            System.out.println("把玩家 " + user.getUsername() + " 加入到了highQueue中！");
        } else {
            synchronized (veryHighQueue) {
                veryHighQueue.offer(user);
                // 玩家进入队列，那么进行通知
                veryHighQueue.notify(); // 和matchQueue.wait();对应
            }
            System.out.println("把玩家 " + user.getUsername() + " 加入到了veryHighQueue中！");
        }
    }

    // 当玩家点击停止匹配的时候，需要把玩家从匹配队列中删除
    public void remove(User user) {
        if (user.getScore() < 2000) {
            synchronized (normalQueue) {
                normalQueue.remove(user);
            }
            System.out.println("把玩家 " + user.getUsername() + " 从normalQueue中删除！");
        } else if (user.getScore() >= 2000 && user.getScore() < 3000) {
            synchronized (highQueue) {
                highQueue.remove(user);
            }
            System.out.println("把玩家 " + user.getUsername() + " 从highQueue删除！");
        } else {
            synchronized (veryHighQueue) {
                veryHighQueue.remove(user);
            }
            System.out.println("把玩家 " + user.getUsername() + " 从veryHighQueue删除！");
        }
    }

    // Matcher constructor
    public Matcher() {
        // 分别创建三个线程，分别针对这三个匹配队列，进行操作。
        // 匿名内部类的方式，继承这个类，并重写 run() 这个方法
        Thread t1 = new Thread() {
            @Override
            public void run() {
                // 扫描normal queue
                // 循环不断扫描
                while (true) {
                    // 分装成一个方法，叫handlerMatch
                    handlerMatch(normalQueue);
                }
            }
        };
        t1.start();

        Thread t2 = new Thread() {
            @Override
            public void run() {
                while (true) {
                    handlerMatch(highQueue);
                }
            }
        };
        t2.start();

        Thread t3 = new Thread() {
            @Override
            public void run() {
                while (true) {
                    handlerMatch(veryHighQueue);
                }
            }
        };
        t3.start();
    }
    // matchQueue涵盖了三个所有队列, 在public Matcher()中，调用的是同一个matcherQueue，所以在这个方法里面加锁
    // 对形参 matchQueue 进行加锁，传入参数是normalQueue，就是对normalQueue加锁。以此类推
    private void handlerMatch(Queue<User> matchQueue) {
        synchronized (matchQueue) {
            try {
                // 1. 检测队列中元素个数是否达到2
                // 队列的初始情况是空
                // 如果往队列中添加一个元素，仍然是不能进行后续匹配操作的，
                // 因此用while来进行循环检查，是更加合理的。
                while (matchQueue.size() < 2) {
                    // 调取哪个对象的wait，是针对哪个对象进行加锁。就用那个队列来调用wait()，
                    // wait做三件事：1. 释放锁。 2. 等待通知。3. 通知到了以后，被唤醒，尝试重新获取锁
                    matchQueue.wait();
                    //return;
                }
                // 2. 从队列中取出两个玩家
                User player1 = matchQueue.poll();
                User player2 = matchQueue.poll();
                // 写代码时，加入关键的日志，这样帮助理解，程序是如何运行的。
                System.out.println("匹配出两个玩家: " + player1.getUsername() + ", " + player2.getUsername());

                // 3. 获取到玩家的 websocket 的会话
                //    获取会话的目的是为了告诉玩家，你排到了
                //    OnlineUserManager 这个 class 里面，ConcurrentHashMap 存了 userId 和 对应的 webSocketSession 的信息
                WebSocketSession session1 = onlineUserManager.getFromGameHall(player1.getUserId());
                WebSocketSession session2 = onlineUserManager.getFromGameHall(player2.getUserId());

                // 理论上来说，匹配队列中的玩家一定是在线状态，因为前面的逻辑已经判断过
                // 当某个玩家断开连接，会话则为空，就会把玩家从匹配队列中移除了。
                // 为了避免出现极端情况，再次判定是否为空。进行双重判断。
                if (session1 == null) {
                    // 如果玩家1 现在不在线了，就把玩家2 重新放回匹配队列中
                    matchQueue.offer(player2);
                    return;
                }
                if (session2 == null) {
                    // 如果玩家2 现在不在线了，就把玩家1 重新放回匹配队列中
                    matchQueue.offer(player1);
                    return;
                }
                // 当前能否排到两个玩家是同一个用户的情况吗？一个玩家入队列了两次？
                // 理论上不会出现
                // 1）如果玩家下线，就会对玩家移除匹配队列
                // 2）又禁止了玩家多开
                // 但是仍然在这里多进行一次判定，以免前面的逻辑出现bug是带来的严重后果（双重校验）
                if (session1 == session2) {
                    // 把其中一个玩家放回匹配队列，即可。
                    matchQueue.offer(player1);
                    return;
                }

                // 4. 把这两个玩家和房间放到一个游戏房间里
                // 创建房间
                Room room = new Room();
                roomManager.add(room, player1.getUserId(), player2.getUserId());

                // 5. 给玩家反馈信息：你匹配到对手了
                //    通过 websocket 返回一个 message 为 ‘matchSuccess’ 这样的响应
                //    此处是要给两个玩家都返回 ‘匹配成功’ 这样的信息。
                //    因此需要返回两次响应。

                //    返回player1的信息
                MatchResponse response1 = new MatchResponse();
                response1.setOk(true);
                response1.setMessage("matchSuccess");
                // sendMessage()的参数是 textMessage，对应的是websocket的数据包，textMessage 要通过字符串进行构造
                // 需要从 Java 对象转成 JSON 格式，因此，创建一个 objectMapper

                // 用 objectMapper.writeValueAsString(), 将 Java 对象，转换成 JSON string
                String json1 = objectMapper.writeValueAsString(response1);
                // 对象转成 JSON 格式
                session1.sendMessage(new TextMessage(json1));

                // 返回 player2 的信息
                MatchResponse response2 = new MatchResponse();
                response2.setOk(true);
                response2.setMessage("matchSuccess");
                String json2 = objectMapper.writeValueAsString(response2);
                // sendMessage()的参数是 textMessage，对应的是websocket的数据包，textMessage 要通过字符串进行构造
                // 需要从 Java 对象转成 JSON 格式，因此，创建一个 objectMapper
                session2.sendMessage(new TextMessage(json2));
            } catch (IOException | InterruptedException e) { // 两个异常合并成一个
                e.printStackTrace();
            }
        }
    }
}