## 实现匹配模块

让多个用户，在游戏大厅中能够进行匹配，系统会把实力相近的两个玩家凑成一桌，进行对战。

约定前后端交互接口（为什么要前后端分离：https://blog.51cto.com/u_15499328/5283578）

约定的前后端交互接口，也都是基于 websocket 来展开的

websocket 可以传输文本数据，也能传输二进制数据
此处就直接设计成让 websocket 传输 JSON 格式的文本数据即可
The WebSocket API (WebSockets) URL: https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API#:~:text=The%20WebSocket%20API%20is%20an,the%20server%20for%20a%20reply.


匹配请求,用 findMath 这个路径 来匹配，和发送 websocket：
客户端通过 websocket 给服务器发送一个 JSON 格式的文本数据.ws 是 websocket 的 protocol的缩写
ws://127.0.0.1:8080/findMatch

{
    message: 'startMatch' / 'stopMatch',    // 开始/结束匹配 的路径。通过 websocket 传输请求数据的时候，数据中是不必带有用户身份信息的
                                            // 当前用户的身份信息，在登录完成后，就已经保存到 HttpSession 中了，websocket里，也是能拿
                                            // 到之前登录好的 HttpSession 中的信息
}

匹配相应1：服务器立即返回的响应。 websocket.onclick = function(e) in game_hall.html
ws://127.0.0.1:8080/findMatch
这个响应，是客户端给服务器发送匹配请求之后，服务器立即返回的匹配响应。（服务器接到请求，会立即给客户端返回相应，表示当前的“开始/停止请求”是成功还是失败，但是这个响应，只是作为应答，告诉客户端，我知道了，仅此而已。）

{
    ok: true                                // 匹配成功
    reason: '',                             // 如果匹配失败，失败原因的信息
    message: 'startMatch' / 'stopMatch',    
}


// 真正在有结果的时候，需要依靠第二组的匹配相应。
匹配相应2： websocket.onmessage = function(e) in game_hall.html
ws://127.0.0.1:8080/findMatch

// 匹配成功, 这个响应是真正匹配到对手之后，服务器主动推送回来的信息。
// 匹配到的对手，不需要在这个响应中体现，仍然都放到服务器这边来保存即可。
{
    ok: true                                // 匹配成功
    reason: '',                             
    message: 'matchSuccess',    
}

--------------------------------------------------------------------------------------------------------------------------------
## 实现匹配页面的游戏大厅   game_hall.html

匹配按钮，第一次点击，表示开始匹配，第二次点击，表示停止匹配。


--------------------------------------------------------------------------------------------------------------------------------

UserAPI.java: 
通过前端页面，基于 Ajax 来向服务器后台，请求 ‘/userInfo’的路径，从而拿到当前的用户信息。
public Object getUserInfo(HttpServletRequest req)

1. 需要在game_hall.html里面，引入 jquery
2. 页面加载的时候发送的，所以ajax写在script标签里面。

当我们修改了 css 样式 / js 文件之后，往往要在浏览器中使用 ctrl+f5 强制刷新，才能生效。否则浏览器可能仍然在执行旧版本的代码，因为浏览器自带缓存，本地电脑的硬盘上。

--------------------------------------------------------------------------------------------------------------------------------

点击“开始匹配”时，当前这个页面，要和服务器，基于websocket，进行数据交互，来完成匹配的过程。

JSON string <==> JS object
JSON string => JS object:   JSON.parse
JS object => JSON string:   JSON.stringify

JSON string <==> Java object
JSON string => Java object: ObjectMapper.readValue
Java object => JSON string: ObjectMapper.writeValueAsString

--------------------------------------------------------------------------------------------------------------------------------

MatchAPI:
处理websocket请求，需要 extends TextWebSocketHandler 类。
public class MatchAPI extends TextWebSocketHandler
重写的4个父类的方法。

websocket session vs HttpSession
In WebSocketConfig.java file, there are important comments in this method. 
public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        // 当客户端(testAPI)连接 “/test” 路径的时候，就会触发到testAPI，调用执行到testAPI里的方法
        // 将testAPI这个类，和路径 ‘/test’ 关联起来。写前端代码的时候，跟url关联在一起。
        webSocketHandlerRegistry.addHandler(testAPI, "/test");
        // ws://127.0.0.1:8080/findMatch，这个是约定好的. addInterceptors，使用拦截器，拿到Httpsession，
        // （放到Websocket的session中）。用户登录就会给HttpSession中，保存用户的信息.
        // 用户的Httpsession和Websocket的session是不一样的。
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

        // /game路径和这个gameAPI类 关联起来了，再通过add来获取session
        webSocketHandlerRegistry.addHandler(gameAPI, "/game")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    video # 