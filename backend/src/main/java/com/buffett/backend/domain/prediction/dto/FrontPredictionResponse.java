package com.buffett.backend.domain.prediction.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 프론트엔드로 내려가는 예측 응답 DTO.
 * - expectedReturn 은 raw 값 (예: 0.045 → 4.5%)
 *   포맷팅("%" 붙이기, 소수점 자리)은 프론트에서 처리합니다.
 * - 해당 ticker에 예측 데이터가 없으면 available=false 로 내려갑니다.
 *   (수익률 0% 와 "데이터 없음" 을 구분하기 위함)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FrontPredictionResponse(
        String ticker,
        boolean available,
        BigDecimal expectedReturn,
        BigDecimal currentPrice,
        BigDecimal predictedPrice,
        LocalDate targetDate,
        LocalDate predictionDate
) {
    public static FrontPredictionResponse unavailable(String ticker) {
        return new FrontPredictionResponse(ticker, false, null, null, null, null, null);
    }
}
