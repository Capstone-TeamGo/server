package com.project.doongdoong.global.util;


import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.project.doongdoong.domain.analysis.dto.response.FellingStateCreateResponse;
import com.project.doongdoong.domain.counsel.model.Counsel;
import com.project.doongdoong.domain.voice.model.Voice;
import com.project.doongdoong.global.exception.ErrorType;
import com.project.doongdoong.global.exception.servererror.ExternalApiCallException;
import com.project.doongdoong.global.util.dto.VoiceToS3Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Component @Slf4j
@RequiredArgsConstructor
public class WebClientUtil
{
    private final WebClient webClient;
    private final AmazonS3Client amazonS3Client;

    @Value("${spring.lambda.text.url}")
    private String lambdaTextApiUrl;
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

    public String callConsult(HashMap<String, Object> parameters) {

        parameters.values().stream()
                .forEach(value -> log.info("value = {}", value));

        return "안녕하세요. 메롱메롱!";
    }
}
