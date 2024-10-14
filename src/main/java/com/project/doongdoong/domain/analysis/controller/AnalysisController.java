package com.project.doongdoong.domain.analysis.controller;

import com.project.doongdoong.domain.analysis.dto.response.*;
import com.project.doongdoong.domain.analysis.service.AnalysisService;
import com.project.doongdoong.domain.answer.dto.AnswerCreateResponseDto;
import com.project.doongdoong.domain.answer.service.AnswerService;
import com.project.doongdoong.domain.image.exception.FileEmptyException;
import com.project.doongdoong.global.annotation.CurrentUser;
import com.project.doongdoong.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AnswerService answerService;

    @PostMapping
    public ApiResponse<AnalysisCreateResponseDto> createAnaysis(@CurrentUser String uniqueValue){

        return ApiResponse.of(HttpStatus.OK, null, analysisService.createAnalysis(uniqueValue));
    }


    @GetMapping("{id}")
    public ApiResponse<AnalysisDetailResponse> getAnalysis(@PathVariable(name = "id", required = true) Long id){

        return ApiResponse.of(HttpStatus.OK, null, analysisService.getAnalysis(id));
    }

    @GetMapping
    public ApiResponse<AnaylsisListResponseDto> getAnalyses(@CurrentUser String uniqueValue
            , @RequestParam(name = "pageNumber", required = false, defaultValue = "1") int pageNumber){
        pageNumber -= 1; // 페이징은 0번부터이므로 1페이지로 표시

        return ApiResponse.of(HttpStatus.OK, null, analysisService.getAnalysisList(uniqueValue ,pageNumber));
    }

    @GetMapping("/week")
    public ApiResponse<FeelingStateResponseListDto> getAnalysesGroupByDay(@CurrentUser String uniqueValue){

        return ApiResponse.of(HttpStatus.OK, null, analysisService.getAnalysisListGroupByDay(uniqueValue));
    }

    @PostMapping("/{id}")
    public ApiResponse<FellingStateCreateResponse> analyzeEmotion(@PathVariable("id") Long analysisId, @CurrentUser String uniqueValue){

        return ApiResponse.of(HttpStatus.OK, null, analysisService.analyzeEmotion(analysisId, uniqueValue));
    }

    /*@PostMapping("/{id}/answer")
    public ApiResponse<AnswerCreateResponseDto> createAnswer(@PathVariable("id") Long analysisId,
                                                             @RequestPart("file") MultipartFile file,
                                                             @RequestPart("dto") @Valid AnswerCreateRequestDto dto){

        if(file.isEmpty()){
            throw new FileEmptyException();
        }

        return ApiResponse.of(HttpStatus.OK, null, answerService.createAnswer(analysisId, file, dto));
    }*/

    @PostMapping("/{id}/answer")
    public ApiResponse<AnswerCreateResponseDto> createAnswer(@PathVariable("id") Long analysisId,
                                                             @RequestPart("file") MultipartFile file,
                                                             @RequestParam("questionId") Long questionId){

        if(file == null || file.isEmpty()){
            throw new FileEmptyException();
        }

        return ApiResponse.of(HttpStatus.OK, null, answerService.createAnswer(analysisId, file, questionId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public ApiResponse<?> deleteAnalysis(@PathVariable("id") Long analysisId){
        analysisService.removeAnaylsis(analysisId);
        return ApiResponse.of(HttpStatus.NO_CONTENT,null, null);
    }
}
