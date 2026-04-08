"""
AI 모듈 단위 테스트
"""

import numpy as np
import pandas as pd
import pytest
import torch

from ai.data.collector import compute_technical_indicators, compute_target
from ai.model.lstm_model import LSTMStockPredictor
from ai.model.train import create_sequences
from ai.backtest.backtest import (
    run_backtest,
    _calculate_max_drawdown,
    _calculate_sharpe_ratio,
)


# ── 샘플 OHLCV 데이터 생성 헬퍼 ───────────────────────────────────

def make_sample_ohlcv(n: int = 100) -> pd.DataFrame:
    """테스트용 OHLCV DataFrame을 생성합니다."""
    np.random.seed(42)
    close = 50000 + np.cumsum(np.random.randn(n) * 500)
    df = pd.DataFrame(
        {
            "Open": close * 0.99,
            "High": close * 1.01,
            "Low": close * 0.98,
            "Close": close,
            "Volume": np.random.randint(100_000, 1_000_000, n).astype(float),
            "Adj Close": close,
        },
        index=pd.date_range("2024-01-01", periods=n, freq="B"),
    )
    return df


# ── 데이터 수집 테스트 ────────────────────────────────────────────

class TestTechnicalIndicators:
    def test_ma_columns_exist(self):
        df = make_sample_ohlcv(100)
        result = compute_technical_indicators(df)
        for col in ["MA5", "MA20", "MA60", "RSI14", "MACD", "MACD_signal", "BB_upper", "BB_lower", "Volume_MA5"]:
            assert col in result.columns, f"컬럼 누락: {col}"

    def test_ma5_value(self):
        df = make_sample_ohlcv(100)
        result = compute_technical_indicators(df)
        expected = df["Close"].rolling(5).mean()
        pd.testing.assert_series_equal(result["MA5"], expected, check_names=False)

    def test_rsi_range(self):
        df = make_sample_ohlcv(100)
        result = compute_technical_indicators(df)
        rsi = result["RSI14"].dropna()
        assert (rsi >= 0).all() and (rsi <= 100).all(), "RSI 범위 오류"


class TestComputeTarget:
    def test_target_column_exists(self):
        df = make_sample_ohlcv(100)
        df = compute_technical_indicators(df)
        result = compute_target(df)
        assert "target_return" in result.columns

    def test_target_formula(self):
        """r_{t,5} = (P_{t+5} - P_t) / P_t 검증"""
        df = make_sample_ohlcv(100)
        result = compute_target(df)
        close = df["Close"]
        expected = close.pct_change(periods=5).shift(-5)
        pd.testing.assert_series_equal(
            result["target_return"], expected, check_names=False, rtol=1e-5
        )


# ── LSTM 모델 테스트 ──────────────────────────────────────────────

class TestLSTMModel:
    def test_forward_shape(self):
        model = LSTMStockPredictor(input_size=14, hidden_size=64, num_layers=2)
        model.eval()
        x = torch.randn(8, 30, 14)  # (batch=8, seq=30, features=14)
        with torch.no_grad():
            output = model(x)
        assert output.shape == (8, 1), f"출력 shape 오류: {output.shape}"

    def test_single_sample(self):
        model = LSTMStockPredictor(input_size=14)
        model.eval()
        x = torch.randn(1, 30, 14)
        with torch.no_grad():
            output = model(x)
        assert output.shape == (1, 1)
        assert not torch.isnan(output).any()


# ── 시퀀스 생성 테스트 ────────────────────────────────────────────

class TestCreateSequences:
    def test_sequence_shape(self):
        n, f = 100, 14
        features = np.random.randn(n, f).astype(np.float32)
        targets = np.random.randn(n).astype(np.float32)
        X, y = create_sequences(features, targets, window_size=30)
        assert X.shape == (70, 30, f), f"X shape 오류: {X.shape}"
        assert y.shape == (70,), f"y shape 오류: {y.shape}"

    def test_sequence_values(self):
        features = np.arange(100).reshape(50, 2).astype(np.float32)
        targets = np.arange(50).astype(np.float32)
        X, y = create_sequences(features, targets, window_size=5)
        # 첫 번째 시퀀스: features[0:5]
        np.testing.assert_array_equal(X[0], features[0:5])
        assert y[0] == targets[5]


# ── 백테스트 테스트 ───────────────────────────────────────────────

class TestBacktest:
    def make_predictions_df(self) -> pd.DataFrame:
        return pd.DataFrame(
            {
                "ticker": ["005930", "000660", "AAPL"] * 5,
                "pred_date": pd.date_range("2024-01-02", periods=5, freq="B").repeat(3),
                "target_date": pd.date_range("2024-01-09", periods=5, freq="B").repeat(3),
                "pred_return": [0.05, 0.03, -0.01, 0.04, 0.02, -0.02, 0.06, 0.01, -0.03, 0.07, 0.03, 0.01, 0.02, -0.01, 0.04],
                "current_price": [75000, 90000, 180] * 5,
            }
        )

    def test_threshold_strategy(self):
        df = self.make_predictions_df()
        result = run_backtest(
            tickers=["005930", "000660", "AAPL"],
            start_date="2024-01-02",
            end_date="2024-01-15",
            predictions=df,
            strategy="threshold",
            threshold=0.02,
        )
        assert "total_return" in result
        assert "max_drawdown" in result
        assert result["total_trades"] > 0

    def test_top_n_strategy(self):
        df = self.make_predictions_df()
        result = run_backtest(
            tickers=["005930", "000660", "AAPL"],
            start_date="2024-01-02",
            end_date="2024-01-15",
            predictions=df,
            strategy="top_n",
            top_n=2,
        )
        assert result["total_trades"] == 10  # 5일 × 상위2개

    def test_max_drawdown_calculation(self):
        cumulative = np.array([1.0, 1.1, 1.05, 0.9, 0.95, 1.0])
        mdd = _calculate_max_drawdown(cumulative)
        # 최고점 1.1에서 0.9로 낙폭 = (0.9 - 1.1) / 1.1
        expected = (0.9 - 1.1) / 1.1
        assert abs(mdd - expected) < 1e-5

    def test_sharpe_ratio_zero_variance(self):
        returns = np.zeros(10)
        sharpe = _calculate_sharpe_ratio(returns)
        assert sharpe == 0.0

    def test_empty_predictions(self):
        df = pd.DataFrame(columns=["ticker", "pred_date", "target_date", "pred_return", "current_price"])
        result = run_backtest(
            tickers=["005930"],
            start_date="2024-01-01",
            end_date="2024-01-31",
            predictions=df,
        )
        assert result["total_trades"] == 0
        assert result["total_return"] == 0.0
