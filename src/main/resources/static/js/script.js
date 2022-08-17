// 客户端发给服务器请求，然后服务器会给客户端返回的响应。
// 用全局变量来记录，并初始化信息
gameInfo = {
    roomId: null,
    thisUserId: null,
    thatUserId: null,
    isWhite: true,
}

//////////////////////////////////////////////////
// 设定界面显示相关操作，来回切换界面
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

// 此处写的路径要写作 /game, 不要写作 /game/
// let websocketUrl = "ws://" + location.host + "/game";
// let websocket = new WebSocket(websocketUrl);

// websocket.onopen = function() {
//     console.log("连接游戏房间成功!");
// }

// websocket.close = function() {
//     console.log("和游戏服务器断开连接!");
// }

// websocket.onerror = function() {
//     console.log("和服务器的连接出现异常!");
// }

// window.onbeforeunload = function() {
//     websocket.close();
// }

// // 处理服务器返回的响应数据
// websocket.onmessage = function(event) {
//     console.log("[handlerGameReady] " + event.data);
//     let resp = JSON.parse(event.data);

//     if (!resp.ok) {
//         alert("连接游戏失败! reason: " + resp.reason);
//         // 如果出现连接失败的情况, 回到游戏大厅
//         location.assign("/game_hall.html");
//         return;
//     }

//     if (resp.message == 'gameReady') {
//         gameInfo.roomId = resp.roomId;
//         gameInfo.thisUserId = resp.thisUserId;
//         gameInfo.thatUserId = resp.thatUserId;
//         gameInfo.isWhite = (resp.whiteUser == resp.thisUserId);

//         // 初始化棋盘
//         initGame();
//         // 设置显示区域的内容
//         setScreenText(gameInfo.isWhite);
//     } else if (resp.message == 'repeatConnection') {
//         alert("检测到游戏多开! 请使用其他账号登录!");
//         location.assign("/login.html");
//     }
// }

//////////////////////////////////////////////////
// 初始化一局游戏
//////////////////////////////////////////////////
function initGame() {
    // 是我下还是对方下. 根据服务器分配的先后手情况决定， me=true，我下
    let me = gameInfo.isWhite;
    // 游戏是否结束
    let over = false;
    // 构造 2D array 的棋盘
    let chessBoard = [];
    //初始化chessBord数组(表示棋盘的数组) 15 x 15 
    for (let i = 0; i < 15; i++) {
        chessBoard[i] = [];
        // 针对小数组，再次添加元素，初始化为0
        for (let j = 0; j < 15; j++) {
            chessBoard[i][j] = 0;
        }
    }
    // canvas API
    // 拿到canvas标签
    let chess = document.querySelector('#chess');
    let context = chess.getContext('2d'); // 通过上下文，进入到canvas
    context.strokeStyle = "#BFBFBF";
    // 背景图片
    let logo = new Image();
    logo.src = "image/snow.jpg";
    // 加载
    logo.onload = function () {
        // 把图片画上去
        context.drawImage(logo, 0, 0, 450, 450);
        // 初始化(绘制)棋盘
        initChessBoard();
    }

    // 绘制棋盘网格，循环绘制，moveTo kineTo 画笔进行绘制
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

    // 绘制一个棋子, me 为 true， 参照MDN manual
    function oneStep(i, j, isWhite) {
        context.beginPath();
        // 画圆
        context.arc(15 + i * 30, 15 + j * 30, 13, 0, 2 * Math.PI);
        context.closePath();
        var gradient = context.createRadialGradient(15 + i * 30 + 2, 15 + j * 30 - 2, 13, 15 + i * 30 + 2, 15 + j * 30 - 2, 0);
        // 白子还是黑子
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

    // 针对chess（棋盘canvas），设定了点击回调。
    // e是点击回调中的事件参数，这里就会记录点击的实际位置（坐标）
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
        // /30 是为了点击操作，能够对应到网格线上。因为总体的棋盘size是450*450，col and row是15*15
        // 每格占用 450 / 15 = 30 
        let col = Math.floor(x / 30);
        let row = Math.floor(y / 30);
        if (chessBoard[row][col] == 0) {
            // 发送坐标给服务器, 服务器要返回结果
            //send(row, col);

            // 留到浏览器收到落子响应的时候再处理(收到响应再来画棋子)
            // 走一步（里面会 绘制一个棋子）
            oneStep(col, row, gameInfo.isWhite);
            // 设置为1，表示已经落子
            chessBoard[row][col] = 1;
        }
    }

    // function send(row, col) {
    //     let req = {
    //         message: 'putChess',
    //         userId: gameInfo.thisUserId,
    //         row: row,
    //         col: col
    //     };

    //     websocket.send(JSON.stringify(req));
    // }

    // 之前 websocket.onmessage 主要是用来处理了游戏就绪响应. 在游戏就绪之后, 初始化完毕之后, 也就不再有这个游戏就绪响应了. 
    // 就在这个 initGame 内部, 修改 websocket.onmessage 方法~~, 让这个方法里面针对落子响应进行处理!
    // websocket.onmessage = function(event) {
    //     console.log("[handlerPutChess] " + event.data);

    //     let resp = JSON.parse(event.data);
    //     if (resp.message != 'putChess') {
    //         console.log("响应类型错误!");
    //         return;
    //     }

    //     // 先判定当前这个响应是自己落的子, 还是对方落的子.
    //     if (resp.userId == gameInfo.thisUserId) {
    //         // 我自己落的子
    //         // 根据我自己子的颜色, 来绘制一个棋子
    //         oneStep(resp.col, resp.row, gameInfo.isWhite);
    //     } else if (resp.userId == gameInfo.thatUserId) {
    //         // 我的对手落的子
    //         oneStep(resp.col, resp.row, !gameInfo.isWhite);
    //     } else {
    //         // 响应错误! userId 是有问题的!
    //         console.log('[handlerPutChess] resp userId 错误!');
    //         return;
    //     }

    //     // 给对应的位置设为 1, 方便后续逻辑判定当前位置是否已经有子了. 
    //     chessBoard[resp.row][resp.col] = 1;

    //     // 交换双方的落子轮次
    //     me = !me;
    //     setScreenText(me);

    //     // 判定游戏是否结束
    //     let screenDiv = document.querySelector('#screen');
    //     if (resp.winner != 0) {
    //         if (resp.winner == gameInfo.thisUserId) {
    //             // alert('你赢了!');
    //             screenDiv.innerHTML = '你赢了!';
    //         } else if (resp.winner = gameInfo.thatUserId) {
    //             // alert('你输了!');
    //             screenDiv.innerHTML = '你输了!';
    //         } else {
    //             alert("winner 字段错误! " + resp.winner);
    //         }
    //         // 回到游戏大厅
    //         // location.assign('/game_hall.html');

    //         // 增加一个按钮, 让玩家点击之后, 再回到游戏大厅~
    //         let backBtn = document.createElement('button');
    //         backBtn.innerHTML = '回到大厅';
    //         backBtn.onclick = function() {
    //             location.replace('/game_hall.html');
    //         }
    //         let fatherDiv = document.querySelector('.container>div');
    //         fatherDiv.appendChild(backBtn);
    //     }
    // }
}

initGame();