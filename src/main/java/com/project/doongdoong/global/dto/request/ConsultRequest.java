package com.project.doongdoong.global.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ConsultRequest {
    private String category;
    private String question;
    private String analysisContent;

    @Builder
    public ConsultRequest(String category, String question, String analysisContent) {
        this.category = category;
        this.question = question;
        this.analysisContent = analysisContent;
    }
}
