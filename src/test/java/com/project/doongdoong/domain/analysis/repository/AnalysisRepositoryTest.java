package com.project.doongdoong.domain.analysis.repository;

import com.project.doongdoong.module.IntegrationSupportTest;
import com.project.doongdoong.domain.analysis.dto.response.FeelingStateResponseDto;
import com.project.doongdoong.domain.analysis.model.Analysis;
import com.project.doongdoong.domain.answer.model.Answer;
import com.project.doongdoong.domain.answer.repository.AnswerRepository;
import com.project.doongdoong.domain.question.model.Question;
import com.project.doongdoong.domain.question.model.QuestionContent;
import com.project.doongdoong.domain.question.repository.QuestionRepository;
import com.project.doongdoong.domain.user.model.SocialType;
import com.project.doongdoong.domain.user.model.User;
import com.project.doongdoong.domain.user.repository.UserRepository;
import com.project.doongdoong.domain.voice.model.Voice;
import com.project.doongdoong.domain.voice.repository.VoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class AnalysisRepositoryTest extends IntegrationSupportTest {

    @Autowired AnalysisRepository analysisRepository;
    @Autowired QuestionRepository questionRepository;
    @Autowired UserRepository userRepository;
    @Autowired AnswerRepository answerRepository;
    @Autowired VoiceRepository voiceRepository;

    @Test
    @DisplayName("접근 회원과 고유 분석 번호를 통해 일치하는 분석 정보를 조회합니다.")
    void findByUserAndId(){
        //given
        User user = createUser("socialId1", SocialType.APPLE);
        User savedUser = userRepository.save(user);

        Analysis analysis = createAnalysis(user);
        Analysis savedAnalysis = analysisRepository.save(analysis);

        Long requestId = savedAnalysis.getId();

        //when
        Optional<Analysis> findAnalysis = analysisRepository.findByUserAndId(savedUser, requestId);

        //then
        assertThat(findAnalysis.get())
                .isNotNull()
                .isEqualTo(savedAnalysis);
        assertThat(findAnalysis.get().getUser())
                .isNotNull()
                .isEqualTo(savedUser);

    }

    @Test
    @DisplayName("사용자의 분석 결과를 최신순으로 페이징 조회합니다.")
    void findAllByUserOrderByCreatedTime(){
        //given
        User user = createUser("socialId", SocialType.APPLE);
        User savedUser = userRepository.save(user);
        int pageNumber = 1;
        int pageSize = 5;
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);

        Analysis analysis1 = createAnalysis(user);
        Analysis analysis2 = createAnalysis(user);
        Analysis analysis3 = createAnalysis(user);
        Analysis analysis4 = createAnalysis(user);
        Analysis analysis5 = createAnalysis(user);
        Analysis analysis6 = createAnalysis(user);
        Analysis analysis7 = createAnalysis(user);
        analysis1.changeFeelingStateAndAnalyzeTime(10, null);
        analysis2.changeFeelingStateAndAnalyzeTime(20, null);
        analysis3.changeFeelingStateAndAnalyzeTime(30, null);
        analysis4.changeFeelingStateAndAnalyzeTime(40, null);
        analysis5.changeFeelingStateAndAnalyzeTime(50, null);
        analysis6.changeFeelingStateAndAnalyzeTime(60, null);
        analysis7.changeFeelingStateAndAnalyzeTime(70, null);

        analysisRepository.saveAll(List.of(analysis1, analysis2, analysis3, analysis4, analysis5, analysis6, analysis7));

        //when
        Page<Analysis> result = analysisRepository.findAllByUserOrderByCreatedTime(savedUser, pageRequest);

        //then
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getTotalElements()).isEqualTo(7);
        assertThat(result.getContent())
                .hasSize(2)
                .extracting("user.socialId", "feelingState")
                .containsExactly(
                        tuple("socialId", 60.0),
                        tuple("socialId", 70.0)
                );



    }

    @DisplayName("특정 시간 내에 있는 사용자의 감정 분석 시간과 감정 수치를 조회합니다.")
    @ParameterizedTest
    @CsvSource({"2024-03-05, 2024-03-15", "2024-03-02, 2024-03-30","2024-03-03, 2024-03-27" })
    void findAllByDateBetween(LocalDate startTime, LocalDate endTime){
        //given
        User user = createUser("socialId", SocialType.APPLE);

        User savedUser = userRepository.save(user);

        Analysis analysis1 = createAnalysis(savedUser);
        Analysis analysis2 = createAnalysis(savedUser);
        Analysis analysis3 = createAnalysis(savedUser);
        Analysis analysis4 = createAnalysis(savedUser);
        Analysis analysis5 = createAnalysis(savedUser);

        analysis1.changeFeelingStateAndAnalyzeTime(72.5, LocalDate.of(2024,3,1));
        analysis2.changeFeelingStateAndAnalyzeTime(75, LocalDate.of(2024,3,5));
        analysis3.changeFeelingStateAndAnalyzeTime(77.5, LocalDate.of(2024,3, 15));
        analysis4.changeFeelingStateAndAnalyzeTime(80, LocalDate.of(2024, 3, 15));
        analysis5.changeFeelingStateAndAnalyzeTime(80, LocalDate.of(2024, 3, 31));

        analysisRepository.saveAll(List.of(analysis1, analysis2, analysis3, analysis4, analysis5));

        //when
        List<FeelingStateResponseDto> result = analysisRepository.findAllByDateBetween(savedUser, startTime, endTime);
        //then
        assertThat(result).hasSize(2)
                .extracting("date", "avgFeelingState")
                .containsExactly(
                        tuple("2024-3-5", Double.valueOf(75)),
                        tuple("2024-3-15", Double.valueOf(78.75))
                );

    }

    @Test
    @DisplayName("사용자의 가장 최근 분석 조회하기")
    void findFirstByUserOrderByAnalyzeTimeDesc(){
        //given
        User user = createUser("socialId", SocialType.APPLE);
        User savedUser = userRepository.save(user);

        Analysis analysis1 = createAnalysis(savedUser);
        Analysis analysis2 = createAnalysis(savedUser);
        Analysis analysis3 = createAnalysis(savedUser);
        Analysis analysis4 = createAnalysis(savedUser);

        analysis1.changeFeelingStateAndAnalyzeTime(70, LocalDate.of(2023, 12, 5));
        analysis2.changeFeelingStateAndAnalyzeTime(75, LocalDate.of(2024, 3, 5));
        analysis3.changeFeelingStateAndAnalyzeTime(80, LocalDate.of(2024, 6, 27));
        analysis4.changeFeelingStateAndAnalyzeTime(90, LocalDate.of(2024, 11, 19));

        analysisRepository.saveAll(List.of(analysis1, analysis2, analysis3, analysis4));

        //when
        Optional<Analysis> result = analysisRepository.findFirstByUserOrderByAnalyzeTimeDesc(savedUser);

        //then
        assertThat(result.get()).isNotNull()
                .isEqualTo(analysis4)
                .extracting("user", "feelingState", "analyzeTime")
                .contains(savedUser, analysis4.getFeelingState(), analysis4.getAnalyzeTime());
    }

    @Test
    @DisplayName("분석 정보와 분석에 사용된 질문들을 조회합니다.")
    void findAnalysisWithQuestion(){
        //given
        User user = createUser("socialId", SocialType.APPLE);
        User savedUser = userRepository.save(user);

        Question question1 = createQuestion(QuestionContent.FIXED_QUESTION1);
        Question question2 = createQuestion(QuestionContent.FIXED_QUESTION2);
        Question question3 = createQuestion(QuestionContent.UNFIXED_QUESTION1);
        Question question4 = createQuestion(QuestionContent.UNFIXED_QUESTION1);

        List<Question> questions = List.of(question1, question2, question3, question4);

        Analysis analysis = createAnalysis(savedUser,questions);
        question1.connectAnalysis(analysis);
        question2.connectAnalysis(analysis);
        question3.connectAnalysis(analysis);
        question4.connectAnalysis(analysis);

        Analysis savedAnaylsis = analysisRepository.save(analysis);

        //when
        Optional<Analysis> result = analysisRepository.findAnalysisWithQuestion(savedAnaylsis.getId());
        //then
        assertThat(result.get()).isNotNull()
                .isEqualTo(savedAnaylsis)
                .extracting("id", "user")
                .contains(savedAnaylsis.getId(), savedUser);

        assertThat(result.get().getQuestions())
                .hasSize(questions.size())
                .containsExactlyInAnyOrder(question1, question2, question3, question4);
        ;
    }


    /**
     * QueryDsl Test
     */

    @Test
    @DisplayName("음성 답변과 답변 정보가 담긴 분석 정보를 조회한다.")
    void searchAnalysisWithVoiceOfAnswer(){
        //given
        String socialId = "socialId";
        SocialType socialType = SocialType.APPLE;
        User user = createUser(socialId, socialType);

        Analysis analysis = createAnalysis(user);
        userRepository.save(user);

        Voice voice1 = createVoice("파일이름1", QuestionContent.FIXED_QUESTION1);
        Voice voice2 = createVoice("파일이름2", QuestionContent.FIXED_QUESTION2);
        Voice voice3 = createVoice("파일이름3", QuestionContent.UNFIXED_QUESTION1);
        Answer answer1 = createAnswer(voice1, "질문에 대한 답변 텍스트1");
        Answer answer2 = createAnswer(voice2, "질문에 대한 답변 텍스트2");
        Answer answer3 = createAnswer(voice3, "질문에 대한 답변 텍스트3");
        answer1.connectAnalysis(analysis);
        answer2.connectAnalysis(analysis);
        answer3.connectAnalysis(analysis);

        voiceRepository.saveAll(List.of(voice1,voice2,voice3));
        answerRepository.saveAll(List.of(answer1,answer2,answer3));
        analysisRepository.save(analysis);

        //when
        Optional<Analysis> result = analysisRepository.searchAnalysisWithVoiceOfAnswer(analysis.getId());
        //then
        result.ifPresent(findAnalysis ->{
            assertThat(findAnalysis).isNotNull();
            List<Answer> answers = findAnalysis.getAnswers();
            assertThat(answers).hasSize(3);
            assertThat(answers)
                    .extracting("content")
                    .containsExactlyInAnyOrder(
                            "질문에 대한 답변 텍스트1",
                            "질문에 대한 답변 텍스트2",
                            "질문에 대한 답변 텍스트3"
                            //"질문에 대한 답변 텍스트4"
                    );
            assertThat(answers)
                    .extracting("voice.originName")
                    .containsExactlyInAnyOrder(
                            "파일이름1",
                            "파일이름2",
                            "파일이름3"
                            //"파일이름4"
                    );
        });
    }


    private static Voice createVoice(String fileName, QuestionContent questionContent) {
        return Voice.initVoiceContentBuilder()
                .originName(fileName)
                .questionContent(questionContent)
                .build();
    }

    private static Answer createAnswer(Voice voice, String content) {
        return Answer.builder()
                .voice(voice)
                .content(content)
                .build();
    }

    private static Question createQuestion(QuestionContent questionContent) {
        Question question = Question.builder()
                .questionContent(questionContent)
                .build();

        return question;
    }

    private static Analysis createAnalysis(User user) {
        return Analysis.builder()
                .user(user)
                .build();
    }
    private static Analysis createAnalysis(User user, List<Question> questions) {
        return Analysis.builder()
                .user(user)
                .questions(questions)
                .build();
    }

    private static User createUser(String socialId, SocialType socialType) {
        return User.builder()
                .socialId(socialId)
                .socialType(socialType)
                .build();
    }

}