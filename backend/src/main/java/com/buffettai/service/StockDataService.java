package com.buffettai.service;

import com.buffettai.dto.StockDataResponseDto;
import com.buffettai.entity.StockData;
import com.buffettai.repository.StockDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주가 데이터 서비스
 * 과거 OHLCV 데이터 조회를 담당합니다.
 * 데이터 수집은 Python AI 모듈(yfinance)이 담당하고,
 * 백엔드는 DB에 저장된 데이터를 프론트엔드에 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockDataService {

    private final StockDataRepository stockDataRepository;

    /**
     * 특정 종목의 기간별 주가 데이터를 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<StockDataResponseDto> getStockData(String ticker, LocalDate startDate, LocalDate endDate) {
        return stockDataRepository.findByTickerAndTradeDateBetweenOrderByTradeDateAsc(ticker, startDate, endDate)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 종목의 최근 N일 주가 데이터를 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<StockDataResponseDto> getRecentStockData(String ticker, int days) {
        return stockDataRepository.findRecentByTicker(ticker, days)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private StockDataResponseDto toDto(StockData s) {
        return StockDataResponseDto.builder()
                .id(s.getId())
                .ticker(s.getTicker())
                .tradeDate(s.getTradeDate())
                .openPrice(s.getOpenPrice())
                .highPrice(s.getHighPrice())
                .lowPrice(s.getLowPrice())
                .closePrice(s.getClosePrice())
                .volume(s.getVolume())
                .adjustedClose(s.getAdjustedClose())
                .build();
    }
}
