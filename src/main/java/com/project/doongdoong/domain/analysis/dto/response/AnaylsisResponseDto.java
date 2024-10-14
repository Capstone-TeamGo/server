package com.project.doongdoong.domain.analysis.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class AnaylsisResponseDto {
    private Long analysisId;
    private String time;
    private double feelingState;
    private List<String> questionContent;

    @Builder
    public AnaylsisResponseDto(Long analysisId, double feelingState, String time, List<String> questionContent) {
        this.analysisId = analysisId;
        this.time = time;
        this.feelingState = feelingState;
        this.questionContent = questionContent;
    }


}
