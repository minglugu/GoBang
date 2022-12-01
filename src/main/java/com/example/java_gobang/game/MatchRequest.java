package com.example.java_gobang.game;

// 这是表示一个 websocket 的响应请求，内容是按照readme.txt里面约定的接口
public class MatchRequest {
    private String message = ""; // 初始化的时候，设置成空字符串

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
