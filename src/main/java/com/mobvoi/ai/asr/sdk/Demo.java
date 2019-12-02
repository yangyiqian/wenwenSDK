// Copyright(c) 2018 Mobvoi Inc. All Rights Reserved.
package com.mobvoi.ai.asr.sdk;

import com.mobvoi.ai.asr.sdk.utils.ConferenceSpeechListener;
import com.mobvoi.ai_commerce.speech.v1.SpeechProto;
import org.apache.commons.cli.*;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * @author qli <qli@mobvoi.com>
 * @date 2019/3/19
 */
public class Demo {

  public static void main(String[] args) throws InterruptedException, IOException, UnsupportedAudioFileException {
    Options options = new Options();

    Option wavfileOpt = new Option("wav", "wav", true, "input wav file path");
    Option grpcUriOpt = new Option("grpc_uri", "remote asr uri in grpc protocol in form of 127.0.0.1:8000",
        true, "remote asr uri in grpc protocol, contact solutions@mobvoi.com to get one.");
    Option httpUriOpt = new Option("http_uri", "remote asr uri in http protocol", true,
        "remote asr uri in http protocol, contact solutions@mobvoi.com to get one.");
    Option modeOpt = new Option("mode", "mode", true,"In which mode you would like to experience the asr, either " +
        "BatchRecognize or BatchHTTPRecognize or StreamingRecognize or SessionRecognize or ConferenceSpeechRecognize");
    wavfileOpt.setRequired(true);
    modeOpt.setRequired(true);
    options.addOption(wavfileOpt).addOption(grpcUriOpt).addOption(httpUriOpt).addOption(modeOpt);


    CommandLine cmd = null;
    try {
      CommandLineParser parser = new BasicParser();
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("utility-name", options);
      System.exit(1);
    }

    String wavfilePath = cmd.getOptionValue("wav");

    if (!new File(wavfilePath).exists()) {
      System.out.println("wav is not found in the specified path: <" + wavfilePath + "> use test wav in resources instead");
      System.exit(-1);
    }

    String mode = cmd.getOptionValue("mode");
    String httpUri = cmd.getOptionValue("http_uri");
    String grpcUri = cmd.getOptionValue("grpc_uri");

    if ("BatchHTTPRecognize".equals(mode)) {
      if (httpUri.isEmpty()) {
        System.out.println("You should specify http_uri to use BatchHTTPRecognize");
        System.exit(-1);
      }
      BatchRecognizeHttpClient client = new BatchRecognizeHttpClient();
      boolean res = client.batchRecognize(httpUri, wavfilePath, "");
      if (res) {
        System.out.println("rtf: " + client.GetRtf());
        System.out.println("result: " + client.GetText());
      } else {
        System.out.println("recognize failure!");
      }
      System.exit(1);
    }

    if ("BatchRecognize".equals(mode)) {
      System.out.println("Run BatchRecognize on " + wavfilePath);
      if (grpcUri.isEmpty()) {
        System.out.println("You should specify grpc_uri to use BatchRecognize");
        System.exit(-1);
      }
      BatchRecognizeClient client = new BatchRecognizeClient(grpcUri);
      client.batchRecognize(wavfilePath, SpeechProto.RecognitionConfig.DiarizationMode.SEGMENT_AND_DIARIZATION);
      client.shutdown();
      System.exit(1);
    }

    if ("StreamingRecognize".equals(mode)) {
      if (grpcUri.isEmpty()) {
        System.out.println("You should specify grpc_uri to use StreamingRecognize");
        System.exit(-1);
      }
      StreamingRecognizeClient client = new StreamingRecognizeClient(grpcUri);
      client.streamingRecognize(wavfilePath, true, true, 0, 1);
      client.shutdown();
      System.exit(1);
    }

    if ("SessionRecognize".equals(mode)) {
      if (grpcUri.isEmpty()) {
        System.out.println("You should specify grpc_uri to use SessionRecognize");
        System.exit(-1);
      }

      StreamingRecognizeClient client = new StreamingRecognizeClient(grpcUri);
      client.sessionRecognize(wavfilePath, true, true, 0, 1);
      client.shutdown();
      System.exit(1);
    }

    if ("LiveRecognize".equals(mode)) {
      if (grpcUri.isEmpty()) {
        System.out.println("You should specify grpc_uri to use LiveRecognize");
        System.exit(-1);
      }
      StreamingRecognizeClient client = new StreamingRecognizeClient(grpcUri);
      client.liveRecognize(wavfilePath, false, true, 0, 1);
      client.shutdown();
      System.exit(1);
    }

    if ("ConferenceSpeechRecognize".equals(mode)) {
      if (grpcUri.isEmpty()) {
        System.out.println("You should specify grpc_uri to use ConferenceSpeechRecognize");
        System.exit(-1);
      }
      ConferenceSpeechClient client = new ConferenceSpeechClient(grpcUri);
      ConferenceSpeechListener listener = new ConferenceSpeechListener("audio id", "sample.docx");
      client.batchRecognize(wavfilePath, listener);
      System.out.println("Recognized transcripts is written to sample.docx");
      System.exit(1);
    }
    System.out.println("Unrecognize mode " + mode);
  }
}
