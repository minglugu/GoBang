package com.example.java_gobang.api;

import com.example.java_gobang.game.MatchRequest;
import com.example.java_gobang.game.MatchResponse;
import com.example.java_gobang.game.Matcher;
import com.example.java_gobang.game.OnlineUserManager;
import com.example.java_gobang.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

// 通过这个类来处理匹配功能中的 websocket 请求
// 光写这个类，还不能处理websocket 请求。把这个MatchAPI 类 注册到 WebSocketConfig 这个类里面，
// 进行注册websocket handler核心配置类，告知当前websocket，和哪个路径相匹配
@Component
public class MatchAPI extends TextWebSocketHandler {
    // objectMapper 用于处理 JSON string <==> Java object 之间的转换
    private ObjectMapper objectMapper = new ObjectMapper();

    // 建好OnlineUserManager以后，就可以在MatchAPI里面，使用到OnlineUserManager里面的对象
    @Autowired
    private OnlineUserManager onlineUserManager;

    // matcher来决定增加还是删除用户。
    @Autowired
    private Matcher matcher;

    // 是否是多线程
    // 如果有多个用户和服务器建立连接/断开连接，此时服务器就是并发得在针对HashMap 进行修改
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 玩家上线，加入到OnlineUserManager中

        // 1. 当前的用户是谁，先获取到当前用户的身份信息（谁在游戏大厅中，建立的连接），websocket 通过WebSocketConfig拦截器,
        //    已经拿到登录用户的Httpsession，
        //    存到websocket的session里面
        //    此处是的session,是websocket的session。session.getAttributes()直接返回了map，其中的key-value，来自httpsession。
        //    此处的代码，能够getAttributes,是因为在注册WebSocket的时候，加上的（WebSocketConfig类里面的
        //    .addInterceptors(new HttpSessionHandshakeInterceptor()这个方法）.这个逻辑就把HttpSession 中的 Attribute
        //    都给拿到 WebSocketSession里了。在UserAPI里面，在Http登录逻辑中，往HttpSession中存了 User 数据，
        //    有这样一段代码：httpSession.setAttribute("user", user); 此时就可以在WebSocketSession 中把之前HttpSession 里存
        //    的User对象给拿到了。
        //    session.getAttributes() 返回的是 map，包含的键值对，是从httpSession的attribute 里面 拿回的键值对。
        //    此处拿到的user，可能为空。
        //    如果用户，没有通过HTTP 来进行登录，直接就通过 /game_hall.html 这个 url 来访问游戏大厅页面，此时会出现user为空的情况。
        //    所以要判定user是否为null
        try {
            User user = (User) session.getAttributes().get("user"); //得到 user 对象。
            // 2. 先判定当前用户，是否已经登陆过(已经是在线状态)，如果是已经在线，就不继续进行后续逻辑（不许多次登录。。。）。
            //    多开的问题：用户打开多个浏览器，尝试登录。那么userId的value是唯一的，但每次连接，产生的session不一样。
            //    那么存在hashmap里的 value 会被后来的session value给覆盖掉。
            //    所以此处的逻辑是，禁止多开。
            if (onlineUserManager.getFromGameRoom(user.getUserId()) != null
                    || onlineUserManager.getFromGameHall(user.getUserId()) != null) {
                // 当前用户已经登录
                // 针对这个情况，通过构造 MatchResponse 要告知客户端，这里重复登录
                MatchResponse response = new MatchResponse();
                response.setOk(true);
                response.setReason("当前禁止多开(同个用户重复登录)");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                // 关掉websocket连接, 断开与服务器的连接。
                // 关掉的这个操作，会立即触发 afterConnectionClosed()，会有一个exitGameHall 这个做移除操作。
                // 会根据userId删除，但是userId是相同的，可能将正确的键值对（userId和session）删除。需要防止出现这样的情况。
                // 所以在afterConnectionClosed()，也做出一个判定。
                session.close();
                return;
            }
            // 3. 拿到身份信息后，把玩家设置成在线状态了。把用户和session这个键值对，加到“在线用户管理器”里面。
            onlineUserManager.enterGameHall(user.getUserId(), session);
            System.out.println("玩家 " + user.getUsername() + " 进入游戏大厅!");
        } catch (NullPointerException e) {
            System.out.println("[MatchAPI.afterConnectionEstablished] 当前用户未登录！");
            // e.printStackTrace();
            // 出现空指针异常，说明当前用户的身份信息是空，用户未登录
            // 把当前用户尚未登录这个信息，给返回 回去。按照约定的前后端接口。要另外在game package里面，单独建立websocket的
            // 请求和响应的类(MatchRequest 和 MatchResponse。
            MatchResponse response = new MatchResponse();
            response.setOk(false);
            response.setMessage("尚未登录！不能进行后续的匹配功能！");
            // 把response构造成JSON格式的字符串. sendMessage(WebSocket 对象)
            // object ->  JSON 字符串的转换：先通过ObjectMapper 把 MatchResponse 对象转成JSON 字符串，
            // 然后再包装上一层TextMessage, 再进行传输。
            // TextMessage 就表示一个 文本格式的 websocket 数据包
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }

    // 实现处理来自客户端的开始匹配请求和处理停止匹配请求
    /*
       @param session: 客户端的websocket session
       @param message: 客户端发来的 startMatch 和 stopMatch 的 message
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        User user = (User)session.getAttributes().get("user");
        // 获取到客户端给服务器发送的数据，是由textMessage来表示的数据
        // getPayload()得到websocket数据包里面的数据载荷（是JSON格式的字符串），再解析成我们希望的对象。
        String payload = message.getPayload();
        // 当前数据载荷是一个 JSON 格式的字符串，需要把他转成 Java 的MatchRequest对象。是从客户端读来的数据
        MatchRequest request = objectMapper.readValue(payload, MatchRequest.class);
        // 这是个客户端返回的数据
        MatchResponse response = new MatchResponse();
        // stopMatch和startMatch是在约定前后端接口的时候，已经设计好的情况
        if (request.getMessage().equals("startMatch")) {
            // 进入匹配队列
            // 先创建一个类，表示匹配对列，把当前用户给加进去
            matcher.add(user);
            // 把玩家信息放入匹配队列后，就可以返回一个响应给客户端了，告诉客户端，已经放入队列成功。
            response.setOk(true);
            response.setMessage("startMatch");
        } else if (request.getMessage().equals("stopMatch")) {
            // 退出匹配队列
            // 先创建一个类，表示匹配对列，把当前用户从队列中移除
            matcher.remove(user);
            // 移除之后，就可以返回一个响应给客户端了
            response.setOk(true);
            response.setMessage("stopMatch");
        } else {
            // 非法情况
            response.setOk(false);
            response.setReason("非法的匹配请求");
        }
        // 服务器这边在处理 匹配请求的时候，是要立即返回一个 websocket 响应的
        // 虽然在服务器代码这里构造了响应对象，但是忘记sendMessage，给发回去了
        // 一开始写代码的时候，没有这两行，所以console.log里面没有返回的数据信息
        // 服务器的 response，需要返回给前端。
        String jsonString = objectMapper.writeValueAsString(response);
        session.sendMessage(new TextMessage(jsonString));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        try {
            // 玩家下线，从 OnlineUserManager 中删除
            User user = (User) session.getAttributes().get("user");
            // 1. 先获取session
            WebSocketSession tmpSession = onlineUserManager.getFromGameHall(user.getUserId());
            // 2. 如果相等，才执行下线操作。当前的tmpSession，是不是等于session，他俩相等，才执行下线操作。
            //    参数的session 是当前连接的用户的会话。tmpSession 是之前存好的会话。保证下线的人，是保存的session的这个人的本身
            if (tmpSession == session) {
                onlineUserManager.exitGameHall(user.getUserId());
            }
            // 如果玩家正在匹配中，而websocket连接断开了，就应该移除匹配队列
            matcher.remove(user);
        } catch (NullPointerException e) {
            System.out.println("[MatchAPI.handleTransportError] 当前用户未登录！");
//            e.printStackTrace();
//            // 出现空指针异常，说明当前用户的身份信息是空，用户未登录
//            // 把当前用户尚未登录这个信息，给返回 回去。按照约定的前后端接口。要另外在game package里面，单独建立websocket请求和响应的类/
            // 不应该在连接关闭之后，还尝试发送消息给客户端
//            MatchResponse response = new MatchResponse();
//            response.setOk(false);
//            response.setMessage("尚未登录！不能进行后续的匹配功能！");
//            // 把response构造成JSON格式的字符串. sendMessage(WebSocket 对象)
//            // 先通过ObjectMapper 把 MatchResponse 对象转成JSON 字符串，然后再包装上一层TextMessage, 再进行传输。
//            // TextMessage 就表示一个 文本格式的 websocket 数据包
//            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }

    // 当第二个浏览器用同一个userId登录，会让第二个多开的浏览器，下线删除。但会误删第一个浏览器登录的userId
    // 参数里的session，当前连接的用户的会话。
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            // 玩家下线，从 OnlineUserManager 中删除
            // 同上线的逻辑
            User user = (User) session.getAttributes().get("user");
            // tmpSession是之前，存好的session
            WebSocketSession tmpSession = onlineUserManager.getFromGameHall(user.getUserId());
            // 如果相等，才执行下线操作。当前的tmpSession，是不是等于session，他俩相等，才执行下线操作。
            if (tmpSession == session) {
                onlineUserManager.exitGameHall(user.getUserId());
            }
            // 如果玩家正在匹配中，而websocket连接断开了，就应该移除匹配队列
            matcher.remove(user);
        } catch (NullPointerException e) {
            System.out.println("[MatchAPI.afterConnectionClosed] 当前用户未登录！");

            // e.printStackTrace();
            // 出现空指针异常，说明当前用户的身份信息是空，用户未登录
            // 把当前用户尚未登录这个信息，给返回 回去。按照约定的前后端接口。要另外在game package里面，单独建立websocket请求和响应的类/
            // 不应该在连接关闭之后，还尝试发送消息给客户端
//            MatchResponse response = new MatchResponse();
//            response.setOk(false);
//            response.setMessage("尚未登录！不能进行后续的匹配功能！");
//            // 把response构造成JSON格式的字符串. sendMessage(WebSocket 对象)
//            // 先通过ObjectMapper 把 MatchResponse 对象转成JSON 字符串，然后再包装上一层TextMessage, 再进行传输。
//            // TextMessage 就表示一个 文本格式的 websocket 数据包
//            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
    }
}
