"""
모델 추론(Inference) 모듈

추론 흐름:
  1. 저장된 LSTM 모델 로드
  2. 최근 30일 주가 데이터 수집 (yfinance)
  3. 보조지표 계산 및 정규화
  4. LSTM 모델로 5일 후 수익률 예측
  5. 결과 반환 (pred_return, pred_price, confidence)
"""

import logging
import pickle
from datetime import datetime
from pathlib import Path
from typing import Optional

import numpy as np
import torch

from ai.data.collector import prepare_dataset
from ai.model.lstm_model import LSTMStockPredictor
from ai.model.train import CHECKPOINT_DIR, FEATURE_COLS, WINDOW_SIZE

logger = logging.getLogger(__name__)


def load_model(model_version: str = "v1.0-LSTM-PyTorch") -> Optional[LSTMStockPredictor]:
    """
    저장된 모델 체크포인트를 로드합니다.

    Args:
        model_version: 로드할 모델 버전명

    Returns:
        로드된 LSTMStockPredictor 모델, 실패 시 None
    """
    checkpoint_path = CHECKPOINT_DIR / f"{model_version}.pt"
    if not checkpoint_path.exists():
        logger.error("모델 파일 없음: %s", checkpoint_path)
        return None

    checkpoint = torch.load(checkpoint_path, map_location="cpu", weights_only=True)
    model = LSTMStockPredictor(
        input_size=checkpoint.get("input_size", len(FEATURE_COLS)),
        hidden_size=checkpoint.get("hidden_size", 128),
        num_layers=checkpoint.get("num_layers", 2),
    )
    model.load_state_dict(checkpoint["model_state_dict"])
    model.eval()
    logger.info("모델 로드 완료: %s", model_version)
    return model


def load_scaler(model_version: str = "v1.0-LSTM-PyTorch"):
    """
    학습 시 저장된 StandardScaler를 로드합니다.
    추론 시 반드시 학습과 동일한 scaler를 사용해야 올바른 정규화가 보장됩니다.

    Args:
        model_version: 모델 버전명

    Returns:
        로드된 StandardScaler, 실패 시 None
    """
    scaler_path = CHECKPOINT_DIR / f"{model_version}.scaler.pkl"
    if not scaler_path.exists():
        logger.error("Scaler 파일 없음: %s", scaler_path)
        return None
    with open(scaler_path, "rb") as f:
        scaler = pickle.load(f)
    logger.info("Scaler 로드 완료: %s", model_version)
    return scaler


def predict(
    ticker: str,
    model_version: str = "v1.0-LSTM-PyTorch",
) -> Optional[dict]:
    """
    특정 종목에 대해 5일 후 수익률을 예측합니다.

    Args:
        ticker:        종목 코드 (예: "005930.KS", "AAPL")
        model_version: 사용할 모델 버전

    Returns:
        예측 결과 딕셔너리:
        {
            "ticker":        "005930.KS",
            "pred_return":   0.0450,       # r_{t,5}
            "pred_price":    78375,         # P_{t+5}
            "current_price": 75000,         # P_t
            "confidence":    0.85,
            "analysis_note": "...",
            "model_version": "v1.0-LSTM-PyTorch"
        }
        실패 시 None
    """
    model = load_model(model_version)
    if model is None:
        logger.error("모델 로드 실패: %s", model_version)
        return None

    # 학습 시 저장된 scaler 로드 (추론에도 동일한 정규화 파라미터 사용)
    scaler = load_scaler(model_version)
    if scaler is None:
        logger.error("Scaler 로드 실패: %s", model_version)
        return None

    df = prepare_dataset(ticker, period="6mo")
    if df is None or df.empty or len(df) < WINDOW_SIZE:
        logger.error("추론용 데이터 부족: ticker=%s", ticker)
        return None

    features = df[FEATURE_COLS].values
    features_scaled = scaler.transform(features)

    # 가장 최근 WINDOW_SIZE일 데이터를 입력으로 사용
    x_input = features_scaled[-WINDOW_SIZE:]
    x_tensor = torch.from_numpy(x_input).float().unsqueeze(0)  # (1, 30, 14)

    with torch.no_grad():
        pred_return_tensor = model(x_tensor)

    pred_return = float(pred_return_tensor.squeeze().item())
    current_price = float(df["Close"].iloc[-1])
    pred_price = current_price * (1 + pred_return)

    # 신뢰도: 모델 출력의 절댓값을 sigmoid로 정규화 (간단한 근사)
    confidence = float(torch.sigmoid(pred_return_tensor.abs()).squeeze().item())
    confidence = min(max(confidence, 0.0), 1.0)

    result = {
        "ticker": ticker,
        "current_price": round(current_price, 2),
        "pred_return": round(pred_return, 6),
        "pred_price": round(pred_price, 2),
        "confidence": round(confidence, 4),
        "analysis_note": _generate_analysis_note(df, pred_return),
        "model_version": model_version,
        "generated_at": datetime.utcnow().isoformat() + "Z",
    }
    logger.info("예측 완료: ticker=%s, pred_return=%.4f", ticker, pred_return)
    return result


def _generate_analysis_note(df, pred_return: float) -> str:
    """
    예측 근거 분석 메모를 생성합니다.
    최근 보조지표 상태를 요약하여 반환합니다.
    """
    notes = []
    last = df.iloc[-1]

    # 거래량 증가 여부
    if last["Volume"] > last["Volume_MA5"] * 1.5:
        notes.append("거래량 급증")

    # 이동평균선 골든크로스
    if last["MA5"] > last["MA20"]:
        notes.append("단기 골든크로스")

    # RSI 과매수/과매도
    if last["RSI14"] > 70:
        notes.append("RSI 과매수 구간")
    elif last["RSI14"] < 30:
        notes.append("RSI 과매도 구간 (반등 기대)")

    # MACD 양전환
    if last["MACD"] > last["MACD_signal"]:
        notes.append("MACD 매수 신호")

    if not notes:
        direction = "상승" if pred_return > 0 else "하락"
        notes.append(f"AI 모델 {direction} 예측")

    return " 및 ".join(notes) + " 기반"
