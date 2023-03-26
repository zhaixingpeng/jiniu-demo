package com.demo.nacos.auto.config;

import com.alibaba.nacos.api.config.ConfigChangeItem;
import com.alibaba.nacos.api.config.ConfigType;
import com.alibaba.nacos.api.config.PropertyChangeType;
import com.alibaba.nacos.api.config.annotation.NacosValue;
import com.alibaba.nacos.spring.context.event.config.NacosConfigReceivedEvent;
import com.alibaba.spring.beans.factory.annotation.AbstractAnnotationBeanPostProcessor;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


@Component
public class NacosConfigRefreshAnnotationPostProcess
        implements InstantiationAwareBeanPostProcessor, EnvironmentAware {

    private final Logger logger = Logger.getLogger(NacosConfigRefreshAnnotationPostProcess.class.getName());

    /**
     * nacos对应的propertySource的类名称
     */

    private static final String NACOS_PROPERTY_SOURCE_CLASS_NAME =
            "com.alibaba.nacos.spring.core.env.NacosPropertySource";

    @Autowired
    private StringEncryptor stringEncryptor;


    private static final String NACOS_CONFIG_TYPE = "nacos.config.type";


    private StandardEnvironment standardEnvironment;

    /**
     * 存放被@Value和@NacosValue修饰的属性 key = 占位符 value = 属性集合
     */

    private final static Map<String, List<FieldInstance>> placeholderValueTargetMap = new HashMap<>();

    /*
     *
     * 当前Nacos环境配置，第一次从环境对象中获取，后续变更后会被新的属性覆盖

     */

    private Map<String, Object> currentPlaceholderConfigMap = new ConcurrentHashMap<>();

    /*
     *
     * 类型转换服务

     */

    private ConversionService conversionService = new DefaultConversionService();

    /*public NacosConfigRefreshAnnotationPostProcess() {
        super(Value.class, NacosValue.class);
    }*/

    //@Override
    protected Object doGetInjectedBean(AnnotationAttributes annotationAttributes, Object o, String s, Class<?> aClass,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {
        // 注解占位符内容
        String placaholderKey = (String) annotationAttributes.get("value");
        // 解析嵌套占位符（只剩下最外层占位符）
        placaholderKey = PlaceholderUtils.parseStringValue(placaholderKey, standardEnvironment, null);
        // 剔除默认值后的占位符
        String key = PlaceholderUtils.getPlaceholderKey(placaholderKey);
        // 默认值
        String defaultValue = PlaceholderUtils.getPlaceholderDefaultValue(placaholderKey);

        Field field = (Field) injectedElement.getMember();
        // 属性记录到缓存中
        addFieldInstance(key, field, o);

        // 环境对象中当前属性
        String value = standardEnvironment.getProperty(key);
        // 环境对象不存在该值，就取默认值
        if (Objects.isNull(value)) {
            value = defaultValue;
        }
        // 将字符串类型转换为目标属性类型
        return conversionService.convert(value, field.getType());

    }

    //@Override
    protected String buildInjectedObjectCacheKey(AnnotationAttributes annotationAttributes, Object o, String s,
                                                 Class<?> clazz, InjectionMetadata.InjectedElement injectedElement) {
        return o.getClass().getName() + "#" + injectedElement.getMember().getName();
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.standardEnvironment = (StandardEnvironment) environment;
        for (PropertySource<?> propertySource : standardEnvironment.getPropertySources()) {
            // 筛选出nacos的配置
            if (propertySource.getClass().getName().equals(NACOS_PROPERTY_SOURCE_CLASS_NAME)) {
                MapPropertySource mapPropertySource = (MapPropertySource) propertySource;
                // 配置以键值对形式存储到当前属性配置集合中
                for (String propertyName : mapPropertySource.getPropertyNames()) {
                    currentPlaceholderConfigMap.put(propertyName, mapPropertySource.getProperty(propertyName));
                }
            }
        }
    }


    /*@Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        System.out.println("[TestInstantiationAwareBeanPostProcessor] before instantiation " + beanName);
        addFieldInstance(key, field, o);
        return null;
    }*/

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //System.out.println("[TestInstantiationAwareBeanPostProcessor] after initialization " + beanName);
        // 获取类的所有属性
        if (!bean.getClass().getPackage().getName().contains("com.demo.example.demo")) {
            return bean;
        }
        Field[] fields = bean.getClass().getDeclaredFields();

        for (Field field : fields) {
            Value value = field.getAnnotation(Value.class);
            if (Objects.isNull(value)) {
                continue;
            }
            String placaholderKey = value.value();
            // 解析嵌套占位符（只剩下最外层占位符）
            placaholderKey = PlaceholderUtils.parseStringValue(placaholderKey, standardEnvironment, null);

            // 剔除默认值后的占位符
            String key = PlaceholderUtils.getPlaceholderKey(placaholderKey);
            addFieldInstance(key, field, bean);
        }

        return bean;
    }

    private void refreshTargetObjectFieldValue(Map<String, Object> newConfigMap) {
        // 对比两次配置内容，筛选出变更后的配置项
        Map<String, ConfigChangeItem> configChangeItemMap =
                NacosConfigPaserUtils.filterChangeData(currentPlaceholderConfigMap, newConfigMap);

        // 反射给对象赋值
        for (String key : configChangeItemMap.keySet()) {
            // 没有用到的配置 直接跳过
            if (!placeholderValueTargetMap.containsKey(key)) {
                continue;
            }
            ConfigChangeItem item = configChangeItemMap.get(key);
            /*if(!item.getNewValue().contains("ENC(")){
                 continue;
            }*/
            // 删除配置不改变对象的值
            /*if (PropertyChangeType.DELETED.equals(item.getType())) {
                logger.info("Nacos-config-refresh-starter: deleted property [" + item.getKey() + "]");
                continue;
            }*/
            // 嵌套占位符 防止中途嵌套中的配置变了 导致对象属性刷新失败
            /*if (placeholderValueTargetMap.containsKey(item.getOldValue())) {
                List<FieldInstance> fieldInstances = placeholderValueTargetMap.get(item.getOldValue());
                placeholderValueTargetMap.put(item.getNewValue(), fieldInstances);
                placeholderValueTargetMap.remove(item.getOldValue());
            }*/
            updateFieldValue(key, item.getNewValue(), item.getOldValue());
        }
    }


    private Map<String, Object> parseNacosConfigContext(String newContent, String type) throws Exception {
        // 解析nacos推送的配置内容为键值对
        Map<String, Object> newConfigMap = new HashMap<>();
        if (ConfigType.YAML.getType().equals(type)) {
            newConfigMap = (new Yaml()).load(newContent);
        } else if (ConfigType.PROPERTIES.getType().equals(type)) {
            Properties newProps = new Properties();
            newProps.load(new StringReader(newContent));
            newConfigMap = new HashMap<>((Map) newProps);
        }
        // 筛选出正确的配置
        return NacosConfigPaserUtils.getFlattenedMap(newConfigMap);
    }

    //@Override
    public void onApplicationEvent(NacosConfigReceivedEvent event) {
        // 将配置内容解析成键值对
        Map<String, Object> newConfigMap = null;
        try {
            newConfigMap = parseNacosConfigContext(event.getContent(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 刷新变更对象的值
        refreshTargetObjectFieldValue(newConfigMap);
        // 当前配置指向最新的配置
        currentPlaceholderConfigMap = newConfigMap;
    }

    public void changeValue(String configInfo, String type) {
        // 将配置内容解析成键值对
        Map<String, Object> newConfigMap = null;
        try {
            newConfigMap = parseNacosConfigContext(configInfo, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 刷新变更对象的值
        refreshTargetObjectFieldValue(newConfigMap);
        // 当前配置指向最新的配置
        currentPlaceholderConfigMap = newConfigMap;
    }

    private static class FieldInstance {
        final Object bean;

        final Field field;

        public FieldInstance(Object bean, Field field) {
            this.bean = bean;
            this.field = field;
        }
    }


    private void addFieldInstance(String key, Field field, Object bean) {
        List<FieldInstance> fieldInstances = placeholderValueTargetMap.get(key);
        if (CollectionUtils.isEmpty(fieldInstances)) {
            fieldInstances = new ArrayList<>();
        }
        fieldInstances.add(new FieldInstance(bean, field));
        placeholderValueTargetMap.put(key, fieldInstances);
    }


    private void updateFieldValue(String key, String newValue, String oldValue) {
        List<FieldInstance> fieldInstances = placeholderValueTargetMap.get(key);
        if (newValue.toUpperCase().contains("ENC(")) {
            newValue = stringEncryptor.decrypt(newValue);
        }
        for (FieldInstance instance : fieldInstances) {
            try {
                ReflectionUtils.makeAccessible(instance.field);
                // 类型转换
                Object value = conversionService.convert(newValue, instance.field.getType());
                instance.field.set(instance.bean, value);
            } catch (Throwable e) {
                logger.warning("Can't update value of the " + instance.field.getName() + " (field) in "
                        + instance.bean.getClass().getSimpleName() + " (bean)");
            }
            logger.info("Nacos-config-refresh-starter: " + instance.bean.getClass().getSimpleName() + "#"
                    + instance.field.getName() + " field value changed from [" + oldValue + "] to [" + newValue + "]");
        }
    }
}
