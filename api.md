# KumdoriGrow API 명세서

## 개요
KumdoriGrow는 영수증 인식을 통한 경험치 시스템을 제공하는 백엔드 API입니다.

## Base URL
```
운영 서버: http://3.36.54.191:8082
API 경로: /api
```

## 프론트엔드 연동 가이드
- 모든 API 요청은 `http://3.36.54.191:8082/api` 를 베이스로 사용
- 서버 상태: ✅ **정상 운영 중** (2025-08-24 테스트 완료)
- 헬스체크: `GET /actuator/health`
- 디버그 정보: `GET /api/_debug/boot` (운영환경 확인용)
- CORS 설정 확인 필요

---

## 영수증 관련 API

### 1. 영수증 OCR 파싱
영수증 이미지 파일을 업로드하여 OCR 분석 결과를 반환합니다.

**Endpoint:** `POST /api/receipts/parse`

**Content-Type:** `multipart/form-data`

**⚠️ OCR 기능 상태:**
- 현재 운영환경에서 OCR 기능이 비활성화되어 있습니다 (`enabled=false`)
- OCR API 설정이 없을 경우 빈 결과 객체를 반환합니다
- 향후 OCR 서비스 활성화 시 정상 동작 예정

**Request:**
```
Form Data:
- file: MultipartFile (영수증 이미지 파일)
```

**Response:**
```json
{
  "storeName": "string",
  "totalPrice": "integer", 
  "rawText": "string",
  "confidence": "double"
}
```

**Response 설명:**
- `storeName`: 인식된 상점명 (OCR 비활성화 시 null)
- `totalPrice`: 인식된 총 결제 금액 (OCR 비활성화 시 null)
- `rawText`: OCR로 인식한 원본 텍스트 (OCR 비활성화 시 null)
- `confidence`: OCR 신뢰도 (0.0 ~ 1.0, OCR 비활성화 시 null)

---

### 2. 영수증 등록 및 경험치 지급 (자동 가게 매칭)
파싱된 영수증 정보를 등록하고 OCR 텍스트 기반 자동 가게 매칭을 통해 경험치를 지급합니다.

**Endpoint:** `POST /api/receipts`

**Content-Type:** `application/json`

**Request:**
```json
{
  "userId": "long",
  "storeName": "string",         // 선택사항 (OCR 매칭 실패시 사용)
  "totalAmount": "long", 
  "categoryCode": "string",      // 선택사항 (OCR 매칭 성공시 자동 설정)
  "imagePath": "string",
  "ocrRaw": "string"            // OCR 원본 텍스트 (가게 매칭에 사용)
}
```

**Request 유효성 검사:**
- `userId`: 필수, null 불가
- `totalAmount`: 필수, 0 이상의 값
- `storeName`: 선택사항, OCR 매칭 실패시 사용 (최대 255자)
- `categoryCode`: 선택사항, OCR 매칭 실패시 필수 (FRANCHISE/LOCAL/MARKET)
- `imagePath`: 선택사항
- `ocrRaw`: 선택사항, 가게 자동 매칭에 사용

**⭐ 자동 가게 매칭 프로세스:**
1. OCR 텍스트(`ocrRaw`)에서 가게명 추출
2. DB 가게 사전과 매칭 (정확 매칭 → 부분 매칭 → 퍼지 매칭)
3. 매칭 성공: 가게의 카테고리와 경험치 배수 자동 적용
4. 매칭 실패: 요청 데이터 사용 또는 `NEED_REVIEW` 상태

**Response:**
```json
{
  "receiptId": "long",
  "expAwarded": "integer",
  "totalExpAfter": "long", 
  "levelAfter": "integer",
  "matchedStoreName": "string",  // 매칭된 가게명 (새로 추가)
  "confidence": "double"         // 매칭 신뢰도 0.0~1.0 (새로 추가)
}
```

**Response 설명:**
- `receiptId`: 생성된 영수증 ID
- `expAwarded`: 이번에 지급된 경험치
- `totalExpAfter`: 지급 후 총 누적 경험치
- `levelAfter`: 지급 후 사용자 레벨
- `matchedStoreName`: 자동 매칭된 가게명 (매칭 성공시)
- `confidence`: 가게 매칭 신뢰도 (0.85 이상이면 자동 적용)

---

### 3. 사용자 경험치/레벨 조회
특정 사용자의 현재 누적 경험치와 레벨을 조회합니다.

**Endpoint:** `GET /api/receipts/users/{userId}/xp`

**Path Parameters:**
- `userId`: 사용자 ID (long)

**Response:**
```json
{
  "totalExp": "long",
  "level": "integer"
}
```

**Response 설명:**
- `totalExp`: 사용자의 총 누적 경험치
- `level`: 현재 사용자 레벨

---

### 4. 사용자 영수증 목록 조회
특정 사용자의 영수증 목록을 최신순으로 페이징하여 조회합니다.

**Endpoint:** `GET /api/receipts/users/{userId}/receipts`

**Path Parameters:**
- `userId`: 사용자 ID (long)

**Query Parameters:**
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 20)

**Response:**
```json
{
  "content": [
    {
      "id": "long",
      "userId": "long", 
      "storeName": "string",
      "totalAmount": "long",
      "categoryCode": "string",
      "expAwarded": "integer",
      "imagePath": "string",
      "ocrRaw": "string",
      "status": "string",
      "recognizedAt": "string",
      "createdAt": "string"
    }
  ],
  "pageable": {
    "sort": {
      "sorted": "boolean",
      "unsorted": "boolean"
    },
    "pageNumber": "integer",
    "pageSize": "integer",
    "offset": "long",
    "paged": "boolean",
    "unpaged": "boolean"
  },
  "totalElements": "long",
  "totalPages": "integer",
  "last": "boolean",
  "first": "boolean",
  "numberOfElements": "integer",
  "size": "integer",
  "number": "integer",
  "sort": {
    "sorted": "boolean", 
    "unsorted": "boolean"
  }
}
```

**Response 설명:**
- Spring Data JPA의 `Page<Receipt>` 형태로 반환
- `content`: 영수증 객체 배열
- 페이징 관련 메타데이터 포함

---

## 사용자 관리 API

### 1. 사용자 생성
새로운 사용자를 생성합니다.

**Endpoint:** `POST /api/users`

**Query Parameters:**
- `nickname`: 사용자 닉네임 (required)

**Response:**
```json
{
  "id": "long",
  "nickname": "string",
  "createdAt": "string"
}
```

**⚠️ 주의사항:**
- 현재 중복 닉네임 검증 미구현 (500 에러 발생 가능)
- 프로덕션 환경에서는 고유 제약조건으로 인해 실패할 수 있음

---

### 2. 모든 사용자 조회 ✅ **테스트 완료**
등록된 모든 사용자 목록을 조회합니다.

**Endpoint:** `GET /api/users`

**Response:**
```json
[
  {
    "id": "long",
    "nickname": "string",
    "createdAt": "string"
  }
]
```

---

### 3. 특정 사용자 조회 ✅ **테스트 완료**
사용자 ID로 특정 사용자 정보를 조회합니다.

**Endpoint:** `GET /api/users/{id}`

**Path Parameters:**
- `id`: 사용자 ID (long)

**Response:**
```json
{
  "id": "long",
  "nickname": "string"
}
```

---

## 시스템/디버그 API

### 1. 헬스체크 ✅ **정상**
서버 상태를 확인합니다.

**Endpoint:** `GET /actuator/health`

**Response:**
```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```

---

### 2. 운영 환경 디버그 정보 ✅ **신규 추가**
운영 환경의 설정 상태를 확인합니다.

**Endpoint:** `GET /api/_debug/boot`

**Response:**
```json
{
  "activeProfiles": ["prod"],
  "jdbcUrl": "jdbc:mysql://mysql:3306/kumdori_grow?serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true",
  "dbUsername": "app_user@172.20.0.3",
  "currentDatabase": "kumdori_grow",
  "ocr": {
    "enabled": false,
    "apiUrlConfigured": true,
    "apiKeyConfigured": true,
    "apiUrlLength": 124,
    "apiKeyLength": 44
  },
  "status": "SUCCESS"
}
```

---

### 3. 기존 DB 디버그 정보 ✅ **정상**
데이터베이스 연결 및 사용자 정보를 확인합니다.

**Endpoint:** `GET /api/debug/db-info`

**Response:**
```json
{
  "jdbcUrl": "string",
  "username": "string", 
  "database": "string",
  "usersCount": "integer",
  "hasUser999": "boolean"
}
```

---

## 가게 매칭 시스템

### 경험치 계산 규칙 ✅ **테스트 완료**
**카테고리별 경험치 배수:**
- `FRANCHISE`: 0.6배 (프랜차이즈) - 스타벅스 8500원 → 85 경험치 ✅
- `LOCAL`: 1.0배 (지역상점) - 테스트카페 5000원 → 75 경험치 ✅ 
- `MARKET`: 2.0배 (전통시장/특산물)

**계산 공식:** `경험치 = floor(결제금액 × 카테고리 배수 / 100 * 1.5)`

**레벨 시스템:**
- 레벨 1~5: 고정 구간 (100, 500, 1000, 2000, 5000 경험치)
- 레벨 6~30: 1000 경험치당 1레벨씩 상승
- **최대 레벨**: 30레벨 (30,000 경험치)

**실제 테스트 결과:**
- LOCAL 5000원: `floor(5000 × 1.0 × 1.5 / 100)` = 75 경험치 ✅
- FRANCHISE 8500원: `floor(8500 × 0.6 × 1.5 / 100)` = 85 경험치 ✅
- 레벨업: 160 경험치로 레벨 2 달성 ✅

### 가게 매칭 로직
1. **정확 매칭**: 가게명/별칭과 완전 일치 (신뢰도 0.99)
2. **부분 매칭**: 가게명/별칭 부분 포함 (신뢰도 0.95/0.93)
3. **퍼지 매칭**: 유사도 계산 (임계값 0.88 이상)

### 매칭 우선순위
1. 신뢰도 높은 순
2. 매칭된 텍스트 길이 긴 순 (더 구체적)
3. 브랜드 정보가 있는 가게 우선

---

## 엔티티 구조

### Receipt (업데이트됨)
```json
{
  "id": "long",
  "userId": "long",
  "storeName": "string",
  "totalAmount": "long", 
  "categoryCode": "string",
  "expAwarded": "integer",
  "imagePath": "string",
  "ocrRaw": "string",
  "status": "string",
  "recognizedAt": "string",
  "createdAt": "string",
  "matchedStoreId": "long",
  "storeNameConfidence": "double"
}
```

**필드 설명:**
- `id`: 영수증 고유 ID
- `userId`: 영수증을 등록한 사용자 ID
- `storeName`: 상점명 (최대 255자)
- `totalAmount`: 총 결제 금액
- `categoryCode`: 카테고리 코드 (FRANCHISE/LOCAL/MARKET)
- `expAwarded`: 지급된 경험치
- `imagePath`: 영수증 이미지 경로 (최대 512자)
- `ocrRaw`: OCR 원본 데이터 (JSON 형태)
- `status`: 처리 상태 ("DONE" / "NEED_REVIEW")
- `recognizedAt`: OCR 인식 시간
- `createdAt`: 영수증 생성 시간
- `matchedStoreId`: 매칭된 가게 ID (새로 추가)
- `storeNameConfidence`: 가게 매칭 신뢰도 (새로 추가)

### Store
```json
{
  "id": "long",
  "name": "string",
  "normalizedName": "string",
  "categoryCode": "string",
  "brand": "string",
  "createdAt": "string"
}
```

### Category
```json
{
  "code": "string",
  "name": "string", 
  "weight": "double",
  "createdAt": "string"
}
```

### User
```json
{
  "id": "long",
  "nickname": "string", 
  "createdAt": "string"
}
```

**필드 설명:**
- `id`: 사용자 고유 ID
- `nickname`: 사용자 닉네임 (최대 50자)
- `createdAt`: 사용자 생성 시간

---

## 에러 응답
API에서 발생할 수 있는 에러는 Spring Boot의 기본 에러 처리를 따릅니다.

**일반적인 에러 응답 형태:**
```json
{
  "timestamp": "string",
  "status": "integer",
  "error": "string", 
  "message": "string",
  "path": "string"
}
```

**주요 HTTP 상태 코드:**
- `400 Bad Request`: 요청 데이터 유효성 검사 실패
- `404 Not Found`: 요청한 리소스를 찾을 수 없음  
- `500 Internal Server Error`: 서버 내부 오류

---

## 실제 테스트 예시 (2025-08-24)

### 영수증 등록 테스트

**요청 예시 1 - LOCAL 카테고리:**
```bash
curl -X POST -H "Content-Type: application/json" -d '{
  "userId": 999,
  "storeName": "테스트카페",
  "totalAmount": 5000,
  "categoryCode": "LOCAL"
}' http://3.36.54.191:8082/api/receipts
```

**응답:**
```json
{
  "receiptId": 2,
  "expAwarded": 75,
  "totalExpAfter": 75,
  "levelAfter": 1,
  "matchedStoreName": null,
  "confidence": 0.0
}
```

**요청 예시 2 - FRANCHISE 카테고리:**
```bash
curl -X POST -H "Content-Type: application/json" -d '{
  "userId": 999,
  "storeName": "스타벅스 대흥점",
  "totalAmount": 8500,
  "categoryCode": "FRANCHISE"
}' http://3.36.54.191:8082/api/receipts
```

**응답:**
```json
{
  "receiptId": 3,
  "expAwarded": 85,
  "totalExpAfter": 160,
  "levelAfter": 2,
  "matchedStoreName": null,
  "confidence": 0.0
}
```

### 경험치 조회 테스트

**요청:**
```bash
curl -X GET http://3.36.54.191:8082/api/receipts/users/999/xp
```

**응답:**
```json
{
  "totalExp": 160,
  "level": 2
}
```

---

## 프론트엔드 연동 체크리스트

✅ **서버 연결**
- [x] 헬스체크: `GET /actuator/health`
- [x] 디버그 정보: `GET /api/_debug/boot`

✅ **사용자 관리**
- [x] 사용자 조회: `GET /api/users`
- [x] 특정 사용자: `GET /api/users/{id}`
- [ ] 사용자 생성: `POST /api/users` (500 에러 주의)

✅ **영수증 시스템**
- [x] 영수증 등록: `POST /api/receipts` 
- [x] 경험치 조회: `GET /api/receipts/users/{userId}/xp`
- [x] 영수증 목록: `GET /api/receipts/users/{userId}/receipts`
- [ ] OCR 파싱: `POST /api/receipts/parse` (현재 비활성화)

**운영 환경 상태:**
- 🟢 **MySQL 정상 연결**
- 🟢 **경험치 시스템 정상**
- 🟢 **레벨업 로직 정상**  
- 🟡 **OCR 기능 비활성화** (향후 활성화 예정)
- 🔴 **사용자 생성 500 에러** (중복 제약조건 문제)

---

## 포인트 박스 시스템 🎁

### 개요
사용자가 레벨업할 때(5의 배수) 포인트 박스를 자동으로 개봉하여 포인트를 지급하는 보상 시스템입니다.

### 보상 규칙
- **개봉 조건**: 레벨이 5의 배수(5, 10, 15, 20...)가 될 때마다 자동 개봉
- **중복 방지**: 같은 레벨로는 1회만 보상 지급 (멱등성 보장)
- **확률표**:
  - 10,000p: 1%
  - 5,000p: 2%  
  - 3,000p: 5%
  - 1,000p: 8%
  - 500p: 14%
  - 100p: 30%
  - 50p: 40%

### 보상 API

#### 1. 사용자 포인트 요약 조회
사용자의 총 경험치, 총 보상 포인트, 보상 받은 횟수를 조회합니다.

**Endpoint:** `GET /api/rewards/summary`

**Query Parameters:**
- `userId`: 사용자 ID (required)

**Response:**
```json
{
  "totalExp": "long",        // 영수증으로 얻은 총 경험치
  "totalRewardPoints": "long", // 보상으로 받은 총 포인트
  "rewardCount": "long"      // 보상 받은 총 횟수
}
```

**예시:**
```bash
curl -X GET "http://3.36.54.191:8082/api/rewards/summary?userId=999"
```

---

#### 2. 보상 히스토리 조회
사용자의 포인트 박스 개봉 이력을 페이징으로 조회합니다.

**Endpoint:** `GET /api/rewards/history`

**Query Parameters:**
- `userId`: 사용자 ID (required)
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 10, 최대: 100)

**Response:**
```json
{
  "content": [
    {
      "id": "long",           // 보상 레코드 ID
      "points": "integer",    // 받은 포인트
      "receivedAt": "string", // 보상 받은 시간 (ISO 8601)
      "snapshot": "string"    // JSON 스냅샷 (레벨, 확률, 난수값 등)
    }
  ],
  "pageable": { /* 페이징 정보 */ },
  "totalElements": "long",
  "totalPages": "integer",
  "last": "boolean",
  "first": "boolean"
}
```

**예시:**
```bash
curl -X GET "http://3.36.54.191:8082/api/rewards/history?userId=999&page=0&size=5"
```

---

#### 3. 포인트 박스 확률표 조회
포인트 박스의 확률표를 조회합니다.

**Endpoint:** `GET /api/rewards/probabilities`

**Response:**
```json
{
  "probabilities": [
    {
      "points": 10000,
      "weight": 0.01
    },
    {
      "points": 5000,
      "weight": 0.02
    },
    // ... 전체 확률표
  ],
  "totalWeight": 1.0,
  "note": "Actual probability = weight / totalWeight"
}
```

**예시:**
```bash
curl -X GET "http://3.36.54.191:8082/api/rewards/probabilities"
```

---

#### 4. 포인트 박스 강제 개봉 (테스트 전용)
테스트 환경에서만 사용 가능한 포인트 박스 강제 개봉 API입니다.

**Endpoint:** `POST /api/rewards/open`

**⚠️ 제약사항:** 
- `spring.profiles.active=test` 환경에서만 동작
- 운영환경에서는 404 에러 발생

**Content-Type:** `application/json`

**Request:**
```json
{
  "userId": "long",
  "testSeed": "long"  // 선택사항, 테스트용 시드값
}
```

**Response:**
```json
{
  "points": "integer",  // 당첨된 포인트
  "level": "integer",   // null (강제 개봉이므로)
  "roll": "double"      // 사용된 난수값 (0.0~1.0)
}
```

**예시:**
```bash
curl -X POST -H "Content-Type: application/json" -d '{
  "userId": 999,
  "testSeed": 12345
}' http://localhost:8082/api/rewards/open
```

---

### 자동 보상 시스템

#### 레벨업 시 자동 개봉
영수증 등록 API(`POST /api/receipts`)에서 레벨업이 발생하면:
1. 새 레벨이 5의 배수인지 확인
2. 해당 레벨로 이미 보상받았는지 확인 (중복 방지)
3. 조건 만족 시 포인트 박스 자동 개봉
4. 보상 내역을 `receipts` 테이블에 기록

#### 보상 기록 방식
포인트 박스 보상은 `receipts` 테이블을 재사용하여 기록됩니다:

```json
{
  "user_id": 999,
  "store_name": "POINT_BOX",
  "total_amount": 1000,        // 당첨 포인트
  "category_code": "REWARD",
  "exp_awarded": 0,
  "status": "REWARD",
  "recognized_at": "2025-08-23T10:30:00Z",
  "ocr_raw": {                 // JSON 스냅샷
    "type": "POINT_REWARD",
    "level": 5,
    "roll": 0.7234,
    "weights": [...],
    "normalized": [...],
    "version": "v1"
  }
}
```

---

### 에러 응답

**400 Bad Request:**
```json
{
  "timestamp": "2025-08-23T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid userId",
  "path": "/api/rewards/summary"
}
```

**403 Forbidden (테스트 API):**
```json
{
  "timestamp": "2025-08-23T10:30:00.000Z",
  "status": 403,
  "error": "Forbidden",
  "message": "This endpoint is only available in test profile",
  "path": "/api/rewards/open"
}
```

**409 Conflict:**
```json
{
  "timestamp": "2025-08-23T10:30:00.000Z", 
  "status": 409,
  "error": "Conflict",
  "message": "Reward already granted for this level",
  "path": "/api/rewards/open"
}
```

---

### 검증 시나리오

#### 레벨업 테스트
1. 사용자 경험치를 5레벨(5000경험치)까지 올리기
2. `GET /api/rewards/history`로 보상 1건 확인
3. 10레벨까지 올리기  
4. `GET /api/rewards/history`로 보상 2건 확인
5. `GET /api/rewards/summary`로 총합 확인

#### 테스트 API 검증
1. 테스트 환경에서 `POST /api/rewards/open` 3회 호출
2. `GET /api/rewards/history`로 3건 확인
3. 운영환경에서 같은 API 호출 시 404 에러 확인

#### 확률 검증
1. `GET /api/rewards/probabilities`로 확률표 조회
2. 모든 확률의 합이 1.0인지 확인