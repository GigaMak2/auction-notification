# auction-notification

> 역경매 서비스의 **실시간 알림 서버**입니다.
> Redis Pub/Sub로 경매 이벤트를 수신하여 PostgreSQL에 저장하고, SSE(Server-Sent Events)로 사용자에게 실시간 알림을 전달합니다.

---

## 처리 흐름

```
auction (메인 서버)
        │  Redis Pub/Sub 발행 (auction:notification)
        ▼
NotificationMessageListener
        │  JSON 역직렬화 → NotificationService.save()
        ▼
PostgreSQL (Notification 저장)
        │  트랜잭션 커밋 후 SSE 전송
        ▼
SseEmitterService ──► 연결된 클라이언트 (SSE)
```

---

## 알림 유형

| NotificationType | 메시지 |
|-----------------|--------|
| `AUCTION_STARTED` | '[itemName]' 경매가 시작됐습니다. |
| `NEW_BID` | '[itemName]' 경매에 새 입찰이 들어왔습니다. |
| `LOWEST_BID_UPDATED` | '[itemName]' 경매에 더 낮은 입찰이 들어왔습니다. 재입찰을 고려해보세요. |
| `AUCTION_CLOSED_WIN` | 축하합니다! '[itemName]' 경매에 낙찰됐습니다. |
| `AUCTION_CLOSED_BUYER` | '[itemName]' 경매의 낙찰자가 결정됐습니다. |
| `AUCTION_NO_BID` | '[itemName]' 경매가 입찰자 없이 종료됐습니다. |

---

## 주요 기능

### 실시간 알림 (SSE)

- `GET /api/notifications/subscribe` — SSE 구독 (클라이언트당 하나의 emitter 유지)
- 30초 간격 ping으로 커넥션 유지
- 트랜잭션 커밋 후 SSE 전송 (`TransactionSynchronization`)으로 데이터 정합성 보장
- 가상 스레드 기반 비동기 처리 (Java 21)

### 알림 관리 REST API

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/notifications` | 알림 목록 조회 (생성일 내림차순) |
| `PATCH` | `/api/notifications/{id}/read` | 단건 읽음 처리 |
| `PATCH` | `/api/notifications/read-all` | 전체 읽음 처리 |
| `DELETE` | `/api/notifications/{id}` | 단건 삭제 |
| `DELETE` | `/api/notifications` | 전체 삭제 |

---

## 패키지 구조

```
com.example.auctionnotification/
├── common/
│   ├── config/
│   │   ├── redis/        # RedisConfig, RedisSubscriberConfig
│   │   └── security/     # SecurityConfig, JwtProvider, JwtAuthenticationFilter
│   ├── dto/              # BaseResponse
│   └── exception/        # GlobalExceptionHandler, ServiceErrorException
│
└── notification/
    ├── controller/        # NotificationController, NotificationTestController
    ├── service/           # NotificationService, SseEmitterService
    ├── listener/          # NotificationMessageListener (Redis Pub/Sub)
    ├── repository/        # NotificationRepository
    ├── entity/            # Notification
    ├── dto/               # NotificationMessage, NotificationResponse
    └── enums/             # NotificationType
```

---

## 환경변수

| 변수명 | 설명 | 필수 |
|--------|------|------|
| `POSTGRES_URL` | PostgreSQL JDBC URL | ✅ |
| `POSTGRES_USERNAME` | DB 사용자명 | ✅ |
| `POSTGRES_PASSWORD` | DB 비밀번호 | ✅ |
| `REDIS_HOST` | Redis 호스트 | ✅ |
| `JWT_SECRET` | JWT 서명 키 (32자 이상) | ✅ |
| `REDIS_SSL_ENABLED` | Redis SSL 활성화 여부 (기본값: `false`) | ❌ |

---

## 로컬 실행 방법

### 사전 요구사항

- Java 21
- Docker / Docker Compose

### 1단계 — 환경변수 파일 생성

프로젝트 루트에 `.env` 파일을 생성합니다.

```dotenv
POSTGRES_URL=jdbc:postgresql://localhost:5432/auction_notification
POSTGRES_USERNAME=postgres
POSTGRES_PASSWORD=your_password

REDIS_HOST=localhost

JWT_SECRET=your_jwt_secret_key_min_32_chars
```

### 2단계 — 인프라 실행

```bash
docker compose up -d postgres redis
```

### 3단계 — 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

서버가 뜨면 `http://localhost:8081` 로 접근할 수 있습니다.

---

## 기술 스택

<div align="center">

<img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=openjdk&logoColor=white">
<img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
<img src="https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
<img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white">
<img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white">
<img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">
<img src="https://img.shields.io/badge/Amazon%20ECS-FF9900?style=for-the-badge&logo=amazonecs&logoColor=white">
<img src="https://img.shields.io/badge/Amazon%20RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white">
<img src="https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white">
<img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white">
<img src="https://img.shields.io/badge/Github%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">

</div>