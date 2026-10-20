-- 제조품목 주 BOM 1단계 구성품 (팬텀 제외)
SELECT bsb.assembly_item_id,
       bcb.component_item_id,
       bcb.component_quantity,
       NVL(bcb.component_yield_factor, 1) AS component_yield_factor
  FROM bom_structures_b bsb
  JOIN bom_components_b bcb   ON bcb.bill_sequence_id = bsb.common_bill_sequence_id
  JOIN mtl_system_items_b asy ON asy.inventory_item_id = bsb.assembly_item_id
                             AND asy.organization_id   = bsb.organization_id
 WHERE bsb.organization_id = :orgId
   AND bsb.alternate_bom_designator IS NULL
   AND asy.build_in_wip_flag = 'Y'
   AND bcb.implementation_date IS NOT NULL
   AND bcb.effectivity_date <= :today
   AND (bcb.disable_date IS NULL OR bcb.disable_date > :today)
   AND NVL(bcb.wip_supply_type, 1) <> 6
