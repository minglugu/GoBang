package com.example.java_gobang.game;

/* 客户端成功连接到游戏房间后，服务器返回的响应
  当玩家匹配成功后，由服务器生成的内容，返回到浏览器中。
 这个是响应的JSON信息。
 {
      message: 'gameReady',     //  消息类别是 游戏就绪
      ok: true,                 //
      reason: '',               //
      roomId: '12345678',       // 玩家所处的房间UUID
      thisUserId: 1,            // 玩家自己的id
      thatUserId: 2,            // 玩家的对手的id
      whiteUser: 1              // 哪个玩家是白子（先落子）
 }
 不需要生成单独的一个类，生成一个请求(request)
 因为请求里，也不需要传递信息，
 所以连接成功后，给客户端，返回一个响应对象(上面的JSON对象里的信息)就可以了
 ws://127.0.0.1：8080/game 这个为request
 */
public class GameReadyResponse {
    private String message;
    private boolean ok;
    private String reason;
    private String roomId;
    private int thisUserId;
    private int thatUserId; // 对手id
    private int whiteUser;  // 谁是先手(白子)

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

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

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public int getThisUserId() {
        return thisUserId;
    }

    public void setThisUserId(int thisUserId) {
        this.thisUserId = thisUserId;
    }

    public int getThatUserId() {
        return thatUserId;
    }

    public void setThatUserId(int thatUserId) {
        this.thatUserId = thatUserId;
    }

    public int getWhiteUser() {
        return whiteUser;
    }

    public void setWhiteUser(int whiteUser) {
        this.whiteUser = whiteUser;
    }
}
