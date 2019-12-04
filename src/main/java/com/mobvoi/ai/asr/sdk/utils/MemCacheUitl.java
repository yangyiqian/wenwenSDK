package com.mobvoi.ai.asr.sdk.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单例模式MemCacheUitl(双重检查加锁)
 * 内存cache
 *
 * @author 杨嶷岍
 */
public class MemCacheUitl {

    private volatile static MemCacheUitl uniqueInstance = null;
    private Map<String, Object> cacheMap = null;

    private MemCacheUitl() {
        cacheMap = new ConcurrentHashMap<String, Object>();
    }

    public static MemCacheUitl getInstance() {
        if (uniqueInstance == null) {
            synchronized (MemCacheUitl.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new MemCacheUitl();
                }
            }
        }
        return uniqueInstance;
    }

    private Map getCache() {
        return cacheMap;
    }

    public static void put(String key, Object obj) {
        getInstance().getCache().put(key, obj);
    }

    public static Boolean containsKey(String key) {
        return getInstance().getCache().containsKey(key);
    }

    public static Object get(String key) {
        Map tMap = getInstance().getCache();
        return tMap.get(key);
    }

    public static Integer del(String key) {
        Map<String, Object> m = getInstance().getCache();
        if (m.containsKey(key)) {
            m.remove(key);
            return 1;
        } else {
            return -1;
        }
    }
}
