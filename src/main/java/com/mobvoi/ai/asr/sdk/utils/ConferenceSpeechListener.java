// Copyright(c) 2018 Mobvoi Inc. All Rights Reserved.
package com.mobvoi.ai.asr.sdk.utils;

import java.util.concurrent.CountDownLatch;

import com.google.protobuf.TextFormat;
import com.mobvoi.speech.recognition.conference.v1.ConferenceSpeechGrpc;
import com.mobvoi.speech.recognition.conference.v1.ConferenceSpeechProto;

import io.grpc.stub.StreamObserver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

// TODO(业务方): 业务方可以根据需要修改该函数来对接其系统
// 一个语音文件一个listener
@Slf4j
@Data
public class ConferenceSpeechListener {

    // TODO(业务方)：这个latch是为了让demo可以等待结果并及早结束，业务方可以根据需求保留或者删除，使用别的方式。
    private final CountDownLatch latch = new CountDownLatch(1);
    private String audioId;
    private String outputDocFilePath;
    private StreamObserver<ConferenceSpeechProto.ConferenceSpeechResponse> rStreamObserver;
    private float decodingProgress = 0;

    public ConferenceSpeechListener(String audioId, String outputDocFilePath) {
        this.audioId = audioId;
        this.outputDocFilePath = outputDocFilePath;
        this.rStreamObserver = setupResponseObserver(outputDocFilePath);
    }

    private StreamObserver<ConferenceSpeechProto.ConferenceSpeechResponse> setupResponseObserver(String outputDocFilePath) {
        ConferenceSpeechListener tSpeechListener = this;
        return new StreamObserver<ConferenceSpeechProto.ConferenceSpeechResponse>() {
            @Override
            public void onNext(ConferenceSpeechProto.ConferenceSpeechResponse response) {
                if (response.hasError() && !ConferenceSpeechProto.Error.Code.OK.equals(response.getError().getCode())) {
                    // TODO(业务方): 业务方可以根据conference.proto中定义的error进行处理
                    System.out.println("Error met " + TextFormat.printToUnicodeString(response));
                    latch.countDown();
                }
                if (ConferenceSpeechProto.ConferenceSpeechResponse.ConferenceSpeechEventType.CONFERENCE_SPEECH_EOS
                        .equals(response.getSpeechEventType())) {
                    String finalTranscript = response.getResult().getTranscript();
                    try {
                        DocUtils.toWord(finalTranscript, outputDocFilePath);
                    } catch (Exception e) {
                        System.err.println("Failed to write final transcript to word file with content \n" + finalTranscript);
                    }
                    latch.countDown();
                } else {
                    float decodedWavTime = response.getResult().getDecodedWavTime();
                    float totalWavTime = response.getResult().getTotalWavTime();
                    tSpeechListener.setDecodingProgress(decodedWavTime / totalWavTime);
                    String conclusion = String.format("Current docoding progress: decoded wav time %s, total wav time %s, progress %s",
                            decodedWavTime, totalWavTime, tSpeechListener.getDecodingProgress());
                    System.out.println(conclusion);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error(t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("complete asr call");
                latch.countDown();
            }
        };
    }
}