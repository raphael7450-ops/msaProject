# CRM과 user-service 경계 재설계

작성일: 2026-07-13

## 1. 목적

현재 `crm-service`는 Feign을 통해 `user-service`의 사용자 CRUD API를 직접 호출하고 있다. 이 구조는 CRM 화면이 사용자 데이터의 생성, 수정, 삭제까지 수행하게 만들어 서비스 책임 경계를 흐리게 한다.

이번 설계의 목적은 다음과 같다.

- `user-service`를 사용자/고객 데이터의 단일 원천으로 정리한다.
- `crm-service`는 CRM 화면과 영업 기능 중심으로 역할을 축소한다.
- CRUD 쓰기 기능은 관리자 기능으로 한정한다.
- `user-service` 장애가 `crm-service` 전체 장애로 전파되지 않도록 최소한의 회복력 장치를 둔다.

## 2. 현재 문제

### 2.1 서비스 책임 혼재

`crm-service`의 `CustomerService`는 `UserServiceClient`를 통해 사용자 목록 조회뿐 아니라 생성, 수정, 삭제까지 수행한다. 그러나 사용자는 `user-service`가 소유해야 하는 핵심 도메인이다.

이 상태에서는 다음 문제가 발생한다.

- 고객/사용자 데이터 변경 책임이 CRM과 사용자 서비스에 나뉘어 보인다.
- 사용자 정책 변경 시 `crm-service`도 함께 수정될 가능성이 커진다.
- 관리자 기능과 일반 CRM 화면의 권한 경계가 흐려진다.

### 2.2 장애 전파

`crm-service`는 화면 렌더링 중 `user-service`를 동기 호출한다. timeout, fallback, circuit breaker가 없으면 `user-service` 지연 또는 장애가 CRM 화면 장애로 바로 이어진다.

### 2.3 Gateway 권한 정책 부족

Gateway는 JWT 검증을 수행하지만, 현재 설계상 사용자 쓰기 API에 대한 관리자 권한 정책이 명확히 분리되어 있지 않다. `/user-service/**` 접근은 인증 여부뿐 아니라 읽기/쓰기와 권한에 따라 구분되어야 한다.

## 3. 설계 방향

추천안은 다음과 같다.

> `user-service`는 사용자/고객 데이터의 단일 원천으로 유지하고, `crm-service`는 조회 중심 소비자로 축소한다. CRM의 등록, 수정, 삭제 기능은 제거하거나 관리자 화면으로 이동한다. 조회 연동에는 timeout, fallback, circuit breaker를 적용한다.

이 방식은 현재 Spring Cloud Gateway, OpenFeign, Spring Boot 구조를 유지하면서도 서비스 책임과 장애 전파 문제를 가장 현실적으로 줄인다.

## 4. 서비스별 책임

### 4.1 user-service

`user-service`는 사용자/고객 데이터의 단일 원천이다.

담당 기능:

- 사용자/고객 목록 조회
- 사용자/고객 단건 조회
- 사용자/고객 생성
- 사용자/고객 수정
- 사용자/고객 삭제

권한 정책:

- 조회 API는 인증된 사용자에게 허용한다.
- 생성, 수정, 삭제 API는 관리자 권한이 있는 사용자에게만 허용한다.

예상 API:

```text
GET    /users
GET    /users/{id}
POST   /users          ADMIN only
PUT    /users/{id}     ADMIN only
DELETE /users/{id}     ADMIN only
```

### 4.2 crm-service

`crm-service`는 CRM 화면과 영업 업무 흐름을 담당한다.

담당 기능:

- 고객/사용자 목록 표시
- 고객/사용자 상세 정보 표시
- 리드, 딜, 대시보드 등 CRM 고유 기능 제공

제외 기능:

- 고객/사용자 생성
- 고객/사용자 수정
- 고객/사용자 삭제

CRM 화면에서 기존 등록, 수정, 삭제 버튼이 필요하다면 직접 처리하지 않고 관리자 기능으로 이동하는 링크만 제공한다.

### 4.3 gateway-service

`gateway-service`는 외부 요청의 인증과 권한 정책 진입점이다.

담당 기능:

- `/crm-service/**` JWT 필수
- `/user-service/**` JWT 필수
- `/user-service/users` 쓰기 요청은 관리자 권한 필수
- `/login-service/**`는 로그인 경로이므로 JWT 필터 제외

권한 판단은 JWT claim의 `role` 또는 `authorities` 값을 기준으로 한다. 구현 시 하나의 표준 claim 이름을 정해야 하며, 본 설계에서는 `role=ADMIN`을 기본안으로 둔다.

## 5. 데이터 흐름

### 5.1 관리자 CRUD

```text
관리자
  -> gateway-service
  -> user-service
  -> users DB
```

관리자는 Gateway를 통해 `user-service`의 CRUD API를 호출한다. Gateway는 JWT를 검증하고 관리자 권한을 확인한다.

### 5.2 CRM 고객 조회

```text
CRM 사용자
  -> gateway-service
  -> crm-service
  -> Feign GET
  -> user-service
  -> users DB
```

CRM은 사용자/고객 데이터를 직접 소유하지 않고 `user-service` 조회 API를 통해 읽는다.

## 6. 장애 처리

`crm-service`는 `user-service` 조회 실패에 대비해야 한다.

### 6.1 timeout

Feign 조회 호출에는 짧은 timeout을 둔다.

기본값:

```text
connect timeout: 1초
read timeout: 2초
```

### 6.2 fallback

`user-service` 조회 실패 시 CRM 화면 전체가 실패하지 않도록 처리한다.

목록 화면:

- 빈 목록을 표시한다.
- 화면 상단에 “사용자 서비스가 일시적으로 응답하지 않습니다.” 메시지를 표시한다.

상세 화면:

- 상세 조회 실패 메시지를 표시한다.
- 목록 화면으로 되돌아갈 수 있게 한다.

### 6.3 circuit breaker

`crm-service -> user-service` 조회 호출에는 circuit breaker를 적용한다.

적용 대상:

- `getUsers`
- `getUser`

쓰기 API는 CRM에서 제거되므로 circuit breaker 대상에서 제외한다.

## 7. 테스트 기준

### 7.1 user-service

검증 항목:

- 인증 없이 `/users` 접근 시 401 또는 Gateway 차단
- 일반 사용자 권한으로 `POST /users` 요청 시 403
- 관리자 권한으로 `POST /users` 요청 시 성공
- 일반 사용자 권한으로 `PUT /users/{id}` 요청 시 403
- 관리자 권한으로 `PUT /users/{id}` 요청 시 성공
- 일반 사용자 권한으로 `DELETE /users/{id}` 요청 시 403
- 관리자 권한으로 `DELETE /users/{id}` 요청 시 성공

### 7.2 crm-service

검증 항목:

- `user-service` 정상 상태에서 고객 목록이 표시된다.
- `user-service` 장애 상태에서 CRM 홈 화면은 200 응답을 유지한다.
- `user-service` 장애 상태에서 고객 목록 영역에 장애 안내 메시지가 표시된다.
- CRM 화면에서 고객 생성, 수정, 삭제 직접 처리 기능이 제거된다.

### 7.3 gateway-service

검증 항목:

- JWT 없이 `/crm-service/**` 접근 시 401
- JWT 없이 `/user-service/**` 접근 시 401
- 일반 사용자 JWT로 `/user-service/users` 쓰기 요청 시 403
- 관리자 JWT로 `/user-service/users` 쓰기 요청 시 user-service로 전달
- `/login-service/**`는 로그인 요청을 위해 JWT 없이 접근 가능

## 8. 구현 범위

1차 구현 범위:

- CRM 고객 생성/수정/삭제 화면 및 서비스 호출 제거
- `crm-service`의 Feign Client를 조회 전용으로 축소
- `crm-service`에 Feign timeout과 fallback 추가
- Gateway에서 user-service 쓰기 API 관리자 권한 정책 추가
- JWT에 `role` claim을 포함하도록 login-service 토큰 발급 구조 보완
- 관련 테스트 추가 또는 기존 테스트 보강

2차 구현 후보:

- CRM read model 분리
- user-service 사용자 변경 이벤트 발행
- Kafka 또는 RabbitMQ 기반 비동기 동기화

2차 구현은 현재 목표인 실용적 결합도 완화 범위를 넘어가므로 이번 구현 계획에는 포함하지 않는다.

## 9. 비범위

이번 설계에는 다음을 포함하지 않는다.

- Kafka, RabbitMQ 등 메시징 인프라 도입
- CRM 전용 고객 테이블 신설
- 사용자 도메인 전체 재설계
- SSO 또는 OAuth2 도입
- 운영 권한 관리 화면 전체 구현

## 10. 승인 기준

설계가 완료된 것으로 보는 기준은 다음과 같다.

- `user-service`가 사용자/고객 데이터의 단일 원천으로 명확히 정의된다.
- `crm-service`는 사용자/고객 조회만 수행한다.
- 사용자/고객 쓰기 API는 관리자 권한으로 제한된다.
- `user-service` 장애 시 `crm-service` 전체 화면 장애가 발생하지 않는다.
- Gateway 권한 정책이 인증과 관리자 권한을 구분한다.
