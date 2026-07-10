package com.mymoney.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MyMoneyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyMoneyApiApplication.class, args);
    }
}
