-- 이 프로그램이 넣은 작업지시 생성 요청 이력
SELECT wjsi.group_id,
       wjsi.interface_id,
       wjsi.source_line_id,
       msi.segment1 AS item_code,
       wjsi.start_quantity,
       wjsi.first_unit_start_date,
       wjsi.process_phase,
       wjsi.process_status,
       NVL(wjsi.job_name, we.wip_entity_name) AS job_name,
       wjsi.wip_entity_id,
       wjsi.creation_date,
       wjsi.description
  FROM wip_job_schedule_interface wjsi
  LEFT JOIN mtl_system_items_b msi ON msi.inventory_item_id = wjsi.primary_item_id
                                  AND msi.organization_id   = wjsi.organization_id
  LEFT JOIN wip_entities we        ON we.wip_entity_id      = wjsi.wip_entity_id
 WHERE wjsi.organization_id = :orgId
   AND wjsi.source_code     = :sourceCode
   AND wjsi.creation_date  >= :fromDate
 ORDER BY wjsi.group_id DESC, wjsi.interface_id
