package com.project.doongdoong.domain.analysis.service;

import com.project.doongdoong.domain.analysis.dto.response.*;
import com.project.doongdoong.domain.analysis.exception.AllAnswersNotFoundException;
import com.project.doongdoong.domain.analysis.exception.AlreadyAnalyzedException;
import com.project.doongdoong.module.IntegrationSupportTest;
import com.project.doongdoong.domain.analysis.exception.AnalysisNotFoundException;
import com.project.doongdoong.domain.analysis.model.Analysis;
import com.project.doongdoong.domain.analysis.repository.AnalysisRepository;
import com.project.doongdoong.domain.answer.model.Answer;
import com.project.doongdoong.domain.answer.repository.AnswerRepository;
import com.project.doongdoong.domain.question.model.Question;
import com.project.doongdoong.domain.question.model.QuestionContent;
import com.project.doongdoong.domain.user.exeception.UserNotFoundException;
import com.project.doongdoong.domain.user.model.SocialType;
import com.project.doongdoong.domain.user.model.User;
import com.project.doongdoong.domain.user.repository.UserRepository;
import com.project.doongdoong.domain.voice.model.Voice;
import com.project.doongdoong.domain.voice.repository.VoiceRepository;
import org.hibernate.query.sqm.mutation.internal.cte.CteInsertStrategy;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisServiceImpTest extends IntegrationSupportTest {

    // TODO: 2024-08-01 : 남은 메서드 :




    // TODO: 2024-08-01 : 완료 메서드 : createAnalysis, getAnalysis, getAnalysisList

    @Autowired AnalysisService analysisService;

    @Autowired UserRepository userRepository;
    @Autowired VoiceRepository voiceRepository;
    @Autowired AnswerRepository answerRepository;
    @Autowired AnalysisRepository analysisRepository;

    @TestFactory
    @DisplayName("서비스 회원 정보와 존재하지 않는 서비스 회원 정보의 경우, 분석에 관한 정보 접근 시나리오")
    Collection<DynamicTest> createAnalysis(){
        //given
        for(QuestionContent questionContent : QuestionContent.values()){
            Voice voice = Voice.initVoiceContentBuilder()
                    .originName(questionContent.getText() +"_voice.mp3")
                    .questionContent(questionContent)
                    .build();
            voice.changeAccessUrl("임의의 접근 url 주소");
            voiceRepository.save(voice);
        }

        User user1 = createUser("socialId", SocialType.APPLE);
        User savedUser = userRepository.save(user1);
        String uniqueValue1 = savedUser.getSocialId() + "_" + savedUser.getSocialType().getText();

        int analysisRelatedSize = 4;
        List<String> allQuestionTexts = Arrays.stream(QuestionContent.values())
                .map(QuestionContent::getText)
                .collect(Collectors.toList());

        //when
        return List.of(
                DynamicTest.dynamicTest("질문 목록에 대한 접근 url, 내용 등 분석에 사용할 정보를 생성할 수 있다.", () -> {
                    //when
                    AnalysisCreateResponseDto responseDto = analysisService.createAnalysis(uniqueValue1);
                    //then
                    assertThat(responseDto).isNotNull();
                    assertThat(responseDto.getAnalysisId()).isExactlyInstanceOf(Long.class);
                    assertThat(responseDto.getQuestionTexts())
                            .hasSize(analysisRelatedSize)
                            .allMatch(allQuestionTexts::contains);
                    assertThat(responseDto.getAccessUrls()).hasSize(analysisRelatedSize);

                }),
                DynamicTest.dynamicTest("존재하지 않는 사용자 정보로는 분석 관련 정보에 접근할 수 없습니다.", () -> {
                    //given
                    User user2 = createUser("notFoundSocialId", SocialType.GOOGLE);
                    String uniqueValue2 = user2.getSocialId() + "_" + user2.getSocialType();
                    //when & then
                    assertThatThrownBy(()->analysisService.createAnalysis(uniqueValue2))
                            .isInstanceOf(UserNotFoundException.class)
                            .hasMessage("해당 사용자는 존재하지 않습니다.");

                })
        );
    }

    @Test
    @DisplayName("분석 정보를 조회합니다.")
    void getAnalysis(){
        //given
        for(QuestionContent questionContent : QuestionContent.values()){ // initProvider 대체 -> TEST용
            Voice voice = Voice.initVoiceContentBuilder()
                    .originName(questionContent.getText() +"_voice.mp3")
                    .questionContent(questionContent)
                    .build();
            voice.changeAccessUrl("임의의 접근 url 주소");
            voiceRepository.save(voice);
        }

        Answer answer1 = createAnswer("답변1보이스를 STT로 변경한 텍스트");
        Answer answer2 = createAnswer("답변2보이스를 STT로 변경한 텍스트");
        Answer answer3 = createAnswer("답변3보이스를 STT로 변경한 텍스트");
        Answer answer4 = createAnswer("답변4보이스를 STT로 변경한 텍스트");
        List<Answer> answers = List.of(answer1, answer2, answer3, answer4);
        answerRepository.saveAll(answers);

        User user = createUser("socialId", SocialType.APPLE);
        Question question1 = createQuestion(QuestionContent.FIXED_QUESTION1);
        Question question2 = createQuestion(QuestionContent.FIXED_QUESTION2);
        Question question3 = createQuestion(QuestionContent.UNFIXED_QUESTION1);
        Question question4 = createQuestion(QuestionContent.UNFIXED_QUESTION3);
        List<Question> questions = List.of(question1, question2, question3, question4);
        Analysis analysis = createAnalysis(user, questions);

        question1.connectAnswer(answer1);
        question2.connectAnswer(answer2);
        question3.connectAnswer(answer3);
        question4.connectAnswer(answer4);

        userRepository.save(user);
        Analysis savedAnalysis = analysisRepository.save(analysis);

        LocalDate analyzeTime = now();
        double feelingState = 72.1;
        analysis.changeFeelingStateAndAnalyzeTime(feelingState, analyzeTime);

        int questionSize = 4;
        int answerSize = 4;
        //when
        AnalysisDetailResponse response = analysisService.getAnalysis(savedAnalysis.getId());
        //then
        assertThat(response.getQuestionContentVoiceUrls()).hasSize(questionSize);
        assertThat(response.getQuestionIds()).hasSize(answerSize);
        assertThat(response)
                .extracting(
                        "analysisId",
                        "feelingState",
                        "questionContent",
                        "answerContent"
                )
                .containsExactly(
                        analysis.getId(),
                        feelingState,
                        questions.stream()
                                .map(question -> question.getQuestionContent().getText())
                                .collect(Collectors.toList()),
                        answers.stream()
                                .map(answer -> answer.getContent())
                                .collect(Collectors.toList())
                );
    }

    @Test
    @DisplayName("존재하지 않는 분석 정보를 조회하는 경우, 예외가 발생합니다.")
    void getAnalysis_AnalysisNotFoundException(){
        //given
        for(QuestionContent questionContent : QuestionContent.values()){ // initProvider 대체 -> TEST용
            Voice voice = Voice.initVoiceContentBuilder()
                    .originName(questionContent.getText() +"_voice.mp3")
                    .questionContent(questionContent)
                    .build();
            voice.changeAccessUrl("임의의 접근 url 주소");
            voiceRepository.save(voice);
        }
        Answer answer1 = createAnswer("답변1보이스를 STT로 변경한 텍스트");
        Answer answer2 = createAnswer("답변2보이스를 STT로 변경한 텍스트");
        Answer answer3 = createAnswer("답변3보이스를 STT로 변경한 텍스트");
        Answer answer4 = createAnswer("답변4보이스를 STT로 변경한 텍스트");
        List<Answer> answers = List.of(answer1, answer2, answer3, answer4);
        answerRepository.saveAll(answers);

        Question question1 = createQuestion(QuestionContent.FIXED_QUESTION1);
        Question question2 = createQuestion(QuestionContent.FIXED_QUESTION2);
        Question question3 = createQuestion(QuestionContent.UNFIXED_QUESTION1);
        Question question4 = createQuestion(QuestionContent.UNFIXED_QUESTION3);
        List<Question> questions = List.of(question1, question2, question3, question4);

        String socialId = "socialId";
        SocialType socialType =SocialType.APPLE;
        User user = createUser(socialId, socialType);
        User savedUser = userRepository.save(user);


        Analysis analysis = createAnalysis(savedUser, questions);

        Analysis savedAnalysis = analysisRepository.save(analysis);
        Long analysisId = savedAnalysis.getId();
        Long anyLongValue = 10L;
        Long notFoundAnalysisId = analysisId + anyLongValue;

        //when & then
        assertThatThrownBy(() -> analysisService.getAnalysis(notFoundAnalysisId))
                .isInstanceOf(AnalysisNotFoundException.class)
                .hasMessage("해당 분석은 존재하지 않습니다.");

    }
    @Test
    @DisplayName("특정 사용자의 분석 정보 리스트를 페이징합니다.")
    void getAnalysisList(){
        //given
        String socialId = "socialId";
        SocialType socialType = SocialType.APPLE;
        User user = createUser(socialId, socialType);
        User savedUser = userRepository.save(user);

        Question question1 = createQuestion(QuestionContent.FIXED_QUESTION1);
        Question question2 = createQuestion(QuestionContent.FIXED_QUESTION2);
        Question question3 = createQuestion(QuestionContent.FIXED_QUESTION3);
        Question question4 = createQuestion(QuestionContent.UNFIXED_QUESTION3);
        Question question5 = createQuestion(QuestionContent.UNFIXED_QUESTION1);
        Question question6 = createQuestion(QuestionContent.UNFIXED_QUESTION4);
        List<Question> questions1 = List.of(question1, question2, question4, question5);
        List<Question> questions2 = List.of(question2, question3, question5, question6);

        Analysis analysis1 = createAnalysis(savedUser, questions1);
        Analysis analysis2 = createAnalysis(savedUser, questions1);
        Analysis analysis3 = createAnalysis(savedUser, questions1);
        Analysis analysis4 = createAnalysis(savedUser, questions2);
        Analysis analysis5 = createAnalysis(savedUser, questions2);
        Analysis analysis6 = createAnalysis(savedUser, questions2);
        Analysis analysis7 = createAnalysis(savedUser, questions2);
        List<Analysis> analysies = List.of(analysis1, analysis2, analysis3, analysis4, analysis5, analysis6, analysis7);
        analysisRepository.saveAll(analysies);

        String uniqueValue = socialId + "_" + socialType.getText();
        int pageNumber = 0;

        //when
        AnaylsisListResponseDto response = analysisService.getAnalysisList(uniqueValue, pageNumber);

        //then
        assertThat(response)
                .extracting("pageNumber", "totalPage")
                .containsExactly(1, 1);

        assertThat(response.getAnaylsisResponseDtoList())
                .hasSize(analysies.size())
                .extracting("analysisId", "questionContent")
                .containsExactlyInAnyOrder(
                        tuple(analysis1.getId(), questions1.stream()
                                .map(question -> question.getQuestionContent().getText())
                                .collect(Collectors.toList())),
                        tuple(analysis2.getId(), questions1.stream()
                                .map(question -> question.getQuestionContent().getText())
                                .collect(Collectors.toList())),
                        tuple(analysis3.getId(), questions1.stream()
                                .map(question -> question.getQuestionContent().getText())
                                .collect(Collectors.toList())),
                        tuple(analysis4.getId(), questions2.stream()
                                .map(question -> question.getQuestionContent().getText())
                                .collect(Collectors.toList())),
                        tuple(analysis5.getId(), questions2.stream()
                                .map(question -> question.getQuestionContent().getText())
                                .collect(Collectors.toList())),
                        tuple(analysis6.getId(), questions2.stream()
                                .map(question -> question.getQuestionContent().getText())
                                .collect(Collectors.toList())),
                        tuple(analysis7.getId(), questions2.stream()
                                .map(question -> question.getQuestionContent().getText())
                                .collect(Collectors.toList()))
                );
    }

    @Test
    @DisplayName("가장 최근 감정 분석 결과 날짜를 기준으로 과거 7일 동안 일자 별 평균 분석 점수 목록을 제공한다.")
    void getAnalysisListGroupByDay(){
        //given
        String socialId = "socialId";
        SocialType socialType = SocialType.APPLE;
        User user = createUser(socialId, socialType);
        User savedUser = userRepository.save(user);
        String uniqueValue = savedUser.getSocialId() + "_" + savedUser.getSocialType().getText();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d");

        List<Analysis> arrayList = new ArrayList<>();
        LocalDate nowDate = now();
        double score1 = 10.0;
        for (int i = 0; i < 3; i++) { // 오늘 데이터 3개 추가
            Analysis analysis = createAnalysis(user);
            arrayList.add(analysis);
            analysis.changeFeelingStateAndAnalyzeTime(score1, nowDate);
        }
        LocalDate dateTwoDaysAgo = now().minusDays(2);
        double score2 = 30.0;
        for (int i = 0; i < 3; i++) { // 2일 전 데이터 4개 추가
            Analysis analysis = createAnalysis(user);
            arrayList.add(analysis);
            analysis.changeFeelingStateAndAnalyzeTime(score2, dateTwoDaysAgo);
        }
        LocalDate dateFourDaysAgo = now().minusDays(4);
        double score3 = 40.0;
        for (int i = 0; i < 4; i++) {        // 4일 전 데이터 4개 추가
            Analysis analysis = createAnalysis(user);
            arrayList.add(analysis);
            analysis.changeFeelingStateAndAnalyzeTime(score3, dateFourDaysAgo);
        }
        LocalDate dateFiveDaysAgo = now().minusDays(5);
        double score4 = 50.0;
        for (int i = 0; i < 3; i++) { // 5일 전 데이터 2개 추가
            Analysis analysis = createAnalysis(user);
            arrayList.add(analysis);
            analysis.changeFeelingStateAndAnalyzeTime(score4, dateFiveDaysAgo);
        }
        LocalDate dateSevenDaysAgo = now().minusDays(7);
        double score5 = 60.0;
        for (int i = 0; i < 2; i++) { // 7일 전 데이터 2개 추가
            Analysis analysis = createAnalysis(user);
            arrayList.add(analysis);
            analysis.changeFeelingStateAndAnalyzeTime(score5, dateSevenDaysAgo);
        }
        analysisRepository.saveAll(arrayList);

        //when
        FeelingStateResponseListDto result = analysisService.getAnalysisListGroupByDay(uniqueValue);

        //then
        assertThat(result.getFeelingStateResponsesDto())
                .hasSize(4)
                .extracting("date", "avgFeelingState")
                .containsExactlyInAnyOrder(
                        tuple(nowDate.format(formatter), score1),
                        tuple(dateTwoDaysAgo.format(formatter), score2),
                        tuple(dateFourDaysAgo.format(formatter), score3),
                        tuple(dateFiveDaysAgo.format(formatter), score4)
                );
    }

    @TestFactory
    @DisplayName("여러 경우에 대한 감정 분석 시나리오 테스트")
    java.util.Collection<DynamicTest> analyzeEmotion(){
        //given
        String socialId = "socialId";
        SocialType socialType = SocialType.APPLE;
        User user = createUser(socialId, socialType);
        User savedUser = userRepository.save(user);
        String uniqueValue = savedUser.getSocialId() + "_" + savedUser.getSocialType().getText();

        Question question1 = createQuestion(QuestionContent.FIXED_QUESTION1);
        Question question2 = createQuestion(QuestionContent.FIXED_QUESTION2);
        Question question3 = createQuestion(QuestionContent.UNFIXED_QUESTION1);
        Question question4 = createQuestion(QuestionContent.UNFIXED_QUESTION2);
        Analysis analysis = createAnalysis(savedUser, List.of(question1, question2, question3, question4));
        Analysis savedAnalysis = analysisRepository.save(analysis);

        //when & then
        return List.of(
                DynamicTest.dynamicTest("모든 질문에 대한 답변이 없는 분석 정보를 감정 분석할 수 없습니다.", () -> {
                    //when & then
                    assertThatThrownBy(() -> analysisService.analyzeEmotion(savedAnalysis.getId(), uniqueValue))
                            .isInstanceOf(AllAnswersNotFoundException.class)
                            .hasMessage("질문에 해당하는 모든 답변이 존재하지 않습니다.");

                }),
                DynamicTest.dynamicTest("각 질문에 대한 모든 답변이 이뤄진 분석 정보는 감정 분석으로 감정 상태 결과값을 만든다.", () -> {
                    // given
                    Answer answer1 = createAnswer("질문1에 대한 분석 완료");
                    Answer answer2 = createAnswer("질문2에 대한 분석 완료");
                    Answer answer3 = createAnswer("질문3에 대한 분석 완료");
                    Answer answer4 = createAnswer("질문4에 대한 분석 완료");
                    answer1.connectAnalysis(analysis);
                    answer2.connectAnalysis(analysis);
                    answer3.connectAnalysis(analysis);
                    answer4.connectAnalysis(analysis);

                    List<FellingStateCreateResponse> responseListByText = new ArrayList<>();
                    List<FellingStateCreateResponse> responseListByVoice = new ArrayList<>();
                    for(int index=0; index<4; index++){
                        double randomValue1 = 3, randomValue2 = 5;
                        responseListByText.add(createFellingStatus(randomValue1, index));
                        responseListByVoice.add(createFellingStatus(randomValue2, index));
                    }
                    when(webClientUtil.callAnalyzeEmotion(any(List.class)))
                            .thenReturn(responseListByText);
                    when(webClientUtil.callAnalyzeEmotionVoice(any(List.class)))
                            .thenReturn(responseListByVoice);

                    double analysisTextRate = 0.35;
                    double analysisVoiceRate = 0.65;
                    double resultStatus = analysisTextRate * averageFellingStatusBy(responseListByText)
                            + analysisVoiceRate * averageFellingStatusBy(responseListByVoice);
                    //when
                    FellingStateCreateResponse result = analysisService.analyzeEmotion(savedAnalysis.getId(), uniqueValue);
                    //then
                    assertThat(result)
                            .extracting("transcribedText", "feelingState")
                            .containsExactly(null, resultStatus);

                    assertThat(savedAnalysis)
                            .extracting("feelingState")
                            .isEqualTo(resultStatus);

                }),
                DynamicTest.dynamicTest("이미 감정 분석된 분석 정보는 더 이상 감정 분석할 수 없습니다." ,() -> {
                    // when & then
                    assertThatThrownBy(() -> analysisService.analyzeEmotion(savedAnalysis.getId(), uniqueValue))
                            .isInstanceOf(AlreadyAnalyzedException.class)
                            .hasMessage("이미 분석했습니다.");
                })
        );


    }

    @Test
    @DisplayName("존재하는 분석을 삭제합니다.")
    void removeAnaylsis(){
        //given
        for(QuestionContent questionContent : QuestionContent.values()){ // initProvider 대체 -> TEST용
            Voice voice = Voice.initVoiceContentBuilder()
                    .originName(questionContent.getText() +"_voice.mp3")
                    .questionContent(questionContent)
                    .build();
            voice.changeAccessUrl("임의의 접근 url 주소");
            voiceRepository.save(voice);
        }

        Answer answer1 = createAnswer("답변1보이스를 STT로 변경한 텍스트");
        Answer answer2 = createAnswer("답변2보이스를 STT로 변경한 텍스트");
        Answer answer3 = createAnswer("답변3보이스를 STT로 변경한 텍스트");
        Answer answer4 = createAnswer("답변4보이스를 STT로 변경한 텍스트");
        List<Answer> answers = List.of(answer1, answer2, answer3, answer4);
        answerRepository.saveAll(answers);

        User user = createUser("socialId", SocialType.APPLE);
        Question question1 = createQuestion(QuestionContent.FIXED_QUESTION1);
        Question question2 = createQuestion(QuestionContent.FIXED_QUESTION2);
        Question question3 = createQuestion(QuestionContent.UNFIXED_QUESTION1);
        Question question4 = createQuestion(QuestionContent.UNFIXED_QUESTION3);
        List<Question> questions = List.of(question1, question2, question3, question4);
        Analysis analysis = createAnalysis(user, questions);

        question1.connectAnswer(answer1);
        question2.connectAnswer(answer2);
        question3.connectAnswer(answer3);
        question4.connectAnswer(answer4);

        userRepository.save(user);
        Analysis savedAnalysis = analysisRepository.save(analysis);
        //when
        analysisService.removeAnaylsis(savedAnalysis.getId());
        //then
        boolean exists = analysisRepository.existsById(savedAnalysis.getId());
        assertThat(exists).isFalse(); // 삭제되었음을 검증
    }

    private static double averageFellingStatusBy(List<FellingStateCreateResponse> responseListByText) {
        return responseListByText.stream()
                .mapToDouble(value -> value.getFeelingState())
                .average().getAsDouble();
    }

    private static FellingStateCreateResponse createFellingStatus(double randomValue, int index) {
        return FellingStateCreateResponse.builder()
                .feelingState(30.0 + randomValue)
                .transcribedText("음성 답변을 텍스트로 변환한 답변 텍스트" + index)
                .build();
    }

    private static Voice createVoice(String fileName) {
        return Voice.commonBuilder()
                .originName(fileName)
                .build();
    }


    private static Answer createAnswer(String content) {
        return Answer.builder()
                .content(content)
                .build();
    }

    private static Question createQuestion(QuestionContent questionContent) {
        Question question = Question.builder()
                .questionContent(questionContent)
                .build();

        return question;
    }

    private static Analysis createAnalysis(User user, List<Question> questions) {
        return Analysis.builder()
                .user(user)
                .questions(questions)
                .build();
    }

    private static Analysis createAnalysis(User user) {
        return Analysis.builder()
                .user(user)
                .build();
    }

    private static User createUser(String socialId, SocialType socialType) {
        return User.builder()
                .socialId(socialId)
                .socialType(socialType)
                .build();
    }


}