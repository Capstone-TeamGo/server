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
@RedisHash(value = "jwtToken", timeToLive = 60 * 60 * 24 * 14)
public class RefreshToken implements Serializable {

    @Id
    private String id;

    @Indexed
    private String uniqueId;

    @Indexed
    private String refreshToken;

    private boolean isLogout;
    // 권한 필드가 추후 필요하면 추가

    public static RefreshToken of(String socialId, String socialType, String refreshToken){
        String uniqueId = findUniqueId(socialId, socialType);

        return RefreshToken.builder()
                .uniqueId(uniqueId)
                .refreshToken(refreshToken)
                .isLogout(false)
                .build();
    }

    private static String findUniqueId(String socialId, String socialType) {
        String uniqueId = socialId + "_" + socialType;
        return uniqueId;
    }
}
