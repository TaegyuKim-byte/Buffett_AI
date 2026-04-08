package com.buffettai.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 백테스트 결과 응답 DTO (프론트엔드용)
 */
@Getter
@Builder
public class BacktestResponseDto {

    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String modelVersion;
    private BigDecimal totalReturn;
    private BigDecimal maxDrawdown;
    private BigDecimal sharpeRatio;
    private BigDecimal accuracy;
    private Integer totalTrades;
    private Integer winningTrades;
    private String strategyDescription;
    private LocalDateTime createdAt;
}
