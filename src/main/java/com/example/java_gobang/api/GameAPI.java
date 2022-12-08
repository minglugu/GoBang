package com.example.java_gobang.api;

import com.example.java_gobang.game.*;
import com.example.java_gobang.model.User;
import com.example.java_gobang.model.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.io.IOException;

// description: 处理game websocket的连接请求，处理类似match API，继承自 TextWebSocketHandler
// override 4 个方法
// 注册 Spring 里面的组件
@Component
public class GameAPI extends TextWebSocketHandler {

    // 创建objectMapper对象，把对象转换成 JSON 格式的字符串
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private OnlineUserManager onlineUserManager;

    // 操作数据库，所以用resource
    @Resource
    private UserMapper userMapper;

    // 连接成功之后，服务器处理的逻辑. video 55 & 56
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 准备好GameReadyResponse对象
        GameReadyResponse resp = new GameReadyResponse();

        // 1. 先获取到用户的身份信息，（从Httpsession 里拿到当前用户的对象）.根据“user”这个key，拿到user这个对象
        // 跟游戏大厅 (game_hall) 是同样一个效果
        // 前面注册的时候，已经加上了拦截器的操作。webSocketHandlerRegistry.addHandler(gameAPI, "/game")...
        // 就可以拿到Httpsession里面的attribute了 get(Object key)
        User user = (User) session.getAttributes().get("user");

        // 先判定异常，再处理正常逻辑
        // 针对用户尚未登录的情况。User跳过登录，可能是null,
        // 所以，此处进行一个判断
        if (user == null) {
            resp.setOk(false);
            resp.setReason("用户尚未登录");
            // 转换成JSON格式的字符串，返回到客户那里
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            // session.close(); // 也可以通过客户端界面处理
            return;
        }
        // 2. 判定当前用户是否在此房间里，用 RoomManager 这个房间管理器，进行查询。
        Room room = roomManager.getRoomByUserId(user.getUserId());
        if (room == null) {
            // 如果为null，当前没有找到对应的房间。该玩家还没有匹配到
            resp.setOk(false);
            resp.setReason("当前用户尚未匹配到");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            return;
        }

        // 3. 判定当前是不是多开。该用户是不是在其他地方，进入游戏
        //    onlineUserManager(可以用来管理用户在线状态，但局限于匹配页面game_hall)来检测.
        //    目前在game_room里，因此，玩家从game_hall页面离开之后，进入game_room,需要重新管理用户的在线状态了。
        //    对用户进行查找
        if (onlineUserManager.getFromGameHall(user.getUserId()) != null
                || onlineUserManager.getFromGameRoom(user.getUserId()) != null) {
            // 如果一个账号，同时在游戏大厅，也在游戏房间，也算是多开
            resp.setOk(true);
            resp.setReason("禁止多开游戏界面");
            resp.setMessage("multiple connections are not allowed");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            return;
        }

        // 4. 设置当前玩家上线！
        onlineUserManager.enterGameRoom(user.getUserId(), session);

        // 5. 把两个玩家加入到游戏房间中 （此处两个user，同时执行if的逻辑操作，为多线程操作，需要考虑线程安全问题）
        //    两个玩家都以为自己是先手。
        //    当前这个逻辑是在 game_room.html 页面加载的时候进行的。
        //    前面的创建房间/匹配过程, 是在 game_hall.html 页面中完成的. 涉及到页面跳转
        //    因此前面匹配到对手之后, 需要经过页面跳转, 来到 game_room.html 才算正式进入游戏房间(才算玩家准备就绪)
        //    当前这个逻辑是在 game_room.html 页面加载的时候进行的.
        //    执行到当前逻辑, 说明玩家已经页面跳转成功了!!
        //    页面跳转, 其实是个大活~~ (很有可能出现 "失败" 的情况的)
        //    第一个玩家：
        synchronized (room) {
            if (room.getUser1() == null) {
                // 第一个玩家还尚未加入房间.
                // 就把当前连上 websocket 的玩家作为 user1, 加入到房间中.
                room.setUser1(user);
                // 把先连入房间的玩家作为先手方.
                room.setWhiteUser(user.getUserId());
                System.out.println("玩家 " + user.getUsername() + " 已经准备就绪! 作为玩家1");
                return;
            }
            //     第二个玩家：
            if (room.getUser2() == null) {
                // 第一个玩家还尚未加入房间.
                // 就把当前连上 websocket 的玩家作为 user1, 加入到房间中.
                room.setUser2(user);
                // 把先连入房间的玩家作为先手方.
                System.out.println("玩家 " + user.getUsername() + " 已经准备就绪! 作为玩家2");

                // 当两个玩家都加入成功之后, 就要让服务器, 给这两个玩家都返回 websocket 的响应数据.
                // 通知这两个玩家说, 游戏双方都已经准备好了.
                // 通知玩家1, pay attention to the order of parameters of user1(thisUser) and user2(thatUser)
                noticeGameReady(room, room.getUser1(), room.getUser2());
                // 通知玩家2, pay attention to the order of parameters of user2(thisUser) and user1(thatUser)
                noticeGameReady(room, room.getUser2(), room.getUser1());
                return;
            }
        }

        // 6. 此处如果又有玩家尝试连接同一个房间, 就提示报错.
        //    这种情况理论上是不存在的, 为了让程序更加的健壮, 还是做一个判定和提示.
        resp.setOk(false);
        resp.setReason("当前房间已满, 您不能加入房间");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
    }

    private void noticeGameReady(Room room, User thisUser, User thatUser) throws IOException {
        GameReadyResponse resp = new GameReadyResponse();
        resp.setMessage("gameReady"); // based on the agreed value and param
        resp.setOk(true);
        resp.setReason("");
        resp.setRoomId(room.getRoomId());
        resp.setThisUserId(thisUser.getUserId());
        resp.setThatUserId(thatUser.getUserId());
        resp.setWhiteUser(room.getWhiteUser());
        // 把当前的响应数据传回给玩家(客户端)。
        WebSocketSession webSocketSession = onlineUserManager.getFromGameRoom(thisUser.getUserId());
        webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));

    }

    // 收到请求之后的处理
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 1. 先从 session 里，拿到当前用户的身份信息
        User user = (User)session.getAttributes().get("user");
        if (user == null) {
            System.out.println("[handleTextMessage] 当前玩家尚未登录！");
            return;
        }

        // 2. 根据玩家id，获取到房间对象，通过roomManager
        Room room = roomManager.getRoomByUserId(user.getUserId());

        // 3. 通过room对象，处理这次具体的请求
        room.putChess(message.getPayload());
    }

    // 连接异常的处理
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        User user = (User) session.getAttributes().get("user");
        if (user == null) {
            // 此处就简单处理，在断开连接的时候，就不给客户端返回响应了。
            return;
        }
        // 查一下用户的状态
        WebSocketSession exitSession = onlineUserManager.getFromGameRoom(user.getUserId());
        // 是同一个会话的情况下，再进行下线操作
        // 加上这个判定，目的是为了避免在多开的情况下, 第二个用户退出连接动作, 导致第一个用户的会话被删除.
        if (exitSession == session) {
            onlineUserManager.exitGameRoom(user.getUserId());
        }
        System.out.println("当前用户 " + user.getUsername() + " 游戏房间连接异常!");

        // 通知对手获胜
        noticeThatUserWin(user);
    }

    // 连接关闭后的处理，网络断开，或者用户关闭页面。
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        User user = (User) session.getAttributes().get("user");
        if (user == null) {
            // 此处就简单处理，在断开连接的时候，就不给客户端返回响应了。
            return;
        }
        // 查一下用户的状态，如果是在线，那么就设置成离线。
        WebSocketSession exitSession = onlineUserManager.getFromGameRoom(user.getUserId());
        // 是同一个会话的情况下，再进行下线操作
        // 加上这个判定，目的是为了避免在多开的情况下, 第二个用户退出连接动作, 导致第一个用户的会话被删除.
        if (exitSession == session) {
            onlineUserManager.exitGameRoom(user.getUserId());
        }
        // 打印日志
        System.out.println("当前用户 " + user.getUsername() + " 离开游戏房间!");

        // 通知对手获胜
        noticeThatUserWin(user);
    }

    private void noticeThatUserWin(User user) throws IOException {
        // 1. 根据当前玩家，找到玩家所在的room
        Room room = roomManager.getRoomByUserId(user.getUserId());
        if (room == null) {
            // 房间已经被释放了，也就没有“对手了”
            System.out.println("当前房间已经释放，无需通知对手");
            return;
        }

        // 2. 根据房间找到对手, 是获胜方
        User thatUser = (user == room.getUser1()) ? room.getUser2() : room.getUser1();
        // 3. 找到对手的在线状态
        WebSocketSession webSocketSession = onlineUserManager.getFromGameRoom(thatUser.getUserId());
        if (webSocketSession == null) {
            // 对手也掉线了！
            System.out.println("对手也掉线了，无需通知！");
            return;
        }

        // 4. 构造响应，来通知对手，你是获胜方
        GameResponse resp = new GameResponse();
        resp.setMessage("putChess");
        resp.setUserId(thatUser.getUserId());
        resp.setWinner(thatUser.getUserId());

        webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));

        // 5. 玩家掉线，也需要更新数据库里的获胜方和失败方
        //    获胜方是thatUser
        int winUserId = thatUser.getUserId();
        int loseUserId = user.getUserId();
        userMapper.userWin(winUserId);
        userMapper.userLose(loseUserId);

        // 6. 释放房间对象.不释放的话，造成roomManager里的room，越来越多，会造成内存泄露的问题
        roomManager.remove(room.getRoomId(), room.getUser1().getUserId(), room.getUser2().getUserId());

    }
}
