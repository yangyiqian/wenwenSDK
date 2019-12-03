package com.mobvoi.ai.asr.sdk.utils;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

/**
 * @Author: yangyiqian
 * @Description: //单例Chanel,用于客户端获取到当前的Channel
 * @Date: 13:34 2019/11/29
 * @Param:
 * @return:
 **/
@Slf4j
public class SingletonChannel {
    private ManagedChannel channel = null;

    public SingletonChannel(String uri) {

        log.info("Set uri {} for BatchRecognizeClient.", uri);
        Pair<String, Integer> hostAndPort = CommonUtils.parseHostIp(uri);
        // 为简化code，暂时不处理hostAndPort为null的情况
        channel = ManagedChannelBuilder
                .forAddress(hostAndPort.getKey(), hostAndPort.getValue())
                .maxInboundMessageSize(Integer.MAX_VALUE)
                .enableRetry()
                .maxRetryAttempts(3)
                .usePlaintext()
                .build();

    }

    private static class SingletonHolder {
        //获取语音识别服务器地址
        final static String uri = PropertiesLoader.getString("speechRecServer");
        //单例channel
        public final static SingletonChannel instance = new SingletonChannel(uri);
    }

    public static SingletonChannel getInstance() {
        return SingletonHolder.instance;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public static Logger getLog() {
        return log;
    }
}