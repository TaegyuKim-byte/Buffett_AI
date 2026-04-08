package com.buffettai.exception;

/**
 * AI 예측 결과 처리 실패 시 발생하는 예외
 * (빈 응답, 유효하지 않은 예측 결과 등)
 */
public class AiPredictionException extends RuntimeException {

    public AiPredictionException(String message) {
        super(message);
    }

    public AiPredictionException(String message, Throwable cause) {
        super(message, cause);
    }
}
