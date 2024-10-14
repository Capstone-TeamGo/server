package com.project.doongdoong.domain.user.service;

import com.project.doongdoong.domain.user.dto.UserInfomationResponse;
import com.project.doongdoong.domain.user.exeception.RefreshTokenNoutFoundException;
import com.project.doongdoong.domain.user.exeception.SocialTypeNotFoundException;
import com.project.doongdoong.domain.user.exeception.TokenInfoFobiddenException;
import com.project.doongdoong.domain.user.exeception.UserNotFoundException;
import com.project.doongdoong.domain.user.model.SocialType;
import com.project.doongdoong.domain.user.model.User;
import com.project.doongdoong.domain.user.repository.UserRepository;
import com.project.doongdoong.global.common.BlackAccessToken;
import com.project.doongdoong.global.util.JwtProvider;
import com.project.doongdoong.global.common.RefreshToken;
import com.project.doongdoong.global.dto.request.LogoutDto;
import com.project.doongdoong.global.dto.request.OAuthTokenDto;
import com.project.doongdoong.global.dto.request.ReissueDto;
import com.project.doongdoong.global.dto.response.TokenDto;
import com.project.doongdoong.global.repositoty.BlackAccessTokenRepository;
import com.project.doongdoong.global.repositoty.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service @Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlackAccessTokenRepository blackAccessTokenRepository;
    private final JwtProvider jwtProvider;


    @Transactional
    public TokenDto checkRegistration(OAuthTokenDto oAuthTokenInfo) {

        String socialId = oAuthTokenInfo.getSocialId();
        String email = oAuthTokenInfo.getEmail();
        String nickname = oAuthTokenInfo.getNickname();
        SocialType socialType = SocialType.customValueOf(oAuthTokenInfo.getSocialType());
        if(socialType == null){
            new SocialTypeNotFoundException();
        }
        log.info("socialType = {}", socialType);
        log.info("socialId = {}", socialId);
        User user = userRepository.findBySocialTypeAndSocialId(socialType, socialId)
                .orElse(toEntity(socialId, email, nickname, socialType));
        checkChange(email, nickname, user); // 기존 회원 정보와 달라졌는지 확인
        user.checkRoles(); // 새롭게 생성된 경우 사용자 권한 제공
        userRepository.save(user); // 새롭게 생성된 경우에는 영속화 필요

        Optional<BlackAccessToken> blackAccessToken =
                blackAccessTokenRepository.findById(BlackAccessToken.findUniqueId(socialId,socialType.getText()));
        if(blackAccessToken.isPresent()) {
            blackAccessTokenRepository.delete(blackAccessToken.get());
            log.info("blackAccessToken 존재해서 삭제");
        }

        TokenDto tokenInfoResponse = jwtProvider.generateToken(socialId, socialType.getText(), user.getRoles());

        RefreshToken refresh = RefreshToken.of(user.getSocialId(), user.getSocialType().getText(), tokenInfoResponse.getRefreshToken());
        Optional<RefreshToken> findRefreshToken = refreshTokenRepository.findByUniqueId(refresh.getUniqueId());
        if(findRefreshToken.isPresent()){ // 기존 rft이 존재한다면 삭제하고 새롭게 저장
            refreshTokenRepository.delete(findRefreshToken.get()); log.info("기존 RefreshToken 삭제");
        }
        refreshTokenRepository.save(refresh); log.info("새로운 RefreshToken 저장");

        return tokenInfoResponse;
    }

    private void checkChange(String email, String nickname, User user) {
        if(email == null)
            return;

        if (!user.getEmail().equals(email) || !user.getNickname().equals(nickname)) {
            user.changeEmail(email);
            user.changeNickname(nickname);
        }
    }

    private User toEntity(String socialId, String email, String nickname, SocialType socialType) {
        return User
                .builder()
                .socialId(socialId)
                .email(email)
                .nickname(nickname)
                .socialType(socialType)
                .build();
    }

    @Transactional
    public TokenDto reissue(ReissueDto reissueTokenDto){
        String refreshToken = reissueTokenDto.getRefreshToken();
        Optional<RefreshToken> findRefreshToken = refreshTokenRepository.findByRefreshToken(refreshToken);
        
        if(findRefreshToken.isPresent()) { // rft이 존재한다면
            String token = findRefreshToken.get().getRefreshToken().substring(7);
            String socialId = jwtProvider.extractSocialId(token);
            String socialType = jwtProvider.extractSocialType(token);
            String role = jwtProvider.extractRole(token);
            String accessToken = jwtProvider.createAccessToken(socialId, socialType, role); // act 갱신

            return TokenDto.builder()
                    .accessToken(accessToken)
                    .build();
        }else{
            throw new RefreshTokenNoutFoundException();
        }

    }

    @Transactional
    public void logout(LogoutDto tokenInfoDto, String accessToken) {
        String socialType = jwtProvider.extractSocialType(accessToken.substring(7));
        String socialId = jwtProvider.extractSocialId(accessToken.substring(7));
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByRefreshToken(tokenInfoDto.getRefreshToken());
        if(refreshToken.isPresent()){       // blackAccessToken으로 이미 접근 권한을 막았지만 더 안전한 보안으로 rft도 삭제
            refreshTokenRepository.delete(refreshToken.get()); // rft이 존재한다면 삭제
        }

        String accessSocialId = jwtProvider.extractSocialId(accessToken.substring(7));
        String accessSocailType = jwtProvider.extractSocialType(accessToken.substring(7));
        if (!accessSocialId.equals(socialId)){
            new TokenInfoFobiddenException();
        }
        if (!accessSocailType.equals(socialType)){
            new TokenInfoFobiddenException();
        }

        BlackAccessToken blackAccessToken = BlackAccessToken.of(socialId, socialType, accessToken);
        blackAccessTokenRepository.save(blackAccessToken); // blackAccessToken 저장 -> 해당 act은 만료기간 남았더라도 접근 불가
    }

    public UserInfomationResponse getMyPage(String uniqueValue) {
        String[] value = parseUniqueValue(uniqueValue);
        User findUser = userRepository.findBySocialTypeAndSocialId(SocialType.customValueOf(value[1]), value[0])
                .orElseThrow(() -> new UserNotFoundException());

        return UserInfomationResponse.builder()
                .nickname(findUser.getNickname())
                .email(findUser.getEmail())
                .socialType(findUser.getSocialType().getText())
                .analysisCount(findUser.getEmotionGrowth())
                .build();
    }

    private static String[] parseUniqueValue(String uniqueValue) {
        String[] values = uniqueValue.split("_"); // 사용자 찾기
        return values;
    }

    /*public boolean checkBlackToken(HttpServletRequest request){
        String accessToken = request.getHeader("Authorization");
        Optional<BlackAccessToken> findBlackToken = blackAccessTokenRepository.findByAccessToken(accessToken);
        if(findBlackToken.isPresent()){
            throw new CustomException.UnauthorizedException(HttpStatus.UNAUTHORIZED, "로그아웃으로 인해 인가 권한이 없습니다.");
        }
    }*/


}
