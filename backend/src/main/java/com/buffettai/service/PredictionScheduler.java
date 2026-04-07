package com.buffettai.service;

import com.buffettai.entity.StockMaster;
import com.buffettai.repository.StockMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 예측 스케줄링 서비스
 *
 * 예측 업데이트 타임라인:
 *   - 매일 오후 4시 (장 마감 후): 당일 주가 데이터 수집 시작
 *   - 매일 오후 5시: AI 모델 추론(Inference) 실행 → DB 저장
 *   - 다음날 오전 9시: 예측 결과 확인 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionScheduler {

    private final PredictionService predictionService;
    private final StockMasterRepository stockMasterRepository;

    /**
     * 매일 오후 5시에 활성화된 모든 종목에 대해 AI 예측을 요청합니다.
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 17 * * MON-FRI", zone = "Asia/Seoul")
    public void scheduleDailyPrediction() {
        LocalDate today = LocalDate.now();
        log.info("[스케줄러] 일별 예측 시작: {}", today);

        List<StockMaster> activeStocks = stockMasterRepository.findByIsActiveTrue();
        log.info("[스케줄러] 예측 대상 종목 수: {}", activeStocks.size());

        for (StockMaster stock : activeStocks) {
            try {
                predictionService.requestAndSavePrediction(stock.getTicker(), today);
                log.info("[스케줄러] 예측 완료: {}", stock.getTicker());
            } catch (Exception e) {
                log.error("[스케줄러] 예측 실패: ticker={}, error={}", stock.getTicker(), e.getMessage());
            }
        }

        log.info("[스케줄러] 일별 예측 완료: {} 종목 처리", activeStocks.size());
    }
}
