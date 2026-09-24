# Todo API

Spring Boot 3 · Spring Data JPA · PostgreSQL 로 만든 할 일 REST API 서버입니다.
회원 기능은 없습니다.

- status 업데이트 후와 그 이후 조회의 시간 포맷이 다름

---

## 실행 방법

### 필요한 것

**Docker 만 있으면 됩니다.** JDK·Gradle 은 설치하지 않아도 됩니다 — 빌드가 컨테이너 안에서 수행됩니다.

- Docker / Docker Compose (Compose V2)

### 실행

```bash
docker compose up
```

이 한 줄로 PostgreSQL 과 애플리케이션이 함께 올라갑니다.
앱은 DB 가 접속을 받을 준비를 마칠 때까지 (`healthcheck`) 기다린 뒤 시작합니다.

| 주소                                  | 설명               |
|---------------------------------------|--------------------|
| http://localhost:8080/api/v1/todos    | API                |
| http://localhost:8080/swagger-ui.html | Swagger UI         |
| http://localhost:8080/v3/api-docs     | OpenAPI 문서(JSON) |

첫 기동 시 예시 데이터 20건이 생성됩니다. 이미 데이터가 있으면 건너뜁니다.

### 종료 / 초기화

```bash
docker compose down      # 종료 (데이터 유지)
docker compose down -v   # 종료 + DB 볼륨 삭제 (예시 데이터부터 다시 생성)
```

### 설정 (참고)

DB 접속 정보와 포트는 `compose.yml` 에 그대로 적혀 있습니다. 별도 설정 파일은 없습니다.

| 항목                        | 값                       |
|-----------------------------|--------------------------|
| DB 이름 / 사용자 / 비밀번호 | `todo` / `todo` / `todo` |
| 앱 포트                     | `8080`                   |
| DB 포트                     | `5432`                   |

여기 적힌 `todo/todo` 는 로컬 컨테이너 전용 값이며 운영 비밀번호가 아닙니다. 8080 이나 5432 가 이미 쓰이고 있다면 `compose.yml` 의 `ports` 를 고치면 됩니다.

애플리케이션은 DB 접속 정보를 아래 세 환경변수로 읽습니다 (`application.yaml` 의 `${...:기본값}` 자리). `compose.yml` 이 앱 컨테이너에 이 이름으로 주입하고, 값이
없으면 기본값이 쓰입니다.

| 변수                | 기본값                                  |
|---------------------|-----------------------------------------|
| `DATABASE_URL`      | `jdbc:postgresql://localhost:5432/todo` |
| `DATABASE_USERNAME` | `todo`                                  |
| `DATABASE_PASSWORD` | `todo`                                  |

`DATABASE_URL` 은 JDBC URL 입니다. 일부 PaaS 가 주입하는 `postgres://user:pass@host/db` 형식과는 다릅니다.

### 호스트에서 직접 실행 (선택)

JDK 21 이 있고 DB 만 컨테이너로 쓰고 싶다면:

```bash
docker compose up -d postgres
./gradlew bootRun
```

### 테스트

```bash
./gradlew test
```

---

## API 명세

기본 경로: `/api/v1/todos`

| 메서드   | 주소                        | 설명                    | 성공    |
|----------|-----------------------------|-------------------------|---------|
| `POST`   | `/api/v1/todos`             | 생성                    | **201** |
| `GET`    | `/api/v1/todos`             | 목록 (페이징·상태 필터) | **200** |
| `GET`    | `/api/v1/todos/{id}`        | 단건 조회               | **200** |
| `PATCH`  | `/api/v1/todos/{id}`        | 제목·내용 수정          | **200** |
| `PATCH`  | `/api/v1/todos/{id}/status` | 완료/미완료 변경        | **200** |
| `DELETE` | `/api/v1/todos/{id}`        | 삭제                    | **204** |

### 할 일 표현

```json
{
  "id": 21,
  "title": "우체국 가기",
  "content": "등기 보내기",
  "status": "TODO",
  "createdAt": "2026-09-18T16:56:39.189812",
  "updatedAt": "2026-09-18T16:56:39.189812"
}
```

| 필드        | 타입             | 설명                      |
|-------------|------------------|---------------------------|
| `id`        | number           | 서버가 부여               |
| `title`     | string           | 필수, 1–20자, 공백만 불가 |
| `content`   | string \| null   | 선택, 최대 50자           |
| `status`    | `TODO` \| `DONE` | 생성 시 항상 `TODO`       |
| `createdAt` | string           | 생성 시각                 |
| `updatedAt` | string           | 마지막 수정 시각          |

### POST /api/v1/todos

요청

```json
{
  "title": "우체국 가기",
  "content": "등기 보내기"
}
```

- `title` 필수 (1–20자, 공백만 불가)
- `content` 선택 (최대 50자)
- `status` 는 받지 않습니다 — 새 할 일은 항상 미완료입니다

응답 **201** — 생성된 할 일 표현

### GET /api/v1/todos

| 쿼리     | 기본값 | 설명                              |
|----------|--------|-----------------------------------|
| `status` | (없음) | `TODO` 또는 `DONE`. 생략하면 전체 |
| `page`   | `0`    | 0 이상                            |
| `size`   | `10`   | 1–100                             |

응답 **200**

```json
{
  "content": [
    {
      "id": 1,
      "title": "우유 사기",
      "...": "..."
    }
  ],
  "page": {
    "size": 2,
    "number": 0,
    "totalElements": 22,
    "totalPages": 11
  }
}
```

정렬은 서버가 등록순으로 고정합니다 (아래 설계 설명 참고).

### GET /api/v1/todos/{id}

응답 **200** — 할 일 표현 / **404** — 없는 id

### PATCH /api/v1/todos/{id}

부분 수정입니다. **보낸 필드만 바뀌고, 생략한 필드는 그대로 유지됩니다.**

```json
{
  "content": "2L 저지방 한 통"
}
```

- `title` 선택 (보냈다면 1–20자, 공백만 불가)
- `content` 선택 (최대 50자)
- `status` 는 이 API 로 바꿀 수 없습니다

응답 **200** — 수정된 할 일 표현 / **400** / **404**

### PATCH /api/v1/todos/{id}/status

```json
{
  "status": "DONE"
}
```

- `status` 필수, `TODO` 또는 `DONE` (대소문자 구분)

응답 **200** — 수정된 할 일 표현 / **400** / **404**

### DELETE /api/v1/todos/{id}

응답 **204** (본문 없음) / **404**

### 오류 응답

**모든 오류가 같은 모양을 씁니다.**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "title: 제목은 비어 있을 수 없습니다."
}
```

| 필드      | 설명                                                                                |
|-----------|-------------------------------------------------------------------------------------|
| `status`  | HTTP 상태 코드                                                                      |
| `error`   | 상태 코드의 표준 이유 구절                                                          |
| `message` | 무엇이 잘못됐는지. 검증 오류는 `필드명: 이유` 형식이며 여러 건은 `, ` 로 이어집니다 |

발생 시각은 HTTP `Date` 헤더로, 요청 주소는 클라이언트가 보낸 요청 자체로 알 수 있으므로 본문에 중복해 담지 않습니다.

적용 범위 — 도메인 예외뿐 아니라 Spring MVC 가 던지는 오류까지 같은 형식입니다.

| 상황                                   | 상태                                        |
|----------------------------------------|---------------------------------------------|
| 검증 실패 (본문·쿼리 파라미터)         | 400                                         |
| 잘못된 JSON, 타입 불일치, 없는 enum 값 | 400                                         |
| 없는 id                                | 404                                         |
| 없는 경로                              | 404                                         |
| 지원하지 않는 메서드                   | 405                                         |
| 지원하지 않는 Content-Type             | 415                                         |
| 그 외 예상하지 못한 오류               | 500 (스택트레이스는 서버 로그에만 남깁니다) |

---

## 설계 설명

### 주소

- **`/api/v1/todos`** — 컬렉션은 복수형 명사, 동사는 쓰지 않습니다. `v1` 을 둬서 이후 호환되지 않는 변경을 새 버전으로 낼 수 있게 했습니다.
- **`PATCH /{id}/status` 를 따로 뒀습니다.** 상태 변경은 "할 일을 완료했다"는 독립된 행위이고, 호출 빈도와 권한 조건이 제목·내용 수정과 달라질 여지가 큽니다. 수정 API 에
  `status` 를 섞으면 `{"status":"DONE"}` 하나를 보내려고 부분 수정 규칙을 통과해야 하고, 나중에 "완료 처리만 별도로 제한한다" 같은 요구가 생길 때 갈라내기 어려워집니다.
- **`PUT` 대신 `PATCH`** — 클라이언트가 리소스 전체를 보내지 않아도 되게 했습니다. `PUT` 은 전체 교체라서 제목만 바꿀 때도 `content` 를 함께 보내야 하고, 빠뜨리면 의도치 않게
  `null` 이 됩니다.

### 상태 코드

| 코드    | 쓰는 곳   | 이유                                                                                                                                                                                                                                                |
|---------|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **201** | 생성      | 새 리소스가 생겼으므로 200 이 아닙니다. 응답 본문에 서버가 부여한 `id`·`createdAt`·`status` 를 담아 클라이언트가 다시 조회하지 않아도 되게 했습니다. `Location` 헤더는 두지 않았습니다 — 본문의 `id` 로 주소를 그대로 만들 수 있어 정보가 겹칩니다. |
| **200** | 조회·수정 | 수정 결과를 본문으로 돌려줍니다.                                                                                                                                                                                                                    |
| **204** | 삭제      | 돌려줄 표현이 없습니다. `{"message":"삭제됨"}` 같은 본문은 클라이언트가 쓸 일이 없습니다.                                                                                                                                                           |
| **400** | 검증 실패 | 어떤 필드가 왜 거절됐는지 `message` 에 담습니다.                                                                                                                                                                                                    |
| **404** | 없는 id   | 조회·수정·삭제 모두 404 입니다. 엄격한 REST 관점에서 DELETE 는 멱등이라 재삭제에 204 를 주는 설계도 있지만, "없는 것을 지우려 했다"를 알려주는 편이 디버깅에 유리하다고 판단했습니다.                                                               |

- **목록은 비어 있어도 200** 입니다. 빈 컬렉션은 정상 상태이며 404 가 아닙니다.
- **`PATCH` 에 빈 본문 (`{}`)을 보내면 200 이고 아무것도 바뀌지 않습니다.** 부분 수정의 정의상 "바꿀 필드를 하나도 안 보냈다"는 유효한 요청입니다.

### 부분 수정에서 "안 보냄"과 "값" 구분

요청 DTO 의 필드를 래퍼 타입으로 두고, `null` 을 "변경하지 않음"으로 해석합니다. 검증은 "보냈다면 유효한가"만 보고 (`@Size`·`@Pattern` 은 `null` 을 통과시킵니다), 변경 여부
판단은 엔티티의 `update()` 가 담당합니다.

그래서 생성과 수정의 DTO 를 나눴습니다 — 생성은 `title` 이 필수 (`@NotBlank`)이고 수정은 선택이므로, 한 DTO 로는 이 차이를 표현할 수 없습니다.

### 정렬을 열지 않은 이유

`Pageable` 을 그대로 파라미터로 받으면 클라이언트가 `?sort=아무필드명` 을 보낼 수 있고, 존재하지 않는 프로퍼티면 **500** 이 납니다. 정렬 요구가 없으므로 `page`·`size` 만 받고
정렬은 서버가 고정했습니다. `size` 는 100 으로 상한을 둬 과도한 조회를 막습니다.

### 완료 여부를 `boolean` 이 아닌 enum 으로

`TodoStatus { TODO, DONE }` 을 씁니다. `boolean done` 은 상태가 둘일 때만 성립하고, 나중에 상태를 늘리려면 필드를 새로 만들고 API 계약을 바꿔야 합니다. enum 이면 상수를
추가하는 것으로 끝나고 `PATCH /{id}/status` 계약이 그대로 유지됩니다. `@Enumerated(EnumType.STRING)` 으로 저장하므로 선언 순서를 바꿔도 기존 데이터가 깨지지 않습니다.

### DB 를 PostgreSQL 로 고른 이유

- **개발·채점·운영 환경의 DB 를 동일하게 맞추기 위해서입니다.** H2 는 `MODE=MySQL` 같은 호환 모드 설정이 필요하고, 같은 엔티티에서도 방언에 따라 DDL 이 달라집니다 (enum 컬럼이 H2
  에서는 네이티브 `enum`, PostgreSQL 에서는 `varchar + check` 로 생성됩니다). 실제 DB 를 쓰면 이런 차이를 나중에 발견하지 않습니다.
- **`docker compose up` 한 번으로 앱과 함께 올라가므로** 받는 사람이 DB 를 따로 설치할 필요가 없습니다. H2 파일 DB 의 장점 (설치 불필요)이 컨테이너로 상쇄됩니다.
- 테스트만 인메모리 H2 를 씁니다. 외부 DB 없이 `./gradlew test` 가 돌아가는 편이 CI 에 유리하고, 이 프로젝트의 쿼리는 방언에 의존하지 않습니다.

### 구조

```
com.beac
├── global/exception     ErrorResponse, GlobalExceptionHandler
└── todo
    ├── controller       TodoController          — HTTP 경계, DTO 변환
    ├── service          TodoService             — 트랜잭션, 조회 후 위임
    ├── repository       TodoRepository          — Spring Data JPA
    ├── entity           Todo, TodoStatus        — 도메인 규칙
    ├── dto              요청·응답 DTO
    └── exception        TodoNotFoundException
```

- **요청·응답에 엔티티를 쓰지 않습니다.** 모든 엔드포인트가 `TodoResponseDto` 를 반환합니다.
- **상태 변경 로직은 엔티티 안에 있습니다.** 세터를 열지 않고 `update()`·`changeStatus()` 만 공개해, 어떤 경로로 수정되든 같은 규칙을 지나게 했습니다. `@Transactional`
  안에서 필드를 바꾸면 JPA 변경 감지가 UPDATE 를 내보내므로 서비스가 `save()` 를 부르지 않습니다.
- **생성은 정적 팩토리 `Todo.create(title, content)`** 만 허용합니다. `id`·`createdAt`·`updatedAt` 은 외부에서 지정할 수 없고, `title` 누락이 컴파일
  단계에서 걸립니다.

---

## 실행 결과

`docker compose up` 으로 올린 서버에 실제로 호출한 요청과 응답입니다. (`BASE=http://localhost:8080/api/v1/todos`)

### 1. 만들기

```bash
curl -i -X POST $BASE \
  -H 'Content-Type: application/json' \
  -d '{"title":"우체국 가기","content":"등기 보내기"}'
```

```
HTTP/1.1 201
Content-Type: application/json
```

```json
{
  "id": 21,
  "title": "우체국 가기",
  "content": "등기 보내기",
  "status": "TODO",
  "createdAt": "2026-09-18T16:56:39.189812",
  "updatedAt": "2026-09-18T16:56:39.189812"
}
```

### 2. 목록

```bash
curl "$BASE?size=2"
```

```
HTTP/1.1 200
```

```json
{
  "content": [
    {
      "id": 1,
      "title": "우유 사기",
      "content": "2L 한 통",
      "status": "TODO",
      "createdAt": "2026-09-18T16:56:38.924247",
      "updatedAt": "2026-09-18T16:56:38.924247"
    },
    {
      "id": 2,
      "title": "스프링 공부하기",
      "content": "JPA 변경 감지와 영속성 컨텍스트 정리",
      "status": "DONE",
      "createdAt": "2026-09-18T16:56:38.934935",
      "updatedAt": "2026-09-18T16:56:38.934935"
    }
  ],
  "page": {
    "size": 2,
    "number": 0,
    "totalElements": 22,
    "totalPages": 11
  }
}
```

### 3. 완료 처리

```bash
curl -i -X PATCH $BASE/21/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"DONE"}'
```

```
HTTP/1.1 200
```

```json
{
  "id": 21,
  "title": "우체국 가기",
  "content": "등기 보내기",
  "status": "DONE",
  "createdAt": "2026-09-18T16:56:39.189812",
  "updatedAt": "2026-09-18T16:56:39.288623"
}
```

`createdAt` 은 그대로고 `updatedAt` 만 갱신됩니다.

### 4. 삭제

```bash
curl -i -X DELETE $BASE/22
```

```
HTTP/1.1 204
Date: Fri, 18 Sep 2026 16:56:39 GMT
```

본문이 없습니다. `Content-Type` 헤더도 붙지 않습니다.

### 5. 400 — 공백뿐인 제목

```bash
curl -i -X POST $BASE \
  -H 'Content-Type: application/json' \
  -d '{"title":"   "}'
```

```
HTTP/1.1 400
```

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "title: 제목은 비어 있을 수 없습니다."
}
```

### 6. 404 — 없는 id 수정

```bash
curl -i -X PATCH $BASE/9999 \
  -H 'Content-Type: application/json' \
  -d '{"title":"없음"}'
```

```
HTTP/1.1 404
```

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "해당 할 일을 찾을 수 없습니다. id = 9999"
}
```

### 7. 상태 필터 (가산점)

```bash
curl "$BASE?status=DONE&size=1"
```

```json
{
  "content": [
    {
      "id": 2,
      "title": "스프링 공부하기",
      "content": "JPA 변경 감지와 영속성 컨텍스트 정리",
      "status": "DONE",
      "createdAt": "2026-09-18T16:56:38.934935",
      "updatedAt": "2026-09-18T16:56:38.934935"
    }
  ],
  "page": {
    "size": 1,
    "number": 0,
    "totalElements": 8,
    "totalPages": 8
  }
}
```

`totalElements` 가 전체 22 건이 아니라 `DONE` 8 건으로 집계됩니다.

### 8. 부분 수정 — 보낸 필드만 바뀝니다 (가산점 외)

```bash
curl -X PATCH $BASE/1 \
  -H 'Content-Type: application/json' \
  -d '{"content":"2L 저지방 한 통"}'
```

```json
{
  "id": 1,
  "title": "우유 사기",
  "content": "2L 저지방 한 통",
  "status": "TODO",
  "createdAt": "2026-09-18T16:56:38.924247",
  "updatedAt": "2026-09-18T16:56:39.456552086"
}
```

`title` 과 `status` 는 보내지 않았으므로 그대로 유지됩니다.

---

## 기술 스택

|             |                                      |
|-------------|--------------------------------------|
| Java        | 21                                   |
| Spring Boot | 3.5.16                               |
| 영속성      | Spring Data JPA (Hibernate 6)        |
| DB          | PostgreSQL 17 (테스트는 인메모리 H2) |
| 문서        | springdoc-openapi 2.8.17             |
| 빌드        | Gradle (Kotlin DSL)                  |
