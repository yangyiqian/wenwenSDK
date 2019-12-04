package com.mobvoi.ai.asr.sdk.utils;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @Author: yangyiqian
 * @Description: //封装Properties操作，通过统一配置文件，加载分散的.properties文件
 * @Date: 19:59 2019/8/3
 * @Param:
 * @return:
 **/
public class PropertiesLoader {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PropertiesLoader.class.getName());
    private volatile static PropertiesLoader uniqueInstance = null;
    private static final CompositeConfiguration config = new CompositeConfiguration();

    /**
     * @Author: yangyiqian
     * @Description: //PropertiesLoader构造器，以config.thinkwin.properties为入口，加载Properties文件
     * @Date: 16:46 2019/8/3
     * @Param: []
     * @return:
     **/
    private PropertiesLoader() {
        config.addConfiguration(new SystemConfiguration());
        try {
            config.addConfiguration(new PropertiesConfiguration("config.thinkwin.properties"));
            String[] propFiles = config.getStringArray("properties.file.list");
            for (int i = 0; i < propFiles.length; i++)
                config.addConfiguration(new PropertiesConfiguration(propFiles[i]));
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 功能:功能获取PropertiesLoader实例 取默认属性文件名
     * auther:yyq
     * date:2019.03.16
     *
     * @return
     */
    public static PropertiesLoader getInstance() {
        if (uniqueInstance == null) {
            synchronized (PropertiesLoader.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new PropertiesLoader();
                }
            }
        }
        return uniqueInstance;
    }

    public static String getString(String key) {
        return getInstance().config.getString(key);
    }

    public static String getString(String key, String defaultValue) {
        return getInstance().config.getString(key, defaultValue);
    }

    public static int getInt(String key) {
        return getInstance().config.getInt(key);
    }

    public static int getInt(String key, int defaultValue) {
        return getInstance().config.getInt(key, defaultValue);
    }

    public static Integer getInteger(String key, Integer defaultValue) {
        return getInstance().config.getInteger(key, defaultValue);
    }

    public static long getLong(String key) {
        return getInstance().config.getLong(key);
    }

    public static long getLong(String key, long defaultValue) {
        return getInstance().config.getLong(key, defaultValue);
    }

    public static Long getLong(String key, Long defaultValue) {
        return getInstance().config.getLong(key, defaultValue);
    }

    public static String[] getStringArray(String key) {
        return getInstance().config.getStringArray(key);
    }

    public static List getList(String key) {
        return getInstance().config.getList(key);
    }

    public static float getFloat(String key) {
        return getInstance().config.getFloat(key);
    }

    /**
     * 获取float类型数值，如果为空，则返回缺省值
     **/
    public static float getFloat(String key, float defaultValue) {
        return getInstance().config.getFloat(key, defaultValue);
    }

    /**
     * 获取Float类型数值，如果为空，则返回缺省值
     **/
    public static Float getFloat(String key, Float defaultValue) {
        return getInstance().config.getFloat(key, defaultValue);
    }

    public static double getDouble(String key) {
        return getInstance().config.getDouble(key);
    }

    /**
     * 获取double类型数值，如果为空，则返回缺省值
     **/
    public static double getDouble(String key, double defaultValue) {
        return getInstance().config.getDouble(key, defaultValue);
    }

    /**
     * 获取Double类型数值，如果为空，则返回缺省值
     **/
    public static Double getDouble(String key, Double defaultValue) {
        return getInstance().config.getDouble(key, defaultValue);
    }

    /**
     * @Author: yangyiqian
     * @Description: //map 转Properties
     * @Date: 20:48 2019/8/3
     * @Param: [map]
     * @return: java.util.Properties
     **/
    public static Properties fromMap(Map map) {
        if ((map instanceof Properties)) {
            return (Properties) map;
        }

        Properties props = new Properties();
        Iterator itr = map.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry entry = (Map.Entry) itr.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            if (value != null) {
                props.setProperty(key, value);
            }
        }
        return props;
    }

    /**
     * @Author: yangyiqian
     * @Description: //Properties copy
     * @Date: 20:48 2019/8/3
     * @Param: [from, to]
     * @return: void
     **/
    public static void copyProperties(Properties from, Properties to) {
        Iterator itr = from.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry entry = (Map.Entry) itr.next();
            to.setProperty((String) entry.getKey(), (String) entry.getValue());
        }
    }

    /**
     * @Author: yangyiqian
     * @Description: //从Properties copy 到map
     * @Date: 20:55 2019/8/3
     * @Param: [props, map]
     * @return: void
     **/
    public static void fromProperties(Properties props, Map map) {
        map.clear();
        Iterator itr = props.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry entry = (Map.Entry) itr.next();
            map.put(entry.getKey(), entry.getValue());
        }
    }


    public static Properties load(String str) throws IOException {
        Properties props = new Properties();
        load(props, str);
        return props;
    }

    public static void load(Properties props, String str) throws IOException {
        if (StringUtils.isNotBlank(str)) {
            str = UnicodeUtils.stringToUnicode(str);
            str = StringUtils.replace(str, "\\u003d", "=");
            str = StringUtils.replace(str, "\\u000a", "\n");
            str = StringUtils.replace(str, "\\u0021", "!");
            str = StringUtils.replace(str, "\\u0023", "#");
            str = StringUtils.replace(str, "\\u0020", " ");
            str = StringUtils.replace(str, "\\u005c", "\\");

            props.load(new ByteArrayInputStream(str.getBytes()));

            List propertyNames = Collections.list(props.propertyNames());

            for (int i = 0; i < propertyNames.size(); i++) {
                String key = (String) propertyNames.get(i);
                String value = props.getProperty(key);

                if (value != null) {
                    value = value.trim();
                    props.setProperty(key, value);
                }
            }
        }
    }
}
