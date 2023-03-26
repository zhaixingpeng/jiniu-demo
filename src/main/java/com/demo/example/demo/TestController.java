package com.demo.example.demo;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RefreshScope
public class TestController {

    @Value("${testzxp}")
    private String testUser;

    @Autowired
    private StringEncryptor stringEncryptor;

    @RequestMapping("/test")
    @ResponseBody
    public Object chargeCallback() throws Exception {
        Thread.sleep(10000l);
       System.out.println("--------zzz-----------");
       Map<String,String> zz=new HashMap<>();
        zz.put("12","12");
       return zz ;
    }


    @RequestMapping("/test/user")
    public String testUser() {
        return testUser;
    }

    public void  settestUser(String test) {
        testUser=test;
    }


}
