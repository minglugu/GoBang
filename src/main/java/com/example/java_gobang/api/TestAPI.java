package com.example.java_gobang.api;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class TestAPI extends TextWebSocketHandler {

    // 重写以下方法，去掉调用父类的方法。

    // 当连接建立就绪之后，就会触发这个方法。会感知到客户端建立连接或客户上线这个事情。
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("连接成功");
    }

    // 如果客户端给服务器，发了文本消息，会感知到消息内容是什么？ textMessage
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("收到消息：" + message.getPayload()); // 返回string的类型
        // 让服务器收到数据后，把数据原封不动的返回回去，达到客户端接受这样的效果。
        // websocket session，与http的session不一样
        // 实现消息推送的效果
        session.sendMessage(message);
    }

    // 传输出现异常：网络出现波动，导致连接断了。会触发这个方法，知道连接出现问题。
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.out.println("连接异常");
    }

    // 客户端和服务器，连接被关闭。就会执行这个函数。感知用户下线的过程。
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("连接关闭");
    }
}
