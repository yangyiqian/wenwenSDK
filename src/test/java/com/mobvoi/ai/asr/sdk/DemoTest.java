// Copyright(c) 2018 Mobvoi Inc. All Rights Reserved.
package com.mobvoi.ai.asr.sdk;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author qli <qli@mobvoi.com>
 * @date 2019/3/19
 */
public class DemoTest {
  // @Test
  // 需要测试并发的时候，开启该测试
  public void loadTest() throws Exception {
    String wavfilePath = "src/main/resources/test.wav";
    String grpcUri = "127.0.0.1:8000";
    int nThreads = 1;
    StreamingRecognizeClient client = new StreamingRecognizeClient(grpcUri);
    ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
    CountDownLatch finishLatch = new CountDownLatch(nThreads);
    for (int i = 0; i < nThreads; ++i) {
      executorService.execute(() -> {
        try {
          client.sessionRecognize(wavfilePath, true, false, 0, 1);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        finishLatch.countDown();
      });
    }
    finishLatch.await(10, TimeUnit.MINUTES);
    client.shutdown();
  }
}