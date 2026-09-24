# Todo API

Spring Boot 3, Spring Data JPA, PostgreSQL 로 만든 Todo REST API 서버입니다.

---

## 실행 방법

### 필요한 것

- Docker / Docker Compose (Compose V2)

### 실행

```bash
docker compose up
```

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

### 테스트

```bash
./gradlew test
```

---

## API 명세

기본 경로: `/api/v1/todos`

| 메서드   | 주소                 | 설명                         | 성공    |
|----------|----------------------|------------------------------|---------|
| `POST`   | `/api/v1/todos`      | 생성                         | **201** |
| `GET`    | `/api/v1/todos`      | 목록 (페이징, 상태 필터)     | **200** |
| `GET`    | `/api/v1/todos/{id}` | 단건 조회                    | **200** |
| `PUT`    | `/api/v1/todos/{id}` | 수정 (제목, 내용, 완료 여부) | **200** |
| `DELETE` | `/api/v1/todos/{id}` | 삭제                         | **204** |

### 할 일 표현

```json
{
  "id": 21,
  "title": "우체국 가기",
  "content": "등기 보내기",
  "status": "TODO",
  "createdAt": "2026-09-24T17:26:36",
  "updatedAt": "2026-09-24T17:26:36"
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

응답: **201**

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
    "totalElements": 21,
    "totalPages": 11
  }
}
```

### GET /api/v1/todos/{id}

응답: **200** / **404**

### PUT /api/v1/todos/{id}

```json
{
  "title": "우체국 가기",
  "content": "등기 보내기",
  "status": "DONE"
}
```

- `title` 필수 (1–20자, 공백만 불가)
- `content` 선택 (최대 50자). 빼거나 `null` 을 보내면 내용이 지워집니다
- `status` 필수, `TODO` 또는 `DONE` (대소문자 구분)

응답: **200** / **400** / **404**

### DELETE /api/v1/todos/{id}

응답: **204** / **404**

### 오류 응답

모든 오류가 같은 형태입니다.

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

| 상황                                   | 상태                                        |
|----------------------------------------|---------------------------------------------|
| 검증 실패 (본문•쿼리 파라미터)         | 400                                         |
| 잘못된 JSON, 타입 불일치, 없는 enum 값 | 400                                         |
| 없는 id                                | 404                                         |
| 없는 경로                              | 404                                         |
| 지원하지 않는 메서드                   | 405                                         |
| 지원하지 않는 Content-Type             | 415                                         |
| 그 외 예상하지 못한 오류               | 500 (스택트레이스는 서버 로그에만 남깁니다) |

---

## 설계 설명

### 주소

- `/api/v1/todos`: 컬렉션은 복수형 명사, 동사는 쓰지 않습니다. `v1` 을 둬서 이후 호환되지 않는 변경을 새 버전으로 낼 수 있게 했습니다.
- 수정은 `PUT /{id}` 하나: 제목, 내용, 완료 여부를 하나의 API 로 바꿉니다.
- `PATCH` 대신 `PUT`: 전체 교체라서 "보내지 않은 필드"와 "`null` 을 보낸 필드"를 구분할 필요가 없고, 내용을 지우려면 `content` 를 빼고 보내면 됩니다.
  또 `PUT` 은 HTTP 명세상 멱등이므로, 응답을 받지 못했을 때 클라이언트가 같은 요청을 다시 보내도 안전하다는 것이 메서드만으로 드러납니다.

### 상태 코드

| 코드    | 쓰는 곳    | 이유                                             |
|---------|------------|--------------------------------------------------|
| **201** | 생성       | 새 리소스가 생겼으므로 200 이 아닙니다.          |
| **200** | 조회, 수정 | 수정 결과를 본문으로 돌려줍니다.                 |
| **204** | 삭제       | 돌려줄 응답이 없습니다.                          |
| **400** | 검증 실패  | 어떤 필드가 왜 거절됐는지 `message` 에 담습니다. |
| **404** | 없는 id    | 조회, 수정, 삭제 모두 404 입니다.                |

- 목록이 비어 있어도 200 입니다. 빈 컬렉션은 정상 상태이며 404 가 아닙니다.
- 같은 `PUT` 을 여러 번 보내도 결과가 같습니다. 값이 바뀌지 않으면 `updatedAt` 도 그대로입니다.

### 정렬을 열지 않은 이유

정렬 요구가 없으므로 `page`, `size` 만 받고 정렬은 생성시각 (createdAt)으로 고정했습니다. `size` 는 100 으로 상한을 둬 과도한 조회를 막습니다.

### 완료 여부를 `boolean` 이 아닌 enum 으로 한 이유

`TodoStatus { TODO, DONE }` 을 씁니다. `boolean done` 은 상태가 둘일 때만 성립하고, 나중에 상태를 늘리려면 필드를 새로 만들고 API 계약을 바꿔야 합니다.
enum 이면 상수만 추가하면 되고, `status` 필드의 계약이 그대로 유지됩니다.

### DB 를 PostgreSQL 로 고른 이유

- 개발과 운영 환경의 DB 를 동일하게 맞추기 위해서입니다.
- `docker compose up` 한 번으로 앱과 함께 올라가므로 따로 DB를 설치할 필요가 없습니다.
- 테스트만 인메모리 H2 를 씁니다. 외부 DB 없이 `./gradlew test` 가 돌아가는 편이 CI 에 유리하고, 이 프로젝트의 쿼리는 방언 (Dialect)에 의존하지 않습니다.

---

## 실행 결과

예시 데이터 20건 (id 1–20)이 들어 있는 상태에서 시작합니다.

- `BASE=http://localhost:8080/api/v1/todos`

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
  "createdAt": "2026-09-24T17:26:36",
  "updatedAt": "2026-09-24T17:26:36"
}
```

### 2. 목록

```bash
curl -i "$BASE?size=2"
```

```
HTTP/1.1 200
Content-Type: application/json
```

```json
{
  "content": [
    {
      "id": 1,
      "title": "우유 사기",
      "content": "2L 한 통",
      "status": "TODO",
      "createdAt": "2026-09-24T17:26:33",
      "updatedAt": "2026-09-24T17:26:33"
    },
    {
      "id": 2,
      "title": "스프링 공부하기",
      "content": "JPA 변경 감지와 영속성 컨텍스트 정리",
      "status": "DONE",
      "createdAt": "2026-09-24T17:26:33",
      "updatedAt": "2026-09-24T17:26:33"
    }
  ],
  "page": {
    "size": 2,
    "number": 0,
    "totalElements": 21,
    "totalPages": 11
  }
}
```

예시 데이터 20건과 방금 만든 21번을 합쳐 `totalElements` 가 21 입니다.

### 3. 완료 처리

```bash
curl -i -X PUT $BASE/21 \
  -H 'Content-Type: application/json' \
  -d '{"title":"우체국 가기","content":"등기 보내기","status":"DONE"}'
```

```
HTTP/1.1 200
Content-Type: application/json
```

```json
{
  "id": 21,
  "title": "우체국 가기",
  "content": "등기 보내기",
  "status": "DONE",
  "createdAt": "2026-09-24T17:26:36",
  "updatedAt": "2026-09-24T17:26:39"
}
```

`createdAt` 은 그대로고 `updatedAt` 만 갱신됩니다.

### 4. 삭제

```bash
curl -i -X DELETE $BASE/21
```

```
HTTP/1.1 204
```

### 5. 400 - 제목 공백

```bash
curl -i -X POST $BASE \
  -H 'Content-Type: application/json' \
  -d '{"title":"   "}'
```

```
HTTP/1.1 400
Content-Type: application/json
```

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "title: 제목은 비어 있을 수 없습니다."
}
```

### 6. 404 - 존재하지 않은 id 수정

```bash
curl -i -X PUT $BASE/9999 \
  -H 'Content-Type: application/json' \
  -d '{"title":"없음","status":"DONE"}'
```

```
HTTP/1.1 404
Content-Type: application/json
```

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "해당 할 일을 찾을 수 없습니다. id = 9999"
}
```

### 7. 상태 필터

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
      "createdAt": "2026-09-24T17:26:33",
      "updatedAt": "2026-09-24T17:26:33"
    }
  ],
  "page": {
    "size": 1,
    "number": 0,
    "totalElements": 7,
    "totalPages": 7
  }
}
```

### 8. 수정은 전체 교체 - 내용 지우기

```bash
curl -i -X PUT $BASE/1 \
  -H 'Content-Type: application/json' \
  -d '{"title":"우유 사기","status":"TODO"}'
```

```
HTTP/1.1 200
Content-Type: application/json
```

```json
{
  "id": 1,
  "title": "우유 사기",
  "content": null,
  "status": "TODO",
  "createdAt": "2026-09-24T17:26:33",
  "updatedAt": "2026-09-24T17:26:42"
}
```

`content` 를 null로 보내거나 보내지 않으면 기존 content ("2L 한 통")이 지워집니다.
