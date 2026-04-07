package com.buffettai.exception;

/**
 * AI 서버 API 호출 실패 시 발생하는 예외
 */
public class AiApiException extends RuntimeException {

    public AiApiException(String message) {
        super(message);
    }

    public AiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
