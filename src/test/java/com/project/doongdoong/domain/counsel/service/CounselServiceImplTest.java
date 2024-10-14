package com.project.doongdoong.domain.counsel.service;

import com.project.doongdoong.module.IntegrationSupportTest;
import com.project.doongdoong.domain.analysis.exception.AnalysisAccessDeny;
import com.project.doongdoong.domain.analysis.model.Analysis;
import com.project.doongdoong.domain.analysis.repository.AnalysisRepository;
import com.project.doongdoong.domain.answer.model.Answer;
import com.project.doongdoong.domain.counsel.dto.request.CounselCreateRequest;
import com.project.doongdoong.domain.counsel.dto.response.CounselResultResponse;
import com.project.doongdoong.domain.counsel.repository.CounselRepository;
import com.project.doongdoong.domain.user.model.SocialType;
import com.project.doongdoong.domain.user.model.User;
import com.project.doongdoong.domain.user.repository.UserRepository;
import com.project.doongdoong.global.dto.response.CounselAiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;



class CounselServiceImplTest extends IntegrationSupportTest {
    @Autowired CounselServiceImpl counselService;
    @Autowired AnalysisRepository analysisRepository;
    @Autowired CounselRepository counselRepository;
    @Autowired UserRepository userRepository;

    @Test
    @DisplayName("회원의 질문에 대한 상담 답변을 제공한다.")
    void consult(){
        //given
        String socialId = "123456";
        User user = User.builder()
                .socialId(socialId)
                .socialType(SocialType.APPLE)
                .build();
        User savedUser = userRepository.save(user);

        CounselCreateRequest request = new CounselCreateRequest(
                "취업진로",
                "나는 취업에 대한 고민이 있어. 내가 개발자가 될 수 있을까? 어떤 노력이 필요해?"
        );
        CounselAiResponse mockResponse = new CounselAiResponse(
                "개발자가 되기 위해서는 꾸준한 학습과 프로젝트 경험이 필요합니다.",
                "http://example.com/image.jpg"
        );
        String uniqueValue = savedUser.getSocialId() + "_" + savedUser.getSocialType().getText();

        when(webClientUtil.callConsult(any(HashMap.class)))
                .thenReturn(mockResponse);
        //when
        CounselResultResponse result = counselService.consult(uniqueValue, request);
        //then
        assertThat(result).isNotNull()
                .extracting("counselContent", "imageUrl")
                .containsExactly(mockResponse.getAnswer(), mockResponse.getImageUrl());
        assertThat(result.getCounselId()).isNotNull();
    }

    @Test
    @DisplayName("분석 답변과 함께 회원의 질문에 대한 상담 답변을 제공한다.")
    void consultWithAnalysis(){
        //given
        String socialId = "123456";
        User user = User.builder()
                .socialId(socialId)
                .socialType(SocialType.APPLE)
                .build();
        User savedUser = userRepository.save(user);

        Analysis analysis = Analysis.builder()
                .user(savedUser)
                .build();
        Analysis savedAnalysis = analysisRepository.save(analysis);

        Answer answer1 = Answer.builder()
                .build();
        Answer answer2 = Answer.builder()
                .build();
        Answer answer3 = Answer.builder()
                .build();
        Answer answer4 = Answer.builder()
                .build();
        answer1.connectAnalysis(savedAnalysis);
        answer2.connectAnalysis(savedAnalysis);
        answer3.connectAnalysis(savedAnalysis);
        answer4.connectAnalysis(savedAnalysis);

        CounselCreateRequest request = new CounselCreateRequest(
                savedAnalysis.getId(),
                "취업진로",
                "나는 취업에 대한 고민이 있어. 내가 개발자가 될 수 있을까? 어떤 노력이 필요해?"
        );
        CounselAiResponse mockResponse = new CounselAiResponse(
                "개발자가 되기 위해서는 꾸준한 학습과 프로젝트 경험이 필요합니다.",
                "http://example.com/image.jpg"
        );
        String uniqueValue = savedUser.getSocialId() + "_" + savedUser.getSocialType().getText();

        when(webClientUtil.callConsult(any(HashMap.class)))
                .thenReturn(mockResponse);
        //when
        CounselResultResponse result = counselService.consult(uniqueValue, request);

        //then
        assertThat(result).isNotNull()
                .extracting("counselContent", "imageUrl")
                .containsExactly(mockResponse.getAnswer(), mockResponse.getImageUrl());
        assertThat(result.getCounselId()).isNotNull();

    }

    @Test
    @DisplayName("다른 사용자의 분석 정보를 통해 상담할 수 없습니다.")
    void exceptionWhenAnalysisDoesNotBelongToUser(){
        //given
        String socialId = "123456";
        User user = User.builder()
                .socialId(socialId)
                .socialType(SocialType.APPLE)
                .build();
        String socialId2 = "455678";
        User otherUser = User.builder()
                .socialId(socialId2)
                .socialType(SocialType.APPLE)
                .build();
        User savedUser = userRepository.save(user);
        User savedOtherUser = userRepository.save(otherUser);

        Analysis analysis = Analysis.builder()
                .user(savedUser)
                .build();
        Analysis savedAnalysis = analysisRepository.save(analysis);
        Answer answer1 = Answer.builder()
                .build();
        Answer answer2 = Answer.builder()
                .build();
        Answer answer3 = Answer.builder()
                .build();
        Answer answer4 = Answer.builder()
                .build();
        answer1.connectAnalysis(savedAnalysis);
        answer2.connectAnalysis(savedAnalysis);
        answer3.connectAnalysis(savedAnalysis);
        answer4.connectAnalysis(savedAnalysis);

        CounselCreateRequest request = new CounselCreateRequest(
                savedAnalysis.getId(),
                "취업진로",
                "나는 취업에 대한 고민이 있어. 내가 개발자가 될 수 있을까? 어떤 노력이 필요해?"
        );
        CounselAiResponse mockResponse = new CounselAiResponse(
                "개발자가 되기 위해서는 꾸준한 학습과 프로젝트 경험이 필요합니다.",
                "http://example.com/image.jpg"
        );
        String otherUniqueValue = savedOtherUser.getSocialId() + "_" + savedOtherUser.getSocialType().getText();

        when(webClientUtil.callConsult(any(HashMap.class)))
                .thenReturn(mockResponse);

        //when & then
        assertThatThrownBy(() -> counselService.consult(otherUniqueValue, request))
                .isInstanceOf(AnalysisAccessDeny.class)
                .hasMessageContaining("해당 사용자의 분석이 아니거나 존재하지 않는 분석입니다."); // 예외 메시지는 비즈니스 로직에 맞게 설정


    }
}