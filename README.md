# 网页版五子棋对战

## 项目背景

实现一个网页版五子棋对战程序. 

支持以下核心功能:

* 用户模块: 用户注册, 用户登录, 用户天梯分数记录, 用户比赛场次记录. 
* 匹配模块: 按照用户的天梯分数实现匹配机制. 
* 对战模块: 实现两个玩家在网页端进行五子棋对战的功能. 

## 核心技术

* Spring/SpringBoot/SpringMVC
* WebSocket
* MySQL
* MyBatis
* HTML/CSS/JS/AJAX

## 前置知识介绍

### WebSocket

#### 背景介绍

WebSocket 是从 HTML5 开始支持的一种网页端和服务端保持长连接的 **消息推送机制**. 

> 理解消息推送: 
> 
> 传统的 web 程序, 都是属于 "一问一答" 的形式. 客户端给服务器发送了一个 HTTP 请求, 服务器给客户端返回一个 HTTP 响应. 
> 
> 这种情况下, 服务器是属于被动的一方. 如果客户端不主动发起请求, 服务器就无法主动给客户端响应. 

像五子棋这样的程序, 或者聊天这样的程序, 都是非常依赖 "消息推送" 的. 如果只是使用原生的 HTTP 协议, 要想实现消息推送一般需要通过 "轮询" 的方式. 

> 轮询的成本比较高, 而且也不能及时的获取到消息的响应. 

而 WebSocket 则是更接近于 TCP 这种级别的通信方式. 一旦连接建立完成, 客户端或者服务器都可以主动的向对方发送数据. 

#### 原理解析

**握手过程**

WebSocket 协议本质上是一个基于 TCP 的协议。为了建立一个 WebSocket 连接，客户端浏览器首先要向服务器发起一个 HTTP 请求，这个请求和通常的 HTTP 请求不同，包含了一些附加头信息，通过这个附加头信息完成握手过程. 

![](readme-image/webp.webp)

**报文格式**

![image-20220426114725995](readme-image/image-20220426114725995.png)

* FIN: 为 1 表示要断开 websocket 连接. 
* RSV1/RSV2/RSV3: 保留位, 一般为 0. 
* opcode: 操作代码. 决定了如何理解后面的数据载荷. 
  * 0x0: 表示这是个延续帧. 当 opcode 为 0, 表示本次数据传输采用了数据分片, 当前收到的帧为其中一个分片. 
  * 0x1: 表示这是文本帧. 
  * 0x2: 表示这是二进制帧. 
  * 0x3-0x7: 保留, 暂未使用. 
  * 0x8: 表示连接断开. 
  * 0x9: 表示 ping 帧. 
  * 0xa: 表示 pong 帧.
  * 0xb-0xf: 保留, 暂未使用. 
* mask: 表示是否要对数据载荷进行掩码操作。从客户端向服务端发送数据时，需要对数据进行掩码操作；从服务端向客户端发送数据时，不需要对数据进行掩码操作。
* Payload length：数据载荷的长度，单位是字节。为7位，或7+16位，或1+64位。

> 假设数Payload length === x，如果
> 
> - x为0~126：数据的长度为x字节。
> - x为126：后续2个字节代表一个16位的无符号整数，该无符号整数的值为数据的长度。
> - x为127：后续8个字节代表一个64位的无符号整数（最高位为0），该无符号整数的值为数据的长度。

* Masking-key：0或4字节（32位）所有从客户端传送到服务端的数据帧，数据载荷都进行了掩码操作，Mask为1，且携带了4字节的Masking-key。如果Mask为0，则没有Masking-key

> 为啥要使用掩码算法? 
> 
> 主要是从安全角度考虑, 避免一些缓冲区溢出攻击. 

* payload data: 报文携带的载荷数据. 

#### 代码示例

Spring 内置了 websocket . 可以直接进行使用. 

**服务器代码**

创建 `api.TestAPI` 类. 

> 这个类用来处理 websocket 请求, 并返回响应. 
> 
> 每个方法中都带有一个 session 对象, 这个 session 和 Servlet 的 session 并不相同, 而是 WebSocket 内部搞的另外一组 Session. 
> 
> 通过这个 Session 可以给客户端返回数据, 或者主动断开连接. 

```java
@Component
public class TestAPI extends TextWebSocketHandler {
    public TestAPI() {
        System.out.println("TestAPI load!");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("onOpen!");
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.out.println("onError!");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("onClose!");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("onMessage: " + message.toString());
        session.sendMessage(message);
    }
}
```

创建 `config.WebSocketConfig` 类

> 这个类用于配置 请求路径和 TextWebSocketHandler 之间的对应关系. 

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private TestAPI testAPI;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(testAPI, "/test");
}
```

**客户端代码**

创建 test.html

```html
<input type="text" id="message">
<button id="sendButton">发送</button>

<script>
    let websocket = new WebSocket("ws://127.0.0.1:8080/test");

    websocket.onopen = function() {
        console.log("open!");
    }

    websocket.onclose = function() {
        console.log("close!");
    }

    websocket.onerror = function() {
        console.log("error!");
    }

    websocket.onmessage = function(e) {
        console.log("recv: " + e.data);
    }

    let messageInput = document.querySelector("#message");
    let sendButton = document.querySelector("#sendButton");
    sendButton.onclick = function() {
        console.log("send: " + messageInput.value);
        websocket.send(messageInput.value);
    }
</script>
```

启动服务器, 通过浏览器访问页面, 观察效果. 

#### 参考资料

https://geek-docs.com/spring/spring-tutorials/websocket.html

https://www.sohu.com/a/227600866_472869

## 需求分析和概要设计

整个项目分成以下模块

* 用户模块
* 匹配模块
* 对战模块

### 用户模块

用户模块主要负责用户的注册, 登录, 分数记录功能. 

使用 MySQL 数据库存储数据. 

客户端提供一个登录页面+注册页面.

服务器端基于 Spring + MyBatis 来实现数据库的增删改查.  

### 匹配模块

用户登录成功, 则进入游戏大厅页面. 

游戏大厅中, 能够显示用户的名字, 天梯分数, 比赛场数和获胜场数. 

同时显示一个 "匹配按钮". 

点击匹配按钮则用户进入匹配队列, 并且界面上显示为 "取消匹配" . 

再次点击则把用户从匹配队列中删除. 

如果匹配成功, 则跳转进入到游戏房间页面. 

页面加载时和服务器建立 websocket 连接. 双方通过 websocket 来传输 "开始匹配", "取消匹配", "匹配成功" 这样的信息. 

### 对战模块

玩家匹配成功, 则进入游戏房间页面. 

每两个玩家在同一个游戏房间中. 

在游戏房间页面中, 能够显示五子棋棋盘. 玩家点击棋盘上的位置实现落子功能. 

并且五子连珠则触发胜负判定, 显示 "你赢了" "你输了". 

页面加载时和服务器建立 websocket 连接. 双方通过 websocket 来传输 "准备就绪", "落子位置", "胜负" 这样的信息. 

* 准备就绪: 两个玩家均连上游戏房间的 websocket 时, 则认为双方准备就绪. 
* 落子位置: 有一方玩家落子时, 会通过 websocket 给服务器发送落子的用户信息和落子位置, 同时服务器再将这样的信息返回给房间内的双方客户端. 然后客户端根据服务器的响应来绘制棋子位置. 
* 胜负: 服务器判定这一局游戏的胜负关系. 如果某一方玩家落子, 产生了五子连珠, 则判定胜负并返回胜负信息. 或者如果某一方玩家掉线(比如关闭页面), 也会判定对方获胜. 

## 项目创建

使用 IDEA 创建 SpringBoot 项目. 具体过程不再详细展开. 

引入依赖如下:

> 依赖都是常规的 SpringBoot / Spring MVC / MyBatis 等, 没啥特别的依赖. 

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.6.4</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.example</groupId>
    <artifactId>demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>gobang</name>
    <description>联机对战五子棋</description>
    <properties>
        <java.version>1.8</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>2.2.2</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

## 实现用户模块

### 编写数据库代码

#### 数据库设计

创建 user 表, 表示用户信息和分数信息. 

```sql
create database if not exists java_gobang;

use java_gobang;

drop table if exists user;
create table user(
    userId int primary key auto_increment,
    username varchar(50) unique,
    password varchar(50),
    score int, -- 天梯分数
    totalCount int, -- 比赛总场次
    winCount int -- 获胜场次
);

insert into user values(null, '张三', '123', 1000, 0, 0);
insert into user values(null, '李四', '123', 1000, 0, 0);
insert into user values(null, '王五', '123', 1000, 0, 0);
insert into user values(null, '赵六', '123', 1000, 0, 0);
insert into user values(null, '田七', '123', 1000, 0, 0);
insert into user values(null, '朱八', '123', 1000, 0, 0);
```

#### 配置 MyBatis

编辑 application.yml

```yml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/java_gobang?characterEncoding=utf8&useSSL=false
    username: root
    password: 2222
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/**Mapper.xml
```

#### 创建实体类

创建 `model.User` 类

```java
public class User {
    private int userId;
    private String username;
    private String password;
    private int score;
    private int totalCount;
    private int winCount;
}
```

#### 创建 UserMapper

创建 `model.UserMapper` 接口. 

此处主要提供四个方法:

* selectByName: 根据用户名查找用户信息. 用于实现登录.
* insert: 新增用户. 用户实现注册. 
* userWin: 用于给获胜玩家修改分数.
* userLose: 用户给失败玩家修改分数. 

```java
@Mapper
public interface UserMapper {
    User selectByName(String username);

    int insert(User user);
    void userWin(User user);
    void userLose(User user);
}
```

#### 实现 UserMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.demo.model.UserMapper">
    <select id="selectByName" resultType="com.example.demo.model.User">
        select * from user where username = #{username}
    </select>

    <select id="selectById" resultType="com.example.demo.model.User">
        select * from user where userId = #{userId}
    </select>

    <insert id="insert">
        insert into user values(null, #{username}, #{password}, 1000, 0, 0)
    </insert>

    <update id="userWin">
        update user set score = score + 25, totalCount = totalCount + 1, winCount = winCount + 1 where userId = #{userId}
    </update>
    <update id="userLose">
        update user set score = score - 25, totalCount = totalCount + 1 where userId = #{userId}
    </update>
</mapper>
```

### 前后端交互接口

需要明确用户模块的前后端交互接口. 这里主要涉及到三个部分. 

#### 登录接口

请求:

```json
POST /login HTTP/1.1
Content-Type: application/x-www-form-urlencoded

username=zhangsan&password=123
```

响应:

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
    userId: 1,
    username: 'zhangsan',
    score: 1000,
    totalCount: 10,
    winCount: 5
}    
```

> 如果登录失败, 返回的是一个 userId 为 0 的对象. 

#### 注册接口

请求:

```json
POST /register HTTP/1.1
Content-Type: application/x-www-form-urlencoded

username=zhangsan&password=123
```

响应:

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
    userId: 1,
    username: 'zhangsan',
    score: 1000,
    totalCount: 10,
    winCount: 5
}    
```

> 如果注册失败(比如用户名重复), 返回的是一个 userId 为 0 的对象. 

#### 获取用户信息

请求:

```json
GET /userInfo HTTP/1.1
```

响应:

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
    userId: 1,
    username: 'zhangsan',
    score: 1000,
    totalCount: 10,
    winCount: 5
}    
```

### 服务器开发

创建 `api.UserAPI` 类

主要实现三个方法:

* login: 用来实现登录逻辑. 
* register: 用来实现注册逻辑. 
* getUserInfo: 用来实现登录成功后显示用户分数的信息. 

```java
@RestController
public class UserAPI {
    @Resource
    private UserMapper userMapper;

    @PostMapping("/login")
    @ResponseBody
    public Object login(String username, String password, HttpServletRequest req) {
        User user = userMapper.selectByName(username);
        System.out.println("login! user=" + user);
        if (user == null || !user.getPassword().equals(password)) {
            return new User();
        }
        HttpSession session = req.getSession(true);
        session.setAttribute("user", user);
        return user;
    }

    @PostMapping("/register")
    @ResponseBody
    public Object register(String username, String password) {
        User user = null;
        try {
            user = new User();
            user.setUsername(username);
            user.setPassword(password);
            System.out.println("register! user=" + user);
            int ret = userMapper.insert(user);
            System.out.println("ret: " + ret);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            user = new User();
        }
        return user;
    }

    @GetMapping("/userInfo")
    @ResponseBody
    public Object getUserInfo(HttpServletRequest req) {
        // 从 session 中拿到用户信息
        HttpSession session = req.getSession(false);
        if (session == null) {
            return new User();
        }
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return new User();
        }
        return user;
    }
}
```

### 客户端开发

#### 登录页面

创建 login.html

```html
<div class="nav">
    联机五子棋
</div>
<div class="login-container">
    <div class="login-dialog">
        <!-- 标题 -->
        <h3>登录</h3>
        <!-- 输入用户名 -->
        <div class="row">
            <span>用户名</span>
            <input type="text" id="username" name="username">
        </div>
        <!-- 输入密码 -->
        <div class="row">
            <span>密码</span>
            <input type="password" id="password" name="password">
        </div>
        <!-- 提交按钮 -->
        <div class="row submit-row">
            <button id="submit">提交</button> 
        </div>
    </div>  
</div>
```

创建 css/common.css

```css
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

html, body {
    height: 100%;
    background-image: url(../image/cat.jpg);
    background-repeat: no-repeat;
    background-position: center;
    background-size: cover;
}

.nav {
    width: 100%;
    height: 50px;
    background-color: rgb(51, 51, 51);
    color: white;

    display: flex;
    align-items: center;
    padding-left: 20px;
}

.container {
    height: calc(100% - 50px);
    width: 100%;

    display: flex;
    justify-content: center;
    align-items: center;
    background-color: rgba(255, 255, 255, 0.7);
}
```

创建 css/login.css

```css
.login-container {
    width: 100%;
    height: calc(100% - 50px);
    display: flex;
    justify-content: center;
    align-items: center;
}

.login-dialog {
    width: 400px;
    height: 320px;
    background-color: rgba(255, 255, 255, 0.8);
    border-radius: 10px;
}

.login-dialog h3 {
    text-align: center;
    padding: 50px 0;
}

.login-dialog .row {
    width: 100%;
    height: 50px;
    display: flex;
    justify-content: center;
    align-items: center;
}

.login-dialog .row span {
    display: block;
    /* 设置固定宽度, 能让文字和后面的输入框之间有间隙 */
    width: 100px;
    font-weight: 700;
}

.login-dialog #username,
.login-dialog #password {
    width: 200px;
    height: 40px;
    font-size: 20px;
    text-indent: 10px;
    border-radius: 10px;
    border: none;
    outline: none;
}

.login-dialog .submit-row {
    margin-top: 10px;
}

.login-dialog #submit {
    width: 300px;
    height: 50px;
    color: white;
    background-color: rgb(0, 128, 0);
    border: none;
    border-radius: 10px;
    font-size: 20px;
}

.login-dialog #submit:active {
    background-color: #666;
}
```

在 login.html 中编写 js 代码

* 通过 jQuery 中的 AJAX 和服务器进行交互. 

```html
<script src="http://lib.sinaapp.com/js/jquery/1.9.1/jquery-1.9.1.min.js"></script>
<script>
    // 通过 ajax 的方式实现登录过程
    let submitButton = document.querySelector('#submit');
    submitButton.onclick = function() {
        // 1. 先获取到用户名和密码
        let username = document.querySelector('#username').value;
        let password = document.querySelector('#password').value;

        $.ajax({
            method: 'post',
            url: '/login',
            data: {
                username: username,
                password: password
            },
            success: function(data) {
                console.log(JSON.stringify(data));
                if (data && data.userId > 0) {
                    // 登录成功, 跳转到游戏大厅
                    alert("登录成功!")
                    location.assign('/game_hall.html');
                } else {
                    alert("登录失败! 用户名密码错误! 或者该账号正在游戏中!");
                }
            }
        });
    }
</script>
```

编写完成后, 验证登录功能. 

#### 注册页面

创建 register.html

```html
<div class="nav">
    联机五子棋
</div>
<div class="login-container">
    <div class="login-dialog">
        <!-- 标题 -->
        <h3>注册</h3>
        <!-- 输入用户名 -->
        <div class="row">
            <span>用户名</span>
            <input type="text" id="username" name="username">
        </div>
        <!-- 输入密码 -->
        <div class="row">
            <span>密码</span>
            <input type="password" id="password" name="password">
        </div>
        <!-- 提交按钮 -->
        <div class="row submit-row">
            <button id="submit">提交</button> 
        </div>
    </div>  
</div>
```

css 部分可以直接复用 common.css 和 login.css

在 register.html 中, 编写 js 代码

```html
<script src="http://lib.sinaapp.com/js/jquery/1.9.1/jquery-1.9.1.min.js"></script>
<script>
    // 通过 ajax 的方式实现登录过程
    let submitButton = document.querySelector('#submit');
    submitButton.onclick = function() {
        // 1. 先获取到用户名和密码
        let username = document.querySelector('#username').value;
        let password = document.querySelector('#password').value;

        $.ajax({
            method: 'post',
            url: '/register',
            data: {
                username: username,
                password: password
            },
            success: function(data) {
                console.log(JSON.stringify(data));
                if (data && data.username) {
                    // 注册成功, 跳转到游戏大厅
                    alert('注册成功!')
                    location.assign('/login.html');
                } else {
                    alert("注册失败!");
                }
            }
        });
    }
</script>
```

代码编写完毕后, 运行程序, 验证注册效果. 

> 获取用户信息的接口, 后续再游戏大厅页面中再使用. 此处暂时不涉及. 

## 实现匹配模块

### 前后端交互接口

连接:

```
ws://127.0.0.1:8080/findMatch
```

请求: 

```json
{
    message: 'startMatch' / 'stopMatch',
}
```

响应1: (收到请求后立即响应)

```json
{
    ok: true,                // 是否成功. 比如用户 id 不存在, 则返回 false
    reason: '',                // 错误原因
    message: 'startMatch' / 'stopMatch'
}
```

响应2: (匹配成功后的响应)

```json
{
    ok: true,                // 是否成功. 比如用户 id 不存在, 则返回 false
    reason: '',                // 错误原因
    message: 'matchSuccess',    
}
```

**备注:**

* 页面这端拿到匹配响应之后, 就跳转到游戏房间.
* 如果返回的响应 ok 为 false, 则弹框的方式显示错误原因, 并跳转到登录页面. 

### 客户端开发

#### 实现页面基本结构

创建 game_hall.html, 主要包含

* #screen 用于显示玩家的分数信息
* button#match-button 作为匹配按钮. 

```html
<div class="nav">
    联机五子棋
</div>
<div class="container">
    <div>
        <div id="screen"></div>
        <button id="match-button">开始匹配</button>
    </div>
</div>
```

创建 game_hall.css

```css
#match-button {
    width: 400px;
    height: 200px;
    font-size: 40px;
    line-height: 200px;
    color:white;
    background-color: orange;
    border: none;
    outline: none;
    border-radius: 20px;
}

#match-button:active {
    background-color: gray;
}

#screen {
    width: 400px;
    height: 200px;
    font-size: 20px;
    background-color: gray;
    text-align: center;
    line-height: 100px;
    color: white;
    border-radius: 20px;
    margin-bottom: 5px;
}
```

编写 JS 代码, 获取到用户信息.

```html
<script src="http://libs.baidu.com/jquery/2.0.0/jquery.min.js"></script>
<script>
    $.ajax({
        method: 'get',
        url: '/userInfo',
        success: function(data) {
            let screen = document.querySelector('#screen');
            screen.innerHTML = '玩家: ' + data.username + ', 分数: ' + data.score + "<br> 比赛场次: " + data.totalCount + ", 获胜场次: " + data.winCount;
        }
    });
</script>
```

#### 实现匹配功能

编辑 game_hall.html 的 js 部分代码. 

* 点击匹配按钮, 就会进入匹配逻辑. 同时按钮上提示 "匹配中...(点击取消)" 字样.
* 再次点击匹配按钮, 则会取消匹配. 
* 当匹配成功后, 服务器会返回匹配成功响应, 页面跳转到 game_room.html

```js
// 1. 和服务器建立连接. 路径要写作 /findMatch, 不要写作 /findMatch/
let websocket = new WebSocket('ws://127.0.0.1:8080/findMatch');
// 2. 点击开始匹配
let button = document.querySelector('#match-button');
button.onclick = function() {
    if (websocket.readyState == websocket.OPEN) {
        if (button.innerHTML == '开始匹配') {
            console.log('开始匹配!');
            websocket.send(JSON.stringify({
                message: 'startMatch',
            }));
        } else if (button.innerHTML == '匹配中...(点击取消)') {
            console.log('取消匹配!');
            websocket.send(JSON.stringify({
                message: 'stopMatch'
            }));
        }
    } else {
        alert('当前您连接断开! 请重新登录!');
        location.assign('/login.html');
    }
}

// 3. 处理服务器的响应
websocket.onmessage = function(e) {
    let resp = JSON.parse(e.data)
    if (!resp.ok) {
        console.log('游戏大厅中发生错误: ' + resp.reason);
        location.assign('/login.html');
        return;
    }
    if (resp.message == 'startMatch') {
        console.log('进入匹配队列成功!');
        button.innerHTML = '匹配中...(点击取消)';
    } else if (resp.message == 'stopMatch') {
        console.log('离开匹配队列成功!');
        button.innerHTML = '开始匹配';
    } else if (resp.message == 'matchSuccess') {
        console.log('匹配成功! 进入游戏页面!');
        location.assign('/game_room.html');
    } else {
        console.log('非法的 message: ' + resp.message);
    }
}

// 4. 监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
window.onbeforeunload = function () {
    websocket.close();
}
```

### 服务器开发

#### 创建并注册 MatchAPI 类

创建 `api.MatchAPI`, 继承自 `TextWebSocketHandler` 作为处理 websocket 请求的入口类. 

* 准备好一个 ObjectMapper, 后续用来处理 JSON 数据. 

```java
@Component
public class MatchAPI extends TextWebSocketHandler {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Component
    public class MatchAPI extends TextWebSocketHandler {
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    }
}
```

修改 `config.WebSocketConfig`, 把 MatchAPI 注册进去. 

* 在 `addHandler` 之后, 再加上一个 `.addInterceptors(new HttpSessionHandshakeInterceptor())` 代码, 这样可以把之前登录过程中往 HttpSession 中存放的数据(主要是 User 对象), 放到 WebSocket 的 session 中. 方便后面的代码中获取到当前用户信息. 

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Autowired
    private TestAPI testAPI;
    @Autowired
    private MatchAPI matchAPI;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(testAPI, "/test");
        // 通过 .addInterceptors(new HttpSessionHandshakeInterceptor() 这个操作来把 HttpSession 里的属性放到 WebSocket 的 session 中
        // 参考: https://docs.spring.io/spring-framework/docs/5.0.7.RELEASE/spring-framework-reference/web.html#websocket-server-handshake
        // 然后就可以在 WebSocket 代码中 WebSocketSession 里拿到 HttpSession 中的 attribute.
        registry.addHandler(matchAPI, "/findMatch")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
```

#### 实现用户管理器

创建 `game.OnlineUserManager` 类, 用于管理当前用户的在线状态. 本质上是 哈希表 的结构. key 为用户 id, value 为用户的 WebSocketSession. 

借助这个类, 一方面可以判定用户是否是在线, 同时也可以进行方便的获取到 Session 从而给客户端回话. 

* 当玩家建立好 websocket 连接, 则将键值对加入 OnlineUserManager 中. 
* 当玩家断开 websocket 连接, 则将键值对从 OnlineUserManager 中删除. 
* 在玩家连接好的过程中, 随时可以通过 userId 来查询到对应的会话, 以便向客户端返回数据. 

> 由于存在两个页面, 游戏大厅和游戏房间, 使用两个 哈希表 来分别存储两部分的会话. 

```java
@Component
public class OnlineUserManager {
    private ConcurrentHashMap<Integer, WebSocketSession> gameHall = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, WebSocketSession> gameRoom = new ConcurrentHashMap<>();

    public void enterGameHall(int userId, WebSocketSession session) {
        gameHall.put(userId, session);
    }

    // 只有当前页面退出的时候, 能销毁自己的 session
    // 避免当一个 userId 打开两次 游戏页面, 错误的删掉之前的会话的问题.
    public void exitGameHall(int userId) {
        gameHall.remove(userId);
    }

    public WebSocketSession getSessionFromGameHall(int userId) {
        return gameHall.get(userId);
    }

    public void enterGameRoom(int userId, WebSocketSession session) {
        gameRoom.put(userId, session);
    }

    public void exitGameRoom(int userId) {
        gameRoom.remove(userId);
    }

    public WebSocketSession getSessionFromGameRoom(int userId) {
        return gameRoom.get(userId);
    }
}
```

给 MatchAPI 注入 OnlineUserManager

```java
@Component
public class MatchAPI extends TextWebSocketHandler {
    @Autowired
    private OnlineUserManager onlineUserManager;
}
```

#### 创建匹配请求/响应对象

创建 `game.MatchRequest` 类

```java
public class MatchRequest {
    private String message = "";
}
```

创建 `game.MatchResponse` 类

```java
public class MatchResponse {
    private boolean ok = true;
    private String reason = "";
    private String message = "";
}
```

#### 处理连接成功

实现 `afterConnectionEstablished` 方法. 

* 通过参数中的 session 对象, 拿到之前登录时设置的 User 信息. 
* 使用 onlineUserManager 来管理用户的在线状态. 
* 先判定用户是否是已经在线, 如果在线则直接返回出错 (禁止同一个账号多开). 
* 设置玩家的上线状态. 

```java
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    // 1. 拿到用户信息.
    User user = (User) session.getAttributes().get("user");
    if (user == null) {
        // 拿不到用户的登录信息, 说明玩家未登录就进入游戏大厅了.
        // 则返回错误信息并关闭连接
        MatchResponse response = new MatchResponse();
        response.setOk(false);
        response.setReason("玩家尚未登录!");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        return;
    }
    // 2. 检查玩家的上线状态
    if (onlineUserManager.getSessionFromGameHall(user.getUserId()) != null
        || onlineUserManager.getSessionFromGameRoom(user.getUserId()) != null) {
        MatchResponse response = new MatchResponse();
        response.setOk(false);
        response.setReason("禁止多开游戏大厅页面!");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        return;
    }
    // 3. 设置玩家上线状态
    onlineUserManager.enterGameHall(user.getUserId(), session);
    System.out.println("玩家进入匹配页面: " + user.getUserId());
}
```

#### 处理开始匹配/取消匹配请求

实现 handleTextMessage

* 先从会话中拿到当前玩家的信息. 
* 解析客户端发来的请求
* 判定请求的类型, 如果是 startMatch, 则把用户对象加入到匹配队列. 如果是 stopMatch, 则把用户对象从匹配队列中删除. 
* 此处需要实现一个 匹配器 对象, 来处理匹配的实际逻辑. 

```java
@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    // 1. 拿到用户信息.
    User user = (User) session.getAttributes().get("user");
    if (user == null) {
        System.out.println("[onMessage] 玩家尚未登录!");
        return;
    }
    System.out.println("开始匹配: " + user.getUserId() + " message: " + message.toString());
    // 2. 解析读到的数据为 json 对象
    MatchRequest request = objectMapper.readValue(message.getPayload(), MatchRequest.class);
    MatchResponse response = new MatchResponse();
    if (request.getMessage().equals("startMatch")) {
        matcher.add(user);
        response.setMessage("startMatch");
    } else if (request.getMessage().equals("stopMatch")) {
        matcher.remove(user);
        response.setMessage("stopMatch");
    } else {
        // 匹配失败
        response.setOk(false);
        response.setReason("非法的匹配请求!");
    }
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
}
```

#### 实现匹配器(1)

创建 `game.Matcher` 类. 

* 在 Matcher 中创建三个队列 (队列中存储 User 对象), 分别表示不同的段位的玩家. (此处约定 <2000 一档, 2000-3000 一档, >3000 一档)
* 提供 add 方法, 供 MatchAPI 类来调用, 用来把玩家加入匹配队列. 
* 提供 remove 方法, 供 MatchAPI 类来调用, 用来把玩家移出匹配队列. 
* 同时 Matcher 找那个要记录 OnlineUserManager, 来获取到玩家的 Session. 

```java
@Component
public class Matcher {
    private ObjectMapper objectMapper = new ObjectMapper();
    // 三个匹配队列
    private Queue<User> normalQueue = new LinkedList<>();
    private Queue<User> highQueue = new LinkedList<>();
    private Queue<User> veryHighQueue = new LinkedList<>();
    // 玩家在线状态
    @Autowired
    private OnlineUserManager onlineUserManager;

    public void add(User user) throws InterruptedException {
        if (user.getScore() < 2000) {
            normalQueue.offer(user);
            System.out.println("[Matcher] " + user.getUserId() + " 进入 normalQueue!");
        } else if (user.getScore() < 3000) {
            highQueue.offer(user);
            System.out.println("[Matcher] " + user.getUserId() + " 进入 highQueue!");
        } else {
            veryHighQueue.offer(user);
            System.out.println("[Matcher] " + user.getUserId() + " 进入 veryHighQueue!");
        }
    }

    public void remove(User user) {
        if (user.getScore() < 2000) {
            removeFromQueue(normalQueue, user);
            System.out.println("[Matcher] " + user.getUserId() + " 移出 normalQueue!");
        } else if (user.getScore() < 3000) {
            removeFromQueue(highQueue, user);
            System.out.println("[Matcher] " + user.getUserId() + " 移出 highQueue!");
        } else {
            removeFromQueue(veryHighQueue, user);
            System.out.println("[Matcher] " + user.getUserId() + " 移出 veryHighQueue!");
        }
    }

    private void removeFromQueue(Queue<User> queue, User user) {
        queue.remove(user);
    }
}
```

#### 实现匹配器(2)

修改 `game.Matcher` , 实现匹配逻辑. 

在 Matcher 的构造方法中, 创建一个线程, 使用该线程扫描每个队列, 把每个队列的头两个元素取出来, 匹配到一组中. 

```java
private Matcher() {
    // 搞三个线程, 各自匹配各自的~
    new Thread() {
        @Override
        public void run() {
            while (true) {
                handlerMatch(normalQueue);
            }
        }
    }.start();

    new Thread() {
        @Override
        public void run() {
            while (true) {
                handlerMatch(highQueue);
            }
        }
    }.start();

    new Thread() {
        @Override
        public void run() {
            while (true) {
                handlerMatch(veryHighQueue);
            }
        }
    }.start();
}
```

实现 `handlerMatch`

* 由于 `handlerMatch` 在单独的线程中调用. 因此要考虑到访问队列的线程安全问题. 需要加上锁. 
* 每个队列分别使用队列对象本身作为锁即可. 
* 在入口处使用 wait 来等待, 直到队列中达到 2 个元素及其以上, 才唤醒线程消费队列. 

```java
private void handlerMatch(Queue<User> matchQueue) {
    synchronized (matchQueue) {
        try {
            // 保证只有一个玩家在队列的时候, 不会被出队列. 从而能支持取消功能.
            while (matchQueue.size() < 2) {
                matchQueue.wait();
            }
            // 1. 尝试获取两个元素
            User player1 = matchQueue.poll();
            User player2 = matchQueue.poll();
            System.out.println("匹配出两个玩家: " + player1.getUserId() + ", " + player2.getUserId());
            // 2. 检查玩家在线状态(可能在匹配中玩家突然关闭页面)
            WebSocketSession session1 = onlineUserManager.getSessionFromGameHall(player1.getUserId());
            WebSocketSession session2 = onlineUserManager.getSessionFromGameHall(player2.getUserId());
            if (session1 == null) {
                // 如果玩家1 下线, 则把玩家2 放回匹配队列
                matchQueue.offer(player2);
                return;
            }
            if (session2 == null) {
                // 如果玩家2 下线, 则把玩家1 放回匹配队列
                matchQueue.offer(player1);
                return;
            }
            if (session1 == session2) {
                // 如果得到的两个 session 相同, 说明是同一个玩家两次进入匹配队列
                // 例如玩家点击开始匹配后, 刷新页面, 重新再点开始匹配
                // 此时也把玩家放回匹配队列
                matchQueue.offer(player1);
                return;
            }

            // 3. 将这两个玩家加入到游戏房间中.
              // TODO 一会再写

            // 4. 给玩家1 发回响应数据
            MatchResponse response1 = new MatchResponse();
            response1.setMessage("matchSuccess");
            session1.sendMessage(new TextMessage(objectMapper.writeValueAsString(response1)));
            // 5. 给玩家2 发回响应数据
            MatchResponse response2 = new MatchResponse();
            response2.setMessage("matchSuccess");
            session2.sendMessage(new TextMessage(objectMapper.writeValueAsString(response2)));
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
```

需要给上面的插入队列元素, 删除队列元素也加上锁. 

* 插入成功后要通知唤醒上面的等待逻辑. 

```java
public void add(User user) throws InterruptedException {
    if (user.getScore() < 2000) {
        synchronized (normalQueue) {
            normalQueue.offer(user);
            normalQueue.notify();
        }
        System.out.println("[Matcher] " + user.getUserId() + " 进入 normalQueue!");
    } else if (user.getScore() < 3000) {
        synchronized (highQueue) {
            highQueue.offer(user);
            highQueue.notify();
        }
        System.out.println("[Matcher] " + user.getUserId() + " 进入 highQueue!");
    } else {
        synchronized (veryHighQueue) {
            veryHighQueue.offer(user);
            veryHighQueue.notify();
        }
        System.out.println("[Matcher] " + user.getUserId() + " 进入 veryHighQueue!");
    }
}

// ......

private void removeFromQueue(Queue<User> queue, User user) {
    synchronized (queue) {
        queue.remove(user);
    }
}
```

#### 创建房间类

匹配成功之后, 需要把对战的两个玩家放到同一个房间对象中. 

创建 `game.Room` 类

* 一个房间要包含一个房间 ID, 使用 UUID 作为房间的唯一身份标识. 
* 房间内要记录对弈的玩家双方信息. 
* 记录先手方的 ID
* 记录一个 二维数组 , 作为对弈的棋盘. 
* 记录一个 OnlineUserManager, 以备后面和客户端进行交互. 
* 当然, 少不了 ObjectMapper 来处理 json

```java
public class Room {
    private String roomId;
    // 玩家1
    private User user1;
    // 玩家2
    private User user2;
    // 先手方的用户 id
    private int whiteUserId = 0;
    // 棋盘, 数字 0 表示未落子位置. 数字 1 表示玩家 1 的落子. 数字 2 表示玩家 2 的落子
    private static final int MAX_ROW = 15;
    private static final int MAX_COL = 15;
    private int[][] chessBoard = new int[MAX_ROW][MAX_COL];

    private ObjectMapper objectMapper = new ObjectMapper();

    private OnlineUserManager onlineUserManager;

    public Room() {
        // 使用 uuid 作为唯一身份标识
        roomId = UUID.randomUUID().toString();
    }

    // getter / setter 方法略
}
```

#### 实现房间管理器

Room 对象会存在很多. 每两个对弈的玩家, 都对应一个 Room 对象. 

需要一个管理器对象来管理所有的 Room. 

创建 `game.RoomManager`

* 使用一个 Hash 表, 保存所有的房间对象, key 为 roomId, value 为 Room 对象
* 再使用一个 Hash 表, 保存 userId -> roomId 的映射, 方便根据玩家来查找所在的房间. 
* 提供增, 删, 查的 API. (查包含两个版本, 基于房间 ID 的查询和基于用户 ID 的查询).

```java
@Component
public class RoomManager {
    // key 为 roomId, value 为一个 Room 对象
    private ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, String> userIdToRoomId = new ConcurrentHashMap<>();

    public void addRoom(Room room, int userId1, int userId2) {
        rooms.put(room.getRoomId(), room);
        userIdToRoomId.put(userId1, room.getRoomId());
        userIdToRoomId.put(userId2, room.getRoomId());
    }

    public Room getRoomByRoomId(String roomId) {
        return rooms.get(roomId);
    }

    public Room getRoomByUserId(int userId) {
        String roomId = userIdToRoomId.get(userId);
        if (roomId == null) {
            return null;
        }
        return getRoomByRoomId(roomId);
    }

    public void removeRoom(String roomId, int userId1, int userId2) {
        rooms.remove(roomId);
        userIdToRoomId.remove(userId1);
        userIdToRoomId.remove(userId2);
    }
}
```

#### 实现匹配器(3)

完善刚才匹配逻辑中的 TODO. 创建房间, 并把玩家放到这个房间中. 

先给 Matcher 找那个注入 RoomManager 对象

```java
@Component
public class Matcher {
    // ......

    // 房间管理器
    @Autowired
    private RoomManager roomManager;

    // ......
}
```

然后修改 Matcher.handlerMatch, 补完之前 TODO 的内容. 

```java
private void handlerMatch(Queue<User> matchQueue) {
    // ......

    // 3. 将这两个玩家加入到游戏房间中.
    Room room = new Room();
    roomManager.addRoom(room, player1.getUserId(), player2.getUserId());

    // ......
}
```

#### 处理连接关闭

实现 afterConnectionClosed

* 主要的工作就是把玩家从 onlineUserManager 中退出. 
* 退出的时候要注意判定, 当前玩家是否是多开的情况(一个userId, 对应到两个 websocket 连接). 如果一个玩家开启了第二个 websocket 连接, 那么这第二个 websocket 连接不会影响到玩家从 OnlineUserManager 中退出. 
* 如果玩家当前在匹配队列中, 则直接从匹配队列里移除. 

```java
@Override
public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    User user = (User) session.getAttributes().get("user");
    if (user == null) {
        System.out.println("[onClose] 玩家尚未登录!");
        return;
    }
    WebSocketSession existSession = onlineUserManager.getSessionFromGameHall(user.getUserId());
    if (existSession != session) {
        System.out.println("当前的会话不是玩家游戏中的会话, 不做任何处理!");
        return;
    }
    System.out.println("玩家离开匹配页面: " + user.getUserId());
    onlineUserManager.exitGameHall(user.getUserId());
    // 如果玩家在匹配中, 则关闭页面时把玩家移出匹配队列
    matcher.remove(user);
}
```

#### 处理连接异常

实现 handleTransportError. 逻辑同上. 

```java
@Override
public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    User user = (User) session.getAttributes().get("user");
    if (user == null) {
        System.out.println("[onError] 玩家尚未登录!");
        return;
    }
    WebSocketSession existSession = onlineUserManager.getSessionFromGameHall(user.getUserId());
    if (existSession != session) {
        System.out.println("当前的会话不是玩家游戏中的会话, 不做任何处理!");
        return;
    }
    System.out.println("匹配页面连接出现异常! userId: " + user.getUserId() + ", message: " + exception.getMessage());
    onlineUserManager.exitGameHall(user.getUserId());
    // 如果玩家在匹配中, 则关闭页面时把玩家移出匹配队列
    matcher.remove(user);
}
```

### 验证匹配功能

运行程序, 验证匹配功能是否正常. 

## 实现对战模块

### 前后端交互接口

连接: 

```
ws://127.0.0.1:8080/game
```

连接响应:

> 当两个玩家都连接好了, 则给双方都返回一个数据表示就绪

```json
{
    message: 'gameReady',    // 游戏就绪
    ok: true,                // 是否成功. 
    reason: '',                // 错误原因
    roomId: 'abcdef',        // 房间号. 用来辅助调试. 
    thisUserId: 1,            // 玩家自己的 id
    thatUserId: 2,            // 对手的 id
    whiteUser: 1,            // 先手方的 id
}
```

落子请求:

```json
{
    message: 'putChess',
    userId: 1,
    row: 0,
    col: 0
}
```

落子响应: 

```json
{
    message: 'putChess',
    userId: 1,    
    row: 0,
    col: 0, 
    winner: 0
}
```

### 客户端开发

#### 实现页面基本结构

创建 game_room.html, 表示对战页面. 

* 此处引入了 canvas 标签.这个是 HTML5 引入的 "画布". 后续的棋盘和棋子的绘制, 就依赖这个画布功能. 
* #screen 用于显示当前的状态. 例如 "等待玩家连接中...", "轮到你落子", "轮到对方落子" 等. 

```html
<div class="nav">
    联机五子棋
</div>
<div class="container">
    <div>
        <canvas id="chess" width="450px" height="450px"></canvas>
        <div id="screen">等待玩家连接中...</div>
    </div>
</div>
```

创建 css/game_room.css

```css
#screen {
    font-size: 22px;
    text-align: center;
}
```

#### 实现棋盘/棋子绘制

创建 `js/app.js`

* 这部分代码基于 canvas API. 我们不需要理解这部分内容. 只需要直接**复制粘贴下列代码**即可. 
* 使用一个二维数组来表示棋盘. 虽然胜负是通过服务器判定的, 但是客户端的棋盘可以避免 "一个位置重复落子" 这样的情况
* oneStep 函数起到的效果是在一个指定的位置上绘制一个棋子. 可以区分出绘制白字还是黑子. 参数是横坐标和纵坐标, **分别对应列和行**. 
* 用 onclick 来处理用户点击事件. 当用户点击的时候通过这个函数来控制绘制棋子. 
* me 变量用来表示当前是否轮到我落子. over 变量用来表示游戏结束. 
* 这个代码中会用到一个背景图(sky.jpg), 放到 image 目录中即可. 

```js
gameInfo = {
    roomId: null,
    thisUserId: null,
    thatUserId: null,
    isWhite: true,
}

//////////////////////////////////////////////////
// 设定界面显示相关操作
//////////////////////////////////////////////////

function setScreenText(me) {
    let screen = document.querySelector('#screen');
    if (me) {
        screen.innerHTML = "轮到你落子了!";
    } else {
        screen.innerHTML = "轮到对方落子了!";
    }
}

//////////////////////////////////////////////////
// 初始化 websocket
//////////////////////////////////////////////////
// TODO

//////////////////////////////////////////////////
// 初始化一局游戏
//////////////////////////////////////////////////
function initGame() {
    // 是我下还是对方下. 根据服务器分配的先后手情况决定
    let me = gameInfo.isWhite;
    // 游戏是否结束
    let over = false;
    let chessBoard = [];
    //初始化chessBord数组(表示棋盘的数组)
    for (let i = 0; i < 15; i++) {
        chessBoard[i] = [];
        for (let j = 0; j < 15; j++) {
            chessBoard[i][j] = 0;
        }
    }
    let chess = document.querySelector('#chess');
    let context = chess.getContext('2d');
    context.strokeStyle = "#BFBFBF";
    // 背景图片
    let logo = new Image();
    logo.src = "image/sky.jpeg";
    logo.onload = function () {
        context.drawImage(logo, 0, 0, 450, 450);
        initChessBoard();
    }

    // 绘制棋盘网格
    function initChessBoard() {
        for (let i = 0; i < 15; i++) {
            context.moveTo(15 + i * 30, 15);
            context.lineTo(15 + i * 30, 430);
            context.stroke();
            context.moveTo(15, 15 + i * 30);
            context.lineTo(435, 15 + i * 30);
            context.stroke();
        }
    }

    // 绘制一个棋子, me 为 true
    function oneStep(i, j, isWhite) {
        context.beginPath();
        context.arc(15 + i * 30, 15 + j * 30, 13, 0, 2 * Math.PI);
        context.closePath();
        var gradient = context.createRadialGradient(15 + i * 30 + 2, 15 + j * 30 - 2, 13, 15 + i * 30 + 2, 15 + j * 30 - 2, 0);
        if (!isWhite) {
            gradient.addColorStop(0, "#0A0A0A");
            gradient.addColorStop(1, "#636766");
        } else {
            gradient.addColorStop(0, "#D1D1D1");
            gradient.addColorStop(1, "#F9F9F9");
        }
        context.fillStyle = gradient;
        context.fill();
    }

    chess.onclick = function (e) {
        if (over) {
            return;
        }
        if (!me) {
            return;
        }
        let x = e.offsetX;
        let y = e.offsetY;
        // 注意, 横坐标是列, 纵坐标是行
        let col = Math.floor(x / 30);
        let row = Math.floor(y / 30);
        if (chessBoard[row][col] == 0) {
            // TODO 发送坐标给服务器, 服务器要返回结果

            oneStep(col, row, gameInfo.isWhite);
            chessBoard[row][col] = 1;
        }
    }

    // TODO 实现发送落子请求逻辑, 和处理落子响应逻辑. 
}

initGame();
```

此时单独运行这个页面, 效果形如:

![image-20220426185430865](readme-image/image-20220426185430865.png)

#### 初始化 websocket

在 game_room.html 中, 加入 websocket 的连接代码, 实现前后端交互. 

* 先删掉原来的 `initGame` 函数的调用. 一会在获取到服务器反馈的就绪响应之后, 再初始化棋盘. 
* 创建 websocket 对象, 并注册 onopen/onclose/onerror 函数. 其中在 onerror 中做一个跳转到游戏大厅的逻辑. 当网络异常断开, 则回到大厅. 
* 实现 onmessage 方法. onmessage 先处理游戏就绪响应. 

```js
// 注意, 路径要写作 /game 不要写作 /game/
websocket = new WebSocket("ws://127.0.0.1:8080/game");
//连接成功建立的回调方法
websocket.onopen = function (event) {
    console.log("open");
}
//连接关闭的回调方法
websocket.onclose = function () {
    console.log("close");
}
//连接发生错误的回调方法
websocket.onerror = function () {
    console.log("error");
    alert('和服务器连接断开! 返回游戏大厅!')
    location.assign('/game_hall.html')
};
//监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
window.onbeforeunload = function () {
    websocket.close();
}

websocket.onmessage = function (event) {
    console.log('handlerGameReady: ' + event.data);

    let response = JSON.parse(event.data);
    if (response.message != 'gameReady') {
        console.log('响应类型错误!');
        return;
    }
    if (!response.ok) {
        alert('连接游戏失败! reason: ' + response.reason);
        location.assign('/game_hall.html')
        return;
    }
    // 初始化游戏信息
    gameInfo.roomId = response.roomId;
    gameInfo.thisUserId = response.thisUserId;
    gameInfo.thatUserId = response.thatUserId;
    gameInfo.isWhite = (response.whiteUserId == gameInfo.thisUserId);
    console.log('[gameReady] ' + JSON.stringify(gameInfo));
    // 初始化棋盘
    initGame();
    // 设置 #screen 的显示
    setScreenText(gameInfo.isWhite);
}
```

#### 发送落子请求

修改 onclick 函数, 在落子操作时加入发送请求的逻辑.

* 注释掉原有的 onStep 和 修改 chessBoard 的操作, 放到接收落子响应时处理. 
* 实现 send , 通过 websocket 发送落子请求. 

```js
chess.onclick = function (e) {
    if (over) {
        return;
    }
    if (!me) {
        return;
    }
    let x = e.offsetX;
    let y = e.offsetY;
    // 注意, 横坐标是列, 纵坐标是行
    let col = Math.floor(x / 30);
    let row = Math.floor(y / 30);
    if (chessBoard[row][col] == 0) {
        // 发送坐标给服务器, 服务器要返回结果
        send(row, col);

        // oneStep(col, row, gameInfo.isWhite);
        // chessBoard[row][col] = 1;
        // me = !me; 
    }
}

function send(row, col) {
    console.log("send");
    let request = {
        message: "putChess",
        userId: gameInfo.thisUserId,
        row: row,
        col: col,
    }
    websocket.send(JSON.stringify(request));
}
```

#### 处理落子响应

在 initGame 中, 修改 websocket 的 onmessage

* 在 initGame 之前, 处理的是游戏就绪响应, 在收到游戏响应之后, 就改为接收落子响应了. 
* 在处理落子响应中要处理胜负手. 

```js
websocket.onmessage = function (event) {
    console.log('handlerPutChess: ' + event.data);

    let response = JSON.parse(event.data);
    if (response.message != 'putChess') {
        console.log('响应类型错误!');
        return;
    }

    // 1. 判断 userId 是自己的响应还是对方的响应, 
    //    以此决定当前这个子该画啥颜色的
    if (response.userId == gameInfo.thisUserId) {
        oneStep(response.col, response.row, gameInfo.isWhite);
    } else if (response.userId == gameInfo.thatUserId) {
        oneStep(response.col, response.row, !gameInfo.isWhite);
    } else {
        console.log('[putChess] response userId 错误! response=' + JSON.stringify(response));
        return;
    }
    chessBoard[response.row][response.col] = 1;
    me = !me; // 接下来该下个人落子了. 

    // 2. 判断游戏是否结束
    if (response.winner != 0) {
        // 胜负已分
        if (response.winner == gameInfo.thisUserId) {
            alert("你赢了!");
        } else {
            alert("你输了");
        }
        // 如果游戏结束, 则关闭房间, 回到游戏大厅. 
        location.assign('/game_hall.html')
    }

    // 3. 更新界面显示
    setScreenText(me);
}
```

### 服务器开发

#### 创建并注册 GameAPI 类

创建 `api.GameAPI` , 处理 websocket 请求. 

* 这里准备好一个 ObjectMapper
* 同时注入一个 RoomManager 和 OnlineUserMananger

```java
@Component
public class GameAPI extends TextWebSocketHandler {
    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private RoomManager roomManager;
    // 这个是管理 game 页面的会话
    @Autowired
    private OnlineUserManager onlineUserManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    }
}
```

修改 WebSocketConfig, 将 GameAPI 进行注册.

```java
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(testAPI, "/test");
    // 通过 .addInterceptors(new HttpSessionHandshakeInterceptor() 这个操作来把 HttpSession 里的属性放到 WebSocket 的 session 中
    // 参考: https://docs.spring.io/spring-framework/docs/5.0.7.RELEASE/spring-framework-reference/web.html#websocket-server-handshake
    // 然后就可以在 WebSocket 代码中 WebSocketSession 里拿到 HttpSession 中的 attribute.
    registry.addHandler(matchAPI, "/findMatch")
        .addInterceptors(new HttpSessionHandshakeInterceptor());
    registry.addHandler(gameAPI, "/game")
        .addInterceptors(new HttpSessionHandshakeInterceptor());
}
```

#### 创建落子请求/响应对象

这部分内容要和约定的前后端交互接口匹配. 

创建 `game.GameReadyResponse` 类

```java
public class GameReadyResponse {
    private String message = "gameReady";
    private boolean ok = true;
    private String reason = "";
    private String roomId = "";
    private int thisUserId = 0;
    private int thatUserId = 0;
    private int whiteUserId = 0;
}
```

创建 `game.GameRequest` 类

```java
public class GameRequest {
    // 如果不给 message 设置 getter / setter, 则不会被 jackson 序列化
    private String message = "putChess";
    private int userId;
    private int row;
    private int col;
}
```

创建 `game.GameResponse` 类

```java
public class GameResponse {
    // 如果不给 message 设置 getter / setter, 则不会被 jackson 序列化
    private String message = "putChess";
    private int userId;
    private int row;
    private int col;
    private int winner; // 胜利玩家的 userId
}
```

#### 处理连接成功

实现 GameAPI 的 afterConnectionEstablished 方法. 

* 首先需要检测用户的登录状态. 从 Session 中拿到当前用户信息. 
* 然后要判定当前玩家是否是在房间中. 
* 接下来进行多开判定.如果玩家已经在游戏中, 则不能再次连接. 
* 把两个玩家放到对应的房间对象中. 当两个玩家都建立了连接, 房间就放满了.这个时候通知两个玩家双方都准备就绪. 
* 如果有第三个玩家尝试也想加入房间, 则给出一个提示, 房间已经满了. 

```java
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    GameReadyResponse resp = new GameReadyResponse();

    User user = (User) session.getAttributes().get("user");
    if (user == null) {
        resp.setOk(false);
        resp.setReason("用户尚未登录!");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
        return;
    }
    Room room = roomManager.getRoomByUserId(user.getUserId());
    if (room == null) {
        resp.setOk(false);
        resp.setReason("用户并未匹配成功! 不能开始游戏!");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
        return;
    }
    System.out.println("连接游戏! roomId=" + room.getRoomId() + ", userId=" + user.getUserId());

    // 先判定用户是不是已经在游戏中了.
    if (onlineUserManager.getSessionFromGameHall(user.getUserId()) != null
        || onlineUserManager.getSessionFromGameRoom(user.getUserId()) != null) {
        resp.setOk(false);
        resp.setReason("禁止多开游戏页面!");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
        return;
    }
    // 更新会话
    onlineUserManager.enterGameRoom(user.getUserId(), session);

    // 同一个房间的两个玩家, 同时连接时要考虑线程安全问题.
    synchronized (room) {
        if (room.getUser1() == null) {
            room.setUser1(user);
            // 设置 userId1 为先手方
            room.setWhiteUserId(user.getUserId());
            System.out.println("userId=" + user.getUserId() + " 玩家1准备就绪!");
            return;
        }
        if (room.getUser2() == null) {
            room.setUser2(user);
            System.out.println("userId=" + user.getUserId() + " 玩家2准备就绪!");

            // 通知玩家1 就绪
            noticeGameReady(room, room.getUser1().getUserId(), room.getUser2().getUserId());
            // 通知玩家2 就绪
            noticeGameReady(room, room.getUser2().getUserId(), room.getUser1().getUserId());
            return;
        }
    }
    // 房间已经满了!
    resp.setOk(false);
    String log = "roomId=" + room.getRoomId() + " 已经满了! 连接游戏失败!";
    resp.setReason(log);
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
    System.out.println(log);
}
```

实现通知玩家就绪

```java
private void noticeGameReady(Room room, int thisUserId, int thatUserId) throws IOException {
    GameReadyResponse resp = new GameReadyResponse();
    resp.setRoomId(room.getRoomId());
    resp.setThisUserId(thisUserId);
    resp.setThatUserId(thatUserId);
    resp.setWhiteUserId(room.getWhiteUserId());
    WebSocketSession session1 = onlineUserManager.getSessionFromGameRoom(thisUserId);
    session1.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
}
```



#### 玩家下线的处理

* 下线的时候要注意针对多开情况的判定. 

```java
@Override
public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    User user = (User) session.getAttributes().get("user");
    if (user == null) {
        return;
    }
    WebSocketSession existSession = onlineUserManager.getSessionFromGameRoom(user.getUserId());
    if (existSession != session) {
        System.out.println("当前的会话不是玩家游戏中的会话, 不做任何处理!");
        return;
    }
    System.out.println("连接出错! userId=" + user.getUserId());
    onlineUserManager.exitGameRoom(user.getUserId());
}

@Override
public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    User user = (User) session.getAttributes().get("user");
    if (user == null) {
        return;
    }
    WebSocketSession existSession = onlineUserManager.getSessionFromGameRoom(user.getUserId());
    if (existSession != session) {
        System.out.println("当前的会话不是玩家游戏中的会话, 不做任何处理!");
        return;
    }
    System.out.println("用户退出! userId=" + user.getUserId());
    onlineUserManager.exitGameRoom(user.getUserId());
}
```





#### 修改 Room 类(1)

给 Room 类里加上 RoomManager 实例 和 UserMapper 实例

* Room 类内部要在游戏结束的时候销毁房间, 需要用到 RoomManager
* Room 类内部要修改玩家的分数, 需要用到 UserMapper

```java
public class Room {
    private static final int MAX_ROW = 15;
    private static final int MAX_COL = 15;

    private String roomId;
    // 玩家1
    private User user1;
    // 玩家2
    private User user2;
    // 先手方的用户 id
    private int whiteUserId = 0;
    // 棋盘, 数字 0 表示未落子位置. 数字 1 表示玩家 1 的落子. 数字 2 表示玩家 2 的落子
    private int[][] chessBoard = new int[MAX_ROW][MAX_COL];

    private ObjectMapper objectMapper = new ObjectMapper();

    // @Autowired
    private OnlineUserManager onlineUserManager;
    // @Autowired
    private RoomManager roomManager;
    // @Resource
    private UserMapper userMapper;

    // ......
}
```

#### 修改 Room 类(2)

由于我们的 Room 并没有通过 Spring 来管理. 因此内部就无法通过 `@Autowired` 来自动注入. 

需要手动的通过 SpringBoot 的启动类来获取里面的对象. 

```java
@SpringBootApplication
public class GobangApplication {
    // 添加一个 ConfigurableApplicationContext 对象
    public static ConfigurableApplicationContext ac;

    public static void main(String[] args) {
        // 使用 ac 作为 run 的返回值. 
        ac = SpringApplication.run(GobangApplication.class, args);
    }
}
```

然后再 Room 类的构造方法中, 手动获取到 Bean

```java
public Room() {
    // 使用 uuid 作为唯一身份标识
    roomId = UUID.randomUUID().toString();

    onlineUserManager = GobangApplication.ac.getBean(OnlineUserManager.class);
    roomManager = GobangApplication.ac.getBean(RoomManager.class);
    userMapper = GobangApplication.ac.getBean(UserMapper.class);

    System.out.println("create Room: " + roomId + ", roomManager: " + roomManager);
}
```

#### 处理落子请求

实现 handleTextMessage

```java
@Override
protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    User user = (User) session.getAttributes().get("user");
    if (user == null) {
        return;
    }
    Room room = roomManager.getRoomByUserId(user.getUserId());
    room.putChess(message.getPayload());
}
```

#### 实现对弈功能(1)

实现 room 中的 putChess 方法. 

* 先把请求解析成请求对象. 
* 根据请求对象中的信息, 往棋盘上落子. 
* 落子完毕之后, 为了方便调试, 可以打印出棋盘的当前状况. 
* 检查游戏是否结束. 
* 构造落子响应, 写回给每个玩家. 
* 写回的时候如果发现某个玩家掉线, 则判定另一方为获胜. 
* 如果游戏胜负已分, 则修改玩家的分数, 并销毁房间. 

```java
// 玩家落子
public void putChess(String message) throws IOException {
    GameRequest req = objectMapper.readValue(message, GameRequest.class);
    GameResponse response = new GameResponse();
    // 1. 进行落子
    int chess = req.getUserId() == user1.getUserId() ? 1 : 2;
    int row = req.getRow();
    int col = req.getCol();
    if (chessBoard[row][col] != 0) {
        System.out.println("落子位置有误! " + req);
        return;
    }
    chessBoard[row][col] = chess;
    printChessBoard();
    // 2. 检查游戏结束
    //    返回的 winner 为玩家的 userId
    int winner = checkWinner(chess, row, col);
    // 3. 把响应写回给玩家
    response.setUserId(req.getUserId());
    response.setRow(row);
    response.setCol(col);
    response.setWinner(winner);
    WebSocketSession session1 = onlineUserManager.getSessionFromGameRoom(user1.getUserId());
    WebSocketSession session2 = onlineUserManager.getSessionFromGameRoom(user2.getUserId());
    if (session1 == null) {
        // 玩家1 掉线, 直接认为玩家2 获胜
        response.setWinner(user2.getUserId());
        System.out.println("玩家1 掉线!");
    }
    if (session2 == null) {
        // 玩家2 掉线, 直接认为玩家1 获胜
        response.setWinner(user1.getUserId());
        System.out.println("玩家2 掉线!");
    }
    String responseJson = objectMapper.writeValueAsString(response);
    if (session1 != null) {
        session1.sendMessage(new TextMessage(responseJson));
    }
    if (session2 != null) {
        session2.sendMessage(new TextMessage(responseJson));
    }
    // 4. 如果玩家胜负已分, 就把 room 从管理器中销毁
    if (response.getWinner() != 0) {
        userMapper.userWin(response.getWinner() == user1.getUserId() ? user1 : user2);
        userMapper.userLose(response.getWinner() == user1.getUserId() ? user2 : user1);
        roomManager.removeRoom(roomId, user1.getUserId(), user2.getUserId());
        System.out.println("游戏结束, 房间已经销毁! roomId: " + roomId + " 获胜方为: " + response.getWinner());
    }
}
```

#### 实现对弈功能(2)

实现打印棋盘的逻辑

```java
private void printChessBoard() {
    System.out.println("打印棋盘信息: ");
    System.out.println("===========================");
    for (int r = 0; r < MAX_ROW; r++) {
        for (int c = 0; c < MAX_COL; c++) {
            System.out.print(chessBoard[r][c] + " ");
        }
        System.out.println();
    }
    System.out.println("===========================");
}
```

#### 实现对弈功能(3)

实现胜负判定

* 如果游戏分出胜负, 则返回玩家的 id. 如果未分出胜负,则返回 0. 
* 棋盘中值为 1 表示是玩家 1 的落子, 值为 2 表示是玩家 2 的落子. 
* 检查胜负的时候, 以当前落子位置为中心, 检查所有相关的行,列, 对角线即可. 不必遍历整个棋盘. 

```java
// 判定棋盘形式, 找出胜利的玩家.
// 如果游戏分出胜负, 则返回玩家的 id.
// 如果未分出胜负, 则返回 0
// chess 值为 1 表示玩家1 的落子. 为 2 表示玩家2 的落子
private int checkWinner(int chess, int row, int col) {
    // 以 row, col 为中心
    boolean done = false;
    // 1. 检查所有的行(循环五次)
    for (int c = col - 4; c <= col; c++) {
        if (c < 0 || c >= MAX_COL) {
            continue;
        }
        if (chessBoard[row][c] == chess
            && chessBoard[row][c + 1] == chess
            && chessBoard[row][c + 2] == chess
            && chessBoard[row][c + 3] == chess
            && chessBoard[row][c + 4] == chess) {
            done = true;
        }
    }
    // 2. 检查所有的列(循环五次)
    for (int r = row - 4; r <= row; r++) {
        if (r < 0 || r >= MAX_ROW) {
            continue;
        }
        if (chessBoard[r][col] == chess
            && chessBoard[r + 1][col] == chess
            && chessBoard[r + 2][col] == chess
            && chessBoard[r + 3][col] == chess
            && chessBoard[r + 4][col] == chess) {
            done = true;
        }
    }
    // 3. 检查左对角线
    for (int r = row - 4, c = col - 4; r <= row && c <= col; r++, c++) {
        if (r < 0 || r >= MAX_ROW || c < 0 || c >= MAX_COL) {
            continue;
        }
        if (chessBoard[r][c] == chess
            && chessBoard[r + 1][c + 1] == chess
            && chessBoard[r + 2][c + 2] == chess
            && chessBoard[r + 3][c + 3] == chess
            && chessBoard[r + 4][c + 4] == chess) {
            done = true;
        }
    }
    // 4. 检查右对角线
    for (int r = row - 4, c = col + 4; r <= row && c >= col; r++, c--) {
        if (r < 0 || r >= MAX_ROW || c < 0 || c >= MAX_COL) {
            continue;
        }
        if (chessBoard[r][c] == chess
            && chessBoard[r + 1][c - 1] == chess
            && chessBoard[r + 2][c - 2] == chess
            && chessBoard[r + 3][c - 3] == chess
            && chessBoard[r + 4][c - 4] == chess) {
            done = true;
        }
    }
    if (!done) {
        return 0;
    }
    return chess == 1 ? user1.getUserId() : user2.getUserId();
}
```

#### 处理玩家中途退出

在 GameAPI 中

```java
@Override
public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    User user = (User) session.getAttributes().get("user");
    if (user == null) {
        return;
    }
    WebSocketSession existSession = onlineUserManager.getSessionFromGameRoom(user.getUserId());
    if (existSession != session) {
        System.out.println("当前的会话不是玩家游戏中的会话, 不做任何处理!");
        return;
    }
    System.out.println("连接出错! userId=" + user.getUserId());
    onlineUserManager.exitGameRoom(user.getUserId());
    
    // [代码加在这里]
    noticeThatUserWin(user);
}

@Override
public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    User user = (User) session.getAttributes().get("user");
    if (user == null) {
        return;
    }
    WebSocketSession existSession = onlineUserManager.getSessionFromGameRoom(user.getUserId());
    if (existSession != session) {
        System.out.println("当前的会话不是玩家游戏中的会话, 不做任何处理!");
        return;
    }
    System.out.println("用户退出! userId=" + user.getUserId());
    onlineUserManager.exitGameRoom(user.getUserId());
    
    // [代码加在这里]
    noticeThatUserWin(user);
}
```



```java
// 通知另外一个玩家直接获胜!
private void noticeThatUserWin(User user) throws IOException {
    Room room = roomManager.getRoomByUserId(user.getUserId());
    if (room == null) {
        System.out.println("房间已经释放, 无需通知!");
        return;
    }
    User thatUser = (user == room.getUser1() ? room.getUser2() : room.getUser1());
    WebSocketSession session = onlineUserManager.getSessionFromGameRoom(thatUser.getUserId());
    if (session == null) {
        System.out.println(thatUser.getUserId() + " 该玩家已经下线, 无需通知!");
        return;
    }
    GameResponse resp = new GameResponse();
    resp.setUserId(thatUser.getUserId());
    resp.setWinner(thatUser.getUserId());
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(resp)));
}
```





### 验证对战功能

运行程序, 验证当前对战功能是否正常. 



## 后续扩展功能

### 实现局时, 步时

局时: 一局游戏中玩家能思考的总时间. 

步时: 一步落子过程中, 玩家能思考的时间. 

例如, 给每一局游戏设定 10 分钟局时, 1 分钟步时. 

在页面上使用 JS 中的定时器, 来实时的显示当前剩余时间. 

如果某玩家超时, 则直接判定对方获胜. 

### 保存棋谱&录像回放

首先需要在数据库中创建一个新的表, 用来表示每个玩家的游戏房间编号. 

服务器把每一局对局, 玩家轮流落子的位置都记录下来(比如保存到一个文本文件中). 

然后玩家可以选定某个曾经的比萨, 在页面上回放出对局的过程. 

### 观战功能

在游戏大厅除了显示匹配按钮之外, 还能显示当前所有的对局房间. 

玩家可以选中某个房间, 以观众的形式加入到房间中. 同时能实时的看到选手的对局情况. 

### 聊天功能

同一个房间中的选手之间可以发送文本消息. 

### 人机对战

支持 AI 功能, 实现人机对战. 

### 虚拟对手

如果当前长时间匹配不到选手, 则自动分配一个 AI 对手. 
