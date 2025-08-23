# KumdoriGrow Database SQL

## 테이블 생성 스크립트

### 1. users 테이블
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nickname VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 2. receipts 테이블
```sql
CREATE TABLE receipts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    store_name VARCHAR(255) NOT NULL,
    total_amount BIGINT NOT NULL,
    category_code VARCHAR(20) NOT NULL,
    exp_awarded INTEGER NOT NULL,
    image_path VARCHAR(512),
    ocr_raw JSON,
    status VARCHAR(20) NOT NULL DEFAULT 'DONE',
    recognized_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_receipts_user_created (user_id, created_at)
);
```

## 샘플 데이터 삽입

### 테스트 사용자 생성
```sql
INSERT INTO users (nickname) VALUES 
('테스트유저1'),
('테스트유저2'),
('김철수');
```

### 테스트 영수증 데이터 생성
```sql
INSERT INTO receipts (user_id, store_name, total_amount, category_code, exp_awarded, recognized_at) VALUES 
(1, '스타벅스', 5000, 'FRANCHISE', 30, NOW()),
(1, '동네카페', 3000, 'LOCAL', 30, NOW()),
(1, '이마트', 25000, 'MARKET', 500, NOW()),
(2, 'CGV', 12000, 'FRANCHISE', 72, NOW()),
(2, '로컬식당', 8000, 'LOCAL', 80, NOW()),
(3, '홈플러스', 35000, 'MARKET', 700, NOW());
```

## 유용한 조회 쿼리

### 사용자별 경험치 합계 조회
```sql
SELECT 
    u.id,
    u.nickname,
    COALESCE(SUM(r.exp_awarded), 0) as total_exp,
    CASE 
        WHEN COALESCE(SUM(r.exp_awarded), 0) >= 1000 THEN 3
        WHEN COALESCE(SUM(r.exp_awarded), 0) >= 300 THEN 2
        ELSE 1
    END as level
FROM users u
LEFT JOIN receipts r ON u.id = r.user_id
GROUP BY u.id, u.nickname
ORDER BY total_exp DESC;
```

### 최근 영수증 목록 조회
```sql
SELECT 
    r.id,
    u.nickname as user_name,
    r.store_name,
    r.total_amount,
    r.category_code,
    r.exp_awarded,
    r.created_at
FROM receipts r
JOIN users u ON r.user_id = u.id
ORDER BY r.created_at DESC
LIMIT 10;
```

### 카테고리별 영수증 통계
```sql
SELECT 
    category_code,
    COUNT(*) as receipt_count,
    SUM(total_amount) as total_spent,
    SUM(exp_awarded) as total_exp
FROM receipts
GROUP BY category_code
ORDER BY total_exp DESC;
```

### 특정 사용자의 영수증 목록
```sql
SELECT 
    id,
    store_name,
    total_amount,
    category_code,
    exp_awarded,
    status,
    created_at
FROM receipts 
WHERE user_id = 1
ORDER BY created_at DESC;
```

## 테이블 초기화 (필요시)

### 모든 데이터 삭제
```sql
DELETE FROM receipts;
DELETE FROM users;
```

### 테이블 삭제
```sql
DROP TABLE IF EXISTS receipts;
DROP TABLE IF EXISTS users;
```

## 가게 매칭 시스템 스키마

### 3. categories 테이블
```sql
CREATE TABLE categories (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    weight DOUBLE NOT NULL DEFAULT 1.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 4. stores 테이블 (가게 마스터)
```sql
CREATE TABLE stores (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    category_code VARCHAR(20) NOT NULL,
    brand VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stores_normalized (normalized_name),
    CONSTRAINT fk_store_category FOREIGN KEY (category_code) REFERENCES categories(code)
);
```

### 5. store_aliases 테이블 (가게 별칭)
```sql
CREATE TABLE store_aliases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    alias VARCHAR(255) NOT NULL,
    normalized_alias VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alias_store FOREIGN KEY (store_id) REFERENCES stores(id),
    UNIQUE KEY uk_alias_normalized (normalized_alias)
);
```

### receipts 테이블 수정
```sql
ALTER TABLE receipts
ADD COLUMN matched_store_id BIGINT NULL,
ADD COLUMN store_name_confidence DOUBLE NULL,
ADD CONSTRAINT fk_receipt_store FOREIGN KEY (matched_store_id) REFERENCES stores(id);
```

## 시드 데이터

### 카테고리 시드 데이터
```sql
INSERT INTO categories(code, name, weight) VALUES
('FRANCHISE', '프랜차이즈', 1.0),
('LOCAL', '지역상점', 1.5),
('MARKET', '전통시장/특산물', 2.0)
ON DUPLICATE KEY UPDATE name=VALUES(name), weight=VALUES(weight);
```

### 가게 시드 데이터
```sql
-- 프랜차이즈 가게들
INSERT INTO stores(name, normalized_name, category_code, brand) VALUES
('스타벅스', 'starbucks', 'FRANCHISE', '스타벅스'),
('맥도날드', 'mcdonalds', 'FRANCHISE', '맥도날드'),
('CGV', 'cgv', 'FRANCHISE', 'CGV'),
('롯데리아', 'lotteria', 'FRANCHISE', '롯데리아'),
('이디야커피', 'ediya', 'FRANCHISE', '이디야');

-- 지역상점들
INSERT INTO stores(name, normalized_name, category_code, brand) VALUES
('동네카페', 'dongne_cafe', 'LOCAL', NULL),
('우리동네식당', 'uri_dongne_restaurant', 'LOCAL', NULL),
('동네마트', 'dongne_mart', 'LOCAL', NULL);

-- 전통시장
INSERT INTO stores(name, normalized_name, category_code, brand) VALUES
('대흥시장', 'daeheung_market', 'MARKET', NULL),
('남대문시장', 'namdaemun_market', 'MARKET', NULL),
('동대문시장', 'dongdaemun_market', 'MARKET', NULL);
```

### 가게 별칭 시드 데이터
```sql
-- 스타벅스 별칭들
INSERT INTO store_aliases(store_id, alias, normalized_alias) VALUES
(1, '스타벅스커피', 'starbucks_coffee'),
(1, 'STARBUCKS', 'starbucks'),
(1, '스벅', 'sbucks'),
(1, '스타벅스 대흥점', 'starbucks_daeheung'),
(1, '스타벅스 용산점', 'starbucks_yongsan');

-- 맥도날드 별칭들
INSERT INTO store_aliases(store_id, alias, normalized_alias) VALUES
(2, '맥날', 'mcnal'),
(2, 'McDonald''s', 'mcdonalds'),
(2, '맥도날드 강남점', 'mcdonalds_gangnam');

-- CGV 별칭들
INSERT INTO store_aliases(store_id, alias, normalized_alias) VALUES
(3, 'CGV 용산아이파크몰', 'cgv_yongsan'),
(3, 'CGV 강남', 'cgv_gangnam'),
(3, 'CGV 명동', 'cgv_myeongdong');

-- 대흥시장 별칭들
INSERT INTO store_aliases(store_id, alias, normalized_alias) VALUES
(10, '대흥전통시장', 'daeheung_traditional_market'),
(10, '대흥시장 한우', 'daeheung_market_hanwoo'),
(10, '대흥재래시장', 'daeheung_traditional');
```

## DB 연결 정보
- **Host**: localhost:3306
- **Database**: kumdori_grow  
- **Username**: app_user
- **Password**: heathunting