package com.buffett.backend.domain.prediction.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) // 스네이크 케이스 자동 변환
public record PredictionItem(
        String ticker,
        LocalDate targetDate,
        BigDecimal currentPrice,
        BigDecimal predictedPrice,
        BigDecimal expectedReturn,
        BigDecimal confidenceScore
) {}
