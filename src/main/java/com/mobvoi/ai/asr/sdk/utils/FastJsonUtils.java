package com.mobvoi.ai.asr.sdk.utils;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.JSONObject;

/**
 * @Author: yangyiqian
 * @Description: //基于fastjson封装的json转换工具类sss
 * @Date: 14:46 2019/12/6
 * @Param:
 * @return:
 **/
public class FastJsonUtils {


    /**
     * 功能描述：把JSON数据转换成指定的java对象
     *
     * @param jsonData JSON数据
     * @param clazz    指定的java对象
     * @return 指定的java对象
     */
    public static <T> T getJsonToBean(String jsonData, Class<T> clazz) {
        return JSON.parseObject(jsonData, clazz);
    }

    /**
     * 功能描述：把java对象转换成JSON数据
     *
     * @param object java对象
     * @return JSON数据
     */
    public static String getBeanToJson(Object object) {
        return JSON.toJSONString(object);
    }

    /**
     * 功能描述：把JSON数据转换成指定的java对象列表
     *
     * @param jsonData JSON数据
     * @param clazz    指定的java对象
     * @return List<T>
     */
    public static <T> List<T> getJsonToList(String jsonData, Class<T> clazz) {
        return JSON.parseArray(jsonData, clazz);
    }


    /**
     * @Author: yangyiqian
     * @Description: //把JSON数据转换成较为复杂的List<Map<String, Object>>
     * @Date: 14:48 2019/12/6
     * @Param: [jsonData]JSON数据
     * @return: java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/
    public static List<Map<String, Object>> getJsonToListMap(String jsonData) {
        return JSON.parseObject(jsonData, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    /**
     * @Author: yangyiqian
     * @Description: //将json串转成json对象
     * @Date: 14:47 2019/12/6
     * @Param: [jsonData]
     * @return: org.json.JSONObject
     **/
    public static JSONObject toJSONObject(String jsonData) {
        return  JSONObject.parseObject(jsonData);
    }

}
