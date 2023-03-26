/*
package com.demo.nacos.auto.config;


import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

*/
/**
 * 实现springcloud应用@Value配置的自动刷新
 *
 * @author lwc
 *//*


@Configuration
@SuppressWarnings("ALL")
@ConditionalOnClass({ContextRefresher.class, RefreshScope.class, EnvironmentChangeEvent.class})
public class SpringValueAutoRefreshConfiguration {

    public static final String PROCESSOR_BEAN_NAME = "com.rutron.framework.conf.SpringValueAutoRefreshProcessor";

    @Role(ROLE_INFRASTRUCTURE)
    @Bean(PROCESSOR_BEAN_NAME)
    public BeanPostProcessor springValueAutoRefreshProcessor() {
        return new SpringValueAutoRefreshProcessor();
    }

}*/
