# KumdoriGrow API 명세서

## 개요
KumdoriGrow는 영수증 인식을 통한 경험치 시스템을 제공하는 백엔드 API입니다.

## Base URL
```
개발 서버: http://3.36.54.191:8082
API 경로: /api
```

## 프론트엔드 연동 가이드
- 모든 API 요청은 `http://3.36.54.191:8082/api` 를 베이스로 사용
- 서버 상태: 정상 운영 중 (테스트 완료)
- CORS 설정 확인 필요

---

## 영수증 관련 API

### 1. 영수증 OCR 파싱
영수증 이미지 파일을 업로드하여 OCR 분석 결과를 반환합니다.

**Endpoint:** `POST /api/receipts/parse`

**Content-Type:** `multipart/form-data`

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
- `storeName`: 인식된 상점명
- `totalPrice`: 인식된 총 결제 금액
- `rawText`: OCR로 인식한 원본 텍스트
- `confidence`: OCR 신뢰도 (0.0 ~ 1.0)

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

## 가게 매칭 시스템

### 경험치 계산 규칙
**카테고리별 경험치 배수:**
- `FRANCHISE`: 1.0배 (프랜차이즈)
- `LOCAL`: 1.5배 (지역상점)  
- `MARKET`: 2.0배 (전통시장/특산물)

**계산 공식:** `경험치 = floor(결제금액 × 카테고리 배수 / 100)`

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