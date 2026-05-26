/**
 * Buffett AI - API 클라이언트 (Updated 2026-05-17)
 * M7 전용 API 및 미국 주식 포맷팅 최적화
 */

const API_BASE = 'http://localhost:8080/api';

/**
 * [A안] M7 전체 예측 데이터 조회
 * GET /api/predictions/m7
 * 페이지 진입 시 한 방에 7개를 가져오는 핵심 함수입니다.
 */
async function fetchM7Predictions() {
    const res = await fetch(`${API_BASE}/predictions/m7`);
    if (!res.ok) throw new Error(`M7 데이터 조회 실패: ${res.status}`);
    return res.json();
}

/**
 * 특정 종목 상세 예측 조회
 * GET /api/predictions/{ticker}
 */
async function fetchPredictionsByTicker(ticker) {
    const res = await fetch(`${API_BASE}/predictions/${encodeURIComponent(ticker.toUpperCase())}`);
    if (!res.ok) throw new Error(`예측 데이터 조회 실패: ${res.status}`);
    return res.json();
}

/**
 * 숫자 포매팅 헬퍼 (미국 주식/USD 기준)
 * M7 주식은 달러($)이므로 KRW에서 USD로 변경했습니다.
 */
function formatPrice(price) {
    if (price == null) return '-';
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        maximumFractionDigits: 2,
    }).format(price);
}

/**
 * 수익률 포매팅 (소수점 2자리 + 기호)
 * 0.045 -> +4.50%
 */
function formatReturn(ret) {
    if (ret == null) return '-';
    const num = Number(ret);
    const pct = (num * 100).toFixed(2);
    return (num >= 0 ? '+' : '') + pct + '%';
}

/**
 * 신뢰도 포매팅
 * 0.85 -> 85%
 */
function formatConfidence(conf) {
    if (conf == null) return '-';
    return (Number(conf) * 100).toFixed(1) + '%';
}

/**
 * 날짜 포매팅
 * 2026-05-17 -> 2026. 05. 17.
 */
function formatDate(dateStr) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('ko-KR');
}

/**
 * M7 종목 코드 -> 회사명 매핑
 */
function getCompanyName(ticker) {
    const companyMap = {
        'AAPL': 'Apple Inc.',
        'MSFT': 'Microsoft Corp.',
        'GOOGL': 'Alphabet Inc.',
        'AMZN': 'Amazon.com Inc.',
        'NVDA': 'NVIDIA Corp.',
        'TSLA': 'Tesla Inc.',
        'META': 'Meta Platforms Inc.'
    };
    return companyMap[ticker] || ticker;
}
