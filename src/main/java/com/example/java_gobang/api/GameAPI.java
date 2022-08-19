package com.example.java_gobang.api;

import com.example.java_gobang.game.GameReadyResponse;
import com.example.java_gobang.game.OnlineUserManager;
import com.example.java_gobang.game.Room;
import com.example.java_gobang.game.RoomManager;
import com.example.java_gobang.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.servlet.http.HttpSession;

// 处理game websocket的连接请求，类似match API
@Component
public class GameAPI extends TextWebSocketHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private OnlineUserManager onlineUserManager;

    // 连接成功之后的处理
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        GameReadyResponse resp = new GameReadyResponse();

        // 1. 先获取到用户的身份信息，（从Httpsession 里拿到当前用户的对象）.根据user这个key，拿到user这个对象
        User user = (User) session.getAttributes().get("user");

        // 先判定异常，再处理正常逻辑

        // 针对用户尚未登录的情况。User跳过登录，可能是null
        if (user == null) {
            resp.setOk(false);
            resp.setReason("用户尚未登录");
            // 转换成JSON格式的字符串，返回到客户那里
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            // session.close();
            return;
        }
        // 2. 判定当前用户是否在此房间里
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
            // 如果一个账号，在游戏大厅，也在游戏房间，也算是多开
            resp.setOk(true);
            resp.setReason("禁止多开游戏界面");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
            return;
        }

        // 4. 设置当前玩家上线！
        onlineUserManager.enterGameRoom(user.getUserId(), session);
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
    }

    // 连接关闭后的处理
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    }
}
