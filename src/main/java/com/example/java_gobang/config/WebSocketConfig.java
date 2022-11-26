package com.example.java_gobang.config;

import com.example.java_gobang.api.GameAPI;
import com.example.java_gobang.api.MatchAPI;
import com.example.java_gobang.api.TestAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration  // 跟 Spring 关联
@EnableWebSocket // 开启 websocket
public class WebSocketConfig implements WebSocketConfigurer {

    // 用Spring的方式，注入进来
    @Autowired
    private TestAPI testAPI;

    // 匹配机制，涉及的类
    @Autowired
    private MatchAPI matchAPI;

    // 对战模块，涉及的类
    @Autowired
    private GameAPI gameAPI;

    @Override
    // 注册handler到框架里面，告知当前websocket，和哪个路径相匹配，这个testAPI和/test这个路径匹配
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        // 当客户端(testAPI)连接 “/test” 路径的时候，就会触发到testAPI，调用执行到testAPI里的方法
        // 将testAPI这个类，和路径 ‘/test’ 关联起来。写前端代码的时候，跟url关联在一起。
        webSocketHandlerRegistry.addHandler(testAPI, "/test");
        // ws://127.0.0.1:8080/findMatch，这个是约定好的. addInterceptors，使用拦截器，拿到Httpsession，
        // （放到Websocket的session中）。用户登录就会给HttpSession中，保存用户的信息.
        // 用户的Httpsession和Websocket的session是不一样的。
        webSocketHandlerRegistry.addHandler(matchAPI, "/findMatch")
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        // /game路径和这个gameAPI类 关联起来了，再通过add来获取session
        webSocketHandlerRegistry.addHandler(gameAPI, "/game")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
