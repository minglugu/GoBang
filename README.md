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



