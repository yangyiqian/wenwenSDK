// Copyright(c) 2018 Mobvoi Inc. All Rights Reserved.
package com.mobvoi.ai.asr.sdk.utils;

import org.apache.commons.lang3.tuple.Pair;

/**
 * @author qli <qli@mobvoi.com>
 * @date 2019/3/19
 */
public class CommonUtils {

  /**
   * 解析127.0.0.1:8000格式的URI为一个pair，
   * pair的key为ip地址，
   * pair的value为端口号
   * @param uri 127.0.0.1:8000
   * @return <"127.0.0.1", 8000>
   */
  public static Pair<String, Integer> parseHostIp(String uri) {
    if (uri == null || uri.isEmpty()) {
      return null;
    }
    String[] segs = uri.split(":");
    if (segs.length != 2) {
      return null;
    }
    String host = segs[0];
    int port = Integer.valueOf(segs[1]);
    return Pair.of(host, port);
  }
}
