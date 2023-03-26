package com.demo.nacos.auto.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;


/*@Configuration
@ConditionalOnProperty(prefix = "nacos.config", name = "auto-refresh", havingValue = "true")*/
public class NacosConfigRefreshAutoConfiguration {

    private final Logger logger = Logger.getLogger(NacosConfigRefreshAutoConfiguration.class.getName());

 /*@Bean
    public NacosConfigRefreshAnnotationPostProcess nacosRefreshAnnotationPostProcess() {
        logger.info("\n--------------------------------------------\nNacos-config-refresh-starter load successful" +
                "\n--------------------------------------------");
        return new NacosConfigRefreshAnnotationPostProcess();
    }*/

}
