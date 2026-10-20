-- H2 전용 캘린더 (Oracle 은 02_demo_reset.sql 에서 CONNECT BY 로 같은 내용 생성)
-- 주말 휴무 + 9일 뒤 임시휴일 1일
INSERT INTO bom_calendar_dates (calendar_code, exception_set_id, calendar_date, seq_num)
SELECT 'DEMO-CAL', -1, DATEADD('DAY', x - 200, CURRENT_DATE),
       CASE WHEN ISO_DAY_OF_WEEK(DATEADD('DAY', x - 200, CURRENT_DATE)) IN (6, 7) THEN NULL ELSE x + 1 END
  FROM SYSTEM_RANGE(0, 799);
UPDATE bom_calendar_dates SET seq_num = NULL WHERE calendar_date = DATEADD('DAY', 9, CURRENT_DATE);
