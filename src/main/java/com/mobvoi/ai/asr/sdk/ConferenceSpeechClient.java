package com.mobvoi.ai.asr.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import com.mobvoi.ai.asr.sdk.utils.CommonUtils;
import com.mobvoi.ai.asr.sdk.utils.DocUtils;
import com.mobvoi.ai_commerce.speech.v1.SpeechGrpc;
import com.mobvoi.ai_commerce.speech.v1.SpeechProto;
import com.mobvoi.speech.recognition.conference.v1.ConferenceSpeechGrpc;
import com.mobvoi.speech.recognition.conference.v1.ConferenceSpeechProto;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author qli <qli@mobvoi.com>
 * @date 2018/12/4
 */
@Slf4j
public class ConferenceSpeechClient {
  private final ManagedChannel channel;

  public ConferenceSpeechClient(String uri) {
    //log.info("Set uri {} for BatchRecognizeClient.", uri);
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

  private ConferenceSpeechProto.ConferenceSpeechRequest setupRecognizeRequest(byte[] bytes) {
    ConferenceSpeechProto.ConferenceSpeechRequest.Builder builder = ConferenceSpeechProto.ConferenceSpeechRequest
        .newBuilder();
    builder.setAudioContent(ByteString.copyFrom(bytes));
    return builder.build();
  }

  // TODO(业务方): 业务方可以根据需要修改该函数来对接其系统
  private StreamObserver<ConferenceSpeechProto.ConferenceSpeechResponse> setupResponseObserver(String audioInfo, CountDownLatch latch, String outputDocFilePath) {
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
          String conclusion = String.format("Current docoding progress: decoded wav time %s, total wav time %s, progress %s", 
              decodedWavTime, totalWavTime, decodedWavTime / totalWavTime);
          System.out.println(conclusion);
        }
      }

      @Override
      public void onError(Throwable t) {
        //log.error(t.getMessage());
        latch.countDown();
      }

      @Override
      public void onCompleted() {
        //log.info("complete asr call");
        latch.countDown();
      }
    };
  }

  private ConferenceSpeechProto.ConferenceSpeechRequest setupStreamingRecognizeRequestAsAudioContent(byte[] bytes) {
    ConferenceSpeechProto.ConferenceSpeechRequest.Builder builder = ConferenceSpeechProto.ConferenceSpeechRequest.newBuilder();
    builder.setAudioContent(ByteString.copyFrom(bytes));
    return builder.build();
  }

  // 此接口用作非实时语音识别, 由于音频文件可能会很大，所以采用流式发送的方法。
  public boolean batchRecognize(String audioFile, String outputWordFileName, String audioInfo)
      throws IOException, UnsupportedAudioFileException {

    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    CountDownLatch latch = new CountDownLatch(1);
    ConferenceSpeechGrpc.ConferenceSpeechStub stub = ConferenceSpeechGrpc.newStub(channel);
    StreamObserver<ConferenceSpeechProto.ConferenceSpeechResponse> responseObserver = setupResponseObserver(audioInfo, latch, outputWordFileName);
    StreamObserver<ConferenceSpeechProto.ConferenceSpeechRequest> requestObserver = stub.recognize(responseObserver);
    
    try (FileInputStream fis = new FileInputStream(audioFile)) {
      int numBytes = 160;
      byte [] bytes = new byte[numBytes];
      int length;
      while ((length = fis.read(bytes, 0, numBytes)) > 0) {
        if (length == numBytes) {
          requestObserver.onNext(setupStreamingRecognizeRequestAsAudioContent(bytes));
        } else {
          requestObserver.onNext(setupStreamingRecognizeRequestAsAudioContent(ArrayUtils.subarray(bytes, 0, length)));
        }
      }
      // Avoid busy looping.
      try {
        Thread.sleep(1);
      } catch (java.lang.InterruptedException e) {
        e.printStackTrace();
      }
      
    }

    // Mark the end of requests
    requestObserver.onCompleted();

    // Receiving happens asynchronously
    try {    
      if (!latch.await(4, TimeUnit.HOURS)) {
        //log.warn("recognition can not finish within 4 hours");
      }
    } catch (java.lang.InterruptedException e) {
      e.printStackTrace();
    }

    stopWatch.stop();
    long elapsed = stopWatch.getTime(TimeUnit.MILLISECONDS);
    System.out.println("Elapsed " + elapsed + "ms");
    return true;
  }

  public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
    ConferenceSpeechClient client = new ConferenceSpeechClient("0.0.0.0:8080");
    client.batchRecognize("/Users/qli/Documents/出门问问技术ToB/盛科维/1-写给云-低质量1.amr", "sample.docx", "customized info");
  }
}
