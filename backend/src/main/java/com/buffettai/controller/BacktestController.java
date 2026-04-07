package com.buffettai.controller;

import com.buffettai.dto.BacktestResponseDto;
import com.buffettai.service.BacktestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 백테스트 API 컨트롤러
 *
 * GET /api/backtest               - 전체 백테스트 결과 목록
 * GET /api/backtest?model=...     - 특정 모델 버전 백테스트 결과
 */
@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestService backtestService;

    @GetMapping
    public ResponseEntity<List<BacktestResponseDto>> getBacktestResults(
            @RequestParam(required = false) String model) {
        if (model != null && !model.isBlank()) {
            return ResponseEntity.ok(backtestService.getBacktestResultsByModelVersion(model));
        }
        return ResponseEntity.ok(backtestService.getAllBacktestResults());
    }
}
