package com.example.java_gobang.game;

// 这个类表示的是落子的响应，包含的内容，跟约定的格式是相匹配的
public class GameResponse {
    private String message;
    private int userId;
    private int row;
    private int col;
    private int winner; // 谁获胜了

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

    public int getWinner() {
        return winner;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }
}
