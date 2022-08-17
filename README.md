# 工程简介
Keywords：Java, Spring/Spring Boot/Spring MVC, HTML/CSS/JS/AJAX, MySQL/MyBatis, WebSocket


websocket frames(报文格式)
https://www.openmymind.net/WebSocket-Framing-Masking-Fragmentation-and-More/
https://www.youtube.com/watch?v=1VNyhBfF6e8&t=12s

Connection: Upgrade
Upgrade: Websocket
这两个header，其实就是告知服务器，要进行协议升级。响应的状态码时101（切换协议）

#37 录播：线程安全问题
使用到多线程代码的时候，要注意“线程安全”的问题。
Spring, websocket, & servlet
加锁：synchronized(锁对象)
只有多个线程 在尝试针对同一个锁对象进行加锁的时候，才有互斥效果
(多个线程访问的是不同队列，不涉及线程安全问题)

此处，在加锁的时候，选取的对象，就是normalQueue, highQueue, veryHighQueue
这三个队列对象的本身。

# 39 录播：
游戏房间管理器：一个游戏服务器上，有多个游戏房间，每个游戏房间，有两个游戏玩家

如何设置：roomManager 管理多个房间(room1, room2, room3...)
生成键值对(hashMap)，每个room生成唯一的roomId.

# 引入玩家id到房间的映射
通过playerId或者roomId找到room对象

# 46 总结：how to match players
点击开始匹配之后：
    1) 先触发js中的按钮的点击事件回调(matchButton.onclick = function()), 会给服务器发送websocket请求(websocket.send(JSON.stringify))。
    2) 然后发送一个websocket请求给服务器。然后服务器处理请求。
       Matcher这个class中，有个handleTextMessage(WebSocketSession session, TextMessage message) 方法。
       关键部分为：按照JSON格式，会对请求内容[payload,就是websocket.send(JSON.stringify({message: 'startMatch'}))发送的数据]，进行解析。
       解析成MatchRequest里面的message，拿到里面的内容，判断是"startMatch"还是"stopMatch".
       如果是startMatch，那么会调用matcher.add()操作[Matcher class里面的add()], 把用户加入到匹配的队列里面。
       并且把响应发回给客户端[String jsonString = objectMapper.writeValueAsString(response); session.sendMessage(new TextMessage(jsonString))]。
       告诉客户端，已经把用户加入到匹配队列了。
    3) 客户端收到服务器返回的响应之后，就会立即进行处理。客户端的[websocket.onmessage里面，有resp.message == 'startMatch'],会更新按钮里面的文本。
        2) 中的response和3)中的resp是对应的。MatchResponse这个class里面，有三个重要信息，ok，reason and message.
    4) 匹配器的处理。add以后，匹配队列里面，就会有玩家了。还有线程(t1,t2,t3)扫描匹配队列，进行handlerMatch的操作。
       如果队列里面的玩家少于2个，会产生阻塞(matchQueue.wait),等待.
    5) 当另外一个玩家，也点击匹配操作。流程同上。先通过onclick，发送websocket请求，服务器收到请求以后，把player加到匹配队列，加到相应的队列。
       可能会让handlerMatch，在wait()处，进行返回。于是就要继续执行匹配的逻辑。匹配器匹配到多个玩家，就会取出两个玩家。把两个玩家加入到一个房间(Room)，再把房间加入到
       房间管理器(RoomManager)里面. 过程为，先将2个player加入到userIdToRoomId的ConcurrentHashMap里面(userId -> roomId)，然后再建立roomId->room对象(映射关系)的ConcurrentHashMap里面。
       一共有3个键值对

# 对战模块
约定前后端交互的接口
 
建立连接的过程：websocket
ws://127.0.0.1:8080/game

建立连接后的响应

    


