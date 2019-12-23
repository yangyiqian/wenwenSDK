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
    public boolean batchRecognize(String audioFile, ConferenceSpeechListener listener)
            throws IOException, UnsupportedAudioFileException {

        ConferenceSpeechGrpc.ConferenceSpeechStub stub = ConferenceSpeechGrpc.newStub(channel).withDeadlineAfter(listener.getTimeOutInMinutes(), TimeUnit.MINUTES);
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
        requestObserver.onCompleted();
        return true;
    }

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException, InterruptedException {
        ConferenceSpeechClient client = new ConferenceSpeechClient();
        long timeoutInMinutes = 10;
        ConferenceSpeechListener listener = new ConferenceSpeechListener("12345678", "sample.docx", timeoutInMinutes);
        //TODO 超时时间从数据库获取
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        client.batchRecognize("/Users/qli/Desktop/180 1860 2866_20191215162834_01_01.wav", listener);
        listener.getLatch().await(4, TimeUnit.HOURS);  // 等待程序结束
        log.info("=========================xxx>>>"+ listener.getCallbackMessage().getCallBackJson());
        stopWatch.stop();
        long elapsed = stopWatch.getTime(TimeUnit.MILLISECONDS);
        log.info("Elapsed " + elapsed + "ms");
    }
}
