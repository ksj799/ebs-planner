-- 미완료 작업지시의 미출고 자재 소요 (팬텀 제외)
SELECT wro.inventory_item_id,
       we.wip_entity_name,
       wro.date_required,
       wro.required_quantity - NVL(wro.quantity_issued, 0) AS open_quantity
  FROM wip_requirement_operations wro
  JOIN wip_discrete_jobs wdj ON wdj.wip_entity_id = wro.wip_entity_id
  JOIN wip_entities we       ON we.wip_entity_id  = wro.wip_entity_id
 WHERE wro.organization_id = :orgId
   AND wdj.status_type IN (1, 3, 6)
   AND NVL(wro.wip_supply_type, 1) <> 6
   AND wro.required_quantity - NVL(wro.quantity_issued, 0) > 0
 ORDER BY wro.date_required
