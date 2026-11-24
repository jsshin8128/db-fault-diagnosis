# 진단 서비스 설계

단일 에이전트의 기준선 측정에 필요한 범위까지 설계합니다. 전면 협력 구조와 점진적 협력 구조의 구현은 이번 설계 범위에서 제외합니다.

## 1. 모듈 구성과 협력 관계

Gradle 루트 프로젝트 `db-fault-diagnosis` 아래에 두 개의 모듈을 구성하고, 각각 독립적인 Spring Boot 애플리케이션으로 실행합니다. 모듈 간 통신은 HTTP와 데이터베이스를 통해 이루어집니다.

| 모듈                  | 기본 패키지                           | 역할                          |
| ------------------- | -------------------------------- | --------------------------- |
| `order-service`     | `io.github.jsshin8128.order`     | 장애를 재현하는 진단 대상 애플리케이션입니다.   |
| `diagnosis-service` | `io.github.jsshin8128.diagnosis` | 느린 API의 원인을 분석하는 진단 서비스입니다. |

진단 서비스 내부의 주요 협력 관계는 다음 세 가지입니다.

| 요청 주체               | 대상               | 요청 및 응답                                              |
| ------------------- | ---------------- | ---------------------------------------------------- |
| 실험 스크립트 또는 사용자      | 진단 컨트롤러          | 느린 API의 HTTP 메서드와 경로를 전달하고 진단 결과를 받습니다.              |
| `DiagnosisStrategy` | `AgentLoop`      | 프롬프트와 도구 목록을 포함한 `AgentSpec`을 전달하고 `AgentRun`을 받습니다. |
| `AgentLoop`         | `DiagnosticTool` | 도구 입력값을 전달하고 관측 데이터를 받습니다.                           |

인터페이스로 분리하는 역할은 `DiagnosticTool`과 `DiagnosisStrategy` 두 가지입니다.

`DiagnosticTool`은 향후 커넥션 분석과 락 분석 도구가 추가될 예정이며, `DiagnosisStrategy`는 연구 계획에 따라 세 가지 협력 구조를 구현해야 합니다. 두 역할 모두 구현체가 추가되더라도 호출하는 쪽의 코드를 변경하지 않도록 인터페이스를 사용합니다.

`AgentLoop`는 세 가지 협력 구조에서 공통으로 사용하는 실행 로직이므로 구체 클래스로 구현합니다. 모델 호출을 위한 별도 인터페이스는 두지 않습니다.

## 2. 진단 도구

`DiagnosticTool`은 `name`, `description`, `inputSchema`, `execute` 네 가지 메서드를 정의합니다. 진단 서비스는 각 구현체를 `List<DiagnosticTool>`로 주입받습니다.

데이터베이스에는 읽기 전용 진단 계정으로 접속하며, 기본 스키마는 지정하지 않습니다.

| 모델에 노출하는 이름        | 구현 클래스           | 입력           | 데이터 출처                                | 출력                                                        |
| ------------------ | ---------------- | ------------ | ------------------------------------- | --------------------------------------------------------- |
| `get_api_latency`  | `ApiLatencyTool` | HTTP 메서드, 경로 | Actuator의 `http.server.requests` 지표   | 호출 횟수, 평균, 최대, p50, p95, p99                              |
| `get_slow_queries` | `SlowQueryTool`  | 조회 개수        | `events_statements_summary_by_digest` | `digest`, 정규화된 SQL, 호출 횟수, 평균 실행 시간, 평균 조회 행 수, 평균 반환 행 수 |
| `get_query_plan`   | `QueryPlanTool`  | `digest`     | 동일 테이블의 `QUERY_SAMPLE_TEXT`           | 예시 SQL, 실행 계획                                             |

진단 도구의 주요 설계 원칙은 다음 세 가지입니다.

1. **`get_query_plan`은 SQL 문자열 대신 `digest`를 입력받습니다.** 도구가 `digest`에 해당하는 예시 SQL을 직접 조회한 뒤 `EXPLAIN FORMAT=TREE FOR SCHEMA`로 실행 계획을 확인합니다. 따라서 에이전트가 생성한 SQL이 데이터베이스에 직접 전달되지 않습니다. 이때 실행한 문장은 통계 테이블에 스키마 이름이 없는 항목으로 기록되므로 애플리케이션 쿼리와 구분할 수 있습니다.
2. **`EXPLAIN ANALYZE`는 사용하지 않습니다.** 운영 데이터베이스에서 진단 목적으로 쿼리를 실제 실행하지 않기 위해서입니다.
3. **`get_slow_queries`는 시스템 스키마의 쿼리를 제외하고 반환합니다.** `SUM_NO_INDEX_USED`는 출력에 포함하지 않습니다. 인덱스 사용 여부는 실행 계획을 통해 판단합니다.

## 3. 에이전트 실행과 진단 결과

`AgentLoop`는 Anthropic Java SDK의 Messages API를 호출합니다. 모델 응답에 도구 호출 요청이 포함되면 해당 도구를 실행하고 결과를 모델에 전달하는 과정을 반복합니다.

실행 중에는 각 API 요청의 토큰 사용량을 합산하고 도구 호출 순서를 기록합니다.

단일 에이전트 구조인 `SingleAgentStrategy`는 모든 진단 도구를 포함한 하나의 `AgentSpec`으로 `AgentLoop`를 한 번 실행합니다.

에이전트의 최종 응답은 구조화 출력으로 받아 `Diagnosis`에 저장합니다.

`Diagnosis`는 다음 다섯 가지 필드로 구성합니다.

| 필드                  | 설명                              |
| ------------------- | ------------------------------- |
| `rootCause`         | 진단한 원인 범주입니다.                   |
| `targetTable`       | 원인이 발생한 테이블이며, 해당하지 않으면 비워 둡니다. |
| `targetColumn`      | 원인이 발생한 컬럼이며, 해당하지 않으면 비워 둡니다.  |
| `evidence`          | 진단 판단의 근거가 된 관측값입니다.            |
| `recommendedAction` | 권장 조치입니다.                       |

`rootCause`는 `RootCause` 열거형으로 정의하며, 다음 일곱 가지 값을 사용합니다.

- `REPEATED_DB_CALLS`
- `CONNECTION_POOL_EXHAUSTION`
- `INEFFICIENT_SQL_CONDITION`
- `MISSING_INDEX`
- `LOCK_WAIT`
- `NO_PROBLEM`
- `UNDETERMINED`

앞의 다섯 가지는 연구 계획에서 정의한 장애 시나리오에 대응합니다. `NO_PROBLEM`과 `UNDETERMINED`는 정상 API와 진단 근거가 부족한 경우를 구분하기 위해 사용합니다.

`POST /api/diagnoses` 응답에는 `Diagnosis`와 함께 다음 실행 정보를 포함합니다.

- 입력 토큰 수
- 출력 토큰 수
- 캐시에서 읽은 입력 토큰 수
- API 요청 횟수
- 총 소요 시간
- 도구 호출 목록

## 4. 측정 절차와 채점

데이터베이스는 인덱스가 적용된 상태를 기본으로 구성합니다. 인덱스 미사용 장애는 `scenarios/missing-index/inject.sql`로 주입하고, `restore.sql`로 원래 상태를 복구합니다.

각 측정 회차는 다음 순서로 진행합니다.

1. `order-service`를 재기동하여 Actuator 지표를 초기화합니다.
2. 관리 계정으로 통계 테이블을 `TRUNCATE`하여 기존 통계를 초기화합니다.
3. 동시 요청 수 2로 `GET /api/orders?customerId={난수}`를 총 200회 호출합니다.
4. `POST /api/diagnoses`에 `GET /api/orders`를 전달하여 진단을 실행합니다.
5. 진단 응답을 측정 조건 및 회차 번호와 함께 JSONL 형식으로 한 줄씩 저장합니다.

측정 조건과 횟수는 다음과 같습니다.

- **장애 주입 조건:** 인덱스를 제거한 상태에서 30회 측정합니다.
- **기본 조건:** 인덱스가 적용된 상태에서 10회 측정합니다.

본 측정에 앞서 파일럿 3회를 진행하여 측정 절차와 회차당 비용을 확인합니다.

모델, effort, 시스템 프롬프트, 도구 정의는 전체 40회 측정 동안 동일하게 유지합니다.

채점은 측정 기록 파일을 읽는 스크립트에서 자동으로 수행합니다.

- **장애 주입 조건:** `rootCause`가 `MISSING_INDEX`이고, `targetTable`이 `orders`, `targetColumn`이 `customer_id`이면 성공으로 판정합니다.
- **기본 조건:** `rootCause`가 `NO_PROBLEM`이면 성공으로 판정합니다.

## 5. 미결정 사항

1. **실험 모델 및 effort:** 연구 결과에 영향을 미치는 변수이므로 확정해야 합니다.
2. **도구 호출 순서 강제 여부:** 시스템 프롬프트에서 도구 호출 순서를 강제하면 불필요한 도구 호출 여부를 관찰하기 어렵습니다. 따라서 호출 순서를 강제하지 않고 에이전트가 선택한 순서를 기록하는 방안을 권장합니다.
3. **MySQL 부하 조건:** 데이터 1,000만 행과 동시 요청 수 2인 조건에서 p99 응답 시간이 500ms를 초과하는지 다시 측정해야 합니다.
