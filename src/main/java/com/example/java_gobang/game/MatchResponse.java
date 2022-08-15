package com.example.java_gobang.game;

// 这是表示一个 websocket 的匹配响应，内容是按照readme.txt里面约定的接口
public class MatchResponse {
    private boolean ok;
    private String reason;
    private String message; // 和请求里面的message是相匹配的

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
