package com.mobvoi.ai.asr.sdk;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

/**
 * @author xiaokai.yang <xiaokai.yang@mobvoi.com>
 * @date 2019/2/14
 */
@Slf4j
public class BatchRecognizeHttpClient {
    private String audioType = "audio/pcm";
    private String text = "";
    private float rtf = 0;

    public String GetText() {
        return text;
    }

    public float GetRtf() {
        return rtf;
    }

    // 此接口用作离线语音识别，支持话者分离
    public boolean batchRecognize(String url, String filePath, String queryContext) throws IOException {
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(new File(filePath));
        } catch (UnsupportedAudioFileException e) {
            log.info(e.toString());
            return false;
        }
        AudioFormat audioFormat = audioInputStream.getFormat();
        byte[] bytes = new byte[audioInputStream.available()];
        audioInputStream.read(bytes);

        OkHttpClient client = new OkHttpClient.Builder().build();

        RequestBody body = RequestBody.create(MediaType.parse(audioType), bytes);
        Headers headers = new Headers.Builder()
                .add("Content-Type", audioType)
                .add("Content-Length", String.valueOf(bytes.length))
                .add("sample-rate", String.valueOf((int) audioFormat.getSampleRate()))
                .add("asr-context", queryContext)
                .add("channel-number", String.valueOf(audioFormat.getChannels()))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(headers)
                .build();
        try (Response response = client.newCall(request).execute()) {
            JSONObject jsonResponse = new JSONObject(response.body().string());
            int errCode = jsonResponse.getInt("error_code");
            float rtf = jsonResponse.getFloat("rtf");
            String status = jsonResponse.getString("status");
            String text = jsonResponse.getString("text");
            if (errCode == 0) {
                this.text = text;
                this.rtf = rtf;
                return true;
            } else {
                log.info("response error: error code: " + errCode);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
