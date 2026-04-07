package com.buffettai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주가 데이터 테이블 (과거 OHLCV 차트)
 * 종목별 일별 시가/고가/저가/종가/거래량을 저장합니다.
 */
@Entity
@Table(
    name = "stock_data",
    indexes = {
        @Index(name = "idx_stock_data_ticker_date", columnList = "ticker, trade_date"),
        @Index(name = "idx_stock_data_trade_date", columnList = "trade_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_data_ticker_date", columnNames = {"ticker", "trade_date"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class StockData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "open_price", precision = 18, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "high_price", precision = 18, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 18, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal closePrice;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "adjusted_close", precision = 18, scale = 2)
    private BigDecimal adjustedClose;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
