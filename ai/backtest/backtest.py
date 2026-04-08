"""
백테스트 모듈

전략:
  1. threshold 기반 매수: pred_return > threshold 이면 매수
  2. 상위 N개 매수: 예측 수익률 상위 N개 종목을 매수

평가 지표:
  - 전체 수익률 (total_return)
  - 최대 낙폭 MDD (max_drawdown)
  - 샤프 비율 (sharpe_ratio)
  - 방향성 정확도 (direction_accuracy)
"""

import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional

import numpy as np
import pandas as pd

logger = logging.getLogger(__name__)


def run_backtest(
    tickers: List[str],
    start_date: str,
    end_date: str,
    predictions: pd.DataFrame,
    strategy: str = "threshold",
    threshold: float = 0.02,
    top_n: int = 5,
    initial_capital: float = 10_000_000,
) -> Dict:
    """
    예측 결과를 기반으로 백테스트를 실행합니다.

    Args:
        tickers:         백테스트 대상 종목 목록
        start_date:      백테스트 시작일 (YYYY-MM-DD)
        end_date:        백테스트 종료일 (YYYY-MM-DD)
        predictions:     예측 결과 DataFrame
                         (columns: ticker, pred_date, target_date, pred_return, current_price)
        strategy:        매매 전략 ("threshold" 또는 "top_n")
        threshold:       threshold 기반 매수 기준 수익률
        top_n:           상위 N개 매수 기준
        initial_capital: 초기 투자 자본 (원)

    Returns:
        백테스트 결과 딕셔너리:
        {
            "start_date": ..., "end_date": ...,
            "total_return": ..., "max_drawdown": ...,
            "sharpe_ratio": ..., "accuracy": ...,
            "total_trades": ..., "winning_trades": ...,
            "strategy_description": ...
        }
    """
    logger.info("백테스트 시작: %s ~ %s, 전략=%s", start_date, end_date, strategy)

    trades = []
    pred_df = predictions.copy()
    pred_df["pred_date"] = pd.to_datetime(pred_df["pred_date"])
    pred_df["target_date"] = pd.to_datetime(pred_df["target_date"])

    for date, group in pred_df.groupby("pred_date"):
        if strategy == "threshold":
            selected = group[group["pred_return"] > threshold]
        elif strategy == "top_n":
            selected = group.nlargest(top_n, "pred_return")
        else:
            raise ValueError(f"알 수 없는 전략: {strategy}")

        for _, row in selected.iterrows():
            trades.append(row.to_dict())

    if not trades:
        logger.warning("매매 내역 없음")
        return _empty_result(start_date, end_date, strategy)

    trades_df = pd.DataFrame(trades)

    # 실제 수익률 계산 (current_price와 actual_return이 있을 경우)
    if "actual_return" in trades_df.columns:
        returns = trades_df["actual_return"].values
    else:
        # 예측 수익률로 대체 (백테스트 시뮬레이션)
        returns = trades_df["pred_return"].values

    direction_correct = np.sign(returns) == np.sign(trades_df["pred_return"].values)
    accuracy = float(direction_correct.mean()) if len(direction_correct) > 0 else 0.0

    # 포트폴리오 수익 계산
    portfolio_returns = []
    for date, group in trades_df.groupby("pred_date"):
        day_return = group["pred_return"].mean()
        portfolio_returns.append(day_return)

    portfolio_returns = np.array(portfolio_returns)
    cumulative = np.cumprod(1 + portfolio_returns)
    total_return = float(cumulative[-1] - 1) if len(cumulative) > 0 else 0.0

    # 최대 낙폭 (MDD)
    max_drawdown = _calculate_max_drawdown(cumulative)

    # 샤프 비율 (무위험 수익률 0% 가정)
    sharpe_ratio = _calculate_sharpe_ratio(portfolio_returns)

    winning = int((returns > 0).sum())

    strategy_desc = (
        f"threshold={threshold}" if strategy == "threshold" else f"top_{top_n}"
    )

    result = {
        "start_date": start_date,
        "end_date": end_date,
        "model_version": "v1.0-LSTM-PyTorch",
        "total_return": round(total_return * 100, 4),
        "max_drawdown": round(max_drawdown * 100, 4),
        "sharpe_ratio": round(sharpe_ratio, 4),
        "accuracy": round(accuracy, 4),
        "total_trades": len(trades),
        "winning_trades": winning,
        "strategy_description": f"{strategy} ({strategy_desc})",
    }
    logger.info("백테스트 완료: total_return=%.2f%%, MDD=%.2f%%", result["total_return"], result["max_drawdown"])
    return result


def _calculate_max_drawdown(cumulative: np.ndarray) -> float:
    """최대 낙폭(MDD)을 계산합니다."""
    if len(cumulative) == 0:
        return 0.0
    peak = np.maximum.accumulate(cumulative)
    drawdown = (cumulative - peak) / (peak + 1e-9)
    return float(drawdown.min())


def _calculate_sharpe_ratio(returns: np.ndarray, risk_free_rate: float = 0.0) -> float:
    """샤프 비율을 계산합니다. (연환산 기준, 252 거래일)"""
    if len(returns) < 2:
        return 0.0
    excess_returns = returns - risk_free_rate / 252
    std = np.std(excess_returns)
    if std < 1e-9:
        return 0.0
    return float(np.mean(excess_returns) / std * np.sqrt(252))


def _empty_result(start_date: str, end_date: str, strategy: str) -> Dict:
    return {
        "start_date": start_date,
        "end_date": end_date,
        "model_version": "v1.0-LSTM-PyTorch",
        "total_return": 0.0,
        "max_drawdown": 0.0,
        "sharpe_ratio": 0.0,
        "accuracy": 0.0,
        "total_trades": 0,
        "winning_trades": 0,
        "strategy_description": strategy,
    }
