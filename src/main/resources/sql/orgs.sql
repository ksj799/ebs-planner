-- WIP 사용 조직
SELECT mp.organization_id,
       mp.organization_code,
       hou.name                              AS organization_name,
       mp.calendar_code,
       NVL(mp.calendar_exception_set_id, -1) AS calendar_exception_set_id,
       wp.default_discrete_class
  FROM mtl_parameters mp
  JOIN hr_all_organization_units hou ON hou.organization_id = mp.organization_id
  JOIN wip_parameters wp             ON wp.organization_id  = mp.organization_id
 ORDER BY mp.organization_code
