#!/usr/bin/env bash
# H2 데모 샘플(src/main/resources/demo/data.sql, item_attributes.sql)로 Oracle 용 02_demo_reset.sql 을 만든다.
# 샘플 데이터를 고칠 때는 H2 파일만 고치고 이 스크립트를 다시 실행한다:  bash db/oracle/generate.sh
set -euo pipefail
cd "$(dirname "$0")"
SRC=../../src/main/resources/demo
OUT=02_demo_reset.sql

body=$(cat "$SRC/data.sql" "$SRC/item_attributes.sql" \
  | sed -E "s/DATEADD\('DAY', *(-?[0-9]+), *CURRENT_DATE\)/(d0 + (\1))/g" \
  | sed 's/^/  /')
if grep -qiE "DATEADD|CURRENT_DATE|SYSTEM_RANGE" <<<"$body"; then
  echo "변환되지 않은 H2 전용 구문이 남아 있습니다:" >&2
  grep -niE "DATEADD|CURRENT_DATE|SYSTEM_RANGE" <<<"$body" >&2
  exit 1
fi

{
cat <<'HEAD'
-- =====================================================================
-- [자동 생성 파일 – 직접 고치지 말 것] db/oracle/generate.sh 가 H2 데모 샘플에서 만든다.
-- 가상 데이터 생성 프로시저 + 매일 초기화 작업. ADMIN 으로 실행해도 EBSDEMO 소유로 만들어진다.
-- 모든 날짜는 실행일(d0) 기준 상대값이라, 매일 다시 만들면 항상 "오늘" 기준 데모가 된다.
-- 실제 회사 데이터는 한 건도 들어가지 않는다.
-- =====================================================================
ALTER SESSION SET CURRENT_SCHEMA = EBSDEMO;

CREATE OR REPLACE PROCEDURE demo_reset AUTHID DEFINER AS
  d0 DATE := TRUNC(SYSDATE);
BEGIN
  -- ---------- 초기화 ----------
  DELETE FROM wip_interface_errors;          DELETE FROM wip_job_schedule_interface;
  DELETE FROM wip_requirement_operations;    DELETE FROM wip_discrete_jobs;          DELETE FROM wip_entities;
  DELETE FROM po_line_locations_all;         DELETE FROM po_lines_all;               DELETE FROM po_headers_all;
  DELETE FROM ap_suppliers;                  DELETE FROM oe_order_lines_all;         DELETE FROM oe_order_headers_all;
  DELETE FROM hz_cust_accounts;              DELETE FROM hz_parties;                 DELETE FROM mtl_onhand_quantities_detail;
  DELETE FROM mtl_item_locations_kfv;        DELETE FROM mtl_secondary_inventories;  DELETE FROM bom_components_b;
  DELETE FROM bom_structures_b;              DELETE FROM bom_operation_resources;    DELETE FROM bom_operation_sequences;
  DELETE FROM bom_operational_routings;      DELETE FROM mtl_system_items_b;         DELETE FROM mtl_planners;
  DELETE FROM fnd_common_lookups;            DELETE FROM bom_resource_shifts;        DELETE FROM bom_department_resources;
  DELETE FROM bom_resources;                 DELETE FROM bom_departments;            DELETE FROM bom_shift_times;
  DELETE FROM bom_calendar_dates;            DELETE FROM wip_parameters;             DELETE FROM hr_all_organization_units;
  DELETE FROM mtl_parameters;

  -- ---------- 캘린더 (H2 의 demo/calendar.sql 과 같은 내용): 주말 휴무 + 9일 뒤 임시휴일 ----------
  INSERT INTO bom_calendar_dates (calendar_code, exception_set_id, calendar_date, seq_num)
  SELECT 'DEMO-CAL', -1, d0 - 200 + LEVEL - 1,
         CASE WHEN TO_CHAR(d0 - 200 + LEVEL - 1, 'DY', 'NLS_DATE_LANGUAGE=AMERICAN') IN ('SAT', 'SUN') THEN NULL ELSE LEVEL END
    FROM dual CONNECT BY LEVEL <= 800;
  UPDATE bom_calendar_dates SET seq_num = NULL WHERE calendar_date = d0 + 9;

HEAD
printf '%s\n' "$body"
cat <<'TAIL'

  COMMIT;
END demo_reset;
/

-- 지금 한 번 생성
BEGIN
  EBSDEMO.demo_reset;
END;
/

-- 매일 03:00 KST (= 18:00 UTC) 초기화
BEGIN
  BEGIN
    DBMS_SCHEDULER.DROP_JOB('EBSDEMO.DEMO_RESET_DAILY');
  EXCEPTION WHEN OTHERS THEN NULL;
  END;
  DBMS_SCHEDULER.CREATE_JOB(
    job_name        => 'EBSDEMO.DEMO_RESET_DAILY',
    job_type        => 'STORED_PROCEDURE',
    job_action      => 'EBSDEMO.DEMO_RESET',
    start_date      => SYSTIMESTAMP,
    repeat_interval => 'FREQ=DAILY;BYHOUR=18;BYMINUTE=0;BYSECOND=0',
    enabled         => TRUE,
    comments        => '포트폴리오 데모 데이터를 오늘 날짜 기준으로 다시 생성');
END;
/

-- 확인: items 20, so_lines 27, jobs 8, po_shipments 15
SELECT 'items' AS t, COUNT(*) AS n FROM EBSDEMO.mtl_system_items_b
UNION ALL SELECT 'so_lines', COUNT(*) FROM EBSDEMO.oe_order_lines_all
UNION ALL SELECT 'jobs', COUNT(*) FROM EBSDEMO.wip_discrete_jobs
UNION ALL SELECT 'po_shipments', COUNT(*) FROM EBSDEMO.po_line_locations_all;
TAIL
} > "$OUT"
echo "생성: db/oracle/$OUT ($(wc -l < "$OUT") 줄)"
