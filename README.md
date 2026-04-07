# Buffett AI

> LSTM 기반 중기 가치투자 주가 예측 시스템

**5일 후 수익률** 예측을 통해 투자 의사결정을 지원합니다.

$$r_{t,5} = \frac{P_{t+5} - P_t}{P_t}$$

---

## 시스템 아키텍처

```
┌──────────────────────────────────────────────────────────┐
│  Frontend (HTML + JavaScript)                            │
│  - 대시보드 / 예측 결과 / 백테스트 결과 시각화           │
└────────────────────┬─────────────────────────────────────┘
                     │ REST API
┌────────────────────▼─────────────────────────────────────┐
│  Backend (Spring Boot)                                   │
│  - 예측 결과 조회/저장 API                               │
│  - 주가 차트 데이터 API                                  │
│  - 백테스트 결과 API                                     │
│  - 매일 17:00 예측 스케줄링 (MON~FRI)                   │
└────────────────────┬─────────────────────────────────────┘
         ┌───────────┴──────────┐
         │ JPA (MySQL)          │ HTTP POST /predict
┌────────▼────────┐   ┌─────────▼────────────────────────┐
│   MySQL DB      │   │  AI Server (FastAPI + PyTorch)   │
│  - stock_data   │   │  - LSTM 모델 추론 (Inference)    │
│  - prediction   │   │  - 백테스트 실행                 │
│  - backtest_    │   │  - yfinance 데이터 수집          │
│    result       │   └──────────────────────────────────┘
│  - stock_master │
└─────────────────┘
```

---

## 역할 분배

| 역할 | 담당 | 기술 스택 |
|------|------|-----------|
| 시스템 엔지니어 (FE/BE) | 김태규 | Spring Boot, HTML, JavaScript |
| AI/데이터 엔지니어 | 최민우 | PyTorch, yfinance, FastAPI |

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Frontend | HTML5 + JavaScript (Vanilla) + Chart.js |
| Backend | Spring Boot 3.2 (Java 17) |
| AI 모델 | PyTorch + LSTM |
| 데이터 수집 | yfinance + pandas |
| AI API 서버 | FastAPI + uvicorn |
| 데이터베이스 | MySQL 8.0 |

---

## 디렉토리 구조

```
Buffett_AI/
├── backend/                        # Spring Boot 백엔드
│   ├── build.gradle
│   └── src/main/java/com/buffettai/
│       ├── BuffettAiApplication.java
│       ├── controller/             # REST API 컨트롤러
│       │   ├── PredictionController.java
│       │   ├── StockDataController.java
│       │   └── BacktestController.java
│       ├── service/                # 비즈니스 로직
│       │   ├── PredictionService.java
│       │   ├── StockDataService.java
│       │   ├── BacktestService.java
│       │   └── PredictionScheduler.java
│       ├── repository/             # JPA 리포지토리
│       ├── entity/                 # DB 엔티티
│       ├── dto/                    # 요청/응답 DTO
│       ├── client/                 # AI API HTTP 클라이언트
│       └── config/                 # 설정 (CORS, RestTemplate)
│
├── ai/                             # Python AI 모듈
│   ├── requirements.txt
│   ├── data/
│   │   └── collector.py            # yfinance 데이터 수집 + 보조지표
│   ├── model/
│   │   ├── lstm_model.py           # LSTM 모델 정의
│   │   ├── train.py                # 모델 학습
│   │   └── inference.py            # 모델 추론
│   ├── backtest/
│   │   └── backtest.py             # 백테스트 로직
│   ├── api/
│   │   └── app.py                  # FastAPI 서버
│   └── tests/
│       └── test_ai_modules.py      # 단위 테스트
│
├── frontend/                       # HTML + JavaScript
│   ├── index.html                  # 메인 대시보드
│   ├── css/style.css
│   ├── js/
│   │   ├── api.js                  # 백엔드 API 클라이언트
│   │   └── dashboard.js            # 대시보드 로직
│   └── pages/
│       ├── predictions.html        # 예측 이력 페이지
│       └── backtest.html           # 백테스트 결과 페이지
│
└── database/
    └── schema.sql                  # MySQL 스키마 (테이블 DDL + 초기 데이터)
```

---

## 백엔드 ↔ AI API 호출 규격

### 예측 요청

```
POST http://localhost:8000/predict
Content-Type: application/json

{
  "ticker": "005930",
  "target_date": "2026-03-30"
}
```

### 예측 응답

```json
{
  "status": "success",
  "metadata": {
    "generated_at": "2026-04-05T18:30:00Z",
    "model_version": "v1.0-LSTM-PyTorch",
    "total_count": 1
  },
  "predictions": [
    {
      "ticker": "005930",
      "pred_date": "2026-04-05",
      "target_date": "2026-04-10",
      "current_price": 75000,
      "pred_return": 0.0450,
      "pred_price": 78375,
      "confidence": 0.85,
      "analysis_note": "단기 골든크로스 및 MACD 매수 신호 기반"
    }
  ]
}
```

---

## AI 모델 입력 데이터 (Feature)

| 특성 | 설명 |
|------|------|
| Open, High, Low, Close, Volume | 과거 30일 OHLCV |
| MA5, MA20, MA60 | 이동평균선 |
| RSI14 | 상대강도지수 (14일) |
| MACD, MACD_signal | MACD 지표 |
| BB_upper, BB_lower | 볼린저 밴드 (20일, 2σ) |
| Volume_MA5 | 거래량 5일 이동평균 |

**출력 (Target):** $r_{t,5} = \frac{P_{t+5} - P_t}{P_t}$

---

## 백엔드 REST API

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/predictions/{ticker}` | 종목별 예측 이력 |
| GET | `/api/predictions/top?date=` | 예측 수익률 상위 10 종목 |
| POST | `/api/predictions/request?ticker=&targetDate=` | AI 예측 요청 |
| GET | `/api/stocks/{ticker}/history?startDate=&endDate=` | 기간별 주가 조회 |
| GET | `/api/stocks/{ticker}/recent?days=30` | 최근 N일 주가 조회 |
| GET | `/api/backtest?model=` | 백테스트 결과 조회 |

---

## 예측 업데이트 스케줄링

| 시간 | 작업 |
|------|------|
| 15:30 | 한국 주식 장 마감 |
| 16:00 | yfinance로 당일 주가 데이터 수집 (Python) |
| 17:00 | AI 모델 추론 실행 → DB 저장 (Spring Scheduler) |
| 익일 09:00 | 예측 결과 확인 가능 |

---

## 실행 방법

### 1. 데이터베이스 초기화

```bash
mysql -u root -p < database/schema.sql
```

### 2. AI 서버 실행

```bash
cd ai
pip install -r requirements.txt
python -m ai.api.app
# → http://localhost:8000
```

### 3. 백엔드 실행

```bash
cd backend
./gradlew bootRun
# → http://localhost:8080
```

### 4. 프론트엔드 실행

`frontend/index.html`을 브라우저에서 열거나 정적 파일 서버로 서빙합니다.

```bash
# Python 간이 서버 예시
cd frontend
python -m http.server 3000
# → http://localhost:3000
```

### 5. AI 모델 학습

```bash
cd ai
python -m ai.model.train
```

### 6. AI 테스트 실행

```bash
cd ai
pytest tests/ -v
```

---

## 평가 지표

| 지표 | 설명 |
|------|------|
| 총 수익률 | 전체 기간 누적 수익률 |
| MDD | 최대 낙폭 (Maximum DrawDown) |
| 샤프 비율 | 위험 대비 수익률 |
| 방향성 정확도 | 상승/하락 예측 정확도 |

---

## 백테스트 전략

1. **Threshold 기반 매수**: `pred_return > threshold` 인 종목 매수
2. **상위 N개 매수**: 예측 수익률 상위 N개 종목 매수