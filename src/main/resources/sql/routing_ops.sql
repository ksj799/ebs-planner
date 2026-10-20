-- 주 라우팅의 공정별 자원 사용량 (병목 자원 산정용)
SELECT msi.inventory_item_id,
       msi.segment1            AS item_code,
       msi.description         AS item_desc,
       msi.primary_uom_code,
       bos.operation_seq_num,
       bos.department_id,
       bores.resource_id,
       br.unit_of_measure      AS resource_uom,
       bores.usage_rate_or_amount,
       bores.basis_type
  FROM mtl_system_items_b msi
  JOIN bom_operational_routings rt   ON rt.assembly_item_id = msi.inventory_item_id
                                    AND rt.organization_id  = msi.organization_id
                                    AND rt.alternate_routing_designator IS NULL
  JOIN bom_operation_sequences bos   ON bos.routing_sequence_id = rt.common_routing_sequence_id
  JOIN bom_operation_resources bores ON bores.operation_sequence_id = bos.operation_sequence_id
  JOIN bom_resources br              ON br.resource_id = bores.resource_id
 WHERE msi.organization_id   = :orgId
   AND msi.inventory_item_id IN (:itemIds)
   AND msi.build_in_wip_flag = 'Y'
   AND bos.effectivity_date <= :today
   AND (bos.disable_date IS NULL OR bos.disable_date > :today)
 ORDER BY msi.inventory_item_id, bos.operation_seq_num
