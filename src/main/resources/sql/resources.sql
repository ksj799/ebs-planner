-- 부서별 자원과 능력
SELECT bd.department_id,
       bd.department_code,
       br.resource_id,
       br.resource_code,
       br.description,
       br.unit_of_measure,
       NVL(bdr.capacity_units, 1)          AS capacity_units,
       NVL(bdr.available_24_hours_flag, 2) AS available_24_hours_flag
  FROM bom_departments bd
  JOIN bom_department_resources bdr ON bdr.department_id = bd.department_id
  JOIN bom_resources br             ON br.resource_id    = bdr.resource_id
 WHERE bd.organization_id = :orgId
   AND (bd.disable_date IS NULL OR bd.disable_date > :today)
   AND (br.disable_date IS NULL OR br.disable_date > :today)
 ORDER BY bd.department_code, br.resource_code
