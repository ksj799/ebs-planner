-- 자원별 일 가동시간 = Σ(교대 시간 × 교대 능력단위). FROM_TIME/TO_TIME 은 자정 기준 초
SELECT brs.department_id,
       brs.resource_id,
       SUM((CASE WHEN bst.to_time > bst.from_time THEN bst.to_time - bst.from_time
                 ELSE bst.to_time + 86400 - bst.from_time END) / 3600
           * NVL(brs.capacity_units, NVL(bdr.capacity_units, 1))) AS shift_unit_hours
  FROM bom_resource_shifts brs
  JOIN bom_department_resources bdr ON bdr.department_id = brs.department_id
                                   AND bdr.resource_id   = brs.resource_id
  JOIN bom_departments bd           ON bd.department_id  = brs.department_id
  JOIN bom_shift_times bst          ON bst.calendar_code = :calendarCode
                                   AND bst.shift_num     = brs.shift_num
 WHERE bd.organization_id = :orgId
 GROUP BY brs.department_id, brs.resource_id
