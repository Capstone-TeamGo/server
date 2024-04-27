package com.project.doongdoong.domain.analysis.repository.querydls;

import com.project.doongdoong.domain.analysis.model.Analysis;
import com.project.doongdoong.domain.voice.model.QVoice;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import java.util.Optional;

import static com.project.doongdoong.domain.analysis.model.QAnalysis.analysis;
import static com.project.doongdoong.domain.answer.model.QAnswer.answer;
import static com.project.doongdoong.domain.voice.model.QVoice.*;

public class AnalysisRepositoryImpl implements AnalysisRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    public AnalysisRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Optional<Analysis> searchAnalysisWithVoiceOfAnswer(Long analysisId) {

        return Optional.of(
                queryFactory
                .selectFrom(analysis)
                .join(analysis.answers, answer).fetchJoin()
                .join(answer.voice, voice).fetchJoin()
                .where(analysis.id.eq(analysisId))
                .fetchOne()
                );
    }
}
