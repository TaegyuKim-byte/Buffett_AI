package com.buffettai.service;

import com.buffettai.client.AiApiClient;
import com.buffettai.dto.AiPredictionRequest;
import com.buffettai.dto.AiPredictionResponse;
import com.buffettai.dto.PredictionResponseDto;
import com.buffettai.entity.Prediction;
import com.buffettai.exception.AiPredictionException;
import com.buffettai.repository.PredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 예측 서비스
 * AI 모델 호출 및 예측 결과 저장/조회를 담당합니다.
 *
 * 예측 로직:
 *   - AI가 과거 30일 OHLCV + 보조지표 + 감성점수를 입력받아
 *     5일 후 수익률 r_{t,5} = (P_{t+5} - P_t) / P_t 를 예측합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final AiApiClient aiApiClient;

    /**
     * 특정 종목에 대해 AI 예측을 요청하고 결과를 DB에 저장합니다.
     *
     * @param ticker     종목 코드 (예: "005930")
     * @param targetDate 예측 기준일
     * @return 저장된 예측 결과 DTO
     */
    @Transactional
    public PredictionResponseDto requestAndSavePrediction(String ticker, LocalDate targetDate) {
        AiPredictionRequest request = new AiPredictionRequest(ticker, targetDate);
        AiPredictionResponse response = aiApiClient.requestPrediction(request);

        if (response == null || response.getPredictions() == null || response.getPredictions().isEmpty()) {
            throw new AiPredictionException(
                "AI 서버로부터 유효한 예측 결과를 받지 못했습니다. ticker=" + ticker + ", targetDate=" + targetDate
            );
        }

        AiPredictionResponse.PredictionItem item = response.getPredictions().get(0);
        Prediction prediction = new Prediction();
        prediction.setTicker(item.getTicker());
        prediction.setPredDate(item.getPredDate());
        prediction.setTargetDate(item.getTargetDate());
        prediction.setCurrentPrice(item.getCurrentPrice());
        prediction.setPredReturn(item.getPredReturn());
        prediction.setPredPrice(item.getPredPrice());
        prediction.setConfidence(item.getConfidence());
        prediction.setAnalysisNote(item.getAnalysisNote());
        prediction.setModelVersion(response.getMetadata() != null ? response.getMetadata().getModelVersion() : null);
        prediction.setGeneratedAt(response.getMetadata() != null ? response.getMetadata().getGeneratedAt() : null);

        Prediction saved = predictionRepository.save(prediction);
        log.info("예측 결과 저장: ticker={}, predReturn={}", ticker, item.getPredReturn());
        return toDto(saved);
    }

    /**
     * 특정 종목의 최근 예측 결과 목록을 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<PredictionResponseDto> getPredictionsByTicker(String ticker) {
        return predictionRepository.findByTickerOrderByPredDateDesc(ticker)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜 기준 예측 수익률 상위 10개 종목을 반환합니다. (매수 후보군)
     */
    @Transactional(readOnly = true)
    public List<PredictionResponseDto> getTopPredictionsByDate(LocalDate predDate) {
        return predictionRepository.findTop10ByPredDateOrderByPredReturnDesc(predDate)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private PredictionResponseDto toDto(Prediction p) {
        return PredictionResponseDto.builder()
                .id(p.getId())
                .ticker(p.getTicker())
                .predDate(p.getPredDate())
                .targetDate(p.getTargetDate())
                .currentPrice(p.getCurrentPrice())
                .predReturn(p.getPredReturn())
                .predPrice(p.getPredPrice())
                .confidence(p.getConfidence())
                .analysisNote(p.getAnalysisNote())
                .modelVersion(p.getModelVersion())
                .generatedAt(p.getGeneratedAt())
                .build();
    }
}
