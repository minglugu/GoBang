package com.example.java_gobang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class JavaGobangApplication {
    public static ConfigurableApplicationContext context;

    // spring 的入口类
    public static void main(String[] args) {
        context = SpringApplication.run(JavaGobangApplication.class, args);
    }

}
