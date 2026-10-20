-- 미완료 작업지시: 1=미해제, 3=해제, 6=보류
SELECT wdj.wip_entity_id,
       we.wip_entity_name,
       wdj.primary_item_id,
       msi.segment1    AS item_code,
       msi.description AS item_desc,
       wdj.status_type,
       wdj.start_quantity - NVL(wdj.quantity_completed, 0) - NVL(wdj.quantity_scrapped, 0) AS remain_quantity,
       wdj.scheduled_start_date,
       wdj.scheduled_completion_date,
       wdj.source_code
  FROM wip_discrete_jobs wdj
  JOIN wip_entities we        ON we.wip_entity_id      = wdj.wip_entity_id
  JOIN mtl_system_items_b msi ON msi.inventory_item_id = wdj.primary_item_id
                             AND msi.organization_id   = wdj.organization_id
 WHERE wdj.organization_id = :orgId
   AND wdj.status_type IN (1, 3, 6)
   AND wdj.start_quantity - NVL(wdj.quantity_completed, 0) - NVL(wdj.quantity_scrapped, 0) > 0
 ORDER BY wdj.scheduled_completion_date
