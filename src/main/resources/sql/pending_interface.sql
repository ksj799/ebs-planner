-- Mass Load 처리 전(1=대기, 2=실행중) 작업지시 생성 요청. 이중 생성을 막기 위해 공급으로 본다
SELECT wjsi.interface_id,
       wjsi.group_id,
       wjsi.primary_item_id,
       msi.segment1    AS item_code,
       msi.description AS item_desc,
       wjsi.status_type,
       wjsi.start_quantity,
       wjsi.first_unit_start_date,
       wjsi.last_unit_completion_date,
       wjsi.source_code
  FROM wip_job_schedule_interface wjsi
  JOIN mtl_system_items_b msi ON msi.inventory_item_id = wjsi.primary_item_id
                             AND msi.organization_id   = wjsi.organization_id
 WHERE wjsi.organization_id = :orgId
   AND wjsi.load_type       = 1
   AND wjsi.process_status IN (1, 2)
