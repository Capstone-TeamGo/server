package com.project.doongdoong.global.annotation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)// 타겟은 파라미터에만 붙이겠다.
@AuthenticationPrincipal(expression = "#this == 'anonymousUser' ? null : username")
public @interface CurrentUser {
    // 익명 사용자인 경우에는 null로, 익명 사용자가 아닌 경우에는 실제 account 객체로
    // Principal 을 다이나믹 하게 꺼내기 위해 @CurrentUser 생성
}
