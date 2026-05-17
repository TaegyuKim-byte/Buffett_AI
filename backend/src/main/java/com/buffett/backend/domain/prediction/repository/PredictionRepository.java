package com.buffett.backend.domain.prediction.repository;

import com.buffett.backend.domain.prediction.entity.DailyPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<DailyPrediction, Long> {

    // 특정 종목의 가장 최근 예측 데이터를 가져온다.
    // predictionDate가 같은 날 중복으로 들어왔을 때를 대비해 id로 tie-break.
    Optional<DailyPrediction> findFirstByTickerOrderByPredictionDateDescIdDesc(String ticker);

    // 여러 종목(M7 등)의 가장 최근 예측을 한 번에 조회.
    // 각 ticker별 최대 id 행만 추려서 반환한다.
    @Query("""
            SELECT p FROM DailyPrediction p
            WHERE p.id IN (
                SELECT MAX(p2.id) FROM DailyPrediction p2
                WHERE p2.ticker IN :tickers
                GROUP BY p2.ticker
            )
            """)
    List<DailyPrediction> findLatestByTickers(@Param("tickers") Collection<String> tickers);
}
