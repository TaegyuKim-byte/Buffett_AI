package com.buffettai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 백테스트 결과 테이블 (AI 모델 성적표)
 * 특정 기간 동안 AI 예측을 기반으로 매매했을 때의 성과를 저장합니다.
 */
@Entity
@Table(
    name = "backtest_result",
    indexes = {
        @Index(name = "idx_backtest_start_end", columnList = "start_date, end_date"),
        @Index(name = "idx_backtest_model_version", columnList = "model_version")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class BacktestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    /** 전체 수익률 (%) */
    @Column(name = "total_return", precision = 10, scale = 4)
    private BigDecimal totalReturn;

    /** 최대 낙폭 MDD (Maximum DrawDown, %) */
    @Column(name = "max_drawdown", precision = 10, scale = 4)
    private BigDecimal maxDrawdown;

    /** 샤프 비율 */
    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    /** 예측 정확도 (방향성) */
    @Column(name = "accuracy", precision = 5, scale = 4)
    private BigDecimal accuracy;

    /** 총 거래 횟수 */
    @Column(name = "total_trades")
    private Integer totalTrades;

    /** 수익 거래 횟수 */
    @Column(name = "winning_trades")
    private Integer winningTrades;

    /** 백테스트 전략 설명 */
    @Column(name = "strategy_description", length = 255)
    private String strategyDescription;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
