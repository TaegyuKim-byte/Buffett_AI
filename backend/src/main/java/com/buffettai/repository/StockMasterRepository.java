package com.buffettai.repository;

import com.buffettai.entity.StockMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockMasterRepository extends JpaRepository<StockMaster, Long> {

    Optional<StockMaster> findByTicker(String ticker);

    List<StockMaster> findByIsActiveTrue();

    boolean existsByTicker(String ticker);
}
