package com.buffett.backend.domain.prediction.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.time.LocalDate;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AiPredictionRequest(
        LocalDate predictionDate,
        String modelVersion,
        List<PredictionItem> predictions
) {}
