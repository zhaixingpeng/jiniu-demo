package com.demo.nacos.auto.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.cloud.nacos.NacosPropertySourceRepository;
import com.alibaba.cloud.nacos.client.NacosPropertySource;
import com.alibaba.cloud.nacos.refresh.NacosRefreshHistory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractSharedListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.demo.example.demo.TestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * On application start up, NacosContextRefresher add nacos listeners to all application
 * level dataIds, when there is a change in the data, listeners will refresh
 * configurations.
 *
 * @author juven.xuxb
 * @author pbting
 * @author freeman
 */

@Component
public class NacosContextListen
        implements ApplicationListener<ApplicationReadyEvent>, ApplicationContextAware {

    private final static Logger log = LoggerFactory
            .getLogger(com.alibaba.cloud.nacos.refresh.NacosContextRefresher.class);

    private static final AtomicLong REFRESH_COUNT = new AtomicLong(0);

    private NacosConfigProperties nacosConfigProperties;

    private final boolean isRefreshEnabled;

    private final NacosRefreshHistory nacosRefreshHistory;

    private final ConfigService configService;

    private ApplicationContext applicationContext;

    private AtomicBoolean ready = new AtomicBoolean(false);

    private Map<String, Listener> listenerMap = new ConcurrentHashMap<>();

    @Autowired
    private  NacosConfigRefreshAnnotationPostProcess nacosConfigRefreshAnnotationPostProcess;



    public NacosContextListen(NacosConfigManager nacosConfigManager,
                                 NacosRefreshHistory refreshHistory) {
        this.nacosConfigProperties = nacosConfigManager.getNacosConfigProperties();
        this.nacosRefreshHistory = refreshHistory;
        this.configService = nacosConfigManager.getConfigService();
        this.isRefreshEnabled = this.nacosConfigProperties.isRefreshEnabled();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // many Spring context
        if (this.ready.compareAndSet(false, true)) {
            this.registerNacosListenersForApplications();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * register Nacos Listeners.
     */
    private void registerNacosListenersForApplications() {
        if (isRefreshEnabled()) {
            for (NacosPropertySource propertySource : NacosPropertySourceRepository
                    .getAll()) {
                if (!propertySource.isRefreshable()) {
                    continue;
                }
                String dataId = propertySource.getDataId();
                registerNacosListener(propertySource.getGroup(), dataId);
                log.info("listening config: dataId={}, group={}", dataId, propertySource.getGroup());
            }
        }
    }

    private void registerNacosListener(final String groupKey, final String dataKey) {
        String key = NacosPropertySourceRepository.getMapKey(dataKey, groupKey);
        Listener listener = listenerMap.computeIfAbsent(key,
                lst -> new AbstractSharedListener() {
                    @Override
                    public void innerReceive(String dataId, String group,
                                             String configInfo) {
                        new Thread(){
                            public void run(){
                                try {
                                    Thread.sleep(10000l);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                                log.info("--------:"+configInfo);
                                nacosConfigRefreshAnnotationPostProcess.changeValue(configInfo,nacosConfigProperties.getFileExtension());
                            }
                        }.start();

                        //nacosConfigRefreshAnnotationPostProcess.changeValue(configInfo,nacosConfigProperties.getFileExtension());
                    }
                });
        try {
            configService.addListener(dataKey, groupKey, listener);
        }
        catch (NacosException e) {
            log.warn(String.format(
                    "register fail for nacos listener ,dataId=[%s],group=[%s]", dataKey,
                    groupKey), e);
        }
    }

    public NacosConfigProperties getNacosConfigProperties() {
        return nacosConfigProperties;
    }

    public NacosContextListen setNacosConfigProperties(
            NacosConfigProperties nacosConfigProperties) {
        this.nacosConfigProperties = nacosConfigProperties;
        return this;
    }

    public boolean isRefreshEnabled() {
        if (null == nacosConfigProperties) {
            return isRefreshEnabled;
        }
        // Compatible with older configurations
        if (nacosConfigProperties.isRefreshEnabled() && !isRefreshEnabled) {
            return false;
        }
        return isRefreshEnabled;
    }

    public static long getRefreshCount() {
        return REFRESH_COUNT.get();
    }

    public static void refreshCountIncrement() {
        REFRESH_COUNT.incrementAndGet();
    }

}
