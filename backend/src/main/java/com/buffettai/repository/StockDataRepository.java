package com.buffettai.repository;

import com.buffettai.entity.StockData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT s FROM StockData s WHERE s.ticker = :ticker ORDER BY s.tradeDate DESC LIMIT :days")
    List<StockData> findRecentByTicker(@Param("ticker") String ticker, @Param("days") int days);

    boolean existsByTickerAndTradeDate(String ticker, LocalDate tradeDate);
}
