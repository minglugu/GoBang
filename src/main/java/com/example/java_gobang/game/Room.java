package com.example.java_gobang.game;

import com.example.java_gobang.JavaGobangApplication;
import com.example.java_gobang.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.UUID;

// 这个类，是一个游戏房间
public class Room {
    // 使用字符串类型来表示，用于生成唯一值。
    private String roomId;

    private User user1;
    private User user2;

    // 使用常量方式，替代hardcode
    private static final int MAX_ROW = 15;
    private static final int MAX_COL = 15;

    // 先落子的玩家 id
    private int whiteUser;

    // 2D array 表示棋盘
    // 约定：
    // 1）使用0，表示当前位子 没有落子。初始话好的 int 2D array，相当于 全部是0
    // 2）使用1，表示user1 的落子位置
    // 3）使用2，表示user2 的落子位置
    private int[][] board = new int[MAX_ROW][MAX_COL];
    // 通过这个方法，来处理一次落子的操作

    // objectMapper来转换JSON
    private ObjectMapper objectMapper = new ObjectMapper();

    // 引入OnlineUserManager，根据用户Id 来查询到 WebSocketSession
    // 手动注入
    // @Autowired
    private OnlineUserManager onlineUserManager;

    // 引入RoomManager，用于分出胜负后，移除所在的房间
    // 手动注入
    // @Autowired
    private RoomManager roomManager;

    // # video 63
    public void putChess(String reqJson) throws IOException {
        // 1. 记录当前落子的位子， 2D array, 对请求的解析操作
        GameRequest request = objectMapper.readValue(reqJson, GameRequest.class);
        // 构造的响应
        GameResponse response = new GameResponse();
        // 当前的子，是玩家1还是玩家2 落的子。根据玩家1 和玩家2 来决定，往数组里是写1 还是 2.
        int chess = request.getUserId() == user1.getUserId() ? 1 : 2;
        int row = request.getRow();
        int col = request.getCol();
        // row 和 col 是否已经落子
        if (board[row][col] == 1) {
            // 已经在客户端，对重复落子，已经进行判断了。为了程序稳定，在服务器端，再次进行判断
            System.out.println("当前位置(" + row + ", " + col + ") 已经有子了！");
            return;
        }
        // 记录落子的位置
        board[row][col] = chess;
        // 2. 打印出棋盘信息，方便观察局势，也方便后面验证胜负关系的判定
        printBoard();

        // 3. 进行胜负判定
        int winner = checkWinner(row, col);

        // 4. 给客户端返回响应对象，构造响应数据，返回给客户端.
        //    给房间里的所有用户端，都返回这个响应
        response.setMessage("putChess");
        response.setUserId(request.getUserId());
        response.setRow(row);
        response.setCol(col);
        response.setWinner(winner);

        // 要想给客户发送websocket数据，就需要获取到这个用户的WebsocketSession
        // 在OnlineUserManager里，从gameRoom里面(有HashMap),用玩家的id 就可以查到WebSocketSession
        WebSocketSession session1 = onlineUserManager.getFromGameRoom(user1.getUserId());
        WebSocketSession session2 = onlineUserManager.getFromGameRoom(user2.getUserId());

        // 判定会话是否为空，比如说玩家下线
        if(session1 == null) {
            // 玩家1 下线，认为玩家2 获胜
            response.setWinner(user2.getUserId());
            System.out.println("玩家1 掉线！");
        }
        if(session2 == null) {
            // 玩家2 下线，认为玩家1 获胜
            response.setWinner(user1.getUserId());
            System.out.println("玩家2 掉线！");
        }
        // 响应的json字符串。把响应构造成JSON 字符串，通过session进行传输
        String respJson = objectMapper.writeValueAsString(response);
        if (session1 != null) {
            session1.sendMessage(new TextMessage(respJson));
        }
        if (session2 != null) {
            session2.sendMessage(new TextMessage(respJson));
        }

        // 5. 如果当前胜负已经分，那么房间就失去存在的意义了，将房间从房间管理器中移除
        if (response.getWinner() != 0) {
            // 胜负已分
            System.out.println("游戏结束，房间即将销毁！Room Id = " + roomId + ", 获胜方为: " + response.getWinner());
            // 删除房间
            roomManager.remove(roomId, user1.getUserId(), user2.getUserId());
        }
    }

    private void printBoard() {
        // 打印棋盘
        System.out.println("[打印棋盘信息] " + roomId);
        System.out.println("==========================================================");
        for (int r = 0; r < MAX_ROW; r++) {
            for (int c = 0; c < MAX_COL; c++) {
                // 同一行，无需换行符
                System.out.print(board[r][c] + " ");
            }
            // 打印完一行，换行
            System.out.println();
        }
        System.out.println("==========================================================");

    }

    // 每个玩家落子后，使用这个方法，来判定，当前落子是否分出胜负
    // 约定：如果玩家1 获胜，返回玩家1 的userId，如果玩家2 获胜，返回玩家2 的userId.
    // 如果胜负未分，那么返回0
    private int checkWinner(int row, int col) {
        // TODO checkWinner(row, col)
        // 判定棋面上，是否出现5子 连珠。一行，一列，或对角线
        // 只判定落子周围的5子连珠的情况。

        return 0;
    }

    // constructor
    public Room() {
        // 构造 Room 的时候，生成一个唯一的字符串，表示房间 id
        // 使用 UUID 来作为房间 id
        // UUID 表示“世界上唯一的身份标识”，16进制表示的数字，
        // 任意一次调用，每次结果都不同
        roomId = UUID.randomUUID().toString();
        // 通过入口类中记录的 context 来手动获取到前面的 RoomManager 和 OnlineUserManager
        onlineUserManager = JavaGobangApplication.context.getBean(OnlineUserManager.class);
        roomManager = JavaGobangApplication.context.getBean(RoomManager.class);
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public User getUser1() {
        return user1;
    }

    public void setUser1(User user1) {
        this.user1 = user1;
    }

    public User getUser2() {
        return user2;
    }

    public void setUser2(User user2) {
        this.user2 = user2;
    }
}
