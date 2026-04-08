"""
데이터 수집 모듈
yfinance + pandas를 사용하여 주가 OHLCV 데이터를 수집합니다.

수집 대상:
  - 과거 주가 데이터 (OHLCV)
  - AI 모델 학습 입력 Feature로 사용되는 보조지표 계산
"""

import logging
from datetime import datetime, timedelta
from typing import Optional

import pandas as pd
import yfinance as yf

logger = logging.getLogger(__name__)


def fetch_ohlcv(ticker: str, period: str = "2y", interval: str = "1d") -> pd.DataFrame:
    """
    yfinance를 사용하여 종목의 OHLCV 데이터를 수집합니다.

    Args:
        ticker:   종목 코드 (예: "005930.KS", "AAPL")
        period:   수집 기간 (예: "1y", "2y", "5y")
        interval: 봉 단위 (예: "1d", "1wk")

    Returns:
        OHLCV DataFrame (columns: Open, High, Low, Close, Volume, Adj Close)
    """
    logger.info("주가 데이터 수집 시작: ticker=%s, period=%s", ticker, period)
    try:
        data = yf.download(ticker, period=period, interval=interval, auto_adjust=False, progress=False)
        if data.empty:
            logger.warning("데이터 없음: ticker=%s", ticker)
            return pd.DataFrame()
        data.index = pd.to_datetime(data.index)
        data = data.dropna(subset=["Close"])
        logger.info("주가 데이터 수집 완료: ticker=%s, rows=%d", ticker, len(data))
        return data
    except Exception as e:
        logger.error("주가 데이터 수집 실패: ticker=%s, error=%s", ticker, e)
        raise


def compute_technical_indicators(df: pd.DataFrame) -> pd.DataFrame:
    """
    보조지표(Technical Indicators)를 계산합니다.
    AI 모델의 입력 Feature로 사용됩니다.

    계산 지표:
      - MA5, MA20, MA60: 이동평균선 (5일, 20일, 60일)
      - RSI14: 상대강도지수 (14일)
      - MACD, MACD_signal: MACD 지표
      - BB_upper, BB_lower: 볼린저 밴드 (20일, 2σ)
      - Volume_MA5: 거래량 5일 이동평균

    Args:
        df: OHLCV DataFrame

    Returns:
        보조지표가 추가된 DataFrame
    """
    df = df.copy()
    close = df["Close"]
    volume = df["Volume"]

    # 이동평균선
    df["MA5"] = close.rolling(window=5).mean()
    df["MA20"] = close.rolling(window=20).mean()
    df["MA60"] = close.rolling(window=60).mean()

    # RSI (14일)
    delta = close.diff()
    gain = delta.clip(lower=0).rolling(window=14).mean()
    loss = (-delta.clip(upper=0)).rolling(window=14).mean()
    rs = gain / (loss + 1e-9)
    df["RSI14"] = 100 - (100 / (1 + rs))

    # MACD
    ema12 = close.ewm(span=12, adjust=False).mean()
    ema26 = close.ewm(span=26, adjust=False).mean()
    df["MACD"] = ema12 - ema26
    df["MACD_signal"] = df["MACD"].ewm(span=9, adjust=False).mean()

    # 볼린저 밴드 (20일, 2σ)
    ma20 = close.rolling(window=20).mean()
    std20 = close.rolling(window=20).std()
    df["BB_upper"] = ma20 + 2 * std20
    df["BB_lower"] = ma20 - 2 * std20

    # 거래량 이동평균
    df["Volume_MA5"] = volume.rolling(window=5).mean()

    return df


def compute_target(df: pd.DataFrame, horizon: int = 5) -> pd.DataFrame:
    """
    AI 학습 타겟(Target) 수익률을 계산합니다.
    수식: r_{t,5} = (P_{t+5} - P_t) / P_t

    Args:
        df:      OHLCV + 보조지표 DataFrame
        horizon: 예측 대상 기간 (기본 5일)

    Returns:
        'target_return' 컬럼이 추가된 DataFrame
    """
    df = df.copy()
    df["target_return"] = df["Close"].pct_change(periods=horizon).shift(-horizon)
    return df


def prepare_dataset(ticker: str, period: str = "2y") -> Optional[pd.DataFrame]:
    """
    학습/추론용 데이터셋을 완전히 준비합니다.
    OHLCV 수집 → 보조지표 계산 → 타겟 계산 → NaN 제거

    Args:
        ticker: 종목 코드
        period: 수집 기간

    Returns:
        학습 준비된 DataFrame, 실패 시 None
    """
    df = fetch_ohlcv(ticker, period=period)
    if df.empty:
        return None

    df = compute_technical_indicators(df)
    df = compute_target(df)
    df = df.dropna()
    logger.info("데이터셋 준비 완료: ticker=%s, rows=%d", ticker, len(df))
    return df
