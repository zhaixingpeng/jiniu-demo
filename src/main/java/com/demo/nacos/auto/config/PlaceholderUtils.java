package com.demo.nacos.auto.config;

import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;



public class PlaceholderUtils {



    private static final String PLACEHOLDER_PREFIX = "${";



    private static final String PLACEHOLDER_SIMPLE_PREFIX = "{";



    private static final String PLACEHOLDER_SUFFIX = "}";



    private static final String valueSeparator = ":";



    public static String parseStringValue(String value, Environment environment,
        @Nullable Set<String> visitedPlaceholders) {

        int startIndex = value.indexOf(PLACEHOLDER_PREFIX);
        if (startIndex == -1) {
            return value;
        }

        StringBuilder result = new StringBuilder(value);
        while (startIndex != -1) {
            int endIndex = findPlaceholderEndIndex(result, startIndex);
            if (endIndex != -1) {
                String placeholder = result.substring(startIndex + PLACEHOLDER_PREFIX.length(), endIndex);
                String originalPlaceholder = placeholder;
                if (visitedPlaceholders == null) {
                    visitedPlaceholders = new HashSet<>(4);
                }
                if (!visitedPlaceholders.add(originalPlaceholder)) {
                    throw new IllegalArgumentException(
                        "Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
                }
                // Recursive invocation, parsing placeholders contained in the placeholder key.
                placeholder = parseStringValue(placeholder, environment, visitedPlaceholders);
                // 跳过最外层占位符解析工作
                if (visitedPlaceholders.size() == 1) {
                    return placeholder;
                }
                // Now obtain the value for the fully resolved key...
                String propVal = environment.getProperty(placeholder);
                if (propVal == null && valueSeparator != null) {
                    int separatorIndex = placeholder.indexOf(valueSeparator);
                    if (separatorIndex != -1) {
                        String actualPlaceholder = placeholder.substring(0, separatorIndex);
                        String defaultValue = placeholder.substring(separatorIndex + valueSeparator.length());
                        propVal = environment.getProperty(actualPlaceholder);
                        if (propVal == null) {
                            propVal = defaultValue;
                        }
                    }
                }
                if (propVal != null) {
                    // Recursive invocation, parsing placeholders contained in the
                    // previously resolved placeholder value.
                    propVal = parseStringValue(propVal, environment, visitedPlaceholders);
                    result.replace(startIndex, endIndex + PLACEHOLDER_SUFFIX.length(), propVal);
                    startIndex = result.indexOf(PLACEHOLDER_PREFIX, startIndex + propVal.length());
                } else {
                    throw new IllegalArgumentException(
                        "Could not resolve placeholder '" + placeholder + "'" + " in value \"" + value + "\"");
                }
                visitedPlaceholders.remove(originalPlaceholder);
            } else {
                startIndex = -1;
            }
        }
        return result.toString();
    }

    private static int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        // ${长度
        int index = startIndex + PLACEHOLDER_PREFIX.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (StringUtils.substringMatch(buf, index, PLACEHOLDER_SUFFIX)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index = index + PLACEHOLDER_SUFFIX.length();
                } else {
                    return index;
                }
            } else if (StringUtils.substringMatch(buf, index, PLACEHOLDER_SIMPLE_PREFIX)) {
                withinNestedPlaceholder++;
                index = index + PLACEHOLDER_SIMPLE_PREFIX.length();
            } else {
                index++;
            }
        }
        return -1;
    }



    public static String getPlaceholderKey(String placeholder) {
        return placeholder.split(valueSeparator)[0];
    }


    public static String getPlaceholderDefaultValue(String placeholder) {
        if (placeholder.contains(valueSeparator)) {
            return placeholder.split(valueSeparator)[1];
        }
        return null;
    }
}
