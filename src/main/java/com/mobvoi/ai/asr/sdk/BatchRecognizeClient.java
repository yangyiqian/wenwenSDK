package com.mobvoi.ai.asr.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import com.mobvoi.ai.asr.sdk.utils.CommonUtils;
import com.mobvoi.ai_commerce.speech.v1.SpeechGrpc;
import com.mobvoi.ai_commerce.speech.v1.SpeechProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;

/**
 * @author qli <qli@mobvoi.com>
 * @date 2018/12/4
 */
@Slf4j
public class BatchRecognizeClient {
    private final ManagedChannel channel;

    public BatchRecognizeClient(String uri) {
        log.info("Set uri {} for BatchRecognizeClient.", uri);
        Pair<String, Integer> hostAndPort = CommonUtils.parseHostIp(uri);
        // 为简化code，暂时不处理hostAndPort为null的情况
        channel = ManagedChannelBuilder.forAddress(hostAndPort.getKey(), hostAndPort.getValue()).usePlaintext().build();
    }

    public synchronized void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Override
    protected void finalize() {
        try {
            shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private SpeechProto.RecognizeRequest setupRecognizeRequest(int sampleRate, int numChannel,
                                                               SpeechProto.RecognitionConfig.DiarizationMode diarizationMode,
                                                               byte[] bytes) {
        SpeechProto.RecognizeRequest.Builder builder = SpeechProto.RecognizeRequest.newBuilder();
        builder.setConfig(setupRecognitionConfig(sampleRate, numChannel, diarizationMode));
        builder.setAudio(SpeechProto.RecognitionAudio.newBuilder().setContent(ByteString.copyFrom(bytes)));
        return builder.build();
    }

    private SpeechProto.RecognitionConfig setupRecognitionConfig(int sampleRate, int numChannel,
                                                                 SpeechProto.RecognitionConfig.DiarizationMode diarizationMode) {
        SpeechProto.RecognitionConfig.Builder builder = SpeechProto.RecognitionConfig.newBuilder();
        builder.setEncoding(SpeechProto.RecognitionConfig.Encoding.WAV16);
        builder.setSampleRate(sampleRate);
        builder.setChannel(numChannel);
        builder.setDiarizationMode(diarizationMode);
        return builder.build();
    }

    // 此接口用作离线语音识别，支持话者分离
    public void batchRecognize(String audioFile, SpeechProto.RecognitionConfig.DiarizationMode diarizationMode)
            throws IOException, UnsupportedAudioFileException {
        AudioInputStream audioInputStream =
                AudioSystem.getAudioInputStream(new File(audioFile));
        AudioFormat audioFormat = audioInputStream.getFormat();
        byte[] bytes = new byte[audioInputStream.available()];
        audioInputStream.read(bytes);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        SpeechGrpc.SpeechBlockingStub stub = SpeechGrpc.newBlockingStub(channel);
        SpeechProto.RecognizeResponse response = stub.recognize(
                setupRecognizeRequest((int) audioFormat.getSampleRate(), audioFormat.getChannels(), diarizationMode, bytes));
        stopWatch.stop();
        long elapsed = stopWatch.getTime(TimeUnit.MILLISECONDS);
        System.out.println("Elapsed " + elapsed + "ms");
        System.out.println(TextFormat.printToUnicodeString(response));
    }
}
