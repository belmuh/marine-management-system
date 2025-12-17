package com.marine.management.modules.finance.infrastructure.loader;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

public class DataLoaderSharedData {

    private static final Map<String, Long> mainCategoryIdsByCode = new HashMap<>();

    public static void addMainCategory(String code, Long id) {
        mainCategoryIdsByCode.put(code, id);
    }

    public static Long getMainCategoryId(String code) {
        return mainCategoryIdsByCode.get(code);
    }
}
