package com.example.java_gobang.api;

import com.example.java_gobang.model.User;
import com.example.java_gobang.model.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

// 和用户相关的API，定位是controller
@RestController
public class UserAPI {

    // 后端提供的接口
    @Resource // 自动注入，所以无需new一个对象
    private UserMapper userMapper;

    // 当路径里面，有login时，就会触发到这个方法
    @PostMapping("/login")
    @ResponseBody
    public Object login(String username, String password, HttpServletRequest req) {
        // 关键操作，根据用户名传来的username，去数据库查询，
        // 如果能找到匹配的用户，并且密码也一致，就认为登录成功
        // 在UserMapper的interface里面，有selectByName()这个方法，
        User user = userMapper.selectByName(username);
        System.out.println("[login] username=" + username); // 哪个用户名在登录
        // 对user进行判断
        // 用户名不存在，或者用户存在但登录失败
        if(user == null || !user.getPassword().equals(password)) {
            // 登录失败,返回空的对象(无效对象)
            System.out.println("登录失败！");
            return new User();
        }
        // 登录成功，返回对象的同时，还需要对session进行设置，查询到的user保存到session里面，
        // 下次访问服务器，正确识别当前客户端的身份信息。
        // true, 当session存在，直接返回。不存在，创建一个。如果是false，存在就返回，不存在，就直接返回空。
        HttpSession httpSession = req.getSession(true);
        // user对象保存到会话里面，key为user，value是user对象
        httpSession.setAttribute("user", user);
        // 登录成功
        return user;
    }

    // 用来注册的逻辑
    @PostMapping("/register")
    @ResponseBody
    public Object register(String username, String password) {
        try {
            // 类似登录模式
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            // 只需要插入用户名和密码，因为在UserMapper.xml文件里面，insert的时候，有默认值了（e.g. 1000,0,0）。
            // 定位到userMapper.xml生成的相关代码，完成数据库插入操作
            // 用户名不能重复(unique)
            userMapper.insert(user);
            return user;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            User user = new User();
            return user; // 如果有重复的用户名(注册失败)，那么返回一个空的对象
        }
    }

    @GetMapping("/userInfo")
    @ResponseBody
    // HttpServletRequest req这个参数，才能拿到session
    public Object getUserInfo(HttpServletRequest req) {
        //不需要查询数据库，从session里面，直接获取到对应user对象，并返回
        // 已经登录完了，不需要再创建新的session，所以就设置成false
        // 判断是否空，所以用try catch
        try {
            HttpSession httpSession = req.getSession(false);
            User user = (User) httpSession.getAttribute("user");
            // 拿着这个 user 对象，去数据库中找，找到最新的数据
            User newUser = userMapper.selectByName(user.getUsername());
            return newUser;
        } catch (NullPointerException e) {
            return new User();
        }
    }
}
