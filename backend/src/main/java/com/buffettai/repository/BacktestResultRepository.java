package com.buffettai.repository;

import com.buffettai.entity.BacktestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BacktestResultRepository extends JpaRepository<BacktestResult, Long> {

    List<BacktestResult> findByModelVersionOrderByCreatedAtDesc(String modelVersion);

    List<BacktestResult> findAllByOrderByCreatedAtDesc();
}
