package com.demo.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages={"com.demo"})
public class DemoApplication {

    public static void main(String[] args) {
        System.setProperty("spring.cloud.bootstrap.enabled", "true");
        SpringApplication.run(DemoApplication.class, args);
    }

}
