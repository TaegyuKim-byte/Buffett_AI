/**
 * Buffett AI - API 클라이언트
 * 백엔드 Spring Boot API와 통신합니다.
 */

const API_BASE = 'http://localhost:8080/api';

/**
 * 예측 결과 조회
 * GET /api/predictions/{ticker}
 */
async function fetchPredictionsByTicker(ticker) {
    const res = await fetch(`${API_BASE}/predictions/${encodeURIComponent(ticker)}`);
    if (!res.ok) throw new Error(`예측 데이터 조회 실패: ${res.status}`);
    return res.json();
}

/**
 * 예측 수익률 상위 10 종목 조회
 * GET /api/predictions/top?date=YYYY-MM-DD
 */
async function fetchTopPredictions(date = null) {
    const query = date ? `?date=${date}` : '';
    const res = await fetch(`${API_BASE}/predictions/top${query}`);
    if (!res.ok) throw new Error(`상위 종목 조회 실패: ${res.status}`);
    return res.json();
}

/**
 * AI 예측 요청
 * POST /api/predictions/request?ticker=...&targetDate=...
 */
async function requestPrediction(ticker, targetDate) {
    const params = new URLSearchParams({ ticker, targetDate });
    const res = await fetch(`${API_BASE}/predictions/request?${params}`, {
        method: 'POST',
    });
    if (!res.ok) throw new Error(`예측 요청 실패: ${res.status}`);
    return res.json();
}

/**
 * 주가 차트 데이터 조회 (최근 N일)
 * GET /api/stocks/{ticker}/recent?days=30
 */
async function fetchRecentStockData(ticker, days = 30) {
    const res = await fetch(
        `${API_BASE}/stocks/${encodeURIComponent(ticker)}/recent?days=${days}`
    );
    if (!res.ok) throw new Error(`주가 데이터 조회 실패: ${res.status}`);
    return res.json();
}

/**
 * 백테스트 결과 조회
 * GET /api/backtest?model=...
 */
async function fetchBacktestResults(modelVersion = null) {
    const query = modelVersion ? `?model=${encodeURIComponent(modelVersion)}` : '';
    const res = await fetch(`${API_BASE}/backtest${query}`);
    if (!res.ok) throw new Error(`백테스트 데이터 조회 실패: ${res.status}`);
    return res.json();
}

/**
 * 숫자 포매팅 헬퍼
 */
function formatPrice(price) {
    if (price == null) return '-';
    return new Intl.NumberFormat('ko-KR', {
        style: 'currency',
        currency: 'KRW',
        maximumFractionDigits: 0,
    }).format(price);
}

function formatReturn(ret) {
    if (ret == null) return '-';
    const pct = (ret * 100).toFixed(2);
    return (ret >= 0 ? '+' : '') + pct + '%';
}

function formatConfidence(conf) {
    if (conf == null) return '-';
    return (conf * 100).toFixed(1) + '%';
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('ko-KR');
}
