package com.buffettai.service;

import com.buffettai.dto.BacktestResponseDto;
import com.buffettai.entity.BacktestResult;
import com.buffettai.repository.BacktestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 백테스트 서비스
 * 백테스트 결과를 DB에서 조회하여 반환합니다.
 * 실제 백테스트 실행은 Python AI 모듈이 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestService {

    private final BacktestResultRepository backtestResultRepository;

    /**
     * 전체 백테스트 결과 목록을 최신순으로 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<BacktestResponseDto> getAllBacktestResults() {
        return backtestResultRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 모델 버전의 백테스트 결과를 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<BacktestResponseDto> getBacktestResultsByModelVersion(String modelVersion) {
        return backtestResultRepository.findByModelVersionOrderByCreatedAtDesc(modelVersion)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private BacktestResponseDto toDto(BacktestResult b) {
        return BacktestResponseDto.builder()
                .id(b.getId())
                .startDate(b.getStartDate())
                .endDate(b.getEndDate())
                .modelVersion(b.getModelVersion())
                .totalReturn(b.getTotalReturn())
                .maxDrawdown(b.getMaxDrawdown())
                .sharpeRatio(b.getSharpeRatio())
                .accuracy(b.getAccuracy())
                .totalTrades(b.getTotalTrades())
                .winningTrades(b.getWinningTrades())
                .strategyDescription(b.getStrategyDescription())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
