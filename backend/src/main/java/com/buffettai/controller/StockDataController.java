package com.buffettai.controller;

import com.buffettai.dto.StockDataResponseDto;
import com.buffettai.service.StockDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 주가 데이터 API 컨트롤러
 *
 * GET /api/stocks/{ticker}/history?startDate=...&endDate=...  - 기간별 주가 조회
 * GET /api/stocks/{ticker}/recent?days=30                     - 최근 N일 주가 조회
 */
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockDataController {

    private final StockDataService stockDataService;

    @GetMapping("/{ticker}/history")
    public ResponseEntity<List<StockDataResponseDto>> getStockHistory(
            @PathVariable String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(stockDataService.getStockData(ticker, startDate, endDate));
    }

    @GetMapping("/{ticker}/recent")
    public ResponseEntity<List<StockDataResponseDto>> getRecentStockData(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(stockDataService.getRecentStockData(ticker, days));
    }
}
