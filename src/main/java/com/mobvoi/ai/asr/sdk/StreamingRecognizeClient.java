// Copyright(c) 2018 Mobvoi Inc. All Rights Reserved.
package com.mobvoi.ai.asr.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import com.mobvoi.ai.asr.sdk.utils.CommonUtils;
import com.mobvoi.ai_commerce.speech.v1.SpeechGrpc;
import com.mobvoi.ai_commerce.speech.v1.SpeechProto;
import com.mobvoi.ai_commerce.speech.v1.SpeechProto.StreamingRecognizeResponse.SpeechEventType;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author qli <qli@mobvoi.com>
 * @date 2018/11/26
 */
@Slf4j
public class StreamingRecognizeClient {
  private final ManagedChannel channel;

  public StreamingRecognizeClient(String uri) {
    log.info("Set uri {} for StreamingRecognizeClient.", uri);
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

  private StreamObserver<SpeechProto.StreamingRecognizeResponse> setupResponseObserver(CountDownLatch latch) {
    StreamObserver<SpeechProto.StreamingRecognizeResponse> responseObserver = new StreamObserver<SpeechProto.StreamingRecognizeResponse>() {

      @Override
      public void onNext(SpeechProto.StreamingRecognizeResponse streamingRecognizeResponse) {
        if (SpeechEventType.LIVE_DECODING_END_OF_UTTERANCE.equals(streamingRecognizeResponse.getSpeechEventType())) {
          System.out.print(TextFormat.printToUnicodeString(streamingRecognizeResponse));
        } else if (SpeechEventType.END_OF_SINGLE_UTTERANCE.equals(streamingRecognizeResponse.getSpeechEventType())) {
          log.info(TextFormat.printToUnicodeString(streamingRecognizeResponse));
        } else {
          log.info(TextFormat.printToUnicodeString(streamingRecognizeResponse));
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
    return responseObserver;
  }

  private SpeechProto.StreamingRecognizeRequest setupStreamingRecognizeRequestAsConfig(int sampleRate,
                                                                                     boolean enableEndpointDetection,
                                                                                     boolean enablePartialResult,
                                                                                     boolean enableLiveDecoding,
                                                                                     float startSilence,
                                                                                     float endSilence) {
    SpeechProto.StreamingRecognizeRequest.Builder builder = SpeechProto.StreamingRecognizeRequest.newBuilder();
    builder.setStreamingConfig(setupStreamingRecognitionConfig(sampleRate, enableEndpointDetection,
        enablePartialResult, enableLiveDecoding, startSilence, endSilence));
    return builder.build();
  }

  private SpeechProto.StreamingRecognizeRequest setupStreamingRecognizeRequestAsAudioContent(byte[] bytes) {
    SpeechProto.StreamingRecognizeRequest.Builder builder = SpeechProto.StreamingRecognizeRequest.newBuilder();
    builder.setAudioContent(ByteString.copyFrom(bytes));
    return builder.build();
  }

  private SpeechProto.StreamingRecognitionConfig setupStreamingRecognitionConfig(int sampleRate,
                                                                               boolean enableEndpointDetection,
                                                                               boolean enablePartialResult,
                                                                               boolean enableLiveDecoding,
                                                                               float startSilence,
                                                                               float endSilence) {
    SpeechProto.StreamingRecognitionConfig.Builder builder = SpeechProto.StreamingRecognitionConfig.newBuilder();
    builder.setConfig(setupRecognitionConfig(sampleRate, enableLiveDecoding));
    builder.setEndpointDetection(enableEndpointDetection);
    builder.setPartialResult(enablePartialResult);
    builder.setEndpointConfig(setupEndpointConfig(startSilence, endSilence));
    return builder.build();
  }

  private SpeechProto.EndpointConfig setupEndpointConfig(float startSilence, float endSilence) {
    SpeechProto.EndpointConfig.Builder builder = SpeechProto.EndpointConfig.newBuilder();
    builder.setStartSilence(startSilence);
    builder.setEndSilence(endSilence);
    return builder.build();
  }

  private SpeechProto.RecognitionConfig setupRecognitionConfig(int sampleRate, boolean enableLiveDecoding) {
    SpeechProto.RecognitionConfig.Builder builder = SpeechProto.RecognitionConfig.newBuilder();
    builder.setEncoding(SpeechProto.RecognitionConfig.Encoding.WAV16);
    builder.setSampleRate(sampleRate);
    // streaming 模式只支持单声道
    builder.setChannel(1);
    builder.setEnableLiveDecoding(enableLiveDecoding);
    return builder.build();
  }

  /**
   * 将音频文件按10ms分割成一个个小的data block，放到一个queue当中，供识别使用
   * @param audioFile 待识别的音频文件
   * @param intervalMills 按每intervalMills毫秒发送intervalMills毫秒对等的语音数据
   * @return a pair with key = sample rate of the audio file, value = data blocks of the audio file segmented in intervalMills
   */
  private Pair<Integer, ConcurrentLinkedQueue<byte[]>> convertAudioFileToSimulatedDataQueue(String audioFile, int intervalMills) {
    ConcurrentLinkedQueue<byte[]> dataQueue = new ConcurrentLinkedQueue();
    try (AudioInputStream audioInputStream =
             AudioSystem.getAudioInputStream(new File(audioFile))) {
      AudioFormat audioFormat = audioInputStream.getFormat();
      byte[] bytes = new byte[(int) audioFormat.getSampleRate() / 1000 * 2 * intervalMills];
      while (true) {
        int length = audioInputStream.read(bytes);
        if (length == -1) {
          break;
        }
        dataQueue.add(Arrays.copyOfRange(bytes, 0, length));
        if (length < bytes.length) {
          break;
        }
      }
      return Pair.of((int) audioFormat.getSampleRate(), dataQueue);
    } catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * 此接口用于模拟会话中的流式语音识别，一般用于呼叫中心。
   * 客户端模拟Live Recognize，客户端发现有silence，则自动重新开启新的语音识别会话
   * @param audioFile
   * @param enableEndpointDetection
   * @param enablePartialResult
   * @param startSilence
   * @param endSilence
   * @throws InterruptedException
   */
  public void sessionRecognize(String audioFile,
                               boolean enableEndpointDetection,
                               boolean enablePartialResult,
                               float startSilence,
                               float endSilence) throws InterruptedException {
    // 准备模拟数据
    int intervalMills = 10;
    Pair<Integer, ConcurrentLinkedQueue<byte[]>> audioInfo =
        convertAudioFileToSimulatedDataQueue(audioFile, intervalMills);
    ConcurrentLinkedQueue<byte[]> dataQueue = audioInfo.getValue();
    int sampleRate = audioInfo.getKey();

    // 开始识别
    log.info("Run session recognize on " + audioFile);
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    while (!dataQueue.isEmpty()) {
      startRecognizeInternal(dataQueue, intervalMills, sampleRate, enableEndpointDetection, enablePartialResult, false,
          startSilence, endSilence);
    }
    stopWatch.stop();
    long elapsed = stopWatch.getTime(TimeUnit.MILLISECONDS);
    log.info("Elapsed " + elapsed + "ms");
  }

  private void startRecognizeInternal(ConcurrentLinkedQueue<byte[]> dataQueue,
                             int intervalMills,
                             int sampleRate,
                             boolean enableEndpointDetection,
                             boolean enablePartialResult,
                             boolean enableLiveDecoding,
                             float startSilence,
                             float endSilence) throws InterruptedException {
    CountDownLatch finishLatch = new CountDownLatch(1);
    StreamObserver<SpeechProto.StreamingRecognizeResponse> responseObserver = setupResponseObserver(finishLatch);
    SpeechGrpc.SpeechStub speechStub = SpeechGrpc.newStub(channel);
    StreamObserver<SpeechProto.StreamingRecognizeRequest> requestObserver = speechStub.streamingRecognize(responseObserver);
    requestObserver.onNext(setupStreamingRecognizeRequestAsConfig(sampleRate, enableEndpointDetection,
        enablePartialResult, enableLiveDecoding, startSilence, endSilence));

    while(!dataQueue.isEmpty() && finishLatch.getCount() != 0) {
      byte[] bytes = dataQueue.poll();
      requestObserver.onNext(setupStreamingRecognizeRequestAsAudioContent(bytes));
      Thread.sleep(intervalMills);
    }

    // Mark the end of requests
    requestObserver.onCompleted();

    // Receiving happens asynchronously
    if (!finishLatch.await(1, TimeUnit.MINUTES)) {
      log.warn("recognition can not finish within 1 minutes");
    }
  }

  private void dumpDataQueueToFile(ConcurrentLinkedQueue<byte[]> dataQueue, String outputFile) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    while (!dataQueue.isEmpty()) {
      bos.write(dataQueue.poll());
    }
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      bos.writeTo(fos);
    }
    log.info("Done");
  }

  private void streamingRecognizeInternal(String audioFile,
      boolean enableEndpointDetection,
      boolean enablePartialResult,
      boolean enableLiveDecoding,
      float startSilence,
      float endSilence) throws InterruptedException {
    // 准备模拟数据
    int intervalMills = 10;
    Pair<Integer, ConcurrentLinkedQueue<byte[]>> audioInfo = convertAudioFileToSimulatedDataQueue(audioFile, intervalMills);
    int sampleRate = audioInfo.getKey();
    ConcurrentLinkedQueue<byte[]> dataQueue = audioInfo.getValue();

    // 开始识别
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    startRecognizeInternal(dataQueue, intervalMills, sampleRate, enableEndpointDetection, enablePartialResult, enableLiveDecoding, startSilence, endSilence);
    stopWatch.stop();
    long elapsed = stopWatch.getTime(TimeUnit.MILLISECONDS);
    log.info("Elapsed " + elapsed + "ms");
  }

  /**
   * 此接口用作模拟实时语音识别
   * @param audioFile 待模拟实时语音识别的音频文件
   * @param enableEndpointDetection 开启自动静音识别
   * @param enablePartialResult 开启中间结果返回
   * @param startSilence 从开启语音识别，到startSilence时间内用户一直没有说话，则认为用户一直没说话，则停止语音识别
   * @param endSilence 用户在说完之后endSilence时间内没有说话，则认为用户说完话了，则停止语音识别
   * @throws InterruptedException
   */
  public void streamingRecognize(String audioFile,
                                 boolean enableEndpointDetection,
                                 boolean enablePartialResult,
                                 float startSilence,
                                 float endSilence) throws InterruptedException {
    streamingRecognizeInternal(audioFile, enableEndpointDetection, enablePartialResult, false, startSilence, endSilence);
  }

  /**
   * 此接口用于模拟会话中的流式语音识别，一般用于呼叫中心。
   * 服务器端模拟Live Recognize，服务端发现有silence，则自动重新开启新的语音识别会话
   * @param audioFile
   * @param enableEndpointDetection
   * @param enablePartialResult
   * @param startSilence
   * @param endSilence
   * @throws InterruptedException
   */
  public void liveRecognize(String audioFile,
                            boolean enableEndpointDetection,
                            boolean enablePartialResult,
                            float startSilence,
                            float endSilence) throws InterruptedException {
    streamingRecognizeInternal(audioFile, enableEndpointDetection, enablePartialResult, true, startSilence, endSilence);
  }
}
