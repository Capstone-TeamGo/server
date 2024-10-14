package com.project.doongdoong.global.util;


import com.amazonaws.services.s3.AmazonS3Client;
import com.project.doongdoong.domain.analysis.dto.response.FellingStateCreateResponse;
import com.project.doongdoong.domain.voice.model.Voice;
import com.project.doongdoong.global.dto.response.CounselAiResponse;
import com.project.doongdoong.global.exception.servererror.ExternalApiCallException;
import com.project.doongdoong.global.dto.request.ConsultRequest;
import com.project.doongdoong.global.dto.request.VoiceToS3Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;

@Component @Slf4j
@RequiredArgsConstructor
public class WebClientUtil
{
    private final WebClient webClient;
    private final AmazonS3Client amazonS3Client;

    @Value("${spring.lambda.text.url}")
    private String lambdaTextApiUrl;
    @Value("${spring.lambda.cosult}")
    private String lambdaConsultApiUrl;

    @Value("${spring.lambda.emotion_voice}")
    private String lambdaVoiceApiUrl;

    @Value("${cloud.aws.bucket}")
    private String bucketName;
    private static final String VOICE_KEY = "voice/";



    public List<FellingStateCreateResponse> callAnalyzeEmotion(List<Voice> voices) {

        Flux<FellingStateCreateResponse> responseFlux = Flux
                .fromIterable(voices)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(this::callLambdaApi)
                .sequential();

        List<FellingStateCreateResponse> response = responseFlux.collectList().block();
        for(FellingStateCreateResponse dto : response){
            log.info("dto.getFeelingState() = {}", dto.getFeelingState());
            log.info("dto.getTranscribedText() = {}", dto.getTranscribedText());
        }


        return response;
    }

    private Mono<FellingStateCreateResponse> callLambdaApi(Voice voice) {

        VoiceToS3Request body = VoiceToS3Request.builder()
                .fileKey(VOICE_KEY + voice.getStoredName())
                .build();

        return webClient.mutate().build()
                .post()
                .uri(lambdaTextApiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(FellingStateCreateResponse.class)
                .doOnError(e -> {
                    log.info("error 발생 = {}", e.getMessage());
                    throw new ExternalApiCallException();
                });

    }

    public List<FellingStateCreateResponse> callAnalyzeEmotionVoice(List<Voice> voices) {

        Flux<FellingStateCreateResponse> responseFlux = Flux
                .fromIterable(voices)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(this::callLambdaApiVoice)
                .sequential();

        List<FellingStateCreateResponse> response = responseFlux.collectList().block();
        for(FellingStateCreateResponse dto : response){
            log.info("dto.getFeelingState() = {}", dto.getFeelingState());
            log.info("dto.getTranscribedText() = {}", dto.getTranscribedText());
        }


        return response;
    }

    private Mono<FellingStateCreateResponse> callLambdaApiVoice(Voice voice) {

        VoiceToS3Request body = VoiceToS3Request.builder()
                .fileKey(VOICE_KEY + voice.getStoredName())
                .build();

        return webClient.mutate().build()
                .post()
                .uri(lambdaVoiceApiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(FellingStateCreateResponse.class)
                .doOnError(e -> {
                    log.info("error 발생 = {}", e.getMessage());
                    throw new ExternalApiCallException();
                });

    }

    public CounselAiResponse callConsult(HashMap<String, Object> parameters) {

        log.info("analysisTotalContent = {}", parameters.get("analysisContent"));
        log.info("question = {}", parameters.get("question"));
        log.info("analysisContent = {}", parameters.get("analysisContent"));

        ConsultRequest body = ConsultRequest.builder()
                .category(parameters.get("category").toString())
                .question(parameters.get("question").toString())
                .analysisContent(parameters.get("analysisContent").toString())
                .build();

        CounselAiResponse counselAiResponse = new CounselAiResponse("답변입니다.", "임시 imageUrl 입니다.");
        return  counselAiResponse;
        /*
        return webClient.mutate().build()
                .post()
                .uri(lambdaConsultApiUrl)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(CounselAiResponse.class)
                .doOnError(e -> {
                    log.info("error 발생 = {}", e.getMessage());
                    throw new ExternalApiCallException();
                })
                .block();*/
    }
}
