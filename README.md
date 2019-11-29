# 安装

1. 编译：mvn package

2. 调用Batch Recognizer, 具体的接口参数参考proto/speech.proto: java -cp target/mrcp-grpc-1.0-SNAPSHOT.jar com.mobvoi.ai.asr.sdk.Demo -mode BatchRecognize -wav src/main/resources/test.wav -grpc_uri 127.0.0.1:8000 

3. 调用Streaming Recognizer, 具体的接口参数参考proto/speech.proto: java -cp target/mrcp-grpc-1.0-SNAPSHOT.jar com.mobvoi.ai.asr.sdk.Demo -mode StreamingRecognize -wav src/main/resources/test.wav -grpc_uri 127.0.0.1:8000

4. 电话信道中按session做流式语音识别调用Session Recognizer, 具体的接口参数参考proto/speech.proto: java -cp target/mrcp-grpc-1.0-SNAPSHOT.jar com.mobvoi.ai.asr.sdk.Demo -mode SessionRecognize -wav src/main/resources/test.wav -grpc_uri 127.0.0.1:8000

5. 调用基于HTTP的Batch Recognizer, 具体的接口参数参考proto/speech.proto: java -cp target/mrcp-grpc-1.0-SNAPSHOT.jar com.mobvoi.ai.asr.sdk.Demo -mode BatchHTTPRecognize -wav src/main/resources/test.wav -http_uri http://127.0.0.1:8000/http/asr

6. 调用Live Recognizer, 做实时无间断语音识别，需要客户端主动发起EndRecognize命令, 具体的接口参数参考proto/speech.proto 具体实现参考随附源代码: java -cp target/mrcp-grpc-1.0-SNAPSHOT.jar com.mobvoi.ai.asr.sdk.Demo -mode LiveRecognize -wav src/main/resources/test.wav -grpc_uri 127.0.0.1:8000

7. 调用Conference Speech Recognizer, 做和CM对接的批处理语音识别, 具体的接口参数参考proto/conference.proto
java -cp target/mrcp-grpc-1.0-SNAPSHOT.jar com.mobvoi.ai.asr.sdk.Demo -mode ConferenceSpeechRecognize -wav src/main/resources/test.wav  -grpc_uri 0.0.0.0:8080

# API接口文档

0. 可将该sdk源代码将入到您的项目当中，具体调用方式根据您的需求来选择BatchRecognizeClient或StreamingRecognizeClient，来进行您的定制化修改

1. 概述
    ```
    本协议作为 ASR 语音识别
    通信协议为： gRPC / HTTP
    gRPC请求与数据格式由 speech.proto 文件确定
    ```
2. 接口要求

    2.1. 接口格式 
    ```
    接口地址：联系服务提供方获取
    ```
    2.2. 音频文件格式
    ```
    采样率: 16k, 8k
    比特率: 16bit
    编码格式: wav
    只有BatchRecognize, BatchHTTPRecognize模式支持双声道, 其余只支持单声道
    ```
    2.3. 接口参数说明
    ```
    grpc_uri: asr grpc 服务器地址，字符串，取值为ip:host，例如127.0.0.1:8000
    http_uri: asr http 服务器地址，字符串，例如http://127.0.0.1:8000/http/asr
    wav: 待测试的音频文件
    mode: 接口模式，可选项有BatchRecognize，StreamingRecognize，SessionRecognize，BatchHTTPRecognize
    ```
    2.4. 接口示例代码
    ```
    本目录下的src/main/java/com/mobvoi/ai/asr/sdk/*.java是我们为java使用者所写的示例代码，使用方法如下
    java -cp target/mrcp-grpc-1.0-SNAPSHOT.jar com.mobvoi.ai.asr.sdk.Demo -mode BatchRecognize -wav src/main/resources/test.wav -grpc_uri 127.0.0.1:8000 
    java -cp target/mrcp-grpc-1.0-SNAPSHOT.jar com.mobvoi.ai.asr.sdk.Demo -mode StreamingRecognize -wav src/main/resources/test.wav -grpc_uri 127.0.0.1:8000
    java -cp target/mrcp-grpc-1.0-SNAPSHOT.jar com.mobvoi.ai.asr.sdk.Demo -mode SessionRecognize -wav src/main/resources/test.wav -grpc_uri 127.0.0.1:8000
    java -cp target/mrcp-grpc-1.0-SNAPSHOT.jar com.mobvoi.ai.asr.sdk.Demo -mode BatchHTTPRecognize -wav src/main/resources/test.wav -http_uri http://127.0.0.1:8000/http/asr
    java -cp target/mrcp-grpc-1.0-SNAPSHOT.jar com.mobvoi.ai.asr.sdk.Demo -mode LiveRecognize -wav src/main/resources/test.wav -grpc_uri 127.0.0.1:8000
    java -cp target/mrcp-grpc-1.0-SNAPSHOT.jar com.mobvoi.ai.asr.sdk.Demo -mode ConferenceSpeechRecognize -wav src/main/resources/test.wav  -grpc_uri 10.1.205.126:8080
    ```

# wenwenSDK
问问对接Sdk client 为方便对接双方对SDK的修改，采用github方式合作开发。

