package com.buffettai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * AI 서버 요청 DTO
 * 백엔드가 AI API를 호출할 때 전송하는 요청 형식입니다.
 *
 * 요청 예시:
 * { "ticker": "005930", "target_date": "2026-03-30" }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiPredictionRequest {

    @NotBlank(message = "종목 코드는 필수입니다.")
    private String ticker;

    @NotNull(message = "예측 기준일은 필수입니다.")
    @JsonProperty("target_date")
    private LocalDate targetDate;
}
