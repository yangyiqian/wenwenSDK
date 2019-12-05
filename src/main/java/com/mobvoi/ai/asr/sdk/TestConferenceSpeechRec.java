package com.mobvoi.ai.asr.sdk;

import com.mobvoi.ai.asr.sdk.utils.ConferenceSpeechListener;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

public class TestConferenceSpeechRec {

	public void start(){


		for (int i = 0; i < 10; i++) {
			new Thread(){
				@Override
				public void run(){

					try {
						ConferenceSpeechClient client = new ConferenceSpeechClient();
						ConferenceSpeechListener listener = new ConferenceSpeechListener("12345678", "sample.docx");
						client.batchRecognize("D://1-写给云-低质量1.amr", listener);
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
