package com.buffettai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예측 결과 응답 DTO (프론트엔드용)
 */
@Getter
@Builder
public class PredictionResponseDto {

    private Long id;
    private String ticker;

    @JsonProperty("pred_date")
    private LocalDate predDate;

    @JsonProperty("target_date")
    private LocalDate targetDate;

    @JsonProperty("current_price")
    private BigDecimal currentPrice;

    @JsonProperty("pred_return")
    private BigDecimal predReturn;

    @JsonProperty("pred_price")
    private BigDecimal predPrice;

    private BigDecimal confidence;

    @JsonProperty("analysis_note")
    private String analysisNote;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("generated_at")
    private LocalDateTime generatedAt;
}
