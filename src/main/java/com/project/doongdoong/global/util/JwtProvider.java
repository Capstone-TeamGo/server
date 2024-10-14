package com.project.doongdoong.global.util;

import com.project.doongdoong.global.common.BlackAccessToken;
import com.project.doongdoong.global.dto.response.TokenDto;
import com.project.doongdoong.global.repositoty.BlackAccessTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component @Getter
@RequiredArgsConstructor
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretKey;
    private Key key;

    private final long ACCESS_TOKEN_VALIDATION_TIME = 30 * 60 * 1000L; // 30분
    private final long REFRESH_TOKEN_VALIDATION_TIME = 1000L * 60L * 60L * 24L * 14; // 2주
    public final static String BEARER_PREFIX = "Bearer ";

    private final BlackAccessTokenRepository blackAccessTokenRepository;




    @PostConstruct
    protected void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }


    public boolean validateToken(String token) {  // 토큰의 유효성 검증을 수행
        if (token == null) {
            log.info("인가가 필요없는 API 호출에서는 토큰 필요 X, 따라서 토큰이 null인 경우는 그렇게 판단하고 제외");
            return false;
        }
        Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
        return true;
    }

    public TokenDto generateToken(String socialId, String socialType, List<String> roles) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        String role = authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));

        // refreshToken과 accessToken을 생성한다.
        String refreshToken = createRefreshToken(socialId, socialType, role);
        String accessToken = createAccessToken(socialId, socialType, role);

        /*// 토큰을 Redis에 저장한다.
        tokenService.saveTokenInfo(email, refreshToken, accessToken);*/
        return new TokenDto(accessToken, refreshToken);
    }

    public String createRefreshToken(String socialId, String socialType, String role) {

        Claims claims = Jwts.claims().setSubject(socialId);
        claims.put("socialType",socialType); // 새로운 클레임 객체를 생성하고, 이메일과 역할(권한)을 셋팅
        claims.put("role",role);

        return BEARER_PREFIX + Jwts.builder() // Payload를 구성하는 속성들을 정의한다.
                .setClaims(claims)
                .setExpiration(new Date(new Date().getTime() + REFRESH_TOKEN_VALIDATION_TIME)) // 토큰의 만료일시를 설정한다.
                .signWith(key, SignatureAlgorithm.HS256) // 지정된 서명 알고리즘과 비밀 키를 사용하여 토큰을 서명한다.
                .compact();
    }

    public String createAccessToken(String socialId,String socialType,  String role) {

        Claims claims = Jwts.claims().setSubject(socialId);
        claims.put("socialType",socialType); // 새로운 클레임 객체를 생성하고, 이메일과 역할(권한)을 셋팅
        claims.put("role",role);

        return BEARER_PREFIX + Jwts.builder()
                        .setClaims(claims)
                        .setExpiration(new Date(new Date().getTime() + ACCESS_TOKEN_VALIDATION_TIME))
                        .signWith(key, SignatureAlgorithm.HS256)
                        .compact();
    }

    // JWT 토큰을 복호화하여 토큰에 들어있는 정보를 꺼내는 메서드
    public Authentication getAuthentication(String accessToken) {
        // 토큰 복호화
        Claims claims = parseClaims(accessToken);
        if (claims.get("role") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // 클레임에서 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("role").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // UserDetails 객체를 만들어서 Authentication 리턴
        // password가 없는데 이렇게 작성하면 보안 문제가 발생할 거 같음. 나중에 확인해 보자.
        log.info("claims.getSubject() = {}",claims.getSubject());
        UserDetails principal = new User(claims.getSubject() + "_" +claims.get("socialType", String.class), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }
    public Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // 토큰에서 Email을 추출한다.
    public String extractSocialId(String token) {
        return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody().getSubject();
    }

    // 토큰에서 ROLE(권한)만 추출한다.
    public String extractRole(String token) {
        return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody().get("role", String.class);
    }

    public String extractSocialType(String token) {
        return Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody().get("socialType", String.class);
    }
    public boolean checkLogoutToken(String token) { // blackAccessToken의 존재 유무 반환 메소드 -> 존재한다면 해당 토큰은 인가 권한 X
        Optional<BlackAccessToken> findBlackToken = blackAccessTokenRepository.findByAccessToken(BEARER_PREFIX + token);
        if (findBlackToken.isPresent()){ // 로그아웃을 이미 했고 소셜타입까지 일치하면 정확한 사용자가 맞고 로그아웃한 토큰이다.
            log.info("블랙 토큰 존재 ->  현재 act은 접근 권한이 없음");
            return true;
        }
        return false;
    }
}

/**
 * 로그아웃 성공 -> 블랙 토큰 저장
 * 토큰 가지고 접근 -> 블랙 토큰이 존재 -> 접근 불가
 * 로그인 성공 -> 블랙 토큰 존재한다면 삭제
 *
 */