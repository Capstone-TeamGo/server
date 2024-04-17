package com.project.doongdoong.domain.analysis.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AnalysisCreateResponseDto {

    private Long analysisId;
    private List<String> questionTexts;
    private List<String> accessUrls;

    @Builder
    public AnalysisCreateResponseDto(Long analysisId, List<String> questionTexts, List<String> accessUrls){
        this.analysisId = analysisId;
        this.questionTexts = questionTexts;
        this.accessUrls = accessUrls;

    }
}