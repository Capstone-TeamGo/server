package com.project.doongdoong.domain.analysis.service;

import com.project.doongdoong.domain.analysis.dto.response.*;
import com.project.doongdoong.domain.analysis.exception.AllAnswersNotFoundException;
import com.project.doongdoong.domain.analysis.exception.AlreadyAnalyzedException;
import com.project.doongdoong.domain.analysis.exception.AnalysisNotFoundException;
import com.project.doongdoong.domain.analysis.model.Analysis;
import com.project.doongdoong.domain.analysis.repository.AnalysisRepository;
import com.project.doongdoong.domain.answer.model.Answer;
import com.project.doongdoong.domain.question.model.Question;
import com.project.doongdoong.domain.question.model.QuestionContent;
import com.project.doongdoong.domain.question.service.QuestionService;
import com.project.doongdoong.domain.user.exeception.UserNotFoundException;
import com.project.doongdoong.domain.user.model.SocialType;
import com.project.doongdoong.domain.user.model.User;
import com.project.doongdoong.domain.user.repository.UserRepository;
import com.project.doongdoong.domain.voice.model.Voice;
import com.project.doongdoong.domain.voice.repository.VoiceRepository;
import com.project.doongdoong.domain.voice.service.VoiceService;
import com.project.doongdoong.global.util.WebClientUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.project.doongdoong.domain.answer.service.AnswerServiceImp.MAX_ANSWER_COUNT;

@Service @Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisServiceImp implements AnalysisService{

    private final VoiceRepository voiceRepository;
    private final UserRepository userRepository;
    private final AnalysisRepository analsisRepository;
    private final QuestionService questionService;
    private final VoiceService voiceService;
    private final WebClientUtil webClientUtil;

    private final static int ANALYSIS_PAGE_SIZE = 10;
    private final static double ANALYSIS_VOICE_RATE = 0.65;
    private final static double ANALYSIS_TEXT_RATE = 0.35;
    private final static String DEFAULT_NO_ANSWER_MESSAGE = "질문에 대한 답변이 없습니다.";
    private final static String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";

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
        List<String> questionTexts = new ArrayList<>(); // 질문에 대한 내용 텍스트 리스트
        for(int i=0; i<questions.size(); i++){
            Question question = questions.get(i);
            question.connectAnalysis(analysis); // 연관관계 편의 메서드

            Optional<Voice> voice = voiceRepository.findVoiceByQuestionContent(question.getQuestionContent());
            accessUrls.add(voice.get().getAccessUrl());
            questionTexts.add(question.getQuestionContent().getText());

        } // ConcurrentModificationException 으로 인해 for문 사용

        analsisRepository.save(analysis);
        List<Long> questionIds = analysis.getQuestions().stream().map(question -> question.getId())
                .collect(Collectors.toList());

        return AnalysisCreateResponseDto.builder()
                .analysisId(analysis.getId())
                .questionIds(questionIds)
                .questionTexts(questionTexts)
                .accessUrls(accessUrls)
                .build();
    }

    private static String[] parseUniqueValue(String uniqueValue) {
        String[] values = uniqueValue.split("_"); // 사용자 찾기
        return values;
    }

    @Override
    public AnalysisDetailResponse getAnalysis(Long analysisId) {
        Analysis findAnalysis = analsisRepository.findById(analysisId).orElseThrow(() -> new AnalysisNotFoundException()); // fetch join으로 최적화 필요
        List<Question> questions = findAnalysis.getQuestions();
        // questions에 해당하는 answers 가져오기

        List<QuestionContent> questionContents = extractQuestionContentsBy(questions);
        List<String> questionTexts = extractQuestionTextBy(questionContents);
        List<Long> questionIds = questions.stream().map(question -> question.getId()).collect(Collectors.toList());

        List<Voice> findVoices = voiceRepository.findVoiceAllByQuestionContentIn(questionContents);

        Map<QuestionContent, String> voiceMap = mapVoiceToQuestionInformation(findVoices);
        List<String> questionVoiceAccessUrls = questionContents.stream()
                .map(key -> voiceMap.get(key))
                .collect(Collectors.toList());

        List<String> answerContents = questions.stream()
                .map(question -> Optional.ofNullable(question.getAnswer())
                        .map(answer -> answer.getContent())
                        .orElse(DEFAULT_NO_ANSWER_MESSAGE))
                .collect(Collectors.toList());


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);

        return AnalysisDetailResponse.builder()
                .analysisId(findAnalysis.getId())
                .time(findAnalysis.getCreatedTime().format(formatter))
                .feelingState(findAnalysis.getFeelingState())
                .questionIds(questionIds)
                .questionContent(questionTexts)
                .questionContentVoiceUrls(questionVoiceAccessUrls)
                .answerContent(answerContents)
                .build();
    }

    private static Map<QuestionContent, String> mapVoiceToQuestionInformation(List<Voice> findVoices) {
        return findVoices.stream()
                .collect(Collectors.toMap(
                        Voice::getQuestionContent,
                        Voice::getAccessUrl,
                        (existing, replacement) -> existing
                ));
    }

    private static List<String> extractQuestionTextBy(List<QuestionContent> questionContents) {
        List<String> questionTexts = questionContents.stream()
                .map(questionContent -> questionContent.getText())
                .collect(Collectors.toList());
        return questionTexts;
    }

    private static List<QuestionContent> extractQuestionContentsBy(List<Question> questions) {
        List<QuestionContent> questionContents = questions.stream()
                .map(question -> question.getQuestionContent())
                .collect(Collectors.toList());
        return questionContents;
    }

    @Override
    public AnaylsisListResponseDto getAnalysisList(String uniqueValue, int pageNumber) {
        String[] values = parseUniqueValue(uniqueValue);
        User user = userRepository.findBySocialTypeAndSocialId(SocialType.customValueOf(values[1]), values[0])
                .orElseThrow(() -> new UserNotFoundException());

        PageRequest pageable = PageRequest.of(pageNumber, ANALYSIS_PAGE_SIZE);
        Page<Analysis> analysisPages = analsisRepository.findAllByUserOrderByCreatedTime(user, pageable);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT);


        return AnaylsisListResponseDto.builder()
                .pageNumber(analysisPages.getNumber() + 1)
                .totalPage(analysisPages.getTotalPages())
                .anaylsisResponseDtoList(analysisPages.getContent().stream()
                        .map(analysis -> AnaylsisResponseDto.builder()
                                .analysisId(analysis.getId())
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

        Optional<Analysis> analysis = analsisRepository.findFirstByUserOrderByAnalyzeTimeDesc(user);
        List<FeelingStateResponseDto> result = null;
        if(analysis.isPresent())
        {
            Analysis findAnalysis = analysis.get();
            LocalDateTime endTime = findAnalysis.getCreatedTime().plusDays(1).truncatedTo(ChronoUnit.DAYS);
            LocalDateTime startTime = endTime.minusDays(6).truncatedTo(ChronoUnit.DAYS);
            result = analsisRepository.findAllByDateBetween(user, startTime.toLocalDate(), endTime.toLocalDate());
        }

        return FeelingStateResponseListDto.builder()
                .feelingStateResponsesDto(result)
                .build();
    }

    @Transactional
    @Override
    public FellingStateCreateResponse analyzeEmotion(Long analysisId, String uniqueValue) {
        String[] values = parseUniqueValue(uniqueValue);

        LocalDateTime now = LocalDate.now().atStartOfDay();
        log.info("now = {}", now);
        User user = userRepository.findBySocialTypeAndSocialIdWithAnalysisToday(SocialType.customValueOf(values[1]), values[0], now)
                .orElseThrow(() -> new UserNotFoundException());

        if(checkFirstGrowthToday(user)){
            user.growUp();
        }

        Analysis findAnalysis = analsisRepository.searchAnalysisWithVoiceOfAnswer(analysisId).orElseThrow(() -> new AnalysisNotFoundException());
        List<Voice> voices = findAnalysis.getAnswers().stream() // 1. 분석에 대한 답변 매칭 파일 리스트 가져오기
                .map(answer -> answer.getVoice())
                .collect(Collectors.toList());

        if(isAllAnswerdBy(voices)){ //만약 모든 질문에 대한 답변이 없는 경우, 답변이 부족하다는 예외 발생
            throw new AllAnswersNotFoundException();
        }
        if (isAlreadyAnalyzed(findAnalysis)) { // 분석은 1번만 가능
            throw new AlreadyAnalyzedException();
        }

        List<FellingStateCreateResponse> responseByText = webClientUtil.callAnalyzeEmotion(voices); // 2. 파일을 request 값으로 외부 lambda API 비동기 처리(동일한 외부 API 4번 호출)
        List<FellingStateCreateResponse> responseByVoice = webClientUtil.callAnalyzeEmotionVoice(voices);

        List<Answer> answers = findAnalysis.getAnswers();
        for(int i=0; i<responseByText.size(); i++){
            answers.get(i).changeContent(responseByText.get(i).getTranscribedText());
        }

        double resultByText = caluateFellingStatusAverage(responseByText);
        double resultByVoice = caluateFellingStatusAverage(responseByVoice);
        double result = ANALYSIS_TEXT_RATE * resultByText + ANALYSIS_VOICE_RATE * resultByVoice;

        findAnalysis.changeFeelingStateAndAnalyzeTime(result, LocalDate.now());

        return FellingStateCreateResponse.builder()
                .feelingState(result)
                .build();
    }

    private double caluateFellingStatusAverage(List<FellingStateCreateResponse> responseByText) {
        return responseByText.stream() // 3. 처리가 끝나면 double값 4개 값을 평균 내서 감정 수치 값 반환하기
                .mapToDouble(value -> value.getFeelingState())
                .average()
                .getAsDouble();
    }

    private boolean isAllAnswerdBy(List<Voice> voices) {
        return voices.size() != MAX_ANSWER_COUNT;
    }

    private boolean isAlreadyAnalyzed(Analysis findAnalysis) {
        return findAnalysis.getAnalyzeTime() != null ? true : false;
    }

    private boolean checkFirstGrowthToday(User user) {
        List<Analysis> list = user.getAnalysisList().stream()
                .filter(analysis ->   LocalDate.now().equals(analysis.getAnalyzeTime()))
                .collect(Collectors.toList());
        return list.size() == 0 ? true : false;
    }

    @Transactional
    @Override
    public void removeAnaylsis(Long analysisId) {
        // anlaysis와 관련된 answer의 voice에 해당하는 S3 파일 삭제 로직
        Analysis findAnalysis = analsisRepository.searchAnalysisWithVoiceOfAnswer(analysisId).orElseThrow(() -> new AnalysisNotFoundException());
        List<String> accessUrls = findAnalysis.getAnswers().stream()
                .map(answer -> answer.getVoice().getAccessUrl())
                .collect(Collectors.toList());

        if(findAnalysis.getAnswers().size() != 0){
            findAnalysis.getAnswers().stream().forEach(answer -> answer.disconnectWithVoice()); // 연관관계 끊기
            voiceService.deleteVoices(accessUrls); // voice를 참조하는 객체 없으므로 삭제 가능 -> 벌크 삭제로 쿼리 최적화 필요
        }

        analsisRepository.deleteById(analysisId); // analysis 삭제로 question, answer 삭제 로직 -> voiceService.deleteVoices로 answer.voice와 관련 S3 파일은 이미 삭제.
    }




}
