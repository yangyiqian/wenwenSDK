package com.mobvoi.ai.asr.sdk;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.mobvoi.ai.asr.sdk.utils.*;
import com.mobvoi.speech.recognition.conference.v1.ConferenceSpeechGrpc;
import com.mobvoi.speech.recognition.conference.v1.ConferenceSpeechProto;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author qli <qli@mobvoi.com>
 * @date 2018/12/4
 */
@Slf4j
public class ConferenceSpeechClient {
    private final ManagedChannel channel;

    {
        channel = SingletonChannel.getInstance().getChannel();
        log.info("--------->>>>>" + channel.hashCode());
    }


    private ConferenceSpeechProto.ConferenceSpeechRequest setupRecognizeRequest(byte[] bytes) {
        ConferenceSpeechProto.ConferenceSpeechRequest.Builder builder = ConferenceSpeechProto.ConferenceSpeechRequest
                .newBuilder();
        builder.setAudioContent(ByteString.copyFrom(bytes));
        return builder.build();
    }

    private ConferenceSpeechProto.ConferenceSpeechRequest setupStreamingRecognizeRequestAsAudioContent(byte[] bytes) {
        ConferenceSpeechProto.ConferenceSpeechRequest.Builder builder = ConferenceSpeechProto.ConferenceSpeechRequest.newBuilder();
        builder.setAudioContent(ByteString.copyFrom(bytes));
        return builder.build();
    }

    // 此接口用作非实时语音识别, 由于音频文件可能会很大，所以采用流式发送的方法。[超时时间可调整]
    public boolean batchRecognize(String audioFile, ConferenceSpeechListener listener, Integer timeOut)
            throws IOException, UnsupportedAudioFileException {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        CountDownLatch latch = new CountDownLatch(1);
        ConferenceSpeechGrpc.ConferenceSpeechStub stub = ConferenceSpeechGrpc.newStub(channel);
        StreamObserver<ConferenceSpeechProto.ConferenceSpeechRequest> requestObserver = stub.recognize(listener.getRStreamObserver());

        try (FileInputStream fis = new FileInputStream(audioFile)) {
            int numBytes = 2100000;
            byte[] bytes = new byte[numBytes];
            int length;
            while ((length = fis.read(bytes, 0, numBytes)) > 0) {
                if (length == numBytes) {
                    requestObserver.onNext(setupStreamingRecognizeRequestAsAudioContent(bytes));
                } else {
                    requestObserver.onNext(setupStreamingRecognizeRequestAsAudioContent(ArrayUtils.subarray(bytes, 0, length)));
                }
                // Avoid busy looping.
                try {
                    Thread.sleep(1);
                } catch (java.lang.InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
        // Mark the end of requests
        if(StringUtils.isBlank(listener.getCallbackMessage().getCallBackJson())){
            requestObserver.onCompleted();
            ResultJsonUtil rju = new ResultJsonUtil();
            rju.setSuccess(1);
            rju.setMsg("语音转换正常");
            listener.getCallbackMessage().setCallBackJson(FastJsonUtils.getBeanToJson(rju));
        }

        // Receiving happens asynchronously
        ResultJsonUtil rjuTimeOut = new ResultJsonUtil();
        try {
            rjuTimeOut.setSuccess(0);
            if (timeOut != null) {
                if (!listener.getLatch().await(timeOut, TimeUnit.MINUTES)) {
                    rjuTimeOut.setMsg("语音识别超过"+timeOut +"分钟");
                    JSONObject jsobj =  FastJsonUtils.toJSONObject("{\"error\":{\"code\":\"WENWEN_SDK_TIMEOUT\",\"message\":超时"+timeOut+"分钟}}");
                    rjuTimeOut.setThirdJsonData(jsobj);
                    listener.getCallbackMessage().setCallBackJson(FastJsonUtils.getBeanToJson(rjuTimeOut));
                    log.warn("recognition can not finish within 4 hours");
                }
            } else {
                if (!listener.getLatch().await(240, TimeUnit.MINUTES)) {
                    rjuTimeOut.setMsg("语音识别超过240分钟");
                    JSONObject jsobj =  FastJsonUtils.toJSONObject("{\"error\":{\"code\":\"WENWEN_SDK_TIMEOUT\",\"message\":超时240分钟}}");
                    listener.getCallbackMessage().setCallBackJson(FastJsonUtils.getBeanToJson(rjuTimeOut));
                    log.warn("recognition can not finish within 240 Minutes");
                }
            }
        } catch (java.lang.InterruptedException e) {
            rjuTimeOut.setSuccess(0);
            rjuTimeOut.setMsg("语音识别出现中断异常");
            listener.getCallbackMessage().setCallBackJson(FastJsonUtils.getBeanToJson(rjuTimeOut));
            log.warn("recognition can not finish within 240 Minutes");
            e.printStackTrace();
        } catch (Exception e) {
            rjuTimeOut.setSuccess(0);
            rjuTimeOut.setMsg("语音识别出现未知异常");
            listener.getCallbackMessage().setCallBackJson(FastJsonUtils.getBeanToJson(rjuTimeOut));
            e.printStackTrace();
        }

        stopWatch.stop();
        long elapsed = stopWatch.getTime(TimeUnit.MILLISECONDS);
        log.info("Elapsed " + elapsed + "ms");
        return true;
    }


    // 此接口用作非实时语音识别, 由于音频文件可能会很大，所以采用流式发送的方法。[超时时间默认240分钟]
    public boolean batchRecognize(String audioFile, ConferenceSpeechListener listener)
            throws IOException, UnsupportedAudioFileException {
        return batchRecognize(audioFile, listener, null);
    }

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        ConferenceSpeechClient client = new ConferenceSpeechClient();
        ConferenceSpeechListener listener = new ConferenceSpeechListener("12345678", "sample.docx");
        //TODO 超时时间从数据库获取
        client.batchRecognize("D://月球挖矿讲座__7h57m.mp3", listener,120);
        log.info("=========================xxx>>>"+ listener.getCallbackMessage().getCallBackJson());

    }
}
