package com.buffett.backend.domain.prediction.service;

import com.buffett.backend.domain.prediction.dto.AiPredictionRequest;
import com.buffett.backend.domain.prediction.dto.FrontPredictionResponse;
import com.buffett.backend.domain.prediction.entity.DailyPrediction;
import com.buffett.backend.domain.prediction.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PredictionService {

    // Magnificent 7
    public static final List<String> M7_TICKERS = List.of(
            "AAPL", "MSFT", "GOOGL", "AMZN", "META", "TSLA", "NVDA"
    );

    private final PredictionRepository predictionRepository;

    // 1. AI 서버가 보낸 예측 데이터를 DB에 저장
    @Transactional
    public void saveAiPredictions(AiPredictionRequest request) {
        var entities = request.predictions().stream()
                .map(item -> DailyPrediction.builder()
                        .ticker(item.ticker())
                        .predictionDate(request.predictionDate())
                        .targetDate(item.targetDate())
                        .currentPrice(item.currentPrice())
                        .predictedPrice(item.predictedPrice())
                        .expectedReturn(item.expectedReturn())
                        .confidenceScore(item.confidenceScore())
                        .modelVersion(request.modelVersion())
                        .build()
                ).toList();

        predictionRepository.saveAll(entities);
    }

    // 2. 단일 종목의 가장 최근 예측을 반환. 없으면 available=false.
    public FrontPredictionResponse getLatestPrediction(String ticker) {
        String normalized = ticker.toUpperCase();
        return predictionRepository.findFirstByTickerOrderByPredictionDateDescIdDesc(normalized)
                .map(this::toResponse)
                .orElseGet(() -> FrontPredictionResponse.unavailable(normalized));
    }

    // 3. M7 전 종목의 가장 최근 예측을 한 번에 반환.
    //    데이터가 없는 종목은 available=false 로 자리만 채워서 내려준다.
    public List<FrontPredictionResponse> getM7Predictions() {
        Map<String, DailyPrediction> byTicker = predictionRepository.findLatestByTickers(M7_TICKERS).stream()
                .collect(Collectors.toMap(DailyPrediction::getTicker, Function.identity()));

        return M7_TICKERS.stream()
                .map(t -> byTicker.containsKey(t)
                        ? toResponse(byTicker.get(t))
                        : FrontPredictionResponse.unavailable(t))
                .toList();
    }

    private FrontPredictionResponse toResponse(DailyPrediction p) {
        return new FrontPredictionResponse(
                p.getTicker(),
                true,
                p.getExpectedReturn(),
                p.getCurrentPrice(),
                p.getPredictedPrice(),
                p.getTargetDate(),
                p.getPredictionDate()
        );
    }
}
