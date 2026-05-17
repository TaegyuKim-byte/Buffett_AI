package com.buffett.backend.domain.prediction.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction")
@Getter
@NoArgsConstructor
public class DailyPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticker;
    private LocalDate predictionDate;
    private LocalDate targetDate;
    private BigDecimal currentPrice;
    private BigDecimal predictedPrice;
    private BigDecimal expectedReturn;
    private BigDecimal confidenceScore;
    private String analysisNote;
    private String modelVersion;
    private LocalDateTime generatedAt;

    @Builder
    public DailyPrediction(String ticker, LocalDate predictionDate, LocalDate targetDate,
                           BigDecimal currentPrice, BigDecimal predictedPrice, BigDecimal expectedReturn,
                           BigDecimal confidenceScore, String modelVersion) {
        this.ticker = ticker;
        this.predictionDate = predictionDate;
        this.targetDate = targetDate;
        this.currentPrice = currentPrice;
        this.predictedPrice = predictedPrice;
        this.expectedReturn = expectedReturn;
        this.confidenceScore = confidenceScore;
        this.modelVersion = modelVersion;
        this.generatedAt = LocalDateTime.now();
    }
}
