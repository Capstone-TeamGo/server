package com.project.doongdoong.domain.analysis.service;

import com.project.doongdoong.domain.analysis.dto.*;
import com.project.doongdoong.domain.analysis.exception.AnalysisNotFoundException;
import com.project.doongdoong.domain.analysis.model.Analysis;
import com.project.doongdoong.domain.analysis.repository.AnalysisRepository;
import com.project.doongdoong.domain.question.model.Question;
import com.project.doongdoong.domain.question.model.QuestionContent;
import com.project.doongdoong.domain.question.repository.QuestionRepository;
import com.project.doongdoong.domain.question.service.QuestionService;
import com.project.doongdoong.domain.user.exeception.UserNotFoundException;
import com.project.doongdoong.domain.user.model.SocialType;
import com.project.doongdoong.domain.user.model.User;
import com.project.doongdoong.domain.user.repository.UserRepository;
import com.project.doongdoong.domain.voice.dto.response.VoiceDetailResponseDto;
import com.project.doongdoong.domain.voice.model.Voice;
import com.project.doongdoong.domain.voice.repository.VoiceRepository;
import com.project.doongdoong.domain.voice.service.VoiceService;
import com.project.doongdoong.global.util.GoogleTtsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service @Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisServiceImp implements AnalysisService{

    private final GoogleTtsProvider googleTtsProvider;
    private final VoiceRepository voiceRepository;
    private final UserRepository userRepository;
    private final AnalysisRepository analsisRepository;
    private final QuestionService questionService;
    private final VoiceService voiceService;
    private final static long WEEK_TIME = 60 * 60 * 24 * 7;

    private final static int ANALYSIS_PAGE_SIZE = 10;

    @Transactional
    @Override //        추가적으로 사용자 정보가 있어야 함.
    public AnalysisCreateResponseDto createAnalysis(String uniqueValue) {
        String[] values = parseUniqueValue(uniqueValue); // 사용자 정보 찾기
        User user = userRepository.findBySocialTypeAndSocialId(SocialType.customValueOf(values[1]), values[0])
                .orElseThrow(() -> new UserNotFoundException());

        List<Question> questions = questionService.createQuestions(); // 질문 가져오기
        Analysis analysis = Analysis.builder()
                .user(user)
                .questions(questions)
                .build();

        List<String> accessUrls = new ArrayList<>(); // 음성 파일 접근 url 리스트
        for(int i=0; i<questions.size(); i++){
            Question question = questions.get(i);
            question.connectAnalysis(analysis); // 연관관계 편의 메서드


            Optional<Voice> voice = voiceRepository.findVoiceByQuestionContent(question.getQuestionContent());
            if(voice.isPresent()){ // voiceRepository를 통해 이미 저장된 음성 파일이라면 TTS 과정 생략하고 음성 조회하기
                accessUrls.add(voice.get().getAccessUrl());
            }else { // 새로운 음성 파일 생성이라면 TTS 과정을 통해 음성 파일 저장하고 반환하기
                byte[] bytes = googleTtsProvider.convertTextToSpeech(question.getQuestionContent().getText());// 질문 내용 -> 음성 파일 변환
                String filename = "voice-question" + String.valueOf(question.getQuestionContent().getNumber());
                VoiceDetailResponseDto voiceDto = voiceService.saveTtsVoice(bytes, filename, question.getQuestionContent());
                accessUrls.add(voiceDto.getAccessUrl());
            }

        } // ConcurrentModificationException 으로 인해 for문 사용

        analsisRepository.save(analysis);

        return AnalysisCreateResponseDto.builder()
                .analysisId(analysis.getId())
                .accessUrls(accessUrls)
                .build();
    }

    private static String[] parseUniqueValue(String uniqueValue) {
        String[] values = uniqueValue.split("_"); // 사용자 찾기
        return values;
    }

    @Override
    public AnaylsisResponseDto getAnalysis(Long analysisId) {
        Analysis findAnalysis = analsisRepository.findById(analysisId).orElseThrow(() -> new AnalysisNotFoundException());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        return AnaylsisResponseDto.builder()
                .anaylisId(findAnalysis.getId())
                .time(findAnalysis.getCreatedTime().format(formatter))
                .feelingState(findAnalysis.getFeelingState())
                .questionContent(findAnalysis.getQuestions().stream()
                        .map(question -> question.getQuestionContent().getText())
                        .collect(Collectors.toList()))
                .answerContent(findAnalysis.getAnswers().stream()
                        .map(answer -> answer.getContent())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public AnaylsisListResponseDto getAnalysisList(String uniqueValue, int pageNumber) {
        String[] values = parseUniqueValue(uniqueValue);
        User user = userRepository.findBySocialTypeAndSocialId(SocialType.customValueOf(values[1]), values[0])
                .orElseThrow(() -> new UserNotFoundException());

        PageRequest pageable = PageRequest.of(pageNumber, ANALYSIS_PAGE_SIZE);
        Page<Analysis> analysisPages = analsisRepository.findAllByUserOrderByCreatedTime(user, pageable);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


        return AnaylsisListResponseDto.builder()
                .pageNumber(analysisPages.getNumber() + 1)
                .totalPage(analysisPages.getTotalPages())
                .anaylsisResponseDtoList(analysisPages.getContent().stream()
                        .map(analysis -> AnaylsisResponseDto.builder()
                                .anaylisId(analysis.getId())
                                .time(analysis.getCreatedTime().format(formatter))
                                .feelingState(analysis.getFeelingState())
                                .questionContent(analysis.getQuestions().stream()
                                        .map(a -> a.getQuestionContent().getText())
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public FeelingStateResponseListDto getAnalysisListGroupByDay(String uniqueValue) {
        String[] values = parseUniqueValue(uniqueValue);
        User user = userRepository.findBySocialTypeAndSocialId(SocialType.customValueOf(values[1]), values[0])
                .orElseThrow(() -> new UserNotFoundException());

        LocalDateTime endTime = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime startTime = endTime.minusDays(6).truncatedTo(ChronoUnit.DAYS);


        return FeelingStateResponseListDto.builder()
                .feelingStateResponsesDto(analsisRepository.findAllByDateBetween(user,startTime,endTime))
                .build();

    }


}
