package com.mobvoi.ai.asr.sdk.utils;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import com.alibaba.fastjson.JSONObject;

/**
 * @ClassName:CallBackResult
 * @Description:问问方面返回消息处理
 * @Author:yangyiqian
 * @DATE:2019/12/5 14:31
 * @version:1.0
 **/
@Getter
@Setter
public class ResultJsonUtil {
    /**
     * 语音识别状态
     **/
    private Integer success;
    /**
     * 语音识别消息
     **/
    private String msg;
    /**
     * 语音识别三方信息
     */
    private JSONObject thirdJsonData;
}
