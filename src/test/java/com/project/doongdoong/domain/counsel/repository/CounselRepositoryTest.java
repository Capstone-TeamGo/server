package com.project.doongdoong.domain.counsel.repository;

import com.project.doongdoong.module.IntegrationSupportTest;
import com.project.doongdoong.domain.analysis.model.Analysis;
import com.project.doongdoong.domain.analysis.repository.AnalysisRepository;
import com.project.doongdoong.domain.counsel.model.Counsel;
import com.project.doongdoong.domain.counsel.model.CounselType;
import com.project.doongdoong.domain.user.model.User;
import com.project.doongdoong.domain.user.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;


//@DataJpaTest
class CounselRepositoryTest extends IntegrationSupportTest{

    @Autowired
    CounselRepository counselRepository;
    @Autowired
    AnalysisRepository analysisRepository;
    @Autowired
    UserRepository userRepository;

    @Test
    @DisplayName("상담 고유 번호로 분석 정보가 담긴 상담 정보를 조회한다.")
    void findWithAnalysisById(){
        //given
        Analysis analysis = Analysis.builder()
                .build();

        Counsel counsel = Counsel.builder()
                .question("질문1")
                .counselType(CounselType.FAMILY)
                .build();

        counsel.addAnalysis(analysis);
        Analysis savedAnalysis = analysisRepository.save(analysis);
        Counsel savedCounsel = counselRepository.save(counsel);


        //when
        Optional<Counsel> findCounsel = counselRepository.findWithAnalysisById(savedCounsel.getId());
        //then
        Assertions.assertThat(findCounsel.get()).isNotNull()
                .extracting("question", "counselType", "id")
                .containsExactlyInAnyOrder(
                        "질문1", CounselType.FAMILY, savedCounsel.getId()
                );
        Assertions.assertThat(findCounsel.get().getAnalysis().getId()).isEqualTo(savedAnalysis.getId());
    }

    @Test
    @DisplayName("본인의 상담 기록 중 특정 페이지를 조회한다.")
    void searchPageCounselList(){
        //given
        User user = User.builder()
                .nickname("짱구")
                .email("whffkaos007@naver.com")
                .build();
        User savedUser = userRepository.save(user);

        Analysis analysis = Analysis.builder()
                .build();
        Analysis savedAnalysis = analysisRepository.save(analysis);

        Counsel counsel1 = Counsel.builder()
                .user(savedUser)
                .question("질문1")
                .counselType(CounselType.FAMILY)
                .build();
        Counsel counsel2 = Counsel.builder()
                .user(savedUser)
                .question("질문2")
                .counselType(CounselType.JOB)
                .build();
        Counsel counsel3 = Counsel.builder()
                .user(savedUser)
                .question("질문3")
                .counselType(CounselType.LOVE)
                .build();

        counsel2.addAnalysis(savedAnalysis);
        counselRepository.saveAll(List.of(counsel1, counsel2, counsel3));

        int pageNumber = 0;
        int pageSize = 10;
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);

        //when
        Page<Counsel> counselPages = counselRepository.searchPageCounselList(savedUser, pageRequest);

        //then
        Assertions.assertThat(counselPages.getNumber()).isEqualTo(pageNumber);
        Assertions.assertThat(counselPages.getSize()).isEqualTo(pageSize);
        Assertions.assertThat(counselPages.getContent())
                .hasSize(3)
                .extracting("user", "question", "counselType", "analysis")
                .containsExactlyInAnyOrder(
                        Assertions.tuple(savedUser, "질문1", CounselType.FAMILY, null),
                        Assertions.tuple(savedUser, "질문2", CounselType.JOB, savedAnalysis),
                        Assertions.tuple(savedUser, "질문3", CounselType.LOVE, null)
                );


    }
}