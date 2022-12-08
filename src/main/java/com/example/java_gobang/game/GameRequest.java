package com.example.java_gobang.game;

// 这个类表示的是落子的请求request，包含落子的相关信息
public class GameRequest {
    private String message;
    private int userId; // 是谁落的子
    // 落子位置的坐标
    private int row;    // 第几行
    private int col;    // 第几列

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }
}
