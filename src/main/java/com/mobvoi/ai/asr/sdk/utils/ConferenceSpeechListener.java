// Copyright(c) 2018 Mobvoi Inc. All Rights Reserved.
package com.mobvoi.ai.asr.sdk.utils;

import com.mobvoi.speech.recognition.conference.v1.ConferenceSpeechProto;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.concurrent.CountDownLatch;

// TODO(业务方): 业务方可以根据需要修改该函数来对接其系统
// 一个语音文件一个listener
@Slf4j
@Data
public class ConferenceSpeechListener {

    // TODO(业务方)：这个latch是为了让demo可以等待结果并及早结束，业务方可以根据需求保留或者删除，使用别的方式。
    private final CountDownLatch latch = new CountDownLatch(1);
    private String audioId;
    private String outputDocFilePath;
    private long timeOutInMinutes;
    private StreamObserver<ConferenceSpeechProto.ConferenceSpeechResponse> rStreamObserver;
    private float decodingProgress = 0;
    /**
     * 用于保存语音识别转换后的状态信息(成功 or 失败 json)
     **/
    private CallBackMessage callbackMessage = new CallBackMessage();


    /**
     * json串返回，需要外接变量
     */
    public ConferenceSpeechListener(String audioId, String outputDocFilePath, long timeOutInMinutes) {
        this.audioId = audioId;
        this.outputDocFilePath = outputDocFilePath;
        this.rStreamObserver = setupResponseObserver(outputDocFilePath);
        this.timeOutInMinutes = timeOutInMinutes;
    }

    public CallBackMessage getCallbackMessage() {
        return callbackMessage;
    }

    public void setCallbackMessage(CallBackMessage callbackMessage) {
        this.callbackMessage = callbackMessage;
    }

    //
    private StreamObserver<ConferenceSpeechProto.ConferenceSpeechResponse> setupResponseObserver(String outputDocFilePath) {
        ConferenceSpeechListener tSpeechListener = this;
        return new StreamObserver<ConferenceSpeechProto.ConferenceSpeechResponse>() {
            @Override
            public void onNext(ConferenceSpeechProto.ConferenceSpeechResponse response) {
                if (response.hasError() && !ConferenceSpeechProto.Error.Code.OK.equals(response.getError().getCode())) {
                    // TODO(业务方): 业务方可以根据conference.proto中定义的error进行处理
                    //log.info("Error met " + TextFormat.printToUnicodeString(response));
                    try {
                        ResultJsonUtil rju = new ResultJsonUtil();
                        rju.setSuccess(0);
                        rju.setMsg("语音转换出现异常");
                        JSONObject jsobj =  FastJsonUtils.toJSONObject(ProtoJsonUtils.toJson(response));
                        rju.setThirdJsonData(jsobj);
                        callbackMessage.setCallBackJson(FastJsonUtils.getBeanToJson(rju));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }finally {
                        //输出json异常信息
                        log.info(callbackMessage.getCallBackJson());
                        latch.countDown();
                        return;
                    }
                }
                if (ConferenceSpeechProto.ConferenceSpeechResponse.ConferenceSpeechEventType.CONFERENCE_SPEECH_EOS
                        .equals(response.getSpeechEventType())) {
                    String finalTranscript = response.getResult().getTranscript();
                    try {
                        DocUtils.toWord(finalTranscript, outputDocFilePath);
                        ResultJsonUtil rju = new ResultJsonUtil();
                        rju.setSuccess(1);
                        rju.setMsg("语音转换正常");
                        JSONObject jsobj =  FastJsonUtils.toJSONObject(ProtoJsonUtils.toJson(response));
                        rju.setThirdJsonData(jsobj);
                        callbackMessage.setCallBackJson(FastJsonUtils.getBeanToJson(rju));
                    } catch (IOException e) {
                        log.error("语音转换发生IOException");
                        e.printStackTrace();
                    }catch (Exception e) {
                        log.error("Failed to write final transcript to word file with content \n" + finalTranscript);
                    }finally {
                        latch.countDown();
                        return;
                    }
                } else {
                    float decodedWavTime = response.getResult().getDecodedWavTime();
                    float totalWavTime = response.getResult().getTotalWavTime();
                    tSpeechListener.setDecodingProgress(decodedWavTime / totalWavTime);
                    DecimalFormat speechRecFormat = new DecimalFormat("0.00");
                    String progressStr = speechRecFormat.format(tSpeechListener.getDecodingProgress());
                    String conclusion = String.format("Current docoding progress: decoded wav time %s, total wav time %s, progress %s",
                            decodedWavTime, totalWavTime,progressStr);
                    String audioPrefix = PropertiesLoader.getString("speechRecCacheFilePrefix");
                    //保存音频进度
                    MemCacheUitl.put(audioPrefix + audioId, progressStr);
                    //保存音频总时长
                    MemCacheUitl.put(audioPrefix + audioId+"_totalWavTime", new BigDecimal(totalWavTime).toPlainString());
                    log.info("------------>>>>" + (String) MemCacheUitl.get(audioPrefix + audioId));
                    log.info(conclusion);
                }
            }

            // Grpc error code 可以参考https://grpc.io/docs/guides/error/
            @Override
            public void onError(Throwable t) {
                try {
                    final Status status = Status.fromThrowable(t);
                    String message = "语音转换出现系统级异常";
                    String code = "WENWEN_SYSTEM_ERROR";
                    if (Status.DEADLINE_EXCEEDED.getCode().equals(status.getCode())) {
                        message = "语音识别超过"+ timeOutInMinutes +"分钟";
                        code = "WENWEN_SDK_TIMEOUT";
                    }
                    ResultJsonUtil rju = new ResultJsonUtil();
                    rju.setSuccess(0);
                    rju.setMsg(message);
                    JSONObject jsobj =  FastJsonUtils.toJSONObject("{\"error\":{\"code\":\"" + code + "\",\"message\":\""+t.getMessage()+"\"}}");
                    rju.setThirdJsonData(jsobj);
                    String errJson=FastJsonUtils.getBeanToJson(rju);
                    callbackMessage.setCallBackJson(errJson);
                    log.error(errJson);
                } catch (Exception e) {
                    log.error("问问SDK onError 方法出现异常");
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onCompleted() {
                log.info("complete asr call");
                latch.countDown();
            }
        };
    }
}