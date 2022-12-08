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


// 进行注册 webSocketHandler 的核心配置类
// 在 WebSocketConfig 这个类里，告知当前的websocket和告知Spring，当前哪个类 是和 哪个路径是相匹配的。
// 比如说 testAPI 是和 "/test"这个路径是相匹配的。
@Configuration  // 跟 Spring 关联
@EnableWebSocket // 开启 websocket
public class WebSocketConfig implements WebSocketConfigurer {

    // 用Spring的方式，注入进来。
    // 处理客户端的请求，与服务器连接
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
        // 注意：用户的Httpsession 和 Websocket 的 session 是不一样的。
        // 关联的路径，是前面约定好的 “/findMatch” 路径，靠这个路径，来跟服务器建立连接
        // 看到 findMatch 这个路径的 websocket 请求，就会执行 matchAPI 里面的代码
        // 加拦截器的作用，起到的效果是，能够拿到前面的http session
        // 在注册 websocket API 的时候，就需要把前面准备好的HttpSession拿过来。(拿到 websocket 的 Session)
        // 用户登录就会给 HttpSession 中保存用户的信息。对匹配很重要，因为会根据相近的积分来匹配。
        // 然而约定的接口里面，没有给定用户信息，需要拿到 在用户登录时，存到HttpSession里面的信息。
        // HttpSessionHandshakeInterceptor reference URL:
        // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/socket/server/support/HttpSessionHandshakeInterceptor.html
        // 当 websocket 需要拿到用户的信息时，从httpsession拿，因为用户登录的时候，会保存用户的信息在http的session里面。

        // 简而言之：拦截器 拦截到HttpSession 的用户信息，加载到 websocket的session里面。
        webSocketHandlerRegistry.addHandler(matchAPI, "/findMatch")
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        // “/game路径” 和这个gameAPI类 关联起来了，再通过addInterceptor 这样的拦截器，来获取session
        webSocketHandlerRegistry.addHandler(gameAPI, "/game")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
