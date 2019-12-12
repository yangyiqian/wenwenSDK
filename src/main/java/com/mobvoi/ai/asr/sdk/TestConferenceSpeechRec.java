package com.mobvoi.ai.asr.sdk;

import com.mobvoi.ai.asr.sdk.utils.CallBackMessage;
import com.mobvoi.ai.asr.sdk.utils.ConferenceSpeechListener;
import com.mobvoi.ai.asr.sdk.utils.RandomNumberUtil;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
@Slf4j
public class TestConferenceSpeechRec {

    public void start() {
        for (int i = 0; i < 2; i++) {
            new Thread() {
                @Override
                public void run() {
                    Integer randomNumber = RandomNumberUtil.getRandomNumber();

                    try {
                        ConferenceSpeechClient client = new ConferenceSpeechClient();
                        ConferenceSpeechListener listener = new ConferenceSpeechListener("12345678" + randomNumber, "sample" + randomNumber + ".docx");
                        client.batchRecognize("D://1-写给云-低质量1.amr", listener);
                        log.info("=========================xxx>>>"+ listener.getCallbackMessage().getCallBackJson());
                    } catch (UnsupportedAudioFileException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }.start();
        }
    }

    public static void main(String[] args) {
        new TestConferenceSpeechRec().start();

        System.out.println("222");
    }

}
