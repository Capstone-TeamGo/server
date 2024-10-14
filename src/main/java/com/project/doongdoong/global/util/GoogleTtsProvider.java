package com.project.doongdoong.global.util;

import com.google.cloud.texttospeech.v1.*;
import com.project.doongdoong.global.exception.GoogleTtsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component @Slf4j
public class GoogleTtsProvider {


    public byte[] convertTextToSpeech(String text){
        log.info("시작");
        byte[] audioContent = null;
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()){ // TTS API 생성
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build(); // 전달할 텍스트 설정
            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder() // 요청할 음성 형식 지정
                    .setLanguageCode("ko-KR") // 한국어 설정
                    .setSsmlGender(SsmlVoiceGender.NEUTRAL) // 음성 성별 중립
                    .build();
            AudioConfig audioConfig = AudioConfig.newBuilder() // 음성 파일 확장자 설정
                    .setAudioEncoding(AudioEncoding.MP3)
                    .build();
            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig); // TTS API 결과로 전달받은 reponse
            audioContent = response.getAudioContent().toByteArray(); // 바이트로 변환
        } catch (IOException e) {
            throw new GoogleTtsException();
        }
        return audioContent;
    }
}
