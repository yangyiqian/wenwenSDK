package com.mobvoi.ai.asr.sdk.utils;

import com.google.gson.Gson;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import java.io.IOException;

/**
 * @Author: yangyiqian
 * @Description: //protobuf、json互转工具
 * @Date: 11:26 2019/12/5
 * @Param:
 * @return:
 **/
public class ProtoJsonUtils {

    /**
     * @Author: yangyiqian
     * @Description: //Message 实例或者子类转json
     * @Date: 11:27 2019/12/5
     * @Param: [sourceMessage]
     * @return: java.lang.String
     **/
    public static String toJson(Message sourceMessage)
            throws IOException {
        String json = JsonFormat.printer().print(sourceMessage);
        return json;
    }

    /**
    *
     * @Author: yangyiqian
     * @Description: //Json 转 protobuf Message
     * @Date: 11:28 2019/12/5
     * @Param: [targetBuilder, json]
     * @return: com.google.protobuf.Message
     **/
    public static Message toProtoBean(Message.Builder targetBuilder, String json) throws IOException {
        JsonFormat.parser().merge(json, targetBuilder);
        return targetBuilder.build();
    }
}
