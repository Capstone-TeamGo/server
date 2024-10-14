package com.project.doongdoong.global.util;

import com.project.doongdoong.domain.question.model.QuestionContent;
import com.project.doongdoong.domain.voice.model.Voice;
import com.project.doongdoong.domain.voice.repository.VoiceRepository;
import com.project.doongdoong.domain.voice.service.VoiceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

// @Component // 결제 문제로 일단 사용 X
@Slf4j
@RequiredArgsConstructor
@Profile("!test")
public class InitProvider {

    private final GoogleTtsProvider googleTtsProvider;
    private final VoiceRepository voiceRepository;
    private final VoiceService voiceService;

    private final static String VOICE_QUESTION = "voice-question";

    @PostConstruct
    public void initQuestionVoiceFiles(){

        for (QuestionContent questionContent : QuestionContent.values()) {
            Optional<Voice> existingVoice = voiceRepository.findVoiceByQuestionContent(questionContent);
            if (!existingVoice.isPresent()) {
                byte[] audioContent = googleTtsProvider.convertTextToSpeech(questionContent.getText());
                String filename = VOICE_QUESTION + questionContent.getNumber();
                voiceService.saveTtsVoice(audioContent, filename, questionContent);
                log.info("Voice for question {} created and saved.", questionContent.getNumber());
            }
        }
    }
}
