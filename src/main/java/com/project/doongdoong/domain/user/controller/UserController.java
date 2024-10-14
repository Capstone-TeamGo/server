package com.project.doongdoong.domain.user.controller;

import com.project.doongdoong.domain.user.service.UserService;
import com.project.doongdoong.global.annotation.CurrentUser;
import com.project.doongdoong.global.common.ApiResponse;
import com.project.doongdoong.global.dto.request.LogoutDto;
import com.project.doongdoong.global.dto.request.OAuthTokenDto;
import com.project.doongdoong.global.dto.request.ReissueDto;
import com.project.doongdoong.global.dto.response.TokenDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 API", description = "로그인, 로그아웃 등 사용자 API")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "jwt 없이 호출용 test API")
    @GetMapping("/ping")
    public ApiResponse<?> testPing(){
        return ApiResponse.of(HttpStatus.OK, null, "ping");
    }


    @PostMapping("/login-oauth")
    public ApiResponse<?> userSignIn(@Valid @RequestBody OAuthTokenDto oAuthTokenInfo){
        TokenDto tokenInfoResponse = userService.checkRegistration(oAuthTokenInfo);

        return ApiResponse.of(HttpStatus.OK, null, tokenInfoResponse);
    }



    @PostMapping("/logout-oauth")
    public ApiResponse<?> userLogout(@Valid @RequestBody LogoutDto logoutDto,
            //@Parameter(description = "Authorization token", required = true,schema = @Schema(type = "string"), in = ParameterIn.HEADER)
            @RequestHeader("Authorization") String accessToken){
        userService.logout(logoutDto, accessToken);

        return ApiResponse.of(HttpStatus.OK, null, "logout success");
    }


    @PostMapping("/reissue")
    public ApiResponse<?> userReissue(@Valid @RequestBody ReissueDto reissueDto){
        TokenDto reissuedToken = userService.reissue(reissueDto);

        return ApiResponse.of(HttpStatus.OK, null, reissuedToken);
    }

    @GetMapping("/my-page")
    public ApiResponse<?> userMyPage(@CurrentUser String uniqueValue){

        return ApiResponse.of(HttpStatus.OK, null, userService.getMyPage(uniqueValue));
    }

    @Operation(summary = "jwt 테스트 API")
    @GetMapping("/jwt-test")
    public ApiResponse<?> test(){
        
        return ApiResponse.of(HttpStatus.OK, null, "성공");
    }

}
