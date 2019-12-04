package com.mobvoi.ai.asr.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import com.mobvoi.ai.asr.sdk.utils.CommonUtils;
import com.mobvoi.ai.asr.sdk.utils.ConferenceSpeechListener;
import com.mobvoi.ai.asr.sdk.utils.DocUtils;
import com.mobvoi.ai.asr.sdk.utils.SingletonChannel;
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

  {
    channel =SingletonChannel.getInstance().getChannel();
    log.info("--------->>>>>"+channel.hashCode());
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

  // 此接口用作非实时语音识别, 由于音频文件可能会很大，所以采用流式发送的方法。
  public boolean batchRecognize(String audioFile, ConferenceSpeechListener listener)
      throws IOException, UnsupportedAudioFileException {

    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    CountDownLatch latch = new CountDownLatch(1);
    ConferenceSpeechGrpc.ConferenceSpeechStub stub = ConferenceSpeechGrpc.newStub(channel);
    StreamObserver<ConferenceSpeechProto.ConferenceSpeechRequest> requestObserver = stub.recognize(listener.getRStreamObserver());
    
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
      if (!listener.getLatch().await(4, TimeUnit.HOURS)) {
        log.warn("recognition can not finish within 4 hours");
      }
    } catch (java.lang.InterruptedException e) {
      e.printStackTrace();
    }

    stopWatch.stop();
    long elapsed = stopWatch.getTime(TimeUnit.MILLISECONDS);
    log.info("Elapsed " + elapsed + "ms");
    return true;
  }

  public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
    ConferenceSpeechClient client = new ConferenceSpeechClient();
    ConferenceSpeechListener listener = new ConferenceSpeechListener("audio id", "sample.docx");
    client.batchRecognize("D://1-写给云-低质量1.amr", listener);


      ConferenceSpeechClient client2 = new ConferenceSpeechClient();
      ConferenceSpeechListener listener2 = new ConferenceSpeechListener("audio id", "sample123.docx");
      client2.batchRecognize("D://1-写给云-低质量1.amr", listener2);
  }
}
