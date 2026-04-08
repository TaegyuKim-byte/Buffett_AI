package com.buffettai.repository;

import com.buffettai.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    List<Prediction> findByTickerOrderByPredDateDesc(String ticker);

    List<Prediction> findByPredDateOrderByPredReturnDesc(LocalDate predDate);

    Optional<Prediction> findByTickerAndPredDate(String ticker, LocalDate predDate);

    List<Prediction> findByPredDateBetweenOrderByPredDateDesc(LocalDate start, LocalDate end);

    /**
     * 특정 날짜 기준 상위 N개 종목 (예측 수익률 내림차순)
     */
    List<Prediction> findTop10ByPredDateOrderByPredReturnDesc(LocalDate predDate);
}
