"""
모델 학습 모듈

학습 흐름:
  1. yfinance로 OHLCV 데이터 수집
  2. 보조지표 계산 (MA, RSI, MACD, BB)
  3. 시계열 슬라이딩 윈도우 데이터셋 생성 (window_size=30)
  4. LSTM 모델 학습
  5. 모델 저장 (checkpoints/)

평가 지표:
  - MSE (Mean Squared Error)
  - 수익률 방향성 정확도 (Direction Accuracy)
"""

import logging
import os
from pathlib import Path
from typing import List, Tuple

import numpy as np
import torch
import torch.nn as nn
from sklearn.preprocessing import StandardScaler
from torch.utils.data import DataLoader, TensorDataset

from ai.data.collector import prepare_dataset
from ai.model.lstm_model import LSTMStockPredictor

logger = logging.getLogger(__name__)

# 학습에 사용할 Feature 컬럼
FEATURE_COLS = [
    "Open", "High", "Low", "Close", "Volume",
    "MA5", "MA20", "MA60",
    "RSI14", "MACD", "MACD_signal",
    "BB_upper", "BB_lower", "Volume_MA5",
]
TARGET_COL = "target_return"
WINDOW_SIZE = 30  # 과거 30일치 데이터를 입력으로 사용
CHECKPOINT_DIR = Path(__file__).parent.parent / "checkpoints"


def create_sequences(
    features: np.ndarray, targets: np.ndarray, window_size: int
) -> Tuple[np.ndarray, np.ndarray]:
    """
    시계열 슬라이딩 윈도우 데이터셋을 생성합니다.

    Args:
        features:    (N, feature_size) 특성 배열
        targets:     (N,) 타겟 배열
        window_size: 윈도우 크기 (기본 30일)

    Returns:
        X: (N - window_size, window_size, feature_size)
        y: (N - window_size,)
    """
    X, y = [], []
    for i in range(window_size, len(features)):
        X.append(features[i - window_size : i])
        y.append(targets[i])
    return np.array(X, dtype=np.float32), np.array(y, dtype=np.float32)


def train(
    tickers: List[str],
    epochs: int = 50,
    batch_size: int = 64,
    learning_rate: float = 1e-3,
    model_version: str = "v1.0-LSTM-PyTorch",
) -> LSTMStockPredictor:
    """
    여러 종목 데이터를 합쳐서 LSTM 모델을 학습합니다.

    Args:
        tickers:        학습 대상 종목 코드 목록
        epochs:         학습 에포크 수
        batch_size:     배치 크기
        learning_rate:  학습률
        model_version:  저장될 모델 버전명

    Returns:
        학습 완료된 LSTMStockPredictor 모델
    """
    all_X, all_y = [], []
    scaler = StandardScaler()

    for ticker in tickers:
        logger.info("데이터 준비: %s", ticker)
        df = prepare_dataset(ticker)
        if df is None or df.empty:
            logger.warning("데이터 없음, 건너뜀: %s", ticker)
            continue

        features = df[FEATURE_COLS].values
        targets = df[TARGET_COL].values
        features_scaled = scaler.fit_transform(features)
        X, y = create_sequences(features_scaled, targets, WINDOW_SIZE)
        all_X.append(X)
        all_y.append(y)

    if not all_X:
        raise RuntimeError("학습 데이터가 없습니다.")

    X_all = np.concatenate(all_X, axis=0)
    y_all = np.concatenate(all_y, axis=0)

    logger.info("전체 학습 샘플 수: %d", len(X_all))

    dataset = TensorDataset(
        torch.from_numpy(X_all),
        torch.from_numpy(y_all).unsqueeze(1),
    )
    loader = DataLoader(dataset, batch_size=batch_size, shuffle=True)

    model = LSTMStockPredictor(input_size=len(FEATURE_COLS))
    optimizer = torch.optim.Adam(model.parameters(), lr=learning_rate)
    criterion = nn.MSELoss()

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model.to(device)
    logger.info("학습 디바이스: %s", device)

    for epoch in range(1, epochs + 1):
        model.train()
        total_loss = 0.0
        for X_batch, y_batch in loader:
            X_batch, y_batch = X_batch.to(device), y_batch.to(device)
            optimizer.zero_grad()
            output = model(X_batch)
            loss = criterion(output, y_batch)
            loss.backward()
            optimizer.step()
            total_loss += loss.item()

        avg_loss = total_loss / len(loader)
        if epoch % 10 == 0 or epoch == 1:
            logger.info("에포크 [%d/%d] 평균 Loss: %.6f", epoch, epochs, avg_loss)

    # 모델 저장
    CHECKPOINT_DIR.mkdir(parents=True, exist_ok=True)
    checkpoint_path = CHECKPOINT_DIR / f"{model_version}.pt"
    torch.save(
        {
            "model_state_dict": model.state_dict(),
            "model_version": model_version,
            "input_size": len(FEATURE_COLS),
            "hidden_size": model.hidden_size,
            "num_layers": model.num_layers,
        },
        checkpoint_path,
    )
    logger.info("모델 저장 완료: %s", checkpoint_path)
    return model


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    tickers = ["005930.KS", "000660.KS", "AAPL", "MSFT", "NVDA"]
    train(tickers, epochs=50)
