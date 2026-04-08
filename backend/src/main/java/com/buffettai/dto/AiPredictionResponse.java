package com.buffettai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 서버 응답 DTO
 * AI API가 반환하는 예측 결과 형식을 정의합니다.
 *
 * 응답 예시:
 * {
 *   "status": "success",
 *   "metadata": { "generated_at": "...", "model_version": "v1.2-LSTM-PyTorch", "total_count": 1 },
 *   "predictions": [ { "ticker": "005930", "pred_return": 0.045, ... } ]
 * }
 */
@Getter
@Setter
@NoArgsConstructor
public class AiPredictionResponse {

    private String status;
    private Metadata metadata;
    private List<PredictionItem> predictions;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Metadata {
        @JsonProperty("generated_at")
        private LocalDateTime generatedAt;

        @JsonProperty("model_version")
        private String modelVersion;

        @JsonProperty("total_count")
        private int totalCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PredictionItem {
        private String ticker;

        @JsonProperty("pred_date")
        private LocalDate predDate;

        @JsonProperty("target_date")
        private LocalDate targetDate;

        @JsonProperty("current_price")
        private BigDecimal currentPrice;

        /** AI 예측 수익률 r_{t,5} = (P_{t+5} - P_t) / P_t */
        @JsonProperty("pred_return")
        private BigDecimal predReturn;

        @JsonProperty("pred_price")
        private BigDecimal predPrice;

        private BigDecimal confidence;

        @JsonProperty("analysis_note")
        private String analysisNote;
    }
}
