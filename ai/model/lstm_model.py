"""
LSTM 모델 정의 모듈

입력 데이터 (Feature):
  - 과거 30일치 주가 데이터 (OHLCV)
  - 보조지표: MA5, MA20, MA60, RSI14, MACD, MACD_signal, BB_upper, BB_lower, Volume_MA5

출력 (Target):
  - 5일 후 수익률 r_{t,5} = (P_{t+5} - P_t) / P_t

모델 구조:
  LSTM (2-layer) → Dropout → Linear
"""

import torch
import torch.nn as nn


class LSTMStockPredictor(nn.Module):
    """
    주가 예측을 위한 LSTM 모델

    Args:
        input_size:   입력 특성 수 (OHLCV + 보조지표)
        hidden_size:  LSTM 히든 레이어 크기
        num_layers:   LSTM 레이어 수
        dropout:      드롭아웃 비율
        output_size:  출력 크기 (수익률 예측 = 1)
    """

    def __init__(
        self,
        input_size: int = 14,
        hidden_size: int = 128,
        num_layers: int = 2,
        dropout: float = 0.2,
        output_size: int = 1,
    ):
        super().__init__()
        self.hidden_size = hidden_size
        self.num_layers = num_layers

        self.lstm = nn.LSTM(
            input_size=input_size,
            hidden_size=hidden_size,
            num_layers=num_layers,
            batch_first=True,
            dropout=dropout if num_layers > 1 else 0.0,
        )
        self.dropout = nn.Dropout(dropout)
        self.fc = nn.Linear(hidden_size, output_size)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        """
        순전파

        Args:
            x: (batch_size, seq_len, input_size) 형태의 텐서

        Returns:
            (batch_size, output_size) 형태의 예측 수익률 텐서
        """
        # LSTM 출력: (batch, seq_len, hidden_size)
        lstm_out, _ = self.lstm(x)

        # 마지막 시점의 출력만 사용
        last_out = lstm_out[:, -1, :]
        out = self.dropout(last_out)
        prediction = self.fc(out)
        return prediction
