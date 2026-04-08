package com.buffettai.repository;

import com.buffettai.entity.StockData;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockDataRepository extends JpaRepository<StockData, Long> {

    List<StockData> findByTickerOrderByTradeDateDesc(String ticker);

    List<StockData> findByTickerAndTradeDateBetweenOrderByTradeDateAsc(
            String ticker, LocalDate startDate, LocalDate endDate);

    Optional<StockData> findByTickerAndTradeDate(String ticker, LocalDate tradeDate);

    List<StockData> findByTickerOrderByTradeDateDesc(String ticker, Pageable pageable);

    boolean existsByTickerAndTradeDate(String ticker, LocalDate tradeDate);

    default List<StockData> findRecentByTicker(String ticker, int days) {
        return findByTickerOrderByTradeDateDesc(ticker, PageRequest.of(0, days));
    }
}
