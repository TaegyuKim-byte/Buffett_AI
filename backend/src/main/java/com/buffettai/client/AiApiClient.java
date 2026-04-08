package com.buffettai.client;

import com.buffettai.dto.AiPredictionRequest;
import com.buffettai.dto.AiPredictionResponse;
import com.buffettai.exception.AiApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * AI 서버 HTTP 클라이언트
 *
 * 백엔드 ↔ AI API 호출 규격:
 *   요청: POST /predict  { "ticker": "005930", "target_date": "2026-03-30" }
 *   응답: AiPredictionResponse
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiApiClient {

    private final RestTemplate restTemplate;

    @Value("${ai.api.base-url}")
    private String aiBaseUrl;

    private static final String PREDICT_PATH = "/predict";
    private static final String BACKTEST_PATH = "/backtest";

    /**
     * AI 서버에 단일 종목 예측 요청을 전송합니다.
     *
     * @param request 예측 요청 (ticker + target_date)
     * @return AI 예측 응답
     */
    public AiPredictionResponse requestPrediction(AiPredictionRequest request) {
        String url = aiBaseUrl + PREDICT_PATH;
        log.info("AI 예측 요청: ticker={}, targetDate={}", request.getTicker(), request.getTargetDate());
        try {
            AiPredictionResponse response = restTemplate.postForObject(url, request, AiPredictionResponse.class);
            log.info("AI 예측 응답 수신: status={}", response != null ? response.getStatus() : "null");
            return response;
        } catch (RestClientException e) {
            log.error("AI 서버 호출 실패: {}", e.getMessage());
            throw new AiApiException("AI 서버 호출 실패: " + e.getMessage(), e);
        }
    }
}
