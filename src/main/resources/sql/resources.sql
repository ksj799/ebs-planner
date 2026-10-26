-- 부서별 작업 라인(리소스)과 능력. resource_type: 1=설비, 2=인원
SELECT bd.department_id,
       bd.department_code,
       bd.description                      AS department_desc,
       br.resource_id,
       br.resource_code,
       br.description,
       NVL(br.resource_type, 1)            AS resource_type,
       br.unit_of_measure,
       NVL(bdr.capacity_units, 1)          AS capacity_units,
       NVL(bdr.available_24_hours_flag, 2) AS available_24_hours_flag
  FROM bom_departments bd
  JOIN bom_department_resources bdr ON bdr.department_id = bd.department_id
  JOIN bom_resources br             ON br.resource_id    = bdr.resource_id
 WHERE bd.organization_id = :orgId
   AND (bd.disable_date IS NULL OR bd.disable_date > :today)
   AND (br.disable_date IS NULL OR br.disable_date > :today)
 ORDER BY bd.description, bd.department_code, br.resource_code
