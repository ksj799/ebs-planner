-- 근무일 (SEQ_NUM 이 NULL 이면 휴무)
SELECT bcd.calendar_date
  FROM bom_calendar_dates bcd
 WHERE bcd.calendar_code    = :calendarCode
   AND bcd.exception_set_id = :exceptionSetId
   AND bcd.calendar_date BETWEEN :fromDate AND :toDate
   AND bcd.seq_num IS NOT NULL
 ORDER BY bcd.calendar_date
