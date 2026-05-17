-- Buffett AI Database Schema
-- MySQL 8.0+
-- 문자셋: utf8mb4, 정렬: utf8mb4_unicode_ci

CREATE DATABASE IF NOT EXISTS buffett_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE buffett_db;

-- 1. 종목 마스터 테이블
--    지원하는 종목의 기본 정보를 관리합니다.
CREATE TABLE IF NOT EXISTS stock_master (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    ticker       VARCHAR(20)  NOT NULL COMMENT '종목 코드 (예: 005930)',
    company_name VARCHAR(100) NOT NULL COMMENT '회사명',
    market       VARCHAR(20)           COMMENT '시장 구분 (KOSPI, KOSDAQ, NASDAQ 등)',
    sector       VARCHAR(100)          COMMENT '섹터',
    industry     VARCHAR(100)          COMMENT '산업',
    is_active    TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '활성 여부',
    created_at   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_master_ticker (ticker),
    KEY idx_stock_master_ticker (ticker)
) ENGINE=InnoDB COMMENT='종목 마스터';

-- 2. 주가 데이터 테이블 (과거 OHLCV 차트)
--    Python yfinance로 수집한 일별 주가 데이터를 저장합니다.
CREATE TABLE IF NOT EXISTS stock_data (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    ticker         VARCHAR(20)    NOT NULL COMMENT '종목 코드',
    trade_date     DATE           NOT NULL COMMENT '거래일',
    open_price     DECIMAL(18, 2)          COMMENT '시가',
    high_price     DECIMAL(18, 2)          COMMENT '고가',
    low_price      DECIMAL(18, 2)          COMMENT '저가',
    close_price    DECIMAL(18, 2) NOT NULL COMMENT '종가',
    volume         BIGINT                  COMMENT '거래량',
    adjusted_close DECIMAL(18, 2)          COMMENT '수정 종가',
    created_at     DATETIME                DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_data_ticker_date (ticker, trade_date),
    KEY idx_stock_data_ticker_date (ticker, trade_date),
    KEY idx_stock_data_trade_date (trade_date)
) ENGINE=InnoDB COMMENT='주가 OHLCV 데이터';

-- 3. 예측 결과 테이블
--    AI LSTM 모델이 생성한 5일 후 수익률 예측 결과를 저장합니다.
CREATE TABLE IF NOT EXISTS prediction (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    ticker         VARCHAR(20)     NOT NULL COMMENT '종목 코드',
    prediction_date      DATE            NOT NULL COMMENT '예측 생성일 (AI 분석 수행일)',
    target_date    DATE            NOT NULL COMMENT '예측 대상일 (pred_date + 5 영업일)',
    current_price  DECIMAL(18, 2)  NOT NULL COMMENT '현재 주가 P_t',
    expected_return    DECIMAL(10, 6)  NOT NULL COMMENT 'AI 예측 수익률 r_{t,5}',
    predicted_price     DECIMAL(18, 2)           COMMENT 'AI 예측 가격 P_{t+5}',
    confidence_score     DECIMAL(5, 4)            COMMENT '모델 예측 신뢰도 (0~1)',
    analysis_note  VARCHAR(500)             COMMENT '예측 근거 분석 메모',
    model_version  VARCHAR(50)              COMMENT '사용된 모델 버전',
    generated_at   DATETIME                 COMMENT '예측 생성 타임스탬프',
    created_at     DATETIME                 DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_prediction_ticker_pred_date (ticker, prediction_date),
    KEY idx_prediction_target_date (target_date)
) ENGINE=InnoDB COMMENT='AI 예측 결과';

-- 4. 백테스트 결과 테이블 (AI 모델 성적표)
--    특정 기간 동안 AI 예측 기반 매매 시의 성과를 저장합니다.
--    Python에서 실행되며 결과가 이 테이블에 저장됩니다.
CREATE TABLE IF NOT EXISTS backtest_result (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    start_date           DATE           NOT NULL COMMENT '백테스트 시작일',
    end_date             DATE           NOT NULL COMMENT '백테스트 종료일',
    model_version        VARCHAR(50)             COMMENT '사용된 모델 버전',
    total_return         DECIMAL(10, 4)          COMMENT '전체 수익률 (%)',
    max_drawdown         DECIMAL(10, 4)          COMMENT '최대 낙폭 MDD (%)',
    sharpe_ratio         DECIMAL(10, 4)          COMMENT '샤프 비율',
    accuracy             DECIMAL(5, 4)           COMMENT '예측 정확도 (방향성)',
    total_trades         INT                     COMMENT '총 거래 횟수',
    winning_trades       INT                     COMMENT '수익 거래 횟수',
    strategy_description VARCHAR(255)            COMMENT '백테스트 전략 설명',
    created_at           DATETIME                DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_backtest_start_end (start_date, end_date),
    KEY idx_backtest_model_version (model_version)
) ENGINE=InnoDB COMMENT='백테스트 결과 (AI 모델 성적표)';

-- 초기 데이터: 종목 마스터 (미국 주요 종목)
INSERT INTO stock_master (ticker, company_name, market, sector, industry) VALUES
    ('AAPL',  'Apple',      'NASDAQ', '기술',   '소비자 전자'),
    ('MSFT',  'Microsoft',  'NASDAQ', '기술',   '소프트웨어'),
    ('GOOGL', 'Alphabet',   'NASDAQ', '기술',   '인터넷'),
    ('AMZN',  'Amazon',     'NASDAQ', '소비재', '전자상거래'),
    ('NVDA',  'NVIDIA',     'NASDAQ', '기술',   '반도체'),
    ('META',  'Meta',       'NASDAQ', '기술',   '소셜 미디어'),
    ('TSLA',  'Tesla',      'NASDAQ', '자동차', '전기차')
ON DUPLICATE KEY UPDATE company_name = VALUES(company_name);

-- 회원가입용 DB 테이블 -> 초반엔 안 쓸 듯
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE COMMENT '구글 계정 이메일',
    nickname VARCHAR(50) NOT NULL COMMENT '유저 닉네임 (초기엔 구글 이름)',
    profile_image_url VARCHAR(500) COMMENT '구글 프로필 사진 URL',
    
    -- 소셜 로그인 핵심 컬럼 2가지
    provider VARCHAR(20) NOT NULL COMMENT '가입 경로 (예: google, kakao)',
    provider_id VARCHAR(100) NOT NULL COMMENT '가입 경로 주체(google, kakao ..)가 발급한 고유 회원 번호(sub)',
    
    role VARCHAR(20) DEFAULT 'ROLE_USER' COMMENT '권한 (일반유저, 관리자)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    -- 보안: 같은 제공자의 같은 아이디로 중복 가입 방지
    UNIQUE KEY uk_user_provider (provider, provider_id)
)