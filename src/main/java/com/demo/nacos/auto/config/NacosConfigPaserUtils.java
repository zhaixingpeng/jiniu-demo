package com.demo.nacos.auto.config;

import com.alibaba.nacos.api.config.ConfigChangeItem;
import com.alibaba.nacos.api.config.PropertyChangeType;
import com.alibaba.nacos.common.utils.StringUtils;

import java.util.*;


public class NacosConfigPaserUtils {

    public static Map<String, ConfigChangeItem> filterChangeData(Map oldMap, Map newMap) {
        Map<String, ConfigChangeItem> result = new HashMap<>(16);
        for (Iterator<Map.Entry<String, Object>> entryItr = oldMap.entrySet().iterator(); entryItr.hasNext();) {
            Map.Entry<String, Object> e = entryItr.next();
            ConfigChangeItem cci = null;
            if (newMap.containsKey(e.getKey())) {
                if (e.getValue().equals(newMap.get(e.getKey()))) {
                    continue;
                }
                cci = new ConfigChangeItem(e.getKey(), e.getValue().toString(), newMap.get(e.getKey()).toString());
                cci.setType(PropertyChangeType.MODIFIED);
                if (Objects.equals(cci.getNewValue(), cci.getOldValue())) {
                    continue;
                }
            } else {
                cci = new ConfigChangeItem(e.getKey(), e.getValue().toString(), null);
                cci.setType(PropertyChangeType.DELETED);
            }
            result.put(e.getKey(), cci);
        }

        for (Iterator<Map.Entry<String, Object>> entryItr = newMap.entrySet().iterator(); entryItr.hasNext();) {
            Map.Entry<String, Object> e = entryItr.next();
            if (!oldMap.containsKey(e.getKey())) {
                ConfigChangeItem cci = new ConfigChangeItem(e.getKey(), null, e.getValue().toString());
                cci.setType(PropertyChangeType.ADDED);
                result.put(e.getKey(), cci);
            }
        }

        return result;
    }



    public static final Map<String, Object> getFlattenedMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>(128);
        buildFlattenedMap(result, source, null);
        return result;
    }



    private static void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
        for (Iterator<Map.Entry<String, Object>> itr = source.entrySet().iterator(); itr.hasNext();) {
            Map.Entry<String, Object> e = itr.next();
            String key = e.getKey();
            if (StringUtils.isNotBlank(path)) {
                if (e.getKey().startsWith("[")) {
                    key = path + key;
                } else {
                    key = path + '.' + key;
                }
            }
            if (e.getValue() instanceof String) {
                result.put(key, e.getValue());
            } else if (e.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>)e.getValue();
                buildFlattenedMap(result, map, key);
            } else if (e.getValue() instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<Object> collection = (Collection<Object>)e.getValue();
                if (collection.isEmpty()) {
                    result.put(key, "");
                } else {
                    int count = 0;
                    for (Object object : collection) {
                        buildFlattenedMap(result, Collections.singletonMap("[" + (count++) + "]", object), key);
                    }
                }
            } else {
                result.put(key, (e.getValue() != null ? e.getValue() : ""));
            }
        }
    }
}
