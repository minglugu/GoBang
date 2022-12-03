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

--------------------------------------------------------------------------------------------------------------------------------

匹配玩家的逻辑：

目标：从带匹配的玩家中，选出分数尽量相近的玩家
把整个所有的玩家，按照分数，划分成 三类：
Normal        score < 2000
High          score >= 2000 && score < 3000
Very High     score >= 3000

可以根据score，细分出更多的队列，匹配出更好的效果。

给这三个等级，分配三个不同的队列，根据当前玩家的分数，来把这个玩家的用户信息，放到对应的队列里

通过一个专门的线程，去不停的扫描这个匹配队列。
只要队列里的元素（匹配中的玩家）凑成一对，把这一对玩家取出来，放到同一个游戏房间中。

--------------------------------------------------------------------------------------------------------------------------------

线程安全的问题。
Matcher.java 中的三个匹配队列（normalQueue ... ），是多线程执行的。

MatchAPI 是处理匹配请求的时候，handleTextMessage()这个方法把玩家放到[matcher.add(user)]匹配队列里面去的。

从队列里面，取元素：Matcher.java 这个类里面，在三个线程(t1, t2, t3)里，用 handlerMatch() 进行取玩家。

删除操作：1. MatcherAPI.java 里面，这几行代码：
         else if (request.getMessage().equals("stopMatch")) {
            // 退出匹配队列
            // 先创建一个类，表示匹配对列，把当前用户从队列中移除
            matcher.remove(user);
            // 移除之后，就可以返回一个响应给客户端了
            response.setOk(true);
            response.setMessage("stopMatch");
        
         2. 断开的地方，也有删除操作
         if (tmpSession == session) {
                onlineUserManager.exitGameHall(user.getUserId());
            }
            // 如果玩家正在匹配中，而websocket连接断开了，就应该移除匹配队列
            matcher.remove(user);
上述这些操作，可能是在不同的线程中，操作执行的。

首先Spring在处理websocket请求的时候，就有一组专门的线程。玩家什么时候，访问websocket的服务器，是不确定的。有几个玩家来访问，也是不确定的。
所以通过多线程的方式，来并发处理这里的请求。

出队列的操作，是另外一组自己写的线程(t1, t2, t3), handlerMatch （Matcher/java）。此线程跟websocket不是同一个线程，当两组线程，对队列进行操作时，add和remove users的时候，会有线程安全的问题。此处，需要注意线程安全问题。

如何解决： 加锁 synchronized。需要指定锁对象，针对谁进行加锁。
只有多个线程，在尝试对同一个锁对象，进行加锁的时候，才会有互斥效果。

此处加锁的时候，如果是多个线程，访问的是不同的队列，不涉及线程安全问题。
必须得是多个线程操作同一个队列，才需要加锁。

三个对类，是没有竞争关系的。

--------------------------------------------------------------------------------------------------------------------------------

如果匹配队列中，没有或只有一个玩家，就会反复调用handlerMatch（见下面的代码）和返回，处于循环状态。cpu负载很高，但是没什么意思。忙等。
public void run() {
    while(true) {
        handlerMatch(normalQueue);
    }
}
解决方法：用wait / notify
在扫描过程中，用wait来等待，当真正有玩家进入匹配队列的时候，调用notify来唤醒

--------------------------------------------------------------------------------------------------------------------------------

通过匹配的方式，自动给玩家 加入到一个房间，即游戏进行的场所。
服务器上，同时存在多个游戏房间，需要一个“游戏房间管理器”，管理多个游戏房间。game_room
Room.java

UUID

RoomManager.java: 希望根据 房间id 找到房间对象。
也希望根据 玩家id 找到玩家所属的房间。

--------------------------------------------------------------------------------------------------------------------------------

项目的验证：
在服务器的 terminal 里面，打印的内容如下：
[login] username=zhangsan
玩家 zhangsan 进入游戏大厅!
这段日志的打印，是在MatchAPI.java 中，System.out.println("玩家 " + user.getUsername() + " 进入游戏大厅!"); 这段代码实现的。

--------------------------------------------------------------------------------------------------------------------------------

问题1.
当前发现，玩家点击匹配之后，匹配按钮文本没有改变。

分析之前写的代码，点击按钮的时候，仅仅是给服务器发送了 websocket 请求，告诉服务器，我要开始匹配了。
服务器会立刻返回一个响应，“进入匹配队列成功”，然后页面再修改按钮的文本

但是console里面，没有打印其它的日志。说明服务器没有返回。
protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {}
这里的代码出了问题，忘记给前端返回response了。缺少了下面两行的代码

// 服务器这边在处理 匹配请求的时候，是要立即返回一个 websocket 响应的
// 虽然在服务器代码这里构造了响应对象，但是忘记sendMessage，给发回去了
// 一开始写代码的时候，没有这两行，所以console.log里面没有返回的数据信息
// 服务器的 response，需要返回给前端。
    String jsonString = objectMapper.writeValueAsString(response);
    session.sendMessage(new TextMessage(jsonString));

--------------------------------------------------------------------------------------------------------------------------------
 
 同一个用户，多次登录。需要断开后面登录的页面，同时还要在界面上有个明确的提示。
 此处需要调整前端代码game_hall.html，当检测到多开的时候，就给用户一个明确的提示。

        websocket.onclose = function() {
            console.log("onClose");
        
            // 之前为了实现多开的效果，在这个逻辑中加入了 alert 和跳转。
            // 如果多开了，服务器就会主动关闭 websocket 连接，导致客户端跳转到 login.html 页面
            alert("和游戏大厅断开连接");
            loation.replace("/login.html");
        }

--------------------------------------------------------------------------------------------------------------------------------

location.replace vs location.assign
URL: https://developer.mozilla.org/en-US/docs/Web/API/Location/replace

The replace() method of the Location interface replaces the current resource with the one at the provided URL. The difference from the assign() method is that after using replace() the current page will not be saved in session History, meaning the user won't be able to use the back button to navigate to it.


--------------------------------------------------------------------------------------------------------------------------------
Clear cache: ctrl + f5

修改JS 代码以后，刷新页面的时候，要使用 ctrl + f5 强制刷新页面，因为可能还是运行的是旧版本 JS 的代码。因为浏览器是有缓存的(cache)

--------------------------------------------------------------------------------------------------------------------------------

Summary of matching two players：视频 #46

1. 点击开始匹配之后 
   1）先触发 JS 中的按钮的点击事件回调。matchButton.onclick() in game_hall.html
      websocket.send(JSON.stringify(...)), 来给服务器发送请求。
        matchButton.onclick = function() {
            // 点击事件的回调
            // 触发websocket请求之前，先确认一下，websocket 连接是否好着呢
            // 检测，websocket 是否在连接的状态。
            // websocket.readyState: The current state of the connection.
            // https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/readyState
            if (websocket.readyState == websocket.OPEN) {
                // 如果当前 readyState 处在 OPEN 状态，说明连接是好的，可以发送数据
                // 这里发送的数据有两种可能，开始匹配/停止匹配。此处有两种情况，需要分开处理。
                // 当发送的请求时“开始匹配”时
                if (matchButton.innerHTML == '开始匹配') {
                    console.log("开始匹配");
                    // websocket 就会将 JS object 转成 JSON string，发送startMatch请求给服务器。
                    // JSON.stringify：https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON/stringify
                    websocket.send(JSON.stringify({                        
                            message: 'startMatch',  // 此处的参数，是具体的JSON对象。
                    }));

    2）服务器处理这个匹配请求。
       在handleTextMeesage()这个方法里 in MatcherAPI.java 这个类里。
       按照 JSON 格式，给当前请求的内容，进行解析。此处的payload，就是刚才websocket发送的数据(上面1）部分的数据)。在解析成 MatchRequest 对象，其中只有一个关键的字段(attribute)，叫message。用来区分，是匹配还是停止匹配的(startMatch / stopMatch)。

       关键操作：
       a) 然后再调用matcher.add(user), 将用户加入到响应的匹配队列中。（Match.java 的 add() 操作）
       b) 用这两行代码，服务器把响应立即发回给客户端，把用户加入到匹配队列。(in MatcherAPI.java)
          String jsonString = objectMapper.writeValueAsString(response);
          session.sendMessage(new TextMessage(jsonString));
       c) 客户端game_hall.html在收到响应后，在 websocket.onmessage 这里，收到一个匹配请求。
          resp = JSON.parse(e.data)  // 和 line275 的response对象 是和 resp 对应的
          当收到这个匹配响应之后 resp.message == 'startMatch' ， 打印日志，
          并更新按钮中的文本：
          console.log("进入匹配队列成功！")
          matchButton.innerHTML = '匹配中...(点击停止)'

          MatchResponse.java 中 message 是关键，通过它来区分，当前的响应是“进入匹配队列” 还是“移除匹配队列”。

    4. 匹配器Matcher的处理
    将用户加入匹配队列后，Matcher.java 中的三个匹配队列，就有用户/元素了。在 Matcher.java 里，线程会扫描匹配队列，调用 handlerMatch()，
    由于当前只有一个玩家，点击了开始匹配，此时队列中，也就只有一个元素。因此扫描线程，就会在wait()处，阻塞。


    5. 当前，又有一个玩家点击匹配操作。流程同 a), b) and c).
    此时，就可能从匹配队列的wait()中返回了。
    于是，执行匹配逻辑。

    6. 关键的操作在Matcher.java 的第4步，把这两个玩家，放到一个游戏房间中
    Room room = new Room();
    roomManager.add(room, player1.getUserId(), player2.getUserId())

    7. RoomManager.java 里面有两个 ConcurrentHashMap 对象。键值对的映射关系。
       
--------------------------------------------------------------------------------------------------------------------------------
#47
实现两个玩家，在游戏房间中的对战：下五子棋
约定前后端的接口。

1. 建立连接
ws://127.0.0.1:8080/game

2. 建立连接的响应

3. 针对“落子”的请求和响应
   [row][col]

--------------------------------------------------------------------------------------------------------------------------------
游戏房间的设计
game_room.html




--------------------------------------------------------------------------------------------------------------------------------






--------------------------------------------------------------------------------------------------------------------------------









--------------------------------------------------------------------------------------------------------------------------------