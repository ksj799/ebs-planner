# 실제 Oracle EBS 연결 가이드 (Discrete WIP)

Oracle EBS R12 Discrete Manufacturing 테이블을 직접 읽어 **수주 기반 유한능력 생산계획**을 세우고,
확정한 계획을 **WIP 표준 인터페이스(`WIP_JOB_SCHEDULE_INTERFACE`)로 작업지시 생성**까지 하는 웹 프로그램.

- 백엔드: Spring Boot 3 (Java 17), JdbcTemplate, ojdbc11
- 화면: `src/main/resources/static/index.html` (단일 페이지)
- SQL: `src/main/resources/sql/*.sql` – DBA 검토·튜닝용으로 분리

## 실행

### 1) 데모 (EBS 없이)
EBS 테이블 구조를 흉내 낸 H2 인메모리 DB로 실행. 작업지시 생성 시 Mass Load 도 흉내 낸다.
```
mvn -DskipTests package
java -jar target/ebs-planner-0.1.0.jar --spring.profiles.active=demo
```
→ http://localhost:8080

### 2) 실제 EBS (반드시 TEST/CRP 인스턴스부터)
```
set EBS_DB_URL=jdbc:oracle:thin:@//ebs-db-host:1521/EBSTEST
set EBS_DB_USER=apps
set EBS_DB_PASSWORD=********
set EBS_USER_ID=1234          (FND_USER.USER_ID – 생성자로 기록됨)
java -jar target/ebs-planner-0.1.0.jar
```
`EBS_USER_ID` 가 없으면 조회·계획만 되고 작업지시 생성 버튼은 비활성화된다.

## 사용하는 EBS 테이블

| 구분 | 테이블 | 용도 |
|---|---|---|
| 조직 | MTL_PARAMETERS, HR_ALL_ORGANIZATION_UNITS, WIP_PARAMETERS | WIP 사용 조직, 캘린더, 기본 회계클래스 |
| 캘린더 | BOM_CALENDAR_DATES, BOM_SHIFT_TIMES | 근무일(SEQ_NUM NULL=휴무), 교대시간 |
| 자원 | BOM_DEPARTMENTS, BOM_RESOURCES, BOM_DEPARTMENT_RESOURCES, BOM_RESOURCE_SHIFTS | 일 능력 = Σ(교대시간 × 능력단위), 24시간 자원은 24 × 단위 |
| 라우팅 | BOM_OPERATIONAL_ROUTINGS, BOM_OPERATION_SEQUENCES, BOM_OPERATION_RESOURCES | 주 라우팅, 개당(BASIS 1)/로트당(BASIS 2=셋업) 시간 |
| 품목 | MTL_SYSTEM_ITEMS_B | BUILD_IN_WIP_FLAG='Y' |
| 수요 | OE_ORDER_HEADERS_ALL, OE_ORDER_LINES_ALL, HZ_CUST_ACCOUNTS, HZ_PARTIES | 예약(Booked)·미결 수주라인 미출하량, 납기=출하예정일(없으면 요청일) |
| 공급 | WIP_DISCRETE_JOBS, WIP_ENTITIES | 상태 1·3·6 작업지시 잔량 |
| 공급 | WIP_JOB_SCHEDULE_INTERFACE | 처리대기(1·2) 행 – 이중 생성 방지 |
| 공급 | MTL_ONHAND_QUANTITIES_DETAIL, MTL_SECONDARY_INVENTORIES, MTL_ITEM_LOCATIONS_KFV | 현재고(보관장소·로케이터·로트), Nettable 여부 |
| 자재 | BOM_STRUCTURES_B, BOM_COMPONENTS_B | 주 BOM 1단계 구성품 (팬텀 제외) |
| 자재 | WIP_REQUIREMENT_OPERATIONS | 기존 작업지시 미출고 자재 소요 |
| 구매 | PO_HEADERS_ALL, PO_LINES_ALL, PO_LINE_LOCATIONS_ALL, AP_SUPPLIERS | 미입고 PO 납품일정 (필요일·약속일) |
| 구매 | MTL_SYSTEM_ITEMS_B | 전처리/처리/후처리 리드타임, 최소주문량, 주문배수 |
| 품목 정보 | MTL_SYSTEM_ITEMS_B, MTL_PLANNERS, FND_COMMON_LOOKUPS(ITEM_TYPE) | Oracle 품목 화면의 주요·일반 계획·MPS/MRP 계획·리드타임·재공 탭 속성 (조회 전용) |
| **쓰기** | **WIP_JOB_SCHEDULE_INTERFACE** | LOAD_TYPE=1 표준 작업지시 생성 요청 |
| 결과 | WIP_INTERFACE_ERRORS | Mass Load 오류 |

EBS 기본 테이블에는 **아무것도 직접 쓰지 않는다.** 작업지시는 EBS 의 WIP Mass Load 가 검증 후 만든다.

## 계획 로직

1. **순소요**: 품목별로 (현재고 + 기존 작업지시 잔량 + 처리대기 인터페이스) 를 납기순 수주라인에 충당 → 부족분마다 계획 작업지시 1건(Lot-for-Lot)
2. **병목 자원**: 품목의 주 라우팅에서 개당 시간이 가장 큰 부서-자원 하나로 부하를 잡는다
3. **기존 부하**: 기존 작업지시 잔량 시간을 EBS 일정(시작~완료) 근무일에 균등 배분
4. **유한능력 일정**: 계획 작업지시를 납기순으로, 시작일부터 남은 능력에 셋업 → 수량 순으로 전진 배정. 완료일 > 납기면 "지연"
5. 화면에서 수량·시작일을 고치면 즉시 재계산. 생성 시 서버가 **최신 EBS 데이터로 다시 계산**한 값으로 적재

## 자재 · 구매 (구매부품 리드타임 반영)

- **구매 입고예정 화면**
  - PO 납품일정마다 *입고예정일*을 수정할 수 있다 (적용 순서: 수정값 → 약속일 → 필요일). 예정일이 지났으면 오늘 입고로 본다
  - 품목별 전처리·처리·후처리 리드타임을 수정할 수 있다 (빈칸 = EBS 값)
  - **수정값은 `planner.store-path` JSON 파일(기본 `~/.ebs-planner/settings.json`)에만 저장되고 EBS PO·품목은 바뀌지 않는다**
- **자재 계획**
  - 사용 가능일 = 입고예정일 + 후처리 LT (근무일)
  - 구매부품 공급(현재고 + PO)을 기존 작업지시 소요 → 계획 작업지시(납기순) 순서로 선입선출 배정한다
  - 모자라면 구매 제안을 만든다. 사용 가능일 = max(필요일, 오늘 + 총 LT), 발주 착수일 = 사용 가능일 − 후처리 − 처리 − 전처리. 수량에는 최소주문량·주문배수를 적용한다
  - 계획 작업지시는 모든 구매부품이 사용 가능해지는 날 이후에만 시작한다 (화면의 *자재 대기*)
- **현재고 화면**: 보관장소·로케이터·로트·리비전별 상세와 품목별 합계(가용/비가용)를 보여준다

## 작업지시 생성 흐름

1. 생산계획 탭에서 계획을 선택 → 상태(1 미해제 / 3 해제), 회계 클래스 지정 → **EBS 작업지시 생성**
2. 한 트랜잭션으로 `GROUP_ID`(WIP_JOB_SCHEDULE_INTERFACE_S) 하나에 행들을 INSERT
   - SOURCE_CODE=`XXPP`, SOURCE_LINE_ID=수주라인 ID, DESCRIPTION=`SO 번호-라인 고객`
   - SCHEDULING_METHOD 1(기본): 시작일만 넘기고 EBS 가 라우팅으로 일정 계산 / 3: 계획의 시작·완료일 그대로
3. `planner.mass-load.submit=true` 면 같은 트랜잭션에서 `FND_REQUEST.SUBMIT_REQUEST('WIP','WICMLP', …)` 제출.
   아니면 EBS 화면에서 *WIP Mass Load* 를 해당 Group ID 로 실행
4. **생성 이력** 탭에서 처리 상태·생성된 작업지시 번호·오류(WIP_INTERFACE_ERRORS) 확인

## 설정 (`application.yml`)

| 키 | 기본값 | 설명 |
|---|---|---|
| planner.user-id | (환경변수 EBS_USER_ID) | CREATED_BY, apps_initialize 사용자 |
| planner.source-code | XXPP | 이 프로그램이 만든 데이터 식별자 |
| planner.wip.status-type | 1 | 1 미해제 / 3 해제 (화면에서 변경 가능) |
| planner.wip.scheduling-method | 1 | 1 라우팅 기준 / 3 수동 |
| planner.wip.class-code | (빈값) | 비우면 WIP_PARAMETERS.DEFAULT_DISCRETE_CLASS |
| planner.mass-load.submit | false | true 면 Mass Load 자동 제출 |
| planner.mass-load.resp-id | (EBS_RESP_ID) | 제출 책임 ID (WIP 책임) |
| planner.mass-load.validation-level / print-report | "0" / "2" | **인스턴스의 WICMLP 동시 프로그램 파라미터 정의를 확인 후 조정** |
| planner.planning.default-hours-per-day | 8 | 교대 미등록 자원의 일 가동시간 |
| planner.planning.uom-hours | HR=1, MIN=1/60, SEC=1/3600 | 자원 UOM → 시간 환산. 사내 시간 UOM 코드가 다르면 추가 |

## APPS 가 아닌 전용 계정을 쓸 때 필요한 권한
- 위 조회 테이블 SELECT (APPS 시노님 또는 스키마 접두어)
- `WIP.WIP_JOB_SCHEDULE_INTERFACE` INSERT, `WIP_JOB_SCHEDULE_INTERFACE_S`·`WIP_INTERFACE_S` SELECT
- 자동 제출 시 `FND_GLOBAL`, `FND_REQUEST`, `FND_MESSAGE` EXECUTE

## 운영 전 점검 목록
- [ ] TEST 인스턴스에서 Mass Load 까지 실제 생성 확인, 생성된 작업지시의 일정·자재·클래스 검토
- [ ] 수주 단위 ≠ 기본 단위인 품목 여부 (현재는 환산하지 않고 경고만 표시)
- [ ] 대량 데이터 조직에서 `demands.sql`, `routing_ops.sql` 실행계획 확인
- [ ] 여러 사람이 쓸 경우 `server.address` 변경 + 로그인/권한(Spring Security, EBS 책임 연동) 추가

## 현재 한계 (다음 단계 후보)
- 품목당 병목 자원 1개만 부하 반영 (공정별 다중 자원·공정 간 리드타임 미반영)
- BOM 은 1단계 구매부품만 반영 (제조 반제품 다단계 전개·팬텀 전개 미반영)
- 구매 공급은 PO 만 반영 (승인 구매요청·운송중·입고 후 미반입 RCV_SUPPLY 미반영), PO 단위 ≠ 기본 단위 환산 없음
- 구매 제안은 화면 표시만 (PO_REQUISITIONS_INTERFACE_ALL 로 구매요청 생성은 다음 단계 후보)
- 로트 합치기(같은 품목 수요 묶기), 최소/배수 로트(MTL_SYSTEM_ITEMS_B 의 FIXED_LOT_MULTIPLIER 등) 미적용
- 수요는 수주만 (Forecast/MDS 미포함)
