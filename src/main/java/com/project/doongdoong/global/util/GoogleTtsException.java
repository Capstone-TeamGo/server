package com.project.doongdoong.global.util;

import com.project.doongdoong.global.exception.CustomException;
import com.project.doongdoong.global.exception.ErrorType;

public class GoogleTtsException extends CustomException.ServerErrorException {
    public GoogleTtsException() {
        super(ErrorType.ServerError.EXTERNAL_SERVER_ERROR, "Google Text-To-Speech API가 동작하지 않습니다.");
    }
}
