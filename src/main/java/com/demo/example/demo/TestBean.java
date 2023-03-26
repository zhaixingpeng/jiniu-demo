package com.demo.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

@Service
public class TestBean {


    @PreDestroy
    public void testDestroy(){
        System.out.println("------destroy------");
    }

}
