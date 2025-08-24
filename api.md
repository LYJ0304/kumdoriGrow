# KumdoriGrow API 명세서

## 개요

KumdoriGrow는 영수증 인식을 통한 경험치 및 레벨 시스템을 제공하는 백엔드
API입니다.\
레벨업 시에는 일정 조건에서 **포인트 박스 보상** 기능이 자동으로
동작합니다.

------------------------------------------------------------------------

## Base URL

    운영 서버: http://3.36.54.191:8082
    API 경로: /api

-   서버 상태: ✅ 정상 운영 중 (2025-08-24 기준)
-   헬스체크: `GET /actuator/health`
-   디버그 정보: `GET /api/_debug/boot`

------------------------------------------------------------------------

## 📌 영수증 관련 API

### 1. 영수증 OCR 파싱

**Endpoint:** `POST /api/receipts/parse`\
**상태:** 현재 OCR 비활성화됨 → 빈 결과 반환

**Request:** `multipart/form-data` - file: 이미지 파일

**Response 예시:**

``` json
{
  "storeName": null,
  "totalPrice": null,
  "rawText": null,
  "confidence": null
}
```

------------------------------------------------------------------------

### 2. 영수증 등록 및 경험치 지급

**Endpoint:** `POST /api/receipts`\
**Content-Type:** `application/json`

**Request 예시:**

``` json
{
  "userId": 999,
  "storeName": "스타벅스 대흥점",
  "totalAmount": 8500,
  "categoryCode": "FRANCHISE",
  "ocrRaw": null
}
```

**유효성:** - `userId`: 필수 - `totalAmount`: 0 이상 - `ocrRaw`: `null`
또는 문자열(JSON 형태 문자열도 허용)\
❗ JSON 객체 `{}` 그대로 넣으면 400 에러 발생

**Response 예시:**

``` json
{
  "receiptId": 7,
  "expAwarded": 85,
  "totalExpAfter": 490,
  "levelAfter": 2,
  "matchedStoreName": null,
  "confidence": 0.0
}
```

------------------------------------------------------------------------

### 3. 사용자 경험치/레벨 조회

**Endpoint:** `GET /api/receipts/users/{userId}/xp`

**Response:**

``` json
{
  "totalExp": 490,
  "level": 2
}
```

------------------------------------------------------------------------

### 4. 사용자 영수증 목록 조회

**Endpoint:** `GET /api/receipts/users/{userId}/receipts?page=0&size=20`

Spring JPA의 `Page<Receipt>` 형태 반환

------------------------------------------------------------------------

## 👤 사용자 관리 API

### 1. 사용자 생성

**Endpoint:** `POST /api/users?nickname=테스트유저`\
⚠️ 현재 중복 닉네임 시 500 에러 발생 가능

### 2. 사용자 목록 조회

`GET /api/users`

### 3. 특정 사용자 조회

`GET /api/users/{id}`

------------------------------------------------------------------------

## ⚙️ 시스템/디버그 API

-   `GET /actuator/health` → 서버 UP 상태 확인\
-   `GET /api/_debug/boot` → DB 연결/JDBC URL 확인\
-   `GET /api/debug/db-info` → DB 연결 및 유저 수 확인

------------------------------------------------------------------------

## 🏪 경험치/레벨 규칙

-   카테고리별 배수
  -   `FRANCHISE`: 0.6배
  -   `LOCAL`: 1.0배
  -   `MARKET`: 2.0배
-   공식: `floor(결제금액 × 배수 / 100 × 1.5)`
-   레벨 시스템:
  -   1\~5레벨: 고정 구간 (100, 500, 1000, 2000, 5000 XP)
  -   6\~30레벨: 1000 XP마다 1레벨
  -   최대: 30레벨

------------------------------------------------------------------------

## 🎁 포인트 박스 시스템

### 규칙

-   5의 배수 레벨 달성 시 자동 개봉
-   보상 확률:
  -   10,000p: 1%
  -   5,000p: 2%
  -   3,000p: 5%
  -   1,000p: 8%
  -   500p: 14%
  -   100p: 30%
  -   50p: 40%
-   멱등 보장: 같은 레벨에서 중복 지급 없음

### 기록 방식

보상도 `receipts` 테이블에 저장됨:

``` json
{
  "user_id": 999,
  "store_name": "POINT_BOX",
  "total_amount": 1000,
  "category_code": "REWARD",
  "exp_awarded": 0,
  "status": "REWARD",
  "ocr_raw": {
    "type": "POINT_REWARD",
    "level": 5,
    "roll": 0.72,
    "version": "v1"
  }
}
```

------------------------------------------------------------------------

### 보상 API

#### 1. 요약 조회

`GET /api/rewards/summary?userId=999`

``` json
{
  "totalExp": 52000,
  "totalRewardPoints": 1100,
  "rewardCount": 2
}
```

#### 2. 히스토리 조회

`GET /api/rewards/history?userId=999&page=0&size=5`

#### 3. 확률표 조회

`GET /api/rewards/probabilities`

#### 4. 강제 개봉 (테스트 전용)

`POST /api/rewards/open`\
운영환경에서는 404 반환

------------------------------------------------------------------------

## 🚦 에러 응답 패턴

-   `400 Bad Request`: 유효하지 않은 파라미터\
-   `404 Not Found`: 존재하지 않는 리소스\
-   `409 Conflict`: 이미 지급된 리워드\
-   `500 Internal Server Error`: 서버 내부 오류

------------------------------------------------------------------------

## ✅ 실제 테스트 결과 (2025-08-24)

-   로컬 서버: **전체 플로우 정상 작동**
-   EC2 서버:
  -   영수증 등록: `ocrRaw=null`로 정상 동작
  -   레벨업 → 보상 지급: 정상
  -   30레벨 상한선 적용: 정상
