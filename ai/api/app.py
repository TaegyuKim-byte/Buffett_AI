"""
AI FastAPI 서버

백엔드 ↔ AI API 호출 규격:

  POST /predict
    요청: { "ticker": "005930", "target_date": "2026-03-30" }
    응답: AiPredictionResponse (predictions 배열)

  POST /backtest
    요청: { "tickers": [...], "start_date": "...", "end_date": "..." }
    응답: 백테스트 결과

  GET /health
    응답: { "status": "ok" }
"""

import logging
import os
from datetime import date, datetime
from typing import List, Optional

import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from ai.model.inference import predict
from ai.backtest.backtest import run_backtest

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Buffett AI - 주가 예측 API",
    description="LSTM 기반 5일 후 주가 수익률 예측 API",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

MODEL_VERSION = os.getenv("MODEL_VERSION", "v1.0-LSTM-PyTorch")


# ── 요청/응답 스키마 ─────────────────────────────────────────────

class PredictRequest(BaseModel):
    ticker: str
    target_date: date


class PredictionItem(BaseModel):
    ticker: str
    pred_date: date
    target_date: date
    current_price: float
    pred_return: float
    pred_price: float
    confidence: float
    analysis_note: str


class PredictMetadata(BaseModel):
    generated_at: datetime
    model_version: str
    total_count: int


class PredictResponse(BaseModel):
    status: str
    metadata: PredictMetadata
    predictions: List[PredictionItem]


class BacktestRequest(BaseModel):
    tickers: List[str]
    start_date: str
    end_date: str
    strategy: str = "threshold"
    threshold: float = 0.02
    top_n: int = 5


# ── 엔드포인트 ──────────────────────────────────────────────────

@app.get("/health")
def health_check():
    """서버 상태 확인"""
    return {"status": "ok", "model_version": MODEL_VERSION}


@app.post("/predict", response_model=PredictResponse)
def predict_endpoint(request: PredictRequest):
    """
    단일 종목 5일 후 수익률 예측

    요청: { "ticker": "005930", "target_date": "2026-03-30" }
    응답: AiPredictionResponse
    """
    logger.info("예측 요청: ticker=%s, target_date=%s", request.ticker, request.target_date)

    # 한국 종목 코드에 .KS 접미사 추가
    yf_ticker = _normalize_ticker(request.ticker)

    result = predict(yf_ticker, model_version=MODEL_VERSION)
    if result is None:
        raise HTTPException(status_code=500, detail=f"예측 실패: {request.ticker}")

    now = datetime.utcnow()
    prediction_item = PredictionItem(
        ticker=request.ticker,
        pred_date=now.date(),
        target_date=request.target_date,
        current_price=result["current_price"],
        pred_return=result["pred_return"],
        pred_price=result["pred_price"],
        confidence=result["confidence"],
        analysis_note=result["analysis_note"],
    )

    return PredictResponse(
        status="success",
        metadata=PredictMetadata(
            generated_at=now,
            model_version=result["model_version"],
            total_count=1,
        ),
        predictions=[prediction_item],
    )


@app.post("/backtest")
def backtest_endpoint(request: BacktestRequest):
    """
    백테스트 실행

    백테스트 전략:
      - threshold: pred_return > threshold 이면 매수
      - top_n: 예측 수익률 상위 N개 종목 매수
    """
    logger.info("백테스트 요청: %s ~ %s", request.start_date, request.end_date)

    # 예측 데이터가 없는 경우 빈 DataFrame으로 시뮬레이션
    import pandas as pd
    predictions_df = pd.DataFrame(columns=["ticker", "pred_date", "target_date", "pred_return", "current_price"])

    result = run_backtest(
        tickers=request.tickers,
        start_date=request.start_date,
        end_date=request.end_date,
        predictions=predictions_df,
        strategy=request.strategy,
        threshold=request.threshold,
        top_n=request.top_n,
    )
    return {"status": "success", "result": result}


def _normalize_ticker(ticker: str) -> str:
    """
    종목 코드를 yfinance 형식으로 변환합니다.
    한국 종목 (숫자 6자리): "005930" → "005930.KS"
    미국 종목: "AAPL" → "AAPL" (변환 없음)
    """
    if ticker.isdigit() and len(ticker) == 6:
        return ticker + ".KS"
    return ticker


if __name__ == "__main__":
    uvicorn.run("ai.api.app:app", host="0.0.0.0", port=8000, reload=False)
