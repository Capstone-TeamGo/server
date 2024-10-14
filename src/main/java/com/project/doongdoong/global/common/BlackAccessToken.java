package com.project.doongdoong.global.common;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;

@Getter @Builder
@AllArgsConstructor
@NoArgsConstructor
@RedisHash(value = "blackToken", timeToLive = 60 * 30)
public class BlackAccessToken implements Serializable {

    @Id
    private String id;

    @Indexed
    private String uniqueId;

    @Indexed
    private String accessToken;


    public static BlackAccessToken of(String socialId, String socialType, String accessToken){

        return BlackAccessToken.builder()
                .uniqueId(findUniqueId(socialId,socialType))
                .accessToken(accessToken)
                .build();
    }
    public static String findUniqueId(String socialId, String socialType){
        return socialId + "_" + socialType;
    }
}
