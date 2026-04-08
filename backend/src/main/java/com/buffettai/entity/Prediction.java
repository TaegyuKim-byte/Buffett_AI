package com.buffettai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예측 결과 테이블
 * AI 모델이 생성한 종목별 5일 후 수익률 예측 결과를 저장합니다.
 * 예측 수식: r_{t,5} = (P_{t+5} - P_t) / P_t
 */
@Entity
@Table(
    name = "prediction",
    indexes = {
        @Index(name = "idx_prediction_ticker_pred_date", columnList = "ticker, pred_date"),
        @Index(name = "idx_prediction_target_date", columnList = "target_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String ticker;

    /** 예측 생성일 (AI가 분석을 수행한 날짜) */
    @Column(name = "pred_date", nullable = false)
    private LocalDate predDate;

    /** 예측 대상일 (pred_date + 5 영업일) */
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "current_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal currentPrice;

    /** AI 예측 수익률 r_{t,5} */
    @Column(name = "pred_return", nullable = false, precision = 10, scale = 6)
    private BigDecimal predReturn;

    /** AI 예측 가격 P_{t+5} = P_t * (1 + pred_return) */
    @Column(name = "pred_price", precision = 18, scale = 2)
    private BigDecimal predPrice;

    /** 모델 예측 신뢰도 (0~1) */
    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    /** 예측 근거 분석 메모 */
    @Column(name = "analysis_note", length = 500)
    private String analysisNote;

    /** 사용된 모델 버전 */
    @Column(name = "model_version", length = 50)
    private String modelVersion;

    /** 예측 생성 타임스탬프 */
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
