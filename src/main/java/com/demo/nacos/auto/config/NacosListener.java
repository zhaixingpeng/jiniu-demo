/*
package com.demo.nacos.auto.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.nacos.api.config.listener.Listener;
import com.demo.example.demo.TestController;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.concurrent.Executor;

@RefreshScope
@Configuration
@Order
public class NacosListener implements InitializingBean {

    @Value("${spring.application.name}")
    private String appName;
    @Autowired
    private NacosConfigManager nacosConfigManager;
    @Autowired
    private NacosConfigProperties configProperties;

//    @NacosConfigListener(dataId = "${spring.application.name}.yaml")
//    public void onMessage(String config) {
//        System.out.println();
//    }

    @Autowired
    private TestController testController;


    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("----------");
        nacosConfigManager.getConfigService().addListener(appName + "-test.yaml", configProperties.getGroup(),
                new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        //cloud.logger.level  cloud.logger.package
                        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                        loggerContext.getLogger("root").setLevel(Level.toLevel(""));
                        System.out.println();
                        testController.settestUser("yhyjgjkj");
                    }
                });
    }
}
*/
