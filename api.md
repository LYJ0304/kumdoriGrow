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

### 2. 영수증 등록 및 경험치 지급
파싱된 영수증 정보를 등록하고 사용자에게 경험치를 지급합니다.

**Endpoint:** `POST /api/receipts`

**Content-Type:** `application/json`

**Request:**
```json
{
  "userId": "long",
  "storeName": "string",
  "totalAmount": "long", 
  "categoryCode": "string",
  "imagePath": "string",
  "ocrRaw": "string"
}
```

**Request 유효성 검사:**
- `userId`: 필수, null 불가
- `storeName`: 필수, 공백 불가, 최대 255자
- `totalAmount`: 필수, 0 이상의 값
- `categoryCode`: 필수, 공백 불가 (FRANCHISE/LOCAL/MARKET)
- `imagePath`: 선택사항
- `ocrRaw`: 선택사항

**Response:**
```json
{
  "receiptId": "long",
  "expAwarded": "integer",
  "totalExpAfter": "long", 
  "levelAfter": "integer"
}
```

**Response 설명:**
- `receiptId`: 생성된 영수증 ID
- `expAwarded`: 이번에 지급된 경험치
- `totalExpAfter`: 지급 후 총 누적 경험치
- `levelAfter`: 지급 후 사용자 레벨

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

## 영수증 엔티티 구조

### Receipt
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
  "createdAt": "string"
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
- `status`: 처리 상태 (기본값: "DONE")
- `recognizedAt`: OCR 인식 시간
- `createdAt`: 영수증 생성 시간

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