## 实现匹配模块

让多个用户，在游戏大厅中能够进行匹配，系统会把实力相近的两个玩家凑成一桌，进行对战。

约定前后端交互接口（为什么要前后端分离：https://blog.51cto.com/u_15499328/5283578）

约定的前后端交互接口，也都是基于 websocket 来展开的

websocket 可以传输文本数据，也能传输二进制数据
此处就直接设计成让 websocket 传输 JSON 格式的文本数据即可

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


