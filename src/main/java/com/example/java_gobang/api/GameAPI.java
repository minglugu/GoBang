package com.example.java_gobang.api;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

// 处理game websocket的连接请求，类似match API
@Component
public class GameAPI extends TextWebSocketHandler {
    // 连接成功之后的处理
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    }

    // 收到请求之后的处理
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
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
