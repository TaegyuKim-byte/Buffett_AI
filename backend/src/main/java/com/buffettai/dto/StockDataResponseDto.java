package com.buffettai.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주가 데이터 응답 DTO (프론트엔드용)
 */
@Getter
@Builder
public class StockDataResponseDto {

    private Long id;
    private String ticker;
    private LocalDate tradeDate;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private Long volume;
    private BigDecimal adjustedClose;
}
