# KumdoriGrow Backend

> 영수증 인식을 통한 경험치 시스템 백엔드 API

KumdoriGrow는 사용자가 업로드한 영수증을 OCR로 분석하여 자동으로 가게를 매칭하고, 카테고리별 차등 경험치를 부여하는 게이미피케이션 시스템입니다.

## 🎯 주요 기능

### 📸 OCR 영수증 인식
- **Clova OCR API** 연동
- 영수증 이미지에서 가게명, 금액, 텍스트 추출
- 신뢰도 기반 품질 평가

### 🏪 AI 가게 자동 매칭
- **3단계 매칭 시스템**: 정확 매칭 → 부분 매칭 → 퍼지 매칭
- **가게 사전 DB**: 프랜차이즈, 지역상점, 전통시장 구분
- **별칭 지원**: "스벅" → "스타벅스", "맥날" → "맥도날드" 등
- **신뢰도 기반**: 0.85 이상시 자동 적용, 이하시 수동 리뷰

### 🎮 차등 경험치 시스템
- **프랜차이즈** (1.0배): 스타벅스, 맥도날드, CGV 등
- **지역상점** (1.5배): 동네카페, 로컬식당 등  
- **전통시장** (2.0배): 대흥시장, 남대문시장 등
- **레벨 시스템**: 경험치 누적에 따른 단계별 레벨업

## 🛠️ 기술 스택

### Backend
- **Java 21** + **Spring Boot 3.5**
- **Spring Data JPA** + **MySQL 8.0**
- **Lombok** (코드 간소화)
- **Gradle** (빌드 도구)

### Infrastructure
- **Docker & Docker Compose**
- **GitHub Container Registry**
- **AWS EC2** (배포)

### External APIs
- **Naver Clova OCR** (영수증 인식)

## 📋 시스템 요구사항

### 개발 환경
- Java 21 이상
- MySQL 8.0
- Docker & Docker Compose

### 환경변수 설정
```bash
# .env 파일 생성
DB_HOST=localhost
DB_PORT=3306
DB_NAME=kumdori_grow
DB_USER=app_user  
DB_PASSWORD=your_password
CLOVA_OCR_URL=your_clova_ocr_endpoint
CLOVA_OCR_SECRET=your_clova_ocr_secret
```

## 🚀 시작하기

### 1. 저장소 복제
```bash
git clone https://github.com/your-username/kumdoriGrow.git
cd kumdoriGrow
```

### 2. 데이터베이스 초기화
```bash
# Docker로 MySQL 실행
docker-compose up mysql -d

# 가게 매칭 시스템 스키마 및 시드 데이터 추가
mysql -h localhost -P 3306 -u app_user -p kumdori_grow < init-store-matching-system.sql
```

### 3. 애플리케이션 실행
```bash
# 개발 환경 (H2 DB)
./gradlew bootRun --args="--spring.profiles.active=local"

# 운영 환경 (MySQL)
./gradlew bootRun --args="--spring.profiles.active=prod"
```

### 4. API 테스트
```bash
# 건강 상태 확인
curl http://localhost:8080/actuator/health

# 영수증 등록 테스트 (자동 가게 매칭)
curl -X POST http://localhost:8080/api/receipts \
-H "Content-Type: application/json" \
-d '{
  "userId": 999,
  "totalAmount": 5000,
  "ocrRaw": "스타벅스 대흥점\n아메리카노 Tall\n5,000원"
}'
```

## 📊 데이터베이스 구조

### 핵심 테이블
- **`users`**: 사용자 정보
- **`receipts`**: 영수증 데이터 + 가게 매칭 결과
- **`categories`**: 카테고리별 경험치 배수
- **`stores`**: 가게 마스터 데이터  
- **`store_aliases`**: 가게 별칭 관리

### 가게 매칭 시스템
```
OCR 텍스트 → 가게명 추출 → DB 매칭 → 카테고리 결정 → 경험치 계산
```

## 🔗 API 문서

### 주요 엔드포인트
- `POST /api/receipts/parse` - 영수증 OCR 분석
- `POST /api/receipts` - 영수증 등록 (자동 가게 매칭)
- `GET /api/receipts/users/{userId}/xp` - 사용자 경험치 조회
- `GET /api/receipts/users/{userId}/receipts` - 영수증 목록

📖 **자세한 API 명세**: [api.md](./api.md)

## 🧪 테스트 시나리오

### 성공 케이스
```bash
# 1. 정확 매칭 (스타벅스)
curl -X POST http://localhost:8080/api/receipts \
-H "Content-Type: application/json" \
-d '{"userId":999,"totalAmount":5000,"ocrRaw":"스타벅스 대흥점\n아메리카노\n5,000원"}'

# 2. 별칭 매칭 (스벅 → 스타벅스)  
curl -X POST http://localhost:8080/api/receipts \
-H "Content-Type: application/json" \
-d '{"userId":999,"totalAmount":3000,"ocrRaw":"스벅\n라떼\n3,000원"}'

# 3. 전통시장 매칭 (2배 경험치)
curl -X POST http://localhost:8080/api/receipts \
-H "Content-Type: application/json" \
-d '{"userId":999,"totalAmount":25000,"ocrRaw":"대흥시장 한우\n등심 1kg\n25,000원"}'
```

### 예상 응답
```json
{
  "receiptId": 123,
  "expAwarded": 50,
  "totalExpAfter": 580,
  "levelAfter": 6,
  "matchedStoreName": "스타벅스",
  "confidence": 0.99
}
```

## 📈 성능 최적화

### 구현된 최적화
- **정규화된 가게명** 인덱싱으로 빠른 검색
- **복합 인덱스** (user_id, created_at)로 사용자별 영수증 조회 최적화
- **Levenshtein 거리** 기반 효율적인 퍼지 매칭

### 향후 개선 사항
- **Redis 캐싱** (가게 매칭 결과)
- **Elasticsearch** 도입 (고급 검색)
- **형태소 분석기** 연동 (한국어 처리 향상)

## 🔧 운영 & 모니터링

### 배포
```bash
# Docker 이미지 빌드
./gradlew bootBuildImage

# Docker Compose로 전체 스택 실행
docker-compose up -d
```

### 모니터링
- **Spring Boot Actuator**: `/actuator/health`, `/actuator/info`
- **로그 레벨**: WARN (운영), DEBUG (개발)
- **DB 커넥션 풀**: HikariCP 기본 설정

### 헬스체크
- **애플리케이션**: `GET /actuator/health`
- **데이터베이스**: 자동 연결 상태 확인
- **OCR API**: 요청시 응답 상태 확인

## 🔍 배포 후 점검 절차

### 디버깅 및 검증 단계 (Base URL: http://3.36.54.191:8082/api)

**1. 데이터베이스 연결 확인**
```bash
curl -X GET http://3.36.54.191:8082/api/debug/db-info
```
- 예상 응답: `jdbcUrl`, `database=kumdori_grow`, `hasUser999=true` 확인

**2. 사용자 API 테스트**
```bash
curl -X GET http://3.36.54.191:8082/api/users/999
```
- 예상 응답: 200 OK with user data
- 실패시 응답: 404 NOT FOUND

**3. 영수증 생성 API 테스트**
```bash
curl -X POST http://3.36.54.191:8082/api/receipts \
-H "Content-Type: application/json" \
-d '{
  "userId": 999,
  "storeName": "스타벅스 대흥점",
  "totalAmount": 8500,
  "categoryCode": "FRANCHISE",
  "ocrRaw": "스타벅스 대흥점 결제 금액 8500원"
}'
```
- 예상 응답: 200/201 with `expAwarded` 포함

**4. 존재하지 않는 사용자 테스트**
```bash
curl -X POST http://3.36.54.191:8082/api/receipts \
-H "Content-Type: application/json" \
-d '{
  "userId": 99999,
  "storeName": "테스트 가게",
  "totalAmount": 1000,
  "categoryCode": "LOCAL",
  "ocrRaw": "테스트"
}'
```
- 예상 응답: 404 NOT FOUND

### 로그 확인 포인트

**애플리케이션 시작시 나타날 로그:**
```
[DB] url=jdbc:mysql://..., user=app_user, database=kumdori_grow
```

**정상 동작시 로그:**
```
Store matched: 스타벅스 (confidence: 0.95)
Using provided store info: 스타벅스 대흥점 (FRANCHISE)
```

**에러 처리시 로그:**
```
Store resolution failed, proceeding with manual review
Experience calculation failed, using default value
User not found: 99999
```

## 🐛 문제 해결

### 자주 발생하는 이슈

**1. 500 Internal Server Error 발생**
- `/api/debug/db-info` 로 DB 연결 상태 확인
- 애플리케이션 로그에서 DB URL 및 사용자 확인
- 필수 컴포넌트 빈 등록 상태 확인

**2. 가게 매칭이 안 되는 경우**
- OCR 텍스트 품질 확인
- 가게 사전에 누락된 가게/별칭 추가
- 신뢰도 임계값 조정 (현재 0.85)

**3. 경험치가 예상과 다른 경우**
- 카테고리별 배수 확인 (DB categories 테이블)
- 계산 공식: `floor(금액 × 배수 / 100)`

**4. DB 연결 실패**
- 환경변수 설정 확인
- MySQL 서비스 상태 확인
- 방화벽/보안그룹 설정 확인

## 🤝 기여하기

### 개발 워크플로우
1. **이슈 생성** - 기능 요청 또는 버그 리포트
2. **브랜치 생성** - `feature/기능명` 또는 `fix/버그명`
3. **개발 & 테스트** - 단위 테스트 작성 권장
4. **Pull Request** - 코드 리뷰 후 병합

### 코딩 컨벤션
- **Java Code Style**: Google Java Style Guide
- **Commit Message**: Conventional Commits
- **브랜치 전략**: Git Flow

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 📞 문의

- **개발자**: [your-email@example.com]
- **이슈 리포트**: [GitHub Issues](https://github.com/your-username/kumdoriGrow/issues)
- **문서**: [api.md](./api.md) | [sql.md](./sql.md)

---

**⭐ 이 프로젝트가 도움이 되셨다면 스타를 눌러주세요!**

