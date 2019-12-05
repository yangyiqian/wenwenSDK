package com.mobvoi.ai.asr.sdk.utils;

import java.util.Random;

/**
 * @ClassName:RandomNumberUtil
 * @Description:随机数生成工具
 * @Author:yangyiqian
 * @DATE:2019/12/5 13:12
 * @version:1.0
 **/
public class RandomNumberUtil {

    public static Integer getRandomNumber(Integer maxNumber, Integer minNumber) {
        if (maxNumber == null) {
            maxNumber = 1000;
        }
        if (minNumber == null) {
            minNumber = 0;
        }
        Random random = new Random();
        return random.nextInt(maxNumber) % (maxNumber - minNumber + 1) + minNumber;
    }

    /**
    *
     * @Author: yangyiqian
     * @Description: //默认生成0~1000的随机数
     * @Date: 13:21 2019/12/5
     * @Param: []
     * @return: java.lang.Integer
     **/
    public static Integer getRandomNumber() {
        return  getRandomNumber(null,null);
    }
}
