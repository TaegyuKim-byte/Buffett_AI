/**
 * Buffett AI - 대시보드 메인 로직
 */

let priceChart = null;

// 페이지 로드 시 상위 예측 종목 로드
document.addEventListener('DOMContentLoaded', () => {
    loadTopPicks();
});

/**
 * 종목 예측 조회
 */
async function loadPrediction() {
    const ticker = document.getElementById('tickerInput').value.trim();
    if (!ticker) return;

    hideError();

    const today = new Date().toISOString().split('T')[0];

    try {
        // 최신 예측 요청 (없으면 새로 생성)
        let predictions = await fetchPredictionsByTicker(ticker);

        if (!predictions || predictions.length === 0) {
            // 예측 데이터가 없으면 새로 요청
            const newPred = await requestPrediction(ticker, today);
            predictions = [newPred];
        }

        const latest = predictions[0];
        renderPrediction(latest);
    } catch (err) {
        showError('예측 데이터를 불러오지 못했습니다: ' + err.message);
        console.error(err);
    }
}

/**
 * 주가 차트 로드
 */
async function loadChart() {
    const ticker = document.getElementById('tickerInput').value.trim();
    if (!ticker) return;

    hideError();

    try {
        const stockData = await fetchRecentStockData(ticker, 30);
        renderPriceChart(ticker, stockData);
    } catch (err) {
        showError('차트 데이터를 불러오지 못했습니다: ' + err.message);
        console.error(err);
    }
}

/**
 * 예측 결과 렌더링
 */
function renderPrediction(pred) {
    const section = document.getElementById('predictionResult');
    section.classList.remove('hidden');

    const returnVal = pred.predReturn ?? pred.pred_return;
    const isPositive = returnVal >= 0;

    document.getElementById('currentPrice').textContent =
        formatPrice(pred.currentPrice ?? pred.current_price);

    const returnEl = document.getElementById('predReturn');
    returnEl.textContent = formatReturn(returnVal);
    returnEl.className = 'stat-value ' + (isPositive ? 'positive' : 'negative');

    document.getElementById('predPrice').textContent =
        formatPrice(pred.predPrice ?? pred.pred_price);
    document.getElementById('confidence').textContent =
        formatConfidence(pred.confidence);
    document.getElementById('analysisNote').textContent =
        pred.analysisNote ?? pred.analysis_note ?? '-';
    document.getElementById('modelVersion').textContent =
        '모델: ' + (pred.modelVersion ?? pred.model_version ?? 'N/A');
    document.getElementById('predDate').textContent =
        '예측일: ' + formatDate(pred.predDate ?? pred.pred_date);
}

/**
 * 주가 차트 렌더링 (Chart.js)
 */
function renderPriceChart(ticker, stockData) {
    const section = document.getElementById('chartSection');
    section.classList.remove('hidden');

    if (priceChart) {
        priceChart.destroy();
    }

    const labels = stockData.map(d => d.tradeDate ?? d.trade_date);
    const closes = stockData.map(d => parseFloat(d.closePrice ?? d.close_price));

    const ctx = document.getElementById('priceChart').getContext('2d');
    priceChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels,
            datasets: [{
                label: `${ticker} 종가`,
                data: closes,
                borderColor: '#1a56db',
                backgroundColor: 'rgba(26, 86, 219, 0.08)',
                borderWidth: 2,
                pointRadius: 2,
                fill: true,
                tension: 0.3,
            }],
        },
        options: {
            responsive: true,
            plugins: {
                legend: { display: true },
                tooltip: {
                    callbacks: {
                        label: ctx => formatPrice(ctx.parsed.y),
                    },
                },
            },
            scales: {
                x: { ticks: { maxTicksLimit: 8 } },
                y: {
                    ticks: {
                        callback: val => val.toLocaleString('ko-KR'),
                    },
                },
            },
        },
    });
}

/**
 * 상위 예측 종목 로드
 */
async function loadTopPicks() {
    const tbody = document.getElementById('topPicksBody');
    try {
        const picks = await fetchM7Predictions();
        if (!picks || picks.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center">예측 데이터가 없습니다.</td></tr>';
            return;
        }
        tbody.innerHTML = picks.map((p, i) => {
            const ret = p.expected_return;
            const isPos = ret >= 0;
            return `
                <tr>
                    <td><strong>${p.ticker}</strong></td>
                    <td>${getCompanyName(p.ticker)}</td>
                    <td>${formatPrice(p.current_price)}</td>
                    <td class="${isPos ? 'positive' : 'negative'}">${formatReturn(ret)}</td>
                    <td>${formatPrice(p.predicted_price)}</td>
                    <td>-</td>
                    <td>-</td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center" style="color:#e02424">데이터 로드 실패: ${err.message}</td></tr>`;
        console.error(err);
    }
}

// CSS 클래스 헬퍼
function hideError() {
    document.getElementById('errorMsg').classList.add('hidden');
}

function showError(msg) {
    const el = document.getElementById('errorMsg');
    el.textContent = msg;
    el.classList.remove('hidden');
}
