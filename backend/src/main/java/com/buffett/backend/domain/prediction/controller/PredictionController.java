package com.buffett.backend.domain.prediction.controller;

import com.buffett.backend.domain.prediction.dto.AiPredictionRequest;
import com.buffett.backend.domain.prediction.dto.FrontPredictionResponse;
import com.buffett.backend.domain.prediction.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PredictionController {

    private final PredictionService predictionService;

    // 1. AI 서버 → 백엔드: 예측 데이터 수신 (POST /api/predictions)
    @PostMapping
    public ResponseEntity<Map<String, Object>> receivePredictions(@RequestBody AiPredictionRequest request) {
        predictionService.saveAiPredictions(request);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "총 " + request.predictions().size() + "건의 예측 데이터가 성공적으로 반영되었습니다.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // 2. 프론트 → 백엔드: M7 전체 예측 한 번에 조회 (GET /api/predictions/m7)
    //    7번 round-trip 없이 한 방에 7종목 결과를 받는다.
    @GetMapping("/m7")
    public ResponseEntity<List<FrontPredictionResponse>> getM7Predictions() {
        return ResponseEntity.ok(predictionService.getM7Predictions());
    }

    // 3. 프론트 → 백엔드: 단일 종목 예측 조회 (GET /api/predictions/AAPL)
    @GetMapping("/{ticker}")
    public ResponseEntity<FrontPredictionResponse> getOnePrediction(@PathVariable String ticker) {
        return ResponseEntity.ok(predictionService.getLatestPrediction(ticker));
    }
}
