package com.buffettai.controller;

import com.buffettai.dto.PredictionResponseDto;
import com.buffettai.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 예측 API 컨트롤러
 *
 * GET  /api/predictions/{ticker}       - 종목별 예측 이력 조회
 * GET  /api/predictions/top?date=...   - 특정 날짜 예측 수익률 상위 10 종목
 * POST /api/predictions/request        - AI 예측 요청 (ticker + targetDate)
 */
@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @GetMapping("/{ticker}")
    public ResponseEntity<List<PredictionResponseDto>> getPredictionsByTicker(
            @PathVariable String ticker) {
        return ResponseEntity.ok(predictionService.getPredictionsByTicker(ticker));
    }

    @GetMapping("/top")
    public ResponseEntity<List<PredictionResponseDto>> getTopPredictions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(predictionService.getTopPredictionsByDate(targetDate));
    }

    @PostMapping("/request")
    public ResponseEntity<PredictionResponseDto> requestPrediction(
            @RequestParam String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
        return ResponseEntity.ok(predictionService.requestAndSavePrediction(ticker, targetDate));
    }
}
